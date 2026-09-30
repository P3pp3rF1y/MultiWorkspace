param(
    [Parameter(Position = 0)]
    [ValidateSet("run", "status", "catalog")]
    [string]$Command = "run",
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [Parameter(Mandatory = $false)]
    [string]$Suite,
    [Parameter(Mandatory = $false)]
    [string]$Test,
    [string]$BaseUrl,
    [int]$TimeoutSeconds = 180,
    [string]$ArtifactDirectory = (Join-Path $WorkspaceRoot "build\reports\dev-client-regression")
)

$ErrorActionPreference = "Stop"

function Invoke-RegressionRequest {
    param(
        [Parameter(Mandatory = $true)] [string]$Method,
        [Parameter(Mandatory = $true)] [string]$Path,
        [object]$Body = $null
    )

    $uri = "$BaseUrl$Path"
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 15
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType "application/json" -Body ($Body | ConvertTo-Json -Compress) -TimeoutSec 15
}

function Write-JunitResult {
    param([Parameter(Mandatory = $true)] [object]$Result, [Parameter(Mandatory = $true)] [string]$Path)

    $document = New-Object System.Xml.XmlDocument
    $declaration = $document.CreateXmlDeclaration("1.0", "UTF-8", $null)
    [void]$document.AppendChild($declaration)
    $suite = $document.CreateElement("testsuite")
    $suite.SetAttribute("name", $Result.suiteId)
    $runtimeCases = @()
    if ($null -ne $Result.runtimeConformance) {
        $runtimeCases += @($Result.runtimeConformance)
    }
    $cases = $runtimeCases + @([pscustomobject]@{ testId = $Result.testId; outcome = $Result.outcome; message = $Result.message; classname = $Result.suiteId })
    $suite.SetAttribute("tests", [string]$cases.Count)
    $suite.SetAttribute("failures", [string]@($cases | Where-Object { $_.outcome -eq "failed" }).Count)
    $suite.SetAttribute("skipped", [string]@($cases | Where-Object { $_.outcome -eq "skipped" }).Count)
    foreach ($caseResult in $cases) {
        $case = $document.CreateElement("testcase")
        $case.SetAttribute("classname", $(if ($caseResult.classname) { $caseResult.classname } else { "$($Result.suiteId).runtimeConformance" }))
        $case.SetAttribute("name", $caseResult.testId)
        if ($caseResult.outcome -eq "failed") {
            $failure = $document.CreateElement("failure")
            $failure.SetAttribute("message", [string]$caseResult.message)
            [void]$case.AppendChild($failure)
        } elseif ($caseResult.outcome -eq "skipped") {
            $skipped = $document.CreateElement("skipped")
            $skipped.SetAttribute("message", [string]$caseResult.message)
            [void]$case.AppendChild($skipped)
        }
        [void]$suite.AppendChild($case)
    }
    [void]$document.AppendChild($suite)
    $document.Save($Path)
}

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $launch = & (Join-Path $PSScriptRoot "start-and-ready.ps1") -WorkspaceRoot $WorkspaceRoot -RecipeViewer none -SkipRecipeViewerReady -TimeoutSeconds $TimeoutSeconds -CloseOnExit
    $BaseUrl = $launch.baseUrl
}

if ($Command -eq "catalog") {
    Invoke-RegressionRequest -Method Get -Path "/regression/catalog"
    return
}
if ($Command -eq "status") {
    Invoke-RegressionRequest -Method Get -Path "/regression/status"
    return
}
if ([string]::IsNullOrWhiteSpace($Suite) -or [string]::IsNullOrWhiteSpace($Test)) {
    throw "run requires -Suite and -Test."
}

$result = Invoke-RegressionRequest -Method Post -Path "/regression/run" -Body @{ suiteId = $Suite; testId = $Test }
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ($result.outcome -eq "running" -and (Get-Date) -lt $deadline) {
    Start-Sleep -Milliseconds 250
    $result = Invoke-RegressionRequest -Method Get -Path "/regression/status"
}
if ($result.outcome -eq "running") {
    throw "Timed out waiting for regression $Suite/$Test."
}

New-Item -ItemType Directory -Force -Path $ArtifactDirectory | Out-Null
$artifactBase = "$($Suite)-$($Test)" -replace "[^A-Za-z0-9_.-]", "_"
$jsonPath = Join-Path $ArtifactDirectory "$artifactBase.json"
$junitPath = Join-Path $ArtifactDirectory "$artifactBase.junit.xml"
$result | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $jsonPath -Encoding utf8
Write-JunitResult -Result $result -Path $junitPath

[pscustomobject]@{
    ok = $result.ok
    outcome = $result.outcome
    suiteId = $result.suiteId
    testId = $result.testId
    json = $jsonPath
    junit = $junitPath
    result = $result
}

if (-not $result.ok) {
    throw "Regression $Suite/$Test $($result.outcome): $($result.message)"
}
