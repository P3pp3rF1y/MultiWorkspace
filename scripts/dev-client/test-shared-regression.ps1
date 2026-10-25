param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot/../..").Path)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/client-lifecycle.ps1"
$root = Join-Path $WorkspaceRoot "build/reports/dev-client-regression/shared-client/$([guid]::NewGuid())"
$harness = Join-Path $root 'scripts/dev-client'
$null = New-Item -ItemType Directory -Force -Path $harness
Copy-Item "$PSScriptRoot/run-regression.ps1", "$PSScriptRoot/regression.ps1", "$PSScriptRoot/client-lifecycle.ps1" $harness
Copy-Item "$PSScriptRoot/test-fixtures/lifecycle-launch.ps1" (Join-Path $harness 'start-and-ready.ps1')
$old = @{}
foreach ($name in 'DCA_LIFECYCLE_SOURCE','DCA_LIFECYCLE_DIRECTORY','DCA_LIFECYCLE_MODE','DCA_LIFECYCLE_REPORT_PATH') { $old[$name] = [Environment]::GetEnvironmentVariable($name) }
$env:DCA_LIFECYCLE_SOURCE = $PSScriptRoot
$cases = @()
try {
    foreach ($mode in 'success','fail-fast','continue','borrowed','keep','qualified-skip','unqualified-skip','mixed-skip','mixed-error') {
        $directory = Join-Path $root $mode
        $null = New-Item -ItemType Directory -Force -Path (Join-Path $directory 'workspace/run')
        $env:DCA_LIFECYCLE_DIRECTORY = $directory
        $env:DCA_LIFECYCLE_MODE = if ($mode -in 'fail-fast','continue') { 'shared-cleanup-error' } elseif ($mode -eq 'qualified-skip') { 'shared-qualified-skip' } elseif ($mode -eq 'unqualified-skip') { 'shared-unqualified-skip' } elseif ($mode -in 'mixed-skip','mixed-error') { "shared-$mode" } else { 'success' }
        $fixture = Join-Path $PSScriptRoot 'test-fixtures/lifecycle-client.ps1'
        "@pwsh -NoProfile -File `"$fixture`" -Directory `"$directory`" -Mode `"$env:DCA_LIFECYCLE_MODE`"" | Set-Content -LiteralPath (Join-Path $directory 'gradlew.bat')
        $arguments = @('-NoProfile','-File',"$harness/run-regression.ps1",'-WorkspaceRoot',$WorkspaceRoot,'-RegressionSuite','proof-first,proof-second,proof-third','-ArtifactDirectory',"$directory/reports",'-CandidateIdentity','candidate-proof','-RuntimeBuildIdentity','runtime-proof')
        $stable = $mode -in 'borrowed','keep','qualified-skip','unqualified-skip','mixed-skip','mixed-error'
        $env:DCA_LIFECYCLE_REPORT_PATH = if ($stable) { Join-Path $directory 'stable/run.json' } else { $null }
        if ($stable) { $arguments += @('-ReportPath', $env:DCA_LIFECYCLE_REPORT_PATH) }
        if ($mode -eq 'unqualified-skip') { $arguments += '-ContinueOnFailure' }
        $ownership = $null
        try {
            if ($mode -eq 'borrowed') {
                $ready = & "$harness/start-and-ready.ps1" -WorkspaceRoot $WorkspaceRoot
                $ownership = $ready.ownership; $arguments += @('-BaseUrl',$ready.baseUrl)
            }
            if ($mode -eq 'keep') { $arguments += '-KeepClientOpen' }
            if ($mode -eq 'continue') { $arguments += '-ContinueOnFailure' }
            $output = @(& pwsh @arguments 2>&1); $exit = $LASTEXITCODE
            $files = @(if ($stable) { Get-Item -LiteralPath $env:DCA_LIFECYCLE_REPORT_PATH -ErrorAction SilentlyContinue | ForEach-Object FullName } else { [IO.Directory]::GetFiles("$directory/reports", 'run.json', [IO.SearchOption]::AllDirectories) })
            if ($files.Count -ne 1) { throw "${mode}: missing single durable batch report: $($output -join ' ')" }
            if ($stable -and (Test-Path -LiteralPath "$directory/reports")) { throw "${mode}: default GUID report directory was unexpectedly created" }
            $report = Get-Content -LiteralPath $files[0] -Raw | ConvertFrom-Json -Depth 64
            $expected = if ($mode -in 'fail-fast','mixed-error') { 2 } else { 3 }
            if ($report.results.Count -ne $expected -or $report.accounting.notRun -ne 3 - $expected) { throw "${mode}: lost per-suite results/accounting" }
            if (($exit -eq 0) -ne ($mode -notin 'fail-fast','continue','unqualified-skip','mixed-error')) { throw "${mode}: wrong outcome: $($output -join ' ')" }
            $urls = @($report.results.result.launchIdentity.baseUrl | Select-Object -Unique)
            if ($urls.Count -ne 1 -or $urls[0] -ne $report.launchIdentity.baseUrl) { throw "${mode}: suites did not reuse one client" }
            foreach ($entry in $report.results) { if (-not (Test-Path -LiteralPath $entry.json) -or $entry.result.bindings.candidateIdentity -ne 'candidate-proof') { throw 'Individual report/binding was lost' } }
            if ($mode -in 'fail-fast','continue') {
                if ($report.results[0].outcome -ne 'passed' -or $report.results[1].result.outcome -ne 'error' -or $report.results[1].result.results[0].category -ne 'cleanup') { throw 'Framework cleanup error was hidden or reinterpreted' }
            }
            if ($mode -eq 'qualified-skip' -and ($report.results[1].outcome -ne 'skipped' -or $report.accounting.skipped -ne 1 -or $report.accounting.failed -ne 0 -or $report.results[2].outcome -ne 'passed')) { throw 'Qualified external-unavailability did not allow the next suite' }
            if ($mode -eq 'unqualified-skip' -and ($report.results[1].outcome -ne 'failed' -or $report.accounting.failed -ne 1 -or $report.results[2].outcome -ne 'passed')) { throw 'Unqualified skip was treated as success or aborted continuation' }
            if ($mode -in 'mixed-skip','mixed-error') {
                $native = $report.results[1].result
                if ($native.accounting.selected -ne 3 -or $native.accounting.attempted -ne 3 -or $native.accounting.skipped -ne 1 -or
                    $native.results[0].outcome -ne 'passed' -or $native.results[1].outcome -ne 'skipped' -or
                    $native.results[1].category -ne 'expectedUnavailable' -or @($native.results[1].missingRequirements)[0] -ne 'external-mod') { throw "${mode}: native cases or qualification were lost" }
                if ($mode -eq 'mixed-skip' -and ($report.results[1].outcome -ne 'skipped' -or $native.outcome -ne 'skipped' -or
                    $native.accounting.passed -ne 2 -or $native.results[2].outcome -ne 'passed' -or
                    $report.accounting.skipped -ne 1 -or $report.accounting.failed -ne 0 -or $report.results[2].outcome -ne 'passed')) { throw 'Mixed qualified skip did not continue with native accounting' }
                if ($mode -eq 'mixed-error' -and ($report.results[1].outcome -ne 'failed' -or $native.accounting.error -ne 1 -or
                    $native.results[2].outcome -ne 'error' -or $report.accounting.failed -ne 1 -or $report.accounting.notRun -ne 1)) { throw 'Real error alongside qualified skip was accepted' }
            }
            $recordPath = Join-Path (Split-Path -Parent $files[0]) 'client-ownership.json'
            if ($mode -eq 'borrowed') {
                if (Test-Path -LiteralPath $recordPath) { throw 'Borrowed BaseUrl claimed ownership' }
            } else {
                if (-not (Test-Path -LiteralPath $recordPath)) { throw "${mode}: owned launch did not persist ownership" }
                $ownedRecord = Get-Content -LiteralPath $recordPath -Raw | ConvertFrom-Json
                if (-not $ownedRecord.launcherStartedAtUtc -or -not $ownedRecord.processStartedAtUtc -or $ownedRecord.processId -le 0 -or $ownedRecord.baseUrl -ne $report.launchIdentity.baseUrl) { throw "${mode}: unusable owned-client record" }
            }
            $ownership = Get-Content -LiteralPath "$directory/ownership.json" -Raw | ConvertFrom-Json
            $requests = @(Get-Content -LiteralPath "$directory/requests.jsonl" | ForEach-Object { $_ | ConvertFrom-Json })
            if ($stable -and $mode -ne 'borrowed' -and @($requests | Where-Object { $_.path -eq '/regression/run' -and $_.ownershipBeforeCase -ne $true }).Count) { throw "${mode}: ownership was not durable before the first case" }
            if ($mode -eq 'borrowed' -and @($requests | Where-Object { $_.path -eq '/regression/run' -and $_.ownershipBeforeCase -ne $false }).Count) { throw 'Borrowed client gained owned launch record' }
            $stops = @($requests | Where-Object path -EQ '/client/stop').Count
            if ($stops -ne $(if ($mode -in 'borrowed','keep') { 0 } else { 1 })) { throw "${mode}: wrong owned-client shutdown count" }
            $expectedRequests = $expected + $(if ($mode -in 'mixed-skip','mixed-error') { 2 } else { 0 })
            if (@($requests | Where-Object path -EQ '/regression/run').Count -ne $expectedRequests) { throw 'Unexpected extra regression checks were executed' }
            $cases += @{ mode = $mode; suites = $expected; shutdowns = $stops; ok = $true }
        } finally {
            if (-not $ownership -and (Test-Path -LiteralPath "$directory/ownership.json")) { $ownership = Get-Content -LiteralPath "$directory/ownership.json" -Raw | ConvertFrom-Json }
            if ($ownership) { $null = Stop-OwnedClient $ownership -WaitForRegression:$false -TimeoutSeconds 2 }
        }
    }
    @{ ok = $true; cases = $cases; artifacts = $root } | ConvertTo-Json -Depth 8
} finally {
    foreach ($name in $old.Keys) { [Environment]::SetEnvironmentVariable($name, $old[$name]) }
}
