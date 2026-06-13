param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$BaseUrl = "",
    [string]$Suite = "sophisticatedbackpacks",
    [int]$TimeoutSeconds = 360,
    [switch]$NoStartClient,
    [switch]$MaximizeClient,
    [switch]$MinimalRuntime
)

$ErrorActionPreference = "Stop"

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
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress -Depth 16) -TimeoutSec $TimeoutSeconds
}

function Get-SuitePath {
    param([string]$SuiteName)

    if (Test-Path $SuiteName -PathType Leaf) {
        return (Resolve-Path $SuiteName).Path
    }
    return Join-Path $PSScriptRoot "backpack-suites\$SuiteName.json"
}

function Stop-AutomationClient {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        return
    }
    try {
        Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    } catch {
        Write-Warning "Failed to stop dev client through automation bridge: $($_.Exception.Message)"
    }
}

$startedClient = $false

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
        $startedClient = $true
    }

    $suitePath = Get-SuitePath -SuiteName $Suite
    Assert-True (Test-Path $suitePath) "Backpack regression suite not found: $suitePath"
    $suiteData = Get-Content $suitePath -Raw | ConvertFrom-Json

    $state = Invoke-BridgeJson -Method Get -Path "/state"
    Assert-True $state.playerLoaded "Dev client world is not loaded."

    $results = @()
    foreach ($test in @($suiteData.tests)) {
        $result = Invoke-BridgeJson -Method Post -Path "/backpack/gui-regression/run" -Body $test
        Assert-True $result.ok "Backpack regression failed for '$($test.name)': $($result.error). Result=$($result | ConvertTo-Json -Compress -Depth 16)"
        $results += [pscustomobject]@{ name = $test.name; type = $test.type; context = $test.context; passed = $true; result = $result }
        Write-Host "PASS $($test.name)"
    }

    [pscustomobject]@{
        ok = $true
        suite = $suiteData.name
        baseUrl = $BaseUrl
        passed = $results.Count
        results = $results
    }
} finally {
    if ($startedClient) {
        Stop-AutomationClient
    }
}
