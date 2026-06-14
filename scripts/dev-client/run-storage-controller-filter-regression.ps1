param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$BaseUrl = "",
    [int]$TimeoutSeconds = 360,
    [switch]$NoStartClient,
    [switch]$MaximizeClient,
    [switch]$MinimalRuntime,
    [switch]$Jfr,
    [int]$Runs = 0
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
$jfrStarted = $false

if ($Runs -le 0) {
    $Runs = if ($Jfr) { 1000 } else { 1 }
}

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

    $state = Invoke-BridgeJson -Method Get -Path "/state"
    Assert-True $state.playerLoaded "Dev client world is not loaded."

    if ($Jfr) {
        $setupResult = Invoke-BridgeJson -Method Post -Path "/storage/controller-filter-regressions" -Body @{ mode = "setup"; profileCapacity = $true }
        Assert-True $setupResult.ok "Storage controller filter setup failed: $($setupResult | ConvertTo-Json -Compress -Depth 16)"
        Write-Host "PASS storage_controller_filter_setup: connected=$($setupResult.connectedStorages), setup=$($setupResult.setupMillis)ms"

        Invoke-BridgeJson -Method Post -Path "/command" -Body @{ command = "jfr start" } | Out-Null
        $jfrStarted = $true
        Write-Host "JFR recording start command dispatched."

        $profileResult = Invoke-BridgeJson -Method Post -Path "/storage/controller-filter-regressions" -Body @{ mode = "profile"; runs = $Runs }
        Assert-True $profileResult.ok "Storage controller filter profile failed: $($profileResult | ConvertTo-Json -Compress -Depth 16)"

        Invoke-BridgeJson -Method Post -Path "/command" -Body @{ command = "jfr stop" } | Out-Null
        $jfrStarted = $false
        Write-Host "JFR recording stop command dispatched. Check the dev client log for the recording path."

        $verifyResult = Invoke-BridgeJson -Method Post -Path "/storage/controller-filter-regressions" -Body @{}
        Assert-True $verifyResult.ok "Storage controller filter verification failed: $($verifyResult | ConvertTo-Json -Compress -Depth 16)"
        Write-Host "PASS storage_controller_filter_profile: $($profileResult.insertCalls) inserts, $($profileResult.itemsInserted) accepted items, insert=$($profileResult.insertMillis)ms"
        Write-Host "PASS storage_controller_filter_verify: $($verifyResult.insertCalls) inserts, $($verifyResult.itemsInserted) items, setup=$($verifyResult.setupMillis)ms, insert=$($verifyResult.insertMillis)ms, verify=$($verifyResult.verifyMillis)ms"

        [pscustomobject]@{
            ok = $true
            baseUrl = $BaseUrl
            jfr = $true
            runs = $Runs
            setup = $setupResult
            profile = $profileResult
            verify = $verifyResult
        }
        return
    }

    $result = Invoke-BridgeJson -Method Post -Path "/storage/controller-filter-regressions" -Body @{}
    Assert-True $result.ok "Storage controller filter regression failed: $($result | ConvertTo-Json -Compress -Depth 16)"
    Write-Host "PASS storage_controller_filter_routing: $($result.insertCalls) inserts, $($result.itemsInserted) items, setup=$($result.setupMillis)ms, insert=$($result.insertMillis)ms, verify=$($result.verifyMillis)ms"

    [pscustomobject]@{
        ok = $true
        baseUrl = $BaseUrl
        jfr = $false
        result = $result
    }
} finally {
    if ($jfrStarted) {
        try {
            Invoke-BridgeJson -Method Post -Path "/command" -Body @{ command = "jfr stop" } | Out-Null
            Write-Host "JFR recording stop command dispatched. Check the dev client log for the recording path."
        } catch {
            Write-Warning "Failed to stop JFR recording through automation bridge: $($_.Exception.Message)"
            throw
        }
    }
    if ($startedClient) {
        Stop-AutomationClient
    }
}
