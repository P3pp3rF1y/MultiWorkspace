param([string]$Directory, [string]$Mode)
$ErrorActionPreference = 'Stop'
$PID | Set-Content -LiteralPath (Join-Path $Directory 'client.pid')
if ($Mode -eq 'no-bridge') { Start-Sleep -Seconds 120; return }
$socket = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$socket.Start()
$port = $socket.LocalEndpoint.Port
$socket.Stop()
$server = [System.Net.HttpListener]::new()
$server.Prefixes.Add("http://127.0.0.1:$port/")
$server.Start()
@{ host = '127.0.0.1'; port = $port; processId = $PID } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Directory 'workspace/run/dev-client-automation.json')
$cancelled = $false
$cleanupPolls = 0
$active = $false
$interrupted = $false
$stateReads = 0
try {
    while ($true) {
        $exchange = $server.GetContext()
        $path = $exchange.Request.Url.AbsolutePath
        $body = if ($path -eq '/regression/run') { [IO.StreamReader]::new($exchange.Request.InputStream).ReadToEnd() | ConvertFrom-Json } else { $null }
        $stop = $false
        $outcome = if ($Mode -in @('assertion', 'combined-failure')) { 'failed' } else { 'passed' }
        if ($Mode -eq 'shared-cleanup-error' -and $body -and $body.suiteId -eq 'proof-second') { $outcome = 'error' }
        if ($Mode -in @('shared-qualified-skip', 'shared-unqualified-skip') -and $body -and $body.suiteId -eq 'proof-second') { $outcome = 'skipped' }
        if ($Mode -in @('shared-mixed-skip', 'shared-mixed-error') -and $body -and $body.suiteId -eq 'proof-second') {
            if ($body.testId -eq 'two') { $outcome = 'skipped' }
            if ($Mode -eq 'shared-mixed-error' -and $body.testId -eq 'three') { $outcome = 'error' }
        }
        if ($Mode -in @('cancel', 'interrupted', 'teardown-poll', 'cleanup-timeout') -and $active) {
            $outcome = 'running'
            if ($cancelled -and $path -eq '/regression/status') {
                $cleanupPolls++
                if ($cleanupPolls -ge 2 -and $Mode -ne 'cleanup-timeout') { $outcome = 'cancelled' }
            }
        }
        $message = if ($outcome -eq 'failed') { 'Injected product assertion' } else { 'Fixture result' }
        $category = if ($outcome -eq 'error') { 'cleanup' } elseif ($outcome -eq 'failed') { 'assertion' } elseif ($outcome -eq 'cancelled') { 'cancellation' } elseif ($outcome -eq 'skipped') { $(if ($Mode -eq 'shared-unqualified-skip') { 'other' } else { 'expectedUnavailable' }) } else { '' }
        $junit = if ($outcome -eq 'error') { 'error' } elseif ($outcome -eq 'failed') { 'failure' } elseif ($outcome -in 'cancelled','skipped') { 'skipped' } else { '' }
        $response = switch ($path) {
            '/state' {
                $stateReads++
                if ($Mode -eq 'discovery-overwrite' -and $stateReads -eq 2) {
                    @{ host = '127.0.0.1'; port = 1; processId = 1 } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Directory 'workspace/run/dev-client-automation.json')
                }
                @{ playerLoaded = $true; screenSimpleName = 'TitleScreen' }
            }
            '/capabilities' { @{ ok = ($Mode -ne 'invalid-startup'); protocolVersion = 1; loader = 'neoforge'; minecraftVersion = 'test'; backgroundInput = $true } }
            '/regression/catalog' { @{ suiteTests = @{ 'proof-first' = @('one'); 'proof-second' = $(if ($Mode -in 'shared-mixed-skip','shared-mixed-error') { @('one','two','three') } else { @('one') }); 'proof-third' = @('one') } } }
            '/regression/run' {
                if ($Mode -in @('transport', 'teardown-poll', 'cleanup-timeout')) { $active = $true; $exchange.Response.StatusCode = 503; @{ message = 'Injected transport failure' }; break }
                $active = $true
                if ($Mode -in @('cancel', 'interrupted')) { @{ outcome = 'running' }; break }
                @{ ok = ($outcome -eq 'passed'); runId = "$($body.suiteId)/$($body.testId)"; suiteId = $body.suiteId; testId = $body.testId; outcome = $outcome; category = $category
                    effectiveRequirements = $(if ($outcome -eq 'skipped') { @('external-mod') } else { @() }); missingRequirements = $(if ($outcome -eq 'skipped') { @('external-mod') } else { @() })
                    message = $message; durationMillis = 1; testDurationMillis = 1; outcomePriority = $(if ($outcome -in 'failed','error') { 5 } elseif ($outcome -eq 'skipped') { 1 } else { 0 })
                    conformance = @(); runtimeConformance = @(); cleanup = @(); steps = @(); diagnostics = @{}
                    rerunCommand = $body.reportContext.rerunCommand; runtimeIdentity = $body.reportContext.runtimeIdentity; artifacts = $body.reportContext.artifacts
                    reportCases = @(@{ testId = $body.testId; classname = 'proof'; role = 'scenario'; outcome = $outcome; category = $category; message = $message; durationMillis = 1; junitElement = $junit; diagnostics = @{} }) }
            }
            '/regression/status' {
                if ($Mode -eq 'teardown-poll' -and $cancelled -and $cleanupPolls -eq 1) {
                    $exchange.Response.StatusCode = 503
                    @{ message = 'Injected transient cleanup-poll failure' }
                    break
                }
                if ($Mode -eq 'interrupted' -and -not $interrupted) {
                    $interrupted = $true
                    $exchange.Response.StatusCode = 503
                    @{ message = 'Injected interruption during an active test' }
                    break
                }
                @{ ok = ($outcome -eq 'passed'); runId = 'proof/one'; suiteId = 'proof'; testId = 'one'; outcome = $outcome; category = $category
                    message = $message; durationMillis = 1; testDurationMillis = 1; outcomePriority = 3
                    conformance = @(); runtimeConformance = @(); cleanup = @(); steps = @(); diagnostics = @{}
                    reportCases = @(@{ testId = 'one'; classname = 'proof'; role = 'scenario'; outcome = $outcome; category = $category; message = $message; durationMillis = 1; junitElement = $junit; diagnostics = @{} }) }
            }
            '/regression/cancel' { $active = $true; $cancelled = $true; @{ outcome = 'running' } }
            '/client/stop' {
                if ($Mode -in @('shutdown-failure', 'combined-failure')) { $exchange.Response.StatusCode = 503; @{ message = 'Injected shutdown failure' } }
                else { $stop = $true; @{ ok = $true } }
            }
            default { $exchange.Response.StatusCode = 404; @{ message = 'Unexpected fixture request' } }
        }
        @{ path = $path; outcome = $outcome; at = [datetime]::UtcNow.ToString('o'); ownershipBeforeCase = $(if ($path -eq '/regression/run' -and $env:DCA_LIFECYCLE_REPORT_PATH) {
                    Test-Path -LiteralPath (Join-Path (Split-Path -Parent $env:DCA_LIFECYCLE_REPORT_PATH) 'client-ownership.json') -PathType Leaf
                } else { $null }) } | ConvertTo-Json -Compress | Add-Content -LiteralPath (Join-Path $Directory 'requests.jsonl')
        $bytes = [Text.Encoding]::UTF8.GetBytes(($response | ConvertTo-Json -Depth 32 -Compress))
        $exchange.Response.ContentType = 'application/json'
        $exchange.Response.ContentLength64 = $bytes.Length
        $exchange.Response.OutputStream.Write($bytes)
        $exchange.Response.Close()
        if ($stop) { break }
    }
} finally { $server.Close() }
