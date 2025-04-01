# List of repos with their paths and optional wait time after push (in seconds)
$repos = @(
    @{ Name = "SophisticatedCore"; Path = "SophisticatedCore"; WaitAfterPush = 120 },
    @{ Name = "SophisticatedBackpacks"; Path = "SophisticatedBackpacks"; WaitAfterPush = 120 },
    @{ Name = "SophisticatedStorage"; Path = "SophisticatedStorage"; WaitAfterPush = 120 },
    @{ Name = "SophisticatedStorageInMotion"; Path = "SophisticatedStorageInMotion"; WaitAfterPush = 0 },
    @{ Name = "SophisticatedStorageCreateIntegration"; Path = "SophisticatedStorageCreateIntegration"; WaitAfterPush = 0 }  # no wait needed
)
$branch="1.21.x"

foreach ($repo in $repos) {
    Write-Host "`n=== Processing $($repo.Name) ==="
    Set-Location -Path $repo.Path

    # Check if anything needs to be pushed
    $currentBranch = git rev-parse --abbrev-ref HEAD
    $hasChangesToPush = git log origin/$currentBranch..HEAD

    if (-not [string]::IsNullOrWhiteSpace($hasChangesToPush)) {
        Write-Host "Pushing changes to $($repo.Name)..."
        git push origin $currentBranch

        $waitTime = $repo.WaitAfterPush
        if ($waitTime -gt 0) {
            Write-Host "Waiting $waitTime seconds for Maven/CI to process..."
            Start-Sleep -Seconds $waitTime
        }
    } else {
        Write-Host "No changes to push for $($repo.Name), skipping wait."
    }
    Set-Location -Path ..
}