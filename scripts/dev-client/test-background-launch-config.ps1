param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path)

$ErrorActionPreference = 'Stop'
$gradle = Join-Path $WorkspaceRoot 'gradlew.bat'
$argumentsDirectory = Join-Path $WorkspaceRoot 'workspace/build/moddev'
$manualRuns = @('client', 'client2')

function Assert-RunMode {
    param([string]$Run, [string]$Expected)

    $path = Join-Path $argumentsDirectory "${Run}RunVmArgs.txt"
    $values = @(Get-Content -LiteralPath $path | Where-Object { $_ -like '-Ddevclientautomation.backgroundInput=*' })
    if ($values.Count -ne 1 -or $values[0] -ne "-Ddevclientautomation.backgroundInput=$Expected") {
        throw "Wrong generated input mode for ${Run}: $values"
    }
    [pscustomobject]@{ run = $Run; mode = $Expected; arguments = $path; sha256 = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash }
}

# The old property must not contaminate manual IDE argument files, even if an
# existing command still supplies it while preparing background automation.
& $gradle -p $WorkspaceRoot :workspace:prepareClientRun :workspace:prepareClient2Run :workspace:prepareRegressionClientRun -Pdev_client_background_input=true --console=plain
if ($LASTEXITCODE) { throw 'Could not prepare the separate launch configurations' }
$manualBefore = @($manualRuns | ForEach-Object { Assert-RunMode $_ 'false' })
$background = Assert-RunMode 'regressionClient' 'true'

& $gradle -p $WorkspaceRoot :workspace:prepareRegressionClientRun -Pdev_client_background_input=true --console=plain
if ($LASTEXITCODE) { throw 'Could not reprepare the background launch' }
$manualAfter = @($manualRuns | ForEach-Object { Assert-RunMode $_ 'false' })
for ($i = 0; $i -lt $manualRuns.Count; $i++) {
    if ($manualBefore[$i].sha256 -ne $manualAfter[$i].sha256) {
        throw "Background preparation changed manual IDE arguments: $($manualRuns[$i])"
    }
}

$directory = Join-Path $WorkspaceRoot 'build/reports/dev-client-regression/background-input/launch-config'
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$result = [pscustomobject]@{ ok = $true; manualBefore = $manualBefore; background = $background; manualAfter = $manualAfter; manualArgumentsUnchanged = $true }
$result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $directory 'evidence.json')
$result
