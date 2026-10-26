param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot/../..").Path)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/client-lifecycle.ps1"
$root = Join-Path $WorkspaceRoot "build/reports/dev-client-regression/client-lifecycle/$([guid]::NewGuid())"
$harness = Join-Path $root 'runner'
New-Item -ItemType Directory -Force -Path $harness | Out-Null
Copy-Item "$PSScriptRoot/regression.ps1", "$PSScriptRoot/client-lifecycle.ps1" $harness
Copy-Item "$PSScriptRoot/test-fixtures/lifecycle-launch.ps1" (Join-Path $harness 'start-and-ready.ps1')
$previousEnvironment = @{}
foreach ($name in @('DCA_LIFECYCLE_SOURCE', 'DCA_LIFECYCLE_DIRECTORY', 'DCA_LIFECYCLE_MODE')) { $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name) }
$env:DCA_LIFECYCLE_SOURCE = $PSScriptRoot
$cases = @()
try {
    $cases += @(& {
        # Mock only discovery's process graph; no real process is launched or stopped.
        $epoch = [datetime]::Parse('2026-10-25T12:00:00Z').ToUniversalTime()
        function Edge([int]$id, [int]$minutes) {
            [pscustomobject]@{ ProcessId = $id; CreationDate = $epoch.AddMinutes($minutes) }
        }
        function Get-OwnedClientProcess([int]$ProcessId, [object]$StartedAtUtc) {
            if ($ProcessId -eq 100) { return [pscustomobject]@{ Id = 100; StartTime = $epoch } }
            return $null
        }
        function Get-CimInstance {
            param([string]$ClassName, [string]$Filter)
            $state.queries++
            if ($ClassName -ne 'Win32_Process' -or $Filter -notmatch '^ParentProcessId = (\d+)$' -or $state.queries -gt 8) { throw 'Discovery traversal exceeded its fixture graph.' }
            $parentId = [int]$Matches[1]
            if ($graph.ContainsKey($parentId)) { return $graph[$parentId] }
            return @()
        }
        function Get-Process {
            [CmdletBinding()]
            param([int]$Id)
            if ($Id -eq 400) { return [pscustomobject]@{ Id = 400; StartTime = $epoch.AddMinutes(3) } }
            throw 'Discovery requested an unrelated process.'
        }
        foreach ($scenario in @(
                @{ mode = 'cyclic-valid-tree'; graph = @{ 100 = @((Edge 200 2)); 200 = @((Edge 300 2)); 300 = @((Edge 200 2), (Edge 400 3)) }; client = 400; accepted = $true },
                @{ mode = 'recycled-parent'; graph = @{ 100 = @((Edge 200 2)); 200 = @((Edge 300 1)); 300 = @((Edge 400 3)) }; client = 400; accepted = $false },
                @{ mode = 'unrelated-discovery'; graph = @{ 100 = @((Edge 200 2)); 200 = @((Edge 300 2)) }; client = 400; accepted = $false }
            )) {
            $graph = $scenario.graph
            $state = [pscustomobject]@{ queries = 0 }
            $ownership = [pscustomobject]@{ launcherProcessId = 100; launcherStartedAtUtc = $epoch; processId = 0; processStartedAtUtc = $null; baseUrl = $null }
            $accepted = $false
            try { Set-OwnedClientDiscovery $ownership ([pscustomobject]@{ processId = $scenario.client; host = '127.0.0.1'; port = 60811 }); $accepted = $true }
            catch { if (-not $_.Exception.Message.Contains('does not belong')) { throw } }
            if ($accepted -ne $scenario.accepted -or ($accepted -and $ownership.processId -ne 400) -or (-not $accepted -and $ownership.processId -ne 0)) {
                throw "$($scenario.mode) authorized an incorrect discovery"
            }
            [pscustomobject]@{ mode = $scenario.mode; ok = $true; graphQueries = $state.queries }
        }
    })
    foreach ($mode in @('success', 'discovery-overwrite', 'assertion', 'transport', 'cancel', 'interrupted', 'teardown-poll', 'cleanup-timeout', 'keep', 'borrowed', 'borrowed-assertion', 'invalid-startup', 'no-bridge', 'startup-keep', 'shutdown-failure', 'combined-failure')) {
        $directory = Join-Path $root $mode
        New-Item -ItemType Directory -Force -Path (Join-Path $directory 'workspace/run') | Out-Null
        $env:DCA_LIFECYCLE_DIRECTORY = $directory
        $env:DCA_LIFECYCLE_MODE = if ($mode -eq 'startup-keep') { 'invalid-startup' } elseif ($mode -eq 'borrowed-assertion') { 'assertion' } else { $mode }
        $fixture = Join-Path $PSScriptRoot 'test-fixtures/lifecycle-client.ps1'
        "@pwsh -NoProfile -File `"$fixture`" -Directory `"$directory`" -Mode `"$env:DCA_LIFECYCLE_MODE`"" | Set-Content -LiteralPath (Join-Path $directory 'gradlew.bat')
        $arguments = @('-NoProfile', '-File', (Join-Path $harness 'regression.ps1'), '-WorkspaceRoot', $WorkspaceRoot, '-Suite', 'proof', '-Test', 'one', '-ArtifactDirectory', (Join-Path $directory 'reports'))
        if ($mode -in @('keep', 'startup-keep')) { $arguments += '-KeepClientOpen' }
        if ($mode -eq 'cancel') { $arguments += @('-TimeoutSeconds', '0') }
        $ownership = $null
        try {
            if ($mode -in @('borrowed', 'borrowed-assertion')) {
                $ready = & "$harness/start-and-ready.ps1" -WorkspaceRoot $WorkspaceRoot
                $ownership = $ready.ownership
                $arguments += @('-BaseUrl', $ready.baseUrl)
            }
            $output = @(& pwsh @arguments 2>&1)
            $exitCode = $LASTEXITCODE
            if ($mode -eq 'borrowed' -and $exitCode -eq 0) {
                $output += @(& pwsh @arguments 2>&1)
                $exitCode = $LASTEXITCODE
            }
            $output | Out-String | Set-Content -LiteralPath (Join-Path $directory 'runner-output.txt')
            $ownership = Get-Content -LiteralPath (Join-Path $directory 'ownership.json') -Raw | ConvertFrom-Json
            $clientId = [int](Get-Content -LiteralPath (Join-Path $directory 'client.pid') -Raw)
            $alive = $null -ne (Get-Process -Id $clientId -ErrorAction SilentlyContinue)
            $kept = $mode -in @('keep', 'borrowed', 'borrowed-assertion', 'startup-keep')
            if ($alive -ne $kept) { throw "$mode left wrong client lifetime (alive=$alive)" }
            if ($kept) {
                if (-not (Get-OwnedClientProcess $ownership.processId $ownership.processStartedAtUtc)) { throw 'JSON-roundtripped ownership lost process identity' }
                if (Get-OwnedClientProcess $ownership.processId ($ownership.processStartedAtUtc.AddSeconds(1))) { throw 'Mismatched creation time authorized a process' }
            }
            $expectedSuccess = $mode -in @('success', 'discovery-overwrite', 'keep', 'borrowed')
            if (($exitCode -eq 0) -ne $expectedSuccess) { throw "$mode returned unexpected exit code $exitCode" }
            $requests = if (Test-Path (Join-Path $directory 'requests.jsonl')) { @(Get-Content -LiteralPath (Join-Path $directory 'requests.jsonl') | ForEach-Object { $_ | ConvertFrom-Json }) } else { @() }
            $stops = @($requests | Where-Object path -eq '/client/stop').Count
            if ($kept -and $stops -ne 0) { throw "$mode stopped a preserved/borrowed client" }
            if (-not $kept -and $mode -ne 'no-bridge' -and $stops -ne 1) { throw "$mode did not request exactly one shutdown" }
            if ($mode -in @('cancel', 'interrupted', 'teardown-poll')) {
                $paths = @($requests.path)
                $terminal = @($requests | Where-Object { $_.path -eq '/regression/status' -and $_.outcome -eq 'cancelled' })
                if ('/regression/cancel' -notin $paths -or $terminal.Count -eq 0 -or [array]::LastIndexOf($paths, '/client/stop') -le [array]::LastIndexOf($paths, '/regression/cancel')) { throw 'Client stopped before cancellation/cleanup completed' }
            }
            if ($mode -in @('assertion', 'combined-failure') -and ($output -join "`n") -notmatch 'Injected product assertion') { throw 'Original product failure was hidden' }
            if ($mode -in @('shutdown-failure', 'combined-failure', 'cleanup-timeout')) {
                $failure = Get-Content -LiteralPath (Join-Path $directory 'reports/proof-client-shutdown.json') -Raw | ConvertFrom-Json
                if ($failure.result.outcome -ne 'error' -or $failure.result.category -ne 'infrastructure') { throw 'Shutdown failure lost canonical error reporting' }
                if ($mode -eq 'cleanup-timeout' -and $failure.result.message -notmatch 'Cancellation/cleanup timed out') { throw 'Cleanup timeout was not reported' }
            }
            if ($mode -in @('success', 'keep', 'borrowed')) {
                $report = Get-Content -LiteralPath (Join-Path $directory 'reports/proof-one.json') -Raw | ConvertFrom-Json
                if ($report.result.rerunCommand.Contains('-BaseUrl') -ne ($mode -eq 'borrowed')) { throw 'Rerun points to a disposed owned client' }
            }
            if ($mode -eq 'borrowed') {
                $foreignLauncher = Start-Process pwsh -ArgumentList '-NoProfile', '-Command', 'Start-Sleep -Seconds 120' -PassThru -WindowStyle Hidden
                $foreignOwnership = New-ClientOwnership $foreignLauncher
                try {
                    $rejected = $false
                    try { Set-OwnedClientDiscovery $foreignOwnership ([pscustomobject]@{ processId = $clientId; host = '127.0.0.1'; port = ([uri]$ready.baseUrl).Port }) }
                    catch { $rejected = $_.Exception.Message.Contains('does not belong') }
                    if (-not $rejected) { throw 'Foreign bridge discovery was accepted' }
                } finally { $null = Stop-OwnedClient $foreignOwnership -WaitForRegression:$false -TimeoutSeconds 1 }
                if (-not (Get-Process -Id $clientId -ErrorAction SilentlyContinue)) { throw 'Rejecting discovery stopped a borrowed client' }
                $cases += [pscustomobject]@{ mode = 'foreign-discovery'; ok = $true; clientPreserved = $true }
            }
            $cases += [pscustomobject]@{ mode = $mode; ok = $true; exitCode = $exitCode; clientPreserved = $alive; stopRequests = $stops }
        } finally {
            if ($ownership) {
                $cleanup = Stop-OwnedClient $ownership -WaitForRegression:$false -TimeoutSeconds 1
                if (($ownership.processId -and (Get-Process -Id $ownership.processId -ErrorAction SilentlyContinue)) -or
                        (Get-Process -Id $ownership.launcherProcessId -ErrorAction SilentlyContinue)) { throw 'Fixture client cleanup failed' }
            }
        }
    }
    $evidence = [pscustomobject]@{ ok = $true; cases = $cases; artifacts = $root }
    $evidence | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath (Join-Path $root 'evidence.json')
    $evidence
} finally {
    foreach ($name in $previousEnvironment.Keys) { [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name]) }
}
