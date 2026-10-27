param([string]$WorkspaceRoot = (Resolve-Path "$PSScriptRoot\..\..").Path)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/client-lifecycle.ps1"
$directory = Join-Path $WorkspaceRoot ('build/reports/dev-client-regression/startup-readiness/' + [guid]::NewGuid())
$null = New-Item -ItemType Directory -Force -Path $directory
$python = (Get-Command python -ErrorAction Stop).Source
function Assert-True { param([bool]$Condition, [string]$Message) if (-not $Condition) { throw $Message } }
$fixture = @'
import json, os, sys
from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
root=Path(sys.argv[1]); mode=sys.argv[2]; loader=sys.argv[3]; version=sys.argv[4]; loaded=False
class Client(BaseHTTPRequestHandler):
 def log_message(self,*args): pass
 def respond(self):
  global loaded
  self.rfile.read(int(self.headers.get('Content-Length',0)))
  stop=False
  if self.path=='/state':
   player=mode not in ('pending-world','bridge-only')
   screen='ReceivingLevelScreen' if mode=='receiving' or (mode=='pending-world' and loaded) else ('TitleScreen' if not player else 'None')
   response={'playerLoaded':player,'screenSimpleName':screen}
  elif self.path=='/capabilities': response={'ok':True,'protocolVersion':1,'loader':loader,'minecraftVersion':version,'backgroundInput':True}
  elif self.path=='/world/load': loaded=True; response={'ok':True}
  elif self.path=='/recipe-viewer/state': response={'ok':True,'indexStackCount':0 if mode=='pending-viewer' else 1}
  elif self.path=='/client/stop': response={'ok':True}; stop=True
  else: self.send_error(404); return
  data=json.dumps(response).encode(); self.send_response(200); self.send_header('Content-Type','application/json'); self.send_header('Content-Length',str(len(data))); self.end_headers(); self.wfile.write(data)
  if stop: self.server.stopped=True
 def do_GET(self): self.respond()
 def do_POST(self): self.respond()
server=HTTPServer(('127.0.0.1',0),Client); (root/'workspace/run/dev-client-automation.json').write_text(json.dumps({'host':'127.0.0.1','port':server.server_port,'processId':os.getpid()})); server.stopped=False
while not server.stopped: server.handle_request()
'@
$fixture | Set-Content -LiteralPath "$directory/client.py" -Encoding utf8
foreach ($mode in @('pending-world', 'receiving', 'pending-viewer', 'ready', 'ready-viewer', 'bridge-only')) {
    $root = Join-Path $directory $mode
    $null = New-Item -ItemType Directory -Force -Path "$root/workspace/run"
    "@`"$python`" `"$directory/client.py`" `"$root`" $mode neoforge 26.2" | Set-Content -LiteralPath "$root/gradlew.bat" -Encoding ascii
    $ownership = $null; $cleanup = $null; $failure = $null; $launch = $null
    try {
        try {
            $launch = & "$PSScriptRoot/start-and-ready.ps1" -WorkspaceRoot $root -TimeoutSeconds 6 -CloseOnExit -MinimalRuntime -SkipRecipeViewerReady:($mode -notin @('pending-viewer', 'ready-viewer')) -LoadWorld:($mode -ne 'bridge-only')
            $ownership = $launch.ownership
        } catch {
            $failure = $_
            $ownership = $_.Exception.Data['ClientOwnership']
            $cleanup = $_.Exception.Data['ClientLifecycle']
        }
        $expectedFailure = $mode -in @('pending-world', 'receiving', 'pending-viewer')
        Assert-True (($null -ne $failure) -eq $expectedFailure) "$mode produced the wrong startup outcome."
        Assert-True ($null -ne $ownership -and $ownership.processId) "$mode lost owned startup identity."
        if ($expectedFailure) {
            Assert-True ($failure.Exception.Message -like '*world/player or recipe-viewer readiness*' -and $null -eq $launch) "$mode returned a launch product or lost its readiness diagnosis."
        } else {
            Assert-True ($launch.baseUrl -and $launch.capabilities.backgroundInput) "$mode did not return a ready launch."
        }
    } finally {
        if ($null -ne $ownership -and $null -eq $cleanup) { $cleanup = Stop-OwnedClient -Ownership $ownership -WaitForRegression:$false }
        @{ ownership = $ownership; lifecycle = $cleanup; error = if ($failure) { $failure.Exception.Message } else { $null } } | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath "$root/evidence.json"
    }
    Assert-True ($cleanup.ok -and -not $cleanup.forced -and -not (Get-Process -Id $ownership.processId -ErrorAction SilentlyContinue)) "$mode did not cleanly stop only its owned client."
}
[pscustomobject]@{ ok = $true; scenarios = 6; artifacts = $directory }
