param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [ValidateSet("forge")]
    [string]$Loader = "forge",
    [string]$BaseUrl = "",
    [string]$Suite = "sophisticatedstorage-linked-storage",
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

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -TimeoutSec $TimeoutSeconds
    }
    return Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress -Depth 16) -TimeoutSec $TimeoutSeconds
}

function Get-SuitePath {
    param([string]$SuiteName)

    if (Test-Path $SuiteName -PathType Leaf) {
        return (Resolve-Path $SuiteName).Path
    }
    return Join-Path $PSScriptRoot "storage-suites\$SuiteName.json"
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

function Start-AutomationClient {
    $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; Loader = $Loader; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
    if ($MaximizeClient) {
        $readyArgs.Maximize = $true
    }
    if ($MinimalRuntime) {
        $readyArgs.MinimalRuntime = $true
    }
    $ready = & "$PSScriptRoot\start-and-ready.ps1" @readyArgs
    $script:BaseUrl = $ready.baseUrl
    $script:clientProcessId = $ready.processId
    $script:startedClient = $true
}

function Wait-AutomationClientStopped {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        Start-Sleep -Milliseconds 500
        if ($clientProcessId -and -not (Get-Process -Id $clientProcessId -ErrorAction SilentlyContinue)) {
            return
        }
    } while ((Get-Date) -lt $deadline)

    # The bridge accepted /client/stop; terminate only the launcher owned by this runner if FML leaves the JVM alive.
    Stop-Process -Id $clientProcessId -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
    if (-not (Get-Process -Id $clientProcessId -ErrorAction SilentlyContinue)) {
        return
    }
    throw "Timed out waiting for dev client to stop."
}

function Run-LinkedLimitedReloadProjectionRegression {
    Assert-True $startedClient "The linked limited-barrel reload regression must start and own the dev client."
    $setup = Invoke-BridgeJson -Method Post -Path "/storage/linked-limited-reload/setup"
    Assert-True $setup.ok "Failed to set up linked limited-barrel reload regression: $($setup | ConvertTo-Json -Compress)"
    $shutdown = Invoke-BridgeJson -Method Post -Path "/client/shutdown-world"
    Assert-True $shutdown.ok "Integrated server did not shut down cleanly."
    Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    Wait-AutomationClientStopped
    Start-AutomationClient
    $status = Invoke-BridgeJson -Method Post -Path "/storage/linked-limited-reload/status" -Body @{
        groupId = $setup.groupId
        primaryX = $setup.primaryX
        primaryY = $setup.primaryY
        primaryZ = $setup.primaryZ
    }
    Assert-True $status.ok "Linked limited-barrel render projection did not survive restart: $($status | ConvertTo-Json -Compress)"
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

    $suitePath = Get-SuitePath -SuiteName $Suite
    Assert-True (Test-Path $suitePath) "Storage regression suite not found: $suitePath"
    $suiteData = Get-Content $suitePath -Raw | ConvertFrom-Json

    $state = Invoke-BridgeJson -Method Get -Path "/state"
    Assert-True $state.playerLoaded "Dev client world is not loaded."

    $results = @()
    foreach ($test in @($suiteData.tests)) {
        switch ($test.type) {
            "storageLinkedStorageRegressionSuite" { $result = Invoke-BridgeJson -Method Post -Path "/storage/linked-storage-regression" }
            "linkedLimitedReloadProjection" { $result = Run-LinkedLimitedReloadProjectionRegression }
            default { throw "Unsupported storage regression suite type '$($test.type)'." }
        }
        Assert-True $result.ok "Storage regression failed for '$($test.name)': $($result.error). Result=$($result | ConvertTo-Json -Compress -Depth 16)"
        $results += [pscustomobject]@{ name = $test.name; type = $test.type; passed = $true; result = $result }
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
        Wait-AutomationClientStopped
    }
}
