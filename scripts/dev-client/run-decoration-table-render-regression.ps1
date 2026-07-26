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
        $minY = 0
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

function Get-RedPixelBounds {
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
                if ($color.R -ge 100 -and $color.G -le 100 -and $color.B -le 100) {
                    $left = [Math]::Min($left, $x)
                    $right = [Math]::Max($right, $x)
                    $top = [Math]::Min($top, $y)
                    $bottom = [Math]::Max($bottom, $y)
                    $count++
                }
            }
        }
        if ($count -eq 0) {
            return [pscustomobject]@{ count = 0; left = 0; right = 0; top = 0; bottom = 0; width = 0; height = 0; centerX = 0; centerY = 0; imageWidth = $bitmap.Width; imageHeight = $bitmap.Height }
        }
        return [pscustomobject]@{
            count = $count
            left = $left
            right = $right
            top = $top
            bottom = $bottom
            width = $right - $left + 1
            height = $bottom - $top + 1
            centerX = $left + ($right - $left + 1) / 2D
            centerY = $top + ($bottom - $top + 1) / 2D
            imageWidth = $bitmap.Width
            imageHeight = $bitmap.Height
        }
    } finally {
        $bitmap.Dispose()
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

    $barrelOpen = Invoke-BridgeJson -Method Post -Path "/storage/decoration-table-render-preview/open" -Body @{ item = "barrel" }
    Assert-True $barrelOpen.ok "Failed to set up decoration table preview for barrel rotations: $($barrelOpen.error)"
    Assert-True ($null -ne $barrelOpen.coreSlotHoverTargets) "Decoration table core-slot hover targets were not returned for barrel."
    $rotationDirectory = Join-Path $ScreenshotDirectory "barrel-rotations"
    New-Item -ItemType Directory -Path $rotationDirectory -Force | Out-Null
    $rotationResults = @()
    foreach ($rotation in @("top", "side", "bottom")) {
        $target = $barrelOpen.coreSlotHoverTargets.$rotation
        Assert-True ($null -ne $target) "Decoration table $rotation core-slot hover target was not returned for barrel."
        Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ x = $target.x; y = $target.y } | Out-Null
        Start-Sleep -Milliseconds 2000

        $screenshotPath = Join-Path $rotationDirectory "$rotation.png"
        Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $screenshotPath | Out-Null
        $state = Invoke-BridgeJson -Method Get -Path "/state"
        $previewCropPath = Join-Path $rotationDirectory "$rotation-preview.png"
        Save-DecorationTablePreviewCrop -ScreenshotPath $screenshotPath -PreviewCropPath $previewCropPath -State $state -Preview $barrelOpen.preview
        $bounds = Get-VisibleBounds -ScreenshotPath $previewCropPath
        Assert-True ($bounds.count -ge 30) "No substantial barrel result was rendered for the $rotation rotation. Preview crop: $previewCropPath"
        Assert-True ($bounds.width -ge $bounds.imageWidth * 0.55 -and $bounds.height -ge $bounds.imageHeight * 0.35) "Barrel result for the $rotation rotation is too small. Bounds=$($bounds | ConvertTo-Json -Compress)"
        if ($rotation -ne "side") {
            Assert-True ($bounds.left -ge $bounds.imageWidth * 0.05 -and $bounds.right -le $bounds.imageWidth * 0.95 -and $bounds.top -ge $bounds.imageHeight * 0.05 -and $bounds.bottom -le $bounds.imageHeight * 0.95) "Barrel result for the $rotation rotation is clipped by its preview. Bounds=$($bounds | ConvertTo-Json -Compress)"
        }
        $rotationResults += [pscustomobject]@{ rotation = $rotation; screenshot = $screenshotPath; previewCrop = $previewCropPath; bounds = $bounds }
        Write-Host "PASS barrel-$rotation $($bounds | ConvertTo-Json -Compress)"
    }

    $sidePreviewCropPath = ($rotationResults | Where-Object rotation -eq "side").previewCrop
    $sideRedBounds = Get-RedPixelBounds -ScreenshotPath $sidePreviewCropPath
    Assert-True ($sideRedBounds.width -ge $sideRedBounds.imageWidth * 0.5 -and $sideRedBounds.height -ge $sideRedBounds.imageHeight * 0.35) "Barrel side rotation is not head-on. Bounds=$($sideRedBounds | ConvertTo-Json -Compress)"
    Assert-True ($sideRedBounds.centerX -ge $sideRedBounds.imageWidth * 0.4 -and $sideRedBounds.centerX -le $sideRedBounds.imageWidth * 0.6 -and $sideRedBounds.centerY -ge $sideRedBounds.imageHeight * 0.3 -and $sideRedBounds.centerY -le $sideRedBounds.imageHeight * 0.7) "Barrel side rotation is not centered head-on. Bounds=$($sideRedBounds | ConvertTo-Json -Compress)"
    Assert-True ($sideRedBounds.count -ge 250) "Barrel side rotation is not showing the redstone side head-on. Preview crop: $sidePreviewCropPath"

    [pscustomobject]@{
        ok = $true
        baseUrl = $BaseUrl
        screenshotDirectory = $ScreenshotDirectory
        results = $results
        rotationResults = $rotationResults
    }
} finally {
    if ($startedClient) {
        Stop-AutomationClient -ProcessId $clientProcessId
    }
}
