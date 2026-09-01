package net.p3pp3rf1y.devclientautomation.recipeviewer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public final class RecipeJsonUtil {
	private static final Gson GSON = new Gson();

	private RecipeJsonUtil() {
	}

	public static String itemStackJson(ItemStack stack) {
		JsonObject result = new JsonObject();
		result.addProperty("type", "item");
		result.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		result.addProperty("name", stack.getHoverName().getString());
		result.addProperty("count", stack.getCount());
		result.addProperty("components", stack.getComponents().toString());
		result.add("componentKeys", componentKeysJson(stack.getComponents()));
		result.add("encoded", encodedStackJson(stack));
		return GSON.toJson(result);
	}

	public static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static JsonArray componentKeysJson(DataComponentMap components) {
		JsonArray componentKeys = new JsonArray();
		components.keySet().forEach(component -> componentKeys.add(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component).toString()));
		return componentKeys;
	}

	private static JsonElement encodedStackJson(ItemStack stack) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return JsonNull.INSTANCE;
		}
		return ItemStack.CODEC.encodeStart(minecraft.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), stack).map(JsonElement::toString)
				.result().map(JsonParser::parseString).orElse(JsonNull.INSTANCE);
	}

	public static String stackJson(String type, String id, String name, long amount, float chance) {
		JsonObject result = new JsonObject();
		result.addProperty("type", type);
		result.addProperty("id", id);
		result.addProperty("name", name);
		result.addProperty("amount", amount);
		result.addProperty("chance", chance);
		return GSON.toJson(result);
	}

	public static String ingredientJson(List<String> alternatives) {
		JsonObject result = new JsonObject();
		result.addProperty("alternativeCount", alternatives.size());
		JsonArray alternativeArray = new JsonArray();
		alternatives.forEach(alternative -> alternativeArray.add(JsonParser.parseString(alternative)));
		result.add("alternatives", alternativeArray);
		return GSON.toJson(result);
	}

	public static String ingredientsJson(List<String> ingredients) {
		JsonArray result = new JsonArray();
		ingredients.forEach(ingredient -> result.add(JsonParser.parseString(ingredient)));
		return GSON.toJson(result);
	}

	public static String itemStackIngredientsJson(List<ItemStack> stacks) {
		return ingredientsJson(stacks.stream().map(stack -> ingredientJson(List.of(itemStackJson(stack)))).collect(Collectors.toList()));
	}

	public static String itemStackIngredientGroupsJson(List<List<ItemStack>> stacks) {
		return ingredientsJson(stacks.stream().map(group -> ingredientJson(group.stream().map(RecipeJsonUtil::itemStackJson).collect(Collectors.toList())))
				.collect(Collectors.toList()));
	}
}
