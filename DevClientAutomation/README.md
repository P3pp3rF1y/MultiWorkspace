# Dev Client Automation

This module provides a dev-only local HTTP bridge for in-game testing from
automation tools.

Recipe-viewer automation endpoints are intentionally viewer-neutral. The loaded
recipe-viewer mod and the matching adapter determine whether JEI, EMI, or REI
handles the request.

Current recipe-viewer endpoints:

- `GET /recipe-viewer/state`
- `POST /recipe-viewer/query`
- `POST /recipe-viewer/search`
- `POST /recipe-viewer/open`
- `POST /client/stop`

Use `POST /mouse/move` before screenshots to avoid tooltip overlap, and
`POST /window/maximize` before visual recipe-viewer checks when possible.

## Recipe-Viewer Regression Suites

Run the same logical recipe-viewer scenario suite against a specific loaded
viewer with:

```powershell
.\scripts\dev-client\run-recipe-viewer-regression.ps1 -Viewer emi -Suite sophisticatedbackpacks
.\scripts\dev-client\run-recipe-viewer-regression.ps1 -Viewer jei -Suite sophisticatedbackpacks
.\scripts\dev-client\run-recipe-viewer-regression.ps1 -Viewer rei -Suite sophisticatedbackpacks
```

Suites live under `scripts/dev-client/recipe-viewer-suites`. They validate the
runtime viewer API data exposed by `DevClientAutomation`; the existing JUnit
`recipeViewerRegression` tests remain the faster common catalog checks.

When the regression runner starts the client itself, it stops that client through
`POST /client/stop` in a `finally` block after the suite completes or fails.

## Inventory Interaction Regression

Run the vanilla and Sophisticated inventory keybind regression with:

```powershell
.\scripts\dev-client\run-inventory-interactions-regression.ps1 -MinimalRuntime
```

The regression covers both transfer directions with and without the Shift
override, plus sorting vanilla chests, player inventory, and backpacks.

## Backpack Regression

Run the Sophisticated Backpacks GUI regression with:

```powershell
.\scripts\dev-client\run-backpack-regression.ps1 -MinimalRuntime
```

The suite includes an Inception magnet persistence scenario. It opens a
backpack GUI, triggers a real item-entity pickup, closes the GUI, restarts the
client, and checks the nested backpack after world reload.

## Inventory Interaction Regression

Run the vanilla and Sophisticated inventory keybind regression with:

```powershell
.\scripts\dev-client\run-inventory-interactions-regression.ps1 -MinimalRuntime
```

The regression covers both transfer directions with and without the Shift
override, plus sorting vanilla chests, player inventory, crafting tables,
furnaces, and backpacks.

## Storage Controller Filter Regression

Run the controller filter routing regression with:

```powershell
.\scripts\dev-client\run-storage-controller-filter-regression.ps1 -MinimalRuntime
.\scripts\dev-client\run-storage-controller-filter-regression.ps1 -MinimalRuntime -Jfr
.\scripts\dev-client\run-storage-controller-filter-regression.ps1 -MinimalRuntime -Jfr -Runs 200
```

The suite creates a controller with about 60 connected storages, including locked
seeded barrels, input-filtered barrels, a deny-list filter barrel, and nearby
overflow barrels. It performs many controller inserts and asserts that items land
in the expected storages. The `-Jfr` option dispatches Minecraft's `jfr start`
and `jfr stop` commands around the insert-routing profile. JFR mode performs
setup before recording, profiles repeated real controller inserts against that
fixed setup, then runs full verification after recording. The fixed JFR setup
adds storage stack upgrades for profiling capacity. JFR runs default to 1000
profile repetitions so most inserts have room while later inserts can exercise
full/no-space paths; use `-Runs` to override the repetition count.
