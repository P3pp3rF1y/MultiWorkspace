param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$BaseUrl = "",
    [string]$Item = "chest",
    [string]$ScreenshotDirectory = (Join-Path (Resolve-Path "$PSScriptRoot\..\..\workspace\run").Path "decoration-table-manual-rotation"),
    [string]$ReferenceDirectory = "",
    [int]$TimeoutSeconds = 360,
    [switch]$NoStartClient,
    [switch]$MinimalRuntime
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Assert-True { param([bool]$Condition, [string]$Message) if (-not $Condition) { throw $Message } }
function Invoke-BridgeJson { param([string]$Method, [string]$Path, [object]$Body = $null) $uri = "$BaseUrl$Path"; if ($null -eq $Body) { return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 10 }; return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec $TimeoutSeconds }
function Stop-AutomationClient { param([int]$ProcessId) try { Invoke-BridgeJson Post "/client/stop" | Out-Null } catch { Write-Warning "Failed to stop dev client: $($_.Exception.Message)" }; if ($ProcessId -gt 0 -and $null -ne (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) { & taskkill.exe /PID $ProcessId /T /F | Out-Null } }
function Save-PreviewCrop { param([string]$ScreenshotPath, [string]$CropPath, [object]$State, [object]$Preview) $image = [System.Drawing.Bitmap]::FromFile($ScreenshotPath); try { $bounds = [System.Drawing.Rectangle]::new([int][Math]::Round($Preview.x * $image.Width / [double]$State.guiWidth), [int][Math]::Round($Preview.y * $image.Height / [double]$State.guiHeight), [int][Math]::Round($Preview.width * $image.Width / [double]$State.guiWidth), [int][Math]::Round($Preview.height * $image.Height / [double]$State.guiHeight)); $crop = $image.Clone($bounds, $image.PixelFormat); try { $crop.Save($CropPath, [System.Drawing.Imaging.ImageFormat]::Png) } finally { $crop.Dispose() } } finally { $image.Dispose() } }
function Get-PixelDifferenceCount { param([string]$FirstPath, [string]$SecondPath) $first = [System.Drawing.Bitmap]::FromFile($FirstPath); $second = [System.Drawing.Bitmap]::FromFile($SecondPath); try { Assert-True ($first.Width -eq $second.Width -and $first.Height -eq $second.Height) "Rotation crops have different dimensions."; $count = 0; for ($y = 0; $y -lt $first.Height; $y++) { for ($x = 0; $x -lt $first.Width; $x++) { $a = $first.GetPixel($x, $y); $b = $second.GetPixel($x, $y); if ([Math]::Abs($a.R - $b.R) + [Math]::Abs($a.G - $b.G) + [Math]::Abs($a.B - $b.B) -ge 45) { $count++ } } }; return $count } finally { $second.Dispose(); $first.Dispose() } }
function Get-HandleSidePixelCount { param([string]$FirstPath, [string]$SecondPath) $first = [System.Drawing.Bitmap]::FromFile($FirstPath); $second = [System.Drawing.Bitmap]::FromFile($SecondPath); try { Assert-True ($first.Width -eq $second.Width -and $first.Height -eq $second.Height) "Rotation crops have different dimensions."; $count = 0; $startX = [int][Math]::Floor($first.Width * 0.625); for ($y = 0; $y -lt $first.Height; $y++) { for ($x = $startX; $x -lt $first.Width; $x++) { $a = $first.GetPixel($x, $y); $b = $second.GetPixel($x, $y); if ([Math]::Abs($a.R - $b.R) + [Math]::Abs($a.G - $b.G) + [Math]::Abs($a.B - $b.B) -ge 45) { $count++ } } }; return $count } finally { $second.Dispose(); $first.Dispose() } }

$startedClient = $false; $clientProcessId = 0
try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) { Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."; $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }; if ($MinimalRuntime) { $readyArgs.MinimalRuntime = $true }; $ready = & "$PSScriptRoot\start-and-ready.ps1" @readyArgs; $BaseUrl = $ready.baseUrl; $clientProcessId = $ready.processId; $startedClient = $true }
    $state = Invoke-BridgeJson Get "/state"
    Assert-True $state.playerLoaded "Dev client world is not loaded."
    if ($state.screenSimpleName -eq "PauseScreen") { Invoke-BridgeJson Post "/game/unpause" | Out-Null; Start-Sleep -Milliseconds 250 }
    New-Item -ItemType Directory -Path $ScreenshotDirectory -Force | Out-Null
    $cropDirectory = Join-Path $ScreenshotDirectory "preview-crops"
    New-Item -ItemType Directory -Path $cropDirectory -Force | Out-Null
    $open = Invoke-BridgeJson Post "/storage/decoration-table-render-preview/open" @{ item = $Item }
    Assert-True $open.ok "Failed to open Decoration Table $Item preview."
    Invoke-BridgeJson Post "/mouse/move" @{ x = 0; y = 0 } | Out-Null
    Start-Sleep -Milliseconds 2250
    $steps = @(
        [pscustomobject]@{ name = "default"; dragX = 0; dragY = 0 },
        [pscustomobject]@{ name = "yaw-2"; dragX = 1; dragY = 0 },
        [pscustomobject]@{ name = "yaw-90"; dragX = 44; dragY = 0 },
        [pscustomobject]@{ name = "pitch-90"; dragX = 0; dragY = 45 }
    )
    $results = @()
    foreach ($step in $steps) {
        if ($step.dragX -ne 0 -or $step.dragY -ne 0) {
            $drag = Invoke-BridgeJson Post "/storage/decoration-table-render-preview/drag" @{ dragX = $step.dragX; dragY = $step.dragY; x = [int]($open.preview.x + $open.preview.width / 2); y = [int]($open.preview.y + $open.preview.height / 2) }
            Assert-True (($drag.dragged -eq $true) -or ($drag.ok -eq $true)) "Decoration Table preview did not accept $($step.name) drag."
            Start-Sleep -Milliseconds 1250
        }
        $screenshot = Join-Path $ScreenshotDirectory "$($step.name).png"
        Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $screenshot | Out-Null
        $crop = Join-Path $cropDirectory "$($step.name).png"
        Save-PreviewCrop $screenshot $crop (Invoke-BridgeJson Get "/state") $open.preview
        $results += [pscustomobject]@{ step = $step.name; crop = $crop }
    }
    $default = Join-Path $cropDirectory "default.png"
    foreach ($result in $results | Where-Object step -ne "default") {
        $result | Add-Member changedPixels (Get-PixelDifferenceCount $default $result.crop)
        Assert-True ($result.changedPixels -ge 30) "Preview did not visibly change after $($result.step)."
    }
    $firstYawDrag = $results | Where-Object step -eq "yaw-2"
    $firstYawDrag | Add-Member handleSidePixels (Get-HandleSidePixelCount $default $firstYawDrag.crop)
    Assert-True ($firstYawDrag.handleSidePixels -le 300) "The first horizontal drag rotated the chest away from its handle side."
    if (-not [string]::IsNullOrWhiteSpace($ReferenceDirectory)) {
        foreach ($result in $results) {
            $reference = Join-Path $ReferenceDirectory "$($result.step).png"
            Assert-True (Test-Path $reference) "Reference crop is missing: $reference"
            $result | Add-Member referenceDifferencePixels (Get-PixelDifferenceCount $reference $result.crop)
        }
    }
    [pscustomobject]@{ ok = $true; screenshotDirectory = $ScreenshotDirectory; results = $results }
} finally { if ($startedClient) { Stop-AutomationClient $clientProcessId } }
