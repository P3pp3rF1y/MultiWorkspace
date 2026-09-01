package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.p3pp3rf1y.sophisticatedstorage.init.ModItems.BASIC_TO_COPPER_TIER_UPGRADE;

public final class StorageControllerRegressions {
	private StorageControllerRegressions() {
	}

	public static String runDoubleChestRegressions(boolean inspectOnly) {
		return AutomationRuntime.runOnServer(player -> runStorageControllerDoubleChestRegressions(player, inspectOnly));
	}

	public static String runDoubleChestTierUpgradeRegressions() {
		return AutomationRuntime.runOnServer(player -> {
			List<String> failures = new ArrayList<>();
			runControllerDoubleChestTierUpgradeRegression(player, player.blockPosition().offset(0, 0, 6), true, failures);
			runControllerDoubleChestTierUpgradeRegression(player, player.blockPosition().offset(6, 0, 6), false, failures);
			return "{\"ok\":" + failures.isEmpty() + ",\"failed\":" + failures.size() + ",\"failures\":["
					+ failures.stream().map(failure -> "\"" + escapeJson(failure) + "\"").collect(Collectors.joining(",")) + "]}";
		});
	}

	public static String runFilterRegressions(String mode, int runs, boolean profileCapacity) {
		return AutomationRuntime.runOnServer(player -> {
			if (mode.equals("profile")) {
				return profileStorageControllerFilterRegressions(player, runs);
			}
			if (mode.equals("manualDepositProfile")) {
				return profileStorageControllerFilterManualDeposit(player, runs);
			}
			return runStorageControllerFilterRegressions(player, !mode.equals("setup"), profileCapacity);
		});
	}

	private static String runStorageControllerDoubleChestRegressions(ServerPlayer player, boolean inspectOnly) {
		List<ControllerDoubleChestRegressionResult> results = List.of(
				runControllerDoubleChestRegression(player, "double_chest_then_controller", player.blockPosition().offset(0, 0, 6), false, true, inspectOnly),
				runControllerDoubleChestRegression(player, "controller_then_left_chest_then_right_chest", player.blockPosition().offset(6, 0, 6), true, false,
						inspectOnly),
				runControllerDoubleChestRegression(player, "controller_then_right_chest_then_left_chest", player.blockPosition().offset(12, 0, 6), true, true,
						inspectOnly));

		long failed = results.stream().filter(result -> !result.passed()).count();
		StringBuilder json = new StringBuilder("{\"ok\":").append(failed == 0).append(",\"inspectOnly\":").append(inspectOnly).append(",\"total\":")
				.append(results.size()).append(",\"failed\":").append(failed).append(",\"results\":[");
		for (int i = 0; i < results.size(); i++) {
			if (i > 0) {
				json.append(',');
			}
			ControllerDoubleChestRegressionResult result = results.get(i);
			json.append('{').append(jsonProperty("name", result.name())).append(",\"passed\":").append(result.passed()).append(",\"registeredStorages\":")
					.append(result.registeredStorages()).append(",\"slots\":").append(result.slots()).append(',')
					.append(jsonProperty("positions", result.positions())).append(',').append(jsonProperty("chestState", result.chestState())).append(',')
					.append(jsonProperty("error", result.error())).append('}');
		}
		json.append("]}");
		return json.toString();
	}

	private static String runStorageControllerFilterRegressions(ServerPlayer player, boolean runInserts, boolean profileCapacity) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 12);
		List<String> failures = new ArrayList<>();
		long startedAt = System.nanoTime();

		clearStorageControllerFilterRegressionArea(level, controllerPos);
		placeController(level, player, controllerPos);

		List<BlockPos> overflowPositions = createOverflowPositions(controllerPos);
		List<ControllerFilterStorageSpec> filterSpecs = createControllerFilterStorageSpecs(controllerPos);
		List<ControllerLockedStorageSpec> lockedSpecs = createControllerFilterLockedStorageSpecs(controllerPos);
		List<BlockPos> allPositions = getAllControllerFilterStoragePositions(overflowPositions, filterSpecs, lockedSpecs);
		Map<BlockPos, Item> barrelItems = createControllerFilterBarrelItems(overflowPositions, filterSpecs, lockedSpecs);

		allPositions.forEach(pos -> placeBarrel(level, player, pos, barrelItems.getOrDefault(pos, ModBlocks.DIAMOND_BARREL_ITEM.get())));
		if (profileCapacity) {
			for (BlockPos pos : overflowPositions) {
				addControllerFilterProfileCapacity(getBarrelStorage(level, pos), 0);
			}
		}
		for (ControllerLockedStorageSpec spec : lockedSpecs) {
			StorageBlockEntity storage = getBarrelStorage(level, spec.pos());
			if (profileCapacity) {
				addControllerFilterProfileCapacity(storage, 0);
			}
			InventoryHandler inventory = storage.getStorageWrapper().getInventoryHandler();
			inventory.setStackInSlot(0, new ItemStack(spec.item(), 16 + spec.slot() % 32));
			inventory.saveInventory();
			if (!storage.isLocked()) {
				storage.toggleLock();
			}
		}
		for (ControllerFilterStorageSpec spec : filterSpecs) {
			configureControllerFilterStorage(getBarrelStorage(level, spec.pos()), spec, profileCapacity);
		}

		long setupMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

		ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
		if (controller == null) {
			failures.add("controller block entity missing at " + controllerPos);
			return buildStorageControllerFilterRegressionJson(false, setupMillis, 0, 0, 0, 0, 0, 0, 0, 0, failures);
		}

		int connectedStorages = controller.getStoragePositions().size();
		if (connectedStorages != allPositions.size()) {
			failures.add("expected " + allPositions.size() + " connected storages, got " + connectedStorages + ": " + controller.getStoragePositions());
		}
		assertStorageLockState(level, overflowPositions, false, "overflow", failures);
		assertStorageLockState(level, filterSpecs.stream().map(ControllerFilterStorageSpec::pos).toList(), false, "filtered", failures);
		assertStorageLockState(level, lockedSpecs.stream().map(ControllerLockedStorageSpec::pos).toList(), true, "locked", failures);

		Map<Item, Set<BlockPos>> lockedPositionsByItem = getLockedPositionsByItem(lockedSpecs);
		List<ControllerFilterInsertExpectation> expectations = createControllerFilterInsertExpectations(controllerPos, overflowPositions,
				lockedPositionsByItem);

		long insertStartedAt = System.nanoTime();
		int insertCalls = 0;
		long itemsInserted = 0;
		if (runInserts) {
			for (ControllerFilterInsertExpectation expectation : expectations) {
				ControllerFilterInsertStats stats = runControllerFilterInsertExpectation(level, controller, allPositions, expectation, failures);
				insertCalls += stats.calls();
				itemsInserted += stats.items();
			}
		}
		long insertMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - insertStartedAt);

		long verifyStartedAt = System.nanoTime();
		if (runInserts) {
			for (ControllerFilterStorageSpec spec : filterSpecs) {
				if (spec.allowList() && countItemInPositions(level, Set.of(spec.pos()), Items.FEATHER) > 0) {
					failures.add("allow-list filtered storage " + spec.name() + " received denied overflow test item");
				}
			}
		}
		long verifyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - verifyStartedAt);

		return buildStorageControllerFilterRegressionJson(failures.isEmpty(), setupMillis, insertMillis, verifyMillis, connectedStorages, lockedSpecs.size(),
				filterSpecs.size(), overflowPositions.size(), insertCalls, itemsInserted, failures);
	}

	private static String profileStorageControllerFilterRegressions(ServerPlayer player, int runs) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 12);
		List<String> failures = new ArrayList<>();
		List<BlockPos> overflowPositions = createOverflowPositions(controllerPos);
		List<ControllerFilterStorageSpec> filterSpecs = createControllerFilterStorageSpecs(controllerPos);
		List<ControllerLockedStorageSpec> lockedSpecs = createControllerFilterLockedStorageSpecs(controllerPos);
		List<BlockPos> allPositions = getAllControllerFilterStoragePositions(overflowPositions, filterSpecs, lockedSpecs);

		ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
		if (controller == null) {
			failures.add("controller block entity missing at " + controllerPos + "; run setup mode before profile mode");
			return buildStorageControllerFilterRegressionJson(false, 0, 0, 0, 0, lockedSpecs.size(), filterSpecs.size(), overflowPositions.size(), 0, 0,
					failures);
		}
		int connectedStorages = controller.getStoragePositions().size();
		if (connectedStorages != allPositions.size()) {
			failures.add("expected " + allPositions.size() + " connected storages, got " + connectedStorages + ": " + controller.getStoragePositions());
		}

		Map<Item, Set<BlockPos>> lockedPositionsByItem = getLockedPositionsByItem(lockedSpecs);
		List<ControllerFilterInsertExpectation> expectations = createControllerFilterInsertExpectations(controllerPos, overflowPositions,
				lockedPositionsByItem);

		long startedAt = System.nanoTime();
		int insertCalls = 0;
		long itemsInserted = 0;
		for (int run = 0; run < runs; run++) {
			for (ControllerFilterInsertExpectation expectation : expectations) {
				ControllerFilterInsertStats stats = runControllerFilterProfileExpectation(controller, expectation);
				insertCalls += stats.calls();
				itemsInserted += stats.items();
			}
		}
		long insertMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
		return buildStorageControllerFilterRegressionJson(failures.isEmpty(), 0, insertMillis, 0, connectedStorages, lockedSpecs.size(), filterSpecs.size(),
				overflowPositions.size(), insertCalls, itemsInserted, failures);
	}

	private static String profileStorageControllerFilterManualDeposit(ServerPlayer player, int runs) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 12);
		List<String> failures = new ArrayList<>();
		List<BlockPos> overflowPositions = createOverflowPositions(controllerPos);
		List<ControllerFilterStorageSpec> filterSpecs = createControllerFilterStorageSpecs(controllerPos);
		List<ControllerLockedStorageSpec> lockedSpecs = createControllerFilterLockedStorageSpecs(controllerPos);

		ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
		if (controller == null) {
			failures.add("controller block entity missing at " + controllerPos + "; run setup mode before manual deposit profile mode");
			return buildStorageControllerFilterRegressionJson("storage_controller_filter_manual_deposit", false, 0, 0, 0, 0, lockedSpecs.size(),
					filterSpecs.size(), overflowPositions.size(), 0, 0, failures);
		}
		int connectedStorages = controller.getStoragePositions().size();
		if (connectedStorages != overflowPositions.size() + filterSpecs.size() + lockedSpecs.size()) {
			failures.add("expected " + (overflowPositions.size() + filterSpecs.size() + lockedSpecs.size()) + " connected storages, got " + connectedStorages
					+ ": " + controller.getStoragePositions());
		}

		List<Item> depositItems = createControllerFilterManualDepositItems(lockedSpecs);
		long depositNanos = 0;
		long itemsDeposited = 0;
		int depositCalls = 0;
		player.getInventory().setSelectedSlot(0);
		clearControllerFilterManualDepositInventory(player);
		for (int run = 0; run < runs; run++) {
			fillControllerFilterManualDepositInventory(player, depositItems);
			long beforeDeposited = countControllerFilterManualDepositRemaining(player, depositItems.size());
			long startedAt = System.nanoTime();
			controller.depositPlayerItems(player, InteractionHand.MAIN_HAND);
			controller.depositPlayerItems(player, InteractionHand.MAIN_HAND);
			depositNanos += System.nanoTime() - startedAt;
			depositCalls += 2;
			long remaining = countControllerFilterManualDepositRemaining(player, depositItems.size());
			itemsDeposited += beforeDeposited - remaining;
			if (remaining != 0) {
				failures.add("manual deposit run " + run + " left " + remaining + " items in player inventory");
			}
		}
		clearControllerFilterManualDepositInventory(player);
		long depositMillis = TimeUnit.NANOSECONDS.toMillis(depositNanos);
		return buildStorageControllerFilterRegressionJson("storage_controller_filter_manual_deposit", failures.isEmpty(), 0, depositMillis, 0,
				connectedStorages, lockedSpecs.size(), filterSpecs.size(), overflowPositions.size(), depositCalls, itemsDeposited, failures);
	}

	private static List<BlockPos> createOverflowPositions(BlockPos controllerPos) {
		return List.of(controllerPos.offset(1, 0, -1), controllerPos.offset(1, 0, 0), controllerPos.offset(1, 0, 1), controllerPos.offset(1, 0, 2));
	}

	private static List<ControllerFilterStorageSpec> createControllerFilterStorageSpecs(BlockPos controllerPos) {
		return List.of(new ControllerFilterStorageSpec("specific_amethyst", controllerPos.offset(2, 0, -1), Items.AMETHYST_SHARD, true, PrimaryMatch.ITEM),
				new ControllerFilterStorageSpec("specific_honeycomb", controllerPos.offset(2, 0, 0), Items.HONEYCOMB, true, PrimaryMatch.ITEM),
				new ControllerFilterStorageSpec("specific_echo_shard", controllerPos.offset(2, 0, 1), Items.ECHO_SHARD, true, PrimaryMatch.ITEM),
				new ControllerFilterStorageSpec("mod_sophisticatedstorage", controllerPos.offset(3, 0, -1), ModBlocks.BARREL_ITEM.get(), true,
						PrimaryMatch.MOD),
				new ControllerFilterStorageSpec("mod_sophisticatedbackpacks", controllerPos.offset(3, 0, 0), ModItems.GOLD_BACKPACK.get(), true,
						PrimaryMatch.MOD),
				new ControllerFilterStorageSpec("deny_feather", controllerPos.offset(3, 0, 1), Items.FEATHER, false, PrimaryMatch.ITEM));
	}

	private static List<BlockPos> getAllControllerFilterStoragePositions(List<BlockPos> overflowPositions, List<ControllerFilterStorageSpec> filterSpecs,
			List<ControllerLockedStorageSpec> lockedSpecs) {
		List<BlockPos> allPositions = new ArrayList<>();
		allPositions.addAll(overflowPositions);
		filterSpecs.forEach(spec -> allPositions.add(spec.pos()));
		lockedSpecs.forEach(spec -> allPositions.add(spec.pos()));
		return allPositions;
	}

	private static Map<Item, Set<BlockPos>> getLockedPositionsByItem(List<ControllerLockedStorageSpec> lockedSpecs) {
		Map<Item, Set<BlockPos>> lockedPositionsByItem = new HashMap<>();
		for (ControllerLockedStorageSpec spec : lockedSpecs) {
			lockedPositionsByItem.computeIfAbsent(spec.item(), item -> new HashSet<>()).add(spec.pos());
		}
		return lockedPositionsByItem;
	}

	private static List<Item> createControllerFilterManualDepositItems(List<ControllerLockedStorageSpec> lockedSpecs) {
		List<Item> depositItems = new ArrayList<>();
		lockedSpecs.stream().limit(24).map(ControllerLockedStorageSpec::item).forEach(depositItems::add);
		depositItems.add(Items.AMETHYST_SHARD);
		depositItems.add(Items.HONEYCOMB);
		depositItems.add(Items.ECHO_SHARD);
		depositItems.add(ModBlocks.GOLD_BARREL_ITEM.get());
		depositItems.add(ModItems.STACK_UPGRADE_TIER_1.get());
		depositItems.add(Items.NAUTILUS_SHELL);
		return depositItems;
	}

	private static void fillControllerFilterManualDepositInventory(ServerPlayer player, List<Item> depositItems) {
		for (int slot = 0; slot < depositItems.size(); slot++) {
			player.getInventory().setItem(slot, new ItemStack(depositItems.get(slot)));
		}
	}

	private static void clearControllerFilterManualDepositInventory(ServerPlayer player) {
		for (int slot = 0; slot < 36; slot++) {
			player.getInventory().setItem(slot, ItemStack.EMPTY);
		}
	}

	private static long countControllerFilterManualDepositRemaining(ServerPlayer player, int slots) {
		long remaining = 0;
		for (int slot = 0; slot < slots; slot++) {
			remaining += player.getInventory().getItem(slot).getCount();
		}
		return remaining;
	}

	private static List<ControllerFilterInsertExpectation> createControllerFilterInsertExpectations(BlockPos controllerPos, List<BlockPos> overflowPositions,
			Map<Item, Set<BlockPos>> lockedPositionsByItem) {
		List<ControllerFilterInsertExpectation> expectations = new ArrayList<>();
		lockedPositionsByItem
				.forEach((item, positions) -> expectations.add(new ControllerFilterInsertExpectation("locked_" + itemId(item), item, 3, 20, positions)));
		expectations.add(new ControllerFilterInsertExpectation("specific_amethyst", Items.AMETHYST_SHARD, 4, 80, Set.of(controllerPos.offset(2, 0, -1))));
		expectations.add(new ControllerFilterInsertExpectation("specific_honeycomb", Items.HONEYCOMB, 4, 80, Set.of(controllerPos.offset(2, 0, 0))));
		expectations.add(new ControllerFilterInsertExpectation("specific_echo_shard", Items.ECHO_SHARD, 4, 80, Set.of(controllerPos.offset(2, 0, 1))));
		expectations.add(new ControllerFilterInsertExpectation("mod_sophisticatedstorage", ModBlocks.GOLD_BARREL_ITEM.get(), 2, 80,
				Set.of(controllerPos.offset(3, 0, -1))));
		expectations.add(new ControllerFilterInsertExpectation("mod_sophisticatedbackpacks", ModItems.STACK_UPGRADE_TIER_1.get(), 2, 80,
				Set.of(controllerPos.offset(3, 0, 0))));
		expectations.add(new ControllerFilterInsertExpectation("deny_accepts_unmatched", Items.NAUTILUS_SHELL, 4, 80, Set.of(controllerPos.offset(3, 0, 1))));
		expectations.add(new ControllerFilterInsertExpectation("denied_item_overflows", Items.FEATHER, 4, 80, Set.copyOf(overflowPositions)));
		return expectations;
	}

	private static List<ControllerLockedStorageSpec> createControllerFilterLockedStorageSpecs(BlockPos controllerPos) {
		List<Item> items = List.of(Items.COBBLESTONE, Items.DIRT, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.TUFF, Items.DEEPSLATE, Items.CALCITE,
				Items.SAND, Items.RED_SAND, Items.GRAVEL, Items.CLAY_BALL, Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG,
				Items.DARK_OAK_LOG, Items.MANGROVE_LOG, Items.CHERRY_LOG, Items.BAMBOO, Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.STICK,
				Items.COAL, Items.CHARCOAL, Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.LAPIS_LAZULI, Items.EMERALD, Items.DIAMOND,
				Items.QUARTZ, Items.FLINT, Items.STRING, Items.SPIDER_EYE, Items.BONE, Items.ROTTEN_FLESH, Items.GUNPOWDER, Items.LEATHER, Items.RABBIT_HIDE,
				Items.EGG, Items.WHEAT, Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.COBBLESTONE, Items.IRON_INGOT);
		List<ControllerLockedStorageSpec> specs = new ArrayList<>();
		int slot = 0;
		for (int x = 4; x <= 12 && slot < items.size(); x++) {
			for (int z = -2; z <= 3 && slot < items.size(); z++) {
				specs.add(new ControllerLockedStorageSpec(controllerPos.offset(x, 0, z), items.get(slot), slot, getControllerFilterLockedBarrelItem(slot)));
				slot++;
			}
		}
		return specs;
	}

	private static Item getControllerFilterLockedBarrelItem(int index) {
		if (index < 5) {
			return ModBlocks.NETHERITE_BARREL_ITEM.get();
		}
		if (index < 35) {
			return ModBlocks.DIAMOND_BARREL_ITEM.get();
		}
		if (index < 43) {
			return ModBlocks.GOLD_BARREL_ITEM.get();
		}
		if (index < 48) {
			return ModBlocks.IRON_BARREL_ITEM.get();
		}
		return ModBlocks.BARREL_ITEM.get();
	}

	private static Map<BlockPos, Item> createControllerFilterBarrelItems(List<BlockPos> overflowPositions, List<ControllerFilterStorageSpec> filterSpecs,
			List<ControllerLockedStorageSpec> lockedSpecs) {
		Map<BlockPos, Item> barrelItems = new HashMap<>();
		List<Item> overflowBarrels = List.of(ModBlocks.BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(),
				ModBlocks.DIAMOND_BARREL_ITEM.get());
		for (int i = 0; i < overflowPositions.size(); i++) {
			barrelItems.put(overflowPositions.get(i), overflowBarrels.get(i % overflowBarrels.size()));
		}
		List<Item> filterBarrels = List.of(ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(),
				ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get());
		for (int i = 0; i < filterSpecs.size(); i++) {
			barrelItems.put(filterSpecs.get(i).pos(), filterBarrels.get(i % filterBarrels.size()));
		}
		lockedSpecs.forEach(spec -> barrelItems.put(spec.pos(), spec.barrelItem()));
		return barrelItems;
	}

	private static void clearStorageControllerFilterRegressionArea(ServerLevel level, BlockPos controllerPos) {
		discardStorageControllerFilterRegressionItemEntities(level, controllerPos);
		for (int x = -2; x <= 14; x++) {
			for (int y = -1; y <= 2; y++) {
				for (int z = -5; z <= 6; z++) {
					BlockPos pos = controllerPos.offset(x, y, z);
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
						storage.clearContent();
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		discardStorageControllerFilterRegressionItemEntities(level, controllerPos);
	}

	private static void discardStorageControllerFilterRegressionItemEntities(ServerLevel level, BlockPos controllerPos) {
		AABB area = new AABB(controllerPos.getX() - 4, controllerPos.getY() - 2, controllerPos.getZ() - 7, controllerPos.getX() + 17, controllerPos.getY() + 5,
				controllerPos.getZ() + 9);
		for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
			itemEntity.discard();
		}
	}

	private static void placeBarrel(ServerLevel level, ServerPlayer player, BlockPos pos, Item barrelItem) {
		placeBlockWithItem(level, player, pos, new ItemStack(barrelItem));
	}

	private static StorageBlockEntity getBarrelStorage(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get()).map(storage -> (StorageBlockEntity) storage)
				.orElseThrow(() -> new IllegalStateException("Missing barrel storage at " + pos));
	}

	private static void configureControllerFilterStorage(StorageBlockEntity storage, ControllerFilterStorageSpec spec, boolean profileCapacity) {
		UpgradeHandler upgrades = storage.getStorageWrapper().getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModItems.ADVANCED_FILTER_UPGRADE.get()));
		if (profileCapacity) {
			addControllerFilterProfileCapacity(storage, 1);
		}
		FilterUpgradeWrapper filter = upgrades.getWrappersThatImplement(FilterUpgradeWrapper.class).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Filter upgrade wrapper missing in " + spec.name()));
		filter.setDirection(net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction.INPUT);
		filter.getFilterLogic().setDepositFilterType(spec.allowList() ? ContentsFilterType.ALLOW : ContentsFilterType.BLOCK);
		filter.getFilterLogic().setPrimaryMatch(spec.primaryMatch());
		filter.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(spec.filterItem()));
		upgrades.saveInventory();
		storage.getStorageWrapper().refreshInventoryForInputOutput();
	}

	private static void addControllerFilterProfileCapacity(StorageBlockEntity storage, int firstUpgradeSlot) {
		UpgradeHandler upgrades = storage.getStorageWrapper().getUpgradeHandler();
		for (int slot = firstUpgradeSlot; slot < Math.min(upgrades.size(), firstUpgradeSlot + 2); slot++) {
			upgrades.setStackInSlot(slot, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModItems.STACK_UPGRADE_TIER_5.get()));
		}
		upgrades.saveInventory();
	}

	private static void assertStorageLockState(ServerLevel level, List<BlockPos> positions, boolean locked, String group, List<String> failures) {
		for (BlockPos pos : positions) {
			StorageBlockEntity storage = getBarrelStorage(level, pos);
			if (storage.isLocked() != locked) {
				failures.add(group + " storage lock state mismatch at " + pos + ": expected locked=" + locked + ", actual=" + storage.isLocked());
			}
		}
	}

	private static ControllerFilterInsertStats runControllerFilterInsertExpectation(ServerLevel level, ControllerBlockEntity controller,
			List<BlockPos> allPositions, ControllerFilterInsertExpectation expectation, List<String> failures) {
		Set<BlockPos> outsidePositions = new HashSet<>(allPositions);
		outsidePositions.removeAll(expectation.expectedPositions());
		long expectedBefore = countItemInPositions(level, expectation.expectedPositions(), expectation.item());
		long outsideBefore = countItemInPositions(level, outsidePositions, expectation.item());
		long inserted = 0;
		for (int i = 0; i < expectation.calls(); i++) {
			ItemStack remainder = insertIntoController(controller, new ItemStack(expectation.item(), expectation.count()));
			if (!remainder.isEmpty()) {
				failures.add(expectation.name() + " insert " + i + " returned remainder " + remainder.getCount() + "x" + itemId(remainder.getItem()));
			}
			inserted += expectation.count() - remainder.getCount();
		}
		long expectedAfter = countItemInPositions(level, expectation.expectedPositions(), expectation.item());
		long outsideAfter = countItemInPositions(level, outsidePositions, expectation.item());
		if (expectedAfter - expectedBefore != inserted) {
			failures.add(expectation.name() + " expected destination delta " + inserted + " for " + itemId(expectation.item()) + ", got "
					+ (expectedAfter - expectedBefore) + " at " + expectation.expectedPositions());
		}
		if (outsideAfter != outsideBefore) {
			failures.add(expectation.name() + " changed outside destination count for " + itemId(expectation.item()) + " by " + (outsideAfter - outsideBefore));
		}
		return new ControllerFilterInsertStats(expectation.calls(), inserted);
	}

	private static ControllerFilterInsertStats runControllerFilterProfileExpectation(ControllerBlockEntity controller,
			ControllerFilterInsertExpectation expectation) {
		long inserted = 0;
		for (int i = 0; i < expectation.calls(); i++) {
			ItemStack remainder = insertIntoController(controller, new ItemStack(expectation.item(), expectation.count()));
			inserted += expectation.count() - remainder.getCount();
		}
		return new ControllerFilterInsertStats(expectation.calls(), inserted);
	}

	private static ItemStack insertIntoController(ControllerBlockEntity controller, ItemStack stack) {
		try (Transaction transaction = Transaction.openRoot()) {
			int inserted = controller.insert(ItemResource.of(stack), stack.getCount(), transaction);
			if (inserted > 0) {
				transaction.commit();
			}
			return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
		}
	}

	private static long countItemInPositions(ServerLevel level, Set<BlockPos> positions, Item item) {
		long count = 0;
		for (BlockPos pos : positions) {
			InventoryHandler inventory = getBarrelStorage(level, pos).getStorageWrapper().getInventoryHandler();
			for (int slot = 0; slot < inventory.size(); slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				if (stack.is(item)) {
					count += stack.getCount();
				}
			}
		}
		return count;
	}

	private static String buildStorageControllerFilterRegressionJson(boolean ok, long setupMillis, long insertMillis, long verifyMillis, int connectedStorages,
			int lockedStorages, int filteredStorages, int overflowStorages, int insertCalls, long itemsInserted, List<String> failures) {
		return buildStorageControllerFilterRegressionJson("storage_controller_filter_routing", ok, setupMillis, insertMillis, verifyMillis, connectedStorages,
				lockedStorages, filteredStorages, overflowStorages, insertCalls, itemsInserted, failures);
	}

	private static String buildStorageControllerFilterRegressionJson(String scenario, boolean ok, long setupMillis, long insertMillis, long verifyMillis,
			int connectedStorages, int lockedStorages, int filteredStorages, int overflowStorages, int insertCalls, long itemsInserted, List<String> failures) {
		StringBuilder json = new StringBuilder("{\"ok\":").append(ok).append(",\"scenario\":\"").append(scenario).append("\"").append(",\"connectedStorages\":")
				.append(connectedStorages).append(",\"lockedStorages\":").append(lockedStorages).append(",\"filteredStorages\":").append(filteredStorages)
				.append(",\"overflowStorages\":").append(overflowStorages).append(",\"insertCalls\":").append(insertCalls).append(",\"itemsInserted\":")
				.append(itemsInserted).append(",\"setupMillis\":").append(setupMillis).append(",\"insertMillis\":").append(insertMillis)
				.append(",\"verifyMillis\":").append(verifyMillis).append(",\"failed\":").append(failures.size()).append(",\"failures\":[");
		for (int i = 0; i < failures.size(); i++) {
			if (i > 0) {
				json.append(',');
			}
			json.append('"').append(escapeJson(failures.get(i))).append('"');
		}
		json.append("]}");
		return json.toString();
	}

	private static String itemId(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	private static ControllerDoubleChestRegressionResult runControllerDoubleChestRegression(ServerPlayer player, String name, BlockPos controllerPos,
			boolean controllerFirst, boolean farChestFirst, boolean inspectOnly) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos leftChestPos = controllerPos.east();
		BlockPos rightChestPos = leftChestPos.east();
		if (!inspectOnly) {
			clearControllerDoubleChestRegressionArea(level, controllerPos);

			if (controllerFirst) {
				placeController(level, player, controllerPos);
			}
			if (farChestFirst) {
				placeChest(level, player, rightChestPos);
				placeChest(level, player, leftChestPos);
			} else {
				placeChest(level, player, leftChestPos);
				placeChest(level, player, rightChestPos);
			}
			if (!controllerFirst) {
				placeController(level, player, controllerPos);
			}
		}

		return level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).map(controller -> {
			List<BlockPos> storagePositions = controller.getStoragePositions();
			int registeredStorages = storagePositions.size();
			String positions = storagePositions.toString();
			String chestState = getChestState(level, leftChestPos) + "; " + getChestState(level, rightChestPos);
			int slots = registeredStorages == 1 ? controller.getSlots(0) : 0;
			boolean mainStorageRegistered = registeredStorages == 1 && storagePositions.contains(rightChestPos);
			boolean passed = mainStorageRegistered && slots == 54 && isDoubleChest(level, leftChestPos, rightChestPos);
			String error = null;
			if (!passed) {
				error = "expected one connected double chest registered at " + rightChestPos + " with 54 slots; slots=" + slots + "; chestState=" + chestState;
			}
			return new ControllerDoubleChestRegressionResult(name, passed, registeredStorages, slots, positions, chestState, error);
		}).orElseGet(() -> new ControllerDoubleChestRegressionResult(name, false, 0, 0, "[]",
				getChestState(level, leftChestPos) + "; " + getChestState(level, rightChestPos), "controller block entity missing"));
	}

	private static void runControllerDoubleChestTierUpgradeRegression(ServerPlayer player, BlockPos controllerPos, boolean upgradeMainChest,
			List<String> failures) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos leftChestPos = controllerPos.east();
		BlockPos rightChestPos = leftChestPos.east();
		clearControllerDoubleChestRegressionArea(level, controllerPos);
		placeController(level, player, controllerPos);
		placeChest(level, player, leftChestPos);
		placeChest(level, player, rightChestPos);

		ChestBlockEntity mainChest = level.getBlockEntity(rightChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).orElse(null);
		if (mainChest == null) {
			failures.add("missing main chest before " + (upgradeMainChest ? "main" : "secondary") + " upgrade");
			return;
		}
		mainChest.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));

		BlockPos upgradedChestPos = upgradeMainChest ? rightChestPos : leftChestPos;
		ItemStack tierUpgrade = new ItemStack(BASIC_TO_COPPER_TIER_UPGRADE.get(), 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, tierUpgrade);
		InteractionResult result = player.gameMode.useItemOn(player, level, tierUpgrade, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(upgradedChestPos), Direction.UP, upgradedChestPos, false));
		String scenario = upgradeMainChest ? "main" : "secondary";
		if (!result.consumesAction()) {
			failures.add(scenario + " upgrade did not consume the interaction: " + result);
			return;
		}
		if (!level.getBlockState(leftChestPos).is(ModBlocks.COPPER_CHEST.get()) || !level.getBlockState(rightChestPos).is(ModBlocks.COPPER_CHEST.get())) {
			failures.add(scenario + " upgrade did not convert both chest halves to copper");
			return;
		}

		ChestBlockEntity upgradedMainChest = level.getBlockEntity(rightChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).orElse(null);
		if (upgradedMainChest == null || !upgradedMainChest.getStorageWrapper().getInventoryHandler().getStackInSlot(0).is(Items.DIAMOND)) {
			failures.add(scenario + " upgrade did not retain the chest item");
			return;
		}
		ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
		if (controller == null || controller.getStoragePositions().size() != 1 || !controller.getStoragePositions().contains(rightChestPos)
				|| controller.getSlots(0) != upgradedMainChest.getStorageWrapper().getInventoryHandler().size()) {
			failures.add(scenario + " upgrade did not correctly update the controller storage");
			return;
		}
		for (BlockPos chestPos : List.of(leftChestPos, rightChestPos)) {
			player.closeContainer();
			InteractionResult openResult = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(chestPos), Direction.UP, chestPos, false));
			if (!openResult.consumesAction() || !(player.containerMenu instanceof StorageContainerMenu menu)
					|| !menu.getStorageBlockEntity().getBlockPos().equals(rightChestPos) || !menu.getSlot(0).getItem().is(Items.DIAMOND)) {
				failures.add(scenario + " upgrade did not open the retained item from " + chestPos);
			}
		}
		player.closeContainer();
	}

	private static void clearControllerDoubleChestRegressionArea(ServerLevel level, BlockPos controllerPos) {
		for (int x = -1; x <= 3; x++) {
			for (int y = -1; y <= 2; y++) {
				for (int z = -1; z <= 1; z++) {
					level.setBlock(controllerPos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static void placeController(ServerLevel level, ServerPlayer player, BlockPos pos) {
		placeBlockWithItem(level, player, pos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
	}

	private static void placeChest(ServerLevel level, ServerPlayer player, BlockPos pos) {
		placeBlockWithItem(level, player, pos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
	}

	private static void placeBlockWithItem(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		BlockPos supportPos = pos.below();
		level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
		player.setYRot(0);
		player.setXRot(0);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false);
		player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitResult);
	}

	private static boolean isDoubleChest(ServerLevel level, BlockPos leftChestPos, BlockPos rightChestPos) {
		return level.getBlockState(leftChestPos).is(ModBlocks.CHEST.get()) && level.getBlockState(rightChestPos).is(ModBlocks.CHEST.get())
				&& level.getBlockState(leftChestPos).getValue(ChestBlock.TYPE) == ChestType.LEFT
				&& level.getBlockState(rightChestPos).getValue(ChestBlock.TYPE) == ChestType.RIGHT
				&& level.getBlockEntity(leftChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(be -> be.getMainPos().equals(rightChestPos)).orElse(false)
				&& level.getBlockEntity(rightChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(be -> be.getMainPos().equals(rightChestPos)).orElse(false);
	}

	private static String getChestState(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(ModBlocks.CHEST.get())) {
			return pos + "=not_chest(" + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + ")";
		}
		return level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
				.map(be -> pos + "=type:" + state.getValue(ChestBlock.TYPE) + ",facing:" + state.getValue(ChestBlock.FACING) + ",main:" + be.getMainPos()
						+ ",hasData:" + be.hasStorageData() + ",controller:" + be.getControllerPos().map(Object::toString).orElse("none") + ",slots:"
						+ be.getStorageWrapper().getInventoryHandler().size())
				.orElse(pos + "=chest_without_be");
	}

	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private record ControllerDoubleChestRegressionResult(String name, boolean passed, int registeredStorages, int slots, String positions, String chestState,
			String error) {
	}

	private record ControllerFilterStorageSpec(String name, BlockPos pos, Item filterItem, boolean allowList, PrimaryMatch primaryMatch) {
	}

	private record ControllerLockedStorageSpec(BlockPos pos, Item item, int slot, Item barrelItem) {
	}

	private record ControllerFilterInsertExpectation(String name, Item item, int count, int calls, Set<BlockPos> expectedPositions) {
	}

	private record ControllerFilterInsertStats(int calls, long items) {
	}
}
