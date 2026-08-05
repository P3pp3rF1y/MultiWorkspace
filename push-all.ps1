#requires -Version 7.0
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------
# Push root repo first, then child repos in dependency order.
# For repos marked WaitForCI = $true, wait for the GitHub Actions workflow
# at .github/workflows/gradle.yml (matching the pushed commit SHA) to complete before continuing.
# Script is expected to live in MultiWorkspace root (repo folders are subdirs).
# ---------------------------------------------------------------------

# --- Config ------------------------------------------------------------

$workspaceRoot = $PSScriptRoot

$owner        = "P3pp3rF1y"
$branch       = "1.21.4"
$workflowPath = ".github/workflows/gradle.yml"

# Polling behavior
$pollSeconds        = 10
$runLookback        = 50
$maxRunFindAttempts = 60   # 60 * 10s = ~10 minutes to find the run after push

# Root repo first, then child repos in dependency order
$rootRepo = @{ Name = "MultiWorkspace";                          Path = ".";                                       WaitForCI = $false }

$repos = @(
    @{ Name = "Reliquary";                               Path = "Reliquary";                               WaitForCI = $true  },
    @{ Name = "SophisticatedCore";                       Path = "SophisticatedCore";                       WaitForCI = $true  },
    @{ Name = "SophisticatedInventoryInteractions";      Path = "SophisticatedInventoryInteractions";      WaitForCI = $false },
    @{ Name = "SophisticatedBackpacks";                  Path = "SophisticatedBackpacks";                  WaitForCI = $true  },
    @{ Name = "SophisticatedBackpacksCreateIntegration"; Path = "SophisticatedBackpacksCreateIntegration"; WaitForCI = $false },
    @{ Name = "SophisticatedStorage";                    Path = "SophisticatedStorage";                    WaitForCI = $true  },
    @{ Name = "SophisticatedStorageCreateIntegration";   Path = "SophisticatedStorageCreateIntegration";   WaitForCI = $false },
    @{ Name = "SophisticatedStorageInMotion";            Path = "SophisticatedStorageInMotion";            WaitForCI = $true  },
    @{ Name = "SophisticatedItemActions";                Path = "SophisticatedItemActions";                WaitForCI = $false }
)

# ---------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------

function Require-Command([string]$cmd) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        throw "Missing required command '$cmd'."
    }
}

function Test-HasChangesToPush([string]$branchName) {
    git fetch origin $branchName *> $null
    if ($LASTEXITCODE -ne 0) {
        return $true
    }

    $aheadCount = (git rev-list --count "origin/$branchName..HEAD").Trim()
    return ([int]$aheadCount -gt 0)
}

function Get-RunIdForSha([string]$repoFull, [string]$sha) {
    $json = gh run list `
        -R $repoFull `
        -b $branch `
        -w $workflowPath `
        -L $runLookback `
        --json databaseId,headSha `
        2>$null

    if (-not $json) { return "" }

    $match = ($json | ConvertFrom-Json |
        Where-Object { $_.headSha -eq $sha } |
        Select-Object -First 1)

    if ($null -eq $match) { return "" }
    return [string]$match.databaseId
}

function Wait-RunCompletion([string]$repoFull, [string]$runId) {
    $consecutiveFailures = 0
    $maxConsecutiveFailures = 12   # 12 * 10s = ~2 minutes

    while ($true) {
        $status = gh run view $runId -R $repoFull --json status --jq ".status" 2>$null
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($status)) {
            $consecutiveFailures++
            Write-Host "  -> run $runId status=<unavailable> (gh run view transient failure $consecutiveFailures/$maxConsecutiveFailures)" -ForegroundColor Yellow

            if ($consecutiveFailures -ge $maxConsecutiveFailures) {
                # One more attempt with stderr visible to make the failure actionable
                Write-Host "  -> Last attempt with stderr for diagnostics:" -ForegroundColor Yellow
                gh run view $runId -R $repoFull --json status,conclusion
                throw "gh run view repeatedly failed for $repoFull runId=$runId (status)."
            }

            Start-Sleep -Seconds $pollSeconds
            continue
        }

        $consecutiveFailures = 0
        $status = $status.Trim()

        $conclusion = gh run view $runId -R $repoFull --json conclusion --jq ".conclusion" 2>$null
        if ($LASTEXITCODE -ne 0) {
            $conclusion = ""
        } elseif ($null -eq $conclusion) {
            $conclusion = ""
        } else {
            $conclusion = $conclusion.Trim()
        }

        Write-Host ("  -> run {0} status={1} conclusion={2}" -f $runId, $status, ($conclusion -ne "" ? $conclusion : "null"))

        if ($status -eq "completed") {
            return
        }

        Start-Sleep -Seconds $pollSeconds
    }
}

function Invoke-PushRepo([hashtable]$repo) {
    Write-Host "`n=== Processing $($repo.Name) ===" -ForegroundColor Cyan

    $repoPath = Join-Path $workspaceRoot $repo.Path
    if (-not (Test-Path $repoPath)) {
        throw "Repo path not found: $repoPath"
    }

    Push-Location $repoPath
    try {
        $currentBranch = (git rev-parse --abbrev-ref HEAD).Trim()
        if ($currentBranch -ne $branch) {
            Write-Host "On branch '$currentBranch' (expected '$branch'), skipping." -ForegroundColor Yellow
            return
        }

        if (-not (Test-HasChangesToPush $branch)) {
            Write-Host "No changes to push, skipping."
            return
        }

        $repoFull = "$owner/$($repo.Name)"

        $sha = (git rev-parse HEAD).Trim()
        Write-Host "Pushing $($repo.Name) ($branch) sha=$sha"
        git push -u origin $branch

        if (-not $repo.WaitForCI) {
            Write-Host "WaitForCI = false, continuing immediately."
            return
        }

        Write-Host "WaitForCI = true, waiting for workflow '$workflowPath' to complete..."

        $runId = ""
        for ($i = 0; $i -lt $maxRunFindAttempts; $i++) {
            $runId = Get-RunIdForSha -repoFull $repoFull -sha $sha
            if ($runId) { break }
            Start-Sleep -Seconds $pollSeconds
        }

        if (-not $runId) {
            Write-Host "Workflow run not found for sha=$sha; continuing (downstream may still race if publish is delayed)." -ForegroundColor Yellow
            return
        }

        Write-Host "Matched run_id=$runId, waiting for completion..."
        Wait-RunCompletion -repoFull $repoFull -runId $runId
        Write-Host "CI completed for $($repo.Name)."
    }
    finally {
        Pop-Location
    }
}

# ---------------------------------------------------------------------
# Preconditions
# ---------------------------------------------------------------------

Require-Command git
Require-Command gh

# Ensure gh auth is set up
gh auth status *> $null

# ---------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------

Invoke-PushRepo -repo $rootRepo
foreach ($repo in $repos) {
    Invoke-PushRepo -repo $repo
}

Write-Host "`n🎉 All repositories processed." -ForegroundColor Green
