param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot/../..").Path)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/client-lifecycle.ps1"
$fixtureStarted = [datetime]'2026-10-27T00:00:00Z'
function Get-OwnedClientProcess { param($ProcessId, $StartedAtUtc) [pscustomobject]@{ Id = 101; StartTime = $fixtureStarted } }
function Get-Process { param($Id, $ErrorAction) [pscustomobject]@{ Id = $Id; StartTime = $fixtureStarted.AddSeconds(3) } }
function Get-CimInstance {
    param($ClassName, $Filter)
    $script:queries++
    if ($script:queries -gt 4) { throw 'Ancestry traversal repeated a process' }
    $parent = [int]($Filter -replace 'ParentProcessId = ', '')
    if ($parent -eq 101) {
        [pscustomobject]@{ ProcessId = 202; CreationDate = $fixtureStarted.AddSeconds($(if ($mode -eq 'recycled-parent') { -1 } else { 1 })) }
    } elseif ($parent -eq 202) {
        if ($mode -eq 'cycle') { [pscustomobject]@{ ProcessId = 101; CreationDate = $fixtureStarted.AddSeconds(2) } }
        else { [pscustomobject]@{ ProcessId = 303; CreationDate = $fixtureStarted.AddSeconds($(if ($mode -eq 'recycled-child') { 0 } else { 2 })) } }
    }
}
$cases = @()
foreach ($mode in @('valid', 'recycled-parent', 'recycled-child', 'cycle', 'foreign')) {
    $script:queries = 0
    $ownership = [pscustomobject]@{ launcherProcessId = 101; launcherStartedAtUtc = $fixtureStarted; processId = 0; processStartedAtUtc = $null; baseUrl = $null }
    $discovery = [pscustomobject]@{ processId = $(if ($mode -eq 'foreign') { 404 } else { 303 }); host = '127.0.0.1'; port = 12345 }
    $failure = $null
    try { Set-OwnedClientDiscovery $ownership $discovery } catch { $failure = $_.Exception.Message }
    if ($mode -eq 'valid') {
        if ($failure -or $ownership.processId -ne 303 -or $ownership.baseUrl -ne 'http://127.0.0.1:12345') { throw 'Valid descendant was rejected' }
    } elseif (-not $failure -or $failure -notlike '*does not belong*' -or $ownership.processId -ne 0 -or $ownership.baseUrl) {
        throw "$mode authorized an unrelated process or failed without the expected diagnosis"
    }
    $cases += [pscustomobject]@{ mode = $mode; ok = $true; queries = $script:queries }
}
[pscustomobject]@{ ok = $true; cases = $cases }
