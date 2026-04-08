---
description: Iterate on a mod design until architecture review converges
agent: general
subtask: true
---

Run an iterative design loop for a planned mod change before coding.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/version-rules.md`
- `../knowledge/design-principles.md`
- `../knowledge/mod-architecture.md`
- `../knowledge/version-reference-index.md`

User context:
$ARGUMENTS

Loop contract:

1. Draft or refine an implementation plan from the user context.
2. Run an independent architecture review pass with `mod-architect`.
3. If design status is `REVISE`, adjust the plan minimally and repeat.
4. Stop when status is `APPROVE` or max rounds are exhausted.

Defaults:

- `MAX_DESIGN_ROUNDS=2` unless user context sets a different value.
- Persist the converged design as an artifact file before returning final output.
- Default artifact path: `.opencode/design-artifacts/<kebab-design-target>-design.md` unless user context sets `DESIGN_ARTIFACT_PATH`.
- Preferred for shared multi-project workspaces: set `DESIGN_ARTIFACT_PATH` to a parent-relative folder such as `../design-artifacts/<kebab-design-target>-design.md`.
- This command is design-only. Do not implement code.
- Keep adjustments minimal and localized.
- Keep guidance aligned to target module version and loader.
- Avoid recommending cross-version networking/API migrations unless the task is explicitly a port.

Strict design gate:

- `APPROVE` means design is ready for implementation.
- `REVISE` means unresolved design risks remain.
- If max rounds are exhausted with `REVISE`, return unresolved risks and an exact next patch order.

Required reporting per round:

- Round number
- Architect status (`APPROVE|REVISE`) and one-line rationale
- Design adjustments applied in that round
- Remaining major design risks (if any)

Artifact requirements:

- Write a design artifact containing: scope, assumptions, constraints, module/version targets, final recommended plan, risk list, and explicit build handoff notes.
- Treat the artifact as the source of truth for downstream build commands.
- Ensure the final output includes the artifact path.

Output format:

Return exactly this shape:

```text
design_target: <brief>
design_artifact_path: <path to saved design artifact>
rounds_executed: <int>
design_status: APPROVE|REVISE
rounds:
  - round: <int>
    architect_status: APPROVE|REVISE
    rationale: <one-line architect rationale>
    adjustments_applied:
      - <short adjustment>
    remaining_major_risks:
      - <short risk>
final_recommended_plan:
  - <ordered implementation step>
next_patch_order:
  - <ordered design-only patch step>
gate_reason: <single sentence tied to design_status and gate policy>
```

Schema notes:

- Always include all keys in the order shown.
- Use an empty list (`[]`) for `remaining_major_risks` when none remain.
- Use an empty list (`[]`) for `next_patch_order` when `design_status: APPROVE`.
