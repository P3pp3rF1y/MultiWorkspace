param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$ArtifactDirectory = (Join-Path $WorkspaceRoot 'DevClientAutomation\build\reports\live-audio'),
    [string]$CandidateIdentity,
    [string]$RuntimeBuildIdentity
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\client-lifecycle.ps1"
$ready = $null
$evidence = $null
if ([string]::IsNullOrWhiteSpace($CandidateIdentity) -or [string]::IsNullOrWhiteSpace($RuntimeBuildIdentity)) {
    throw 'Live audio probe requires candidate and runtime build identities.'
}

try {
    # No world, native input, or options mutation is needed to observe the live listener.
    $ready = & "$PSScriptRoot\start-and-ready.ps1" -WorkspaceRoot $WorkspaceRoot -LoadWorld:$false -SkipRecipeViewerReady -CloseOnExit
    $deadline = (Get-Date).AddSeconds(90)
    do {
        $state = Invoke-RestMethod -Method Get -Uri "$($ready.baseUrl)/state" -TimeoutSec 15
        if ($state.soundEngineLoaded -and $state.effectiveMasterGain -eq 0) { break }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)
    $capabilities = Invoke-RestMethod -Method Get -Uri "$($ready.baseUrl)/capabilities" -TimeoutSec 15
    if (-not $capabilities.backgroundInput -or -not $state.backgroundInput) { throw 'Regression launch did not enable isolated background mode.' }
    if ($state.configuredMasterVolume -le 0) { throw 'Configured MASTER volume is zero: cannot distinguish live mute from saved preference without changing settings.' }
    if (-not $state.soundEngineLoaded -or $state.effectiveMasterGain -ne 0) {
        throw "Loaded sound engine was not muted: loaded=$($state.soundEngineLoaded), gain=$($state.effectiveMasterGain) (configured $($state.configuredMasterVolume))."
    }
    if (-not (Test-Path -LiteralPath $ArtifactDirectory)) { New-Item -ItemType Directory -Path $ArtifactDirectory -Force | Out-Null }
    $evidence = [pscustomobject]@{
        candidateIdentity = $CandidateIdentity
        runtimeBuildIdentity = $RuntimeBuildIdentity
        backgroundInput = $state.backgroundInput
        capabilitiesBackgroundInput = $capabilities.backgroundInput
        configuredMasterVolume = $state.configuredMasterVolume
        soundEngineLoaded = $state.soundEngineLoaded
        effectiveMasterGain = $state.effectiveMasterGain
        gameDirectory = $state.gameDirectory
    }
} finally {
    if ($null -ne $ready) {
        $stopped = Stop-OwnedClient -Ownership $ready.ownership
        if (-not $stopped.ok) { throw "Owned audio probe teardown failed: $($stopped.errors -join '; ')" }
    }
}
if ($evidence) {
    $evidence | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $ArtifactDirectory 'live-audio.json') -Encoding utf8
    $evidence
}
