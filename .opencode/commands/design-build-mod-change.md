---
description: Run a quick design convergence then implementation review loop
agent: general
subtask: true
---

Run a convenience flow for smaller mod changes that combines design and build phases.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/version-rules.md`
- `../knowledge/design-principles.md`
- `../knowledge/review-checklists.md`
- `../knowledge/mod-architecture.md`
- `../knowledge/compatibility-checklist.md`
- `../knowledge/version-reference-index.md`

User context:
$ARGUMENTS

Flow contract:

1. Run `/design-mod-solution` first.
2. Default to quick design mode (`MAX_DESIGN_ROUNDS=1`) unless user context sets otherwise.
3. If final design status is `APPROVE`, pass `design_artifact_path` to `/build-review-loop` as `DESIGN_ARTIFACT_PATH` and continue.
4. If final design status is `REVISE`, stop and return unresolved design risks.

Defaults:

- Quick mode by default for simple changes.
- Preserve strict gates in both phases; do not force progress on `REVISE` or `FAIL`.
- Focus fixes on findings from each phase; avoid unrelated refactors.

Output format:

Return exactly this shape:

```text
workflow_target: <brief>
design_artifact_path: <path to saved design artifact from design phase>
design_phase:
  rounds_executed: <int>
  design_status: APPROVE|REVISE
  key_adjustments:
    - <short adjustment>
  unresolved_design_risks:
    - <short risk>
  next_design_patch_order:
    - <ordered design-only patch step>
build_phase:
  started: true|false
  rounds_executed: <int>
  final_review_verdict: PASS|FAIL|NOT_RUN
  findings_addressed:
    - <short finding handled>
  unresolved_blocker_major_findings:
    - <path[:line]>: <one-line issue>
  next_implementation_patch_order:
    - <ordered implementation patch step>
final_verdict: PASS|FAIL
gate_reason: <single sentence tied to the first failing gate, or pass condition>
residual_minor_nit:
  - <short item>
```

Schema notes:

- Always include all keys in the order shown.
- Always include `design_artifact_path`; if design did not converge, still point to the latest artifact draft.
- If design ends `REVISE`, set `build_phase.started: false`, `build_phase.rounds_executed: 0`, `build_phase.final_review_verdict: NOT_RUN`, and use empty lists for all other build-phase lists.
- If final verdict is `PASS`, use empty lists for `design_phase.unresolved_design_risks`, `design_phase.next_design_patch_order`, `build_phase.unresolved_blocker_major_findings`, and `build_phase.next_implementation_patch_order`.
- Keep `final_verdict: FAIL` when either design stops at `REVISE` or build ends with `final_review_verdict: FAIL`.
