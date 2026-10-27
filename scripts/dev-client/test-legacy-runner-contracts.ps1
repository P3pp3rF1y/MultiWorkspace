param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path)

$ErrorActionPreference = 'Stop'
function Assert-True { param([bool]$Condition, [string]$Message) if (-not $Condition) { throw $Message } }
function Import-RunnerFunction {
    param([string]$Script, [string]$Name)
    $tokens = $null; $errors = $null
    $ast = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $Script), [ref]$tokens, [ref]$errors)
    Assert-True ($errors.Count -eq 0) "Could not parse $Script."
    $function = $ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq $Name }, $true)
    Assert-True ($null -ne $function) "Missing $Name in $Script."
    return [scriptblock]::Create($function.Extent.Text)
}
. (Import-RunnerFunction run-backpack-regression.ps1 Invoke-BridgeJson)
$TimeoutSeconds = 37
$BaseUrl = 'http://fixture.invalid'
$script:requests = [System.Collections.Generic.List[object]]::new()
function Invoke-RestMethod {
    param($Method, $Uri, $TimeoutSec, $ContentType, $Body)
    [void]$script:requests.Add(@{ method = $Method; uri = $Uri; timeout = $TimeoutSec; body = $Body })
    return @{ ok = $true }
}
Invoke-BridgeJson -Method Get -Path /state | Out-Null
Invoke-BridgeJson -Method Post -Path /client/stop | Out-Null
Invoke-BridgeJson -Method Post -Path /proof -Body @{ value = 1 } | Out-Null
Assert-True ($script:requests.Count -eq 3 -and @($script:requests | Where-Object { $_.timeout -ne 37 }).Count -eq 0) 'A bridge request ignored the configured timeout.'

. (Import-RunnerFunction run-storage-regression.ps1 Wait-AutomationClientStopped)
$script:alive = $true
$script:killed = $false
$clientProcessId = 123
$TimeoutSeconds = 0
function Get-Process { param($Id, $ErrorAction) if ($script:alive) { return @{ Id = $Id } } }
function Start-Sleep { param($Milliseconds, $Seconds) }
function Stop-Process { param($Id, [switch]$Force, $ErrorAction) $script:killed = $true; $script:alive = $false }
$failed = $false
try { Wait-AutomationClientStopped } catch { $failed = $_.Exception.Message -like '*Timed out waiting for dev client to stop*' }
Assert-True ($failed -and -not $script:killed -and $script:alive) 'A shutdown timeout was hidden or force-killed a bare PID.'
$script:alive = $false
Wait-AutomationClientStopped
[pscustomobject]@{ ok = $true; scenarios = 5 }
