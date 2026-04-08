---
description: Quality-check and merge translation PRs, then port across 1.20+ workspaces
agent: general
subtask: true
---

Merge one translation PR (or the latest translation PR) with a strict quality gate,
then propagate the same translation commit across remaining maintained 1.20+
branches/workspaces for the same repository/submodule.

User context:
$ARGUMENTS

## Scope and intent

- This command is for translation-only PRs that modify language JSON files.
- Preferred target shape:
  - explicit: `repo=<repo> pr=<number>`
  - inferred: `latest translation PR in <repo>`
- `dry_run=true` is supported and should be used by default when requested.

## Hard guardrails (mandatory)

- Same-submodule-only propagation:
  - Cherry-pick only inside the same GitHub repository/submodule that was merged.
  - Never cherry-pick across different repositories/mods.
- Existing equivalent PR guard:
  - Before cherry-picking to a target branch, check for an open equivalent
    translation PR in the same repository for that branch.
  - If equivalent PR exists, skip cherry-pick for that branch and report:
    `skipped: equivalent PR exists`.
- If two equivalent PRs were merged in this run (for different base branches), use
  the newer branch merge commit as cherry-pick source for remaining branches.
- Translation commits use `feat` message format but must NOT trigger
  `gradle.properties` mod version bumps.
- Never use destructive git operations (`reset --hard`, force pushes, etc.).

## PR resolution

1. Resolve repository from user input (for example `SophisticatedStorage` to
   `P3pp3rF1y/SophisticatedStorage`).
2. Resolve target PR:
   - If `pr` is provided, use it.
   - Otherwise pick the latest open non-draft translation PR in that repository by
     checking title/body/files for translation indicators (for example
     `translation`, `localization`, `lang`, `ru_ru.json`, etc.).
3. Identify equivalent PR candidates in the same repository:
   - same author,
   - same translation language file(s),
   - different base branch,
   - translation-focused PR.
4. Process in descending base branch recency (newer first).

## Quality gate (must pass before any merge)

Run these checks for each PR candidate before merging any of them:

1. JSON validity
   - Ensure modified lang files parse as valid JSON.
2. Key integrity vs English baseline
   - Compare changed keys against matching `en_us.json` keys.
   - Flag missing English source keys or broken key mapping.
3. Placeholder/format parity
   - Compare token parity against English source for changed keys:
     `%s`, `%d`, positional forms (`%1$s`), brace tokens (`{0}`), line breaks,
     and formatting markers.
   - Exception: treat `%s%s` in English vs a single `%s` in translation as
     non-blocking when the second `%s` is only a join/spacing fragment and the
     target language legitimately compounds words (no lost runtime argument
     semantics).
   - Review note: newline (`\\n`) differences are allowed for translation
     readability (for example multiline tooltips) and must not be treated as a
     blocking issue by themselves.
4. Multilingual bad-words screening (model-based)
   - Use model judgment to detect likely profanity/hate/slurs/explicit abusive
     language in the translation text regardless of language.
5. Translation plausibility check (model-based)
   - For changed keys, compare source English and target translation to catch
     clearly incorrect/malicious/nonsensical translations.

Gate policy:

- Any blocking issue => `FAIL`, do not merge, output findings.
- No blocking issues => `PASS`, continue.

## Merge behavior

For each approved PR in execution order:

1. Build merge commit subject exactly as:
   `feat: ✨ Updated [LANGUAGE_DISPLAY] translation (Thanks [CONTRIBUTOR_LOGIN])`
   where `CONTRIBUTOR_LOGIN` is the GitHub username shown on the PR (for
   example `Fr0stmatic`), and `LANGUAGE_DISPLAY` is resolved as:
   - Prefer a human-readable language name when known (for example
     `ru_ru -> Russian`, `pt_br -> Portuguese (Brazil)`,
     `zh_cn -> Chinese (Simplified)`).
   - If no known mapping exists, use the raw language code with only the first
     character capitalized (for example `pt_br -> Pt_br`).
   Treat this subject as `EXPECTED_SUBJECT` and reuse it unchanged for merge and
   propagated commits.
2. Squash merge on GitHub with explicit empty body:
   `gh pr merge --squash --subject ... --body ""`.
   This prevents GitHub's default extended description text from being included
   in the final squash commit message body.
3. Validate merge output before continuing (hard gate):
   - PR state is `MERGED`.
   - merged commit subject equals `EXPECTED_SUBJECT`.
   - merged commit body is empty.
   - merged commit has a single parent (squash/non-merge commit shape).
   If any check fails, stop with `FAIL_METHOD_OR_MESSAGE` and do not propagate.

Merge retry state machine (mandatory):

1. Attempt merge once with `gh pr merge --squash --subject ... --body ""`.
2. If merge returns transient server/network failure (for example `502`, timeout,
   transport failure), do not immediately issue a second merge call.
3. Poll PR status for up to 120 seconds (every 5-10 seconds):
   - `state` (`OPEN|MERGED`)
   - `mergedAt`
   - `mergeCommit`
4. If any poll shows `state=MERGED`, treat merge as completed and continue.
5. If merge command returns `Merge already in progress`, treat this as an active
   lock and continue polling; do not call alternate merge endpoint yet.
6. Only if polling window expires and PR is still not merged:
   - Re-check whether base branch already contains the expected squash subject or
     effective translation diff is already applied.
   - If already applied, do not merge again (avoid duplicate empty commit);
     report as `already_effectively_merged` and stop.
7. Fallback to REST merge endpoint (`PUT /pulls/{n}/merge`) is allowed only when
   all are true:
   - PR is still `OPEN`,
   - no in-progress merge lock is reported,
   - base branch does not already include the effective translation diff.
   When fallback is used, force squash settings and exact message:
   `merge_method=squash`, `commit_title=EXPECTED_SUBJECT`, `commit_message=""`.
8. After fallback merge, verify resulting merge commit actually changes at least
   one translation file. If commit is empty, report `empty_merge_commit` as a
   major process warning and do not use that empty commit as cherry-pick source.

## Workspace synchronization

After each successful PR merge:

1. Fetch the merged base branch in the same repository/submodule and pull the
   new squash-merge commit from GitHub.
2. In the corresponding `MultiWorkspace...` repo for that base branch, update the
   submodule pointer to the merged commit.
3. Create a workspace commit for the pointer update.

## Propagation to remaining 1.20+ workspaces

1. Determine remaining maintained `1.20+` branches/workspaces for this same
   repository/submodule.
2. Choose cherry-pick source commit:
   - If one PR merged: use that merge commit.
   - If two equivalent PRs merged: use the merge commit from the newer branch.
   - If the preferred source merge commit is empty (no file changes), fall back
     to the next merged equivalent PR commit that contains translation changes.
     Prefer newest non-empty commit.
3. For each remaining target branch:
   - Check `CHERRY_PICK_EXISTING_PR_GUARD` first.
   - If no equivalent PR exists, apply source commit with
     `git cherry-pick --no-commit <source>` and create a new commit using
     `EXPECTED_SUBJECT` exactly (do not reuse source subject).
   - Push only after commit subject is verified to equal `EXPECTED_SUBJECT`.
4. For each propagated branch/workspace, update and commit the workspace
   submodule pointer and push branch updates as needed.

## Dry run mode

If `dry_run=true`, do all discovery and quality checks but do not merge,
cherry-pick, commit, or push.

Dry run report must include:

- resolved repository and PR targets,
- equivalent PR mapping,
- quality gate findings and verdict,
- chosen cherry-pick source commit (or planned source),
- planned target branches/workspaces,
- branches skipped due to equivalent open PR.

## Output contract

Return exactly this shape:

```text
repo: <owner/repo>
mode: EXECUTE|DRY_RUN
resolved_prs:
  - pr: <number>
    base_branch: <branch>
    author: <login>
    language: <lang>
quality_gate:
  verdict: PASS|FAIL
  findings:
    - severity: blocker|major|minor
      pr: <number>
      location: <path[:line]>
      issue: <one-line>
      why: <one-line>
      suggested_fix: <one-line>
merge_results:
  - pr: <number>
    action: merged|skipped
    merge_commit: <sha|none>
    reason: <short>
workspace_pointer_updates:
  - workspace: <MultiWorkspace...>
    submodule: <name>
    pointer_commit: <sha>
    workspace_commit: <sha|none>
propagation:
  cherry_pick_scope: same_repo_only
  cherry_pick_source_commit: <sha|planned>
  targets:
    - branch: <branch>
      action: cherry_picked|skipped
      reason: <short>
      commit: <sha|none>
  skipped_due_to_equivalent_pr:
    - branch: <branch>
      pr: <number>
compliance:
  merge_method_ok: true|false
  merge_subject_ok: true|false
  merge_body_empty_ok: true|false
  propagation_subject_ok_all: true|false
final_verdict: PASS|FAIL
gate_reason: <single sentence>
```
