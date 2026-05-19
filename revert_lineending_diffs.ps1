function Test-HasDiffAgainstHead {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,

        [switch]$IgnoreLineEndings
    )

    $gitArgs = @("-c", "core.safecrlf=false", "diff", "--quiet")
    if ($IgnoreLineEndings) {
        $gitArgs += "--ignore-cr-at-eol"
    }

    $gitArgs += "HEAD", "--", $FilePath

    & git @gitArgs
    if ($LASTEXITCODE -eq 0) {
        return $false
    }
    if ($LASTEXITCODE -eq 1) {
        return $true
    }

    throw "git diff failed for '$FilePath'"
}

function Get-TrackedChangedFiles {
    param(
        [string[]]$ExcludePaths = @()
    )

    git status --porcelain=v1 --untracked-files=no | ForEach-Object {
        if ($_ -match '^(..)[ ](.*)$') {
            $status = $matches[1]
            $path = $matches[2]

            if ($status -eq '??' -or $status.Contains('R') -or $status.Contains('C')) {
                return
            }

            if ($path -and $path -notin $ExcludePaths) {
                $path
            }
        }
    } | Sort-Object -Unique
}

function Invoke-RevertLineEndingDiffs {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoPath,

        [Parameter(Mandatory = $true)]
        [string]$DisplayPath
    )

    Push-Location $RepoPath
    try {
        Write-Host "Checking $DisplayPath"

        $submodulePaths = @()
        if (Test-Path ".gitmodules") {
            $submodulePaths = git config --file .gitmodules --get-regexp path 2>$null | ForEach-Object {
                $parts = $_ -split '\s+', 2
                if ($parts.Count -eq 2) {
                    $parts[1]
                }
            }
        }

        $changedFiles = Get-TrackedChangedFiles -ExcludePaths $submodulePaths

        foreach ($file in $changedFiles) {
            if (-not (Test-HasDiffAgainstHead -FilePath $file -IgnoreLineEndings)) {
                Write-Host "Reverting $DisplayPath/$file"
                git restore --source=HEAD --staged --worktree -- "$file"
            }
        }

        foreach ($submodulePath in $submodulePaths) {
            $fullSubmodulePath = Join-Path $RepoPath $submodulePath
            if (-not (Test-Path $fullSubmodulePath)) {
                continue
            }

            $childDisplayPath = if ($DisplayPath -eq ".") {
                $submodulePath
            }
            else {
                "$DisplayPath/$submodulePath"
            }

            Invoke-RevertLineEndingDiffs -RepoPath $fullSubmodulePath -DisplayPath $childDisplayPath
        }
    }
    finally {
        Pop-Location
    }
}

Invoke-RevertLineEndingDiffs -RepoPath (Get-Location).Path -DisplayPath "."
