package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SimpleMaterialScenarios {
	private SimpleMaterialScenarios() {
	}

	public static String verifyDecoration() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos tablePos = player.blockPosition().offset(0, 0, 4);
			level.setBlockAndUpdate(tablePos, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(tablePos, ModBlocks.DECORATION_TABLE.get().defaultBlockState());

			DecorationTableBlockEntity table = level.getBlockEntity(tablePos, ModBlocks.DECORATION_TABLE_BLOCK_ENTITY_TYPE.get()).orElse(null);
			if (table == null) {
				return "{\"ok\":false," + jsonProperty("error", "Decoration table block entity missing") + "}";
			}

			ResourceLocation expectedMaterial = BuiltInRegistries.BLOCK.getKey(Blocks.OAK_PLANKS);
			List<Item> itemsToVerify = new ArrayList<>(List.of(ModBlocks.CONTROLLER_ITEM.get(), ModBlocks.STORAGE_IO_ITEM.get(),
					ModBlocks.STORAGE_INPUT_ITEM.get(), ModBlocks.STORAGE_OUTPUT_ITEM.get(), ModBlocks.STORAGE_LINK_ITEM.get()));
			ModBlocks.STORAGE_CONNECTOR_ITEMS.values().forEach(item -> itemsToVerify.add(item.get()));

			StringBuilder results = new StringBuilder();
			int failed = 0;
			for (int i = 0; i < itemsToVerify.size(); i++) {
				Item item = itemsToVerify.get(i);
				if (i > 0) {
					results.append(',');
				}

				table.getStorageBlock().setStackInSlot(0, ItemStack.EMPTY);
				for (int slot = 0; slot < 7; slot++) {
					table.getDecorativeBlocks().setStackInSlot(slot, ItemStack.EMPTY);
				}

				ItemStack input = new ItemStack(item);
				ItemStack materialStack = new ItemStack(Items.OAK_PLANKS, 2);
				table.getStorageBlock().setStackInSlot(0, input);

				boolean slot0AcceptsMaterial = table.getDecorativeBlocks().isItemValid(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT, materialStack);
				boolean slot1AcceptsMaterial = table.getDecorativeBlocks().isItemValid(DecorationTableBlockEntity.TOP_TRIM_SLOT, materialStack);
				table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT, materialStack.copy());

				ItemStack result = table.getResult().copy();
				Optional<ResourceLocation> resultMaterial = SimpleMaterialBlockItem.getMaterial(result);
				Map<ResourceLocation, Integer> partsNeeded = table.getPartsNeeded();
				int oakPartsNeeded = partsNeeded.getOrDefault(expectedMaterial, 0);
				ItemStack extracted = table.extractResult(1);
				table.consumeIngredientsOnCraft();
				int remainingMaterialCount = table.getDecorativeBlocks().getStackInSlot(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT).getCount();

				boolean layoutSingle = table.getMaterialLayout() == DecorationTableBlockEntity.MaterialLayout.SINGLE;
				boolean resultMaterialMatches = resultMaterial.filter(expectedMaterial::equals).isPresent();
				Optional<ResourceLocation> extractedMaterial = SimpleMaterialBlockItem.getMaterial(extracted);
				boolean extractedMaterialMatches = extractedMaterial.filter(expectedMaterial::equals).isPresent();
				boolean passed = layoutSingle && table.isMaterialSlotActive(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT)
						&& !table.isMaterialSlotActive(DecorationTableBlockEntity.TOP_TRIM_SLOT) && !table.areTintsActive() && slot0AcceptsMaterial
						&& !slot1AcceptsMaterial && !result.isEmpty() && resultMaterialMatches && !extracted.isEmpty() && extractedMaterialMatches
						&& oakPartsNeeded == 24 && remainingMaterialCount == 1;
				if (!passed) {
					failed++;
				}

				results.append('{').append(jsonProperty("item", BuiltInRegistries.ITEM.getKey(item).toString())).append(',').append("\"passed\":")
						.append(passed).append(',').append(jsonProperty("layout", table.getMaterialLayout().name())).append(',').append("\"slot0Active\":")
						.append(table.isMaterialSlotActive(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT)).append(',').append("\"slot1Active\":")
						.append(table.isMaterialSlotActive(DecorationTableBlockEntity.TOP_TRIM_SLOT)).append(',').append("\"slot0AcceptsMaterial\":")
						.append(slot0AcceptsMaterial).append(',').append("\"slot1AcceptsMaterial\":").append(slot1AcceptsMaterial).append(',')
						.append("\"tintsActive\":").append(table.areTintsActive()).append(',')
						.append(jsonProperty("resultItem", result.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(result.getItem()).toString())).append(',')
						.append(jsonProperty("resultMaterial", resultMaterial.map(ResourceLocation::toString).orElse(null))).append(',')
						.append(jsonProperty("extractedMaterial", extractedMaterial.map(ResourceLocation::toString).orElse(null))).append(',')
						.append("\"oakPartsNeeded\":").append(oakPartsNeeded).append(',').append("\"remainingMaterialCount\":").append(remainingMaterialCount)
						.append('}');
			}

			return "{\"ok\":" + (failed == 0) + ',' + jsonProperty("tablePos", tablePos.toShortString()) + ',' + "\"total\":" + itemsToVerify.size() + ','
					+ "\"failed\":" + failed + ',' + "\"results\":[" + results + "]}";
		});
	}

	public static String setupRenderVerification() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos tablePos = player.blockPosition().offset(0, 0, 4);
			BlockPos controllerPos = player.blockPosition().offset(3, 1, 4);
			level.setBlockAndUpdate(tablePos, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(controllerPos, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(controllerPos.above(), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(tablePos, ModBlocks.DECORATION_TABLE.get().defaultBlockState());

			DecorationTableBlockEntity table = level.getBlockEntity(tablePos, ModBlocks.DECORATION_TABLE_BLOCK_ENTITY_TYPE.get()).orElse(null);
			if (table == null) {
				return "{\"ok\":false," + jsonProperty("error", "Decoration table block entity missing") + "}";
			}

			table.getStorageBlock().setStackInSlot(0, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT, new ItemStack(Items.DIAMOND_BLOCK, 2));
			ItemStack decoratedController = table.extractResult(1);
			table.consumeIngredientsOnCraft();
			Optional<ResourceLocation> decoratedMaterial = SimpleMaterialBlockItem.getMaterial(decoratedController);

			player.getInventory().clearContent();
			player.getInventory().setItem(0, decoratedController.copy());
			player.getInventory().setItem(1, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));

			level.setBlockAndUpdate(controllerPos, ModBlocks.CONTROLLER.get().defaultBlockState());
			ISimpleMaterialHolder holder = WorldHelper.getBlockEntity(level, controllerPos, ISimpleMaterialHolder.class).orElseThrow();
			SimpleMaterialBlockItem.getMaterial(decoratedController).ifPresentOrElse(holder::setMaterial, () -> holder.setMaterial(null));
			holder.setOverlayHidden(false);

			return "{\"ok\":" + (!decoratedController.isEmpty() && decoratedMaterial.isPresent()) + ',' + jsonProperty("tablePos", tablePos.toShortString())
					+ ',' + jsonProperty("controllerPos", controllerPos.toShortString()) + ','
					+ jsonProperty("decoratedItem",
							decoratedController.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(decoratedController.getItem()).toString())
					+ ',' + jsonProperty("decoratedMaterial", decoratedMaterial.map(ResourceLocation::toString).orElse(null)) + '}';
		});
	}

	public static String setupOverlayComparison() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos origin = player.blockPosition().offset(0, 0, 6);
			ResourceLocation material = BuiltInRegistries.BLOCK.getKey(Blocks.DIAMOND_BLOCK);

			BlockPos controllerShown = origin.offset(-3, 0, 0);
			BlockPos controllerHidden = origin.offset(-1, 0, 0);
			BlockPos linkShown = origin.offset(1, 0, 0);
			BlockPos linkHidden = origin.offset(3, 0, 0);
			List<BlockPos> positions = List.of(controllerShown, controllerHidden, linkShown, linkHidden);
			for (BlockPos pos : positions) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(pos.below(), Blocks.GRAY_CONCRETE.defaultBlockState());
			}

			level.setBlockAndUpdate(controllerShown, ModBlocks.CONTROLLER.get().defaultBlockState());
			level.setBlockAndUpdate(controllerHidden, ModBlocks.CONTROLLER.get().defaultBlockState());
			level.setBlockAndUpdate(linkShown, ModBlocks.STORAGE_LINK.get().defaultBlockState());
			level.setBlockAndUpdate(linkHidden, ModBlocks.STORAGE_LINK.get().defaultBlockState());

			boolean controllerShownSet = setSimpleMaterialState(level, controllerShown, material, false);
			boolean controllerHiddenSet = setSimpleMaterialState(level, controllerHidden, material, true);
			boolean linkShownSet = setSimpleMaterialState(level, linkShown, material, false);
			boolean linkHiddenSet = setSimpleMaterialState(level, linkHidden, material, true);

			player.getInventory().clearContent();
			ItemStack storageTool = new ItemStack(ModItems.STORAGE_TOOL.get());
			storageTool.set(ModDataComponents.TOOL_MODE, StorageToolItem.Mode.TIER_DISPLAY);
			player.getInventory().setItem(0, storageTool);
			player.getInventory().setChanged();

			return "{\"ok\":" + (controllerShownSet && controllerHiddenSet && linkShownSet && linkHiddenSet) + ','
					+ jsonProperty("material", material.toString()) + ',' + jsonProperty("controllerShown", controllerShown.toShortString()) + ','
					+ jsonProperty("controllerHidden", controllerHidden.toShortString()) + ',' + jsonProperty("linkShown", linkShown.toShortString()) + ','
					+ jsonProperty("linkHidden", linkHidden.toShortString()) + ',' + "\"controllerShownSet\":" + controllerShownSet + ','
					+ "\"controllerHiddenSet\":" + controllerHiddenSet + ',' + "\"linkShownSet\":" + linkShownSet + ',' + "\"linkHiddenSet\":" + linkHiddenSet
					+ '}';
		});
	}

	private static boolean setSimpleMaterialState(ServerLevel level, BlockPos pos, ResourceLocation material, boolean overlayHidden) {
		return WorldHelper.getBlockEntity(level, pos, ISimpleMaterialHolder.class).map(holder -> {
			holder.setMaterial(material);
			holder.setOverlayHidden(overlayHidden);
			return true;
		}).orElse(false);
	}

	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}
}
