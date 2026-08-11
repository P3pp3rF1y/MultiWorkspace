param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$WorldName = "Dev Client Automation Void Platform",
    [int]$TimeoutSeconds = 300,
    [switch]$Maximize,
    [switch]$ShowLauncherWindow,
    [bool]$LoadWorld = $true,
    [switch]$CloseOnExit,
    [switch]$SkipRecipeViewerReady,
    [switch]$MinimalRuntime,
    [ValidateSet("", "emi", "jei", "rei", "none")]
    [string]$RecipeViewer = ""
)

$ErrorActionPreference = "Stop"

$discoveryPath = Join-Path $WorkspaceRoot "workspace\run\dev-client-automation.json"
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

if (Test-Path $discoveryPath) {
    Remove-Item -LiteralPath $discoveryPath -Force
}

$gradleCommand = "gradlew.bat :workspace:runClient"
if (-not [string]::IsNullOrWhiteSpace($RecipeViewer)) {
    $gradleCommand = "$gradleCommand -Precipe_viewer=$RecipeViewer -Pdev_client_minimal_runtime=true"
} elseif ($MinimalRuntime) {
    $gradleCommand = "$gradleCommand -Pdev_client_minimal_runtime=true -Pdev_client_curios_runtime=true"
}

$cmdMode = if ($CloseOnExit) { '/c' } else { '/k' }
$launcherWindowStyle = if ($ShowLauncherWindow) { 'Normal' } else { 'Minimized' }
Start-Process cmd.exe -ArgumentList $cmdMode, "cd /d $WorkspaceRoot && $gradleCommand" -WorkingDirectory $WorkspaceRoot -WindowStyle $launcherWindowStyle | Out-Null

function Get-BridgeDiscovery {
    if (-not (Test-Path $discoveryPath)) {
        return $null
    }
    try {
        return Get-Content $discoveryPath -Raw | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Invoke-BridgeJson {
    param(
        [Parameter(Mandatory = $true)] [string]$Method,
        [Parameter(Mandatory = $true)] [string]$Path,
        [object]$Body = $null
    )

    $discovery = Get-BridgeDiscovery
    if ($null -eq $discovery) {
        throw "Bridge discovery file is not available yet."
    }
    $uri = "http://$($discovery.host):$($discovery.port)$Path"
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 10
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec $TimeoutSeconds
}

do {
    Start-Sleep -Seconds 2
    try {
        $state = Invoke-BridgeJson -Method Get -Path "/state"
        break
    } catch {
        $state = $null
    }
} while ((Get-Date) -lt $deadline)

if ($null -eq $state) {
    throw "Timed out waiting for dev-client automation bridge."
}

if ($Maximize) {
    Invoke-BridgeJson -Method Post -Path "/window/maximize" | Out-Null
}

if ($LoadWorld -and -not $state.playerLoaded) {
    do {
        Start-Sleep -Seconds 1
        $state = Invoke-BridgeJson -Method Get -Path "/state"
        if ($state.screenSimpleName -eq "TitleScreen") {
            break
        }
    } while ((Get-Date) -lt $deadline)

    if ($state.screenSimpleName -ne "TitleScreen") {
        throw "Timed out waiting for title screen before loading world."
    }

    Invoke-BridgeJson -Method Post -Path "/world/load" -Body @{ worldName = $WorldName; autoConfirmExperimental = $true; timeoutMs = $TimeoutSeconds * 1000 } | Out-Null
}

do {
    Start-Sleep -Seconds 1
    $state = Invoke-BridgeJson -Method Get -Path "/state"
    if ($SkipRecipeViewerReady) {
        if (-not $LoadWorld -or $state.playerLoaded) {
            break
        }
        continue
    }
    $viewerState = Invoke-BridgeJson -Method Get -Path "/recipe-viewer/state"
    if ($viewerState.ok -and (-not $LoadWorld -or ($state.playerLoaded -and $viewerState.indexStackCount -gt 0))) {
        break
    }
} while ((Get-Date) -lt $deadline)

$discovery = Get-BridgeDiscovery
[pscustomobject]@{
    host = $discovery.host
    port = $discovery.port
    processId = $discovery.processId
    baseUrl = "http://$($discovery.host):$($discovery.port)"
    state = Invoke-BridgeJson -Method Get -Path "/state"
    recipeViewer = if ($SkipRecipeViewerReady) { $null } else { $viewerState }
}
