param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [ValidateSet("neoforge", "fabric")]
    [string]$Loader = "neoforge",
    [string[]]$Group = @(),
    [string[]]$Suite = @(),
    [ValidateSet("fast", "full")]
    [string]$Tier = "fast",
    [int]$TimeoutSeconds = 360,
    [switch]$MaximizeClient,
    [switch]$ContinueOnFailure,
    [switch]$List
)

$ErrorActionPreference = "Stop"
$manifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot "regression-suites.json") -Raw | ConvertFrom-Json
$ranks = @{ fast = 0; full = 1 }
$selected = @($manifest.suites | Where-Object {
    @($_.loaders) -contains $Loader -and (($Suite.Count -gt 0 -and $Suite -contains $_.id) -or ($Suite.Count -eq 0 -and $ranks[$_.tier] -le $ranks[$Tier] -and ($Group.Count -eq 0 -or @($_.groups | Where-Object { $Group -contains $_ }).Count -gt 0)))
})
if ($List) {
    $selected | Select-Object id, groups, tier, loaders, script, arguments
    return
}
if ($selected.Count -eq 0) {
    throw "No matching regression suites are registered for loader '$Loader'."
}
$results = @()
foreach ($selectedSuite in $selected) {
    $arguments = @{ WorkspaceRoot = $WorkspaceRoot; Loader = $Loader; TimeoutSeconds = $TimeoutSeconds }
    foreach ($argument in $selectedSuite.arguments.PSObject.Properties) { $arguments[$argument.Name] = $argument.Value }
    if ($MaximizeClient -and $selectedSuite.acceptsMaximizeClient) { $arguments.MaximizeClient = $true }
    try {
        $result = & (Join-Path $PSScriptRoot $selectedSuite.script) @arguments
        $results += [pscustomobject]@{ id = $selectedSuite.id; passed = $true; result = $result; error = $null }
        Write-Host "PASS $($selectedSuite.id)"
    } catch {
        $results += [pscustomobject]@{ id = $selectedSuite.id; passed = $false; result = $null; error = $_.Exception.Message }
        Write-Warning "FAIL $($selectedSuite.id): $($_.Exception.Message)"
        if (-not $ContinueOnFailure) { break }
    }
}
$failed = @($results | Where-Object { -not $_.passed })
if ($failed.Count -gt 0) { throw "Regression run failed: $($failed.Count) of $($results.Count) selected suites failed." }
[pscustomobject]@{ ok = $true; loader = $Loader; selected = $selected.Count; passed = $results.Count; results = $results }
