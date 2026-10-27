param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [switch]$Live,
    [int]$TimeoutSeconds = 360
)
$ErrorActionPreference = 'Stop'

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

$suitePath = Join-Path $PSScriptRoot 'backpack-suites/sophisticatedbackpacks-mounted-linked-storage.json'
$suite = Get-Content $suitePath -Raw | ConvertFrom-Json
$names = @('mounted_regular_backpack_syncs_menu_columns_after_tank_changes',
    'mounted_linked_backpack_links_in_place_and_syncs_menu_columns_and_remote_render_data',
    'mounted_linked_backpack_restores_colors_and_display_after_world_reload')
$types = @('mountedStorageRegression', 'mountedLinkedStorageRegression', 'mountedLinkedStorageReload')
Assert-True (($suite.tests.name -join ',') -ceq ($names -join ',')) 'Mounted scenario selection/order changed.'
Assert-True (($suite.tests.type -join ',') -ceq ($types -join ',')) 'Mounted scenario dispatch types changed.'

$clientSource = Get-Content (Join-Path $WorkspaceRoot 'DevClientAutomation/src/main/java/net/p3pp3rf1y/devclientautomation/DevClientAutomationClient.java') -Raw
$handlers = @{
    '/backpack/mounted-storage-regression' = 'handleOrdinary'
    '/backpack/mounted-linked-storage-regression' = 'handle'
    '/backpack/mounted-linked-storage-reload/setup' = 'setupReload'
    '/backpack/mounted-linked-storage-reload/status' = 'reloadStatus'
}
foreach ($entry in $handlers.GetEnumerator()) {
    $registration = 'createContext("' + $entry.Key + '", MountedLinkedBackpackRegression::' + $entry.Value + ')'
    Assert-True $clientSource.Contains($registration) "Missing mounted endpoint registration: $registration"
}

if ($Live) {
    $root = Join-Path $WorkspaceRoot "build/reports/dev-client-regression/mounted-live/$([guid]::NewGuid())"
    New-Item -ItemType Directory -Path $root -Force | Out-Null
    Copy-Item (Join-Path $PSScriptRoot 'run-backpack-regression.ps1') (Join-Path $root 'run-backpack-regression.ps1')
    Copy-Item (Join-Path $PSScriptRoot 'assert-background-input.ps1') (Join-Path $root 'assert-background-input.ps1')
    $hadLive = Test-Path variable:global:mountedLive
    $oldLive = $global:mountedLive
    $global:mountedLive = @{ launcher = Join-Path $PSScriptRoot 'start-and-ready.ps1'; launches = [System.Collections.Generic.List[object]]::new(); directory = '' }
    @'
param($WorkspaceRoot, $Loader, $TimeoutSeconds, [switch]$CloseOnExit, [switch]$SkipRecipeViewerReady, [switch]$MinimalRuntime)
$ready = & $global:mountedLive.launcher @PSBoundParameters
$global:mountedLive.launches.Add($ready)
$ready | ConvertTo-Json -Depth 32 | Set-Content (Join-Path $global:mountedLive.directory "launch-$($global:mountedLive.launches.Count).json")
$ready
'@ | Set-Content (Join-Path $root 'start-and-ready.ps1')
    $evidence = @()
    try {
        foreach ($test in $suite.tests) {
            $directory = Join-Path $root $test.type
            New-Item -ItemType Directory -Path $directory | Out-Null
            $global:mountedLive.directory = $directory
            $global:mountedLive.launches.Clear()
            $selectedSuite = Join-Path $directory 'suite.json'
            @{ name = $suite.name; tests = @($test) } | ConvertTo-Json -Depth 16 | Set-Content $selectedSuite
            $records = [System.Collections.Generic.List[object]]::new()
            $errorMessage = $null
            try {
                & (Join-Path $root 'run-backpack-regression.ps1') -WorkspaceRoot $WorkspaceRoot -Suite $selectedSuite -MinimalRuntime -TimeoutSeconds $TimeoutSeconds |
                    ForEach-Object { $records.Add($_) }
            } catch { $errorMessage = $_.Exception.Message }
            $stopped = @($global:mountedLive.launches | ForEach-Object {
                @{ processId = $_.processId; stopped = -not [bool](Get-Process -Id $_.processId -ErrorAction SilentlyContinue) }
            })
            $ok = $null -eq $errorMessage -and $records.Count -eq 1 -and $records[0].ok -and $records[0].passed -eq 1
            if ($ok) {
                $nativeCases = @($records[0].results)
                $ok = $records[0].suite -ceq $suite.name -and $nativeCases.Count -eq 1 -and
                    $nativeCases[0].name -ceq $test.name -and $nativeCases[0].type -ceq $test.type -and
                    $nativeCases[0].passed -and $nativeCases[0].result.ok
            }
            if ($test.type -eq 'mountedLinkedStorageReload' -and $ok) {
                $ok = $global:mountedLive.launches.Count -eq 2 -and
                    $global:mountedLive.launches[0].processId -ne $global:mountedLive.launches[1].processId
            }
            if (@($stopped | Where-Object { -not $_.stopped }).Count -gt 0) { $ok = $false }
            if ($records.Count -eq 1) { $records[0] | ConvertTo-Json -Depth 32 | Set-Content (Join-Path $directory 'native-result.json') }
            $case = @{ name = $test.name; type = $test.type; ok = $ok; error = $errorMessage; launches = $global:mountedLive.launches.Count; cleanup = $stopped }
            $case | ConvertTo-Json -Depth 16 | Set-Content (Join-Path $directory 'evidence.json')
            $evidence += $case
        }
    } finally {
        if ($hadLive) { $global:mountedLive = $oldLive } else { Remove-Variable mountedLive -Scope Global }
    }
    $ok = @($evidence | Where-Object { -not $_.ok }).Count -eq 0
    [pscustomobject]@{ ok = $ok; cases = $evidence; artifacts = $root }
    if (-not $ok) { throw "Mounted live verification failed; retained evidence: $root" }
    return
}

$root = Join-Path $WorkspaceRoot "build/reports/dev-client-regression/mounted-contract/$([guid]::NewGuid())"
New-Item -ItemType Directory -Path $root -Force | Out-Null
Copy-Item (Join-Path $PSScriptRoot 'run-backpack-regression.ps1') (Join-Path $root 'run-backpack-regression.ps1')
@'
param($WorkspaceRoot, $Loader, $TimeoutSeconds, [switch]$CloseOnExit, [switch]$SkipRecipeViewerReady, [switch]$MinimalRuntime)
$global:mountedCalls.Add('start')
[pscustomobject]@{ baseUrl = 'http://mounted-fixture'; processId = [int]::MaxValue }
'@ | Set-Content (Join-Path $root 'start-and-ready.ps1')
'' | Set-Content (Join-Path $root 'assert-background-input.ps1')

function Invoke-RestMethod {
    param($Method, $Uri, $TimeoutSec, $ContentType, $Body)
    $path = ([uri]$Uri).AbsolutePath
    $global:mountedCalls.Add($path)
    switch ($path) {
        '/state' { return @{ playerLoaded = $true } }
        '/backpack/mounted-storage-regression' { return @{ ok = $global:mountedMode -ne 'ordinary-failure'; mountedMenuColumns = $true } }
        '/backpack/mounted-linked-storage-regression' { return @{ ok = $true; mountedMenuColumns = $true; remoteColumns = $true; remoteDisplayProjection = $true } }
        '/backpack/mounted-linked-storage-reload/setup' { return @{ ok = $true; groupId = 'group-proof'; mainColor = 17; accentColor = 23 } }
        '/backpack/mounted-linked-storage-reload/status' {
            Assert-True (($Body | ConvertFrom-Json).groupId -eq 'group-proof') 'Reload group was not forwarded.'
            return @{ ok = $true; groupId = $(if ($global:mountedMode -eq 'wrong-group') { 'wrong' } else { 'group-proof' });
                mainColor = $(if ($global:mountedMode -eq 'wrong-color') { 99 } else { 17 }); accentColor = 23;
                hasDisplayItem = $global:mountedMode -ne 'missing-display' }
        }
        '/client/shutdown-world' { return @{ ok = $true } }
        '/client/stop' { return @{ ok = $true } }
        default { throw "Unexpected mounted dispatch: $path" }
    }
}

$hadCalls = Test-Path variable:global:mountedCalls
$oldCalls = $global:mountedCalls
$oldMode = $global:mountedMode
try {
    foreach ($mode in @('success', 'ordinary-failure', 'wrong-group', 'wrong-color', 'missing-display', 'borrowed')) {
        $global:mountedMode = $mode
        $global:mountedCalls = [System.Collections.Generic.List[string]]::new()
        $arguments = @{ WorkspaceRoot = $WorkspaceRoot; Suite = $suitePath; TimeoutSeconds = 1 }
        if ($mode -eq 'borrowed') { $arguments.BaseUrl = 'http://mounted-fixture'; $arguments.NoStartClient = $true }
        $failed = $false
        $records = @()
        try { $records = @(& (Join-Path $root 'run-backpack-regression.ps1') @arguments) } catch { $failed = $true }
        Assert-True ($failed -eq ($mode -ne 'success')) "Unexpected mounted outcome for $mode"
        if ($mode -eq 'success') {
            Assert-True ($records.Count -eq 1 -and $records[0].ok -and $records[0].passed -eq 3) 'Expected exactly three mounted passes.'
            Assert-True (($records[0].results.type -join ',') -ceq ($types -join ',')) 'Runner did not execute the authored mounted types.'
            $expected = @('start', '/state', '/backpack/mounted-storage-regression', '/backpack/mounted-linked-storage-regression',
                '/backpack/mounted-linked-storage-reload/setup', '/client/shutdown-world', '/client/stop', 'start',
                '/backpack/mounted-linked-storage-reload/status', '/client/stop')
            Assert-True (($global:mountedCalls -join ',') -ceq ($expected -join ',')) 'Mounted restart order or dispatch changed.'
        } elseif ($mode -eq 'borrowed') {
            Assert-True (-not $global:mountedCalls.Contains('/client/stop') -and -not $global:mountedCalls.Contains('start') -and
                -not $global:mountedCalls.Contains('/backpack/mounted-linked-storage-reload/setup')) 'Borrowed client was restarted or stopped.'
        } else {
            Assert-True ($global:mountedCalls[$global:mountedCalls.Count - 1] -eq '/client/stop') 'Owned client cleanup was skipped after failure.'
        }
        @{ mode = $mode; failed = $failed; calls = @($global:mountedCalls) } | ConvertTo-Json -Depth 8 |
            Set-Content (Join-Path $root "$mode.json")
    }
} finally {
    if ($hadCalls) { $global:mountedCalls = $oldCalls } else { Remove-Variable mountedCalls -Scope Global }
    $global:mountedMode = $oldMode
}
[pscustomobject]@{ ok = $true; scenarios = 6; artifacts = $root }
