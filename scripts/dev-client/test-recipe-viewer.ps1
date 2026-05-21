param(
    [Parameter(Mandatory = $true)] [string]$BaseUrl,
    [string]$Item = "sophisticatedbackpacks:backpack",
    [int]$Limit = 5,
    [string[]]$Modes = @("recipes", "uses"),
    [string]$ScreenshotDirectory = (Join-Path (Resolve-Path "$PSScriptRoot\..\..\workspace\run").Path "automation-screenshots"),
    [switch]$MoveMouseForScreenshots
)

$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Path $ScreenshotDirectory -Force | Out-Null

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
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec 10
}

$state = Invoke-BridgeJson -Method Get -Path "/recipe-viewer/state"
$query = Invoke-BridgeJson -Method Post -Path "/recipe-viewer/query" -Body @{ item = $Item; limit = $Limit }
$screenshots = @()

foreach ($mode in $Modes) {
    $open = Invoke-BridgeJson -Method Post -Path "/recipe-viewer/open" -Body @{ item = $Item; mode = $mode }
    if ($MoveMouseForScreenshots) {
        Invoke-BridgeJson -Method Post -Path "/mouse/move" -Body @{ position = "top-left" } | Out-Null
    }
    $safeItem = $Item.Replace(":", "-").Replace("/", "-")
    $path = Join-Path $ScreenshotDirectory "$($state.viewer)-$safeItem-$mode.png"
    Invoke-WebRequest -Uri "$BaseUrl/screenshot" -OutFile $path | Out-Null
    $screenshots += [pscustomobject]@{ mode = $mode; path = $path; open = $open }
}

[pscustomobject]@{
    state = $state
    query = $query
    screenshots = $screenshots
}
