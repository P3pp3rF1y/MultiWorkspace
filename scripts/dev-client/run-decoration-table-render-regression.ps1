param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [ValidateSet("neoforge", "fabric")]
    [string]$Loader = "neoforge",
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
    param(
        [bool]$Condition,
        [string]$Message
    )

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

function Stop-AutomationClient {
    try {
        Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    } catch {
        Write-Warning "Failed to stop dev client through automation bridge: $($_.Exception.Message)"
    }
}

function Wait-AutomationClientStopped {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        Start-Sleep -Milliseconds 500
        if ($clientProcessId -and -not (Get-Process -Id $clientProcessId -ErrorAction SilentlyContinue)) {
            return
        }
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for dev client to stop."
}

function Test-VisiblePixel {
    param([System.Drawing.Color]$Color)

    return $Color.R -ge 35 -or $Color.G -ge 35 -or $Color.B -ge 35
}

function Get-VisibleBounds {
    param([Parameter(Mandatory = $true)] [string]$ScreenshotPath)

    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $minX = 0
        $maxX = $bitmap.Width
        # Slot tooltips can overlap the top edge of the preview crop after opening the legacy screen.
        # All valid preview geometry starts below this band.
        $minY = 24
        $maxY = $bitmap.Height
        $left = $maxX
        $right = $minX
        $top = $maxY
        $bottom = $minY
        $count = 0

        for ($y = $minY; $y -lt $maxY; $y++) {
            for ($x = $minX; $x -lt $maxX; $x++) {
                if (-not (Test-VisiblePixel -Color $bitmap.GetPixel($x, $y))) {
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
        Assert-True ($first.Width -eq $second.Width -and $first.Height -eq $second.Height) "Cannot compare crops with different dimensions. First=$FirstPath Second=$SecondPath"
        $count = 0
        for ($y = 0; $y -lt $first.Height; $y++) {
            for ($x = 0; $x -lt $first.Width; $x++) {
                if ($first.GetPixel($x, $y).ToArgb() -ne $second.GetPixel($x, $y).ToArgb()) {
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

function Save-DecorationTablePreviewCrop {
    param(
        [Parameter(Mandatory = $true)] [string]$ScreenshotPath,
        [Parameter(Mandatory = $true)] [string]$PreviewCropPath,
        [Parameter(Mandatory = $true)] [object]$State,
        [Parameter(Mandatory = $true)] [object]$Preview
    )

    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $scaleX = $bitmap.Width / [double]$State.guiWidth
        $scaleY = $bitmap.Height / [double]$State.guiHeight
        $rectangle = [System.Drawing.Rectangle]::new(
            [int][Math]::Round($Preview.x * $scaleX),
            [int][Math]::Round($Preview.y * $scaleY),
            [int][Math]::Round($Preview.width * $scaleX),
            [int][Math]::Round($Preview.height * $scaleY)
        )
        $crop = $bitmap.Clone($rectangle, $bitmap.PixelFormat)
        try {
            $crop.Save($PreviewCropPath, [System.Drawing.Imaging.ImageFormat]::Png)
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
        $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; Loader = $Loader; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
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

    $state = Invoke-BridgeJson -Method Get -Path "/state"
    Assert-True $state.playerLoaded "Dev client world is not loaded."
    New-Item -ItemType Directory -Path $ScreenshotDirectory -Force | Out-Null
    $previewCropDirectory = Join-Path $ScreenshotDirectory "preview-crops"
    New-Item -ItemType Directory -Path $previewCropDirectory -Force | Out-Null
    $resultSlotCropDirectory = Join-Path $ScreenshotDirectory "result-slot-crops"
    New-Item -ItemType Directory -Path $resultSlotCropDirectory -Force | Out-Null

    $results = @()
    $items = @("storage_io", "controller", "storage_link", "barrel", "limited_barrel_3", "chest", "shulker_box", "backpack", "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots")
    foreach ($item in $items) {
        $open = Invoke-BridgeJson -Method Post -Path "/storage/decoration-table-render-preview/open" -Body @{ item = $item }
        Assert-True $open.ok "Failed to set up decoration table preview for ${item}: $($open.error)"
        Assert-True ($null -ne $open.preview) "Decoration table preview bounds were not returned for $item."
        Assert-True ($null -ne $open.resultSlot) "Decoration table result slot bounds were not returned for $item."
        Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ position = "top-left" } | Out-Null
        Start-Sleep -Milliseconds 1250

        $screenshotPath = Join-Path $ScreenshotDirectory "$item.png"
        Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $screenshotPath | Out-Null
        $state = Invoke-BridgeJson -Method Get -Path "/state"
        $previewCropPath = Join-Path $previewCropDirectory "$item.png"
        Save-DecorationTablePreviewCrop -ScreenshotPath $screenshotPath -PreviewCropPath $previewCropPath -State $state -Preview $open.preview
        $bounds = Get-VisibleBounds -ScreenshotPath $previewCropPath
        $minimumWidthFraction = if ($item -like "leather_*") { 0.3 } elseif ($item -eq "backpack") { 0.5 } else { 0.55 }
        Assert-True ($bounds.count -ge 30) "No substantial decoration result was rendered for $item. Preview crop: $previewCropPath"
        Assert-True ($bounds.width -ge $bounds.imageWidth * 0.2 -and $bounds.height -ge $bounds.imageHeight * 0.15) "Decoration result for $item is too small. Bounds=$($bounds | ConvertTo-Json -Compress)"
        Assert-True ($bounds.width -ge $bounds.imageWidth * $minimumWidthFraction) "Decoration result for $item does not fill enough of the preview. Bounds=$($bounds | ConvertTo-Json -Compress)"
        Assert-True ($bounds.left -ge $bounds.imageWidth * 0.05 -and $bounds.right -le $bounds.imageWidth * 0.95 -and $bounds.top -ge $bounds.imageHeight * 0.05 -and $bounds.bottom -le $bounds.imageHeight * 0.95) "Decoration result for $item is clipped by its preview. Bounds=$($bounds | ConvertTo-Json -Compress)"
        Assert-True ($bounds.centerX -ge $bounds.imageWidth * 0.35 -and $bounds.centerX -le $bounds.imageWidth * 0.65 -and $bounds.centerY -ge $bounds.imageHeight * 0.2 -and $bounds.centerY -le $bounds.imageHeight * 0.75) "Decoration result for $item is not centered in its preview. Bounds=$($bounds | ConvertTo-Json -Compress)"

        $resultSlotCropPath = Join-Path $resultSlotCropDirectory "$item.png"
        Save-DecorationTablePreviewCrop -ScreenshotPath $screenshotPath -PreviewCropPath $resultSlotCropPath -State $state -Preview $open.resultSlot
        if ($item -eq "backpack") {
            Assert-True ((Get-MagentaPixelCount -ScreenshotPath $resultSlotCropPath) -ge 5) "Backpack cloth tint is missing from the decoration table result slot. Crop: $resultSlotCropPath"
        }

        $results += [pscustomobject]@{ item = $item; screenshot = $screenshotPath; previewCrop = $previewCropPath; resultSlotCrop = $resultSlotCropPath; bounds = $bounds }
        Write-Host "PASS $item $($bounds | ConvertTo-Json -Compress)"
    }

    $barrelOpen = Invoke-BridgeJson -Method Post -Path "/storage/decoration-table-render-preview/open" -Body @{ item = "barrel_directional" }
    Assert-True $barrelOpen.ok "Failed to set up directional barrel preview: $($barrelOpen.error)"
    Assert-True ($null -ne $barrelOpen.coreSlotHoverTargets) "Decoration table core-slot hover targets were not returned for directional barrel."
    $rotationDirectory = Join-Path $ScreenshotDirectory "barrel-directional"
    New-Item -ItemType Directory -Path $rotationDirectory -Force | Out-Null
    $rotationResults = @()
    Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ position = "top-left" } | Out-Null
    Start-Sleep -Milliseconds 2500
    $firstOpenScreenshotPath = Join-Path $rotationDirectory "first-open.png"
    Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $firstOpenScreenshotPath | Out-Null
    $state = Invoke-BridgeJson -Method Get -Path "/state"
    $firstOpenCropPath = Join-Path $rotationDirectory "first-open-preview.png"
    Save-DecorationTablePreviewCrop -ScreenshotPath $firstOpenScreenshotPath -PreviewCropPath $firstOpenCropPath -State $state -Preview $barrelOpen.preview
    $firstOpenBounds = Get-VisibleBounds -ScreenshotPath $firstOpenCropPath
    Assert-True ($firstOpenBounds.count -ge 30) "No directional barrel result was rendered on first open. Preview crop: $firstOpenCropPath"

    foreach ($rotation in @("top", "side", "bottom")) {
        $target = $barrelOpen.coreSlotHoverTargets.$rotation
        Assert-True ($null -ne $target) "Decoration table $rotation core-slot hover target was not returned for directional barrel."
        Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ x = $target.x; y = $target.y } | Out-Null
        Start-Sleep -Milliseconds 2000

        $screenshotPath = Join-Path $rotationDirectory "$rotation.png"
        Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $screenshotPath | Out-Null
        $state = Invoke-BridgeJson -Method Get -Path "/state"
        $previewCropPath = Join-Path $rotationDirectory "$rotation-preview.png"
        Save-DecorationTablePreviewCrop -ScreenshotPath $screenshotPath -PreviewCropPath $previewCropPath -State $state -Preview $barrelOpen.preview
        $bounds = Get-VisibleBounds -ScreenshotPath $previewCropPath
        Assert-True ($bounds.count -ge 30) "No directional barrel result was rendered for $rotation. Preview crop: $previewCropPath"
        $changedPixels = Get-PixelDifferenceCount -FirstPath $firstOpenCropPath -SecondPath $previewCropPath
        Assert-True ($changedPixels -ge 30) "Directional barrel $rotation hover did not change the preview. Changed pixels=$changedPixels Crop=$previewCropPath"
        $rotationResults += [pscustomobject]@{ rotation = $rotation; screenshot = $screenshotPath; previewCrop = $previewCropPath; bounds = $bounds; changedPixels = $changedPixels }
        Write-Host "PASS barrel-directional-$rotation $($bounds | ConvertTo-Json -Compress)"
    }

    Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ position = "top-left" } | Out-Null
    Start-Sleep -Milliseconds 2500
    $returnScreenshotPath = Join-Path $rotationDirectory "return.png"
    Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $returnScreenshotPath | Out-Null
    $state = Invoke-BridgeJson -Method Get -Path "/state"
    $returnCropPath = Join-Path $rotationDirectory "return-preview.png"
    Save-DecorationTablePreviewCrop -ScreenshotPath $returnScreenshotPath -PreviewCropPath $returnCropPath -State $state -Preview $barrelOpen.preview
    $returnBounds = Get-VisibleBounds -ScreenshotPath $returnCropPath
    Assert-True ($returnBounds.count -ge 30) "No directional barrel result was rendered after returning to the default preview. Crop: $returnCropPath"
    $returnChangedPixels = Get-PixelDifferenceCount -FirstPath $firstOpenCropPath -SecondPath $returnCropPath
    Assert-True ($returnChangedPixels -le 250) "Directional barrel preview did not return to its first-open view. Changed pixels=$returnChangedPixels Crop=$returnCropPath"

    [pscustomobject]@{
        ok = $true
        baseUrl = $BaseUrl
        screenshotDirectory = $ScreenshotDirectory
        results = $results
        firstOpen = [pscustomobject]@{ screenshot = $firstOpenScreenshotPath; previewCrop = $firstOpenCropPath; bounds = $firstOpenBounds }
        rotationResults = $rotationResults
        return = [pscustomobject]@{ screenshot = $returnScreenshotPath; previewCrop = $returnCropPath; bounds = $returnBounds; changedPixels = $returnChangedPixels }
    }
} finally {
    if ($startedClient) {
        Stop-AutomationClient
        Wait-AutomationClientStopped
    }
}
