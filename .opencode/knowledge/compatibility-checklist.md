# Compatibility Checklist

Use this checklist for vanilla/mod compatibility-focused review findings.

## Side and lifecycle

- No client-only classes referenced from common/server code.
- Event subscriptions and registries are wired in expected lifecycle phase.
- Initialization order follows local module conventions.

## Vanilla behavior compatibility

- Core gameplay behavior remains consistent with expected vanilla semantics unless explicitly changed.
- Feature toggles/config defaults do not unexpectedly alter vanilla behavior.

## Mod and version compatibility

- Changes align with target MC/loader APIs for the module.
- No accidental dependency on APIs from other maintained version lines.
- Network payload/schema changes include compatibility reasoning.

## Persistence and datagen

- Save format changes include backward-compatibility or migration plan.
- Datagen input changes are flagged for required execution of the module's
  configured datagen run task (for example `runData`/`runClientData`).

## Severity calibration

- `blocker`: crash-risk or hard incompatibility path.
- `major`: high-likelihood compatibility defect.
- `minor`: limited-scope correctness concern.
- `nit`: non-blocking cleanup or clarity suggestion.
