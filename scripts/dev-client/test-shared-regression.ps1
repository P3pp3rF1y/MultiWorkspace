param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot/../..").Path)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/client-lifecycle.ps1"
$root = Join-Path $WorkspaceRoot "build/reports/dev-client-regression/shared-client/$([guid]::NewGuid())"
$harness = Join-Path $root 'scripts/dev-client'
$null = New-Item -ItemType Directory -Force -Path $harness
Copy-Item "$PSScriptRoot/run-regression.ps1", "$PSScriptRoot/regression.ps1", "$PSScriptRoot/client-lifecycle.ps1" $harness
Copy-Item "$PSScriptRoot/test-fixtures/lifecycle-launch.ps1" (Join-Path $harness 'start-and-ready.ps1')
$old = @{}
foreach ($name in 'DCA_LIFECYCLE_SOURCE','DCA_LIFECYCLE_DIRECTORY','DCA_LIFECYCLE_MODE','DCA_LIFECYCLE_ARTIFACT_DIRECTORY') { $old[$name] = [Environment]::GetEnvironmentVariable($name) }
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
        $env:DCA_LIFECYCLE_ARTIFACT_DIRECTORY = "$directory/reports"
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
            if ([IO.Directory]::GetFiles("$directory/reports", 'run.json', [IO.SearchOption]::AllDirectories).Count) { throw "${mode}: unexpected aggregate report" }
            $files = @([IO.Directory]::GetFiles("$directory/reports", '*.json', [IO.SearchOption]::AllDirectories) | Where-Object {
                [IO.Path]::GetFileName($_) -in 'proof-first.json','proof-second.json','proof-third.json'
            } | Sort-Object { [IO.Path]::GetFileName($_) })
            $reports = @($files | ForEach-Object { Get-Content -LiteralPath $_ -Raw | ConvertFrom-Json -Depth 64 })
            $expected = if ($mode -in 'fail-fast','mixed-error') { 2 } else { 3 }
            if ($reports.Count -ne $expected) { throw "${mode}: lost native suite reports: $($output -join ' ')" }
            if (($exit -eq 0) -ne ($mode -notin 'fail-fast','continue','unqualified-skip','mixed-error')) { throw "${mode}: wrong outcome: $($output -join ' ')" }
            $urls = @($reports.launchIdentity.baseUrl | Select-Object -Unique)
            if ($urls.Count -ne 1) { throw "${mode}: suites did not reuse one client" }
            foreach ($report in $reports) { if ($report.bindings.candidateIdentity -ne 'candidate-proof' -or $report.bindings.runtimeBuildIdentity -ne 'runtime-proof') { throw 'Native report binding was lost' } }
            if ($mode -in 'fail-fast','continue') {
                if ($reports[0].outcome -ne 'passed' -or $reports[1].outcome -ne 'error' -or $reports[1].results[0].category -ne 'cleanup') { throw 'Framework cleanup error was hidden or reinterpreted' }
            }
            if ($mode -eq 'qualified-skip' -and ($reports[1].outcome -ne 'skipped' -or $reports[1].results[0].category -ne 'expectedUnavailable' -or $reports[2].outcome -ne 'passed')) { throw 'Qualified external-unavailability did not allow the next suite' }
            if ($mode -eq 'unqualified-skip' -and ($reports[1].outcome -ne 'skipped' -or $reports[1].results[0].category -ne 'other' -or $reports[2].outcome -ne 'passed')) { throw 'Unqualified skip was reinterpreted or aborted continuation' }
            if ($mode -in 'mixed-skip','mixed-error') {
                $native = $reports[1]
                if ($native.accounting.selected -ne 3 -or $native.accounting.attempted -ne 3 -or $native.accounting.skipped -ne 1 -or
                    $native.results[0].outcome -ne 'passed' -or $native.results[1].outcome -ne 'skipped' -or
                    $native.results[1].category -ne 'expectedUnavailable' -or @($native.results[1].missingRequirements)[0] -ne 'external-mod') { throw "${mode}: native cases or qualification were lost" }
                if ($mode -eq 'mixed-skip' -and ($native.outcome -ne 'skipped' -or
                    $native.accounting.passed -ne 2 -or $native.results[2].outcome -ne 'passed' -or
                    $reports[2].outcome -ne 'passed')) { throw 'Mixed qualified skip did not continue with native accounting' }
                if ($mode -eq 'mixed-error' -and ($native.accounting.error -ne 1 -or $native.results[2].outcome -ne 'error')) { throw 'Real error alongside qualified skip was accepted' }
            }
            $recordPaths = @([IO.Directory]::GetFiles("$directory/reports", 'client-ownership.json', [IO.SearchOption]::AllDirectories))
            if ($mode -eq 'borrowed') {
                if ($recordPaths.Count) { throw 'Borrowed BaseUrl claimed ownership' }
            } else {
                if ($recordPaths.Count -ne 1) { throw "${mode}: owned launch did not persist exactly one ownership record" }
                $ownedRecord = Get-Content -LiteralPath $recordPaths[0] -Raw | ConvertFrom-Json
                if (-not $ownedRecord.launcherStartedAtUtc -or -not $ownedRecord.processStartedAtUtc -or $ownedRecord.processId -le 0 -or $ownedRecord.baseUrl -ne $urls[0]) { throw "${mode}: unusable owned-client record" }
            }
            $ownership = Get-Content -LiteralPath "$directory/ownership.json" -Raw | ConvertFrom-Json
            $requests = @(Get-Content -LiteralPath "$directory/requests.jsonl" | ForEach-Object { $_ | ConvertFrom-Json })
            if ($mode -ne 'borrowed' -and @($requests | Where-Object { $_.path -eq '/regression/run' -and $_.ownershipBeforeCase -ne $true }).Count) { throw "${mode}: ownership was not durable before the first case" }
            if ($mode -eq 'borrowed' -and @($requests | Where-Object { $_.path -eq '/regression/run' -and $_.ownershipBeforeCase -ne $false }).Count) { throw 'Borrowed client gained owned launch record' }
            $stops = @($requests | Where-Object path -EQ '/client/stop').Count
            if ($stops -ne $(if ($mode -in 'borrowed','keep') { 0 } else { 1 })) { throw "${mode}: wrong owned-client shutdown count" }
            $expectedRequests = $expected + $(if ($mode -in 'mixed-skip','mixed-error') { 2 } else { 0 })
            if (@($requests | Where-Object path -EQ '/regression/run').Count -ne $expectedRequests) { throw 'Unexpected extra regression checks were executed' }
            $suiteOrder = @($requests | Where-Object path -EQ '/regression/run' | ForEach-Object suiteId | Select-Object -Unique)
            if (($suiteOrder -join ',') -ne (@('proof-first','proof-second','proof-third')[0..($expected - 1)] -join ',')) { throw 'Suite execution order changed' }
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
