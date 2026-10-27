param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [ValidateSet("neoforge", "fabric")]
    [string]$Loader = "neoforge",
    [string]$WorldName = "Dev Client Automation Void Platform",
    [int]$TimeoutSeconds = 300,
    [switch]$ShowLauncherWindow,
    [bool]$LoadWorld = $true,
    # Controls the launcher console, not the client lifetime.
    [switch]$CloseOnExit,
    [switch]$KeepClientOpen,
    [switch]$SkipRecipeViewerReady,
    [switch]$MinimalRuntime,
    [ValidateSet("", "emi", "jei", "rei", "none")]
    [string]$RecipeViewer = ""
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\client-lifecycle.ps1"

$validationDirectory = $env:DEV_CLIENT_VALIDATION_RUN_DIRECTORY
$validationInit = $env:DEV_CLIENT_VALIDATION_INIT_SCRIPT
if ([bool]$validationDirectory -ne [bool]$validationInit) { throw 'Validation runtime directory and init script must be supplied together.' }
if ($validationDirectory -and (-not (Test-Path -LiteralPath $validationDirectory -PathType Container) -or
        -not (Test-Path -LiteralPath $validationInit -PathType Leaf))) { throw 'Validation runtime directory or init script is unavailable.' }
$discoveryPath = if ($validationDirectory) { Join-Path $validationDirectory 'dev-client-automation.json' } else { Join-Path $WorkspaceRoot 'workspace\run\dev-client-automation.json' }
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

if (Test-Path $discoveryPath) {
    Remove-Item -LiteralPath $discoveryPath -Force
}

$gradleCommand = switch ($Loader) {
    "neoforge" { "gradlew.bat :workspace:runRegressionClient --no-daemon" }
    "fabric" { throw "Fabric dev-client launching is not configured in this workspace yet." }
}
if (-not [string]::IsNullOrWhiteSpace($RecipeViewer)) {
    $gradleCommand = "$gradleCommand -Precipe_viewer=$RecipeViewer -Pdev_client_minimal_runtime=true"
} elseif ($MinimalRuntime) {
    $gradleCommand = "$gradleCommand -Pdev_client_minimal_runtime=true -Pdev_client_curios_runtime=true"
}
if ($validationDirectory) { $gradleCommand = "$gradleCommand -I `"$validationInit`"" }

$cmdMode = if ($CloseOnExit) { '/c' } else { '/k' }
$launcherWindowStyle = if ($ShowLauncherWindow) { 'Normal' } else { 'Minimized' }
$launcher = Start-Process cmd.exe -ArgumentList $cmdMode, "cd /d `"$WorkspaceRoot`" && $gradleCommand" -WorkingDirectory $WorkspaceRoot -WindowStyle $launcherWindowStyle -PassThru
$ownership = New-ClientOwnership $launcher

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

    if (-not $ownership.baseUrl) { throw 'Owned bridge discovery is not available yet.' }
    $uri = "$($ownership.baseUrl)$Path"
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 10
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec $TimeoutSeconds
}

try {
do {
    Start-Sleep -Seconds 2
    try {
        $discovery = Get-BridgeDiscovery
        if ($null -eq $discovery) { continue }
        Set-OwnedClientDiscovery -Ownership $ownership -Discovery $discovery
        $state = Invoke-BridgeJson -Method Get -Path "/state"
        break
    } catch {
        $state = $null
    }
} while ((Get-Date) -lt $deadline)

if ($null -eq $state) {
    throw "Timed out waiting for dev-client automation bridge."
}

$capabilities = Invoke-BridgeJson -Method Get -Path "/capabilities"
if (-not $capabilities.ok -or $capabilities.protocolVersion -ne 1) {
    throw "Dev-client automation bridge does not support protocol version 1."
}
if ($capabilities.loader -ne $Loader) {
    throw "Expected loader '$Loader' but automation bridge reports '$($capabilities.loader)'."
}
if (-not $capabilities.backgroundInput) {
    throw "Regression client was not launched with background-safe input enabled."
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

$ready = $false
do {
    Start-Sleep -Seconds 1
    $state = Invoke-BridgeJson -Method Get -Path "/state"
    if ($SkipRecipeViewerReady) {
        if (-not $LoadWorld -or ($state.playerLoaded -and $state.screenSimpleName -ne "ReceivingLevelScreen")) {
            $ready = $true
            break
        }
        continue
    }
    $viewerState = Invoke-BridgeJson -Method Get -Path "/recipe-viewer/state"
    if ($viewerState.ok -and (-not $LoadWorld -or ($state.playerLoaded -and $viewerState.indexStackCount -gt 0))) {
        $ready = $true
        break
    }
} while ((Get-Date) -lt $deadline)
if (-not $ready) {
    throw "Timed out waiting for dev-client world/player or recipe-viewer readiness."
}

$bridgeUri = [uri]$ownership.baseUrl
[pscustomobject]@{
    host = $bridgeUri.Host
    port = $bridgeUri.Port
    processId = $ownership.processId
    ownership = $ownership
    baseUrl = $ownership.baseUrl
    capabilities = $capabilities
    state = Invoke-BridgeJson -Method Get -Path "/state"
    recipeViewer = if ($SkipRecipeViewerReady) { $null } else { $viewerState }
}
} catch {
    $cleanup = Stop-OwnedClient -Ownership $ownership -KeepClientOpen:$KeepClientOpen -WaitForRegression:$false
    $_.Exception.Data['ClientOwnership'] = $ownership
    $_.Exception.Data['ClientLifecycle'] = $cleanup
    if (-not $cleanup.ok) { Write-Warning "Startup cleanup failed: $($cleanup.errors -join '; ')" }
    if ($KeepClientOpen) { Write-Warning "Keeping failed startup open for inspection (launcher PID $($ownership.launcherProcessId))." }
    throw
}
