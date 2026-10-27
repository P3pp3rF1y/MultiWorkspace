param([string]$ReportPath)
$ErrorActionPreference = "Stop"
$tokens = $null
$parseErrors = $null
foreach ($script in @('regression.ps1', 'run-regression.ps1')) {
$ast = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $script), [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count -gt 0) { throw ($parseErrors | Out-String) }
foreach ($name in @("Add-SensitiveValues", "ConvertTo-SanitizedText", "Copy-SanitizedTelemetry")) {
    $function = $ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq $name }, $true)
    . ([scriptblock]::Create($function.Extent.Text))
}
$secretValues = [System.Collections.Generic.List[string]]::new()
$inputValue = [pscustomobject]@{
    message = "later secret abc-123456 and prefix abc-123"
    ordinary = [pscustomobject]@{ count = 3; enabled = $true; nothing = $null; empty = @(); singleton = @("one") }
    nested = @([ordered]@{ trace = "abc-123456"; PASSWORD = "abc-123" }, [pscustomobject]@{ apiKey = "abc-123456" })
    url = "https://alice:credential@example.test/path?ordinary=yes&access%5Ftoken=hidden"
    stackTrace = "Request failed (password='inline credential'); {`"secret`":`"inline secret`"}"
}

$sanitized = Copy-SanitizedTelemetry $inputValue
$json = $sanitized | ConvertTo-Json -Depth 64 -Compress

foreach ($sensitive in @("abc-123", "alice:credential", "hidden", "inline credential", "inline secret", "PASSWORD", "apiKey")) {
    if ($json.Contains($sensitive)) { throw "Sensitive data survived: $sensitive" }
}
if (-not $json.Contains("[REDACTED]") -or -not $sanitized.url.Contains("ordinary=yes") -or $sanitized.ordinary.count -ne 3 -or
    -not $sanitized.ordinary.enabled -or $null -ne $sanitized.ordinary.nothing -or $sanitized.ordinary.empty.Count -ne 0 -or
    $sanitized.ordinary.singleton.Count -ne 1 -or $sanitized.nested.Count -ne 2) { throw "Sanitization changed non-sensitive structure" }
if ($inputValue.nested[1].apiKey -ne "abc-123456" -or -not $inputValue.message.Contains("abc-123456")) { throw "Input was mutated" }
if ((Copy-SanitizedTelemetry ([pscustomobject]@{ repeated = "abc-123456" })).repeated -ne "[REDACTED]") { throw "Known secrets were forgotten across copies" }

$timing = $null
if ($ReportPath) {
    $secretValues.Clear()
    $report = Get-Content -LiteralPath $ReportPath -Raw | ConvertFrom-Json -Depth 64
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    $copy = Copy-SanitizedTelemetry $report
    $watch.Stop()
    $timing = $watch.Elapsed.TotalMilliseconds
    if ($copy.suiteId -ne $report.suiteId -or $copy.result.outcome -ne $report.result.outcome) { throw "Saved report semantics changed" }
}
[pscustomobject]@{ ok = $true; script = $script; redactionAndStructure = "passed"; reportPath = $ReportPath; reportSanitizationMillis = $timing }
}
