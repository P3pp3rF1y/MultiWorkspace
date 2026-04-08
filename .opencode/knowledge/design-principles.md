# Mod Design Principles (MC 1.20+)

Use these principles during design-phase review before implementation.

## Goals

- Keep solutions minimal and localized to the requested feature.
- Reuse existing module patterns before adding new abstractions.
- Preserve save/network compatibility unless the task explicitly requires a breaking change.

## Architectural fit

- Put shared logic in the right module (`SophisticatedCore` vs feature modules).
- Keep compat behavior in compat paths, not core feature paths, unless broadly reusable.
- Favor extension points already present in the codebase over parallel systems.

## Side and lifecycle safety

- Design common/server flow so it never depends on client-only classes, directly or indirectly.
- Use side-specific wiring patterns already used by the target branch/module.
- Ensure registration/event lifecycle placement matches existing module conventions.

## Portability awareness

- Keep design concepts portable across maintained MC lines.
- Isolate version-specific API details behind existing branch/module adaptation points.
- Do not hardcode assumptions from a different MC/loader line.

## Pattern selection

- Prefer patterns that are common and well-understood in Minecraft mod development when a similar problem already exists in that domain.
- If a common vanilla/mod pattern is known to be outdated or has clear code smells, adapt it to a cleaner modern variant instead of copying it blindly.
- If no good Minecraft/mod precedent exists, use proven software design patterns from outside the mod domain, then map them back to existing module conventions.

## Design review output expectations

- Call out risks early (side safety, lifecycle, persistence, networking, datagen).
- Propose the smallest viable design adjustment for each risk.
- Prefer concrete patch direction over broad refactor plans.
