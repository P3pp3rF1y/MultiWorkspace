# Porting Knowledge (Multi-Workspace Mods)

Use this checklist when porting modules between maintained workspace branches.

## Build tooling parity (required)

- Keep the module Gradle wrapper version aligned with the target workspace baseline and sibling submodules for that MC line.
- Do not keep a newer wrapper from another line (for example 1.21.x) when porting to an older line (for example 1.20.x).
- Keep plugin family and plugin versions compatible with the target line's wrapper/JDK combination.

## CI branch parity (required)

- Reusable workflow references must point to the matching workspace branch line.
- Example: 1.20.x modules should use the 1.20.x reusable workflow branch, not 1.21.x.
- Mismatched workflow branches can silently switch Java/build behavior and produce non-obvious failures.

## Java/runtime parity (required)

- Match Java version to the target line's known-good baseline in both local and CI runs.
- For 1.20.x lines in this workspace family, use Java 17 unless the target branch explicitly documents otherwise.

## Dependency mapping parity (required)

- For 1.20.x ForgeGradle userdev lines, treat Minecraft mod dependencies as obfuscated inputs.
- Use `fg.deobf(...)` for all Minecraft mod dependencies so they are remapped for dev/runtime.
- If a dependency intentionally stays `compileOnly` for optional integration, still apply mapping rules for the configuration that is actually consumed at runtime.

## Porting sanity checks

- Verify wrapper version, Java version, and workflow branch before debugging code-level errors.
- If errors mention class file major version mismatches, first re-check Java/workflow parity.
- If errors look like wrong-MC/no-such-method at runtime, first re-check dependency remapping parity.

## Access transformers vs shadow fields

- When a Minecraft or NeoForge update makes an inherited field `final` or too
  restrictive for an existing mod screen or container implementation, prefer an
  access transformer if the project already uses ATs for GUI/container internals.
- Avoid adding same-named shadow fields in subclasses just to work around parent
  field access or mutability changes. That creates split state and makes future
  ports/debugging harder.
- Example pattern: if `AbstractContainerScreen.imageWidth` / `imageHeight`
  become unusable for an existing screen hierarchy, make the inherited fields
  accessible/mutable through AT and keep using the inherited state directly.

## Texture atlas placement reminder

- Texture atlas placement is a general mod standard, not just a porting detail.
- See `mod-standards.md` for the full atlas ownership rule and placement
  guidance.
- When debugging missing or dark break particles, explicitly check whether the
  particle path is using terrain/block particle logic, which still expects block
  atlas sprites.

## Package-info parity during ports

- When porting code that creates or moves Java packages, keep `package-info.java`
  coverage in sync with the target module's existing package structure.
- Use the target workspace's existing `package-info.java` files as the source of
  truth for which nonnull annotations belong there; do not blindly copy the
  source branch's annotation list.
