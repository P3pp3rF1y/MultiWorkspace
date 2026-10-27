param([Parameter(Mandatory)][string]$OwnershipPath)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/client-lifecycle.ps1"
if (-not (Test-Path -LiteralPath $OwnershipPath -PathType Leaf)) { return }
$ownership = Get-Content -LiteralPath $OwnershipPath -Raw | ConvertFrom-Json
$result = Stop-OwnedClient -Ownership $ownership
if (-not $result.ok) { throw "Owned client cleanup failed: $($result.errors -join '; ')" }
