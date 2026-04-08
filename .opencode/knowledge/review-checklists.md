# Mod Change Review Checklist

Use this checklist to classify findings and decide gate outcome.

Related standards:

- `code-standards.md` for coding and review quality rules (reflection, exception handling, nullability checks, and obviousness audit).
- `mod-architecture.md` for project-specific architecture and runtime safety expectations (including container mutation safety and client rendering API preferences).
- `style-standards.md` for style-focused review guidance.
- `performance-checklist.md` for performance-focused review guidance.
- `compatibility-checklist.md` for vanilla/mod compatibility-focused review guidance.
- `security-robustness-checklist.md` for trust-boundary and robustness-focused review guidance.

## Severity policy

- `blocker`: unsafe or clearly broken behavior; merge must stop.
- `major`: high-probability defect, compatibility issue, or architecture violation; merge must stop.
- `minor`: non-blocking quality issue; should be fixed soon.
- `nit`: style/readability suggestion; optional.

Gate rule: any `blocker` or `major` => final verdict `FAIL`.

## Core checks (always apply)

- Change minimality: implementation is as small and local as possible for the requested behavior.
- Side correctness: no client classes in common/server paths; no indirect/proxy references from common/server code into client-dependent project code.
- Lifecycle correctness: registration/events wired in expected phase and bus.
- Null/empty/error paths handled in gameplay-affecting logic.
- Player-visible path check: if change claims to show/notify player, verify at least one reachable display path and ensure "shown/success" state is committed only after display call is invoked.
- Feature scope: no unrelated refactor or behavior drift in a focused change.

## Concern split (for specialist passes)

- Style/standards: naming, readability, maintainability, local pattern consistency.
- Performance: hot-path cost, allocation churn, repeated work, sync frequency/size.
- Compatibility: side/lifecycle/registry correctness, version/loader compatibility, persistence/datagen compatibility.
- Security/robustness: trust-boundary validation, permission/authority checks, malformed input and crash resistance.

## Registry and initialization checks

- Uses existing module registration patterns (`DeferredRegister`, `registerHandlers`).
- IDs/names follow module naming conventions and avoid collisions.
- New content is initialized in the right module and not through cross-module shortcuts.

## Networking checks

- Payload registration exists and uses module pattern.
- Encode/decode and handler semantics are consistent and deterministic.
- Handler side assumptions are valid (logical client/server).
- Payload changes consider compatibility/versioning impacts.
- Suggestions must stay on the target MC/loader networking model; do not recommend 1.21+ APIs/patterns for 1.20 branches.

## Data and persistence checks

- Persistent schema changes include compatibility reasoning.
- Serialization format changes are deliberate and reviewed for load safety.
- Datagen inputs changed? Corresponding `runData` task should be run.

## Performance and safety checks

- No obvious hot-path allocations/loops introduced without need.
- Avoids duplicate work in tick/event/render critical paths.
- For low-level render/API code, verify equivalent vanilla/NeoForge patterns
  were checked first for the target version.
- Avoids raw `GL11` and sync GPU readbacks in render hot paths unless explicitly justified as unavoidable.
- Threading assumptions remain valid for game thread/network thread behavior.

## Review output contract

For each finding report:

- `severity`
- `location` (single path with optional line)
- `issue`
- `why`
- `suggested_fix`
- `confidence` (`high|medium|low`)

Then provide:

- `verdict`: `PASS` or `FAIL`
- `gate_reason`: one sentence tied to severity policy
