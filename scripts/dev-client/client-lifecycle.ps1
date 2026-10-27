# Dot-source from a launcher or an outer test batch. Only an explicit ownership
# record can authorize shutdown; a caller-supplied BaseUrl never creates one.
function New-ClientOwnership {
    param([Parameter(Mandatory = $true)] [System.Diagnostics.Process]$Launcher)

    [pscustomobject]@{
        launcherProcessId = $Launcher.Id
        launcherStartedAtUtc = $Launcher.StartTime.ToUniversalTime().ToString('o')
        processId = 0
        processStartedAtUtc = $null
        baseUrl = $null
    }
}

function Get-OwnedClientProcess {
    param([int]$ProcessId, [object]$StartedAtUtc)

    if (-not $ProcessId) { return $null }
    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    # ConvertFrom-Json can materialize ISO timestamps as DateTime. Do not coerce
    # those back through a culture-specific string, which loses fractional ticks.
    $expected = if ($StartedAtUtc -is [datetime]) { $StartedAtUtc } else { [datetime]::Parse([string]$StartedAtUtc, [cultureinfo]::InvariantCulture, [System.Globalization.DateTimeStyles]::RoundtripKind) }
    if ($process -and $process.StartTime.ToUniversalTime().Ticks -eq $expected.ToUniversalTime().Ticks) {
        return $process
    }
    return $null
}

function Set-OwnedClientDiscovery {
    param([Parameter(Mandatory = $true)] [object]$Ownership, [Parameter(Mandatory = $true)] [object]$Discovery)

    $launcher = Get-OwnedClientProcess $Ownership.launcherProcessId $Ownership.launcherStartedAtUtc
    if (-not $launcher) { throw 'The owned launcher exited before client readiness.' }
    $parents = @([pscustomobject]@{ id = $launcher.Id; started = $launcher.StartTime })
    $visited = [System.Collections.Generic.HashSet[int]]::new()
    $null = $visited.Add($launcher.Id)
    $descendants = @()
    while ($parents.Count -gt 0) {
        $next = @()
        foreach ($parent in $parents) {
            foreach ($child in @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $($parent.id)")) {
                $id = [int]$child.ProcessId
                $started = [datetime]$child.CreationDate
                # A child predating its live parent belongs to a recycled PID, not this launch.
                if ($started.ToUniversalTime() -lt $parent.started.ToUniversalTime() -or -not $visited.Add($id)) { continue }
                $next += [pscustomobject]@{ id = $id; started = $started }
                $descendants += $id
            }
        }
        $parents = $next
    }
    if ([int]$Discovery.processId -notin $descendants) {
        throw 'Bridge discovery does not belong to this launch; refusing to attach or stop it.'
    }
    $client = Get-Process -Id $Discovery.processId -ErrorAction Stop
    $Ownership.processId = $client.Id
    $Ownership.processStartedAtUtc = $client.StartTime.ToUniversalTime().ToString('o')
    $Ownership.baseUrl = "http://$($Discovery.host):$($Discovery.port)"
}

function Stop-OwnedClient {
    param(
        [Parameter(Mandatory = $true)] [object]$Ownership,
        [switch]$KeepClientOpen,
        [bool]$WaitForRegression = $true,
        [int]$TimeoutSeconds = 60
    )

    $result = [pscustomobject]@{
        ok = $true; status = 'stopped'; ownership = $Ownership
        cancellationRequested = $false; forced = $false; diagnostics = @(); errors = @()
    }
    if ($KeepClientOpen) { $result.status = 'keptOpen'; return $result }
    $client = Get-OwnedClientProcess $Ownership.processId $Ownership.processStartedAtUtc
    if ($client -and $Ownership.baseUrl) {
        # This is a lifecycle boundary: still attempt shutdown if cancellation or
        # transport fails, retain the journal, and report every teardown error.
        if ($WaitForRegression) {
            $deadline = [datetime]::UtcNow.AddSeconds($TimeoutSeconds)
            $terminal = $false
            $cancellationAccepted = $false
            do {
                $requestTimeout = [math]::Max(1, [math]::Min(10, [math]::Ceiling(($deadline - [datetime]::UtcNow).TotalSeconds)))
                try {
                    $state = Invoke-RestMethod -Method Get -Uri "$($Ownership.baseUrl)/regression/status" -TimeoutSec $requestTimeout
                    if ($state.outcome -ne 'running') { $terminal = $true; break }
                } catch { $result.diagnostics += "Cleanup status retry: $($_.Exception.Message)" }
                if (-not $cancellationAccepted -and [datetime]::UtcNow -lt $deadline) {
                    try {
                        $result.cancellationRequested = $true
                        $requestTimeout = [math]::Max(1, [math]::Min(10, [math]::Ceiling(($deadline - [datetime]::UtcNow).TotalSeconds)))
                        $state = Invoke-RestMethod -Method Post -Uri "$($Ownership.baseUrl)/regression/cancel" -ContentType 'application/json' -Body '{}' -TimeoutSec $requestTimeout
                        $cancellationAccepted = $true
                        if ($state.outcome -ne 'running') { $terminal = $true; break }
                    } catch { $result.diagnostics += "Cancellation retry: $($_.Exception.Message)" }
                }
                if ([datetime]::UtcNow -lt $deadline) { Start-Sleep -Milliseconds 250 }
            } while ([datetime]::UtcNow -lt $deadline)
            if (-not $terminal) { $result.errors += 'Cancellation/cleanup timed out; recovery journal was retained.' }
        }
        try {
            $null = Invoke-RestMethod -Method Post -Uri "$($Ownership.baseUrl)/client/stop" -ContentType 'application/json' -Body '{}' -TimeoutSec 10
        } catch { $result.errors += "Client shutdown: $($_.Exception.Message)" }
        $deadline = [datetime]::UtcNow.AddSeconds($TimeoutSeconds)
        while ((Get-OwnedClientProcess $Ownership.processId $Ownership.processStartedAtUtc) -and [datetime]::UtcNow -lt $deadline) {
            Start-Sleep -Milliseconds 250
        }
        if (Get-OwnedClientProcess $Ownership.processId $Ownership.processStartedAtUtc) {
            $result.errors += 'Owned client did not exit within the shutdown deadline.'
        }
    }

    # Launches use --no-daemon, so this tree cannot contain a shared Gradle daemon.
    # This also handles startup failure before any bridge/PID was published.
    $launcher = Get-OwnedClientProcess $Ownership.launcherProcessId $Ownership.launcherStartedAtUtc
    $client = Get-OwnedClientProcess $Ownership.processId $Ownership.processStartedAtUtc
    if ($launcher -or $client) {
        try {
            if ($launcher) {
                if (-not $client -and $Ownership.processId) { $null = $launcher.WaitForExit(5000) }
                if (-not $launcher.HasExited) { $result.forced = $true; $launcher.Kill($true); $null = $launcher.WaitForExit(5000) }
            }
            if ($client -and -not $client.HasExited) { $result.forced = $true; $client.Kill($true); $null = $client.WaitForExit(5000) }
        } catch { $result.errors += "Owned process cleanup: $($_.Exception.Message)" }
    }
    if ((Get-OwnedClientProcess $Ownership.processId $Ownership.processStartedAtUtc) -or
            (Get-OwnedClientProcess $Ownership.launcherProcessId $Ownership.launcherStartedAtUtc)) {
        $result.errors += 'An owned process is still running.'
    }
    $result.ok = $result.errors.Count -eq 0
    if (-not $result.ok) { $result.status = 'error' }
    return $result
}
