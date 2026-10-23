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
        $stop = $false
        $outcome = if ($Mode -in @('assertion', 'combined-failure')) { 'failed' } else { 'passed' }
        if ($Mode -in @('cancel', 'interrupted', 'teardown-poll', 'cleanup-timeout') -and $active) {
            $outcome = 'running'
            if ($cancelled -and $path -eq '/regression/status') {
                $cleanupPolls++
                if ($cleanupPolls -ge 2 -and $Mode -ne 'cleanup-timeout') { $outcome = 'cancelled' }
            }
        }
        $message = if ($outcome -eq 'failed') { 'Injected product assertion' } else { 'Fixture result' }
        $category = if ($outcome -eq 'failed') { 'assertion' } elseif ($outcome -eq 'cancelled') { 'cancellation' } else { '' }
        $junit = if ($outcome -eq 'failed') { 'failure' } elseif ($outcome -eq 'cancelled') { 'skipped' } else { '' }
        $response = switch ($path) {
            '/state' {
                $stateReads++
                if ($Mode -eq 'discovery-overwrite' -and $stateReads -eq 2) {
                    @{ host = '127.0.0.1'; port = 1; processId = 1 } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Directory 'workspace/run/dev-client-automation.json')
                }
                @{ playerLoaded = $true; screenSimpleName = 'TitleScreen' }
            }
            '/capabilities' { @{ ok = ($Mode -ne 'invalid-startup'); protocolVersion = 1; loader = 'neoforge'; minecraftVersion = 'test'; backgroundInput = $true } }
            '/regression/run' {
                if ($Mode -in @('transport', 'teardown-poll', 'cleanup-timeout')) { $active = $true; $exchange.Response.StatusCode = 503; @{ message = 'Injected transport failure' }; break }
                $active = $true
                if ($Mode -in @('cancel', 'interrupted')) { @{ outcome = 'running' }; break }
                $body = [IO.StreamReader]::new($exchange.Request.InputStream).ReadToEnd() | ConvertFrom-Json
                @{ ok = ($outcome -eq 'passed'); runId = 'proof/one'; suiteId = 'proof'; testId = 'one'; outcome = $outcome; category = $category
                    message = $message; durationMillis = 1; testDurationMillis = 1; outcomePriority = $(if ($outcome -eq 'failed') { 5 } else { 0 })
                    conformance = @(); runtimeConformance = @(); cleanup = @(); steps = @(); diagnostics = @{}
                    rerunCommand = $body.reportContext.rerunCommand; runtimeIdentity = $body.reportContext.runtimeIdentity; artifacts = $body.reportContext.artifacts
                    reportCases = @(@{ testId = 'one'; classname = 'proof'; role = 'scenario'; outcome = $outcome; category = $category; message = $message; durationMillis = 1; junitElement = $junit; diagnostics = @{} }) }
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
        @{ path = $path; outcome = $outcome; at = [datetime]::UtcNow.ToString('o') } | ConvertTo-Json -Compress | Add-Content -LiteralPath (Join-Path $Directory 'requests.jsonl')
        $bytes = [Text.Encoding]::UTF8.GetBytes(($response | ConvertTo-Json -Depth 32 -Compress))
        $exchange.Response.ContentType = 'application/json'
        $exchange.Response.ContentLength64 = $bytes.Length
        $exchange.Response.OutputStream.Write($bytes)
        $exchange.Response.Close()
        if ($stop) { break }
    }
} finally { $server.Close() }
