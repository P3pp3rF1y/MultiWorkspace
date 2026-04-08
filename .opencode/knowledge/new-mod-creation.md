# New Mod Creation (Generic Multi-Workspace Guidance)

Use this guide when adding a new mod module to a shared multi-workspace repo.

Do not hardcode a specific MC line or branch name here. Keep version-specific
exceptions in `version-reference-index.md`.

## Required wiring for a new module

1. Register the module as a git submodule (do not copy as a plain folder).
2. Verify `.gitmodules` contains the new entry and branch naming follows the
   conventions used by other mods in the same workspace.
3. Ensure the module branch tracks the same-named remote branch (for example
   local `1.21.11` -> `origin/1.21.11`) instead of a shared fallback branch.
   If the same-named remote branch does not exist yet, leave it untracked and
   do not push automatically.
4. Add the module to root `settings.gradle`:
   - `include '<ModuleName>'`
5. Ensure module-local Gradle files exist and match workspace conventions
   (`build.gradle`, `gradle.properties`, wrapper files, CI config, metadata).

## Push policy

- Never push branches unless the user explicitly asks for push.

## Make it load in workspace dev/test runs

Update the workspace launcher project (for example `workspace` or
`workspaceForge`) in both places:

1. Run/mod source wiring (`mods { ... }` or equivalent) so source set is loaded.
2. Project dependency wiring (`implementation project(':<ModuleName>')` or
   equivalent guarded by `findProject`).

Without both, the module may compile but not load in workspace run
configurations.

## Build and version alignment

- Match plugin/toolchain style used by the target workspace and maintained MC
  line (do not mix build families across lines unless explicitly porting).
- Align module `gradle.properties` versions/ranges with workspace baseline:
  - `minecraft_version`
  - loader version (`neo_version` and/or `forge_version`)
  - shared dependency ranges (for example `sc_version`)
- Keep publication metadata loader/release targets consistent with the branch.

## Metadata and resources

- Use the metadata format expected by the target loader/toolchain line
  (`mods.toml`, `neoforge.mods.toml`, templates, etc.).
- Ensure required basic resources exist (for example `pack.mcmeta`, language
  file, and mod metadata).

## Validation checklist

- `./gradlew :<ModuleName>:compileJava`
- Workspace run loads the module with other workspace mods
  (`:workspace:runClient`, `:workspaceForge:runClient`, or IDE equivalent).
- Run the module's configured datagen task when datagen inputs/providers were
  added or changed (task name may vary by version/tooling).
- Confirm common/server code has no direct or indirect client-only references.
