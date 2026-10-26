param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot/../..").Path)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/client-lifecycle.ps1"
$root = Join-Path $WorkspaceRoot "build/reports/dev-client-regression/startup-warning/$([guid]::NewGuid())"
$cases = @()
foreach ($mode in 'startup-warning','startup-wrong-version','startup-fatal','startup-inactive') {
    $directory = Join-Path $root $mode
    $null = New-Item -ItemType Directory -Force -Path (Join-Path $directory 'workspace/run')
    $fixture = Join-Path $PSScriptRoot 'test-fixtures/lifecycle-client.ps1'
    "@pwsh -NoProfile -File `"$fixture`" -Directory `"$directory`" -Mode `"$mode`"" | Set-Content -LiteralPath (Join-Path $directory 'gradlew.bat')
    $ownership = $null
    $ready = $null
    $failed = $false
    try {
        try {
            $ready = & "$PSScriptRoot/start-and-ready.ps1" -WorkspaceRoot $directory -TimeoutSeconds 6 -CloseOnExit -SkipRecipeViewerReady
            $ownership = $ready.ownership
        } catch {
            $failed = $true
            $ownership = $_.Exception.Data['ClientOwnership']
            if ($mode -eq 'startup-warning') { throw }
            if ($_.Exception.Message -notmatch 'Timed out waiting for title screen') { throw }
        }
        if (($null -ne $ready) -ne ($mode -eq 'startup-warning') -or $failed -eq ($mode -eq 'startup-warning')) { throw "${mode}: wrong startup outcome" }
        $requests = @(Get-Content -LiteralPath (Join-Path $directory 'requests.jsonl') | ForEach-Object { $_ | ConvertFrom-Json })
        $clicks = @($requests | Where-Object path -eq '/click-widget')
        $loads = @($requests | Where-Object path -eq '/world/load')
        if ($mode -eq 'startup-warning') {
            if ($clicks.Count -ne 1 -or $clicks[0].text -cne 'Proceed to main menu' -or $loads.Count -ne 1 -or
                [array]::IndexOf(@($requests.path), '/click-widget') -ge [array]::IndexOf(@($requests.path), '/world/load') -or
                -not $ready.state.playerLoaded) { throw '26.2 warning was not acknowledged exactly before loading the world' }
        } elseif ($clicks.Count -ne 0 -or $loads.Count -ne 0) { throw "${mode}: startup accepted a non-qualifying screen" }
        $cases += @{ mode = $mode; ok = $true; clicks = $clicks.Count; worldLoads = $loads.Count }
    } finally {
        if ($ownership) { $null = Stop-OwnedClient $ownership -WaitForRegression:$false -TimeoutSeconds 2 }
        $clientIdPath = Join-Path $directory 'client.pid'
        if (Test-Path -LiteralPath $clientIdPath) {
            $clientId = [int](Get-Content -LiteralPath $clientIdPath -Raw)
            if (Get-Process -Id $clientId -ErrorAction SilentlyContinue) { throw "${mode}: fixture client was not stopped" }
        }
    }
}
@{ ok = $true; cases = $cases; artifacts = $root } | ConvertTo-Json -Depth 8
