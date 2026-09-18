package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.devclientautomation.platform.neoforge.NeoForgeModelDiagnostics;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ClientStorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.*;
import net.p3pp3rf1y.sophisticatedstorage.network.OpenStorageInventoryPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

public final class StorageLinkedStorageRegression {
	private static final int FIXTURE_PLATFORM_RADIUS = 15;
	private static final int LINKED_LIMITED_RELOAD_ITEM_COUNT = 23;

	private StorageLinkedStorageRegression() {
	}

	public static String run() {
		try {
			recoverClientPlayerForFixtures();
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::preparePlayerForFixtures);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runOrdinaryLinkedStorage);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runCanonicalStorageTypeInsertionRules);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedShulkerStashAndFamilyCompatibility);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runStorageFamilyCompatibilityRegression);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerCanonicalContents);
			runDroppedPrimaryRenameRegression();
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedDoubleChestControllerCanonicalContents);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedDoubleChestLifecycle);
			runCreativeEndpointPlacementRegression();
			runLinkedControllerNonListenerRemovalIndexesRegression();
			runComponentlessBarrelItemModelRegression();
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runEndpointUnloadReloadRegression);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerTopologyAndLockedRoutingRegression);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedStorageTierUpgradeRegression);
			runLinkedStorageMenuSnapshotRegression();
			runLinkedUpgradeRefreshRegression();
			runLinkedStorageMenuTransitionRegression();
			runLinkedPrimaryChestExpansionRegression();
			runLinkedLimitedBarrelMemorizedSlotMenuRegression();
			runLinkedControllerClientOutlineRegression();
			runTierUpgradeMenuInvalidationRegression();
			StoragePreviewScenarios.runOpenSettingsSelectionRegression();
			runSecondaryMemorySyncRegression();
			runLinkedStorageStackTooltipRegression();
			runDroppedItemPickupRegression();
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerTierCandidateAndPaintRegression);
			return "{\"ok\":true,\"ordinaryBarrelLinkingSharesCanonicalContents\":true,\"canonicalStorageTypeControlsInsertionRules\":true,"
					+ "\"linkedControllerEndpointJoinKeepsOneCanonicalContentIndex\":true,"
					+ "\"linkedDoubleChestControllerUsesOneCanonicalContentIndex\":true,\"linkedControllerRoutesAndFansOutToolOperations\":true,"
					+ "\"controllerRestorationReconnectsLinkedStorage\":true,\"linkedDoubleChestLifecyclePreservesEndpoints\":true,"
					+ "\"linkedStorageMenuReceivesCanonicalSnapshot\":true,\"linkedStorageMenuTitlesRolesAndSlots\":true,"
					+ "\"linkedControllerPhysicalHighlightsAndVisibility\":true,\"linkedControllerTierCandidateAndPaintFanout\":true,"
					+ "\"linkedShulkerStashUsesCanonicalHostAndPrimaryRules\":true,\"storageFamilyCompatibilitySharesCanonicalContents\":true,"
					+ "\"endpointUnloadReloadPreservesEndpointAndCanonicalContents\":true,\"controllerReconnectPruningAndLockedRouting\":true,"
					+ "\"linkedTierUpgradePreservesPrimaryOnlyCanonicalCapacity\":true,\"linkedStorageSettingsMenuTransition\":true,"
					+ "\"linkedUpgradeRefreshesOpenCanonicalMenu\":true,"
					+ "\"linkedPrimaryExpandsAndClosesStaleMenu\":true,\"linkedLimitedMemorySurvivesEmptying\":true,"
					+ "\"linkedControllerClientOutlineIncludesPhysicalMembers\":true,\"linkedTierMenuInvalidates\":true,"
					+ "\"settingsSelectionRendersAndLinkedSnapshotsFanOut\":true,\"secondaryMemorySynchronizes\":true,"
					+ "\"linkedStorageTooltipUsesCanonicalCache\":true,\"secondarySkipsDroppedItemPickup\":true,\"ordinaryMagnetPicksDroppedItem\":true}";
		} catch (RuntimeException e) {
			return "{\"ok\":false,\"error\":" + jsonString(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()) + "}";
		}
	}

	private static void recoverClientPlayerForFixtures() {
		boolean respawnRequested = AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.screen instanceof DeathScreen && minecraft.player != null) {
				minecraft.player.respawn();
				return true;
			}
			return false;
		});
		if (!respawnRequested) {
			return;
		}
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.player != null && minecraft.player.isAlive() && !(minecraft.screen instanceof DeathScreen);
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client player did not recover from the void before linked-storage fixtures");
	}

	private static boolean preparePlayerForFixtures(ServerPlayer player) {
		ServerLevel level = player.level();
		if (player.getY() < 64) {
			if (!player.teleportTo(level, player.getX(), 80, player.getZ(), Set.of(), player.getYRot(), player.getXRot(), false)) {
				throw new IllegalStateException("Could not recover the linked-storage regression player from the void");
			}
		}
		createFixturePlatform(level, player.blockPosition());
		return true;
	}

	private static void runLinkedStorageMenuSnapshotRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 3);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked storage menu fixture");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			WorldHelper.notifyBlockUpdate(primary);
			WorldHelper.notifyBlockUpdate(secondary);
			String title = "Linked menu snapshot";
			primary.setCustomName(Component.literal(title));
			return new LinkedStorageMenuFixture(primaryPos, secondaryPos, endpoint(primary, "linked storage menu primary").groupId(), profile(primary), 7);
		});
		try {
			waitForClientLinkedStorageEndpoint(fixture, fixture.secondaryPos());
			openLinkedStorageMenu(fixture, fixture.secondaryPos());
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			openLinkedStorageMenu(fixture, fixture.primaryPos());
			waitForClientLinkedStorageMenu(fixture, fixture.primaryPos());
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void openLinkedStorageMenu(LinkedStorageMenuFixture fixture, BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			ItemStack originalMainHand = player.getMainHandItem().copy();
			InteractionResult interactionResult;
			try {
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				interactionResult = player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
			} finally {
				player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
			}
			require(interactionResult.consumesAction(), "Linked storage endpoint did not consume the block interaction: " + interactionResult);
			StorageBlockEntity storage = WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing linked storage menu endpoint"));
			require(player.containerMenu instanceof StorageContainerMenu, "Linked storage fixture did not open a storage menu");
			StorageContainerMenu menu = (StorageContainerMenu) player.containerMenu;
			int diamonds = count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND);
			LinkedStorageEndpointData endpoint = storage.getLinkedStorageEndpointData();
			boolean primary = endpoint != null
					&& LinkedStorageGroupsSavedData.get(player.level()).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId());
			require(menu.getBlockPosition().filter(pos::equals).isPresent() && menu.getNumberOfStorageInventorySlots() == fixture.profile().inventorySlots()
					&& menu.getNumberOfUpgradeSlots() == fixture.profile().upgradeSlots() && menu.getColumnsTaken() == fixture.profile().columnsTaken()
					&& diamonds == fixture.expectedDiamonds() && primary == pos.equals(fixture.primaryPos()),
					"Linked storage server menu did not expose its canonical slots and contents: pos=" + pos + ", endpoint="
							+ storage.getLinkedStorageEndpointData() + ", expected=" + fixture.profile() + ", menuSlots="
							+ menu.getNumberOfStorageInventorySlots() + ", menuUpgradeSlots=" + menu.getNumberOfUpgradeSlots() + ", menuColumns="
							+ menu.getColumnsTaken() + ", wrapperSlots=" + menu.getStorageWrapper().getInventoryHandler().size() + ", wrapperUpgradeSlots="
							+ menu.getStorageWrapper().getUpgradeHandler().size() + ", wrapperColumns=" + menu.getStorageWrapper().getColumnsTaken()
							+ ", diamonds=" + diamonds);
			return true;
		});
	}

	private static void waitForClientLinkedStorageEndpoint(LinkedStorageMenuFixture fixture, BlockPos pos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof StorageBlockEntity storage
							&& storage.getLinkedStorageEndpointData() != null && fixture.groupId().equals(storage.getLinkedStorageEndpointData().groupId())))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Linked storage endpoint did not synchronize to client");
	}

	private static void waitForClientLinkedStorageMenu(LinkedStorageMenuFixture fixture, BlockPos pos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu playerMenu
					&& Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu menu
					&& menu.getBlockPosition().filter(pos::equals).isPresent() && playerMenu.containerId == menu.containerId
					&& screen.getTitle().equals(fixture.profile().groupName())
					&& fixture.groupId().equals(menu.getStorageBlockEntity().getLinkedStorageEndpointData().groupId())
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
					&& endpointProvider.getLinkedStorageEndpointRole().filter(
							role -> role == (pos.equals(fixture.primaryPos()) ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY))
							.isPresent()
					&& menu.getStorageWrapper().getInventoryHandler().size() == fixture.profile().inventorySlots()
					&& menu.getStorageWrapper().getUpgradeHandler().size() == fixture.profile().upgradeSlots()
					&& menu.getStorageWrapper().getColumnsTaken() == fixture.profile().columnsTaken()
					&& menu.getNumberOfStorageInventorySlots() == fixture.profile().inventorySlots()
					&& menu.getNumberOfUpgradeSlots() == fixture.profile().upgradeSlots() && menu.getColumnsTaken() == fixture.profile().columnsTaken()
					&& ClientLinkedStorageContents.getGroupName(fixture.groupId()).filter(fixture.profile().groupName()::equals).isPresent()
					&& ClientLinkedStorageContents.getInventorySlots(fixture.groupId()).orElse(-1) == fixture.profile().inventorySlots()
					&& ClientLinkedStorageContents.getUpgradeSlots(fixture.groupId()).orElse(-1) == fixture.profile().upgradeSlots()
					&& ClientLinkedStorageContents.getColumnsTaken(fixture.groupId()).orElse(-1) == fixture.profile().columnsTaken()
					&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == fixture.expectedDiamonds()))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Linked storage menu did not receive the canonical snapshot: " + describeClientLinkedStorageMenu(fixture) + "; server="
				+ describeServerLinkedStorageMenu());
	}

	private static String describeServerLinkedStorageMenu() {
		return AutomationRuntime.runOnServer(player -> {
			if (!(player.containerMenu instanceof StorageContainerMenu menu))
				return "menu=" + player.containerMenu.getClass().getSimpleName() + "#" + player.containerMenu.containerId;
			return "menu=StorageContainerMenu#" + menu.containerId + ", position=" + menu.getBlockPosition() + ", stillValid=" + menu.stillValid(player)
					+ ", carried=" + menu.getCarried();
		});
	}

	private static String describeClientLinkedStorageMenu(LinkedStorageMenuFixture fixture) {
		return AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
				return "screen=" + minecraft.screen;
			}
			if (!(screen.getMenu() instanceof StorageContainerMenu menu)) {
				return "screen=" + screen.getClass().getSimpleName() + ", menu=" + screen.getMenu().getClass().getSimpleName();
			}
			return "title=" + screen.getTitle() + ", blockPosition=" + menu.getBlockPosition() + ", endpoint="
					+ menu.getStorageBlockEntity().getLinkedStorageEndpointData() + ", wrapperSlots=" + menu.getStorageWrapper().getInventoryHandler().size()
					+ ", wrapperUpgradeSlots=" + menu.getStorageWrapper().getUpgradeHandler().size() + ", wrapperColumns="
					+ menu.getStorageWrapper().getColumnsTaken() + ", menuSlots=" + menu.getNumberOfStorageInventorySlots() + ", menuUpgradeSlots="
					+ menu.getNumberOfUpgradeSlots() + ", menuColumns=" + menu.getColumnsTaken() + ", cachedName="
					+ ClientLinkedStorageContents.getGroupName(fixture.groupId()) + ", cachedSlots="
					+ ClientLinkedStorageContents.getInventorySlots(fixture.groupId()) + ", cachedUpgradeSlots="
					+ ClientLinkedStorageContents.getUpgradeSlots(fixture.groupId()) + ", cachedColumns="
					+ ClientLinkedStorageContents.getColumnsTaken(fixture.groupId()) + ", diamonds="
					+ count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND);
		});
	}

	public static String setupLinkedLimitedBarrelReloadProjection() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = limitedPrimaryPos(player);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity primary = limited(level, primaryPos);
			LimitedBarrelBlockEntity secondary = limited(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not link limited barrels for reload projection");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, LINKED_LIMITED_RELOAD_ITEM_COUNT));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			WorldHelper.notifyBlockUpdate(primary);
			WorldHelper.notifyBlockUpdate(secondary);
			LinkedStorageEndpointData primaryEndpoint = endpoint(primary, "limited reload primary");
			require(hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary),
					"Linked limited barrels did not initialize their render projection before reload");
			return "{\"ok\":true,\"groupId\":\"" + primaryEndpoint.groupId() + "\",\"primaryEndpointId\":\"" + primaryEndpoint.endpointId()
					+ "\",\"secondaryEndpointId\":\"" + endpoint(secondary, "limited reload secondary").endpointId() + "\"}";
		});
	}

	public static String linkedLimitedBarrelReloadProjectionStatus(UUID groupId) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			LimitedBarrelBlockEntity primary = limited(level, limitedPrimaryPos(player));
			LimitedBarrelBlockEntity secondary = limited(level, limitedPrimaryPos(player).east(3));
			LinkedStorageEndpointData primaryEndpoint = endpoint(primary, "reloaded limited primary");
			LinkedStorageEndpointData secondaryEndpoint = endpoint(secondary, "reloaded limited secondary");
			require(primaryEndpoint.groupId().equals(groupId) && secondaryEndpoint.groupId().equals(groupId),
					"Reloaded limited barrels do not belong to the expected linked-storage group");
			requirePersistedNestedEndpoint(level, primary, primaryEndpoint, LinkedStorageEndpointRole.PRIMARY);
			requirePersistedNestedEndpoint(level, secondary, secondaryEndpoint, LinkedStorageEndpointRole.SECONDARY);
			require(hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary),
					"Reloaded linked limited barrels did not restore server render projection");
			WorldHelper.notifyBlockUpdate(primary);
			WorldHelper.notifyBlockUpdate(secondary);
			return true;
		});
		waitForClientLimitedBarrelReloadProjection();
		return "{\"ok\":true,\"endpointRoles\":true,\"clientDisplayItems\":true,\"clientCounts\":true,\"clientFillLevels\":true}";
	}

	private static Boolean runOrdinaryLinkedStorage(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType gameType = player.gameMode.getGameModeForPlayer();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 4);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			require(primary.isLinkedStorageCandidate() && secondary.isLinkedStorageCandidate(), "Ordinary barrels were not link candidates");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link ordinary barrels");
			ResourceHandler<ItemResource> capability = level.getCapability(Capabilities.Item.BLOCK, secondaryPos, Direction.UP);
			int inserted;
			try (Transaction transaction = Transaction.openRoot()) {
				inserted = capability == null ? 0 : capability.insert(ItemResource.of(new ItemStack(Items.EMERALD)), 3, transaction);
				transaction.commit();
			}
			require(inserted == 3 && count(primary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
					&& count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
					&& count(primary.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3,
					"Linked barrel endpoints did not expose one canonical inventory through capabilities");
			secondary.toggleLock();
			require(insertThroughCapability(level, secondaryPos, new ItemStack(Items.REDSTONE)) == 0
					&& insertThroughCapability(level, secondaryPos, new ItemStack(Items.DIAMOND)) == 1
					&& insertThroughCapability(level, primaryPos, new ItemStack(Items.REDSTONE)) == 1,
					"Linked endpoint locks did not retain independent external input filtering");
			secondary.toggleLock();
			return true;
		} finally {
			player.setGameMode(gameType);
			clearArea(level, primaryPos);
		}
	}

	private static Boolean runCanonicalStorageTypeInsertionRules(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType gameType = player.gameMode.getGameModeForPlayer();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 4);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			place(level, player, primaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			StorageBlockEntity primary = WorldHelper.getBlockEntity(level, primaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing canonical shulker endpoint"));
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link canonical shulker to barrel endpoint");
			require(insertThroughCapability(level, secondaryPos, new ItemStack(Items.SHULKER_BOX)) == 0,
					"Physical barrel endpoint bypassed the canonical shulker insertion restriction");

			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			primary = WorldHelper.getBlockEntity(level, primaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing canonical barrel endpoint"));
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link canonical barrel to shulker endpoint");
			require(insertThroughCapability(level, secondaryPos, new ItemStack(Items.SHULKER_BOX)) == 1,
					"Physical shulker endpoint overrode the canonical barrel insertion rule");
			return true;
		} finally {
			player.setGameMode(gameType);
			clearArea(level, primaryPos);
		}
	}

	private static long insertThroughCapability(ServerLevel level, BlockPos pos, ItemStack stack) {
		ResourceHandler<ItemResource> capability = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.UP);
		if (capability == null) {
			return 0;
		}
		try (Transaction transaction = Transaction.openRoot()) {
			long inserted = capability.insert(ItemResource.of(stack), stack.getCount(), transaction);
			transaction.commit();
			return inserted;
		}
	}

	private static Boolean runLinkedControllerCanonicalContents(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType gameType = player.gameMode.getGameModeForPlayer();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 4);
		BlockPos existingPos = controllerPos.east();
		BlockPos joiningPos = existingPos.east();
		clearArea(level, controllerPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			placeWithPlayer(level, player, existingPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeWithPlayer(level, player, joiningPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			ControllerBlockEntity controller = controller(level, controllerPos);
			BarrelBlockEntity existing = barrel(level, existingPos);
			BarrelBlockEntity joining = barrel(level, joiningPos);
			int slots = existing.getStorageWrapper().getInventoryHandler().size();
			require(controller.getStoragePositions().size() == 2 && controller.size() == slots * 2,
					"Controller did not register both unlinked barrels before linking");
			require(linkWithPlayer(player, existing, existingPos, joiningPos), "Could not create controller-connected linked barrel group");
			existing.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			existing.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(controller.getStoragePositions().size() == 1 && controller.size() == slots && controller.getStackStorages(diamondKey).size() == 1,
					"Controller duplicated canonical contents when a connected endpoint joined a linked group");
			Set<BlockPos> physicalPositions = controller.getStorageBlockPositions();
			List<BlockPos> highlightPositions = controller.getHighlightStoragePositions(controller.getStackStorages(diamondKey));
			require(physicalPositions.containsAll(Set.of(existingPos, joiningPos)) && highlightPositions.containsAll(Set.of(existingPos, joiningPos)),
					"Controller physical positions or matching highlights omitted a linked endpoint: physical=" + physicalPositions + ", highlights="
							+ highlightPositions + ", canonical=" + controller.getStoragePositions() + ", matching=" + controller.getStackStorages(diamondKey));
			try (Transaction transaction = Transaction.openRoot()) {
				require(controller.insert(ItemResource.of(new ItemStack(Items.EMERALD)), 3, transaction) == 3,
						"Controller did not route items to linked contents");
				transaction.commit();
			}
			require(count(joining.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3,
					"Controller-routed items were not visible through the linked endpoint");
			controller.toggleLock();
			require(existing.isLocked() && joining.isLocked(), "Controller lock did not fan out to linked members");
			boolean lockVisible = existing.shouldShowLock();
			boolean tierVisible = existing.shouldShowTier();
			boolean upgradesVisible = existing.shouldShowUpgrades();
			controller.toggleLockVisibility();
			controller.toggleTierVisiblity();
			controller.toggleUpgradesVisiblity();
			require(existing.shouldShowLock() != lockVisible && joining.shouldShowLock() != lockVisible && existing.shouldShowTier() != tierVisible
					&& joining.shouldShowTier() != tierVisible && existing.shouldShowUpgrades() != upgradesVisible
					&& joining.shouldShowUpgrades() != upgradesVisible, "Controller visibility toggles did not fan out to linked members");
			controller.toggleLock();
			level.setBlock(controllerPos, Blocks.AIR.defaultBlockState(), 3);
			require(existing.getControllerPos().isEmpty() && joining.getControllerPos().isEmpty(), "Removing controller did not detach linked members");
			place(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			ControllerBlockEntity restored = controller(level, controllerPos);
			restored.searchAndAddBoundables();
			require(existing.getControllerPos().filter(controllerPos::equals).isPresent()
					&& joining.getControllerPos().filter(controllerPos::equals).isPresent() && restored.getStoragePositions().size() == 1
					&& restored.getStackStorages(diamondKey).size() == 1, "Restored controller did not reconnect linked canonical storage index");
			return true;
		} finally {
			player.setGameMode(gameType);
			clearArea(level, controllerPos);
		}
	}

	private static Boolean runLinkedControllerTierCandidateAndPaintRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 16);
		BlockPos primaryPos = controllerPos.east();
		BlockPos secondaryPos = primaryPos.east();
		GameType gameType = player.gameMode.getGameModeForPlayer();
		ItemStack originalMainHand = player.getMainHandItem().copy();
		ItemStack originalDyes = player.getInventory().getItem(0).copy();
		clearArea(level, controllerPos);
		try {
			placeWithPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeWithPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create controller tool fixture");
			placeWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			ControllerBlockEntity controller = controller(level, controllerPos);
			require(controller.getStorageTierUpgradePositions().equals(Set.of(primaryPos)),
					"Controller production tier candidate iterable did not contain exactly the linked primary endpoint");
			player.setGameMode(GameType.SURVIVAL);
			ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
			require(player.gameMode
					.useItemOn(player, level, upgrade, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false))
					.consumesAction() && level.getBlockState(primaryPos).is(ModBlocks.DIAMOND_BARREL.get())
					&& level.getBlockState(secondaryPos).is(ModBlocks.BARREL.get()),
					"Controller tier upgrade did not choose only the linked primary candidate");
			BarrelBlockEntity upgradedPrimary = barrel(level, primaryPos);
			ItemStack paintbrush = new ItemStack(ModItems.PAINTBRUSH.get());
			PaintbrushItem.setMainColor(paintbrush, 0xFFFF0000);
			PaintbrushItem.ItemRequirements requirements = PaintbrushItem.getItemRequirements(paintbrush, player, level, controllerPos)
					.orElseThrow(() -> new IllegalStateException("Controller paintbrush did not report dye requirements"));
			require(countRequirements(requirements, Items.RED_DYE) == 2, "Controller paintbrush did not require exactly two red dyes");
			player.getInventory().setItem(0, new ItemStack(Items.RED_DYE, 2));
			require(paintbrush.getItem().onItemUseFirst(paintbrush,
					new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false))) == InteractionResult.SUCCESS
					&& upgradedPrimary.getStorageWrapper().getMainColor() == 0xFFFF0000
					&& barrel(level, secondaryPos).getStorageWrapper().getMainColor() == 0xFFFF0000,
					"Controller paintbrush did not color both physical linked endpoints");
			return true;
		} finally {
			player.setGameMode(gameType);
			player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
			player.getInventory().setItem(0, originalDyes);
			clearArea(level, controllerPos);
		}
	}

	private static void runLinkedPrimaryChestExpansionRegression() {
		runLinkedPrimaryChestExpansionRegression(Direction.EAST, 3);
		runLinkedPrimaryChestExpansionRegression(Direction.WEST, 12);
	}

	private static void runLinkedPrimaryChestExpansionRegression(Direction direction, int zOffset) {
		ExpansionFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, zOffset);
			clearArea(level, primaryPos);
			placeWithPlayer(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity primary = chest(level, primaryPos);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
					primary) == LinkedStorageService.LinkResult.SUCCESS, "Could not link primary chest for expansion");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			return new ExpansionFixture(primaryPos, primaryPos.relative(direction), endpoint(primary, "expansion primary"), profile(primary), player.position(),
					player.getYRot(), player.getXRot());
		});
		try {
			AutomationRuntime.runOnServer(player -> {
				require(player.teleportTo(player.level(), fixture.primaryPos().getX() + .5D, fixture.primaryPos().getY(), fixture.primaryPos().getZ() + .5D,
						Set.of(), player.getYRot(), player.getXRot(), false), "Could not position player for linked primary chest expansion");
				return true;
			});
			waitForClientPlayerNear(fixture.primaryPos());
			LinkedStorageMenuFixture initialMenuFixture = new LinkedStorageMenuFixture(fixture.primaryPos(), fixture.primaryPos(), fixture.endpoint().groupId(),
					AutomationRuntime.runOnServer(player -> profile(chest(player.level(), fixture.primaryPos()))), 7);
			waitForClientLinkedStorageEndpoint(initialMenuFixture, fixture.primaryPos());
			openLinkedStorageMenu(initialMenuFixture, fixture.primaryPos());
			BlockPos expandedPrimaryPos = AutomationRuntime.runOnServer(player -> {
				require(player.containerMenu instanceof StorageContainerMenu, "Linked primary menu closed before chest expansion: player=" + player.position()
						+ ", primary=" + fixture.primaryPos() + ", distanceSquared=" + player.distanceToSqr(Vec3.atCenterOf(fixture.primaryPos())));
				StorageContainerMenu staleMenu = (StorageContainerMenu) player.containerMenu;
				placeWithPlayer(player.level(), player, fixture.addedPos(), new ItemStack(ModBlocks.CHEST_ITEM.get()));
				require(player.containerMenu == player.inventoryMenu && player.containerMenu != staleMenu, "Expanding linked primary did not close stale menu");
				ChestBlockEntity originalPart = chest(player.level(), fixture.primaryPos());
				ChestBlockEntity addedPart = chest(player.level(), fixture.addedPos());
				List<ChestBlockEntity> endpointHolders = List.of(originalPart, addedPart).stream()
						.filter(part -> fixture.endpoint().equals(part.getLinkedStorageEndpointData())).toList();
				require(endpointHolders.size() == 1 && endpointHolders.getFirst().isMainChest(),
						"Expanded linked chest did not preserve exactly one endpoint on its main part");
				ChestBlockEntity expanded = endpointHolders.getFirst();
				int expandedSlots = expanded.getStorageWrapper().getInventoryHandler().size();
				int expandedDiamonds = count(expanded.getStorageWrapper().getInventoryHandler(), Items.DIAMOND);
				require(expandedSlots == 54 && expandedDiamonds == 7,
						"Expanded linked primary did not preserve endpoint, capacity, and contents: position=" + expanded.getBlockPos() + ", slots="
								+ expandedSlots + ", diamonds=" + expandedDiamonds + ", originalEndpoint=" + originalPart.getLinkedStorageEndpointData()
								+ ", addedEndpoint=" + addedPart.getLinkedStorageEndpointData());
				return expanded.getBlockPos();
			});
			waitForClosedStorageMenu();
			LinkedStorageSnapshotProfile expandedProfile = AutomationRuntime.runOnServer(player -> profile(chest(player.level(), expandedPrimaryPos)));
			require(expandedProfile.groupName().equals(fixture.profile().groupName()), "Expanded linked primary did not preserve its menu title");
			LinkedStorageMenuFixture expandedFixture = new LinkedStorageMenuFixture(expandedPrimaryPos, expandedPrimaryPos, fixture.endpoint().groupId(),
					expandedProfile, 7);
			require(expandedFixture.profile().inventorySlots() == 54, "Expanded linked primary profile did not expose 54 inventory slots");
			waitForClientLinkedStorageEndpoint(expandedFixture, expandedPrimaryPos);
			openLinkedStorageMenu(expandedFixture, expandedPrimaryPos);
			waitForClientLinkedStorageMenu(expandedFixture, expandedPrimaryPos);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), fixture.primaryPos());
				player.teleportTo(player.level(), fixture.originalPosition().x, fixture.originalPosition().y, fixture.originalPosition().z, Set.of(),
						fixture.originalYRot(), fixture.originalXRot(), false);
				return true;
			});
		}
	}

	private static void runLinkedLimitedBarrelMemorizedSlotMenuRegression() {
		MemoryFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 24);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity primary = limited(level, primaryPos);
			LimitedBarrelBlockEntity secondary = limited(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked limited-barrel emptying fixture");
			primary.toggleLock();
			return new MemoryFixture(primaryPos, secondaryPos, endpoint(primary, "limited memory primary").groupId(), profile(primary),
					player.getMainHandItem().copy(), player.position(), player.getYRot(), player.getXRot());
		});
		try {
			waitForClientLinkedStorageEndpoint(fixture.menuFixture(), fixture.primaryPos());
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				LimitedBarrelBlockEntity primary = limited(level, fixture.primaryPos());
				var state = level.getBlockState(fixture.primaryPos());
				LimitedBarrelBlock block = (LimitedBarrelBlock) state.getBlock();
				Direction facing = block.getFacing(state);
				ItemStack diamonds = new ItemStack(Items.DIAMOND, LINKED_LIMITED_RELOAD_ITEM_COUNT);
				player.setItemInHand(InteractionHand.MAIN_HAND, diamonds);
				require(player.gameMode.useItemOn(player, level, diamonds, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()).add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(.5)), facing,
								fixture.primaryPos(), false))
						.consumesAction(), "Could not insert diamonds through the locked linked limited-barrel interaction");
				Vec3 originalPosition = player.position();
				float originalYRot = player.getYRot();
				float originalXRot = player.getXRot();
				try {
					require(player.teleportTo(level, fixture.primaryPos().getX() + .5D + facing.getStepX() * 2, fixture.primaryPos().getY() - 1.12D,
							fixture.primaryPos().getZ() + .5D + facing.getStepZ() * 2, Set.of(), facing.getOpposite().toYRot(), 0, false),
							"Could not position player for linked limited-barrel extraction interaction");
					player.setYHeadRot(facing.getOpposite().toYRot());
					for (int removed = 0; removed < LINKED_LIMITED_RELOAD_ITEM_COUNT; removed++) {
						require(block.tryToTakeItem(state, level, fixture.primaryPos(), player),
								"Could not remove a diamond through the linked limited-barrel interaction");
					}
				} finally {
					player.setYRot(originalYRot);
					player.setXRot(originalXRot);
					player.teleportTo(level, originalPosition.x, originalPosition.y, originalPosition.z, Set.of(), originalYRot, originalXRot, false);
				}
				require(primary.getStorageWrapper().getInventoryHandler().getStackInSlot(0).isEmpty() && hasMemory(primary, Items.DIAMOND),
						"Empty linked limited barrel did not retain its memorized diamond");
				var canonicalContents = LinkedStorageGroupsSavedData.get(level).manager().resolveContents(fixture.groupId())
						.orElseThrow(() -> new IllegalStateException("Linked limited-barrel group contents disappeared")).contents();
				require(canonicalContents.settings().equals(primary.getStorageWrapper().getSettingsHandler().getSettingsData()),
						"Linked limited-barrel canonical settings did not retain its memory");
				return true;
			});
			AutomationRuntime.runOnServer(player -> {
				require(player.teleportTo(player.level(), fixture.primaryPos().getX() + .5D, fixture.primaryPos().getY(), fixture.primaryPos().getZ() + .5D,
						Set.of(), player.getYRot(), player.getXRot(), false), "Could not position player for linked limited-barrel menu");
				return true;
			});
			waitForClientPlayerNear(fixture.primaryPos());
			openLinkedStorageMenu(fixture.menuFixture(), fixture.primaryPos());
			waitForClientLinkedStorageMenu(fixture.menuFixture(), fixture.primaryPos());
			waitForClientMemorizedStack(fixture.primaryPos(), Items.DIAMOND);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalHand());
				clearArea(player.level(), fixture.primaryPos());
				player.teleportTo(player.level(), fixture.originalPosition().x, fixture.originalPosition().y, fixture.originalPosition().z, Set.of(),
						fixture.originalYRot(), fixture.originalXRot(), false);
				return true;
			});
		}
	}

	private static void runSecondaryMemorySyncRegression() {
		MemoryFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 3);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity primary = limited(level, primaryPos);
			LimitedBarrelBlockEntity secondary = limited(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create memory sync fixture");
			return new MemoryFixture(primaryPos, secondaryPos, endpoint(primary, "memory primary").groupId(), profile(primary), player.getMainHandItem().copy(),
					player.position(), player.getYRot(), player.getXRot());
		});
		try {
			waitForClientLinkedStorageEndpoint(fixture.menuFixture(), fixture.secondaryPos());
			openLinkedStorageMenu(fixture.menuFixture(), fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				LimitedBarrelBlockEntity primary = limited(player.level(), fixture.primaryPos());
				LimitedBarrelBlockEntity secondary = limited(player.level(), fixture.secondaryPos());
				secondary.toggleLock();
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 3));
				require(secondary.depositItem(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), 0),
						"Real deposit through linked limited secondary failed");
				require(hasMemory(primary, Items.EMERALD) && hasMemory(secondary, Items.EMERALD),
						"Real limited-barrel deposit did not update memory through both linked wrappers");
				return true;
			});
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (System.nanoTime() < deadline) {
				if (AutomationRuntime.runOnClient(
						() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu
								&& menu.getMemorizedStackInSlot(0).filter(stack -> stack.is(Items.EMERALD)).isPresent()))
					return;
				sleep(50);
			}
			throw new IllegalStateException("Secondary menu did not receive canonical memory update");
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalHand());
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void runLinkedStorageStackTooltipRegression() {
		TooltipFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos pos = player.blockPosition().offset(0, 0, 52);
			clearArea(level, pos);
			place(level, player, pos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, pos);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
					primary) == LinkedStorageService.LinkResult.SUCCESS, "Could not create tooltip fixture");
			LinkedStorageEndpointData endpoint = endpoint(primary, "tooltip primary");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStack original = player.getInventory().getItem(8).copy();
			level.destroyBlock(pos, true, player);
			List<ItemEntity> carriers = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
					entity -> endpoint.equals(entity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)));
			require(carriers.size() == 1, "Linked storage break produced " + carriers.size() + " primary carriers instead of exactly one");
			ItemStack carrier = carriers.getFirst().getItem().copy();
			require(Boolean.TRUE.equals(carrier.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT)),
					"Dropped linked tooltip carrier was not marked PRIMARY");
			level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).forEach(ItemEntity::discard);
			player.getInventory().setItem(8, carrier);
			player.inventoryMenu.broadcastChanges();
			return new TooltipFixture(pos, endpoint, 8, original);
		});
		try {
			waitForClientTooltipCarrier(fixture);
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				ItemStack carrier = minecraft.player.getInventory().getItem(fixture.inventorySlot());
				ItemStack originalCarried = minecraft.player.containerMenu.getCarried().copy();
				minecraft.player.containerMenu.setCarried(carrier.copy());
				try {
					var tooltipImage = StorageItemClient.getTooltipImage(carrier);
					require(tooltipImage instanceof StorageContentsTooltip, "Production storage tooltip path did not create a contents tooltip");
					StorageContentsTooltip contentsTooltip = (StorageContentsTooltip) tooltipImage;
					require(contentsTooltip.linkedStorageTooltip() instanceof LinkedStorageTooltip linkedStorageTooltip
							&& linkedStorageTooltip.role() == LinkedStorageEndpointRole.PRIMARY
							&& linkedStorageTooltip.groupId().equals(fixture.endpoint().groupId()),
							"Production storage contents tooltip did not include the primary linked-storage group metadata");
					initializeTooltipRequest(new ClientStorageContentsTooltip(contentsTooltip), minecraft.player,
							StackStorageWrapper.fromStack(minecraft.level.registryAccess(), carrier));
					minecraft.player.containerMenu.setCarried(ItemStack.EMPTY);
					var compactTooltip = StorageItemClient.getTooltipImage(carrier);
					require(compactTooltip instanceof LinkedStorageTooltip linkedStorageTooltip
							&& linkedStorageTooltip.role() == LinkedStorageEndpointRole.PRIMARY
							&& linkedStorageTooltip.groupId().equals(fixture.endpoint().groupId()),
							"Compact linked storage tooltip did not expose the primary group metadata");
				} finally {
					minecraft.player.containerMenu.setCarried(originalCarried);
				}
				return true;
			});
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (System.nanoTime() < deadline) {
				if (AutomationRuntime.runOnClient(
						() -> Minecraft.getInstance().player != null && ClientLinkedStorageContents.getRevision(fixture.endpoint().groupId()).isPresent()
								&& count(
										StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(),
												Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot())).getInventoryHandler(),
										Items.DIAMOND) == 7))
					return;
				sleep(50);
			}
			throw new IllegalStateException("Linked carrier tooltip cache did not resolve canonical contents");
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().setItem(fixture.inventorySlot(), fixture.originalStack());
				player.inventoryMenu.broadcastChanges();
				clearArea(player.level(), fixture.pos());
				return true;
			});
		}
	}
	private static void initializeTooltipRequest(ClientStorageContentsTooltip tooltip, LocalPlayer player, IStorageWrapper wrapper) {
		try {
			var initContents = ClientStorageContentsTooltipBase.class.getDeclaredMethod("initContents", LocalPlayer.class, IStorageWrapper.class);
			initContents.setAccessible(true);
			initContents.invoke(tooltip, player, wrapper);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not invoke the production storage tooltip request path", e);
		}
	}

	private static void runDroppedItemPickupRegression() {
		PickupFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 12);
			BlockPos secondaryPos = primaryPos.east(32);
			BlockPos ordinaryPos = primaryPos.west(32);
			clearArea(level, primaryPos);
			clearArea(level, secondaryPos);
			clearArea(level, ordinaryPos);
			clearPickupEntities(level, primaryPos, secondaryPos, ordinaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, ordinaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			BarrelBlockEntity ordinary = barrel(level, ordinaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create pickup fixture");
			configureMagnet(primary);
			configureMagnet(ordinary);
			require(level.getBlockState(primaryPos).getValue(StorageBlockBase.TICKING) && !level.getBlockState(secondaryPos).getValue(StorageBlockBase.TICKING),
					"Secondary endpoint incorrectly enabled pickup ticking");
			require(level.getBlockState(ordinaryPos).getValue(StorageBlockBase.TICKING), "Ordinary magnet barrel did not enable pickup ticking");
			ItemEntity first = spawnPickupItem(level, secondaryPos, -0.25);
			ItemEntity second = spawnPickupItem(level, secondaryPos, 0.25);
			ItemEntity ordinaryItem = spawnPickupItem(level, ordinaryPos, 0);
			return new PickupFixture(primaryPos, secondaryPos, ordinaryPos, List.of(first.getUUID(), second.getUUID()), ordinaryItem.getUUID(),
					level.getGameTime() + 20);
		});
		try {
			waitForServerCondition(player -> player.level().getGameTime() >= fixture.verifyTime());
			AutomationRuntime.runOnServer(player -> {
				List<ItemEntity> items = fixture.itemIds().stream().map(id -> {
					ItemEntity item = player.level().getEntity(id) instanceof ItemEntity entity ? entity : null;
					require(item != null && item.getItem().is(Items.ENDER_PEARL) && item.getItem().getCount() == 1,
							"Secondary pickup suppression state: entity=" + describeEntity(player.level(), id));
					return item;
				}).toList();
				int canonicalCount = count(barrel(player.level(), fixture.primaryPos()).getStorageWrapper().getInventoryHandler(), Items.ENDER_PEARL);
				int ordinaryCount = count(barrel(player.level(), fixture.ordinaryPos()).getStorageWrapper().getInventoryHandler(), Items.ENDER_PEARL);
				require(canonicalCount == 0, "Secondary pickup suppression state: canonicalCount=" + canonicalCount + ", entities="
						+ fixture.itemIds().stream().map(id -> describeEntity(player.level(), id)).toList() + ", player=" + player.position());
				require(player.level().getEntity(fixture.ordinaryItemId()) == null && ordinaryCount == 1,
						"Ordinary pickup control state: entity=" + describeEntity(player.level(), fixture.ordinaryItemId()) + ", count=" + ordinaryCount);
				items.forEach(item -> item.setPos(Vec3.atCenterOf(fixture.primaryPos())));
				return true;
			});
			waitForServerCondition(player -> fixture.itemIds().stream().allMatch(id -> player.level().getEntity(id) == null)
					&& count(barrel(player.level(), fixture.primaryPos()).getStorageWrapper().getInventoryHandler(), Items.ENDER_PEARL) == 2);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				fixture.itemIds().forEach(id -> {
					if (player.level().getEntity(id) instanceof ItemEntity item) {
						item.discard();
					}
				});
				if (player.level().getEntity(fixture.ordinaryItemId()) instanceof ItemEntity item) {
					item.discard();
				}
				clearPickupEntities(player.level(), fixture.primaryPos(), fixture.secondaryPos(), fixture.ordinaryPos());
				clearArea(player.level(), fixture.primaryPos());
				clearArea(player.level(), fixture.secondaryPos());
				clearArea(player.level(), fixture.ordinaryPos());
				return true;
			});
		}
	}

	private static void configureMagnet(StorageBlockEntity storage) {
		UpgradeHandler upgrades = storage.getStorageWrapper().getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(ModItems.MAGNET_UPGRADE.get()));
		MagnetUpgradeWrapper magnet = upgrades.getWrappersThatImplement(MagnetUpgradeWrapper.class).stream().findFirst().orElseThrow();
		magnet.setPickupItems(true);
		magnet.setPickupXp(false);
		magnet.getFilterLogic().setDepositFilterType(ContentsFilterType.ALLOW);
		magnet.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.ENDER_PEARL));
		upgrades.saveInventory();
	}
	private static ItemEntity spawnPickupItem(ServerLevel level, BlockPos pos, double xOffset) {
		ItemEntity item = new ItemEntity(level, Vec3.atCenterOf(pos).x + xOffset, Vec3.atCenterOf(pos).y - 0.25D, Vec3.atCenterOf(pos).z,
				new ItemStack(Items.ENDER_PEARL));
		item.setDeltaMovement(Vec3.ZERO);
		item.setNoGravity(true);
		item.setDefaultPickUpDelay();
		require(level.addFreshEntity(item), "Could not add isolated pickup regression entity at " + item.position());
		return item;
	}
	private static String describeEntity(ServerLevel level, UUID id) {
		return level.getEntity(id) instanceof ItemEntity item ? id + "@" + item.position() + "x" + item.getItem().getCount() : id + "=missing";
	}
	private static void clearPickupEntities(ServerLevel level, BlockPos... positions) {
		for (BlockPos pos : positions) {
			level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(ItemEntity::discard);
		}
	}
	private static void waitForClientTooltipCarrier(TooltipFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().player != null
							&& fixture.endpoint()
									.equals(Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot())
											.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
							&& Boolean.TRUE.equals(Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot())
									.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT))))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Exact linked tooltip carrier did not synchronize to client inventory slot 8");
	}
	private static LinkedStorageSnapshotProfile profile(StorageBlockEntity storage) {
		return new LinkedStorageSnapshotProfile(storage.getMenuDisplayName(), storage.getStorageWrapper().getInventoryHandler().size(),
				storage.getStorageWrapper().getUpgradeHandler().size(), storage.getStorageWrapper().getColumnsTaken());
	}
	private static boolean hasMemory(StorageBlockEntity storage, Item item) {
		return storage.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotFilterStack(0, true)
				.filter(stack -> stack.is(item)).isPresent();
	}
	private static int countRequirements(PaintbrushItem.ItemRequirements requirements, Item item) {
		return java.util.stream.Stream.concat(requirements.itemsPresent().stream(), requirements.itemsMissing().stream()).filter(stack -> stack.is(item))
				.mapToInt(ItemStack::getCount).sum();
	}

	private static void waitForServerCondition(java.util.function.Predicate<ServerPlayer> condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(condition::test))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for linked storage server state");
	}

	private static void waitForClosedStorageMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(player -> player.containerMenu == player.inventoryMenu)
					&& AutomationRuntime.runOnClient(() -> !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Stale linked storage menu remained open");
	}

	private static void runLinkedControllerClientOutlineRegression() {
		LinkedControllerOutlineFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 132);
			BlockPos primaryPos = controllerPos.east();
			BlockPos secondaryPos = primaryPos.east();
			clearArea(level, controllerPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked controller outline fixture");
			place(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			require(controller(level, controllerPos).getStorageBlockPositions().containsAll(Set.of(primaryPos, secondaryPos)),
					"Server controller did not discover both linked outline members");
			return new LinkedControllerOutlineFixture(controllerPos, primaryPos, secondaryPos);
		});
		try {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (System.nanoTime() < deadline) {
				if (AutomationRuntime
						.runOnClient(
								() -> WorldHelper.getBlockEntity(Minecraft.getInstance().level, fixture.controllerPos(), ControllerBlockEntity.class)
										.map(controller -> controller.getStorageBlockPositions().containsAll(
												Set.of(fixture.primaryPos(), fixture.secondaryPos())) && !controller.getStorageBlockEdges().isEmpty())
										.orElse(false))) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Client linked controller outline did not include both physical members and edges");
		} finally {
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.level(), fixture.controllerPos());
				return true;
			});
		}
	}

	private static Boolean runLinkedDoubleChestControllerCanonicalContents(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType gameType = player.gameMode.getGameModeForPlayer();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 4);
		BlockPos primaryLeft = controllerPos.east();
		BlockPos primaryMain = primaryLeft.east();
		BlockPos secondaryLeft = controllerPos.south();
		BlockPos secondaryMain = secondaryLeft.east();
		clearArea(level, controllerPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			placeWithPlayer(level, player, primaryLeft, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeWithPlayer(level, player, primaryMain, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeWithPlayer(level, player, secondaryLeft, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeWithPlayer(level, player, secondaryMain, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ControllerBlockEntity controller = controller(level, controllerPos);
			ChestBlockEntity primary = chest(level, primaryMain);
			ChestBlockEntity secondary = chest(level, secondaryMain);
			require(isDoubleChest(level, primaryLeft, primaryMain) && isDoubleChest(level, secondaryLeft, secondaryMain),
					"Real-player placement did not create both linked-storage double chests");
			require(linkWithPlayer(player, primary, primaryMain, secondaryMain), "Could not link player-placed double chests");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7 && controller.getStoragePositions().size() == 1
					&& controller.getStoragePositions().contains(primaryMain) && controller.size() == 54 && controller.getStackStorages(diamondKey).size() == 1,
					"Linked double chests did not share one canonical controller storage");
			BlockPos ordinaryStoragePos = controllerPos.west();
			placeWithPlayer(level, player, ordinaryStoragePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity ordinaryStorage = barrel(level, ordinaryStoragePos);
			ChestBlockEntity primaryOtherHalf = chest(level, primaryLeft);
			require(controller.getStoragePositions().size() == 2 && controller.getStoragePositions().contains(ordinaryStoragePos),
					"Controller did not register the ordinary storage with the linked double chest");
			controller.toggleLock();
			require(primary.isLocked() && primaryOtherHalf.isLocked() && ordinaryStorage.isLocked(),
					"Controller lock did not lock both linked double-chest halves and the ordinary storage exactly once");
			controller.toggleLock();
			require(!primary.isLocked() && !primaryOtherHalf.isLocked() && !ordinaryStorage.isLocked(),
					"Controller unlock did not unlock both linked double-chest halves and the ordinary storage");
			return true;
		} finally {
			player.setGameMode(gameType);
			clearArea(level, controllerPos);
		}
	}

	private static Boolean runLinkedDoubleChestLifecycle(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType gameType = player.gameMode.getGameModeForPlayer();
		BlockPos primaryLeft = player.blockPosition().offset(0, 0, 4);
		BlockPos primaryMain = primaryLeft.east();
		BlockPos primaryPeer = primaryLeft.south(3);
		BlockPos secondaryLeft = primaryLeft.east(4);
		BlockPos secondaryMain = secondaryLeft.east();
		BlockPos secondaryPeer = secondaryLeft.south(3);
		clearArea(level, primaryLeft);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeWithPlayer(level, player, primaryLeft, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeWithPlayer(level, player, primaryPeer, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			ChestBlockEntity primarySingle = chest(level, primaryLeft);
			require(linkWithPlayer(player, primarySingle, primaryLeft, primaryPeer), "Could not link the primary chest before double-chest formation");
			LinkedStorageEndpointData primaryEndpoint = endpoint(primarySingle, "primary chest before double-chest formation");
			placeWithPlayer(level, player, primaryMain, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity primary = chest(level, primaryMain);
			LinkedStorageEndpointData formedPrimaryEndpoint = primary.getLinkedStorageEndpointData();
			int formedPrimarySlots = primary.getStorageWrapper().getInventoryHandler().size();
			require(isDoubleChest(level, primaryLeft, primaryMain) && primary.isPrimaryLinkedStorage() && primaryEndpoint.equals(formedPrimaryEndpoint)
					&& formedPrimarySlots == 54,
					"Primary linked endpoint did not transfer and expand during real-player double-chest formation: main=" + primary.isPrimaryLinkedStorage()
							+ ", endpoint=" + formedPrimaryEndpoint + ", slots=" + formedPrimarySlots);
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();

			player.setGameMode(GameType.SURVIVAL);
			clearDroppedItems(level, primaryMain);
			require(player.gameMode.destroyBlock(primaryMain), "Primary linked double-chest destruction failed");
			ItemStack droppedDoubleChest = findAndRemoveDroppedChest(level, primaryMain);
			require(level.getBlockState(primaryLeft).isAir() && level.getBlockState(primaryMain).isAir() && droppedDoubleChest.is(ModBlocks.CHEST_ITEM.get())
					&& ChestBlockItem.isDoubleChest(droppedDoubleChest)
					&& primaryEndpoint.equals(droppedDoubleChest.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& Boolean.TRUE.equals(droppedDoubleChest.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT))
					&& !hasDroppedItem(level, primaryMain, Items.DIAMOND),
					"Primary linked double-chest removal did not preserve one marked endpoint drop without duplicate contents");
			placeWithPlayer(level, player, primaryLeft, droppedDoubleChest);
			BlockPos restoredPrimaryLeft = primaryLeft.west();
			require(isDoubleChest(level, restoredPrimaryLeft, primaryLeft), "Deferred linked double-chest placement did not recreate the expected pair: left="
					+ level.getBlockState(restoredPrimaryLeft) + ", main=" + level.getBlockState(primaryLeft));
			ChestBlockEntity restoredPrimary = chest(level, primaryLeft);
			require(restoredPrimary.isPrimaryLinkedStorage() && primaryEndpoint.equals(endpoint(restoredPrimary, "restored primary double chest"))
					&& restoredPrimary.getStorageWrapper().getInventoryHandler().size() == 54
					&& count(restoredPrimary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
					"Deferred linked double-chest item restoration did not restore the primary endpoint and contents");
			clearDroppedItems(level, restoredPrimaryLeft);
			require(player.gameMode.destroyBlock(restoredPrimaryLeft), "Restored primary linked double-chest non-main destruction failed");
			ItemStack restoredPrimaryDrop = findAndRemoveDroppedChest(level, primaryLeft);
			require(level.getBlockState(restoredPrimaryLeft).isAir() && level.getBlockState(primaryLeft).isAir()
					&& ChestBlockItem.isDoubleChest(restoredPrimaryDrop)
					&& primaryEndpoint.equals(restoredPrimaryDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& Boolean.TRUE.equals(restoredPrimaryDrop.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT)),
					"Restored primary linked double-chest non-main removal did not preserve one primary endpoint carrier");

			player.setGameMode(GameType.CREATIVE);
			placeWithPlayer(level, player, secondaryPeer, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeWithPlayer(level, player, secondaryLeft, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity secondarySingle = chest(level, secondaryLeft);
			require(linkWithPlayer(player, barrel(level, secondaryPeer), secondaryPeer, secondaryLeft),
					"Could not link the secondary chest before double-chest formation");
			LinkedStorageEndpointData secondaryEndpoint = endpoint(secondarySingle, "secondary chest before double-chest formation");
			placeWithPlayer(level, player, secondaryMain, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity secondary = chest(level, secondaryMain);
			require(isDoubleChest(level, secondaryLeft, secondaryMain) && !secondary.isPrimaryLinkedStorage()
					&& secondaryEndpoint.equals(endpoint(secondary, "secondary double chest after endpoint transfer")),
					"Secondary linked endpoint did not transfer during real-player double-chest formation");
			player.setGameMode(GameType.SURVIVAL);
			ItemStack nonMainTierUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, nonMainTierUpgrade);
			require(player.gameMode.useItemOn(player, level, nonMainTierUpgrade, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(secondaryLeft), Direction.UP, secondaryLeft, false)) == InteractionResult.FAIL,
					"Linked secondary double-chest non-main half accepted a tier upgrade");
			require(nonMainTierUpgrade.getCount() == 1 && level.getBlockState(secondaryLeft).is(ModBlocks.CHEST.get())
					&& level.getBlockState(secondaryMain).is(ModBlocks.CHEST.get())
					&& secondaryEndpoint.equals(endpoint(chest(level, secondaryMain), "secondary double-chest main after rejected tier upgrade")),
					"Rejected linked secondary double-chest tier upgrade changed a chest or its endpoint");
			player.setGameMode(GameType.CREATIVE);
			level.setBlock(secondaryMain, Blocks.BARREL.defaultBlockState(), 3);
			require(level.getBlockState(secondaryLeft).is(ModBlocks.CHEST.get())
					&& level.getBlockState(secondaryLeft).getValue(ChestBlock.TYPE) == ChestType.SINGLE
					&& secondaryEndpoint.equals(endpoint(chest(level, secondaryLeft), "split secondary linked chest")),
					"Replacing a linked secondary double-chest half did not split and retain its endpoint");
			runLinkedSecondaryChestMainBreakNameRegression(level, player, primaryLeft.south(6));
			runLinkedSecondaryChestBreakRegression(level, player, primaryLeft.east(4).south(6), Direction.WEST, false);
			return true;
		} finally {
			player.setGameMode(gameType);
			clearArea(level, primaryLeft);
		}
	}

	private static void runLinkedSecondaryChestMainBreakNameRegression(ServerLevel level, ServerPlayer player, BlockPos primaryPos) {
		BlockPos originalSecondaryPos = primaryPos.east(5);
		BlockPos firstSurvivorPos = originalSecondaryPos.west();
		BlockPos secondSurvivorPos = firstSurvivorPos.west();
		placeWithPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeWithPlayer(level, player, originalSecondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
		BarrelBlockEntity linkedPrimary = barrel(level, primaryPos);
		require(linkWithPlayer(player, linkedPrimary, primaryPos, originalSecondaryPos),
				"Could not link the unnamed secondary chest before double-chest break regression");
		LinkedStorageEndpointData secondaryEndpoint = endpoint(chest(level, originalSecondaryPos),
				"unnamed secondary chest before double-chest break regression");
		List<ItemStack> canonicalContents = fillInventoryWithTestContents(linkedPrimary.getStorageWrapper().getInventoryHandler());
		linkedPrimary.getStorageWrapper().getInventoryHandler().saveInventory();
		placeWithPlayer(level, player, firstSurvivorPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
		require(isDoubleChest(level, firstSurvivorPos, originalSecondaryPos) && !chest(level, originalSecondaryPos).isPrimaryLinkedStorage(),
				"Real-player placement did not make the original linked secondary chest the unnamed double-chest main part");

		player.setGameMode(GameType.SURVIVAL);
		clearDroppedItems(level, originalSecondaryPos);
		require(player.gameMode.destroyBlock(originalSecondaryPos), "Unnamed secondary linked double-chest main destruction failed");
		List<ItemEntity> firstDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(originalSecondaryPos).inflate(1.5D));
		ItemStack firstDrop = firstDrops.size() == 1 ? firstDrops.getFirst().getItem().copy() : ItemStack.EMPTY;
		firstDrops.forEach(ItemEntity::discard);
		ChestBlockEntity firstSurvivor = chest(level, firstSurvivorPos);
		require(firstSurvivor.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
				&& secondaryEndpoint.equals(endpoint(firstSurvivor, "first unnamed secondary chest survivor"))
				&& hasInventoryContents(linkedPrimary.getStorageWrapper().getInventoryHandler(), canonicalContents) && firstSurvivor.getCustomName() == null
				&& firstDrops.size() == 1 && !ChestBlockItem.isDoubleChest(firstDrop) && firstDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
				&& firstDrop.get(DataComponents.CUSTOM_NAME) == null,
				"Breaking the unnamed linked secondary double-chest main did not preserve an unnamed endpoint survivor and plain physical drop");

		placeWithPlayer(level, player, secondSurvivorPos, firstDrop);
		require(isDoubleChest(level, secondSurvivorPos, firstSurvivorPos) && !firstSurvivor.isPrimaryLinkedStorage() && firstSurvivor.getCustomName() == null,
				"Placing the plain drop west of the survivor did not keep the former survivor as the unnamed linked secondary main part");
		clearDroppedItems(level, firstSurvivorPos);
		require(player.gameMode.destroyBlock(firstSurvivorPos), "Shifted unnamed secondary linked double-chest main destruction failed");
		List<ItemEntity> secondDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(firstSurvivorPos).inflate(1.5D));
		ItemStack secondDrop = secondDrops.size() == 1 ? secondDrops.getFirst().getItem().copy() : ItemStack.EMPTY;
		secondDrops.forEach(ItemEntity::discard);
		ChestBlockEntity secondSurvivor = chest(level, secondSurvivorPos);
		require(secondSurvivor.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
				&& secondaryEndpoint.equals(endpoint(secondSurvivor, "second unnamed secondary chest survivor"))
				&& hasInventoryContents(linkedPrimary.getStorageWrapper().getInventoryHandler(), canonicalContents) && secondSurvivor.getCustomName() == null
				&& secondDrops.size() == 1 && !ChestBlockItem.isDoubleChest(secondDrop) && secondDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
				&& secondDrop.get(DataComponents.CUSTOM_NAME) == null,
				"Breaking the shifted unnamed linked secondary double-chest main did not preserve an unnamed endpoint survivor and plain physical drop");
		require(LinkedStorageGroupsSavedData.get(level).manager().unregisterEndpoint(secondaryEndpoint.groupId(), secondaryEndpoint.endpointId()),
				"Could not clean up the unnamed secondary chest endpoint after the double-chest break regression");
		player.setGameMode(GameType.CREATIVE);
	}

	private static void runCreativeEndpointPlacementRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos sourcePos = player.blockPosition().offset(0, 0, 60);
			BlockPos creativePos = sourcePos.east(3);
			ItemStack originalMainHand = player.getMainHandItem().copy();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			clearArea(level, sourcePos);
			try {
				player.setGameMode(GameType.SURVIVAL);
				place(level, player, sourcePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity source = barrel(level, sourcePos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, source) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create the creative endpoint-placement linked-storage group");
				LinkedStorageEndpointData sourceEndpoint = endpoint(source, "creative endpoint source barrel");
				level.destroyBlock(sourcePos, true, player);
				ItemStack endpointDrop = level.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos)).stream()
						.filter(itemEntity -> sourceEndpoint.equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
						.map(itemEntity -> itemEntity.getItem().copy()).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing linked endpoint drop for creative placement"));
				level.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos)).forEach(ItemEntity::discard);

				player.setGameMode(GameType.CREATIVE);
				place(level, player, creativePos, endpointDrop);
				LinkedStorageEndpointData creativeEndpoint = endpoint(barrel(level, creativePos), "creative endpoint placement");
				require(creativeEndpoint.groupId().equals(sourceEndpoint.groupId()) && !creativeEndpoint.endpointId().equals(sourceEndpoint.endpointId())
						&& sourceEndpoint.equals(endpointDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
						"Creative placement did not create a fresh secondary endpoint without mutating its held carrier");
				return true;
			} finally {
				player.setGameMode(originalGameMode);
				player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
				clearArea(level, sourcePos);
			}
		});
	}

	private static void runComponentlessBarrelItemModelRegression() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			ItemStack componentlessBarrel = new ItemStack(ModBlocks.BARREL_ITEM.get());
			ItemStackRenderState expectedState = renderBarrelItemModel(minecraft, componentlessBarrel);
			String expectedParticle = NeoForgeModelDiagnostics.particleName(expectedState);
			ItemStack tintedBarrel = new ItemStack(ModBlocks.BARREL_ITEM.get());
			StorageBlockItem barrelItem = (StorageBlockItem) tintedBarrel.getItem();
			barrelItem.setMainColor(tintedBarrel, 0xFF00FF);
			barrelItem.setAccentColor(tintedBarrel, 0x00FFFF);
			renderBarrelItemModel(minecraft, tintedBarrel);
			ItemStackRenderState rerenderedState = renderBarrelItemModel(minecraft, componentlessBarrel);
			require(!expectedState.isEmpty() && !rerenderedState.isEmpty()
					&& Objects.equals(expectedParticle, NeoForgeModelDiagnostics.particleName(rerenderedState)),
					"A componentless barrel item inherited the previous barrel render state");
			return true;
		});
	}

	private static void runDroppedPrimaryRenameRegression() {
		DroppedPrimaryRenameFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 6);
			BlockPos secondaryPos = primaryPos.east(3);
			ItemStack originalHand = player.getMainHandItem().copy();
			GameType originalMode = player.gameMode.getGameModeForPlayer();
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create the dropped-primary rename group");
			LinkedStorageEndpointData primaryEndpoint = endpoint(primary, "dropped-primary rename barrel");
			String originalGroupName = linkedGroupName(level, primaryEndpoint);
			player.setGameMode(GameType.SURVIVAL);
			level.destroyBlock(primaryPos, true, player);
			ItemStack renamedPrimary = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos)).stream()
					.filter(itemEntity -> primaryEndpoint.equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
					.map(itemEntity -> itemEntity.getItem().copy()).findFirst()
					.orElseThrow(() -> new IllegalStateException("Breaking a linked primary barrel did not create an endpoint item"));
			require(!renamedPrimary.has(ModCoreDataComponents.STORAGE_UUID)
					&& Boolean.TRUE.equals(renamedPrimary.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT)),
					"Dropped linked primary barrel did not retain only its primary endpoint identity");
			String groupName = "Anvil Renamed Linked Storage";
			renamedPrimary.set(DataComponents.CUSTOM_NAME, Component.literal(groupName));
			level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos)).forEach(ItemEntity::discard);
			level.destroyBlock(secondaryPos, true, player);
			ItemStack renamedSecondary = level.getEntitiesOfClass(ItemEntity.class, new AABB(secondaryPos)).stream().map(ItemEntity::getItem)
					.filter(stack -> StorageBlockEntity.getLinkedStorageEndpointData(stack)
							.map(endpoint -> primaryEndpoint.groupId().equals(endpoint.groupId())
									&& !Boolean.TRUE.equals(stack.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT)))
							.orElse(false))
					.map(ItemStack::copy).findFirst()
					.orElseThrow(() -> new IllegalStateException("Breaking a linked secondary barrel did not create an endpoint item"));
			renamedSecondary.set(DataComponents.CUSTOM_NAME, Component.literal("Secondary Stack Name"));
			player.setItemInHand(InteractionHand.MAIN_HAND, renamedSecondary);
			return new DroppedPrimaryRenameFixture(primaryPos, secondaryPos, primaryEndpoint, originalGroupName, groupName, renamedPrimary, renamedSecondary,
					originalHand, originalMode, level.getGameTime() + 2L);
		});
		try {
			waitForServerCondition(player -> player.level().getGameTime() >= fixture.secondaryVerificationGameTime()
					&& linkedGroupName(player.level(), fixture.endpoint()).equals(fixture.originalGroupName())
					&& player.getMainHandItem().getHoverName().getString().equals(fixture.renamedSecondary().getHoverName().getString()));
			AutomationRuntime.runOnServer(player -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.renamedPrimary());
				return true;
			});
			waitForServerCondition(player -> linkedGroupName(player.level(), fixture.endpoint()).equals(fixture.groupName())
					&& player.getMainHandItem().getHoverName().getString().equals(fixture.groupName()));
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				BlockPos primaryPos = fixture.primaryPos();
				BlockPos secondaryPos = fixture.secondaryPos();
				place(level, player, secondaryPos, fixture.renamedSecondary());
				place(level, player, primaryPos, fixture.renamedPrimary());
				BarrelBlockEntity replacedPrimary = barrel(level, primaryPos);
				BarrelBlockEntity replacedSecondary = barrel(level, secondaryPos);
				require(fixture.endpoint().equals(endpoint(replacedPrimary, "replaced renamed primary"))
						&& replacedPrimary.getMenuDisplayName().getString().equals(fixture.groupName())
						&& replacedSecondary.getMenuDisplayName().getString().equals(fixture.groupName()),
						"Replaced renamed endpoints did not retain the canonical primary identity and title");
				level.destroyBlock(primaryPos, true, player);
				ItemStack redroppedPrimary = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos)).stream()
						.filter(itemEntity -> fixture.endpoint().equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
						.map(ItemEntity::getItem).findFirst()
						.orElseThrow(() -> new IllegalStateException("Placed renamed primary did not retain an endpoint drop"));
				require(redroppedPrimary.getHoverName().getString().equals(fixture.groupName()),
						"Placed renamed primary lost its custom name on the next endpoint drop");
				return true;
			});
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.setGameMode(fixture.originalMode());
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalHand());
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static String linkedGroupName(ServerLevel level, LinkedStorageEndpointData endpoint) {
		return LinkedStorageGroupsSavedData.get(level).manager().resolveVirtualHost(endpoint.groupId())
				.flatMap(ILinkedStorageVirtualHost::getLinkedStorageDisplayName).map(Component::getString)
				.orElseThrow(() -> new IllegalStateException("Missing linked-storage host for " + endpoint.groupId()));
	}

	private static ItemStackRenderState renderBarrelItemModel(Minecraft minecraft, ItemStack stack) {
		ItemStackRenderState state = new ItemStackRenderState();
		minecraft.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, minecraft.level, minecraft.player, 0);
		return state;
	}

	private static void runLinkedControllerNonListenerRemovalIndexesRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 132);
			BlockPos primaryPos = controllerPos.east();
			BlockPos secondaryPos = primaryPos.east();
			clearArea(level, controllerPos);
			try {
				place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = barrel(level, primaryPos);
				BarrelBlockEntity secondary = barrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked controller non-listener fixture");
				place(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = controller(level, controllerPos);
				require(controller.getStoragePositions().equals(List.of(primaryPos)), "Controller did not elect the linked primary as its listener source");
				level.destroyBlock(secondaryPos, false, player);
				require(controller.getStoragePositions().equals(List.of(primaryPos)),
						"Destroying a linked non-listener changed the surviving controller group");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
				require(controller.hasMatchingStack(diamondKey) && controller.getStackStorages(diamondKey).equals(List.of(primaryPos)),
						"Surviving linked listener did not update the controller item index");
				MemorySettingsCategory memory = primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.EMERALD));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				memory.selectSlot(1);
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, ItemStack.EMPTY);
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				ItemStackKey emeraldKey = ItemStackKey.of(new ItemStack(Items.EMERALD));
				require(controller.hasMatchingItem(Items.EMERALD) && controller.getItemStorages(emeraldKey).equals(List.of(primaryPos)),
						"Surviving linked listener did not update the controller memory index");
				IStorageWrapper linkedPrimary = primary.getMenuStorageWrapper();
				UpgradeHandler upgrades = linkedPrimary.getUpgradeHandler();
				upgrades.setStackInSlot(0, new ItemStack(ModItems.ADVANCED_FILTER_UPGRADE.get()));
				FilterUpgradeWrapper filter = upgrades.getWrappersThatImplement(FilterUpgradeWrapper.class).stream().findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing linked controller filter upgrade wrapper"));
				filter.setDirection(net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction.INPUT);
				filter.getFilterLogic().setDepositFilterType(ContentsFilterType.ALLOW);
				filter.getFilterLogic().setPrimaryMatch(PrimaryMatch.ITEM);
				filter.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
				upgrades.saveInventory();
				linkedPrimary.refreshInventoryForInputOutput();
				ItemStackKey goldKey = ItemStackKey.of(new ItemStack(Items.GOLD_INGOT));
				require(controller.hasMatchingFilter(new ItemStack(Items.GOLD_INGOT)) && controller.getEmptyTargetSlotStorages(goldKey).contains(primaryPos),
						"Surviving linked listener did not update the controller filter or empty-slot indexes");
				return true;
			} finally {
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedSecondaryChestBreakRegression(ServerLevel level, ServerPlayer player, BlockPos secondaryPos, Direction addedChestDirection,
			boolean breakMainChest) {
		BlockPos secondaryPeerPos = secondaryPos.south(3);
		BlockPos addedChestPos = secondaryPos.relative(addedChestDirection);
		placeWithPlayer(level, player, secondaryPeerPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeWithPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
		BarrelBlockEntity linkedPrimary = barrel(level, secondaryPeerPos);
		require(linkWithPlayer(player, linkedPrimary, secondaryPeerPos, secondaryPos),
				"Could not link the secondary chest before double-chest break regression");
		LinkedStorageEndpointData secondaryEndpoint = endpoint(chest(level, secondaryPos), "secondary chest before double-chest break regression");
		List<ItemStack> canonicalContents = fillInventoryWithTestContents(linkedPrimary.getStorageWrapper().getInventoryHandler());
		linkedPrimary.getStorageWrapper().getInventoryHandler().saveInventory();
		placeWithPlayer(level, player, addedChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
		ChestBlockEntity secondaryMain = chest(level, secondaryPos).isMainChest() ? chest(level, secondaryPos) : chest(level, addedChestPos);
		BlockPos brokenChestPos = breakMainChest
				? secondaryMain.getBlockPos()
				: secondaryMain.getBlockPos().equals(secondaryPos) ? addedChestPos : secondaryPos;
		BlockPos remainingChestPos = brokenChestPos.equals(secondaryPos) ? addedChestPos : secondaryPos;
		player.setGameMode(GameType.SURVIVAL);
		clearDroppedItems(level, brokenChestPos);
		require(player.gameMode.destroyBlock(brokenChestPos), "Secondary linked double-chest destruction failed");
		List<ItemEntity> brokenDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(brokenChestPos).inflate(1.5D));
		ItemStack brokenChestDrop = brokenDrops.size() == 1 ? brokenDrops.getFirst().getItem().copy() : ItemStack.EMPTY;
		brokenDrops.forEach(ItemEntity::discard);
		ChestBlockEntity remainingChest = chest(level, remainingChestPos);
		require(remainingChest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
				&& secondaryEndpoint.equals(endpoint(remainingChest, "remaining secondary chest after double-chest break"))
				&& hasInventoryContents(linkedPrimary.getStorageWrapper().getInventoryHandler(), canonicalContents) && brokenDrops.size() == 1
				&& !ChestBlockItem.isDoubleChest(brokenChestDrop) && brokenChestDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null,
				"Breaking a linked secondary double-chest half changed or dropped linked inventory contents");
		clearDroppedItems(level, remainingChestPos);
		require(player.gameMode.destroyBlock(remainingChestPos), "Remaining linked secondary chest destruction failed");
		ItemStack remainingChestDrop = findAndRemoveDroppedChest(level, remainingChestPos);
		require(!ChestBlockItem.isDoubleChest(remainingChestDrop)
				&& secondaryEndpoint.equals(remainingChestDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
				"Breaking the remaining linked secondary chest did not preserve its endpoint drop");
		player.setGameMode(GameType.CREATIVE);
	}

	private static void waitForClientLimitedBarrelReloadProjection() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.level == null || minecraft.player == null)
					return false;
				BlockPos primaryPos = limitedPrimaryPos(minecraft.player);
				return minecraft.level.getBlockEntity(primaryPos) instanceof LimitedBarrelBlockEntity primary
						&& minecraft.level.getBlockEntity(primaryPos.east(3)) instanceof LimitedBarrelBlockEntity secondary
						&& hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary);
			}))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Reloaded linked limited barrels did not synchronize display items, counts, and fill levels to the client");
	}

	private static boolean hasLimitedRenderProjection(LimitedBarrelBlockEntity barrel) {
		return barrel.getStorageWrapper().getRenderDataHandler().getDisplayData().displayItems().stream().anyMatch(item -> item.item().is(Items.DIAMOND))
				&& barrel.getSlotCounts().contains(LINKED_LIMITED_RELOAD_ITEM_COUNT) && barrel.getSlotFillLevels().stream().anyMatch(level -> level > 0F);
	}

	private static BlockPos limitedPrimaryPos(net.minecraft.world.entity.player.Player player) {
		return player.blockPosition().offset(0, 0, 4);
	}
	private static LinkedStorageEndpointData endpoint(StorageBlockEntity storage, String name) {
		LinkedStorageEndpointData result = storage.getLinkedStorageEndpointData();
		if (result == null)
			throw new IllegalStateException("Missing linked-storage endpoint data for " + name);
		return result;
	}

	private static Boolean runLinkedShulkerStashAndFamilyCompatibility(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos shulkerPos = player.blockPosition().offset(0, 0, 28);
		BlockPos barrelPos = shulkerPos.east(3);
		clearArea(level, shulkerPos);
		try {
			place(level, player, shulkerPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			place(level, player, barrelPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			ShulkerBoxBlockEntity shulker = level.getBlockEntity(shulkerPos, ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get())
					.orElseThrow(() -> new IllegalStateException("Missing shulker stash endpoint"));
			BarrelBlockEntity barrel = barrel(level, barrelPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, shulker) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, barrel) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create shulker-primary linked storage fixture");
			ItemStack carrier = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
			shulker.copyLinkedStorageEndpointTo(carrier);
			ShulkerBoxItem shulkerItem = (ShulkerBoxItem) carrier.getItem();
			try (Transaction transaction = Transaction.openRoot()) {
				require(shulkerItem.stash(level.registryAccess(), carrier, ItemResource.of(new ItemStack(Items.DIAMOND)), 7, transaction) == 7,
						"Linked shulker carrier did not stash into canonical contents");
				transaction.commit();
			}
			require(Boolean.TRUE.equals(carrier.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT))
					&& count(shulker.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
					&& count(barrel.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
					"Shulker stash did not retain primary role and shared canonical contents");
			try (Transaction transaction = Transaction.openRoot()) {
				require(shulkerItem.stash(level.registryAccess(), carrier, ItemResource.of(new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get())), 1,
						transaction) == 0, "Linked shulker carrier accepted a nested shulker");
			}
			return true;
		} finally {
			clearArea(level, shulkerPos);
		}
	}

	private static Boolean runStorageFamilyCompatibilityRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos barrelPos = player.blockPosition().offset(0, 0, 36);
		BlockPos shulkerPos = barrelPos.east(3);
		BlockPos limitedOnePos = barrelPos.south(3);
		BlockPos limitedCopperOnePos = limitedOnePos.east(3);
		BlockPos limitedTwoPos = limitedCopperOnePos.east(3);
		BlockPos incompatibleStandardPos = limitedTwoPos.east(3);
		GameType originalMode = player.gameMode.getGameModeForPlayer();
		clearArea(level, barrelPos);
		try {
			place(level, player, barrelPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, shulkerPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			BarrelBlockEntity barrel = barrel(level, barrelPos);
			ShulkerBoxBlockEntity shulker = level.getBlockEntity(shulkerPos, ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get())
					.orElseThrow(() -> new IllegalStateException("Missing linked family shulker"));
			ItemStack standardLinker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), standardLinker, barrel) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), standardLinker, shulker) == LinkedStorageService.LinkResult.SUCCESS,
					"Standard Barrel and Shulker did not form a compatible linked-storage family");
			LinkedStorageEndpointData shulkerEndpoint = endpoint(shulker, "family shulker");
			barrel.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			barrel.getStorageWrapper().getInventoryHandler().saveInventory();
			require(count(shulker.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
					"Compatible standard families did not expose canonical contents");

			level.destroyBlock(shulkerPos, true, player);
			List<ItemEntity> shulkerDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(shulkerPos).inflate(2),
					itemEntity -> itemEntity.getItem().is(ModBlocks.SHULKER_BOX_ITEM.get()));
			require(shulkerDrops.size() == 1 && shulkerEndpoint.equals(shulkerDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& shulkerDrops.getFirst().getItem().get(ModCoreDataComponents.STORAGE_UUID) == null,
					"Linked Shulker break did not retain only the endpoint carrier");
			ItemStack shulkerCarrier = shulkerDrops.getFirst().getItem().copy();
			shulkerDrops.forEach(ItemEntity::discard);
			player.setGameMode(GameType.SURVIVAL);
			place(level, player, shulkerPos, shulkerCarrier);
			ShulkerBoxBlockEntity restoredShulker = level.getBlockEntity(shulkerPos, ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get())
					.orElseThrow(() -> new IllegalStateException("Missing restored linked family shulker"));
			require(shulkerEndpoint.equals(endpoint(restoredShulker, "restored family shulker"))
					&& count(restoredShulker.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
					"Linked Shulker did not restore its endpoint and canonical contents");

			BlockPos chestLeftPos = barrelPos.south(6);
			BlockPos chestMainPos = chestLeftPos.east();
			placeWithPlayer(level, player, chestLeftPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeWithPlayer(level, player, chestMainPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			require(isDoubleChest(level, chestLeftPos, chestMainPos), "Family compatibility fixture did not create a double chest");
			ChestBlockEntity chestMain = chest(level, chestMainPos);
			ChestBlockEntity chestLeft = chest(level, chestLeftPos);
			ItemStack chestLinker = new ItemStack(ENDER_LINKER.get());
			require(useLinker(player, chestLinker, chestLeftPos).consumesAction() && chestMain.isLinkedStorage() && !chestLeft.isLinkedStorage(),
					"Linker interaction on the non-main chest part did not route only to the main endpoint");
			ItemStack packingTape = new ItemStack(ModItems.PACKING_TAPE.get());
			Vec3 originalPackingTapePosition = player.position();
			try {
				require(player.teleportTo(level, chestLeftPos.getX() + .5D, chestLeftPos.getY() + 1D, chestLeftPos.getZ() + .5D, Set.of(), player.getYRot(),
						player.getXRot(), false), "Could not position player for non-main packing-tape interaction");
				player.setItemInHand(InteractionHand.MAIN_HAND, packingTape);
				InteractionResult packingTapeResult = player.gameMode.useItemOn(player, level, packingTape, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(chestLeftPos), Direction.UP, chestLeftPos, false));
				require(packingTapeResult == InteractionResult.PASS && chestMain.isLinkedStorage() && !chestMain.isPacked() && !chestLeft.isPacked()
						&& packingTape.getDamageValue() == 0,
						"Packing tape on a non-main linked chest part did not use the target PASS rejection path without changing linked or packed state: result="
								+ packingTapeResult + ", linked=" + chestMain.isLinkedStorage() + ", mainPacked=" + chestMain.isPacked() + ", leftPacked="
								+ chestLeft.isPacked() + ", damage=" + packingTape.getDamageValue());
			} finally {
				player.teleportTo(level, originalPackingTapePosition.x, originalPackingTapePosition.y, originalPackingTapePosition.z, Set.of(),
						player.getYRot(), player.getXRot(), false);
			}

			place(level, player, limitedOnePos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			place(level, player, limitedCopperOnePos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			place(level, player, limitedTwoPos, new ItemStack(ModBlocks.LIMITED_BARREL_2_ITEM.get()));
			place(level, player, incompatibleStandardPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			LimitedBarrelBlockEntity limitedOne = limited(level, limitedOnePos);
			LimitedBarrelBlockEntity limitedCopperOne = limited(level, limitedCopperOnePos);
			LimitedBarrelBlockEntity limitedTwo = limited(level, limitedTwoPos);
			BarrelBlockEntity incompatibleStandard = barrel(level, incompatibleStandardPos);
			ItemStack limitedLinker = new ItemStack(ENDER_LINKER.get(), 4);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker, limitedOne) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker, limitedCopperOne) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker,
							limitedTwo) == LinkedStorageService.LinkResult.INCOMPATIBLE_ENDPOINT
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker,
							incompatibleStandard) == LinkedStorageService.LinkResult.INCOMPATIBLE_ENDPOINT,
					"Limited barrels did not retain matching-family-only linking");
			limitedOne.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, LINKED_LIMITED_RELOAD_ITEM_COUNT));
			limitedOne.getStorageWrapper().getInventoryHandler().saveInventory();
			require(limitedOne.getSlotCounts().contains(LINKED_LIMITED_RELOAD_ITEM_COUNT)
					&& limitedCopperOne.getSlotCounts().contains(LINKED_LIMITED_RELOAD_ITEM_COUNT)
					&& limitedCopperOne.getSlotFillLevels().stream().anyMatch(fillLevel -> fillLevel > 0F) && !limitedTwo.isLinkedStorage()
					&& !incompatibleStandard.isLinkedStorage(), "Limited family compatibility did not retain canonical render projection and rejection");
			return true;
		} finally {
			player.setGameMode(originalMode);
			clearArea(level, barrelPos);
		}
	}

	private static Boolean runEndpointUnloadReloadRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 72);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos);
		try {
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create endpoint unload/reload fixture");
			LinkedStorageEndpointData primaryEndpoint = endpoint(primary, "unload/reload primary");
			LinkedStorageEndpointData secondaryEndpoint = endpoint(secondary, "unload/reload secondary");
			CompoundTag secondaryData = ValueIOHelper.collectOutputToTag(level.registryAccess(), secondary::saveAdditional);
			secondary.onChunkUnloaded();
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.LAPIS_LAZULI, 5));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			level.removeBlockEntity(secondaryPos);
			BarrelBlockEntity reloadedSecondary = new BarrelBlockEntity(secondaryPos, level.getBlockState(secondaryPos));
			reloadedSecondary.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), secondaryData));
			level.setBlockEntity(reloadedSecondary);
			reloadedSecondary.onLoad();
			require(secondaryEndpoint.equals(endpoint(reloadedSecondary, "reloaded secondary"))
					&& count(reloadedSecondary.getStorageWrapper().getInventoryHandler(), Items.LAPIS_LAZULI) == 5,
					"Reloaded secondary did not restore endpoint identity and canonical contents");
			CompoundTag primaryData = ValueIOHelper.collectOutputToTag(level.registryAccess(), primary::saveAdditional);
			primary.onChunkUnloaded();
			level.removeBlockEntity(primaryPos);
			BarrelBlockEntity reloadedPrimary = new BarrelBlockEntity(primaryPos, level.getBlockState(primaryPos));
			reloadedPrimary.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), primaryData));
			level.setBlockEntity(reloadedPrimary);
			reloadedPrimary.onLoad();
			reloadedSecondary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.AMETHYST_SHARD, 4));
			reloadedSecondary.getStorageWrapper().getInventoryHandler().saveInventory();
			require(primaryEndpoint.equals(endpoint(reloadedPrimary, "reloaded primary"))
					&& count(reloadedPrimary.getStorageWrapper().getInventoryHandler(), Items.LAPIS_LAZULI) == 5
					&& count(reloadedPrimary.getStorageWrapper().getInventoryHandler(), Items.AMETHYST_SHARD) == 4,
					"Reloaded primary did not restore endpoint identity and canonical contents");
			return true;
		} finally {
			clearArea(level, primaryPos);
		}
	}

	private static Boolean runLinkedControllerTopologyAndLockedRoutingRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 96);
		BlockPos firstPos = controllerPos.east();
		BlockPos secondPos = firstPos.east();
		BlockPos primaryPos = secondPos.east();
		BlockPos regularPos = primaryPos.east();
		clearArea(level, controllerPos);
		try {
			for (BlockPos pos : List.of(firstPos, secondPos, primaryPos, regularPos)) {
				place(level, player, pos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			}
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity first = barrel(level, firstPos);
			BarrelBlockEntity second = barrel(level, secondPos);
			BarrelBlockEntity regular = barrel(level, regularPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 3);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, first) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, second) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create controller topology linked group");
			place(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			ControllerBlockEntity controller = controller(level, controllerPos);
			require(controller.getStoragePositions().size() == 2
					&& controller.getStorageBlockPositions().containsAll(Set.of(firstPos, secondPos, primaryPos, regularPos)),
					"Controller did not register linked topology and regular fallback");
			controller.toggleLock();
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			try (Transaction transaction = Transaction.openRoot()) {
				require(controller.insert(ItemResource.of(new ItemStack(Items.OAK_PLANKS)), 1, transaction) == 1,
						"Controller did not route a matching item into a locked linked canonical inventory");
				transaction.commit();
			}
			try (Transaction transaction = Transaction.openRoot()) {
				require(controller.insert(ItemResource.of(new ItemStack(Items.BIRCH_PLANKS)), 1, transaction) == 0,
						"Controller routed a new item while every physical target was locked");
			}
			first.toggleLock();
			try (Transaction transaction = Transaction.openRoot()) {
				require(controller.insert(ItemResource.of(new ItemStack(Items.BIRCH_PLANKS)), 1, transaction) == 1,
						"Controller did not route a new item after the linked secondary unlocked");
				transaction.commit();
			}
			level.destroyBlock(secondPos, false, player);
			require(first.getControllerPos().filter(controllerPos::equals).isPresent() && primary.getControllerPos().isEmpty()
					&& regular.getControllerPos().isEmpty() && controller.getStorageBlockPositions().equals(Set.of(firstPos)),
					"Controller did not prune the disconnected linked branch and downstream topology");
			place(level, player, secondPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			require(primary.getControllerPos().filter(controllerPos::equals).isPresent() && regular.getControllerPos().filter(controllerPos::equals).isPresent()
					&& controller.getStorageBlockPositions().containsAll(Set.of(firstPos, secondPos, primaryPos, regularPos)),
					"Controller did not reconnect the linked branch and downstream topology after bridge restoration");
			return true;
		} finally {
			clearArea(level, controllerPos);
		}
	}

	private static Boolean runLinkedStorageTierUpgradeRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 120);
		BlockPos secondaryPos = primaryPos.east(3);
		GameType originalMode = player.gameMode.getGameModeForPlayer();
		ItemStack originalHand = player.getMainHandItem().copy();
		clearArea(level, primaryPos);
		try {
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create tier-upgrade linked group");
			LinkedStorageEndpointData primaryEndpoint = endpoint(primary, "tier primary");
			LinkedStorageEndpointData secondaryEndpoint = endpoint(secondary, "tier secondary");
			player.setGameMode(GameType.SURVIVAL);
			ItemStack rejected = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, rejected);
			require(!player.gameMode.useItemOn(player, level, rejected, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(secondaryPos), Direction.UP, secondaryPos, false)).consumesAction() && rejected.getCount() == 1,
					"Secondary linked endpoint accepted a tier upgrade");
			ItemStack accepted = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, accepted);
			require(player.gameMode.useItemOn(player, level, accepted, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(primaryPos), Direction.UP, primaryPos, false)).consumesAction() && accepted.isEmpty(),
					"Primary linked endpoint did not accept its tier upgrade");
			BarrelBlockEntity upgradedPrimary = barrel(level, primaryPos);
			BarrelBlockEntity upgradedSecondary = barrel(level, secondaryPos);
			require(primaryEndpoint.groupId().equals(endpoint(upgradedPrimary, "upgraded primary").groupId())
					&& secondaryEndpoint.groupId().equals(endpoint(upgradedSecondary, "upgraded secondary").groupId())
					&& upgradedPrimary.getStorageWrapper().getInventoryHandler().size() == ModBlocks.DIAMOND_BARREL.get().getNumberOfInventorySlots(),
					"Primary tier upgrade did not preserve linked-group membership and canonical capacity");
			return true;
		} finally {
			player.setGameMode(originalMode);
			player.setItemInHand(InteractionHand.MAIN_HAND, originalHand);
			clearArea(level, primaryPos);
		}
	}

	private static void runTierUpgradeMenuInvalidationRegression() {
		LinkedStorageTierUpgradeFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 3);
			BlockPos secondaryPos = primaryPos.east(3);
			GameType originalMode = player.gameMode.getGameModeForPlayer();
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create tier-menu invalidation fixture");
			return new LinkedStorageTierUpgradeFixture(primaryPos, secondaryPos, endpoint(primary, "tier menu primary"), profile(primary), originalMode);
		});
		try {
			AutomationRuntime.runOnServer(player -> {
				player.setGameMode(GameType.SURVIVAL);
				return true;
			});
			LinkedStorageMenuFixture menuFixture = new LinkedStorageMenuFixture(fixture.primaryPos(), fixture.secondaryPos(), fixture.endpoint().groupId(),
					fixture.profile(), 0);
			waitForClientLinkedStorageEndpoint(menuFixture, fixture.primaryPos());
			openLinkedStorageMenu(menuFixture, fixture.primaryPos());
			waitForClientLinkedStorageMenu(menuFixture, fixture.primaryPos());
			AutomationRuntime.runOnServer(player -> {
				ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
				InteractionResult result = player.gameMode.useItemOn(player, player.level(), upgrade, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()), Direction.UP, fixture.primaryPos(), false));
				require(upgrade.getCount() == 1 && player.containerMenu instanceof StorageContainerMenu,
						"Primary tier upgrade consumed an item or closed its linked menu: result=" + result + ", upgrade=" + upgrade + ", menu="
								+ player.containerMenu.getClass().getSimpleName());
				player.closeContainer();
				return true;
			});
			waitForClosedStorageMenu();
			AutomationRuntime.runOnServer(player -> {
				ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
				require(player.gameMode
						.useItemOn(player, player.level(), upgrade, InteractionHand.MAIN_HAND,
								new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()), Direction.UP, fixture.primaryPos(), false))
						.consumesAction() && upgrade.isEmpty(), "Primary tier upgrade did not succeed after closing its linked menu");
				return true;
			});
			LinkedStorageSnapshotProfile upgradedProfile = AutomationRuntime.runOnServer(player -> profile(barrel(player.level(), fixture.primaryPos())));
			LinkedStorageMenuFixture upgradedMenuFixture = new LinkedStorageMenuFixture(fixture.primaryPos(), fixture.secondaryPos(),
					fixture.endpoint().groupId(), upgradedProfile, 0);
			openLinkedStorageMenu(upgradedMenuFixture, fixture.secondaryPos());
			waitForClientLinkedStorageMenu(upgradedMenuFixture, fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			LinkedStorageMenuFixture secondaryFixture = AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				BlockPos primaryPos = fixture.primaryPos();
				BlockPos secondaryPos = primaryPos.east(3);
				clearArea(level, primaryPos);
				place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = barrel(level, primaryPos);
				BarrelBlockEntity secondary = barrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create secondary tier-menu invalidation fixture");
				return new LinkedStorageMenuFixture(primaryPos, secondaryPos, endpoint(primary, "secondary tier menu primary").groupId(), profile(primary), 0);
			});
			try {
				waitForClientLinkedStorageEndpoint(secondaryFixture, secondaryFixture.secondaryPos());
				openLinkedStorageMenu(secondaryFixture, secondaryFixture.secondaryPos());
				waitForClientLinkedStorageMenu(secondaryFixture, secondaryFixture.secondaryPos());
				AutomationRuntime.runOnServer(player -> {
					ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
					player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
					require(player.gameMode
							.useItemOn(player, player.level(), upgrade, InteractionHand.MAIN_HAND,
									new BlockHitResult(Vec3.atCenterOf(secondaryFixture.primaryPos()), Direction.UP, secondaryFixture.primaryPos(), false))
							.consumesAction() && upgrade.isEmpty(), "Primary tier upgrade did not invalidate the open secondary linked menu");
					return true;
				});
				waitForClosedStorageMenu();
				LinkedStorageSnapshotProfile secondaryUpgradedProfile = AutomationRuntime
						.runOnServer(player -> profile(barrel(player.level(), secondaryFixture.primaryPos())));
				LinkedStorageMenuFixture secondaryUpgradedMenu = new LinkedStorageMenuFixture(secondaryFixture.primaryPos(), secondaryFixture.secondaryPos(),
						secondaryFixture.groupId(), secondaryUpgradedProfile, 0);
				openLinkedStorageMenu(secondaryUpgradedMenu, secondaryFixture.secondaryPos());
				waitForClientLinkedStorageMenu(secondaryUpgradedMenu, secondaryFixture.secondaryPos());
			} finally {
				AutomationRuntime.runOnServer(player -> {
					player.closeContainer();
					clearArea(player.level(), secondaryFixture.primaryPos());
					return true;
				});
			}
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setGameMode(fixture.originalMode());
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void runLinkedStorageMenuTransitionRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			// Target menu validation closes remote server-opened menus. Keep this normal
			// interaction fixture in range instead of using the source teleport path.
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 2);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked settings-menu fixture");
			return new LinkedStorageMenuFixture(primaryPos, secondaryPos, endpoint(primary, "settings menu primary").groupId(), profile(primary), 0);
		});
		try {
			waitForClientLinkedStorageEndpoint(fixture, fixture.secondaryPos());
			openLinkedStorageMenu(fixture, fixture.secondaryPos());
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				if (!(player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Linked inventory menu was missing before stale settings action");
				}
				CompoundTag staleAction = new CompoundTag();
				staleAction.putString("action", "openSettings");
				staleAction.putInt("sourceContainerId", menu.containerId + 1);
				menu.handlePacket(staleAction);
				require(player.containerMenu == menu, "Stale source-container settings action replaced the active linked inventory menu: source="
						+ staleAction.getInt("sourceContainerId").orElse(Integer.MIN_VALUE) + ", menu=" + menu.containerId);
				return true;
			});
			openClientStorageSettings();
			waitForClientSettingsMenu(fixture.secondaryPos());
			AutomationRuntime.runOnClient(() -> {
				ClientPacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.primaryPos()));
				return true;
			});
			waitForServerCondition(
					player -> player.containerMenu instanceof StorageSettingsContainerMenu menu && menu.getBlockPosition().equals(fixture.secondaryPos()));
			AutomationRuntime.runOnClient(() -> {
				ClientPacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.secondaryPos()));
				return true;
			});
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedStorageMenu();
			AutomationRuntime.runOnServer(player -> {
				require(!barrel(player.level(), fixture.secondaryPos()).isOpen(),
						"Linked settings opener was not removed after the inventory transition closed");
				return true;
			});
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void runLinkedUpgradeRefreshRegression() {
		LinkedUpgradeRefreshFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			// The target menu validates the ordinary interaction range every tick. Keep this
			// fixture nearby because target teleportation changes the menu slot shape.
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 2);
			BlockPos secondaryPos = primaryPos.east(3);
			int playerUpgradeSlot = player.getInventory().getSelectedSlot() == 0 ? 1 : 0;
			ItemStack originalUpgradeStack = player.getInventory().getItem(playerUpgradeSlot).copy();
			ItemStack originalMainHand = player.getMainHandItem().copy();
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked upgrade-refresh fixture");
			player.getInventory().setItem(playerUpgradeSlot, new ItemStack(ModItems.ADVANCED_FILTER_UPGRADE.get()));
			return new LinkedUpgradeRefreshFixture(primaryPos, secondaryPos, endpoint(primary, "upgrade-refresh primary").groupId(), profile(primary),
					playerUpgradeSlot, originalUpgradeStack, originalMainHand);
		});
		try {
			LinkedStorageMenuFixture menuFixture = new LinkedStorageMenuFixture(fixture.primaryPos(), fixture.secondaryPos(), fixture.groupId(),
					fixture.profile(), 0);
			waitForClientLinkedStorageEndpoint(menuFixture, fixture.primaryPos());
			AutomationRuntime.runOnServer(player -> {
				ItemStack originalMainHand = player.getMainHandItem().copy();
				InteractionResult interactionResult;
				try {
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					interactionResult = player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()).add(0, .5, 0), Direction.UP, fixture.primaryPos(), false));
				} finally {
					player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
				}
				require(interactionResult.consumesAction() && player.containerMenu instanceof StorageContainerMenu,
						"Could not open linked primary storage through its normal interaction for upgrade-refresh regression");
				return true;
			});
			waitForClientLinkedStorageMenu(menuFixture, fixture.primaryPos());
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null || minecraft.gameMode == null || !(minecraft.player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Client linked primary storage menu was unavailable for upgrade-refresh regression");
				}
				Slot playerUpgradeSlot = menu.slots.stream().filter(slot -> slot.getItem().is(ModItems.ADVANCED_FILTER_UPGRADE.get())).findFirst()
						.orElseThrow(() -> new IllegalStateException("Client player inventory did not contain the Advanced Filter Upgrade"));
				minecraft.gameMode.handleInventoryMouseClick(menu.containerId, playerUpgradeSlot.index, 0, ClickType.PICKUP, minecraft.player);
				minecraft.gameMode.handleInventoryMouseClick(menu.containerId, menu.getFirstUpgradeSlot(), 0, ClickType.PICKUP, minecraft.player);
				return true;
			});
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (System.nanoTime() < deadline) {
				if (AutomationRuntime.runOnClient(
						() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu
								&& menu.getBlockPosition().filter(fixture.primaryPos()::equals).isPresent()
								&& menu.getSlot(menu.getFirstUpgradeSlot()).getItem().is(ModItems.ADVANCED_FILTER_UPGRADE.get()) && menu.canDisableUpgrade(0)
								&& Minecraft.getInstance().screen instanceof StorageScreenBase<?> screen
								&& screen.children().stream().anyMatch(child -> child instanceof ToggleButton<?> button
										&& button.getX() == screen.getGuiLeft() - 22 && button.getY() == screen.getGuiTop() + 8)))
					return;
				sleep(50);
			}
			throw new IllegalStateException("Linked Advanced Filter Upgrade did not refresh the open primary menu");
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), fixture.primaryPos());
				player.getInventory().setItem(fixture.playerUpgradeSlot(), fixture.originalUpgradeStack());
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalMainHand());
				return true;
			});
		}
	}

	private static void openClientStorageSettings() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
						|| !(screen.getMenu() instanceof StorageContainerMenu menu)) {
					return false;
				}
				menu.openSettings();
				return true;
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client linked storage inventory menu is missing before settings transition");
	}

	private static void waitForClientSettingsMenu(BlockPos expectedPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageSettingsContainerMenu menu
							&& menu.getBlockPosition().equals(expectedPos)))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Linked storage settings menu did not retain its endpoint during the menu transition");
	}

	private static void requirePersistedNestedEndpoint(ServerLevel level, StorageBlockEntity storage, LinkedStorageEndpointData endpoint,
			LinkedStorageEndpointRole expectedRole) {
		CompoundTag saved = ValueIOHelper.collectOutputToTag(level.registryAccess(), storage::saveAdditional);
		CompoundTag endpointTag = saved.getCompound("linkedStorageEndpoint")
				.orElseThrow(() -> new IllegalStateException("Reloaded storage block entity did not persist a nested linkedStorageEndpoint compound"));
		require(endpoint.groupId().equals(endpointTag.read("groupId", UUIDUtil.CODEC).orElse(null))
				&& endpoint.endpointId().equals(endpointTag.read("endpointId", UUIDUtil.CODEC).orElse(null))
				&& endpointTag.getBooleanOr("primary", false) == (expectedRole == LinkedStorageEndpointRole.PRIMARY) && !saved.contains("primary"),
				"Reloaded storage block entity did not persist the nested endpoint identity and role schema for " + expectedRole + ": " + saved);
	}
	private static BarrelBlockEntity barrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing barrel at " + pos));
	}
	private static LimitedBarrelBlockEntity limited(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing limited barrel at " + pos));
	}
	private static ChestBlockEntity chest(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing chest at " + pos));
	}
	private static ControllerBlockEntity controller(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing controller at " + pos));
	}
	private static void place(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem))
			throw new IllegalStateException("Regression fixture item is not placeable at " + pos);
		level.setBlock(pos, blockItem.getBlock().defaultBlockState(), 3);
		blockItem.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), player, stack);
	}
	private static void placeWithPlayer(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		BlockPos support = pos.below();
		level.setBlock(support, Blocks.DIRT.defaultBlockState(), 3);
		player.setYRot(0);
		player.setXRot(0);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		require(player.gameMode
				.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false))
				.consumesAction(), "Player placement did not consume interaction at " + pos);
	}
	private static boolean linkWithPlayer(ServerPlayer player, StorageBlockEntity primary, BlockPos primaryPos, BlockPos secondaryPos) {
		ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
		if (!useLinker(player, linker, primaryPos).consumesAction())
			return false;
		UUID groupId = endpoint(primary, "player-linked primary").groupId();
		ItemStack bound = ItemStack.EMPTY;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(ENDER_LINKER.get()) && stack.get(ModCoreDataComponents.ENDER_LINKER_TARGET) != null
					&& groupId.equals(stack.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId())) {
				bound = stack;
				break;
			}
		}
		return !bound.isEmpty() && useLinker(player, bound, secondaryPos).consumesAction();
	}
	private static net.minecraft.world.InteractionResult useLinker(ServerPlayer player, ItemStack linker, BlockPos pos) {
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		return player.gameMode.useItemOn(player, player.level(), linker, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}
	private static int count(InventoryHandler handler, Item item) {
		int count = 0;
		for (int slot = 0; slot < handler.size(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (stack.is(item))
				count += stack.getCount();
		}
		return count;
	}
	private static List<ItemStack> fillInventoryWithTestContents(InventoryHandler handler) {
		List<ItemStack> contents = new ArrayList<>();
		for (int slot = 0; slot < handler.size(); slot++) {
			ItemStack stack = new ItemStack(Items.DIAMOND, slot + 1);
			handler.setStackInSlot(slot, stack);
			contents.add(stack.copy());
		}
		return contents;
	}
	private static boolean hasInventoryContents(InventoryHandler handler, List<ItemStack> contents) {
		if (handler.size() != contents.size())
			return false;
		for (int slot = 0; slot < handler.size(); slot++) {
			if (!ItemStack.matches(handler.getStackInSlot(slot), contents.get(slot)))
				return false;
		}
		return true;
	}
	private static void waitForClientMemorizedStack(BlockPos pos, Item item) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu
							&& menu.getBlockPosition().filter(pos::equals).isPresent()
							&& menu.getMemorizedStackInSlot(0).filter(stack -> stack.is(item)).isPresent())) {
				return;
			}
			sleep(50);
		}
		String clientState = AutomationRuntime.runOnClient(() -> {
			if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu)) {
				return "menu=closed";
			}
			return "menuPos=" + menu.getBlockPosition() + ", memorized=" + menu.getMemorizedStackInSlot(0) + ", endpoint="
					+ menu.getStorageBlockEntity().getLinkedStorageEndpointData();
		});
		throw new IllegalStateException("Linked limited-barrel menu did not receive its memorized " + item + ": " + clientState);
	}
	private static void waitForClientPlayerNear(BlockPos pos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.distanceToSqr(Vec3.atCenterOf(pos)) < 4)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client player did not move near linked primary chest expansion fixture");
	}
	private static boolean isDoubleChest(ServerLevel level, BlockPos left, BlockPos main) {
		return level.getBlockState(left).is(ModBlocks.CHEST.get()) && level.getBlockState(main).is(ModBlocks.CHEST.get())
				&& level.getBlockState(left).getValue(ChestBlock.TYPE) == ChestType.LEFT
				&& level.getBlockState(main).getValue(ChestBlock.TYPE) == ChestType.RIGHT
				&& level.getBlockEntity(left, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(chest -> chest.getMainPos().equals(main)).orElse(false)
				&& level.getBlockEntity(main, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(chest -> chest.getMainPos().equals(main)).orElse(false);
	}
	private static ItemStack findAndRemoveDroppedChest(ServerLevel level, BlockPos pos) {
		AABB area = new AABB(pos).inflate(1);
		List<ItemEntity> chestDrops = level.getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(ModBlocks.CHEST_ITEM.get()));
		require(chestDrops.size() == 1, "Chest destruction produced " + chestDrops.size() + " chest drops instead of exactly one");
		ItemStack stack = chestDrops.getFirst().getItem().copy();
		chestDrops.forEach(ItemEntity::discard);
		return stack;
	}
	private static void clearDroppedItems(ServerLevel level, BlockPos pos) {
		level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).forEach(ItemEntity::discard);
	}
	private static boolean hasDroppedItem(ServerLevel level, BlockPos pos, Item item) {
		return !level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), entity -> entity.getItem().is(item)).isEmpty();
	}
	private static void clearArea(ServerLevel level, BlockPos center) {
		discardItemEntities(level, center);
		for (int x = -2; x <= 8; x++)
			for (int y = -1; y <= 2; y++)
				for (int z = -2; z <= 10; z++) {
					BlockPos pos = center.offset(x, y, z);
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage)
						storage.clearContent();
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
		discardItemEntities(level, center);
		createFixturePlatform(level, center);
	}

	private static void createFixturePlatform(ServerLevel level, BlockPos center) {
		for (int x = -FIXTURE_PLATFORM_RADIUS; x <= FIXTURE_PLATFORM_RADIUS; x++)
			for (int z = -FIXTURE_PLATFORM_RADIUS; z <= FIXTURE_PLATFORM_RADIUS; z++) {
				level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
			}
	}
	private record LinkedStorageMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, LinkedStorageSnapshotProfile profile,
			int expectedDiamonds) {
	}

	private record LinkedStorageTierUpgradeFixture(BlockPos primaryPos, BlockPos secondaryPos, LinkedStorageEndpointData endpoint,
			LinkedStorageSnapshotProfile profile, GameType originalMode) {
	}

	private record LinkedControllerOutlineFixture(BlockPos controllerPos, BlockPos primaryPos, BlockPos secondaryPos) {
	}
	private record LinkedUpgradeRefreshFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, LinkedStorageSnapshotProfile profile,
			int playerUpgradeSlot, ItemStack originalUpgradeStack, ItemStack originalMainHand) {
	}
	private record ExpansionFixture(BlockPos primaryPos, BlockPos addedPos, LinkedStorageEndpointData endpoint, LinkedStorageSnapshotProfile profile,
			Vec3 originalPosition, float originalYRot, float originalXRot) {
	}
	private record DroppedPrimaryRenameFixture(BlockPos primaryPos, BlockPos secondaryPos, LinkedStorageEndpointData endpoint, String originalGroupName,
			String groupName, ItemStack renamedPrimary, ItemStack renamedSecondary, ItemStack originalHand, GameType originalMode,
			long secondaryVerificationGameTime) {
	}
	private record MemoryFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, LinkedStorageSnapshotProfile profile, ItemStack originalHand,
			Vec3 originalPosition, float originalYRot, float originalXRot) {
		private LinkedStorageMenuFixture menuFixture() {
			return new LinkedStorageMenuFixture(primaryPos, secondaryPos, groupId, profile, 0);
		}
	}
	private record TooltipFixture(BlockPos pos, LinkedStorageEndpointData endpoint, int inventorySlot, ItemStack originalStack) {
	}
	private record PickupFixture(BlockPos primaryPos, BlockPos secondaryPos, BlockPos ordinaryPos, List<UUID> itemIds, UUID ordinaryItemId, long verifyTime) {
	}
	private static void discardItemEntities(ServerLevel level, BlockPos center) {
		for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(10))) {
			itemEntity.discard();
		}
	}
	private static void require(boolean condition, String message) {
		if (!condition)
			throw new IllegalStateException(message);
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
}
