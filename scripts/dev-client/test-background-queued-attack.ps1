param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$WorldName = "DevClientQueuedAttack-$([guid]::NewGuid().ToString('N'))",
    [string]$ArtifactDirectory = (Join-Path $WorkspaceRoot 'DevClientAutomation\build\reports\queued-attack'),
    [string]$CandidateIdentity,
    [string]$RuntimeBuildIdentity
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\client-lifecycle.ps1"
$ready = $null
$result = $null
$probePath = '/regression/probe-background-attack'
if ([string]::IsNullOrWhiteSpace($CandidateIdentity) -or [string]::IsNullOrWhiteSpace($RuntimeBuildIdentity)) {
    throw 'Queued attack probe requires candidate and runtime build identities.'
}
function Invoke-Bridge {
    param([string]$Method, [string]$Path, [object]$Body = $null)
    $args = @{ Method = $Method; Uri = "$($ready.baseUrl)$Path"; TimeoutSec = 20 }
    if ($null -ne $Body) {
        $args.ContentType = 'application/json'
        $args.Body = ($Body | ConvertTo-Json -Compress)
    }
    Invoke-RestMethod @args
}
function Wait-Run {
    param([string]$Outcome)
    $deadline = (Get-Date).AddSeconds(90)
    do {
        Start-Sleep -Milliseconds 100
        $status = Invoke-Bridge Get '/regression/status'
    } while ($status.outcome -eq 'running' -and (Get-Date) -lt $deadline)
    if ($status.outcome -ne $Outcome) { throw "Expected $Outcome, got $($status.outcome): $($status.message)" }
    return $status
}
function Assert-Stones {
    $cell = Invoke-Bridge Post $probePath @{ mode = 'stones' }
    if ($cell.stones -ne 81 -or -not $cell.creative -or $cell.x -ne 256.5 -or $cell.y -ne 73 -or $cell.z -ne 256.5 -or
        $cell.yaw -ne -90 -or $cell.pitch -ne 20) { throw "Unsafe regression cell/pose: $($cell | ConvertTo-Json -Compress)" }
    return $cell
}
try {
    $ready = & "$PSScriptRoot\start-and-ready.ps1" -WorkspaceRoot $WorkspaceRoot -WorldName $WorldName -CloseOnExit -SkipRecipeViewerReady -TimeoutSeconds 360
    if (-not $ready.capabilities.backgroundInput) { throw 'Background input was not enabled.' }
    $initial = Invoke-Bridge Post '/regression/run' @{ suiteId = 'linked-storage-action-conformance'; testId = 'empty-crafting-times-out-with-slot-diagnostics' }
    if ($initial.outcome -ne 'running') { throw "Initial fixture did not start: $($initial | ConvertTo-Json -Compress)" }
    $initial = Wait-Run 'passed'
    $creative = Invoke-Bridge Post '/command' @{ command = 'gamemode creative @s' }
    $pose = Invoke-Bridge Post '/command' @{ command = 'tp @s 256.5 73 256.5 -90 20' }
    if (-not $creative.ok -or -not $pose.ok) { throw 'Could not set Creative reset pose.' }
    $cell = Assert-Stones
    $screen = Invoke-Bridge Get '/screen'
    if (-not $screen.screenClass -or $screen.screenClass -eq 'none') {
        $open = Invoke-Bridge Post '/key' @{ key = 'e' }
        if (-not $open.handled) { throw 'Virtual inventory key did not open the screen.' }
    }
    $native = Invoke-Bridge Post $probePath @{ mode = 'native' }
    if ($native.attackDown -or $native.queuedClicks -ne 0) { throw "Native onPress bypassed background guard: $($native | ConvertTo-Json -Compress)" }
    $armed = Invoke-Bridge Post $probePath @{ mode = 'armStart' }
    if (-not $armed.armed) { throw "Entry attack was not armed: $($armed | ConvertTo-Json -Compress)" }
    $entry = Wait-Run 'passed'
    if ($entry.diagnostics.'backgroundAttack.entry'.drainedClicks -lt 1 -or
        $entry.diagnostics.'backgroundAttack.termination'.drainedClicks -ne 0) {
        throw "Entry drain was not observed: $($entry.diagnostics | ConvertTo-Json -Compress)"
    }
    $afterEntry = Invoke-Bridge Post $probePath @{ mode = 'inspect' }
    if ($afterEntry.attackDown -or $afterEntry.queuedClicks -ne 0) { throw "Attack survived entry/normal termination: $($afterEntry | ConvertTo-Json -Compress)" }
    $cell = Assert-Stones
    $screen = Invoke-Bridge Get '/screen'
    if (-not $screen.screenClass -or $screen.screenClass -eq 'none') {
        $open = Invoke-Bridge Post '/key' @{ key = 'e' }
        if (-not $open.handled) { throw 'Virtual key did not reopen inventory before cancellation.' }
    }
    $again = Invoke-Bridge Post '/regression/run' @{ suiteId = 'linked-storage-action-conformance'; testId = 'empty-crafting-times-out-with-slot-diagnostics' }
    if ($again.outcome -ne 'running') { throw 'Cancellation fixture did not start.' }
    $armedCancel = Invoke-Bridge Post $probePath @{ mode = 'armCancel' }
    if (-not $armedCancel.armed) { throw "Termination attack was not armed: $($armedCancel | ConvertTo-Json -Compress)" }
    $cancelled = Wait-Run 'cancelled'
    if ($cancelled.diagnostics.'backgroundAttack.termination'.drainedClicks -lt 1) {
        throw "Termination drain was not observed: $($cancelled.diagnostics | ConvertTo-Json -Compress)"
    }
    $afterCancel = Invoke-Bridge Post $probePath @{ mode = 'inspect' }
    if ($afterCancel.attackDown -or $afterCancel.queuedClicks -ne 0) { throw 'Attack survived cancellation.' }
    $cell = Assert-Stones
    $result = [pscustomobject]@{ ok = $true; candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity;
        world = $WorldName; native = $native; entry = $entry.diagnostics.'backgroundAttack.entry';
        termination = $cancelled.diagnostics.'backgroundAttack.termination'; virtualKey = $open.handled; stones = $cell.stones; ownedTeardown = 'pending' }
} finally {
    if ($null -ne $ready) {
        $stopped = Stop-OwnedClient -Ownership $ready.ownership
        if (-not $stopped.ok) { throw "Owned queued-attack probe teardown failed: $($stopped.errors -join '; ')" }
    }
}
if ($null -ne $result) {
    $result.ownedTeardown = $true
    if (-not (Test-Path -LiteralPath $ArtifactDirectory)) { New-Item -ItemType Directory -Path $ArtifactDirectory -Force | Out-Null }
    $result | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $ArtifactDirectory 'queued-attack.json') -Encoding utf8
    $result
}
