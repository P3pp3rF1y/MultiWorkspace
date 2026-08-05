param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$BaseUrl = "",
    [string]$ScreenshotDirectory = (Join-Path (Resolve-Path "$PSScriptRoot\..\..\workspace\run").Path "decoration-table-manual-rotation"),
    [string]$ReferenceDirectory = "",
    [string]$Item = "chest",
    [int]$TimeoutSeconds = 360,
    [switch]$NoStartClient,
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
    param([string]$Method, [string]$Path, [object]$Body = $null)

    $uri = "$BaseUrl$Path"
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 10
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec $TimeoutSeconds
}

function Stop-AutomationClient {
    param([int]$ProcessId)

    try {
        Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    } catch {
        Write-Warning "Failed to stop dev client through automation bridge: $($_.Exception.Message)"
    }
    if ($ProcessId -gt 0 -and $null -ne (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        & taskkill.exe /PID $ProcessId /T /F | Out-Null
    }
}

function Save-PreviewCrop {
    param([string]$ScreenshotPath, [string]$CropPath, [object]$State, [object]$Preview)

    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $rectangle = [System.Drawing.Rectangle]::new(
            [int][Math]::Round($Preview.x * $bitmap.Width / [double]$State.guiWidth),
            [int][Math]::Round($Preview.y * $bitmap.Height / [double]$State.guiHeight),
            [int][Math]::Round($Preview.width * $bitmap.Width / [double]$State.guiWidth),
            [int][Math]::Round($Preview.height * $bitmap.Height / [double]$State.guiHeight)
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

function Get-VisiblePixelCount {
    param([string]$Path)

    $bitmap = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $count = 0
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $pixel = $bitmap.GetPixel($x, $y)
                if ($pixel.R -ge 35 -or $pixel.G -ge 35 -or $pixel.B -ge 35) {
                    $count++
                }
            }
        }
        return $count
    } finally {
        $bitmap.Dispose()
    }
}

function Get-RightSideGrayPixelCount {
    param([string]$Path)

    $bitmap = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $count = 0
        $minimumX = [int][Math]::Floor($bitmap.Width * 0.6)
        $minimumY = [int][Math]::Floor($bitmap.Height * 0.25)
        $maximumY = [int][Math]::Ceiling($bitmap.Height * 0.75)
        for ($y = $minimumY; $y -lt $maximumY; $y++) {
            for ($x = $minimumX; $x -lt $bitmap.Width; $x++) {
                $pixel = $bitmap.GetPixel($x, $y)
                if ($pixel.R -ge 80 -and $pixel.R -le 220 -and [Math]::Abs($pixel.R - $pixel.G) -le 12 -and [Math]::Abs($pixel.G - $pixel.B) -le 12) {
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
    param([string]$FirstPath, [string]$SecondPath)

    $first = [System.Drawing.Bitmap]::FromFile($FirstPath)
    $second = [System.Drawing.Bitmap]::FromFile($SecondPath)
    try {
        Assert-True ($first.Width -eq $second.Width -and $first.Height -eq $second.Height) "Rotation crops have different dimensions."
        $count = 0
        for ($y = 0; $y -lt $first.Height; $y++) {
            for ($x = 0; $x -lt $first.Width; $x++) {
                $firstPixel = $first.GetPixel($x, $y)
                $secondPixel = $second.GetPixel($x, $y)
                if ([Math]::Abs($firstPixel.R - $secondPixel.R) + [Math]::Abs($firstPixel.G - $secondPixel.G) + [Math]::Abs($firstPixel.B - $secondPixel.B) -ge 45) {
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

function Save-ComparisonImage {
    param([string]$ReferencePath, [string]$CandidatePath, [string]$OutputPath)

    $reference = [System.Drawing.Bitmap]::FromFile($ReferencePath)
    $candidate = [System.Drawing.Bitmap]::FromFile($CandidatePath)
    try {
        Assert-True ($reference.Width -eq $candidate.Width -and $reference.Height -eq $candidate.Height) "Reference and candidate crops have different dimensions."
        $comparison = [System.Drawing.Bitmap]::new($reference.Width * 2, $reference.Height)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($comparison)
            try {
                $graphics.DrawImageUnscaled($reference, 0, 0)
                $graphics.DrawImageUnscaled($candidate, $reference.Width, 0)
            } finally {
                $graphics.Dispose()
            }
            $comparison.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $comparison.Dispose()
        }
    } finally {
        $candidate.Dispose()
        $reference.Dispose()
    }
}

$startedClient = $false
$clientProcessId = 0

try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."
        $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
        if ($MinimalRuntime) {
            $readyArgs.MinimalRuntime = $true
        }
        $ready = & "$PSScriptRoot\start-and-ready.ps1" @readyArgs
        $BaseUrl = $ready.baseUrl
        $clientProcessId = $ready.processId
        $startedClient = $true
    }

    Assert-True (Invoke-BridgeJson -Method Get -Path "/state").playerLoaded "Dev client world is not loaded."
    New-Item -ItemType Directory -Path $ScreenshotDirectory -Force | Out-Null
    $cropDirectory = Join-Path $ScreenshotDirectory "preview-crops"
    New-Item -ItemType Directory -Path $cropDirectory -Force | Out-Null

    $open = Invoke-BridgeJson -Method Post -Path "/storage/decoration-table-render-preview/open" -Body @{ item = $Item }
    Assert-True $open.ok "Failed to open Decoration Table $Item preview."
    $steps = @(
        [pscustomobject]@{ name = "default"; dragX = 0; dragY = 0 },
        [pscustomobject]@{ name = "yaw-initial1"; dragX = 1; dragY = 0 },
        [pscustomobject]@{ name = "yaw90+44"; dragX = 44; dragY = 0 },
        [pscustomobject]@{ name = "yaw180+45"; dragX = 45; dragY = 0 },
        [pscustomobject]@{ name = "pitch"; dragX = 0; dragY = 45 }
    )
    $results = @()
    foreach ($step in $steps) {
        if ($step.dragX -ne 0 -or $step.dragY -ne 0) {
            $drag = Invoke-BridgeJson -Method Post -Path "/storage/decoration-table-render-preview/drag" -Body @{ dragX = $step.dragX; dragY = $step.dragY }
            Assert-True $drag.dragged "Decoration Table preview did not accept $($step.name) drag."
            Start-Sleep -Milliseconds 250
        }
        $screenshotPath = Join-Path $ScreenshotDirectory "$($step.name).png"
        Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $screenshotPath | Out-Null
        $cropPath = Join-Path $cropDirectory "$($step.name).png"
        Save-PreviewCrop -ScreenshotPath $screenshotPath -CropPath $cropPath -State (Invoke-BridgeJson -Method Get -Path "/state") -Preview $open.preview
        $visiblePixels = Get-VisiblePixelCount -Path $cropPath
        Assert-True ($visiblePixels -ge 30) "Decoration Table preview is empty after $($step.name) drag."
        $rightSideGrayPixels = if ($step.name -eq "default") { Get-RightSideGrayPixelCount -Path $cropPath } else { 0 }
        if ($step.name -eq "default") {
            Assert-True ($rightSideGrayPixels -ge 3) "Chest preview is missing its gray latch on the right side. Crop: $cropPath"
        }
        $results += [pscustomobject]@{ step = $step.name; crop = $cropPath; visiblePixels = $visiblePixels; rightSideGrayPixels = $rightSideGrayPixels }
    }

    $defaultCrop = Join-Path $cropDirectory "default.png"
    foreach ($result in $results | Where-Object step -ne "default") {
        $result | Add-Member -NotePropertyName changedPixels -NotePropertyValue (Get-PixelDifferenceCount -FirstPath $defaultCrop -SecondPath $result.crop)
        Assert-True ($result.changedPixels -ge 30) "Decoration Table preview did not visibly change after $($result.step) drag."
    }

    if (-not [string]::IsNullOrWhiteSpace($ReferenceDirectory)) {
        $comparisonDirectory = Join-Path $ScreenshotDirectory "comparison-with-reference"
        New-Item -ItemType Directory -Path $comparisonDirectory -Force | Out-Null
        foreach ($result in $results) {
            $referencePath = Join-Path $ReferenceDirectory "$($result.step).png"
            Assert-True (Test-Path -LiteralPath $referencePath) "Reference crop is missing: $referencePath"
            $comparisonPath = Join-Path $comparisonDirectory "$($result.step).png"
            Save-ComparisonImage -ReferencePath $referencePath -CandidatePath $result.crop -OutputPath $comparisonPath
            $result | Add-Member -NotePropertyName referenceDifferencePixels -NotePropertyValue (Get-PixelDifferenceCount -FirstPath $referencePath -SecondPath $result.crop)
        }
    }

    [pscustomobject]@{ ok = $true; baseUrl = $BaseUrl; screenshotDirectory = $ScreenshotDirectory; results = $results }
} finally {
    if ($startedClient) {
        Stop-AutomationClient -ProcessId $clientProcessId
    }
}
