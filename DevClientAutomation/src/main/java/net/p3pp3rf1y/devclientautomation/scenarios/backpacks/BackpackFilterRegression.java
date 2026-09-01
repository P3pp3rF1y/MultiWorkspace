package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class BackpackFilterRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);

	private BackpackFilterRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		FilterCase testCase = new FilterCase(string(request, "upgrade", "basic"), string(request, "operation", "input"));
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(player -> run(player, testCase)));
	}

	private static String run(ServerPlayer player, FilterCase testCase) {
		FilterSpec spec = FilterSpec.of(testCase);
		ItemStack backpack = BackpackRegressionFixture.create(ModItems.DIAMOND_BACKPACK.get(), 0x376FA9, 0xE6A33A).backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		InventoryHandler inventory = wrapper.getInventoryHandler();
		clearInventory(inventory);
		configure(wrapper, spec);

		ITrackedContentsItemResourceHandler ioInventory = wrapper.getInventoryForInputOutput();
		FilterResult operation = spec.operation() == Operation.INPUT ? testInput(ioInventory, inventory) : testOutput(ioInventory, inventory);
		boolean persisted = hasPersistedSettings(backpack, spec);
		boolean passed = operation.passed() && persisted;
		return "{\"ok\":" + passed + ",\"upgrade\":\"" + testCase.upgrade() + "\",\"operation\":\"" + testCase.operation() + "\",\"filterSlots\":"
				+ spec.filterSlots() + ",\"operationPassed\":" + operation.passed() + ",\"persisted\":" + persisted + ",\"allowedCount\":"
				+ operation.allowedCount() + ",\"blockedCount\":" + operation.blockedCount() + "}";
	}

	private static void clearInventory(InventoryHandler inventory) {
		for (int slot = 0; slot < inventory.size(); slot++) {
			inventory.setStackInSlot(slot, ItemStack.EMPTY);
		}
		inventory.saveInventory();
	}

	private static void configure(IBackpackWrapper wrapper, FilterSpec spec) {
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(1, new ItemStack(spec.upgradeItem()));
		FilterUpgradeWrapper filterUpgrade = filterUpgrade(wrapper);
		filterUpgrade.setDirection(spec.direction());
		filterUpgrade.getFilterLogic().setDepositFilterType(spec.filterType());
		filterUpgrade.getFilterLogic().getFilterHandler().setStackInSlot(spec.filterSlots() - 1, new ItemStack(spec.filterItem()));
		upgrades.saveInventory();
		wrapper.refreshInventoryForInputOutput();
	}

	private static FilterResult testInput(ITrackedContentsItemResourceHandler ioInventory, InventoryHandler inventory) {
		int allowed;
		int blocked;
		try (Transaction transaction = Transaction.openRoot()) {
			allowed = ioInventory.insert(ItemResource.of(new ItemStack(Items.DIAMOND)), 1, transaction);
			blocked = ioInventory.insert(ItemResource.of(new ItemStack(Items.REDSTONE)), 1, transaction);
			transaction.commit();
		}
		int allowedCount = count(inventory, Items.DIAMOND);
		int blockedCount = count(inventory, Items.REDSTONE);
		return new FilterResult(allowed == 1 && blocked == 0 && allowedCount == 1 && blockedCount == 0, allowedCount, blockedCount);
	}

	private static FilterResult testOutput(ITrackedContentsItemResourceHandler ioInventory, InventoryHandler inventory) {
		inventory.setStackInSlot(0, new ItemStack(Items.REDSTONE));
		inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND));
		inventory.saveInventory();
		int blockedExtract;
		int allowedExtract;
		try (Transaction transaction = Transaction.openRoot()) {
			blockedExtract = ioInventory.extract(ItemResource.of(new ItemStack(Items.REDSTONE)), 1, transaction);
			allowedExtract = ioInventory.extract(ItemResource.of(new ItemStack(Items.DIAMOND)), 1, transaction);
			transaction.commit();
		}
		int allowedCount = count(inventory, Items.DIAMOND);
		int blockedCount = count(inventory, Items.REDSTONE);
		return new FilterResult(blockedExtract == 0 && allowedExtract == 1 && allowedCount == 0 && blockedCount == 1, allowedCount, blockedCount);
	}

	private static boolean hasPersistedSettings(ItemStack backpack, FilterSpec spec) {
		IBackpackWrapper reloadedWrapper = BackpackWrapper.fromStack(backpack);
		FilterUpgradeWrapper filterUpgrade = filterUpgrade(reloadedWrapper);
		return filterUpgrade.getDirection() == spec.direction() && filterUpgrade.getFilterLogic().getFilterType() == spec.filterType()
				&& filterUpgrade.getFilterLogic().getFilterHandler().size() == spec.filterSlots()
				&& filterUpgrade.getFilterLogic().getFilterHandler().getStackInSlot(spec.filterSlots() - 1).is(spec.filterItem());
	}

	private static FilterUpgradeWrapper filterUpgrade(IBackpackWrapper wrapper) {
		List<FilterUpgradeWrapper> filterUpgrades = wrapper.getUpgradeHandler().getWrappersThatImplement(FilterUpgradeWrapper.class);
		if (filterUpgrades.size() != 1) {
			throw new IllegalStateException("Expected exactly one filter upgrade, found " + filterUpgrades.size());
		}
		return filterUpgrades.getFirst();
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

	private record FilterCase(String upgrade, String operation) {
	}

	private record FilterResult(boolean passed, int allowedCount, int blockedCount) {
	}

	private record FilterSpec(FilterUpgradeItem upgradeItem, Direction direction, ContentsFilterType filterType, Item filterItem, int filterSlots,
			Operation operation) {
		private static FilterSpec of(FilterCase testCase) {
			FilterUpgradeItem upgradeItem = switch (testCase.upgrade()) {
				case "basic" -> ModItems.FILTER_UPGRADE.get();
				case "advanced" -> ModItems.ADVANCED_FILTER_UPGRADE.get();
				default -> throw new IllegalArgumentException("Unsupported filter upgrade " + testCase.upgrade());
			};
			return switch (testCase.operation()) {
				case "input" ->
					new FilterSpec(upgradeItem, Direction.INPUT, ContentsFilterType.ALLOW, Items.DIAMOND, upgradeItem.getFilterSlotCount(), Operation.INPUT);
				case "output" ->
					new FilterSpec(upgradeItem, Direction.OUTPUT, ContentsFilterType.BLOCK, Items.REDSTONE, upgradeItem.getFilterSlotCount(), Operation.OUTPUT);
				default -> throw new IllegalArgumentException("Unsupported filter operation " + testCase.operation());
			};
		}
	}

	private enum Operation {
		INPUT, OUTPUT
	}
}
