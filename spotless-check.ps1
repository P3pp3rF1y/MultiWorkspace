#requires -Version 7.0
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$workspaceRoot = $PSScriptRoot
$gradleWrapper = Join-Path $workspaceRoot "gradlew.bat"

if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "Gradle wrapper not found: $gradleWrapper"
}

Write-Host "Running Spotless check in $(Split-Path -Leaf $workspaceRoot)..." -ForegroundColor Cyan
Push-Location $workspaceRoot
try {
    & $gradleWrapper spotlessCheck
    if ($LASTEXITCODE -ne 0) {
        throw "spotlessCheck failed in $workspaceRoot with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
