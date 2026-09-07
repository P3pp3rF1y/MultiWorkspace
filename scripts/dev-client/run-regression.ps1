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
$manifestPath = Join-Path $PSScriptRoot "regression-suites.json"
$tierRanks = @{ fast = 0; full = 1 }

function Test-SuiteMatches {
    param([object]$Candidate)

    if (@($Candidate.loaders) -notcontains $Loader) {
        return $false
    }
    if ($Suite.Count -gt 0) {
        return $Suite -contains $Candidate.id
    }
    if ($tierRanks[$Candidate.tier] -gt $tierRanks[$Tier]) {
        return $false
    }
    if ($Group.Count -eq 0) {
        return $true
    }
    return @($Candidate.groups | Where-Object { $Group -contains $_ }).Count -gt 0
}

function Wait-PreviousAutomationClientStopped {
    $discoveryPath = Join-Path $WorkspaceRoot "workspace\run\dev-client-automation.json"
    if (-not (Test-Path -LiteralPath $discoveryPath -PathType Leaf)) {
        return
    }

    $discovery = Get-Content -LiteralPath $discoveryPath -Raw | ConvertFrom-Json
    $processId = [int]$discovery.processId
    if ($processId -le 0) {
        return
    }

    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($null -ne $process -and -not $process.WaitForExit($TimeoutSeconds * 1000)) {
        throw "Timed out waiting for prior dev client process $processId to stop."
    }
}

if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "Regression suite manifest not found: $manifestPath"
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
if ($manifest.protocolVersion -ne 1) {
    throw "Unsupported regression suite manifest version '$($manifest.protocolVersion)'."
}

$matchingSuites = @($manifest.suites | Where-Object { Test-SuiteMatches $_ })
if ($List) {
    $matchingSuites | Select-Object id, groups, tier, loaders, script, arguments
    return
}
if ($matchingSuites.Count -eq 0) {
    $requested = if ($Suite.Count -gt 0) { "suite '$($Suite -join ", ")'" } elseif ($Group.Count -gt 0) { "group '$($Group -join ", ")'" } else { "tier '$Tier'" }
    throw "No $requested regression suites are registered for loader '$Loader'."
}

$results = @()
for ($suiteIndex = 0; $suiteIndex -lt $matchingSuites.Count; $suiteIndex++) {
    if ($suiteIndex -gt 0) {
        Wait-PreviousAutomationClientStopped
    }

    $selectedSuite = $matchingSuites[$suiteIndex]
    $scriptPath = Join-Path $PSScriptRoot $selectedSuite.script
    if (-not (Test-Path -LiteralPath $scriptPath -PathType Leaf)) {
        throw "Regression script for '$($selectedSuite.id)' was not found: $scriptPath"
    }

    $scriptArguments = @{ WorkspaceRoot = $WorkspaceRoot; Loader = $Loader; TimeoutSeconds = $TimeoutSeconds }
    if ($selectedSuite.arguments) {
        foreach ($argument in $selectedSuite.arguments.PSObject.Properties) {
            $scriptArguments[$argument.Name] = $argument.Value
        }
    }
    if ($MaximizeClient -and $selectedSuite.acceptsMaximizeClient) {
        $scriptArguments.MaximizeClient = $true
    }

    try {
        $result = & $scriptPath @scriptArguments
        $results += [pscustomobject]@{ id = $selectedSuite.id; passed = $true; result = $result; error = $null }
        Write-Host "PASS $($selectedSuite.id)"
    } catch {
        $results += [pscustomobject]@{ id = $selectedSuite.id; passed = $false; result = $null; error = $_.Exception.Message }
        Write-Warning "FAIL $($selectedSuite.id): $($_.Exception.Message)"
        if (-not $ContinueOnFailure) {
            break
        }
    }
}

$summary = [pscustomobject]@{
    ok = (@($results | Where-Object { -not $_.passed }).Count -eq 0)
    protocolVersion = $manifest.protocolVersion
    loader = $Loader
    tier = $Tier
    groups = $Group
    requestedSuites = $Suite
    passed = @($results | Where-Object passed).Count
    failed = @($results | Where-Object { -not $_.passed }).Count
    results = $results
}

if (-not $summary.ok) {
    throw "Regression run failed: $($summary.failed) of $($results.Count) selected suites failed."
}

$summary
