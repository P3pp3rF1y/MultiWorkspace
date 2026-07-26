param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$BaseUrl = "",
    [string]$ScreenshotDirectory = (Join-Path (Resolve-Path "$PSScriptRoot\..\..\workspace\run").Path "decoration-table-render-screenshots"),
    [int]$TimeoutSeconds = 360,
    [switch]$NoStartClient,
    [switch]$MaximizeClient,
    [switch]$MinimalRuntime
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function Assert-True {
    param([bool]$Condition, [string]$Message)

    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-BridgeJson {
    param(
        [Parameter(Mandatory = $true)] [string]$Method,
        [Parameter(Mandatory = $true)] [string]$Path,
        [object]$Body = $null
    )

    $uri = "$BaseUrl$Path"
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 10
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec $TimeoutSeconds
}

function Stop-ProcessTree {
    param([int]$ProcessId)

    if ($ProcessId -le 0 -or $null -eq (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        return
    }
    try {
        & taskkill.exe /PID $ProcessId /T /F | Out-Null
    } catch {
        Write-Warning "Failed to kill dev client process tree ${ProcessId}: $($_.Exception.Message)"
    }
}

function Stop-AutomationClient {
    param([int]$ProcessId)

    try {
        Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    } catch {
        Write-Warning "Failed to stop dev client through automation bridge: $($_.Exception.Message)"
    }
    Stop-ProcessTree -ProcessId $ProcessId
}

function Get-VisibleBounds {
    param([Parameter(Mandatory = $true)] [string]$ScreenshotPath)

    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $left = $bitmap.Width
        $right = 0
        $top = $bitmap.Height
        $bottom = 0
        $count = 0
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $color = $bitmap.GetPixel($x, $y)
                if ($color.R -lt 35 -and $color.G -lt 35 -and $color.B -lt 35) {
                    continue
                }
                $left = [Math]::Min($left, $x)
                $right = [Math]::Max($right, $x)
                $top = [Math]::Min($top, $y)
                $bottom = [Math]::Max($bottom, $y)
                $count++
            }
        }
        if ($count -eq 0) {
            return [pscustomobject]@{ count = 0; left = 0; right = 0; top = 0; bottom = 0; width = 0; height = 0; centerX = 0; centerY = 0; imageWidth = $bitmap.Width; imageHeight = $bitmap.Height }
        }
        $width = $right - $left + 1
        $height = $bottom - $top + 1
        return [pscustomobject]@{
            count = $count
            left = $left
            right = $right
            top = $top
            bottom = $bottom
            width = $width
            height = $height
            centerX = $left + $width / 2D
            centerY = $top + $height / 2D
            imageWidth = $bitmap.Width
            imageHeight = $bitmap.Height
        }
    } finally {
        $bitmap.Dispose()
    }
}

function Get-MagentaPixelCount {
    param([Parameter(Mandatory = $true)] [string]$ScreenshotPath)

    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $count = 0
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $color = $bitmap.GetPixel($x, $y)
                if ($color.R -ge 150 -and $color.B -ge 150 -and $color.G -le 100) {
                    $count++
                }
            }
        }
        return $count
    } finally {
        $bitmap.Dispose()
    }
}

function Get-PixelDifferenceCount {
    param(
        [Parameter(Mandatory = $true)] [string]$FirstPath,
        [Parameter(Mandatory = $true)] [string]$SecondPath
    )

    $first = [System.Drawing.Bitmap]::FromFile($FirstPath)
    $second = [System.Drawing.Bitmap]::FromFile($SecondPath)
    try {
        Assert-True ($first.Width -eq $second.Width -and $first.Height -eq $second.Height) "Rotation crops have different dimensions. First: $FirstPath Second: $SecondPath"
        $count = 0
        for ($y = 0; $y -lt $first.Height; $y++) {
            for ($x = 0; $x -lt $first.Width; $x++) {
                $firstColor = $first.GetPixel($x, $y)
                $secondColor = $second.GetPixel($x, $y)
                if ([Math]::Abs($firstColor.R - $secondColor.R) + [Math]::Abs($firstColor.G - $secondColor.G) + [Math]::Abs($firstColor.B - $secondColor.B) -ge 45) {
                    $count++
                }
            }
        }
        return $count
    } finally {
        $second.Dispose()
        $first.Dispose()
    }
}

function Save-Crop {
    param(
        [Parameter(Mandatory = $true)] [string]$ScreenshotPath,
        [Parameter(Mandatory = $true)] [string]$CropPath,
        [Parameter(Mandatory = $true)] [object]$State,
        [Parameter(Mandatory = $true)] [object]$Bounds
    )

    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $rectangle = [System.Drawing.Rectangle]::new(
            [int][Math]::Round($Bounds.x * $bitmap.Width / [double]$State.guiWidth),
            [int][Math]::Round($Bounds.y * $bitmap.Height / [double]$State.guiHeight),
            [int][Math]::Round($Bounds.width * $bitmap.Width / [double]$State.guiWidth),
            [int][Math]::Round($Bounds.height * $bitmap.Height / [double]$State.guiHeight)
        )
        $crop = $bitmap.Clone($rectangle, $bitmap.PixelFormat)
        try {
            $crop.Save($CropPath, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $crop.Dispose()
        }
    } finally {
        $bitmap.Dispose()
    }
}

$startedClient = $false
$clientProcessId = 0

try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."
        $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
        if ($MaximizeClient) {
            $readyArgs.Maximize = $true
        }
        if ($MinimalRuntime) {
            $readyArgs.MinimalRuntime = $true
        }
        $ready = & "$PSScriptRoot\start-and-ready.ps1" @readyArgs
        $BaseUrl = $ready.baseUrl
        $clientProcessId = $ready.processId
        $startedClient = $true
    }

    Assert-True (Invoke-BridgeJson -Method Get -Path "/state").playerLoaded "Dev client world is not loaded."
    $previewCropDirectory = Join-Path $ScreenshotDirectory "preview-crops"
    $resultSlotCropDirectory = Join-Path $ScreenshotDirectory "result-slot-crops"
    New-Item -ItemType Directory -Path $previewCropDirectory, $resultSlotCropDirectory -Force | Out-Null
    $rotationCropDirectory = Join-Path $ScreenshotDirectory "rotation-crops"
    New-Item -ItemType Directory -Path $rotationCropDirectory -Force | Out-Null

    $results = @()
    $items = @("storage_io", "controller", "storage_link", "barrel", "limited_barrel_3", "chest", "shulker_box", "backpack", "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots")
    foreach ($item in $items) {
        $open = Invoke-BridgeJson -Method Post -Path "/storage/decoration-table-render-preview/open" -Body @{ item = $item }
        Assert-True $open.ok "Failed to set up decoration table preview for ${item}: $($open.error)"
        Assert-True ($null -ne $open.preview -and $null -ne $open.resultSlot) "Decoration table bounds were not returned for $item."
        Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ position = "top-left" } | Out-Null
        Start-Sleep -Milliseconds 1250

        $screenshotPath = Join-Path $ScreenshotDirectory "$item.png"
        Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $screenshotPath | Out-Null
        $state = Invoke-BridgeJson -Method Get -Path "/state"
        $previewCropPath = Join-Path $previewCropDirectory "$item.png"
        Save-Crop -ScreenshotPath $screenshotPath -CropPath $previewCropPath -State $state -Bounds $open.preview
        $bounds = Get-VisibleBounds -ScreenshotPath $previewCropPath
        $minimumWidthFraction = if ($item -like "leather_*") { 0.4 } else { 0.55 }
        Assert-True ($bounds.count -ge 30) "No substantial decoration result was rendered for $item. Preview crop: $previewCropPath"
        Assert-True ($bounds.width -ge $bounds.imageWidth * 0.2 -and $bounds.height -ge $bounds.imageHeight * 0.15) "Decoration result for $item is too small. Bounds=$($bounds | ConvertTo-Json -Compress)"
        Assert-True ($bounds.width -ge $bounds.imageWidth * $minimumWidthFraction) "Decoration result for $item does not fill enough of the preview. Bounds=$($bounds | ConvertTo-Json -Compress)"
        Assert-True ($bounds.left -ge $bounds.imageWidth * 0.05 -and $bounds.right -le $bounds.imageWidth * 0.95 -and $bounds.top -ge $bounds.imageHeight * 0.05 -and $bounds.bottom -le $bounds.imageHeight * 0.95) "Decoration result for $item is clipped by its preview. Bounds=$($bounds | ConvertTo-Json -Compress)"
        Assert-True ($bounds.centerX -ge $bounds.imageWidth * 0.35 -and $bounds.centerX -le $bounds.imageWidth * 0.65 -and $bounds.centerY -ge $bounds.imageHeight * 0.2 -and $bounds.centerY -le $bounds.imageHeight * 0.75) "Decoration result for $item is not centered in its preview. Bounds=$($bounds | ConvertTo-Json -Compress)"

        $resultSlotCropPath = Join-Path $resultSlotCropDirectory "$item.png"
        Save-Crop -ScreenshotPath $screenshotPath -CropPath $resultSlotCropPath -State $state -Bounds $open.resultSlot
        if ($item -eq "backpack") {
            Assert-True ((Get-MagentaPixelCount -ScreenshotPath $resultSlotCropPath) -ge 5) "Backpack cloth tint is missing from the decoration table result slot. Crop: $resultSlotCropPath"
        }

        $rotationCrops = @()
        if ($item -eq "barrel") {
            Assert-True ($null -ne $open.rotationTargets) "Decoration table rotation targets were not returned for barrel."
            $expectedRotations = @{ top = @(90, 180); side = @(0, 180); bottom = @(-90, 180) }
            $rotationBaselineCropPath = Join-Path $rotationCropDirectory "barrel-default.png"
            Save-Crop -ScreenshotPath $screenshotPath -CropPath $rotationBaselineCropPath -State $state -Bounds $open.preview
            foreach ($targetName in @("top", "side", "bottom")) {
                $target = $open.rotationTargets.$targetName
                $expectedRotation = $expectedRotations[$targetName]
                Assert-True ($null -ne $target) "Missing barrel $targetName rotation target."
                Assert-True ($target.xAxisRotation -eq $expectedRotation[0] -and $target.yAxisRotation -eq $expectedRotation[1]) "Unexpected barrel $targetName rotation target. Target=$($target | ConvertTo-Json -Compress)"
                Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ x = $target.x; y = $target.y } | Out-Null
                Start-Sleep -Milliseconds 2000

                $rotationScreenshotPath = Join-Path $ScreenshotDirectory "barrel-$targetName.png"
                Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $rotationScreenshotPath | Out-Null
                $rotationCropPath = Join-Path $rotationCropDirectory "barrel-$targetName.png"
                Save-Crop -ScreenshotPath $rotationScreenshotPath -CropPath $rotationCropPath -State (Invoke-BridgeJson -Method Get -Path "/state") -Bounds $open.preview
                $rotationBounds = Get-VisibleBounds -ScreenshotPath $rotationCropPath
                $pixelDifferenceCount = Get-PixelDifferenceCount -FirstPath $rotationBaselineCropPath -SecondPath $rotationCropPath
                Assert-True ($rotationBounds.count -ge 30 -and $rotationBounds.width -ge $rotationBounds.imageWidth * 0.55) "Barrel $targetName rotation did not render a substantial preview. Bounds=$($rotationBounds | ConvertTo-Json -Compress)"
                $rotationCrops += [pscustomobject]@{ target = $targetName; crop = $rotationCropPath; pixelDifferenceCount = $pixelDifferenceCount; bounds = $rotationBounds }
            }
            Assert-True (($rotationCrops | Measure-Object -Property pixelDifferenceCount -Maximum).Maximum -ge 30) "Barrel material hover rotations did not visibly change the preview. Crops: $rotationCropDirectory"
        }

        $results += [pscustomobject]@{ item = $item; screenshot = $screenshotPath; previewCrop = $previewCropPath; resultSlotCrop = $resultSlotCropPath; rotationCrops = $rotationCrops; bounds = $bounds }
        Write-Host "PASS $item $($bounds | ConvertTo-Json -Compress)"
    }

    [pscustomobject]@{ ok = $true; baseUrl = $BaseUrl; screenshotDirectory = $ScreenshotDirectory; results = $results }
} finally {
    if ($startedClient) {
        Stop-AutomationClient -ProcessId $clientProcessId
    }
}
