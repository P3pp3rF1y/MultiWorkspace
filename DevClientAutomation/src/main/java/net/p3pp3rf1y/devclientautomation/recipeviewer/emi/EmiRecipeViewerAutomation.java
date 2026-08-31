package net.p3pp3rf1y.devclientautomation.recipeviewer.emi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.recipeviewer.*;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EmiRecipeViewerAutomation implements RecipeViewerAutomation {
	private static final int STATS_LIMIT = 40;

	@Override
	public String id() {
		return "emi";
	}

	@Override
	public String stateJson() {
		Screen screen = Minecraft.getInstance().screen;
		JsonObject response = new JsonObject();
		response.addProperty("ok", screen instanceof RecipeScreen);
		response.addProperty("viewer", id());
		response.addProperty("searchText", EmiApi.getSearchText());
		response.addProperty("indexStackCount", EmiApi.getIndexStacks().size());
		response.addProperty("recipeCount", EmiApi.getRecipeManager().getRecipes().size());
		response.addProperty("recipeScreenOpen", screen instanceof RecipeScreen);
		response.addProperty("screenClass", screen == null ? null : screen.getClass().getName());
		if (!(screen instanceof RecipeScreen)) {
			response.addProperty("error", "EMI recipe screen did not open");
		}
		return response.toString();
	}

	@Override
	public String searchJson(String query) {
		EmiApi.setSearchText(query);
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", id());
		response.addProperty("searchText", EmiApi.getSearchText());
		return response.toString();
	}

	@Override
	public String openJson(String requestJson) {
		JsonObject request = RecipeViewerRequest.parse(requestJson);
		String itemId = RecipeViewerRequest.itemId(request);
		Optional<EmiStack> stack = findStack(request);
		if (stack.isEmpty()) {
			return itemNotFoundJson(itemId);
		}
		String mode = RecipeViewerRequest.mode(request, "recipes");
		String normalizedMode = mode.toLowerCase(Locale.ROOT);
		if (normalizedMode.equals("recipes") || normalizedMode.equals("recipe") || normalizedMode.equals("crafting")) {
			ensureEmiHostScreen();
			EmiApi.displayRecipes(stack.get());
		} else if (normalizedMode.equals("uses") || normalizedMode.equals("usage") || normalizedMode.equals("crafts_into")) {
			ensureEmiHostScreen();
			EmiApi.displayUses(stack.get());
		} else {
			JsonObject response = new JsonObject();
			response.addProperty("ok", false);
			response.addProperty("error", "Unknown mode: " + mode);
			return response.toString();
		}
		Screen screen = Minecraft.getInstance().screen;
		JsonObject response = new JsonObject();
		response.addProperty("ok", screen instanceof RecipeScreen);
		response.addProperty("viewer", id());
		response.addProperty("item", stack.get().getId().toString());
		response.addProperty("mode", normalizedMode);
		response.addProperty("recipeScreenOpen", screen instanceof RecipeScreen);
		response.addProperty("screenClass", screen == null ? null : screen.getClass().getName());
		if (!(screen instanceof RecipeScreen)) {
			response.addProperty("error", "EMI recipe screen did not open");
		}
		return response.toString();
	}

	@Override
	public String queryJson(String requestJson) {
		JsonObject request = RecipeViewerRequest.parse(requestJson);
		String itemId = RecipeViewerRequest.itemId(request);
		Optional<EmiStack> stack = findStack(request);
		if (stack.isEmpty()) {
			return itemNotFoundJson(itemId);
		}
		int limit = RecipeViewerRequest.limit(request, 20);
		EmiRecipeManager manager = EmiApi.getRecipeManager();
		List<EmiRecipe> recipes = openedRecipes(manager, stack.get(), false);
		List<EmiRecipe> uses = openedRecipes(manager, stack.get(), true);
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", id());
		response.addProperty("item", stack.get().getId().toString());
		response.addProperty("name", stack.get().getName().getString());
		response.addProperty("uiBacked", true);
		response.add("focusStack",
				stack.get().getItemStack().isEmpty() ? null : JsonParser.parseString(RecipeJsonUtil.itemStackJson(stack.get().getItemStack())));
		response.addProperty("recipeCount", recipes.size());
		response.addProperty("useCount", uses.size());
		response.add("recipes", recipesJson(recipes, limit, stack.get().getItemStack()));
		response.add("uses", recipesJson(uses, limit, stack.get().getItemStack()));
		return response.toString();
	}

	@Override
	public String statsJson() {
		List<EmiRecipe> recipes = EmiApi.getRecipeManager().getRecipes();
		List<RecipeStats> stats = recipes.stream().map(RecipeStats::of).toList();
		long inputAlternatives = stats.stream().mapToLong(RecipeStats::inputAlternatives).sum();
		long outputAlternatives = stats.stream().mapToLong(RecipeStats::outputAlternatives).sum();
		long broadInputSlots = stats.stream().mapToLong(RecipeStats::broadInputSlots).sum();
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", id());
		response.addProperty("indexStackCount", EmiApi.getIndexStacks().size());
		response.addProperty("recipeCount", recipes.size());
		response.addProperty("inputAlternatives", inputAlternatives);
		response.addProperty("outputAlternatives", outputAlternatives);
		response.addProperty("broadInputSlots", broadInputSlots);
		response.add("byClass", groupedStatsJson(stats, RecipeStats::className));
		response.add("byCategory", groupedStatsJson(stats, RecipeStats::category));
		response.add("byNamespace", groupedStatsJson(stats, RecipeStats::namespace));
		response.add("byPathBucket", groupedStatsJson(stats, RecipeStats::pathBucket));
		return response.toString();
	}

	private static JsonArray groupedStatsJson(List<RecipeStats> stats, Function<RecipeStats, String> classifier) {
		Map<String, MutableRecipeStats> grouped = new LinkedHashMap<>();
		for (RecipeStats recipeStats : stats) {
			grouped.computeIfAbsent(classifier.apply(recipeStats), ignored -> new MutableRecipeStats()).add(recipeStats);
		}
		JsonArray result = new JsonArray();
		grouped.entrySet().stream().sorted(Comparator.<Map.Entry<String, MutableRecipeStats>>comparingLong(entry -> entry.getValue().recipes()).reversed())
				.limit(STATS_LIMIT).map(entry -> entry.getValue().json(entry.getKey())).forEach(result::add);
		return result;
	}

	private static Optional<EmiStack> findStack(JsonObject request) {
		Optional<EmiStack> encodedStack = RecipeViewerFocus.encodedStack(request).map(EmiStack::of);
		if (encodedStack.isPresent()) {
			return encodedStack;
		}
		Optional<JsonObject> focus = RecipeViewerRequest.focus(request);
		String itemId = RecipeViewerRequest.itemId(request);
		String normalized = itemId.toLowerCase(Locale.ROOT);
		Optional<EmiStack> exact = EmiApi
				.getIndexStacks().stream().filter(stack -> stack.getId().toString().equals(normalized)).filter(stack -> focus
						.map(selector -> !stack.getItemStack().isEmpty() && RecipeViewerStackMatcher.matches(stack.getItemStack(), selector)).orElse(true))
				.findFirst();
		if (exact.isPresent()) {
			return exact;
		}
		return EmiApi
				.getIndexStacks().stream().filter(stack -> stack.getName().getString().toLowerCase(Locale.ROOT).equals(normalized)).filter(stack -> focus
						.map(selector -> !stack.getItemStack().isEmpty() && RecipeViewerStackMatcher.matches(stack.getItemStack(), selector)).orElse(true))
				.findFirst();
	}

	private static String itemNotFoundJson(String itemId) {
		JsonObject response = new JsonObject();
		response.addProperty("ok", false);
		response.addProperty("error", "Item not found in EMI index: " + itemId);
		return response.toString();
	}

	private static JsonArray recipesJson(List<EmiRecipe> recipes, int limit, ItemStack focusedStack) {
		int safeLimit = Math.max(0, limit);
		JsonArray result = new JsonArray();
		recipes.stream().limit(safeLimit).map(recipe -> recipeJson(recipe, focusedStack)).forEach(result::add);
		return result;
	}

	private static List<EmiRecipe> matchingRecipes(EmiRecipeManager manager, EmiStack stack, boolean usages) {
		return usages ? manager.getRecipesByInput(stack) : manager.getRecipesByOutput(stack);
	}

	private static List<EmiRecipe> openedRecipes(EmiRecipeManager manager, EmiStack stack, boolean usages) {
		ensureEmiHostScreen();
		if (usages) {
			EmiApi.displayUses(stack);
		} else {
			EmiApi.displayRecipes(stack);
		}
		Screen screen = Minecraft.getInstance().screen;
		if (!(screen instanceof RecipeScreen)) {
			return matchingRecipes(manager, stack, usages);
		}
		try {
			List<EmiRecipe> recipes = recipesFromScreen(screen);
			ItemStack itemStack = stack.getItemStack();
			List<EmiRecipe> filteredScreenRecipes = recipes.stream().filter(recipe -> itemStack.isEmpty() || containsRecipeStack(recipe, itemStack, usages))
					.collect(Collectors.toList());
			return mergeRecipes(filteredScreenRecipes, matchingRecipes(manager, stack, usages));
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return matchingRecipes(manager, stack, usages);
		}
	}

	private static List<EmiRecipe> mergeRecipes(List<EmiRecipe> first, List<EmiRecipe> second) {
		Map<String, EmiRecipe> recipes = new LinkedHashMap<>();
		for (EmiRecipe recipe : first) {
			recipes.put(recipeKey(recipe), recipe);
		}
		for (EmiRecipe recipe : second) {
			recipes.putIfAbsent(recipeKey(recipe), recipe);
		}
		return List.copyOf(recipes.values());
	}

	private static String recipeKey(EmiRecipe recipe) {
		return recipe.getCategory().getId() + "|" + (recipe.getId() == null ? System.identityHashCode(recipe) : recipe.getId().toString());
	}

	private static void ensureEmiHostScreen() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen != null) {
			return;
		}
		if (minecraft.player == null) {
			throw new IllegalStateException("Client player is not available");
		}
		minecraft.setScreen(new InventoryScreen(minecraft.player));
	}

	private static List<EmiRecipe> recipesFromScreen(Screen screen) throws ReflectiveOperationException {
		Object recipesObject = getField(screen, "recipes");
		if (!(recipesObject instanceof Map<?, ?> recipesByCategory)) {
			throw new IllegalStateException("Unexpected EMI recipe screen recipes field");
		}
		List<EmiRecipe> recipes = new ArrayList<>();
		for (Object categoryRecipes : recipesByCategory.values()) {
			if (!(categoryRecipes instanceof Collection<?> collection)) {
				throw new IllegalStateException("Unexpected EMI recipe category value");
			}
			for (Object recipe : collection) {
				if (!(recipe instanceof EmiRecipe emiRecipe)) {
					throw new IllegalStateException("Unexpected EMI recipe entry");
				}
				recipes.add(emiRecipe);
			}
		}
		return recipes;
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

	private static boolean containsStack(List<? extends EmiIngredient> ingredients, ItemStack stack) {
		return ingredients.stream().flatMap(ingredient -> ingredient.getEmiStacks().stream()).map(EmiStack::getItemStack)
				.anyMatch(candidate -> stacksMatch(candidate, stack));
	}

	private static boolean containsRecipeStack(EmiRecipe recipe, ItemStack stack, boolean usages) {
		EmiDisplaySlots displaySlots = displaySlots(recipe);
		List<EmiIngredient> stacks = usages ? displaySlots.inputs() : displaySlots.outputs();
		return containsStack(stacks.isEmpty() ? usages ? recipe.getInputs() : recipe.getOutputs() : stacks, stack);
	}

	private static boolean stacksMatch(ItemStack candidate, ItemStack focusedStack) {
		if (ItemStack.isSameItemSameComponents(candidate, focusedStack)) {
			return true;
		}
		return (!hasColorComponents(focusedStack) || !hasColorComponents(candidate)) && ItemStack.isSameItem(candidate, focusedStack);
	}

	private static boolean hasColorComponents(ItemStack stack) {
		return stack.has(ModCoreDataComponents.MAIN_COLOR.get()) || stack.has(ModCoreDataComponents.ACCENT_COLOR.get());
	}

	private static JsonObject recipeJson(EmiRecipe recipe, ItemStack focusedStack) {
		EmiDisplaySlots displaySlots = displaySlots(recipe);
		List<? extends EmiIngredient> inputs = displaySlots.inputs().isEmpty() ? recipe.getInputs() : displaySlots.inputs();
		List<? extends EmiIngredient> outputs = displaySlots.outputs().isEmpty() ? recipe.getOutputs() : displaySlots.outputs();
		JsonObject result = new JsonObject();
		result.addProperty("id", recipe.getId() == null ? null : recipe.getId().toString());
		result.addProperty("category", recipe.getCategory().getId().toString());
		result.addProperty("categoryName", recipe.getCategory().getName().getString());
		result.addProperty("inputCount", inputs.size());
		result.addProperty("outputCount", outputs.size());
		result.add("inputs", JsonParser.parseString(ingredientsJson(inputs)));
		result.add("outputs", JsonParser.parseString(ingredientsJson(outputs)));
		return result;
	}

	private static EmiDisplaySlots displaySlots(EmiRecipe recipe) {
		CapturingWidgetHolder holder = new CapturingWidgetHolder(recipe);
		recipe.addWidgets(holder);
		return new EmiDisplaySlots(List.copyOf(holder.inputs), List.copyOf(holder.outputs));
	}

	private static class CapturingWidgetHolder implements WidgetHolder {
		private final EmiRecipe recipe;
		private final List<EmiIngredient> inputs = new ArrayList<>();
		private final List<EmiIngredient> outputs = new ArrayList<>();

		private CapturingWidgetHolder(EmiRecipe recipe) {
			this.recipe = recipe;
		}

		@Override
		public int getWidth() {
			return recipe.getDisplayWidth();
		}

		@Override
		public int getHeight() {
			return recipe.getDisplayHeight();
		}

		@Override
		public <T extends Widget> T add(T widget) {
			if (widget instanceof SlotWidget slot) {
				EmiIngredient stack = slotIngredient(slot);
				if (!stack.isEmpty()) {
					if (isOutputSlot(slot, recipe)) {
						outputs.add(stack);
					} else {
						inputs.add(stack);
					}
				}
			}
			return widget;
		}

		private static boolean isOutputSlot(SlotWidget slot, EmiRecipe recipe) {
			try {
				Field outputField = SlotWidget.class.getDeclaredField("output");
				outputField.setAccessible(true);
				return outputField.getBoolean(slot) || slot.getRecipe() != null || slot.getBounds().x() > recipe.getDisplayWidth() / 2;
			} catch (ReflectiveOperationException | RuntimeException e) {
				return slot.getRecipe() != null || slot.getBounds().x() > recipe.getDisplayWidth() / 2;
			}
		}

		@SuppressWarnings("unchecked")
		private static EmiIngredient slotIngredient(SlotWidget slot) {
			if (slot instanceof GeneratedSlotWidget generatedSlot) {
				try {
					Function<Random, EmiIngredient> supplier = (Function<Random, EmiIngredient>) generatedSlotSupplierField().get(generatedSlot);
					List<EmiStack> stacks = new ArrayList<>();
					for (int i = 0; i < 128; i++) {
						stacks.addAll(supplier.apply(new Random(0x9E3779B97F4A7C15L * i + 0xD1B54A32D192ED03L)).getEmiStacks());
					}
					return EmiIngredient.of(stacks);
				} catch (ReflectiveOperationException | RuntimeException e) {
					return slot.getStack();
				}
			}
			return slot.getStack();
		}

		private static Field generatedSlotSupplierField() throws NoSuchFieldException {
			try {
				Field field = GeneratedSlotWidget.class.getDeclaredField("stackSupplier");
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException ignored) {
				for (Field field : GeneratedSlotWidget.class.getDeclaredFields()) {
					if (Function.class.isAssignableFrom(field.getType())) {
						field.setAccessible(true);
						return field;
					}
				}
				throw ignored;
			}
		}
	}

	private record EmiDisplaySlots(List<EmiIngredient> inputs, List<EmiIngredient> outputs) {
	}

	private static String ingredientsJson(List<? extends EmiIngredient> ingredients) {
		return RecipeJsonUtil.ingredientsJson(ingredients.stream().map(EmiRecipeViewerAutomation::ingredientJson).collect(Collectors.toList()));
	}

	private static String ingredientJson(EmiIngredient ingredient) {
		return RecipeJsonUtil.ingredientJson(ingredient.getEmiStacks().stream()
				.map(stack -> stack.getItemStack().isEmpty()
						? RecipeJsonUtil.stackJson("entry", stack.getId().toString(), stack.getName().getString(), stack.getAmount(), stack.getChance())
						: RecipeJsonUtil.itemStackJson(stack.getItemStack()))
				.collect(Collectors.toList()));
	}

	@SuppressWarnings("unused")
	private static Map<String, Long> categoryCounts(List<EmiRecipe> recipes) {
		return recipes.stream().map(EmiRecipe::getCategory).map(EmiRecipeCategory::getId).map(Object::toString)
				.collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
	}

	private record RecipeStats(String className, String category, String namespace, String pathBucket, int inputs, int outputs, int inputAlternatives,
			int outputAlternatives, int broadInputSlots) {
		private static RecipeStats of(EmiRecipe recipe) {
			List<? extends EmiIngredient> inputs = recipe.getInputs();
			List<? extends EmiIngredient> outputs = recipe.getOutputs();
			String id = recipe.getId() == null ? "unknown:unknown" : recipe.getId().toString();
			String namespace = recipe.getId() == null ? "unknown" : recipe.getId().getNamespace();
			return new RecipeStats(recipe.getClass().getName(), recipe.getCategory().getId().toString(), namespace, pathBucket(id), inputs.size(),
					outputs.size(), alternatives(inputs), alternatives(outputs), broadSlots(inputs));
		}

		private static int alternatives(List<? extends EmiIngredient> ingredients) {
			return ingredients.stream().mapToInt(ingredient -> ingredient.getEmiStacks().size()).sum();
		}

		private static int broadSlots(List<? extends EmiIngredient> ingredients) {
			return (int) ingredients.stream().filter(ingredient -> ingredient.getEmiStacks().size() > 1).count();
		}

		private static String pathBucket(String id) {
			int namespaceSeparator = id.indexOf(':');
			String path = namespaceSeparator >= 0 ? id.substring(namespaceSeparator + 1) : id;
			String[] segments = path.split("/");
			if (segments.length >= 3 && "input".equals(segments[segments.length - 3])) {
				return String.join("/", Arrays.copyOf(segments, segments.length - 2));
			}
			if (segments.length >= 2 && "source".equals(segments[segments.length - 2])) {
				return String.join("/", Arrays.copyOf(segments, segments.length - 1));
			}
			if (segments.length >= 2 && segments[segments.length - 1].matches("\\d+")) {
				return String.join("/", Arrays.copyOf(segments, segments.length - 1));
			}
			return path;
		}
	}

	private static class MutableRecipeStats {
		private long recipes;
		private long inputs;
		private long outputs;
		private long inputAlternatives;
		private long outputAlternatives;
		private long broadInputSlots;

		private void add(RecipeStats stats) {
			recipes++;
			inputs += stats.inputs();
			outputs += stats.outputs();
			inputAlternatives += stats.inputAlternatives();
			outputAlternatives += stats.outputAlternatives();
			broadInputSlots += stats.broadInputSlots();
		}

		private long recipes() {
			return recipes;
		}

		private JsonObject json(String key) {
			JsonObject result = new JsonObject();
			result.addProperty("key", key);
			result.addProperty("recipes", recipes);
			result.addProperty("inputs", inputs);
			result.addProperty("outputs", outputs);
			result.addProperty("inputAlternatives", inputAlternatives);
			result.addProperty("outputAlternatives", outputAlternatives);
			result.addProperty("broadInputSlots", broadInputSlots);
			return result;
		}
	}
}
