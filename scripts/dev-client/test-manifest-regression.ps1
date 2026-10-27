param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path)

$ErrorActionPreference = 'Stop'
$directory = Join-Path $WorkspaceRoot ('build/reports/dev-client-regression/manifest/' + [guid]::NewGuid())
$null = New-Item -ItemType Directory -Force -Path $directory
Copy-Item -LiteralPath "$PSScriptRoot/run-regression.ps1" -Destination "$directory/run-regression.ps1"
@'
param($WorkspaceRoot, $Loader, $TimeoutSeconds, $Mode, $ApiKey)
Add-Content -LiteralPath "$PSScriptRoot/calls.txt" -Value "$Mode/$Loader/$TimeoutSeconds"
switch ($Mode) {
    'throw' { throw "Child failed $ApiKey" }
    'error' { Write-Error "Child error $ApiKey" -ErrorAction Continue }
    'empty' { return }
    'multiple' { [pscustomobject]@{ ok = $true } }
    'false-ok' { [pscustomobject]@{ ok = $false }; return }
    'false-passed' { [pscustomobject]@{ passed = $false }; return }
}
Write-Warning "Warning $ApiKey"
Write-Information "Information $ApiKey" -InformationAction Continue
[pscustomobject]@{ ok = $true; baseUrl = 'http://user:credential@localhost/?access_token=hidden'; message = "Result $ApiKey"; apiKey = $ApiKey }
'@ | Set-Content -LiteralPath "$directory/child.ps1" -Encoding utf8

function Assert-True { param([bool]$Condition, [string]$Message) if (-not $Condition) { throw $Message } }
function Write-Manifest {
    param([string]$Mode)
    [pscustomobject]@{ protocolVersion = 1; suites = @(
        [pscustomobject]@{ id = 'first'; loaders = @('forge'); tier = 'fast'; groups = @('proof'); script = 'child.ps1'; arguments = @{ Mode = $Mode; ApiKey = 'fixture-secret' } },
        [pscustomobject]@{ id = 'second'; loaders = @('forge'); tier = 'fast'; groups = @('proof'); script = 'child.ps1'; arguments = @{ Mode = 'success'; ApiKey = 'fixture-secret' } },
        [pscustomobject]@{ id = 'foreign'; loaders = @('neoforge'); tier = 'fast'; groups = @('proof'); script = 'missing.ps1'; arguments = @{} }
    ) } | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath "$directory/regression-suites.json" -Encoding utf8
    if (Test-Path -LiteralPath "$directory/calls.txt") { Remove-Item -LiteralPath "$directory/calls.txt" }
}
function Invoke-Fixture {
    param([hashtable]$Arguments = @{}, [int]$TimeoutSeconds = 9)
    $captured = [System.Collections.Generic.List[object]]::new()
    $failed = $false
    try {
        & "$directory/run-regression.ps1" -WorkspaceRoot $directory -TimeoutSeconds $TimeoutSeconds -CandidateIdentity candidate-proof -RuntimeBuildIdentity runtime-proof @Arguments *>&1 |
            ForEach-Object { [void]$captured.Add($_) }
    } catch { $failed = $true; [void]$captured.Add($_) }
    $summaries = @($captured | Where-Object { $_ -is [pscustomobject] -and $null -ne $_.accounting })
    $text = $captured | Out-String -Width 4096
    foreach ($secret in @('fixture-secret', 'user:credential', 'hidden')) {
        Assert-True (-not $text.Contains($secret)) "A child secret leaked: $secret"
    }
    return [pscustomobject]@{ failed = $failed; records = $captured; summaries = $summaries }
}

foreach ($mode in @('success', 'throw', 'error', 'empty', 'multiple', 'false-ok', 'false-passed')) {
    foreach ($continue in @($false, $true)) {
        Write-Manifest $mode
        $run = Invoke-Fixture @{ ContinueOnFailure = $continue }
        Assert-True ($run.summaries.Count -eq 1) "$mode/$continue did not return one summary."
        $summary = $run.summaries[0]
        $success = $mode -eq 'success'
        $expectedAttempts = if ($success -or $continue) { 2 } else { 1 }
        Assert-True ($run.failed -ne $success -and $summary.ok -eq $success) "$mode/$continue returned the wrong outcome."
        Assert-True ($summary.accounting.selected -eq 2 -and $summary.accounting.attempted -eq $expectedAttempts -and
            $summary.accounting.notRun -eq (2 - $expectedAttempts) -and $summary.accounting.failed -eq [int](-not $success)) "$mode/$continue lost accounting."
        Assert-True (@(Get-Content -LiteralPath "$directory/calls.txt").Count -eq $expectedAttempts) "$mode/$continue ran unexpected children."
        Assert-True ($summary.bindings.candidateIdentity -eq 'candidate-proof' -and $summary.bindings.runtimeBuildIdentity -eq 'runtime-proof' -and
            $summary.selection.manifestSha256.Length -eq 64 -and $summary.selection.invocation.boundParameters.TimeoutSeconds -eq 9 -and
            $summary.runIdentity.controllerRunId -and $summary.runIdentity.finishedAtUtc) "$mode/$continue lost provenance."
        if ($success) {
            Assert-True ($summary.results[0].diagnostics.Count -eq 2 -and $summary.results[0].executionArguments.Loader -eq 'forge' -and
                $summary.results[0].executionArguments.TimeoutSeconds -eq 9 -and $summary.results[0].result.ok) 'Streams were mistaken for products or launch defaults changed.'
        }
    }
}
Write-Manifest success
$run = Invoke-Fixture @{ Suite = @('second') }
Assert-True ($run.summaries[0].accounting.selected -eq 1 -and $run.summaries[0].results[0].id -eq 'second') 'Explicit manifest selection was inaccurate.'
Write-Manifest success
$run = Invoke-Fixture @{ Group = @('proof'); List = $true }
Assert-True (-not $run.failed -and -not (Test-Path -LiteralPath "$directory/calls.txt") -and
    @($run.records | Where-Object { $_.id -in @('first', 'second') }).Count -eq 2) 'List launched children or included a foreign loader.'
$discoveryPaths = @('workspace/run/regressionClient/dev-client-automation.json', 'workspace/run/dev-client-automation.json', 'workspace/run/client/dev-client-automation.json')
foreach ($relativePath in $discoveryPaths) {
    foreach ($mode in @('success', 'false-ok')) {
        Write-Manifest $mode
        $path = Join-Path $directory $relativePath
        $null = New-Item -ItemType Directory -Force -Path (Split-Path $path)
        @{ processId = $PID } | ConvertTo-Json | Set-Content -LiteralPath $path
        $run = Invoke-Fixture @{ ContinueOnFailure = $true } -TimeoutSeconds 0
        Assert-True ($run.failed -and $run.summaries[0].results[1].outcome -eq 'failed') "$relativePath/$mode accepted a live previous client."
        Assert-True (@(Get-Content -LiteralPath "$directory/calls.txt").Count -eq 1) "$relativePath/$mode launched another child while a previous client remained alive."
        Assert-True ($run.summaries[0].results[1].error -like '*previous dev client process*') 'The guard lost its timeout diagnosis.'
        Remove-Item -LiteralPath $path
    }
}
Write-Manifest success
$primary = Join-Path $directory $discoveryPaths[0]
$fallback = Join-Path $directory $discoveryPaths[1]
'{' | Set-Content -LiteralPath $primary
@{ processId = $PID } | ConvertTo-Json | Set-Content -LiteralPath $fallback
$run = Invoke-Fixture @{ ContinueOnFailure = $true } -TimeoutSeconds 0
Assert-True ($run.failed -and @(Get-Content -LiteralPath "$directory/calls.txt").Count -eq 1) 'Malformed primary discovery masked a live fallback client.'
Remove-Item -LiteralPath $primary, $fallback
Write-Manifest success
@{ processId = [int]::MaxValue } | ConvertTo-Json | Set-Content -LiteralPath $primary
$run = Invoke-Fixture -TimeoutSeconds 0
Assert-True (-not $run.failed -and @(Get-Content -LiteralPath "$directory/calls.txt").Count -eq 2) 'Stale discovery blocked an already stopped client.'
Remove-Item -LiteralPath $primary
foreach ($mode in @('empty-selection', 'bad-version', 'malformed', 'missing-script')) {
    Write-Manifest success
    $arguments = @{}
    switch ($mode) {
        'empty-selection' { $arguments.Suite = @('missing') }
        'bad-version' { '{"protocolVersion":99,"suites":[]}' | Set-Content -LiteralPath "$directory/regression-suites.json" }
        'malformed' { '{' | Set-Content -LiteralPath "$directory/regression-suites.json" }
        'missing-script' { Remove-Item -LiteralPath "$directory/child.ps1" }
    }
    $run = Invoke-Fixture $arguments
    Assert-True ($run.failed -and $run.summaries.Count -eq 1 -and -not $run.summaries[0].ok) "$mode did not produce a failure summary."
    Assert-True (-not (Test-Path -LiteralPath "$directory/calls.txt")) "$mode launched an unexpected child."
}
[pscustomobject]@{ ok = $true; scenarios = 28; artifacts = $directory }
