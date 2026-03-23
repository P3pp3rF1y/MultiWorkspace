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
