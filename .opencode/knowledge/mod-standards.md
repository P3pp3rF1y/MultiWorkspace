# Mod Standards

Use this file for recurring implementation standards that apply to new mod
features, refactors, and cleanup work, not just version porting.

## Texture atlas placement

- Put textures into the atlas appropriate for how they are used, not just the
  first location that works.
- Atlas ownership rule: every texture must live in the atlas used by the
  renderer/model path that resolves it.
- Determine the runtime atlas first, then choose the resource path and atlas
  source mapping to match it.
- Follow atlas-by-purpose rules:
  - block model textures, block-state-model textures, and normal block break
    particles -> `BLOCKS` atlas
  - item-specific textures resolved through item rendering/model paths ->
    `ITEMS` atlas
  - chest entity textures and chest-specific particles -> `CHESTS` atlas
  - shulker box, sign, banner, shield, and other special-renderer assets -> the
    dedicated atlas expected by that renderer/model system
- Do not infer atlas from gameplay category alone. A block-related feature may
  still use `ITEMS`, `CHESTS`, or another atlas depending on the actual runtime
  lookup path.
- Terrain particles and other block-particle paths are a special case: if the
  particle is produced through block state / terrain particle logic, its sprite
  still needs to come from the block atlas path used by that system.
- Prefer folder placement and atlas source configuration that make the intended
  atlas obvious from the resource path.
- If a renderer or dynamic block state model resolves sprites from a dedicated
  atlas (for example `AtlasIds.CHESTS`), keep its custom textures in that atlas
  too instead of splitting related assets across unrelated atlases.
- If a texture serves two different systems with different atlas requirements,
  prefer the atlas required by the runtime lookup path that actually consumes it,
  or split the assets intentionally instead of forcing one texture into the
  wrong atlas.
- For new functionality, verify all three together:
  1. texture file path
  2. atlas JSON/source stitching
  3. runtime sprite lookup/reference
- A correct texture file in the wrong atlas is still a bug.

## Package-level nonnull defaults

- Every new Java package in these mod codebases should include a `package-info.java`
  declaring the package-level nonnull defaults already used by the surrounding
  module.
- When creating a new package, copy the annotation set and import style from the
  nearest existing `package-info.java` in the same module/version instead of
  inventing a new combination.
- Do not assume the annotation list is identical across maintained MC lines.
  During ports, treat the target workspace/version as the source of truth and
  align new or updated `package-info.java` files with the package-info files that
  already exist there.
- If a port introduces a new package into a module that already uses
  `package-info.java`, add one for the new package in the target line as part of
  the same change.
- If package-level defaults changed for that version (for example an added or
  removed nonnull-related annotation), make the new package match the current
  version's existing convention rather than preserving the source version's
  annotation list.
