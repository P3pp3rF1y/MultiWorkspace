---
description: Generate a concise end-of-day agentic AI work report
agent: general
---

Generate a concise end-of-day report about agentic AI work.

User context:
$ARGUMENTS

Source selection:

- Use an attached context file if one is provided.
- Otherwise use the current conversation context.
- If both exist, the attached context file is the primary source of truth.

Workflow:

1. Extract evidence for completed work, issues or repeated prompts, and context improvements.
2. Write exactly three sections with one to three sentences each.
3. If evidence for a section is weak, include the phrase `Not clearly evidenced.` and stay conservative.

Guardrails:

- Do not invent facts outside the selected source.
- Keep wording concrete and specific.
- Prefer outcomes, bottlenecks, and explicit context-improvement actions.

Output format:

```text
What I did today with agentic AI:
<1-3 sentences>

Issues / repeated prompts I ran into:
<1-3 sentences>

What I improved for faster convergence:
<1-3 sentences>
```
