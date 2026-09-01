package net.p3pp3rf1y.devclientautomation.scenarios.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;

public final class ItemModelDiagnostics {
	private ItemModelDiagnostics() {
	}

	public static String forItem(String itemName) {
		return AutomationRuntime.runOnClient(() -> {
			Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemName))
					.orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemName));
			return describe(new ItemStack(item));
		});
	}

	public static String forHotbarSlot(int slot) {
		return AutomationRuntime.runOnClient(() -> {
			if (Minecraft.getInstance().player == null) {
				throw new IllegalStateException("Client player is not available");
			}
			return describe(Minecraft.getInstance().player.getInventory().getItem(slot));
		});
	}

	private static String describe(ItemStack stack) {
		Minecraft minecraft = Minecraft.getInstance();
		String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		ResourceLocation itemModelId = stack.get(DataComponents.ITEM_MODEL);
		ItemModel model = minecraft.getModelManager().getItemModel(itemModelId);
		StringBuilder json = new StringBuilder("{\"ok\":true,");
		json.append(jsonProperty("item", itemName)).append(',');
		json.append(jsonProperty("simpleMaterial", SimpleMaterialBlockItem.getMaterial(stack).map(ResourceLocation::toString).orElse(null))).append(',');
		json.append(jsonProperty("itemModel", itemModelId.toString())).append(',');
		json.append(jsonProperty("modelClass", model.getClass().getName())).append(',');
		json.append("\"passCount\":0,\"passes\":[]}");
		return json.toString();
	}

	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}
}
