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
    if (-not $Condition) { throw $Message }
}

function Invoke-BridgeJson {
    param([string]$Method, [string]$Path, [object]$Body = $null)
    $uri = "$BaseUrl$Path"
    if ($null -eq $Body) { return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 10 }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec $TimeoutSeconds
}

function Stop-AutomationClient {
    param([int]$ProcessId)
    try { Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null } catch { Write-Warning "Failed to stop dev client through automation bridge: $($_.Exception.Message)" }
    if ($ProcessId -gt 0 -and $null -ne (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) { & taskkill.exe /PID $ProcessId /T /F | Out-Null }
}

function Save-PreviewCrop {
    param([string]$ScreenshotPath, [string]$CropPath, [object]$State, [object]$Preview)
    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $crop = $bitmap.Clone([System.Drawing.Rectangle]::new(
                [int][Math]::Round($Preview.x * $bitmap.Width / [double]$State.guiWidth),
                [int][Math]::Round($Preview.y * $bitmap.Height / [double]$State.guiHeight),
                [int][Math]::Round($Preview.width * $bitmap.Width / [double]$State.guiWidth),
                [int][Math]::Round($Preview.height * $bitmap.Height / [double]$State.guiHeight)), $bitmap.PixelFormat)
        try { $crop.Save($CropPath, [System.Drawing.Imaging.ImageFormat]::Png) } finally { $crop.Dispose() }
    } finally { $bitmap.Dispose() }
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
                $a = $first.GetPixel($x, $y)
                $b = $second.GetPixel($x, $y)
                if ([Math]::Abs($a.R - $b.R) + [Math]::Abs($a.G - $b.G) + [Math]::Abs($a.B - $b.B) -ge 45) { $count++ }
            }
        }
        return $count
    } finally { $second.Dispose(); $first.Dispose() }
}

function Get-LatchSideGrayPixelCounts {
    param([string]$ScreenshotPath)
    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $left = 0; $right = 0
        for ($y = [int]($bitmap.Height * 0.27); $y -lt [int]($bitmap.Height * 0.73); $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $color = $bitmap.GetPixel($x, $y)
                if ($color.R -lt 100 -or $color.G -lt 100 -or $color.B -lt 100 -or [Math]::Abs($color.R - $color.G) -gt 15 -or [Math]::Abs($color.G - $color.B) -gt 15) { continue }
                if ($x -lt $bitmap.Width / 2) { $left++ } else { $right++ }
            }
        }
        return [pscustomobject]@{ left = $left; right = $right }
    } finally { $bitmap.Dispose() }
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
            try { $graphics.DrawImageUnscaled($reference, 0, 0); $graphics.DrawImageUnscaled($candidate, $reference.Width, 0) } finally { $graphics.Dispose() }
            $comparison.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $comparison.Dispose() }
    } finally { $candidate.Dispose(); $reference.Dispose() }
}

$startedClient = $false
$clientProcessId = 0
try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."
        $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
        if ($MinimalRuntime) { $readyArgs.MinimalRuntime = $true }
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
    Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ x = 0; y = 0 } | Out-Null
    Start-Sleep -Milliseconds 2250
    $steps = @(
        [pscustomobject]@{ name = "default"; dragX = 0; dragY = 0 },
        [pscustomobject]@{ name = "yaw-initial"; dragX = 1; dragY = 0 },
        [pscustomobject]@{ name = "yaw90"; dragX = 44; dragY = 0 },
        [pscustomobject]@{ name = "yaw180"; dragX = 45; dragY = 0 },
        [pscustomobject]@{ name = "pitch45"; dragX = 0; dragY = 45 }
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
        $results += [pscustomobject]@{ step = $step.name; crop = $cropPath }
    }
    $defaultCrop = Join-Path $cropDirectory "default.png"
    foreach ($result in $results | Where-Object step -ne "default") {
        $result | Add-Member -NotePropertyName changedPixels -NotePropertyValue (Get-PixelDifferenceCount -FirstPath $defaultCrop -SecondPath $result.crop)
        Assert-True ($result.changedPixels -ge 30) "Decoration Table preview did not visibly change after $($result.step) drag."
    }
    $initialYawLatchPixels = Get-LatchSideGrayPixelCounts (Join-Path $cropDirectory "yaw-initial.png")
    $results | Where-Object step -eq "yaw-initial" | Add-Member -NotePropertyName latchGrayPixels -NotePropertyValue $initialYawLatchPixels
    Assert-True ($initialYawLatchPixels.right -ge 10 -and $initialYawLatchPixels.right -gt $initialYawLatchPixels.left) "The first horizontal drag moved the chest latch from its expected right side. Gray pixels=$($initialYawLatchPixels | ConvertTo-Json -Compress)"
    if (-not [string]::IsNullOrWhiteSpace($ReferenceDirectory)) {
        $comparisonDirectory = Join-Path $ScreenshotDirectory "comparison-with-reference"
        New-Item -ItemType Directory -Path $comparisonDirectory -Force | Out-Null
        foreach ($result in $results) {
            $referencePath = Join-Path $ReferenceDirectory "$($result.step).png"
            Assert-True (Test-Path -LiteralPath $referencePath) "Reference crop is missing: $referencePath"
            Save-ComparisonImage -ReferencePath $referencePath -CandidatePath $result.crop -OutputPath (Join-Path $comparisonDirectory "$($result.step).png")
            $result | Add-Member -NotePropertyName referenceDifferencePixels -NotePropertyValue (Get-PixelDifferenceCount -FirstPath $referencePath -SecondPath $result.crop)
        }
    }
    [pscustomobject]@{ ok = $true; baseUrl = $BaseUrl; screenshotDirectory = $ScreenshotDirectory; results = $results }
} finally {
    if ($startedClient) { Stop-AutomationClient -ProcessId $clientProcessId }
}
