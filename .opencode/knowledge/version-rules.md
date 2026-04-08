# Version-Aware Review Rules (MC 1.20+)

Reviewer behavior must adapt to the target module/version and avoid cross-version assumptions.

This file is intentionally version-agnostic and applies across maintained Minecraft lines.

## Version source of truth

- Read module `gradle.properties` values for:
  - `minecraft_version`
  - `neo_version` and/or `forge_version` (depending on module/branch)
- Use those values as the active review target for API and lifecycle expectations.
- Treat module-local values as authoritative; do not assume all modules share identical patch versions.

## Rule application order

1. Apply local project conventions first.
2. Apply loader conventions (NeoForge/Forge) for the target module.
3. Apply version-specific deltas only when needed.

## Uncertainty policy

- Do not guess framework internals for APIs/events that may differ across versions.
- If source evidence is missing, lower confidence and explicitly call out uncertainty.
- Prefer “needs source confirmation” over speculative blockers unless risk is clearly high.

## Cross-version safety heuristics

- Imports, method names, or event classes known to differ across mappings/version lines are high risk.
- Client rendering APIs are especially sensitive to version drift.
- Registry and event lifecycle behavior should match existing patterns already present in the same module and branch.
- Serialization, attachment/data-component, and payload format changes are compatibility-sensitive across ports.
- Networking transport/registration patterns differ across MC lines; never suggest cross-line API replacements unless explicitly porting.

## Side-gating expectations

- Do not require vanilla `@OnlyIn(Dist.CLIENT)` usage in mod code.
- Enforce practical side safety via branch-consistent loader checks and side-specific wiring patterns already used in the target module.

## Loader guidance boundary

- Prefer the loader conventions actually used by the target module (NeoForge or Forge).
- Mention alternatives only when directly relevant to portability or migration.
- Do not enforce speculative best practices that contradict established project patterns.
