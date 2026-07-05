package net.p3pp3rf1y.devclientautomation.recipeviewer.jei;

import com.google.gson.JsonObject;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientSupplier;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.devclientautomation.JsonUtil;
import net.p3pp3rf1y.devclientautomation.recipeviewer.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

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
		Screen screen = Minecraft.getInstance().gui.screen();
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
			return runtimeUnavailableJson(Minecraft.getInstance().gui.screen());
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
			return runtimeUnavailableJson(Minecraft.getInstance().gui.screen());
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
		Screen screen = Minecraft.getInstance().gui.screen();
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
			return runtimeUnavailableJson(Minecraft.getInstance().gui.screen());
		}
		Optional<ItemStack> stack = findStack(jeiRuntime.getIngredientManager(), request);
		if (stack.isEmpty()) {
			return itemNotFoundJson(itemId);
		}
		int limit = RecipeViewerRequest.limit(request, 20);
		List<RecipeEntry> recipes = distinctRecipes(withFocusedFallback(jeiRuntime, stack.get(), RecipeIngredientRole.OUTPUT));
		List<RecipeEntry> uses = distinctRecipes(withFocusedFallback(jeiRuntime, stack.get(), RecipeIngredientRole.INPUT));
		return "{\"ok\":true," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("item", itemId(stack.get())) + ","
				+ JsonUtil.property("name", stack.get().getHoverName().getString()) + "," + "\"uiBacked\":true,"
				+ JsonUtil.rawProperty("focusStack", RecipeJsonUtil.itemStackJson(stack.get())) + "," + "\"recipeCount\":" + recipes.size() + ","
				+ "\"useCount\":" + uses.size() + "," + "\"recipes\":" + recipesJson(recipes, limit) + "," + "\"uses\":" + recipesJson(uses, limit) + "}";
	}

	@Override
	public String transferJson(String requestJson) {
		JsonObject request = RecipeViewerRequest.parse(requestJson);
		String itemId = RecipeViewerRequest.itemId(request);
		IJeiRuntime jeiRuntime = runtime;
		if (jeiRuntime == null) {
			return runtimeUnavailableJson(Minecraft.getInstance().gui.screen());
		}
		Optional<ItemStack> stack = findStack(jeiRuntime.getIngredientManager(), request);
		if (stack.isEmpty()) {
			return itemNotFoundJson(itemId);
		}

		IFocus<ItemStack> focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack.get());
		jeiRuntime.getRecipesGui().show(focus);
		Screen screen = Minecraft.getInstance().gui.screen();
		if (!isRecipeScreenOpen(screen)) {
			return "{\"ok\":false," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("error", "JEI recipe screen did not open") + ","
					+ JsonUtil.property("screenClass", screen == null ? null : screen.getClass().getName()) + "}";
		}

		try {
			Object parentContainerObject = invoke(screen, "getParentContainerMenu");
			if (!(parentContainerObject instanceof AbstractContainerMenu parentContainer)) {
				return "{\"ok\":true," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("item", itemId(stack.get()))
						+ ",\"recipeScreenOpen\":true,\"hasParentContainer\":false,\"hasTransferHandler\":false,\"visible\":false,\"active\":false}";
			}
			Player player = Minecraft.getInstance().player;
			if (player == null) {
				return "{\"ok\":false," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("error", "Client player is not available") + "}";
			}
			List<IRecipeLayoutDrawable<?>> layouts = openedRecipeLayouts(screen);
			if (layouts.isEmpty()) {
				return "{\"ok\":false," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("error", "No visible JEI recipe layouts") + "}";
			}

			return transferStateJson(jeiRuntime, parentContainer, player, layouts.get(0), stack.get());
		} catch (ReflectiveOperationException | RuntimeException e) {
			return "{\"ok\":false," + JsonUtil.property("viewer", id()) + "," + JsonUtil.property("error", e.getMessage()) + "}";
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
		Screen screen = Minecraft.getInstance().gui.screen();
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

	private static List<RecipeEntry> withFocusedFallback(IJeiRuntime jeiRuntime, ItemStack stack, RecipeIngredientRole role) {
		List<RecipeEntry> recipes = new ArrayList<>(openedRecipes(jeiRuntime, stack, role));
		if (!recipes.isEmpty() && !hasSophisticatedComponents(stack) && !isSophisticatedItem(stack)) {
			return recipes;
		}
		recipes.addAll(matchingRecipes(jeiRuntime, stack, role));
		return recipes;
	}

	private static boolean isSophisticatedItem(ItemStack stack) {
		String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
		return namespace.startsWith("sophisticated");
	}

	private static boolean hasSophisticatedComponents(ItemStack stack) {
		String stackJson = RecipeJsonUtil.itemStackJson(stack);
		return stackJson.contains("sophisticatedcore:") || stackJson.contains("sophisticatedstorage:") || stackJson.contains("sophisticatedstorageinmotion:");
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
			return List.of();
		}
		List<IRecipeLayoutDrawable<?>> entries = new ArrayList<>();
		for (Object recipeLayoutWithButtons : recipeLayoutsWithButtons) {
			Object recipeLayout = invokeAny(recipeLayoutWithButtons, "recipeLayout", "getRecipeLayout");
			if (recipeLayout instanceof IRecipeLayoutDrawable<?> layout) {
				entries.add(layout);
			}
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
		return "{\"ok\":true," + JsonUtil.property("viewer", "jei") + "," + JsonUtil.property("item", itemId(focusStack))
				+ ",\"recipeScreenOpen\":true,\"hasParentContainer\":true," + JsonUtil.property("parentContainerClass", parentContainer.getClass().getName())
				+ "," + JsonUtil.property("recipeId", recipeId(layout.getRecipe())) + "," + JsonUtil.property("category", category.getRecipeType().getUid().toString())
				+ ",\"hasTransferHandler\":" + handler.isPresent() + ",\"visible\":" + visible + ",\"active\":" + active + ","
				+ JsonUtil.property("errorType", errorType == null ? null : errorType.name()) + ",\"missingCountHint\":"
				+ (transferError == null ? 0 : transferError.getMissingCountHint()) + "}";
	}

	@SuppressWarnings("unchecked")
	private static RecipeEntry recipeEntry(IRecipeLayoutDrawable<?> layout) {
		IRecipeCategory<Object> category = (IRecipeCategory<Object>) layout.getRecipeCategory();
		Object recipe = layout.getRecipe();
		Identifier registryName = category.getRegistryName(recipe);
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
		return recipeEntry(recipeManager, category, recipe, recipeId(recipe));
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
		return layout.getRecipeSlotsView().getSlotViews(role).stream().map(slot -> slotStacks(slot, role)).toList();
	}

	private static List<ItemStack> slotStacks(IRecipeSlotView slot, RecipeIngredientRole role) {
		List<ItemStack> stacks = new ArrayList<>(slot.getItemStacks().toList());
		for (ItemStack overrideStack : displayOverrideItemStacks(slot)) {
			if (stacks.stream().noneMatch(stack -> ItemStack.isSameItemSameComponents(stack, overrideStack))) {
				stacks.add(0, overrideStack);
			}
		}
		slot.getDisplayedItemStack().filter(displayed -> stacks.stream().noneMatch(stack -> ItemStack.isSameItemSameComponents(stack, displayed)))
				.ifPresent(displayed -> stacks.add(0, displayed));
		return stacks;
	}

	private static List<ItemStack> displayOverrideItemStacks(IRecipeSlotView slot) {
		try {
			Field displayOverridesField = slot.getClass().getDeclaredField("displayOverrides");
			displayOverridesField.setAccessible(true);
			Object displayOverrides = displayOverridesField.get(slot);
			if (displayOverrides == null) {
				return List.of();
			}
			Method getAllIngredients = displayOverrides.getClass().getDeclaredMethod("getAllIngredients");
			getAllIngredients.setAccessible(true);
			Object ingredients = getAllIngredients.invoke(displayOverrides);
			if (!(ingredients instanceof List<?> ingredientList)) {
				return List.of();
			}
			List<ItemStack> stacks = new ArrayList<>();
			for (Object ingredient : ingredientList) {
				if (ingredient instanceof ITypedIngredient<?> typedIngredient) {
					typedIngredient.getIngredient(VanillaTypes.ITEM_STACK).ifPresent(stacks::add);
				}
			}
			return stacks;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return List.of();
		}
	}

	private static String recipeId(Object recipe) {
		if (recipe instanceof RecipeHolder<?> holder) {
			return holder.id().toString();
		}
		return null;
	}

	private static String recipesJson(List<RecipeEntry> recipes, int limit) {
		int safeLimit = Math.max(0, limit);
		String entries = recipes.stream().limit(safeLimit).map(JeiRecipeViewerAutomation::recipeJson).collect(Collectors.joining(","));
		return "[" + entries + "]";
	}

	private static List<RecipeEntry> distinctRecipes(List<RecipeEntry> recipes) {
		Map<String, RecipeEntry> distinct = new LinkedHashMap<>();
		for (RecipeEntry recipe : recipes) {
			String key = recipeIdentityKey(recipe);
			RecipeEntry existing = distinct.get(key);
			if (existing == null || componentScore(recipe) > componentScore(existing)) {
				distinct.put(key, recipe);
			}
		}
		return List.copyOf(distinct.values());
	}

	private static int componentScore(RecipeEntry recipe) {
		return componentScore(recipe.inputs()) + componentScore(recipe.outputs());
	}

	private static int componentScore(List<List<ItemStack>> stacks) {
		return stacks.stream().mapToInt(slot -> slot.stream().mapToInt(JeiRecipeViewerAutomation::componentScore).max().orElse(0)).sum();
	}

	private static int componentScore(ItemStack stack) {
		String stackJson = RecipeJsonUtil.itemStackJson(stack);
		return namespaceOccurrences(stackJson, "sophisticatedcore:") + namespaceOccurrences(stackJson, "sophisticatedstorage:")
				+ namespaceOccurrences(stackJson, "sophisticatedstorageinmotion:");
	}

	private static int namespaceOccurrences(String value, String namespace) {
		int count = 0;
		int index = value.indexOf(namespace);
		while (index >= 0) {
			count++;
			index = value.indexOf(namespace, index + namespace.length());
		}
		return count;
	}

	private static String recipeIdentityKey(RecipeEntry recipe) {
		return normalizedRecipeId(recipe.id()) + "|" + recipe.category();
	}

	private static String normalizedRecipeId(@Nullable String id) {
		if (id != null && id.startsWith("ResourceKey[minecraft:recipe / ") && id.endsWith("]")) {
			return id.substring("ResourceKey[minecraft:recipe / ".length(), id.length() - 1);
		}
		return String.valueOf(id);
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
