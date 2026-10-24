param([string]$WorkspaceRoot, [switch]$MinimalRuntime, [switch]$SkipRecipeViewerReady, [switch]$CloseOnExit, [switch]$KeepClientOpen, [int]$TimeoutSeconds)
$PSDefaultParameterValues['Stop-OwnedClient:TimeoutSeconds'] = 2
$timeout = if ($env:DCA_LIFECYCLE_MODE -eq 'no-bridge') { 3 } else { 20 }
try {
    $ready = & "$env:DCA_LIFECYCLE_SOURCE/start-and-ready.ps1" -WorkspaceRoot $env:DCA_LIFECYCLE_DIRECTORY -TimeoutSeconds $timeout -SkipRecipeViewerReady -CloseOnExit -KeepClientOpen:$KeepClientOpen
    $ready.ownership | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $env:DCA_LIFECYCLE_DIRECTORY 'ownership.json')
    $ready
} catch {
    $_.Exception.Data['ClientOwnership'] | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $env:DCA_LIFECYCLE_DIRECTORY 'ownership.json')
    throw
}
