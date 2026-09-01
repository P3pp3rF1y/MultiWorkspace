package net.p3pp3rf1y.devclientautomation.scenarios.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.devclientautomation.platform.neoforge.NeoForgeModelDiagnostics;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;

public final class ItemModelDiagnostics {
	private ItemModelDiagnostics() {
	}

	public static String forItem(String itemName) {
		return AutomationRuntime.runOnClient(() -> {
			Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemName))
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
		ItemStackRenderState state = new ItemStackRenderState();
		minecraft.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, minecraft.level, minecraft.player, 0);
		StringBuilder json = new StringBuilder("{\"ok\":true,");
		json.append(jsonProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())).append(',');
		json.append(jsonProperty("simpleMaterial", SimpleMaterialBlockItem.getMaterial(stack).map(Identifier::toString).orElse(null))).append(',');
		json.append(jsonProperty("modelClass", ItemStackRenderState.class.getName())).append(',');
		json.append(jsonProperty("particle", NeoForgeModelDiagnostics.particleName(state))).append(',');
		json.append("\"passCount\":").append(state.isEmpty() ? 0 : 1).append(',');
		return json.append("\"passes\":[{").append(jsonProperty("class", ItemStackRenderState.class.getName())).append(",\"quadCount\":0,\"sprites\":[]}]}")
				.toString();
	}

	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}
}
