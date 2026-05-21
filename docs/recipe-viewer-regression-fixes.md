# Recipe Viewer Regression Fixes - 1.21.8

- `Ingredient.of(HolderSet.empty())` and `Ingredient.of(Items.AIR)` now throw in 1.21.8. Empty recipe-viewer display slots need a viewer-only empty ingredient and shaped synthetic recipe holders must convert empty display ingredients back to `Optional.empty()`.
- `CraftingDisplaySpec.replacesCraftingRecipe` must compare `recipeHolder.id().location()` to stored `ResourceLocation` replacement ids. Comparing a `ResourceKey` to a `ResourceLocation` silently breaks exact recipe replacement ownership.
- `ClientRecipeHelper.getResultItem` must unwrap `IWrapperRecipe` before checking vanilla shaped/shapeless recipes. Custom shapeless wrapper recipes otherwise produce `ItemStack.EMPTY` in tier-upgrade displays.
- Backpack smithing display specs should use the optional smithing template/addition accessors. Legacy `getTemplateIngredient()` / `getAdditionIngredient()` fallback to air ingredients is invalid in 1.21.8.
- Moving-storage tier-upgrade display generation cannot derive contained storage from `Ingredient.items()` for `MovingStorageIngredient`; it only exposes the carrier item. Preserve the custom ingredient's moving-storage display stacks for catalog generation.
- Tests were adjusted to assert semantic presence of the focused moving-storage variant instead of assuming a single matching variant where global generated variants can also exist.
