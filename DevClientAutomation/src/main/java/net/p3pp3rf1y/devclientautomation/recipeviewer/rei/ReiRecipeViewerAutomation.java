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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.JsonUtil;
import net.p3pp3rf1y.devclientautomation.recipeviewer.*;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ReiRecipeViewerAutomation implements RecipeViewerAutomation {
	@Override
	public String id() {
		return "rei";
	}

	@Override
	public String stateJson() {
		Screen screen = Minecraft.getInstance().screen;
		TextField searchTextField = REIRuntime.getInstance().getSearchTextField();
		return "{\"ok\":true," + JsonUtil.property("viewer", id()) + ","
				+ JsonUtil.property("searchText", searchTextField == null ? null : searchTextField.getText()) + "," + "\"indexStackCount\":"
				+ EntryRegistry.getInstance().size() + "," + "\"recipeCount\":" + DisplayRegistry.getInstance().size() + "," + "\"recipeScreenOpen\":"
				+ isRecipeScreenOpen(screen) + "," + JsonUtil.property("screenClass", screen == null ? null : screen.getClass().getName()) + "}";
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
		Screen screen = Minecraft.getInstance().screen;
		return "{\"ok\":true," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("item", stackItemId(stack.get())) + ","
				+ JsonUtil.property("mode", normalizedMode) + "," + "\"opened\":" + opened + "," + "\"recipeScreenOpen\":" + isRecipeScreenOpen(screen) + ","
				+ JsonUtil.property("screenClass", screen == null ? null : screen.getClass().getName()) + "}";
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
		boolean allowItemFallback = RecipeViewerRequest.focus(request).isEmpty();
		List<Display> recipes;
		List<Display> uses;
		try {
			recipes = openedDisplays(stack.get(), false, allowItemFallback);
			uses = openedDisplays(stack.get(), true, allowItemFallback);
		} catch (ConcurrentModificationException e) {
			recipes = List.of();
			uses = List.of();
		}
		return "{\"ok\":true," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("item", stackItemId(stack.get())) + ","
				+ JsonUtil.property("name", stackName(stack.get())) + "," + "\"uiBacked\":true,"
				+ JsonUtil.rawProperty("focusStack", RecipeJsonUtil.itemStackJson(stack.get().castValue())) + "," + "\"recipeCount\":" + recipes.size() + ","
				+ "\"useCount\":" + uses.size() + "," + "\"recipes\":" + recipesJson(recipes, limit) + "," + "\"uses\":" + recipesJson(uses, limit) + "}";
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
		Screen screen = Minecraft.getInstance().screen;
		if (!isRecipeScreenOpen(screen)) {
			return matchingDisplays(stack, usages, allowItemFallback);
		}
		try {
			List<Display> displays = displaysFromScreen(screen);
			if (!displays.isEmpty()) {
				return displays.stream()
						.filter(display -> containsStack(usages ? display.getInputEntries() : display.getOutputEntries(), stack, allowItemFallback))
						.collect(Collectors.collectingAndThen(
								Collectors.toMap(ReiRecipeViewerAutomation::displayIdKey, display -> display, (first, ignored) -> first, LinkedHashMap::new),
								map -> List.copyOf(map.values())));
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
		Optional<EntryStack<?>> exact = EntryRegistry.getInstance().getEntryStacks().filter(ReiRecipeViewerAutomation::isItemStack)
				.filter(stack -> stackItemId(stack).equals(normalized))
				.filter(stack -> focus.map(selector -> RecipeViewerStackMatcher.matches(stack.castValue(), selector)).orElse(true)).findFirst();
		if (exact.isPresent()) {
			return exact;
		}
		return EntryRegistry.getInstance().getEntryStacks().filter(ReiRecipeViewerAutomation::isItemStack)
				.filter(stack -> stackName(stack).toLowerCase(Locale.ROOT).equals(normalized))
				.filter(stack -> focus.map(selector -> RecipeViewerStackMatcher.matches(stack.castValue(), selector)).orElse(true)).findFirst();
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
		return java.util.stream.Stream
				.concat(registry.getAll().values().stream().flatMap(Collection::stream).filter(display -> display.getDisplayLocation().isPresent())
						.filter(registry::isDisplayVisible)
						.filter(display -> containsStack(usages ? display.getInputEntries() : display.getOutputEntries(), stack, allowItemFallback)),
						java.util.stream.Stream
								.concat(registry.getGlobalDisplayGenerators().stream(),
										registry.getCategoryDisplayGenerators().values().stream().flatMap(Collection::stream))
								.flatMap(generator -> generatedDisplays(generator, stack, usages, allowItemFallback).stream()))
				.collect(Collectors.collectingAndThen(
						Collectors.toMap(ReiRecipeViewerAutomation::displayKey, display -> display, (first, ignored) -> first, LinkedHashMap::new),
						map -> List.copyOf(map.values())));
	}

	private static String displayKey(Display display) {
		return display.getCategoryIdentifier().getIdentifier() + "|" + display.getDisplayLocation().map(ResourceLocation::toString).orElse("") + "|"
				+ ingredientsJson(display.getInputEntries()) + "|" + ingredientsJson(display.getOutputEntries());
	}

	private static List<? extends Display> generatedDisplays(DynamicDisplayGenerator<?> generator, EntryStack<?> stack, boolean usages) {
		return usages ? generator.getUsageFor(stack).orElse(List.of()) : generator.getRecipeFor(stack).orElse(List.of());
	}

	private static List<? extends Display> generatedDisplays(DynamicDisplayGenerator<?> generator, EntryStack<?> stack, boolean usages,
			boolean allowItemFallback) {
		List<? extends Display> focusedDisplays = generatedDisplays(generator, stack, usages);
		if (!usages && !focusedDisplays.isEmpty()) {
			return focusedDisplays;
		}
		List<? extends Display> globalDisplays = generatedDisplays(generator).stream()
				.filter(display -> containsStack(usages ? display.getInputEntries() : display.getOutputEntries(), stack, allowItemFallback)).toList();
		if (focusedDisplays.isEmpty()) {
			return globalDisplays;
		}
		Set<String> focusedIds = focusedDisplays.stream().map(ReiRecipeViewerAutomation::displayIdKey).collect(Collectors.toCollection(HashSet::new));
		return java.util.stream.Stream.concat(focusedDisplays.stream(), globalDisplays.stream().filter(display -> !focusedIds.contains(displayIdKey(display))))
				.toList();
	}

	private static String displayIdKey(Display display) {
		return display.getCategoryIdentifier().getIdentifier() + "|" + display.getDisplayLocation().map(ResourceLocation::toString).orElse("");
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
		return stack.has(ModCoreDataComponents.MAIN_COLOR.get()) || stack.has(ModCoreDataComponents.ACCENT_COLOR.get());
	}

	private static String itemNotFoundJson(String itemId) {
		return "{\"ok\":false," + JsonUtil.property("error", "Item not found in REI index: " + itemId) + "}";
	}

	private static String recipesJson(List<Display> displays, int limit) {
		int safeLimit = Math.max(0, limit);
		String entries = displays.stream().limit(safeLimit).map(ReiRecipeViewerAutomation::recipeJson).collect(Collectors.joining(","));
		return "[" + entries + "]";
	}

	private static String recipeJson(Display display) {
		return "{" + JsonUtil.property("id", display.getDisplayLocation().map(ResourceLocation::toString).orElse(null)) + ","
				+ JsonUtil.property("category", display.getCategoryIdentifier().getIdentifier().toString()) + ","
				+ JsonUtil.property("categoryName", categoryName(display)) + "," + "\"inputCount\":" + display.getInputEntries().size() + ","
				+ "\"outputCount\":" + display.getOutputEntries().size() + "," + "\"inputs\":" + ingredientsJson(display.getInputEntries()) + ","
				+ "\"outputs\":" + ingredientsJson(display.getOutputEntries()) + "}";
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
		return CategoryRegistry.getInstance().tryGet(display.getCategoryIdentifier()).map(configuration -> configuration.getCategory().getTitle().getString())
				.orElse(display.getCategoryIdentifier().getIdentifier().toString());
	}

	private static boolean isRecipeScreenOpen(Screen screen) {
		return screen != null && screen.getClass().getName().contains("DisplayViewingScreen");
	}
}
