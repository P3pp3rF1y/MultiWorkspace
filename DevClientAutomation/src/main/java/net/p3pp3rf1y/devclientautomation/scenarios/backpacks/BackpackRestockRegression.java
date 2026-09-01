package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.InventoryInteractionHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class BackpackRestockRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);

	private BackpackRestockRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		RestockCase testCase = new RestockCase(string(request, "upgrade", "basic"), string(request, "mode", "allow"));
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(player -> run(player, testCase)));
	}

	private static String run(ServerPlayer player, RestockCase testCase) {
		RestockSpec spec = RestockSpec.of(testCase);
		ServerLevel level = (ServerLevel) player.level();
		BlockPos barrelPos = player.blockPosition().relative(player.getDirection(), 3);
		try {
			player.closeContainer();
			player.getInventory().clearContent();
			ItemStack backpack = configuredBackpack(spec);
			player.getInventory().setItem(0, backpack);
			player.getInventory().setSelectedSlot(0);
			player.getInventory().setChanged();

			StorageBlockEntity barrel = createBarrel(level, barrelPos, spec);
			boolean interactionHandled = InventoryInteractionHelper.tryInventoryInteraction(barrelPos, level, backpack, Direction.UP, player);
			IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
			int backpackDiamonds = count(wrapper.getInventoryHandler(), Items.DIAMOND);
			int backpackRedstone = count(wrapper.getInventoryHandler(), Items.REDSTONE);
			int barrelDiamonds = count(barrel.getStorageWrapper().getInventoryHandler(), Items.DIAMOND);
			int barrelRedstone = count(barrel.getStorageWrapper().getInventoryHandler(), Items.REDSTONE);
			boolean persisted = hasPersistedSettings(backpack, spec);
			boolean transferMatches = spec.expectTransfer()
					? backpackDiamonds == 5 && backpackRedstone == 0 && barrelDiamonds == 0 && barrelRedstone == 5
					: backpackDiamonds == 0 && backpackRedstone == 0 && barrelDiamonds == 0 && barrelRedstone == 0;
			boolean passed = interactionHandled && persisted && transferMatches;
			return "{\"ok\":" + passed + ",\"upgrade\":\"" + testCase.upgrade() + "\",\"mode\":\"" + testCase.mode() + "\",\"filterSlots\":"
					+ spec.filterSlots() + ",\"interactionHandled\":" + interactionHandled + ",\"settingsPersisted\":" + persisted + ",\"transferMatches\":"
					+ transferMatches + ",\"backpackDiamonds\":" + backpackDiamonds + ",\"backpackRedstone\":" + backpackRedstone + ",\"barrelDiamonds\":"
					+ barrelDiamonds + ",\"barrelRedstone\":" + barrelRedstone + "}";
		} finally {
			player.closeContainer();
			player.getInventory().clearContent();
			player.getInventory().setChanged();
			clearBarrel(level, barrelPos);
		}
	}

	private static ItemStack configuredBackpack(RestockSpec spec) {
		ItemStack backpack = BackpackRegressionFixture.create(ModItems.DIAMOND_BACKPACK.get(), 0x3870AA, 0xE5A038).backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		clearInventory(wrapper.getInventoryHandler());
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(1, new ItemStack(spec.upgradeItem()));
		RestockUpgradeWrapper restock = restockUpgrade(wrapper);
		restock.getFilterLogic().setDepositFilterType(spec.filterType());
		restock.getFilterLogic().getFilterHandler().setStackInSlot(spec.filterSlots() - 1, new ItemStack(spec.filterItem()));
		upgrades.saveInventory();
		return backpack;
	}

	private static StorageBlockEntity createBarrel(ServerLevel level, BlockPos pos, RestockSpec spec) {
		clearBarrel(level, pos);
		level.setBlock(pos, ModBlocks.BARREL.get().defaultBlockState(), 3);
		StorageBlockEntity barrel = level.getBlockEntity(pos, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get()).map(be -> (StorageBlockEntity) be)
				.orElseThrow(() -> new IllegalStateException("Missing restock test barrel"));
		InventoryHandler inventory = barrel.getStorageWrapper().getInventoryHandler();
		if (spec.expectTransfer()) {
			inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
			inventory.setStackInSlot(1, new ItemStack(Items.REDSTONE, 5));
		}
		inventory.saveInventory();
		return barrel;
	}

	private static void clearBarrel(ServerLevel level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
			storage.clearContent();
		}
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	private static boolean hasPersistedSettings(ItemStack backpack, RestockSpec spec) {
		RestockUpgradeWrapper restock = restockUpgrade(BackpackWrapper.fromStack(backpack));
		return restock.getFilterLogic().getFilterType() == spec.filterType() && restock.getFilterLogic().getFilterHandler().size() == spec.filterSlots()
				&& restock.getFilterLogic().getFilterHandler().getStackInSlot(spec.filterSlots() - 1).is(spec.filterItem());
	}

	private static RestockUpgradeWrapper restockUpgrade(IBackpackWrapper wrapper) {
		List<RestockUpgradeWrapper> restockUpgrades = wrapper.getUpgradeHandler().getWrappersThatImplement(RestockUpgradeWrapper.class);
		if (restockUpgrades.size() != 1) {
			throw new IllegalStateException("Expected exactly one restock upgrade, found " + restockUpgrades.size());
		}
		return restockUpgrades.getFirst();
	}

	private static void clearInventory(InventoryHandler inventory) {
		for (int slot = 0; slot < inventory.size(); slot++) {
			inventory.setStackInSlot(slot, ItemStack.EMPTY);
		}
		inventory.saveInventory();
	}

	private static int count(InventoryHandler inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private record RestockCase(String upgrade, String mode) {
	}

	private record RestockSpec(RestockUpgradeItem upgradeItem, ContentsFilterType filterType, Item filterItem, int filterSlots, boolean expectTransfer) {
		private static RestockSpec of(RestockCase testCase) {
			RestockUpgradeItem upgradeItem = switch (testCase.upgrade()) {
				case "basic" -> ModItems.RESTOCK_UPGRADE.get();
				case "advanced" -> ModItems.ADVANCED_RESTOCK_UPGRADE.get();
				default -> throw new IllegalArgumentException("Unsupported restock upgrade " + testCase.upgrade());
			};
			return switch (testCase.mode()) {
				case "allow" -> new RestockSpec(upgradeItem, ContentsFilterType.ALLOW, Items.DIAMOND, upgradeItem.getFilterSlotCount(), true);
				case "block" -> new RestockSpec(upgradeItem, ContentsFilterType.BLOCK, Items.REDSTONE, upgradeItem.getFilterSlotCount(), true);
				case "empty" -> new RestockSpec(upgradeItem, ContentsFilterType.ALLOW, Items.DIAMOND, upgradeItem.getFilterSlotCount(), false);
				default -> throw new IllegalArgumentException("Unsupported restock mode " + testCase.mode());
			};
		}
	}
}
