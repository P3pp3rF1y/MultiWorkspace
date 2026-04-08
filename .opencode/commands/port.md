---
description: Apply changes from this workspace to other supported workspaces
agent: general
---

Apply changes from this workspace to all other supported Minecraft version workspaces.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/code-standards.md`
- `../knowledge/multiworkspace-scope.md`
- `../knowledge/porting-knowledge.md`

User context:
$ARGUMENTS

Workflow:

1. Identify the commit or commits to port.
2. Determine the target workspaces:
   - if the user gives a version floor such as `1.20+`, `1.21+`, or `1.21.11+`, include every `MultiWorkspace*` whose version is greater than or equal to that floor
   - do not assume workspace names always stay on the `1.x` pattern; include newer naming schemes such as `MultiWorkspace26.1`, `MultiWorkspace26.1.1`, `MultiWorkspace26.2`, or `MultiWorkspace27.1` when they are at or above the requested floor
   - parse version segments numerically, not lexicographically, so `1.21.11+` includes newer releases correctly
   - when the user does not give a version floor, default to all maintained `MultiWorkspace*` targets at `1.20+`
   - exclude the current workspace
   - exclude workspaces below the requested floor unless the user explicitly asks
   - if a workspace name does not expose a parseable version, report it as skipped instead of guessing
3. For each target workspace:
   - apply the change using the `/cherry-pick` workflow
   - run a compile check with `gradlew.bat build` or `./gradlew build`
   - if compile fails, adapt the code using the cherry-pick rules
   - if the port succeeds, commit using the original commit message verbatim
4. Report the result for each workspace and any skipped or failed ports.

Guardrails:

- Do not push.
- If the source commit set is unclear, ask which commit or range to port before touching other workspaces.
- If the requested floor is ambiguous, infer the narrowest reasonable numeric floor from the user's wording and state it in the result.
- If a workspace cannot be adapted quickly, report the failure and continue to the next workspace.
