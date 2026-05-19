$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$baseDir = 'D:\Development'
$minimumVersion = @(1, 20, 0)

$targetMessages = @{
	'SophisticatedCore' = @"
feat: ✨ Added recent results and expanded view to stonecutter, sawmill and chipped upgrades
- recent results keeps the 4 most recently crafted outputs at the top of the selection list
- expanded view opens a new UI with more outputs in view and search
"@
	'SophisticatedBackpacks' = @"
fix: 🐛 Fixed backpack collisions to use the standard shape to prevent client and server disagreeing ho much player can move around and on top of backpack
"@
	'SophisticatedInventoryInteractions' = @"
fix: 🐛 Fixed sort and transfer buttons sometimes overlapping
"@
}

function Get-VersionParts {
	param(
		[Parameter(Mandatory = $true)]
		[string]$WorkspaceName
	)

	if ($WorkspaceName -notmatch '^MultiWorkspace(?<version>\d+(?:\.\d+){0,2})$') {
		return $null
	}

	$parts = $Matches.version.Split('.') | ForEach-Object { [int]$_ }
	while ($parts.Count -lt 3) {
		$parts += 0
	}

	return ,$parts
}

function Test-VersionAtLeast {
	param(
		[int[]]$Left,
		[int[]]$Right
	)

	for ($i = 0; $i -lt 3; $i++) {
		if ($Left[$i] -gt $Right[$i]) {
			return $true
		}
		if ($Left[$i] -lt $Right[$i]) {
			return $false
		}
	}

	return $true
}

function Normalize-Message {
	param([string]$Message)

	if ($null -eq $Message) {
		return ''
	}

	return $Message.TrimEnd("`r", "`n")
}

$workspaceDirs = Get-ChildItem -Path $baseDir -Directory |
	Where-Object {
		$parts = Get-VersionParts -WorkspaceName $_.Name
		$null -ne $parts -and (Test-VersionAtLeast -Left $parts -Right $minimumVersion)
	} |
	Sort-Object Name

foreach ($workspaceDir in $workspaceDirs) {
	foreach ($repoName in $targetMessages.Keys) {
		$repoDir = Join-Path $workspaceDir.FullName $repoName
		if (-not (Test-Path $repoDir)) {
			continue
		}

		Push-Location $repoDir
		try {
			$targetMessage = Normalize-Message $targetMessages[$repoName]
			$currentMessage = Normalize-Message (git log -1 --format=%B)
			$status = git status --porcelain
			$hasChanges = -not [string]::IsNullOrWhiteSpace(($status | Out-String))
			$needsMessageFix = $currentMessage -ne $targetMessage

			if (-not $hasChanges -and -not $needsMessageFix) {
				Write-Host "Skipping $($workspaceDir.Name)\$repoName (already matches target message and has no changes)"
				continue
			}

			git add -A

			$tempMessageFile = Join-Path ([System.IO.Path]::GetTempPath()) ([System.IO.Path]::GetRandomFileName())
			try {
				[System.IO.File]::WriteAllText($tempMessageFile, $targetMessage + [Environment]::NewLine)
				git commit --amend --allow-empty -F "$tempMessageFile"
			}
			finally {
				if (Test-Path $tempMessageFile) {
					Remove-Item $tempMessageFile -Force
				}
			}

			Write-Host "Amended $($workspaceDir.Name)\$repoName"
		}
		finally {
			Pop-Location
		}
	}
}
