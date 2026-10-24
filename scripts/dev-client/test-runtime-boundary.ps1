param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$WorldName = 'DevClientRuntimeBoundary',
    [string]$ArtifactDirectory = (Join-Path $WorkspaceRoot 'DevClientAutomation\build\reports\runtime-boundary'),
    [string]$CandidateIdentity,
    [string]$RuntimeBuildIdentity
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\client-lifecycle.ps1"
$ready = $null
$result = $null
if ([string]::IsNullOrWhiteSpace($CandidateIdentity) -or [string]::IsNullOrWhiteSpace($RuntimeBuildIdentity)) {
    throw 'Runtime boundary requires candidate and runtime build identities.'
}

function Invoke-OwnedBridge {
    param([string]$Method, [string]$Path, [object]$Body = $null)
    $uri = "$($ready.baseUrl)$Path"
    if ($null -eq $Body) { return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 15 }
    return Invoke-RestMethod -Method $Method -Uri $uri -Body ($Body | ConvertTo-Json -Compress -Depth 8) -ContentType 'application/json' -TimeoutSec 15
}

try {
    $ready = & "$PSScriptRoot\start-and-ready.ps1" -WorkspaceRoot $WorkspaceRoot -WorldName $WorldName -CloseOnExit -SkipRecipeViewerReady -TimeoutSeconds 300
    if (-not $ready.capabilities.backgroundInput -or -not $ready.state.playerLoaded) { throw 'Dedicated client is not ready for virtual input.' }
    $catalog = Invoke-OwnedBridge Get '/regression/catalog'
    if (@($catalog.suites).Count -lt 23 -or @($catalog.actions.PSObject.Properties).Count -lt 119) { throw 'Packaged runtime catalog is incomplete.' }

    $move = Invoke-OwnedBridge Post '/mouse/move' @{ position = 'top-left' }
    $clear = Invoke-OwnedBridge Post '/mouse/clear-hover'
    $open = Invoke-OwnedBridge Post '/key' @{ key = 'e' }
    $screen = Invoke-OwnedBridge Get '/screen'
    $close = Invoke-OwnedBridge Post '/key' @{ key = 'escape' }
    if (-not $move.ok -or $move.x -ne 4 -or -not $clear.hoverCleared -or -not $open.handled -or
        $screen.screenClass -notin @('net.minecraft.client.gui.screens.inventory.InventoryScreen',
            'net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen') -or -not $close.handled) {
        throw "Virtual pointer/keybind input mismatch: move=$($move | ConvertTo-Json -Compress) clear=$($clear | ConvertTo-Json -Compress) open=$($open | ConvertTo-Json -Compress) screen=$($screen.screenClass) close=$($close | ConvertTo-Json -Compress)"
    }

    $failure = Invoke-OwnedBridge Post '/regression/run' @{
        suiteId = 'item-entity-action-conformance'; testId = 'exact-item-identity-survives-same-item-neighbor'
        actionFailureAfter = 'fixture.player.setGameMode'; cleanupFailure = $true
    }
    if ($failure.outcome -ne 'running') { throw 'Combined failure did not enter regression execution.' }
    $deadline = (Get-Date).AddSeconds(60)
    do {
        Start-Sleep -Milliseconds 250
        $failure = Invoke-OwnedBridge Get '/regression/status'
    } while ($failure.outcome -eq 'running' -and (Get-Date) -lt $deadline)
    if ($failure.outcome -ne 'error' -or $failure.primaryOutcome -ne 'failed' -or $failure.category -ne 'cleanup' -or
        $failure.cleanupFailure.category -ne 'cleanup' -or $failure.primaryFailure.message -notmatch 'Injected action failure') {
        throw "Primary/cleanup failure mismatch: outcome=$($failure.outcome) primary=$($failure.primaryOutcome)/$($failure.primaryFailure.message) cleanup=$($failure.cleanupFailure.category)"
    }

    $cancel = Invoke-OwnedBridge Post '/regression/run' @{
        suiteId = 'linked-storage-action-conformance'; testId = 'missing-controller-and-capabilities-fail-deterministically'
    }
    if ($cancel.outcome -ne 'running') { throw 'Cancellation probe did not enter regression execution.' }
    $deadline = (Get-Date).AddSeconds(20)
    do {
        Start-Sleep -Milliseconds 100
        $cancel = Invoke-OwnedBridge Get '/regression/status'
    } while ($cancel.outcome -eq 'running' -and $cancel.phase -ne 'execution' -and (Get-Date) -lt $deadline)
    if ($cancel.outcome -ne 'running' -or $cancel.phase -ne 'execution') { throw "Cancellation probe missed the active action window: $($cancel.outcome)/$($cancel.phase)" }
    $requested = Invoke-OwnedBridge Post '/regression/cancel'
    if ($requested.outcome -ne 'running') { throw 'Cancellation request did not reach the active client.' }
    $deadline = (Get-Date).AddSeconds(60)
    do {
        Start-Sleep -Milliseconds 250
        $cancel = Invoke-OwnedBridge Get '/regression/status'
    } while ($cancel.outcome -eq 'running' -and (Get-Date) -lt $deadline)
    if ($cancel.outcome -ne 'cancelled' -or $cancel.primaryOutcome -ne 'cancelled' -or @($cancel.cleanup).Count -lt 1) {
        throw "Cancellation mismatch: outcome=$($cancel.outcome) primary=$($cancel.primaryOutcome) cleanup=$(@($cancel.cleanup).Count)"
    }

    # A borrowed controller may inspect and run tests, but has no authority to stop this client.
    $borrowed = & "$PSScriptRoot\regression.ps1" catalog -BaseUrl $ready.baseUrl -ArtifactDirectory $ArtifactDirectory -CandidateIdentity $CandidateIdentity -RuntimeBuildIdentity $RuntimeBuildIdentity
    if (@($borrowed.suites).Count -lt 23) { throw 'Borrowed controller catalog mismatch.' }
    if (-not (Invoke-OwnedBridge Get '/state').playerLoaded) { throw 'Borrowed controller stopped the owned client.' }
    $result = [pscustomobject]@{ ok = $true; candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity;
        catalogSuites = @($catalog.suites).Count; virtualInput = $true; primaryAndCleanupFailure = $true; cancellation = $true; borrowedClientPreserved = $true }
} finally {
    if ($null -ne $ready) {
        $stopped = Stop-OwnedClient -Ownership $ready.ownership
        if (-not $stopped.ok) { throw "Owned runtime probe teardown failed: $($stopped.errors -join '; ')" }
    }
}
if ($result) {
    if (-not (Test-Path -LiteralPath $ArtifactDirectory)) { New-Item -ItemType Directory -Path $ArtifactDirectory -Force | Out-Null }
    $result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $ArtifactDirectory 'runtime-boundary.json') -Encoding utf8
    $result
}
