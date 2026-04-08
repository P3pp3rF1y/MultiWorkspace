---
name: smart-commit
aliases:
  - smart commit
  - smart_commit
  - smartCommit
description: Run the repository's smart commit workflow following the project's conventions
agent: general
---

Run the repository's smart commit workflow following the project's conventions and bump the mod patch version when appropriate.

This command is a custom workflow command, not a plain `git commit` alias.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/code-standards.md`

User context:
$ARGUMENTS

Workflow:

1. Inspect the changed files and determine the intended commit scope.
2. Check for any uncommitted changes in the working tree.
3. Classify uncommitted changes into:
   - changes clearly covered by the intended commit
   - changes clearly outside the intended commit
   - changes whose fit is ambiguous
4. Automatically include changes that are clearly covered by the intended commit.
5. If any changes are clearly outside the intended commit, ask the user before including them.
6. If any changes are ambiguous, ask the user whether to include them or leave them for a later commit.
7. When asking, group files by likely purpose and provide a brief recommendation.
8. Decide whether the final result belongs in one commit or should be split.
9. If the commit type is `feat` or `fix`, increase the mod patch version by `+1` unless the user explicitly asks to skip the bump.
10. If the commit scope also includes dependent modules or repos that declare version ranges for changed dependencies in `gradle.properties` (for example `sc_version`, `sb_version`, or `ss_version`), update those dependency lower bounds to the dependency `mod_version` being committed in the same overall change.
11. Create a commit message using the repository's commit format.

Commit format:

`<type>: <emoji> <summary>`

Allowed commit types and emoji mapping:

- `feat` -> `✨`
- `fix` -> `🐛`
- `refactor` -> `♻️`
- `chore` -> `🔧`
- `test` -> `🧪`

Summary rules:

- For `feat` and `fix`, use past tense and describe the user-visible result.
- For `refactor`, `chore`, and `test`, use imperative tense.
- Keep the summary concise and avoid vague phrasing.
- If multiple unrelated changes exist, propose splitting them into separate commits.

Execution guardrails:

- Before running `git commit`, print a short check line with the commit type, `mod_version` before and after, and whether the user opted out of the bump.
- Do not ask about uncommitted changes when they are all clearly part of the intended commit.
- Ask before including any uncommitted changes that are clearly outside the intended commit, even if combining them into one commit would be acceptable.
- Ask about uncommitted changes whose fit is ambiguous.
- When asking, identify the specific files or file groups that are out of scope or ambiguous and provide a brief recommendation.
- If the user chooses to include extra out-of-scope changes, expand the commit scope and message to reflect the broader set of changes.
- If the user chooses to leave extra changes out, proceed with the narrower commit.
- Do not run `git commit` until any out-of-scope or ambiguous uncommitted changes have been resolved.
- If the commit type is `feat` or `fix` and no opt-out was given, do not commit until the patch version bump is included.
- If the overall change also updates a dependency module that is referenced by version range from another included module or repo, do not commit until the dependent `gradle.properties` range has been updated to the new dependency version too.
- If a commit attempt fails, fix the issue and create a new commit instead of amending unless an explicit amend flow is required by repository rules.
