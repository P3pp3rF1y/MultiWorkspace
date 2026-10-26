param([Parameter(Mandatory = $true)] [string]$BaseUrl)
$ErrorActionPreference = "Stop"
$capabilities = Invoke-RestMethod -Method Get -Uri "$BaseUrl/capabilities" -TimeoutSec 10
if (-not $capabilities.backgroundInput) {
    throw "Regression execution requires a background-input client. Launch :workspace:runRegressionClient or let the regression runner start the client."
}
