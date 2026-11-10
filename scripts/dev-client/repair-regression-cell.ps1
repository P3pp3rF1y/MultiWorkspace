param(
    [Parameter(Mandatory = $true)] [string]$BaseUrl,
    # Explicit authorization covers the entire fixed support/cell below. Read the
    # fixture's complete blocker diagnostic before choosing this operation.
    [switch]$AuthorizeCellClear
)
$ErrorActionPreference = 'Stop'
if (-not $AuthorizeCellClear) {
    throw 'Cell repair requires -AuthorizeCellClear: replaces support x/z 252..260 at y=72 with stone, clears x/z 253..259 at y=73..78, and relocates non-player entities from x/z 252..261, y=72..80 to 245 73 245.'
}

# The caller supplies a running bridge. Never launch, attach ownership, or stop
# that client here. Relocate existing entities first, then any container drops
# created by clearing. No entity is deleted, including unrelated nearby items.
$commands = @(
    'execute as @e[type=!minecraft:player,x=252,y=72,z=252,dx=9,dy=8,dz=9] run tp @s 245 73 245',
    'fill 253 73 253 259 78 259 minecraft:air',
    'fill 252 72 252 260 72 260 minecraft:stone',
    'execute as @e[type=!minecraft:player,x=252,y=72,z=252,dx=9,dy=8,dz=9] run tp @s 245 73 245'
)
$results = @()
foreach ($command in $commands) {
    $result = Invoke-RestMethod -Method Post -Uri "$($BaseUrl.TrimEnd('/'))/command" -ContentType 'application/json' -Body (@{ command = $command } | ConvertTo-Json -Compress) -TimeoutSec 30
    if (-not $result.ok) { throw "Cell repair command failed: $command; $($result | ConvertTo-Json -Depth 8 -Compress)" }
    $results += [pscustomobject]@{ command = $command; response = $result }
}
# /command acknowledges dispatch, not Brigadier success. The fixture's next
# region reset is the authoritative verification; never manufacture a pass.
[pscustomobject]@{ commandsDispatched = $true; requiresFixtureReset = $true; commands = $results; entitiesRelocationTarget = '245 73 245'; clientStopped = $false }
