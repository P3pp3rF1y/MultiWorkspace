param(
    [Parameter(Position = 0)]
    [ValidateSet("run", "status", "catalog", "cancel")]
    [string]$Command = "run",
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [Parameter(Mandatory = $false)]
    [string]$Suite,
    [Parameter(Mandatory = $false)]
    [string]$Test,
    [string]$BaseUrl,
    [string]$GameDirectory = "",
    [string]$OptionsSeed = "",
    [string]$ConformanceFailureAction,
    [string]$ActionFailureAfter,
    [string]$PreparationToken,
    [switch]$CleanupFailure,
    [switch]$KeepClientOpen,
    [int]$TimeoutSeconds = 180,
    [string]$ArtifactDirectory = (Join-Path $WorkspaceRoot "build\reports\dev-client-regression"),
    [string]$CandidateIdentity,
    [string]$RuntimeBuildIdentity
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\client-lifecycle.ps1"
$ownedClient = $null
$invocationError = $null
$runner = [pscustomobject]@{
    script = "scripts/dev-client/regression.ps1"
    version = 1
    procedure = "catalog-selected-test-sequential-http-execution"
}
$runIdentity = [pscustomobject]@{
    controllerRunId = [guid]::NewGuid().ToString()
    startedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
}
$launchIdentity = $null
$scriptInvocation = $MyInvocation
$secretValues = [System.Collections.Generic.List[string]]::new()

function Add-SensitiveValues {
    param([object]$Value, [switch]$Sensitive)

    if ($null -eq $Value) { return }
    if ($Value -is [string] -or $Value.GetType().IsValueType) {
        if ($Sensitive -and -not [string]::IsNullOrEmpty([string]$Value) -and -not $secretValues.Contains([string]$Value)) {
            $secretValues.Add([string]$Value)
        }
        return
    }
    if ($Value -is [System.Collections.IDictionary]) {
        foreach ($entry in $Value.GetEnumerator()) {
            Add-SensitiveValues -Value $entry.Value -Sensitive:($Sensitive -or ([string]$entry.Key -match "(?i)(token|secret|password|key)"))
        }
        return
    }
    if ($Value -is [pscustomobject]) {
        foreach ($property in $Value.PSObject.Properties) {
            Add-SensitiveValues -Value $property.Value -Sensitive:($Sensitive -or ($property.Name -match "(?i)(token|secret|password|key)"))
        }
        return
    }
    if ($Value -is [System.Collections.IEnumerable] -and $Value -isnot [string]) {
        foreach ($item in $Value) { Add-SensitiveValues -Value $item -Sensitive:$Sensitive }
        return
    }
    if ($Sensitive -and -not [string]::IsNullOrEmpty([string]$Value) -and -not $secretValues.Contains([string]$Value)) {
        $secretValues.Add([string]$Value)
    }
}

function ConvertTo-SanitizedText {
    param([AllowNull()][object]$Value)

    if ($null -eq $Value) { return $null }
    $text = [string]$Value
    if ($secretValues.Count -gt 0) {
        foreach ($secret in @($secretValues | Sort-Object Length -Descending)) { $text = $text.Replace($secret, "[REDACTED]") }
    }
    if (-not $text.Contains(":") -and -not $text.Contains("=")) { return $text }
    $sensitiveName = "(?:api[-_]?key|access[-_]?token|token|secret|password|key)"
    if ($text.Contains("://") -and $text.Contains("@")) {
        $text = [regex]::Replace($text, "(?i)([a-z][a-z0-9+.-]*://)[^/@\s]+@", '${1}[REDACTED]@')
    }
    if ($text -match "(?i)(token|secret|password|key)") {
        $text = [regex]::Replace($text, "(?i)([`"']?$sensitiveName[`"']?\s*:\s*[`"'])[^`"']*([`"'])", '${1}[REDACTED]${2}')
        $text = [regex]::Replace($text, "(?i)((?<![\w-])$sensitiveName(?![\w-])\s*(?:=|:)\s*)(?:`"[^`"]*`"|'[^']*'|[^\s,;\)\}\]\&]+)", '${1}[REDACTED]')
    }
    if (-not $text.Contains("=")) { return $text }
    return [regex]::Replace($text, "([?&])([^?&=]+)=([^&]*)", [System.Text.RegularExpressions.MatchEvaluator]{
            param($match)
            $name = [uri]::UnescapeDataString($match.Groups[2].Value)
            if ($name -match "(?i)(token|secret|password|key)") {
                return "$($match.Groups[1].Value)$($match.Groups[2].Value)=[REDACTED]"
            }
            return $match.Value
        })
}

function Test-PowerShellStreamRecord {
    param([object]$Value)

    return $Value -is [System.Management.Automation.ErrorRecord] -or
        $Value -is [System.Management.Automation.WarningRecord] -or
        $Value -is [System.Management.Automation.VerboseRecord] -or
        $Value -is [System.Management.Automation.DebugRecord] -or
        $Value -is [System.Management.Automation.InformationRecord] -or
        $Value -is [System.Management.Automation.ProgressRecord]
}

function ConvertTo-SanitizedStreamDiagnostic {
    param([Parameter(Mandatory = $true)] [object]$Record)

    if ($Record -is [System.Management.Automation.ErrorRecord]) {
        $message = if (-not [string]::IsNullOrWhiteSpace($Record.ErrorDetails.Message)) { $Record.ErrorDetails.Message } else { $Record.Exception.Message }
        return [pscustomobject]@{ stream = "error"; message = ConvertTo-SanitizedText $message }
    }
    if ($Record -is [System.Management.Automation.InformationRecord]) {
        return [pscustomobject]@{ stream = "information"; message = ConvertTo-SanitizedText $Record.MessageData }
    }
    if ($Record -is [System.Management.Automation.ProgressRecord]) {
        return [pscustomobject]@{ stream = "progress"; message = ConvertTo-SanitizedText $Record.StatusDescription }
    }
    $stream = if ($Record -is [System.Management.Automation.WarningRecord]) { "warning" } elseif ($Record -is [System.Management.Automation.VerboseRecord]) { "verbose" } else { "debug" }
    return [pscustomobject]@{ stream = $stream; message = ConvertTo-SanitizedText $Record.Message }
}

function Write-SanitizedDiagnostic {
    param([Parameter(Mandatory = $true)] [object]$Diagnostic, [string]$Prefix = "Child")

    $message = ConvertTo-SanitizedText "$Prefix $($Diagnostic.stream): $($Diagnostic.message)"
    switch ($Diagnostic.stream) {
        "information" { Write-Information $message; break }
        "verbose" { Write-Verbose $message; break }
        "debug" { Write-Debug $message; break }
        default { Write-Warning $message }
    }
}

function Copy-SanitizedTelemetry {
    param([object]$Value, [switch]$Nested)

    if ($null -eq $Value) { return $null }
    # Discover secrets across the entire root before copying, including later siblings.
    if (-not $Nested) { Add-SensitiveValues $Value }
    if ($Value -is [string]) { return ConvertTo-SanitizedText $Value }
    if ($Value.GetType().IsValueType) { return $Value }
    if ($Value -is [System.Collections.IDictionary]) {
        $copy = [ordered]@{}
        foreach ($entry in $Value.GetEnumerator()) {
            if ([string]$entry.Key -notmatch "(?i)(token|secret|password|key)") { $copy[$entry.Key] = Copy-SanitizedTelemetry $entry.Value -Nested }
        }
        return $copy
    }
    if ($Value -is [pscustomobject]) {
        $copy = [ordered]@{}
        foreach ($property in $Value.PSObject.Properties) {
            if ($property.Name -notmatch "(?i)(token|secret|password|key)") { $copy[$property.Name] = Copy-SanitizedTelemetry $property.Value -Nested }
        }
        return [pscustomobject]$copy
    }
    if ($Value -is [System.Collections.IEnumerable] -and $Value -isnot [string]) {
        return ,@(foreach ($item in $Value) { Copy-SanitizedTelemetry $item -Nested })
    }
    return $Value
}

function Get-InvocationProvenance {
    $boundParameters = [ordered]@{}
    if ($null -ne $scriptInvocation.BoundParameters) {
        Add-SensitiveValues $scriptInvocation.BoundParameters
        foreach ($parameter in @($scriptInvocation.BoundParameters.GetEnumerator() | Sort-Object Key)) {
            $boundParameters[$parameter.Key] = Copy-SanitizedTelemetry $parameter.Value
        }
    }
    $parametersJson = $boundParameters | ConvertTo-Json -Compress -Depth 8
    return [pscustomobject]@{
        source = "sanitized-bound-parameters"
        command = "$($runner.script) $parametersJson"
        boundParameters = $boundParameters
    }
}

function Test-RegressionResultOk {
    param([AllowNull()][object]$Result)

    if ($null -eq $Result) { return $false }
    $okProperty = $Result.PSObject.Properties['ok']
    return $null -ne $okProperty -and [bool]$okProperty.Value
}

function Invoke-RegressionRequest {
    param(
        [Parameter(Mandatory = $true)] [string]$Method,
        [Parameter(Mandatory = $true)] [string]$Path,
        [object]$Body = $null
    )

    $uri = "$BaseUrl$Path"
    try {
        if ($null -eq $Body) {
            return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec 15
        }
        return Invoke-RestMethod -Method $Method -Uri $uri -ContentType "application/json" -Body ($Body | ConvertTo-Json -Depth 64 -Compress) -TimeoutSec 15
    } catch {
        throw (ConvertTo-SanitizedText "Regression $Method request failed: $($_.Exception.Message)")
    }
}

function Format-Seconds {
    param([double]$Milliseconds)
    return ($Milliseconds / 1000).ToString("0.######", [System.Globalization.CultureInfo]::InvariantCulture)
}

function Write-JunitResult {
    param([Parameter(Mandatory = $true)] [object[]]$Results, [Parameter(Mandatory = $true)] [string]$Path,
        [double]$DurationMillis = 0, [switch]$IncludeConformance, [object[]]$NotRun = @())

    $document = New-Object System.Xml.XmlDocument
    $declaration = $document.CreateXmlDeclaration("1.0", "UTF-8", $null)
    [void]$document.AppendChild($declaration)
    $suite = $document.CreateElement("testsuite")
    $suite.SetAttribute("name", $Results[0].suiteId)
    $cases = @()
    foreach ($result in $Results) {
        if ($null -eq $result.reportCases) { throw "Runtime does not expose the shared regression result model." }
        $cases += @($result.reportCases)
    }
    foreach ($notRun in $NotRun) {
        $cases += @($notRun.reportCases)
    }
    $suite.SetAttribute("tests", [string]$cases.Count)
    $suite.SetAttribute("failures", [string]@($cases | Where-Object { $_.junitElement -eq "failure" }).Count)
    $suite.SetAttribute("errors", [string]@($cases | Where-Object { $_.junitElement -eq "error" }).Count)
    $suite.SetAttribute("skipped", [string]@($cases | Where-Object { $_.junitElement -eq "skipped" }).Count)
    $suite.SetAttribute("time", (Format-Seconds $(if ($DurationMillis -gt 0) { $DurationMillis } else { $Results[0].durationMillis })))
    foreach ($caseResult in $cases) {
        $case = $document.CreateElement("testcase")
        $case.SetAttribute("classname", $caseResult.classname)
        $case.SetAttribute("name", $caseResult.testId)
        $case.SetAttribute("time", (Format-Seconds $caseResult.durationMillis))
        if (-not [string]::IsNullOrEmpty($caseResult.junitElement)) {
            if ($caseResult.junitElement -notin @("failure", "error", "skipped")) { throw "Invalid shared JUnit element." }
            $detail = $document.CreateElement($caseResult.junitElement)
            $detail.SetAttribute("type", $caseResult.category)
            $detail.SetAttribute("message", (ConvertTo-SanitizedText $caseResult.message))
            $detail.InnerText = ConvertTo-SanitizedText ($caseResult.diagnostics | ConvertTo-Json -Depth 24)
            [void]$case.AppendChild($detail)
        }
        $output = $document.CreateElement("system-out")
        $output.InnerText = ConvertTo-SanitizedText ($caseResult.diagnostics | ConvertTo-Json -Depth 24)
        [void]$case.AppendChild($output)
        [void]$suite.AppendChild($case)
    }
    [void]$document.AppendChild($suite)
    $document.Save($Path)
}

function Get-RegressionAccounting {
    param([int]$Selected, [object[]]$Cases)

    $attempted = $Cases.Count
    $passed = @($Cases | Where-Object { $_.outcome -eq "passed" }).Count
    $skipped = @($Cases | Where-Object { $_.outcome -eq "skipped" }).Count
    return [pscustomobject]@{
        selected = $Selected
        attempted = $attempted
        passed = $passed
        failed = @($Cases | Where-Object { $_.outcome -eq "failed" }).Count
        error = @($Cases | Where-Object { $_.outcome -eq "error" }).Count
        blocked = @($Cases | Where-Object { $_.outcome -eq "blocked" }).Count
        cancelled = @($Cases | Where-Object { $_.outcome -eq "cancelled" }).Count
        skipped = $skipped
        notRun = $Selected - $attempted
    }
}

function New-RegressionReportContext {
    param([string]$SuiteId, [string]$TestId, [object]$Telemetry)

    $rerun = "pwsh -NoProfile -File '$($PSScriptRoot.Replace("'", "''"))/regression.ps1' -Command run -WorkspaceRoot '$($WorkspaceRoot.Replace("'", "''"))' -Suite '$SuiteId' -Test '$TestId'"
    if ($scriptInvocation.BoundParameters.ContainsKey('BaseUrl') -and -not [string]::IsNullOrWhiteSpace($BaseUrl)) {
        $rerun += " -BaseUrl '$($BaseUrl.Replace("'", "''"))'"
    }
    if ($KeepClientOpen) { $rerun += ' -KeepClientOpen' }
    if (-not [string]::IsNullOrWhiteSpace($GameDirectory)) { $rerun += " -GameDirectory '$($GameDirectory.Replace("'", "''"))'" }
    if (-not [string]::IsNullOrWhiteSpace($OptionsSeed)) { $rerun += " -OptionsSeed '$($OptionsSeed.Replace("'", "''"))'" }
    foreach ($option in @("ConformanceFailureAction", "ActionFailureAfter", "PreparationToken")) {
        $value = Get-Variable -Name $option -ValueOnly
        if (-not [string]::IsNullOrWhiteSpace($value)) { $rerun += " -$option '$($value.Replace("'", "''"))'" }
    }
    if ($CleanupFailure) { $rerun += " -CleanupFailure" }
    $rerun += " -TimeoutSeconds $TimeoutSeconds -ArtifactDirectory '$(([System.IO.Path]::GetFullPath($ArtifactDirectory)).Replace("'", "''"))'"
    $artifactBase = "$SuiteId-$TestId" -replace "[^A-Za-z0-9_.-]", "_"
    return @{
        runtimeIdentity = @{ loader = [string]$Telemetry.launchIdentity.loader; minecraftVersion = [string]$Telemetry.launchIdentity.minecraftVersion;
            processId = 0; controller = (Copy-SanitizedTelemetry $Telemetry) }
        rerunCommand = $rerun
        artifacts = @{ json = [System.IO.Path]::GetFullPath((Join-Path $ArtifactDirectory "$artifactBase.json"));
            junit = [System.IO.Path]::GetFullPath((Join-Path $ArtifactDirectory "$artifactBase.junit.xml")) }
    }
}

function New-ControllerRegressionResult {
    param([ValidateSet("ERROR", "NOT_RUN")] [string]$Kind, [string]$SuiteId, [string]$TestId, [string]$Message,
        [object]$Telemetry, [object]$Failure = $null, [string[]]$EffectiveRequirements = @())

    if (-not $script:resultToolReady) {
        $buildOutput = @(& (Join-Path $WorkspaceRoot "gradlew.bat") -p $WorkspaceRoot :DevClientAutomation:regressionResultTool --console=plain 2>&1)
        if ($LASTEXITCODE -ne 0) { throw "Could not build the canonical regression result serializer: $($buildOutput -join [Environment]::NewLine)" }
        $script:resultToolReady = $true
    }
    $request = @{ kind = $Kind; suiteId = $SuiteId; testId = $TestId; message = $Message; failure = $Failure;
        effectiveRequirements = @($EffectiveRequirements); context = (New-RegressionReportContext $SuiteId $TestId $Telemetry) }
    $serialized = $request | ConvertTo-Json -Depth 64 -Compress
    $output = $serialized | & java -jar (Join-Path $WorkspaceRoot "DevClientAutomation/build/tools/regression-result-tool.jar")
    if ($LASTEXITCODE -ne 0) { throw "Canonical regression result serialization failed: $output" }
    return $output | ConvertFrom-Json
}

function Write-RegressionCaseArtifact {
    param(
        [Parameter(Mandatory = $true)] [object]$Result,
        [Parameter(Mandatory = $true)] [object]$Telemetry
    )

    New-Item -ItemType Directory -Force -Path $ArtifactDirectory | Out-Null
    $artifactBase = "$($Result.suiteId)-$($Result.testId)" -replace "[^A-Za-z0-9_.-]", "_"
    $jsonPath = Join-Path $ArtifactDirectory "$artifactBase.json"
    $junitPath = Join-Path $ArtifactDirectory "$artifactBase.junit.xml"
    $safeTelemetry = Copy-SanitizedTelemetry $Telemetry
    $safeResult = Copy-SanitizedTelemetry $Result
    [pscustomobject]@{ telemetry = $safeTelemetry; result = $safeResult } | ConvertTo-Json -Depth 64 | Set-Content -LiteralPath $jsonPath -Encoding utf8
    Write-JunitResult -Results @($safeResult) -Path $junitPath -IncludeConformance
    return [pscustomobject]@{
        ok = $safeResult.ok
        outcome = $safeResult.outcome
        outcomePriority = $safeResult.outcomePriority
        suiteId = $safeResult.suiteId
        testId = $safeResult.testId
        runId = $safeResult.runId
        durationMillis = $safeResult.durationMillis
        testDurationMillis = $safeResult.testDurationMillis
        json = $jsonPath
        junit = $junitPath
        result = $safeResult
    }
}

function New-ExceptionalRegressionCase {
    param(
        [Parameter(Mandatory = $true)] [string]$SuiteId,
        [Parameter(Mandatory = $true)] [string]$TestId,
        [Parameter(Mandatory = $true)] [object]$Telemetry,
        [Parameter(Mandatory = $true)] [System.Management.Automation.ErrorRecord]$ErrorRecord
    )

    $message = ConvertTo-SanitizedText "Regression invocation failed: $($ErrorRecord.Exception.Message)"
    $failure = @{ category = "infrastructure"; message = $message; exceptionType = $ErrorRecord.Exception.GetType().FullName;
        stackTrace = ConvertTo-SanitizedText "$($ErrorRecord.Exception.ToString())`n$($ErrorRecord.ScriptStackTrace)" }
    $result = New-ControllerRegressionResult -Kind ERROR -SuiteId $SuiteId -TestId $TestId -Message $message -Telemetry $Telemetry -Failure $failure
    return Write-RegressionCaseArtifact -Result $result -Telemetry $Telemetry
}

function Assert-Accounting {
    param([Parameter(Mandatory = $true)] [object]$Accounting)

    if ($Accounting.selected -lt 0 -or $Accounting.attempted -lt 0 -or $Accounting.passed -lt 0 -or
            $Accounting.failed -lt 0 -or $Accounting.skipped -lt 0 -or $Accounting.notRun -lt 0 -or
            $Accounting.selected -ne ($Accounting.passed + $Accounting.failed + $Accounting.error + $Accounting.blocked + $Accounting.cancelled + $Accounting.skipped + $Accounting.notRun) -or
            $Accounting.attempted -ne ($Accounting.passed + $Accounting.failed + $Accounting.error + $Accounting.blocked + $Accounting.cancelled + $Accounting.skipped)) {
        throw (ConvertTo-SanitizedText "Internal regression accounting inconsistency: $($Accounting | ConvertTo-Json -Compress)")
    }
}

function Write-ExceptionalRegressionReport {
    param(
        [Parameter(Mandatory = $true)] [string]$SuiteId,
        [Parameter(Mandatory = $true)] [string]$TestId,
        [Parameter(Mandatory = $true)] [int]$Selected,
        [Parameter(Mandatory = $true)] [object]$Selection,
        [Parameter(Mandatory = $true)] [System.Management.Automation.ErrorRecord]$ErrorRecord
    )

    $telemetry = [pscustomobject]@{
        runner = $runner
        runIdentity = $runIdentity
        launchIdentity = $launchIdentity
        bindings = [pscustomobject]@{ candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity }
        selection = $Selection
    }
    $case = New-ExceptionalRegressionCase -SuiteId $SuiteId -TestId $TestId -Telemetry $telemetry -ErrorRecord $ErrorRecord
    $accounting = Get-RegressionAccounting -Selected $Selected -Cases $(if ($Selected -gt 0) { @($case) } else { @() })
    Assert-Accounting -Accounting $accounting
    $runIdentity | Add-Member -NotePropertyName runtimeRunIds -NotePropertyValue $(if ($Selected -gt 0) { @($case.runId) } else { @() }) -Force
    $runIdentity | Add-Member -NotePropertyName finishedAtUtc -NotePropertyValue ((Get-Date).ToUniversalTime().ToString("o")) -Force
    $report = [pscustomobject]@{
        ok = $false
        outcome = "error"
        suiteId = $SuiteId
        testId = $TestId
        json = $case.json
        junit = $case.junit
        result = $case.result
        runner = $runner
        runIdentity = $runIdentity
        launchIdentity = $launchIdentity
        bindings = $telemetry.bindings
        selection = $Selection
        accounting = $accounting
        case = $case
        error = ConvertTo-SanitizedText $ErrorRecord.Exception.Message
    }
    $report = Copy-SanitizedTelemetry $report
    $report | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath $case.json -Encoding utf8
    $report
}

function Invoke-RegressionTest {
    param([string]$SuiteId, [string]$TestId, [object]$Telemetry)

    $request = @{ suiteId = $SuiteId; testId = $TestId; actionFailureAfter = $ActionFailureAfter; cleanupFailure = [bool]$CleanupFailure;
        preparationToken = $PreparationToken; reportContext = (New-RegressionReportContext $SuiteId $TestId $Telemetry) }
    if ($scriptInvocation.BoundParameters.ContainsKey("ConformanceFailureAction")) {
        $request.conformanceFailureAction = $ConformanceFailureAction
    }
    $result = Invoke-RegressionRequest -Method Post -Path "/regression/run" -Body $request
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ($result.outcome -eq "running" -and (Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 250
        $result = Invoke-RegressionRequest -Method Get -Path "/regression/status"
    }
    if ($result.outcome -eq "running") {
        $result = Invoke-RegressionRequest -Method Post -Path "/regression/cancel" -Body @{}
        $result = Wait-ForTerminalRegressionResult -InitialResult $result -Operation "Regression $SuiteId/$TestId cancellation/cleanup"
    }

    Add-SensitiveValues $result
    return Write-RegressionCaseArtifact -Result $result -Telemetry $Telemetry
}

function Wait-ForTerminalRegressionResult {
    param(
        [Parameter(Mandatory = $true)] [object]$InitialResult,
        [Parameter(Mandatory = $true)] [string]$Operation
    )

    $result = $InitialResult
    $cleanupDeadline = (Get-Date).AddSeconds(60)
    while ($result.outcome -eq "running" -and (Get-Date) -lt $cleanupDeadline) {
        Start-Sleep -Milliseconds 250
        $result = Invoke-RegressionRequest -Method Get -Path "/regression/status"
    }
    if ($result.outcome -eq "running") {
        throw (ConvertTo-SanitizedText "$Operation timed out; recovery journal was retained.")
    }
    return $result
}

Add-SensitiveValues $scriptInvocation.BoundParameters

if ($Command -eq "run" -and [string]::IsNullOrWhiteSpace($Suite)) {
    $selection = [pscustomobject]@{
        invocation = Get-InvocationProvenance
        suiteId = $null
        testIds = @()
        source = "validation"
    }
    try {
        throw "run requires -Suite; optionally supply -Test for a single case."
    } catch {
        Write-ExceptionalRegressionReport -SuiteId "validation" -TestId "missing-suite" -Selected 0 -Selection $selection -ErrorRecord $_
        throw (ConvertTo-SanitizedText $_.Exception.Message)
    }
}

try {
if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    try {
        $previousProgressPreference = $ProgressPreference
        $ProgressPreference = "SilentlyContinue"
        $launchArguments = @{ WorkspaceRoot = $WorkspaceRoot; MinimalRuntime = $true; SkipRecipeViewerReady = $true;
            CloseOnExit = $true; KeepClientOpen = [bool]$KeepClientOpen; TimeoutSeconds = $TimeoutSeconds }
        if (-not [string]::IsNullOrWhiteSpace($GameDirectory)) { $launchArguments.GameDirectory = $GameDirectory }
        if (-not [string]::IsNullOrWhiteSpace($OptionsSeed)) { $launchArguments.OptionsSeed = $OptionsSeed }
        $capturedLaunch = @(& (Join-Path $PSScriptRoot "start-and-ready.ps1") @launchArguments *>&1)
        $launchDiagnostics = @($capturedLaunch | Where-Object { Test-PowerShellStreamRecord $_ } | ForEach-Object { ConvertTo-SanitizedStreamDiagnostic $_ })
        $launchRecords = @($capturedLaunch | Where-Object { -not (Test-PowerShellStreamRecord $_) })
        foreach ($diagnostic in $launchDiagnostics) {
            Write-SanitizedDiagnostic -Diagnostic $diagnostic -Prefix "start-and-ready.ps1"
        }
        $launchErrors = @($launchDiagnostics | Where-Object { $_.stream -eq "error" })
        if ($launchErrors.Count -gt 0) {
            throw (ConvertTo-SanitizedText "start-and-ready.ps1 reported an error: $($launchErrors[0].message)")
        }
        if ($launchRecords.Count -ne 1) {
            throw (ConvertTo-SanitizedText "start-and-ready.ps1 returned $($launchRecords.Count) product records; expected exactly one.")
        }
        $launch = $launchRecords[0]
        $ownedClient = $launch.ownership
    } catch {
        $startupMessage = ConvertTo-SanitizedText "Dev-client startup failed: $($_.Exception.Message)"
        if ($_.Exception.Data.Contains('ClientLifecycle')) {
            $launchIdentity = [pscustomobject]@{ source = 'failed-start-and-ready.ps1'; lifecycle = $_.Exception.Data['ClientLifecycle'] }
        }
        $selection = [pscustomobject]@{
            invocation = Get-InvocationProvenance
            suiteId = $Suite
            testIds = $(if (-not [string]::IsNullOrWhiteSpace($Test)) { @($Test) } else { @() })
            source = $(if (-not [string]::IsNullOrWhiteSpace($Test)) { "controller-supplied-single-test" } else { "startup-before-catalog" })
        }
        Write-ExceptionalRegressionReport -SuiteId $Suite -TestId $(if (-not [string]::IsNullOrWhiteSpace($Test)) { $Test } else { "startup" }) -Selected $(if (-not [string]::IsNullOrWhiteSpace($Test)) { 1 } else { 0 }) -Selection $selection -ErrorRecord $_
        throw $startupMessage
    } finally {
        $ProgressPreference = $previousProgressPreference
    }
    $BaseUrl = $launch.baseUrl
    $launchIdentity = [pscustomobject]@{ source = "start-and-ready.ps1"; baseUrl = (ConvertTo-SanitizedText $launch.baseUrl); processId = $launch.processId }
} else {
    $launchIdentity = [pscustomobject]@{ source = "controller-supplied-baseUrl"; baseUrl = (ConvertTo-SanitizedText $BaseUrl); processId = $null }
}
try {
    $capabilities = Invoke-RegressionRequest -Method Get -Path "/capabilities"
    if ($Command -eq "run" -and -not $capabilities.backgroundInput) {
        throw "Regression execution requires a background-input client. Launch :workspace:runRegressionClient or let this runner start the client."
    }
    $launchIdentity | Add-Member -NotePropertyName loader -NotePropertyValue $capabilities.loader
    $launchIdentity | Add-Member -NotePropertyName minecraftVersion -NotePropertyValue $capabilities.minecraftVersion
    $launchIdentity | Add-Member -NotePropertyName backgroundInput -NotePropertyValue $capabilities.backgroundInput
} catch {
    if ($Command -ne "run") { throw }
    $selection = [pscustomobject]@{ invocation = Get-InvocationProvenance; suiteId = $Suite;
        testIds = $(if ($Test) { @($Test) } else { @() }); source = "capabilities" }
    Write-ExceptionalRegressionReport -SuiteId $Suite -TestId $(if ($Test) { $Test } else { "capabilities" }) `
        -Selected $(if ($Test) { 1 } else { 0 }) -Selection $selection -ErrorRecord $_
    throw
}

if ($Command -eq "catalog") {
    Copy-SanitizedTelemetry (Invoke-RegressionRequest -Method Get -Path "/regression/catalog")
    return
}
if ($Command -eq "status") {
    Copy-SanitizedTelemetry (Invoke-RegressionRequest -Method Get -Path "/regression/status")
    return
}
if ($Command -eq "cancel") {
    $cancelled = Invoke-RegressionRequest -Method Post -Path "/regression/cancel" -Body @{}
    Copy-SanitizedTelemetry (Wait-ForTerminalRegressionResult -InitialResult $cancelled -Operation "Standalone regression cancellation/cleanup")
    return
}
if (-not [string]::IsNullOrWhiteSpace($Test)) {
    $selection = [pscustomobject]@{
        invocation = Get-InvocationProvenance
        suiteId = $Suite
        testIds = @($Test)
        source = "controller-supplied-single-test"
        requestOptions = [pscustomobject]@{
            conformanceFailureAction = $ConformanceFailureAction
            actionFailureAfter = $ActionFailureAfter
            cleanupFailure = [bool]$CleanupFailure
            preparationTokenProvided = -not [string]::IsNullOrWhiteSpace($PreparationToken)
            timeoutSeconds = $TimeoutSeconds
        }
    }
    $caseTelemetry = [pscustomobject]@{
        runner = $runner
        runIdentity = $runIdentity
        launchIdentity = $launchIdentity
        bindings = [pscustomobject]@{ candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity }
        selection = $selection
    }
    try {
        $case = Invoke-RegressionTest -SuiteId $Suite -TestId $Test -Telemetry $caseTelemetry
    } catch {
        $case = New-ExceptionalRegressionCase -SuiteId $Suite -TestId $Test -Telemetry $caseTelemetry -ErrorRecord $_
    }
    $accounting = Get-RegressionAccounting -Selected 1 -Cases @($case)
    Assert-Accounting -Accounting $accounting
    $runIdentity | Add-Member -NotePropertyName runtimeRunIds -NotePropertyValue @($case.runId)
    $runIdentity | Add-Member -NotePropertyName finishedAtUtc -NotePropertyValue ((Get-Date).ToUniversalTime().ToString("o"))
    $report = [pscustomobject]@{
        ok = ($accounting.failed -eq 0 -and $accounting.notRun -eq 0 -and (Test-RegressionResultOk $case))
        outcome = $case.outcome
        suiteId = $case.suiteId
        testId = $case.testId
        json = $case.json
        junit = $case.junit
        result = $case.result
        runner = $runner
        runIdentity = $runIdentity
        launchIdentity = $launchIdentity
        bindings = $caseTelemetry.bindings
        selection = $selection
        accounting = $accounting
        case = $case
    }
    $report = Copy-SanitizedTelemetry $report
    $report | ConvertTo-Json -Depth 64 | Set-Content -LiteralPath $case.json -Encoding utf8
    $report
    if (-not $report.ok) {
        throw (ConvertTo-SanitizedText "Regression $Suite/$Test $($case.outcome): $($case.result.message)")
    }
    return
}

try {
    $catalog = Invoke-RegressionRequest -Method Get -Path "/regression/catalog"
} catch {
    $selection = [pscustomobject]@{
        invocation = Get-InvocationProvenance
        suiteId = $Suite
        testIds = @()
        source = "regression-catalog"
    }
    Write-ExceptionalRegressionReport -SuiteId $Suite -TestId "catalog" -Selected 0 -Selection $selection -ErrorRecord $_
    throw (ConvertTo-SanitizedText "Regression catalog retrieval failed: $($_.Exception.Message)")
}
$testIds = @($catalog.suiteTests.$Suite)
if ($testIds.Count -eq 0 -or [string]::IsNullOrWhiteSpace($testIds[0])) {
    $selection = [pscustomobject]@{
        invocation = Get-InvocationProvenance
        suiteId = $Suite
        testIds = @()
        source = "regression-catalog"
    }
    try {
        throw (ConvertTo-SanitizedText "No regression tests in suite $Suite.")
    } catch {
        Write-ExceptionalRegressionReport -SuiteId $Suite -TestId "empty-suite" -Selected 0 -Selection $selection -ErrorRecord $_
        throw (ConvertTo-SanitizedText $_.Exception.Message)
    }
}
$timer = [System.Diagnostics.Stopwatch]::StartNew()
$cases = @()
$selection = [pscustomobject]@{
    invocation = Get-InvocationProvenance
    suiteId = $Suite
    testIds = $testIds
    source = "regression-catalog"
    requestOptions = [pscustomobject]@{
        conformanceFailureAction = $ConformanceFailureAction
        actionFailureAfter = $ActionFailureAfter
        cleanupFailure = [bool]$CleanupFailure
        preparationTokenProvided = -not [string]::IsNullOrWhiteSpace($PreparationToken)
        timeoutSeconds = $TimeoutSeconds
    }
}
foreach ($testId in $testIds) {
    $caseTelemetry = [pscustomobject]@{
        runner = $runner
        runIdentity = $runIdentity
        launchIdentity = $launchIdentity
        bindings = [pscustomobject]@{ candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity }
        selection = [pscustomobject]@{ invocation = $selection.invocation; suiteId = $Suite; testIds = @($testId); source = "regression-catalog";
            requestOptions = $selection.requestOptions }
    }
    try {
        $cases += Invoke-RegressionTest -SuiteId $Suite -TestId $testId -Telemetry $caseTelemetry
    } catch {
        $cases += New-ExceptionalRegressionCase -SuiteId $Suite -TestId $testId -Telemetry $caseTelemetry -ErrorRecord $_
        break
    }
}
$timer.Stop()
$results = @($cases | ForEach-Object { $_.result })
$allSteps = @()
foreach ($result in $results) {
    foreach ($step in $result.steps) {
        $allSteps += [pscustomobject]@{ testId = $result.testId; action = $step.action; id = $step.id;
            durationMillis = $step.durationMillis; outcome = $step.outcome; provenance = $step.provenance; revision = $step.revision }
    }
    foreach ($conformance in $result.runtimeConformance) {
        foreach ($step in $conformance.steps) {
            $allSteps += [pscustomobject]@{ testId = "$($result.testId) (conformance: $($conformance.testId))"; action = $step.action;
                id = $step.id; durationMillis = $step.durationMillis; outcome = $step.outcome; provenance = $step.provenance; revision = $step.revision }
        }
    }
}
$actionSummary = @($allSteps | Group-Object action | ForEach-Object {
    $times = @($_.Group | ForEach-Object { [double]$_.durationMillis })
    [pscustomobject]@{
        action = $_.Name
        count = $times.Count
        totalMillis = ($times | Measure-Object -Sum).Sum
        averageMillis = ($times | Measure-Object -Average).Average
        maxMillis = ($times | Measure-Object -Maximum).Maximum
    }
} | Sort-Object totalMillis -Descending)
$slowestSteps = @($allSteps | Sort-Object durationMillis -Descending | Select-Object -First 10 testId, action, revision, id, durationMillis, outcome, provenance)
$wallMillis = $timer.Elapsed.TotalMilliseconds
$runMillis = ($cases | Measure-Object -Property durationMillis -Sum).Sum
$caseTimings = @($cases | ForEach-Object {
    $caseSteps = @($_.result.steps; $_.result.runtimeConformance | ForEach-Object { $_.steps })
    $slowest = $caseSteps | Sort-Object durationMillis -Descending | Select-Object -First 1
    [pscustomobject]@{
        testId = $_.testId
        outcome = $_.outcome
        durationMillis = $_.durationMillis
        testDurationMillis = $_.testDurationMillis
        actionDurationMillis = ($caseSteps | Measure-Object -Property durationMillis -Sum).Sum
        slowestAction = $slowest.action
        slowestActionMillis = $slowest.durationMillis
        json = $_.json
        junit = $_.junit
    }
})
$accounting = Get-RegressionAccounting -Selected $testIds.Count -Cases $cases
Assert-Accounting -Accounting $accounting
$notRunCases = @($testIds | Where-Object { @($cases | ForEach-Object { $_.testId }) -notcontains $_ } | ForEach-Object {
    $reason = "Not run after a previous regression invocation failed."
    $notRunTelemetry = [pscustomobject]@{ runner = $runner; runIdentity = $runIdentity; launchIdentity = $launchIdentity;
        bindings = [pscustomobject]@{ candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity }
        selection = [pscustomobject]@{ invocation = $selection.invocation; suiteId = $Suite; testIds = @($_); source = "regression-catalog";
            requestOptions = $selection.requestOptions } }
    $notRunResult = New-ControllerRegressionResult -Kind NOT_RUN -SuiteId $Suite -TestId $_ -Message $reason -Telemetry $notRunTelemetry `
        -EffectiveRequirements @($catalog.testMetadata.$Suite.$_.effectiveRequirements)
    (Write-RegressionCaseArtifact -Result $notRunResult -Telemetry $notRunTelemetry).result
})
$runIdentity | Add-Member -NotePropertyName runtimeRunIds -NotePropertyValue @($cases | ForEach-Object { $_.runId })
$runIdentity | Add-Member -NotePropertyName finishedAtUtc -NotePropertyValue ((Get-Date).ToUniversalTime().ToString("o"))
$report = [pscustomobject]@{
    suiteId = $Suite
    ok = ($accounting.failed -eq 0 -and $accounting.notRun -eq 0 -and @($cases | Where-Object { -not (Test-RegressionResultOk $_) }).Count -eq 0)
    outcome = (@($results; $notRunCases) | Sort-Object outcomePriority -Descending | Select-Object -First 1).outcome
    runner = $runner
    runIdentity = $runIdentity
    launchIdentity = $launchIdentity
    bindings = [pscustomobject]@{ candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity }
    selection = $selection
    accounting = $accounting
    durationMillis = $wallMillis
    caseRunDurationMillis = $runMillis
    cases = $caseTimings
    results = $results
    notRun = $notRunCases
    actionSummary = $actionSummary
    slowestSteps = $slowestSteps
}
$artifactBase = $Suite -replace "[^A-Za-z0-9_.-]", "_"
$jsonPath = Join-Path $ArtifactDirectory "$artifactBase.json"
$junitPath = Join-Path $ArtifactDirectory "$artifactBase.junit.xml"
$report = Copy-SanitizedTelemetry $report
$report | ConvertTo-Json -Depth 64 | Set-Content -LiteralPath $jsonPath -Encoding utf8
Write-JunitResult -Results $results -Path $junitPath -DurationMillis $wallMillis -IncludeConformance -NotRun $notRunCases

Copy-SanitizedTelemetry ([pscustomobject]@{
    ok = $report.ok
    outcome = $report.outcome
    accounting = $report.accounting
    suiteId = $Suite
    durationMillis = $wallMillis
    caseRunDurationMillis = $runMillis
    json = $jsonPath
    junit = $junitPath
    cases = $report.cases
    actionSummary = $actionSummary
})

if (-not $report.ok) {
    throw (ConvertTo-SanitizedText "Regression suite $Suite failed; see $jsonPath and $junitPath.")
}
} catch {
    $invocationError = $_
    throw
} finally {
    if ($null -ne $ownedClient) {
        try {
            $lifecycle = Stop-OwnedClient -Ownership $ownedClient -KeepClientOpen:$KeepClientOpen
            New-Item -ItemType Directory -Force -Path $ArtifactDirectory | Out-Null
            $lifecyclePath = Join-Path $ArtifactDirectory "$($runIdentity.controllerRunId)-client-lifecycle.json"
            Copy-SanitizedTelemetry $lifecycle | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath $lifecyclePath -Encoding utf8
            if (-not $lifecycle.ok) {
                $message = ConvertTo-SanitizedText "Owned-client teardown failed: $($lifecycle.errors -join '; ')"
                try { throw $message } catch {
                    $selection = [pscustomobject]@{ invocation = Get-InvocationProvenance; suiteId = $Suite; testIds = @(); source = 'client-lifecycle' }
                    Write-ExceptionalRegressionReport -SuiteId $(if ($Suite) { $Suite } else { 'controller' }) -TestId 'client-shutdown' -Selected 0 -Selection $selection -ErrorRecord $_
                }
                if ($null -eq $invocationError) { throw $message }
                Write-Warning $message
            }
        } catch {
            if ($null -eq $invocationError) { throw }
            Write-Warning (ConvertTo-SanitizedText "Client teardown also failed: $($_.Exception.Message); original invocation failure retained.")
        }
    }
}
