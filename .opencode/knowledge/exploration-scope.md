# Exploration Scope Policy (Token Efficiency)

Apply this policy during code exploration and implementation tasks.

## Scope selection

- Start from the module named by the user.
- If no module is named, default to the module most likely owning the feature or bug.
- Do not start from workspace root unless required.

## Expansion order

1. `src/main/java/**`
2. `src/main/resources/**`
3. module `build.gradle` / `gradle.properties` (only if build/dependency related)
4. workspace-level files only if module-local evidence is insufficient

## Default excludes

- `**/src/generated/**`
- `**/build/**`
- `**/.gradle/**`
- `**/.opencode/node_modules/**`
- `**/.git/**`
- `**/.idea/**`
- `**/workspace/run/**`
- `**/workspace/run2/**`
- `**/workspace/run3/**`
- `**/tmp/**`
- Binary assets (`**/*.png`, `**/*.jar`, `**/*.class`) unless explicitly needed

## Re-include rules

- `src/generated`: only for datagen output verification or generated diff inspection
- `build` / run dirs: only for build/runtime failure diagnosis
- binary assets/images: only for texture/model/rendering tasks
