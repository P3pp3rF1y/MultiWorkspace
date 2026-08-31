package net.p3pp3rf1y.devclientautomation.recipeviewer.jei;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientSupplier;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.devclientautomation.recipeviewer.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

public class JeiRecipeViewerAutomation implements RecipeViewerAutomation {
	@Nullable
	private static IJeiRuntime runtime;

	public static void setRuntime(@Nullable IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
	}

	@Override
	public String id() {
		return "jei";
	}

	@Override
	public String stateJson() {
		IJeiRuntime jeiRuntime = runtime;
		Screen screen = Minecraft.getInstance().screen;
		if (jeiRuntime == null) {
			return runtimeUnavailableJson(screen);
		}
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", id());
		response.addProperty("searchText", jeiRuntime.getIngredientFilter().getFilterText());
		response.addProperty("indexStackCount", jeiRuntime.getIngredientManager().getAllItemStacks().size());
		response.addProperty("recipeCount", recipeCount(jeiRuntime));
		response.addProperty("recipeScreenOpen", isRecipeScreenOpen(screen));
		response.addProperty("screenClass", screen == null ? null : screen.getClass().getName());
		return response.toString();
	}

	@Override
	public String searchJson(String query) {
		IJeiRuntime jeiRuntime = runtime;
		if (jeiRuntime == null) {
			return runtimeUnavailableJson(Minecraft.getInstance().screen);
		}
		jeiRuntime.getIngredientFilter().setFilterText(query);
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", id());
		response.addProperty("searchText", jeiRuntime.getIngredientFilter().getFilterText());
		return response.toString();
	}

	@Override
	public String openJson(String requestJson) {
		JsonObject request = RecipeViewerRequest.parse(requestJson);
		String itemId = RecipeViewerRequest.itemId(request);
		IJeiRuntime jeiRuntime = runtime;
		if (jeiRuntime == null) {
			return runtimeUnavailableJson(Minecraft.getInstance().screen);
		}
		Optional<ItemStack> stack = findStack(jeiRuntime.getIngredientManager(), request);
		if (stack.isEmpty()) {
			return itemNotFoundJson(itemId);
		}
		String mode = RecipeViewerRequest.mode(request, "recipes");
		String normalizedMode = mode.toLowerCase(Locale.ROOT);
		RecipeIngredientRole role;
		if (normalizedMode.equals("recipes") || normalizedMode.equals("recipe") || normalizedMode.equals("crafting")) {
			role = RecipeIngredientRole.OUTPUT;
		} else if (normalizedMode.equals("uses") || normalizedMode.equals("usage") || normalizedMode.equals("crafts_into")) {
			role = RecipeIngredientRole.INPUT;
		} else {
			JsonObject response = new JsonObject();
			response.addProperty("ok", false);
			response.addProperty("error", "Unknown mode: " + mode);
			return response.toString();
		}
		IFocus<ItemStack> focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack.get());
		jeiRuntime.getRecipesGui().show(focus);
		Screen screen = Minecraft.getInstance().screen;
		JsonObject response = new JsonObject();
		response.addProperty("ok", isRecipeScreenOpen(screen));
		response.addProperty("viewer", id());
		response.addProperty("item", itemId(stack.get()));
		response.addProperty("mode", normalizedMode);
		response.addProperty("recipeScreenOpen", isRecipeScreenOpen(screen));
		response.addProperty("screenClass", screen == null ? null : screen.getClass().getName());
		if (!isRecipeScreenOpen(screen)) {
			response.addProperty("error", "JEI recipe screen did not open");
		}
		return response.toString();
	}

	@Override
	public String queryJson(String requestJson) {
		JsonObject request = RecipeViewerRequest.parse(requestJson);
		String itemId = RecipeViewerRequest.itemId(request);
		IJeiRuntime jeiRuntime = runtime;
		if (jeiRuntime == null) {
			return runtimeUnavailableJson(Minecraft.getInstance().screen);
		}
		Optional<ItemStack> stack = findStack(jeiRuntime.getIngredientManager(), request);
		if (stack.isEmpty()) {
			return itemNotFoundJson(itemId);
		}
		int limit = RecipeViewerRequest.limit(request, 20);
		List<RecipeEntry> recipes = openedRecipes(jeiRuntime, stack.get(), RecipeIngredientRole.OUTPUT);
		List<RecipeEntry> uses = openedRecipes(jeiRuntime, stack.get(), RecipeIngredientRole.INPUT);
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", id());
		response.addProperty("item", itemId(stack.get()));
		response.addProperty("name", stack.get().getHoverName().getString());
		response.addProperty("uiBacked", true);
		response.add("focusStack", JsonParser.parseString(RecipeJsonUtil.itemStackJson(stack.get())));
		response.addProperty("recipeCount", recipes.size());
		response.addProperty("useCount", uses.size());
		response.add("recipes", recipesJson(recipes, limit));
		response.add("uses", recipesJson(uses, limit));
		return response.toString();
	}

	@Override
	public String transferJson(String requestJson) {
		JsonObject request = RecipeViewerRequest.parse(requestJson);
		String itemId = RecipeViewerRequest.itemId(request);
		IJeiRuntime jeiRuntime = runtime;
		if (jeiRuntime == null) {
			return runtimeUnavailableJson(Minecraft.getInstance().screen);
		}
		Optional<ItemStack> stack = findStack(jeiRuntime.getIngredientManager(), request);
		if (stack.isEmpty()) {
			return itemNotFoundJson(itemId);
		}

		IFocus<ItemStack> focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack.get());
		jeiRuntime.getRecipesGui().show(focus);
		Screen screen = Minecraft.getInstance().screen;
		if (!isRecipeScreenOpen(screen)) {
			JsonObject response = new JsonObject();
			response.addProperty("ok", false);
			response.addProperty("viewer", id());
			response.addProperty("error", "JEI recipe screen did not open");
			response.addProperty("screenClass", screen == null ? null : screen.getClass().getName());
			return response.toString();
		}

		try {
			Object parentContainerObject = invoke(screen, "getParentContainerMenu");
			if (!(parentContainerObject instanceof AbstractContainerMenu parentContainer)) {
				JsonObject response = new JsonObject();
				response.addProperty("ok", false);
				response.addProperty("viewer", id());
				response.addProperty("error", "JEI recipe screen has no parent container");
				return response.toString();
			}
			Player player = Minecraft.getInstance().player;
			if (player == null) {
				JsonObject response = new JsonObject();
				response.addProperty("ok", false);
				response.addProperty("viewer", id());
				response.addProperty("error", "Client player is not available");
				return response.toString();
			}
			List<IRecipeLayoutDrawable<?>> layouts = openedRecipeLayouts(screen);
			if (layouts.isEmpty()) {
				JsonObject response = new JsonObject();
				response.addProperty("ok", false);
				response.addProperty("viewer", id());
				response.addProperty("error", "No visible JEI recipe layouts");
				return response.toString();
			}

			return transferStateJson(jeiRuntime, parentContainer, player, layouts.getFirst(), stack.get());
		} catch (ReflectiveOperationException | RuntimeException e) {
			JsonObject response = new JsonObject();
			response.addProperty("ok", false);
			response.addProperty("viewer", id());
			response.addProperty("error", e.getMessage());
			return response.toString();
		}
	}

	private static Optional<ItemStack> findStack(IIngredientManager ingredientManager, JsonObject request) {
		Optional<ItemStack> encodedStack = RecipeViewerFocus.encodedStack(request);
		if (encodedStack.isPresent()) {
			return encodedStack;
		}
		Optional<JsonObject> focus = RecipeViewerRequest.focus(request);
		String itemId = RecipeViewerRequest.itemId(request);
		String normalized = itemId.toLowerCase(Locale.ROOT);
		Optional<ItemStack> exact = ingredientManager.getAllItemStacks().stream().filter(stack -> itemId(stack).equals(normalized))
				.filter(stack -> focus.map(selector -> RecipeViewerStackMatcher.matches(stack, selector)).orElse(true)).findFirst();
		if (exact.isPresent()) {
			return exact;
		}
		return ingredientManager.getAllItemStacks().stream().filter(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT).equals(normalized))
				.filter(stack -> focus.map(selector -> RecipeViewerStackMatcher.matches(stack, selector)).orElse(true)).findFirst();
	}

	private static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static int recipeCount(IJeiRuntime jeiRuntime) {
		return jeiRuntime.getRecipeManager().createRecipeCategoryLookup().get()
				.mapToInt(category -> recipesForCategory(jeiRuntime.getRecipeManager(), category).size()).sum();
	}

	private static List<RecipeEntry> openedRecipes(IJeiRuntime jeiRuntime, ItemStack stack, RecipeIngredientRole role) {
		IFocus<ItemStack> focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
		jeiRuntime.getRecipesGui().show(focus);
		Screen screen = Minecraft.getInstance().screen;
		if (!isRecipeScreenOpen(screen)) {
			throw new IllegalStateException("JEI did not open a recipe screen");
		}
		try {
			Object logic = getField(screen, "logic");
			List<RecipeEntry> entries = new ArrayList<>();
			Set<String> visitedCategories = new HashSet<>();
			int categoryGuard = 0;
			do {
				Object selectedCategory = invoke(logic, "getSelectedRecipeCategory");
				String categoryKey = recipeCategoryKey(selectedCategory);
				if (!visitedCategories.add(categoryKey)) {
					break;
				}
				collectOpenedPages(screen, logic, entries);
				invoke(logic, "nextRecipeCategory");
				invoke(screen, "updateLayout");
				categoryGuard++;
			} while (categoryGuard < 100);
			return entries;
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw new IllegalStateException("Failed to read opened JEI recipes", e);
		}
	}

	private static void collectOpenedPages(Screen screen, Object logic, List<RecipeEntry> entries) throws ReflectiveOperationException {
		Set<String> visitedPages = new HashSet<>();
		int pageGuard = 0;
		do {
			invoke(screen, "updateLayout");
			String pageKey = recipeCategoryKey(invoke(logic, "getSelectedRecipeCategory")) + "|" + getField(screen, "pageString");
			if (!visitedPages.add(pageKey)) {
				break;
			}
			entries.addAll(openedRecipeLayoutEntries(screen));
			invoke(logic, "nextPage");
			pageGuard++;
		} while (pageGuard < 100);
	}

	private static List<RecipeEntry> openedRecipeLayoutEntries(Screen screen) throws ReflectiveOperationException {
		List<IRecipeLayoutDrawable<?>> layouts = openedRecipeLayouts(screen);
		List<RecipeEntry> entries = new ArrayList<>();
		for (IRecipeLayoutDrawable<?> layout : layouts) {
			entries.add(recipeEntry(layout));
		}
		return entries;
	}

	private static List<IRecipeLayoutDrawable<?>> openedRecipeLayouts(Screen screen) throws ReflectiveOperationException {
		Object layouts = getField(screen, "layouts");
		Object layoutEntries = getField(layouts, "recipeLayoutsWithButtons");
		if (!(layoutEntries instanceof List<?> recipeLayoutsWithButtons)) {
			throw new IllegalStateException("Unexpected JEI recipe layout entries");
		}
		List<IRecipeLayoutDrawable<?>> entries = new ArrayList<>();
		for (Object recipeLayoutWithButtons : recipeLayoutsWithButtons) {
			Object recipeLayout = invokeAny(recipeLayoutWithButtons, "recipeLayout", "getRecipeLayout");
			if (!(recipeLayout instanceof IRecipeLayoutDrawable<?> layout)) {
				throw new IllegalStateException("Unexpected JEI recipe layout entry");
			}
			entries.add(layout);
		}
		return entries;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static String transferStateJson(IJeiRuntime jeiRuntime, AbstractContainerMenu parentContainer, Player player, IRecipeLayoutDrawable<?> layout,
			ItemStack focusStack) {
		IRecipeCategory<Object> category = (IRecipeCategory<Object>) layout.getRecipeCategory();
		Optional<IRecipeTransferHandler<AbstractContainerMenu, Object>> handler = (Optional) jeiRuntime.getRecipeTransferManager()
				.getRecipeTransferHandler(parentContainer, category);
		IRecipeTransferError transferError = null;
		if (handler.isPresent()) {
			transferError = handler.get().transferRecipe(parentContainer, layout.getRecipe(), layout.getRecipeSlotsView(), player, false, false);
		}
		IRecipeTransferError.Type errorType = transferError == null ? null : transferError.getType();
		boolean active = handler.isPresent() && (transferError == null || errorType.allowsTransfer);
		boolean visible = active || errorType == IRecipeTransferError.Type.USER_FACING;
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		response.addProperty("viewer", "jei");
		response.addProperty("item", itemId(focusStack));
		response.addProperty("recipeScreenOpen", true);
		response.addProperty("hasParentContainer", true);
		response.addProperty("parentContainerClass", parentContainer.getClass().getName());
		response.addProperty("recipeId", recipeId(layout.getRecipe()));
		response.addProperty("category", category.getRecipeType().getUid().toString());
		response.addProperty("hasTransferHandler", handler.isPresent());
		response.addProperty("visible", visible);
		response.addProperty("active", active);
		response.addProperty("errorType", errorType == null ? null : errorType.name());
		response.addProperty("missingCountHint", transferError == null ? 0 : transferError.getMissingCountHint());
		return response.toString();
	}

	@SuppressWarnings("unchecked")
	private static RecipeEntry recipeEntry(IRecipeLayoutDrawable<?> layout) {
		IRecipeCategory<Object> category = (IRecipeCategory<Object>) layout.getRecipeCategory();
		Object recipe = layout.getRecipe();
		ResourceLocation registryName = category.getRegistryName(recipe);
		String id = registryName == null ? recipeId(recipe) : registryName.toString();
		List<List<ItemStack>> inputs = slotStacks(layout, RecipeIngredientRole.INPUT);
		List<List<ItemStack>> outputs = slotStacks(layout, RecipeIngredientRole.OUTPUT);
		if (inputs.isEmpty() && outputs.isEmpty() && runtime != null) {
			return recipeEntry(runtime.getRecipeManager(), category, recipe, id);
		}
		return new RecipeEntry(id, category.getRecipeType().getUid().toString(), category.getTitle().getString(), inputs, outputs);
	}

	private static String recipeCategoryKey(Object category) {
		if (category instanceof IRecipeCategory<?> recipeCategory) {
			return recipeCategory.getRecipeType().getUid().toString();
		}
		return String.valueOf(category);
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

	private static Object invokeAny(Object target, String... names) throws ReflectiveOperationException {
		ReflectiveOperationException lastException = null;
		for (String name : names) {
			try {
				return invoke(target, name);
			} catch (ReflectiveOperationException e) {
				lastException = e;
			}
		}
		throw lastException == null ? new NoSuchMethodException(Arrays.toString(names)) : lastException;
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

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static List<?> recipesForCategory(IRecipeManager recipeManager, IRecipeCategory<?> category) {
		IRecipeCategory rawCategory = category;
		return recipeManager.createRecipeLookup(rawCategory.getRecipeType()).get().toList();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static RecipeEntry recipeEntry(IRecipeManager recipeManager, IRecipeCategory<?> category, Object recipe, String id) {
		IRecipeCategory rawCategory = category;
		IIngredientSupplier ingredients = recipeManager.getRecipeIngredients(rawCategory, recipe);
		return new RecipeEntry(id, category.getRecipeType().getUid().toString(), category.getTitle().getString(),
				ingredients.getIngredients(RecipeIngredientRole.INPUT).stream().map(typed -> typed.getItemStack()).flatMap(Optional::stream).map(List::of)
						.toList(),
				ingredients.getIngredients(RecipeIngredientRole.OUTPUT).stream().map(typed -> typed.getItemStack()).flatMap(Optional::stream).map(List::of)
						.toList());
	}

	private static List<List<ItemStack>> slotStacks(IRecipeLayoutDrawable<?> layout, RecipeIngredientRole role) {
		return layout.getRecipeSlotsView().getSlotViews(role).stream().map(IRecipeSlotView::getItemStacks).map(Stream::toList).toList();
	}

	private static String recipeId(Object recipe) {
		if (recipe instanceof RecipeHolder<?> holder) {
			return holder.id().toString();
		}
		return null;
	}

	private static JsonArray recipesJson(List<RecipeEntry> recipes, int limit) {
		int safeLimit = Math.max(0, limit);
		JsonArray result = new JsonArray();
		recipes.stream().limit(safeLimit).map(JeiRecipeViewerAutomation::recipeJson).forEach(result::add);
		return result;
	}

	private static JsonObject recipeJson(RecipeEntry recipe) {
		JsonObject result = new JsonObject();
		result.addProperty("id", recipe.id());
		result.addProperty("category", recipe.category());
		result.addProperty("categoryName", recipe.categoryName());
		result.addProperty("inputCount", recipe.inputs().size());
		result.addProperty("outputCount", recipe.outputs().size());
		result.add("inputs", JsonParser.parseString(RecipeJsonUtil.itemStackIngredientGroupsJson(recipe.inputs())));
		result.add("outputs", JsonParser.parseString(RecipeJsonUtil.itemStackIngredientGroupsJson(recipe.outputs())));
		return result;
	}

	private static String itemNotFoundJson(String itemId) {
		JsonObject response = new JsonObject();
		response.addProperty("ok", false);
		response.addProperty("error", "Item not found in JEI index: " + itemId);
		return response.toString();
	}

	private static String runtimeUnavailableJson(@Nullable Screen screen) {
		JsonObject response = new JsonObject();
		response.addProperty("ok", false);
		response.addProperty("viewer", "jei");
		response.addProperty("error", "JEI runtime is not available");
		response.addProperty("screenClass", screen == null ? null : screen.getClass().getName());
		return response.toString();
	}

	private static boolean isRecipeScreenOpen(@Nullable Screen screen) {
		return screen != null && screen.getClass().getName().contains("RecipesGui");
	}

	private record RecipeEntry(@Nullable String id, String category, String categoryName, List<List<ItemStack>> inputs, List<List<ItemStack>> outputs) {
	}
}
