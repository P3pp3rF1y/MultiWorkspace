---
description: Apply or adapt a commit from another Minecraft version branch
agent: general
---

Apply or adapt a commit from another Minecraft version branch to the current branch.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/code-standards.md`
- `../knowledge/porting-knowledge.md`

User context:
$ARGUMENTS

Workflow:

1. Inspect the source commit and identify which files changed.
2. Apply the same intent to the current branch, adapting code as needed for the current Minecraft, Forge, or NeoForge version.
3. Update version numbers and dependencies according to the rules below.
4. If data generation inputs or outputs are affected, rerun the appropriate data generation task.

Version rules:

- When the source commit increases the mod version, apply the same kind of increase in the current branch.
- Increase the matching major, minor, or patch part rather than copying the exact source branch version number.

Dependency rules:

- If the source commit updates this mod's dependency on another of the author's mods, map it to the equivalent dependency version for the current branch instead of copying the exact version.
- If the correct target dependency version is unclear, ask instead of guessing.

Gradle properties rules:

- Change only the lines actually touched by the source commit.
- Preserve current branch formatting when unrelated layout differences appear in conflicts.

Generated files:

- Do not manually recreate generated output unless explicitly requested.
- Apply source changes to the real inputs and rerun generation instead.

Commit message policy:

- Preserve the original commit message verbatim.
- Do not prepend, append, or otherwise rewrite the original message unless the user explicitly requests it.
