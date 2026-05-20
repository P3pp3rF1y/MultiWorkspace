# Recipe Viewer Regression Fixes - 1.21.5

## Findings

- Minecraft 1.21.5 rejects `Ingredient.of(HolderSet.empty())` and `Ingredient.of(Items.AIR)`. Recipe-viewer display makers need a non-matching display placeholder for empty slots instead of constructing an empty/air vanilla ingredient.
- Smithing display specs already model optional template/addition ingredients. Backpack smithing display construction should pass the recipe optionals through instead of materializing absent ingredients as air.
- `CraftingDisplaySpec.replacesCraftingRecipe` must compare stored `ResourceLocation` recipe ids against `recipeHolder.id().location()`, not the `ResourceKey` object.
- Moving-storage tier ingredients expose concrete contained-storage stacks through custom ingredient display state, while `Ingredient.items()` only returns the moving-storage item holder. Tier display construction needs access to the concrete matching stacks to preserve contained storage, tint, wood, and boat/minecart dimensions.
- Regression tests that assemble wrapped shapeless/custom recipes should use the wrapped compose result or recipe `assemble` carefully. Direct `assemble` can touch config-backed slot defaults before configs are loaded; using compose results plus explicit component copying keeps tests focused on recipe-viewer semantics.

## Fixes Applied

- Replaced empty display-slot placeholders in backpack, storage, and moving-storage display makers with a NeoForge `DifferenceIngredient` that has no matching/display stacks but avoids 1.21.5 empty ingredient construction failures.
- Updated backpack smithing display recipes to keep optional template/addition ingredients and adjusted the regression assertion to validate the catalog-created generic smithing recipe result instead of casting to the module-specific recipe class.
- Fixed exact crafting recipe replacement ownership lookup in core by comparing recipe locations.
- Exposed `MovingStorageIngredient` matching stacks and used them when expanding moving-storage tier recipe variants.
- Adjusted Storage and StorageInMotion regression tests to avoid order-dependent tier-chain selection and to normalize wrapped custom recipe results in test-only helper code.

## Verification

- `./gradlew.bat :SophisticatedBackpacks:recipeViewerRegressionTest`
- `./gradlew.bat :SophisticatedStorage:recipeViewerRegressionTest`
- `./gradlew.bat :SophisticatedStorageInMotion:recipeViewerRegressionTest`

All three tasks passed in the final reruns.
