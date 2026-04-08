---
description: Plan and port a MultiWorkspace mod set to a brand new Minecraft or NeoForge version
agent: general
subtask: true
---

Plan and execute a full MultiWorkspace port to a brand new Minecraft and NeoForge version.

Required knowledge files:

- `../knowledge/index.md`
- `../knowledge/porting-knowledge.md`
- `../knowledge/curseforge-resolution.md`
- `../knowledge/version-rules.md`
- `../knowledge/source-discovery.md`
- `../knowledge/compatibility-checklist.md`
- `../knowledge/version-reference-index.md`

User context:
$ARGUMENTS

Intent:

- This command is generic across maintained version lines.
- It is not tied to any specific source or target version.
- It always runs in two stages:
  1. preflight discovery and summary
  2. execution only after explicit user confirmation

Inputs:

- Required: `to=<target-version>`
- Optional:
  - `from=<source-version-or-branch>`
  - `minecraft=<explicit-minecraft-version>`
  - `neoforge=<explicit-neoforge-version>`
  - `workspace-name=<override-name>`
  - `mode=<strict|fast>` for dependency resolution, default `strict`

Defaults:

- `workspace-name` defaults to `MultiWorkspace<target-version>`.
- The new workspace location must be derived from the current workspace parent directory so the new folder is created alongside sibling `MultiWorkspace*` folders.
- If `from` is omitted, auto-detect the latest usable branch for the root repo and each submodule.
- Prefer exact verified dependency resolutions over broad guesses.

Workflow contract:

1. Detect the current workspace root and derive the parent directory from it. Do not hardcode a machine-specific base path.
2. Propose the target clone folder as a sibling workspace named `MultiWorkspace<target-version>` unless overridden.
3. If the target folder already exists, stop and ask the user instead of guessing how to reuse it.
4. Resolve the source branch for the root repo:
   - use `from` when provided
   - otherwise inspect remote branches and choose the latest usable version branch
   - if latest is materially ambiguous, stop and ask
5. Resolve the source branch for each submodule:
   - use the requested `from` branch when it exists
   - otherwise choose the latest usable version branch found for that submodule
   - report every fallback in the preflight summary
6. Discover target Minecraft and NeoForge baseline data from official NeoForged release notes and the linked porting primer unless explicit overrides were provided.
7. Extract and summarize the target platform requirements:
   - Minecraft version
   - NeoForge version
   - Java version
   - Gradle wrapper requirement
   - ModDevGradle or NeoGradle requirement
   - noteworthy porting deltas called out by the official notes
8. Inspect workspace and module Gradle files to classify dependency sources:
   - local included projects
   - normal Maven dependencies
   - CurseForge dependencies
   - optional compile-only or runtime-only integrations
9. Resolve normal Maven dependencies from available repository metadata or listings.
10. Resolve every CurseForge dependency through `/resolve-curseforge-dependency` instead of ad hoc guessing.
11. For CurseForge lookups, treat any `needs_confirmation`, `not_found`, `rate_limited`, or verification mismatch as unresolved and surface it in the summary.
12. Build a preflight summary that includes:
   - clone destination and workspace name
   - root branch source -> target
   - submodule branch source -> target and any fallback selections
   - target Minecraft, NeoForge, Java, Gradle, and plugin update plan
   - dependency resolution plan with statuses such as `resolved`, `resolved_from_cache`, `compile_only_fallback`, `ambiguous_needs_confirmation`, and `not_found`
   - any unclear or blocked items
13. Stop after the preflight summary and ask for explicit confirmation before making changes.
14. Only after confirmation, clone the current workspace into the derived sibling folder.
15. In the cloned root repo, check out the resolved source branch and create a new branch named exactly the target version.
16. Initialize and update submodules, then for each submodule check out the resolved source branch and create a new branch named exactly the target version.
17. Apply platform and toolchain updates required by the target line:
   - NeoForge version
   - Minecraft version properties
   - Java toolchain version
   - Gradle wrapper version
   - compatible plugin versions
   - Parchment removal or adjustment when appropriate for the target line
18. Apply dependency updates:
   - update Maven dependency versions to target-compatible ones
   - update CurseForge file ids to verified target-compatible ones
19. For dependencies that do not have a target-version update available, use this decision order:
   - keep the older dependency version for compile first
   - if the code still compiles, only comment out that mod's runtime dependency in `workspace/build.gradle`
   - if compile fails because compat code broke due to Minecraft or NeoForge changes, also move the affected compat classes to `src/disabledcompats/...` while preserving package structure
20. Port source code module by module, using local source discovery and old-vs-new workspace comparison instead of guessing framework API changes.
21. Keep changes focused and preserve existing project conventions.
22. Run required verification:
   - workspace or module builds needed to confirm the port
   - datagen for modules whose datagen inputs changed
23. If any critical build, datagen, or dependency verification step fails, stop and report the exact blocker instead of claiming completion.
24. Return a final report with what was created, updated, disabled, moved, and what remains unresolved.

Branch selection rules:

- Prefer exact version branches over broader line branches when both exist and the exact version is clearly newer or more specific.
- Ignore feature, PR, and experimental branches unless no maintained version branch exists.
- Never silently choose between multiple plausible latest branches when that choice would materially change the result.

Dependency rules:

- Use exact target Minecraft version and exact target loader when resolving runtime dependencies.
- Do not invent dependency versions or file ids.
- Prefer compile preservation with runtime disablement over removing code when the old dependency still compiles.
- Only move compat classes to `disabledcompats` when the unchanged older dependency can no longer compile after platform-port changes.

Confirmation policy:

- The preflight summary is mandatory.
- Do not clone, branch, edit files, or run full porting steps before the user explicitly confirms the plan.
- If the summary contains unresolved items, call them out clearly and explain what will block execution.

Expected output:

For the preflight stage, return exactly this shape:

```text
stage: preflight
status: needs_confirmation|blocked
target_workspace:
  parent_directory: <derived parent path>
  workspace_name: <target workspace folder name>
  workspace_path: <derived full path>
branch_plan:
  root: <source branch> -> <target branch>
  submodules:
    - <submodule>: <source branch> -> <target branch>
platform_plan:
  minecraft_version: <version or unresolved>
  neoforge_version: <version or unresolved>
  java_version: <version or unresolved>
  gradle_version: <version or unresolved>
  plugin_updates:
    - <plugin update>
dependency_plan:
  resolved:
    - <dependency>: <resolution summary>
  compile_only_fallback:
    - <dependency>: <why runtime will be commented out>
  unresolved:
    - <dependency or source>: <reason>
porting_hotspots:
  - <likely code migration area>
blocking_questions:
  - <question or empty>
confirmation_request: <single concise sentence>
```

For the completion stage, return exactly this shape:

```text
stage: execution
status: completed|blocked
created_workspace: <path>
created_branches:
  root: <target branch>
  submodules:
    - <submodule>: <target branch>
updated_platform:
  minecraft_version: <version>
  neoforge_version: <version>
  java_version: <version>
  gradle_version: <version>
  plugin_updates:
    - <plugin update>
dependency_results:
  updated:
    - <dependency>: <new version or file id>
  runtime_disabled:
    - <dependency>: <reason>
  disabled_compats:
    - <path>: <reason>
verification:
  builds:
    - <command>: PASS|FAIL
  datagen:
    - <command>: PASS|FAIL|NOT_RUN
remaining_blockers:
  - <blocker or empty>
next_steps:
  - <follow-up or empty>
```

Output rules:

- Keep both outputs concise but complete.
- If execution is blocked, populate `remaining_blockers` with exact unresolved items.
- Do not mark execution `completed` while blocker-level issues remain.
