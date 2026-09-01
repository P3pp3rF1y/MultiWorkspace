package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.LinkedStorageBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.inception.InventoryOrder;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.StorageWrapperRepository;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

public final class BackpackOperations {
	private static final String AUTOMATION_WORLD_NAME = "Dev Client Automation Void Platform";
	private static final int LINKED_STORAGE_RELOAD_CANONICAL_ITEM_COUNT = 7;

	private BackpackOperations() {
	}

	public static String setupBackpacks(boolean mainMagnet, int redstoneCount) {
		return AutomationRuntime.runOnServer(player -> setupBackpacks(player, mainMagnet, redstoneCount));
	}

	public static String runIssue1528Test(int autosaveCount, long autosaveTimeoutMs, boolean stressMagnetPickups, int stressStacks, int stressCount,
			double stressRadius) {
		Issue1528SetupResult setup = AutomationRuntime.runOnServer(BackpackOperations::setupIssue1528Backpacks);
		String openMainResult = AutomationRuntime.runOnServer(BackpackOperations::openMainBackpack);
		String openNestedResult = AutomationRuntime.runOnServer(player -> openNestedBackpack(player, 0));
		String stressResult = stressMagnetPickups
				? AutomationRuntime.runOnServer(player -> stressBackpacks(player, stressStacks, stressCount, stressRadius))
				: null;
		StringBuilder autosaveResult = new StringBuilder();
		boolean autosavesComplete = appendAutosaveResult(autosaveResult, AUTOMATION_WORLD_NAME, autosaveCount, autosaveTimeoutMs, 1_000L);
		AutomationRuntime.runOnServer(BackpackOperations::useFireworkFromIssue1528Hotbar);
		boolean refilled = waitForPlayerSlotItem(8, Items.FIREWORK_ROCKET, 5_000L);
		Issue1528ActionResult actionResult = AutomationRuntime.runOnServer(player -> removeNestedDiamondAfterIssue1528Firework(player, refilled));

		return "{\"ok\":" + (autosavesComplete && actionResult.refilled()) + "," + jsonProperty("mainUuid", setup.mainUuid()) + ","
				+ jsonProperty("nestedUuid", setup.nestedUuid()) + "," + autosaveResult + "," + jsonProperty("openMainResult", openMainResult) + ","
				+ jsonProperty("openNestedResult", openNestedResult) + ",\"stressMagnetPickups\":" + stressMagnetPickups + ","
				+ jsonProperty("stressResult", stressResult) + ",\"fireworkRefilled\":" + actionResult.refilled() + ",\"hotbarFireworks\":"
				+ actionResult.hotbarFireworks() + ",\"nestedFireworks\":" + actionResult.nestedFireworks() + ",\"nestedDiamonds\":"
				+ actionResult.nestedDiamonds() + ",\"removedDiamonds\":" + actionResult.removedDiamonds() + "}";
	}

	public static String setupIssue1528BackpacksForInspection() {
		return AutomationRuntime.runOnServer(BackpackOperations::setupIssue1528BackpacksForInspection);
	}

	public static String issue1528Status() {
		return AutomationRuntime.runOnServer(BackpackOperations::issue1528Status);
	}

	public static String useIssue1528Firework() {
		return AutomationRuntime.runOnServer(BackpackOperations::useIssue1528Firework);
	}

	public static String tickIssue1528Refill() {
		return AutomationRuntime.runOnServer(BackpackOperations::tickIssue1528Refill);
	}

	public static String stressBackpacks(int stacks, int count, double radius) {
		return AutomationRuntime.runOnServer(player -> stressBackpacks(player, stacks, count, radius));
	}

	public static String backpackStatus() {
		return AutomationRuntime.runOnServer(BackpackOperations::backpackStatus);
	}

	public static String openMainBackpack() {
		return AutomationRuntime.runOnServer(BackpackOperations::openMainBackpack);
	}

	public static String openNestedBackpack(int nestedSlot) {
		return AutomationRuntime.runOnServer(player -> openNestedBackpack(player, nestedSlot));
	}

	public static String emptyNestedBackpacks() {
		return AutomationRuntime.runOnServer(BackpackOperations::emptyNestedBackpacks);
	}

	public static String clearBackpackCache() {
		StorageWrapperRepository.clearCache();
		return "{\"ok\":true}";
	}

	public static String setupInceptionMagnetPersistence() {
		return AutomationRuntime.runOnServer(BackpackOperations::setupInceptionMagnetPersistence);
	}

	public static String pickupWithInceptionMagnet() {
		return AutomationRuntime.runOnServer(BackpackOperations::pickupWithInceptionMagnet);
	}

	public static String inceptionMagnetPersistenceStatus() {
		return AutomationRuntime.runOnServer(BackpackOperations::inceptionMagnetPersistenceStatus);
	}

	public static String setupLinkedStorageReload() {
		return AutomationRuntime.runOnServer(BackpackOperations::setupLinkedStorageReload);
	}

	public static String linkedStorageReloadStatus() {
		return AutomationRuntime.runOnServer(BackpackOperations::linkedStorageReloadStatus);
	}

	public static String changeMagnetSettings(String target, ContentsFilterType filterType) {
		return AutomationRuntime.runOnServer(player -> changeMagnetSettings(player, target, filterType));
	}

	public static String moveBackpacks(String target, boolean clearCache) {
		return AutomationRuntime.runOnServer(player -> moveBackpacks(player, target, clearCache));
	}

	public static String spreadNestedBackpacks() {
		return AutomationRuntime.runOnServer(BackpackOperations::spreadNestedBackpacks);
	}

	public static String fillMainBackpackNoise() {
		return AutomationRuntime.runOnServer(BackpackOperations::fillMainBackpackNoise);
	}

	public static String changeMagnetPickup(String target, boolean pickupItems) {
		return AutomationRuntime.runOnServer(player -> changeMagnetPickup(player, target, pickupItems));
	}

	public static String seedNestedBackpack(int nestedSlot, int count) {
		return AutomationRuntime.runOnServer(player -> seedNestedBackpack(player, nestedSlot, Items.REDSTONE, count));
	}

	public static String bulkDropFromNestedBackpack(int nestedSlot, int maxStacks, int pickupDelay, boolean clearCache) {
		return AutomationRuntime.runOnServer(player -> bulkDropFromNestedBackpack(player, nestedSlot, Items.REDSTONE, maxStacks, pickupDelay, clearCache));
	}

	public static String droppedItemsStatus() {
		return AutomationRuntime.runOnServer(player -> droppedItemsStatus(player, Items.REDSTONE));
	}

	public static String clearDroppedItems() {
		return AutomationRuntime.runOnServer(player -> clearDroppedItems(player, Items.REDSTONE));
	}

	public static String giveLinkedStorageStarterKit() {
		return AutomationRuntime.runOnServer(BackpackOperations::giveLinkedStorageStarterKit);
	}

	private static String giveLinkedStorageStarterKit(ServerPlayer player) {
		List<ItemStack> stacks = new ArrayList<>();
		stacks.add(new ItemStack(ModItems.GOLD_BACKPACK.get()));
		stacks.add(new ItemStack(ModItems.DIAMOND_BACKPACK.get()));
		stacks.add(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
		stacks.add(createTintedBackpack(0xFF_D32F2F, 0xFF_7F0000));
		stacks.add(createTintedBackpack(0xFF_388E3C, 0xFF_1B5E20));
		stacks.add(createTintedBackpack(0xFF_1976D2, 0xFF_0D47A1));
		for (int i = 0; i < 3; i++) {
			stacks.add(new ItemStack(ENDER_LINKER.get()));
		}
		stacks.add(new ItemStack(ModItems.TANK_UPGRADE.get(), 64));
		stacks.add(new ItemStack(Items.CRAFTING_TABLE));
		stacks.forEach(stack -> player.getInventory().add(stack));
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		return "{\"ok\":true,\"items\":" + stacks.size() + "}";
	}

	private static ItemStack createTintedBackpack(int mainColor, int accentColor) {
		ItemStack backpack = new ItemStack(ModItems.BACKPACK.get());
		BackpackItem.setColors(backpack, mainColor, accentColor);
		return backpack;
	}

	private static String setupBackpacks(ServerPlayer player, boolean mainMagnet, int redstoneCount) {
		player.getInventory().clearContent();
		ItemStack mainBackpack = createBackpackStack();
		ItemStack firstNestedBackpack = createNestedBackpack(Items.COBBLESTONE, Items.IRON_INGOT);
		ItemStack secondNestedBackpack = createNestedBackpack(Items.DIRT, Items.GOLD_INGOT);

		IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
		mainWrapper.setSlotNumbers(80, 5);
		UpgradeHandler mainUpgrades = mainWrapper.getUpgradeHandler();
		mainUpgrades.setStackInSlot(0, new ItemStack(ModItems.INCEPTION_UPGRADE.get()));
		if (mainMagnet) {
			mainUpgrades.setStackInSlot(1, new ItemStack(ModItems.ADVANCED_MAGNET_UPGRADE.get()));
			setMagnetFilterType(mainWrapper, ContentsFilterType.ALLOW);
		}
		mainUpgrades.saveInventory();

		InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
		mainInventory.setStackInSlot(0, firstNestedBackpack);
		mainInventory.setStackInSlot(1, secondNestedBackpack);
		mainInventory.saveInventory();

		if (redstoneCount > 0) {
			seedNestedBackpack(mainWrapper, 0, Items.REDSTONE, redstoneCount);
		}

		player.getInventory().setItem(0, mainBackpack);
		player.getInventory().setChanged();

		return "{\"ok\":true," + jsonProperty("mainUuid", mainWrapper.getContentsUuid().map(Object::toString).orElse(null)) + ","
				+ jsonProperty("nested0Uuid", BackpackWrapper.fromStack(firstNestedBackpack).getContentsUuid().map(Object::toString).orElse(null)) + ","
				+ jsonProperty("nested1Uuid", BackpackWrapper.fromStack(secondNestedBackpack).getContentsUuid().map(Object::toString).orElse(null)) + ","
				+ "\"mainMagnet\":" + mainMagnet + ",\"redstoneCount\":" + redstoneCount + "}";
	}

	private static String setupInceptionMagnetPersistence(ServerPlayer player) {
		player.closeContainer();
		player.getInventory().clearContent();

		ItemStack mainBackpack = createBackpackStack();
		IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
		ItemStack inceptionUpgrade = new ItemStack(ModItems.INCEPTION_UPGRADE.get());
		inceptionUpgrade.set(ModDataComponents.INVENTORY_ORDER, InventoryOrder.INCEPTED_FIRST);
		UpgradeHandler upgrades = mainWrapper.getUpgradeHandler();
		upgrades.setStackInSlot(0, inceptionUpgrade);
		upgrades.setStackInSlot(1, new ItemStack(ModItems.MAGNET_UPGRADE.get()));
		setMagnetFilterType(mainWrapper, ContentsFilterType.BLOCK);
		setMagnetPickupAndCount(mainWrapper, true);
		upgrades.saveInventory();

		ItemStack nestedBackpack = new ItemStack(ModItems.BACKPACK.get());
		mainWrapper.getInventoryHandler().setStackInSlot(0, nestedBackpack);
		mainWrapper.getInventoryHandler().saveInventory();
		player.getInventory().setItem(0, mainBackpack);
		player.getInventory().setSelectedSlot(0);
		player.getInventory().setChanged();

		String openResult = openMainBackpack(player);
		return "{\"ok\":true,\"nestedHasUuid\":" + (nestedBackpack.get(ModCoreDataComponents.STORAGE_UUID) != null) + ","
				+ jsonProperty("openResult", openResult) + "}";
	}

	private static String pickupWithInceptionMagnet(ServerPlayer player) {
		ItemEntity itemEntity = new ItemEntity((ServerLevel) player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), new ItemStack(Items.DIAMOND));
		itemEntity.setPickUpDelay(0);
		((ServerLevel) player.level()).addFreshEntity(itemEntity);
		itemEntity.playerTouch(player);
		player.closeContainer();

		ItemStack nestedBackpack = BackpackWrapper.fromStack(player.getInventory().getItem(0)).getInventoryHandler().getStackInSlot(0);
		UUID nestedUuid = nestedBackpack.get(ModCoreDataComponents.STORAGE_UUID);
		IBackpackWrapper nestedWrapper = nestedUuid == null ? null : BackpackWrapper.fromStack(nestedBackpack);
		UUID wrapperUuid = nestedWrapper == null ? null : nestedWrapper.getContentsUuid().orElse(null);
		int nestedDiamonds = nestedWrapper == null ? 0 : countItems(nestedWrapper.getInventoryHandler(), Items.DIAMOND);
		return "{\"ok\":" + (nestedUuid != null && nestedDiamonds == 1) + ",\"nestedHasUuid\":" + (nestedUuid != null) + ",\"nestedDiamonds\":" + nestedDiamonds
				+ "," + jsonProperty("nestedUuid", nestedUuid == null ? null : nestedUuid.toString()) + ","
				+ jsonProperty("wrapperUuid", wrapperUuid == null ? null : wrapperUuid.toString()) + ","
				+ jsonProperty("threadGroup", Thread.currentThread().getThreadGroup().getName()) + "}";
	}

	private static String inceptionMagnetPersistenceStatus(ServerPlayer player) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem)) {
			return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
		}

		ItemStack nestedBackpack = BackpackWrapper.fromStack(mainBackpack).getInventoryHandler().getStackInSlot(0);
		if (!(nestedBackpack.getItem() instanceof BackpackItem)) {
			return "{\"ok\":false,\"error\":\"No nested backpack in slot 0\"}";
		}

		UUID nestedUuid = nestedBackpack.get(ModCoreDataComponents.STORAGE_UUID);
		if (nestedUuid == null) {
			return "{\"ok\":false,\"nestedHasUuid\":false,\"nestedDiamonds\":0,\"error\":\"Nested backpack UUID was not persisted\"}";
		}

		int nestedDiamonds = countItems(BackpackWrapper.fromStack(nestedBackpack).getInventoryHandler(), Items.DIAMOND);
		return "{\"ok\":" + (nestedDiamonds == 1) + ",\"nestedHasUuid\":true,\"nestedDiamonds\":" + nestedDiamonds + ","
				+ jsonProperty("nestedUuid", nestedUuid.toString()) + "}";
	}

	private static String setupLinkedStorageReload(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos placedPos = linkedStorageReloadPos(level);
		player.closeContainer();
		player.getInventory().clearContent();
		level.setBlock(placedPos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(placedPos.below(), Blocks.DIRT.defaultBlockState(), 3);

		ItemStack carried = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		IBackpackWrapper carriedWrapper = BackpackWrapper.fromStack(carried);
		carriedWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.NETHER_STAR, LINKED_STORAGE_RELOAD_CANONICAL_ITEM_COUNT));
		carriedWrapper.getInventoryHandler().saveInventory();
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		if (!LinkedStorageService.link(level, linker, carried)) {
			throw new IllegalStateException("Could not create the carried linked storage endpoint");
		}

		ItemStack placed = new ItemStack(ModItems.GOLD_BACKPACK.get());
		if (!LinkedStorageService.link(level, linker, placed)) {
			throw new IllegalStateException("Could not create the placed linked storage endpoint");
		}
		level.setBlock(placedPos, ModBlocks.BACKPACK.get().defaultBlockState(), 3);
		BackpackBlockEntity placedBackpack = level.getBlockEntity(placedPos, ModBlocks.BACKPACK_TILE_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Could not place the linked storage Backpack block entity"));
		placedBackpack.setBackpack(placed);
		placedBackpack.setChanged();

		player.getInventory().setItem(0, carried);
		player.getInventory().setSelectedSlot(0);
		player.getInventory().setChanged();
		return linkedStorageReloadStatus(player);
	}

	private static String linkedStorageReloadStatus(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		ItemStack carried = player.getInventory().getItem(0);
		if (!(carried.getItem() instanceof BackpackItem)) {
			return "{\"ok\":false,\"error\":\"No carried Backpack endpoint in player inventory slot 0\"}";
		}

		BackpackBlockEntity placedBackpack = level.getBlockEntity(linkedStorageReloadPos(level), ModBlocks.BACKPACK_TILE_TYPE.get()).orElse(null);
		if (placedBackpack == null) {
			return "{\"ok\":false,\"error\":\"No placed BackpackBlockEntity endpoint\"}";
		}
		ItemStack placed = placedBackpack.getBackpackWrapper().getBackpack();
		LinkedStorageEndpointData carriedEndpoint = carried.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		LinkedStorageEndpointData placedEndpoint = placed.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (carriedEndpoint == null) {
			return "{\"ok\":false,\"error\":\"Carried Backpack endpoint component was not persisted\"}";
		}
		if (placedEndpoint == null) {
			return "{\"ok\":false,\"error\":\"Placed Backpack endpoint component was not persisted\"}";
		}

		IBackpackWrapper carriedCanonical = BackpackLinkedStorageResolver.resolve(level, carried)
				.orElseThrow(() -> new IllegalStateException("Carried Backpack endpoint did not resolve through linked storage"));
		IBackpackWrapper placedCanonical = BackpackLinkedStorageResolver.resolve(level, placed)
				.orElseThrow(() -> new IllegalStateException("Placed Backpack endpoint did not resolve through linked storage"));
		try {
			boolean sharedGroup = carriedEndpoint.groupId().equals(placedEndpoint.groupId())
					&& !carriedEndpoint.endpointId().equals(placedEndpoint.endpointId());
			int carriedNetherStars = countItems(carriedCanonical.getInventoryHandler(), Items.NETHER_STAR);
			int placedNetherStars = countItems(placedCanonical.getInventoryHandler(), Items.NETHER_STAR);
			boolean canonicalContents = carriedNetherStars == LINKED_STORAGE_RELOAD_CANONICAL_ITEM_COUNT
					&& placedNetherStars == LINKED_STORAGE_RELOAD_CANONICAL_ITEM_COUNT;
			boolean sameCanonicalHandler = carriedCanonical.getInventoryHandler() == placedCanonical.getInventoryHandler();
			boolean passed = sharedGroup && canonicalContents && sameCanonicalHandler;
			return "{\"ok\":" + passed + ",\"carriedEndpoint\":true,\"placedEndpoint\":true,\"sharedGroup\":" + sharedGroup + ",\"sameCanonicalHandler\":"
					+ sameCanonicalHandler + ",\"carriedNetherStars\":" + carriedNetherStars + ",\"placedNetherStars\":" + placedNetherStars + ","
					+ jsonProperty("groupId", carriedEndpoint.groupId().toString()) + ","
					+ jsonProperty("carriedEndpointId", carriedEndpoint.endpointId().toString()) + ","
					+ jsonProperty("placedEndpointId", placedEndpoint.endpointId().toString()) + "}";
		} finally {
			closeLinkedStorageFacade(carriedCanonical);
			closeLinkedStorageFacade(placedCanonical);
		}
	}

	private static String issue1528Status(ServerPlayer player) {
		IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
		ItemStack nestedStack = mainWrapper.getInventoryHandler().getStackInSlot(0);
		if (!(nestedStack.getItem() instanceof BackpackItem)) {
			return "{\"ok\":false,\"error\":\"No issue 1528 nested backpack in main slot 0\"}";
		}

		IBackpackWrapper nestedWrapper = BackpackWrapper.fromStack(nestedStack);
		InventoryHandler nestedInventory = nestedWrapper.getInventoryHandler();
		return "{\"ok\":true," + jsonProperty("mainUuid", mainWrapper.getContentsUuid().map(Object::toString).orElse(null)) + ","
				+ jsonProperty("nestedUuid", nestedWrapper.getContentsUuid().map(Object::toString).orElse(null)) + ",\"hotbarFireworks\":"
				+ countItemsInPlayerSlot(player, 8, Items.FIREWORK_ROCKET) + ",\"nestedFireworks\":" + countItems(nestedInventory, Items.FIREWORK_ROCKET)
				+ ",\"nestedDiamonds\":" + countItems(nestedInventory, Items.DIAMOND) + ",\"accessibleRefillUpgrades\":"
				+ mainWrapper.getUpgradeHandler().getWrappersThatImplement(RefillUpgradeWrapper.class).size() + ",\"nestedDirectRefillUpgrades\":"
				+ nestedWrapper.getUpgradeHandler().getWrappersThatImplement(RefillUpgradeWrapper.class).size() + ",\"playerTickCount\":" + player.tickCount
				+ ",\"levelGameTime\":" + player.level().getGameTime() + ",\"selectedSlot\":" + player.getInventory().getSelectedSlot() + "}";
	}

	private static String setupIssue1528BackpacksForInspection(ServerPlayer player) {
		Issue1528SetupResult setup = setupIssue1528Backpacks(player);
		return issue1528Status(player).replace("{\"ok\":true,",
				"{\"ok\":true,\"setup\":true," + jsonProperty("mainUuid", setup.mainUuid()) + "," + jsonProperty("nestedUuid", setup.nestedUuid()) + ",");
	}

	private static String useIssue1528Firework(ServerPlayer player) {
		boolean usedFirework = useFireworkFromIssue1528Hotbar(player);
		return issue1528Status(player).replace("{\"ok\":true,", "{\"ok\":true,\"usedFirework\":" + usedFirework + ",");
	}

	private static String tickIssue1528Refill(ServerPlayer player) {
		IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
		List<RefillUpgradeWrapper> refillWrappers = mainWrapper.getUpgradeHandler().getWrappersThatImplement(RefillUpgradeWrapper.class);
		refillWrappers.forEach(refill -> refill.tick(player, player.level(), player.blockPosition()));
		return issue1528Status(player).replace("{\"ok\":true,", "{\"ok\":true,\"tickedRefillUpgrades\":" + refillWrappers.size() + ",");
	}

	private static Issue1528SetupResult setupIssue1528Backpacks(ServerPlayer player) {
		player.getInventory().clearContent();
		ItemStack mainBackpack = createBackpackStack();
		ItemStack nestedBackpack = createIssue1528NestedBackpack();

		IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
		mainWrapper.setSlotNumbers(80, 5);
		UpgradeHandler mainUpgrades = mainWrapper.getUpgradeHandler();
		mainUpgrades.setStackInSlot(0, new ItemStack(ModItems.INCEPTION_UPGRADE.get()));
		mainUpgrades.setStackInSlot(1, new ItemStack(ModItems.ADVANCED_MAGNET_UPGRADE.get()));
		setMagnetFilterType(mainWrapper, ContentsFilterType.ALLOW);
		mainUpgrades.saveInventory();

		InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
		mainInventory.setStackInSlot(0, nestedBackpack);
		mainInventory.saveInventory();
		mainWrapper.getUpgradeHandler().refreshUpgradeWrappers();
		mainWrapper.onContentsUpdated();

		player.getInventory().setItem(0, mainBackpack);
		player.getInventory().setItem(8, new ItemStack(Items.FIREWORK_ROCKET));
		player.getInventory().setChanged();

		return new Issue1528SetupResult(mainWrapper.getContentsUuid().map(Object::toString).orElse(null),
				BackpackWrapper.fromStack(nestedBackpack).getContentsUuid().map(Object::toString).orElse(null));
	}

	private static ItemStack createIssue1528NestedBackpack() {
		ItemStack backpack = createNestedBackpack(Items.COBBLESTONE, Items.IRON_INGOT);
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(4, new ItemStack(ModItems.REFILL_UPGRADE.get()));
		upgrades.saveInventory();

		RefillUpgradeWrapper refill = wrapper.getUpgradeHandler().getWrappersThatImplement(RefillUpgradeWrapper.class).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Refill upgrade wrapper was not created"));
		refill.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.FIREWORK_ROCKET));
		refill.setTargetSlot(0, RefillUpgradeWrapper.TargetSlot.TOOLBAR_9);

		InventoryHandler inventory = wrapper.getInventoryHandler();
		inventory.setStackInSlot(2, new ItemStack(Items.DIAMOND));
		inventory.setStackInSlot(3, new ItemStack(Items.FIREWORK_ROCKET, 64));
		inventory.saveInventory();
		wrapper.onContentsUpdated();
		return backpack;
	}

	private static boolean useFireworkFromIssue1528Hotbar(ServerPlayer player) {
		player.getInventory().setItem(8, ItemStack.EMPTY);
		player.getInventory().setChanged();
		return true;
	}

	private static Issue1528ActionResult removeNestedDiamondAfterIssue1528Firework(ServerPlayer player, boolean refilled) {
		IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
		ItemStack nestedStack = mainWrapper.getInventoryHandler().getStackInSlot(0);
		if (!(nestedStack.getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("No issue 1528 nested backpack in main slot 0");
		}

		IBackpackWrapper nestedWrapper = BackpackWrapper.fromStack(nestedStack);
		InventoryHandler inventory = nestedWrapper.getInventoryHandler();
		int beforeDiamonds = countItems(inventory, Items.DIAMOND);
		int removedDiamonds = removeItems(inventory, Items.DIAMOND, 1);
		inventory.saveInventory();
		nestedWrapper.onContentsUpdated();

		return new Issue1528ActionResult(refilled, countItemsInPlayerSlot(player, 8, Items.FIREWORK_ROCKET), countItems(inventory, Items.FIREWORK_ROCKET),
				beforeDiamonds - removedDiamonds, removedDiamonds);
	}

	private static boolean waitForPlayerSlotItem(int slot, Item item, long timeoutMs) {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
		do {
			if (AutomationRuntime.runOnServer(player -> player.getInventory().getItem(slot).is(item))) {
				return true;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		return false;
	}

	private static int removeItems(InventoryHandler inventory, Item item, int count) {
		int removed = 0;
		for (int slot = 0; slot < inventory.size() && removed < count; slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (!stack.is(item)) {
				continue;
			}
			int toRemove = Math.min(count - removed, stack.getCount());
			ItemStack remaining = stack.copy();
			remaining.shrink(toRemove);
			inventory.setStackInSlot(slot, remaining);
			removed += toRemove;
		}
		return removed;
	}

	private static ItemStack createNestedBackpack(Item firstSeedItem, Item secondSeedItem) {
		ItemStack backpack = createBackpackStack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.setSlotNumbers(80, 5);
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_3.get()));
		upgrades.setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_3.get()));
		upgrades.setStackInSlot(2, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
		upgrades.setStackInSlot(3, new ItemStack(ModItems.ADVANCED_MAGNET_UPGRADE.get()));
		setMagnetFilterType(wrapper, ContentsFilterType.STORAGE);
		upgrades.saveInventory();

		InventoryHandler inventory = wrapper.getInventoryHandler();
		inventory.setStackInSlot(0, new ItemStack(firstSeedItem, 64));
		inventory.setStackInSlot(1, new ItemStack(secondSeedItem, 64));
		inventory.saveInventory();
		return backpack;
	}

	private static void setMagnetFilterType(IBackpackWrapper wrapper, ContentsFilterType filterType) {
		wrapper.getUpgradeHandler().getWrappersThatImplement(MagnetUpgradeWrapper.class)
				.forEach(magnet -> magnet.getFilterLogic().setDepositFilterType(filterType));
		wrapper.getUpgradeHandler().saveInventory();
	}

	private static ItemStack createBackpackStack() {
		ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 80);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);
		return backpack;
	}

	private static String stressBackpacks(ServerPlayer player, int stacks, int count, double radius) {
		ServerLevel level = (ServerLevel) player.level();
		Item[] items = {Items.COBBLESTONE, Items.IRON_INGOT, Items.DIRT, Items.GOLD_INGOT};
		int spawned = 0;
		for (int i = 0; i < stacks; i++) {
			double angle = Math.PI * 2D * i / Math.max(1, stacks);
			ItemStack stack = new ItemStack(items[i % items.length], Math.max(1, Math.min(count, items[i % items.length].getDefaultMaxStackSize())));
			ItemEntity entity = new ItemEntity(level, player.getX() + Math.cos(angle) * radius, player.getY() + 0.5D, player.getZ() + Math.sin(angle) * radius,
					stack);
			entity.setPickUpDelay(0);
			level.addFreshEntity(entity);
			spawned++;
		}
		return "{\"ok\":true,\"spawned\":" + spawned + "}";
	}

	private static String backpackStatus(ServerPlayer player) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem)) {
			return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
		}

		IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
		StringBuilder json = new StringBuilder("{\"ok\":true,");
		json.append(jsonProperty("mainUuid", mainWrapper.getContentsUuid().map(Object::toString).orElse(null))).append(',');
		json.append("\"mainItems\":").append(countItems(mainWrapper.getInventoryHandler())).append(',');
		json.append("\"nested\":[");
		boolean first = true;
		InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
		for (int slot = 0; slot < mainInventory.size(); slot++) {
			ItemStack stack = mainInventory.getStackInSlot(slot);
			if (!(stack.getItem() instanceof BackpackItem)) {
				continue;
			}
			if (!first) {
				json.append(',');
			}
			first = false;
			IBackpackWrapper nestedWrapper = BackpackWrapper.fromStack(stack);
			json.append('{').append("\"slot\":").append(slot).append(',')
					.append(jsonProperty("uuid", nestedWrapper.getContentsUuid().map(Object::toString).orElse(null))).append(',').append("\"items\":")
					.append(countItems(nestedWrapper.getInventoryHandler())).append('}');
		}
		json.append("]}");
		return json.toString();
	}

	private static String openNestedBackpack(ServerPlayer player, int nestedSlot) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("No backpack in player inventory slot 0");
		}
		ItemStack nestedBackpack = BackpackWrapper.fromStack(mainBackpack).getInventoryHandler().getStackInSlot(nestedSlot);
		if (!(nestedBackpack.getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("No nested backpack in requested slot");
		}
		BackpackContext context = new BackpackContext.ItemSubBackpack(PlayerInventoryProvider.MAIN_INVENTORY, "", 0, false, nestedSlot, true);
		boolean opened = player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new BackpackContainer(windowId, menuPlayer, context),
				Component.literal("Nested automation backpack")), context::toBuffer).isPresent();
		if (!opened) {
			throw new IllegalStateException("Failed to open nested automation backpack");
		}
		return "{\"ok\":true,\"nestedSlot\":" + nestedSlot + "," + jsonProperty("serverMenu", player.containerMenu.getClass().getName()) + "}";
	}

	private static String openMainBackpack(ServerPlayer player) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem backpackItem)) {
			throw new IllegalStateException("No backpack in player inventory slot 0");
		}
		player.getInventory().setSelectedSlot(0);
		backpackItem.use(player.level(), player, InteractionHand.MAIN_HAND);
		if (!(player.containerMenu instanceof BackpackContainer)) {
			throw new IllegalStateException("Failed to open automation backpack");
		}
		return "{\"ok\":true," + jsonProperty("serverMenu", player.containerMenu.getClass().getName()) + "}";
	}

	private static String changeMagnetSettings(ServerPlayer player, String target, ContentsFilterType filterType) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem)) {
			return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
		}
		int changed = 0;
		IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
		if (target.equals("all") || target.equals("main")) {
			changed += setMagnetFilterTypeAndCount(mainWrapper, filterType);
		}
		if (target.equals("all") || target.equals("nested")) {
			InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
			for (int slot = 0; slot < mainInventory.size(); slot++) {
				ItemStack stack = mainInventory.getStackInSlot(slot);
				if (stack.getItem() instanceof BackpackItem) {
					changed += setMagnetFilterTypeAndCount(BackpackWrapper.fromStack(stack), filterType);
				}
			}
		}
		return "{\"ok\":true,\"changed\":" + changed + "," + jsonProperty("filterType", filterType.getSerializedName()) + "}";
	}

	private static String changeMagnetPickup(ServerPlayer player, String target, boolean pickupItems) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem)) {
			return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
		}
		int changed = 0;
		IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
		if (target.equals("all") || target.equals("main")) {
			changed += setMagnetPickupAndCount(mainWrapper, pickupItems);
		}
		if (target.equals("all") || target.equals("nested")) {
			InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
			for (int slot = 0; slot < mainInventory.size(); slot++) {
				ItemStack stack = mainInventory.getStackInSlot(slot);
				if (stack.getItem() instanceof BackpackItem) {
					changed += setMagnetPickupAndCount(BackpackWrapper.fromStack(stack), pickupItems);
				}
			}
		}
		return "{\"ok\":true,\"changed\":" + changed + ",\"pickupItems\":" + pickupItems + "}";
	}

	private static int setMagnetPickupAndCount(IBackpackWrapper wrapper, boolean pickupItems) {
		List<MagnetUpgradeWrapper> magnets = wrapper.getUpgradeHandler().getWrappersThatImplement(MagnetUpgradeWrapper.class);
		magnets.forEach(magnet -> magnet.setPickupItems(pickupItems));
		wrapper.getUpgradeHandler().saveInventory();
		return magnets.size();
	}

	private static int setMagnetFilterTypeAndCount(IBackpackWrapper wrapper, ContentsFilterType filterType) {
		List<MagnetUpgradeWrapper> magnets = wrapper.getUpgradeHandler().getWrappersThatImplement(MagnetUpgradeWrapper.class);
		magnets.forEach(magnet -> magnet.getFilterLogic().setDepositFilterType(filterType));
		wrapper.getUpgradeHandler().saveInventory();
		return magnets.size();
	}

	private static String moveBackpacks(ServerPlayer player, String target, boolean clearCache) {
		int moved = 0;
		if (target.equals("main") || target.equals("all")) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (mainBackpack.getItem() instanceof BackpackItem) {
				player.getInventory().setItem(0, player.getInventory().getItem(1));
				player.getInventory().setItem(1, mainBackpack);
				player.getInventory().setItem(1, player.getInventory().getItem(0));
				player.getInventory().setItem(0, mainBackpack);
				player.getInventory().setChanged();
				moved++;
			}
		}
		if (target.equals("nested") || target.equals("all")) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (mainBackpack.getItem() instanceof BackpackItem) {
				InventoryHandler mainInventory = BackpackWrapper.fromStack(mainBackpack).getInventoryHandler();
				ItemStack first = mainInventory.getStackInSlot(0);
				ItemStack second = mainInventory.getStackInSlot(1);
				mainInventory.setStackInSlot(0, second);
				mainInventory.setStackInSlot(1, first);
				mainInventory.setStackInSlot(0, first);
				mainInventory.setStackInSlot(1, second);
				mainInventory.saveInventory();
				moved++;
			}
		}
		if (clearCache) {
			StorageWrapperRepository.clearCache();
		}
		return "{\"ok\":true,\"moved\":" + moved + ",\"cacheCleared\":" + clearCache + "}";
	}

	private static String spreadNestedBackpacks(ServerPlayer player) {
		IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
		InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
		List<ItemStack> nestedBackpacks = new ArrayList<>();
		for (int slot = 0; slot < mainInventory.size(); slot++) {
			ItemStack stack = mainInventory.getStackInSlot(slot);
			if (stack.getItem() instanceof BackpackItem) {
				nestedBackpacks.add(stack.copy());
				mainInventory.setStackInSlot(slot, ItemStack.EMPTY);
			}
		}
		int[] targetSlots = {13, 47, 72};
		for (int i = 0; i < nestedBackpacks.size() && i < targetSlots.length; i++) {
			mainInventory.setStackInSlot(targetSlots[i], nestedBackpacks.get(i));
		}
		mainInventory.saveInventory();
		return "{\"ok\":true,\"spread\":" + nestedBackpacks.size() + "}";
	}

	private static String fillMainBackpackNoise(ServerPlayer player) {
		IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
		InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
		int filled = 0;
		Item[] items = {Items.REDSTONE, Items.DIAMOND, Items.OAK_LOG, Items.GRAVEL, Items.COPPER_INGOT, Items.EMERALD};
		int[] slots = {0, 2, 5, 21, 34, 63};
		for (int i = 0; i < slots.length; i++) {
			if (mainInventory.getStackInSlot(slots[i]).isEmpty()) {
				mainInventory.setStackInSlot(slots[i], new ItemStack(items[i], 16 + i));
				filled++;
			}
		}
		mainInventory.saveInventory();
		return "{\"ok\":true,\"filled\":" + filled + "}";
	}

	private static String emptyNestedBackpacks(ServerPlayer player) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem)) {
			return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
		}
		int removed = 0;
		InventoryHandler mainInventory = BackpackWrapper.fromStack(mainBackpack).getInventoryHandler();
		for (int slot = 0; slot < mainInventory.size(); slot++) {
			ItemStack stack = mainInventory.getStackInSlot(slot);
			if (!(stack.getItem() instanceof BackpackItem)) {
				continue;
			}
			InventoryHandler nestedInventory = BackpackWrapper.fromStack(stack).getInventoryHandler();
			for (int nestedSlot = 0; nestedSlot < nestedInventory.size(); nestedSlot++) {
				removed += nestedInventory.getStackInSlot(nestedSlot).getCount();
				nestedInventory.setStackInSlot(nestedSlot, ItemStack.EMPTY);
			}
			nestedInventory.saveInventory();
		}
		return "{\"ok\":true,\"removed\":" + removed + "}";
	}

	private static String seedNestedBackpack(ServerPlayer player, int nestedSlot, Item item, int count) {
		IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
		int inserted = seedNestedBackpack(mainWrapper, nestedSlot, item, count);
		return "{\"ok\":true,\"inserted\":" + inserted + "}";
	}

	private static int seedNestedBackpack(IBackpackWrapper mainWrapper, int nestedSlot, Item item, int count) {
		ItemStack nestedStack = mainWrapper.getInventoryHandler().getStackInSlot(nestedSlot);
		if (!(nestedStack.getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("No nested backpack in slot " + nestedSlot);
		}
		InventoryHandler inventory = BackpackWrapper.fromStack(nestedStack).getInventoryHandler();
		int inserted = 0;
		while (inserted < count) {
			int toInsert = Math.min(64, count - inserted);
			int moved;
			try (Transaction tx = Transaction.openRoot()) {
				moved = inventory.insert(ItemResource.of(item), toInsert, tx);
				if (moved > 0) {
					tx.commit();
				}
			}
			inserted += moved;
			if (moved < toInsert) {
				break;
			}
		}
		inventory.saveInventory();
		return inserted;
	}

	private static String bulkDropFromNestedBackpack(ServerPlayer player, int nestedSlot, Item item, int maxStacks, int pickupDelay, boolean clearCache) {
		IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
		ItemStack nestedStack = mainWrapper.getInventoryHandler().getStackInSlot(nestedSlot);
		if (!(nestedStack.getItem() instanceof BackpackItem)) {
			return "{\"ok\":false,\"error\":\"No nested backpack in requested slot\"}";
		}
		IBackpackWrapper nestedWrapper = BackpackWrapper.fromStack(nestedStack);
		InventoryHandler inventory = nestedWrapper.getInventoryHandler();
		int dropped = 0;
		int stacksDropped = 0;
		for (int slot = 0; slot < inventory.size() && stacksDropped < maxStacks; slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (!stack.is(item)) {
				continue;
			}
			while (!inventory.getStackInSlot(slot).isEmpty() && inventory.getStackInSlot(slot).is(item) && stacksDropped < maxStacks) {
				ItemStack stackToExtract = inventory.getStackInSlot(slot);
				int extractedCount;
				try (Transaction tx = Transaction.openRoot()) {
					extractedCount = inventory.extract(slot, ItemResource.of(stackToExtract), Math.min(64, stackToExtract.getCount()), tx);
					if (extractedCount > 0) {
						tx.commit();
					}
				}
				if (extractedCount == 0) {
					break;
				}
				ItemStack extracted = stackToExtract.copyWithCount(extractedCount);
				dropStackAtPlayer(player, extracted, pickupDelay);
				dropped += extracted.getCount();
				stacksDropped++;
			}
		}
		inventory.saveInventory();
		if (clearCache) {
			StorageWrapperRepository.clearCache();
		}
		return "{\"ok\":true,\"dropped\":" + dropped + ",\"stacksDropped\":" + stacksDropped + ",\"pickupDelay\":" + pickupDelay + ",\"cacheCleared\":"
				+ clearCache + "}";
	}

	private static void dropStackAtPlayer(ServerPlayer player, ItemStack stack, int pickupDelay) {
		ItemEntity entity = new ItemEntity((ServerLevel) player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), stack);
		entity.setPickUpDelay(pickupDelay);
		((ServerLevel) player.level()).addFreshEntity(entity);
	}

	private static String droppedItemsStatus(ServerPlayer player, Item item) {
		AABB area = player.getBoundingBox().inflate(8D);
		int entities = 0;
		int items = 0;
		for (ItemEntity itemEntity : ((ServerLevel) player.level()).getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item))) {
			entities++;
			items += itemEntity.getItem().getCount();
		}
		int playerItems = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(item)) {
				playerItems += stack.getCount();
			}
		}
		return "{\"ok\":true,\"entities\":" + entities + ",\"items\":" + items + ",\"playerItems\":" + playerItems + "}";
	}

	private static String clearDroppedItems(ServerPlayer player, Item item) {
		AABB area = player.getBoundingBox().inflate(32D);
		int entities = 0;
		int items = 0;
		for (ItemEntity itemEntity : ((ServerLevel) player.level()).getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item))) {
			entities++;
			items += itemEntity.getItem().getCount();
			itemEntity.discard();
		}
		return "{\"ok\":true,\"entitiesRemoved\":" + entities + ",\"itemsRemoved\":" + items + "}";
	}

	private static IBackpackWrapper getMainBackpackWrapper(ServerPlayer player) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("No backpack in player inventory slot 0");
		}
		return BackpackWrapper.fromStack(mainBackpack);
	}

	private static BlockPos linkedStorageReloadPos(ServerLevel level) {
		return BlockPos.ZERO.east(3);
	}

	private static void closeLinkedStorageFacade(IBackpackWrapper backpack) {
		if (backpack instanceof LinkedStorageBackpackWrapper linkedStorageBackpack) {
			linkedStorageBackpack.close();
		}
	}

	private static int countItems(InventoryHandler inventory) {
		int count = 0;
		for (int slot = 0; slot < inventory.size(); slot++) {
			count += inventory.getStackInSlot(slot).getCount();
		}
		return count;
	}

	private static int countItems(InventoryHandler inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int countItemsInPlayerSlot(ServerPlayer player, int slot, Item item) {
		ItemStack stack = player.getInventory().getItem(slot);
		return stack.is(item) ? stack.getCount() : 0;
	}

	private static boolean appendAutosaveResult(StringBuilder json, String worldName, int count, long timeoutMs, long pollMs) {
		Path logPath = getAutosaveLogPath();
		Path levelDatPath = getWorldLevelDatPath(worldName);
		int startingLogCount = countAutosaveMessages(logPath, worldName);
		FileTime previousLevelDatModified = getLastModifiedTime(levelDatPath);
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
		int seen = 0;
		int seenLevelDatUpdates = 0;
		while (System.nanoTime() < deadline) {
			int seenLogMessages = Math.max(0, countAutosaveMessages(logPath, worldName) - startingLogCount);
			if (seenLogMessages >= count) {
				appendAutosaveResultJson(json, false, count, seenLogMessages, "log", logPath, levelDatPath);
				return true;
			}

			FileTime currentLevelDatModified = getLastModifiedTime(levelDatPath);
			if (currentLevelDatModified != null && (previousLevelDatModified == null || currentLevelDatModified.compareTo(previousLevelDatModified) > 0)) {
				seenLevelDatUpdates++;
				previousLevelDatModified = currentLevelDatModified;
			}
			seen = Math.max(seenLogMessages, seenLevelDatUpdates);
			if (seen >= count) {
				appendAutosaveResultJson(json, false, count, seen, "level.dat", logPath, levelDatPath);
				return true;
			}
			sleep(Math.max(100L, pollMs));
		}
		appendAutosaveResultJson(json, true, count, seen, "timeout", logPath, levelDatPath);
		return false;
	}

	private static void appendAutosaveResultJson(StringBuilder json, boolean timedOut, int requested, int seen, String source, Path logPath,
			Path levelDatPath) {
		json.append("\"autosavesBeforeActions\":").append(seen).append(",\"requestedAutosaves\":").append(requested).append(",\"autosaveTimedOut\":")
				.append(timedOut).append(',').append(jsonProperty("autosaveSource", source)).append(',').append(jsonProperty("autosaveLog", logPath.toString()))
				.append(',').append(jsonProperty("autosaveLevelDat", levelDatPath.toString()));
	}

	private static Path getAutosaveLogPath() {
		Path logsPath = Minecraft.getInstance().gameDirectory.toPath().resolve("logs");
		Path debugLogPath = logsPath.resolve("debug.log");
		return Files.exists(debugLogPath) ? debugLogPath : logsPath.resolve("latest.log");
	}

	private static int countAutosaveMessages(Path logPath, String worldName) {
		if (!Files.exists(logPath)) {
			return 0;
		}
		String autosaveMarker = "Gathered mod list to write to world save " + worldName;
		try {
			return countOccurrences(Files.readString(logPath), autosaveMarker);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read autosave log " + logPath, e);
		}
	}

	private static Path getWorldLevelDatPath(String worldName) {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("saves").resolve(worldName).resolve("level.dat");
	}

	private static FileTime getLastModifiedTime(Path path) {
		try {
			return Files.exists(path) ? Files.getLastModifiedTime(path) : null;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read last modified time for " + path, e);
		}
	}

	private static int countOccurrences(String text, String marker) {
		int count = 0;
		int offset = 0;
		while ((offset = text.indexOf(marker, offset)) >= 0) {
			count++;
			offset += marker.length();
		}
		return count;
	}

	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record Issue1528SetupResult(String mainUuid, String nestedUuid) {
	}

	private record Issue1528ActionResult(boolean refilled, int hotbarFireworks, int nestedFireworks, int nestedDiamonds, int removedDiamonds) {
	}
}
