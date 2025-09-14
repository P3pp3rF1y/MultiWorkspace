# Get files that differ ignoring line endings (actual content changes)
$ignoredDiffs = git diff --ignore-space-at-eol --name-only

# Get all modified files (including those with just line-ending changes)
$allChanges = git status --porcelain | ForEach-Object {
    # git status --porcelain format: XY filename
    # So take the filename from position 3 onward (first 2 chars are status flags)
    $_.Substring(3)
}

# Find files with only line-ending diffs
$onlyLineEndingDiffs = $allChanges | Where-Object { $_ -notin $ignoredDiffs }

# Revert those files
foreach ($file in $onlyLineEndingDiffs) {
    Write-Host "Reverting $file"
    git restore -- "$file"
}