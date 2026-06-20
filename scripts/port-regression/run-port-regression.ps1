param(
    [string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [string]$Suite = "critical",
    [int]$TimeoutSeconds = 0,
    [switch]$ContinueOnFailure,
    [switch]$List,
    [string]$OutputDir = ""
)

$ErrorActionPreference = "Stop"

$suiteRoot = Join-Path $PSScriptRoot "suites"
$devClientRoot = Join-Path $WorkspaceRoot "scripts\dev-client"
$runRoot = Join-Path $WorkspaceRoot "workspace\run\port-regression"
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $runRoot "latest"
}
$logOutputDir = Join-Path $OutputDir "logs"
$screenshotOutputDir = Join-Path $OutputDir "screenshots"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Get-SuitePath {
    param([string]$SuiteName)

    if (Test-Path $SuiteName -PathType Leaf) {
        return (Resolve-Path $SuiteName).Path
    }
    $path = Join-Path $suiteRoot "$SuiteName.json"
    Assert-True (Test-Path $path -PathType Leaf) "Port regression suite not found: $SuiteName ($path)"
    return $path
}

function Get-SuiteData {
    param([string]$SuiteName)

    Get-Content (Get-SuitePath -SuiteName $SuiteName) -Raw | ConvertFrom-Json
}

function Get-SuiteTests {
    param([string]$SuiteName)

    $suiteData = Get-SuiteData -SuiteName $SuiteName
    $tests = @()
    foreach ($includedSuite in @($suiteData.includeSuites)) {
        if (-not [string]::IsNullOrWhiteSpace($includedSuite)) {
            $tests += Get-SuiteTests -SuiteName $includedSuite
        }
    }
    if ($suiteData.PSObject.Properties["tests"] -and $null -ne $suiteData.tests) {
        foreach ($test in @($suiteData.tests)) {
            if ($null -eq $test) {
                continue
            }
            $test | Add-Member -NotePropertyName sourceSuite -NotePropertyValue $suiteData.name -Force
            $tests += $test
        }
    }
    return $tests
}

function Get-TestTimeout {
    param([object]$Test)

    if ($TimeoutSeconds -gt 0) {
        return $TimeoutSeconds
    }
    if ($Test.PSObject.Properties["timeoutSeconds"] -and $Test.timeoutSeconds -gt 0) {
        return [int]$Test.timeoutSeconds
    }
    return 360
}

function Get-SafeName {
    param([string]$Value)

    return ($Value -replace '[^A-Za-z0-9_.-]', '_')
}

function Stop-ProcessTree {
    param([int]$ProcessId)

    if ($ProcessId -le 0 -or $null -eq (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        return
    }
    try {
        & taskkill.exe /PID $ProcessId /T /F | Out-Null
    } catch {
        Write-Warning "Failed to kill process tree ${ProcessId}: $($_.Exception.Message)"
    }
}

function Invoke-BridgeJson {
    param(
        [Parameter(Mandatory = $true)] [string]$BaseUrl,
        [Parameter(Mandatory = $true)] [string]$Method,
        [Parameter(Mandatory = $true)] [string]$Path,
        [object]$Body = $null,
        [int]$Timeout = 60
    )

    $uri = "$BaseUrl$Path"
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -TimeoutSec $Timeout
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress -Depth 32) -TimeoutSec $Timeout
}

function Stop-AutomationClient {
    param([object]$Ready)

    try {
        if ($null -eq $Ready) {
            return
        }
        try {
            Invoke-BridgeJson -BaseUrl $Ready.baseUrl -Method Post -Path "/client/stop" -Timeout 30 | Out-Null
        } catch {
            if ($Ready.processId) {
                Stop-ProcessTree -ProcessId ([int]$Ready.processId)
            }
        }
        if ($Ready.processId) {
            $processId = [int]$Ready.processId
            $deadline = (Get-Date).AddSeconds(10)
            while ((Get-Date) -lt $deadline -and $null -ne (Get-Process -Id $processId -ErrorAction SilentlyContinue)) {
                Start-Sleep -Milliseconds 250
            }
            Stop-ProcessTree -ProcessId $processId
        }
    } catch {
        Write-Warning "Failed to stop automation client cleanly: $($_.Exception.Message)"
    }
}

$script:SharedEndpointReady = $null
$script:SharedEndpointKey = ""

function Stop-SharedEndpointClient {
    if ($null -ne $script:SharedEndpointReady) {
        Stop-AutomationClient -Ready $script:SharedEndpointReady
        $script:SharedEndpointReady = $null
        $script:SharedEndpointKey = ""
    }
}

function Get-EndpointClientKey {
    param([object]$Test)

    $recipeViewer = if ($Test.PSObject.Properties["recipeViewer"]) { $Test.recipeViewer } else { "" }
    $minimalRuntime = $Test.PSObject.Properties["minimalRuntime"] -and [bool]$Test.minimalRuntime
    $skipRecipeViewerReady = $Test.PSObject.Properties["skipRecipeViewerReady"] -and [bool]$Test.skipRecipeViewerReady
    $loadWorld = if ($Test.PSObject.Properties["loadWorld"]) { [bool]$Test.loadWorld } else { $true }
    return "recipeViewer=$recipeViewer|minimal=$minimalRuntime|skipRecipeViewerReady=$skipRecipeViewerReady|loadWorld=$loadWorld"
}

function Get-EndpointReadyArgs {
    param(
        [object]$Test,
        [int]$Timeout
    )

    $readyArgs = @{ WorkspaceRoot = $WorkspaceRoot; TimeoutSeconds = $Timeout; CloseOnExit = $true }
    if ($Test.PSObject.Properties["recipeViewer"] -and -not [string]::IsNullOrWhiteSpace($Test.recipeViewer)) {
        $readyArgs.RecipeViewer = $Test.recipeViewer
    }
    if ($Test.PSObject.Properties["minimalRuntime"] -and [bool]$Test.minimalRuntime) {
        $readyArgs.MinimalRuntime = $true
    }
    if ($Test.PSObject.Properties["skipRecipeViewerReady"] -and [bool]$Test.skipRecipeViewerReady) {
        $readyArgs.SkipRecipeViewerReady = $true
    }
    if ($Test.PSObject.Properties["loadWorld"]) {
        $readyArgs.LoadWorld = [bool]$Test.loadWorld
    }
    return $readyArgs
}

function Get-EndpointClient {
    param([object]$Test)

    $timeout = Get-TestTimeout -Test $Test
    $isolatedClient = $Test.PSObject.Properties["isolatedClient"] -and [bool]$Test.isolatedClient
    if ($isolatedClient) {
        $readyArgs = Get-EndpointReadyArgs -Test $Test -Timeout $timeout
        return [pscustomobject]@{ ready = (& (Join-Path $devClientRoot "start-and-ready.ps1") @readyArgs); shared = $false }
    }

    $key = Get-EndpointClientKey -Test $Test
    if ($null -eq $script:SharedEndpointReady -or $script:SharedEndpointKey -ne $key) {
        Stop-SharedEndpointClient
        $readyArgs = Get-EndpointReadyArgs -Test $Test -Timeout $timeout
        $script:SharedEndpointReady = & (Join-Path $devClientRoot "start-and-ready.ps1") @readyArgs
        $script:SharedEndpointKey = $key
    }

    return [pscustomobject]@{ ready = $script:SharedEndpointReady; shared = $true }
}

function Invoke-ProcessWithTimeout {
    param(
        [Parameter(Mandatory = $true)] [string]$FilePath,
        [string[]]$Arguments = @(),
        [Parameter(Mandatory = $true)] [int]$Timeout,
        [Parameter(Mandatory = $true)] [string]$OutputPath
    )

    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $FilePath
    foreach ($argument in $Arguments) {
        [void]$psi.ArgumentList.Add($argument)
    }
    $psi.WorkingDirectory = $WorkspaceRoot
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $psi
    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    if (-not $process.WaitForExit($Timeout * 1000)) {
        Stop-ProcessTree -ProcessId $process.Id
        throw "Timed out after ${Timeout}s: $FilePath $($Arguments -join ' ')"
    }
    $stdout = $stdoutTask.GetAwaiter().GetResult()
    $stderr = $stderrTask.GetAwaiter().GetResult()
    ($stdout + $stderr) | Set-Content -LiteralPath $OutputPath
    if ($process.ExitCode -ne 0) {
        throw "Command failed with exit code $($process.ExitCode): $FilePath $($Arguments -join ' '). See $OutputPath"
    }
    return @{ exitCode = $process.ExitCode; outputPath = $OutputPath }
}

function Invoke-ProcessWithRetry {
    param(
        [Parameter(Mandatory = $true)] [string]$FilePath,
        [string[]]$Arguments = @(),
        [Parameter(Mandatory = $true)] [int]$Timeout,
        [Parameter(Mandatory = $true)] [string]$OutputPath,
        [int]$Retries = 1
    )

    $attempt = 0
    while ($true) {
        $attempt++
        try {
            Invoke-ProcessWithTimeout -FilePath $FilePath -Arguments $Arguments -Timeout $Timeout -OutputPath $OutputPath | Out-Null
            return
        } catch {
            if ($attempt -gt $Retries) {
                throw
            }

            if (Test-Path $OutputPath -PathType Leaf) {
                Copy-Item -LiteralPath $OutputPath -Destination "$OutputPath.attempt$attempt" -Force
            }
            Start-Sleep -Seconds 5
        }
    }
}

function Assert-ObjectProperties {
    param(
        [object]$Value,
        [object]$Assertions
    )

    if ($null -eq $Assertions) {
        return
    }
    foreach ($property in $Assertions.PSObject.Properties) {
        $actualProperty = $Value.PSObject.Properties[$property.Name]
        Assert-True ($null -ne $actualProperty) "Expected response property '$($property.Name)' to exist."
        Assert-True ($actualProperty.Value.ToString() -eq $property.Value.ToString()) "Expected '$($property.Name)' to be '$($property.Value)' but was '$($actualProperty.Value)'."
    }
}

function Assert-CleanLogs {
    param([object]$Test)

    foreach ($pattern in @($Test.noLogPatterns)) {
        if ([string]::IsNullOrWhiteSpace($pattern)) {
            continue
        }
        $logPath = Join-Path $WorkspaceRoot "workspace\run\logs\latest.log"
        if (-not (Test-Path $logPath -PathType Leaf)) {
            continue
        }
        $matches = Select-String -LiteralPath $logPath -Pattern $pattern -AllMatches
        if ($matches) {
            foreach ($match in @($matches)) {
                if (Test-AllowedLogContext -LogPath $logPath -LineNumber $match.LineNumber -Test $Test) {
                    continue
                }
                throw "Log pattern '$pattern' matched $logPath line $($match.LineNumber): $($match.Line)"
            }
        }
    }
}

function Assert-NoWorkspaceProcesses {
    $currentProcessId = $PID
    $remaining = @(Get-CimInstance Win32_Process | Where-Object {
        $_.ProcessId -ne $currentProcessId -and
        $_.CommandLine -like "*$WorkspaceRoot*" -and
        $_.Name -in @("cmd.exe", "java.exe")
    })
    if ($remaining.Count -gt 0) {
        $summary = @($remaining | ForEach-Object { "PID=$($_.ProcessId) Name=$($_.Name) Parent=$($_.ParentProcessId)" }) -join "; "
        throw "Workspace process leak detected: $summary"
    }
}

function Test-AllowedLogContext {
    param(
        [Parameter(Mandatory = $true)] [string]$LogPath,
        [Parameter(Mandatory = $true)] [int]$LineNumber,
        [Parameter(Mandatory = $true)] [object]$Test
    )

    $allowedPatterns = @($Test.allowedLogContextPatterns)
    if ($allowedPatterns.Count -eq 0) {
        return $false
    }

    $lines = Get-Content -LiteralPath $LogPath
    $start = [Math]::Max(0, $LineNumber - 1)
    $end = [Math]::Min($lines.Count - 1, $LineNumber + 8)
    $context = ($lines[$start..$end] -join "`n")
    foreach ($allowedPattern in $allowedPatterns) {
        if (-not [string]::IsNullOrWhiteSpace($allowedPattern) -and $context -match $allowedPattern) {
            return $true
        }
    }
    return $false
}

function Invoke-StartupTest {
    param([object]$Test)

    $timeout = Get-TestTimeout -Test $Test
    $args = @{ WorkspaceRoot = $WorkspaceRoot; TimeoutSeconds = $timeout; CloseOnExit = $true }
    if ($Test.PSObject.Properties["recipeViewer"] -and -not [string]::IsNullOrWhiteSpace($Test.recipeViewer)) {
        $args.RecipeViewer = $Test.recipeViewer
    }
    if ($Test.PSObject.Properties["minimalRuntime"] -and [bool]$Test.minimalRuntime) {
        $args.MinimalRuntime = $true
    }
    if ($Test.PSObject.Properties["skipRecipeViewerReady"] -and [bool]$Test.skipRecipeViewerReady) {
        $args.SkipRecipeViewerReady = $true
    }
    if ($Test.PSObject.Properties["loadWorld"]) {
        $args.LoadWorld = [bool]$Test.loadWorld
    }

    $ready = $null
    try {
        $ready = & (Join-Path $devClientRoot "start-and-ready.ps1") @args
        if ($Test.assertions) {
            Assert-ObjectProperties -Value $ready.state -Assertions $Test.assertions
        }
        return $ready
    } finally {
        Stop-AutomationClient -Ready $ready
    }
}

function Invoke-EndpointTest {
    param([object]$Test)

    $timeout = Get-TestTimeout -Test $Test
    $client = $null
    try {
        $client = Get-EndpointClient -Test $Test
        $ready = $client.ready
        $method = if ($Test.PSObject.Properties["method"]) { $Test.method } else { "POST" }
        $body = if ($Test.PSObject.Properties["body"]) { $Test.body } else { $null }
        $result = Invoke-BridgeJson -BaseUrl $ready.baseUrl -Method $method -Path $Test.path -Body $body -Timeout $timeout
        if ($Test.assertions) {
            try {
                Assert-ObjectProperties -Value $result -Assertions $Test.assertions
            } catch {
                $rawResult = $result | ConvertTo-Json -Depth 32 -Compress
                throw "$($_.Exception.Message) Response: $rawResult"
            }
        }
        if ($Test.PSObject.Properties["screenshot"] -and -not [string]::IsNullOrWhiteSpace($Test.screenshot)) {
            $screenshotPath = Join-Path $screenshotOutputDir $Test.screenshot
            try {
                Invoke-WebRequest -Method Get -Uri "$($ready.baseUrl)/screenshot" -OutFile $screenshotPath -TimeoutSec 60 | Out-Null
            } catch {
                Write-Warning "Failed to capture screenshot for $($Test.id): $($_.Exception.Message)"
            }
        }
        return $result
    } catch {
        if ($null -ne $client -and $client.shared) {
            Stop-SharedEndpointClient
        } elseif ($null -ne $client) {
            Stop-AutomationClient -Ready $client.ready
        }
        throw
    } finally {
        if ($null -ne $client -and -not $client.shared) {
            Stop-AutomationClient -Ready $client.ready
        }
    }
}

function Invoke-PortRegressionTest {
    param([object]$Test)

    $id = $Test.id
    $safeName = Get-SafeName -Value $id
    $outputPath = Join-Path $logOutputDir "$safeName.log"
    $started = Get-Date
    try {
        switch ($Test.type) {
            "blocked" {
                return [pscustomobject]@{ id = $id; name = $Test.name; suite = $Test.sourceSuite; status = "blocked"; durationSeconds = 0; message = $Test.blockedReason; outputPath = $null }
            }
            "gradle" {
                $gradle = Join-Path $WorkspaceRoot "gradlew.bat"
                $args = @($Test.args)
                Invoke-ProcessWithTimeout -FilePath $gradle -Arguments $args -Timeout (Get-TestTimeout -Test $Test) -OutputPath $outputPath | Out-Null
            }
            "script" {
                $scriptPath = Join-Path $WorkspaceRoot $Test.script
                $args = @("-WorkspaceRoot", $WorkspaceRoot, "-TimeoutSeconds", (Get-TestTimeout -Test $Test)) + @($Test.args)
                Invoke-ProcessWithTimeout -FilePath "pwsh" -Arguments (@("-NoProfile", "-File", $scriptPath) + $args) -Timeout (Get-TestTimeout -Test $Test) -OutputPath $outputPath | Out-Null
            }
            "recipeViewer" {
                $scriptPath = Join-Path $devClientRoot "run-recipe-viewer-regression.ps1"
                $args = @("-WorkspaceRoot", $WorkspaceRoot, "-Viewer", $Test.viewer, "-Suite", $Test.suite, "-TimeoutSeconds", (Get-TestTimeout -Test $Test))
                Invoke-ProcessWithRetry -FilePath "pwsh" -Arguments (@("-NoProfile", "-File", $scriptPath) + $args) -Timeout (Get-TestTimeout -Test $Test) -OutputPath $outputPath -Retries 1
            }
            "startup" {
                $result = Invoke-StartupTest -Test $Test
                $result | ConvertTo-Json -Depth 32 | Set-Content -LiteralPath $outputPath
            }
            "endpoint" {
                $result = Invoke-EndpointTest -Test $Test
                $result | ConvertTo-Json -Depth 32 | Set-Content -LiteralPath $outputPath
            }
            "logScan" {
                "log scan" | Set-Content -LiteralPath $outputPath
            }
            "processCheck" {
                Assert-NoWorkspaceProcesses
                "no workspace cmd/java processes" | Set-Content -LiteralPath $outputPath
            }
            default {
                throw "Unsupported test type '$($Test.type)' for test '$id'."
            }
        }
        Assert-CleanLogs -Test $Test
        $duration = [int]((Get-Date) - $started).TotalSeconds
        return [pscustomobject]@{ id = $id; name = $Test.name; suite = $Test.sourceSuite; status = "passed"; durationSeconds = $duration; message = ""; outputPath = $outputPath }
    } catch {
        $duration = [int]((Get-Date) - $started).TotalSeconds
        $message = $_.Exception.Message
        if (-not (Test-Path $outputPath -PathType Leaf)) {
            $message | Set-Content -LiteralPath $outputPath
        } else {
            Add-Content -LiteralPath $outputPath -Value "`nERROR: $message"
        }
        return [pscustomobject]@{ id = $id; name = $Test.name; suite = $Test.sourceSuite; status = "failed"; durationSeconds = $duration; message = $message; outputPath = $outputPath }
    }
}

function Write-PortRegressionReports {
    param(
        [object[]]$Results,
        [string]$SuiteName
    )

    $summary = [pscustomobject]@{
        suite = $SuiteName
        workspaceRoot = $WorkspaceRoot
        generatedAt = (Get-Date).ToString("o")
        total = $Results.Count
        passed = @($Results | Where-Object status -eq "passed").Count
        failed = @($Results | Where-Object status -eq "failed").Count
        blocked = @($Results | Where-Object status -eq "blocked").Count
        results = $Results
    }
    $summary | ConvertTo-Json -Depth 64 | Set-Content -LiteralPath (Join-Path $OutputDir "report.json")

    $lines = @()
    $lines += "# Port Regression Report"
    $lines += ""
    $lines += "- Suite: $SuiteName"
    $lines += "- Workspace: $WorkspaceRoot"
    $lines += "- Generated: $($summary.generatedAt)"
    $lines += "- Passed: $($summary.passed)"
    $lines += "- Failed: $($summary.failed)"
    $lines += "- Blocked: $($summary.blocked)"
    $lines += ""
    foreach ($status in @("failed", "blocked", "passed")) {
        $group = @($Results | Where-Object status -eq $status)
        if ($group.Count -eq 0) {
            continue
        }
        $lines += "## $($status.Substring(0,1).ToUpperInvariant())$($status.Substring(1))"
        $lines += ""
        foreach ($result in $group) {
            $checkbox = if ($result.status -eq "passed") { "[x]" } else { "[ ]" }
            $line = "- $checkbox $($result.id) - $($result.name) ($($result.durationSeconds)s)"
            if (-not [string]::IsNullOrWhiteSpace($result.message)) {
                $line += " - $($result.message)"
            }
            if (-not [string]::IsNullOrWhiteSpace($result.outputPath)) {
                $line += " - $($result.outputPath)"
            }
            $lines += $line
        }
        $lines += ""
    }
    $lines | Set-Content -LiteralPath (Join-Path $OutputDir "report.md")
}

if ($List) {
    Get-ChildItem -LiteralPath $suiteRoot -Filter "*.json" | Sort-Object Name | ForEach-Object {
        $suiteData = Get-Content $_.FullName -Raw | ConvertFrom-Json
        [pscustomobject]@{ suite = [System.IO.Path]::GetFileNameWithoutExtension($_.Name); description = $suiteData.description }
    }
    return
}

if (Test-Path $OutputDir -PathType Container) {
    Get-ChildItem -LiteralPath $OutputDir -Force | Remove-Item -Recurse -Force
}
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
New-Item -ItemType Directory -Path $logOutputDir -Force | Out-Null
New-Item -ItemType Directory -Path $screenshotOutputDir -Force | Out-Null

$tests = @(Get-SuiteTests -SuiteName $Suite)
Assert-True ($tests.Count -gt 0) "Port regression suite '$Suite' has no tests."

$results = @()
try {
    foreach ($test in $tests) {
        if ($test.type -ne "endpoint") {
            Stop-SharedEndpointClient
        }

        Write-Host "RUN $($test.id) - $($test.name)"
        $result = Invoke-PortRegressionTest -Test $test
        $results += $result
        Write-Host "$($result.status.ToUpperInvariant()) $($test.id)"
        if ($result.status -eq "failed" -and -not $ContinueOnFailure) {
            break
        }
    }
} finally {
    Stop-SharedEndpointClient
}

Write-PortRegressionReports -Results $results -SuiteName $Suite

$failedCount = @($results | Where-Object status -eq "failed").Count
[pscustomobject]@{
    ok = $failedCount -eq 0
    suite = $Suite
    total = $results.Count
    passed = @($results | Where-Object status -eq "passed").Count
    failed = $failedCount
    blocked = @($results | Where-Object status -eq "blocked").Count
    report = Join-Path $OutputDir "report.md"
    json = Join-Path $OutputDir "report.json"
}

if ($failedCount -gt 0) {
    exit 1
}
