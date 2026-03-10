# Skills

## commit

Create a repository commit following the project's conventions and bump the
mod patch version when appropriate.

### Workflow

1. Inspect the changed files and determine the intent of the change.
2. Decide whether the changes belong in a single commit or should be split.
3. If the change adds functionality or fixes a bug, increase the **mod patch version**.
4. Create a commit message using the repository's commit format.

### Commit format

<type>: <emoji> <summary>

Examples:

feat: ✨ Added battery charge tooltip to dynamic model  
fix: 🐛 Fixed translucent quad slicing on the X axis  
refactor: ♻️ Simplify magnet upgrade filtering logic  
chore: 🔧 Bump mod patch version  
test: 🧪 Add tests for upgrade filtering logic

### Allowed commit types

feat  
fix  
refactor  
chore  
test

### Emoji mapping

feat → ✨  
fix → 🐛  
refactor → ♻️  
chore → 🔧  
test → 🧪

### Verb tense rules

**feat and fix**

These commits appear directly in changelogs.

Use **past tense**.

Examples:

feat: ✨ Added support for fluid top face rendering  
fix: 🐛 Fixed magnet upgrade pulling filtered items incorrectly

**refactor, chore, test**

These commits are not included in changelogs.

Use **imperative tense**.

Examples:

refactor: ♻️ Simplify magnet upgrade filtering logic  
chore: 🔧 Bump mod patch version  
test: 🧪 Add tests for upgrade filtering logic

### Summary guidelines

- Keep summaries concise.
- Prefer describing the observable result rather than internal implementation.
- Avoid vague summaries like "various fixes".
- When multiple unrelated changes exist, propose splitting them into separate commits.
- For `feat` and `fix`, write summaries from the player's perspective and describe the visible effect of the change rather than internal implementation details.

## cherry-pick

Apply or adapt a commit from another Minecraft version branch to the current branch.

### Workflow

1. Inspect the cherry-picked commit and identify which files were changed.
2. Apply the same intent to the current branch, adapting code as needed for the
   current Minecraft / Forge / NeoForge version.
3. Update version numbers and dependencies according to the rules below.
4. If data generation outputs are affected, rerun data generation.

### Version update rules

When the source commit increases the mod version, apply the same kind of version
increase in the current branch.

Interpret version changes by part:
- if the source commit increases the major version, increase the major version in the current branch
- if the source commit increases the minor version, increase the minor version in the current branch
- if the source commit increases the patch version, increase the patch version in the current branch

Do not copy the exact version number from the source branch unless explicitly requested.

### Dependency version rules

If the cherry-picked commit updates this mod's dependency on another of the
author's mods, update it to the correct equivalent version for the current branch
rather than copying the exact version number from the source branch.

Example:
- if a 1.21.1 commit updates SophisticatedCore from `1.4.5` to `1.4.7`
- then in 1.20.1 update the dependency from `1.3.5` to the corresponding `1.3.x`
  version, not to `1.4.7`

If other dependency versions are changed and the correct target version is not
obvious from the current branch, ask for the version to use.

### gradle.properties rules

When resolving `gradle.properties`, only change lines that were actually touched
by the cherry-picked commit.

Do not rewrite unrelated lines to match another branch's formatting or layout.

Be especially careful on older branches where `gradle.properties` format differs.
If a merge conflict includes unrelated formatting differences, preserve the
current branch formatting and only apply the intended line changes.

### Code adaptation rules

For non-version files, prefer preserving the original change where possible.

If the source change depends on APIs or behavior that differ in the current
Minecraft / Forge / NeoForge version, adapt the implementation while preserving
the same behavior and intent.

### Data generation

If the cherry-picked commit changes generated resources or data generation inputs,
rerun data generation.

### Summary guidelines

- Preserve the intent of the original commit.
- Minimize unrelated edits.
- Call out places where version-specific adaptation was required.
- If dependency mapping is unclear, ask instead of guessing.