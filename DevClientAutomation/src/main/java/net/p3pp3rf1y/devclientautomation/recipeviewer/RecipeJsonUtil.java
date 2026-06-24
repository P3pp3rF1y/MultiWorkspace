package net.p3pp3rf1y.devclientautomation.recipeviewer;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.JsonUtil;

import java.util.List;
import java.util.stream.Collectors;

public final class RecipeJsonUtil {
	private RecipeJsonUtil() {
	}

	public static String itemStackJson(ItemStack stack) {
		return "{" + JsonUtil.property("type", "item") + "," + JsonUtil.property("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) + ","
				+ JsonUtil.property("name", stack.getHoverName().getString()) + "," + "\"count\":" + stack.getCount() + ","
				+ JsonUtil.property("components", stack.getComponents().toString()) + ","
				+ JsonUtil.rawProperty("componentKeys", componentKeysJson(stack.getComponents())) + ","
				+ JsonUtil.rawProperty("encoded", encodedStackJson(stack)) + "}";
	}

	public static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static String componentKeysJson(DataComponentMap components) {
		return components.keySet().stream()
				.map(component -> JsonUtil.property("", BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component).toString()).substring(3))
				.collect(Collectors.joining(",", "[", "]"));
	}

	private static String encodedStackJson(ItemStack stack) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return "null";
		}
		return ItemStack.CODEC.encodeStart(minecraft.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), stack).map(JsonElement::toString)
				.result().orElse("null");
	}

	public static String stackJson(String type, String id, String name, long amount, float chance) {
		return "{" + JsonUtil.property("type", type) + "," + JsonUtil.property("id", id) + "," + JsonUtil.property("name", name) + "," + "\"amount\":" + amount
				+ "," + "\"chance\":" + chance + "}";
	}

	public static String ingredientJson(List<String> alternatives) {
		return "{\"alternativeCount\":" + alternatives.size() + ",\"alternatives\":[" + String.join(",", alternatives) + "]}";
	}

	public static String ingredientsJson(List<String> ingredients) {
		return "[" + String.join(",", ingredients) + "]";
	}

	public static String itemStackIngredientsJson(List<ItemStack> stacks) {
		return ingredientsJson(stacks.stream().map(stack -> ingredientJson(List.of(itemStackJson(stack)))).collect(Collectors.toList()));
	}

	public static String itemStackIngredientGroupsJson(List<List<ItemStack>> stacks) {
		return ingredientsJson(stacks.stream().map(group -> ingredientJson(group.stream().map(RecipeJsonUtil::itemStackJson).collect(Collectors.toList())))
				.collect(Collectors.toList()));
	}
}
