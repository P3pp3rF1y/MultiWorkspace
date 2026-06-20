package net.p3pp3rf1y.devclientautomation.recipeviewer.rei;

import com.google.gson.JsonObject;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.widgets.TextField;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.p3pp3rf1y.devclientautomation.JsonUtil;
import net.p3pp3rf1y.devclientautomation.recipeviewer.*;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.compat.recipeviewers.common.BackpackRecipeViewerDisplays;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayView;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayContext;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.RecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplayView;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static net.p3pp3rf1y.sophisticatedbackpacks.compat.recipeviewers.common.subtypes.SubtypeInterpreters.getSubtypeInterpreters;

public class ReiRecipeViewerAutomation implements RecipeViewerAutomation {
    @Override
    public String id() {
        return "rei";
    }

    @Override
    public String stateJson() {
        Screen screen = Minecraft.getInstance().gui.screen();
        TextField searchTextField = REIRuntime.getInstance().getSearchTextField();
        return "{\"ok\":true,"
                + JsonUtil.property("viewer", id()) + ","
                + JsonUtil.property("searchText", searchTextField == null ? null : searchTextField.getText()) + ","
                + "\"indexStackCount\":" + EntryRegistry.getInstance().size() + ","
                + "\"recipeCount\":" + DisplayRegistry.getInstance().size() + ","
                + "\"recipeScreenOpen\":" + isRecipeScreenOpen(screen) + ","
                + JsonUtil.property("screenClass", screen == null ? null : screen.getClass().getName())
                + "}";
    }

    @Override
    public String searchJson(String query) {
        TextField searchTextField = REIRuntime.getInstance().getSearchTextField();
        if (searchTextField == null) {
            return "{\"ok\":false," + JsonUtil.property("error", "REI search field is not available") + "}";
        }
        searchTextField.setText(query);
        return "{\"ok\":true," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("searchText", searchTextField.getText()) + "}";
    }

    @Override
    public String openJson(String requestJson) {
        JsonObject request = RecipeViewerRequest.parse(requestJson);
        String itemId = RecipeViewerRequest.itemId(request);
        Optional<EntryStack<?>> stack = findStack(request);
        if (stack.isEmpty()) {
            return itemNotFoundJson(itemId);
        }
        String mode = RecipeViewerRequest.mode(request, "recipes");
        String normalizedMode = mode.toLowerCase(Locale.ROOT);
        ViewSearchBuilder builder = ViewSearchBuilder.builder().addAllCategories();
        if (normalizedMode.equals("recipes") || normalizedMode.equals("recipe") || normalizedMode.equals("crafting")) {
            builder.addRecipesFor(stack.get());
        } else if (normalizedMode.equals("uses") || normalizedMode.equals("usage") || normalizedMode.equals("crafts_into")) {
            builder.addUsagesFor(stack.get());
        } else {
            return "{\"ok\":false," + JsonUtil.property("error", "Unknown mode: " + mode) + "}";
        }
        boolean opened = builder.open();
        Screen screen = Minecraft.getInstance().gui.screen();
        return "{\"ok\":true,"
                + JsonUtil.property("viewer", id()) + ","
                + JsonUtil.property("item", stackItemId(stack.get())) + ","
                + JsonUtil.property("mode", normalizedMode) + ","
                + "\"opened\":" + opened + ","
                + "\"recipeScreenOpen\":" + isRecipeScreenOpen(screen) + ","
                + JsonUtil.property("screenClass", screen == null ? null : screen.getClass().getName())
                + "}";
    }

    @Override
    public String queryJson(String requestJson) {
        JsonObject request = RecipeViewerRequest.parse(requestJson);
        String itemId = RecipeViewerRequest.itemId(request);
        Optional<EntryStack<?>> stack = findStack(request);
        if (stack.isEmpty()) {
            return itemNotFoundJson(itemId);
        }
		int limit = RecipeViewerRequest.limit(request, 20);
		boolean allowItemFallback = allowItemFallback(request);
		Optional<JsonObject> focusSelector = RecipeViewerRequest.focus(request);
		List<RecipeEntry> recipes;
		List<RecipeEntry> uses;
		try {
			recipes = new ArrayList<>(recipeEntries(openedDisplays(stack.get(), false, allowItemFallback)));
			recipes.addAll(recipeBookEntries(stack.get().castValue(), false, allowItemFallback, focusSelector));
			recipes.addAll(commonBackpackCatalogEntries(stack.get().castValue(), false, allowItemFallback, focusSelector));
			recipes = distinctRecipeEntries(recipes);
			uses = new ArrayList<>(recipeEntries(openedDisplays(stack.get(), true, allowItemFallback)));
			uses.addAll(recipeBookEntries(stack.get().castValue(), true, allowItemFallback, focusSelector));
			uses.addAll(commonBackpackCatalogEntries(stack.get().castValue(), true, allowItemFallback, focusSelector));
			uses = distinctRecipeEntries(uses);
		} catch (ConcurrentModificationException e) {
            recipes = List.of();
            uses = List.of();
        }
        return "{\"ok\":true,"
                + JsonUtil.property("viewer", id()) + ","
                + JsonUtil.property("item", stackItemId(stack.get())) + ","
                + JsonUtil.property("name", stackName(stack.get())) + ","
                + "\"uiBacked\":true,"
                + JsonUtil.rawProperty("focusStack", RecipeJsonUtil.itemStackJson(stack.get().castValue())) + ","
                + "\"recipeCount\":" + recipes.size() + ","
                + "\"useCount\":" + uses.size() + ","
                + "\"recipes\":" + recipesJson(recipes, limit) + ","
                + "\"uses\":" + recipesJson(uses, limit)
                + "}";
    }

    private static List<Display> openedDisplays(EntryStack<?> stack, boolean usages, boolean allowItemFallback) {
        ViewSearchBuilder builder = ViewSearchBuilder.builder().addAllCategories();
        if (usages) {
            builder.addUsagesFor(stack);
        } else {
            builder.addRecipesFor(stack);
        }
        if (!builder.open()) {
            return matchingDisplays(stack, usages, allowItemFallback);
        }
        Screen screen = Minecraft.getInstance().gui.screen();
        if (!isRecipeScreenOpen(screen)) {
            return matchingDisplays(stack, usages, allowItemFallback);
        }
		try {
			List<Display> displays = displaysFromScreen(screen);
			if (!displays.isEmpty()) {
				List<Display> screenDisplays = displays.stream()
						.filter(display -> containsStack(usages ? display.getInputEntries() : display.getOutputEntries(), stack, allowItemFallback))
						.collect(Collectors.collectingAndThen(Collectors.toMap(
								ReiRecipeViewerAutomation::displayIdKey,
								display -> display,
								(first, ignored) -> first,
								LinkedHashMap::new
						), map -> List.copyOf(map.values())));
				List<Display> matchingDisplays = matchingDisplays(stack, usages, allowItemFallback);
				return java.util.stream.Stream.concat(screenDisplays.stream(), matchingDisplays.stream())
						.collect(Collectors.collectingAndThen(Collectors.toMap(
								ReiRecipeViewerAutomation::displayIdKey,
								display -> display,
								(first, ignored) -> first,
								LinkedHashMap::new
						), map -> List.copyOf(map.values())));
			}
		} catch (ReflectiveOperationException | RuntimeException ignored) {
            // Fall back to registry query below.
        }
        return matchingDisplays(stack, usages, allowItemFallback);
    }

    private static List<Display> displaysFromScreen(Screen screen) throws ReflectiveOperationException {
        Object categoryMapObject = getField(screen, "categoryMap");
        if (!(categoryMapObject instanceof Map<?, ?> categoryMap)) {
            return List.of();
        }
        List<Display> displays = new ArrayList<>();
        for (Object specsObject : categoryMap.values()) {
            if (!(specsObject instanceof Collection<?> specs)) {
                continue;
            }
            for (Object spec : specs) {
                Object display = invoke(spec, "provideInternalDisplay");
                if (display instanceof Display reiDisplay) {
                    displays.add(reiDisplay);
                }
            }
        }
        return displays;
    }

    private static Object getField(Object target, String name) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object invoke(Object target, String name) throws ReflectiveOperationException {
        Method method = findMethod(target.getClass(), name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Method findMethod(Class<?> type, String name) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Optional<EntryStack<?>> findStack(JsonObject request) {
        Optional<EntryStack<?>> encodedStack = RecipeViewerFocus.encodedStack(request).map(EntryStacks::of);
        if (encodedStack.isPresent()) {
            return encodedStack;
        }
        Optional<JsonObject> focus = RecipeViewerRequest.focus(request);
        String itemId = RecipeViewerRequest.itemId(request);
        String normalized = itemId.toLowerCase(Locale.ROOT);
        Optional<EntryStack<?>> exact = EntryRegistry.getInstance().getEntryStacks()
                .filter(ReiRecipeViewerAutomation::isItemStack)
                .filter(stack -> stackItemId(stack).equals(normalized))
                .filter(stack -> focus.map(selector -> RecipeViewerStackMatcher.matches(stack.castValue(), selector)).orElse(true))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return EntryRegistry.getInstance().getEntryStacks()
                .filter(ReiRecipeViewerAutomation::isItemStack)
                .filter(stack -> stackName(stack).toLowerCase(Locale.ROOT).equals(normalized))
                .filter(stack -> focus.map(selector -> RecipeViewerStackMatcher.matches(stack.castValue(), selector)).orElse(true))
                .findFirst();
    }

    private static boolean allowItemFallback(JsonObject request) {
        Optional<JsonObject> focus = RecipeViewerRequest.focus(request);
        if (focus.isEmpty()) {
            return true;
        }
        JsonObject selector = focus.get();
        return !selector.has("match") && !selector.has("notMatch") && !selector.has("componentsPattern");
    }

    private static boolean isItemStack(EntryStack<?> stack) {
        return stack.getValue() instanceof ItemStack;
    }

    private static String stackItemId(EntryStack<?> stack) {
        ItemStack itemStack = stack.castValue();
        return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
    }

    private static String stackName(EntryStack<?> stack) {
        ItemStack itemStack = stack.castValue();
        return itemStack.getHoverName().getString();
    }

    private static List<Display> matchingDisplays(EntryStack<?> stack, boolean usages, boolean allowItemFallback) {
        DisplayRegistry registry = DisplayRegistry.getInstance();
        return java.util.stream.Stream.concat(
                registry.getAll().values().stream()
                        .flatMap(Collection::stream)
                        .filter(display -> display.getDisplayLocation().isPresent())
                        .filter(registry::isDisplayVisible)
                        .filter(display -> containsStack(usages ? display.getInputEntries() : display.getOutputEntries(), stack, allowItemFallback)),
                java.util.stream.Stream.concat(
                        registry.getGlobalDisplayGenerators().stream(),
                        registry.getCategoryDisplayGenerators().values().stream().flatMap(Collection::stream)
                ).flatMap(generator -> generatedDisplays(generator, stack, usages, allowItemFallback).stream())
        )
                .collect(Collectors.collectingAndThen(Collectors.toMap(
                        ReiRecipeViewerAutomation::displayKey,
                        display -> display,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ), map -> List.copyOf(map.values())));
    }

    private static String displayKey(Display display) {
        return display.getCategoryIdentifier().getIdentifier() + "|"
                + display.getDisplayLocation().map(Identifier::toString).orElse("") + "|"
                + ingredientsJson(display.getInputEntries()) + "|"
                + ingredientsJson(display.getOutputEntries());
    }

    private static List<? extends Display> generatedDisplays(DynamicDisplayGenerator<?> generator, EntryStack<?> stack, boolean usages) {
        return usages ? generator.getUsageFor(stack).orElse(List.of()) : generator.getRecipeFor(stack).orElse(List.of());
    }

    private static List<? extends Display> generatedDisplays(DynamicDisplayGenerator<?> generator, EntryStack<?> stack, boolean usages, boolean allowItemFallback) {
        List<? extends Display> focusedDisplays = generatedDisplays(generator, stack, usages);
        if (!usages && !focusedDisplays.isEmpty()) {
            return focusedDisplays;
        }
        List<? extends Display> globalDisplays = generatedDisplays(generator).stream()
                .filter(display -> containsStack(usages ? display.getInputEntries() : display.getOutputEntries(), stack, allowItemFallback))
                .toList();
        if (focusedDisplays.isEmpty()) {
            return globalDisplays;
        }
        Set<String> focusedIds = focusedDisplays.stream().map(ReiRecipeViewerAutomation::displayIdKey).collect(Collectors.toCollection(HashSet::new));
        return java.util.stream.Stream.concat(focusedDisplays.stream(), globalDisplays.stream().filter(display -> !focusedIds.contains(displayIdKey(display)))).toList();
    }

    private static String displayIdKey(Display display) {
        return display.getCategoryIdentifier().getIdentifier() + "|" + display.getDisplayLocation().map(Identifier::toString).orElse("");
    }

    private static List<? extends Display> generatedDisplays(DynamicDisplayGenerator<?> generator) {
        return generator.generate(ViewSearchBuilder.builder()).orElse(List.of());
    }

    private static boolean containsStack(List<EntryIngredient> ingredients, EntryStack<?> stack, boolean allowItemFallback) {
        return ingredients.stream().flatMap(Collection::stream).anyMatch(candidate -> stacksMatch(candidate, stack, allowItemFallback));
    }

    private static boolean stacksMatch(EntryStack<?> candidate, EntryStack<?> focusedStack, boolean allowItemFallback) {
        if (EntryStacks.equalsFuzzy(candidate, focusedStack)) {
            return true;
        }
        if (!allowItemFallback || !isItemStack(candidate) || !isItemStack(focusedStack)) {
            return false;
        }
        ItemStack candidateStack = candidate.castValue();
        ItemStack focusItemStack = focusedStack.castValue();
        return !hasColorComponents(candidateStack) && ItemStack.isSameItem(candidateStack, focusItemStack);
    }

	private static boolean hasColorComponents(ItemStack stack) {
		Integer mainColor = stack.get(ModCoreDataComponents.MAIN_COLOR.get());
		Integer accentColor = stack.get(ModCoreDataComponents.ACCENT_COLOR.get());
		return mainColor != null && mainColor != BackpackWrapper.DEFAULT_MAIN_COLOR || accentColor != null && accentColor != BackpackWrapper.DEFAULT_ACCENT_COLOR;
	}

    private static List<RecipeEntry> recipeEntries(List<Display> displays) {
        return displays.stream().map(ReiRecipeViewerAutomation::recipeEntry).toList();
    }

	private static List<RecipeEntry> recipeBookEntries(ItemStack focusedStack, boolean usages, boolean allowItemFallback, Optional<JsonObject> focusSelector) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return List.of();
        }
        try {
            if (minecraft.getSingleplayerServer() == null) {
                return List.of();
            }
            ContextMap context = SlotDisplayContext.fromLevel(minecraft.level);
            List<RecipeEntry> entries = new ArrayList<>();
            minecraft.getSingleplayerServer().getRecipeManager().getRecipes().forEach(recipe -> {
                List<RecipeDisplayEntry> displayEntries = new ArrayList<>();
                minecraft.getSingleplayerServer().getRecipeManager().listDisplaysForRecipe(recipe.id(), displayEntries::add);
                for (RecipeDisplayEntry displayEntry : displayEntries) {
                    try {
                        RecipeEntry entry = recipeEntry(displayEntry, context);
						if (!hasSophisticatedBackpackInput(entry) && containsStack(usages ? entry.inputs() : entry.outputs(), focusedStack, allowItemFallback, focusSelector)) {
							entries.add(entry);
						}
                    } catch (RuntimeException ignored) {
                        // Some vanilla display slots cannot resolve without extra UI context; ignore them for regression matching.
                    }
                }
            });
            return entries;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

	private static List<RecipeEntry> commonBackpackCatalogEntries(ItemStack focusedStack, boolean usages, boolean allowItemFallback, Optional<JsonObject> focusSelector) {
        try {
            IRecipeViewerDisplayCatalog catalog = backpackCatalog();
            List<RecipeEntry> entries = new ArrayList<>();
			if (usages || isBaseBackpack(focusedStack)) {
				catalog.getGroupedCraftingSpecs().forEach(spec -> {
					List<RecipeHolder<GroupedCraftingRecipe>> displays = usages ? spec.getUsagesFor(focusedStack) : spec.getRecipesFor(focusedStack);
					if (displays.isEmpty()) {
						GroupedCraftingRecipe recipe = spec.recipe();
						List<List<ItemStack>> focusSlots = usages ? recipe.getFixedInputSlots() : List.of(recipe.getResultStacks());
						if (containsStack(focusSlots, focusedStack, allowItemFallback, focusSelector)) {
							displays = List.of(spec.recipeHolder());
						}
					}
					displays.stream()
							.map(ReiRecipeViewerAutomation::groupedCraftingRecipeEntry)
							.filter(entry -> containsStack(usages ? entry.inputs() : entry.outputs(), focusedStack, allowItemFallback, focusSelector))
							.forEach(entries::add);
				});
			}
			List<CraftingDisplayView> craftingViews = usages ? catalog.getCraftingUsagesFor(focusedStack) : catalog.getCraftingRecipesFor(focusedStack);
			craftingViews.stream()
					.map(ReiRecipeViewerAutomation::craftingDisplayEntry)
					.filter(entry -> containsStack(usages ? entry.inputs() : entry.outputs(), focusedStack, allowItemFallback, focusSelector))
					.forEach(entries::add);
			List<SmithingDisplayView> smithingViews = usages ? catalog.getSmithingUsagesFor(focusedStack) : catalog.getSmithingRecipesFor(focusedStack);
			List<RecipeEntry> smithingEntries = smithingViews.stream().map(ReiRecipeViewerAutomation::smithingDisplayEntry).toList();
			if (smithingEntries.isEmpty()) {
				smithingEntries = catalog.getGlobalSmithingDisplays().stream().map(ReiRecipeViewerAutomation::smithingDisplayEntry).toList();
			}
			smithingEntries.stream()
					.filter(entry -> containsStack(usages ? entry.inputs() : entry.outputs(), focusedStack, allowItemFallback, focusSelector))
					.forEach(entries::add);
            return entries;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

	private static boolean hasSophisticatedBackpackInput(RecipeEntry entry) {
		return entry.inputs().stream().flatMap(Collection::stream).anyMatch(stack -> "sophisticatedbackpacks".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()));
	}

	private static boolean isBaseBackpack(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Identifier.fromNamespaceAndPath("sophisticatedbackpacks", "backpack"));
	}

    private static IRecipeViewerDisplayCatalog backpackCatalog() {
        Map<Item, PropertyBasedSubtypeInterpreter> subtypeInterpreters = getSubtypeInterpreters();
        IRecipeViewerDisplayContext context = stack -> Optional.ofNullable(subtypeInterpreters.get(stack.getItem()));
        IRecipeViewerDisplayCatalog catalog = new RecipeViewerDisplayCatalog();
        BackpackRecipeViewerDisplays.register(catalog, context);
        return catalog;
    }

    private static RecipeEntry groupedCraftingRecipeEntry(RecipeHolder<GroupedCraftingRecipe> recipeHolder) {
        GroupedCraftingRecipe recipe = recipeHolder.value();
        return new RecipeEntry(recipeHolder.id().identifier().toString(), "minecraft:crafting", "Crafting", recipe.getInputSlots(), List.of(recipe.getResultStacks()));
    }

    private static RecipeEntry craftingDisplayEntry(CraftingDisplayView view) {
        return new RecipeEntry(view.spec().id().toString(), "minecraft:crafting", "Crafting", view.spec().getInputSlots(view.variants()), List.of(view.spec().getOutputStacks(view.variants())));
    }

    private static RecipeEntry smithingDisplayEntry(SmithingDisplayView view) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        view.spec().template().map(ReiRecipeViewerAutomation::ingredientStacks).ifPresent(inputs::add);
        inputs.add(view.spec().getBaseStacks(view.variants()));
        view.spec().addition().map(ReiRecipeViewerAutomation::ingredientStacks).ifPresent(inputs::add);
        return new RecipeEntry(view.spec().id().toString(), "minecraft:smithing", "Smithing", inputs, List.of(view.spec().getResultStacks(view.variants())));
    }

    private static List<ItemStack> ingredientStacks(Ingredient ingredient) {
        return ingredient.items().map(ItemStack::new).toList();
    }

    private static RecipeEntry recipeEntry(RecipeDisplayEntry entry, ContextMap context) {
        RecipeDisplay display = entry.display();
        List<List<ItemStack>> inputs = recipeDisplayInputs(display, context);
        List<List<ItemStack>> outputs = List.of(resolveStacks(display.result(), context));
        String category = recipeDisplayCategory(display);
        return new RecipeEntry("recipe_display:" + entry.id().index(), category, recipeDisplayCategoryName(category), inputs, outputs);
    }

    private static List<List<ItemStack>> recipeDisplayInputs(RecipeDisplay display, ContextMap context) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return resolveStacks(shaped.ingredients(), context);
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            return resolveStacks(shapeless.ingredients(), context);
        }
        if (display instanceof SmithingRecipeDisplay smithing) {
            return List.of(
                    resolveStacks(smithing.template(), context),
                    resolveStacks(smithing.base(), context),
                    resolveStacks(smithing.addition(), context)
            );
        }
        return List.of();
    }

    private static List<List<ItemStack>> resolveStacks(List<SlotDisplay> displays, ContextMap context) {
        return displays.stream().map(display -> resolveStacks(display, context)).toList();
    }

    private static List<ItemStack> resolveStacks(SlotDisplay display, ContextMap context) {
        try {
            return display.resolveForStacks(context).stream().filter(stack -> !stack.isEmpty()).toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static String recipeDisplayCategory(RecipeDisplay display) {
        if (display instanceof SmithingRecipeDisplay) {
            return "minecraft:smithing";
        }
        if (display instanceof ShapedCraftingRecipeDisplay || display instanceof ShapelessCraftingRecipeDisplay) {
            return "minecraft:crafting";
        }
        Identifier key = BuiltInRegistries.RECIPE_DISPLAY.getKey(display.type());
        return key == null ? display.type().toString() : key.toString();
    }

    private static String recipeDisplayCategoryName(String category) {
        return switch (category) {
            case "minecraft:crafting" -> "Crafting";
            case "minecraft:smithing" -> "Smithing";
            default -> category;
        };
    }

	private static boolean containsStack(List<List<ItemStack>> ingredients, ItemStack focusedStack, boolean allowItemFallback) {
		return ingredients.stream().flatMap(Collection::stream).anyMatch(candidate -> stacksMatch(candidate, focusedStack, allowItemFallback));
	}

	private static boolean containsStack(List<List<ItemStack>> ingredients, ItemStack focusedStack, boolean allowItemFallback, Optional<JsonObject> focusSelector) {
		return ingredients.stream().flatMap(Collection::stream).anyMatch(candidate -> stacksMatch(candidate, focusedStack, allowItemFallback, focusSelector));
	}

	private static boolean stacksMatch(ItemStack candidate, ItemStack focusedStack, boolean allowItemFallback, Optional<JsonObject> focusSelector) {
		if (focusSelector.isPresent() && ItemStack.isSameItem(candidate, focusedStack) && RecipeViewerStackMatcher.matches(candidate, focusSelector.get())) {
			return true;
		}
		return stacksMatch(candidate, focusedStack, allowItemFallback);
	}

	private static boolean stacksMatch(ItemStack candidate, ItemStack focusedStack, boolean allowItemFallback) {
        if (ItemStack.isSameItemSameComponents(candidate, focusedStack)) {
            return true;
        }
        return allowItemFallback && !hasColorComponents(candidate) && ItemStack.isSameItem(candidate, focusedStack);
    }

    private static List<RecipeEntry> distinctRecipeEntries(List<RecipeEntry> entries) {
        return entries.stream().collect(Collectors.collectingAndThen(Collectors.toMap(
                ReiRecipeViewerAutomation::recipeEntryKey,
                entry -> entry,
                (first, ignored) -> first,
                LinkedHashMap::new
        ), map -> List.copyOf(map.values())));
    }

    private static String recipeEntryKey(RecipeEntry entry) {
        return entry.category() + "|"
                + RecipeJsonUtil.itemStackIngredientGroupsJson(entry.inputs()) + "|"
                + RecipeJsonUtil.itemStackIngredientGroupsJson(entry.outputs());
    }

    private static String itemNotFoundJson(String itemId) {
        return "{\"ok\":false," + JsonUtil.property("error", "Item not found in REI index: " + itemId) + "}";
    }

    private static String recipesJson(List<RecipeEntry> displays, int limit) {
        int safeLimit = Math.max(0, limit);
        String entries = displays.stream().limit(safeLimit).map(ReiRecipeViewerAutomation::recipeJson).collect(Collectors.joining(","));
        return "[" + entries + "]";
    }

    private static RecipeEntry recipeEntry(Display display) {
        return new RecipeEntry(
                display.getDisplayLocation().map(Identifier::toString).orElse(null),
                display.getCategoryIdentifier().getIdentifier().toString(),
                categoryName(display),
                entryIngredients(display.getInputEntries()),
                entryIngredients(display.getOutputEntries())
        );
    }

    private static List<List<ItemStack>> entryIngredients(List<EntryIngredient> ingredients) {
        return ingredients.stream()
                .map(ingredient -> ingredient.stream()
                        .filter(ReiRecipeViewerAutomation::isItemStack)
                        .map(EntryStack::<ItemStack>castValue)
                        .toList())
                .toList();
    }

    private static String recipeJson(RecipeEntry display) {
        return "{"
                + JsonUtil.property("id", display.id()) + ","
                + JsonUtil.property("category", display.category()) + ","
                + JsonUtil.property("categoryName", display.categoryName()) + ","
                + "\"inputCount\":" + display.inputs().size() + ","
                + "\"outputCount\":" + display.outputs().size() + ","
                + "\"inputs\":" + RecipeJsonUtil.itemStackIngredientGroupsJson(display.inputs()) + ","
                + "\"outputs\":" + RecipeJsonUtil.itemStackIngredientGroupsJson(display.outputs())
                + "}";
    }

    private static String ingredientsJson(List<EntryIngredient> ingredients) {
        return RecipeJsonUtil.ingredientsJson(ingredients.stream().map(ReiRecipeViewerAutomation::ingredientJson).collect(Collectors.toList()));
    }

    private static String ingredientJson(EntryIngredient ingredient) {
        return RecipeJsonUtil.ingredientJson(ingredient.stream().map(ReiRecipeViewerAutomation::entryStackJson).collect(Collectors.toList()));
    }

    private static String entryStackJson(EntryStack<?> stack) {
        if (isItemStack(stack)) {
            return RecipeJsonUtil.itemStackJson(stack.castValue());
        }
        return RecipeJsonUtil.stackJson(stack.getType().getId().toString(), stack.getType().getId().toString(), stack.getValue().toString(), 1, 1);
    }

    private static String categoryName(Display display) {
        return CategoryRegistry.getInstance().tryGet(display.getCategoryIdentifier())
                .map(configuration -> configuration.getCategory().getTitle().getString())
                .orElse(display.getCategoryIdentifier().getIdentifier().toString());
    }

    private static boolean isRecipeScreenOpen(Screen screen) {
        return screen != null && screen.getClass().getName().contains("DisplayViewingScreen");
    }

    private record RecipeEntry(String id, String category, String categoryName, List<List<ItemStack>> inputs, List<List<ItemStack>> outputs) {}
}
