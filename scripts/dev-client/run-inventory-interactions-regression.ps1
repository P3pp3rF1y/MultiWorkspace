param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path, [ValidateSet("forge")][string]$Loader = "forge", [string]$BaseUrl = "", [string]$WorldName = "Inventory Interactions Regression", [int]$TimeoutSeconds = 360, [switch]$NoStartClient, [switch]$MinimalRuntime)
$ErrorActionPreference = "Stop"
function Assert-True { param([bool]$Condition, [string]$Message) if (-not $Condition) { throw $Message } }
function Invoke-BridgeJson { param([string]$Method, [string]$Path) return Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -TimeoutSec $TimeoutSeconds }
function Stop-AutomationClient { if ([string]::IsNullOrWhiteSpace($BaseUrl)) { return }; try { Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null } catch { Write-Warning "Failed to stop dev client through automation bridge: $($_.Exception.Message)" } }
function Wait-AutomationClientStopped { if (-not $clientProcessId) { throw "Dev client process ID was not returned." }; Wait-Process -Id $clientProcessId -Timeout $TimeoutSeconds -ErrorAction SilentlyContinue; if (Get-Process -Id $clientProcessId -ErrorAction SilentlyContinue) { throw "Timed out waiting for dev client to stop." } }
$startedClient = $false
$clientProcessId = $null
try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."
		$readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; Loader = $Loader; WorldName = $WorldName; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
        if ($MinimalRuntime) { $readyArgs.MinimalRuntime = $true }
        $ready = & "$PSScriptRoot\start-and-ready.ps1" @readyArgs
        $BaseUrl = $ready.baseUrl; $clientProcessId = $ready.processId; $startedClient = $true
    }
    $state = Invoke-BridgeJson -Method Get -Path "/state"
    Assert-True $state.playerLoaded "Dev client world is not loaded."
    $result = Invoke-BridgeJson -Method Post -Path "/inventory-interactions/keybind-regression"
    Assert-True $result.ok "Inventory-interactions regression failed: $($result.error). Result=$($result | ConvertTo-Json -Compress -Depth 16)"
    $result
} finally { if ($startedClient) { Stop-AutomationClient; Wait-AutomationClientStopped } }
