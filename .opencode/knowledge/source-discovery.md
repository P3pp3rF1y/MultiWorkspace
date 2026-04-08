# Source Discovery

Use this workflow before asking the user for Minecraft, Forge, NeoForge, or
third-party dependency internals.

## Purpose

- Resolve real local source before making claims about framework or dependency
  behavior.
- Prefer workspace-local source when available.
- If local source is unavailable, say so clearly instead of guessing.

## Applies To

- Minecraft source
- NeoForge or Forge source
- Third-party mod dependencies
- Questions about API signatures, behavior, or implementation details

## Lookup Order

1. Included Gradle projects from the active workspace `settings.gradle`
2. Curated local checkouts declared next to the locator script
3. Gradle `-sources.jar` artifacts from local caches
4. Cached or generated Minecraft / loader sources already available locally
5. Honest unresolved result when no local source exists

## Workspace Detection

- Treat the nearest ancestor containing `settings.gradle` as the workspace root
  unless the caller provides `--workspace`.
- Read active version properties from workspace `gradle.properties` files,
  especially `workspace/gradle.properties` when present.
- Use `minecraft_version`, `neo_version`, and `forge_version` to determine
  cache identity.

## Version-Aware Cache Rules

- Extracted sources live under the active workspace `.source-cache/` folder.
- Cache paths are version-scoped.
- A version change must trigger a new lookup and extraction for the new version.
- Old cache folders may remain on disk, but they are not valid for the current
  workspace version.

## Cache Layout

- `.source-cache/minecraft/<mc-version>/sources/...`
- `.source-cache/minecraft/<mc-version>/neoform/...`
- `.source-cache/neoforge/<neo-version>/...`
- `.source-cache/forge/<forge-version>/...`
- `.source-cache/maven/<group>/<artifact>/<version>/...`

## Dependency Policy

- Local workspace modules should be read directly from source roots.
- Minecraft, NeoForge, Forge, and third-party dependency sources are resolved on
  demand.
- For Minecraft, prefer transformed Java sources from local neoformruntime
  outputs when available. Treat the raw neoform archive as secondary patch and
  config material.
- On Forge-era workspaces, prefer ForgeGradle mapped sources jars or decomp
  outputs for the active `minecraft_version` + `forge_version` pair.
- Broad source-oriented queries like `minecraft`, `neoforge`, `forge`, `jei`,
  or `create` count as source-investigation queries and should resolve the named
  source set.
- Broad queries should only resolve directly relevant artifacts, not prewarm the
  full dependency graph.
- `minecraft` queries resolve Minecraft only. They do not automatically warm the
  current loader source.
- Use explicit prewarm only when intentionally populating caches.
- If a dependency is binary-only or lacks local sources, report that directly.

## Query Types

- Module name, for example `SophisticatedCore`
- Broad framework or dependency name, for example `minecraft` or `neoforge`
- Artifact text, for example `mezz.jei` or `net.neoforged:neoforge`
- Package or fully qualified class name, for example
  `net.minecraft.world.item.ItemStack`

## Output Expectations

- Prefer ranked local results with the best source candidate first.
- Include source origin, resolved version, and local path when available.
- Machine-readable consumers should use `--json`.
- If lookup fails, say that no local source was found and avoid inferring the
  missing behavior.

## Examples

```powershell
pwsh -File D:\Development\AgenticAI\tools\locate-sources.ps1 SophisticatedCore --workspace D:\Development\MultiWorkspace1.21.1
pwsh -File D:\Development\AgenticAI\tools\locate-sources.ps1 minecraft --workspace D:\Development\MultiWorkspace1.21.1
pwsh -File D:\Development\AgenticAI\tools\locate-sources.ps1 net.neoforged.neoforge.items.IItemHandler --workspace D:\Development\MultiWorkspace1.21.1
pwsh -File D:\Development\AgenticAI\tools\locate-sources.ps1 mezz.jei.api.runtime.IIngredientManager --workspace D:\Development\MultiWorkspace1.21.1 --json
```

## Failure Behavior

- If local source cannot be resolved, state that clearly.
- Ask the user for exact external source only when the task truly depends on it
  and local lookup did not succeed.
- Do not infer class names, method names, signatures, side effects, or behavior
  from memory when the source is unresolved.
