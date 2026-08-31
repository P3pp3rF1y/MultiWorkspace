package net.p3pp3rf1y.devclientautomation.recipeviewer.rei;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", id());
		response.addProperty("searchText", searchTextField == null ? null : searchTextField.getText());
		response.addProperty("indexStackCount", EntryRegistry.getInstance().size());
		response.addProperty("recipeCount", DisplayRegistry.getInstance().displaySize());
		response.addProperty("recipeScreenOpen", isRecipeScreenOpen(screen));
		response.addProperty("screenClass", screen == null ? null : screen.getClass().getName());
		return response.toString();
	}

	@Override
	public String searchJson(String query) {
		TextField searchTextField = REIRuntime.getInstance().getSearchTextField();
		if (searchTextField == null) {
			JsonObject response = new JsonObject();
			response.addProperty("ok", false);
			response.addProperty("error", "REI search field is not available");
			return response.toString();
		}
		searchTextField.setText(query);
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", id());
		response.addProperty("searchText", searchTextField.getText());
		return response.toString();
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
			JsonObject response = new JsonObject();
			response.addProperty("ok", false);
			response.addProperty("error", "Unknown mode: " + mode);
			return response.toString();
		}
		boolean opened = builder.open();
		Screen screen = Minecraft.getInstance().screen;
		JsonObject response = new JsonObject();
		response.addProperty("ok", opened && isRecipeScreenOpen(screen));
		response.addProperty("viewer", id());
		response.addProperty("item", stackItemId(stack.get()));
		response.addProperty("mode", normalizedMode);
		response.addProperty("opened", opened);
		response.addProperty("recipeScreenOpen", isRecipeScreenOpen(screen));
		response.addProperty("screenClass", screen == null ? null : screen.getClass().getName());
		if (!opened || !isRecipeScreenOpen(screen)) {
			response.addProperty("error", "REI recipe screen did not open");
		}
		return response.toString();
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
			throw new IllegalStateException("REI recipe query changed while its displays were being read", e);
		}
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", id());
		response.addProperty("item", stackItemId(stack.get()));
		response.addProperty("name", stackName(stack.get()));
		response.addProperty("uiBacked", true);
		response.add("focusStack", JsonParser.parseString(RecipeJsonUtil.itemStackJson(stack.get().castValue())));
		response.addProperty("recipeCount", recipes.size());
		response.addProperty("useCount", uses.size());
		response.add("recipes", recipesJson(recipes, limit));
		response.add("uses", recipesJson(uses, limit));
		return response.toString();
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
			List<Display> screenDisplays = displays.stream()
					.filter(display -> containsStack(usages ? display.getInputEntries() : display.getOutputEntries(), stack, allowItemFallback))
					.collect(Collectors.collectingAndThen(
							Collectors.toMap(ReiRecipeViewerAutomation::displayIdKey, display -> display, (first, ignored) -> first, LinkedHashMap::new),
							map -> List.copyOf(map.values())));
			return mergeDisplays(screenDisplays, matchingDisplays(stack, usages, allowItemFallback));
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return matchingDisplays(stack, usages, allowItemFallback);
		}
	}

	private static List<Display> matchingDisplays(EntryStack<?> stack, boolean usages, boolean allowItemFallback) {
		DisplayRegistry registry = DisplayRegistry.getInstance();
		List<Display> registeredDisplays = registry.getAll().values().stream().flatMap(Collection::stream)
				.filter(display -> display.getDisplayLocation().isPresent()).filter(registry::isDisplayVisible)
				.filter(display -> containsStack(usages ? display.getInputEntries() : display.getOutputEntries(), stack, allowItemFallback)).toList();
		List<Display> generatedDisplays = java.util.stream.Stream
				.concat(registry.getGlobalDisplayGenerators().stream(), registry.getCategoryDisplayGenerators().values().stream().flatMap(Collection::stream))
				.flatMap(generator -> generatedDisplays(generator, stack, usages, allowItemFallback).stream()).toList();
		return mergeDisplays(registeredDisplays, generatedDisplays);
	}

	private static List<Display> generatedDisplays(DynamicDisplayGenerator<?> generator, EntryStack<?> stack, boolean usages, boolean allowItemFallback) {
		List<? extends Display> focusedDisplays = usages ? generator.getUsageFor(stack).orElse(List.of()) : generator.getRecipeFor(stack).orElse(List.of());
		if (!usages && !focusedDisplays.isEmpty()) {
			return List.copyOf(focusedDisplays);
		}
		List<Display> globalDisplays = generator.generate(ViewSearchBuilder.builder()).orElse(List.of()).stream().map(display -> (Display) display)
				.filter(display -> containsStack(usages ? display.getInputEntries() : display.getOutputEntries(), stack, allowItemFallback)).toList();
		return mergeDisplays(focusedDisplays, globalDisplays);
	}

	private static List<Display> mergeDisplays(Collection<? extends Display> first, Collection<? extends Display> second) {
		return java.util.stream.Stream.concat(first.stream(), second.stream())
				.collect(Collectors.collectingAndThen(
						Collectors.toMap(ReiRecipeViewerAutomation::displayIdKey, display -> display, (original, ignored) -> original, LinkedHashMap::new),
						map -> List.copyOf(map.values())));
	}

	private static List<Display> displaysFromScreen(Screen screen) throws ReflectiveOperationException {
		Object categoryMapObject = getField(screen, "categoryMap");
		if (!(categoryMapObject instanceof Map<?, ?> categoryMap)) {
			throw new IllegalStateException("Unexpected REI recipe screen category map");
		}
		List<Display> displays = new ArrayList<>();
		for (Object specsObject : categoryMap.values()) {
			if (!(specsObject instanceof Collection<?> specs)) {
				throw new IllegalStateException("Unexpected REI recipe category value");
			}
			for (Object spec : specs) {
				Object display = invoke(spec, "provideInternalDisplay");
				if (!(display instanceof Display reiDisplay)) {
					throw new IllegalStateException("Unexpected REI recipe display");
				}
				displays.add(reiDisplay);
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

	private static String displayIdKey(Display display) {
		return display.getCategoryIdentifier().getIdentifier() + "|"
				+ display.getDisplayLocation().map(ResourceLocation::toString).orElse(String.valueOf(System.identityHashCode(display)));
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
		JsonObject response = new JsonObject();
		response.addProperty("ok", false);
		response.addProperty("error", "Item not found in REI index: " + itemId);
		return response.toString();
	}

	private static JsonArray recipesJson(List<Display> displays, int limit) {
		int safeLimit = Math.max(0, limit);
		JsonArray result = new JsonArray();
		displays.stream().limit(safeLimit).map(ReiRecipeViewerAutomation::recipeJson).forEach(result::add);
		return result;
	}

	private static JsonObject recipeJson(Display display) {
		JsonObject result = new JsonObject();
		result.addProperty("id", display.getDisplayLocation().map(ResourceLocation::toString).orElse(null));
		result.addProperty("category", display.getCategoryIdentifier().getIdentifier().toString());
		result.addProperty("categoryName", categoryName(display));
		result.addProperty("inputCount", display.getInputEntries().size());
		result.addProperty("outputCount", display.getOutputEntries().size());
		result.add("inputs", JsonParser.parseString(ingredientsJson(display.getInputEntries())));
		result.add("outputs", JsonParser.parseString(ingredientsJson(display.getOutputEntries())));
		return result;
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
