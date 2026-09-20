package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
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
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.*;
import net.p3pp3rf1y.sophisticatedstorage.network.OpenStorageInventoryPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

public final class StorageLinkedStorageRegression {
	private static final int MAX_MAGNET_RANGE = 20;
	private static final int FIXTURE_PLATFORM_RADIUS = 15;
	private static final int CLIENT_MENU_CONVERGENCE_SECONDS = 10;
	private static final int LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT = 7;
	private static final int LINKED_LIMITED_RELOAD_ITEM_COUNT = 23;
	private static final BlockPos LINKED_LIMITED_RELOAD_PRIMARY_POS = new BlockPos(0, 73, 48);
	private static final BlockPos LINKED_LIMITED_RELOAD_SECONDARY_POS = LINKED_LIMITED_RELOAD_PRIMARY_POS.east(3);

	private StorageLinkedStorageRegression() {
	}

	public static String run() {
		String result = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runOnServer);
		runCanonicalStorageTypeInsertionRulesRegression();
		runStorageFamilyCompatibilityRegression();
		runLinkedShulkerStashRegression();
		runLinkedUpgradeSwitchRefreshRegression();
		runLinkedLimitedBarrelMemorizedSlotMenuRegression();
		runLinkedPrimaryChestExpansionRegression();
		runLinkedSecondaryChestSplitRegression();
		runCreativeEndpointPlacementRegression();
		runLinkedControllerEndpointJoinDoesNotDuplicateCanonicalContentsRegression();
		runPlacedLinkedDoubleChestControllerRegression();
		runLinkedStorageControllerRegression();
		runLegacyControllerStorageKeysMigrationRegression();
		runLinkedControllerNonListenerRemovalIndexesRegression();
		runLinkedControllerRemovalReconnectRegression();
		runLinkedControllerTierUpgradeRegression();
		runLinkedControllerTierUpgradeRequiresPrimaryRegression();
		runLinkedControllerPaintbrushRegression();
		runLockedLinkedControllerRoutingRegression();
		runLinkedControllerClientOutlineRegression();
		runLinkedControllerBridgeReconnectRegression();
		runLinkedControllerPartialGroupDisconnectRegression();
		runEndpointUnloadReloadRegression();
		runDroppedItemPickupRegression();
		runLinkedStorageStackTooltipRegression();
		runComponentlessBarrelItemModelRegression();
		runLinkedStorageMenuRegression();
		runLinkedStorageMenuTransitionRegression();
		runDroppedPrimaryRenameRegression();
		runLinkedStorageTierUpgradeRegression();
		runTierUpgradeMenuInvalidationRegression();
		return result.substring(0, result.length() - 1) + ",\"canonicalStorageTypeControlsInsertionRules\":true,\"creativePlacementCreatesSecondary\":true,"
				+ "\"endpointUnloadReloadReattachesCanonicalState\":true," + "\"linkedControllerEndpointJoinKeepsOneCanonicalContentIndex\":true,"
				+ "\"placedLinkedDoubleChestKeepsOneCanonicalContentIndex\":true,"
				+ "\"linkedControllerUsesOneCanonicalGroupAndAnyUnlockedMember\":true,\"linkedControllerHighlightsEveryGroupMember\":true,"
				+ "\"legacyControllerStorageKeysMigrationDistinguishesAbsentAndEmpty\":true,"
				+ "\"linkedControllerNonListenerRemovalPreservesRoutingIndexes\":true,\"linkedControllerRemovalDetachesEveryPhysicalMember\":true,"
				+ "\"linkedControllerTierUpgradeUpdatesCanonicalCapacityOnce\":true,\"linkedControllerTierUpgradeRequiresPrimaryEndpoint\":true,"
				+ "\"linkedControllerPaintbrushCoversEveryPhysicalMember\":true,"
				+ "\"linkedControllerOutlineHighlightsEveryGroupMember\":true,\"linkedControllerClientOutlineIncludesEveryGroupMember\":true,"
				+ "\"linkedControllerToolOperationsApplyToEveryGroupMember\":true,\"lockedLinkedControllerRoutingPreservesMatchingItems\":true,"
				+ "\"linkedControllerBridgeReconnectsDownstreamStorage\":true,\"linkedControllerPartialGroupDisconnectPrunesRemoteMembers\":true,"
				+ "\"linkedControllerPartialGroupReconnectsMembersAndToolActions\":true,"
				+ "\"secondarySkipsDroppedItemPickup\":true,\"primaryProcessesDroppedItemPickup\":true,\"canonicalMenuSlots\":true,"
				+ "\"linkedStorageMenuCanonicalSnapshot\":true," + "\"linkedStorageStackTooltipLoadsCanonicalContents\":true,"
				+ "\"componentlessBarrelItemModelDoesNotInheritPreviousRenderState\":true,"
				+ "\"primaryContentsSettingsAndUpgradesMigrate\":true,\"secondaryLocalSettingsAreDiscarded\":true,"
				+ "\"canonicalSettingsAndUpgradesShared\":true," + "\"canonicalRenderProjectionFansOut\":true," + "\"endpointLocksKeepInputFiltersLocal\":true,"
				+ "\"linkedStorageListenerFailuresAreIsolated\":true,"
				+ "\"linkedStorageMenuTitlesAndRoles\":true,\"droppedPrimaryNameSynchronizesBeforePlacement\":true,"
				+ "\"placedPrimaryRetainsSynchronizedGroupName\":true,\"secondaryNameDoesNotSynchronizeGroup\":true,"
				+ "\"primaryStackRetainsCustomName\":true,\"secondaryTierUpgradeRejected\":true,"
				+ "\"primaryTierUpgradeUpdatesCanonicalSlots\":true,\"tierUpgradeClosesStaleLinkedMenus\":true,"
				+ "\"standardStorageTypesInterlink\":true,\"limitedBarrelsRequireMatchingSlotFamily\":true,"
				+ "\"linkedShulkerStashUsesCanonicalHostAndPrimaryRules\":true," + "\"linkedUpgradeSwitchesRefreshInOpenMenu\":true,"
				+ "\"linkedLimitedBarrelsProjectItemCountsAndFillLevels\":true," + "\"linkedLimitedBarrelMemorizedSlotsSynchronizeToOpenMenu\":true,"
				+ "\"linkedPrimaryDoubleChestRestoresAsPaired\":true,\"linkedPrimarySingleChestExpandsToDouble\":true,"
				+ "\"linkedSecondaryDoubleChestSplitsToSingleEndpoint\":true," + "\"linkedStorageMenuTransitionsRejectStaleActions\":true}";
	}

	private static void runLinkedShulkerStashRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos shulkerPos = player.blockPosition().offset(0, 0, 196);
			BlockPos barrelPos = shulkerPos.east(3);
			clearArea(level, shulkerPos);
			try {
				placeBlock(level, player, shulkerPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
				placeBlock(level, player, barrelPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				ShulkerBoxBlockEntity shulker = getShulkerBox(level, shulkerPos);
				BarrelBlockEntity barrel = getBarrel(level, barrelPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, shulker) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, barrel) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create a Shulker-primary linked storage group for stash regression");
				require(shulker.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
						"Linked Shulker stash regression did not retain a Shulker primary");
				ItemStack linkedShulkerCarrier = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
				shulker.copyLinkedStorageEndpointTo(linkedShulkerCarrier);
				ShulkerBoxItem shulkerItem = (ShulkerBoxItem) linkedShulkerCarrier.getItem();
				ItemStack diamonds = new ItemStack(Items.DIAMOND, 7);
				require(shulkerItem.stash(level.registryAccess(), linkedShulkerCarrier, diamonds, false).isEmpty()
						&& count(shulker.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
						&& count(barrel.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
						"Stashing into a linked Shulker carrier did not insert into canonical linked contents");
				ItemStack nestedShulker = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
				require(shulkerItem.stash(level.registryAccess(), linkedShulkerCarrier, nestedShulker, true).getCount() == 1
						&& shulkerItem.stash(level.registryAccess(), linkedShulkerCarrier, nestedShulker, false).getCount() == 1
						&& count(shulker.getStorageWrapper().getInventoryHandler(), ModBlocks.SHULKER_BOX_ITEM.get()) == 0,
						"Shulker-primary linked storage accepted a nested Shulker through stash");
			} finally {
				clearArea(level, shulkerPos);
			}
			return "";
		});
	}

	private static void runCanonicalStorageTypeInsertionRulesRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 20);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				ShulkerBoxBlockEntity shulkerPrimary = getShulkerBox(level, primaryPos);
				BarrelBlockEntity barrelSecondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, shulkerPrimary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, barrelSecondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked storage with a canonical shulker and physical barrel secondary");
				IItemHandler barrelCapability = requireCapability(level, secondaryPos, "physical barrel secondary");
				ItemStack rejected = barrelCapability.insertItem(0, new ItemStack(Items.SHULKER_BOX), false);
				require(rejected.is(Items.SHULKER_BOX) && rejected.getCount() == 1
						&& count(shulkerPrimary.getStorageWrapper().getInventoryHandler(), Items.SHULKER_BOX) == 0
						&& count(barrelCapability, Items.SHULKER_BOX) == 0, "Physical barrel secondary bypassed canonical shulker insertion rules");

				clearArea(level, primaryPos);
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
				BarrelBlockEntity barrelPrimary = getBarrel(level, primaryPos);
				ShulkerBoxBlockEntity shulkerSecondary = getShulkerBox(level, secondaryPos);
				linker = new ItemStack(ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, barrelPrimary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, shulkerSecondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked storage with a canonical barrel and physical shulker secondary");
				IItemHandler shulkerCapability = requireCapability(level, secondaryPos, "physical shulker secondary");
				ItemStack remainder = shulkerCapability.insertItem(0, new ItemStack(Items.SHULKER_BOX), false);
				require(remainder.isEmpty() && count(barrelPrimary.getStorageWrapper().getInventoryHandler(), Items.SHULKER_BOX) == 1
						&& count(shulkerCapability, Items.SHULKER_BOX) == 1,
						"Physical shulker secondary did not apply canonical barrel insertion rules exactly once");
				return "";
			} finally {
				clearArea(level, primaryPos);
			}
		});
	}

	private static void runLinkedUpgradeSwitchRefreshRegression() {
		LinkedUpgradeSwitchFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 40);
			BlockPos secondaryPos = primaryPos.east(3);
			int playerUpgradeSlot = player.getInventory().selected == 0 ? 1 : 0;
			ItemStack originalUpgradeSlot = player.getInventory().getItem(playerUpgradeSlot).copy();
			ItemStack originalMainHand = player.getMainHandItem().copy();
			float originalYRot = player.getYRot();
			float originalXRot = player.getXRot();
			Vec3 originalPosition = player.position();
			clearArea(level, primaryPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked storage for upgrade-switch refresh regression");
				player.getInventory().setItem(playerUpgradeSlot, new ItemStack(ModItems.ADVANCED_FILTER_UPGRADE.get()));
				return new LinkedUpgradeSwitchFixture(primaryPos, playerUpgradeSlot, originalUpgradeSlot, originalMainHand, originalYRot, originalXRot,
						originalPosition);
			} catch (RuntimeException e) {
				clearArea(level, primaryPos);
				throw e;
			}
		});

		try {
			waitForClientStorageBlock(fixture.primaryPos());
			AutomationRuntime.runOnServer(player -> {
				teleportPlayer(player, fixture.primaryPos().getCenter());
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()).add(0, .5, 0), Direction.UP, fixture.primaryPos(), false);
				require(player.gameMode.useItemOn(player, player.serverLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit).consumesAction(),
						"Could not open linked storage through its normal in-world interaction for upgrade-switch refresh regression");
				require(player.containerMenu instanceof StorageContainerMenu,
						"Linked storage did not open a storage menu for upgrade-switch refresh regression");
				return "";
			});
			waitForClientPlayerPosition(fixture.primaryPos().getCenter());
			waitForLinkedStorageMenu(27, "Barrel", LinkedStorageEndpointRole.PRIMARY);
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null || !(minecraft.player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Client linked storage menu was unavailable for upgrade-switch refresh regression");
				}
				Slot playerUpgradeSlot = menu.slots.stream().filter(slot -> slot.getItem().is(ModItems.ADVANCED_FILTER_UPGRADE.get())).findFirst()
						.orElseThrow(() -> new IllegalStateException("Client player inventory did not contain the Advanced Filter Upgrade"));
				minecraft.gameMode.handleInventoryMouseClick(menu.containerId, playerUpgradeSlot.index, 0, ClickType.PICKUP, minecraft.player);
				minecraft.gameMode.handleInventoryMouseClick(menu.containerId, menu.getFirstUpgradeSlot(), 0, ClickType.PICKUP, minecraft.player);
				return "";
			});
			waitForClientLinkedUpgradeSwitch(fixture.primaryPos());
		} finally {
			closeMenu();
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().setItem(fixture.playerUpgradeSlot(), fixture.originalUpgradeSlot());
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalMainHand());
				player.setYRot(fixture.originalYRot());
				player.setYHeadRot(fixture.originalYRot());
				player.setXRot(fixture.originalXRot());
				teleportPlayer(player, fixture.originalPosition());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return "";
			});
		}
	}

	private static void runLinkedLimitedBarrelMemorizedSlotMenuRegression() {
		LinkedLimitedBarrelMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 32);
			BlockPos secondaryPos = primaryPos.east(3);
			ItemStack originalMainHand = player.getMainHandItem().copy();
			float originalYRot = player.getYRot();
			float originalXRot = player.getXRot();
			Vec3 originalPosition = player.position();
			clearArea(level, primaryPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
				LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
				LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked limited barrels for memorized-slot menu regression");
				primary.toggleLock();
				return new LinkedLimitedBarrelMenuFixture(primaryPos, originalMainHand, originalYRot, originalXRot, originalPosition);
			} catch (RuntimeException e) {
				clearArea(level, primaryPos);
				throw e;
			}
		});

		try {
			waitForClientStorageBlock(fixture.primaryPos());
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				LimitedBarrelBlockEntity primary = getLimitedBarrel(level, fixture.primaryPos());
				BlockState state = level.getBlockState(fixture.primaryPos());
				LimitedBarrelBlock block = (LimitedBarrelBlock) state.getBlock();
				Direction facing = block.getFacing(state);
				int diamondCount = 23;
				ItemStack diamonds = new ItemStack(Items.DIAMOND, diamondCount);
				player.setItemInHand(InteractionHand.MAIN_HAND, diamonds);
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(.5)), facing,
						fixture.primaryPos(), false);
				require(player.gameMode.useItemOn(player, level, diamonds, InteractionHand.MAIN_HAND, hit).consumesAction(),
						"Could not insert an item into a locked linked limited-barrel empty slot through its in-world interaction");
				Vec3 originalPosition = player.position();
				float originalYRot = player.getYRot();
				float originalXRot = player.getXRot();
				try {
					teleportPlayer(player, Vec3.atCenterOf(fixture.primaryPos()).add(facing.getStepX() * 2, -1.62, facing.getStepZ() * 2));
					player.setYRot(facing.getOpposite().toYRot());
					player.setYHeadRot(facing.getOpposite().toYRot());
					player.setXRot(0);
					for (int removed = 0; removed < diamondCount; removed++) {
						require(block.tryToTakeItem(state, level, fixture.primaryPos(), player),
								"Could not remove an item from a locked linked limited barrel through its in-world interaction");
					}
				} finally {
					player.setYRot(originalYRot);
					player.setXRot(originalXRot);
					teleportPlayer(player, originalPosition);
				}
				require(primary.getStorageWrapper().getInventoryHandler().getStackInSlot(0).isEmpty(),
						"In-world interaction did not remove the linked limited barrel contents");
				require(primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotFilterStack(0, false)
						.filter(stack -> stack.is(Items.DIAMOND)).isPresent(), "Locked linked limited barrel did not memorize the inserted item");
				LinkedStorageEndpointData endpoint = requireEndpoint(primary, "linked limited barrel memorized-slot menu");
				CompoundTag canonicalContents = LinkedStorageGroupsSavedData.get(level).manager().resolveContents(endpoint.groupId())
						.orElseThrow(() -> new IllegalStateException("Linked limited barrel group contents disappeared")).getContents();
				require(canonicalContents.getCompound("settings").equals(primary.getStorageWrapper().getSettingsHandler().getNbt()),
						"Linked limited barrel canonical contents did not retain the memorized-slot settings");
				return "";
			});
			AutomationRuntime.runOnServer(player -> {
				teleportPlayer(player, fixture.primaryPos().getCenter());
				return "";
			});
			waitForClientPlayerPosition(fixture.primaryPos().getCenter());
			openLimitedBarrelMenu(fixture.primaryPos());
			waitForLinkedStorageMenu(1, "Limited Barrel I", LinkedStorageEndpointRole.PRIMARY);
			waitForClientLimitedBarrelMenu(fixture.primaryPos());
			waitForClientMemorizedStack(fixture.primaryPos(), Items.DIAMOND);
		} finally {
			closeMenu();
			AutomationRuntime.runOnServer(player -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalMainHand());
				player.setYRot(fixture.originalYRot());
				player.setYHeadRot(fixture.originalYRot());
				player.setXRot(fixture.originalXRot());
				teleportPlayer(player, fixture.originalPosition());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return "";
			});
		}
	}

	public static String setupLinkedLimitedBarrelReloadProjection() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
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

			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "limited reload primary");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "limited reload secondary");
			require(hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary),
					"Linked limited barrels did not initialize their render projection before reload");
			return "{\"ok\":true,\"groupId\":\"" + primaryEndpoint.groupId() + "\",\"primaryEndpointId\":\"" + primaryEndpoint.endpointId()
					+ "\",\"secondaryEndpointId\":\"" + secondaryEndpoint.endpointId() + "\"}";
		});
	}

	public static String linkedLimitedBarrelReloadProjectionStatus(UUID groupId) {
		LimitedBarrelReloadStatus status = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, LINKED_LIMITED_RELOAD_PRIMARY_POS);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, LINKED_LIMITED_RELOAD_SECONDARY_POS);
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "reloaded limited primary");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "reloaded limited secondary");
			require(primaryEndpoint.groupId().equals(groupId) && secondaryEndpoint.groupId().equals(groupId),
					"Reloaded limited barrels do not belong to the expected linked-storage group");
			require(hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary),
					"Reloaded linked limited barrels did not restore server render projection");
			return new LimitedBarrelReloadStatus(primaryEndpoint, secondaryEndpoint);
		});
		waitForClientLimitedBarrelReloadProjection(status);
		return "{\"ok\":true,\"clientDisplayItems\":true,\"clientCounts\":true,\"clientFillLevels\":true}";
	}

	private static void runComponentlessBarrelItemModelRegression() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			ItemStack componentlessBarrel = new ItemStack(ModBlocks.BARREL_ITEM.get());
			BakedModel expectedModel = renderBarrelItemModel(minecraft, componentlessBarrel);

			ItemStack tintedBarrel = new ItemStack(ModBlocks.BARREL_ITEM.get());
			StorageBlockItem tintedBarrelItem = (StorageBlockItem) tintedBarrel.getItem();
			tintedBarrelItem.setMainColor(tintedBarrel, 0xFF00FF);
			tintedBarrelItem.setAccentColor(tintedBarrel, 0x00FFFF);
			renderBarrelItemModel(minecraft, tintedBarrel);

			require(renderBarrelItemModel(minecraft, componentlessBarrel) == expectedModel,
					"A componentless barrel item inherited the previous barrel render state");
			return "";
		});
	}

	private static BakedModel renderBarrelItemModel(Minecraft minecraft, ItemStack stack) {
		BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0);
		model.getQuads(null, null, RandomSource.create());
		return model;
	}

	private static void runLinkedControllerEndpointJoinDoesNotDuplicateCanonicalContentsRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 76);
			BlockPos existingEndpointPos = controllerPos.east();
			BlockPos joiningEndpointPos = existingEndpointPos.east();
			clearArea(level, controllerPos);
			try {
				placeBlock(level, player, existingEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, joiningEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing controller for linked endpoint join regression"));
				BarrelBlockEntity existingEndpoint = getBarrel(level, existingEndpointPos);
				BarrelBlockEntity joiningEndpoint = getBarrel(level, joiningEndpointPos);
				int canonicalSlots = existingEndpoint.getStorageWrapper().getInventoryHandler().getSlots();
				require(controller.getSlots() == canonicalSlots * 2, "Controller did not register both unlinked barrels before linking");

				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, existingEndpoint) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create controller-connected linked-storage group");
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, joiningEndpoint) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not add a controller-connected barrel to its existing linked-storage group");
				LinkedStorageEndpointData existingData = requireEndpoint(existingEndpoint, "existing controller endpoint");
				require(existingData.groupId().equals(requireEndpoint(joiningEndpoint, "joining controller endpoint").groupId()),
						"Controller-connected endpoints did not join the same linked-storage group");
				existingEndpoint.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				existingEndpoint.getStorageWrapper().getInventoryHandler().saveInventory();

				ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
				require(controller.getStoragePositions().size() == 1 && controller.getSlots() == canonicalSlots && count(controller, Items.DIAMOND) == 7
						&& controller.getStackStorages(diamondKey).size() == 1,
						"Adding a controller-connected endpoint to an existing linked group duplicated canonical contents: positions="
								+ controller.getStoragePositions() + ", slots=" + controller.getSlots() + ", diamonds=" + count(controller, Items.DIAMOND)
								+ ", indexed=" + controller.getStackStorages(diamondKey));
				return "";
			} finally {
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runPlacedLinkedDoubleChestControllerRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 88);
			BlockPos sourceLeftPos = controllerPos.north(4);
			BlockPos sourceRightPos = sourceLeftPos.east();
			BlockPos placedChestPos = controllerPos.east();
			clearArea(level, controllerPos);
			try {
				placeChest(level, player, sourceLeftPos);
				placeChest(level, player, sourceRightPos);
				ChestBlockEntity sourceMainChest = getChest(level, sourceRightPos);
				require(sourceMainChest.isMainChest(), "Could not create the source linked double chest");
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, sourceMainChest.getBlockPos()).consumesAction(), "Could not create the linked double-chest group");
				LinkedStorageEndpointData endpoint = requireEndpoint(sourceMainChest, "source linked double chest");
				player.setGameMode(GameType.SURVIVAL);
				require(player.gameMode.destroyBlock(sourceLeftPos), "Player break did not remove the linked source double chest");
				player.setGameMode(originalGameMode);
				ItemStack linkedDoubleChestCarrier = level
						.getEntitiesOfClass(ItemEntity.class, new AABB(sourceLeftPos).inflate(2.0D),
								itemEntity -> itemEntity.getItem().is(ModBlocks.CHEST_ITEM.get())
										&& endpoint.equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
						.stream().findFirst().map(itemEntity -> itemEntity.getItem().copy())
						.orElseThrow(() -> new IllegalStateException("Missing linked double-chest carrier"));
				level.getEntitiesOfClass(ItemEntity.class, new AABB(sourceLeftPos).inflate(2.0D)).forEach(ItemEntity::discard);

				placeBlockAsPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()), 0);
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing controller for placed linked double chest"));
				placeChest(level, player, placedChestPos, linkedDoubleChestCarrier, 90);
				ChestBlockEntity placedMainChest = List
						.of(placedChestPos, placedChestPos.north(), placedChestPos.south(), placedChestPos.east(), placedChestPos.west()).stream()
						.map(level::getBlockEntity).filter(ChestBlockEntity.class::isInstance).map(ChestBlockEntity.class::cast)
						.filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing restored linked double-chest main half"));
				require(controller.getStoragePositions().size() == 1 && controller.getSlots() == 54,
						"Controller did not register the restored linked double chest");
				BlockPos upperChestPos = controllerPos.above();
				placeChest(level, player, upperChestPos);
				ChestBlockEntity upperChest = getChest(level, upperChestPos);
				require(useLinkerAsPlayer(player, linker, upperChest.getBlockPos()).consumesAction()
						&& endpoint.groupId().equals(requireEndpoint(upperChest, "upper linked chest").groupId()),
						"Could not add the upper chest to the placed double chest's linked group");
				placedMainChest.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				placedMainChest.getStorageWrapper().getInventoryHandler().saveInventory();

				require(controller.getStoragePositions().size() == 1 && controller.getSlots() == 54 && count(controller, Items.DIAMOND) == 7,
						"Placing a linked double chest next to a controller duplicated canonical contents after linking the upper chest: positions="
								+ controller.getStoragePositions() + ", slots=" + controller.getSlots() + ", diamonds=" + count(controller, Items.DIAMOND)
								+ ".");
				ChestBlockEntity placedOtherChest = getChest(level,
						placedMainChest.getBlockPos().relative(ChestBlock.getConnectedDirection(placedMainChest.getBlockState())));
				BlockPos ordinaryStoragePos = controllerPos.south();
				placeBlockAsPlayer(level, player, ordinaryStoragePos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
				BarrelBlockEntity ordinaryStorage = getBarrel(level, ordinaryStoragePos);
				controller.searchAndAddBoundables();
				require(ordinaryStorage.getControllerPos().filter(controllerPos::equals).isPresent(),
						"Controller did not connect the ordinary storage beside the linked double chest");
				controller.toggleLock();
				require(placedMainChest.isLocked() && placedOtherChest.isLocked() && ordinaryStorage.isLocked(),
						"One controller lock toggle did not lock both double-chest halves and the ordinary connected storage");
				controller.toggleLock();
				require(!placedMainChest.isLocked() && !placedOtherChest.isLocked() && !ordinaryStorage.isLocked(),
						"One controller unlock toggle did not unlock both double-chest halves and the ordinary connected storage");
				return "";
			} finally {
				player.setGameMode(originalGameMode);
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedStorageControllerRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 60);
			BlockPos secondaryPos = primaryPos.east();
			BlockPos controllerPos = primaryPos.west();
			clearArea(level, primaryPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked controller primary group");
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked controller secondary endpoint");
				int canonicalSlots = primary.getStorageWrapper().getInventoryHandler().getSlots();
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing linked storage controller"));
				require(controller.getStoragePositions().size() == 1 && controller.getSlots() == canonicalSlots,
						"Controller registered linked endpoints more than once");
				require(controller.insertItem(new ItemStack(Items.EMERALD, 3), false).isEmpty()
						&& count(primary.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3
						&& count(secondary.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3,
						"Controller did not route through the canonical linked inventory");
				require(controller.extractItem(new ItemStack(Items.EMERALD, 2), false).getCount() == 2
						&& count(primary.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 1,
						"Controller did not extract through the canonical linked inventory");
				ItemStackKey emeraldKey = ItemStackKey.of(new ItemStack(Items.EMERALD));
				require(controller.getHighlightStoragePositions(controller.getStackStorages(emeraldKey)).containsAll(Set.of(primaryPos, secondaryPos)),
						"Controller matching-stack highlights did not include every linked group member");
				require(controller.getHighlightStoragePositions(controller.getEmptyTargetSlotStorages(ItemStackKey.of(new ItemStack(Items.REDSTONE))))
						.containsAll(Set.of(primaryPos, secondaryPos)), "Controller empty-target highlights did not include every linked group member");
				require(controller.getStorageBlockPositions().containsAll(Set.of(primaryPos, secondaryPos)),
						"Controller link-mode outline did not include every linked group member");
				controller.toggleLock();
				require(primary.isLocked() && secondary.isLocked(), "Controller lock mode did not lock every linked group member");
				boolean initialLockVisibility = primary.shouldShowLock();
				require(initialLockVisibility == secondary.shouldShowLock(), "Linked group members did not start with the same lock-display state");
				controller.toggleLockVisibility();
				require(primary.shouldShowLock() == !initialLockVisibility && secondary.shouldShowLock() == !initialLockVisibility,
						"Controller lock-display mode did not update every linked group member");
				boolean initialTierVisibility = primary.shouldShowTier();
				require(initialTierVisibility == secondary.shouldShowTier(), "Linked group members did not start with the same tier-display state");
				controller.toggleTierVisiblity();
				require(primary.shouldShowTier() == !initialTierVisibility && secondary.shouldShowTier() == !initialTierVisibility,
						"Controller tier-display mode did not update every linked group member");
				boolean initialUpgradeVisibility = primary.shouldShowUpgrades();
				require(initialUpgradeVisibility == secondary.shouldShowUpgrades(), "Linked group members did not start with the same upgrades-display state");
				controller.toggleUpgradesVisiblity();
				require(primary.shouldShowUpgrades() == !initialUpgradeVisibility && secondary.shouldShowUpgrades() == !initialUpgradeVisibility,
						"Controller upgrades-display mode did not update every linked group member");
				controller.toggleLock();
				require(!primary.isLocked() && !secondary.isLocked(), "Controller unlock mode did not unlock every linked group member");

				primary.toggleLock();
				require(controller.insertItem(new ItemStack(Items.REDSTONE), false).isEmpty(),
						"Controller did not use an unlocked linked member after its initial source was locked");
				secondary.toggleLock();
				require(controller.insertItem(new ItemStack(Items.LAPIS_LAZULI), false).is(Items.LAPIS_LAZULI),
						"Controller accepted insertion when every linked member was locked");
				primary.toggleLock();
				require(controller.insertItem(new ItemStack(Items.LAPIS_LAZULI), false).isEmpty(),
						"Controller did not resume access when one linked member was unlocked");

				primary.toggleLock();
				secondary.toggleLock();
				level.destroyBlock(primaryPos, false, player);
				require(controller.getStoragePositions().isEmpty() && secondary.getControllerPos().isEmpty(),
						"Controller retained a linked group after its controller-adjacent endpoint was removed");
				CompoundTag controllerData = controller.getUpdateTag(level.registryAccess());
				level.removeBlockEntity(controllerPos);
				ControllerBlockEntity reloadedController = new ControllerBlockEntity(controllerPos, level.getBlockState(controllerPos));
				reloadedController.loadAdditional(controllerData, level.registryAccess());
				level.setBlockEntity(reloadedController);
				reloadedController.onLoad();
				require(reloadedController.getStoragePositions().isEmpty() && secondary.getControllerPos().isEmpty(),
						"Reloaded controller restored a linked group outside its physical topology");
				return "";
			} finally {
				clearArea(level, primaryPos);
			}
		});
	}

	private static void runLegacyControllerStorageKeysMigrationRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
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
				migratedController.loadAdditional(legacyData, level.registryAccess());
				level.setBlockEntity(migratedController);
				migratedController.onLoad();
				require(migratedController.getStoragePositions().equals(List.of(firstStoragePos, secondStoragePos))
						&& migratedController.getStackStorages(ItemStackKey.of(new ItemStack(Items.DIAMOND))).contains(secondStoragePos),
						"Absent storageKeys did not migrate every legacy storagePosition into rebuilt controller routing indexes");
				CompoundTag explicitEmptyKeysData = legacyData.copy();
				explicitEmptyKeysData.put("storageKeys", new ListTag());
				ControllerBlockEntity explicitEmptyKeysController = new ControllerBlockEntity(controllerPos, level.getBlockState(controllerPos));
				explicitEmptyKeysController.loadAdditional(explicitEmptyKeysData, level.registryAccess());
				require(explicitEmptyKeysController.getStoragePositions().isEmpty(), "Present empty storageKeys incorrectly migrated legacy storagePositions");
				return "";
			} finally {
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedControllerNonListenerRemovalIndexesRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 132);
			BlockPos primaryPos = controllerPos.east();
			BlockPos secondaryPos = primaryPos.east();
			clearArea(level, controllerPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked group for non-listener controller removal regression");
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing non-listener removal controller"));
				require(controller.getStoragePositions().equals(List.of(primaryPos)), "Controller did not use the linked primary as its listener source");

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

				UpgradeHandler upgrades = primary.getStorageWrapper().getUpgradeHandler();
				upgrades.setStackInSlot(0, new ItemStack(ModItems.ADVANCED_FILTER_UPGRADE.get()));
				FilterUpgradeWrapper filter = upgrades.getWrappersThatImplement(FilterUpgradeWrapper.class).stream().findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing linked controller filter upgrade wrapper"));
				filter.setDirection(net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction.INPUT);
				filter.getFilterLogic().setDepositFilterType(ContentsFilterType.ALLOW);
				filter.getFilterLogic().setPrimaryMatch(PrimaryMatch.ITEM);
				filter.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
				upgrades.saveInventory();
				primary.getStorageWrapper().refreshInventoryForInputOutput();
				ItemStackKey goldKey = ItemStackKey.of(new ItemStack(Items.GOLD_INGOT));
				require(controller.hasMatchingFilter(new ItemStack(Items.GOLD_INGOT)) && controller.getEmptyTargetSlotStorages(goldKey).contains(primaryPos),
						"Surviving linked listener did not update the controller filter or empty-slot indexes");
				return "";
			} finally {
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedControllerRemovalReconnectRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 156);
			BlockPos primaryPos = controllerPos.east();
			BlockPos secondaryPos = primaryPos.east();
			clearArea(level, controllerPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked group for controller removal regression");
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				require(primary.getControllerPos().filter(controllerPos::equals).isPresent()
						&& secondary.getControllerPos().filter(controllerPos::equals).isPresent(), "Controller did not connect every linked physical endpoint");

				level.destroyBlock(controllerPos, false, player);
				require(primary.getControllerPos().isEmpty() && secondary.getControllerPos().isEmpty(),
						"Removing controller left a linked physical endpoint with controller membership");

				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity reconnectedController = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing reconnected linked controller"));
				require(reconnectedController.getStoragePositions().equals(List.of(primaryPos))
						&& primary.getControllerPos().filter(controllerPos::equals).isPresent()
						&& secondary.getControllerPos().filter(controllerPos::equals).isPresent(),
						"Linked physical endpoints did not reconnect after controller removal");
				return "";
			} finally {
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedControllerTierUpgradeRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 180);
			BlockPos primaryPos = controllerPos.east();
			BlockPos secondaryPos = primaryPos.east();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			ItemStack originalMainHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
			clearArea(level, controllerPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked group for controller tier-upgrade regression");
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				player.setGameMode(GameType.SURVIVAL);
				ItemStack tierUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				player.setItemInHand(InteractionHand.MAIN_HAND, tierUpgrade);
				require(player.gameMode
						.useItemOn(player, level, tierUpgrade, InteractionHand.MAIN_HAND,
								new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false))
						.consumesAction() && tierUpgrade.isEmpty(), "Controller did not apply the linked primary tier upgrade");

				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing controller after linked tier upgrade"));
				BarrelBlockEntity upgradedPrimary = getBarrel(level, primaryPos);
				BarrelBlockEntity upgradedSecondary = getBarrel(level, secondaryPos);
				int expectedSlots = ModBlocks.DIAMOND_BARREL.get().getNumberOfInventorySlots();
				require(controller.getStoragePositions().equals(List.of(primaryPos)) && controller.getSlots() == expectedSlots
						&& upgradedPrimary.getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots
						&& upgradedSecondary.getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots,
						"Controller did not apply linked primary capacity exactly once");
				require(controller.insertItem(new ItemStack(Items.AMETHYST_SHARD), false).isEmpty()
						&& count(upgradedPrimary.getStorageWrapper().getInventoryHandler(), Items.AMETHYST_SHARD) == 1,
						"Controller routing did not use the upgraded linked canonical inventory");
				return "";
			} finally {
				player.setGameMode(originalGameMode);
				player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedControllerTierUpgradeRequiresPrimaryRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos secondaryControllerPos = player.blockPosition().offset(0, 0, 204);
			BlockPos secondaryPos = secondaryControllerPos.east();
			BlockPos primaryPos = secondaryControllerPos.south(5);
			BlockPos primaryControllerPos = primaryPos.west();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			ItemStack originalMainHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
			clearArea(level, secondaryControllerPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked group for controller primary-tier regression");
				placeBlock(level, player, secondaryControllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity secondaryController = level.getBlockEntity(secondaryControllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing secondary-only linked controller"));
				require(secondaryController.getStoragePositions().equals(List.of(secondaryPos)),
						"Secondary-only controller unexpectedly connected the remote linked primary");

				player.setGameMode(GameType.SURVIVAL);
				ItemStack rejectedUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				player.setItemInHand(InteractionHand.MAIN_HAND, rejectedUpgrade);
				player.gameMode.useItemOn(player, level, rejectedUpgrade, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(secondaryControllerPos), Direction.UP, secondaryControllerPos, false));
				require(rejectedUpgrade.getCount() == 1 && level.getBlockState(secondaryPos).is(ModBlocks.BARREL.get()),
						"Controller applied a tier upgrade while only a linked secondary endpoint was connected");

				level.destroyBlock(secondaryControllerPos, false, player);
				placeBlock(level, player, primaryControllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ItemStack acceptedUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				player.setItemInHand(InteractionHand.MAIN_HAND, acceptedUpgrade);
				require(player.gameMode
						.useItemOn(player, level, acceptedUpgrade, InteractionHand.MAIN_HAND,
								new BlockHitResult(Vec3.atCenterOf(primaryControllerPos), Direction.UP, primaryControllerPos, false))
						.consumesAction() && acceptedUpgrade.isEmpty() && level.getBlockState(primaryPos).is(ModBlocks.DIAMOND_BARREL.get()),
						"Controller did not apply the tier upgrade after the linked primary endpoint connected");
				return "";
			} finally {
				player.setGameMode(originalGameMode);
				player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
				clearArea(level, secondaryControllerPos);
			}
		});
	}

	private static void runLinkedControllerPaintbrushRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 228);
			BlockPos primaryPos = controllerPos.east();
			BlockPos secondaryPos = primaryPos.east();
			ItemStack originalFirstInventoryStack = player.getInventory().getItem(0).copy();
			clearArea(level, controllerPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked group for controller paintbrush regression");
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ItemStack paintbrush = new ItemStack(ModItems.PAINTBRUSH.get());
				PaintbrushItem.setMainColor(paintbrush, 0xFFFF0000);
				PaintbrushItem.ItemRequirements requirements = PaintbrushItem.getItemRequirements(paintbrush, player, level, controllerPos)
						.orElseThrow(() -> new IllegalStateException("Linked controller paintbrush requirements were missing"));
				int requiredRedDyes = requirements.itemsPresent().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum()
						+ requirements.itemsMissing().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum();
				require(requiredRedDyes == 2, "Controller paintbrush requirements did not include both linked physical endpoints");

				player.getInventory().setItem(0, new ItemStack(Items.RED_DYE, 2));
				require(paintbrush.getItem().onItemUseFirst(paintbrush,
						new UseOnContext(player, InteractionHand.MAIN_HAND,
								new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false))) == InteractionResult.SUCCESS
						&& primary.getStorageWrapper().getMainColor() == 0xFFFF0000 && secondary.getStorageWrapper().getMainColor() == 0xFFFF0000,
						"Controller paintbrush did not color every linked physical endpoint");
				return "";
			} finally {
				player.getInventory().setItem(0, originalFirstInventoryStack);
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedControllerBridgeReconnectRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 84);
			BlockPos primaryLinkedPos = controllerPos.east();
			BlockPos secondaryLinkedPos = primaryLinkedPos.east();
			BlockPos otherGroupLinkedPos = secondaryLinkedPos.east();
			BlockPos downstreamPos = otherGroupLinkedPos.east();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			clearArea(level, controllerPos);
			try {
				player.setGameMode(GameType.SURVIVAL);
				placeBlock(level, player, primaryLinkedPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryLinkedPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primaryLinkedBarrel = getBarrel(level, primaryLinkedPos);
				BarrelBlockEntity secondaryLinkedBarrel = getBarrel(level, secondaryLinkedPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primaryLinkedBarrel) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create the linked controller bridge group");
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondaryLinkedBarrel) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create the linked controller bridge secondary");
				LinkedStorageEndpointData endpoint = requireEndpoint(primaryLinkedBarrel, "linked controller bridge primary barrel");
				require(endpoint.groupId().equals(requireEndpoint(secondaryLinkedBarrel, "linked controller bridge secondary barrel").groupId()),
						"Linked controller bridge barrels did not join the same group");
				placeBlock(level, player, otherGroupLinkedPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity otherGroupLinkedBarrel = getBarrel(level, otherGroupLinkedPos);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
						otherGroupLinkedBarrel) == LinkedStorageService.LinkResult.SUCCESS, "Could not create the adjacent linked controller bridge group");
				require(!endpoint.groupId().equals(requireEndpoint(otherGroupLinkedBarrel, "adjacent linked controller bridge barrel").groupId()),
						"Adjacent linked controller bridge barrel joined the wrong group");
				placeBlock(level, player, downstreamPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing linked controller bridge"));
				require(controller.getStoragePositions().size() == 3 && primaryLinkedBarrel.getControllerPos().filter(controllerPos::equals).isPresent()
						&& secondaryLinkedBarrel.getControllerPos().filter(controllerPos::equals).isPresent()
						&& otherGroupLinkedBarrel.getControllerPos().filter(controllerPos::equals).isPresent()
						&& getBarrel(level, downstreamPos).getControllerPos().filter(controllerPos::equals).isPresent(),
						"Controller did not initially connect both linked bridge groups and downstream storage");

				level.destroyBlock(primaryLinkedPos, true, player);
				ItemStack endpointDrop = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryLinkedPos)).stream()
						.filter(itemEntity -> endpoint.equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst()
						.map(itemEntity -> itemEntity.getItem().copy()).orElseThrow(() -> new IllegalStateException("Missing linked controller bridge drop"));
				level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryLinkedPos)).forEach(ItemEntity::discard);
				require(getBarrel(level, downstreamPos).getControllerPos().isEmpty(),
						"Downstream storage remained connected after the controller-adjacent linked bridge was removed");

				placeBlock(level, player, primaryLinkedPos, endpointDrop);
				BarrelBlockEntity restoredPrimaryLinkedBarrel = getBarrel(level, primaryLinkedPos);
				boolean restoredEndpoint = endpoint.equals(requireEndpoint(restoredPrimaryLinkedBarrel, "restored linked controller bridge primary barrel"));
				boolean groupAPreserved = endpoint.groupId()
						.equals(requireEndpoint(getBarrel(level, secondaryLinkedPos), "restored linked controller bridge secondary barrel").groupId());
				boolean primaryConnected = restoredPrimaryLinkedBarrel.getControllerPos().filter(controllerPos::equals).isPresent();
				boolean secondaryConnected = getBarrel(level, secondaryLinkedPos).getControllerPos().filter(controllerPos::equals).isPresent();
				boolean otherGroupConnected = getBarrel(level, otherGroupLinkedPos).getControllerPos().filter(controllerPos::equals).isPresent();
				boolean downstreamConnected = getBarrel(level, downstreamPos).getControllerPos().filter(controllerPos::equals).isPresent();
				require(restoredEndpoint && groupAPreserved && controller.getStoragePositions().size() == 3 && primaryConnected && secondaryConnected
						&& otherGroupConnected && downstreamConnected,
						"Restored controller-adjacent linked bridge did not reconnect both linked groups and downstream storage: positions="
								+ controller.getStoragePositions() + ", restoredEndpoint=" + restoredEndpoint + ", groupAPreserved=" + groupAPreserved
								+ ", primaryConnected=" + primaryConnected + ", secondaryConnected=" + secondaryConnected + ", otherGroupConnected="
								+ otherGroupConnected + ", downstreamConnected=" + downstreamConnected);
				return "";
			} finally {
				player.setGameMode(originalGameMode);
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedControllerPartialGroupDisconnectRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 156);
			BlockPos firstSecondaryPos = controllerPos.east();
			BlockPos secondSecondaryPos = firstSecondaryPos.east();
			BlockPos thirdSecondaryPos = secondSecondaryPos.east();
			BlockPos fourthSecondaryPos = thirdSecondaryPos.east();
			BlockPos primaryPos = fourthSecondaryPos.east();
			BlockPos regularPos = primaryPos.east();
			clearArea(level, controllerPos);
			try {
				List<BlockPos> linkedPositions = List.of(firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos, primaryPos);
				linkedPositions.forEach(pos -> placeBlock(level, player, pos, new ItemStack(ModBlocks.BARREL_ITEM.get())));
				placeBlock(level, player, regularPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity firstSecondary = getBarrel(level, firstSecondaryPos);
				BarrelBlockEntity secondSecondary = getBarrel(level, secondSecondaryPos);
				BarrelBlockEntity thirdSecondary = getBarrel(level, thirdSecondaryPos);
				BarrelBlockEntity fourthSecondary = getBarrel(level, fourthSecondaryPos);
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity regular = getBarrel(level, regularPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 4);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked group for partial controller disconnect regression");
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, firstSecondary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondSecondary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, thirdSecondary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, fourthSecondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked secondary endpoints for partial controller disconnect regression");
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing partial linked-group disconnect controller"));
				require(controller.getStoragePositions().size() == 2,
						"Controller did not register the linked group and downstream barrel: positions=" + controller.getStoragePositions());
				require(controller.getStorageBlockPositions().containsAll(linkedPositions) && controller.getStorageBlockPositions().contains(regularPos),
						"Controller did not register every physical linked endpoint and downstream barrel: positions=" + controller.getStorageBlockPositions());
				require(List.of(firstSecondary, secondSecondary, thirdSecondary, fourthSecondary, primary).stream()
						.allMatch(storage -> storage.getControllerPos().filter(controllerPos::equals).isPresent())
						&& regular.getControllerPos().filter(controllerPos::equals).isPresent(),
						"Controller did not assign controller membership to every initial endpoint");

				List.of(firstSecondary, secondSecondary, thirdSecondary, fourthSecondary, primary, regular).forEach(BarrelBlockEntity::toggleLock);
				level.destroyBlock(secondSecondaryPos, false, player);

				require(firstSecondary.getControllerPos().filter(controllerPos::equals).isPresent() && thirdSecondary.getControllerPos().isEmpty()
						&& fourthSecondary.getControllerPos().isEmpty() && primary.getControllerPos().isEmpty() && regular.getControllerPos().isEmpty(),
						"Breaking the second linked secondary did not detach the remote branch from the controller");
				require(controller.getStoragePositions().equals(List.of(firstSecondaryPos))
						&& controller.getStorageBlockPositions().equals(Set.of(firstSecondaryPos)),
						"Controller retained disconnected linked-group members after the physical chain was broken: positions="
								+ controller.getStorageBlockPositions());

				thirdSecondary.toggleLock();
				ItemStack remainder = controller.insertItem(new ItemStack(Items.CHERRY_PLANKS), false);
				require(remainder.is(Items.CHERRY_PLANKS) && count(firstSecondary.getStorageWrapper().getInventoryHandler(), Items.CHERRY_PLANKS) == 0,
						"A disconnected linked endpoint lock still affected controller insertion");

				placeBlock(level, player, secondSecondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity bridge = getBarrel(level, secondSecondaryPos);
				Set<BlockPos> reconnectedPositions = Set.of(firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos, primaryPos,
						regularPos);
				require(controller.getStoragePositions().size() == 3
						&& controller.getStoragePositions().containsAll(List.of(firstSecondaryPos, secondSecondaryPos, regularPos))
						&& controller.getStorageBlockPositions().equals(reconnectedPositions),
						"Controller did not elect one linked-group anchor or restore every physical member after bridging the chain: logical="
								+ controller.getStoragePositions() + ", physical=" + controller.getStorageBlockPositions());
				require(List.of(firstSecondary, thirdSecondary, fourthSecondary, primary, bridge, regular).stream()
						.allMatch(storage -> storage.getControllerPos().filter(controllerPos::equals).isPresent()),
						"Bridging the chain did not restore controller membership to every endpoint");

				List<BarrelBlockEntity> reconnectedStorages = List.of(firstSecondary, thirdSecondary, fourthSecondary, primary, bridge, regular);
				controller.toggleLock();
				require(reconnectedStorages.stream().allMatch(BarrelBlockEntity::isLocked),
						"Controller lock action did not affect every reconnected storage endpoint");
				controller.toggleLockVisibility();
				require(reconnectedStorages.stream().noneMatch(BarrelBlockEntity::shouldShowLock),
						"Controller lock-visibility action did not affect every reconnected storage endpoint");
				return "";
			} finally {
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLockedLinkedControllerRoutingRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 108);
			BlockPos firstSecondaryPos = controllerPos.east();
			BlockPos secondSecondaryPos = firstSecondaryPos.east();
			BlockPos thirdSecondaryPos = secondSecondaryPos.east();
			BlockPos fourthSecondaryPos = thirdSecondaryPos.east();
			BlockPos primaryPos = fourthSecondaryPos.east();
			BlockPos regularPos = primaryPos.east();
			clearArea(level, controllerPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 4);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create the locked linked-controller routing group");
				LinkedStorageEndpointData endpoint = requireEndpoint(primary, "locked linked-controller routing primary");

				BlockPos[] secondaryPositions = {firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos};
				for (BlockPos secondaryPos : secondaryPositions) {
					placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
					BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
					require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS
							&& endpoint.groupId().equals(requireEndpoint(secondary, "locked linked-controller routing secondary").groupId()),
							"Linked controller routing secondary did not join the primary group");
				}
				placeBlock(level, player, regularPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing locked linked-controller routing controller"));
				BarrelBlockEntity firstSecondary = getBarrel(level, firstSecondaryPos);
				BarrelBlockEntity regular = getBarrel(level, regularPos);
				require(controller.getStoragePositions().size() == 2 && controller.getStorageBlockPositions()
						.containsAll(Set.of(firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos, primaryPos, regularPos)),
						"Controller did not connect the locked linked group and regular fallback storage");

				controller.toggleLock();
				require(primary.isLocked() && firstSecondary.isLocked() && getBarrel(level, secondSecondaryPos).isLocked()
						&& getBarrel(level, thirdSecondaryPos).isLocked() && getBarrel(level, fourthSecondaryPos).isLocked() && regular.isLocked(),
						"Controller did not lock every linked routing member and regular fallback storage");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				ItemStackKey oakPlankKey = ItemStackKey.of(new ItemStack(Items.OAK_PLANKS));
				require(controller.getStackStorages(oakPlankKey).size() == 1, "Controller did not index the matching stack for a locked linked group");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, ItemStack.EMPTY);
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getStackStorages(oakPlankKey).isEmpty(), "Controller retained a removed canonical stack in its linked-group index");
				ItemStackKey acaciaPlankKey = ItemStackKey.of(new ItemStack(Items.ACACIA_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.ACACIA_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getStackStorages(acaciaPlankKey).size() == 1,
						"Controller did not index a canonical stack added after the linked group connected");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getStackStorages(oakPlankKey).size() == 1 && controller.getStackStorages(acaciaPlankKey).isEmpty(),
						"Controller did not replace a changed canonical linked-group stack in its index");
				require(controller.insertItem(new ItemStack(Items.OAK_PLANKS), false).isEmpty()
						&& count(primary.getStorageWrapper().getInventoryHandler(), Items.OAK_PLANKS) == 2,
						"Controller did not insert a matching stack into a fully locked linked group");
				require(controller.insertItem(new ItemStack(Items.BIRCH_PLANKS), false).is(Items.BIRCH_PLANKS),
						"Controller inserted a new stack while every linked member and fallback storage was locked");

				firstSecondary.toggleLock();
				ItemStackKey birchPlankKey = ItemStackKey.of(new ItemStack(Items.BIRCH_PLANKS));
				List<BlockPos> birchEmptyTargets = controller.getEmptyTargetSlotStorages(birchPlankKey);
				ItemStack simulatedBirchRemainder = controller.insertItem(new ItemStack(Items.BIRCH_PLANKS), true);
				Set<BlockPos> linkedGroupPositions = Set.of(firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos, primaryPos);
				require(birchEmptyTargets.containsAll(linkedGroupPositions) && birchEmptyTargets.size() == linkedGroupPositions.size()
						&& simulatedBirchRemainder.isEmpty(),
						"Controller did not restore the linked group's empty-slot target after a member unlocked: targets=" + birchEmptyTargets
								+ ", simulatedRemainder=" + simulatedBirchRemainder);
				int lastSlot = primary.getStorageWrapper().getInventoryHandler().getSlots() - 1;
				for (int slot = 0; slot <= lastSlot; slot++) {
					primary.getStorageWrapper().getInventoryHandler().setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS));
				}
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getEmptyTargetSlotStorages(birchPlankKey).stream().noneMatch(linkedGroupPositions::contains),
						"Controller retained linked empty-slot targets after canonical inventory became full");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(lastSlot, ItemStack.EMPTY);
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getEmptyTargetSlotStorages(birchPlankKey).containsAll(linkedGroupPositions),
						"Controller did not restore linked empty-slot targets when canonical inventory gained an empty slot");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(lastSlot, new ItemStack(Items.OAK_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				MemorySettingsCategory memory = primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				memory.selectSlots(0, lastSlot + 1);
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(lastSlot, ItemStack.EMPTY);
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getEmptyTargetSlotStorages(birchPlankKey).stream().noneMatch(linkedGroupPositions::contains),
						"Controller treated a memorized linked slot as an unrestricted empty target");
				memory.unselectSlot(lastSlot);
				require(controller.getEmptyTargetSlotStorages(birchPlankKey).containsAll(linkedGroupPositions),
						"Controller did not restore linked empty-slot targets when canonical memory was removed");
				memory.unselectAllSlots();
				for (int slot = 0; slot <= lastSlot; slot++) {
					primary.getStorageWrapper().getInventoryHandler().setStackInSlot(slot, ItemStack.EMPTY);
				}
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.insertItem(new ItemStack(Items.BIRCH_PLANKS), false).isEmpty()
						&& count(primary.getStorageWrapper().getInventoryHandler(), Items.BIRCH_PLANKS) == 1,
						"Controller did not use an unlocked linked secondary for unrestricted insertion");
				firstSecondary.toggleLock();
				primary.toggleLock();
				require(controller.insertItem(new ItemStack(Items.SPRUCE_PLANKS), false).isEmpty()
						&& count(primary.getStorageWrapper().getInventoryHandler(), Items.SPRUCE_PLANKS) == 1,
						"Controller did not use an unlocked linked primary for unrestricted insertion");

				primary.toggleLock();
				regular.toggleLock();
				require(controller.insertItem(new ItemStack(Items.DARK_OAK_PLANKS), false).isEmpty()
						&& count(regular.getStorageWrapper().getInventoryHandler(), Items.DARK_OAK_PLANKS) == 1,
						"Controller did not use the unlocked regular fallback for a new stack");
				require(controller.insertItem(new ItemStack(Items.OAK_PLANKS), false).isEmpty()
						&& count(primary.getStorageWrapper().getInventoryHandler(), Items.OAK_PLANKS) == 2
						&& count(regular.getStorageWrapper().getInventoryHandler(), Items.OAK_PLANKS) == 0,
						"Controller did not prioritize the matching locked linked group over an unlocked regular fallback");
				return "";
			} finally {
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedControllerClientOutlineRegression() {
		LinkedControllerOutlineFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedControllerOutlineFixture);
		try {
			waitForClientStorageBlock(fixture.primaryPos());
			waitForClientStorageBlock(fixture.secondaryPos());
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
			while (System.nanoTime() < deadline) {
				if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null
						&& Minecraft.getInstance().level.getBlockEntity(fixture.controllerPos()) instanceof ControllerBlockEntity controller
						&& controller.getStorageBlockPositions().containsAll(Set.of(fixture.primaryPos(), fixture.secondaryPos()))
						&& !controller.getStorageBlockEdges().isEmpty())) {
					return;
				}
				sleep(50);
			}
			String clientState = AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().level == null
						|| !(Minecraft.getInstance().level.getBlockEntity(fixture.controllerPos()) instanceof ControllerBlockEntity controller)) {
					return "controller=missing";
				}
				return "logical=" + controller.getStoragePositions() + ", physical=" + controller.getStorageBlockPositions() + ", controllerLinked="
						+ controller.getLinkedBlocks() + ", edges=" + controller.getStorageBlockEdges().size();
			});
			throw new IllegalStateException("Client controller outline did not include every linked group member: " + clientState);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.serverLevel(), fixture.controllerPos());
				return "";
			});
		}
	}

	private static LinkedControllerOutlineFixture setupLinkedControllerOutlineFixture(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 36);
		BlockPos primaryPos = controllerPos.east();
		BlockPos secondaryPos = primaryPos.east();
		clearArea(level, controllerPos);
		placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		BarrelBlockEntity primary = getBarrel(level, primaryPos);
		BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not create the client outline linked-storage group");
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not add the client outline linked-storage member");
		placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
		require(level.getBlockEntity(controllerPos) instanceof ControllerBlockEntity, "Could not place the client outline controller");
		ControllerBlockEntity controller = (ControllerBlockEntity) level.getBlockEntity(controllerPos);
		require(controller.getUpdateTag(level.registryAccess()).getList("storageMemberKeys", Tag.TAG_COMPOUND).size() == 2,
				"Controller update data did not include every linked group member");
		return new LinkedControllerOutlineFixture(controllerPos, primaryPos, secondaryPos);
	}

	private static void runCreativeEndpointPlacementRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos sourcePos = player.blockPosition().offset(0, 0, 60);
			BlockPos creativePos = sourcePos.east(3);
			ItemStack originalMainHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			clearArea(level, sourcePos);
			try {
				player.setGameMode(GameType.SURVIVAL);
				placeBlock(level, player, sourcePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity source = getBarrel(level, sourcePos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, source) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create the identity-ownership linked-storage group");
				LinkedStorageEndpointData sourceEndpoint = requireEndpoint(source, "identity-ownership source barrel");
				level.destroyBlock(sourcePos, true, player);
				ItemStack endpointDrop = level.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos)).stream()
						.filter(itemEntity -> sourceEndpoint.equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst()
						.map(itemEntity -> itemEntity.getItem().copy()).orElseThrow(() -> new IllegalStateException("Missing linked endpoint drop"));
				level.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos)).forEach(ItemEntity::discard);

				player.setGameMode(GameType.CREATIVE);
				placeBlock(level, player, creativePos, endpointDrop);
				LinkedStorageEndpointData creativeEndpoint = requireEndpoint(getBarrel(level, creativePos), "creative endpoint placement");
				require(creativeEndpoint.groupId().equals(sourceEndpoint.groupId()) && !creativeEndpoint.endpointId().equals(sourceEndpoint.endpointId()),
						"Creative placement did not create a fresh secondary endpoint");
				require(sourceEndpoint.equals(endpointDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
						"Creative placement mutated the held endpoint stack");
				return "";
			} finally {
				player.setGameMode(originalGameMode);
				player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
				clearArea(level, sourcePos);
				clearArea(level, creativePos);
			}
		});
	}

	private static void runDroppedItemPickupRegression() {
		DroppedItemPickupFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupDroppedItemPickupFixture);
		try {
			waitForDroppedItemToRemainOnSecondary(fixture);
			waitForDroppedItemToBePickedUpByOrdinaryStorage(fixture);
			AutomationRuntime.runOnServer(player -> {
				ItemEntity droppedItem = requireDroppedItem(player.serverLevel(), fixture);
				droppedItem.setPos(fixture.primaryPos().getCenter().add(0, -0.25D, 0));
				droppedItem.setDeltaMovement(Vec3.ZERO);
				droppedItem.setNoGravity(true);
				droppedItem.setPickUpDelay(0);
				return "";
			});
			waitForDroppedItemToBePickedUpByPrimary(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				clearMagnetExclusionArea(player.serverLevel(), fixture.primaryPos());
				clearMagnetExclusionArea(player.serverLevel(), fixture.secondaryPos());
				clearMagnetExclusionArea(player.serverLevel(), fixture.ordinaryPos());
				return "";
			});
		}
	}

	private static void runLinkedStorageStackTooltipRegression() {
		LinkedStorageStackTooltipFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 36);
			int inventorySlot = 8;
			ItemStack originalStack = player.getInventory().getItem(inventorySlot).copy();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			clearArea(level, primaryPos);
			try {
				player.setGameMode(GameType.SURVIVAL);
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create the linked storage tooltip group");
				LinkedStorageEndpointData endpoint = requireEndpoint(primary, "linked storage tooltip primary barrel");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				configureMagnet(primary);
				level.destroyBlock(primaryPos, true, player);
				ItemStack endpointStack = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos)).stream()
						.filter(itemEntity -> endpoint.equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst()
						.map(itemEntity -> itemEntity.getItem().copy())
						.orElseThrow(() -> new IllegalStateException("Missing dropped linked storage tooltip endpoint stack"));
				level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos)).forEach(ItemEntity::discard);
				player.getInventory().setItem(inventorySlot, endpointStack);
				player.inventoryMenu.broadcastChanges();
				return new LinkedStorageStackTooltipFixture(primaryPos, endpoint.groupId(), inventorySlot, originalStack, originalGameMode);
			} catch (RuntimeException e) {
				player.setGameMode(originalGameMode);
				player.getInventory().setItem(inventorySlot, originalStack);
				player.inventoryMenu.broadcastChanges();
				clearArea(level, primaryPos);
				throw e;
			}
		});
		try {
			waitForClientLinkedStorageStack(fixture);
			AutomationRuntime.runOnClient(() -> {
				ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot());
				StackStorageWrapper wrapper = StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(), stack);
				require(wrapper.getContentsUuid().filter(fixture.groupId()::equals).isPresent(),
						"Linked storage tooltip stack did not use its group UUID for contents lookup");
				PacketDistributor.sendToServer(new RequestLinkedStorageContentsPayload(fixture.groupId(), -1L));
				return "";
			});
			waitForClientLinkedStorageTooltipContents(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().setItem(fixture.inventorySlot(), fixture.originalStack());
				player.inventoryMenu.broadcastChanges();
				player.setGameMode(fixture.originalGameMode());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return "";
			});
		}
	}

	public static String setupDroppedItemPickupInspection() {
		DroppedItemPickupFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupDroppedItemPickupFixture);
		return "{\"ok\":true,\"primary\":\"" + fixture.primaryPos() + "\",\"secondary\":\"" + fixture.secondaryPos() + "\"}";
	}

	private static void runEndpointUnloadReloadRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 84);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create the endpoint unload/reload linked-storage group");
				LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "endpoint unload/reload primary barrel");
				LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "endpoint unload/reload secondary barrel");
				var persistedEndpointData = new CompoundTag();
				secondary.saveAdditional(persistedEndpointData, level.registryAccess());
				secondary.onChunkUnloaded();
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.LAPIS_LAZULI, 5));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				level.removeBlockEntity(secondaryPos);
				BarrelBlockEntity reloadedSecondary = new BarrelBlockEntity(secondaryPos, level.getBlockState(secondaryPos));
				reloadedSecondary.loadAdditional(persistedEndpointData, level.registryAccess());
				level.setBlockEntity(reloadedSecondary);
				reloadedSecondary.onLoad();
				require(secondaryEndpoint.equals(requireEndpoint(reloadedSecondary, "reloaded secondary barrel"))
						&& count(reloadedSecondary.getStorageWrapper().getInventoryHandler(), Items.LAPIS_LAZULI) == 5,
						"Reloaded linked endpoint did not reclaim its identity and canonical contents");
				CompoundTag persistedPrimaryData = new CompoundTag();
				primary.saveAdditional(persistedPrimaryData, level.registryAccess());
				primary.onChunkUnloaded();
				level.removeBlockEntity(primaryPos);
				BarrelBlockEntity reloadedPrimary = new BarrelBlockEntity(primaryPos, level.getBlockState(primaryPos));
				reloadedPrimary.loadAdditional(persistedPrimaryData, level.registryAccess());
				level.setBlockEntity(reloadedPrimary);
				reloadedPrimary.onLoad();
				reloadedSecondary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.AMETHYST_SHARD, 4));
				reloadedSecondary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(primaryEndpoint.equals(requireEndpoint(reloadedPrimary, "reloaded primary barrel"))
						&& count(reloadedPrimary.getStorageWrapper().getInventoryHandler(), Items.LAPIS_LAZULI) == 5
						&& count(reloadedPrimary.getStorageWrapper().getInventoryHandler(), Items.AMETHYST_SHARD) == 4,
						"Reloaded primary endpoint did not reclaim its identity and canonical contents");
				return "";
			} finally {
				clearArea(level, primaryPos);
			}
		});
	}

	private static DroppedItemPickupFixture setupDroppedItemPickupFixture(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 12);
		// Keep the secondary outside every allowed Magnet Upgrade range so this checks only its physical pickup path.
		BlockPos secondaryPos = primaryPos.east(MAX_MAGNET_RANGE + 1);
		BlockPos ordinaryPos = secondaryPos.east(MAX_MAGNET_RANGE + 1);

		clearMagnetExclusionArea(level, primaryPos);
		clearMagnetExclusionArea(level, secondaryPos);
		clearMagnetExclusionArea(level, ordinaryPos);
		// Keep the player outside the pickup path; the test item is created directly at the secondary barrel.
		teleportPlayer(player, primaryPos.getCenter().add(0, 0.0D, -10.0D));
		placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeBlock(level, player, ordinaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		BarrelBlockEntity primary = getBarrel(level, primaryPos);
		BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
		BarrelBlockEntity ordinary = getBarrel(level, ordinaryPos);
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not create the dropped-item pickup linked-storage group");
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not add the dropped-item pickup secondary barrel");
		configureMagnet(primary, Items.ENDER_PEARL);
		configureMagnet(ordinary, Items.ENDER_PEARL);
		require(primary.getBlockState().getValue(StorageBlockBase.TICKING), "Primary linked barrel did not become tickable after adding its Magnet upgrade");
		require(secondary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.SECONDARY).isPresent()
				&& !secondary.getBlockState().getValue(StorageBlockBase.TICKING),
				"Secondary linked barrel did not retain its non-ticking secondary role after adding a Magnet upgrade");

		Vec3 dropPosition = secondaryPos.getCenter().add(0, -0.25D, 0);
		ItemEntity droppedItem = new ItemEntity(level, dropPosition.x, dropPosition.y, dropPosition.z, new ItemStack(Items.ENDER_PEARL, 2));
		droppedItem.setDeltaMovement(Vec3.ZERO);
		droppedItem.setNoGravity(true);
		droppedItem.setDefaultPickUpDelay();
		require(level.addFreshEntity(droppedItem), "Could not add the dropped pickup regression item to the world");
		Vec3 ordinaryDropPosition = ordinaryPos.getCenter().add(0, -0.25D, 0);
		ItemEntity ordinaryDroppedItem = new ItemEntity(level, ordinaryDropPosition.x, ordinaryDropPosition.y, ordinaryDropPosition.z,
				new ItemStack(Items.ENDER_PEARL, 2));
		ordinaryDroppedItem.setDeltaMovement(Vec3.ZERO);
		ordinaryDroppedItem.setNoGravity(true);
		ordinaryDroppedItem.setDefaultPickUpDelay();
		require(level.addFreshEntity(ordinaryDroppedItem), "Could not add the ordinary dropped pickup control item to the world");

		return new DroppedItemPickupFixture(primaryPos, secondaryPos, ordinaryPos, Items.ENDER_PEARL, level.getGameTime() + 20L);
	}

	private static void waitForDroppedItemToRemainOnSecondary(DroppedItemPickupFixture fixture) {
		waitForServerCondition("dropped item to remain on linked secondary barrel", player -> {
			ServerLevel level = player.serverLevel();
			return level.getGameTime() >= fixture.verifyAfterGameTime() && requireDroppedItem(level, fixture).getItem().is(fixture.item())
					&& requireDroppedItem(level, fixture).getItem().getCount() == 2;
		});
	}

	private static void waitForDroppedItemToBePickedUpByPrimary(DroppedItemPickupFixture fixture) {
		waitForServerCondition("dropped item to be picked up by linked primary barrel", player -> {
			return findDroppedItem(player.serverLevel(), fixture, fixture.primaryPos()).isEmpty()
					&& count(getBarrel(player.serverLevel(), fixture.primaryPos()).getStorageWrapper().getInventoryHandler(), fixture.item()) == 2;
		});
	}

	private static void waitForDroppedItemToBePickedUpByOrdinaryStorage(DroppedItemPickupFixture fixture) {
		waitForServerCondition("dropped item to be picked up by ordinary unlinked barrel",
				player -> findDroppedItem(player.serverLevel(), fixture, fixture.ordinaryPos()).isEmpty()
						&& count(getBarrel(player.serverLevel(), fixture.ordinaryPos()).getStorageWrapper().getInventoryHandler(), fixture.item()) == 2);
	}

	private static void runLinkedStorageMenuRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedStorageMenuFixture);
		try {
			AutomationRuntime.runOnServer(player -> {
				teleportPlayer(player, fixture.secondaryPos().getCenter());
				return "";
			});
			waitForClientPlayerPosition(fixture.secondaryPos().getCenter());
			waitForClientStorageBlock(fixture.primaryPos());
			waitForClientStorageBlock(fixture.secondaryPos());
			openLinkedStorageMenu(fixture.secondaryPos());
			assertLinkedStorageMenuCanonicalSnapshot(fixture, fixture.secondaryPos());
			waitForLinkedStorageMenu(fixture.expectedSlots(), fixture.groupName(), LinkedStorageEndpointRole.SECONDARY);
			waitForLinkedStorageMenuCanonicalSnapshot(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			closeMenu();
			waitForClosedStorageMenu();
			openLinkedStorageMenu(fixture.primaryPos());
			assertLinkedStorageMenuCanonicalSnapshot(fixture, fixture.primaryPos());
			waitForLinkedStorageMenu(fixture.expectedSlots(), fixture.groupName(), LinkedStorageEndpointRole.PRIMARY);
			waitForLinkedStorageMenuCanonicalSnapshot(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalMainHand());
				player.setYRot(fixture.originalYRot());
				player.setXRot(fixture.originalXRot());
				teleportPlayer(player, fixture.originalPosition());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return "";
			});
		}
	}

	private static void runLinkedStorageMenuTransitionRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedStorageMenuFixture);
		try {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return "";
			});
			AutomationRuntime.runOnClient(() -> {
				PacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.secondaryPos()));
				return "";
			});
			long noMenuVerificationTime = AutomationRuntime.runOnServer(player -> player.serverLevel().getGameTime() + 2L);
			waitForServerCondition("no-menu linked storage inventory payload rejection",
					player -> player.serverLevel().getGameTime() >= noMenuVerificationTime && player.containerMenu == player.inventoryMenu);
			AutomationRuntime.runOnServer(player -> {
				teleportPlayer(player, fixture.secondaryPos().getCenter());
				return "";
			});
			waitForClientPlayerPosition(fixture.secondaryPos().getCenter());
			waitForClientStorageBlock(fixture.secondaryPos());
			openLinkedStorageMenu(fixture.secondaryPos());
			waitForLinkedStorageMenu(fixture.expectedSlots(), fixture.groupName(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnClient(() -> {
				PacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.secondaryPos()));
				return "";
			});
			long wrongMenuVerificationTime = AutomationRuntime.runOnServer(player -> player.serverLevel().getGameTime() + 2L);
			waitForServerCondition("wrong-menu linked storage inventory payload rejection",
					player -> player.serverLevel().getGameTime() >= wrongMenuVerificationTime && player.containerMenu instanceof StorageContainerMenu);
			AutomationRuntime.runOnServer(player -> {
				if (!(player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Linked storage inventory menu did not open before stale action test");
				}
				var staleAction = new CompoundTag();
				staleAction.putString("action", "openSettings");
				staleAction.putInt("sourceContainerId", menu.containerId + 1);
				menu.handlePacket(staleAction);
				require(player.containerMenu == menu, "Stale linked storage settings action replaced the active menu");
				return "";
			});
			AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Client linked storage inventory menu is missing before settings transition");
				}
				menu.openSettings();
				return "";
			});
			waitForLinkedStorageSettingsMenu(fixture.secondaryPos(), fixture.expectedSlots(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnClient(() -> {
				PacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.primaryPos()));
				return "";
			});
			long stalePayloadVerificationTime = AutomationRuntime.runOnServer(player -> player.serverLevel().getGameTime() + 2L);
			waitForServerCondition("mismatched linked storage inventory payload rejection",
					player -> player.serverLevel().getGameTime() >= stalePayloadVerificationTime
							&& player.containerMenu instanceof StorageSettingsContainerMenu settingsMenu
							&& settingsMenu.getBlockPosition().equals(fixture.secondaryPos()));
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				teleportPlayer(player, fixture.secondaryPos().getCenter());
				return "";
			});
			waitForClientPlayerPosition(fixture.secondaryPos().getCenter());
			waitForClosedStorageMenu();
			openLinkedStorageMenu(fixture.secondaryPos());
			waitForLinkedStorageMenu(fixture.expectedSlots(), fixture.groupName(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Client linked storage inventory menu is missing before valid settings transition");
				}
				menu.openSettings();
				return "";
			});
			waitForLinkedStorageSettingsMenu(fixture.secondaryPos(), fixture.expectedSlots(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnClient(() -> {
				PacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.secondaryPos()));
				return "";
			});
			waitForLinkedStorageMenu(fixture.expectedSlots(), fixture.groupName(), LinkedStorageEndpointRole.SECONDARY);
			closeMenu();
			waitForServerCondition("linked storage opener cleanup after settings transition",
					player -> !getBarrel(player.serverLevel(), fixture.secondaryPos()).isOpen());
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalMainHand());
				player.setYRot(fixture.originalYRot());
				player.setXRot(fixture.originalXRot());
				teleportPlayer(player, fixture.originalPosition());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return "";
			});
		}
	}

	private static void runDroppedPrimaryRenameRegression() {
		DroppedPrimaryRenameFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupDroppedPrimaryRenameFixture);
		try {
			AutomationRuntime.runOnServer(player -> {
				player.setGameMode(GameType.SURVIVAL);
				teleportPlayer(player, fixture.secondaryPos().getCenter());
				return "";
			});
			waitForClientPlayerPosition(fixture.secondaryPos().getCenter());
			waitForServerCondition("renamed linked secondary barrel to leave the group name unchanged",
					player -> player.serverLevel().getGameTime() >= fixture.secondaryVerificationGameTime()
							&& getLinkedStorageGroupName(player.serverLevel(), fixture.endpoint()).equals(fixture.originalGroupName())
							&& player.getItemInHand(InteractionHand.MAIN_HAND).getHoverName().getString().equals(fixture.secondaryName()));
			AutomationRuntime.runOnServer(player -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.renamedPrimary());
				return "";
			});
			waitForServerCondition("renamed dropped primary barrel to update the linked group name",
					player -> getLinkedStorageGroupName(player.serverLevel(), fixture.endpoint()).equals(fixture.groupName())
							&& player.getItemInHand(InteractionHand.MAIN_HAND).getHoverName().getString().equals(fixture.groupName()));
			AutomationRuntime.runOnServer(player -> {
				placeBlock(player.serverLevel(), player, fixture.secondaryPos(), fixture.renamedSecondary());
				return "";
			});
			waitForClientStorageBlock(fixture.secondaryPos());
			openLinkedStorageMenu(fixture.secondaryPos());
			waitForLinkedStorageMenu(fixture.expectedSlots(), fixture.groupName(), LinkedStorageEndpointRole.SECONDARY);
			closeMenu();
			waitForClosedStorageMenu();
			AutomationRuntime.runOnServer(player -> {
				placeBlock(player.serverLevel(), player, fixture.primaryPos(), fixture.renamedPrimary());
				BarrelBlockEntity primary = getBarrel(player.serverLevel(), fixture.primaryPos());
				require(fixture.endpoint().equals(requireEndpoint(primary, "re-placed renamed primary barrel"))
						&& primary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
						"Re-placed renamed primary barrel did not restore its primary endpoint identity");
				return "";
			});
			waitForClientStorageBlock(fixture.primaryPos());
			openLinkedStorageMenu(fixture.primaryPos());
			waitForLinkedStorageMenu(fixture.expectedSlots(), fixture.groupName(), LinkedStorageEndpointRole.PRIMARY);
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				level.destroyBlock(fixture.primaryPos(), true, player);
				ItemStack droppedPrimary = level.getEntitiesOfClass(ItemEntity.class, new AABB(fixture.primaryPos())).stream()
						.filter(itemEntity -> fixture.endpoint().equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst()
						.map(ItemEntity::getItem).orElseThrow(() -> new IllegalStateException("Placed renamed primary did not retain its endpoint drop"));
				require(droppedPrimary.getHoverName().getString().equals(fixture.groupName()),
						"Placed renamed primary lost its custom name on the next endpoint drop");
				return "";
			});
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setGameMode(fixture.originalGameMode());
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalMainHand());
				player.setYRot(fixture.originalYRot());
				player.setXRot(fixture.originalXRot());
				teleportPlayer(player, fixture.originalPosition());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return "";
			});
		}
	}

	private static void runLinkedStorageTierUpgradeRegression() {
		LinkedStorageTierUpgradeFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedStorageTierUpgradeFixture);
		try {
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				player.setGameMode(GameType.SURVIVAL);
				player.setPos(fixture.primaryPos().getCenter());
				ItemStack secondaryUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				require(useTierUpgrade(player, fixture.secondaryPos(), secondaryUpgrade) == InteractionResult.FAIL,
						"Linked storage tier upgrade did not fail outside Main linked storage");
				require(secondaryUpgrade.getCount() == 1, "Secondary linked barrel consumed a tier upgrade");
				require(level.getBlockState(fixture.secondaryPos()).is(ModBlocks.BARREL.get()), "Secondary linked barrel changed tier");
				require(getBarrel(level, fixture.secondaryPos()).getStorageWrapper().getInventoryHandler().getSlots() == fixture.originalSlots(),
						"Secondary linked barrel changed canonical inventory capacity");

				ItemStack primaryUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				require(useTierUpgrade(player, fixture.primaryPos(), primaryUpgrade) == InteractionResult.SUCCESS,
						"Primary linked barrel did not accept its tier upgrade");
				require(primaryUpgrade.isEmpty(), "Primary linked barrel did not consume its tier upgrade");
				require(level.getBlockState(fixture.primaryPos()).is(ModBlocks.DIAMOND_BARREL.get()), "Primary linked barrel did not change tier");

				BarrelBlockEntity primary = getBarrel(level, fixture.primaryPos());
				BarrelBlockEntity secondary = getBarrel(level, fixture.secondaryPos());
				require(requireEndpoint(primary, "upgraded primary barrel").equals(fixture.primaryEndpoint()),
						"Primary linked barrel did not preserve its endpoint identity");
				require(requireEndpoint(secondary, "upgraded secondary barrel").equals(fixture.secondaryEndpoint()),
						"Secondary linked barrel endpoint identity changed during primary upgrade");
				require(primary.getStorageWrapper().getInventoryHandler().getSlots() == fixture.expectedSlots()
						&& secondary.getStorageWrapper().getInventoryHandler().getSlots() == fixture.expectedSlots(),
						"Primary tier upgrade did not update canonical linked inventory capacity");
				require(primary.getStorageWrapper().getUpgradeHandler().getSlots() == fixture.expectedUpgradeSlots()
						&& secondary.getStorageWrapper().getUpgradeHandler().getSlots() == fixture.expectedUpgradeSlots(),
						"Primary tier upgrade did not update canonical linked upgrade capacity");
				require(requireCapability(level, fixture.primaryPos(), "upgraded primary barrel").getSlots() == fixture.expectedSlots()
						&& requireCapability(level, fixture.secondaryPos(), "upgraded secondary barrel").getSlots() == fixture.expectedSlots(),
						"Primary tier upgrade did not update linked endpoint capability capacity");
				return "";
			});
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.setGameMode(fixture.originalGameMode());
				player.setPos(fixture.originalPosition());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return "";
			});
		}
	}

	private static LinkedStorageTierUpgradeFixture setupLinkedStorageTierUpgradeFixture(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 48);
		BlockPos secondaryPos = primaryPos.east(3);
		Vec3 originalPosition = player.position();
		clearArea(level, primaryPos);
		placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		BarrelBlockEntity primary = getBarrel(level, primaryPos);
		BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not create the linked tier-upgrade group");
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not add the linked tier-upgrade secondary barrel");
		return new LinkedStorageTierUpgradeFixture(primaryPos, secondaryPos, requireEndpoint(primary, "tier-upgrade primary barrel"),
				requireEndpoint(secondary, "tier-upgrade secondary barrel"), primary.getStorageWrapper().getInventoryHandler().getSlots(),
				ModBlocks.DIAMOND_BARREL.get().getNumberOfInventorySlots(), ModBlocks.DIAMOND_BARREL.get().getNumberOfUpgradeSlots(),
				getLinkedStorageGroupName(level, requireEndpoint(primary, "tier-upgrade primary barrel")), originalPosition,
				player.gameMode.getGameModeForPlayer());
	}

	private static void runTierUpgradeMenuInvalidationRegression() {
		runTierUpgradeMenuInvalidationRegression(true);
		runTierUpgradeMenuInvalidationRegression(false);
	}

	private static void runTierUpgradeMenuInvalidationRegression(boolean primaryMenu) {
		LinkedStorageTierUpgradeFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedStorageTierUpgradeFixture);
		BlockPos openMenuPos = primaryMenu ? fixture.primaryPos() : fixture.secondaryPos();
		LinkedStorageEndpointRole openMenuRole = primaryMenu ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY;
		try {
			AutomationRuntime.runOnServer(player -> {
				player.setGameMode(GameType.SURVIVAL);
				teleportPlayer(player, fixture.primaryPos().getCenter());
				return "";
			});
			waitForClientPlayerPosition(fixture.primaryPos().getCenter());
			waitForClientStorageBlock(openMenuPos);
			openLinkedStorageMenu(openMenuPos);
			waitForLinkedStorageMenu(fixture.originalSlots(), fixture.groupName(), openMenuRole);
			if (primaryMenu) {
				AutomationRuntime.runOnServer(player -> {
					ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
					require(useTierUpgrade(player, fixture.primaryPos(), upgrade) == InteractionResult.PASS,
							"Primary tier upgrade did not reject its open linked storage menu");
					require(upgrade.getCount() == 1, "Primary tier upgrade consumed an item while its linked storage menu was open");
					return "";
				});
				closeMenu();
				waitForClosedStorageMenu();
			}
			String upgradedGroupName = AutomationRuntime.runOnServer(player -> {
				ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				require(useTierUpgrade(player, fixture.primaryPos(), upgrade) == InteractionResult.SUCCESS,
						"Primary tier upgrade did not complete after its linked menu was closed");
				return getLinkedStorageGroupName(player.serverLevel(), fixture.primaryEndpoint());
			});
			if (!primaryMenu) {
				waitForClosedStorageMenu();
			}
			openLinkedStorageMenu(fixture.secondaryPos());
			waitForLinkedStorageMenu(fixture.expectedSlots(), upgradedGroupName, LinkedStorageEndpointRole.SECONDARY);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setGameMode(fixture.originalGameMode());
				teleportPlayer(player, fixture.originalPosition());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return "";
			});
		}
	}

	private static DroppedPrimaryRenameFixture setupDroppedPrimaryRenameFixture(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 36);
		BlockPos secondaryPos = primaryPos.east(3);
		ItemStack originalMainHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
		float originalYRot = player.getYRot();
		float originalXRot = player.getXRot();
		Vec3 originalPosition = player.position();
		clearArea(level, primaryPos);
		placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		BarrelBlockEntity primary = getBarrel(level, primaryPos);
		BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not create the dropped-primary rename group");
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not add the dropped-primary rename secondary barrel");
		LinkedStorageEndpointData endpoint = requireEndpoint(primary, "dropped-primary rename barrel");
		String originalGroupName = getLinkedStorageGroupName(level, endpoint);
		level.destroyBlock(primaryPos, true, player);
		ItemEntity droppedPrimary = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos)).stream()
				.filter(itemEntity -> endpoint.equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst()
				.orElseThrow(() -> new IllegalStateException("Breaking a linked primary barrel did not create an endpoint item"));
		ItemStack renamedPrimary = droppedPrimary.getItem().copy();
		require(!renamedPrimary.has(ModCoreDataComponents.STORAGE_UUID), "Dropped linked primary barrel retained an ordinary storage UUID");
		require(Boolean.TRUE.equals(renamedPrimary.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT)),
				"Dropped linked primary barrel did not retain its Core primary role component");
		String groupName = "Anvil Renamed Linked Storage";
		renamedPrimary.set(DataComponents.CUSTOM_NAME, Component.literal(groupName));
		droppedPrimary.discard();
		level.destroyBlock(secondaryPos, true, player);
		ItemStack renamedSecondary = level.getEntitiesOfClass(ItemEntity.class, new AABB(secondaryPos)).stream()
				.filter(itemEntity -> StorageBlockEntity.getLinkedStorageEndpointData(itemEntity.getItem())
						.map(secondaryEndpoint -> endpoint.groupId().equals(secondaryEndpoint.groupId())
								&& !Boolean.TRUE.equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT)))
						.orElse(false))
				.findFirst().map(ItemEntity::getItem)
				.orElseThrow(() -> new IllegalStateException("Breaking a linked secondary barrel did not create an endpoint item"));
		String secondaryName = "Secondary Stack Name";
		renamedSecondary.set(DataComponents.CUSTOM_NAME, Component.literal(secondaryName));
		player.setItemInHand(InteractionHand.MAIN_HAND, renamedSecondary);
		return new DroppedPrimaryRenameFixture(primaryPos, secondaryPos, endpoint, primary.getStorageWrapper().getInventoryHandler().getSlots(),
				player.gameMode.getGameModeForPlayer(), originalMainHand, originalYRot, originalXRot, originalPosition, groupName, originalGroupName,
				secondaryName, level.getGameTime() + 2L, renamedPrimary, renamedSecondary);
	}

	private static String getLinkedStorageGroupName(ServerLevel level, LinkedStorageEndpointData endpoint) {
		return LinkedStorageGroupsSavedData.get(level).manager().resolveVirtualHost(endpoint.groupId())
				.flatMap(ILinkedStorageVirtualHost::getLinkedStorageDisplayName).map(Component::getString)
				.orElseThrow(() -> new IllegalStateException("Missing linked storage host for " + endpoint.groupId()));
	}

	private static LinkedStorageMenuFixture setupLinkedStorageMenuFixture(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 24);
		BlockPos secondaryPos = primaryPos.east(3);
		ItemStack originalMainHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
		float originalYRot = player.getYRot();
		float originalXRot = player.getXRot();
		Vec3 originalPosition = player.position();

		clearArea(level, primaryPos);
		placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()));
		placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		BarrelBlockEntity primary = getBarrel(level, primaryPos);
		BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not create the diamond barrel linked-storage menu group");
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not add the basic barrel to the diamond barrel linked-storage menu group");
		primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT));
		primary.getStorageWrapper().getInventoryHandler().saveInventory();
		String groupName = "Automation Linked Storage";
		primary.setCustomName(Component.literal(groupName));
		return new LinkedStorageMenuFixture(primaryPos, secondaryPos, requireEndpoint(primary, "linked storage menu primary").groupId(),
				primary.getStorageWrapper().getInventoryHandler().getSlots(), originalMainHand, originalYRot, originalXRot, originalPosition, groupName);
	}

	private static void openLinkedStorageMenu(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			if (!(player.serverLevel().getBlockEntity(pos) instanceof StorageBlockEntity storageBlockEntity)) {
				throw new IllegalStateException("Missing linked storage at " + pos);
			}
			player.openMenu(new SophisticatedMenuProvider((windowId, inventory, menuPlayer) -> new StorageContainerMenu(windowId, menuPlayer, pos),
					storageBlockEntity.getMenuDisplayName(), false), buffer -> StorageContainerMenu.writeMenuData(buffer, player, pos));
			return "";
		});
	}

	private static void assertLinkedStorageMenuCanonicalSnapshot(LinkedStorageMenuFixture fixture, BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			if (!(player.containerMenu instanceof StorageContainerMenu menu)) {
				throw new IllegalStateException("Linked storage menu did not open at " + pos);
			}
			LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
			require(menu.getBlockPosition().equals(Optional.of(pos)) && endpoint != null && endpoint.groupId().equals(fixture.groupId())
					&& menu.getStorageWrapper().getInventoryHandler().getSlots() == fixture.expectedSlots()
					&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT,
					"Linked storage server menu did not expose the canonical snapshot at " + pos);
			return "";
		});
	}

	private static void openLimitedBarrelMenu(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof LimitedBarrelBlock)) {
				throw new IllegalStateException("Expected a Limited Barrel I before opening its regression menu");
			}
			ItemStack emptyHand = ItemStack.EMPTY;
			player.setItemInHand(InteractionHand.MAIN_HAND, emptyHand);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
			require(player.gameMode.useItemOn(player, level, emptyHand, InteractionHand.MAIN_HAND, hit).consumesAction(),
					"Could not open the Limited Barrel I through its normal in-world interaction");
			require(player.containerMenu instanceof LimitedBarrelContainerMenu, "Limited Barrel I did not open its dedicated server menu");
			return "";
		});
	}

	private static void openLinkedStorageChestMenu(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			ItemStack emptyHand = ItemStack.EMPTY;
			player.setItemInHand(InteractionHand.MAIN_HAND, emptyHand);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
			require(player.gameMode.useItemOn(player, level, emptyHand, InteractionHand.MAIN_HAND, hit).consumesAction(),
					"Could not open the linked chest through its normal in-world interaction");
			require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(pos)),
					"Linked chest did not open its ordinary storage menu");
			return "";
		});
	}

	private static void closeMenu() {
		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			return "";
		});
	}

	private static void waitForClientStorageBlock(BlockPos pos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof StorageBlockEntity)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for linked storage at " + pos);
	}

	private static void waitForClientLimitedBarrelMenu(BlockPos expectedPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof LimitedBarrelContainerMenu menu
							&& menu.getBlockPosition().equals(Optional.of(expectedPos)))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked Limited Barrel I did not open its dedicated Limited Barrel menu");
	}

	private static void waitForClientLimitedBarrelReloadProjection(LimitedBarrelReloadStatus status) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null
					&& Minecraft.getInstance().level.getBlockEntity(LINKED_LIMITED_RELOAD_PRIMARY_POS) instanceof LimitedBarrelBlockEntity primary
					&& Minecraft.getInstance().level.getBlockEntity(LINKED_LIMITED_RELOAD_SECONDARY_POS) instanceof LimitedBarrelBlockEntity secondary
					&& status.primaryEndpoint().equals(primary.getLinkedStorageEndpointData())
					&& status.secondaryEndpoint().equals(secondary.getLinkedStorageEndpointData()) && hasLimitedRenderProjection(primary)
					&& hasLimitedRenderProjection(secondary))) {
				return;
			}
			sleep(50);
		}

		String clientState = AutomationRuntime.runOnClient(() -> describeLimitedBarrelReloadProjection());
		throw new IllegalStateException(
				"Reloaded linked limited barrels did not synchronize display items, counts, and fill levels to the client: " + clientState);
	}

	private static boolean hasLimitedRenderProjection(LimitedBarrelBlockEntity limitedBarrel) {
		return limitedBarrel.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().stream()
				.anyMatch(displayItem -> displayItem.getItem().is(Items.DIAMOND)) && limitedBarrel.getSlotCounts().contains(LINKED_LIMITED_RELOAD_ITEM_COUNT)
				&& limitedBarrel.getSlotFillLevels().stream().anyMatch(fillLevel -> fillLevel > 0F);
	}

	private static String describeLimitedBarrelReloadProjection() {
		if (Minecraft.getInstance().level == null) {
			return "level=missing";
		}
		return List.of(LINKED_LIMITED_RELOAD_PRIMARY_POS, LINKED_LIMITED_RELOAD_SECONDARY_POS).stream().map(pos -> {
			if (!(Minecraft.getInstance().level.getBlockEntity(pos) instanceof LimitedBarrelBlockEntity limitedBarrel)) {
				return pos + "=missing";
			}
			return pos + "=endpoint=" + limitedBarrel.getLinkedStorageEndpointData() + ", displayItems="
					+ limitedBarrel.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems() + ", counts="
					+ limitedBarrel.getSlotCounts() + ", fillLevels=" + limitedBarrel.getSlotFillLevels();
		}).toList().toString();
	}

	private static void waitForClientLinkedStorageStack(LinkedStorageStackTooltipFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& StorageBlockEntity.getLinkedStorageEndpointData(Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot()))
							.filter(endpoint -> endpoint.groupId().equals(fixture.groupId())).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for the dropped linked storage endpoint stack to reach the client inventory");
	}

	private static void waitForClientLinkedStorageTooltipContents(LinkedStorageStackTooltipFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null
						|| ClientLinkedStorageContents.getRevision(fixture.groupId()).isEmpty()) {
					return false;
				}
				StackStorageWrapper wrapper = StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(),
						Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot()));
				return count(wrapper.getInventoryHandler(), Items.DIAMOND) == 7
						&& wrapper.getUpgradeHandler().getStackInSlot(0).is(ModItems.MAGNET_UPGRADE.get());
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for linked storage tooltip contents to load from the canonical group snapshot");
	}

	private static void waitForClientPlayerPosition(Vec3 expectedPosition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.position().distanceToSqr(expectedPosition) < 0.25D)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for the client player to reach the linked storage menu fixture");
	}

	private static void waitForLinkedStorageMenu(int expectedSlots, String expectedTitle, LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu menu
							&& screen.getTitle().getString().equals(expectedTitle) && menu.getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots
							&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
							&& endpointProvider.getLinkedStorageEndpointRole().filter(role -> role == expectedRole).isPresent())) {
				return;
			}
			sleep(50);
		}
		String clientMenuState = AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
				return "screen=" + (Minecraft.getInstance().screen == null ? "none" : Minecraft.getInstance().screen.getClass().getSimpleName());
			}
			if (!(screen.getMenu() instanceof StorageContainerMenu menu)) {
				return "menu=" + screen.getMenu().getClass().getSimpleName();
			}
			boolean titleMatches = screen.getTitle().getString().equals(expectedTitle);
			boolean slotsMatch = menu.getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots;
			boolean roleMatches = menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
					&& endpointProvider.getLinkedStorageEndpointRole().filter(role -> role == expectedRole).isPresent();
			return "title='" + screen.getTitle().getString() + "', slots=" + menu.getStorageWrapper().getInventoryHandler().getSlots() + ", role="
					+ (menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
							? endpointProvider.getLinkedStorageEndpointRole().map(Enum::name).orElse("none")
							: "unlinked")
					+ ", titleMatches=" + titleMatches + ", slotsMatch=" + slotsMatch + ", roleMatches=" + roleMatches;
		});
		throw new IllegalStateException("Timed out waiting for linked storage menu with title '" + expectedTitle + "' and " + expectedSlots + " slots and role "
				+ expectedRole + "; " + clientMenuState);
	}

	private static void waitForLinkedStorageMenuCanonicalSnapshot(LinkedStorageMenuFixture fixture, BlockPos expectedPos,
			LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu menu
							&& menu.getBlockPosition().equals(Optional.of(expectedPos)) && menu.getStorageBlockEntity().getLinkedStorageEndpointData() != null
							&& menu.getStorageBlockEntity().getLinkedStorageEndpointData().groupId().equals(fixture.groupId())
							&& menu.getStorageWrapper().getInventoryHandler().getSlots() == fixture.expectedSlots()
							&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT
							&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
							&& endpointProvider.getLinkedStorageEndpointRole().filter(role -> role == expectedRole).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for linked storage menu canonical snapshot at " + expectedPos + " with group " + fixture.groupId());
	}

	private static void waitForLinkedStorageSettingsMenu(BlockPos expectedPos, int expectedSlots, LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageSettingsContainerMenu menu
							&& menu.getBlockPosition().equals(expectedPos) && menu.getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots
							&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
							&& endpointProvider.getLinkedStorageEndpointRole().filter(role -> role == expectedRole).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for linked storage settings menu with " + expectedSlots + " slots");
	}

	private static void waitForClientLinkedUpgradeSwitch(BlockPos expectedPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu
							&& menu.getBlockPosition().equals(Optional.of(expectedPos))
							&& menu.getSlot(menu.getFirstUpgradeSlot()).getItem().is(ModItems.ADVANCED_FILTER_UPGRADE.get()) && menu.canDisableUpgrade(0)
							&& Minecraft.getInstance().screen instanceof StorageScreenBase<?> screen
							&& screen.children().stream().anyMatch(child -> child instanceof ToggleButton<?> button && button.getX() == screen.getGuiLeft() - 22
									&& button.getY() == screen.getGuiTop() + 8))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked storage Advanced Filter Upgrade did not create an on/off switch while the menu remained open");
	}

	private static void waitForClientMemorizedStack(BlockPos expectedPos, Item expectedItem) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu
							&& menu.getBlockPosition().equals(Optional.of(expectedPos))
							&& menu.getMemorizedStackInSlot(0).filter(stack -> stack.is(expectedItem)).isPresent())) {
				return;
			}
			sleep(50);
		}
		String clientState = AutomationRuntime.runOnClient(() -> {
			if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu)) {
				return "menu=closed";
			}
			Optional<LinkedStorageEndpointData> endpoint = Optional.ofNullable(menu.getStorageBlockEntity().getLinkedStorageEndpointData());
			return "menuPos=" + menu.getBlockPosition() + ", memorized=" + menu.getMemorizedStackInSlot(0) + ", endpoint=" + endpoint + ", revision="
					+ endpoint.flatMap(data -> ClientLinkedStorageContents.getRevision(data.groupId())) + ", cached="
					+ endpoint.flatMap(data -> ItemContentsStorage.get().getStorageContents(data.groupId()));
		});
		throw new IllegalStateException("Locked linked limited barrel did not synchronize its memorized item to the open client menu: " + clientState);
	}

	private static void waitForClosedStorageMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_MENU_CONVERGENCE_SECONDS);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(player -> player.containerMenu == player.inventoryMenu)
					&& AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen == null)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for stale linked storage menu to close");
	}

	private static void runStorageFamilyCompatibilityRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos chestLeftPos = player.blockPosition().offset(0, 0, 180);
			BlockPos chestRightPos = chestLeftPos.east();
			BlockPos barrelPos = chestRightPos.east(2);
			BlockPos shulkerPos = barrelPos.east();
			BlockPos limitedOnePos = chestLeftPos.south(3);
			BlockPos limitedCopperOnePos = limitedOnePos.east();
			BlockPos limitedTwoPos = limitedCopperOnePos.east();
			BlockPos standardPos = limitedTwoPos.east();
			BlockPos restoredChestPos = chestLeftPos.south(6);
			ItemStack originalMainHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
			float originalYRot = player.getYRot();
			float originalXRot = player.getXRot();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			clearArea(level, chestLeftPos);
			try {
				placeChest(level, player, chestLeftPos);
				placeChest(level, player, chestRightPos);
				placeBlock(level, player, barrelPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, shulkerPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));

				ChestBlockEntity leftChest = getChest(level, chestLeftPos);
				ChestBlockEntity mainChest = getChest(level, chestRightPos);
				BarrelBlockEntity barrel = getBarrel(level, barrelPos);
				ShulkerBoxBlockEntity shulker = getShulkerBox(level, shulkerPos);
				require(mainChest.isMainChest() && mainChest.isLinkedStorageCandidate() && !leftChest.isLinkedStorageCandidate(),
						"Only the main half of a double chest should be a linked-storage candidate");

				ItemStack standardLinker = new ItemStack(ENDER_LINKER.get());
				require(useLinker(player, standardLinker, chestLeftPos) == InteractionResult.SUCCESS,
						"Ender Linker did not route a double-chest secondary interaction to its main endpoint");
				LinkedStorageEndpointData chestEndpoint = requireEndpoint(mainChest, "double-chest main endpoint");
				require(leftChest.getLinkedStorageEndpointData() == null, "Double-chest secondary received linked endpoint data instead of its main half");
				ItemStack packingTape = new ItemStack(ModItems.PACKING_TAPE.get());
				Vec3 originalPackingTapePosition = player.position();
				try {
					teleportPlayer(player, chestLeftPos.above().getCenter());
					player.setItemInHand(InteractionHand.MAIN_HAND, packingTape);
					InteractionResult packingTapeResult = player.gameMode.useItemOn(player, level, packingTape, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(chestLeftPos), Direction.UP, chestLeftPos, false));
					require(packingTapeResult.consumesAction() && mainChest.isLinkedStorage() && !mainChest.isPacked() && !leftChest.isPacked()
							&& packingTape.getDamageValue() == 0,
							"Packing tape consumed durability or packed a linked double chest when used on its non-main half");
				} finally {
					teleportPlayer(player, originalPackingTapePosition);
				}
				standardLinker.setCount(3);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), standardLinker, barrel) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), standardLinker, shulker) == LinkedStorageService.LinkResult.SUCCESS,
						"Standard barrel, chest, and shulker storage types did not interlink");
				LinkedStorageEndpointData barrelEndpoint = requireEndpoint(barrel, "standard barrel endpoint");
				LinkedStorageEndpointData shulkerEndpoint = requireEndpoint(shulker, "standard shulker endpoint");
				require(chestEndpoint.groupId().equals(barrelEndpoint.groupId()) && chestEndpoint.groupId().equals(shulkerEndpoint.groupId()),
						"Standard storage types did not share one linked-storage group: chest=" + chestEndpoint.groupId() + ", barrel="
								+ barrelEndpoint.groupId() + ", shulker=" + shulkerEndpoint.groupId() + ", linker="
								+ standardLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET));
				mainChest.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				mainChest.getStorageWrapper().getInventoryHandler().saveInventory();
				require(count(barrel.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
						&& count(shulker.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
						"Standard linked storage types did not expose shared canonical contents");
				level.destroyBlock(shulkerPos, true, player);
				List<ItemEntity> shulkerDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(shulkerPos).inflate(2.0D),
						itemEntity -> itemEntity.getItem().is(ModBlocks.SHULKER_BOX_ITEM.get()));
				require(shulkerDrops.size() == 1 && shulkerEndpoint.equals(shulkerDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
						&& shulkerDrops.getFirst().getItem().get(ModCoreDataComponents.STORAGE_UUID) == null,
						"Linked shulker drop did not retain its endpoint without creating ordinary shulker contents data: drops="
								+ shulkerDrops.stream().map(itemEntity -> "endpoint=" + itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)
										+ ", uuid=" + itemEntity.getItem().get(ModCoreDataComponents.STORAGE_UUID)).toList());
				ItemStack linkedShulkerCarrier = shulkerDrops.getFirst().getItem().copy();
				shulkerDrops.forEach(ItemEntity::discard);
				try {
					player.setGameMode(GameType.SURVIVAL);
					placeBlock(level, player, shulkerPos, linkedShulkerCarrier);
				} finally {
					player.setGameMode(originalGameMode);
				}
				ShulkerBoxBlockEntity restoredShulker = getShulkerBox(level, shulkerPos);
				require(shulkerEndpoint.equals(restoredShulker.getLinkedStorageEndpointData())
						&& count(restoredShulker.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
						"Linked shulker did not restore its endpoint and shared contents after being replaced: endpoint="
								+ restoredShulker.getLinkedStorageEndpointData() + ", candidate=" + restoredShulker.isLinkedStorageCandidate() + ", member="
								+ LinkedStorageGroupsSavedData.get(level).manager().isEndpointMember(shulkerEndpoint.groupId(), shulkerEndpoint.endpointId())
								+ ", diamonds=" + count(restoredShulker.getStorageWrapper().getInventoryHandler(), Items.DIAMOND));

				placeBlock(level, player, limitedOnePos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
				placeBlock(level, player, limitedCopperOnePos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
				placeBlock(level, player, limitedTwoPos, new ItemStack(ModBlocks.LIMITED_BARREL_2_ITEM.get()));
				placeBlock(level, player, standardPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				LimitedBarrelBlockEntity limitedOne = getLimitedBarrel(level, limitedOnePos);
				LimitedBarrelBlockEntity limitedCopperOne = getLimitedBarrel(level, limitedCopperOnePos);
				LimitedBarrelBlockEntity limitedTwo = getLimitedBarrel(level, limitedTwoPos);
				BarrelBlockEntity standardBarrel = getBarrel(level, standardPos);
				ItemStack limitedLinker = new ItemStack(ENDER_LINKER.get(), 4);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker, limitedOne) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker,
								limitedCopperOne) == LinkedStorageService.LinkResult.SUCCESS,
						"Limited barrels with the same slot family did not interlink across tiers");
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker,
						limitedTwo) == LinkedStorageService.LinkResult.INCOMPATIBLE_ENDPOINT
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker,
								standardBarrel) == LinkedStorageService.LinkResult.INCOMPATIBLE_ENDPOINT
						&& !limitedTwo.isLinkedStorage() && !standardBarrel.isLinkedStorage(),
						"Limited barrels accepted a different slot family or a standard barrel");
				limitedOne.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 23));
				limitedOne.getStorageWrapper().getInventoryHandler().saveInventory();
				require(limitedOne.getSlotCounts().contains(23) && limitedCopperOne.getSlotCounts().contains(23)
						&& limitedOne.getSlotFillLevels().stream().anyMatch(fillLevel -> fillLevel > 0F)
						&& limitedCopperOne.getSlotFillLevels().stream().anyMatch(fillLevel -> fillLevel > 0F),
						"Linked limited barrels did not project canonical item counts and fill levels to their render state");

				level.destroyBlock(chestLeftPos, true, player);
				require(level.getBlockState(chestLeftPos).isAir() && level.getBlockState(chestRightPos).isAir(),
						"Breaking one linked double-chest half did not remove the whole endpoint");
				List<ItemEntity> chestDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(chestLeftPos).inflate(2.0D),
						itemEntity -> itemEntity.getItem().is(ModBlocks.CHEST_ITEM.get()));
				require(chestDrops.size() == 1 && ChestBlockItem.isDoubleChest(chestDrops.getFirst().getItem())
						&& chestEndpoint.equals(chestDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
						"Linked double chest did not drop exactly one paired endpoint carrier from either half");
				ItemStack linkedDoubleChestCarrier = chestDrops.getFirst().getItem().copy();
				chestDrops.forEach(ItemEntity::discard);
				try {
					player.setGameMode(GameType.SURVIVAL);
					placeChest(level, player, restoredChestPos, linkedDoubleChestCarrier);
				} finally {
					player.setGameMode(originalGameMode);
				}
				List<ChestBlockEntity> restoredChestParts = List
						.of(restoredChestPos, restoredChestPos.north(), restoredChestPos.south(), restoredChestPos.east(), restoredChestPos.west()).stream()
						.map(level::getBlockEntity).filter(ChestBlockEntity.class::isInstance).map(ChestBlockEntity.class::cast).toList();
				ChestBlockEntity restoredMainChest = restoredChestParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main half after restoring linked double chest"));
				require(restoredChestParts.size() == 2 && restoredChestParts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
						&& restoredMainChest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT
						&& restoredChestParts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
						&& chestEndpoint.equals(requireEndpoint(restoredMainChest, "restored double-chest main endpoint"))
						&& restoredMainChest.getStorageWrapper().getInventoryHandler().getSlots() == 54,
						"Linked double chest did not restore as one paired endpoint with double capacity");
				return "";
			} finally {
				player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
				player.setYRot(originalYRot);
				player.setXRot(originalXRot);
				player.setGameMode(originalGameMode);
				clearArea(level, chestLeftPos);
			}
		});
	}

	private static void runLinkedPrimaryChestExpansionRegression() {
		runLinkedPrimaryChestExpansionRegression(Direction.EAST, 48);
		runLinkedPrimaryChestExpansionRegression(Direction.WEST, 56);
	}

	private static void runLinkedPrimaryChestExpansionRegression(Direction addedChestDirection, int zOffset) {
		LinkedPrimaryChestExpansionFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos originalPrimaryPos = player.blockPosition().offset(0, 0, zOffset);
			BlockPos addedChestPos = originalPrimaryPos.relative(addedChestDirection);
			ItemStack originalMainHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
			float originalYRot = player.getYRot();
			float originalXRot = player.getXRot();
			Vec3 originalPosition = player.position();
			clearArea(level, originalPrimaryPos);
			placeChest(level, player, originalPrimaryPos);
			ChestBlockEntity originalPrimary = getChest(level, originalPrimaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 4);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, originalPrimary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked primary chest for double-chest expansion regression");
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(originalPrimary, "single linked primary chest endpoint");
			String groupName = "Expanded Linked Primary " + addedChestDirection.getName();
			originalPrimary.setCustomName(Component.literal(groupName));
			originalPrimary.getStorageWrapper().getInventoryHandler().setStackInSlot(0,
					new ItemStack(Items.DIAMOND, LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT));
			originalPrimary.getStorageWrapper().getInventoryHandler().saveInventory();
			return new LinkedPrimaryChestExpansionFixture(originalPrimaryPos, addedChestPos, primaryEndpoint, groupName, originalMainHand, originalYRot,
					originalXRot, originalPosition);
		});
		try {
			AutomationRuntime.runOnServer(player -> {
				teleportPlayer(player, fixture.originalPrimaryPos().getCenter());
				return "";
			});
			waitForClientPlayerPosition(fixture.originalPrimaryPos().getCenter());
			waitForClientStorageBlock(fixture.originalPrimaryPos());
			openLinkedStorageChestMenu(fixture.originalPrimaryPos());
			waitForLinkedStorageMenu(27, fixture.groupName(), LinkedStorageEndpointRole.PRIMARY);
			AutomationRuntime.runOnClient(() -> {
				if (!(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Linked primary chest inventory menu did not open before settings transfer test");
				}
				menu.openSettings();
				return "";
			});
			waitForLinkedStorageSettingsMenu(fixture.originalPrimaryPos(), 27, LinkedStorageEndpointRole.PRIMARY);
			AutomationRuntime.runOnServer(player -> {
				require(player.containerMenu instanceof StorageSettingsContainerMenu menu && menu.getStorageWrapper().getInventoryHandler().getSlots() == 27,
						"Linked primary single chest did not have a 27-slot settings menu open before expansion");
				return "";
			});

			BlockPos expandedPrimaryPos = AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				if (!(player.containerMenu instanceof StorageSettingsContainerMenu staleMenu)) {
					throw new IllegalStateException("Linked primary chest menu closed before expansion");
				}
				placeChest(level, player, fixture.addedChestPos());
				List<ChestBlockEntity> chestParts = List.of(getChest(level, fixture.originalPrimaryPos()), getChest(level, fixture.addedChestPos()));
				ChestBlockEntity expandedPrimary = chestParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after expanding a linked primary"));
				require(player.containerMenu == player.inventoryMenu && player.containerMenu != staleMenu,
						"Linked primary root replacement did not close the stale server menu");
				require(chestParts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
						&& expandedPrimary.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT
						&& chestParts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
						&& fixture.primaryEndpoint().equals(requireEndpoint(expandedPrimary, "expanded linked primary endpoint"))
						&& expandedPrimary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.PRIMARY)
								.isPresent()
						&& expandedPrimary.getMenuDisplayName().getString().equals(fixture.groupName())
						&& count(expandedPrimary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT
						&& expandedPrimary.getStorageWrapper().getInventoryHandler().getSlots() == 54,
						"Adding a matching chest did not preserve the linked primary group, role, title, contents, and double-chest capacity");
				return expandedPrimary.getBlockPos();
			});
			waitForClosedStorageMenu();
			waitForClientStorageBlock(expandedPrimaryPos);
			openLinkedStorageMenu(expandedPrimaryPos);
			waitForLinkedStorageMenu(54, fixture.groupName(), LinkedStorageEndpointRole.PRIMARY);
			AutomationRuntime.runOnClient(() -> {
				if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
						|| !(screen.getMenu() instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Expanded linked primary chest did not reopen on the client");
				}
				LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
				require(endpoint != null && endpoint.groupId().equals(fixture.primaryEndpoint().groupId())
						&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT,
						"Reopened expanded linked primary menu did not preserve its group and seven diamonds");
				return "";
			});
			closeMenu();
			waitForClosedStorageMenu();

			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				GameType originalGameMode = player.gameMode.getGameModeForPlayer();
				player.setGameMode(GameType.SURVIVAL);
				try {
					require(player.gameMode.destroyBlock(expandedPrimaryPos), "Player break did not remove the expanded linked primary chest");
					List<ItemEntity> chestDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(fixture.originalPrimaryPos()).inflate(1.5D),
							itemEntity -> itemEntity.getItem().is(ModBlocks.CHEST_ITEM.get()));
					require(level.getBlockState(fixture.originalPrimaryPos()).isAir() && level.getBlockState(fixture.addedChestPos()).isAir()
							&& chestDrops.size() == 1 && ChestBlockItem.isDoubleChest(chestDrops.getFirst().getItem())
							&& fixture.primaryEndpoint().equals(chestDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
							&& StorageBlockEntity.getLinkedStorageEndpointRole(chestDrops.getFirst().getItem())
									.filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
							"Expanded linked primary chest did not drop one double-chest primary carrier");
					ItemStack carrier = chestDrops.getFirst().getItem().copy();
					chestDrops.forEach(ItemEntity::discard);
					placeChest(level, player, fixture.originalPrimaryPos(), carrier);
					List<ChestBlockEntity> restoredParts = List
							.of(fixture.originalPrimaryPos(), fixture.originalPrimaryPos().north(), fixture.originalPrimaryPos().south(),
									fixture.originalPrimaryPos().east(), fixture.originalPrimaryPos().west())
							.stream().map(level::getBlockEntity).filter(ChestBlockEntity.class::isInstance).map(ChestBlockEntity.class::cast).toList();
					ChestBlockEntity restoredPrimary = restoredParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
							.orElseThrow(() -> new IllegalStateException("Missing main chest after restoring expanded primary carrier"));
					require(restoredParts.size() == 2 && restoredParts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
							&& restoredPrimary.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT
							&& restoredParts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
							&& fixture.primaryEndpoint().equals(requireEndpoint(restoredPrimary, "restored expanded primary endpoint"))
							&& restoredPrimary.getStorageWrapper().getInventoryHandler().getSlots() == 54,
							"Expanded linked primary chest carrier did not restore as a linked double chest");
					ChestBlockEntity restoredNonMain = restoredParts.stream().filter(chest -> !chest.isMainChest()).findFirst()
							.orElseThrow(() -> new IllegalStateException("Missing non-main chest after restoring expanded primary carrier"));
					require(player.gameMode.destroyBlock(restoredNonMain.getBlockPos()), "Player break did not remove the restored linked primary chest");
					List<ItemEntity> restoredChestDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(fixture.originalPrimaryPos()).inflate(1.5D),
							itemEntity -> itemEntity.getItem().is(ModBlocks.CHEST_ITEM.get()));
					require(level.getBlockState(fixture.originalPrimaryPos()).isAir() && level.getBlockState(fixture.addedChestPos()).isAir()
							&& restoredChestDrops.size() == 1 && ChestBlockItem.isDoubleChest(restoredChestDrops.getFirst().getItem())
							&& fixture.primaryEndpoint().equals(restoredChestDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
							&& StorageBlockEntity.getLinkedStorageEndpointRole(restoredChestDrops.getFirst().getItem())
									.filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
							"Breaking the non-main half of an expanded linked primary chest did not drop one double-chest primary carrier");
					return "";
				} finally {
					player.setGameMode(originalGameMode);
				}
			});
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalMainHand());
				player.setYRot(fixture.originalYRot());
				player.setXRot(fixture.originalXRot());
				teleportPlayer(player, fixture.originalPosition());
				clearArea(player.serverLevel(), fixture.originalPrimaryPos());
				return "";
			});
		}
	}

	private static void runLinkedSecondaryChestSplitRegression() {
		runLinkedSecondaryChestSplitMenuClosureRegression();
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 64);
			BlockPos firstSecondaryPos = primaryPos.east(5);
			BlockPos firstAddedChestPos = firstSecondaryPos.west();
			BlockPos secondSecondaryPos = firstAddedChestPos.east(3);
			BlockPos secondAddedChestPos = secondSecondaryPos.west();
			clearArea(level, primaryPos);
			try {
				placeChest(level, player, primaryPos);
				placeChest(level, player, firstSecondaryPos);
				ChestBlockEntity primary = getChest(level, primaryPos);
				ChestBlockEntity firstSecondary = getChest(level, firstSecondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 4);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, firstSecondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked primary and secondary single chests for expansion regression");
				LinkedStorageEndpointData firstSecondaryEndpoint = requireEndpoint(firstSecondary, "linked secondary single-chest endpoint");
				List<ItemStack> canonicalContents = fillInventoryWithTestContents(primary.getStorageWrapper().getInventoryHandler());
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				int canonicalSlots = primary.getStorageWrapper().getInventoryHandler().getSlots();

				placeChest(level, player, firstAddedChestPos);
				List<ChestBlockEntity> firstSecondaryParts = List.of(getChest(level, firstSecondaryPos), getChest(level, firstAddedChestPos));
				ChestBlockEntity firstSecondaryMain = firstSecondaryParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after expanding linked secondary"));
				require(firstSecondaryParts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
						&& firstSecondaryMain.getBlockPos().equals(firstSecondaryPos)
						&& firstSecondaryParts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
						&& firstSecondaryEndpoint.equals(requireEndpoint(firstSecondaryMain, "expanded linked secondary endpoint"))
						&& firstSecondaryMain.getStorageWrapper().getInventoryHandler().getSlots() == canonicalSlots,
						"Adding a matching chest did not form a linked secondary double chest without resizing canonical storage");

				player.setGameMode(GameType.SURVIVAL);
				ItemStack nonMainTierUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				require(useTierUpgradeAsPlayer(player, firstAddedChestPos, nonMainTierUpgrade) == InteractionResult.FAIL,
						"Linked secondary double-chest non-main part accepted a tier upgrade");
				require(nonMainTierUpgrade.getCount() == 1 && level.getBlockState(firstSecondaryPos).is(ModBlocks.CHEST.get())
						&& level.getBlockState(firstAddedChestPos).is(ModBlocks.CHEST.get())
						&& firstSecondaryEndpoint
								.equals(requireEndpoint(getChest(level, firstSecondaryPos), "linked secondary main after rejected tier upgrade")),
						"Rejected linked secondary double-chest tier upgrade changed a chest or its endpoint");

				BlockPos firstRemainingPos = firstSecondaryMain.getBlockPos().equals(firstSecondaryPos) ? firstAddedChestPos : firstSecondaryPos;
				player.setGameMode(GameType.SURVIVAL);
				require(player.gameMode.destroyBlock(firstSecondaryMain.getBlockPos()), "Player break did not remove the linked secondary main chest");
				ChestBlockEntity firstRemainingSecondary = getChest(level, firstRemainingPos);
				List<ItemEntity> firstBreakDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(firstSecondaryMain.getBlockPos()).inflate(1.5D));
				ItemStack firstBreakDrop = firstBreakDrops.size() == 1 ? firstBreakDrops.getFirst().getItem().copy() : ItemStack.EMPTY;
				require(firstRemainingSecondary.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& firstSecondaryEndpoint.equals(firstRemainingSecondary.getLinkedStorageEndpointData()) && !firstRemainingSecondary.hasCustomName()
						&& hasInventoryContents(primary.getStorageWrapper().getInventoryHandler(), canonicalContents) && firstBreakDrops.size() == 1
						&& !ChestBlockItem.isDoubleChest(firstBreakDrop) && firstBreakDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
						&& !firstBreakDrop.has(DataComponents.CUSTOM_NAME),
						"Breaking the expanded secondary main chest changed or dropped linked inventory contents");
				firstBreakDrops.forEach(ItemEntity::discard);

				BlockPos shiftedAddedChestPos = firstRemainingPos.west();
				player.setGameMode(GameType.CREATIVE);
				placeChest(level, player, shiftedAddedChestPos, firstBreakDrop);
				List<ChestBlockEntity> shiftedSecondaryParts = List.of(getChest(level, shiftedAddedChestPos), getChest(level, firstRemainingPos));
				ChestBlockEntity shiftedSecondaryMain = shiftedSecondaryParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after shifting linked secondary"));
				require(shiftedSecondaryMain.getBlockPos().equals(firstRemainingPos)
						&& firstSecondaryEndpoint.equals(requireEndpoint(shiftedSecondaryMain, "shifted linked secondary endpoint"))
						&& !shiftedSecondaryMain.hasCustomName(), "Shifting the linked secondary double chest did not preserve an unnamed endpoint");

				player.setGameMode(GameType.SURVIVAL);
				require(player.gameMode.destroyBlock(firstRemainingPos), "Player break did not remove the shifted linked secondary main chest");
				ChestBlockEntity shiftedRemainingSecondary = getChest(level, shiftedAddedChestPos);
				List<ItemEntity> shiftedBreakDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(firstRemainingPos).inflate(1.5D));
				ItemStack shiftedBreakDrop = shiftedBreakDrops.size() == 1 ? shiftedBreakDrops.getFirst().getItem().copy() : ItemStack.EMPTY;
				require(shiftedRemainingSecondary.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& firstSecondaryEndpoint.equals(shiftedRemainingSecondary.getLinkedStorageEndpointData()) && !shiftedRemainingSecondary.hasCustomName()
						&& hasInventoryContents(primary.getStorageWrapper().getInventoryHandler(), canonicalContents) && shiftedBreakDrops.size() == 1
						&& !ChestBlockItem.isDoubleChest(shiftedBreakDrop) && shiftedBreakDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
						&& !shiftedBreakDrop.has(DataComponents.CUSTOM_NAME),
						"Breaking the shifted linked secondary main chest changed or dropped linked inventory contents");
				shiftedBreakDrops.forEach(ItemEntity::discard);

				require(player.gameMode.destroyBlock(shiftedAddedChestPos), "Player break did not remove the shifted linked secondary chest");
				List<ItemEntity> remainingDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(shiftedAddedChestPos).inflate(1.5D),
						itemEntity -> itemEntity.getItem().is(ModBlocks.CHEST_ITEM.get()));
				require(remainingDrops.size() == 1 && !ChestBlockItem.isDoubleChest(remainingDrops.getFirst().getItem())
						&& firstSecondaryEndpoint.equals(remainingDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
						"Breaking the remaining expanded secondary single chest did not drop its linked endpoint");
				remainingDrops.forEach(ItemEntity::discard);

				placeChest(level, player, secondSecondaryPos);
				ChestBlockEntity secondSecondary = getChest(level, secondSecondaryPos);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondSecondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not add second linked secondary single chest for expansion regression");
				LinkedStorageEndpointData secondSecondaryEndpoint = requireEndpoint(secondSecondary, "second linked secondary single-chest endpoint");
				placeChest(level, player, secondAddedChestPos);
				List<ChestBlockEntity> secondSecondaryParts = List.of(getChest(level, secondSecondaryPos), getChest(level, secondAddedChestPos));
				ChestBlockEntity secondSecondaryMain = secondSecondaryParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after reverse linked secondary expansion"));
				require(secondSecondaryParts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
						&& secondSecondaryParts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
						&& secondSecondaryEndpoint.equals(requireEndpoint(secondSecondaryMain, "reverse expanded linked secondary endpoint"))
						&& secondSecondaryMain.getStorageWrapper().getInventoryHandler().getSlots() == canonicalSlots,
						"Adding a matching chest on the other side did not form a linked secondary double chest");

				BlockPos secondNonMainPos = secondSecondaryMain.getBlockPos().equals(secondSecondaryPos) ? secondAddedChestPos : secondSecondaryPos;
				require(player.gameMode.destroyBlock(secondNonMainPos), "Player break did not remove the linked secondary non-main chest");
				List<ItemEntity> secondBreakDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(secondNonMainPos).inflate(1.5D));
				require(secondSecondaryMain.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& secondSecondaryEndpoint.equals(secondSecondaryMain.getLinkedStorageEndpointData())
						&& hasInventoryContents(primary.getStorageWrapper().getInventoryHandler(), canonicalContents) && secondBreakDrops.size() == 1
						&& !ChestBlockItem.isDoubleChest(secondBreakDrops.getFirst().getItem())
						&& secondBreakDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null,
						"Breaking the expanded secondary non-main chest changed or dropped linked inventory contents");
				return "";
			} finally {
				player.setGameMode(originalGameMode);
				clearArea(level, primaryPos);
			}
		});
	}

	private static void runLinkedSecondaryChestSplitMenuClosureRegression() {
		LinkedSecondaryChestSplitMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 72);
			BlockPos secondaryPos = primaryPos.east(3);
			BlockPos addedChestPos = secondaryPos.east();
			Vec3 originalPosition = player.position();
			clearArea(level, primaryPos);
			placeChest(level, player, primaryPos);
			placeChest(level, player, secondaryPos);
			ChestBlockEntity primary = getChest(level, primaryPos);
			ChestBlockEntity secondary = getChest(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked secondary chest for split menu closure regression");
			String groupName = "Secondary Split Transfer";
			primary.setCustomName(Component.literal(groupName));
			placeChest(level, player, addedChestPos);
			ChestBlockEntity secondaryMain = List.of(getChest(level, secondaryPos), getChest(level, addedChestPos)).stream()
					.filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Missing linked secondary main chest before split menu closure regression"));
			return new LinkedSecondaryChestSplitMenuFixture(primaryPos, secondaryPos, addedChestPos, secondaryMain.getBlockPos(),
					primary.getStorageWrapper().getInventoryHandler().getSlots(), groupName, originalPosition);
		});
		try {
			openLinkedSecondaryChestSplitMenu(fixture, false);
			BlockPos remainingPos = AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				if (!(player.containerMenu instanceof StorageContainerMenu staleMenu)) {
					throw new IllegalStateException("Linked secondary inventory menu closed before main-half split transfer");
				}
				BlockPos remaining = fixture.mainPos().equals(fixture.secondaryPos()) ? fixture.addedChestPos() : fixture.secondaryPos();
				level.destroyBlock(fixture.mainPos(), false, player);
				require(player.containerMenu == player.inventoryMenu && player.containerMenu != staleMenu
						&& getChest(level, remaining).getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE,
						"Linked secondary main-half split did not close the stale inventory menu");
				return remaining;
			});
			waitForClosedStorageMenu();

			BlockPos reopenedMainPos = AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				BlockPos removedPos = remainingPos.equals(fixture.secondaryPos()) ? fixture.addedChestPos() : fixture.secondaryPos();
				placeChest(level, player, removedPos);
				return List.of(getChest(level, remainingPos), getChest(level, removedPos)).stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing linked secondary main chest before settings split transfer")).getBlockPos();
			});
			openLinkedSecondaryChestSplitMenu(fixture.withMainPos(reopenedMainPos), true);
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				if (!(player.containerMenu instanceof StorageSettingsContainerMenu staleMenu)) {
					throw new IllegalStateException("Linked secondary settings menu closed before non-main split transfer");
				}
				LinkedStorageEndpointData endpoint = requireEndpoint(getChest(level, reopenedMainPos),
						"linked secondary main chest before settings split transfer");
				BlockPos remainingHalfPos = reopenedMainPos.equals(fixture.secondaryPos()) ? fixture.addedChestPos() : fixture.secondaryPos();
				level.destroyBlock(reopenedMainPos, false, player);
				require(player.containerMenu == player.inventoryMenu && player.containerMenu != staleMenu
						&& getChest(level, remainingHalfPos).getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& endpoint
								.equals(requireEndpoint(getChest(level, remainingHalfPos), "linked secondary remaining chest after settings split transfer")),
						"Linked secondary main-half split did not close the stale settings menu or retain its endpoint");
				return "";
			});
			waitForClosedStorageMenu();
		} finally {
			closeMenu();
			AutomationRuntime.runOnServer(player -> {
				teleportPlayer(player, fixture.originalPosition());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return "";
			});
		}
	}

	private static void openLinkedSecondaryChestSplitMenu(LinkedSecondaryChestSplitMenuFixture fixture, boolean openSettings) {
		AutomationRuntime.runOnServer(player -> {
			teleportPlayer(player, fixture.mainPos().getCenter());
			return "";
		});
		waitForClientPlayerPosition(fixture.mainPos().getCenter());
		waitForClientStorageBlock(fixture.mainPos());
		openLinkedStorageChestMenu(fixture.mainPos());
		waitForLinkedStorageMenu(fixture.slots(), fixture.groupName(), LinkedStorageEndpointRole.SECONDARY);
		if (openSettings) {
			AutomationRuntime.runOnClient(() -> {
				if (!(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Linked secondary inventory menu did not open before settings split transfer");
				}
				menu.openSettings();
				return "";
			});
			waitForLinkedStorageSettingsMenu(fixture.mainPos(), fixture.slots(), LinkedStorageEndpointRole.SECONDARY);
		}
	}

	private static String runOnServer(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 12);
		BlockPos secondaryPos = primaryPos.east(3);
		BlockPos limitedPos = primaryPos.south(3);
		BlockPos packedPos = limitedPos.east(3);
		BlockPos controllerPos = primaryPos.south(8);
		ItemStack originalMainHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
		float originalYRot = player.getYRot();
		float originalXRot = player.getXRot();

		clearArea(level, primaryPos);
		try {
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, limitedPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			ItemStack packedBarrel = new ItemStack(ModBlocks.BARREL_ITEM.get());
			WoodStorageBlockItem.setPacked(packedBarrel, true);
			placeBlock(level, player, packedPos, packedBarrel);
			placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));

			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			LimitedBarrelBlockEntity limited = getLimitedBarrel(level, limitedPos);
			BarrelBlockEntity packed = getBarrel(level, packedPos);
			packed.setPacked(true);
			require(!primary.isLinkedStorage() && !secondary.isLinkedStorage(), "Ordinary barrels must start unlinked");
			require(primary.isLinkedStorageCandidate() && secondary.isLinkedStorageCandidate(), "Ordinary barrels were not accepted as link candidates");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			primary.getStorageWrapper().setSortBy(SortBy.TAGS);
			configureMagnet(primary);
			secondary.getStorageWrapper().setSortBy(SortBy.MOD);

			ItemStack linker = new ItemStack(ENDER_LINKER.get());
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create an ordinary barrel linked-storage group");
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not add the secondary ordinary barrel endpoint");

			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "primary ordinary barrel");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "secondary ordinary barrel");
			boolean sameGroup = primaryEndpoint.groupId().equals(secondaryEndpoint.groupId());
			boolean distinctEndpointIds = !primaryEndpoint.endpointId().equals(secondaryEndpoint.endpointId());
			boolean primaryAndSecondary = LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(primaryEndpoint.groupId(),
					primaryEndpoint.endpointId())
					&& !LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(secondaryEndpoint.groupId(), secondaryEndpoint.endpointId());
			require(sameGroup && distinctEndpointIds && primaryAndSecondary, "Ordinary barrel endpoint identities did not form a primary/secondary group");
			require(primary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent()
					&& secondary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.SECONDARY).isPresent()
					&& limited.getStorageWrapper().getLinkedStorageEndpointRole().isEmpty(),
					"Storage render roles did not keep ordinary and primary endpoints active while suppressing secondary endpoints");
			ItemStack packingTape = new ItemStack(ModItems.PACKING_TAPE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, packingTape);
			InteractionResult packingTapeResult = player.gameMode.useItemOn(player, level, packingTape, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(primaryPos), Direction.UP, primaryPos, false));
			require(packingTapeResult.consumesAction() && primary.isLinkedStorage() && !primary.isPacked() && packingTape.getDamageValue() == 0,
					"Packing tape was not rejected without consuming durability for linked storage");
			require(count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7 && primary.getStorageWrapper().getSortBy() == SortBy.TAGS
					&& secondary.getStorageWrapper().getSortBy() == SortBy.TAGS
					&& secondary.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).is(ModItems.MAGNET_UPGRADE.get()),
					"Primary contents, settings, and upgrades did not migrate to the linked storage group");

			IItemHandler primaryCapability = requireCapability(level, primaryPos, "primary ordinary barrel");
			IItemHandler secondaryCapability = requireCapability(level, secondaryPos, "secondary ordinary barrel");
			require(secondaryCapability.insertItem(1, new ItemStack(Items.EMERALD, 3), false).isEmpty(),
					"Secondary ordinary barrel capability rejected a canonical inventory mutation");
			boolean canonicalMutationVisible = count(primary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
					&& count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7 && count(primaryCapability, Items.DIAMOND) == 7
					&& count(secondaryCapability, Items.DIAMOND) == 7 && count(primary.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3
					&& count(secondary.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3 && count(primaryCapability, Items.EMERALD) == 3
					&& count(secondaryCapability, Items.EMERALD) == 3;
			require(canonicalMutationVisible, "Canonical ordinary barrel inventory mutations were not visible through both wrappers and capabilities");

			secondary.getStorageWrapper().setSortBy(SortBy.COUNT);
			require(primary.getStorageWrapper().getSortBy() == SortBy.COUNT, "Secondary ordinary barrel did not update the canonical sort setting");
			secondary.toggleLock();
			IItemHandler lockedSecondaryCapability = requireCapability(level, secondaryPos, "locked secondary ordinary barrel");
			require(lockedSecondaryCapability.insertItem(2, new ItemStack(Items.REDSTONE), true).is(Items.REDSTONE)
					&& lockedSecondaryCapability.insertItem(0, new ItemStack(Items.DIAMOND), true).isEmpty()
					&& primaryCapability.insertItem(2, new ItemStack(Items.REDSTONE), true).isEmpty(),
					"Linked endpoint locks did not retain independent external input filtering");
			configureMagnet(secondary);
			require(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).is(ModItems.MAGNET_UPGRADE.get()),
					"Secondary ordinary barrel did not configure the canonical upgrade handler");
			var primaryData = new CompoundTag();
			var secondaryData = new CompoundTag();
			primary.saveAdditional(primaryData, level.registryAccess());
			secondary.saveAdditional(secondaryData, level.registryAccess());
			require(!primaryData.getCompound("storageWrapper").getCompound("renderInfo").isEmpty() && primaryData.getCompound("storageWrapper")
					.getCompound("renderInfo").equals(secondaryData.getCompound("storageWrapper").getCompound("renderInfo")),
					"Canonical upgrade render projection did not fan out to both linked barrels");
			boolean limitedBarrelEligible = limited.isLinkedStorageCandidate();
			boolean packedBarrelExcluded = !packed.isLinkedStorageCandidate() && useLinker(player, packedPos) == InteractionResult.FAIL
					&& !packed.isLinkedStorage();
			boolean controllerRejected = useLinker(player, controllerPos) == InteractionResult.PASS;
			require(limitedBarrelEligible, "Limited barrel was not accepted as an Ender Linker candidate");
			require(packedBarrelExcluded, "Packed barrel was not excluded from Ender Linker block use");
			require(controllerRejected, "Controller was not ignored by Ender Linker block use");
			return "{\"ok\":true,\"scenario\":\"ordinary_barrel_linked_storage\",\"linkedViaService\":true,\"sharedGroup\":true,\"distinctEndpointIds\":true,"
					+ "\"primaryContentsSettingsAndUpgradesMigrate\":true,\"secondaryLocalSettingsAreDiscarded\":true,"
					+ "\"canonicalMutationVisibleThroughWrappersAndCapabilities\":true,\"canonicalRenderProjectionFansOut\":true,"
					+ "\"secondarySkipsGlobalTick\":true,\"limitedBarrelEligible\":true,"
					+ "\"packedBarrelFailureFeedback\":true,\"linkedPackingTapeRejected\":true,\"controllerRejected\":true}";
		} finally {
			player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
			player.setYRot(originalYRot);
			player.setXRot(originalXRot);
			clearArea(level, primaryPos);
		}
	}

	private static void configureMagnet(BarrelBlockEntity primary) {
		configureMagnet(primary, Items.GOLD_INGOT);
	}

	private static void configureMagnet(BarrelBlockEntity primary, Item item) {
		UpgradeHandler upgrades = primary.getStorageWrapper().getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(ModItems.MAGNET_UPGRADE.get()));
		MagnetUpgradeWrapper magnet = upgrades.getWrappersThatImplement(MagnetUpgradeWrapper.class).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Ordinary barrel magnet upgrade was not initialized"));
		magnet.setPickupItems(true);
		magnet.setPickupXp(false);
		magnet.getFilterLogic().setDepositFilterType(ContentsFilterType.ALLOW);
		magnet.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(item));
		upgrades.saveInventory();
	}

	private static InteractionResult useLinker(ServerPlayer player, BlockPos pos) {
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		return useLinker(player, linker, pos);
	}

	private static InteractionResult useLinker(ServerPlayer player, ItemStack linker, BlockPos pos) {
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return ENDER_LINKER.get().onItemUseFirst(linker, new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
	}

	private static InteractionResult useLinkerAsPlayer(ServerPlayer player, ItemStack linker, BlockPos pos) {
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return player.gameMode.useItemOn(player, player.serverLevel(), linker, InteractionHand.MAIN_HAND, hitResult);
	}

	private static InteractionResult useTierUpgrade(ServerPlayer player, BlockPos pos, ItemStack upgrade) {
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return upgrade.getItem().onItemUseFirst(upgrade, new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
	}

	private static InteractionResult useTierUpgradeAsPlayer(ServerPlayer player, BlockPos pos, ItemStack upgrade) {
		player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return player.gameMode.useItemOn(player, player.serverLevel(), upgrade, InteractionHand.MAIN_HAND, hitResult);
	}

	private static IItemHandler requireCapability(ServerLevel level, BlockPos pos, String name) {
		IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
		if (handler == null) {
			throw new IllegalStateException("Missing item capability for " + name);
		}
		return handler;
	}

	private static LinkedStorageEndpointData requireEndpoint(StorageBlockEntity storage, String name) {
		LinkedStorageEndpointData endpoint = storage.getLinkedStorageEndpointData();
		if (endpoint == null) {
			throw new IllegalStateException("Missing linked-storage endpoint data for " + name);
		}
		return endpoint;
	}

	private static BarrelBlockEntity getBarrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing ordinary barrel at " + pos));
	}

	private static LimitedBarrelBlockEntity getLimitedBarrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing limited barrel at " + pos));
	}

	private static ChestBlockEntity getChest(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing chest at " + pos));
	}

	private static ShulkerBoxBlockEntity getShulkerBox(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing shulker box at " + pos));
	}

	private static void placeChest(ServerLevel level, ServerPlayer player, BlockPos pos) {
		placeChest(level, player, pos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
	}

	private static void placeChest(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		placeChest(level, player, pos, stack, 0);
	}

	private static void placeChest(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack, float yRot) {
		placeBlockAsPlayer(level, player, pos, stack, yRot);
	}

	private static void placeBlockAsPlayer(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack, float yRot) {
		Vec3 originalPosition = player.position();
		try {
			teleportPlayer(player, pos.above().getCenter());
			BlockPos supportPos = pos.below();
			level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
			player.setYRot(yRot);
			player.setXRot(0);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			InteractionResult result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false));
			require(result.consumesAction(), "Could not place regression fixture at " + pos + ": " + result);
		} finally {
			teleportPlayer(player, originalPosition);
		}
	}

	private static void placeBlock(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			throw new IllegalStateException("Regression fixture item is not placeable at " + pos);
		}
		level.setBlock(pos, blockItem.getBlock().defaultBlockState(), 3);
		blockItem.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), player, stack);
	}

	private static void teleportPlayer(ServerPlayer player, Vec3 position) {
		if (!player.teleportTo(player.serverLevel(), position.x, position.y, position.z, Set.of(), player.getYRot(), player.getXRot())) {
			throw new IllegalStateException("Could not position the player for the linked storage regression");
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
		List<ItemStack> contents = new ArrayList<>(handler.getSlots());
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
		for (int slot = 0; slot < contents.size(); slot++) {
			if (!ItemStack.matches(contents.get(slot), handler.getStackInSlot(slot))) {
				return false;
			}
		}
		return true;
	}

	private static ItemEntity requireDroppedItem(ServerLevel level, DroppedItemPickupFixture fixture) {
		return findDroppedItem(level, fixture, fixture.secondaryPos()).orElseThrow(() -> {
			int storedItems = count(getBarrel(level, fixture.primaryPos()).getStorageWrapper().getInventoryHandler(), fixture.item());
			String nearbyItemPositions = level
					.getEntitiesOfClass(ItemEntity.class, new AABB(fixture.primaryPos()).inflate(MAX_MAGNET_RANGE + 2.0D),
							itemEntity -> itemEntity.isAlive() && itemEntity.getItem().is(fixture.item()))
					.stream().map(itemEntity -> itemEntity.position().toString()).reduce((first, second) -> first + "," + second).orElse("none");
			return new IllegalStateException("Dropped pickup regression item disappeared from linked secondary barrel; canonical items=" + storedItems
					+ ", nearby positions=" + nearbyItemPositions);
		});
	}

	private static Optional<ItemEntity> findDroppedItem(ServerLevel level, DroppedItemPickupFixture fixture, BlockPos center) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(5.0D),
				itemEntity -> itemEntity.isAlive() && itemEntity.getItem().is(fixture.item())).stream().findFirst();
	}

	private static void waitForServerCondition(String description, java.util.function.Function<ServerPlayer, Boolean> condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(condition)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static void clearArea(ServerLevel level, BlockPos primaryPos) {
		for (int x = -1; x <= 8; x++) {
			for (int y = 0; y <= 1; y++) {
				for (int z = -1; z <= 9; z++) {
					BlockPos pos = primaryPos.offset(x, y, z);
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
						storage.clearContent();
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		AABB area = new AABB(primaryPos.getX() - 2, primaryPos.getY() - 1, primaryPos.getZ() - 2, primaryPos.getX() + 9, primaryPos.getY() + 2,
				primaryPos.getZ() + 10);
		level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
		createFixturePlatform(level, primaryPos);
	}

	private static void clearMagnetExclusionArea(ServerLevel level, BlockPos center) {
		for (int x = -MAX_MAGNET_RANGE; x <= MAX_MAGNET_RANGE; x++) {
			for (int y = 0; y <= 2; y++) {
				for (int z = -MAX_MAGNET_RANGE; z <= MAX_MAGNET_RANGE; z++) {
					BlockPos pos = center.offset(x, y, z);
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
						storage.clearContent();
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(MAX_MAGNET_RANGE + 1.0D)).forEach(ItemEntity::discard);
		createFixturePlatform(level, center);
	}

	private static void createFixturePlatform(ServerLevel level, BlockPos center) {
		for (int x = -FIXTURE_PLATFORM_RADIUS; x <= FIXTURE_PLATFORM_RADIUS; x++) {
			for (int z = -FIXTURE_PLATFORM_RADIUS; z <= FIXTURE_PLATFORM_RADIUS; z++) {
				level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for linked storage menu", e);
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}

	private record LinkedStorageMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, int expectedSlots, ItemStack originalMainHand,
			float originalYRot, float originalXRot, Vec3 originalPosition, String groupName) {
	}

	private record LimitedBarrelReloadStatus(LinkedStorageEndpointData primaryEndpoint, LinkedStorageEndpointData secondaryEndpoint) {
	}

	private record LinkedLimitedBarrelMenuFixture(BlockPos primaryPos, ItemStack originalMainHand, float originalYRot, float originalXRot,
			Vec3 originalPosition) {
	}

	private record LinkedUpgradeSwitchFixture(BlockPos primaryPos, int playerUpgradeSlot, ItemStack originalUpgradeSlot, ItemStack originalMainHand,
			float originalYRot, float originalXRot, Vec3 originalPosition) {
	}

	private record LinkedPrimaryChestExpansionFixture(BlockPos originalPrimaryPos, BlockPos addedChestPos, LinkedStorageEndpointData primaryEndpoint,
			String groupName, ItemStack originalMainHand, float originalYRot, float originalXRot, Vec3 originalPosition) {
	}

	private record LinkedSecondaryChestSplitMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, BlockPos addedChestPos, BlockPos mainPos, int slots,
			String groupName, Vec3 originalPosition) {
		private LinkedSecondaryChestSplitMenuFixture withMainPos(BlockPos newMainPos) {
			return new LinkedSecondaryChestSplitMenuFixture(primaryPos, secondaryPos, addedChestPos, newMainPos, slots, groupName, originalPosition);
		}
	}

	private record LinkedControllerOutlineFixture(BlockPos controllerPos, BlockPos primaryPos, BlockPos secondaryPos) {
	}

	private record DroppedItemPickupFixture(BlockPos primaryPos, BlockPos secondaryPos, BlockPos ordinaryPos, Item item, long verifyAfterGameTime) {
	}

	private record LinkedStorageStackTooltipFixture(BlockPos primaryPos, UUID groupId, int inventorySlot, ItemStack originalStack, GameType originalGameMode) {
	}

	private record LinkedStorageTierUpgradeFixture(BlockPos primaryPos, BlockPos secondaryPos, LinkedStorageEndpointData primaryEndpoint,
			LinkedStorageEndpointData secondaryEndpoint, int originalSlots, int expectedSlots, int expectedUpgradeSlots, String groupName,
			Vec3 originalPosition, GameType originalGameMode) {
	}

	private record DroppedPrimaryRenameFixture(BlockPos primaryPos, BlockPos secondaryPos, LinkedStorageEndpointData endpoint, int expectedSlots,
			GameType originalGameMode, ItemStack originalMainHand, float originalYRot, float originalXRot, Vec3 originalPosition, String groupName,
			String originalGroupName, String secondaryName, long secondaryVerificationGameTime, ItemStack renamedPrimary, ItemStack renamedSecondary) {
	}
}
