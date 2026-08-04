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

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Invoke-BridgeJson([string]$Method, [string]$Path, [object]$Body = $null) {
    $uri = "$BaseUrl$Path"
    if ($null -eq $Body) { return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 10 }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType "application/json" -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec $TimeoutSeconds
}

function Stop-AutomationClient([int]$ProcessId) {
    try { Invoke-BridgeJson Post "/client/stop" | Out-Null } catch { Write-Warning "Failed to stop dev client through automation bridge: $($_.Exception.Message)" }
    if ($ProcessId -gt 0 -and $null -ne (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        & taskkill.exe /PID $ProcessId /T /F | Out-Null
    }
}

function Save-Crop([string]$ScreenshotPath, [string]$CropPath, [object]$State, [object]$Bounds) {
    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $scaleX = $bitmap.Width / [double]$State.guiWidth
        $scaleY = $bitmap.Height / [double]$State.guiHeight
        $rectangle = [System.Drawing.Rectangle]::new([int][Math]::Round($Bounds.x * $scaleX), [int][Math]::Round($Bounds.y * $scaleY),
            [int][Math]::Round($Bounds.width * $scaleX), [int][Math]::Round($Bounds.height * $scaleY))
        $crop = $bitmap.Clone($rectangle, $bitmap.PixelFormat)
        try { $crop.Save($CropPath, [System.Drawing.Imaging.ImageFormat]::Png) } finally { $crop.Dispose() }
    } finally { $bitmap.Dispose() }
}

function Get-VisibleBounds([string]$ScreenshotPath) {
    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $left = $bitmap.Width; $right = 0; $top = $bitmap.Height; $bottom = 0; $count = 0
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $color = $bitmap.GetPixel($x, $y)
                if ($color.R -lt 35 -and $color.G -lt 35 -and $color.B -lt 35) { continue }
                $left = [Math]::Min($left, $x); $right = [Math]::Max($right, $x)
                $top = [Math]::Min($top, $y); $bottom = [Math]::Max($bottom, $y); $count++
            }
        }
        if ($count -eq 0) { return [pscustomobject]@{ count = 0; width = 0; height = 0; centerX = 0; centerY = 0; left = 0; right = 0; top = 0; bottom = 0; imageWidth = $bitmap.Width; imageHeight = $bitmap.Height } }
        $width = $right - $left + 1; $height = $bottom - $top + 1
        return [pscustomobject]@{ count = $count; width = $width; height = $height; centerX = $left + $width / 2D; centerY = $top + $height / 2D; left = $left; right = $right; top = $top; bottom = $bottom; imageWidth = $bitmap.Width; imageHeight = $bitmap.Height }
    } finally { $bitmap.Dispose() }
}

function Get-MagentaPixelCount([string]$ScreenshotPath) {
    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $count = 0
        for ($y = 0; $y -lt $bitmap.Height; $y++) { for ($x = 0; $x -lt $bitmap.Width; $x++) {
            $color = $bitmap.GetPixel($x, $y)
            if ($color.R -ge 150 -and $color.B -ge 150 -and $color.G -le 100) { $count++ }
        } }
        return $count
    } finally { $bitmap.Dispose() }
}

function Get-PixelDifferenceCount([string]$FirstPath, [string]$SecondPath) {
    $first = [System.Drawing.Bitmap]::FromFile($FirstPath)
    $second = [System.Drawing.Bitmap]::FromFile($SecondPath)
    try {
        Assert-True ($first.Width -eq $second.Width -and $first.Height -eq $second.Height) "Rotation crops have different dimensions. First: $FirstPath Second: $SecondPath"
        $count = 0
        for ($y = 0; $y -lt $first.Height; $y++) { for ($x = 0; $x -lt $first.Width; $x++) {
            $firstColor = $first.GetPixel($x, $y); $secondColor = $second.GetPixel($x, $y)
            if ([Math]::Abs($firstColor.R - $secondColor.R) + [Math]::Abs($firstColor.G - $secondColor.G) + [Math]::Abs($firstColor.B - $secondColor.B) -ge 45) { $count++ }
        } }
        return $count
    } finally { $second.Dispose(); $first.Dispose() }
}

function Get-CenteredPerceptualHash([string]$ScreenshotPath) {
    $source = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    $crop = $null; $resized = $null
    try {
        $cropSize = [int][Math]::Round($source.Width / 2.0)
        $crop = $source.Clone([System.Drawing.Rectangle]::new([int](($source.Width - $cropSize) / 2), [int](($source.Height - $cropSize) / 2), $cropSize, $cropSize), $source.PixelFormat)
        $resized = [System.Drawing.Bitmap]::new(17, 16)
        $graphics = [System.Drawing.Graphics]::FromImage($resized)
        try { $graphics.DrawImage($crop, 0, 0, 17, 16) } finally { $graphics.Dispose() }
        $hash = ""
        for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
            $left = $resized.GetPixel($x, $y); $right = $resized.GetPixel($x + 1, $y)
            $leftLuminance = $left.R * 0.299 + $left.G * 0.587 + $left.B * 0.114
            $rightLuminance = $right.R * 0.299 + $right.G * 0.587 + $right.B * 0.114
            $hash += [int]($leftLuminance -gt $rightLuminance)
        } }
        return $hash
    } finally {
        if ($null -ne $resized) { $resized.Dispose() }
        if ($null -ne $crop) { $crop.Dispose() }
        $source.Dispose()
    }
}

function Get-HammingDistance([string]$First, [string]$Second) {
    Assert-True ($First.Length -eq $Second.Length) "Perceptual hashes have different lengths."
    $distance = 0
    for ($i = 0; $i -lt $First.Length; $i++) { if ($First[$i] -ne $Second[$i]) { $distance++ } }
    return $distance
}

$startedClient = $false; $clientProcessId = 0
try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."
        $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
        if ($MaximizeClient) { $readyArgs.Maximize = $true }
        if ($MinimalRuntime) { $readyArgs.MinimalRuntime = $true }
        $ready = & "$PSScriptRoot\start-and-ready.ps1" @readyArgs
        $BaseUrl = $ready.baseUrl; $clientProcessId = $ready.processId; $startedClient = $true
    }
    $state = Invoke-BridgeJson Get "/state"
    Assert-True $state.playerLoaded "Dev client world is not loaded."
    $previewCrops = Join-Path $ScreenshotDirectory "preview-crops"
    $resultSlotCrops = Join-Path $ScreenshotDirectory "result-slot-crops"
    $rotationCrops = Join-Path $ScreenshotDirectory "rotation-crops"
    @($ScreenshotDirectory, $previewCrops, $resultSlotCrops, $rotationCrops) | ForEach-Object { New-Item -ItemType Directory -Path $_ -Force | Out-Null }
    $items = @("storage_io", "controller", "storage_link", "barrel", "barrel_directional", "limited_barrel_3", "limited_barrel_3_directional", "chest", "shulker_box", "backpack", "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots")
    $results = @()
    foreach ($item in $items) {
        $open = Invoke-BridgeJson Post "/storage/decoration-table-render-preview/open" @{ item = $item }
        Assert-True $open.ok "Failed to set up decoration table preview for ${item}: $($open.error)"
        Assert-True ($null -ne $open.preview -and $null -ne $open.resultSlot) "Decoration table bounds were not returned for $item."
        Invoke-BridgeJson Post "/mouse/move" @{ position = "top-left" } | Out-Null
        $firstOpenCropPath = ""
        if ($item -eq "barrel_directional") {
            Start-Sleep -Milliseconds 100
            $firstOpenScreenshotPath = Join-Path $ScreenshotDirectory "$item-first-open.png"
            Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $firstOpenScreenshotPath | Out-Null
            $firstOpenCropPath = Join-Path $rotationCrops "$item-first-open.png"
            Save-Crop $firstOpenScreenshotPath $firstOpenCropPath (Invoke-BridgeJson Get "/state") $open.preview
        }
        Start-Sleep -Milliseconds 2250
        $screenshotPath = Join-Path $ScreenshotDirectory "$item.png"
        Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $screenshotPath | Out-Null
        $state = Invoke-BridgeJson Get "/state"
        $previewPath = Join-Path $previewCrops "$item.png"
        Save-Crop $screenshotPath $previewPath $state $open.preview
        $bounds = Get-VisibleBounds $previewPath
        $minimumWidth = if ($item -like "leather_*") { 0.3 } else { 0.5 }
        Assert-True ($bounds.count -ge 30) "No substantial decoration result was rendered for $item. Preview crop: $previewPath"
        Assert-True ($bounds.width -ge $bounds.imageWidth * 0.2 -and $bounds.height -ge $bounds.imageHeight * 0.15 -and $bounds.width -ge $bounds.imageWidth * $minimumWidth) "Decoration result for $item is too small. Bounds=$($bounds | ConvertTo-Json -Compress)"
        Assert-True ($bounds.left -ge $bounds.imageWidth * 0.05 -and $bounds.right -le $bounds.imageWidth * 0.95 -and $bounds.top -ge $bounds.imageHeight * 0.05 -and $bounds.bottom -le $bounds.imageHeight * 0.95) "Decoration result for $item is clipped by its preview. Bounds=$($bounds | ConvertTo-Json -Compress)"
        Assert-True ($bounds.centerX -ge $bounds.imageWidth * 0.35 -and $bounds.centerX -le $bounds.imageWidth * 0.65 -and $bounds.centerY -ge $bounds.imageHeight * 0.2 -and $bounds.centerY -le $bounds.imageHeight * 0.75) "Decoration result for $item is not centered in its preview. Bounds=$($bounds | ConvertTo-Json -Compress)"
        if ($firstOpenCropPath) {
            $firstOpenHash = Get-CenteredPerceptualHash $firstOpenCropPath
            $settledHash = Get-CenteredPerceptualHash $previewPath
            Assert-True ((Get-HammingDistance $firstOpenHash $settledHash) -le 24) "Decoration table preview changed its resting pose after opening. First=$firstOpenCropPath Settled=$previewPath"
        }
        $resultSlotPath = Join-Path $resultSlotCrops "$item.png"
        Save-Crop $screenshotPath $resultSlotPath $state $open.resultSlot
        if ($item -eq "backpack") { Assert-True ((Get-MagentaPixelCount $resultSlotPath) -ge 5) "Backpack cloth tint is missing from the decoration table result slot. Crop: $resultSlotPath" }
        $rotationCaptures = @()
        if ($item -eq "barrel" -or $item -eq "barrel_directional" -or $item -eq "limited_barrel_3_directional") {
            Assert-True ($null -ne $open.rotationTargets) "Decoration table rotation targets were not returned for $item."
            $expectedRotations = @{ top = @(-90, 180, 0); side = @(0, 180, 0); bottom = @(90, 180, 0) }
            $rotationBaselinePath = Join-Path $rotationCrops "$item-default.png"
            Save-Crop $screenshotPath $rotationBaselinePath $state $open.preview
            $rotationBaselineBounds = Get-VisibleBounds $rotationBaselinePath
            foreach ($targetName in @("top", "side", "bottom")) {
                $target = $open.rotationTargets.$targetName; $expectedRotation = $expectedRotations[$targetName]
                Assert-True ($null -ne $target) "Missing $item $targetName rotation target."
                Assert-True ($target.xAxisRotation -eq $expectedRotation[0] -and $target.yAxisRotation -eq $expectedRotation[1] -and $target.zAxisRotation -eq $expectedRotation[2]) "Unexpected $item $targetName rotation target. Target=$($target | ConvertTo-Json -Compress)"
                Invoke-BridgeJson Post "/mouse/move" @{ x = $target.x; y = $target.y } | Out-Null
                Start-Sleep -Milliseconds 2000
                $rotationScreenshotPath = Join-Path $ScreenshotDirectory "$item-$targetName.png"
                Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $rotationScreenshotPath | Out-Null
                $rotationCropPath = Join-Path $rotationCrops "$item-$targetName.png"
                Save-Crop $rotationScreenshotPath $rotationCropPath (Invoke-BridgeJson Get "/state") $open.preview
                $rotationBounds = Get-VisibleBounds $rotationCropPath
                $pixelDifferenceCount = Get-PixelDifferenceCount $rotationBaselinePath $rotationCropPath
                Assert-True ($rotationBounds.count -ge 30 -and $rotationBounds.width -ge $rotationBounds.imageWidth * 0.45) "$item $targetName rotation did not render a substantial preview. Bounds=$($rotationBounds | ConvertTo-Json -Compress)"
                $rotationCaptures += [pscustomobject]@{ target = $targetName; crop = $rotationCropPath; pixelDifferenceCount = $pixelDifferenceCount; bounds = $rotationBounds }
            }
            Assert-True (($rotationCaptures | Measure-Object -Property pixelDifferenceCount -Maximum).Maximum -ge 30) "$item material hover rotations did not visibly change the preview. Crops: $rotationCrops"
            Invoke-BridgeJson Post "/mouse/move" @{ position = "top-left" } | Out-Null
            Start-Sleep -Milliseconds 2250
            $returnedScreenshotPath = Join-Path $ScreenshotDirectory "$item-returned.png"
            Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $returnedScreenshotPath | Out-Null
            $returnedCropPath = Join-Path $rotationCrops "$item-returned.png"
            Save-Crop $returnedScreenshotPath $returnedCropPath (Invoke-BridgeJson Get "/state") $open.preview
            $returnedBounds = Get-VisibleBounds $returnedCropPath
            Assert-True ($returnedBounds.count -ge 30 -and [Math]::Abs($returnedBounds.left - $rotationBaselineBounds.left) -le 2 -and [Math]::Abs($returnedBounds.right - $rotationBaselineBounds.right) -le 2 -and [Math]::Abs($returnedBounds.top - $rotationBaselineBounds.top) -le 2 -and [Math]::Abs($returnedBounds.bottom - $rotationBaselineBounds.bottom) -le 2) "$item material hover did not return to the default preview. Returned=$($returnedBounds | ConvertTo-Json -Compress) Crop=$returnedCropPath"
            $rotationCaptures += [pscustomobject]@{ target = "returned"; crop = $returnedCropPath; bounds = $returnedBounds }
        }
        $results += [pscustomobject]@{ item = $item; previewCrop = $previewPath; resultSlotCrop = $resultSlotPath; rotationCrops = $rotationCaptures; bounds = $bounds }
        Write-Host "PASS $item $($bounds | ConvertTo-Json -Compress)"
    }
    [pscustomobject]@{ ok = $true; baseUrl = $BaseUrl; screenshotDirectory = $ScreenshotDirectory; results = $results }
} finally {
    if ($startedClient) { Stop-AutomationClient $clientProcessId }
}
