param(
    [string]$BaseUrl = "",
    [string]$ScreenshotDirectory = (Join-Path (Resolve-Path "$PSScriptRoot\..\..\workspace\run").Path "item-display-preview-screenshots"),
    [string]$PreviewCropDirectory = "",
    [switch]$StartClient,
    [switch]$MoveMouseToCorner = $true,
    [int]$PreviewSettleMilliseconds = 1250
)

$ErrorActionPreference = "Stop"
$InformationPreference = "Continue"

Add-Type -AssemblyName System.Drawing

function Test-NearBlackPixel {
    param(
        [Parameter(Mandatory = $true)] [System.Drawing.Bitmap]$Bitmap,
        [Parameter(Mandatory = $true)] [int]$X,
        [Parameter(Mandatory = $true)] [int]$Y
    )

    if ($X -lt 0 -or $Y -lt 0 -or $X -ge $Bitmap.Width -or $Y -ge $Bitmap.Height) {
        return $false
    }

    $color = $Bitmap.GetPixel($X, $Y)
    return $color.R -le 12 -and $color.G -le 12 -and $color.B -le 12
}

function Get-PreviewCandidateScore {
    param(
        [Parameter(Mandatory = $true)] [System.Drawing.Bitmap]$Bitmap,
        [Parameter(Mandatory = $true)] [int]$X,
        [Parameter(Mandatory = $true)] [int]$Y,
        [Parameter(Mandatory = $true)] [int]$Size,
        [Parameter(Mandatory = $true)] [int]$Step
    )

    $blackPixels = 0
    $sampledPixels = 0
    for ($offset = 0; $offset -lt $Size; $offset += $Step) {
        foreach ($sample in @(
                @{ x = $X + $offset; y = $Y },
                @{ x = $X + $offset; y = $Y + $Size - 1 },
                @{ x = $X; y = $Y + $offset },
                @{ x = $X + $Size - 1; y = $Y + $offset }
            )) {
            if (Test-NearBlackPixel -Bitmap $Bitmap -X $sample.x -Y $sample.y) {
                $blackPixels++
            }
            $sampledPixels++
        }
    }

    if ($sampledPixels -eq 0) {
        return 0
    }
    return $blackPixels / $sampledPixels
}

function Get-PreviewCropRectangle {
    param(
        [Parameter(Mandatory = $true)] [string]$ScreenshotPath,
        [Parameter(Mandatory = $true)] [object]$State
    )

    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $scaleX = if ($State.guiWidth -gt 0) { $bitmap.Width / [double]$State.guiWidth } else { 1D }
        $scaleY = if ($State.guiHeight -gt 0) { $bitmap.Height / [double]$State.guiHeight } else { $scaleX }
        $scale = [Math]::Min($scaleX, $scaleY)
        $previewSize = [Math]::Max(1, [int][Math]::Round(72 * $scale))
        $step = [Math]::Max(1, [int][Math]::Round($scale))
        $minimumRun = [int][Math]::Round($previewSize * 0.75)
        $maximumRun = [int][Math]::Round($previewSize * 1.8)
        $xSearchStart = [int][Math]::Round($bitmap.Width * 0.45)
        $candidates = @()

        for ($y = 0; $y -le $bitmap.Height - $previewSize; $y += $step) {
            $runStart = 0
            $runLength = 0
            for ($x = $xSearchStart; $x -lt $bitmap.Width; $x += $step) {
                if (Test-NearBlackPixel -Bitmap $bitmap -X $x -Y $y) {
                    if ($runLength -eq 0) {
                        $runStart = $x
                    }
                    $runLength += $step
                    continue
                }

                if ($runLength -ge $minimumRun -and $runLength -le $maximumRun -and $runStart + $previewSize -le $bitmap.Width) {
                    $candidates += [pscustomobject]@{ x = $runStart; y = $y; runLength = $runLength }
                }
                $runLength = 0
            }
        }

        $groups = @()
        foreach ($candidate in ($candidates | Sort-Object x, y)) {
            $group = $groups | Where-Object { [Math]::Abs($_.x - $candidate.x) -le $step -and $candidate.y -le $_.lastY + 2 * $step } | Select-Object -First 1
            if ($null -eq $group) {
                $group = [pscustomobject]@{ x = $candidate.x; firstY = $candidate.y; lastY = $candidate.y; count = 1 }
                $groups += $group
            } else {
                $group.lastY = $candidate.y
                $group.count++
            }
        }

        $bestCandidate = $null
        $bestScore = 0D
        foreach ($group in $groups) {
            if ($group.count -lt 2) {
                continue
            }

            $borderScore = Get-PreviewCandidateScore -Bitmap $bitmap -X $group.x -Y $group.firstY -Size $previewSize -Step $step
            $score = $borderScore + [Math]::Min(0.4D, $group.count / 50D) - ($group.firstY / [double]$bitmap.Height * 0.05D)
            if ($score -gt $bestScore) {
                $bestScore = $score
                $bestCandidate = @{ x = $group.x; y = $group.firstY }
            }
        }

        if ($null -eq $bestCandidate -or $bestScore -lt 0.55) {
            throw "Could not locate the item-display preview square in $ScreenshotPath. Best score: $bestScore"
        }

        $inset = $step
        return [System.Drawing.Rectangle]::new($bestCandidate.x + $inset, $bestCandidate.y + $inset, $previewSize - 2 * $inset, $previewSize - 2 * $inset)
    }
    finally {
        $bitmap.Dispose()
    }
}

function Save-PreviewCrop {
    param(
        [Parameter(Mandatory = $true)] [string]$ScreenshotPath,
        [Parameter(Mandatory = $true)] [string]$PreviewCropPath,
        [Parameter(Mandatory = $true)] [object]$State
    )

    $rectangle = Get-PreviewCropRectangle -ScreenshotPath $ScreenshotPath -State $State
    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $crop = $bitmap.Clone($rectangle, $bitmap.PixelFormat)
        try {
            $crop.Save($PreviewCropPath, [System.Drawing.Imaging.ImageFormat]::Png)
        }
        finally {
            $crop.Dispose()
        }
    }
    finally {
        $bitmap.Dispose()
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
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 30
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec 30
}

function Close-OpenScreen {
    for ($attempt = 0; $attempt -lt 3; $attempt++) {
        $wait = Invoke-BridgeJson -Method Post -Path "/wait" -Body @{ condition = "noScreen"; timeoutMs = 250 }
        if ($wait.ok -and -not $wait.timedOut) {
            return
        }

        Invoke-BridgeJson -Method Post -Path "/key" -Body @{ key = "ESCAPE" } | Out-Null
        Start-Sleep -Milliseconds 150
    }

    $finalWait = Invoke-BridgeJson -Method Post -Path "/wait" -Body @{ condition = "noScreen"; timeoutMs = 2000 }
    if (-not $finalWait.ok -or $finalWait.timedOut) {
        throw "Timed out waiting for the current screen to close before the next capture case."
    }
}

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    if (-not $StartClient) {
        throw "Pass -BaseUrl for an already running dev client, or pass -StartClient to launch one via start-and-ready.ps1."
    }
    $ready = & "$PSScriptRoot\start-and-ready.ps1" -WorkspaceRoot (Resolve-Path "$PSScriptRoot\..\..").Path -Maximize -SkipRecipeViewerReady -MinimalRuntime
    $BaseUrl = $ready.baseUrl
}

New-Item -ItemType Directory -Path $ScreenshotDirectory -Force | Out-Null
if ([string]::IsNullOrWhiteSpace($PreviewCropDirectory)) {
    $PreviewCropDirectory = Join-Path $ScreenshotDirectory "preview-crops"
}
New-Item -ItemType Directory -Path $PreviewCropDirectory -Force | Out-Null

$cases = @(
    @{ scenario = "backpack_item"; displaySide = "front" },
    @{ scenario = "barrel_north"; displaySide = "front" },
    @{ scenario = "barrel_east"; displaySide = "front" },
    @{ scenario = "barrel_up"; displaySide = "front" },
    @{ scenario = "limited_barrel_north"; displaySide = "front" },
    @{ scenario = "limited_barrel_up"; displaySide = "front" },
    @{ scenario = "single_chest_north"; displaySide = "front" },
    @{ scenario = "double_chest_north"; displaySide = "front" },
    @{ scenario = "double_chest_north"; displaySide = "left" },
    @{ scenario = "double_chest_north"; displaySide = "right" },
    @{ scenario = "shulker_north"; displaySide = "front" },
    @{ scenario = "shulker_up"; displaySide = "front" },
    @{ scenario = "moving_minecart_barrel"; displaySide = "front" },
    @{ scenario = "moving_minecart_chest"; displaySide = "front" },
    @{ scenario = "moving_minecart_shulker"; displaySide = "front" },
    @{ scenario = "moving_boat_barrel"; displaySide = "front" },
    @{ scenario = "moving_boat_chest"; displaySide = "front" },
    @{ scenario = "moving_boat_shulker"; displaySide = "front" },
    @{ scenario = "moving_boat_limited_barrel"; displaySide = "front" },
    @{ scenario = "llama_barrel"; displaySide = "front" },
    @{ scenario = "llama_chest"; displaySide = "front" },
    @{ scenario = "create_cart_barrel_north"; displaySide = "front" },
    @{ scenario = "create_cart_birch_barrel_north"; displaySide = "front" },
    @{ scenario = "create_cart_barrel_east"; displaySide = "front" },
    @{ scenario = "create_cart_chest"; displaySide = "front" },
    @{ scenario = "create_cart_double_chest"; displaySide = "front" },
    @{ scenario = "create_cart_shulker"; displaySide = "front" },
    @{ scenario = "create_cart_limited_barrel"; displaySide = "front" },
    @{ scenario = "create_cart_backpack"; displaySide = "front" }
)

$captures = @()
foreach ($case in $cases) {
    $name = "$($case.scenario)-$($case.displaySide)"
    Write-Information "Capturing $name"

    Close-OpenScreen
    $open = Invoke-BridgeJson -Method Post -Path "/storage/item-display-preview/open" -Body $case
    if (-not $open.ok) {
        throw "Failed to open item display preview for $name"
    }
    if ($MoveMouseToCorner) {
        Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ position = "top-left" } | Out-Null
    }
    Start-Sleep -Milliseconds $PreviewSettleMilliseconds

    $pngPath = Join-Path $ScreenshotDirectory "$name.png"
    $previewCropPath = Join-Path $PreviewCropDirectory "$name.png"
    $statePath = Join-Path $ScreenshotDirectory "$name.state.json"
    $screenPath = Join-Path $ScreenshotDirectory "$name.screen.json"
    Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $pngPath | Out-Null
    $state = Invoke-BridgeJson -Method Get -Path "/state"
    $state | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $statePath -Encoding utf8
    Save-PreviewCrop -ScreenshotPath $pngPath -PreviewCropPath $previewCropPath -State $state
    $screen = Invoke-BridgeJson -Method Get -Path "/screen"
    $screen | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $screenPath -Encoding utf8

    $captures += [pscustomobject]@{
        name = $name
        screenshot = $pngPath
        previewCrop = $previewCropPath
        state = $statePath
        screen = $screenPath
        open = $open
    }
}

[pscustomobject]@{
    baseUrl = $BaseUrl
    screenshotDirectory = $ScreenshotDirectory
    previewCropDirectory = $PreviewCropDirectory
    captures = $captures
}
