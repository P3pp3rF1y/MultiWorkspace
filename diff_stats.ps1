# Run at the superproject root.

function Parse-ShortStat($line) {
  $files = 0; $ins = 0; $del = 0
  if ($line) {
    foreach ($m in ([regex]::Matches($line, '(\d+)\s+files?\s+changed|(\d+)\s+insertions?\(\+\)|(\d+)\s+deletions?\(-\)'))) {
      if ($m.Value -match 'files? changed') { $files += ($m.Value -replace '\D','') }
      elseif ($m.Value -match 'insertions') { $ins   += ($m.Value -replace '\D','') }
      elseif ($m.Value -match 'deletions') { $del   += ($m.Value -replace '\D','') }
    }
  }
  [pscustomobject]@{ Files=$files; Insertions=$ins; Deletions=$del; LinesChanged=($ins+$del) }
}

# Collect modules: ROOT + submodules (recursive)
$modules = @([pscustomobject]@{ Name='ROOT'; Path='.' })
$subPaths = git submodule status --recursive 2>$null |
  ForEach-Object { ($_ -split '\s+',3)[1] } | Where-Object { $_ -and (Test-Path $_) }
$subPaths | ForEach-Object { $modules += [pscustomobject]@{ Name=$_; Path=$_ } }

$rows = @()
$totalF=0; $totalI=0; $totalD=0; $totalLC=0

foreach ($m in $modules) {
  Push-Location $m.Path
  $line = git diff HEAD --shortstat -M -C --no-ext-diff 2>$null
  $s = Parse-ShortStat $line
  Pop-Location

  if ($s.Files -or $s.Insertions -or $s.Deletions) {
    $rows += [pscustomobject]@{
      Module=$m.Name; Files=$s.Files; Insertions=$s.Insertions; Deletions=$s.Deletions; LinesChanged=$s.LinesChanged
    }
    $totalF += $s.Files; $totalI += $s.Insertions; $totalD += $s.Deletions; $totalLC += $s.LinesChanged
  }
}

$rows | Sort-Object Module | Format-Table -AutoSize

"---"
"TOTAL   Files: {0}   Insertions: {1}   Deletions: {2}   LinesChanged: {3}" -f $totalF, $totalI, $totalD, $totalLC
