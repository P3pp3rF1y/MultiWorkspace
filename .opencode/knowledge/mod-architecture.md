# Mod Architecture Baseline (Sophisticated Mods)

This knowledge base captures project-specific architectural expectations for code review.

The guidance is intended to stay stable across maintained Minecraft versions; keep version-specific exceptions in `version-reference-index.md`.

## Module boundaries

- `Reliquary`: standalone content module in the same workspace.
- `SophisticatedCore`: shared logic (configs, data components, recipes, fluids, payloads, utility systems).
- `SophisticatedBackpacks`: backpack gameplay/content, depends on `SophisticatedCore`.
- `SophisticatedStorage`: storage gameplay/content, depends on `SophisticatedCore` and includes compat paths for backpack/storage upgrade conversion.
- `SophisticatedItemActions`: shared/action-oriented module in the workspace.
- `SophisticatedStorageInMotion`: add-on module in the workspace.
- `SophisticatedBackpacksCreateIntegration`: Create bridge for backpacks.
- `SophisticatedStorageCreateIntegration`: Create bridge for storage.

## Dependency conventions

- Use local project dependency when available (`findProject(':Module') != null`), Maven fallback otherwise.
- Integration modules should stay thin and avoid duplicating core gameplay logic.
- Shared behaviors belong in `SophisticatedCore` unless strongly feature-local.

## Registration patterns

- Prefer `DeferredRegister`-based registries in module `init` packages.
- Keep module bootstrap in the mod entrypoint and delegate concrete registrations to `init` classes.
- Register handlers from `registerHandlers(IEventBus)` style helpers for consistency.

## Networking patterns

- Keep packet definitions and handlers module-local (`init/ModPayloads` or module content init class).
- Keep serializer + handler wiring explicit and consistent with the target MC/loader line.
- Do not prescribe naming, registration APIs, or transport abstractions from another MC line.
- No client-only classes in common packet serialization/handling paths.

## Data generation patterns

- Datagen providers live in `src/main/java/**/data/**` and are wired through `DataGenerators.gatherData`.
- Generated resources are expected under `src/generated/resources` where configured by each module.
- If datagen inputs change, run the corresponding `:module:runData` task and include generated changes.

## Side separation patterns

- Client-only initialization is guarded by loader side checks used by the target branch (for example environment dist checks or side-specific event subscribers).
- Client rendering/menu/color/tooltip wiring stays in `client` or `*Client` init classes.
- Common/server code must not reference `net.minecraft.client` or client-only NeoForge APIs.
- Common/server code must not reference project classes that themselves depend on client-only classes (no indirect/proxy leaks).

## Save and compatibility expectations

- Treat NBT/data component/attachment/schema changes as compatibility-sensitive.
- For persistent data changes, require explicit migration/compatibility reasoning.
- Avoid silent behavior changes in packet payload formats without versioning/backward strategy.

## Container mutation safety

- Treat `Slot#setChanged()` as behavior-sensitive in modded menus; downstream listeners may observe and react to partial intermediate state.
- For batch operations that mutate multiple slots (for example sorting), write slot contents to their final arrangement first and call `setChanged()` only after state is consistent.
- Track changed slot ids during the batch, and mark them in a final pass.
- If rollback is possible, include rollback writes in the same changed-slot tracking and final `setChanged()` pass.

## Client rendering API preferences

- Before introducing lower-level rendering/API code, check the equivalent
  vanilla and NeoForge implementation in the target version and prefer that
  pattern when it provides the needed behavior.
- Prefer `GuiGraphics`, `RenderSystem`, and existing project render helpers over raw `org.lwjgl.opengl.GL11` calls.
- Use raw `GL11` only as an absolute last resort when no supported higher-level API in the target MC/NeoForge line can provide the behavior.
- Avoid synchronous GPU readback calls (for example `glReadPixels`) in per-frame render paths; if readback is unavoidable, perform it outside hot paths and cache the result.
- Any remaining raw OpenGL usage should include a brief code comment explaining why higher-level APIs are insufficient and what state-safety assumptions are relied on.

## Type matching robustness

- Prefer `instanceof` against concrete/parent types over class-name prefix/string matching for behavior routing.
- Avoid checks like `obj.getClass().getName().startsWith(...)` in gameplay logic; they are fragile under package moves/renames/shading.
- Keep class-name string matching only for explicit user-configurable rule systems (include/exclude lists), not internal type decisions.
