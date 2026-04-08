---
description: Export the most recent OpenCode sessions as JSON files
agent: general
---

Help the user export the most recent OpenCode sessions as JSON files for later review.

Arguments:

- `$1` = number of most recent sessions to export
- `$2` = optional output directory

Defaults:

- If `$1` is missing, use `10`.
- If `$2` is missing, export to `.opencode/session-exports`.

Recommended command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ../AgenticAI/tools/export-opencode-sessions.ps1 10
```

If arguments were provided, adapt the command accordingly.

Rules:

- Prefer `../AgenticAI/tools/export-opencode-sessions.ps1`.
- Treat the first argument as the recent-session count.
- Treat the second argument as the output directory when present.
- If the user asked you to run the export, run the script and report the results.
- If the user only asked how to do it, return the exact command they should run.

After the export completes:

- Report how many sessions were exported.
- Show the manifest path.
- Suggest using `/review-session-improvements` on the exported files next.
