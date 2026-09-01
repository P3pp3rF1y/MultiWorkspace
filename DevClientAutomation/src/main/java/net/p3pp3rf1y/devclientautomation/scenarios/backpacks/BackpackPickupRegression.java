package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class BackpackPickupRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);

	private BackpackPickupRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		PickupCase testCase = new PickupCase(string(request, "upgrade", "basic"), string(request, "mode", "allow"));
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(player -> run(player, testCase)));
	}

	private static String run(ServerPlayer player, PickupCase testCase) {
		PickupSpec spec = PickupSpec.of(testCase);
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		ItemEntity itemEntity = null;
		try {
			player.closeContainer();
			player.getInventory().clearContent();
			player.setGameMode(GameType.SURVIVAL);
			ItemStack backpack = configuredBackpack(spec);
			player.getInventory().setItem(0, backpack);
			player.getInventory().selected = 0;
			player.getInventory().setChanged();

			itemEntity = new ItemEntity(player.serverLevel(), player.getX(), player.getY() + 0.5D, player.getZ(), new ItemStack(spec.droppedItem()));
			itemEntity.setPickUpDelay(0);
			player.serverLevel().addFreshEntity(itemEntity);
			itemEntity.playerTouch(player);

			IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
			boolean persisted = hasSettings(wrapper, spec);
			int storedItems = count(wrapper.getInventoryHandler(), spec.droppedItem());
			int playerItems = countPlayerInventory(player, spec.droppedItem());
			boolean itemEntityPresent = itemEntity.isAlive() && !itemEntity.getItem().isEmpty();
			boolean inventoryFull = !spec.fullInventory()
					|| count(wrapper.getInventoryHandler(), Items.COBBLESTONE) == wrapper.getInventoryHandler().getSlots() * 64;
			boolean pickupMatches = spec.expectBackpackPickup()
					? !itemEntityPresent && storedItems == 1 && playerItems == 0
					: !itemEntityPresent && storedItems == 0 && playerItems == 1;
			boolean passed = persisted && inventoryFull && pickupMatches;
			return "{\"ok\":" + passed + ",\"settingsPersisted\":" + persisted + ",\"pickupMatches\":" + pickupMatches + ",\"itemEntityPresent\":"
					+ itemEntityPresent + ",\"storedItems\":" + storedItems + ",\"playerItems\":" + playerItems + ",\"inventoryFull\":" + inventoryFull + "}";
		} finally {
			if (itemEntity != null && itemEntity.isAlive()) {
				itemEntity.discard();
			}
			player.closeContainer();
			player.getInventory().clearContent();
			player.getInventory().setChanged();
			player.setGameMode(originalGameMode);
		}
	}

	private static ItemStack configuredBackpack(PickupSpec spec) {
		ItemStack backpack = BackpackRegressionFixture.create(ModItems.DIAMOND_BACKPACK.get(), 0x3D7CB8, 0xE4A23C).backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		InventoryHandler inventory = wrapper.getInventoryHandler();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			inventory.setStackInSlot(slot, spec.fullInventory() ? new ItemStack(Items.COBBLESTONE, 64) : ItemStack.EMPTY);
		}
		inventory.saveInventory();

		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(1, new ItemStack(spec.upgradeItem()));
		PickupUpgradeWrapper pickupUpgrade = pickupUpgrade(wrapper);
		pickupUpgrade.getFilterLogic().setDepositFilterType(spec.filterType());
		pickupUpgrade.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(spec.filterItem()));
		upgrades.saveInventory();
		return backpack;
	}

	private static boolean hasSettings(IBackpackWrapper wrapper, PickupSpec spec) {
		PickupUpgradeWrapper pickupUpgrade = pickupUpgrade(wrapper);
		return pickupUpgrade.getFilterLogic().getFilterType() == spec.filterType()
				&& pickupUpgrade.getFilterLogic().getFilterHandler().getStackInSlot(0).is(spec.filterItem());
	}

	private static PickupUpgradeWrapper pickupUpgrade(IBackpackWrapper wrapper) {
		List<PickupUpgradeWrapper> pickupUpgrades = wrapper.getUpgradeHandler().getWrappersThatImplement(PickupUpgradeWrapper.class);
		if (pickupUpgrades.size() != 1) {
			throw new IllegalStateException("Expected exactly one pickup upgrade, found " + pickupUpgrades.size());
		}
		return pickupUpgrades.getFirst();
	}

	private static int count(InventoryHandler inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int countPlayerInventory(ServerPlayer player, Item item) {
		int count = 0;
		for (ItemStack stack : player.getInventory().items) {
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private record PickupCase(String upgrade, String mode) {
	}

	private record PickupSpec(PickupUpgradeItem upgradeItem, ContentsFilterType filterType, Item filterItem, Item droppedItem, boolean fullInventory,
			boolean expectBackpackPickup) {
		private static PickupSpec of(PickupCase testCase) {
			PickupUpgradeItem upgradeItem = switch (testCase.upgrade()) {
				case "basic" -> ModItems.PICKUP_UPGRADE.get();
				case "advanced" -> ModItems.ADVANCED_PICKUP_UPGRADE.get();
				default -> throw new IllegalArgumentException("Unsupported pickup upgrade " + testCase.upgrade());
			};
			return switch (testCase.mode()) {
				case "allow" -> new PickupSpec(upgradeItem, ContentsFilterType.ALLOW, Items.DIAMOND, Items.DIAMOND, false, true);
				case "block" -> new PickupSpec(upgradeItem, ContentsFilterType.BLOCK, Items.REDSTONE, Items.REDSTONE, false, false);
				case "full" -> new PickupSpec(upgradeItem, ContentsFilterType.ALLOW, Items.DIAMOND, Items.DIAMOND, true, false);
				default -> throw new IllegalArgumentException("Unsupported pickup mode " + testCase.mode());
			};
		}
	}
}
