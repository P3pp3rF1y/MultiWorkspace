param(
    [string] $ProjectDir = "",
    [string] $OnlyDirectory = "",
    [string] $DiffStart = "",
    [switch] $PrintProblems,
    [switch] $FailOnProblems,
    [string[]] $ExtraArgs = @()
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$scanProjectDir = $repoRoot
if ($ProjectDir -ne "") {
    if ([System.IO.Path]::IsPathRooted($ProjectDir)) {
        $scanProjectDir = $ProjectDir
    } else {
        $scanProjectDir = Join-Path $repoRoot $ProjectDir
    }
}

$cacheDir = Join-Path $repoRoot "build\reports\qodana\cache"
$reportName = if ($ProjectDir -ne "") { ($ProjectDir -replace '[\\/:]', '_') } else { "root" }
$resultsDir = Join-Path $repoRoot "build\reports\qodana\$reportName\results"
$reportDir = Join-Path $repoRoot "build\reports\qodana\$reportName\html"
$configFileName = "qodana.yaml"
$temporaryConfigPath = $null

if ($scanProjectDir -ne $repoRoot) {
    $configFileName = ".qodana-wrapper.yaml"
    $temporaryConfigPath = Join-Path $scanProjectDir $configFileName
    Copy-Item -Path (Join-Path $repoRoot "qodana.yaml") -Destination $temporaryConfigPath -Force
}

New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

$qodanaArgs = @(
    "scan",
    "--image", "jetbrains/qodana-jvm-community:2025.2",
    "--config", $configFileName,
    "--profile-name", "qodana.recommended",
    "--project-dir", $scanProjectDir,
    "--cache-dir", $cacheDir,
    "--results-dir", $resultsDir,
    "--report-dir", $reportDir,
    "--property", "project.open.type=Gradle",
    "--save-report"
)

if ($OnlyDirectory -ne "") {
    $qodanaArgs += @("--source-directory", $OnlyDirectory)
}

if ($DiffStart -ne "") {
    $qodanaArgs += @("--diff-start", $DiffStart)
}

if ($PrintProblems) {
    $qodanaArgs += "--print-problems"
}

if ($FailOnProblems) {
    $qodanaArgs += @("--fail-threshold", "1")
}

$qodanaArgs += $ExtraArgs

Push-Location $repoRoot
try {
    & qodana @qodanaArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    if ($temporaryConfigPath -ne $null -and (Test-Path $temporaryConfigPath)) {
        Remove-Item $temporaryConfigPath -Force
    }
    Pop-Location
}

Write-Host "Qodana results: $resultsDir"
Write-Host "Qodana HTML report: $reportDir"
