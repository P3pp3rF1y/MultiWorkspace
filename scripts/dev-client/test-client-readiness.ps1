param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot/../..").Path)
$ErrorActionPreference = 'Stop'
$directory = Join-Path $WorkspaceRoot "build/reports/dev-client-regression/client-readiness/$([guid]::NewGuid())"
$run = Join-Path $directory 'workspace/run'
New-Item -ItemType Directory -Force -Path $run | Out-Null
$discoveryPath = Join-Path $run 'dev-client-automation.json'
$cases = @()

$result = & "$PSScriptRoot/start-and-ready.ps1" -WorkspaceRoot $directory -PreflightOnly
if (-not $result.ok -or $result.launched -or (Test-Path $discoveryPath)) { throw 'Missing discovery preflight mutated or launched' }
$cases += 'missing-discovery'
@{ processId = [int]::MaxValue } | ConvertTo-Json | Set-Content -LiteralPath $discoveryPath
$before = [IO.File]::ReadAllBytes($discoveryPath)
$result = & "$PSScriptRoot/start-and-ready.ps1" -WorkspaceRoot $directory -PreflightOnly
if (-not $result.ok -or [Convert]::ToBase64String($before) -ne [Convert]::ToBase64String([IO.File]::ReadAllBytes($discoveryPath))) { throw 'Stale discovery was changed' }
$cases += 'stale-discovery'
@{ processId = $PID; host = '127.0.0.1'; port = 1 } | ConvertTo-Json | Set-Content -LiteralPath $discoveryPath
$before = [IO.File]::ReadAllText($discoveryPath)
$timer = [diagnostics.stopwatch]::StartNew()
$rejected = $false
try { & "$PSScriptRoot/start-and-ready.ps1" -WorkspaceRoot $directory -TimeoutSeconds 300 }
catch { $rejected = $_.Exception.Message -match 'Client launch conflict' }
$timer.Stop()
if (-not $rejected -or $before -ne [IO.File]::ReadAllText($discoveryPath)) { throw 'Live discovery was overwritten or conflict was not rejected' }
$cases += 'live-conflict-preserved'
'invalid-json' | Set-Content -LiteralPath $discoveryPath
$rejected = $false
try { & "$PSScriptRoot/start-and-ready.ps1" -WorkspaceRoot $directory -PreflightOnly }
catch { $rejected = $_.Exception.Message -match 'Unreadable client discovery' }
if (-not $rejected) { throw 'Malformed discovery was silently discarded' }
$cases += 'malformed-discovery'

# Exercise recovery orchestration without changing a Minecraft world. Clearing
# a container emits an item in this transport fixture; both it and the existing
# entity must be relocated, and no delete/stop endpoint is available.
$fixtureState = [pscustomobject]@{
    requests = [collections.generic.list[string]]::new()
    entities = [collections.generic.list[string]]::new()
    relocated = [collections.generic.list[string]]::new()
}
$fixtureState.entities.Add('original-item')
function Invoke-RestMethod {
    param($Method, $Uri, $ContentType, $Body, $TimeoutSec)
    if ($Method -ne 'Post' -or $Uri -ne 'http://fixture/command') { throw 'Unexpected recovery endpoint' }
    $command = ($Body | ConvertFrom-Json).command
    $fixtureState.requests.Add($command)
    if ($command -like 'execute as *') { $fixtureState.relocated.AddRange($fixtureState.entities); $fixtureState.entities.Clear() }
    elseif ($command -like 'fill *minecraft:air') { $fixtureState.entities.Add('container-drop') }
    elseif ($command -notlike 'fill *minecraft:stone') { throw 'Unexpected recovery command' }
    return @{ ok = $true; dispatched = $true }
}
$rejected = $false
try { & "$PSScriptRoot/repair-regression-cell.ps1" -BaseUrl 'http://fixture' }
catch { $rejected = $_.Exception.Message -match 'AuthorizeCellClear' }
if (-not $rejected -or $fixtureState.requests.Count) { throw 'Repair mutated before authorization' }
$result = & "$PSScriptRoot/repair-regression-cell.ps1" -BaseUrl 'http://fixture' -AuthorizeCellClear
if (-not $result.requiresFixtureReset -or $result.PSObject.Properties.Name -contains 'ok' -or $fixtureState.entities.Count -or
        (@($fixtureState.relocated | Sort-Object) -join ',') -ne 'container-drop,original-item') { throw 'Repair lost entities/drops or claimed native acceptance' }
$cases += 'authorized-repair-includes-container-drops'
$fixtureState.requests.Clear()
function Invoke-RestMethod {
    param($Method, $Uri, $ContentType, $Body, $TimeoutSec)
    $fixtureState.requests.Add(($Body | ConvertFrom-Json).command)
    return @{ ok = $false; message = 'Injected transport rejection' }
}
$rejected = $false
try { & "$PSScriptRoot/repair-regression-cell.ps1" -BaseUrl 'http://fixture' -AuthorizeCellClear }
catch { $rejected = $_.Exception.Message -match 'Injected transport rejection' }
if (-not $rejected -or $fixtureState.requests.Count -ne 1) { throw 'Repair continued after failure or hid diagnostics' }
$cases += 'repair-stops-on-transport-failure'
[pscustomobject]@{ ok = $true; cases = $cases; conflictSeconds = $timer.Elapsed.TotalSeconds; artifacts = $directory }
