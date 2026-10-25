param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [ValidateSet("neoforge", "fabric")]
    [string]$Loader = "neoforge",
    [string[]]$Group = @(),
    [string[]]$Suite = @(),
    # Declarative runtime suites share one client; manifest wrappers retain their own lifecycle.
    [string[]]$RegressionSuite = @(),
    [string]$BaseUrl,
    [switch]$KeepClientOpen,
    [string]$ArtifactDirectory = (Join-Path $WorkspaceRoot "build/reports/dev-client-regression"),
    [string]$ReportPath,
    [ValidateSet("fast", "full")]
    [string]$Tier = "fast",
    [int]$TimeoutSeconds = 360,
    [switch]$ContinueOnFailure,
    [switch]$List,
    [string]$CandidateIdentity,
    [string]$RuntimeBuildIdentity,
    [string]$ValidationRunDirectory,
    [string]$ValidationInitScript
)

$ErrorActionPreference = "Stop"
if ([bool]$ValidationRunDirectory -ne [bool]$ValidationInitScript) { throw 'Validation runtime directory and init script must be supplied together.' }
if ($ValidationRunDirectory) {
    $env:DEV_CLIENT_VALIDATION_RUN_DIRECTORY = $ValidationRunDirectory
    $env:DEV_CLIENT_VALIDATION_INIT_SCRIPT = $ValidationInitScript
}
$manifestPath = Join-Path $PSScriptRoot "regression-suites.json"
$tierRanks = @{ fast = 0; full = 1 }
$runner = [pscustomobject]@{
    script = "scripts/dev-client/run-regression.ps1"
    version = 1
    procedure = "manifest-selected-suite-sequential-execution"
}
$runIdentity = [pscustomobject]@{
    controllerRunId = [guid]::NewGuid().ToString()
    startedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
}
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
    foreach ($secret in @($secretValues | Sort-Object Length -Descending)) { $text = $text.Replace($secret, "[REDACTED]") }
    $sensitiveName = "(?:api[-_]?key|access[-_]?token|token|secret|password|key)"
    $text = [regex]::Replace($text, "(?i)([a-z][a-z0-9+.-]*://)[^/@\s]+@", '${1}[REDACTED]@')
    $text = [regex]::Replace($text, "(?i)([`"']?$sensitiveName[`"']?\s*:\s*[`"'])[^`"']*([`"'])", '${1}[REDACTED]${2}')
    $text = [regex]::Replace($text, "(?i)((?<![\w-])$sensitiveName(?![\w-])\s*(?:=|:)\s*)(?:`"[^`"]*`"|'[^']*'|[^\s,;\)\}\]\&]+)", '${1}[REDACTED]')
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

function Copy-SanitizedTelemetry {
    param([object]$Value)

    if ($null -eq $Value) {
        return $null
    }
    Add-SensitiveValues $Value
    if ($Value -is [string]) { return ConvertTo-SanitizedText $Value }
    if ($Value.GetType().IsValueType) { return $Value }
    if ($Value -is [System.Collections.IDictionary]) {
        $copy = [ordered]@{}
        foreach ($entry in $Value.GetEnumerator()) {
            if ([string]$entry.Key -match "(?i)(token|secret|password|key)") {
                continue
            }
            $copy[$entry.Key] = Copy-SanitizedTelemetry $entry.Value
        }
        return $copy
    }
    if ($Value -is [pscustomobject]) {
        $copy = [ordered]@{}
        foreach ($property in $Value.PSObject.Properties) {
            if ($property.Name -match "(?i)(token|secret|password|key)") {
                continue
            }
            $copy[$property.Name] = Copy-SanitizedTelemetry $property.Value
        }
        return [pscustomobject]$copy
    }
    if ($Value -is [System.Collections.IEnumerable] -and $Value -isnot [string]) {
        return ,@($Value | ForEach-Object { Copy-SanitizedTelemetry $_ })
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

function Assert-Accounting {
    param([Parameter(Mandatory = $true)] [object]$Accounting)

    if ($Accounting.selected -lt 0 -or $Accounting.attempted -lt 0 -or $Accounting.passed -lt 0 -or
            $Accounting.failed -lt 0 -or $Accounting.skipped -lt 0 -or $Accounting.notRun -lt 0 -or
            $Accounting.selected -ne ($Accounting.passed + $Accounting.failed + $Accounting.skipped + $Accounting.notRun) -or
            $Accounting.attempted -ne ($Accounting.passed + $Accounting.failed + $Accounting.skipped)) {
        throw (ConvertTo-SanitizedText "Internal regression accounting inconsistency: $($Accounting | ConvertTo-Json -Compress)")
    }
}

function Write-RegressionFailureSummary {
    param([Parameter(Mandatory = $true)] [string]$Message, [object[]]$Results = @(), [int]$Selected = 0, [object]$Selection = $null)

    $accounting = [pscustomobject]@{
        selected = $Selected
        attempted = $Results.Count
        passed = @($Results | Where-Object { $_.outcome -eq "passed" }).Count
        failed = @($Results | Where-Object { $_.outcome -eq "failed" }).Count
        skipped = @($Results | Where-Object { $_.outcome -eq "skipped" }).Count
        notRun = $Selected - $Results.Count
    }
    Assert-Accounting -Accounting $accounting
    $runIdentity | Add-Member -NotePropertyName finishedAtUtc -NotePropertyValue ((Get-Date).ToUniversalTime().ToString("o")) -Force
    $summary = [pscustomobject]@{
        ok = $false
        protocolVersion = if ($null -ne $manifest) { $manifest.protocolVersion } else { $null }
        loader = $Loader
        tier = $Tier
        groups = $Group
        requestedSuites = $Suite
        passed = $accounting.passed
        failed = $accounting.failed
        runner = $runner
        runIdentity = $runIdentity
        bindings = [pscustomobject]@{ candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity }
        selection = $Selection
        accounting = $accounting
        results = $Results
        error = ConvertTo-SanitizedText $Message
    }
    $summary = Copy-SanitizedTelemetry $summary
    $summary
    throw (ConvertTo-SanitizedText $Message)
}

function Test-SuiteMatches {
    param([object]$Candidate)

    if (@($Candidate.loaders) -notcontains $Loader) {
        return $false
    }
    if ($Suite.Count -gt 0) {
        return $Suite -contains $Candidate.id
    }
    if ($tierRanks[$Candidate.tier] -gt $tierRanks[$Tier]) {
        return $false
    }
    if ($Group.Count -eq 0) {
        return $true
    }
    return @($Candidate.groups | Where-Object { $Group -contains $_ }).Count -gt 0
}

function Wait-PreviousAutomationClientStopped {
    $discoveryPath = if ($env:DEV_CLIENT_VALIDATION_RUN_DIRECTORY) {
        Join-Path $env:DEV_CLIENT_VALIDATION_RUN_DIRECTORY 'dev-client-automation.json'
    } else { Join-Path $WorkspaceRoot 'workspace\run\dev-client-automation.json' }
    if (-not (Test-Path -LiteralPath $discoveryPath -PathType Leaf)) {
        return
    }

    $discovery = Get-Content -LiteralPath $discoveryPath -Raw | ConvertFrom-Json
    $processId = [int]$discovery.processId
    if ($processId -le 0) {
        return
    }

    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($null -ne $process -and -not $process.WaitForExit($TimeoutSeconds * 1000)) {
        throw "Timed out waiting for prior dev client process $processId to stop."
    }
}

function Invoke-SharedRegression {
    $runner.procedure = 'runtime-suite-shared-client-sequential-execution'
    # Comma-separated IDs also work with pwsh -File, whose native CLI cannot bind array expressions.
    $RegressionSuite = @($RegressionSuite | ForEach-Object { $_.Split(',') })
    if ($Suite.Count -or $Group.Count -or $List) { throw "RegressionSuite cannot be combined with manifest Suite, Group or List selection." }
    if (@($RegressionSuite | Where-Object { [string]::IsNullOrWhiteSpace($_) }).Count -or @($RegressionSuite | Select-Object -Unique).Count -ne $RegressionSuite.Count) {
        throw "RegressionSuite requires distinct nonempty runtime suite IDs."
    }
    if ($scriptInvocation.BoundParameters.ContainsKey('ReportPath') -and [string]::IsNullOrWhiteSpace($ReportPath)) { throw 'ReportPath requires a nonempty file path.' }
    . "$PSScriptRoot/client-lifecycle.ps1"
    $directory = if ($ReportPath) { Split-Path -Parent $ReportPath } else { Join-Path $ArtifactDirectory $runIdentity.controllerRunId }
    if (-not $directory) { $directory = (Get-Location).Path }
    $null = New-Item -ItemType Directory -Force -Path $directory
    $jsonPath = if ($ReportPath) { $ReportPath } else { Join-Path $directory "run.json" }
    $selection = [pscustomobject]@{ invocation = Get-InvocationProvenance; source = 'explicit-runtime-suites'; selectedSuites = @($RegressionSuite) }
    $results = @(); $ownership = $null; $launchIdentity = $null; $failure = $null
    function Save-SharedReport {
        $accounting = [pscustomobject]@{
            selected = $RegressionSuite.Count; attempted = $results.Count
            passed = @($results | Where-Object outcome -EQ 'passed').Count
            failed = @($results | Where-Object outcome -EQ 'failed').Count
            skipped = @($results | Where-Object outcome -EQ 'skipped').Count
            notRun = $RegressionSuite.Count - $results.Count
        }
        Assert-Accounting $accounting
        $summary = Copy-SanitizedTelemetry ([pscustomobject]@{
            ok = (-not $failure -and $accounting.failed -eq 0 -and $accounting.notRun -eq 0)
            runner = $runner; runIdentity = $runIdentity; selection = $selection
            bindings = [pscustomobject]@{ candidateIdentity = $CandidateIdentity; runtimeBuildIdentity = $RuntimeBuildIdentity }
            launchIdentity = $launchIdentity; accounting = $accounting; results = @($results)
            error = $failure; json = $jsonPath
        })
        $summary | ConvertTo-Json -Depth 64 | Set-Content -LiteralPath $jsonPath -Encoding utf8
        return $summary
    }
    try {
        if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
            $captured = @(& "$PSScriptRoot/start-and-ready.ps1" -WorkspaceRoot $WorkspaceRoot -Loader $Loader -MinimalRuntime -SkipRecipeViewerReady -CloseOnExit -KeepClientOpen:$KeepClientOpen -TimeoutSeconds $TimeoutSeconds *>&1)
            $launchRecords = @($captured | Where-Object { -not (Test-PowerShellStreamRecord $_) })
            $launchErrors = @($captured | Where-Object { $_ -is [System.Management.Automation.ErrorRecord] })
            if ($launchErrors.Count -or $launchRecords.Count -ne 1) { throw "Shared-client launch failed or did not return exactly one launch record: $($launchErrors -join '; ')" }
            $launch = $launchRecords[0]; $ownership = $launch.ownership; $BaseUrl = $launch.baseUrl
            $launchIdentity = [pscustomobject]@{ source = 'start-and-ready.ps1'; baseUrl = $BaseUrl; processId = $launch.processId }
            $ownership | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath (Join-Path $directory 'client-ownership.json') -Encoding utf8
        } else {
            $launchIdentity = [pscustomobject]@{ source = 'controller-supplied-baseUrl'; baseUrl = $BaseUrl; processId = $null }
        }
        $null = Save-SharedReport
        foreach ($id in $RegressionSuite) {
            $suiteDirectory = Join-Path $directory ([guid]::NewGuid().ToString('N'))
            $casePath = Join-Path $suiteDirectory (($id -replace '[^A-Za-z0-9_.-]', '_') + '.json')
            $errorMessage = $null
            try {
                $null = & "$PSScriptRoot/regression.ps1" -WorkspaceRoot $WorkspaceRoot -Suite $id -BaseUrl $BaseUrl -KeepClientOpen -TimeoutSeconds $TimeoutSeconds -ArtifactDirectory $suiteDirectory -CandidateIdentity $CandidateIdentity -RuntimeBuildIdentity $RuntimeBuildIdentity
            } catch { $errorMessage = ConvertTo-SanitizedText $_.Exception.Message }
            $product = if (Test-Path -LiteralPath $casePath) { Get-Content -LiteralPath $casePath -Raw | ConvertFrom-Json -Depth 64 } else { $null }
            # A suite with only passed cases and qualified unavailable cases may continue; retain its native result unchanged.
            $qualifiedSkip = $false
            if ($null -ne $product -and $product.suiteId -eq $id -and $product.outcome -eq 'skipped') {
                $counts = $product.accounting
                $names = 'selected','attempted','passed','skipped','failed','error','blocked','cancelled','notRun'
                $validCounts = @($names | Where-Object {
                    $value = $counts.$_
                    $null -eq $value -or $value -isnot [long] -or $value -lt 0
                }).Count -eq 0
                $selectedIds = @($product.selection.testIds)
                $caseResults = @($product.results)
                if ($validCounts -and $counts.selected -gt 0 -and $counts.skipped -gt 0 -and
                    $counts.attempted -eq $counts.selected -and $counts.passed + $counts.skipped -eq $counts.selected -and
                    $counts.failed -eq 0 -and $counts.error -eq 0 -and $counts.blocked -eq 0 -and
                    $counts.cancelled -eq 0 -and $counts.notRun -eq 0 -and
                    $selectedIds.Count -eq $counts.selected -and $caseResults.Count -eq $counts.selected -and
                    @($selectedIds | Where-Object { $_ -isnot [string] -or [string]::IsNullOrWhiteSpace($_) }).Count -eq 0 -and
                    @($selectedIds | Select-Object -Unique).Count -eq $counts.selected) {
                    $passedCases = 0; $skippedCases = 0; $validCases = $true
                    for ($index = 0; $index -lt $caseResults.Count; $index++) {
                        $case = $caseResults[$index]
                        if ($case.suiteId -ne $id -or $case.testId -ne $selectedIds[$index]) { $validCases = $false; break }
                        if ($case.outcome -eq 'passed' -and $case.ok -eq $true) { $passedCases++; continue }
                        if ($case.outcome -ne 'skipped' -or $case.category -ne 'expectedUnavailable' -or $case.ok -ne $false -or
                            @($case.missingRequirements).Count -eq 0 -or
                            @($case.missingRequirements | Where-Object {
                                $_ -isnot [string] -or [string]::IsNullOrWhiteSpace($_) -or $_ -notin @($case.effectiveRequirements)
                            }).Count -gt 0) { $validCases = $false; break }
                        $skippedCases++
                    }
                    $qualifiedSkip = $validCases -and $passedCases -eq $counts.passed -and $skippedCases -eq $counts.skipped
                }
            }
            $outcome = if ($qualifiedSkip) { 'skipped' } elseif ($errorMessage -or $null -eq $product -or -not $product.ok) { 'failed' } else { 'passed' }
            $results += [pscustomobject]@{ id = $id; outcome = $outcome; json = $(if ($product) { $casePath } else { $null }); result = $product; error = $errorMessage }
            # Framework terminal results already include journal cleanup. Do not inspect/reset its world here.
            $null = Save-SharedReport
            if ($outcome -eq 'failed' -and -not $ContinueOnFailure) { break }
        }
    } catch { $failure = ConvertTo-SanitizedText $_.Exception.Message }
    finally {
        if ($null -ne $ownership) {
            try {
                $lifecycle = Stop-OwnedClient -Ownership $ownership -KeepClientOpen:$KeepClientOpen
                $lifecycle | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath (Join-Path $directory 'client-lifecycle.json') -Encoding utf8
                if (-not $lifecycle.ok) { throw "Owned-client shutdown failed: $($lifecycle.errors -join '; ')" }
            } catch { $failure = @($failure, (ConvertTo-SanitizedText $_.Exception.Message)) | Where-Object { $_ } | Join-String -Separator '; ' }
        }
        $runIdentity | Add-Member -NotePropertyName finishedAtUtc -NotePropertyValue ([datetime]::UtcNow.ToString('o')) -Force
        $summary = Save-SharedReport
    }
    $summary
    if (-not $summary.ok) { throw "Regression run failed; see $jsonPath" }
}

if ($RegressionSuite.Count) {
    Invoke-SharedRegression
    return
}
if ($BaseUrl -or $KeepClientOpen -or $scriptInvocation.BoundParameters.ContainsKey('ReportPath')) { throw "BaseUrl, KeepClientOpen and ReportPath require explicit RegressionSuite selection; manifest wrappers own their lifecycle." }

$manifest = $null
$matchingSuites = @()
$selection = [pscustomobject]@{
    invocation = Get-InvocationProvenance
    manifestPath = $manifestPath
    manifestSha256 = $null
    loader = $Loader
    tier = $Tier
    groups = @($Group)
    requestedSuites = @($Suite)
    selectedSuites = @()
}
try {
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw "Regression suite manifest not found: $manifestPath"
    }
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.protocolVersion -ne 1) {
        throw "Unsupported regression suite manifest version '$($manifest.protocolVersion)'."
    }
    $matchingSuites = @($manifest.suites | Where-Object { Test-SuiteMatches $_ })
    $selection.manifestSha256 = (Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash
    $selection.selectedSuites = @($matchingSuites | ForEach-Object {
        [pscustomobject]@{ id = $_.id; script = $_.script; arguments = (Copy-SanitizedTelemetry $_.arguments) }
    })
} catch {
    Write-RegressionFailureSummary -Message $_.Exception.Message -Selection $selection
}
Add-SensitiveValues $matchingSuites
if ($List) {
    @($matchingSuites | Select-Object id, groups, tier, loaders, script, arguments | ForEach-Object { Copy-SanitizedTelemetry $_ })
    return
}
if ($matchingSuites.Count -eq 0) {
    $requested = if ($Suite.Count -gt 0) { "suite '$($Suite -join ", ")'" } elseif ($Group.Count -gt 0) { "group '$($Group -join ", ")'" } else { "tier '$Tier'" }
    Write-RegressionFailureSummary -Message "No $requested regression suites are registered for loader '$Loader'." -Selection $selection
}

$results = @()
for ($suiteIndex = 0; $suiteIndex -lt $matchingSuites.Count; $suiteIndex++) {
    $selectedSuite = $matchingSuites[$suiteIndex]
    $scriptPath = Join-Path $PSScriptRoot $selectedSuite.script
    $scriptArguments = @{ WorkspaceRoot = $WorkspaceRoot; Loader = $Loader; TimeoutSeconds = $TimeoutSeconds }
    if ($selectedSuite.arguments) {
        foreach ($argument in $selectedSuite.arguments.PSObject.Properties) {
            $scriptArguments[$argument.Name] = $argument.Value
        }
    }
    Add-SensitiveValues $scriptArguments
    $diagnostics = @()

    try {
        if ($suiteIndex -gt 0) {
            Wait-PreviousAutomationClientStopped
        }
        if (-not (Test-Path -LiteralPath $scriptPath -PathType Leaf)) {
            throw "Regression script for '$($selectedSuite.id)' was not found: $scriptPath"
        }
        $captured = @(& $scriptPath @scriptArguments *>&1)
        $diagnostics = @($captured | Where-Object { Test-PowerShellStreamRecord $_ } | ForEach-Object { ConvertTo-SanitizedStreamDiagnostic $_ })
        $productRecords = @($captured | Where-Object { -not (Test-PowerShellStreamRecord $_) })
        foreach ($diagnostic in $diagnostics) {
            Write-Warning (ConvertTo-SanitizedText "Child $($selectedSuite.id) $($diagnostic.stream): $($diagnostic.message)")
        }
        $errorDiagnostics = @($diagnostics | Where-Object { $_.stream -eq "error" })
        if ($errorDiagnostics.Count -gt 0) {
            throw (ConvertTo-SanitizedText "Child $($selectedSuite.id) reported an error: $($errorDiagnostics[0].message)")
        }
        if ($productRecords.Count -ne 1) {
            throw (ConvertTo-SanitizedText "Child $($selectedSuite.id) returned $($productRecords.Count) product records; expected exactly one.")
        }
        $result = $productRecords[0]
        $okProperty = $result.PSObject.Properties['ok']
        $passedProperty = $result.PSObject.Properties['passed']
        if (($null -ne $okProperty -and $okProperty.Value -eq $false) -or ($null -ne $passedProperty -and $passedProperty.Value -eq $false)) {
            throw (ConvertTo-SanitizedText "Child $($selectedSuite.id) reported an unsuccessful result.")
        }
        $results += [pscustomobject]@{
            id = $selectedSuite.id
            outcome = "passed"
            passed = $true
            executionArguments = Copy-SanitizedTelemetry $scriptArguments
            launch = [pscustomobject]@{
                script = $selectedSuite.script
                baseUrl = if ($null -ne $result -and $result.PSObject.Properties.Match("baseUrl").Count -gt 0) { ConvertTo-SanitizedText $result.baseUrl } else { $null }
            }
            result = Copy-SanitizedTelemetry $result
            diagnostics = $diagnostics
            error = $null
        }
        Write-Host (ConvertTo-SanitizedText "PASS $($selectedSuite.id)")
    } catch {
        $results += [pscustomobject]@{
            id = $selectedSuite.id
            outcome = "failed"
            passed = $false
            executionArguments = Copy-SanitizedTelemetry $scriptArguments
            launch = [pscustomobject]@{ script = $selectedSuite.script; baseUrl = $null }
            result = $null
            diagnostics = $diagnostics
            error = ConvertTo-SanitizedText $_.Exception.Message
        }
        Write-Warning (ConvertTo-SanitizedText "FAIL $($selectedSuite.id): $($_.Exception.Message)")
        if (-not $ContinueOnFailure) {
            break
        }
    }
}

$attempted = $results.Count
$passed = @($results | Where-Object { $_.outcome -eq "passed" }).Count
$skipped = @($results | Where-Object { $_.outcome -eq "skipped" }).Count
$failed = $attempted - $passed - $skipped
$accounting = [pscustomobject]@{
    selected = $matchingSuites.Count
    attempted = $attempted
    passed = $passed
    failed = $failed
    skipped = $skipped
    notRun = $matchingSuites.Count - $attempted
}
Assert-Accounting -Accounting $accounting
$runIdentity | Add-Member -NotePropertyName finishedAtUtc -NotePropertyValue ((Get-Date).ToUniversalTime().ToString("o"))

$summary = [pscustomobject]@{
    ok = ($accounting.failed -eq 0 -and $accounting.notRun -eq 0)
    protocolVersion = $manifest.protocolVersion
    loader = $Loader
    tier = $Tier
    groups = $Group
    requestedSuites = $Suite
    passed = $accounting.passed
    failed = $accounting.failed
    runner = $runner
    runIdentity = $runIdentity
    bindings = [pscustomobject]@{
        candidateIdentity = $CandidateIdentity
        runtimeBuildIdentity = $RuntimeBuildIdentity
    }
    selection = $selection
    accounting = $accounting
    results = $results
}

$summary = Copy-SanitizedTelemetry $summary
$summary

if (-not $summary.ok) {
    throw "Regression run failed: $($accounting.failed) failed, $($accounting.notRun) not run, of $($accounting.selected) selected suites."
}
