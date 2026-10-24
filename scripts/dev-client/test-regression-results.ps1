param(
    [string]$FixturePath = (Join-Path $PSScriptRoot "../../DevClientAutomation/build/reports/regression-result-model/goldens.json"),
    [string]$ArtifactDirectory = (Join-Path $PSScriptRoot "../../DevClientAutomation/build/reports/regression-result-model/xml")
)
$ErrorActionPreference = "Stop"
# Exercise the production writer against JVM-produced result models, without starting a client.
$tokens = $null
$parseErrors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot "regression.ps1"), [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count -gt 0) { throw ($parseErrors | Out-String) }
foreach ($name in @("Format-Seconds", "Write-JunitResult")) {
    $function = $ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq $name }, $true)
    . ([scriptblock]::Create($function.Extent.Text))
}
# Redaction is orthogonal to XML/result semantics; keep fixture data intact here.
function Copy-SanitizedTelemetry($Value) { return $Value }
function ConvertTo-SanitizedText($Value) { return [string]$Value }
$goldens = Get-Content -LiteralPath $FixturePath -Raw | ConvertFrom-Json -Depth 64
New-Item -ItemType Directory -Force -Path $ArtifactDirectory | Out-Null
foreach ($golden in $goldens) {
    $path = Join-Path $ArtifactDirectory "$($golden.name).junit.xml"
    Write-JunitResult -Results @($golden.result) -Path $path -IncludeConformance
    $xml = [System.Xml.XmlDocument]::new()
    $xml.Load($path)
    foreach ($counter in @("tests", "failures", "errors", "skipped")) {
        if ([int]$xml.testsuite.GetAttribute($counter) -ne $golden.$counter) { throw "$($golden.name): incorrect $counter" }
    }
    if ($xml.SelectNodes("//testcase").Count -ne $golden.tests -or
        $xml.SelectNodes("//failure").Count -ne $golden.failures -or
        $xml.SelectNodes("//error").Count -ne $golden.errors -or
        $xml.SelectNodes("//skipped").Count -ne $golden.skipped) { throw "$($golden.name): incorrect emitted nodes" }
    if ($golden.name -ne "conformance-root" -and -not $xml.OuterXml.Contains("prepare-member")) { throw "Lost structured step provenance" }
}
$aggregatePath = Join-Path $ArtifactDirectory "aggregate.junit.xml"
Write-JunitResult -Results @($goldens | ForEach-Object { $_.result }) -Path $aggregatePath -IncludeConformance
$aggregate = [System.Xml.XmlDocument]::new()
$aggregate.Load($aggregatePath)
foreach ($counter in @("tests", "failures", "errors", "skipped")) {
    $expected = ($goldens | Measure-Object -Property $counter -Sum).Sum
    if ([int]$aggregate.testsuite.GetAttribute($counter) -ne $expected) { throw "Aggregate: incorrect $counter" }
}
$controller = Get-Content -LiteralPath (Join-Path (Split-Path $FixturePath) "controller.json") -Raw | ConvertFrom-Json -Depth 64
$controllerPath = Join-Path $ArtifactDirectory "controller.junit.xml"
Write-JunitResult -Results @($controller[0]) -NotRun @($controller[1]) -Path $controllerPath
$controllerXml = [System.Xml.XmlDocument]::new()
$controllerXml.Load($controllerPath)
if ($controllerXml.SelectNodes("//error").Count -ne 1 -or $controllerXml.SelectNodes("//skipped").Count -ne 1 -or
    -not $controllerXml.OuterXml.Contains("offline controller cause") -or -not $controllerXml.OuterXml.Contains("candidate-sha")) {
    throw "Controller failure/identity or notRun mapping was lost"
}
[pscustomobject]@{ ok = $true; parsedGoldens = $goldens.Count; controllerCases = $controller.Count; artifacts = $ArtifactDirectory }
