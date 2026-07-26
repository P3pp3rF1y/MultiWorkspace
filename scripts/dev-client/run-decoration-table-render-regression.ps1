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

function Test-BarrelCoreRotationTarget {
    param(
        [Parameter(Mandatory = $true)] [string]$Name,
        [Parameter(Mandatory = $true)] [object]$Target
    )

    $expectedRotations = @{
        topCore = @{ rotationX = 90; rotationY = 180 }
        sideCore = @{ rotationX = 0; rotationY = 180 }
        bottomCore = @{ rotationX = -90; rotationY = 180 }
    }
    Assert-True ($null -ne $expectedRotations[$Name]) "Unknown barrel core rotation target $Name."
    $expected = $expectedRotations[$Name]
    Assert-True ($Target.rotationX -eq $expected.rotationX -and $Target.rotationY -eq $expected.rotationY) "Barrel core rotation target $Name did not match the 1.21.1 head-on reference. Target=$($Target | ConvertTo-Json -Compress)"
}

function Test-BarrelPreviewScale {
    param(
        [Parameter(Mandatory = $true)] [string]$Name,
        [Parameter(Mandatory = $true)] [object]$Bounds
    )

    Assert-True ($Bounds.width -le $Bounds.imageWidth * 0.75 -and $Bounds.height -le $Bounds.imageHeight * 0.75) "Barrel preview $Name is oversized. Bounds=$($Bounds | ConvertTo-Json -Compress)"
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

        $results += [pscustomobject]@{ item = $item; screenshot = $screenshotPath; previewCrop = $previewCropPath; resultSlotCrop = $resultSlotCropPath; bounds = $bounds }
        Write-Host "PASS $item $($bounds | ConvertTo-Json -Compress)"

        if ($item -eq "barrel") {
            Test-BarrelPreviewScale -Name "neutral" -Bounds $bounds
            foreach ($targetName in @("topCore", "sideCore", "bottomCore")) {
                $target = $open.rotationTargets.PSObject.Properties[$targetName].Value
                Assert-True ($null -ne $target) "Decoration table barrel core target $targetName was not returned."
                Test-BarrelCoreRotationTarget -Name $targetName -Target $target
                $move = Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ x = $target.x; y = $target.y }
                Assert-True $move.ok "Failed to move mouse to barrel core target $targetName."
                Start-Sleep -Milliseconds 2000
                $move = Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ x = $target.x; y = $target.y }
                Assert-True $move.ok "Failed to keep mouse on barrel core target $targetName."
                Start-Sleep -Milliseconds 1000

                $sideScreenshotPath = Join-Path $ScreenshotDirectory "barrel-$targetName.png"
                Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $sideScreenshotPath | Out-Null
                $state = Invoke-BridgeJson -Method Get -Path "/state"
                $sidePreviewCropPath = Join-Path $previewCropDirectory "barrel-$targetName.png"
                Save-Crop -ScreenshotPath $sideScreenshotPath -CropPath $sidePreviewCropPath -State $state -Bounds $open.preview
                $sideBounds = Get-VisibleBounds -ScreenshotPath $sidePreviewCropPath
                Assert-True ($sideBounds.count -ge 30) "No substantial barrel core preview was rendered for $targetName. Preview crop: $sidePreviewCropPath"
                Assert-True ($sideBounds.width -ge $sideBounds.imageWidth * 0.55 -and $sideBounds.height -ge $sideBounds.imageHeight * 0.15) "Barrel core preview for $targetName is not head-on enough to fill the reference crop. Bounds=$($sideBounds | ConvertTo-Json -Compress)"
                Test-BarrelPreviewScale -Name $targetName -Bounds $sideBounds
                $results += [pscustomobject]@{ item = "barrel-$targetName"; screenshot = $sideScreenshotPath; previewCrop = $sidePreviewCropPath; target = $target; bounds = $sideBounds }
                Write-Host "PASS barrel-$targetName $($sideBounds | ConvertTo-Json -Compress)"
            }
        }
    }

    [pscustomobject]@{ ok = $true; baseUrl = $BaseUrl; screenshotDirectory = $ScreenshotDirectory; results = $results }
} finally {
    if ($startedClient) {
        Stop-AutomationClient -ProcessId $clientProcessId
    }
}
