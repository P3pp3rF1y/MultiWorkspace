---
description: Iterate implementation with strict review gate until pass
agent: general
subtask: true
---

Run an implementation + review loop for a mod change using strict quality gates.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/version-rules.md`
- `../knowledge/code-standards.md`
- `../knowledge/review-checklists.md`
- `../knowledge/mod-architecture.md`
- `../knowledge/compatibility-checklist.md`
- `../knowledge/version-reference-index.md`

User context:
$ARGUMENTS

Precondition:

- Design is already approved or otherwise provided in user context.
- This command does not perform design-phase architecture convergence.
- Use `DESIGN_ARTIFACT_PATH` when available and treat that artifact as the primary design source of truth.
- For shared multi-project workspaces, prefer a parent-relative artifact path (for example `../design-artifacts/<kebab-design-target>-design.md`) over machine-specific absolute paths.
- If design details in prompt conflict with the artifact, prefer the artifact and report the conflict in round notes.

Loop contract:

1. Apply or update implementation for the requested change.
2. Run a dedicated review pass using `/review-mod-change`.
   - For player-facing hints/messages/UI, explicitly verify at least one reachable display path exists and that "shown/success" state is only recorded after display is invoked.
3. If review returns `FAIL`, address blocker/major findings and repeat.
4. Stop when review returns `PASS` or after max rounds.

Defaults:

- `MAX_ROUNDS=3` unless user context sets a different value.
- Focus fixes on review findings; avoid unrelated refactors.

Strict gate policy:

- `FAIL` means blocker/major findings remain.
- Do not claim completion while status is `FAIL`.
- If max rounds are exhausted, return remaining findings and a concrete next patch plan.

Required reporting per round:

- Round number
- Brief implementation delta
- Review verdict and gate reason
- Findings addressed in that round
- Remaining blocker/major findings (if any)

Final output:

- If pass: include `final_verdict: PASS` and brief residual minor/nit list.
- If not pass: include `final_verdict: FAIL`, unresolved blockers/majors, and exact next-step patch order.
