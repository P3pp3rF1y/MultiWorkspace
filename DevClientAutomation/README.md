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
