param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path)

$ErrorActionPreference = 'Stop'
$controller = Join-Path $PSScriptRoot 'run-restart-regression.ps1'
$fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) ("restart-regression-" + [guid]::NewGuid())

function Assert-True { param([bool]$Condition, [string]$Message) if (-not $Condition) { throw $Message } }

try {
    $null = New-Item -ItemType Directory -Force -Path "$fixtureRoot/workspace/run"
    $python = (Get-Command python -ErrorAction Stop).Source
    $fixture = @'
import json, os, secrets, sys
from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
root=Path(sys.argv[1]); world=root/'world.json'; last={}; server_boot=secrets.token_urlsafe(12)
class Client(BaseHTTPRequestHandler):
 def log_message(self,*args): pass
 def respond(self):
  global last
  stop=False
  if self.path=='/state': response={'playerLoaded':True,'screenSimpleName':'TitleScreen'}
  elif self.path=='/capabilities': response={'ok':True,'protocolVersion':1,'loader':'neoforge','minecraftVersion':'fixture','backgroundInput':True}
  elif self.path=='/regression/status': response=last
  elif self.path=='/regression/cancel': response={'outcome':'cancelled'}
  elif self.path=='/client/stop':
    if last.get('diagnostics',{}).get('stopFails'): self.send_error(500); return
    response={'ok':True}; stop=True
  elif self.path=='/regression/run':
   body=json.loads(self.rfile.read(int(self.headers['Content-Length']))); phase=body['testId']
   if phase in ('prepare','cleanup'): state={'token':secrets.token_urlsafe(24),'preparedBy':os.getpid(),'world':str(world),'boot':server_boot,'stopFails':phase=='cleanup'}; world.write_text(json.dumps(state)); ok=True
   else: state=json.loads(world.read_text()); ok=phase!='wrong' and body.get('preparationToken')==state['token'] and state['preparedBy']!=os.getpid()
   diagnostics={'restartPreparationToken':state['token'],'serverBootId':state['boot'] if phase=='bad-boot' else server_boot,'restartPreparationBootId':state['boot'],'preparedBy':state['preparedBy'],'verifiedBy':os.getpid(),'stopFails':state.get('stopFails',False)}
   response={'ok':ok,'runId':'restart/'+phase,'suiteId':body['suiteId'],'testId':phase,'outcome':'passed' if ok else 'failed','category':'' if ok else 'assertion','message':'fixture','durationMillis':1,'testDurationMillis':1,'outcomePriority':0 if ok else 5,'conformance':[],'runtimeConformance':[],'cleanup':[],'steps':[],'diagnostics':diagnostics,'rerunCommand':body['reportContext']['rerunCommand'],'runtimeIdentity':body['reportContext']['runtimeIdentity'],'artifacts':body['reportContext']['artifacts'],'reportCases':[{'testId':phase,'classname':'fixture','role':'scenario','outcome':'passed' if ok else 'failed','category':'' if ok else 'assertion','message':'fixture','durationMillis':1,'junitElement':'' if ok else 'failure','diagnostics':diagnostics}]}; last=response
  else: self.send_error(404); return
  data=json.dumps(response).encode(); self.send_response(200); self.send_header('Content-Type','application/json'); self.send_header('Content-Length',str(len(data))); self.end_headers(); self.wfile.write(data)
  if stop: self.server.stopped=True
 def do_GET(self): self.respond()
 def do_POST(self): self.respond()
server=HTTPServer(('127.0.0.1',0),Client); (root/'workspace/run/dev-client-automation.json').write_text(json.dumps({'host':'127.0.0.1','port':server.server_port,'processId':os.getpid()})); server.stopped=False
while not server.stopped: server.handle_request()
'@
    $fixture | Set-Content -LiteralPath "$fixtureRoot/restart_fixture.py" -Encoding utf8
    "@`"$python`" `"$fixtureRoot/restart_fixture.py`" `"$fixtureRoot`"" | Set-Content -LiteralPath "$fixtureRoot/gradlew.bat" -Encoding ascii

    $artifacts = Join-Path $fixtureRoot 'reports'
    $result = & $controller -WorkspaceRoot $fixtureRoot -Suite linked-storage-restart -PrepareTest prepare -VerifyTest verify -ArtifactDirectory $artifacts -TimeoutSeconds 20 -CandidateIdentity fixture-candidate -RuntimeBuildIdentity fixture-runtime
    Assert-True $result.ok 'The maintained restart controller did not complete the fixture flow.'
    Assert-True ($result.worldName -eq 'Dev Client Automation Void Platform') 'The restart controller did not retain its stable world selection.'
    Assert-True ($result.prepareServerBootId -eq $result.prepareRestartPreparationBootId -and $result.prepareServerBootId -eq $result.verifyRestartPreparationBootId) 'The restart controller did not retain checkpoint boot provenance.'
    Assert-True ($result.verifyServerBootId -ne $result.prepareServerBootId) 'The restart controller accepted a reused server boot.'
    Assert-True ("$($result.prepareProcessId)/$($result.prepareProcessStartedAtUtc)" -ne "$($result.verifyProcessId)/$($result.verifyProcessStartedAtUtc)") 'The restart controller reused a client process identity.'
    foreach ($path in @($result.prepareOwnership, $result.verifyOwnership, $result.prepareLifecycle, $result.verifyLifecycle)) { Assert-True (Test-Path -LiteralPath $path) "Missing lifecycle record $path" }
    $token = (Get-Content -LiteralPath "$fixtureRoot/world.json" -Raw | ConvertFrom-Json).token
    $reports = Get-ChildItem -LiteralPath $artifacts -Recurse -Filter '*.json' | Get-Content -Raw
    Assert-True (-not ($reports -join "`n").Contains($token)) 'A preparation token leaked into a durable report.'
    Assert-True ((Get-Content -LiteralPath $result.verify.json -Raw | ConvertFrom-Json).outcome -eq 'passed') 'Verify did not retain the native terminal outcome.'

    $reuseRejected = $false
    try { & $controller -WorkspaceRoot $fixtureRoot -Suite linked-storage-restart -PrepareTest prepare -VerifyTest verify -ArtifactDirectory $artifacts -TimeoutSeconds 20 | Out-Null } catch { $reuseRejected = $true }
    Assert-True $reuseRejected 'The restart controller accepted a reused artifact directory.'

    $failedLaunchRoot = Join-Path $fixtureRoot 'failed-launch-root'
    $null = New-Item -ItemType Directory -Force -Path "$failedLaunchRoot/workspace/run"
    '@exit /b 1' | Set-Content -LiteralPath "$failedLaunchRoot/gradlew.bat" -Encoding ascii
    $failedLaunchArtifacts = Join-Path $fixtureRoot 'failed-launch'
    $failed = $false
    try { & $controller -WorkspaceRoot $failedLaunchRoot -Suite linked-storage-restart -PrepareTest prepare -VerifyTest verify -ArtifactDirectory $failedLaunchArtifacts -TimeoutSeconds 1 | Out-Null } catch { $failed = $true }
    Assert-True $failed 'A failed launch was accepted.'
    Assert-True ((Get-ChildItem -LiteralPath $failedLaunchArtifacts -Filter '*prepare-ownership.json').Count -eq 1) 'Failed launch did not persist startup ownership.'
    Assert-True ((Get-ChildItem -LiteralPath $failedLaunchArtifacts -Filter '*prepare-lifecycle.json').Count -eq 1) 'Failed launch did not persist startup cleanup.'

    $failed = $false
    try { & $controller -WorkspaceRoot $fixtureRoot -Suite linked-storage-restart -PrepareTest missing -VerifyTest verify -ArtifactDirectory (Join-Path $fixtureRoot 'failed-prepare') -TimeoutSeconds 20 | Out-Null } catch { $failed = $true }
    Assert-True $failed 'A failed prepare phase was accepted.'
    Assert-True ((Get-ChildItem -LiteralPath "$fixtureRoot/failed-prepare" -Filter '*lifecycle.json').Count -gt 0) 'Failed prepare did not persist owned cleanup.'

    $failedCleanupArtifacts = Join-Path $fixtureRoot 'failed-cleanup'
    $failed = $false
    try { & $controller -WorkspaceRoot $fixtureRoot -Suite linked-storage-restart -PrepareTest cleanup -VerifyTest verify -ArtifactDirectory $failedCleanupArtifacts -TimeoutSeconds 20 | Out-Null } catch { $failed = $true }
    Assert-True $failed 'A failed prepare cleanup was accepted.'
    Assert-True ((Get-Content -LiteralPath (Get-ChildItem -LiteralPath $failedCleanupArtifacts -Filter '*prepare-lifecycle.json').FullName -Raw | ConvertFrom-Json).ok -eq $false) 'Failed prepare cleanup did not retain its lifecycle outcome.'

    $failedVerifyArtifacts = Join-Path $fixtureRoot 'failed-verify'
    $captured = [System.Collections.Generic.List[object]]::new()
    $failed = $false
    try { & $controller -WorkspaceRoot $fixtureRoot -Suite linked-storage-restart -PrepareTest prepare -VerifyTest wrong -ArtifactDirectory $failedVerifyArtifacts -TimeoutSeconds 20 *>&1 | ForEach-Object { [void]$captured.Add($_) } } catch { $failed = $true; [void]$captured.Add($_) }
    Assert-True $failed 'A failed verify phase was accepted.'
    $failedVerifyReport = Join-Path $failedVerifyArtifacts 'wrong/linked-storage-restart-wrong.json'
    Assert-True (Test-Path -LiteralPath $failedVerifyReport) 'Failed verify did not retain its native report.'
    Assert-True ((Get-Content -LiteralPath $failedVerifyReport -Raw | ConvertFrom-Json).outcome -eq 'failed') 'Failed verify native outcome was not retained.'
    Assert-True ((Get-ChildItem -LiteralPath $failedVerifyArtifacts -Filter '*lifecycle.json').Count -ge 2) 'Failed verify did not persist both owned cleanup records.'
    $failedVerifyToken = (Get-Content -LiteralPath "$fixtureRoot/world.json" -Raw | ConvertFrom-Json).token
    Assert-True (-not (($captured | Out-String).Contains($failedVerifyToken))) 'A preparation token leaked through controller output or errors.'
    Assert-True (-not ((Get-ChildItem -LiteralPath $failedVerifyArtifacts -Recurse -Filter '*.json' | Get-Content -Raw | Out-String).Contains($failedVerifyToken))) 'A failed-verify preparation token leaked into durable records.'
    $failedBootArtifacts = Join-Path $fixtureRoot 'failed-boot'
    $failedBoot = $false
    try { & $controller -WorkspaceRoot $fixtureRoot -Suite linked-storage-restart -PrepareTest prepare -VerifyTest bad-boot -ArtifactDirectory $failedBootArtifacts -TimeoutSeconds 20 | Out-Null } catch { $failedBoot = $true }
    Assert-True $failedBoot 'The restart controller accepted a reused server boot.'
    Assert-True ((Get-Content -LiteralPath "$failedBootArtifacts/bad-boot/linked-storage-restart-bad-boot.json" -Raw | ConvertFrom-Json).outcome -eq 'passed') 'Boot provenance rejection did not retain the native verify outcome.'
    'PASS: restart controller fixture success, launch/prepare/verify cleanup, same-world restart, and token redaction.'
} finally {
    if (Test-Path -LiteralPath $fixtureRoot) { Remove-Item -LiteralPath $fixtureRoot -Recurse -Force }
}
