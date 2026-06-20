# Port Regression Suite

Reusable regression harness for future Minecraft/NeoForge ports. The goal is to run the high-risk port checks from suite metadata and produce a machine-readable report plus a Markdown checklist report.

## Usage

```powershell
pwsh -File .\scripts\port-regression\run-port-regression.ps1 -WorkspaceRoot "D:\Development\MultiWorkspace26.2" -Suite critical
pwsh -File .\scripts\port-regression\run-port-regression.ps1 -Suite recipe-viewers
pwsh -File .\scripts\port-regression\run-port-regression.ps1 -Suite render
pwsh -File .\scripts\port-regression\run-port-regression.ps1 -Suite all -ContinueOnFailure
pwsh -File .\scripts\port-regression\run-port-regression.ps1 -List
```

Reports are written to `workspace/run/port-regression/latest/`:

- `report.md`
- `report.json`
- `screenshots/`

## Result Statuses

- `passed`: test completed and assertions/log checks passed.
- `failed`: test ran and failed.
- `blocked`: test is dependency-gated and cannot run for the current port.
- `skipped`: test was not selected by the runner.

## Suite Files

Suite files live in `scripts/port-regression/suites/`. Prefer adding reusable test definitions there instead of adding one-off logic to the runner.
