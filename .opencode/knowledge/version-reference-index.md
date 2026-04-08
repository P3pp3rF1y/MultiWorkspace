# Version Reference Index (Optional Deltas)

Use this file for version- or loader-specific review deltas that cannot be expressed generically.

Keep the base knowledge files generic. Add entries here only when a rule truly starts or changes at a specific boundary.

## How to use

- Determine target from module `gradle.properties` (`minecraft_version`, `neo_version`/`forge_version`).
- Apply matching delta entries in addition to base rules.
- If no entry matches, rely only on base rules.

## Entry format

```text
- id: <short-id>
  applies_when:
    mc: <range expression>
    loader: neoforge|forge|either
    loader_version: <optional range>
  scope: api|lifecycle|network|datagen|rendering|compat
  rule: <short requirement>
  rationale: <why this differs from base rules>
  severity_default: blocker|major|minor
  confidence_notes: <what evidence raises/lowers confidence>
```

## Ranges

- Prefer inclusive minimum ranges, for example `>=1.20.1`.
- When behavior changes again, add a new entry with a tighter range.
- Avoid branch-name coupling; use semantic version ranges only.

## Initial guidance

- Start with no hardcoded entries unless there is confirmed source-backed divergence.
- Add entries from real review findings and confirmed porting pain points.
- Remove stale entries when the base rule can be generalized again.

## Seed deltas

```text
- id: network-api-line-boundary
  applies_when:
    mc: >=1.20
    loader: either
  scope: network
  rule: Review and suggestions must preserve the networking API family already used by the target branch; do not suggest migrating to another MC line's payload/registration model unless the task is explicitly a port.
  rationale: Networking abstractions and registration APIs differ by MC line and loader evolution.
  severity_default: major
  confidence_notes: High when branch/module code clearly uses one established model; medium when module has mixed transitional patterns.
```
