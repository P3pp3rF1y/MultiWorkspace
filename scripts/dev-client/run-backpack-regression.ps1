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

function Stop-ProcessTree {
    param([int]$ProcessId)

    if ($ProcessId -le 0 -or $null -eq (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        return
    }
    try {
        & taskkill.exe /PID $ProcessId /T /F | Out-Null
    } catch {
        Write-Warning "Failed to kill dev client process tree ${ProcessId}: $($_.Exception.Message)"
    }
}

function Wait-ThenStopProcessTree {
    param([int]$ProcessId)

    if ($ProcessId -le 0) {
        return
    }

    $deadline = (Get-Date).AddSeconds(10)
    while ((Get-Date) -lt $deadline -and $null -ne (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        Start-Sleep -Milliseconds 250
    }
    Stop-ProcessTree -ProcessId $ProcessId
}

function Stop-AutomationClient {
    param([int]$ProcessId = 0)

    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Wait-ThenStopProcessTree -ProcessId $ProcessId
        return
    }
    try {
        Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    } catch {
        Write-Warning "Failed to stop dev client through automation bridge: $($_.Exception.Message)"
    }
    Wait-ThenStopProcessTree -ProcessId $ProcessId
}

function Start-AutomationClient {
    $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; TimeoutSeconds = $TimeoutSeconds; CloseOnExit = $true; SkipRecipeViewerReady = $true }
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
    throw "Timed out waiting for dev client to stop."
}

function Run-InceptionMagnetPersistenceRegression {
    Assert-True $startedClient "The inception magnet persistence regression must start and own the dev client."

    $setup = Invoke-BridgeJson -Method Post -Path "/backpack/inception-magnet-persistence/setup"
    Assert-True $setup.ok "Failed to set up inception magnet persistence regression: $($setup | ConvertTo-Json -Compress)"
    Assert-True (-not $setup.nestedHasUuid) "Nested backpack must start without a UUID."

    $screen = Invoke-BridgeJson -Method Post -Path "/wait" -Body @{ condition = "screen"; screen = "BackpackScreen"; timeoutMs = 30000 }
    Assert-True $screen.ok "Backpack GUI did not open before magnet pickup."

    $pickup = Invoke-BridgeJson -Method Post -Path "/backpack/inception-magnet-persistence/pickup"
    Assert-True $pickup.ok "Magnet pickup did not enter the nested backpack: $($pickup | ConvertTo-Json -Compress)"

    $closed = Invoke-BridgeJson -Method Post -Path "/wait" -Body @{ condition = "noScreen"; timeoutMs = 10000 }
    Assert-True $closed.ok "Backpack GUI did not close before client restart."

    $afterClose = Invoke-BridgeJson -Method Get -Path "/backpack/inception-magnet-persistence/status"
    Assert-True $afterClose.ok "Nested backpack did not retain the magnet pickup after closing its GUI: $($afterClose | ConvertTo-Json -Compress)"
    Assert-True ($afterClose.nestedUuid -eq $pickup.nestedUuid) "Nested backpack UUID changed after closing its GUI. Expected=$($pickup.nestedUuid), actual=$($afterClose.nestedUuid)"

    $shutdown = Invoke-BridgeJson -Method Post -Path "/client/shutdown-world" -Body @{}
    Assert-True $shutdown.ok "Integrated server did not shut down cleanly."
    Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    Wait-AutomationClientStopped
    Start-AutomationClient

    $status = Invoke-BridgeJson -Method Get -Path "/backpack/inception-magnet-persistence/status"
    Assert-True $status.ok "Nested backpack did not retain magnet pickup after restart: $($status | ConvertTo-Json -Compress)"
    return $status
}

$startedClient = $false
$clientProcessId = 0

try {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        Assert-True (-not $NoStartClient) "BaseUrl is required when NoStartClient is set."
        Start-AutomationClient
    }

    $suitePath = Get-SuitePath -SuiteName $Suite
    Assert-True (Test-Path $suitePath) "Backpack regression suite not found: $suitePath"
    $suiteData = Get-Content $suitePath -Raw | ConvertFrom-Json

    $state = Invoke-BridgeJson -Method Get -Path "/state"
    Assert-True $state.playerLoaded "Dev client world is not loaded."

    $results = @()
    foreach ($test in @($suiteData.tests)) {
        if ($test.type -eq "inceptionMagnetPersistence") {
            $result = Run-InceptionMagnetPersistenceRegression
        } else {
            $result = Invoke-BridgeJson -Method Post -Path "/backpack/gui-regression/run" -Body $test
        }
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
        Stop-AutomationClient -ProcessId $clientProcessId
    }
}
