---
description: Review a mod change with strict blocker/major gate
agent: mod-reviewer
subtask: true
---

Review the requested mod change and return a strict gate verdict.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/version-rules.md`
- `../knowledge/review-checklists.md`
- `../knowledge/mod-architecture.md`
- `../knowledge/compatibility-checklist.md`
- `../knowledge/performance-checklist.md`
- `../knowledge/security-robustness-checklist.md`
- `../knowledge/version-reference-index.md`

User context:
$ARGUMENTS

Execution policy:

- This is an independent reviewer pass.
- `mod-reviewer` acts as an aggregator over specialist concern passes.
- If any `blocker` or `major` finding exists, return `verdict: FAIL`.
- Do not relax severity to force a pass.

Bypass policy:

- Only skip review when user context explicitly includes `SKIP_REVIEW:` followed by a reason.
- If bypass is present, return:

```text
review_target: <from user context>
module_versions: []
findings: []
verdict: PASS
gate_reason: Review skipped due to explicit SKIP_REVIEW request: <reason>
```

Review focus:

- Apply architecture expectations from the knowledge base.
- Apply generic base knowledge first, then any matching entries from the version reference index.
- Run and aggregate concern-specific findings for:
  - style and standards
  - performance
  - vanilla/mod compatibility (including side/lifecycle and datagen compatibility checks)
  - security and robustness

Output format:

- Use the exact output contract defined by the `mod-reviewer` agent.
