param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [ValidateSet('forge')]
    [string]$Loader = 'forge',
    [Parameter(Mandatory = $true)]
    [string]$Suite,
    [Parameter(Mandatory = $true)]
    [string]$PrepareTest,
    [Parameter(Mandatory = $true)]
    [string]$VerifyTest,
    [string]$WorldName = 'Dev Client Automation Void Platform',
    [string]$ArtifactDirectory = (Join-Path $WorkspaceRoot 'build\reports\dev-client-regression\restart'),
    [int]$TimeoutSeconds = 300,
    [string]$CandidateIdentity,
    [string]$RuntimeBuildIdentity
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\client-lifecycle.ps1"

function Write-RestartRecord {
    param([Parameter(Mandatory = $true)] [object]$Value, [Parameter(Mandatory = $true)] [string]$Path)

    New-Item -ItemType Directory -Force -Path $ArtifactDirectory | Out-Null
    $Value | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath $Path -Encoding utf8
}

function Start-RestartClient {
    param(
        [Parameter(Mandatory = $true)] [string]$OwnershipPath,
        [Parameter(Mandatory = $true)] [string]$LifecyclePath
    )

    try {
        return & (Join-Path $PSScriptRoot 'start-and-ready.ps1') -WorkspaceRoot $WorkspaceRoot -Loader $Loader `
            -WorldName $WorldName -MinimalRuntime -SkipRecipeViewerReady -CloseOnExit -TimeoutSeconds $TimeoutSeconds
    } catch {
        # start-and-ready.ps1 supplies the ownership record and its cleanup under
        # these keys when startup fails after it creates the dedicated launcher.
        if ($_.Exception.Data.Contains('ClientOwnership')) {
            Write-RestartRecord -Value $_.Exception.Data['ClientOwnership'] -Path $OwnershipPath
        }
        if ($_.Exception.Data.Contains('ClientLifecycle')) {
            Write-RestartRecord -Value $_.Exception.Data['ClientLifecycle'] -Path $LifecyclePath
        }
        throw
    }
}

function Invoke-RestartPhase {
    param(
        [Parameter(Mandatory = $true)] [string]$Phase,
        [Parameter(Mandatory = $true)] [object]$Client,
        [AllowEmptyString()] [string]$Token
    )

    $captured = [System.Collections.Generic.List[object]]::new()
    $phaseParameters = @{
        Command = 'run'; WorkspaceRoot = $WorkspaceRoot; Suite = $Suite; Test = $Phase
        BaseUrl = $Client.baseUrl; ArtifactDirectory = (Join-Path $ArtifactDirectory $Phase)
        TimeoutSeconds = $TimeoutSeconds; CandidateIdentity = $CandidateIdentity; RuntimeBuildIdentity = $RuntimeBuildIdentity
    }
    if (-not [string]::IsNullOrEmpty($Token)) { $phaseParameters.PreparationToken = $Token }

    try {
        & (Join-Path $PSScriptRoot 'regression.ps1') @phaseParameters *>&1 | ForEach-Object { [void]$captured.Add($_) }
    } catch {
        # regression.ps1 owns the phase JSON/JUnit and has already sanitized its
        # exception. Do not forward captured streams because the raw token is
        # intentionally private to this controller.
    }
    $records = @($captured | Where-Object {
            $_ -isnot [System.Management.Automation.ErrorRecord] -and
            $_ -isnot [System.Management.Automation.WarningRecord] -and
            $_ -isnot [System.Management.Automation.VerboseRecord] -and
            $_ -isnot [System.Management.Automation.DebugRecord] -and
            $_ -isnot [System.Management.Automation.InformationRecord] -and
            $_ -isnot [System.Management.Automation.ProgressRecord]
        })
    if ($records.Count -ne 1 -or $records[0].suiteId -ne $Suite -or $records[0].testId -ne $Phase -or
            $records[0].outcome -eq 'running' -or -not $records[0].ok) {
        throw "Restart $Phase phase failed with $($records.Count) native product records; inspect its native regression artifacts."
    }
    return $records[0]
}

$prepareClient = $null
$verifyClient = $null
$prepareLifecycle = $null
$verifyLifecycle = $null
$runId = [guid]::NewGuid().ToString()
$prepareOwnershipPath = Join-Path $ArtifactDirectory "$runId-prepare-ownership.json"
$verifyOwnershipPath = Join-Path $ArtifactDirectory "$runId-verify-ownership.json"
$prepareLifecyclePath = Join-Path $ArtifactDirectory "$runId-prepare-lifecycle.json"
$verifyLifecyclePath = Join-Path $ArtifactDirectory "$runId-verify-lifecycle.json"

if (Test-Path -LiteralPath $ArtifactDirectory) { throw "Restart regression requires a new artifact directory: $ArtifactDirectory" }
New-Item -ItemType Directory -Path $ArtifactDirectory | Out-Null

try {
    $prepareClient = Start-RestartClient -OwnershipPath $prepareOwnershipPath -LifecyclePath $prepareLifecyclePath
    Write-RestartRecord -Value $prepareClient.ownership -Path $prepareOwnershipPath

    $prepare = Invoke-RestartPhase -Phase $PrepareTest -Client $prepareClient
    $rawStatus = Invoke-RestMethod -Method Get -Uri "$($prepareClient.baseUrl)/regression/status" -TimeoutSec 15
    $preparationToken = $rawStatus.diagnostics.restartPreparationToken
    if ([string]::IsNullOrEmpty($preparationToken)) { throw 'Restart preparation did not publish a token through native status.' }

    $prepareLifecycle = Stop-OwnedClient -Ownership $prepareClient.ownership -WaitForRegression:$false -TimeoutSeconds $TimeoutSeconds
    Write-RestartRecord -Value $prepareLifecycle -Path $prepareLifecyclePath
    $prepareClient = $null
    if (-not $prepareLifecycle.ok) { throw 'Owned prepare client cleanup failed.' }

    $verifyClient = Start-RestartClient -OwnershipPath $verifyOwnershipPath -LifecyclePath $verifyLifecyclePath
    Write-RestartRecord -Value $verifyClient.ownership -Path $verifyOwnershipPath
    if ($verifyClient.processId -eq $prepareLifecycle.ownership.processId -and
            $verifyClient.ownership.processStartedAtUtc -eq $prepareLifecycle.ownership.processStartedAtUtc) {
        throw 'Restart verification requires a distinct owned client process.'
    }

    $verify = Invoke-RestartPhase -Phase $VerifyTest -Client $verifyClient -Token $preparationToken
    $prepareBoot = $prepare.result.diagnostics.serverBootId
    $prepareCheckpointBoot = $prepare.result.diagnostics.restartPreparationBootId
    $verifyBoot = $verify.result.diagnostics.serverBootId
    $verifyCheckpointBoot = $verify.result.diagnostics.restartPreparationBootId
    if ([string]::IsNullOrEmpty($prepareBoot) -or [string]::IsNullOrEmpty($prepareCheckpointBoot) -or
            [string]::IsNullOrEmpty($verifyBoot) -or [string]::IsNullOrEmpty($verifyCheckpointBoot) -or
            $prepareBoot -ne $prepareCheckpointBoot -or $prepareBoot -ne $verifyCheckpointBoot -or $verifyBoot -eq $prepareBoot) {
        throw 'Restart phases did not retain distinct native server-boot provenance.'
    }
    [pscustomobject]@{
        ok = $true
        worldName = $WorldName
        prepareServerBootId = $prepareBoot
        prepareRestartPreparationBootId = $prepareCheckpointBoot
        verifyServerBootId = $verifyBoot
        verifyRestartPreparationBootId = $verifyCheckpointBoot
        prepare = [pscustomobject]@{ suiteId = $prepare.suiteId; testId = $prepare.testId; outcome = $prepare.outcome; runId = $prepare.runId; json = $prepare.json; junit = $prepare.junit }
        verify = [pscustomobject]@{ suiteId = $verify.suiteId; testId = $verify.testId; outcome = $verify.outcome; runId = $verify.runId; json = $verify.json; junit = $verify.junit }
        prepareOwnership = $prepareOwnershipPath
        verifyOwnership = $verifyOwnershipPath
        prepareLifecycle = $prepareLifecyclePath
        verifyLifecycle = $verifyLifecyclePath
        prepareProcessId = $prepareLifecycle.ownership.processId
        prepareProcessStartedAtUtc = $prepareLifecycle.ownership.processStartedAtUtc
        verifyProcessId = $verifyClient.processId
        verifyProcessStartedAtUtc = $verifyClient.ownership.processStartedAtUtc
    }
} finally {
    if ($null -ne $prepareClient) {
        $prepareLifecycle = Stop-OwnedClient -Ownership $prepareClient.ownership -WaitForRegression:$false -TimeoutSeconds $TimeoutSeconds
        Write-RestartRecord -Value $prepareLifecycle -Path $prepareLifecyclePath
        if (-not $prepareLifecycle.ok) { throw 'Owned prepare client cleanup failed.' }
    }
    if ($null -ne $verifyClient) {
        $verifyLifecycle = Stop-OwnedClient -Ownership $verifyClient.ownership -WaitForRegression:$false -TimeoutSeconds $TimeoutSeconds
        Write-RestartRecord -Value $verifyLifecycle -Path $verifyLifecyclePath
        if (-not $verifyLifecycle.ok) { throw 'Owned verify client cleanup failed.' }
    }
}
