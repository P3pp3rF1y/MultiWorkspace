param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$WorldName = 'DevClientOptionalSkip',
    [string]$ArtifactDirectory = (Join-Path $WorkspaceRoot 'DevClientAutomation\build\reports\optional-skip'),
    [string]$CandidateIdentity,
    [string]$RuntimeBuildIdentity
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\client-lifecycle.ps1"
$ready = $null
$result = $null
if ([string]::IsNullOrWhiteSpace($CandidateIdentity) -or [string]::IsNullOrWhiteSpace($RuntimeBuildIdentity)) {
    throw 'Optional-skip probe requires candidate and runtime build identities.'
}
try {
    $ready = & "$PSScriptRoot\start-and-ready.ps1" -WorkspaceRoot $WorkspaceRoot -WorldName $WorldName -RecipeViewer none -CloseOnExit -SkipRecipeViewerReady -TimeoutSeconds 300
    $catalog = Invoke-RestMethod -Uri "$($ready.baseUrl)/regression/catalog" -TimeoutSec 15
    $suite = 'linked-storage-settings'
    $test = 'barrel-primary-backpack-settings'
    if (@($catalog.testMetadata.$suite.$test.effectiveRequirements) -notcontains 'jei') {
        throw 'Selected source-declared optional requirement is absent from the packaged catalog.'
    }
    $before = Invoke-RestMethod -Uri "$($ready.baseUrl)/state" -TimeoutSec 15
    $start = Invoke-RestMethod -Method Post -Uri "$($ready.baseUrl)/regression/run" -Body (@{ suiteId = $suite; testId = $test; conformanceFailureAction = 'menu.close'; actionFailureAfter = 'fixture.player.setGameMode'; cleanupFailure = $true } | ConvertTo-Json -Compress) -ContentType 'application/json' -TimeoutSec 15
    $after = Invoke-RestMethod -Uri "$($ready.baseUrl)/state" -TimeoutSec 15
    if ($start.outcome -ne 'skipped' -or $start.category -ne 'expectedUnavailable' -or @($start.missingRequirements) -notcontains 'jei' -or
        @($start.steps).Count -ne 0 -or @($start.conformance).Count -ne 0 -or @($start.runtimeConformance).Count -ne 0 -or
        @($start.cleanup).Count -ne 0 -or $after.playerTick -lt $before.playerTick -or $after.playerLoaded -ne $before.playerLoaded) {
        throw "Optional dependency did not skip without execution: $($start | ConvertTo-Json -Compress -Depth 8)"
    }
    $result = [pscustomobject]@{ ok = $true; candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity;
        missing = @($start.missingRequirements); steps = @($start.steps).Count; cleanup = @($start.cleanup).Count }
} finally {
    if ($null -ne $ready) {
        $stopped = Stop-OwnedClient -Ownership $ready.ownership
        if (-not $stopped.ok) { throw "Owned optional-skip probe teardown failed: $($stopped.errors -join '; ')" }
    }
}
if ($result) {
    if (-not (Test-Path -LiteralPath $ArtifactDirectory)) { New-Item -ItemType Directory -Path $ArtifactDirectory -Force | Out-Null }
    $result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $ArtifactDirectory 'optional-skip.json') -Encoding utf8
    $result
}
