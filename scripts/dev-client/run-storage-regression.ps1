param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [ValidateSet("neoforge", "fabric")]
    [string]$Loader = "neoforge",
    [string]$BaseUrl = "",
    [string]$Suite = "sophisticatedstorage-linked-storage",
    [int]$TimeoutSeconds = 360,
    [switch]$NoStartClient,
    [switch]$MaximizeClient,
    [switch]$MinimalRuntime
)

$ErrorActionPreference = "Stop"

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Invoke-BridgeJson {
    param([string]$Method, [string]$Path, [object]$Body = $null)
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -TimeoutSec $TimeoutSeconds
    }
    return Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress -Depth 16) -TimeoutSec $TimeoutSeconds
}

function Start-AutomationClient {
    $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; Loader = $Loader; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
    if ($MaximizeClient) { $readyArgs.Maximize = $true }
    if ($MinimalRuntime) { $readyArgs.MinimalRuntime = $true }
    $ready = & "$PSScriptRoot\start-and-ready.ps1" @readyArgs
    $script:BaseUrl = $ready.baseUrl
    $script:clientProcessId = $ready.processId
    $script:startedClient = $true
}

function Stop-AutomationClient {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) { return }
    try { Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null } catch { Write-Warning "Failed to stop dev client: $($_.Exception.Message)" }
}

function Wait-AutomationClientStopped {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        Start-Sleep -Milliseconds 500
        if ($clientProcessId -and -not (Get-Process -Id $clientProcessId -ErrorAction SilentlyContinue)) { return }
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for dev client to stop."
}

function Run-LinkedLimitedReloadProjectionRegression {
    Assert-True $startedClient "The linked limited-barrel reload regression must own the dev client."
    $setup = Invoke-BridgeJson -Method Post -Path "/storage/linked-limited-reload/setup"
    Assert-True $setup.ok "Failed to set up linked limited-barrel reload regression: $($setup | ConvertTo-Json -Compress)"
    $shutdown = Invoke-BridgeJson -Method Post -Path "/client/shutdown-world"
    Assert-True $shutdown.ok "Integrated server did not shut down cleanly."
    Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    Wait-AutomationClientStopped
    Start-AutomationClient
    $status = Invoke-BridgeJson -Method Post -Path "/storage/linked-limited-reload/status" -Body @{ groupId = $setup.groupId }
    Assert-True $status.ok "Linked limited-barrel projection did not survive restart: $($status | ConvertTo-Json -Compress)"
    Assert-True ($status.clientDisplayItems -and $status.clientCounts -and $status.clientFillLevels) "Reloaded linked limited barrels did not project all render state to the client."
    return $status
}

$startedClient = $false
$clientProcessId = $null

try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."
        Start-AutomationClient
    }
    $suitePath = if (Test-Path $Suite -PathType Leaf) { (Resolve-Path $Suite).Path } else { Join-Path $PSScriptRoot "storage-suites\$Suite.json" }
    Assert-True (Test-Path $suitePath) "Storage regression suite not found: $suitePath"
    $suiteData = Get-Content $suitePath -Raw | ConvertFrom-Json
    Assert-True (Invoke-BridgeJson -Method Get -Path "/state").playerLoaded "Dev client world is not loaded."
    $results = @()
    foreach ($test in @($suiteData.tests)) {
        $result = switch ($test.type) {
            "storageLinkedStorageRegressionSuite" { Invoke-BridgeJson -Method Post -Path "/storage/linked-storage-regression" }
            "linkedLimitedReloadProjection" { Run-LinkedLimitedReloadProjectionRegression }
            default { throw "Unsupported storage regression suite type '$($test.type)'." }
        }
        Assert-True $result.ok "Storage regression failed for '$($test.name)': $($result | ConvertTo-Json -Compress -Depth 16)"
        $results += [pscustomobject]@{ name = $test.name; type = $test.type; passed = $true; result = $result }
        Write-Host "PASS $($test.name)"
    }
    [pscustomobject]@{ ok = $true; suite = $suiteData.name; baseUrl = $BaseUrl; passed = $results.Count; results = $results }
} finally {
    if ($startedClient) {
        Stop-AutomationClient
        Wait-AutomationClientStopped
    }
}
