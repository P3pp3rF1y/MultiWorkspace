param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot/../..").Path)
$ErrorActionPreference = "Stop"
$artifacts = Join-Path $WorkspaceRoot "DevClientAutomation/build/reports/dev-client-regression/controller-contract"
$runner = Join-Path $PSScriptRoot "regression.ps1"
# A loopback stub supplies discovery only. Product execution fails at the HTTP boundary.
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = $listener.LocalEndpoint.Port
$listener.Stop()
$job = Start-Job -ArgumentList $port -ScriptBlock {
    param($port)
    $server = [System.Net.HttpListener]::new()
    $server.Prefixes.Add("http://127.0.0.1:$port/")
    $server.Start()
    try {
        for ($i = 0; $i -lt 6; $i++) {
            $exchange = $server.GetContext()
            $json = switch ($exchange.Request.Url.AbsolutePath) {
                "/capabilities" { @{ loader = "stub"; minecraftVersion = "test"; backgroundInput = ($i -lt 4) } | ConvertTo-Json -Compress }
                "/regression/catalog" { '{"suiteTests":{"controller-proof":["first","second"]},"testMetadata":{"controller-proof":{"first":{"effectiveRequirements":[]},"second":{"effectiveRequirements":["jei"]}}}}' }
                default { $exchange.Response.StatusCode = 503; '{"error":"injected controller transport failure"}' }
            }
            $bytes = [Text.Encoding]::UTF8.GetBytes($json)
            $exchange.Response.ContentType = "application/json"
            $exchange.Response.ContentLength64 = $bytes.Length
            $exchange.Response.OutputStream.Write($bytes)
            $exchange.Response.Close()
        }
    } finally { $server.Close() }
}
try {
    # Wait for the stub to bind without advancing its scripted request sequence.
    $deadline = [DateTime]::UtcNow.AddSeconds(10)
    do {
        $client = [System.Net.Sockets.TcpClient]::new()
        try { $client.Connect("127.0.0.1", $port); $ready = $true } catch { $ready = $false; Start-Sleep -Milliseconds 50 } finally { $client.Dispose() }
    } until ($ready -or [DateTime]::UtcNow -gt $deadline)
    if (-not $ready) { throw "Stub did not bind" }
    $null = & pwsh -NoProfile -File $runner -WorkspaceRoot $WorkspaceRoot -BaseUrl "http://127.0.0.1:$port" -Suite controller-proof -ArtifactDirectory $artifacts 2>&1
    if ($LASTEXITCODE -eq 0) { throw "Interrupted suite unexpectedly succeeded" }
    $report = Get-Content -LiteralPath (Join-Path $artifacts "controller-proof.json") -Raw | ConvertFrom-Json -Depth 64
    if ($report.accounting.error -ne 1 -or $report.accounting.notRun -ne 1 -or $report.outcome -ne "error") { throw "Wrong interrupted-suite accounting" }
    $errorCase = $report.results[0]
    $notRunCase = $report.notRun[0]
    foreach ($case in @($errorCase, $notRunCase)) {
        if ([string]::IsNullOrWhiteSpace($case.runId)) { throw "Canonical run identity missing" }
        if (@($case.runtimeIdentity.controller.selection.testIds) -join ',' -ne $case.testId) { throw "Stale selected-test telemetry" }
        foreach ($artifact in $case.artifacts.PSObject.Properties) {
            if (-not (Test-Path -LiteralPath $artifact.Value)) { throw "Advertised artifact does not exist: $($artifact.Value)" }
        }
        foreach ($field in @("primaryCategory", "primaryMessage", "runtimeIdentity", "artifacts", "effectiveRequirements", "missingRequirements", "reportCases")) {
            if ($null -eq $case.PSObject.Properties[$field]) { throw "Canonical field $field missing" }
        }
        if (-not $case.rerunCommand.Contains("-BaseUrl")) { throw "Exact rerun missing" }
    }
    if ($errorCase.reportCases[0].junitElement -ne "error" -or -not $errorCase.primaryFailure.stackTrace -or
        $notRunCase.reportCases[0].junitElement -ne "skipped" -or $notRunCase.effectiveRequirements[0] -ne "jei") { throw "Canonical controller semantics lost" }
    $xml = [System.Xml.XmlDocument]::new()
    $xml.Load((Join-Path $artifacts "controller-proof.junit.xml"))
    if ($xml.SelectNodes("//error").Count -ne 1 -or $xml.SelectNodes("//skipped").Count -ne 1) { throw "Wrong controller XML" }
    & "$PSScriptRoot/assert-background-input.ps1" -BaseUrl "http://127.0.0.1:$port"
    $rejected = $false
    try { & "$PSScriptRoot/assert-background-input.ps1" -BaseUrl "http://127.0.0.1:$port" } catch { $rejected = $_.Exception.Message.Contains("background-input client") }
    if (-not $rejected) { throw "Ordinary client was accepted for background regression execution" }
    $null = & pwsh -NoProfile -File $runner -WorkspaceRoot $WorkspaceRoot -BaseUrl "http://127.0.0.1:$port" -Suite controller-proof -Test manual-rejected -ArtifactDirectory $artifacts 2>&1
    if ($LASTEXITCODE -eq 0) { throw "Runner accepted an ordinary manual client" }
    $manual = Get-Content -LiteralPath (Join-Path $artifacts "controller-proof-manual-rejected.json") -Raw | ConvertFrom-Json -Depth 64
    if ($manual.result.category -ne "infrastructure" -or -not $manual.result.message.Contains("background-input client")) { throw "Manual-client rejection lost canonical diagnostics" }
    [pscustomobject]@{ ok = $true; selected = 2; error = 1; notRun = 1; manualClientRejectedBeforeRun = $true; artifacts = $artifacts }
} finally {
    Stop-Job $job
    Remove-Job $job
}
