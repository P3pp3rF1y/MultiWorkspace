param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$WorldName = 'DevClientPortFullIndex20261023',
    [string]$ArtifactDirectory = (Join-Path $WorkspaceRoot 'DevClientAutomation\build\reports\port-diagnostic-whole-index'),
    [string]$CandidateIdentity,
    [string]$RuntimeBuildIdentity
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\client-lifecycle.ps1"
$ready = $null
$results = [System.Collections.Generic.List[object]]::new()
$restartSuite = 'linked-storage-restart'
$prepare = 'prepare-canonical-mob-and-stashed-items'
$verify = 'verify-canonical-mob-and-stashed-items-after-server-restart'
$catalog = $null
$preparationToken = $null
$boots = [System.Collections.Generic.List[object]]::new()
$failure = $null
$total = [ordered]@{ selected = 0; attempted = 0; passed = 0; failed = 0; error = 0; blocked = 0; cancelled = 0; skipped = 0; notRun = 0 }

function Invoke-IndexedCase {
    param([string]$Suite, [string]$Test, [string]$Token)
    $arguments = @{ WorkspaceRoot = $WorkspaceRoot; BaseUrl = $ready.baseUrl; Suite = $Suite; ArtifactDirectory = $ArtifactDirectory; TimeoutSeconds = 180;
        CandidateIdentity = $CandidateIdentity; RuntimeBuildIdentity = $RuntimeBuildIdentity }
    if ($Test) { $arguments.Test = $Test }
    if ($Token) { $arguments.PreparationToken = $Token }
    try {
        $report = & "$PSScriptRoot\regression.ps1" run @arguments
        $path = Join-Path $ArtifactDirectory $(if ($Test) { "$Suite-$Test.json" } else { "$Suite.json" })
        $saved = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
        if ($saved.bindings.candidateIdentity -cne $CandidateIdentity -or $saved.bindings.runtimeBuildIdentity -cne $RuntimeBuildIdentity) {
            throw "Regression artifact identity mismatch: $path"
        }
        if (-not $report.ok -or -not $saved.ok) { throw "Regression failed: $path" }
        $results.Add([pscustomobject]@{ suite = $Suite; test = $Test; accounting = $report.accounting; ok = $report.ok; error = $null })
    } catch {
        $name = if ($Test) { "$Suite-$Test" } else { $Suite }
        $path = Join-Path $ArtifactDirectory "$name.json"
        $accounting = if (Test-Path -LiteralPath $path) { (Get-Content -LiteralPath $path -Raw | ConvertFrom-Json).accounting } else { $null }
        $results.Add([pscustomobject]@{ suite = $Suite; test = $Test; accounting = $accounting; ok = $false; error = $_.Exception.Message })
    }
}

if ([string]::IsNullOrWhiteSpace($CandidateIdentity) -or [string]::IsNullOrWhiteSpace($RuntimeBuildIdentity)) {
    throw 'Indexed validation requires both CandidateIdentity and RuntimeBuildIdentity; unbound diagnostics cannot be accepted.'
}
try {
try {
    $ready = & "$PSScriptRoot\start-and-ready.ps1" -WorkspaceRoot $WorkspaceRoot -WorldName $WorldName -CloseOnExit -SkipRecipeViewerReady -TimeoutSeconds 360
    if (-not $ready.state.playerLoaded -or -not $ready.capabilities.backgroundInput) { throw 'Dedicated first boot is not ready.' }
    $catalog = & "$PSScriptRoot\regression.ps1" catalog -BaseUrl $ready.baseUrl
    $caseCount = @($catalog.suiteTests.PSObject.Properties | ForEach-Object { @($_.Value).Count } | Measure-Object -Sum)[0].Sum
    if (@($catalog.suites).Count -ne 24 -or $caseCount -ne 113) {
        throw 'Unexpected indexed suite/case count.'
    }
    foreach ($suite in @($catalog.suites)) {
        if ($suite -ne $restartSuite) { Invoke-IndexedCase -Suite $suite }
    }
    Invoke-IndexedCase -Suite $restartSuite -Test $prepare
    if (@($results | Where-Object { $_.suite -eq $restartSuite -and $_.test -eq $prepare -and $_.ok }).Count -ne 1) {
        throw 'Restart preparation failed; cannot restart and verify.'
    }
    $status = Invoke-RestMethod -Uri "$($ready.baseUrl)/regression/status" -Method Get -TimeoutSec 30
    $preparationToken = $status.diagnostics.restartPreparationToken
    if (-not [guid]::TryParse([string]$preparationToken, [ref]([guid]::Empty))) {
        throw 'Restart preparation did not return a valid in-memory token.'
    }
} finally {
    if ($ready) {
        $stopped = Stop-OwnedClient -Ownership $ready.ownership
        $boots.Add([pscustomobject]@{ boot = 1; processId = $ready.processId; teardown = $stopped.ok; errors = @($stopped.errors) })
        $ready = $null
        if (-not $stopped.ok) { throw "First boot teardown: $($stopped.errors -join '; ')" }
    }
}

try {
    $ready = & "$PSScriptRoot\start-and-ready.ps1" -WorkspaceRoot $WorkspaceRoot -WorldName $WorldName -CloseOnExit -SkipRecipeViewerReady -TimeoutSeconds 360
    if (-not $ready.state.playerLoaded) { throw 'Dedicated second boot is not ready.' }
    Invoke-IndexedCase -Suite $restartSuite -Test $verify -Token $preparationToken
} finally {
    if ($ready) {
        $stopped = Stop-OwnedClient -Ownership $ready.ownership
        $boots.Add([pscustomobject]@{ boot = 2; processId = $ready.processId; teardown = $stopped.ok; errors = @($stopped.errors) })
        $ready = $null
        if (-not $stopped.ok) { throw "Second boot teardown: $($stopped.errors -join '; ')" }
    }
}

foreach ($entry in $results) {
    if (-not $entry.accounting) { throw "No accounting for $($entry.suite)/$($entry.test): $($entry.error)" }
    foreach ($name in @($total.Keys)) { $total[$name] += [int]$entry.accounting.$name }
}
if ($total.selected -ne $caseCount) { throw "Indexed accounting $($total.selected) does not cover $caseCount cases." }
if (@($results | Where-Object { -not $_.ok }).Count) { throw 'Indexed validation contains failed or unbound suite reports.' }
if ($total.passed -ne $caseCount) {
    throw "Indexed validation failed: $($total.passed)/$caseCount passed; inspect $ArtifactDirectory."
}
} catch {
    $failure = $_.Exception.Message
    throw
} finally {
    if (-not (Test-Path -LiteralPath $ArtifactDirectory)) { New-Item -ItemType Directory -Path $ArtifactDirectory -Force | Out-Null }
    $receipt = [pscustomobject]@{ candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity;
        world = $WorldName; suites = $(if ($catalog) { @($catalog.suites).Count } else { 0 }); expectedCases = 113;
        accounting = $total; entries = @($results); boots = @($boots); restartVerified = @($results | Where-Object { $_.suite -eq $restartSuite -and $_.test -eq $verify -and $_.ok }).Count -eq 1;
        ok = ($null -eq $failure -and $total.selected -eq 113 -and $total.passed -eq 113 -and $total.failed -eq 0 -and $total.error -eq 0 -and
            @($results | Where-Object { -not $_.ok }).Count -eq 0 -and $boots.Count -eq 2 -and $boots[0].processId -ne $boots[1].processId -and
            @($boots | Where-Object { -not $_.teardown }).Count -eq 0);
        failure = $failure }
    $receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $ArtifactDirectory 'indexed-aggregate.json') -Encoding utf8
    if ($null -eq $failure) { $receipt }
}
