param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path)

$ErrorActionPreference = 'Stop'
$gradle = Join-Path $WorkspaceRoot 'gradlew.bat'

# ForgeGradle does not generate NeoForge's moddev/*RunVmArgs.txt files.
# The Gradle verification task reads the effective named Forge run configurations
# after all projects have been configured and prepares each run independently.
& $gradle -p $WorkspaceRoot :workspace:verifyBackgroundLaunchConfig -Pdev_client_background_input=true --console=plain
if ($LASTEXITCODE) { throw 'Separate Forge launch configurations failed background mode verification' }

& $gradle -p $WorkspaceRoot :workspace:verifyBackgroundLaunchConfig -Pdev_client_background_input=false --console=plain
if ($LASTEXITCODE) { throw 'Obsolete background property contaminated a named Forge run' }

[pscustomobject]@{ ok = $true; manual = @('client', 'client2'); background = 'regressionClient'; obsoletePropertyIgnored = $true }
