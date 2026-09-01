package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class BackpackMagnetRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);

	private BackpackMagnetRegression() {
	}

	public static void handleSetup(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		MagnetCase testCase = parse(readObject(exchange));
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(player -> setup(player, testCase)));
	}

	public static void handleStatus(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		MagnetCase testCase = parse(readObject(exchange));
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(player -> status(player, testCase)));
	}

	private static MagnetCase parse(JsonObject request) {
		String upgrade = string(request, "upgrade", "basic");
		String mode = string(request, "mode", "allow");
		return new MagnetCase(upgrade, mode);
	}

	private static String setup(ServerPlayer player, MagnetCase testCase) {
		MagnetSpec spec = MagnetSpec.of(testCase);
		cleanup(player, spec.droppedItem());
		ItemStack backpack = configuredBackpack(spec);
		player.getInventory().setItem(0, backpack);
		player.getInventory().setSelectedSlot(0);
		player.getInventory().setChanged();

		ItemEntity itemEntity = new ItemEntity((ServerLevel) player.level(), player.getX() + 2.5D, player.getY() + 2.0D, player.getZ(),
				new ItemStack(spec.droppedItem()));
		itemEntity.setPickUpDelay(0);
		itemEntity.setTarget(UUID.randomUUID());
		((ServerLevel) player.level()).addFreshEntity(itemEntity);

		MagnetUpgradeWrapper magnet = magnet(BackpackWrapper.fromStack(backpack));
		boolean persisted = magnet.shouldPickupItems() == spec.pickupItems() && magnet.getFilterLogic().getFilterType() == spec.filterType()
				&& magnet.getFilterLogic().getFilterHandler().getStackInSlot(0).is(spec.filterItem());
		return "{\"ok\":" + persisted + ",\"settingsPersisted\":" + persisted + ",\"upgrade\":\"" + testCase.upgrade() + "\",\"mode\":\"" + testCase.mode()
				+ "\"}";
	}

	private static String status(ServerPlayer player, MagnetCase testCase) {
		MagnetSpec spec = MagnetSpec.of(testCase);
		try {
			ItemStack backpack = player.getInventory().getItem(0);
			if (!backpack.is(ModItems.DIAMOND_BACKPACK.get())) {
				return "{\"ok\":false,\"error\":\"Configured magnet backpack is missing\"}";
			}
			MagnetUpgradeWrapper magnet = magnet(BackpackWrapper.fromStack(backpack));
			boolean persisted = magnet.shouldPickupItems() == spec.pickupItems() && magnet.getFilterLogic().getFilterType() == spec.filterType()
					&& magnet.getFilterLogic().getFilterHandler().getStackInSlot(0).is(spec.filterItem());
			boolean itemEntityPresent = ((ServerLevel) player.level()).getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(4),
					entity -> entity.isAlive() && entity.getItem().is(spec.droppedItem())).stream().anyMatch(entity -> !entity.getItem().isEmpty());
			int storedItems = count(BackpackWrapper.fromStack(backpack).getInventoryHandler(), spec.droppedItem());
			boolean pickupMatches = spec.expectPickup() ? !itemEntityPresent && storedItems > 0 : itemEntityPresent && storedItems == 0;
			boolean passed = persisted && pickupMatches;
			return "{\"ok\":" + passed + ",\"settingsPersisted\":" + persisted + ",\"pickupMatches\":" + pickupMatches + ",\"itemEntityPresent\":"
					+ itemEntityPresent + ",\"storedItems\":" + storedItems + "}";
		} finally {
			cleanup(player, spec.droppedItem());
		}
	}

	private static ItemStack configuredBackpack(MagnetSpec spec) {
		ItemStack backpack = BackpackRegressionFixture.create(ModItems.DIAMOND_BACKPACK.get(), 0x2978B9, 0xE6A63A).backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.getInventoryHandler().setStackInSlot(0, ItemStack.EMPTY);
		wrapper.getInventoryHandler().saveInventory();
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(1, new ItemStack(spec.upgradeItem()));
		MagnetUpgradeWrapper magnet = magnet(wrapper);
		magnet.setPickupItems(spec.pickupItems());
		magnet.getFilterLogic().setDepositFilterType(spec.filterType());
		magnet.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(spec.filterItem()));
		upgrades.saveInventory();
		return backpack;
	}

	private static MagnetUpgradeWrapper magnet(IBackpackWrapper wrapper) {
		List<MagnetUpgradeWrapper> magnets = wrapper.getUpgradeHandler().getWrappersThatImplement(MagnetUpgradeWrapper.class);
		if (magnets.size() != 1) {
			throw new IllegalStateException("Expected exactly one magnet upgrade, found " + magnets.size());
		}
		return magnets.getFirst();
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

	private static void cleanup(ServerPlayer player, Item droppedItem) {
		player.closeContainer();
		player.getInventory().clearContent();
		player.getInventory().setChanged();
		((ServerLevel) player.level())
				.getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(4), entity -> entity.getItem().is(droppedItem))
				.forEach(ItemEntity::discard);
	}

	private record MagnetCase(String upgrade, String mode) {
	}

	private record MagnetSpec(Item upgradeItem, ContentsFilterType filterType, Item filterItem, boolean pickupItems, Item droppedItem, boolean expectPickup) {
		private static MagnetSpec of(MagnetCase testCase) {
			Item upgradeItem = switch (testCase.upgrade()) {
				case "basic" -> ModItems.MAGNET_UPGRADE.get();
				case "advanced" -> ModItems.ADVANCED_MAGNET_UPGRADE.get();
				default -> throw new IllegalArgumentException("Unsupported magnet upgrade " + testCase.upgrade());
			};
			return switch (testCase.mode()) {
				case "allow" -> new MagnetSpec(upgradeItem, ContentsFilterType.ALLOW, Items.DIAMOND, true, Items.DIAMOND, true);
				case "block" -> new MagnetSpec(upgradeItem, ContentsFilterType.BLOCK, Items.REDSTONE, true, Items.REDSTONE, false);
				case "disabled" -> new MagnetSpec(upgradeItem, ContentsFilterType.ALLOW, Items.DIAMOND, false, Items.DIAMOND, false);
				default -> throw new IllegalArgumentException("Unsupported magnet mode " + testCase.mode());
			};
		}
	}
}
