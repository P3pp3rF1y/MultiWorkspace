package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

public final class StorageLinkedStorageRegression {
	private static final int MAX_MAGNET_RANGE = 20;
	private static final int PLATFORM_RADIUS = 15;
	private static final int LINKED_LIMITED_RELOAD_ITEM_COUNT = 23;
	private static final BlockPos LINKED_LIMITED_RELOAD_PRIMARY_POS = new BlockPos(0, 73, 48);
	private static final BlockPos LINKED_LIMITED_RELOAD_SECONDARY_POS = LINKED_LIMITED_RELOAD_PRIMARY_POS.east(3);

	private StorageLinkedStorageRegression() {
	}

	public static String run() {
		String stage = "ordinary linked storage";
		try {
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runOrdinaryLinkedStorage);
			stage = "canonical storage type insertion rules";
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runCanonicalStorageTypeInsertionRules);
			stage = "linked storage menu snapshot";
			runLinkedStorageMenuCanonicalSnapshotRegression();
			stage = "open-menu expansion";
			runLinkedStorageOpenMenuExpansionRegression();
			stage = "linked primary chest drop/restore";
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedPrimaryChestDropRestore);
			stage = "linked secondary chest split";
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedSecondaryChestSplitLifecycle);
			stage = "linked controller canonical contents";
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerCanonicalContents);
			stage = "legacy controller storage key migration";
			runLegacyControllerStorageKeysMigrationRegression();
			stage = "linked controller tier upgrade";
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerTierUpgrade);
			stage = "linked controller primary candidate";
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerTierUpgradeRequiresPrimary);
			stage = "linked controller tools and restoration";
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerPaintbrushVisibilityAndRestoration);
			stage = "linked double-chest controller contents";
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedDoubleChestControllerCanonicalContents);
			stage = "limited-barrel secondary memory";
			runLinkedLimitedBarrelSecondaryMemorySync();
			stage = "dropped linked tooltip cache";
			runDroppedLinkedStorageTooltipCache();
			stage = "secondary pickup suppression";
			runSecondaryPickupSuppression();
			stage = "controller double-chest real-player regression";
			require(StorageControllerRegressions.runDoubleChestRegressions(false).contains("\"ok\":true"),
					"Target double-chest controller real-player regression did not pass");
			return "{\"ok\":true,\"ordinaryBarrelLinkingSharesCanonicalContents\":true,"
					+ "\"canonicalStorageTypeControlsInsertionRules\":true,\"linkedStorageMenuReceivesCanonicalSnapshot\":true,"
					+ "\"openMenuExpansionInvalidatesStaleSnapshot\":true,\"linkedChestDropAndSplitLifecycle\":true,"
					+ "\"linkedControllerEndpointJoinKeepsOneCanonicalContentIndex\":true," + "\"legacyControllerStorageKeysMigrationRebuildsIndexes\":true,"
					+ "\"linkedDoubleChestControllerUsesOneCanonicalContentIndex\":true," + "\"controllerDoubleChestPlacementUsesOneStorage\":true,"
					+ "\"linkedControllerRoutesAndFansOutToolOperations\":true,\"linkedControllerTierPaintVisibilityLifecycle\":true,"
					+ "\"limitedBarrelSecondaryMemorySync\":true,\"droppedLinkedStorageTooltipCache\":true," + "\"secondaryPickupSuppression\":true}";
		} catch (RuntimeException e) {
			String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
			return "{\"ok\":false,\"error\":" + jsonString(stage + ": " + message) + "}";
		}
	}

	private static void runLinkedStorageMenuCanonicalSnapshotRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedStorageMenuFixture);
		try {
			waitForClientLinkedStorageBlock(fixture.primaryPos(), fixture.groupId(), LinkedStorageEndpointRole.PRIMARY);
			waitForClientLinkedStorageBlock(fixture.secondaryPos(), fixture.groupId(), LinkedStorageEndpointRole.SECONDARY);
			openStorageMenu(fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			waitForClientLinkedStorageMenuCanonicalSnapshot(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			closeStorageMenu();
			openStorageMenu(fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			waitForClientLinkedStorageMenuCanonicalSnapshot(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), fixture.anchor());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
			waitForClientPlayerTransform(fixture.playerState().position(), fixture.playerState().yRot(), fixture.playerState().xRot());
		}
	}

	private static LinkedStorageMenuFixture setupLinkedStorageMenuFixture(ServerPlayer player) {
		ServerLevel level = player.level();
		PlayerState playerState = capturePlayerState(player);
		BlockPos anchor = player.blockPosition().offset(0, 0, 28);
		BlockPos primaryPos = anchor;
		BlockPos secondaryPos = anchor.east(3);
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		clearArea(level, anchor);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link barrels for the linked storage menu snapshot");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "menu snapshot primary barrel");
			require(primaryEndpoint.groupId().equals(requireEndpoint(secondary, "menu snapshot secondary barrel").groupId())
					&& count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
					"Linked barrels did not share the canonical menu snapshot contents");
			String title = "Linked Storage Canonical Snapshot";
			primary.setCustomName(Component.literal(title));
			return new LinkedStorageMenuFixture(anchor, primaryPos, secondaryPos, primaryEndpoint.groupId(),
					primary.getStorageWrapper().getInventoryHandler().getSlots(), title, playerState);
		} catch (RuntimeException e) {
			clearArea(level, anchor);
			restorePlayerState(player, playerState);
			throw e;
		} finally {
			player.setGameMode(originalGameMode);
		}
	}

	private static void waitForClientLinkedStorageMenuCanonicalSnapshot(LinkedStorageMenuFixture fixture, BlockPos openedPos,
			LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null
					&& Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu menu
					&& screen.getTitle().getString().equals(fixture.title())
					&& Minecraft.getInstance().level.getBlockEntity(openedPos) instanceof BarrelBlockEntity barrel
					&& barrel.getLinkedStorageEndpointData() != null && fixture.groupId().equals(barrel.getLinkedStorageEndpointData().groupId())
					&& menu.getStorageBlockEntity().getLinkedStorageEndpointData() != null
					&& fixture.groupId().equals(menu.getStorageBlockEntity().getLinkedStorageEndpointData().groupId())
					&& menu.getBlockPosition().equals(Optional.of(openedPos)) && menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
					&& provider.getLinkedStorageEndpointRole().filter(role -> role == expectedRole).isPresent()
					&& menu.getNumberOfStorageInventorySlots() == fixture.inventorySlots() && menu.getSlot(0).getItem().is(Items.DIAMOND)
					&& menu.getSlot(0).getItem().getCount() == 7 && count(menu, Items.DIAMOND) == 7)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked StorageContainerMenu did not receive the canonical client snapshot");
	}

	private static void waitForClientLinkedStorageBlock(BlockPos pos, UUID groupId, LinkedStorageEndpointRole role) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof StorageBlockEntity storage
							&& storage.getLinkedStorageEndpointData() != null && storage.getLinkedStorageEndpointData().groupId().equals(groupId)
							&& storage.getStorageWrapper().getLinkedStorageEndpointRole().filter(endpointRole -> endpointRole == role).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client did not synchronize the linked storage " + role + " block at " + pos);
	}

	private static void runLinkedStorageOpenMenuExpansionRegression() {
		OpenMenuExpansionFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			PlayerState playerState = capturePlayerState(player);
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 40);
			BlockPos addedPos = primaryPos.east();
			clearArea(level, primaryPos);
			try {
				placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				ChestBlockEntity primary = getChest(level, primaryPos);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
						primary) == LinkedStorageService.LinkResult.SUCCESS, "Could not create the open-menu expansion primary chest");
				primary.setCustomName(Component.literal("Open Menu Expansion"));
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				return new OpenMenuExpansionFixture(primaryPos, addedPos, requireEndpoint(primary, "open-menu expansion primary"), playerState);
			} catch (RuntimeException e) {
				clearArea(level, primaryPos);
				restorePlayerState(player, playerState);
				throw e;
			}
		});
		try {
			waitForClientLinkedStorageBlock(fixture.primaryPos(), fixture.endpoint().groupId(), LinkedStorageEndpointRole.PRIMARY);
			openStorageMenu(fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			waitForStorageMenu("Open Menu Expansion", fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY, 27, Items.DIAMOND, 7);
			BlockPos expandedMainPos = AutomationRuntime.runOnServer(player -> {
				require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(fixture.primaryPos())),
						"The original 27-slot chest menu was not open before expansion");
				placeChest(player.level(), player, fixture.addedPos(), new ItemStack(ModBlocks.CHEST_ITEM.get()));
				List<ChestBlockEntity> parts = List.of(getChest(player.level(), fixture.primaryPos()), getChest(player.level(), fixture.addedPos()));
				ChestBlockEntity main = parts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Expanded chest has no main half"));
				require(parts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
						&& parts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT)
						&& fixture.endpoint().equals(requireEndpoint(main, "expanded open-menu primary"))
						&& main.getStorageWrapper().getInventoryHandler().getSlots() == 54
						&& count(main.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
						"Open-menu expansion did not preserve the linked primary endpoint and contents");
				return main.getBlockPos();
			});
			waitForClosedStorageMenu();
			waitForClientLinkedStorageBlock(expandedMainPos, fixture.endpoint().groupId(), LinkedStorageEndpointRole.PRIMARY);
			openStorageMenu(expandedMainPos, LinkedStorageEndpointRole.PRIMARY);
			waitForStorageMenu("Open Menu Expansion", expandedMainPos, LinkedStorageEndpointRole.PRIMARY, 54, Items.DIAMOND, 7);
		} finally {
			closeStorageMenu();
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.level(), fixture.primaryPos());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
			waitForClientPlayerTransform(fixture.playerState().position(), fixture.playerState().yRot(), fixture.playerState().xRot());
		}
	}

	private static String runLinkedPrimaryChestDropRestore(ServerPlayer player) {
		ServerLevel level = player.level();
		PlayerState playerState = capturePlayerState(player);
		BlockPos firstPos = player.blockPosition().offset(0, 0, 52);
		BlockPos secondPos = firstPos.east();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		clearArea(level, firstPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeChest(level, player, firstPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity primary = getChest(level, firstPos);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
					primary) == LinkedStorageService.LinkResult.SUCCESS, "Could not create the paired primary chest carrier fixture");
			LinkedStorageEndpointData endpoint = requireEndpoint(primary, "paired primary chest");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			placeChest(level, player, secondPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity main = List.of(getChest(level, firstPos), getChest(level, secondPos)).stream().filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Paired primary chest has no main half"));
			player.setGameMode(GameType.SURVIVAL);
			require(player.gameMode.destroyBlock(main.getBlockPos()), "Could not break paired linked primary chest");
			List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(firstPos).inflate(1.5D),
					entity -> entity.getItem().is(ModBlocks.CHEST_ITEM.get()));
			require(drops.size() == 1 && ChestBlockItem.isDoubleChest(drops.getFirst().getItem())
					&& endpoint.equals(drops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)) && StorageBlockEntity
							.getLinkedStorageEndpointRole(drops.getFirst().getItem()).filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
					"Paired linked primary did not drop one primary double-chest carrier");
			ItemStack carrier = drops.getFirst().getItem().copy();
			drops.forEach(ItemEntity::discard);
			placeChest(level, player, firstPos, carrier);
			List<ChestBlockEntity> restored = List.of(firstPos, firstPos.north(), firstPos.south(), firstPos.east(), firstPos.west()).stream()
					.map(level::getBlockEntity).filter(ChestBlockEntity.class::isInstance).map(ChestBlockEntity.class::cast).toList();
			ChestBlockEntity restoredMain = restored.stream().filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Restored paired primary has no main half"));
			require(restored.size() == 2 && restored.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
					&& restored.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT)
					&& endpoint.equals(requireEndpoint(restoredMain, "restored paired primary"))
					&& restoredMain.getStorageWrapper().getInventoryHandler().getSlots() == 54
					&& count(restoredMain.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
					"Primary double-chest carrier did not restore both halves, canonical capacity, and contents");
			return "";
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, firstPos);
			restorePlayerState(player, playerState);
		}
	}

	private static String runLinkedSecondaryChestSplitLifecycle(ServerPlayer player) {
		ServerLevel level = player.level();
		PlayerState playerState = capturePlayerState(player);
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 64);
		BlockPos secondaryPos = primaryPos.east(5);
		BlockPos addedPos = secondaryPos.west();
		BlockPos reverseSecondaryPos = addedPos.east(3);
		BlockPos reverseAddedPos = reverseSecondaryPos.west();
		clearArea(level, primaryPos);
		try {
			placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeChest(level, player, secondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity primary = getChest(level, primaryPos);
			ChestBlockEntity secondary = getChest(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked secondary split fixture");
			LinkedStorageEndpointData endpoint = requireEndpoint(secondary, "secondary split endpoint");
			List<ItemStack> canonicalContents = fillInventoryWithTestContents(primary.getStorageWrapper().getInventoryHandler());
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			int canonicalSlots = primary.getStorageWrapper().getInventoryHandler().getSlots();
			placeChest(level, player, addedPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			List<ChestBlockEntity> parts = List.of(getChest(level, secondaryPos), getChest(level, addedPos));
			ChestBlockEntity main = parts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Expanded linked secondary has no main half"));
			require(main.getBlockPos().equals(secondaryPos) && endpoint.equals(requireEndpoint(main, "expanded linked secondary"))
					&& main.getStorageWrapper().getInventoryHandler().getSlots() == canonicalSlots,
					"Expanding a secondary chest changed canonical capacity or endpoint identity");
			BlockPos remainingPos = main.getBlockPos().equals(secondaryPos) ? addedPos : secondaryPos;
			player.setGameMode(GameType.SURVIVAL);
			ItemStack nonMainTierUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			require(useTierUpgradeAsPlayer(player, addedPos, nonMainTierUpgrade) == InteractionResult.FAIL,
					"Linked secondary double-chest non-main part accepted a tier upgrade");
			require(nonMainTierUpgrade.getCount() == 1 && level.getBlockState(secondaryPos).is(ModBlocks.CHEST.get())
					&& level.getBlockState(addedPos).is(ModBlocks.CHEST.get())
					&& endpoint.equals(requireEndpoint(getChest(level, main.getBlockPos()), "linked secondary main after rejected tier upgrade")),
					"Rejected linked secondary double-chest tier upgrade changed a chest or its endpoint");
			require(player.gameMode.destroyBlock(main.getBlockPos()), "Player break did not remove the linked secondary main chest");
			ChestBlockEntity remaining = getChest(level, remainingPos);
			List<ItemEntity> firstDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(main.getBlockPos()).inflate(1.5D));
			ItemStack firstDrop = firstDrops.size() == 1 ? firstDrops.getFirst().getItem().copy() : ItemStack.EMPTY;
			require(remaining.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE && endpoint.equals(remaining.getLinkedStorageEndpointData())
					&& !remaining.hasCustomName() && hasInventoryContents(primary.getStorageWrapper().getInventoryHandler(), canonicalContents)
					&& firstDrops.size() == 1 && !ChestBlockItem.isDoubleChest(firstDrop)
					&& firstDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null && !firstDrop.has(DataComponents.CUSTOM_NAME),
					"Breaking the linked secondary main chest changed or dropped linked inventory contents");
			firstDrops.forEach(ItemEntity::discard);

			player.setGameMode(GameType.CREATIVE);
			BlockPos shiftedAddedPos = remainingPos.west();
			placeChest(level, player, shiftedAddedPos, firstDrop);
			List<ChestBlockEntity> shiftedParts = List.of(getChest(level, shiftedAddedPos), getChest(level, remainingPos));
			ChestBlockEntity shiftedMain = shiftedParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Shifted linked secondary has no main half"));
			require(shiftedMain.getBlockPos().equals(remainingPos) && endpoint.equals(requireEndpoint(shiftedMain, "shifted linked secondary"))
					&& !shiftedMain.hasCustomName(), "Shifting the linked secondary double chest did not preserve an unnamed endpoint");

			player.setGameMode(GameType.SURVIVAL);
			require(player.gameMode.destroyBlock(remainingPos), "Player break did not remove the shifted linked secondary main chest");
			ChestBlockEntity shiftedRemaining = getChest(level, shiftedAddedPos);
			List<ItemEntity> shiftedDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(remainingPos).inflate(1.5D));
			ItemStack shiftedDrop = shiftedDrops.size() == 1 ? shiftedDrops.getFirst().getItem().copy() : ItemStack.EMPTY;
			require(shiftedRemaining.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
					&& endpoint.equals(shiftedRemaining.getLinkedStorageEndpointData()) && !shiftedRemaining.hasCustomName()
					&& hasInventoryContents(primary.getStorageWrapper().getInventoryHandler(), canonicalContents) && shiftedDrops.size() == 1
					&& !ChestBlockItem.isDoubleChest(shiftedDrop) && shiftedDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
					&& !shiftedDrop.has(DataComponents.CUSTOM_NAME),
					"Breaking the shifted linked secondary main chest changed or dropped linked inventory contents");
			shiftedDrops.forEach(ItemEntity::discard);

			require(player.gameMode.destroyBlock(shiftedAddedPos), "Player break did not remove the shifted linked secondary chest");
			List<ItemEntity> finalDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(shiftedAddedPos).inflate(1.5D),
					entity -> entity.getItem().is(ModBlocks.CHEST_ITEM.get()));
			require(finalDrops.size() == 1 && endpoint.equals(finalDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"The remaining linked secondary single chest did not drop its endpoint carrier");
			finalDrops.forEach(ItemEntity::discard);

			placeChest(level, player, reverseSecondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity reverseSecondary = getChest(level, reverseSecondaryPos);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, reverseSecondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create reverse linked secondary split fixture");
			LinkedStorageEndpointData reverseEndpoint = requireEndpoint(reverseSecondary, "reverse secondary split endpoint");
			placeChest(level, player, reverseAddedPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			List<ChestBlockEntity> reverseParts = List.of(getChest(level, reverseSecondaryPos), getChest(level, reverseAddedPos));
			ChestBlockEntity reverseMain = reverseParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Reverse expanded linked secondary has no main half"));
			BlockPos reverseNonMainPos = reverseMain.getBlockPos().equals(reverseSecondaryPos) ? reverseAddedPos : reverseSecondaryPos;
			require(player.gameMode.destroyBlock(reverseNonMainPos), "Player break did not remove the linked secondary non-main chest");
			List<ItemEntity> reverseDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(reverseNonMainPos).inflate(1.5D));
			require(reverseMain.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
					&& reverseEndpoint.equals(reverseMain.getLinkedStorageEndpointData())
					&& hasInventoryContents(primary.getStorageWrapper().getInventoryHandler(), canonicalContents) && reverseDrops.size() == 1
					&& !ChestBlockItem.isDoubleChest(reverseDrops.getFirst().getItem())
					&& reverseDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null,
					"Breaking the linked secondary non-main chest changed or dropped linked inventory contents");
			return "";
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, primaryPos);
			restorePlayerState(player, playerState);
		}
	}

	public static String setupLinkedLimitedBarrelReloadProjection() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			clearArea(level, LINKED_LIMITED_RELOAD_PRIMARY_POS);
			placeBlock(level, player, LINKED_LIMITED_RELOAD_PRIMARY_POS, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			placeBlock(level, player, LINKED_LIMITED_RELOAD_SECONDARY_POS, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, LINKED_LIMITED_RELOAD_PRIMARY_POS);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, LINKED_LIMITED_RELOAD_SECONDARY_POS);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked limited-barrel reload primary");
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked limited-barrel reload secondary");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, LINKED_LIMITED_RELOAD_ITEM_COUNT));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			WorldHelper.notifyBlockUpdate(primary);
			WorldHelper.notifyBlockUpdate(secondary);
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "limited reload primary");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "limited reload secondary");
			require(hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary),
					"Linked limited barrels did not initialize their render projection before reload");
			return "{\"ok\":true,\"groupId\":\"" + primaryEndpoint.groupId() + "\",\"primaryEndpointId\":\"" + primaryEndpoint.endpointId()
					+ "\",\"secondaryEndpointId\":\"" + secondaryEndpoint.endpointId() + "\"}";
		});
	}

	public static String linkedLimitedBarrelReloadProjectionStatus(UUID groupId) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, LINKED_LIMITED_RELOAD_PRIMARY_POS);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, LINKED_LIMITED_RELOAD_SECONDARY_POS);
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "reloaded limited primary");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "reloaded limited secondary");
			require(primaryEndpoint.groupId().equals(groupId) && secondaryEndpoint.groupId().equals(groupId),
					"Reloaded limited barrels do not belong to the expected linked-storage group");
			require(hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary),
					"Reloaded linked limited barrels did not restore server render projection");
			// The restarted client can resume beyond the persisted fixture chunk, so load it before observing its projection.
			teleportPlayer(player, LINKED_LIMITED_RELOAD_PRIMARY_POS.above().getCenter());
			WorldHelper.notifyBlockUpdate(primary);
			WorldHelper.notifyBlockUpdate(secondary);
			return "";
		});
		waitForClientLimitedBarrelReloadProjection();
		return "{\"ok\":true,\"clientDisplayItems\":true,\"clientCounts\":true,\"clientFillLevels\":true}";
	}

	public static String setupDroppedItemPickupInspection() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 4);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			try {
				player.setGameMode(GameType.CREATIVE);
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create dropped-item pickup inspection linked storage");
				return "{\"ok\":true,\"primary\":\"" + primaryPos + "\",\"secondary\":\"" + secondaryPos + "\"}";
			} finally {
				player.setGameMode(originalGameMode);
			}
		});
	}

	private static String runOrdinaryLinkedStorage(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 4);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			require(primary.isLinkedStorageCandidate() && secondary.isLinkedStorageCandidate(), "Ordinary barrels were not link candidates");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link ordinary barrels");
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "primary barrel");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "secondary barrel");
			IItemHandler secondaryCapability = level.getCapability(Capabilities.ItemHandler.BLOCK, secondaryPos, Direction.UP);
			require(secondaryCapability != null && secondaryCapability.insertItem(1, new ItemStack(Items.EMERALD, 3), false).isEmpty()
					&& primaryEndpoint.groupId().equals(secondaryEndpoint.groupId())
					&& count(primary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
					&& count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
					&& count(primary.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3,
					"Linked barrel endpoints did not expose one canonical inventory through capabilities");
			return "";
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, primaryPos);
		}
	}

	private static String runCanonicalStorageTypeInsertionRules(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos shulkerPrimaryPos = player.blockPosition().offset(0, 0, 4);
		BlockPos barrelSecondaryPos = shulkerPrimaryPos.east(3);
		BlockPos barrelPrimaryPos = shulkerPrimaryPos.south(3);
		BlockPos shulkerSecondaryPos = barrelPrimaryPos.east(3);
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		clearArea(level, shulkerPrimaryPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeBlock(level, player, shulkerPrimaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			placeBlock(level, player, barrelSecondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			StorageBlockEntity shulkerPrimary = levelStorage(level, shulkerPrimaryPos);
			require(linkWithPlayer(player, shulkerPrimary, shulkerPrimaryPos, barrelSecondaryPos), "Could not create Shulker-primary insertion-rules fixture");
			IItemHandler barrelSecondaryCapability = level.getCapability(Capabilities.ItemHandler.BLOCK, barrelSecondaryPos, Direction.UP);
			require(barrelSecondaryCapability != null, "Physical barrel secondary did not expose an item capability");
			ItemStack rejectedShulker = barrelSecondaryCapability.insertItem(0, new ItemStack(Items.SHULKER_BOX), false);
			require(rejectedShulker.is(Items.SHULKER_BOX) && rejectedShulker.getCount() == 1
					&& count(shulkerPrimary.getStorageWrapper().getInventoryHandler(), Items.SHULKER_BOX) == 0,
					"Physical barrel secondary bypassed the canonical Shulker insertion rules");

			placeBlock(level, player, barrelPrimaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, shulkerSecondaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			StorageBlockEntity barrelPrimary = levelStorage(level, barrelPrimaryPos);
			require(linkWithPlayer(player, barrelPrimary, barrelPrimaryPos, shulkerSecondaryPos), "Could not create barrel-primary insertion-rules fixture");
			IItemHandler shulkerSecondaryCapability = level.getCapability(Capabilities.ItemHandler.BLOCK, shulkerSecondaryPos, Direction.UP);
			require(shulkerSecondaryCapability != null, "Physical Shulker secondary did not expose an item capability");
			ItemStack insertedShulker = shulkerSecondaryCapability.insertItem(0, new ItemStack(Items.SHULKER_BOX), false);
			require(insertedShulker.isEmpty() && count(barrelPrimary.getStorageWrapper().getInventoryHandler(), Items.SHULKER_BOX) == 1,
					"Physical Shulker secondary applied its local insertion rules instead of the canonical barrel rules");
			return "";
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, shulkerPrimaryPos);
		}
	}

	private static String runLinkedControllerCanonicalContents(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 4);
		BlockPos existingEndpointPos = controllerPos.east();
		BlockPos joiningEndpointPos = existingEndpointPos.east();
		clearArea(level, controllerPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			placeBlock(level, player, existingEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, joiningEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
					.orElseThrow(() -> new IllegalStateException("Missing controller for linked endpoint join regression"));
			BarrelBlockEntity existingEndpoint = getBarrel(level, existingEndpointPos);
			BarrelBlockEntity joiningEndpoint = getBarrel(level, joiningEndpointPos);
			int canonicalSlots = existingEndpoint.getStorageWrapper().getInventoryHandler().getSlots();
			require(controller.getSlots() == canonicalSlots * 2, "Controller did not register both unlinked barrels before linking");
			require(linkWithPlayer(player, existingEndpoint, existingEndpointPos, joiningEndpointPos),
					"Could not create controller-connected linked barrel group");
			existingEndpoint.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			existingEndpoint.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(controller.getStoragePositions().size() == 1 && controller.getSlots() == canonicalSlots
					&& controller.extractItem(new ItemStack(Items.DIAMOND, 64), true).getCount() == 7 && controller.getStackStorages(diamondKey).size() == 1,
					"Controller duplicated canonical contents when a connected endpoint joined a linked group");
			require(controller.insertItem(new ItemStack(Items.EMERALD, 3), false).isEmpty()
					&& controller.extractItem(new ItemStack(Items.EMERALD, 2), false).getCount() == 2
					&& controller.getHighlightStoragePositions(controller.getStackStorages(diamondKey))
							.containsAll(Set.of(existingEndpointPos, joiningEndpointPos))
					&& controller.getStorageBlockPositions().containsAll(Set.of(existingEndpointPos, joiningEndpointPos)),
					"Linked controller did not route items and highlight every physical member");
			controller.toggleLock();
			require(existingEndpoint.isLocked() && joiningEndpoint.isLocked(), "Controller lock did not fan out to linked members");
			controller.toggleLock();
			return "";
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, controllerPos);
		}
	}

	private static void runLegacyControllerStorageKeysMigrationRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 156);
			BlockPos firstStoragePos = controllerPos.east();
			BlockPos secondStoragePos = firstStoragePos.east();
			clearArea(level, controllerPos);
			try {
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				placeBlock(level, player, firstStoragePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondStoragePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing controller before legacy storage key migration"));
				getBarrel(level, secondStoragePos).getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
				getBarrel(level, secondStoragePos).getStorageWrapper().getInventoryHandler().saveInventory();
				CompoundTag legacyData = controller.getUpdateTag(level.registryAccess());
				ListTag legacyStoragePositions = new ListTag();
				legacyStoragePositions.add(LongTag.valueOf(firstStoragePos.asLong()));
				legacyStoragePositions.add(LongTag.valueOf(secondStoragePos.asLong()));
				legacyData.put("storagePositions", legacyStoragePositions);
				legacyData.remove("storageKeys");
				legacyData.remove("storageMemberKeys");
				level.removeBlockEntity(controllerPos);
				ControllerBlockEntity migratedController = new ControllerBlockEntity(controllerPos, level.getBlockState(controllerPos));
				migratedController.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), legacyData));
				level.setBlockEntity(migratedController);
				migratedController.onLoad();
				require(migratedController.getStoragePositions().equals(List.of(firstStoragePos, secondStoragePos))
						&& migratedController.getStackStorages(ItemStackKey.of(new ItemStack(Items.DIAMOND))).contains(secondStoragePos),
						"Absent storageKeys did not migrate every legacy storagePosition into rebuilt controller routing indexes");
				CompoundTag explicitEmptyKeysData = legacyData.copy();
				explicitEmptyKeysData.put("storageKeys", new ListTag());
				ControllerBlockEntity explicitEmptyKeysController = new ControllerBlockEntity(controllerPos, level.getBlockState(controllerPos));
				explicitEmptyKeysController.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), explicitEmptyKeysData));
				require(explicitEmptyKeysController.getStoragePositions().isEmpty(), "Present empty storageKeys incorrectly migrated legacy storagePositions");
				return "";
			} finally {
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedLimitedBarrelSecondaryMemorySync() {
		LimitedMemoryFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 112);
			BlockPos secondaryPos = primaryPos.east(3);
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create limited-barrel secondary memory fixture");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).selectSlots(0, 1);
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, ItemStack.EMPTY);
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			LinkedStorageEndpointData endpoint = requireEndpoint(primary, "limited memory primary");
			CompoundTag canonical = LinkedStorageGroupsSavedData.get(level).manager().resolveContents(endpoint.groupId())
					.orElseThrow(() -> new IllegalStateException("Limited memory canonical contents disappeared")).getContents(endpoint.groupId());
			require(canonical.getCompoundOrEmpty("settings").equals(primary.getStorageWrapper().getSettingsHandler().getNbt()),
					"Limited-barrel memory was not persisted to canonical contents");
			teleportPlayer(player, primaryPos.getCenter());
			return new LimitedMemoryFixture(primaryPos, secondaryPos, playerState);
		});
		try {
			waitForClientPlayerPosition(fixture.primaryPos().getCenter());
			openLimitedBarrelMenu(fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			waitForClientMemorizedStack(fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			closeStorageMenu();
			openLimitedBarrelMenu(fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			waitForClientMemorizedStack(fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
		} finally {
			closeStorageMenu();
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.level(), fixture.primaryPos());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
			waitForClientPlayerTransform(fixture.playerState().position(), fixture.playerState().yRot(), fixture.playerState().xRot());
		}
	}

	private static void runDroppedLinkedStorageTooltipCache() {
		DroppedTooltipFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 124);
			int inventorySlot = 8;
			ItemStack originalStack = player.getInventory().getItem(inventorySlot).copy();
			clearArea(level, primaryPos);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
					primary) == LinkedStorageService.LinkResult.SUCCESS, "Could not create dropped tooltip/cache fixture");
			LinkedStorageEndpointData endpoint = requireEndpoint(primary, "dropped tooltip primary");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			level.destroyBlock(primaryPos, true, player);
			ItemStack dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos).inflate(1.5D)).stream()
					.filter(entity -> endpoint.equals(entity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst()
					.map(entity -> entity.getItem().copy()).orElseThrow(() -> new IllegalStateException("Missing dropped linked tooltip carrier"));
			level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos).inflate(1.5D)).forEach(ItemEntity::discard);
			player.getInventory().setItem(inventorySlot, dropped);
			player.inventoryMenu.broadcastChanges();
			return new DroppedTooltipFixture(primaryPos, endpoint, inventorySlot, originalStack);
		});
		try {
			waitForClientStack(fixture);
			require(AutomationRuntime.runOnClient(() -> {
				ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot());
				if (!(stack.getItem() instanceof WoodStorageBlockItem item)
						|| !(item.getTooltipImage(stack).orElse(null) instanceof LinkedStorageTooltip tooltip)) {
					return false;
				}
				return tooltip.role() == LinkedStorageEndpointRole.PRIMARY && tooltip.groupId().equals(fixture.endpoint().groupId())
						&& fixture.endpoint().equals(stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
						&& StorageBlockEntity.getLinkedStorageEndpointRole(stack).filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent();
			}), "Dropped linked primary did not expose its canonical LinkedStorageTooltip identity");
			waitForClientTooltipCache(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().setItem(fixture.inventorySlot(), fixture.originalStack());
				player.inventoryMenu.broadcastChanges();
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void runSecondaryPickupSuppression() {
		PickupFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 136);
			BlockPos secondaryPos = primaryPos.east(MAX_MAGNET_RANGE + 1);
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			clearArea(level, secondaryPos);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create secondary pickup-suppression fixture");
			configureMagnet(primary, Items.ENDER_PEARL);
			require(primary.getBlockState().getValue(StorageBlockBase.TICKING)
					&& secondary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.SECONDARY).isPresent()
					&& !secondary.getBlockState().getValue(StorageBlockBase.TICKING), "Linked secondary incorrectly became a physical pickup ticker");
			Vec3 dropPos = secondaryPos.getCenter().add(0, -0.25D, 0);
			ItemEntity dropped = new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, new ItemStack(Items.ENDER_PEARL, 2));
			dropped.setDeltaMovement(Vec3.ZERO);
			dropped.setNoGravity(true);
			dropped.setPickUpDelay(0);
			require(level.addFreshEntity(dropped), "Could not add secondary pickup-suppression item");
			Vec3 tickingPosition = primaryPos.getCenter().add((MAX_MAGNET_RANGE + 1) / 2.0D, 0, 0);
			teleportPlayer(player, tickingPosition);
			return new PickupFixture(primaryPos, secondaryPos, dropped.getUUID(), level.getGameTime() + 20, tickingPosition, playerState);
		});
		try {
			waitForClientPlayerTransform(fixture.tickingPosition(), fixture.playerState().yRot(), fixture.playerState().xRot());
			waitForServerCondition("secondary pickup suppression", player -> player.level().getGameTime() >= fixture.verifyAfterTick()
					&& player.level().getEntity(fixture.itemEntityId()) instanceof ItemEntity item && item.getItem().getCount() == 2);
			AutomationRuntime.runOnServer(player -> {
				ItemEntity item = (ItemEntity) player.level().getEntity(fixture.itemEntityId());
				item.setPos(fixture.primaryPos().getCenter().add(0, -0.25D, 0));
				return true;
			});
			waitForServerCondition("primary pickup after secondary suppression", player -> player.level().getEntity(fixture.itemEntityId()) == null
					&& count(getBarrel(player.level(), fixture.primaryPos()).getStorageWrapper().getInventoryHandler(), Items.ENDER_PEARL) == 2);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.level(), fixture.primaryPos());
				clearArea(player.level(), fixture.secondaryPos());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
			waitForClientPlayerTransform(fixture.playerState().position(), fixture.playerState().yRot(), fixture.playerState().xRot());
		}
	}

	private static String runLinkedControllerTierUpgrade(ServerPlayer player) {
		ServerLevel level = player.level();
		PlayerState playerState = capturePlayerState(player);
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 76);
		BlockPos primaryPos = controllerPos.east();
		BlockPos secondaryPos = primaryPos.east();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		clearArea(level, controllerPos);
		try {
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not create controller tier-upgrade linked group");
			placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			player.setGameMode(GameType.SURVIVAL);
			ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
			require(player.gameMode.useItemOn(player, level, upgrade, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false)).consumesAction() && upgrade.isEmpty(),
					"Controller did not consume the linked primary tier upgrade");
			ControllerBlockEntity controller = getController(level, controllerPos);
			int expectedSlots = ModBlocks.DIAMOND_BARREL.get().getNumberOfInventorySlots();
			require(controller.getStoragePositions().equals(List.of(primaryPos)) && controller.getSlots() == expectedSlots
					&& getBarrel(level, primaryPos).getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots
					&& getBarrel(level, secondaryPos).getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots
					&& controller.insertItem(new ItemStack(Items.AMETHYST_SHARD), false).isEmpty(),
					"Controller tier upgrade did not resize and route through one canonical linked inventory");
			return "";
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, controllerPos);
			restorePlayerState(player, playerState);
		}
	}

	private static String runLinkedControllerTierUpgradeRequiresPrimary(ServerPlayer player) {
		ServerLevel level = player.level();
		PlayerState playerState = capturePlayerState(player);
		BlockPos secondaryControllerPos = player.blockPosition().offset(0, 0, 88);
		BlockPos secondaryPos = secondaryControllerPos.east();
		BlockPos primaryPos = secondaryControllerPos.south(5);
		BlockPos primaryControllerPos = primaryPos.west();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		clearArea(level, secondaryControllerPos);
		try {
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not create controller primary-candidate fixture");
			placeBlock(level, player, secondaryControllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			require(getController(level, secondaryControllerPos).getStoragePositions().equals(List.of(secondaryPos)),
					"Secondary-only controller unexpectedly selected the remote primary endpoint");
			player.setGameMode(GameType.SURVIVAL);
			ItemStack rejected = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, rejected);
			player.gameMode.useItemOn(player, level, rejected, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(secondaryControllerPos), Direction.UP, secondaryControllerPos, false));
			require(rejected.getCount() == 1 && level.getBlockState(secondaryPos).is(ModBlocks.BARREL.get()),
					"Controller upgraded a linked group without a connected primary candidate");
			level.destroyBlock(secondaryControllerPos, false, player);
			placeBlock(level, player, primaryControllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			ItemStack accepted = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, accepted);
			require(player.gameMode
					.useItemOn(player, level, accepted, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(primaryControllerPos), Direction.UP, primaryControllerPos, false))
					.consumesAction() && accepted.isEmpty() && level.getBlockState(primaryPos).is(ModBlocks.DIAMOND_BARREL.get()),
					"Controller did not upgrade after gaining a connected primary candidate");
			return "";
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, secondaryControllerPos);
			restorePlayerState(player, playerState);
		}
	}

	private static String runLinkedControllerPaintbrushVisibilityAndRestoration(ServerPlayer player) {
		ServerLevel level = player.level();
		PlayerState playerState = capturePlayerState(player);
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 100);
		BlockPos primaryPos = controllerPos.east();
		BlockPos secondaryPos = primaryPos.east();
		ItemStack originalInventoryStack = player.getInventory().getItem(0).copy();
		clearArea(level, controllerPos);
		try {
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not create controller paint/visibility fixture");
			placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			ControllerBlockEntity controller = getController(level, controllerPos);
			controller.toggleLock();
			require(primary.isLocked() && secondary.isLocked(), "Controller lock did not fan out before lock-visibility assertions");
			boolean primaryLockVisibility = primary.shouldShowLock();
			boolean secondaryLockVisibility = secondary.shouldShowLock();
			boolean primaryTierVisibility = primary.shouldShowTier();
			boolean secondaryTierVisibility = secondary.shouldShowTier();
			boolean primaryUpgradeVisibility = primary.shouldShowUpgrades();
			boolean secondaryUpgradeVisibility = secondary.shouldShowUpgrades();
			controller.toggleLockVisibility();
			controller.toggleTierVisiblity();
			controller.toggleUpgradesVisiblity();
			require(primary.shouldShowLock() != primaryLockVisibility && secondary.shouldShowLock() != secondaryLockVisibility
					&& primary.shouldShowTier() != primaryTierVisibility && secondary.shouldShowTier() != secondaryTierVisibility
					&& primary.shouldShowUpgrades() != primaryUpgradeVisibility && secondary.shouldShowUpgrades() != secondaryUpgradeVisibility,
					"Controller visibility operations did not fan out to both linked physical endpoints");

			ItemStack paintbrush = new ItemStack(ModItems.PAINTBRUSH.get());
			PaintbrushItem.setMainColor(paintbrush, 0xFFFF0000);
			PaintbrushItem.ItemRequirements requirements = PaintbrushItem.getItemRequirements(paintbrush, player, level, controllerPos)
					.orElseThrow(() -> new IllegalStateException("Linked controller paintbrush requirements were missing"));
			int requiredDyes = requirements.itemsPresent().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum()
					+ requirements.itemsMissing().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum();
			require(requiredDyes == 2, "Paintbrush requirements did not include both linked physical endpoints");
			player.getInventory().setItem(0, new ItemStack(Items.RED_DYE, 2));
			require(paintbrush.getItem().onItemUseFirst(paintbrush,
					new UseOnContext(player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false))) == InteractionResult.SUCCESS
					&& primary.getStorageWrapper().getMainColor() == 0xFFFF0000 && secondary.getStorageWrapper().getMainColor() == 0xFFFF0000,
					"Controller paintbrush did not update both linked physical endpoints");

			level.destroyBlock(controllerPos, false, player);
			require(primary.getControllerPos().isEmpty() && secondary.getControllerPos().isEmpty(),
					"Controller removal left linked physical membership behind");
			placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			ControllerBlockEntity restored = getController(level, controllerPos);
			require(restored.getStoragePositions().equals(List.of(primaryPos))
					&& restored.getStorageBlockPositions().containsAll(Set.of(primaryPos, secondaryPos))
					&& primary.getControllerPos().filter(controllerPos::equals).isPresent()
					&& secondary.getControllerPos().filter(controllerPos::equals).isPresent(),
					"Restored controller did not recover one logical primary and both physical memberships");
			return "";
		} finally {
			player.getInventory().setItem(0, originalInventoryStack);
			clearArea(level, controllerPos);
			restorePlayerState(player, playerState);
		}
	}

	private static String runLinkedDoubleChestControllerCanonicalContents(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 4);
		BlockPos primaryLeftChestPos = controllerPos.east();
		BlockPos primaryMainChestPos = primaryLeftChestPos.east();
		BlockPos secondaryLeftChestPos = controllerPos.south();
		BlockPos secondaryMainChestPos = secondaryLeftChestPos.east();
		clearArea(level, controllerPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			placeBlockWithPlayer(level, player, primaryLeftChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeBlockWithPlayer(level, player, primaryMainChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeBlockWithPlayer(level, player, secondaryLeftChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeBlockWithPlayer(level, player, secondaryMainChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
					.orElseThrow(() -> new IllegalStateException("Missing controller for linked double-chest regression"));
			ChestBlockEntity primary = getChest(level, primaryMainChestPos);
			ChestBlockEntity secondary = getChest(level, secondaryMainChestPos);
			require(isDoubleChest(level, primaryLeftChestPos, primaryMainChestPos) && isDoubleChest(level, secondaryLeftChestPos, secondaryMainChestPos),
					"Player placement did not create both linked-storage double chests");
			require(linkWithPlayer(player, primary, primaryMainChestPos, secondaryMainChestPos), "Could not link player-placed double chests");
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "primary double chest");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "secondary double chest");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(primaryEndpoint.groupId().equals(secondaryEndpoint.groupId())
					&& count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7 && controller.getStoragePositions().size() == 1
					&& controller.getStoragePositions().contains(primaryMainChestPos) && controller.getSlots() == 54
					&& controller.extractItem(new ItemStack(Items.DIAMOND, 64), true).getCount() == 7 && controller.getStackStorages(diamondKey).size() == 1
					&& controller.getStorageBlockPositions().containsAll(Set.of(primaryMainChestPos, secondaryMainChestPos)),
					"Linked double chests did not share one canonical controller storage");
			return "";
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, controllerPos);
		}
	}

	private static void waitForClientLimitedBarrelReloadProjection() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null
					&& Minecraft.getInstance().level.getBlockEntity(LINKED_LIMITED_RELOAD_PRIMARY_POS) instanceof LimitedBarrelBlockEntity primary
					&& Minecraft.getInstance().level.getBlockEntity(LINKED_LIMITED_RELOAD_SECONDARY_POS) instanceof LimitedBarrelBlockEntity secondary
					&& hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Reloaded linked limited barrels did not synchronize display items, counts, and fill levels to the client: "
				+ AutomationRuntime.runOnClient(StorageLinkedStorageRegression::describeClientLimitedBarrelState));
	}

	private static String describeClientLimitedBarrelState() {
		if (Minecraft.getInstance().level == null) {
			return "level unavailable";
		}
		return describeClientLimitedBarrel(Minecraft.getInstance().level.getBlockEntity(LINKED_LIMITED_RELOAD_PRIMARY_POS)) + "; "
				+ describeClientLimitedBarrel(Minecraft.getInstance().level.getBlockEntity(LINKED_LIMITED_RELOAD_SECONDARY_POS));
	}

	private static String describeClientLimitedBarrel(Object blockEntity) {
		if (!(blockEntity instanceof LimitedBarrelBlockEntity barrel)) {
			return String.valueOf(blockEntity);
		}
		return "endpoint=" + barrel.getLinkedStorageEndpointData() + ", displays="
				+ barrel.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems() + ", counts=" + barrel.getSlotCounts() + ", fills="
				+ barrel.getSlotFillLevels();
	}

	private static boolean hasLimitedRenderProjection(LimitedBarrelBlockEntity barrel) {
		return barrel.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().stream()
				.anyMatch(displayItem -> displayItem.getItem().is(Items.DIAMOND)) && barrel.getSlotCounts().contains(LINKED_LIMITED_RELOAD_ITEM_COUNT)
				&& barrel.getSlotFillLevels().stream().anyMatch(fillLevel -> fillLevel > 0F);
	}

	private static LinkedStorageEndpointData requireEndpoint(StorageBlockEntity storage, String name) {
		LinkedStorageEndpointData endpoint = storage.getLinkedStorageEndpointData();
		if (endpoint == null) {
			throw new IllegalStateException("Missing linked-storage endpoint data for " + name);
		}
		return endpoint;
	}

	private static BarrelBlockEntity getBarrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing barrel at " + pos));
	}

	private static LimitedBarrelBlockEntity getLimitedBarrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing limited barrel at " + pos));
	}

	private static ChestBlockEntity getChest(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing chest at " + pos));
	}

	private static ControllerBlockEntity getController(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing controller at " + pos));
	}

	private static void placeBlock(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			throw new IllegalStateException("Regression fixture item is not placeable at " + pos);
		}
		level.setBlock(pos, blockItem.getBlock().defaultBlockState(), 3);
		blockItem.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), player, stack);
	}

	private static void placeBlockWithPlayer(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		BlockPos supportPos = pos.below();
		level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
		player.setYRot(0);
		player.setXRot(0);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		require(player.gameMode
				.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false))
				.consumesAction(), "Player placement did not consume the interaction at " + pos);
	}

	private static void placeChest(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		placeBlockWithPlayer(level, player, pos, stack);
	}

	private static InteractionResult useTierUpgrade(ServerPlayer player, BlockPos pos, ItemStack upgrade) {
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return upgrade.getItem().onItemUseFirst(upgrade, new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
	}

	private static InteractionResult useTierUpgradeAsPlayer(ServerPlayer player, BlockPos pos, ItemStack upgrade) {
		player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return player.gameMode.useItemOn(player, player.level(), upgrade, InteractionHand.MAIN_HAND, hitResult);
	}

	private static void openStorageMenu(BlockPos pos, LinkedStorageEndpointRole expectedRole) {
		AutomationRuntime.runOnServer(player -> {
			StorageBlockEntity storage = levelStorage(player.level(), pos);
			teleportPlayer(player, pos.getCenter());
			SimpleMenuProvider provider = new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new StorageContainerMenu(windowId, menuPlayer, pos),
					storage.getMenuDisplayName());
			require(player.openMenu(provider, buffer -> StorageContainerMenu.writeMenuData(buffer, player, pos)).isPresent(),
					"Branch-native SimpleMenuProvider did not open linked storage at " + pos);
			require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(pos))
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
					&& endpointProvider.getLinkedStorageEndpointRole().filter(role -> role == expectedRole).isPresent(),
					"Server linked storage menu did not retain its opened endpoint role");
			return true;
		});
	}

	private static StorageBlockEntity levelStorage(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof StorageBlockEntity storage)) {
			throw new IllegalStateException("Missing linked storage at " + pos);
		}
		return storage;
	}

	private static void openLimitedBarrelMenu(BlockPos pos, LinkedStorageEndpointRole expectedRole) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			teleportPlayer(player, pos.getCenter());
			require(level.getBlockState(pos).getBlock() instanceof LimitedBarrelBlock, "Expected Limited Barrel I at " + pos);
			ItemStack emptyHand = ItemStack.EMPTY;
			player.setItemInHand(InteractionHand.MAIN_HAND, emptyHand);
			require(player.gameMode.useItemOn(player, level, emptyHand, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5D, 0), Direction.UP, pos, false)).consumesAction(),
					"Could not open linked Limited Barrel I at " + pos);
			require(player.containerMenu instanceof LimitedBarrelContainerMenu menu && menu.getBlockPosition().equals(Optional.of(pos))
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
					&& endpointProvider.getLinkedStorageEndpointRole().filter(role -> role == expectedRole).isPresent(),
					"Limited Barrel I server menu did not retain its endpoint role");
			return true;
		});
	}

	private static void closeStorageMenu() {
		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			return true;
		});
	}

	private static void waitForStorageMenu(String title, BlockPos pos, LinkedStorageEndpointRole role, int slots, Item item, int count) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getTitle().getString().equals(title) && screen.getMenu() instanceof StorageContainerMenu menu
					&& menu.getBlockPosition().equals(Optional.of(pos)) && menu.getNumberOfStorageInventorySlots() == slots
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
					&& endpointProvider.getLinkedStorageEndpointRole().filter(endpointRole -> endpointRole == role).isPresent()
					&& count(menu, item) == count)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for linked storage menu at " + pos + " with " + slots + " slots");
	}

	private static void waitForClosedStorageMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(player -> player.containerMenu == player.inventoryMenu)
					&& AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen == null)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Expanded storage left a stale server or client menu open");
	}

	private static void waitForClientMemorizedStack(BlockPos pos, LinkedStorageEndpointRole role) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof LimitedBarrelContainerMenu menu
					&& menu.getBlockPosition().equals(Optional.of(pos)) && menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
					&& endpointProvider.getLinkedStorageEndpointRole().filter(endpointRole -> endpointRole == role).isPresent()
					&& menu.getMemorizedStackInSlot(0).filter(stack -> stack.is(Items.DIAMOND)).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked limited-barrel " + role + " menu did not receive canonical memory");
	}

	private static void waitForClientStack(DroppedTooltipFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& StorageBlockEntity.getLinkedStorageEndpointData(Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot()))
							.filter(fixture.endpoint()::equals).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Dropped linked storage carrier did not synchronize to the client inventory");
	}

	private static void waitForClientTooltipCache(DroppedTooltipFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null
						|| ClientLinkedStorageContents.getRevision(fixture.endpoint().groupId()).isEmpty()) {
					return false;
				}
				StackStorageWrapper wrapper = StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(),
						Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot()));
				return count(wrapper.getInventoryHandler(), Items.DIAMOND) == 7;
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Dropped linked storage tooltip did not populate canonical client cache");
	}

	private static void configureMagnet(BarrelBlockEntity primary, Item item) {
		UpgradeHandler upgrades = primary.getStorageWrapper().getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(ModItems.MAGNET_UPGRADE.get()));
		MagnetUpgradeWrapper magnet = upgrades.getWrappersThatImplement(MagnetUpgradeWrapper.class).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Magnet upgrade was not initialized"));
		magnet.setPickupItems(true);
		magnet.setPickupXp(false);
		magnet.getFilterLogic().setDepositFilterType(ContentsFilterType.ALLOW);
		magnet.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(item));
		upgrades.saveInventory();
	}

	private static void waitForClientPlayerPosition(Vec3 expected) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.position().distanceToSqr(expected) < 0.25D)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client player did not reach linked storage fixture");
	}

	private static void waitForClientPlayerTransform(Vec3 expectedPosition, float expectedYRot, float expectedXRot) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				var player = Minecraft.getInstance().player;
				return player != null && player.position().distanceToSqr(expectedPosition) < 0.25D
						&& Math.abs(Math.IEEEremainder(player.getYRot() - expectedYRot, 360D)) < 0.1D
						&& Math.abs(Math.IEEEremainder(player.getXRot() - expectedXRot, 360D)) < 0.1D;
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client player did not synchronize linked storage fixture transform");
	}

	private static void teleportPlayer(ServerPlayer player, Vec3 position) {
		require(player.teleportTo(player.level(), position.x, position.y, position.z, Set.of(), player.getYRot(), player.getXRot(), false),
				"Could not teleport player to linked storage fixture");
	}

	private static PlayerState capturePlayerState(ServerPlayer player) {
		return new PlayerState(player.position(), player.getYRot(), player.getXRot(), player.getInventory().getSelectedSlot(), player.getMainHandItem().copy());
	}

	private static void restorePlayerState(ServerPlayer player, PlayerState state) {
		player.getInventory().setSelectedSlot(state.selectedSlot());
		player.setItemInHand(InteractionHand.MAIN_HAND, state.mainHandStack().copy());
		player.inventoryMenu.broadcastChanges();
		player.setYRot(state.yRot());
		player.setXRot(state.xRot());
		teleportPlayer(player, state.position());
	}

	private static void waitForServerCondition(String description, java.util.function.Function<ServerPlayer, Boolean> condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(condition)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static InteractionResult useLinkerAsPlayer(ServerPlayer player, ItemStack linker, BlockPos pos) {
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		return player.gameMode.useItemOn(player, player.level(), linker, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	private static boolean linkWithPlayer(ServerPlayer player, StorageBlockEntity primary, BlockPos primaryPos, BlockPos secondaryPos) {
		int selectedSlot = player.getInventory().getSelectedSlot();
		ItemStack selectedStack = player.getMainHandItem().copy();
		try {
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			if (!useLinkerAsPlayer(player, linker, primaryPos).consumesAction()) {
				return false;
			}
			UUID groupId = requireEndpoint(primary, "player-linked primary").groupId();
			ItemStack boundLinker = ItemStack.EMPTY;
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (stack.is(ENDER_LINKER.get()) && stack.get(ModCoreDataComponents.ENDER_LINKER_TARGET) != null
						&& groupId.equals(stack.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId())) {
					boundLinker = stack;
					break;
				}
			}
			return !boundLinker.isEmpty() && useLinkerAsPlayer(player, boundLinker, secondaryPos).consumesAction();
		} finally {
			player.getInventory().setSelectedSlot(selectedSlot);
			player.setItemInHand(InteractionHand.MAIN_HAND, selectedStack);
			player.inventoryMenu.broadcastChanges();
		}
	}

	private static int count(IItemHandler handler, Item item) {
		int count = 0;
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static List<ItemStack> fillInventoryWithTestContents(InventoryHandler handler) {
		List<ItemStack> contents = new ArrayList<>();
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = new ItemStack(Items.DIAMOND, slot + 1);
			handler.setStackInSlot(slot, stack);
			contents.add(stack.copy());
		}
		return contents;
	}

	private static boolean hasInventoryContents(IItemHandler handler, List<ItemStack> contents) {
		if (handler.getSlots() != contents.size()) {
			return false;
		}
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			if (!ItemStack.matches(handler.getStackInSlot(slot), contents.get(slot))) {
				return false;
			}
		}
		return true;
	}

	private static int count(StorageContainerMenu menu, Item item) {
		int count = 0;
		for (int slot = 0; slot < menu.getNumberOfStorageInventorySlots(); slot++) {
			ItemStack stack = menu.getSlot(slot).getItem();
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static boolean isDoubleChest(ServerLevel level, BlockPos leftChestPos, BlockPos mainChestPos) {
		return level.getBlockState(leftChestPos).is(ModBlocks.CHEST.get()) && level.getBlockState(mainChestPos).is(ModBlocks.CHEST.get())
				&& level.getBlockState(leftChestPos).getValue(ChestBlock.TYPE) == ChestType.LEFT
				&& level.getBlockState(mainChestPos).getValue(ChestBlock.TYPE) == ChestType.RIGHT
				&& level.getBlockEntity(leftChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(chest -> chest.getMainPos().equals(mainChestPos))
						.orElse(false)
				&& level.getBlockEntity(mainChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(chest -> chest.getMainPos().equals(mainChestPos))
						.orElse(false);
	}

	private static void clearArea(ServerLevel level, BlockPos center) {
		for (int x = -2; x <= 8; x++) {
			for (int y = -1; y <= 2; y++) {
				for (int z = -2; z <= 10; z++) {
					BlockPos pos = center.offset(x, y, z);
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
						storage.clearContent();
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(12.0D)).forEach(ItemEntity::discard);
		for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
			for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
				level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for linked storage client state", e);
		}
	}

	private static String jsonString(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
	}

	private record LinkedStorageMenuFixture(BlockPos anchor, BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, int inventorySlots, String title,
			PlayerState playerState) {
	}

	private record OpenMenuExpansionFixture(BlockPos primaryPos, BlockPos addedPos, LinkedStorageEndpointData endpoint, PlayerState playerState) {
	}

	private record PlayerState(Vec3 position, float yRot, float xRot, int selectedSlot, ItemStack mainHandStack) {
	}

	private record LimitedMemoryFixture(BlockPos primaryPos, BlockPos secondaryPos, PlayerState playerState) {
	}

	private record DroppedTooltipFixture(BlockPos primaryPos, LinkedStorageEndpointData endpoint, int inventorySlot, ItemStack originalStack) {
	}

	private record PickupFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID itemEntityId, long verifyAfterTick, Vec3 tickingPosition,
			PlayerState playerState) {
	}

}
