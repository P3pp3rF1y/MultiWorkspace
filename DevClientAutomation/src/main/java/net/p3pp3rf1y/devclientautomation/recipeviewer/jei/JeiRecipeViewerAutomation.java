package net.p3pp3rf1y.devclientautomation.recipeviewer.jei;

import com.google.gson.JsonObject;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.devclientautomation.JsonUtil;
import net.p3pp3rf1y.devclientautomation.recipeviewer.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.UnnecessaryImport")
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
		return "{\"ok\":true," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("searchText", jeiRuntime.getIngredientFilter().getFilterText())
				+ "," + "\"indexStackCount\":" + jeiRuntime.getIngredientManager().getAllItemStacks().size() + "," + "\"recipeCount\":"
				+ recipeCount(jeiRuntime) + "," + "\"recipeScreenOpen\":" + isRecipeScreenOpen(screen) + ","
				+ JsonUtil.property("screenClass", screen == null ? null : screen.getClass().getName()) + "}";
	}

	@Override
	public String searchJson(String query) {
		IJeiRuntime jeiRuntime = runtime;
		if (jeiRuntime == null) {
			return runtimeUnavailableJson(Minecraft.getInstance().screen);
		}
		jeiRuntime.getIngredientFilter().setFilterText(query);
		return "{\"ok\":true," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("searchText", jeiRuntime.getIngredientFilter().getFilterText())
				+ "}";
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
			return "{\"ok\":false," + JsonUtil.property("error", "Unknown mode: " + mode) + "}";
		}
		IFocus<ItemStack> focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack.get());
		jeiRuntime.getRecipesGui().show(focus);
		Screen screen = Minecraft.getInstance().screen;
		return "{\"ok\":true," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("item", itemId(stack.get())) + ","
				+ JsonUtil.property("mode", normalizedMode) + "," + "\"recipeScreenOpen\":" + isRecipeScreenOpen(screen) + ","
				+ JsonUtil.property("screenClass", screen == null ? null : screen.getClass().getName()) + "}";
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
		return "{\"ok\":true," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("item", itemId(stack.get())) + ","
				+ JsonUtil.property("name", stack.get().getHoverName().getString()) + "," + "\"uiBacked\":true,"
				+ JsonUtil.rawProperty("focusStack", RecipeJsonUtil.itemStackJson(stack.get())) + "," + "\"recipeCount\":" + recipes.size() + ","
				+ "\"useCount\":" + uses.size() + "," + "\"recipes\":" + recipesJson(recipes, limit) + "," + "\"uses\":" + recipesJson(uses, limit) + "}";
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

	private static List<RecipeEntry> matchingRecipes(IJeiRuntime jeiRuntime, ItemStack stack, RecipeIngredientRole role) {
		IFocus<ItemStack> focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
		IFocusGroup focusGroup = jeiRuntime.getJeiHelpers().getFocusFactory().createFocusGroup(List.of(focus));
		IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
		return recipeManager.createRecipeCategoryLookup().limitFocus(List.of(focus)).get().flatMap(
				category -> recipesForCategory(recipeManager, category, focus).stream().map(recipe -> recipeEntry(recipeManager, category, recipe, focusGroup)))
				.collect(Collectors.toList());
	}

	private static List<RecipeEntry> openedRecipes(IJeiRuntime jeiRuntime, ItemStack stack, RecipeIngredientRole role) {
		IFocus<ItemStack> focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
		jeiRuntime.getRecipesGui().show(focus);
		Screen screen = Minecraft.getInstance().screen;
		if (!isRecipeScreenOpen(screen)) {
			return List.of();
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
			return List.of();
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
		Object layouts = getField(screen, "layouts");
		Object layoutEntries = getField(layouts, "recipeLayoutsWithButtons");
		if (!(layoutEntries instanceof List<?> recipeLayoutsWithButtons)) {
			return List.of();
		}
		List<RecipeEntry> entries = new ArrayList<>();
		for (Object recipeLayoutWithButtons : recipeLayoutsWithButtons) {
			Object recipeLayout = invoke(recipeLayoutWithButtons, "recipeLayout");
			if (recipeLayout instanceof IRecipeLayoutDrawable<?> layout) {
				entries.add(recipeEntry(layout));
			}
		}
		return entries;
	}

	@SuppressWarnings("unchecked")
	private static RecipeEntry recipeEntry(IRecipeLayoutDrawable<?> layout) {
		IRecipeCategory<Object> category = (IRecipeCategory<Object>) layout.getRecipeCategory();
		Object recipe = layout.getRecipe();
		ResourceLocation registryName = category.getRegistryName(recipe);
		String id = registryName == null ? recipeId(recipe) : registryName.toString();
		List<List<ItemStack>> inputs = slotStacks(layout, RecipeIngredientRole.INPUT);
		List<List<ItemStack>> outputs = slotStacks(layout, RecipeIngredientRole.OUTPUT);
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
	private static List<?> recipesForCategory(IRecipeManager recipeManager, IRecipeCategory<?> category, IFocus<?> focus) {
		IRecipeCategory rawCategory = category;
		return recipeManager.createRecipeLookup(rawCategory.getRecipeType()).limitFocus(List.of(focus)).get().toList();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static RecipeEntry recipeEntry(IRecipeManager recipeManager, IRecipeCategory<?> category, Object recipe, IFocusGroup focusGroup) {
		IRecipeCategory rawCategory = category;
		Optional<IRecipeLayoutDrawable<Object>> layout = recipeManager.createRecipeLayoutDrawable(rawCategory, recipe, focusGroup);
		if (layout.isPresent()) {
			return new RecipeEntry(recipeId(recipe), category.getRecipeType().getUid().toString(), category.getTitle().getString(),
					slotStacks(layout.get(), RecipeIngredientRole.INPUT), slotStacks(layout.get(), RecipeIngredientRole.OUTPUT));
		}
		return new RecipeEntry(recipeId(recipe), category.getRecipeType().getUid().toString(), category.getTitle().getString(), List.of(), List.of());
	}

	private static List<List<ItemStack>> slotStacks(IRecipeLayoutDrawable<?> layout, RecipeIngredientRole role) {
		return layout.getRecipeSlotsView().getSlotViews(role).stream().map(IRecipeSlotView::getItemStacks).map(stream -> stream.toList()).toList();
	}

	private static String recipeId(Object recipe) {
		if (recipe instanceof Recipe<?> recipeWithId) {
			return recipeWithId.getId().toString();
		}
		return null;
	}

	private static String recipesJson(List<RecipeEntry> recipes, int limit) {
		int safeLimit = Math.max(0, limit);
		String entries = recipes.stream().limit(safeLimit).map(JeiRecipeViewerAutomation::recipeJson).collect(Collectors.joining(","));
		return "[" + entries + "]";
	}

	private static String recipeJson(RecipeEntry recipe) {
		return "{" + JsonUtil.property("id", recipe.id()) + "," + JsonUtil.property("category", recipe.category()) + ","
				+ JsonUtil.property("categoryName", recipe.categoryName()) + "," + "\"inputCount\":" + recipe.inputs().size() + "," + "\"outputCount\":"
				+ recipe.outputs().size() + "," + "\"inputs\":" + RecipeJsonUtil.itemStackIngredientGroupsJson(recipe.inputs()) + "," + "\"outputs\":"
				+ RecipeJsonUtil.itemStackIngredientGroupsJson(recipe.outputs()) + "}";
	}

	private static String itemNotFoundJson(String itemId) {
		return "{\"ok\":false," + JsonUtil.property("error", "Item not found in JEI index: " + itemId) + "}";
	}

	private static String runtimeUnavailableJson(@Nullable Screen screen) {
		return "{\"ok\":false," + JsonUtil.property("viewer", "jei") + "," + JsonUtil.property("error", "JEI runtime is not available") + ","
				+ JsonUtil.property("screenClass", screen == null ? null : screen.getClass().getName()) + "}";
	}

	private static boolean isRecipeScreenOpen(@Nullable Screen screen) {
		return screen != null && screen.getClass().getName().contains("RecipesGui");
	}

	private record RecipeEntry(@Nullable String id, String category, String categoryName, List<List<ItemStack>> inputs, List<List<ItemStack>> outputs) {
	}
}
