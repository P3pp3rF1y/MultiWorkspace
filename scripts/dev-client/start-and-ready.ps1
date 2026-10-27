param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
	[ValidateSet("forge")]
	[string]$Loader = "forge",
    [string]$WorldName = "Dev Client Automation Void Platform",
    [int]$TimeoutSeconds = 300,
    [switch]$ShowLauncherWindow,
    [bool]$LoadWorld = $true,
    # Controls the launcher console, not the client lifetime.
    [switch]$CloseOnExit,
    [switch]$KeepClientOpen,
    [switch]$SkipRecipeViewerReady,
    [switch]$MinimalRuntime,
    [string]$GameDirectory = "",
    [string]$OptionsSeed = "",
    [ValidateSet("", "emi", "jei", "rei", "none")]
    [string]$RecipeViewer = ""
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\client-lifecycle.ps1"

$discoveryPaths = @(
    (Join-Path $WorkspaceRoot "workspace\run\regressionClient\dev-client-automation.json"),
    (Join-Path $WorkspaceRoot "workspace\run\dev-client-automation.json"),
    (Join-Path $WorkspaceRoot "workspace\run\client\dev-client-automation.json")
)
if (-not [string]::IsNullOrWhiteSpace($GameDirectory)) {
    $gamePath = [System.IO.Path]::GetFullPath($GameDirectory)
    if (-not $gamePath.StartsWith(([System.IO.Path]::GetFullPath((Join-Path $WorkspaceRoot 'build')) + [System.IO.Path]::DirectorySeparatorChar),
            [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Isolated game directory must be beneath workspace/build.'
    }
    if (-not (Test-Path -LiteralPath $gamePath)) {
        $null = New-Item -ItemType Directory -Path $gamePath -Force
    }
    if (-not [string]::IsNullOrWhiteSpace($OptionsSeed)) {
        $seedPath = [System.IO.Path]::GetFullPath($OptionsSeed)
        if (-not (Test-Path -LiteralPath $seedPath -PathType Leaf)) { throw "Options seed does not exist: $seedPath" }
        $destination = Join-Path $gamePath 'options.txt'
        if (Test-Path -LiteralPath $destination) { throw "Isolated options already exist; use a fresh game directory: $destination" }
        Copy-Item -LiteralPath $seedPath -Destination $destination
    }
    $discoveryPaths = @((Join-Path $gamePath 'dev-client-automation.json'))
} elseif (-not [string]::IsNullOrWhiteSpace($OptionsSeed)) {
    throw 'OptionsSeed requires an isolated GameDirectory.'
}

foreach ($discoveryPath in $discoveryPaths) {
    if (Test-Path $discoveryPath) {
        Remove-Item -LiteralPath $discoveryPath -Force
    }
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

$gradleCommand = "gradlew.bat :DevClientAutomation:jar :workspace:runRegressionClient --no-daemon"
if (-not [string]::IsNullOrWhiteSpace($GameDirectory)) {
    $gradleCommand = "$gradleCommand -Pdev_client_regression_game_directory=`"$gamePath`""
}
if (-not [string]::IsNullOrWhiteSpace($RecipeViewer)) {
    $gradleCommand = "$gradleCommand -Precipe_viewer=$RecipeViewer -Pdev_client_minimal_runtime=true"
} elseif ($MinimalRuntime) {
    $gradleCommand = "$gradleCommand -Pdev_client_minimal_runtime=true -Pdev_client_curios_runtime=true"
}

$cmdMode = if ($CloseOnExit) { '/c' } else { '/k' }
$launcherWindowStyle = if ($ShowLauncherWindow) { 'Normal' } else { 'Minimized' }
$launcher = Start-Process cmd.exe -ArgumentList $cmdMode, "cd /d `"$WorkspaceRoot`" && $gradleCommand" -WorkingDirectory $WorkspaceRoot -WindowStyle $launcherWindowStyle -PassThru
$ownership = New-ClientOwnership $launcher

function Get-BridgeDiscovery {
    foreach ($discoveryPath in $discoveryPaths) {
        if (-not (Test-Path $discoveryPath)) {
            continue
        }
        try {
            return Get-Content $discoveryPath -Raw | ConvertFrom-Json
        } catch {
            return $null
        }
    }
    return $null
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
if (-not $capabilities.ok -or $capabilities.protocolVersion -ne 1 -or $capabilities.loader -ne $Loader -or $capabilities.minecraftVersion -ne "1.20.1") {
	throw "Automation bridge capability mismatch: $($capabilities | ConvertTo-Json -Compress)"
}

if (-not $capabilities.backgroundInput) {
    throw "Regression client was not launched with background-safe input enabled."
}

if ($LoadWorld -and -not $state.playerLoaded) {
    $onboardingContinued = $false
    do {
        Start-Sleep -Seconds 1
        $state = Invoke-BridgeJson -Method Get -Path "/state"
        if ($state.screenSimpleName -eq "AccessibilityOnboardingScreen" -and -not $onboardingContinued) {
            $screen = Invoke-BridgeJson -Method Get -Path "/screen"
            if ($screen.screenClass -ne "net.minecraft.client.gui.screens.AccessibilityOnboardingScreen") {
                throw "First-run onboarding screen changed during inspection."
            }
            # The sole vanilla Button is Continue; the other controls are a cycle button and settings links.
            $continueWidgets = @($screen.widgets | Where-Object { $_.type -eq "net.minecraft.client.gui.components.Button" -and $_.visible -and $_.active })
            if ($continueWidgets.Count -ne 1) { throw "Expected exactly one active Continue button on first-run onboarding screen." }
            $click = Invoke-BridgeJson -Method Post -Path "/click-widget" -Body @{ index = $continueWidgets[0].index }
            if (-not $click.ok -or $click.index -ne $continueWidgets[0].index) { throw "Could not continue first-run onboarding through virtual widget input." }
            $onboardingContinued = $true
            continue
        }
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
    state = Invoke-BridgeJson -Method Get -Path "/state"
	capabilities = $capabilities
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
