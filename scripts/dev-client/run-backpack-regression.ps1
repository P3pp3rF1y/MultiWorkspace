param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [ValidateSet("neoforge", "fabric")]
    [string]$Loader = "neoforge",
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
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec $TimeoutSeconds
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
    throw "Timed out waiting for dev client to stop."
}

function Run-InceptionMagnetPersistenceRegression {
    Assert-True $startedClient "The inception magnet persistence regression must start and own the dev client."

    $setup = Invoke-BridgeJson -Method Post -Path "/backpack/inception-magnet-persistence/setup"
    Assert-True $setup.ok "Failed to set up inception magnet persistence regression: $($setup | ConvertTo-Json -Compress)"
    Assert-True (-not $setup.nestedHasUuid) "Nested backpack must start without a UUID."

    $screen = Invoke-BridgeJson -Method Post -Path "/wait" -Body @{ condition = "screen"; screen = "BackpackScreen"; timeoutMs = 30000 }
    if (-not $screen.ok) {
        Invoke-BridgeJson -Method Post -Path "/backpack/open-main" | Out-Null
        $screen = Invoke-BridgeJson -Method Post -Path "/wait" -Body @{ condition = "screen"; screen = "BackpackScreen"; timeoutMs = 10000 }
    }
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

function Run-LinkedStorageReloadPersistenceRegression {
    Assert-True $startedClient "The linked storage reload persistence regression must start and own the dev client."

    $setup = Invoke-BridgeJson -Method Post -Path "/backpack/linked-storage-reload/setup"
    Assert-True $setup.ok "Failed to set up linked storage reload persistence regression: $($setup | ConvertTo-Json -Compress)"
    Assert-True $setup.carriedEndpoint "Carried Backpack endpoint was not created."
    Assert-True $setup.placedEndpoint "Placed BackpackBlockEntity endpoint was not created."
    Assert-True $setup.sharedGroup "New Backpack endpoints did not share a linked storage group."
    Assert-True ($setup.carriedNetherStars -eq 7 -and $setup.placedNetherStars -eq 7) "New endpoints did not resolve the canonical Nether Star contents."

    $shutdown = Invoke-BridgeJson -Method Post -Path "/client/shutdown-world" -Body @{}
    Assert-True $shutdown.ok "Integrated server did not shut down cleanly."
    Invoke-BridgeJson -Method Post -Path "/client/stop" | Out-Null
    Wait-AutomationClientStopped
    Start-AutomationClient

    $status = Invoke-BridgeJson -Method Get -Path "/backpack/linked-storage-reload/status"
    Assert-True $status.ok "Linked Backpack endpoints did not retain canonical contents after restart: $($status | ConvertTo-Json -Compress)"
    Assert-True ($status.groupId -eq $setup.groupId) "Linked storage group changed after restart. Expected=$($setup.groupId), actual=$($status.groupId)"
    Assert-True ($status.carriedEndpointId -eq $setup.carriedEndpointId) "Carried endpoint identity changed after restart."
    Assert-True ($status.placedEndpointId -eq $setup.placedEndpointId) "Placed endpoint identity changed after restart."
    Assert-True ($status.carriedNetherStars -eq 7 -and $status.placedNetherStars -eq 7) "Endpoints did not resolve the canonical Nether Star contents after restart."
    return $status
}

$startedClient = $false

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

    # Player data is available before the receiving-level screen finishes opening menus.
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ($state.screenSimpleName -eq "ReceivingLevelScreen") {
        Assert-True ((Get-Date) -lt $deadline) "Timed out waiting for the dev client world to finish loading."
        Start-Sleep -Milliseconds 100
        $state = Invoke-BridgeJson -Method Get -Path "/state"
    }

    $results = @()
    foreach ($test in @($suiteData.tests)) {
        switch ($test.type) {
            "inceptionMagnetPersistence" {
                $result = Run-InceptionMagnetPersistenceRegression
            }
            "linkedStorageReloadPersistence" {
                $result = Run-LinkedStorageReloadPersistenceRegression
            }
            "storageGuiRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/storage-gui-regressions"
            }
            "columnUpgradeRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/column-upgrade-regressions"
            }
            "lifecycleRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/lifecycle-regression" -Body $test
            }
            "linkedStorageRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/linked-storage-regression"
            }
            "linkedStorageCarrierProjectionRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/linked-storage-carrier-projection-regression"
            }
            "accessRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/access-regression"
            }
            "curiosAccessRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/curios-access-regression"
            }
            "magnetRegressionSuite" {
                $setup = Invoke-BridgeJson -Method Post -Path "/backpack/magnet-regression/setup" -Body $test
                Assert-True $setup.ok "Magnet regression setup failed for '$($test.name)': $($setup | ConvertTo-Json -Compress)"
                Start-Sleep -Seconds 1
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/magnet-regression/status" -Body $test
            }
            "pickupRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/pickup-regression" -Body $test
            }
            "filterRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/filter-regression" -Body $test
            }
            "restockRegressionSuite" {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/restock-regression" -Body $test
            }
            "refillRegressionSuite" {
                $setup = Invoke-BridgeJson -Method Post -Path "/backpack/refill-regression/setup" -Body $test
                Assert-True $setup.ok "Refill regression setup failed for '$($test.name)': $($setup | ConvertTo-Json -Compress)"
                Start-Sleep -Seconds 1
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/refill-regression/status" -Body $test
            }
            default {
                $result = Invoke-BridgeJson -Method Post -Path "/backpack/gui-regression/run" -Body $test
            }
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
        Stop-AutomationClient
    }
}
