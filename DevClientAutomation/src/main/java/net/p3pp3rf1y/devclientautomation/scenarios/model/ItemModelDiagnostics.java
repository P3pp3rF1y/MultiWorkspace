package net.p3pp3rf1y.devclientautomation.scenarios.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.devclientautomation.platform.neoforge.NeoForgeModelDiagnostics;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
		BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0);
		List<BakedModel> passes = model.getRenderPasses(stack, false);
		StringBuilder json = new StringBuilder("{\"ok\":true,");
		json.append(jsonProperty("item", itemName)).append(',');
		json.append(jsonProperty("simpleMaterial", SimpleMaterialBlockItem.getMaterial(stack).map(ResourceLocation::toString).orElse(null))).append(',');
		json.append(jsonProperty("modelClass", model.getClass().getName())).append(',');
		json.append(jsonProperty("particle", NeoForgeModelDiagnostics.particleName(model))).append(',');
		json.append("\"passCount\":").append(passes.size()).append(',');
		json.append("\"passes\":[");
		for (int i = 0; i < passes.size(); i++) {
			if (i > 0) {
				json.append(',');
			}
			BakedModel pass = passes.get(i);
			List<BakedQuad> quads = new ArrayList<>();
			RandomSource random = RandomSource.create(42);
			for (Direction direction : Direction.values()) {
				random.setSeed(42);
				quads.addAll(pass.getQuads(null, direction, random));
			}
			random.setSeed(42);
			quads.addAll(pass.getQuads(null, null, random));
			Set<String> sprites = new HashSet<>();
			for (BakedQuad quad : quads) {
				sprites.add(quad.getSprite().contents().name().toString());
			}
			json.append('{').append(jsonProperty("class", pass.getClass().getName())).append(',').append("\"quadCount\":").append(quads.size()).append(',')
					.append("\"sprites\":[");
			int spriteIndex = 0;
			for (String sprite : sprites) {
				if (spriteIndex++ > 0) {
					json.append(',');
				}
				json.append('"').append(escapeJson(sprite)).append('"');
			}
			json.append("]}");
		}
		json.append("]}");
		return json.toString();
	}

	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}
}
