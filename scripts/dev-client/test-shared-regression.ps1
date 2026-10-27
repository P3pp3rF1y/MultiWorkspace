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
    $invalidSelections = @(
        @{ name = 'duplicate'; arguments = @('-RegressionSuite','proof-first,proof-first') },
        @{ name = 'empty'; arguments = @('-RegressionSuite','proof-first,') },
        @{ name = 'manifest-suite'; arguments = @('-RegressionSuite','proof-first','-Suite','legacy') },
        @{ name = 'manifest-group'; arguments = @('-RegressionSuite','proof-first','-Group','legacy') },
        @{ name = 'manifest-list'; arguments = @('-RegressionSuite','proof-first','-List') },
        @{ name = 'unselected-borrow'; arguments = @('-BaseUrl','http://127.0.0.1:1') },
        @{ name = 'unselected-keep'; arguments = @('-KeepClientOpen') }
    )
    foreach ($selection in $invalidSelections) {
        $arguments = @('-NoProfile','-File',"$harness/run-regression.ps1",'-WorkspaceRoot',$WorkspaceRoot) + $selection.arguments
        $output = @(& pwsh @arguments 2>&1)
        if ($LASTEXITCODE -eq 0 -or ($output -join ' ') -notmatch 'RegressionSuite') { throw "Invalid selection was accepted: $($selection.name)" }
        $cases += @{ mode = $selection.name; ok = $true }
    }
    foreach ($mode in 'success','fail-fast','continue','borrowed','borrowed-failure','keep','qualified-skip','unqualified-skip','mixed-skip','mixed-error','shutdown-failure') {
        $directory = Join-Path $root $mode
        $null = New-Item -ItemType Directory -Force -Path (Join-Path $directory 'workspace/run')
        $env:DCA_LIFECYCLE_DIRECTORY = $directory
        $env:DCA_LIFECYCLE_MODE = if ($mode -in 'fail-fast','continue','borrowed-failure') { 'shared-cleanup-error' } elseif ($mode -eq 'qualified-skip') { 'shared-qualified-skip' } elseif ($mode -eq 'unqualified-skip') { 'shared-unqualified-skip' } elseif ($mode -in 'mixed-skip','mixed-error') { "shared-$mode" } elseif ($mode -eq 'shutdown-failure') { $mode } else { 'success' }
        $fixture = Join-Path $PSScriptRoot 'test-fixtures/lifecycle-client.ps1'
        "@pwsh -NoProfile -File `"$fixture`" -Directory `"$directory`" -Mode `"$env:DCA_LIFECYCLE_MODE`"" | Set-Content -LiteralPath (Join-Path $directory 'gradlew.bat')
        $arguments = @('-NoProfile','-File',"$harness/run-regression.ps1",'-WorkspaceRoot',$WorkspaceRoot,'-RegressionSuite','proof-first,proof-second,proof-third','-ArtifactDirectory',"$directory/reports",'-CandidateIdentity','candidate-proof','-RuntimeBuildIdentity','runtime-proof')
        $env:DCA_LIFECYCLE_ARTIFACT_DIRECTORY = "$directory/reports"
        if ($mode -eq 'unqualified-skip') { $arguments += '-ContinueOnFailure' }
        $ownership = $null
        try {
            if ($mode -in 'borrowed','borrowed-failure') {
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
            $expected = if ($mode -in 'fail-fast','mixed-error','borrowed-failure') { 2 } else { 3 }
            if ($reports.Count -ne $expected) { throw "${mode}: lost native suite reports: $($output -join ' ')" }
            if (($exit -eq 0) -ne ($mode -notin 'fail-fast','continue','unqualified-skip','mixed-error','shutdown-failure','borrowed-failure')) { throw "${mode}: wrong outcome: $($output -join ' ')" }
            $urls = @($reports.launchIdentity.baseUrl | Select-Object -Unique)
            if ($urls.Count -ne 1) { throw "${mode}: suites did not reuse one client" }
            foreach ($report in $reports) { if ($report.bindings.candidateIdentity -ne 'candidate-proof' -or $report.bindings.runtimeBuildIdentity -ne 'runtime-proof') { throw 'Native report binding was lost' } }
            foreach ($file in $files) {
                $xml = [IO.Path]::ChangeExtension($file, '.junit.xml')
                if (-not (Test-Path -LiteralPath $xml -PathType Leaf)) { throw "${mode}: native JUnit report missing for $file" }
                $null = [xml](Get-Content -LiteralPath $xml -Raw)
            }
            if ($mode -in 'fail-fast','continue','borrowed-failure') {
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
            $lifecyclePaths = @([IO.Directory]::GetFiles("$directory/reports", 'client-lifecycle.json', [IO.SearchOption]::AllDirectories))
            if ($mode -in 'borrowed','borrowed-failure') {
                if ($recordPaths.Count -or $lifecyclePaths.Count) { throw 'Borrowed BaseUrl claimed ownership or cleanup' }
            } else {
                if ($recordPaths.Count -ne 1 -or $lifecyclePaths.Count -ne 1) { throw "${mode}: owned launch did not persist ownership and cleanup records" }
                $ownedRecord = Get-Content -LiteralPath $recordPaths[0] -Raw | ConvertFrom-Json
                if (-not $ownedRecord.launcherStartedAtUtc -or -not $ownedRecord.processStartedAtUtc -or $ownedRecord.processId -le 0 -or $ownedRecord.baseUrl -ne $urls[0]) { throw "${mode}: unusable owned-client record" }
                $lifecycle = Get-Content -LiteralPath $lifecyclePaths[0] -Raw | ConvertFrom-Json
                if ($lifecycle.ok -ne ($mode -ne 'shutdown-failure')) { throw "${mode}: incorrect cleanup evidence" }
            }
            $ownership = Get-Content -LiteralPath "$directory/ownership.json" -Raw | ConvertFrom-Json
            $requests = @(Get-Content -LiteralPath "$directory/requests.jsonl" | ForEach-Object { $_ | ConvertFrom-Json })
            if ($mode -notin 'borrowed','borrowed-failure' -and @($requests | Where-Object { $_.path -eq '/regression/run' -and $_.ownershipBeforeCase -ne $true }).Count) { throw "${mode}: ownership was not durable before the first case" }
            if ($mode -in 'borrowed','borrowed-failure' -and @($requests | Where-Object { $_.path -eq '/regression/run' -and $_.ownershipBeforeCase -ne $false }).Count) { throw 'Borrowed client gained owned launch record' }
            $stops = @($requests | Where-Object path -EQ '/client/stop').Count
            if ($stops -ne $(if ($mode -in 'borrowed','borrowed-failure','keep') { 0 } else { 1 })) { throw "${mode}: wrong owned-client shutdown count" }
            $alive = $null -ne (Get-OwnedClientProcess $ownership.processId $ownership.processStartedAtUtc)
            if ($alive -ne ($mode -in 'borrowed','borrowed-failure','keep')) { throw "${mode}: incorrect client lifetime" }
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
