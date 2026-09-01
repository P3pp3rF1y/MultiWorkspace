param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [ValidateSet("neoforge", "fabric")]
    [string]$Loader = "neoforge",
    [string]$BaseUrl = "",
    [int]$TimeoutSeconds = 360,
    [switch]$NoStartClient,
    [switch]$MaximizeClient,
    [switch]$MinimalRuntime,
    [switch]$Jfr,
    [string]$JfrOutputDirectory = "$WorkspaceRoot\DevClientAutomation\build\reports\linked-storage-performance",
    [int]$Endpoints = 24,
    [int]$Ticks = 200,
    [int]$StacksPerTick = 4
)

$ErrorActionPreference = "Stop"

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
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec $TimeoutSeconds
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType "application/json" -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec $TimeoutSeconds
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

function Wait-ForPhase {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        Start-Sleep -Milliseconds 250
        $status = Invoke-BridgeJson -Method Post -Path "/backpack/linked-storage-performance" -Body @{ mode = "status" }
        if (-not $status.running) {
            return $status
        }
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for linked-storage performance phase."
}

function Invoke-PerformancePhase {
    param([string]$Workload)

    $setup = Invoke-BridgeJson -Method Post -Path "/backpack/linked-storage-performance" -Body @{ mode = "setup"; workload = $Workload; endpoints = $Endpoints; ticks = $Ticks; stacksPerTick = $StacksPerTick }
    Assert-True $setup.ok "Linked-storage $Workload setup failed: $($setup | ConvertTo-Json -Compress)"

    $jfrStarted = $false
    $jfrOutputPath = ""
    try {
        if ($Jfr) {
            $jfrOutputPath = Join-Path $JfrOutputDirectory "$Workload-$(Get-Date -Format 'yyyyMMdd-HHmmss').jfr"
            $jfrStart = Invoke-BridgeJson -Method Post -Path "/backpack/linked-storage-performance" -Body @{ mode = "jfrStart"; outputPath = $jfrOutputPath }
            Assert-True $jfrStart.ok "Could not start linked-storage $Workload JFR: $($jfrStart | ConvertTo-Json -Compress)"
            $jfrStarted = $true
            Write-Host "JFR recording started for linked-storage $Workload at $jfrOutputPath."
        }
        $started = Invoke-BridgeJson -Method Post -Path "/backpack/linked-storage-performance" -Body @{ mode = "start" }
        Assert-True $started.ok "Linked-storage $Workload start failed: $($started | ConvertTo-Json -Compress)"
        $result = Wait-ForPhase
        if ($jfrStarted) {
            $jfrStop = Invoke-BridgeJson -Method Post -Path "/backpack/linked-storage-performance" -Body @{ mode = "jfrStop" }
            Assert-True $jfrStop.ok "Could not stop linked-storage $Workload JFR: $($jfrStop | ConvertTo-Json -Compress)"
            $jfrStarted = $false
            Write-Host "JFR recording stopped for linked-storage $Workload at $jfrOutputPath."
        }
        Assert-True $result.ok "Linked-storage $Workload workload failed: $($result | ConvertTo-Json -Compress)"
        Assert-True $result.carrierDescriptorStable "Linked-storage $Workload changed the virtual carrier descriptor."
        Assert-True $result.renderRevisionStable "Linked-storage $Workload changed the render revision."
        Write-Host "PASS linked_storage_$Workload`: ticks=$($result.elapsedTicks), duration=$($result.durationMillis)ms, inserted=$($result.insertedStacks), extracted=$($result.extractedStacks), spawned=$($result.spawnedItems)"
        return $result
    } finally {
        if ($jfrStarted) {
            Invoke-BridgeJson -Method Post -Path "/backpack/linked-storage-performance" -Body @{ mode = "jfrStop" } | Out-Null
            Write-Host "JFR recording stopped for linked-storage $Workload at $jfrOutputPath."
        }
    }
}

$startedClient = $false
try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."
        $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; Loader = $Loader; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
        if ($MaximizeClient) { $readyArgs.Maximize = $true }
        if ($MinimalRuntime) { $readyArgs.MinimalRuntime = $true }
        $ready = & "$PSScriptRoot\start-and-ready.ps1" @readyArgs
        $BaseUrl = $ready.baseUrl
        $startedClient = $true
    }

    $state = Invoke-BridgeJson -Method Get -Path "/state"
    Assert-True $state.playerLoaded "Dev client world is not loaded."
    $magnet = Invoke-PerformancePhase -Workload "magnet"
    $inventory = Invoke-PerformancePhase -Workload "inventory"
    [pscustomobject]@{ ok = $true; baseUrl = $BaseUrl; jfr = [bool]$Jfr; magnet = $magnet; inventory = $inventory }
} finally {
    if ($startedClient) {
        Stop-AutomationClient
    }
}
