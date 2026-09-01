package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class BackpackRefillRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);
	private static final int BACKPACK_SLOT = 8;

	private BackpackRefillRegression() {
	}

	public static void handleSetup(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		RefillCase testCase = parse(readObject(exchange));
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(player -> setup(player, testCase)));
	}

	public static void handleStatus(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		RefillCase testCase = parse(readObject(exchange));
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(player -> status(player, testCase)));
	}

	private static RefillCase parse(JsonObject request) {
		return new RefillCase(string(request, "upgrade", "basic"), string(request, "mode", "allow"));
	}

	private static String setup(ServerPlayer player, RefillCase testCase) {
		RefillSpec spec = RefillSpec.of(testCase);
		cleanup(player);
		ItemStack backpack = configuredBackpack(spec);
		player.getInventory().setItem(BACKPACK_SLOT, backpack);
		player.getInventory().setItem(0, new ItemStack(spec.targetItem(), spec.targetCount()));
		player.getInventory().selected = 0;
		player.getInventory().setChanged();

		boolean settingsPersisted = hasPersistedSettings(backpack, spec);
		JsonObject result = new JsonObject();
		result.addProperty("ok", settingsPersisted);
		result.addProperty("settingsPersisted", settingsPersisted);
		result.addProperty("upgrade", testCase.upgrade());
		result.addProperty("mode", testCase.mode());
		return result.toString();
	}

	private static String status(ServerPlayer player, RefillCase testCase) {
		RefillSpec spec = RefillSpec.of(testCase);
		try {
			ItemStack backpack = player.getInventory().getItem(BACKPACK_SLOT);
			if (!backpack.is(ModItems.DIAMOND_BACKPACK.get())) {
				return error("Configured refill backpack is missing");
			}
			int playerTargetCount = count(player.getInventory().getItem(0), spec.targetItem());
			int playerRefillCount = count(player, spec.refillItem());
			int backpackTargetCount = count(BackpackWrapper.fromStack(backpack).getInventoryHandler(), spec.refillItem());
			boolean settingsPersisted = hasPersistedSettings(backpack, spec);
			boolean refillMatches = playerTargetCount == spec.expectedTargetCount() && playerRefillCount == spec.expectedPlayerRefillCount()
					&& backpackTargetCount == spec.expectedBackpackCount();
			JsonObject result = new JsonObject();
			result.addProperty("ok", settingsPersisted && refillMatches);
			result.addProperty("settingsPersisted", settingsPersisted);
			result.addProperty("refillMatches", refillMatches);
			result.addProperty("playerTargetCount", playerTargetCount);
			result.addProperty("playerRefillCount", playerRefillCount);
			result.addProperty("backpackTargetCount", backpackTargetCount);
			return result.toString();
		} finally {
			cleanup(player);
		}
	}

	private static ItemStack configuredBackpack(RefillSpec spec) {
		ItemStack backpack = BackpackRegressionFixture.create(ModItems.DIAMOND_BACKPACK.get(), 0x276FA7, 0xE7A232).backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		InventoryHandler inventory = wrapper.getInventoryHandler();
		clearInventory(inventory);
		if (spec.backpackCount() > 0) {
			inventory.setStackInSlot(0, new ItemStack(spec.refillItem(), spec.backpackCount()));
			inventory.saveInventory();
		}

		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(1, new ItemStack(spec.upgradeItem()));
		RefillUpgradeWrapper refill = refillUpgrade(wrapper);
		int filterSlot = spec.upgradeItem().getFilterSlotCount() - 1;
		refill.getFilterLogic().getFilterHandler().setStackInSlot(filterSlot, new ItemStack(spec.refillItem()));
		if (spec.targetSlot() != RefillUpgradeWrapper.TargetSlot.ANY) {
			refill.setTargetSlot(filterSlot, spec.targetSlot());
		}
		upgrades.saveInventory();
		return backpack;
	}

	private static boolean hasPersistedSettings(ItemStack backpack, RefillSpec spec) {
		RefillUpgradeWrapper refill = refillUpgrade(BackpackWrapper.fromStack(backpack));
		int filterSlot = spec.upgradeItem().getFilterSlotCount() - 1;
		return refill.getFilterLogic().getFilterHandler().getSlots() == spec.upgradeItem().getFilterSlotCount()
				&& refill.getFilterLogic().getFilterHandler().getStackInSlot(filterSlot).is(spec.refillItem())
				&& refill.getTargetSlots().getOrDefault(filterSlot, RefillUpgradeWrapper.TargetSlot.ANY) == spec.targetSlot();
	}

	private static RefillUpgradeWrapper refillUpgrade(IBackpackWrapper wrapper) {
		List<RefillUpgradeWrapper> refillUpgrades = wrapper.getUpgradeHandler().getWrappersThatImplement(RefillUpgradeWrapper.class);
		if (refillUpgrades.size() != 1) {
			throw new IllegalStateException("Expected exactly one refill upgrade, found " + refillUpgrades.size());
		}
		return refillUpgrades.getFirst();
	}

	private static void clearInventory(InventoryHandler inventory) {
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			inventory.setStackInSlot(slot, ItemStack.EMPTY);
		}
		inventory.saveInventory();
	}

	private static int count(ItemStack stack, Item item) {
		return stack.is(item) ? stack.getCount() : 0;
	}

	private static int count(InventoryHandler inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			count += count(inventory.getStackInSlot(slot), item);
		}
		return count;
	}

	private static int count(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			count += count(player.getInventory().getItem(slot), item);
		}
		return count;
	}

	private static void cleanup(ServerPlayer player) {
		player.closeContainer();
		player.getInventory().clearContent();
		player.getInventory().setChanged();
	}

	private static String error(String message) {
		JsonObject result = new JsonObject();
		result.addProperty("ok", false);
		result.addProperty("error", message);
		return result.toString();
	}

	private record RefillCase(String upgrade, String mode) {
	}

	private record RefillSpec(RefillUpgradeItem upgradeItem, Item refillItem, Item targetItem, int backpackCount, int targetCount,
			RefillUpgradeWrapper.TargetSlot targetSlot, int expectedTargetCount, int expectedPlayerRefillCount, int expectedBackpackCount) {
		private static RefillSpec of(RefillCase testCase) {
			RefillUpgradeItem upgradeItem = switch (testCase.upgrade()) {
				case "basic" -> ModItems.REFILL_UPGRADE.get();
				case "advanced" -> ModItems.ADVANCED_REFILL_UPGRADE.get();
				default -> throw new IllegalArgumentException("Unsupported refill upgrade " + testCase.upgrade());
			};
			return switch (testCase.mode()) {
				case "allow" -> new RefillSpec(upgradeItem, Items.DIAMOND, Items.DIAMOND, 32, 5, RefillUpgradeWrapper.TargetSlot.ANY, 37, 37, 0);
				case "empty" -> new RefillSpec(upgradeItem, Items.DIAMOND, Items.DIAMOND, 0, 5, RefillUpgradeWrapper.TargetSlot.ANY, 5, 5, 0);
				case "mismatch" -> new RefillSpec(upgradeItem, Items.DIAMOND, Items.REDSTONE, 32, 5, RefillUpgradeWrapper.TargetSlot.ANY, 5, 32, 0);
				case "main_hand" -> new RefillSpec(upgradeItem, Items.DIAMOND, Items.DIAMOND, 32, 5, RefillUpgradeWrapper.TargetSlot.MAIN_HAND, 37, 37, 0);
				case "main_hand_mismatch" ->
					new RefillSpec(upgradeItem, Items.DIAMOND, Items.REDSTONE, 32, 5, RefillUpgradeWrapper.TargetSlot.MAIN_HAND, 5, 0, 32);
				default -> throw new IllegalArgumentException("Unsupported refill mode " + testCase.mode());
			};
		}
	}
}
