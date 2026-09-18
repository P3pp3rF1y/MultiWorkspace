package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerTargetData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointProvider;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSnapshotProfile;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.LinkedStorageTooltip;
import net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageItemClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

public final class StorageLinkedStorageRegression {
	private static final int LINKED_LIMITED_RELOAD_ITEM_COUNT = 23;

	private StorageLinkedStorageRegression() {
	}

	public static String run() {
		boolean ordinaryBarrels = false;
		boolean canonicalStorageTypeControlsInsertionRules = false;
		boolean linkedStorageMenuSnapshot = false;
		boolean ordinaryLinkedMenuRoute = false;
		boolean controllerCanonicalDeduplication = false;
		boolean controllerClientOutlineSynchronization = false;
		boolean ordinaryControllerOutlineRoute = false;
		boolean controllerRoutingLockFanoutAndRestoration = false;
		boolean doubleChestCanonicalIndex = false;
		boolean expansionMemoryTooltipPickup = false;
		boolean doubleChestLifecycle = false;
		boolean controllerTierCandidateAndPaint = false;
		boolean ordinaryDroppedPickupRoute = false;
		boolean ordinaryDroppedPrimaryRenameRoute = false;
		boolean ordinaryTierUpgradeRoute = false;
		String error = null;
		ensurePlayerAlive();
		boolean originalNoGravity = AutomationRuntime.runOnServer(player -> {
			boolean noGravity = player.isNoGravity();
			stabilizePlayer(player);
			player.setNoGravity(true);
			return noGravity;
		});
		try {
			AutomationRuntime.runOnServer(player -> {
				runRealPlayerOrdinaryLinkedBarrelRegression(player);
				return true;
			});
			ordinaryBarrels = true;
			AutomationRuntime.runOnServer(player -> {
				runCanonicalStorageTypeInsertionRules(player);
				return true;
			});
			canonicalStorageTypeControlsInsertionRules = true;
			runLinkedStorageMenuSnapshotRegression();
			linkedStorageMenuSnapshot = true;
			ordinaryLinkedMenuRoute = true;
			runLinkedPrimaryChestExpansionRegression();
			runSecondaryMemorySyncRegression();
			runLinkedStorageStackTooltipRegression();
			runDroppedItemPickupRegression();
			expansionMemoryTooltipPickup = true;
			ordinaryDroppedPickupRoute = true;
			AutomationRuntime.runOnServer(player -> {
				runControllerCanonicalDeduplicationRegression(player);
				return true;
			});
			controllerCanonicalDeduplication = true;
			runControllerClientOutlineSynchronizationRegression();
			controllerClientOutlineSynchronization = true;
			ordinaryControllerOutlineRoute = true;
			AutomationRuntime.runOnServer(player -> {
				runControllerRoutingLockFanoutAndRestorationRegression(player);
				return true;
			});
			controllerRoutingLockFanoutAndRestoration = true;
			AutomationRuntime.runOnServer(player -> {
				runLinkedDoubleChestCanonicalIndexRegression(player);
				return true;
			});
			doubleChestCanonicalIndex = true;
			AutomationRuntime.runOnServer(player -> {
				runLinkedDoubleChestLifecycle(player);
				return true;
			});
			doubleChestLifecycle = true;
			AutomationRuntime.runOnServer(player -> {
				runLinkedControllerTierCandidateAndPaintRegression(player);
				return true;
			});
			controllerTierCandidateAndPaint = true;
			AutomationRuntime.runOnServer(player -> {
				runDroppedPrimaryRenameRegression(player);
				return true;
			});
			ordinaryDroppedPrimaryRenameRoute = true;
			AutomationRuntime.runOnServer(player -> {
				runLinkedStorageTierUpgradeRegression(player);
				return true;
			});
			ordinaryTierUpgradeRoute = true;
		} catch (RuntimeException e) {
			error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		} finally {
			try {
				AutomationRuntime.runOnServer(player -> {
					player.setNoGravity(originalNoGravity);
					return true;
				});
			} catch (RuntimeException e) {
				if (error == null) {
					error = "Failed to restore player gravity: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
				}
			}
		}
		boolean ok = ordinaryBarrels && canonicalStorageTypeControlsInsertionRules && linkedStorageMenuSnapshot && controllerCanonicalDeduplication
				&& controllerClientOutlineSynchronization && controllerRoutingLockFanoutAndRestoration && doubleChestCanonicalIndex
				&& expansionMemoryTooltipPickup && doubleChestLifecycle && controllerTierCandidateAndPaint && ordinaryLinkedMenuRoute
				&& ordinaryControllerOutlineRoute && ordinaryDroppedPickupRoute && ordinaryDroppedPrimaryRenameRoute && ordinaryTierUpgradeRoute;
		return "{\"ok\":" + ok + ",\"ordinaryBarrels\":" + ordinaryBarrels + ",\"canonicalStorageTypeControlsInsertionRules\":"
				+ canonicalStorageTypeControlsInsertionRules + ",\"linkedStorageMenuSnapshot\":" + linkedStorageMenuSnapshot + ",\"ordinaryLinkedMenuRoute\":"
				+ ordinaryLinkedMenuRoute + ",\"controllerCanonicalDeduplication\":" + controllerCanonicalDeduplication
				+ ",\"controllerRoutingLockFanoutAndRestoration\":" + controllerRoutingLockFanoutAndRestoration + ",\"controllerClientOutlineSynchronization\":"
				+ controllerClientOutlineSynchronization + ",\"ordinaryControllerOutlineRoute\":" + ordinaryControllerOutlineRoute
				+ ",\"doubleChestCanonicalIndex\":" + doubleChestCanonicalIndex + ",\"expansionMemoryTooltipPickup\":" + expansionMemoryTooltipPickup
				+ ",\"doubleChestLifecycle\":" + doubleChestLifecycle + ",\"controllerTierCandidateAndPaint\":" + controllerTierCandidateAndPaint
				+ ",\"ordinaryDroppedPickupRoute\":" + ordinaryDroppedPickupRoute + ",\"ordinaryDroppedPrimaryRenameRoute\":"
				+ ordinaryDroppedPrimaryRenameRoute + ",\"ordinaryTierUpgradeRoute\":" + ordinaryTierUpgradeRoute + ",\"error\":"
				+ (error == null ? "null" : "\"" + escapeJson(error) + "\"") + "}";
	}

	public static String runLinkedDoubleChestLifecycleRegression() {
		ensurePlayerAlive();
		boolean originalNoGravity = AutomationRuntime.runOnServer(player -> {
			boolean noGravity = player.isNoGravity();
			stabilizePlayer(player);
			player.setNoGravity(true);
			return noGravity;
		});
		String error = null;
		try {
			AutomationRuntime.runOnServer(player -> {
				runLinkedDoubleChestLifecycle(player);
				return true;
			});
		} catch (RuntimeException e) {
			error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		} finally {
			try {
				AutomationRuntime.runOnServer(player -> {
					player.setNoGravity(originalNoGravity);
					return true;
				});
			} catch (RuntimeException e) {
				if (error == null) {
					error = "Failed to restore player gravity: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
				}
			}
		}
		return error == null
				? "{\"ok\":true,\"doubleChestLifecycle\":true}"
				: "{\"ok\":false,\"doubleChestLifecycle\":false,\"error\":\"" + escapeJson(error) + "\"}";
	}

	public static String setupLinkedLimitedBarrelReloadProjection() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = limitedPrimaryPos(player);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos, 1);
			clearArea(level, secondaryPos, 1);
			placeBlockWithItem(level, player, primaryPos, ModBlocks.LIMITED_BARREL_1_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get());
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not link the primary limited barrel");
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not link the secondary limited barrel");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, LINKED_LIMITED_RELOAD_ITEM_COUNT));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			WorldHelper.notifyBlockUpdate(primary);
			WorldHelper.notifyBlockUpdate(secondary);
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "primary limited barrel");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "secondary limited barrel");
			require(primaryEndpoint.groupId().equals(secondaryEndpoint.groupId()) && hasLimitedRenderProjection(primary)
					&& hasLimitedRenderProjection(secondary), "Linked limited barrels did not project canonical render data before reload");
			return "{\"ok\":true,\"groupId\":\"" + primaryEndpoint.groupId() + "\",\"primaryEndpointId\":\"" + primaryEndpoint.endpointId()
					+ "\",\"secondaryEndpointId\":\"" + secondaryEndpoint.endpointId() + "\"}";
		});
	}

	public static String linkedLimitedBarrelReloadProjectionStatus(UUID groupId) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = limitedPrimaryPos(player);
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, primaryPos.east(3));
			require(groupId.equals(requireEndpoint(primary, "reloaded primary limited barrel").groupId())
					&& groupId.equals(requireEndpoint(secondary, "reloaded secondary limited barrel").groupId()) && hasLimitedRenderProjection(primary)
					&& hasLimitedRenderProjection(secondary), "Linked limited barrel server projection was not restored");
			WorldHelper.notifyBlockUpdate(primary);
			WorldHelper.notifyBlockUpdate(secondary);
			return true;
		});
		waitForClientLimitedRenderProjection(groupId);
		return "{\"ok\":true,\"clientDisplayItems\":true,\"clientCounts\":true,\"clientFillLevels\":true}";
	}

	private static void runRealPlayerOrdinaryLinkedBarrelRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 24);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos, 1);
		clearArea(level, secondaryPos, 1);
		try {
			placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.BARREL_ITEM.get());
			ItemStack blankLinker = new ItemStack(ENDER_LINKER.get(), 2);
			useLinkerAsPlayer(level, player, primaryPos, blankLinker);
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "real-player primary barrel");
			ItemStack boundLinker = findBoundLinker(player, primaryEndpoint.groupId());
			useLinkerAsPlayer(level, player, secondaryPos, boundLinker);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "real-player secondary barrel");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			require(primaryEndpoint.groupId().equals(secondaryEndpoint.groupId()) && !primaryEndpoint.endpointId().equals(secondaryEndpoint.endpointId())
					&& countItem(secondary, Items.DIAMOND) == 7, "Real-player linker interactions did not produce ordinary linked barrel contents");
		} finally {
			clearArea(level, primaryPos, 1);
			clearArea(level, secondaryPos, 1);
		}
	}

	private static void runCanonicalStorageTypeInsertionRules(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 24);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos, 1);
		clearArea(level, secondaryPos, 1);
		try {
			placeBlockWithItem(level, player, primaryPos, ModBlocks.SHULKER_BOX_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.BARREL_ITEM.get());
			StorageBlockEntity primary = WorldHelper.getBlockEntity(level, primaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing canonical shulker endpoint"));
			StorageBlockEntity secondary = WorldHelper.getBlockEntity(level, secondaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing physical barrel endpoint"));
			linkSameGroup(level, player, primary, secondary);
			require(insertThroughCapability(level, secondaryPos, new ItemStack(Items.SHULKER_BOX)) == 0,
					"Physical barrel endpoint bypassed the canonical shulker insertion restriction");

			clearArea(level, primaryPos, 1);
			clearArea(level, secondaryPos, 1);
			placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.SHULKER_BOX_ITEM.get());
			primary = WorldHelper.getBlockEntity(level, primaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing canonical barrel endpoint"));
			secondary = WorldHelper.getBlockEntity(level, secondaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing physical shulker endpoint"));
			linkSameGroup(level, player, primary, secondary);
			require(insertThroughCapability(level, secondaryPos, new ItemStack(Items.SHULKER_BOX)) == 1,
					"Physical shulker endpoint overrode the canonical barrel insertion rule");
		} finally {
			clearArea(level, primaryPos, 1);
			clearArea(level, secondaryPos, 1);
		}
	}

	private static int insertThroughCapability(ServerLevel level, BlockPos pos, ItemStack stack) {
		ResourceHandler<ItemResource> capability = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.UP);
		if (capability == null) {
			return 0;
		}
		try (Transaction transaction = Transaction.openRoot()) {
			int inserted = capability.insert(ItemResource.of(stack), stack.getCount(), transaction);
			transaction.commit();
			return inserted;
		}
	}

	private static void runLinkedStorageMenuSnapshotRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 3);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos, 1);
			clearArea(level, secondaryPos, 1);
			try {
				placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
				placeBlockWithItem(level, player, secondaryPos, ModBlocks.BARREL_ITEM.get());
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				linkSameGroup(level, player, primary, secondary);
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				primary.setCustomName(Component.literal("Linked storage snapshot profile"));
				LinkedStorageSnapshotProfile profile = new LinkedStorageSnapshotProfile(primary.getDisplayName(),
						primary.getStorageWrapper().getInventoryHandler().size(), primary.getStorageWrapper().getUpgradeHandler().size(),
						primary.getStorageWrapper().getColumnsTaken());
				return new LinkedStorageMenuFixture(primaryPos, secondaryPos, requireEndpoint(primary, "linked storage menu primary").groupId(), profile);
			} catch (RuntimeException e) {
				clearArea(level, primaryPos, 1);
				clearArea(level, secondaryPos, 1);
				throw e;
			}
		});
		try {
			movePlayerNear(fixture.secondaryPos().south());
			waitForClientLinkedStorageEndpoint(fixture, fixture.secondaryPos());
			openLinkedStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			movePlayerNear(fixture.primaryPos().south());
			waitForClientLinkedStorageEndpoint(fixture, fixture.primaryPos());
			openLinkedStorageMenu(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			waitForClientLinkedStorageMenu(fixture, fixture.primaryPos());
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), fixture.primaryPos(), 1);
				clearArea(player.level(), fixture.secondaryPos(), 1);
				return true;
			});
		}
	}

	private static void openLinkedStorageMenu(LinkedStorageMenuFixture fixture, BlockPos pos, LinkedStorageEndpointRole expectedRole) {
		AutomationRuntime.runOnServer(player -> {
			StorageBlockEntity storage = player.level().getBlockEntity(pos) instanceof StorageBlockEntity storageBlockEntity ? storageBlockEntity : null;
			require(storage != null, "Missing storage block entity for linked storage menu at " + pos);
			LinkedStorageEndpointData endpoint = requireEndpoint(storage, "linked storage menu endpoint");
			boolean primary = LinkedStorageGroupsSavedData.get(player.level()).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId());
			require(primary == (expectedRole == LinkedStorageEndpointRole.PRIMARY), "Linked storage menu endpoint did not have expected authoritative "
					+ expectedRole + " role: " + describeServerLinkedStorageMenu(player, fixture, pos));
			require(player.openMenu(new SophisticatedMenuProvider((windowId, inventory, menuPlayer) -> new StorageContainerMenu(windowId, menuPlayer, pos),
					storage.getMenuDisplayName(), false), buffer -> StorageContainerMenu.writeMenuData(buffer, player, pos)).isPresent(),
					"Could not open linked storage menu");
			require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(pos))
					&& menu.getNumberOfStorageInventorySlots() == fixture.profile().inventorySlots()
					&& menu.getNumberOfUpgradeSlots() == fixture.profile().upgradeSlots() && menu.getColumnsTaken() == fixture.profile().columnsTaken()
					&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
					"Linked storage server menu did not expose the canonical snapshot: " + describeServerLinkedStorageMenu(player, fixture, pos));
			return true;
		});
	}

	private static void waitForClientLinkedStorageMenu(LinkedStorageMenuFixture fixture, BlockPos pos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		do {
			if (AutomationRuntime.runOnClient(() -> hasClientLinkedStorageMenuSnapshot(fixture, pos))) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted waiting for linked storage menu snapshot", e);
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Linked storage menu did not receive the canonical snapshot: " + describeClientLinkedStorageMenu(fixture, pos));
	}

	private static boolean hasClientLinkedStorageMenuSnapshot(LinkedStorageMenuFixture fixture, BlockPos pos) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen) || minecraft.player == null
				|| !(minecraft.player.containerMenu instanceof StorageContainerMenu menu) || screen.getMenu() != menu) {
			return false;
		}
		LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
		ItemStack firstStack = menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0);
		return menu.getBlockPosition().filter(pos::equals).isPresent() && endpoint != null && fixture.groupId().equals(endpoint.groupId())
				&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
				&& provider.getLinkedStorageEndpoint().filter(endpoint::equals).isPresent()
				&& provider.getLinkedStorageEndpointRole().filter(fixtureRole(fixture, pos)::equals).isPresent()
				&& screen.getTitle().getString().equals(fixture.profile().groupName().getString())
				&& menu.getStorageWrapper().getInventoryHandler().size() == fixture.profile().inventorySlots()
				&& menu.getNumberOfStorageInventorySlots() == fixture.profile().inventorySlots()
				&& menu.getNumberOfUpgradeSlots() == fixture.profile().upgradeSlots() && menu.getColumnsTaken() == fixture.profile().columnsTaken()
				&& ClientLinkedStorageContents.getGroupName(fixture.groupId()).filter(fixture.profile().groupName()::equals).isPresent()
				&& ClientLinkedStorageContents.getInventorySlots(fixture.groupId()).orElse(-1) == fixture.profile().inventorySlots()
				&& ClientLinkedStorageContents.getUpgradeSlots(fixture.groupId()).orElse(-1) == fixture.profile().upgradeSlots()
				&& ClientLinkedStorageContents.getColumnsTaken(fixture.groupId()).orElse(-1) == fixture.profile().columnsTaken() && firstStack.is(Items.DIAMOND)
				&& firstStack.getCount() == 7;
	}

	private static String describeServerLinkedStorageMenu(ServerPlayer player, LinkedStorageMenuFixture fixture, BlockPos pos) {
		StorageBlockEntity storage = player.level().getBlockEntity(pos) instanceof StorageBlockEntity storageBlockEntity ? storageBlockEntity : null;
		LinkedStorageEndpointData endpoint = storage == null ? null : storage.getLinkedStorageEndpointData();
		String role = endpoint == null
				? "missing"
				: LinkedStorageGroupsSavedData.get(player.level()).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())
						? "PRIMARY"
						: "SECONDARY";
		if (!(player.containerMenu instanceof StorageContainerMenu menu)) {
			return "menu=" + player.containerMenu.getClass().getSimpleName() + ", player=" + player.position() + ", position=" + pos + ", endpoint=" + endpoint
					+ ", role=" + role;
		}
		return "menu=" + menu.getClass().getSimpleName() + ", player=" + player.position() + ", menuPos=" + menu.getBlockPosition() + ", title="
				+ storage.getMenuDisplayName() + ", endpoint=" + endpoint + ", role=" + role + ", wrapper={inventorySlots="
				+ menu.getStorageWrapper().getInventoryHandler().size() + ", upgradeSlots=" + menu.getStorageWrapper().getUpgradeHandler().size() + ", columns="
				+ menu.getStorageWrapper().getColumnsTaken() + ", diamonds=" + count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND)
				+ "}, menu={inventorySlots=" + menu.getNumberOfStorageInventorySlots() + ", upgradeSlots=" + menu.getNumberOfUpgradeSlots() + ", columns="
				+ menu.getColumnsTaken() + "}, expectedProfile=" + fixture.profile();
	}

	private static String describeClientLinkedStorageMenu(LinkedStorageMenuFixture fixture, BlockPos pos) {
		try {
			return AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				String screenName = minecraft.gui.screen() == null ? "null" : minecraft.gui.screen().getClass().getSimpleName();
				String title = minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen ? screen.getTitle().getString() : "n/a";
				String clientBlockEntity = "level=null";
				if (minecraft.level != null) {
					clientBlockEntity = minecraft.level.getBlockEntity(pos) instanceof StorageBlockEntity storage
							? storage.getClass().getSimpleName() + " endpoint=" + storage.getLinkedStorageEndpointData()
							: String.valueOf(minecraft.level.getBlockEntity(pos));
				}
				String cache = "name=" + ClientLinkedStorageContents.getGroupName(fixture.groupId()).map(Component::getString).orElse("missing")
						+ ", component=" + ClientLinkedStorageContents.getGroupName(fixture.groupId()).map(Object::toString).orElse("missing") + ", revision="
						+ ClientLinkedStorageContents.getRevision(fixture.groupId()).map(String::valueOf).orElse("missing") + ", inventorySlots="
						+ ClientLinkedStorageContents.getInventorySlots(fixture.groupId()).map(String::valueOf).orElse("missing") + ", upgradeSlots="
						+ ClientLinkedStorageContents.getUpgradeSlots(fixture.groupId()).map(String::valueOf).orElse("missing") + ", columns="
						+ ClientLinkedStorageContents.getColumnsTaken(fixture.groupId()).map(String::valueOf).orElse("missing") + ", diamonds="
						+ ClientLinkedStorageContents.getContents(fixture.groupId()).map(contents -> contents.contents().inventory().stacks().stream()
								.filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()).map(String::valueOf).orElse("missing");
				if (!(minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof StorageContainerMenu menu)) {
					return "screen=" + screenName + ", title=" + title + ", player=" + (minecraft.player == null ? "null" : minecraft.player.position())
							+ ", expectedPosition=" + pos + ", expectedRole=" + fixtureRole(fixture, pos) + ", clientBlockEntity=" + clientBlockEntity
							+ ", cache={" + cache + "}";
				}
				LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
				String role = menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
						? provider.getLinkedStorageEndpointRole().map(Enum::name).orElse("missing")
						: "no-provider";
				return "screen=" + screenName + ", title=" + title + ", titleComponent=" + screen.getTitle() + ", expectedComponent="
						+ fixture.profile().groupName() + ", player=" + minecraft.player.position() + ", menuPos=" + menu.getBlockPosition() + ", endpoint="
						+ endpoint + ", role=" + role + ", expectedRole=" + fixtureRole(fixture, pos) + ", wrapper={inventorySlots="
						+ menu.getStorageWrapper().getInventoryHandler().size() + ", upgradeSlots=" + menu.getStorageWrapper().getUpgradeHandler().size()
						+ ", columns=" + menu.getStorageWrapper().getColumnsTaken() + ", diamonds="
						+ count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) + "}, menu={inventorySlots="
						+ menu.getNumberOfStorageInventorySlots() + ", upgradeSlots=" + menu.getNumberOfUpgradeSlots() + ", columns=" + menu.getColumnsTaken()
						+ "}, clientBlockEntity=" + clientBlockEntity + ", cache={" + cache + "}";
			});
		} catch (RuntimeException e) {
			return "client diagnostics unavailable: " + e;
		}
	}

	private static LinkedStorageEndpointRole fixtureRole(LinkedStorageMenuFixture fixture, BlockPos pos) {
		return fixture.primaryPos().equals(pos) ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY;
	}

	private static void waitForClientLinkedStorageEndpoint(LinkedStorageMenuFixture fixture, BlockPos pos) {
		waitForClientLinkedStorageEndpoint(fixture.groupId(), pos, () -> describeClientLinkedStorageMenu(fixture, pos));
	}

	private static void waitForClientLinkedStorageEndpoint(UUID groupId, BlockPos pos, java.util.function.Supplier<String> diagnostic) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		do {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof StorageBlockEntity storage
						&& storage.getLinkedStorageEndpointData() != null && groupId.equals(storage.getLinkedStorageEndpointData().groupId());
			})) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Client linked endpoint did not synchronize before menu open: " + diagnostic.get());
	}

	private static void runLinkedPrimaryChestExpansionRegression() {
		ExpansionFixture initialFixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos pos = player.blockPosition().offset(0, 0, 3);
			clearArea(level, pos, 3);
			placeBlockWithItem(level, player, pos, ModBlocks.CHEST_ITEM.get());
			ChestBlockEntity primary = getChest(level, pos);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
					primary) == LinkedStorageService.LinkResult.SUCCESS, "Could not link primary chest for expansion");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			LinkedStorageEndpointData endpoint = requireEndpoint(primary, "expansion primary");
			require(LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()),
					"Expansion fixture endpoint was not the authoritative linked primary");
			return new ExpansionFixture(pos, endpoint.groupId(), profile(primary));
		});
		try {
			LinkedStorageMenuFixture initialMenuFixture = new LinkedStorageMenuFixture(initialFixture.primaryPos(), initialFixture.primaryPos(),
					initialFixture.groupId(), initialFixture.profile());
			waitForClientLinkedStorageEndpoint(initialMenuFixture, initialFixture.primaryPos());
			openLinkedStorageMenu(initialMenuFixture, initialFixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			waitForClientLinkedStorageMenu(initialMenuFixture, initialFixture.primaryPos());
			ExpansionFixture fixture = AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				require(player.containerMenu instanceof StorageContainerMenu, "Linked primary menu closed before chest expansion");
				StorageContainerMenu staleMenu = (StorageContainerMenu) player.containerMenu;
				ChestBlockEntity initialPrimary = getChest(level, initialFixture.primaryPos());
				BlockPos addedPos = initialFixture.primaryPos().relative(initialPrimary.getBlockState().getValue(ChestBlock.FACING).getClockWise());
				placeBlockWithItem(level, player, addedPos, ModBlocks.CHEST_ITEM.get(), false);
				ChestBlockEntity originalPart = getChest(level, initialFixture.primaryPos());
				ChestBlockEntity addedPart = getChest(level, addedPos);
				require(player.containerMenu == player.inventoryMenu && player.containerMenu != staleMenu,
						"Expanding linked primary did not close stale menu: active=" + player.containerMenu.getClass().getSimpleName() + ", stalePos="
								+ staleMenu.getStorageBlockEntity().getBlockPos() + ", endpoint="
								+ staleMenu.getStorageBlockEntity().getLinkedStorageEndpointData());
				List<ChestBlockEntity> endpointHolders = List.of(originalPart, addedPart).stream().filter(
						part -> part.getLinkedStorageEndpointData() != null && part.getLinkedStorageEndpointData().groupId().equals(initialFixture.groupId()))
						.toList();
				require(endpointHolders.size() == 1 && endpointHolders.getFirst().isMainChest(),
						"Expanded linked chest did not preserve exactly one endpoint on its main part");
				ChestBlockEntity expanded = endpointHolders.getFirst();
				LinkedStorageEndpointData endpoint = requireEndpoint(expanded, "expanded primary");
				require(LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())
						&& expanded.getStorageWrapper().getInventoryHandler().size() == 54 && countItem(expanded, Items.DIAMOND) == 7,
						"Expanded linked primary did not preserve role, capacity, and contents: primary="
								+ LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()) + ", slots="
								+ expanded.getStorageWrapper().getInventoryHandler().size() + ", diamonds=" + countItem(expanded, Items.DIAMOND));
				return new ExpansionFixture(expanded.getBlockPos(), endpoint.groupId(), profile(expanded));
			});
			waitForClosedStorageMenu();
			waitForClientExpandedLinkedStorageEndpoint(fixture);
			AutomationRuntime.runOnServer(player -> {
				ChestBlockEntity expanded = getChest(player.level(), fixture.primaryPos());
				require(player
						.openMenu(
								new SophisticatedMenuProvider((id, inventory, menuPlayer) -> new StorageContainerMenu(id, menuPlayer, fixture.primaryPos()),
										expanded.getMenuDisplayName(), false),
								buffer -> StorageContainerMenu.writeMenuData(buffer, player, fixture.primaryPos()))
						.isPresent(), "Could not reopen expanded linked primary");
				return true;
			});
			waitForExpandedClientMenu(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), initialFixture.primaryPos(), 3);
				return true;
			});
		}
	}

	private static void waitForClientExpandedLinkedStorageEndpoint(ExpansionFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		int consecutiveMatches = 0;
		do {
			boolean matches = AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.level != null && minecraft.level.getBlockEntity(fixture.primaryPos()) instanceof ChestBlockEntity chest && chest.isMainChest()
						&& chest.getLinkedStorageEndpointData() != null && fixture.groupId().equals(chest.getLinkedStorageEndpointData().groupId());
			});
			if (matches && ++consecutiveMatches >= 3) {
				return;
			}
			if (!matches) {
				consecutiveMatches = 0;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Expanded linked primary client endpoint did not stabilize at " + fixture.primaryPos() + " for group "
				+ fixture.groupId() + ": " + describeClientChest(fixture.primaryPos()));
	}

	private static String describeClientChest(BlockPos pos) {
		return AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.level == null) {
				return "level=null";
			}
			if (!(minecraft.level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
				return "pos=" + pos + ", state=" + minecraft.level.getBlockState(pos) + ", blockEntity=" + minecraft.level.getBlockEntity(pos);
			}
			return "pos=" + pos + ", state=" + chest.getBlockState() + ", main=" + chest.getMainPos() + ", endpoint=" + chest.getLinkedStorageEndpointData()
					+ ", slots=" + chest.getStorageWrapper().getInventoryHandler().size();
		});
	}

	private static void runSecondaryMemorySyncRegression() {
		MemoryFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 3);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos, 4);
			placeBlockWithItem(level, player, primaryPos, ModBlocks.LIMITED_BARREL_1_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get());
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
			linkSameGroup(level, player, primary, secondary);
			ItemStack originalHand = player.getMainHandItem().copy();
			LinkedStorageEndpointData endpoint = requireEndpoint(secondary, "linked limited secondary before memory deposit");
			return new MemoryFixture(primaryPos, secondaryPos, endpoint.groupId(), originalHand);
		});
		try {
			waitForClientLinkedStorageEndpoint(fixture.groupId(), fixture.secondaryPos(),
					() -> "position=" + fixture.secondaryPos() + ", group=" + fixture.groupId());
			AutomationRuntime.runOnServer(player -> {
				LimitedBarrelBlockEntity secondary = getLimitedBarrel(player.level(), fixture.secondaryPos());
				require(player
						.openMenu(
								new SophisticatedMenuProvider((id, inventory, menuPlayer) -> new StorageContainerMenu(id, menuPlayer, fixture.secondaryPos()),
										secondary.getMenuDisplayName(), false),
								buffer -> StorageContainerMenu.writeMenuData(buffer, player, fixture.secondaryPos()))
						.isPresent(), "Could not open secondary memory menu");
				return true;
			});
			waitForClientMemoryMenu(fixture);
			AutomationRuntime.runOnServer(player -> {
				LimitedBarrelBlockEntity primary = getLimitedBarrel(player.level(), fixture.primaryPos());
				LimitedBarrelBlockEntity secondary = getLimitedBarrel(player.level(), fixture.secondaryPos());
				secondary.toggleLock();
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 3));
				require(secondary.depositItem(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), 0),
						"Real deposit through linked limited secondary failed");
				require(hasMemory(primary, Items.EMERALD) && hasMemory(secondary, Items.EMERALD),
						"Real limited-barrel deposit did not update memory through both linked wrappers");
				return true;
			});
			waitForClientCondition(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu
							&& menu.getMemorizedStackInSlot(0).filter(stack -> stack.is(Items.EMERALD)).isPresent(),
					"Secondary menu did not receive canonical memory update");
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalHand());
				clearArea(player.level(), fixture.primaryPos(), 4);
				return true;
			});
		}
	}

	private static void waitForClientMemoryMenu(MemoryFixture fixture) {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof StorageContainerMenu menu)) {
				return false;
			}
			LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
			return menu.getBlockPosition().equals(Optional.of(fixture.secondaryPos())) && endpoint != null && fixture.groupId().equals(endpoint.groupId());
		}, "Secondary linked limited-barrel menu did not open on the synchronized endpoint");
	}

	private static void runLinkedStorageStackTooltipRegression() {
		TooltipFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos pos = player.blockPosition().offset(0, 0, 60);
			clearArea(level, pos, 2);
			placeBlockWithItem(level, player, pos, ModBlocks.BARREL_ITEM.get());
			BarrelBlockEntity primary = getBarrel(level, pos);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
					primary) == LinkedStorageService.LinkResult.SUCCESS, "Could not create tooltip fixture");
			LinkedStorageEndpointData endpoint = requireEndpoint(primary, "tooltip primary");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			require(countCanonicalItem(level, endpoint.groupId(), Items.DIAMOND) == 7,
					"Tooltip fixture did not persist seven diamonds to canonical contents before breaking");
			ItemStack original = player.getInventory().getItem(8).copy();
			level.destroyBlock(pos, true, player);
			require(countCanonicalItem(level, endpoint.groupId(), Items.DIAMOND) == 7, "Breaking the linked primary cleared canonical tooltip contents");
			List<ItemEntity> carriers = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
					entity -> endpoint.equals(entity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)));
			require(carriers.size() == 1, "Linked storage break produced " + carriers.size() + " primary carriers instead of exactly one");
			ItemStack carrier = carriers.getFirst().getItem().copy();
			require(StorageBlockEntity.getLinkedStorageEndpointRole(carrier).filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent(),
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
				require(StorageItemClient.getTooltipImage(carrier) instanceof LinkedStorageTooltip tooltip
						&& tooltip.role() == LinkedStorageEndpointRole.PRIMARY && tooltip.groupId().equals(fixture.endpoint().groupId()),
						"Production linked tooltip did not expose the PRIMARY role and canonical group");
				ItemStack originalCarried = minecraft.player.containerMenu.getCarried().copy();
				minecraft.player.containerMenu.setCarried(carrier.copy());
				try {
					require(StorageItemClient.getTooltipImage(carrier) instanceof StorageContentsTooltip contentsTooltip
							&& contentsTooltip.linkedStorageTooltip() instanceof LinkedStorageTooltip linkedStorageTooltip
							&& linkedStorageTooltip.role() == LinkedStorageEndpointRole.PRIMARY
							&& linkedStorageTooltip.groupId().equals(fixture.endpoint().groupId()),
							"Production linked contents tooltip did not retain the PRIMARY role and canonical group");
				} finally {
					minecraft.player.containerMenu.setCarried(originalCarried);
				}
				return true;
			});
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (System.nanoTime() < deadline) {
				if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().level != null
						&& ClientLinkedStorageContents.getRevision(fixture.endpoint().groupId()).isPresent()
						&& count(
								StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(),
										Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot())).getInventoryHandler(),
								Items.DIAMOND) == 7)) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Linked carrier tooltip cache did not resolve canonical contents: " + describeClientTooltipCache(fixture));
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().setItem(fixture.inventorySlot(), fixture.originalStack());
				player.inventoryMenu.broadcastChanges();
				clearArea(player.level(), fixture.pos(), 2);
				return true;
			});
		}
	}

	private static String describeClientTooltipCache(TooltipFixture fixture) {
		return AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.level == null) {
				return "player-or-level-null";
			}
			ItemStack carrier = minecraft.player.getInventory().getItem(fixture.inventorySlot());
			StackStorageWrapper wrapper = StackStorageWrapper.fromStack(minecraft.level.registryAccess(), carrier);
			int cachedDiamonds = ClientLinkedStorageContents.getContents(fixture.endpoint().groupId()).map(
					contents -> contents.contents().inventory().stacks().stream().filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum())
					.orElse(-1);
			return "carrierEndpoint=" + carrier.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) + ", role="
					+ StorageBlockEntity.getLinkedStorageEndpointRole(carrier) + ", revision="
					+ ClientLinkedStorageContents.getRevision(fixture.endpoint().groupId()) + ", inventorySlots="
					+ ClientLinkedStorageContents.getInventorySlots(fixture.endpoint().groupId()) + ", upgradeSlots="
					+ ClientLinkedStorageContents.getUpgradeSlots(fixture.endpoint().groupId()) + ", columns="
					+ ClientLinkedStorageContents.getColumnsTaken(fixture.endpoint().groupId()) + ", cachedDiamonds=" + cachedDiamonds + ", wrapperSlots="
					+ wrapper.getInventoryHandler().size() + ", wrapperDiamonds=" + count(wrapper.getInventoryHandler(), Items.DIAMOND);
		});
	}

	private static void runDroppedItemPickupRegression() {
		PickupFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 68);
			BlockPos secondaryPos = primaryPos.east(21);
			clearArea(level, primaryPos, 2);
			clearArea(level, secondaryPos, 2);
			clearPickupEntities(level, primaryPos, secondaryPos);
			placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.BARREL_ITEM.get());
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			linkSameGroup(level, player, primary, secondary);
			configureMagnet(primary);
			require(level.getBlockState(primaryPos).getValue(StorageBlockBase.TICKING) && !level.getBlockState(secondaryPos).getValue(StorageBlockBase.TICKING),
					"Secondary endpoint incorrectly enabled pickup ticking");
			ItemEntity first = spawnPickupItem(level, secondaryPos, -0.75);
			ItemEntity second = spawnPickupItem(level, secondaryPos, 0.75);
			return new PickupFixture(primaryPos, secondaryPos, List.of(first.getUUID(), second.getUUID()), level.getGameTime() + 20);
		});
		try {
			waitForServerCondition(player -> player.level().getGameTime() >= fixture.verifyTime());
			AutomationRuntime.runOnServer(player -> {
				List<ItemEntity> items = pickupItems(player.level(), fixture.secondaryPos());
				int canonicalPearls = countItem(getBarrel(player.level(), fixture.primaryPos()), Items.ENDER_PEARL);
				require(items.size() == 2 && canonicalPearls == 0,
						"Secondary pickup suppression did not retain exactly two entities and zero canonical items: entities="
								+ items.stream().map(item -> item.getUUID() + "@" + item.position() + "x" + item.getItem().getCount()).toList()
								+ ", expectedIds=" + fixture.itemIds() + ", canonical=" + canonicalPearls);
				items.forEach(item -> item.setPos(Vec3.atCenterOf(fixture.primaryPos())));
				return true;
			});
			waitForServerCondition(player -> fixture.itemIds().stream().allMatch(id -> player.level().getEntity(id) == null)
					&& countItem(getBarrel(player.level(), fixture.primaryPos()), Items.ENDER_PEARL) == 2);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				fixture.itemIds().forEach(id -> {
					if (player.level().getEntity(id) instanceof ItemEntity item) {
						item.discard();
					}
				});
				clearPickupEntities(player.level(), fixture.primaryPos(), fixture.secondaryPos());
				clearArea(player.level(), fixture.primaryPos(), 2);
				clearArea(player.level(), fixture.secondaryPos(), 2);
				return true;
			});
		}
	}

	private static void runControllerCanonicalDeduplicationRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos controllerPos = player.blockPosition().offset(12, 0, 24);
		BlockPos primaryPos = controllerPos.east();
		BlockPos secondaryPos = primaryPos.east();
		clearArea(level, controllerPos, 3);
		try {
			placeBlockWithItem(level, player, controllerPos, ModBlocks.CONTROLLER_ITEM.get());
			placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.BARREL_ITEM.get());
			ControllerBlockEntity controller = getController(level, controllerPos);
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			require(controller.getStoragePositions().size() == 2, "Controller did not register both ordinary barrels before linking");
			linkSameGroup(level, player, primary, secondary);
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamonds = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(controller.getStoragePositions().size() == 1 && controller.getSlots(0) == primary.getStorageWrapper().getInventoryHandler().size()
					&& countItem(secondary, Items.DIAMOND) == 7 && controller.getStackStorages(diamonds).size() == 1,
					"Controller duplicated a linked barrel canonical content index");
			secondary.reregisterWithController();
			require(controller.getStoragePositions().size() == 1 && controller.getStoragePositions().contains(secondaryPos)
					&& controller.getSlots(0) == secondary.getStorageWrapper().getInventoryHandler().size()
					&& controller.getStorageBlockPositions().containsAll(java.util.List.of(primaryPos, secondaryPos))
					&& controller.getStackStorages(diamonds).size() == 1,
					"Controller did not rebuild a linked group from its noncanonical endpoint: storagePositions=" + controller.getStoragePositions()
							+ ", storageBlockPositions=" + controller.getStorageBlockPositions() + ", slots=" + controller.getSlots(0) + ", expectedSlots="
							+ secondary.getStorageWrapper().getInventoryHandler().size() + ", stackStorages=" + controller.getStackStorages(diamonds).size());
		} finally {
			clearArea(level, controllerPos, 3);
		}
	}

	private static void runControllerClientOutlineSynchronizationRegression() {
		ClientOutlineFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos controllerPos = player.blockPosition().offset(6, 0, 6);
			BlockPos storagePos = controllerPos.east();
			clearArea(level, controllerPos, 2);
			placeBlockWithItem(level, player, controllerPos, ModBlocks.CONTROLLER_ITEM.get());
			return new ClientOutlineFixture(controllerPos, storagePos);
		});
		try {
			waitForClientCondition(
					() -> Minecraft.getInstance().level != null
							&& Minecraft.getInstance().level.getBlockEntity(fixture.controllerPos()) instanceof ControllerBlockEntity,
					"Client did not receive the controller before the linked-outline update");
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				placeBlockWithItem(level, player, fixture.storagePos(), ModBlocks.BARREL_ITEM.get());
				require(getController(level, fixture.controllerPos()).getStorageBlockPositions().contains(fixture.storagePos()),
						"Server controller did not register the newly placed storage");
				return true;
			});
			waitForClientCondition(
					() -> Minecraft.getInstance().level != null
							&& Minecraft.getInstance().level.getBlockEntity(fixture.controllerPos()) instanceof ControllerBlockEntity controller
							&& controller.getStorageBlockPositions().contains(fixture.storagePos()),
					"Client controller did not receive newly connected storage positions for link-mode outlines");
		} finally {
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.level(), fixture.controllerPos(), 2);
				return true;
			});
		}
	}

	private static void runControllerRoutingLockFanoutAndRestorationRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos controllerPos = player.blockPosition().offset(24, 0, 24);
		BlockPos primaryPos = controllerPos.east();
		BlockPos secondaryPos = primaryPos.east();
		clearArea(level, controllerPos, 3);
		try {
			placeBlockWithItem(level, player, controllerPos, ModBlocks.CONTROLLER_ITEM.get());
			placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.BARREL_ITEM.get());
			ControllerBlockEntity controller = getController(level, controllerPos);
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			linkSameGroup(level, player, primary, secondary);
			ItemStack emeralds = new ItemStack(Items.EMERALD, 3);
			require(insert(controller, emeralds) == 3 && extract(controller, new ItemStack(Items.EMERALD, 2)) == 2 && countItem(primary, Items.EMERALD) == 1
					&& countItem(secondary, Items.EMERALD) == 1, "Controller routing did not use linked barrel canonical contents");
			ItemStackKey emeraldKey = ItemStackKey.of(new ItemStack(Items.EMERALD));
			require(controller.getHighlightStoragePositions(controller.getStackStorages(emeraldKey)).containsAll(java.util.List.of(primaryPos, secondaryPos))
					&& controller.getStorageBlockPositions().containsAll(java.util.List.of(primaryPos, secondaryPos)),
					"Controller did not fan canonical highlights out to linked barrel endpoints");
			boolean primaryLockVisible = primary.shouldShowLock();
			boolean secondaryLockVisible = secondary.shouldShowLock();
			boolean primaryTierVisible = primary.shouldShowTier();
			boolean secondaryTierVisible = secondary.shouldShowTier();
			boolean primaryUpgradesVisible = primary.shouldShowUpgrades();
			boolean secondaryUpgradesVisible = secondary.shouldShowUpgrades();
			controller.toggleLock();
			controller.toggleLockVisibility();
			controller.toggleTierVisiblity();
			controller.toggleUpgradesVisiblity();
			require(primary.isLocked() && secondary.isLocked() && primary.shouldShowLock() != primaryLockVisible
					&& secondary.shouldShowLock() != secondaryLockVisible && primary.shouldShowTier() != primaryTierVisible
					&& secondary.shouldShowTier() != secondaryTierVisible && primary.shouldShowUpgrades() != primaryUpgradesVisible
					&& secondary.shouldShowUpgrades() != secondaryUpgradesVisible,
					"Controller lock and display toggles did not fan out to linked barrel endpoints");
			controller.toggleLock();
			primary.toggleLock();
			require(insert(controller, new ItemStack(Items.REDSTONE, 4)) == 4 && countItem(secondary, Items.REDSTONE) == 4,
					"Controller did not route around a locked linked barrel endpoint");
			secondary.toggleLock();
			require(insert(controller, new ItemStack(Items.LAPIS_LAZULI, 4)) == 0, "Controller inserted into a fully locked linked barrel group");
			primary.toggleLock();
			require(insert(controller, new ItemStack(Items.LAPIS_LAZULI, 4)) == 4, "Controller did not resume routing after a linked endpoint unlocked");
			CompoundTag savedLinkedController = controller.getUpdateTag(level.registryAccess());
			level.removeBlockEntity(controllerPos);
			ControllerBlockEntity reloadedLinkedController = new ControllerBlockEntity(controllerPos, level.getBlockState(controllerPos));
			reloadedLinkedController.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), savedLinkedController));
			level.setBlockEntity(reloadedLinkedController);
			reloadedLinkedController.onLoad();
			controller = reloadedLinkedController;
			require(controller.getStoragePositions().size() == 1
					&& controller.getStorageBlockPositions().containsAll(java.util.List.of(primaryPos, secondaryPos))
					&& insert(controller, new ItemStack(Items.GOLD_INGOT, 2)) == 2 && countItem(secondary, Items.GOLD_INGOT) == 2,
					"Reloaded controller did not restore every linked storage member and canonical routing");
			primary.toggleLock();
			level.destroyBlock(primaryPos, false);
			require(controller.getStoragePositions().isEmpty() && secondary.getControllerPos().isEmpty(),
					"Controller did not remove the linked canonical storage after its primary endpoint was destroyed");
			CompoundTag saved = controller.getUpdateTag(level.registryAccess());
			level.removeBlockEntity(controllerPos);
			ControllerBlockEntity restored = new ControllerBlockEntity(controllerPos, level.getBlockState(controllerPos));
			restored.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), saved));
			level.setBlockEntity(restored);
			restored.onLoad();
			require(restored.getStoragePositions().isEmpty() && secondary.getControllerPos().isEmpty(),
					"Controller restoration reconstructed stale linked storage topology");
		} finally {
			clearArea(level, controllerPos, 3);
		}
	}

	private static void runLinkedDoubleChestCanonicalIndexRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos controllerPos = player.blockPosition().offset(36, 0, 24);
		BlockPos leftChestPos = controllerPos.east();
		BlockPos rightChestPos = leftChestPos.east();
		BlockPos barrelPos = rightChestPos.east();
		clearArea(level, controllerPos, 4);
		float originalYRot = player.getYRot();
		try {
			player.setYRot(0);
			placeBlockWithItem(level, player, controllerPos, ModBlocks.CONTROLLER_ITEM.get());
			placeBlockWithItem(level, player, leftChestPos, ModBlocks.CHEST_ITEM.get());
			placeBlockWithItem(level, player, rightChestPos, ModBlocks.CHEST_ITEM.get(), false);
			placeBlockWithItem(level, player, barrelPos, ModBlocks.BARREL_ITEM.get());
			ControllerBlockEntity controller = getController(level, controllerPos);
			ChestBlockEntity chest = level.getBlockEntity(rightChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
					.orElseThrow(() -> new IllegalStateException("Missing main double chest"));
			ChestBlockEntity leftChest = level.getBlockEntity(leftChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
					.orElseThrow(() -> new IllegalStateException("Missing secondary double chest"));
			BarrelBlockEntity barrel = getBarrel(level, barrelPos);
			require(chest.isMainChest() && !leftChest.isMainChest(), "Linked double chest main position was not created");
			controller.toggleLock();
			require(chest.isLocked() && leftChest.isLocked() && barrel.isLocked(), "Controller did not lock every unlinked double-chest member exactly once");
			controller.toggleLock();
			require(!chest.isLocked() && !leftChest.isLocked() && !barrel.isLocked(),
					"Controller did not unlock every unlinked double-chest member exactly once");
			linkSameGroup(level, player, chest, barrel);
			chest.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			chest.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamonds = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(controller.getStoragePositions().size() == 1 && controller.getStoragePositions().contains(rightChestPos) && controller.getSlots(0) == 54
					&& countItem(barrel, Items.DIAMOND) == 7 && controller.getStackStorages(diamonds).size() == 1,
					"Controller did not keep one canonical linked double-chest index");
			controller.toggleLock();
			require(chest.isLocked() && leftChest.isLocked() && barrel.isLocked(), "Controller did not lock every linked double-chest member exactly once");
			controller.toggleLock();
			require(!chest.isLocked() && !leftChest.isLocked() && !barrel.isLocked(),
					"Controller did not unlock every linked double-chest member exactly once");
		} finally {
			player.setYRot(originalYRot);
			clearArea(level, controllerPos, 4);
		}
	}

	private static void runLinkedDoubleChestLifecycle(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType gameType = player.gameMode.getGameModeForPlayer();
		ItemStack originalHand = player.getMainHandItem().copy();
		float originalYRot = player.getYRot();
		BlockPos primaryLeft = player.blockPosition().offset(48, 0, 24);
		BlockPos primaryMain = primaryLeft.east();
		BlockPos primaryPeer = primaryLeft.south(3);
		BlockPos secondaryLeft = primaryLeft.east(4);
		BlockPos secondaryMain = secondaryLeft.east();
		BlockPos secondaryPeer = secondaryLeft.south(3);
		clearArea(level, primaryLeft.offset(3, 0, 3), 7);
		try {
			player.setGameMode(GameType.CREATIVE);
			player.setYRot(0);
			placeBlockWithItem(level, player, primaryLeft, ModBlocks.CHEST_ITEM.get());
			placeBlockWithItem(level, player, primaryPeer, ModBlocks.BARREL_ITEM.get());
			ChestBlockEntity primarySingle = getChest(level, primaryLeft);
			primarySingle.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primarySingle.getStorageWrapper().getInventoryHandler().saveInventory();
			linkSameGroup(level, player, primarySingle, getBarrel(level, primaryPeer));
			require(primarySingle.getStorageWrapper().getContents().inventory().stacks().stream().noneMatch(stack -> stack.is(Items.DIAMOND)),
					"Binding linked storage retained duplicate physical chest contents");
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primarySingle, "primary chest before double formation");
			placeBlockWithItem(level, player, primaryMain, ModBlocks.CHEST_ITEM.get(), false);
			ChestBlockEntity primary = getChest(level, primaryMain);
			require(isDoubleChest(level, primaryLeft, primaryMain) && primary.isPrimaryLinkedStorage()
					&& primaryEndpoint.equals(requireEndpoint(primary, "formed primary double chest"))
					&& primary.getStorageWrapper().getInventoryHandler().size() == 54,
					"Primary linked endpoint did not transfer and expand during double-chest formation");
			ItemStack linker = new ItemStack(ENDER_LINKER.get());
			useLinkerAsPlayer(level, player, primaryLeft, linker);
			require(isBoundTo(player.getMainHandItem(), primaryEndpoint.groupId()),
					"Using the linker on the non-main chest half did not target the canonical linked endpoint");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			player.setGameMode(GameType.SURVIVAL);
			clearDroppedItems(level, primaryMain);
			require(player.gameMode.destroyBlock(primaryMain), "Primary linked double-chest destruction failed");
			ItemStack carrier = findAndRemoveDroppedChest(level, primaryMain);
			require(level.getBlockState(primaryLeft).isAir() && ChestBlockItem.isDoubleChest(carrier)
					&& primaryEndpoint.equals(carrier.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& Boolean.TRUE.equals(carrier.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT))
					&& !hasDroppedItem(level, primaryMain, Items.DIAMOND),
					"Primary linked double-chest removal did not preserve one endpoint carrier without duplicate contents");
			placeBlockWithStack(level, player, primaryLeft, carrier);
			BlockPos restoredLeft = primaryLeft.west();
			ChestBlockEntity restored = getChest(level, primaryLeft);
			require(isDoubleChest(level, restoredLeft, primaryLeft) && restored.isPrimaryLinkedStorage()
					&& primaryEndpoint.equals(requireEndpoint(restored, "restored primary double chest")) && countItem(restored, Items.DIAMOND) == 7,
					"Linked double-chest carrier did not restore endpoint and contents");

			player.setGameMode(GameType.CREATIVE);
			placeBlockWithItem(level, player, secondaryPeer, ModBlocks.BARREL_ITEM.get());
			placeBlockWithItem(level, player, secondaryLeft, ModBlocks.CHEST_ITEM.get());
			ChestBlockEntity secondarySingle = getChest(level, secondaryLeft);
			linkSameGroup(level, player, getBarrel(level, secondaryPeer), secondarySingle);
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondarySingle, "secondary chest before double formation");
			placeBlockWithItem(level, player, secondaryMain, ModBlocks.CHEST_ITEM.get(), false);
			ChestBlockEntity secondary = getChest(level, secondaryMain);
			require(isDoubleChest(level, secondaryLeft, secondaryMain) && !secondary.isPrimaryLinkedStorage()
					&& secondaryEndpoint.equals(requireEndpoint(secondary, "formed secondary double chest")),
					"Secondary linked endpoint did not transfer during double-chest formation");
			BlockState secondaryLeftState = level.getBlockState(secondaryLeft);
			BlockState secondaryMainState = level.getBlockState(secondaryMain);
			player.setGameMode(GameType.SURVIVAL);
			ItemStack secondaryUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			require(player.gameMode.useItemOn(player, level, secondaryUpgrade, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(secondaryLeft), Direction.UP, secondaryLeft, false)) == InteractionResult.FAIL
					&& secondaryUpgrade.getCount() == 1 && level.getBlockState(secondaryLeft).equals(secondaryLeftState)
					&& level.getBlockState(secondaryMain).equals(secondaryMainState)
					&& secondaryEndpoint.equals(requireEndpoint(getChest(level, secondaryMain), "secondary double chest main after rejected tier upgrade"))
					&& getChest(level, secondaryLeft).getMainChestBlockEntity() == getChest(level, secondaryMain),
					"Non-main half of a linked secondary double chest accepted a tier upgrade or moved its endpoint");
			player.setGameMode(GameType.CREATIVE);
			level.setBlock(secondaryMain, Blocks.BARREL.defaultBlockState(), 3);
			require(level.getBlockState(secondaryLeft).getValue(ChestBlock.TYPE) == ChestType.SINGLE
					&& secondaryEndpoint.equals(requireEndpoint(getChest(level, secondaryLeft), "split secondary chest")),
					"Replacing a secondary double-chest half did not retain its endpoint");
			runUnnamedSecondaryDoubleChestMainBreak(level, player, primaryLeft.south(6));
			runSecondaryDoubleChestBreak(level, player, primaryLeft.east(4).south(6), Direction.WEST, false);
		} finally {
			player.setGameMode(gameType);
			player.setItemInHand(InteractionHand.MAIN_HAND, originalHand);
			player.setYRot(originalYRot);
			clearArea(level, primaryLeft.offset(3, 0, 3), 7);
		}
	}

	private static void runUnnamedSecondaryDoubleChestMainBreak(ServerLevel level, ServerPlayer player, BlockPos primaryPos) {
		BlockPos originalMainPos = primaryPos.east(5);
		BlockPos firstSurvivorPos = originalMainPos.west();
		BlockPos secondSurvivorPos = firstSurvivorPos.west();
		placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
		placeBlockWithItem(level, player, originalMainPos, ModBlocks.CHEST_ITEM.get());
		BarrelBlockEntity linkedPrimary = getBarrel(level, primaryPos);
		ChestBlockEntity secondary = getChest(level, originalMainPos);
		linkSameGroup(level, player, linkedPrimary, secondary);
		InventoryHandler canonicalInventory = linkedPrimary.getStorageWrapper().getInventoryHandler();
		fillInventoryWithTestContents(canonicalInventory);
		List<ItemStack> canonicalContents = new ArrayList<>();
		for (int slot = 0; slot < canonicalInventory.size(); slot++) {
			canonicalContents.add(canonicalInventory.getStackInSlot(slot).copy());
		}
		canonicalInventory.saveInventory();
		LinkedStorageEndpointData endpoint = requireEndpoint(secondary, "unnamed secondary chest before main break lifecycle");

		placeBlockWithItem(level, player, firstSurvivorPos, ModBlocks.CHEST_ITEM.get(), false);
		require(isDoubleChest(level, firstSurvivorPos, originalMainPos) && getChest(level, originalMainPos).isMainChest(),
				"Adding a chest west of the linked secondary did not leave it as the double-chest main part");
		player.setGameMode(GameType.SURVIVAL);
		clearDroppedItems(level, originalMainPos);
		require(player.gameMode.destroyBlock(originalMainPos), "Original main linked secondary double-chest destruction failed");
		ItemStack firstDrop = findAndRemoveDroppedChest(level, originalMainPos);
		ChestBlockEntity firstSurvivor = getChest(level, firstSurvivorPos);
		require(firstSurvivor.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
				&& endpoint.equals(requireEndpoint(firstSurvivor, "first unnamed secondary chest survivor"))
				&& hasInventoryContents(firstSurvivor.getStorageWrapper().getInventoryHandler(), canonicalContents) && firstSurvivor.getCustomName() == null
				&& isOrdinaryUnnamedSingleChestDrop(firstDrop),
				"Breaking the original main linked secondary chest did not preserve an unnamed survivor and ordinary drop");

		placeBlockWithStack(level, player, secondSurvivorPos, firstDrop);
		require(isDoubleChest(level, secondSurvivorPos, firstSurvivorPos) && getChest(level, firstSurvivorPos).isMainChest()
				&& getChest(level, firstSurvivorPos).getCustomName() == null,
				"Placing the ordinary drop west of the linked survivor did not keep the former survivor unnamed and main");
		clearDroppedItems(level, firstSurvivorPos);
		require(player.gameMode.destroyBlock(firstSurvivorPos), "Former survivor linked secondary double-chest destruction failed");
		ItemStack secondDrop = findAndRemoveDroppedChest(level, firstSurvivorPos);
		ChestBlockEntity secondSurvivor = getChest(level, secondSurvivorPos);
		require(secondSurvivor.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
				&& endpoint.equals(requireEndpoint(secondSurvivor, "second unnamed secondary chest survivor"))
				&& hasInventoryContents(secondSurvivor.getStorageWrapper().getInventoryHandler(), canonicalContents) && secondSurvivor.getCustomName() == null
				&& isOrdinaryUnnamedSingleChestDrop(secondDrop),
				"Breaking the former linked survivor did not preserve an unnamed secondary endpoint and ordinary drop");
		require(LinkedStorageGroupsSavedData.get(level).manager().unregisterEndpoint(endpoint.groupId(), endpoint.endpointId()),
				"Could not clean up the unnamed secondary chest endpoint");
		player.setGameMode(GameType.CREATIVE);
	}

	private static void runSecondaryDoubleChestBreak(ServerLevel level, ServerPlayer player, BlockPos secondaryPos, Direction addedDirection,
			boolean breakMain) {
		BlockPos peerPos = secondaryPos.south(3);
		BlockPos addedPos = secondaryPos.relative(addedDirection);
		placeBlockWithItem(level, player, peerPos, ModBlocks.BARREL_ITEM.get());
		placeBlockWithItem(level, player, secondaryPos, ModBlocks.CHEST_ITEM.get());
		ChestBlockEntity secondary = getChest(level, secondaryPos);
		BarrelBlockEntity linkedPrimary = getBarrel(level, peerPos);
		linkSameGroup(level, player, linkedPrimary, secondary);
		InventoryHandler canonicalInventory = linkedPrimary.getStorageWrapper().getInventoryHandler();
		fillInventoryWithTestContents(canonicalInventory);
		List<ItemStack> canonicalContents = new ArrayList<>();
		for (int slot = 0; slot < canonicalInventory.size(); slot++) {
			canonicalContents.add(canonicalInventory.getStackInSlot(slot).copy());
		}
		canonicalInventory.saveInventory();
		LinkedStorageEndpointData endpoint = requireEndpoint(secondary, "secondary chest before break lifecycle");
		placeBlockWithItem(level, player, addedPos, ModBlocks.CHEST_ITEM.get(), false);
		ChestBlockEntity main = getChest(level, secondaryPos).isMainChest() ? getChest(level, secondaryPos) : getChest(level, addedPos);
		BlockPos brokenPos = breakMain ? main.getBlockPos() : main.getBlockPos().equals(secondaryPos) ? addedPos : secondaryPos;
		BlockPos remainingPos = brokenPos.equals(secondaryPos) ? addedPos : secondaryPos;
		player.setGameMode(GameType.SURVIVAL);
		clearDroppedItems(level, brokenPos);
		require(player.gameMode.destroyBlock(brokenPos), "Secondary linked double-chest destruction failed");
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(brokenPos).inflate(1.5D));
		ItemStack ordinaryDrop = drops.size() == 1 ? drops.getFirst().getItem().copy() : ItemStack.EMPTY;
		drops.forEach(ItemEntity::discard);
		ChestBlockEntity remaining = getChest(level, remainingPos);
		require(remaining.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
				&& endpoint.equals(requireEndpoint(remaining, "remaining secondary chest")) && !ChestBlockItem.isDoubleChest(ordinaryDrop)
				&& ordinaryDrop.is(ModBlocks.CHEST_ITEM.get()) && ordinaryDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
				&& hasInventoryContents(linkedPrimary.getStorageWrapper().getInventoryHandler(), canonicalContents),
				"Breaking one secondary double-chest half changed linked contents or did not leave one ordinary drop");
		clearDroppedItems(level, remainingPos);
		require(player.gameMode.destroyBlock(remainingPos), "Remaining linked secondary chest destruction failed");
		ItemStack endpointDrop = findAndRemoveDroppedChest(level, remainingPos);
		require(endpoint.equals(endpointDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
				"Remaining linked secondary chest did not preserve its endpoint drop");
		player.setGameMode(GameType.CREATIVE);
	}

	private static void runLinkedControllerTierCandidateAndPaintRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos controllerPos = player.blockPosition().offset(60, 0, 24);
		BlockPos primaryPos = controllerPos.east();
		BlockPos secondaryPos = primaryPos.east();
		GameType gameType = player.gameMode.getGameModeForPlayer();
		ItemStack originalHand = player.getMainHandItem().copy();
		ItemStack originalDyes = player.getInventory().getItem(0).copy();
		clearArea(level, controllerPos, 3);
		try {
			placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.BARREL_ITEM.get());
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			linkSameGroup(level, player, primary, secondary);
			placeBlockWithItem(level, player, controllerPos, ModBlocks.CONTROLLER_ITEM.get());
			ControllerBlockEntity controller = getController(level, controllerPos);
			require(controller.getStorageTierUpgradePositions().equals(Set.of(primaryPos)),
					"Controller tier candidate API did not return exactly the linked primary endpoint");
			player.setGameMode(GameType.SURVIVAL);
			ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
			require(player.gameMode
					.useItemOn(player, level, upgrade, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false))
					.consumesAction() && level.getBlockState(primaryPos).is(ModBlocks.DIAMOND_BARREL.get())
					&& level.getBlockState(secondaryPos).is(ModBlocks.BARREL.get()),
					"Controller tier upgrade did not choose only the linked primary candidate");
			ItemStack paintbrush = new ItemStack(ModItems.PAINTBRUSH.get());
			PaintbrushItem.setMainColor(paintbrush, 0xFFFF0000);
			PaintbrushItem.ItemRequirements requirements = PaintbrushItem.getItemRequirements(paintbrush, player, level, controllerPos)
					.orElseThrow(() -> new IllegalStateException("Controller paintbrush did not report dye requirements"));
			require(countRequirements(requirements, Items.DYE.red()) == 2, "Controller paintbrush did not require exactly two red dyes");
			player.getInventory().setItem(0, new ItemStack(Items.DYE.red(), 2));
			require(paintbrush.getItem().onItemUseFirst(paintbrush,
					new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false))) == InteractionResult.SUCCESS
					&& getBarrel(level, primaryPos).getStorageWrapper().getMainColor() == 0xFFFF0000
					&& getBarrel(level, secondaryPos).getStorageWrapper().getMainColor() == 0xFFFF0000,
					"Controller paintbrush did not color both physical linked endpoints");
		} finally {
			player.setGameMode(gameType);
			player.setItemInHand(InteractionHand.MAIN_HAND, originalHand);
			player.getInventory().setItem(0, originalDyes);
			clearArea(level, controllerPos, 3);
		}
	}

	private static void runDroppedPrimaryRenameRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 72);
		BlockPos secondaryPos = primaryPos.east(3);
		GameType gameType = player.gameMode.getGameModeForPlayer();
		ItemStack originalHand = player.getMainHandItem().copy();
		clearArea(level, primaryPos, 2);
		try {
			placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.BARREL_ITEM.get());
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			linkSameGroup(level, player, primary, secondary);
			LinkedStorageEndpointData endpoint = requireEndpoint(primary, "rename primary barrel");
			player.setGameMode(GameType.SURVIVAL);
			require(player.gameMode.destroyBlock(primaryPos), "Could not break linked primary barrel for rename regression");
			ItemStack renamedPrimary = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos).inflate(1)).stream()
					.filter(item -> endpoint.equals(item.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst()
					.orElseThrow(() -> new IllegalStateException("Breaking linked primary barrel did not create an endpoint drop")).getItem().copy();
			renamedPrimary.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed Linked Storage"));
			player.setItemInHand(InteractionHand.MAIN_HAND, renamedPrimary);
			require(player.gameMode.useItemOn(player, level, renamedPrimary, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(primaryPos.below()), Direction.UP, primaryPos.below(), false)).consumesAction(),
					"Could not place renamed linked primary barrel");
			primary = getBarrel(level, primaryPos);
			require(endpoint.equals(requireEndpoint(primary, "replaced renamed primary barrel"))
					&& primary.getStorageWrapper().getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent()
					&& primary.getMenuDisplayName().getString().equals("Renamed Linked Storage")
					&& secondary.getMenuDisplayName().getString().equals("Renamed Linked Storage"),
					"Dropped primary rename did not update the canonical linked group name");
		} finally {
			player.setGameMode(gameType);
			player.setItemInHand(InteractionHand.MAIN_HAND, originalHand);
			clearArea(level, primaryPos, 2);
		}
	}

	private static void runLinkedStorageTierUpgradeRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 84);
		BlockPos secondaryPos = primaryPos.east(3);
		GameType gameType = player.gameMode.getGameModeForPlayer();
		clearArea(level, primaryPos, 2);
		try {
			placeBlockWithItem(level, player, primaryPos, ModBlocks.BARREL_ITEM.get());
			placeBlockWithItem(level, player, secondaryPos, ModBlocks.BARREL_ITEM.get());
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			linkSameGroup(level, player, primary, secondary);
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "tier upgrade primary barrel");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "tier upgrade secondary barrel");
			player.setGameMode(GameType.SURVIVAL);
			ItemStack secondaryUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			require(player.gameMode.useItemOn(player, level, secondaryUpgrade, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(secondaryPos), Direction.UP, secondaryPos, false)) == InteractionResult.FAIL,
					"Secondary linked barrel accepted a tier upgrade");
			require(secondaryUpgrade.getCount() == 1 && level.getBlockState(secondaryPos).is(ModBlocks.BARREL.get())
					&& secondary.getStorageWrapper().getInventoryHandler().size() == ModBlocks.BARREL.get().getNumberOfInventorySlots(),
					"Secondary linked barrel consumed its tier upgrade or changed canonical capacity");
			ItemStack primaryUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			require(player.gameMode
					.useItemOn(player, level, primaryUpgrade, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(primaryPos), Direction.UP, primaryPos, false))
					.consumesAction() && primaryUpgrade.isEmpty() && level.getBlockState(primaryPos).is(ModBlocks.DIAMOND_BARREL.get()),
					"Primary linked barrel did not accept its tier upgrade");
			primary = getBarrel(level, primaryPos);
			secondary = getBarrel(level, secondaryPos);
			require(primaryEndpoint.equals(requireEndpoint(primary, "upgraded primary barrel"))
					&& secondaryEndpoint.equals(requireEndpoint(secondary, "upgraded secondary barrel"))
					&& primary.getStorageWrapper().getInventoryHandler().size() == ModBlocks.DIAMOND_BARREL.get().getNumberOfInventorySlots()
					&& secondary.getStorageWrapper().getInventoryHandler().size() == ModBlocks.DIAMOND_BARREL.get().getNumberOfInventorySlots()
					&& primary.getStorageWrapper().getUpgradeHandler().size() == ModBlocks.DIAMOND_BARREL.get().getNumberOfUpgradeSlots()
					&& secondary.getStorageWrapper().getUpgradeHandler().size() == ModBlocks.DIAMOND_BARREL.get().getNumberOfUpgradeSlots(),
					"Primary tier upgrade did not preserve linked endpoint identity and canonical capacities");
		} finally {
			player.setGameMode(gameType);
			clearArea(level, primaryPos, 2);
		}
	}

	private static void linkSameGroup(ServerLevel level, ServerPlayer player, StorageBlockEntity primary, StorageBlockEntity secondary) {
		ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not create a linked storage group");
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not add storage to a linked storage group");
		require(requireEndpoint(primary, "primary").groupId().equals(requireEndpoint(secondary, "secondary").groupId()),
				"Linked storage endpoints did not share a group");
	}

	private static int insert(ControllerBlockEntity controller, ItemStack stack) {
		try (Transaction transaction = Transaction.openRoot()) {
			int inserted = controller.insert(ItemResource.of(stack), stack.getCount(), transaction);
			if (inserted > 0) {
				transaction.commit();
			}
			return inserted;
		}
	}

	private static int extract(ControllerBlockEntity controller, ItemStack stack) {
		try (Transaction transaction = Transaction.openRoot()) {
			int extracted = controller.extract(ItemResource.of(stack), stack.getCount(), transaction);
			if (extracted > 0) {
				transaction.commit();
			}
			return extracted;
		}
	}

	private static void waitForClientLimitedRenderProjection(UUID groupId) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		do {
			if (AutomationRuntime.runOnClient(() -> hasClientLimitedRenderProjection(groupId))) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted waiting for linked limited barrel client render projection", e);
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for linked limited barrel client render projection");
	}

	private static boolean hasClientLimitedRenderProjection(UUID groupId) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			return false;
		}
		BlockPos primaryPos = limitedPrimaryPos(minecraft.player);
		return minecraft.level.getBlockEntity(primaryPos) instanceof LimitedBarrelBlockEntity primary
				&& minecraft.level.getBlockEntity(primaryPos.east(3)) instanceof LimitedBarrelBlockEntity secondary && hasLimitedRenderProjection(primary)
				&& hasLimitedRenderProjection(secondary);
	}

	private static BlockPos limitedPrimaryPos(net.minecraft.world.entity.player.Player player) {
		return new BlockPos(player.blockPosition().getX(), player.level().getMinY() + 4, player.blockPosition().getZ() + 4);
	}

	private static boolean hasLimitedRenderProjection(LimitedBarrelBlockEntity barrel) {
		return barrel.getStorageWrapper().getRenderDataHandler().getDisplayData().displayItems().stream()
				.anyMatch(displayItem -> displayItem.item() != null && displayItem.item().is(Items.DIAMOND))
				&& barrel.getSlotCounts().contains(LINKED_LIMITED_RELOAD_ITEM_COUNT)
				&& barrel.getSlotFillLevels().stream().anyMatch(fillLevel -> fillLevel > 0F);
	}

	private static int countItem(StorageBlockEntity storage, Item item) {
		int count = 0;
		for (int slot = 0; slot < storage.getStorageWrapper().getInventoryHandler().size(); slot++) {
			ItemStack stack = storage.getStorageWrapper().getInventoryHandler().getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
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

	private static int countCanonicalItem(ServerLevel level, UUID groupId, Item item) {
		return LinkedStorageGroupsSavedData.get(level).manager().resolveContents(groupId)
				.map(contents -> contents.contents().inventory().stacks().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum())
				.orElse(-1);
	}

	private static void useLinkerAsPlayer(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack linker) {
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		player.gameMode.useItemOn(player, level, linker, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	private static ItemStack findBoundLinker(ServerPlayer player, UUID groupId) {
		ItemStack mainHand = player.getMainHandItem();
		if (isBoundTo(mainHand, groupId)) {
			return mainHand;
		}
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (isBoundTo(stack, groupId)) {
				return stack;
			}
		}
		throw new IllegalStateException("Real-player linker interaction did not deliver a bound linker");
	}

	private static boolean isBoundTo(ItemStack stack, UUID groupId) {
		EnderLinkerTargetData target = stack.get(ModCoreDataComponents.ENDER_LINKER_TARGET);
		return target != null && groupId.equals(target.groupId());
	}

	private static void placeBlockWithItem(ServerLevel level, ServerPlayer player, BlockPos pos, Item item) {
		placeBlockWithItem(level, player, pos, item, true);
	}

	private static void stabilizePlayer(ServerPlayer player) {
		ServerLevel level = player.level();
		int safeY = Math.max(player.blockPosition().getY(), 64);
		BlockPos safePos = new BlockPos(player.blockPosition().getX(), safeY, player.blockPosition().getZ());
		for (int x = -4; x <= 10; x++) {
			for (int z = -4; z <= 4; z++) {
				level.setBlock(safePos.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 3);
			}
		}
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
		if (!player.teleportTo(level, safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D, Set.of(), player.getYRot(), player.getXRot(), false)) {
			throw new IllegalStateException("Failed to stabilize player for linked storage regression");
		}
	}

	private static void movePlayerNear(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
			player.setDeltaMovement(Vec3.ZERO);
			player.resetFallDistance();
			if (!player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, Set.of(), player.getYRot(), player.getXRot(), false)) {
				throw new IllegalStateException("Failed to move player near linked storage fixture");
			}
			return true;
		});
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.blockPosition().closerThan(pos, 2))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out moving the client player near linked storage fixture");
	}

	private static void ensurePlayerAlive() {
		boolean respawnRequested = AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null && (minecraft.player.getHealth() <= 0 || minecraft.gui.screen() instanceof DeathScreen)) {
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
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getHealth() > 0
					&& !(Minecraft.getInstance().gui.screen() instanceof DeathScreen))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for the automation player to respawn");
	}

	private static void placeBlockWithItem(ServerLevel level, ServerPlayer player, BlockPos pos, Item item, boolean sneaking) {
		BlockPos supportPos = pos.below();
		level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
		ItemStack stack = new ItemStack(item);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.setShiftKeyDown(sneaking);
		player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false));
		player.setShiftKeyDown(false);
	}

	private static BarrelBlockEntity getBarrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing barrel at " + pos));
	}

	private static void placeBlockWithStack(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		BlockPos supportPos = pos.below();
		level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.setShiftKeyDown(false);
		player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false));
	}

	private static ChestBlockEntity getChest(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing chest at " + pos));
	}

	private static LimitedBarrelBlockEntity getLimitedBarrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing limited barrel at " + pos));
	}

	private static ControllerBlockEntity getController(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing controller at " + pos));
	}

	private static LinkedStorageEndpointData requireEndpoint(StorageBlockEntity storage, String name) {
		LinkedStorageEndpointData endpoint = storage.getLinkedStorageEndpointData();
		if (endpoint == null) {
			throw new IllegalStateException("Missing linked storage endpoint for " + name);
		}
		return endpoint;
	}

	private static boolean isDoubleChest(ServerLevel level, BlockPos left, BlockPos main) {
		return level.getBlockState(left).is(ModBlocks.CHEST.get()) && level.getBlockState(main).is(ModBlocks.CHEST.get())
				&& level.getBlockState(left).getValue(ChestBlock.TYPE) == ChestType.LEFT
				&& level.getBlockState(main).getValue(ChestBlock.TYPE) == ChestType.RIGHT && getChest(level, left).getMainPos().equals(main)
				&& getChest(level, main).getMainPos().equals(main);
	}

	private static ItemStack findAndRemoveDroppedChest(ServerLevel level, BlockPos pos) {
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), item -> item.getItem().is(ModBlocks.CHEST_ITEM.get()));
		require(drops.size() == 1, "Chest break produced " + drops.size() + " chest drops instead of exactly one");
		ItemStack stack = drops.getFirst().getItem().copy();
		drops.forEach(ItemEntity::discard);
		return stack;
	}

	private static boolean isOrdinaryUnnamedSingleChestDrop(ItemStack stack) {
		return stack.is(ModBlocks.CHEST_ITEM.get()) && !ChestBlockItem.isDoubleChest(stack) && stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
				&& stack.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT) == null && stack.get(DataComponents.CUSTOM_NAME) == null;
	}

	private static void fillInventoryWithTestContents(InventoryHandler handler) {
		for (int slot = 0; slot < handler.size(); slot++) {
			handler.setStackInSlot(slot, new ItemStack(Items.DIAMOND, slot + 1));
		}
	}

	private static boolean hasInventoryContents(InventoryHandler handler, List<ItemStack> contents) {
		if (handler.size() != contents.size()) {
			return false;
		}
		for (int slot = 0; slot < handler.size(); slot++) {
			if (!ItemStack.matches(handler.getStackInSlot(slot), contents.get(slot))) {
				return false;
			}
		}
		return true;
	}

	private static void clearDroppedItems(ServerLevel level, BlockPos pos) {
		level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).forEach(ItemEntity::discard);
	}

	private static int countRequirements(PaintbrushItem.ItemRequirements requirements, Item item) {
		return java.util.stream.Stream.concat(requirements.itemsPresent().stream(), requirements.itemsMissing().stream()).filter(stack -> stack.is(item))
				.mapToInt(ItemStack::getCount).sum();
	}

	private static boolean hasDroppedItem(ServerLevel level, BlockPos pos, Item item) {
		return !level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), entity -> entity.getItem().is(item)).isEmpty();
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
		ItemEntity item = new ItemEntity(level, Vec3.atCenterOf(pos).x + xOffset, Vec3.atCenterOf(pos).y + 1.25, Vec3.atCenterOf(pos).z,
				new ItemStack(Items.ENDER_PEARL));
		item.setNoGravity(true);
		item.setDeltaMovement(Vec3.ZERO);
		item.setPickUpDelay(0);
		level.addFreshEntity(item);
		return item;
	}

	private static List<ItemEntity> pickupItems(ServerLevel level, BlockPos pos) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), entity -> entity.getItem().is(Items.ENDER_PEARL));
	}

	private static void clearPickupEntities(ServerLevel level, BlockPos primaryPos, BlockPos secondaryPos) {
		level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos).inflate(2)).forEach(ItemEntity::discard);
		level.getEntitiesOfClass(ItemEntity.class, new AABB(secondaryPos).inflate(2)).forEach(ItemEntity::discard);
	}

	private static void waitForClientTooltipCarrier(TooltipFixture fixture) {
		waitForClientCondition(
				() -> Minecraft.getInstance().player != null
						&& fixture.endpoint()
								.equals(Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot())
										.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
						&& StorageBlockEntity.getLinkedStorageEndpointRole(Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot()))
								.filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent(),
				"Exact linked tooltip carrier did not synchronize to client inventory slot 8");
	}

	private static LinkedStorageSnapshotProfile profile(StorageBlockEntity storage) {
		return new LinkedStorageSnapshotProfile(storage.getMenuDisplayName(), storage.getStorageWrapper().getInventoryHandler().size(),
				storage.getStorageWrapper().getUpgradeHandler().size(), storage.getStorageWrapper().getColumnsTaken());
	}

	private static boolean hasMemory(StorageBlockEntity storage, Item item) {
		return storage.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotFilterStack(0, true)
				.filter(stack -> stack.is(item)).isPresent();
	}

	private static void waitForExpandedClientMenu(ExpansionFixture fixture) {
		waitForClientCondition(
				() -> Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu menu
						&& fixture.profile().inventorySlots() == 54 && menu.getNumberOfStorageInventorySlots() == fixture.profile().inventorySlots()
						&& menu.getNumberOfUpgradeSlots() == fixture.profile().upgradeSlots() && menu.getColumnsTaken() == fixture.profile().columnsTaken()
						&& menu.getStorageWrapper().getInventoryHandler().size() == fixture.profile().inventorySlots()
						&& menu.getStorageWrapper().getUpgradeHandler().size() == fixture.profile().upgradeSlots()
						&& menu.getStorageWrapper().getColumnsTaken() == fixture.profile().columnsTaken()
						&& screen.getTitle().equals(fixture.profile().groupName()) && menu.getStorageBlockEntity().getLinkedStorageEndpointData() != null
						&& fixture.groupId().equals(menu.getStorageBlockEntity().getLinkedStorageEndpointData().groupId())
						&& ClientLinkedStorageContents.getGroupName(fixture.groupId()).filter(fixture.profile().groupName()::equals).isPresent()
						&& ClientLinkedStorageContents.getInventorySlots(fixture.groupId()).orElse(-1) == fixture.profile().inventorySlots()
						&& ClientLinkedStorageContents.getUpgradeSlots(fixture.groupId()).orElse(-1) == fixture.profile().upgradeSlots()
						&& ClientLinkedStorageContents.getColumnsTaken(fixture.groupId()).orElse(-1) == fixture.profile().columnsTaken()
						&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
				"Expanded linked primary client menu did not expose its 54-slot canonical snapshot");
	}

	private static void waitForServerCondition(java.util.function.Predicate<ServerPlayer> condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		do {
			if (AutomationRuntime.runOnServer(condition::test)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for linked storage server state");
	}

	private static void waitForClientCondition(java.util.function.BooleanSupplier condition, String message) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		do {
			if (AutomationRuntime.runOnClient(condition::getAsBoolean)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(message);
	}

	private static void waitForClosedStorageMenu() {
		waitForClientCondition(() -> !(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?>),
				"Stale linked storage menu remained open on the client");
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted waiting for linked storage state", e);
		}
	}

	private static void clearArea(ServerLevel level, BlockPos center, int radius) {
		for (int x = -radius; x <= radius; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -radius; z <= radius; z++) {
					BlockPos pos = center.offset(x, y, z);
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
						storage.clearContent();
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private record LinkedStorageMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, LinkedStorageSnapshotProfile profile) {
	}

	private record ExpansionFixture(BlockPos primaryPos, UUID groupId, LinkedStorageSnapshotProfile profile) {
	}

	private record MemoryFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, ItemStack originalHand) {
	}

	private record TooltipFixture(BlockPos pos, LinkedStorageEndpointData endpoint, int inventorySlot, ItemStack originalStack) {
	}

	private record PickupFixture(BlockPos primaryPos, BlockPos secondaryPos, List<UUID> itemIds, long verifyTime) {
	}

	private record ClientOutlineFixture(BlockPos controllerPos, BlockPos storagePos) {
	}
}
