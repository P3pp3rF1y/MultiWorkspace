package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointProvider;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.LinkedStorageTooltip;
import net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageItemClient;
import net.p3pp3rf1y.sophisticatedstorage.network.OpenStorageInventoryPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

/** Exercises linked storage through the same block interactions a player uses in-game. */
public final class StorageLinkedStorageRegression {
	private static final int LINKED_LIMITED_RELOAD_ITEM_COUNT = 23;
	private static final int CLIENT_CONTROLLER_OUTLINE_CONVERGENCE_SECONDS = 10;

	private StorageLinkedStorageRegression() {
	}

	public static String run() {
		LinkedStorageFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runOnServer);
		try {
			runCanonicalStorageTypeInsertionRulesRegression();
			runLinkedShulkerStashRegression();
			runCreativeEndpointPlacementRegression();
			runLinkedUpgradeSwitchRefreshRegression();
			runComponentlessBarrelItemModelRegression();
			runEndpointUnloadReloadRegression();
			runLinkedStorageMenuTransitionRegression();
			runDroppedPrimaryRenameRegression();
			openLinkedStorageMenu(fixture.primaryPos());
			waitForClientCanonicalContents(fixture);
			runLinkedStorageMenuCanonicalSnapshotRegression();
			runSecondaryCapabilityMutationRegression();
			runLinkedLimitedBarrelOpenMenuMemoryRegression();
			runLinkedStorageStackTooltipRegression();
			runDroppedItemPickupRegression();
			runLinkedPrimaryChestExpansionRegression();
			runLinkedSecondaryChestSplitRegression();
			runLinkedControllerEndpointJoinDoesNotDuplicateCanonicalContentsRegression();
			runDoubleChestControllerLockRegression();
			runLinkedStorageControllerRegression();
			runLockedLinkedControllerRoutingRegression();
			runLinkedControllerNonListenerRemovalIndexesRegression();
			runLinkedControllerRemovalReconnectRegression();
			runLinkedControllerBridgeReconnectRegression();
			runLinkedControllerPartialGroupDisconnectRegression();
			runLinkedControllerTierUpgradeRegression();
			runLinkedControllerTierUpgradeRequiresPrimaryRegression();
			runLinkedStorageTierUpgradeRegression();
			runLinkedStorageTierUpgradeMenuInvalidationRegression();
			runLinkedControllerPaintbrushRegression();
			runLinkedControllerClientOutlineRegression();
			StoragePreviewScenarios.runOpenSettingsSelectionRegression();
			return "{\"ok\":true,\"playerActions\":true,\"standardStorageTypesInterlink\":true,\"canonicalStorageTypeControlsInsertionRules\":true,"
					+ "\"limitedBarrelsRequireMatchingSlotFamily\":true,\"contentsSettingsAndUpgradesShared\":true,"
					+ "\"secondaryTierUpgradeRejected\":true,\"linkedPackingRejected\":true,"
					+ "\"linkedEndpointRestored\":true,\"clientUsesDirectCoreContents\":true," + "\"linkedStorageMenuReceivesCanonicalSnapshot\":true,"
					+ "\"secondaryCapabilityMutation\":true,\"limitedBarrelOpenMenuMemorySync\":true,\"limitedBarrelMemoryPersistsAfterEmptying\":true,"
					+ "\"linkedStackTooltipCache\":true,\"secondaryPickupSuppressed\":true,"
					+ "\"linkedPrimaryChestExpansionLifecycle\":true,\"linkedSecondaryChestSplitLifecycle\":true,"
					+ "\"linkedControllerEndpointJoinKeepsOneCanonicalContentIndex\":true,"
					+ "\"incompatibleFamiliesRejected\":true,\"linkedShulkerStashAndNestedRejection\":true,\"creativeSecondaryPlacement\":true,"
					+ "\"upgradeSwitchRefresh\":true,\"componentlessModelIsolation\":true,\"endpointReattachment\":true,\"staleMenuActionsRejected\":true,"
					+ "\"primaryRenameIsolation\":true,\"nonListenerRemovalIndexes\":true,\"bridgeAndPartialTopologyReconnect\":true,"
					+ "\"linkedControllerRoutingAndToolOperations\":true,\"lockedLinkedControllerRoutingMatrix\":true,\"linkedControllerReconnects\":true,"
					+ "\"linkedControllerTierUpgradeRequiresPrimaryEndpoint\":true,\"directLinkedTierUpgradeRoute\":true,"
					+ "\"primaryOpenTierUpgradeRejected\":true,\"linkedTierUpgradeInvalidatesSecondaryMenu\":true,"
					+ "\"linkedControllerClientOutlineIncludesEveryGroupMember\":true}";
		} finally {
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.serverLevel(), fixture.anchor());
				return true;
			});
		}
	}

	private static void runLinkedStorageMenuCanonicalSnapshotRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedStorageMenuFixture);
		try {
			movePlayerNear(fixture.secondaryPos(), fixture.playerState());
			waitForClientStorageBlock(fixture.secondaryPos());
			openStorageMenu(fixture.secondaryPos());
			waitForClientLinkedStorageMenuCanonicalSnapshot(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			closeStorageMenu();
			waitForClosedStorageMenu();
			movePlayerNear(fixture.primaryPos(), fixture.playerState());
			waitForClientStorageBlock(fixture.primaryPos());
			openStorageMenu(fixture.primaryPos());
			waitForClientLinkedStorageMenuCanonicalSnapshot(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.serverLevel(), fixture.anchor());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
			waitForClientPlayerState(fixture.playerState());
		}
	}

	private static void runCanonicalStorageTypeInsertionRulesRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 32);
			BlockPos secondaryPos = primaryPos.east(3);
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				ShulkerBoxBlockEntity shulkerPrimary = getShulker(level, primaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction() && useLinkerAsPlayer(player, linker, secondaryPos).consumesAction(),
						"Could not create linked storage with a canonical shulker and physical barrel secondary");
				IItemHandler barrelCapability = requireItemCapability(level, secondaryPos, "physical barrel secondary");
				ItemStack rejected = barrelCapability.insertItem(0, new ItemStack(Items.SHULKER_BOX), false);
				require(rejected.is(Items.SHULKER_BOX) && rejected.getCount() == 1 && count(shulkerPrimary, Items.SHULKER_BOX) == 0
						&& count(barrelCapability, Items.SHULKER_BOX) == 0, "Physical barrel secondary bypassed canonical shulker insertion rules");

				clearArea(level, primaryPos);
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
				BarrelBlockEntity barrelPrimary = getBarrel(level, primaryPos);
				linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction() && useLinkerAsPlayer(player, linker, secondaryPos).consumesAction(),
						"Could not create linked storage with a canonical barrel and physical shulker secondary");
				IItemHandler shulkerCapability = requireItemCapability(level, secondaryPos, "physical shulker secondary");
				ItemStack remainder = shulkerCapability.insertItem(0, new ItemStack(Items.SHULKER_BOX), false);
				require(remainder.isEmpty() && count(barrelPrimary, Items.SHULKER_BOX) == 1 && count(shulkerCapability, Items.SHULKER_BOX) == 1,
						"Physical shulker secondary did not apply canonical barrel insertion rules exactly once");
				return true;
			} finally {
				clearArea(level, primaryPos);
				restorePlayerState(player, playerState);
			}
		});
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
				ShulkerBoxBlockEntity shulker = getShulker(level, shulkerPos);
				BarrelBlockEntity barrel = getBarrel(level, barrelPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, shulker) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, barrel) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create a Shulker-primary linked storage group for stash regression");
				require(shulker.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
						"Linked Shulker stash regression did not retain a Shulker primary");
				ItemStack carrier = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
				shulker.copyLinkedStorageEndpointTo(carrier);
				ShulkerBoxItem shulkerItem = (ShulkerBoxItem) carrier.getItem();
				require(shulkerItem.stash(level.registryAccess(), carrier, new ItemStack(Items.DIAMOND, 7), false).isEmpty()
						&& count(shulker, Items.DIAMOND) == 7 && count(barrel, Items.DIAMOND) == 7,
						"Stashing into a linked Shulker carrier did not insert into canonical linked contents");
				ItemStack nestedShulker = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
				require(shulkerItem.stash(level.registryAccess(), carrier, nestedShulker, true).getCount() == 1
						&& shulkerItem.stash(level.registryAccess(), carrier, nestedShulker, false).getCount() == 1
						&& count(shulker, ModBlocks.SHULKER_BOX_ITEM.get()) == 0, "Shulker-primary linked storage accepted a nested Shulker through stash");
			} finally {
				clearArea(level, shulkerPos);
			}
			return true;
		});
	}

	private static void runCreativeEndpointPlacementRegression() {
		PlayerState playerState = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos sourcePos = player.blockPosition().offset(0, 0, 60);
			BlockPos creativePos = sourcePos.east(3);
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			PlayerState originalState = capturePlayerState(player);
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
			} finally {
				player.setGameMode(originalGameMode);
				restorePlayerState(player, originalState);
				clearArea(level, sourcePos);
				clearArea(level, creativePos);
			}
			return originalState;
		});
		waitForClientPlayerState(playerState);
	}

	private static void runLinkedUpgradeSwitchRefreshRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 40);
			BlockPos secondaryPos = primaryPos.east(3);
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()));
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction() && useLinkerAsPlayer(player, linker, secondaryPos).consumesAction(),
						"Could not create linked storage for upgrade-switch refresh regression");
				player.getInventory().setItem((player.getInventory().selected + 1) % 9, new ItemStack(ModItems.ADVANCED_FILTER_UPGRADE.get()));
				return new LinkedStorageMenuFixture(primaryPos, primaryPos, secondaryPos,
						requireEndpoint(getBarrel(level, primaryPos), "upgrade primary").groupId(), 27, "", playerState);
			} catch (RuntimeException e) {
				clearArea(level, primaryPos);
				throw e;
			}
		});
		try {
			movePlayerNear(fixture.primaryPos(), fixture.playerState());
			waitForClientStorageBlock(fixture.primaryPos());
			AutomationRuntime.runOnServer(player -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				require(player.gameMode.useItemOn(player, player.serverLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()), Direction.UP, fixture.primaryPos(), false)).consumesAction(),
						"Could not open linked storage through normal interaction for upgrade-switch refresh regression");
				return true;
			});
			AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Linked storage menu was unavailable for upgrade-switch refresh regression");
				}
				int inventorySlot = menu.slots.stream().filter(slot -> slot.getItem().is(ModItems.ADVANCED_FILTER_UPGRADE.get())).findFirst()
						.orElseThrow(() -> new IllegalStateException("Advanced Filter Upgrade was not present in player inventory")).index;
				Minecraft.getInstance().gameMode.handleInventoryMouseClick(menu.containerId, inventorySlot, 0, ClickType.PICKUP,
						Minecraft.getInstance().player);
				Minecraft.getInstance().gameMode.handleInventoryMouseClick(menu.containerId, menu.getFirstUpgradeSlot(), 0, ClickType.PICKUP,
						Minecraft.getInstance().player);
				return true;
			});
			waitForClientLinkedUpgradeSwitch(fixture.primaryPos());
		} finally {
			closeStorageMenu();
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.serverLevel(), fixture.anchor());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
			waitForClientPlayerState(fixture.playerState());
		}
	}

	private static void runComponentlessBarrelItemModelRegression() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			ItemStack componentlessBarrel = new ItemStack(ModBlocks.BARREL_ITEM.get());
			resolveBarrelItemModel(minecraft, componentlessBarrel);
			ItemStack tintedBarrel = new ItemStack(ModBlocks.BARREL_ITEM.get());
			StorageBlockItem barrelItem = (StorageBlockItem) tintedBarrel.getItem();
			barrelItem.setMainColor(tintedBarrel, 0xFF00FF);
			barrelItem.setAccentColor(tintedBarrel, 0x00FFFF);
			resolveBarrelItemModel(minecraft, tintedBarrel);
			resolveBarrelItemModel(minecraft, componentlessBarrel);
			require(componentlessBarrel.get(ModCoreDataComponents.MAIN_COLOR) == null && componentlessBarrel.get(ModCoreDataComponents.ACCENT_COLOR) == null,
					"A componentless barrel item inherited the previous barrel render state");
			return true;
		});
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
						"Could not create the endpoint reload linked-storage group");
				LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "endpoint reload primary");
				LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "endpoint reload secondary");
				CompoundTag secondaryData = new CompoundTag();
				secondary.saveAdditional(secondaryData, level.registryAccess());
				secondary.onChunkUnloaded();
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.LAPIS_LAZULI, 5));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				level.removeBlockEntity(secondaryPos);
				BarrelBlockEntity reloadedSecondary = new BarrelBlockEntity(secondaryPos, level.getBlockState(secondaryPos));
				reloadedSecondary.loadAdditional(secondaryData, level.registryAccess());
				level.setBlockEntity(reloadedSecondary);
				reloadedSecondary.onLoad();
				require(secondaryEndpoint.equals(requireEndpoint(reloadedSecondary, "reloaded secondary")) && count(reloadedSecondary, Items.LAPIS_LAZULI) == 5,
						"Reloaded secondary did not reclaim its endpoint identity and canonical contents");
				CompoundTag primaryData = new CompoundTag();
				primary.saveAdditional(primaryData, level.registryAccess());
				primary.onChunkUnloaded();
				level.removeBlockEntity(primaryPos);
				BarrelBlockEntity reloadedPrimary = new BarrelBlockEntity(primaryPos, level.getBlockState(primaryPos));
				reloadedPrimary.loadAdditional(primaryData, level.registryAccess());
				level.setBlockEntity(reloadedPrimary);
				reloadedPrimary.onLoad();
				reloadedSecondary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.AMETHYST_SHARD, 4));
				reloadedSecondary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(primaryEndpoint.equals(requireEndpoint(reloadedPrimary, "reloaded primary")) && count(reloadedPrimary, Items.LAPIS_LAZULI) == 5
						&& count(reloadedPrimary, Items.AMETHYST_SHARD) == 4, "Reloaded primary did not reclaim canonical linked contents");
				return true;
			} finally {
				clearArea(level, primaryPos);
			}
		});
	}

	private static void runLinkedStorageMenuTransitionRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedStorageMenuFixture);
		try {
			movePlayerNear(fixture.secondaryPos(), fixture.playerState());
			waitForClientStorageBlock(fixture.secondaryPos());
			openStorageMenu(fixture.secondaryPos());
			waitForClientStorageMenu(fixture.secondaryPos(), fixture.inventorySlots(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnServer(player -> {
				StorageContainerMenu menu = (StorageContainerMenu) player.containerMenu;
				CompoundTag staleAction = new CompoundTag();
				staleAction.putString("action", "openSettings");
				staleAction.putInt("sourceContainerId", menu.containerId + 1);
				menu.handlePacket(staleAction);
				require(player.containerMenu == menu, "Stale linked storage settings action replaced the active menu");
				return true;
			});
			AutomationRuntime.runOnClient(() -> {
				((StorageContainerMenu) Minecraft.getInstance().player.containerMenu).openSettings();
				return true;
			});
			waitForServerCondition("linked storage settings transition", player -> player.containerMenu instanceof StorageSettingsContainerMenu settings
					&& settings.getBlockPosition().equals(fixture.secondaryPos()));
			AutomationRuntime.runOnClient(() -> {
				PacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.primaryPos()));
				return true;
			});
			waitForServerCondition("mismatched linked storage inventory payload rejection",
					player -> player.containerMenu instanceof StorageSettingsContainerMenu settings
							&& settings.getBlockPosition().equals(fixture.secondaryPos()));
			AutomationRuntime.runOnClient(() -> {
				PacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.secondaryPos()));
				return true;
			});
			waitForClientStorageMenu(fixture.secondaryPos(), fixture.inventorySlots(), LinkedStorageEndpointRole.SECONDARY);
		} finally {
			closeStorageMenu();
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.serverLevel(), fixture.anchor());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
		}
	}

	private static void runDroppedPrimaryRenameRegression() {
		DroppedPrimaryRenameFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 108);
			BlockPos secondaryPos = primaryPos.east(3);
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			try {
				placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create dropped-primary rename group");
				LinkedStorageEndpointData endpoint = requireEndpoint(primary, "dropped primary rename");
				String originalName = getLinkedStorageGroupName(level, endpoint);
				level.destroyBlock(primaryPos, true, player);
				ItemStack renamedPrimary = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos)).stream()
						.filter(item -> endpoint.equals(item.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst().orElseThrow().getItem()
						.copy();
				renamedPrimary.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed Linked Primary"));
				level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos)).forEach(ItemEntity::discard);
				level.destroyBlock(secondaryPos, true, player);
				ItemStack renamedSecondary = level
						.getEntitiesOfClass(ItemEntity.class, new AABB(secondaryPos)).stream().filter(item -> StorageBlockEntity
								.getLinkedStorageEndpointData(item.getItem()).map(data -> data.groupId().equals(endpoint.groupId())).orElse(false))
						.findFirst().orElseThrow().getItem().copy();
				renamedSecondary.set(DataComponents.CUSTOM_NAME, Component.literal("Secondary Stack Name"));
				player.setItemInHand(InteractionHand.MAIN_HAND, renamedSecondary);
				require(getLinkedStorageGroupName(level, endpoint).equals(originalName), "Renaming a dropped secondary changed the linked group name");
				return new DroppedPrimaryRenameFixture(primaryPos, secondaryPos, endpoint, originalName, originalGameMode, playerState, renamedPrimary,
						renamedSecondary);
			} catch (RuntimeException e) {
				restorePlayerState(player, playerState);
				clearArea(level, primaryPos);
				throw e;
			}
		});
		try {
			AutomationRuntime.runOnServer(player -> {
				player.setGameMode(GameType.SURVIVAL);
				return true;
			});
			waitForServerCondition("renamed linked secondary barrel to leave the group name unchanged",
					player -> getLinkedStorageGroupName(player.serverLevel(), fixture.endpoint()).equals(fixture.originalName()));
			AutomationRuntime.runOnServer(player -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.renamedPrimary());
				return true;
			});
			waitForServerCondition("renamed dropped primary barrel to update the linked group name",
					player -> getLinkedStorageGroupName(player.serverLevel(), fixture.endpoint()).equals("Renamed Linked Primary"));
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				placeBlock(level, player, fixture.secondaryPos(), fixture.renamedSecondary());
				return true;
			});
			waitForServerCondition("re-placed renamed secondary barrel to restore its endpoint",
					player -> requireEndpoint(getBarrel(player.serverLevel(), fixture.secondaryPos()), "re-placed renamed secondary").groupId()
							.equals(fixture.endpoint().groupId()));
			AutomationRuntime.runOnServer(player -> {
				placeBlock(player.serverLevel(), player, fixture.primaryPos(), fixture.renamedPrimary());
				return true;
			});
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				BarrelBlockEntity restoredPrimary = getBarrel(level, fixture.primaryPos());
				require(fixture.endpoint().equals(requireEndpoint(restoredPrimary, "replaced renamed primary")),
						"Re-placed renamed primary barrel did not restore its endpoint identity");
				require(restoredPrimary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.PRIMARY)
						.isPresent(), "Re-placed renamed primary barrel did not restore its primary endpoint role");
				require(getLinkedStorageGroupName(level, fixture.endpoint()).equals("Renamed Linked Primary"),
						"Primary rename did not remain canonical after endpoint replacement");
				return true;
			});
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.setGameMode(fixture.originalGameMode());
				restorePlayerState(player, fixture.playerState());
				clearArea(player.serverLevel(), fixture.primaryPos());
				return true;
			});
			waitForClientPlayerState(fixture.playerState());
		}
	}

	private static String getLinkedStorageGroupName(ServerLevel level, LinkedStorageEndpointData endpoint) {
		return LinkedStorageGroupsSavedData.get(level).manager().resolveVirtualHost(endpoint.groupId())
				.flatMap(ILinkedStorageVirtualHost::getLinkedStorageDisplayName).map(Component::getString)
				.orElseThrow(() -> new IllegalStateException("Missing linked storage host for " + endpoint.groupId()));
	}

	private static void resolveBarrelItemModel(Minecraft minecraft, ItemStack stack) {
		ItemStackRenderState renderState = new ItemStackRenderState();
		minecraft.getItemModelResolver().updateForTopItem(renderState, stack, ItemDisplayContext.GUI, false, minecraft.level, minecraft.player, 0);
		if (renderState.isEmpty()) {
			throw new IllegalStateException("Barrel item model resolver did not produce a render model");
		}
	}

	private static LinkedStorageMenuFixture setupLinkedStorageMenuFixture(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos anchor = player.blockPosition().offset(0, 0, 60);
		BlockPos primaryPos = anchor;
		BlockPos secondaryPos = anchor.east(3);
		GameType originalGameType = player.gameMode.getGameModeForPlayer();
		PlayerState playerState = capturePlayerState(player);
		clearArea(level, anchor);
		try {
			player.setGameMode(GameType.SURVIVAL);
			placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			primary.setCustomName(Component.literal("Linked storage canonical snapshot"));
			ItemStack linker = new ItemStack(ENDER_LINKER.get());
			require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction() && useLinkerAsPlayer(player, linker, secondaryPos).consumesAction(),
					"Could not link barrels for the linked storage menu snapshot");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "menu snapshot primary barrel");
			require(primaryEndpoint.groupId().equals(requireEndpoint(secondary, "menu snapshot secondary barrel").groupId())
					&& count(secondary, Items.DIAMOND) == 7, "Linked barrels did not share the canonical menu snapshot contents");
			return new LinkedStorageMenuFixture(anchor, primaryPos, secondaryPos, primaryEndpoint.groupId(),
					primary.getStorageWrapper().getInventoryHandler().getSlots(), primary.getMenuDisplayName().getString(), playerState);
		} catch (RuntimeException e) {
			clearArea(level, anchor);
			throw e;
		} finally {
			player.setGameMode(originalGameType);
			restorePlayerState(player, playerState);
		}
	}

	private static void waitForClientLinkedUpgradeSwitch(BlockPos expectedPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
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

	private static void waitForClientLinkedStorageMenuCanonicalSnapshot(LinkedStorageMenuFixture fixture, BlockPos openedPos,
			LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null
					&& Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu menu
					&& screen.getTitle().getString().equals(fixture.title()) && menu.getBlockPosition().equals(Optional.of(openedPos))
					&& Minecraft.getInstance().level.getBlockEntity(openedPos) instanceof BarrelBlockEntity barrel
					&& barrel.getLinkedStorageEndpointData() != null && fixture.groupId().equals(barrel.getLinkedStorageEndpointData().groupId())
					&& barrel.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
					&& provider.getLinkedStorageEndpoint().filter(endpoint -> fixture.groupId().equals(endpoint.groupId())).isPresent()
					&& provider.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent()
					&& menu.getStorageBlockEntity().getLinkedStorageEndpointData() != null
					&& fixture.groupId().equals(menu.getStorageBlockEntity().getLinkedStorageEndpointData().groupId())
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider menuProvider
					&& menuProvider.getLinkedStorageEndpoint().filter(endpoint -> fixture.groupId().equals(endpoint.groupId())).isPresent()
					&& menuProvider.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent()
					&& menu.getStorageWrapper().getInventoryHandler().getSlots() == fixture.inventorySlots()
					&& menu.getNumberOfStorageInventorySlots() == fixture.inventorySlots() && menu.getSlot(0).getItem().is(Items.DIAMOND)
					&& menu.getSlot(0).getItem().getCount() == 7 && count(menu, Items.DIAMOND) == 7)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked StorageContainerMenu did not receive the canonical client snapshot");
	}

	private static void runSecondaryCapabilityMutationRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 68);
			BlockPos secondaryPos = primaryPos.east(3);
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction() && useLinkerAsPlayer(player, linker, secondaryPos).consumesAction(),
						"Could not link barrels for capability mutation");
				IItemHandler primaryItems = requireItemCapability(level, primaryPos, "primary linked barrel");
				IItemHandler secondaryItems = requireItemCapability(level, secondaryPos, "secondary linked barrel");
				require(secondaryItems.insertItem(0, new ItemStack(Items.EMERALD, 3), false).isEmpty(),
						"Secondary linked barrel capability rejected insertion");
				require(count(primaryItems, Items.EMERALD) == 3 && count(secondaryItems, Items.EMERALD) == 3 && count(primary, Items.EMERALD) == 3
						&& count(secondary, Items.EMERALD) == 3, "Secondary capability mutation did not update canonical linked contents");
				return true;
			} finally {
				clearArea(level, primaryPos);
				restorePlayerState(player, playerState);
			}
		});
	}

	private static void runLinkedLimitedBarrelOpenMenuMemoryRegression() {
		LinkedLimitedMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 84);
			BlockPos secondaryPos = primaryPos.east(3);
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
				LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction() && useLinkerAsPlayer(player, linker, secondaryPos).consumesAction(),
						"Could not link limited barrels for open-menu memory sync");
				primary.toggleLock();
				return new LinkedLimitedMenuFixture(primaryPos, secondaryPos, playerState);
			} catch (RuntimeException e) {
				clearArea(level, primaryPos);
				throw e;
			} finally {
				restorePlayerState(player, playerState);
			}
		});
		try {
			movePlayerNear(fixture.secondaryPos(), fixture.playerState());
			waitForClientStorageBlock(fixture.secondaryPos());
			openStorageMenu(fixture.secondaryPos());
			waitForClientStorageMenu(fixture.secondaryPos(), 1, LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnServer(player -> {
				LimitedBarrelBlockEntity primary = getLimitedBarrel(player.serverLevel(), fixture.primaryPos());
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 23));
				require(primary.depositItem(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), 0),
						"Could not deposit into locked linked limited barrel");
				MemorySettingsCategory primaryMemory = primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				MemorySettingsCategory secondaryMemory = getLimitedBarrel(player.serverLevel(), fixture.secondaryPos()).getStorageWrapper().getSettingsHandler()
						.getTypeCategory(MemorySettingsCategory.class);
				require(isDiamondMemory(primaryMemory) && isDiamondMemory(secondaryMemory),
						"Linked limited barrel memory did not synchronize between wrappers");
				IItemHandler inventory = primary.getStorageWrapper().getInventoryHandler();
				require(inventory.extractItem(0, LINKED_LIMITED_RELOAD_ITEM_COUNT, false).is(Items.DIAMOND) && inventory.getStackInSlot(0).isEmpty()
						&& isDiamondMemory(primaryMemory) && isDiamondMemory(secondaryMemory),
						"Linked limited barrel memory did not persist after its canonical inventory was emptied");
				return true;
			});
			waitForClientLimitedBarrelMemory(fixture.secondaryPos());
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.serverLevel(), fixture.primaryPos());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
			waitForClientPlayerState(fixture.playerState());
		}
	}

	private static void runLinkedStorageStackTooltipRegression() {
		LinkedStackFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos pos = player.blockPosition().offset(0, 0, 92);
			int inventorySlot = 8;
			ItemStack originalStack = player.getInventory().getItem(inventorySlot).copy();
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, pos);
			try {
				placeBlockAsPlayer(level, player, pos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity barrel = getBarrel(level, pos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, pos).consumesAction(), "Could not create linked tooltip endpoint");
				LinkedStorageEndpointData endpoint = requireEndpoint(barrel, "tooltip primary barrel");
				barrel.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				barrel.getStorageWrapper().getInventoryHandler().saveInventory();
				level.destroyBlock(pos, true, player);
				ItemStack dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5D)).stream()
						.filter(entity -> endpoint.equals(entity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst()
						.map(entity -> entity.getItem().copy()).orElseThrow(() -> new IllegalStateException("Missing dropped linked tooltip stack"));
				require(StorageBlockEntity.getLinkedStorageEndpointRole(dropped).filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
						"Dropped linked stack did not retain its primary role");
				level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5D)).forEach(ItemEntity::discard);
				player.getInventory().setItem(inventorySlot, dropped);
				player.inventoryMenu.broadcastChanges();
				return new LinkedStackFixture(pos, endpoint.groupId(), inventorySlot, originalStack, playerState);
			} catch (RuntimeException e) {
				player.getInventory().setItem(inventorySlot, originalStack);
				clearArea(level, pos);
				restorePlayerState(player, playerState);
				throw e;
			}
		});
		try {
			waitForClientLinkedStack(fixture);
			AutomationRuntime.runOnClient(() -> {
				ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot());
				require(StorageItemClient.getTooltipImage(stack) instanceof LinkedStorageTooltip tooltip && tooltip.groupId().equals(fixture.groupId())
						&& tooltip.role() == LinkedStorageEndpointRole.PRIMARY, "Dropped linked stack did not expose its linked tooltip");
				return true;
			});
			waitForClientLinkedStackContents(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().setItem(fixture.inventorySlot(), fixture.originalStack());
				clearArea(player.serverLevel(), fixture.pos());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
		}
	}

	private static void runDroppedItemPickupRegression() {
		DroppedItemFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 100);
			BlockPos secondaryPos = primaryPos.east(21);
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			clearArea(level, secondaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction() && useLinkerAsPlayer(player, linker, secondaryPos).consumesAction(),
						"Could not link pickup barrels");
				configureMagnet(primary);
				require(primary.getBlockState().getValue(StorageBlockBase.TICKING)
						&& primary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent()
						&& !secondary.getBlockState().getValue(StorageBlockBase.TICKING)
						&& secondary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.SECONDARY).isPresent(),
						"Linked pickup endpoints did not retain primary ticking and secondary non-ticking roles");
				Vec3 dropPos = secondaryPos.getCenter().add(0, -0.25D, 0);
				ItemEntity dropped = new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, new ItemStack(Items.ENDER_PEARL, 2));
				dropped.setNoGravity(true);
				dropped.setDeltaMovement(Vec3.ZERO);
				require(level.addFreshEntity(dropped), "Could not spawn linked pickup test item");
				return new DroppedItemFixture(primaryPos, secondaryPos, dropped.getUUID(), level.getGameTime() + 20, playerState);
			} catch (RuntimeException e) {
				clearArea(level, primaryPos);
				clearArea(level, secondaryPos);
				restorePlayerState(player, playerState);
				throw e;
			}
		});
		try {
			waitForServerCondition("secondary pickup suppression",
					player -> player.serverLevel().getGameTime() >= fixture.verifyTime()
							&& player.serverLevel().getEntity(fixture.entityId()) instanceof ItemEntity item && item.getItem().getCount() == 2
							&& count(getBarrel(player.serverLevel(), fixture.primaryPos()), Items.ENDER_PEARL) == 0);
			AutomationRuntime.runOnServer(player -> {
				ItemEntity dropped = (ItemEntity) player.serverLevel().getEntity(fixture.entityId());
				require(dropped != null, "Secondary pickup test item disappeared");
				dropped.setPos(fixture.primaryPos().getCenter().add(0, -0.25D, 0));
				dropped.setPickUpDelay(0);
				return true;
			});
			waitForServerCondition("primary pickup", player -> player.serverLevel().getEntity(fixture.entityId()) == null
					&& count(getBarrel(player.serverLevel(), fixture.primaryPos()), Items.ENDER_PEARL) == 2);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.serverLevel(), fixture.primaryPos());
				clearArea(player.serverLevel(), fixture.secondaryPos());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
		}
	}

	private static void runLinkedPrimaryChestExpansionRegression() {
		runLinkedPrimaryChestExpansionRegression(Direction.EAST, 108);
		runLinkedPrimaryChestExpansionRegression(Direction.WEST, 116);
	}

	private static void runLinkedPrimaryChestExpansionRegression(Direction addedChestDirection, int zOffset) {
		LinkedChestFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, zOffset);
			BlockPos addedPos = primaryPos.relative(addedChestDirection);
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				ChestBlockEntity primary = getChest(level, primaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction(), "Could not create linked primary chest");
				primary.setCustomName(Component.literal("Expanded linked primary"));
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				return new LinkedChestFixture(primaryPos, addedPos, requireEndpoint(primary, "single primary chest"), playerState);
			} catch (RuntimeException e) {
				clearArea(level, primaryPos);
				throw e;
			} finally {
				restorePlayerState(player, playerState);
			}
		});
		try {
			movePlayerNear(fixture.primaryPos(), fixture.playerState());
			waitForClientStorageBlock(fixture.primaryPos());
			openStorageMenu(fixture.primaryPos());
			waitForClientStorageMenu(fixture.primaryPos(), 27, LinkedStorageEndpointRole.PRIMARY);
			BlockPos expandedPrimaryPos = AutomationRuntime.runOnServer(player -> {
				require(player.containerMenu instanceof StorageContainerMenu menu && menu.getNumberOfStorageInventorySlots() == 27,
						"Linked primary chest did not have its 27-slot menu open before expansion");
				placeBlockAsPlayer(player.serverLevel(), player, fixture.addedPos(), new ItemStack(ModBlocks.CHEST_ITEM.get()));
				List<ChestBlockEntity> parts = List.of(getChest(player.serverLevel(), fixture.primaryPos()),
						getChest(player.serverLevel(), fixture.addedPos()));
				ChestBlockEntity expandedPrimary = parts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after linked primary expansion"));
				require(!(player.containerMenu instanceof StorageContainerMenu) && parts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
						&& parts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
						&& parts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT)
						&& fixture.endpoint().equals(requireEndpoint(expandedPrimary, "expanded primary chest"))
						&& expandedPrimary.getStorageWrapper().getInventoryHandler().getSlots() == 54 && count(expandedPrimary, Items.DIAMOND) == 7,
						"Linked primary expansion did not close the stale menu or retain its endpoint and contents");
				return expandedPrimary.getBlockPos();
			});
			waitForClosedStorageMenu();
			waitForClientStorageBlock(expandedPrimaryPos);
			openStorageMenu(expandedPrimaryPos);
			waitForClientStorageMenu(expandedPrimaryPos, 54, LinkedStorageEndpointRole.PRIMARY);
			require(AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu
							&& menu.getStorageWrapper().getInventoryHandler().getSlots() == 54 && count(menu, Items.DIAMOND) == 7),
					"Expanded linked primary did not reopen with 54 canonical slots");
			closeStorageMenu();
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				GameType originalMode = player.gameMode.getGameModeForPlayer();
				player.setGameMode(GameType.SURVIVAL);
				try {
					require(player.gameMode.destroyBlock(expandedPrimaryPos), "Could not break expanded linked primary chest");
					List<ItemEntity> drops = chestDrops(level, fixture.primaryPos());
					require(level.getBlockState(fixture.primaryPos()).isAir() && level.getBlockState(fixture.addedPos()).isAir() && drops.size() == 1
							&& ChestBlockItem.isDoubleChest(drops.getFirst().getItem())
							&& fixture.endpoint().equals(drops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
							&& StorageBlockEntity.getLinkedStorageEndpointRole(drops.getFirst().getItem())
									.filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
							"Expanded linked primary did not drop one paired primary carrier");
					ItemStack carrier = drops.getFirst().getItem().copy();
					drops.forEach(ItemEntity::discard);
					placeBlockAsPlayer(level, player, fixture.primaryPos(), carrier);
					List<ChestBlockEntity> restoredParts = nearbyChests(level, fixture.primaryPos());
					ChestBlockEntity restoredPrimary = restoredParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
							.orElseThrow(() -> new IllegalStateException("Missing restored paired primary chest"));
					require(restoredParts.size() == 2 && fixture.endpoint().equals(requireEndpoint(restoredPrimary, "restored paired primary chest"))
							&& restoredPrimary.getStorageWrapper().getInventoryHandler().getSlots() == 54 && count(restoredPrimary, Items.DIAMOND) == 7,
							"Paired primary carrier did not restore its linked double chest and canonical contents");
					ChestBlockEntity restoredNonMain = restoredParts.stream().filter(chest -> !chest.isMainChest()).findFirst()
							.orElseThrow(() -> new IllegalStateException("Missing restored non-main linked primary chest"));
					require(player.gameMode.destroyBlock(restoredNonMain.getBlockPos()), "Could not break restored non-main linked primary chest");
					List<ItemEntity> restoredDrops = chestDrops(level, fixture.primaryPos());
					require(level.getBlockState(fixture.primaryPos()).isAir() && level.getBlockState(fixture.addedPos()).isAir() && restoredDrops.size() == 1
							&& ChestBlockItem.isDoubleChest(restoredDrops.getFirst().getItem())
							&& fixture.endpoint().equals(restoredDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
							&& StorageBlockEntity.getLinkedStorageEndpointRole(restoredDrops.getFirst().getItem())
									.filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
							"Breaking the restored non-main linked primary chest did not drop one paired primary carrier");
					return true;
				} finally {
					player.setGameMode(originalMode);
				}
			});
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.serverLevel(), fixture.primaryPos());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
			waitForClientPlayerState(fixture.playerState());
		}
	}

	private static void runLinkedSecondaryChestSplitRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 124);
			BlockPos secondaryPos = primaryPos.east(5);
			BlockPos addedPos = secondaryPos.west();
			BlockPos reverseSecondaryPos = addedPos.east(3);
			BlockPos reverseAddedPos = reverseSecondaryPos.west();
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction() && useLinkerAsPlayer(player, linker, secondaryPos).consumesAction(),
						"Could not create linked secondary chest");
				ChestBlockEntity primary = getChest(level, primaryPos);
				LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(getChest(level, secondaryPos), "single secondary chest");
				List<ItemStack> canonicalContents = fillInventoryWithTestContents(primary.getStorageWrapper().getInventoryHandler());
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				int canonicalSlots = primary.getStorageWrapper().getInventoryHandler().getSlots();
				placeBlockAsPlayer(level, player, addedPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				List<ChestBlockEntity> parts = List.of(getChest(level, secondaryPos), getChest(level, addedPos));
				ChestBlockEntity secondaryMain = parts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after linked secondary expansion"));
				require(secondaryMain.getBlockPos().equals(secondaryPos) && secondaryEndpoint.equals(requireEndpoint(secondaryMain, "expanded secondary chest"))
						&& secondaryMain.getStorageWrapper().getInventoryHandler().getSlots() == canonicalSlots,
						"Expanded secondary chest changed canonical size or endpoint");
				BlockPos remainingPos = secondaryMain.getBlockPos().equals(secondaryPos) ? addedPos : secondaryPos;
				ChestBlockEntity secondaryNonMain = getChest(level, remainingPos);
				player.setGameMode(GameType.SURVIVAL);
				ItemStack rejectedTierUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				player.setItemInHand(InteractionHand.MAIN_HAND, rejectedTierUpgrade);
				require(player.gameMode.useItemOn(player, level, rejectedTierUpgrade, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(remainingPos), Direction.UP, remainingPos, false)) == InteractionResult.FAIL
						&& rejectedTierUpgrade.getCount() == 1 && level.getBlockState(secondaryMain.getBlockPos()).is(ModBlocks.CHEST.get())
						&& level.getBlockState(secondaryNonMain.getBlockPos()).is(ModBlocks.CHEST.get())
						&& secondaryEndpoint.equals(requireEndpoint(secondaryMain, "rejected secondary double-chest tier upgrade")),
						"Tier upgrade on the non-main linked secondary chest was not rejected without mutation");
				require(player.gameMode.destroyBlock(secondaryMain.getBlockPos()), "Player break did not remove the linked secondary main chest");
				ChestBlockEntity remaining = getChest(level, remainingPos);
				List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(secondaryMain.getBlockPos()).inflate(1.5D));
				ItemStack drop = drops.size() == 1 ? drops.getFirst().getItem().copy() : ItemStack.EMPTY;
				require(remaining.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& secondaryEndpoint.equals(requireEndpoint(remaining, "split secondary chest")) && !remaining.hasCustomName()
						&& hasInventoryContents(primary.getStorageWrapper().getInventoryHandler(), canonicalContents) && drops.size() == 1
						&& !ChestBlockItem.isDoubleChest(drop) && drop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
						&& !drop.has(DataComponents.CUSTOM_NAME), "Breaking a linked secondary main chest changed or dropped linked inventory contents");
				drops.forEach(ItemEntity::discard);

				player.setGameMode(GameType.CREATIVE);
				BlockPos shiftedAddedPos = remainingPos.west();
				placeBlockAsPlayer(level, player, shiftedAddedPos, drop);
				List<ChestBlockEntity> shiftedParts = List.of(getChest(level, shiftedAddedPos), getChest(level, remainingPos));
				ChestBlockEntity shiftedMain = shiftedParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after shifting linked secondary"));
				require(shiftedMain.getBlockPos().equals(remainingPos) && secondaryEndpoint.equals(requireEndpoint(shiftedMain, "shifted secondary chest"))
						&& !shiftedMain.hasCustomName(), "Shifting the linked secondary double chest did not preserve an unnamed endpoint");

				player.setGameMode(GameType.SURVIVAL);
				require(player.gameMode.destroyBlock(remainingPos), "Player break did not remove the shifted linked secondary main chest");
				ChestBlockEntity shiftedRemaining = getChest(level, shiftedAddedPos);
				List<ItemEntity> shiftedDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(remainingPos).inflate(1.5D));
				ItemStack shiftedDrop = shiftedDrops.size() == 1 ? shiftedDrops.getFirst().getItem().copy() : ItemStack.EMPTY;
				require(shiftedRemaining.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& secondaryEndpoint.equals(requireEndpoint(shiftedRemaining, "shifted split secondary chest")) && !shiftedRemaining.hasCustomName()
						&& hasInventoryContents(primary.getStorageWrapper().getInventoryHandler(), canonicalContents) && shiftedDrops.size() == 1
						&& !ChestBlockItem.isDoubleChest(shiftedDrop) && shiftedDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
						&& !shiftedDrop.has(DataComponents.CUSTOM_NAME),
						"Breaking the shifted linked secondary main chest changed or dropped linked inventory contents");
				shiftedDrops.forEach(ItemEntity::discard);

				require(player.gameMode.destroyBlock(shiftedAddedPos), "Player break did not remove the shifted linked secondary chest");
				List<ItemEntity> remainingDrops = chestDrops(level, shiftedAddedPos);
				require(remainingDrops.size() == 1 && !ChestBlockItem.isDoubleChest(remainingDrops.getFirst().getItem())
						&& secondaryEndpoint.equals(remainingDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
						"Breaking the remaining linked secondary chest did not drop its endpoint carrier");
				remainingDrops.forEach(ItemEntity::discard);

				placeBlockAsPlayer(level, player, reverseSecondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				ChestBlockEntity reverseSecondary = getChest(level, reverseSecondaryPos);
				ItemStack reverseLinker = new ItemStack(ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, reverseLinker, primaryPos).consumesAction()
						&& useLinkerAsPlayer(player, reverseLinker, reverseSecondaryPos).consumesAction(), "Could not add reverse linked secondary chest");
				LinkedStorageEndpointData reverseEndpoint = requireEndpoint(reverseSecondary, "reverse single secondary chest");
				placeBlockAsPlayer(level, player, reverseAddedPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				List<ChestBlockEntity> reverseParts = List.of(getChest(level, reverseSecondaryPos), getChest(level, reverseAddedPos));
				ChestBlockEntity reverseMain = reverseParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after reverse linked secondary expansion"));
				require(reverseParts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
						&& reverseParts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
						&& reverseEndpoint.equals(requireEndpoint(reverseMain, "reverse expanded secondary chest"))
						&& reverseMain.getStorageWrapper().getInventoryHandler().getSlots() == canonicalSlots,
						"Reverse linked secondary expansion changed canonical size or endpoint");
				ChestBlockEntity reverseNonMain = reverseParts.stream().filter(chest -> !chest.isMainChest()).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing non-main reverse linked secondary chest"));
				require(player.gameMode.destroyBlock(reverseNonMain.getBlockPos()), "Player break did not remove the linked secondary non-main chest");
				List<ItemEntity> reverseDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(reverseNonMain.getBlockPos()).inflate(1.5D));
				require(reverseMain.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& reverseEndpoint.equals(requireEndpoint(reverseMain, "split reverse secondary chest"))
						&& hasInventoryContents(primary.getStorageWrapper().getInventoryHandler(), canonicalContents) && reverseDrops.size() == 1
						&& !ChestBlockItem.isDoubleChest(reverseDrops.getFirst().getItem())
						&& reverseDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null,
						"Breaking a linked secondary non-main chest changed or dropped linked inventory contents");
				return true;
			} finally {
				clearArea(level, primaryPos);
				restorePlayerState(player, playerState);
			}
		});
	}

	private static void openStorageMenu(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			StorageBlockEntity storage = (StorageBlockEntity) player.serverLevel().getBlockEntity(pos);
			require(storage != null, "Missing storage to open at " + pos);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new StorageContainerMenu(windowId, menuPlayer, pos),
					storage.getMenuDisplayName()), buffer -> StorageContainerMenu.writeMenuData(buffer, player, pos));
			require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(pos)),
					"Could not open storage menu at " + pos);
			return true;
		});
	}

	private static void closeStorageMenu() {
		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			return true;
		});
	}

	private static void waitForClientStorageMenu(BlockPos pos, int slots, LinkedStorageEndpointRole role) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(pos))
					&& menu.getNumberOfStorageInventorySlots() == slots && menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
					&& provider.getLinkedStorageEndpointRole().filter(role::equals).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client did not open the expected linked storage menu at " + pos);
	}

	private static void waitForClosedStorageMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && !(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Stale linked storage menu did not close on the client");
	}

	private static void waitForClientLimitedBarrelMemory(BlockPos pos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(pos))
					&& isDiamondMemory(menu.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class)))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Open secondary limited-barrel menu did not receive memorized-slot state");
	}

	private static boolean isDiamondMemory(MemorySettingsCategory memory) {
		return memory.getSlotFilterStack(0, false).filter(stack -> stack.is(Items.DIAMOND)).isPresent();
	}

	private static void waitForClientLinkedStack(LinkedStackFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& StorageBlockEntity.getLinkedStorageEndpointData(Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot()))
							.filter(endpoint -> endpoint.groupId().equals(fixture.groupId())).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Dropped linked stack did not synchronize to the client inventory");
	}

	private static void waitForClientLinkedStackContents(LinkedStackFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null
						|| ClientLinkedStorageContents.getContents(fixture.groupId()).isEmpty()) {
					return false;
				}
				StackStorageWrapper wrapper = StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(),
						Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot()));
				return wrapper.getContentsUuid().filter(fixture.groupId()::equals).isPresent() && count(wrapper.getInventoryHandler(), Items.DIAMOND) == 7;
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked stack tooltip did not populate the canonical client cache");
	}

	private static void waitForServerCondition(String description, Predicate<ServerPlayer> condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(condition::test)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static void configureMagnet(BarrelBlockEntity primary) {
		UpgradeHandler upgrades = primary.getStorageWrapper().getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(ModItems.MAGNET_UPGRADE.get()));
		MagnetUpgradeWrapper magnet = upgrades.getWrappersThatImplement(MagnetUpgradeWrapper.class).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Linked primary magnet upgrade was not initialized"));
		magnet.setPickupItems(true);
		magnet.setPickupXp(false);
		magnet.getFilterLogic().setDepositFilterType(ContentsFilterType.ALLOW);
		magnet.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.ENDER_PEARL));
		upgrades.saveInventory();
	}

	private static IItemHandler requireItemCapability(ServerLevel level, BlockPos pos, String name) {
		IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
		if (handler == null) {
			throw new IllegalStateException("Missing item capability for " + name);
		}
		return handler;
	}

	private static List<ItemEntity> chestDrops(ServerLevel level, BlockPos pos) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2D), entity -> entity.getItem().is(ModBlocks.CHEST_ITEM.get()));
	}

	private static List<ChestBlockEntity> nearbyChests(ServerLevel level, BlockPos pos) {
		return List.of(pos, pos.north(), pos.south(), pos.east(), pos.west()).stream().map(level::getBlockEntity).filter(ChestBlockEntity.class::isInstance)
				.map(ChestBlockEntity.class::cast).toList();
	}

	private static void openLinkedStorageMenu(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			ItemStack originalMainHand = player.getMainHandItem().copy();
			GameType originalGameType = player.gameMode.getGameModeForPlayer();
			try {
				player.setGameMode(GameType.SURVIVAL);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				require(player.gameMode.useItemOn(player, player.serverLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)).consumesAction(),
						"Could not open linked storage through player interaction");
				return true;
			} finally {
				player.closeContainer();
				player.setGameMode(originalGameType);
				player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
			}
		});
	}

	public static String setupLinkedLimitedBarrelReloadProjection() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = getReloadPrimaryPos(player);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get());
			require(useLinkerAsPlayer(player, linker, primaryPos).consumesAction() && useLinkerAsPlayer(player, linker, secondaryPos).consumesAction(),
					"Could not link limited barrels through player interaction before reload");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, LINKED_LIMITED_RELOAD_ITEM_COUNT));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			require(hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary),
					"Linked limited barrels did not initialize their shared render projection");
			return "{\"ok\":true,\"groupId\":\"" + requireEndpoint(primary, "reload primary").groupId() + "\"}";
		});
	}

	public static String linkedLimitedBarrelReloadProjectionStatus(UUID groupId) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = getReloadPrimaryPos(player);
			BlockPos secondaryPos = primaryPos.east(3);
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
			require(requireEndpoint(primary, "reloaded primary").groupId().equals(groupId)
					&& requireEndpoint(secondary, "reloaded secondary").groupId().equals(groupId) && hasLimitedRenderProjection(primary)
					&& hasLimitedRenderProjection(secondary), "Linked limited barrel canonical state was not restored after reload");
			return true;
		});
		BlockPos primaryPos = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::getReloadPrimaryPos);
		waitForClientStorageBlock(primaryPos);
		openLinkedStorageMenu(primaryPos);
		waitForClientLimitedBarrelReloadProjection(primaryPos, primaryPos.east(3), groupId);
		return "{\"ok\":true,\"clientDisplayItems\":true,\"clientCounts\":true,\"clientFillLevels\":true}";
	}

	private static LinkedStorageFixture runOnServer(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos anchor = player.blockPosition().offset(0, 0, 24);
		BlockPos chestLeft = anchor;
		BlockPos chestRight = anchor.east();
		BlockPos barrelPos = anchor.east(3);
		BlockPos shulkerPos = anchor.east(5);
		BlockPos limitedPrimaryPos = anchor.south(3);
		BlockPos limitedSecondaryPos = limitedPrimaryPos.east(3);
		BlockPos incompatibleLimitedPos = limitedSecondaryPos.east(3);
		BlockPos incompatibleStandardPos = incompatibleLimitedPos.east(3);
		ItemStack originalMainHand = player.getMainHandItem().copy();
		GameType originalGameType = player.gameMode.getGameModeForPlayer();
		clearArea(level, anchor);
		try {
			player.setGameMode(GameType.SURVIVAL);
			placeBlockAsPlayer(level, player, chestLeft, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeBlockAsPlayer(level, player, chestRight, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeBlockAsPlayer(level, player, barrelPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlockAsPlayer(level, player, shulkerPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			placeBlockAsPlayer(level, player, limitedPrimaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			placeBlockAsPlayer(level, player, limitedSecondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			placeBlockAsPlayer(level, player, incompatibleLimitedPos, new ItemStack(ModBlocks.LIMITED_BARREL_2_ITEM.get()));
			placeBlockAsPlayer(level, player, incompatibleStandardPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));

			ChestBlockEntity chest = List.of(getChest(level, chestLeft), getChest(level, chestRight)).stream().filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Could not form a main double chest for linked storage"));
			ChestBlockEntity nonMainChest = List.of(getChest(level, chestLeft), getChest(level, chestRight)).stream()
					.filter(candidate -> !candidate.isMainChest()).findFirst()
					.orElseThrow(() -> new IllegalStateException("Could not form a non-main double chest half for linked storage"));
			BarrelBlockEntity barrel = getBarrel(level, barrelPos);
			ShulkerBoxBlockEntity shulker = getShulker(level, shulkerPos);
			LimitedBarrelBlockEntity limitedPrimary = getLimitedBarrel(level, limitedPrimaryPos);
			LimitedBarrelBlockEntity limitedSecondary = getLimitedBarrel(level, limitedSecondaryPos);
			LimitedBarrelBlockEntity incompatibleLimited = getLimitedBarrel(level, incompatibleLimitedPos);
			BarrelBlockEntity incompatibleStandard = getBarrel(level, incompatibleStandardPos);

			ItemStack standardLinker = new ItemStack(ENDER_LINKER.get());
			InteractionResult chestLinkResult = useLinkerAsPlayer(player, standardLinker, nonMainChest.getBlockPos());
			InteractionResult barrelLinkResult = useLinkerAsPlayer(player, standardLinker, barrelPos);
			ItemStack shulkerLinker = new ItemStack(ENDER_LINKER.get());
			InteractionResult shulkerTargetResult = useLinkerAsPlayer(player, shulkerLinker, chest.getBlockPos());
			InteractionResult shulkerLinkResult = useLinkerAsPlayer(player, shulkerLinker, shulkerPos);
			require(chestLinkResult instanceof InteractionResult.Success && barrelLinkResult instanceof InteractionResult.Success
					&& shulkerTargetResult instanceof InteractionResult.Success && shulkerLinkResult instanceof InteractionResult.Success,
					"Player Ender Linker interaction did not link the double chest, barrel, and shulker: " + chestLinkResult + ", " + barrelLinkResult + ", "
							+ shulkerTargetResult + ", " + shulkerLinkResult);
			LinkedStorageEndpointData chestEndpoint = requireEndpoint(chest, "main chest");
			require(chestEndpoint.groupId().equals(requireEndpoint(barrel, "barrel").groupId())
					&& chestEndpoint.groupId().equals(requireEndpoint(shulker, "shulker").groupId()) && nonMainChest.getLinkedStorageEndpointData() == null,
					"Non-main double-chest linker interaction did not route to only the main linked endpoint");

			chest.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			chest.getStorageWrapper().getInventoryHandler().saveInventory();
			chest.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
			chest.getStorageWrapper().getUpgradeHandler().saveInventory();
			require(count(barrel, Items.DIAMOND) == 7 && count(shulker, Items.DIAMOND) == 7
					&& barrel.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_TIER_1.get()),
					"Linked storage did not share canonical contents and upgrades");
			barrel.getStorageWrapper().setSortBy(net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy.COUNT);
			require(chest.getStorageWrapper().getSortBy() == net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy.COUNT,
					"Linked storage did not share canonical settings");
			require(chest.canUpgradeStorageTier(level) && !barrel.canUpgradeStorageTier(level) && !shulker.canUpgradeStorageTier(level),
					"Only the primary linked endpoint may tier upgrade");

			ItemStack packingTape = new ItemStack(ModItems.PACKING_TAPE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, packingTape);
			require(player.gameMode
					.useItemOn(player, level, packingTape, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(nonMainChest.getBlockPos()), Direction.UP, nonMainChest.getBlockPos(), false))
					.consumesAction() && chest.isLinkedStorage() && !chest.isPacked() && !nonMainChest.isPacked() && packingTape.getDamageValue() == 0,
					"Packing tape on the non-main linked chest half was not rejected without durability loss");

			ItemStack limitedLinker = new ItemStack(ENDER_LINKER.get(), 4);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker, limitedPrimary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker, limitedSecondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Matching limited barrels did not link across tiers");
			limitedPrimary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.EMERALD, 11));
			limitedPrimary.getStorageWrapper().getInventoryHandler().saveInventory();
			require(count(limitedSecondary, Items.EMERALD) == 11 && hasLimitedRenderProjection(limitedSecondary),
					"Linked limited barrels did not share contents and render projection");
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker,
					incompatibleLimited) == LinkedStorageService.LinkResult.INCOMPATIBLE_ENDPOINT
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), limitedLinker,
							incompatibleStandard) == LinkedStorageService.LinkResult.INCOMPATIBLE_ENDPOINT
					&& !incompatibleLimited.isLinkedStorage() && !incompatibleStandard.isLinkedStorage(),
					"Linked storage accepted an incompatible limited-barrel slot family or standard barrel endpoint");

			ItemStack restoredCarrier = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
			shulker.copyLinkedStorageEndpointTo(restoredCarrier);
			require(restoredCarrier.get(ModCoreDataComponents.STORAGE_UUID) == null, "Linked carrier retained ItemContentsStorage data");
			level.setBlock(shulkerPos, Blocks.AIR.defaultBlockState(), 3);
			placeBlockAsPlayer(level, player, shulkerPos, restoredCarrier);
			ShulkerBoxBlockEntity restoredShulker = getShulker(level, shulkerPos);
			require(chestEndpoint.groupId().equals(requireEndpoint(restoredShulker, "restored shulker").groupId())
					&& count(restoredShulker, Items.DIAMOND) == 7, "Restored linked endpoint did not reattach canonical contents");

			player.setGameMode(originalGameType);
			player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
			return new LinkedStorageFixture(anchor, chest.getBlockPos(), chestEndpoint.groupId(), chestEndpoint, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		} catch (RuntimeException e) {
			clearArea(level, anchor);
			throw e;
		} finally {
			player.setGameMode(originalGameType);
			player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
		}
	}

	private static void waitForClientCanonicalContents(LinkedStorageFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().level != null && ClientLinkedStorageContents.getContents(fixture.groupId()).isPresent())) {
				AutomationRuntime.runOnClient(() -> {
					ItemStack carrier = fixture.carrier().copy();
					carrier.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, fixture.primaryEndpoint());
					StackStorageWrapper wrapper = StackStorageWrapper.fromStack(Minecraft.getInstance().level.registryAccess(), carrier);
					require(count(wrapper.getInventoryHandler(), Items.DIAMOND) == 7 && carrier.get(ModCoreDataComponents.STORAGE_UUID) == null,
							"Client linked stack did not resolve direct Core contents");
					return true;
				});
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client did not receive the linked storage Core contents snapshot");
	}

	private static void runLinkedControllerEndpointJoinDoesNotDuplicateCanonicalContentsRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 76);
			BlockPos existingEndpointPos = controllerPos.east();
			BlockPos joiningEndpointPos = existingEndpointPos.east();
			clearArea(level, controllerPos);
			try {
				placeBlockAsPlayer(level, player, existingEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, joiningEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
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
				return true;
			} finally {
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
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
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
				return true;
			} finally {
				clearArea(level, primaryPos);
			}
		});
	}

	private static void runDoubleChestControllerLockRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 132);
			BlockPos firstChestPos = controllerPos.east();
			BlockPos secondChestPos = firstChestPos.east();
			BlockPos ordinaryStoragePos = controllerPos.above();
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, controllerPos);
			try {
				placeBlockAsPlayer(level, player, firstChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				placeBlockAsPlayer(level, player, secondChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				placeBlockAsPlayer(level, player, ordinaryStoragePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing controller for double-chest lock regression"));
				List<ChestBlockEntity> doubleChestParts = List.of(getChest(level, firstChestPos), getChest(level, secondChestPos));
				ChestBlockEntity mainChest = doubleChestParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main double-chest half for controller lock regression"));
				ChestBlockEntity otherChest = doubleChestParts.stream().filter(chest -> !chest.getBlockPos().equals(mainChest.getBlockPos())).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing other double-chest half for controller lock regression"));
				BarrelBlockEntity ordinaryStorage = getBarrel(level, ordinaryStoragePos);
				require(ordinaryStorage.getControllerPos().filter(controllerPos::equals).isPresent(),
						"Controller did not connect the ordinary storage beside the double chest");

				controller.toggleLock();
				require(mainChest.isLocked() && otherChest.isLocked() && ordinaryStorage.isLocked(),
						"One controller lock toggle did not lock both double-chest halves and the ordinary connected storage");
				controller.toggleLock();
				require(!mainChest.isLocked() && !otherChest.isLocked() && !ordinaryStorage.isLocked(),
						"One controller unlock toggle did not unlock both double-chest halves and the ordinary connected storage");
				return true;
			} finally {
				clearArea(level, controllerPos);
				restorePlayerState(player, playerState);
			}
		});
	}

	private static void runLockedLinkedControllerRoutingRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 144);
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
				List<BlockPos> secondaryPositions = List.of(firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos);
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
				Set<BlockPos> linkedPositions = Set.of(firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos, primaryPos);
				require(controller.getStoragePositions().size() == 2 && controller.getStorageBlockPositions()
						.containsAll(Set.of(firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos, primaryPos, regularPos)),
						"Controller did not connect the locked linked group and regular fallback storage");
				controller.toggleLock();
				require(linkedPositions.stream().allMatch(pos -> getBarrel(level, pos).isLocked()) && regular.isLocked(),
						"Controller did not lock every linked routing member and regular fallback storage");
				ItemStackKey oakKey = ItemStackKey.of(new ItemStack(Items.OAK_PLANKS));
				ItemStackKey acaciaKey = ItemStackKey.of(new ItemStack(Items.ACACIA_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getStackStorages(oakKey).size() == 1, "Controller did not index a matching stack for a locked linked group");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, ItemStack.EMPTY);
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getStackStorages(oakKey).isEmpty(), "Controller retained a removed canonical linked stack in its index");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.ACACIA_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getStackStorages(acaciaKey).size() == 1, "Controller did not index a replacement canonical stack");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getStackStorages(oakKey).size() == 1 && controller.getStackStorages(acaciaKey).isEmpty(),
						"Controller did not replace a changed canonical linked-group stack in its index");
				require(controller.insertItem(new ItemStack(Items.OAK_PLANKS), false).isEmpty() && count(primary, Items.OAK_PLANKS) == 2,
						"Controller did not insert a matching stack into a fully locked linked group");
				require(controller.insertItem(new ItemStack(Items.BIRCH_PLANKS), false).is(Items.BIRCH_PLANKS),
						"Controller inserted a new stack while every linked member and fallback storage was locked");
				firstSecondary.toggleLock();
				ItemStackKey birchKey = ItemStackKey.of(new ItemStack(Items.BIRCH_PLANKS));
				require(controller.getEmptyTargetSlotStorages(birchKey).containsAll(linkedPositions)
						&& controller.insertItem(new ItemStack(Items.BIRCH_PLANKS), true).isEmpty(),
						"Controller did not restore linked empty targets after a member unlocked");
				int lastSlot = primary.getStorageWrapper().getInventoryHandler().getSlots() - 1;
				for (int slot = 0; slot <= lastSlot; slot++) {
					primary.getStorageWrapper().getInventoryHandler().setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS));
				}
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getEmptyTargetSlotStorages(birchKey).stream().noneMatch(linkedPositions::contains),
						"Controller retained linked empty targets after canonical inventory became full");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(lastSlot, ItemStack.EMPTY);
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getEmptyTargetSlotStorages(birchKey).containsAll(linkedPositions),
						"Controller did not restore linked empty targets when canonical inventory gained an empty slot");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(lastSlot, new ItemStack(Items.OAK_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				MemorySettingsCategory memory = primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				memory.selectSlots(0, lastSlot + 1);
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(lastSlot, ItemStack.EMPTY);
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.getEmptyTargetSlotStorages(birchKey).stream().noneMatch(linkedPositions::contains),
						"Controller treated a memorized linked slot as an unrestricted empty target");
				memory.unselectSlot(lastSlot);
				require(controller.getEmptyTargetSlotStorages(birchKey).containsAll(linkedPositions),
						"Controller did not restore linked empty targets when canonical memory was removed");
				memory.unselectAllSlots();
				for (int slot = 0; slot <= lastSlot; slot++) {
					primary.getStorageWrapper().getInventoryHandler().setStackInSlot(slot, ItemStack.EMPTY);
				}
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				require(controller.insertItem(new ItemStack(Items.BIRCH_PLANKS), false).isEmpty() && count(primary, Items.BIRCH_PLANKS) == 1,
						"Controller did not use an unlocked linked secondary for unrestricted insertion");
				firstSecondary.toggleLock();
				primary.toggleLock();
				require(controller.insertItem(new ItemStack(Items.SPRUCE_PLANKS), false).isEmpty() && count(primary, Items.SPRUCE_PLANKS) == 1,
						"Controller did not use an unlocked linked primary for unrestricted insertion");
				primary.toggleLock();
				regular.toggleLock();
				require(controller.insertItem(new ItemStack(Items.DARK_OAK_PLANKS), false).isEmpty() && count(regular, Items.DARK_OAK_PLANKS) == 1,
						"Controller did not use the unlocked regular fallback for a new stack");
				require(controller.insertItem(new ItemStack(Items.OAK_PLANKS), false).isEmpty() && count(primary, Items.OAK_PLANKS) == 2
						&& count(regular, Items.OAK_PLANKS) == 0,
						"Controller did not prioritize the matching locked linked group over an unlocked regular fallback");
				return true;
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
						"Surviving linked listener did not update the controller item index after non-listener removal");

				MemorySettingsCategory memory = primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.EMERALD));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				memory.selectSlot(1);
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, ItemStack.EMPTY);
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				ItemStackKey emeraldKey = ItemStackKey.of(new ItemStack(Items.EMERALD));
				require(controller.hasMatchingItem(Items.EMERALD) && controller.getItemStorages(emeraldKey).equals(List.of(primaryPos)),
						"Surviving linked listener did not update the controller memory index after non-listener removal");

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
						"Surviving linked listener did not isolate and retain its input-filter index after non-listener removal");
				return true;
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
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
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
				return true;
			} finally {
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
				BarrelBlockEntity primary = getBarrel(level, primaryLinkedPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryLinkedPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked controller bridge group");
				LinkedStorageEndpointData endpoint = requireEndpoint(primary, "linked controller bridge primary");
				placeBlock(level, player, otherGroupLinkedPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity otherGroup = getBarrel(level, otherGroupLinkedPos);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
						otherGroup) == LinkedStorageService.LinkResult.SUCCESS
						&& !endpoint.groupId().equals(requireEndpoint(otherGroup, "adjacent bridge group").groupId()),
						"Could not create distinct adjacent linked bridge group");
				placeBlock(level, player, downstreamPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing linked controller bridge"));
				require(controller.getStoragePositions().size() == 3 && primary.getControllerPos().filter(controllerPos::equals).isPresent()
						&& secondary.getControllerPos().filter(controllerPos::equals).isPresent()
						&& otherGroup.getControllerPos().filter(controllerPos::equals).isPresent()
						&& getBarrel(level, downstreamPos).getControllerPos().filter(controllerPos::equals).isPresent(),
						"Controller did not initially connect both linked bridge groups and downstream storage");
				level.destroyBlock(primaryLinkedPos, true, player);
				ItemStack endpointDrop = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryLinkedPos)).stream()
						.filter(item -> endpoint.equals(item.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).findFirst().orElseThrow().getItem()
						.copy();
				level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryLinkedPos)).forEach(ItemEntity::discard);
				require(getBarrel(level, downstreamPos).getControllerPos().isEmpty(), "Downstream storage remained connected after bridge endpoint removal");
				placeBlock(level, player, primaryLinkedPos, endpointDrop);
				BarrelBlockEntity restoredPrimary = getBarrel(level, primaryLinkedPos);
				boolean restoredEndpoint = endpoint.equals(requireEndpoint(restoredPrimary, "restored bridge primary"));
				boolean groupAPreserved = endpoint.groupId()
						.equals(requireEndpoint(getBarrel(level, secondaryLinkedPos), "restored bridge secondary").groupId());
				boolean primaryConnected = restoredPrimary.getControllerPos().filter(controllerPos::equals).isPresent();
				boolean secondaryConnected = getBarrel(level, secondaryLinkedPos).getControllerPos().filter(controllerPos::equals).isPresent();
				boolean otherGroupConnected = getBarrel(level, otherGroupLinkedPos).getControllerPos().filter(controllerPos::equals).isPresent();
				boolean downstreamConnected = getBarrel(level, downstreamPos).getControllerPos().filter(controllerPos::equals).isPresent();
				require(restoredEndpoint && groupAPreserved && controller.getStoragePositions().size() == 3 && primaryConnected && secondaryConnected
						&& otherGroupConnected && downstreamConnected,
						"Restored linked bridge did not reconnect and preserve both groups and downstream storage: positions="
								+ controller.getStoragePositions() + ", restoredEndpoint=" + restoredEndpoint + ", groupAPreserved=" + groupAPreserved
								+ ", primaryConnected=" + primaryConnected + ", secondaryConnected=" + secondaryConnected + ", otherGroupConnected="
								+ otherGroupConnected + ", downstreamConnected=" + downstreamConnected);
				return true;
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
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get(), 4);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked group for partial controller disconnect regression");
				for (BlockPos pos : List.of(firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos)) {
					require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker,
							getBarrel(level, pos)) == LinkedStorageService.LinkResult.SUCCESS,
							"Could not add linked secondary for partial controller disconnect regression");
				}
				placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing partial linked-group disconnect controller"));
				require(controller.getStoragePositions().size() == 2 && controller.getStorageBlockPositions().containsAll(linkedPositions)
						&& controller.getStorageBlockPositions().contains(regularPos),
						"Controller did not register every linked physical endpoint and downstream barrel");
				linkedPositions.forEach(pos -> getBarrel(level, pos).toggleLock());
				getBarrel(level, regularPos).toggleLock();
				level.destroyBlock(secondSecondaryPos, false, player);
				require(getBarrel(level, firstSecondaryPos).getControllerPos().filter(controllerPos::equals).isPresent()
						&& getBarrel(level, thirdSecondaryPos).getControllerPos().isEmpty() && getBarrel(level, regularPos).getControllerPos().isEmpty()
						&& controller.getStoragePositions().equals(List.of(firstSecondaryPos))
						&& controller.getStorageBlockPositions().equals(Set.of(firstSecondaryPos)),
						"Controller retained the disconnected linked branch after the physical chain was broken");
				getBarrel(level, thirdSecondaryPos).toggleLock();
				require(controller.insertItem(new ItemStack(Items.CHERRY_PLANKS), false).is(Items.CHERRY_PLANKS),
						"A disconnected linked endpoint still affected controller insertion");
				placeBlock(level, player, secondSecondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				Set<BlockPos> reconnected = Set.of(firstSecondaryPos, secondSecondaryPos, thirdSecondaryPos, fourthSecondaryPos, primaryPos, regularPos);
				require(controller.getStoragePositions().size() == 3
						&& controller.getStoragePositions().containsAll(List.of(firstSecondaryPos, secondSecondaryPos, regularPos))
						&& controller.getStorageBlockPositions().equals(reconnected),
						"Controller did not restore each physical endpoint and linked anchor after bridge replacement");
				controller.toggleLock();
				require(reconnected.stream().allMatch(pos -> getBarrel(level, pos).isLocked()),
						"Controller lock action did not fan out to every reconnected endpoint");
				return true;
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
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
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
				return true;
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
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked group for controller primary-tier regression");
				placeBlockAsPlayer(level, player, secondaryControllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
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
				placeBlockAsPlayer(level, player, primaryControllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
				ItemStack acceptedUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				player.setItemInHand(InteractionHand.MAIN_HAND, acceptedUpgrade);
				require(player.gameMode
						.useItemOn(player, level, acceptedUpgrade, InteractionHand.MAIN_HAND,
								new BlockHitResult(Vec3.atCenterOf(primaryControllerPos), Direction.UP, primaryControllerPos, false))
						.consumesAction() && acceptedUpgrade.isEmpty() && level.getBlockState(primaryPos).is(ModBlocks.DIAMOND_BARREL.get()),
						"Controller did not apply the tier upgrade after the linked primary endpoint connected");
				return true;
			} finally {
				player.setGameMode(originalGameMode);
				player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
				clearArea(level, secondaryControllerPos);
			}
		});
	}

	private static void runLinkedStorageTierUpgradeRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 240);
			BlockPos secondaryPos = primaryPos.east(3);
			PlayerState playerState = capturePlayerState(player);
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked barrels for direct tier-upgrade routing");
				LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "direct tier-upgrade primary");
				LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "direct tier-upgrade secondary");
				int originalSlots = primary.getStorageWrapper().getInventoryHandler().getSlots();
				player.setGameMode(GameType.SURVIVAL);
				ItemStack secondaryUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				require(useTierUpgrade(player, secondaryPos, secondaryUpgrade) == InteractionResult.FAIL && secondaryUpgrade.getCount() == 1
						&& level.getBlockState(secondaryPos).is(ModBlocks.BARREL.get())
						&& getBarrel(level, secondaryPos).getStorageWrapper().getInventoryHandler().getSlots() == originalSlots,
						"Direct tier upgrade did not reject the linked secondary without mutation");
				ItemStack primaryUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				require(useTierUpgrade(player, primaryPos, primaryUpgrade) == InteractionResult.SUCCESS && primaryUpgrade.isEmpty()
						&& level.getBlockState(primaryPos).is(ModBlocks.DIAMOND_BARREL.get()), "Direct tier upgrade did not upgrade the linked primary");
				BarrelBlockEntity upgradedPrimary = getBarrel(level, primaryPos);
				BarrelBlockEntity upgradedSecondary = getBarrel(level, secondaryPos);
				int expectedSlots = ModBlocks.DIAMOND_BARREL.get().getNumberOfInventorySlots();
				int expectedUpgradeSlots = ModBlocks.DIAMOND_BARREL.get().getNumberOfUpgradeSlots();
				require(primaryEndpoint.equals(requireEndpoint(upgradedPrimary, "upgraded direct tier primary"))
						&& secondaryEndpoint.equals(requireEndpoint(upgradedSecondary, "upgraded direct tier secondary"))
						&& upgradedPrimary.getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots
						&& upgradedSecondary.getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots
						&& upgradedPrimary.getStorageWrapper().getUpgradeHandler().getSlots() == expectedUpgradeSlots
						&& upgradedSecondary.getStorageWrapper().getUpgradeHandler().getSlots() == expectedUpgradeSlots
						&& requireItemCapability(level, primaryPos, "upgraded direct tier primary").getSlots() == expectedSlots
						&& requireItemCapability(level, secondaryPos, "upgraded direct tier secondary").getSlots() == expectedSlots,
						"Direct primary tier upgrade did not preserve linked endpoint identities and shared capacities");
				return true;
			} finally {
				player.setGameMode(originalGameMode);
				clearArea(level, primaryPos);
				restorePlayerState(player, playerState);
			}
		});
	}

	private static void runLinkedStorageTierUpgradeMenuInvalidationRegression() {
		runLinkedStorageTierUpgradeMenuInvalidationRegression(true);
		runLinkedStorageTierUpgradeMenuInvalidationRegression(false);
	}

	private static void runLinkedStorageTierUpgradeMenuInvalidationRegression(boolean primaryMenu) {
		LinkedChestFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 252);
			BlockPos secondaryPos = primaryPos.east(3);
			PlayerState playerState = capturePlayerState(player);
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				ItemStack linker = new ItemStack(ENDER_LINKER.get());
				BarrelBlockEntity primary = getBarrel(level, primaryPos);
				BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked barrels for tier-upgrade menu invalidation");
				return new LinkedChestFixture(primaryPos, secondaryPos, requireEndpoint(primary, "tier-upgrade primary"), playerState);
			} catch (RuntimeException e) {
				clearArea(level, primaryPos);
				throw e;
			} finally {
				restorePlayerState(player, playerState);
			}
		});
		GameType originalGameMode = AutomationRuntime.runOnServer(player -> {
			GameType gameMode = player.gameMode.getGameModeForPlayer();
			player.setGameMode(GameType.SURVIVAL);
			return gameMode;
		});
		try {
			BlockPos openMenuPos = primaryMenu ? fixture.primaryPos() : fixture.addedPos();
			LinkedStorageEndpointRole menuRole = primaryMenu ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY;
			movePlayerNear(openMenuPos, fixture.playerState());
			waitForClientStorageBlock(openMenuPos);
			openStorageMenu(openMenuPos);
			waitForClientStorageMenu(openMenuPos, 27, menuRole);
			if (primaryMenu) {
				AutomationRuntime.runOnServer(player -> {
					ItemStack rejectedUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
					require(useTierUpgrade(player, fixture.primaryPos(), rejectedUpgrade) == InteractionResult.PASS && rejectedUpgrade.getCount() == 1,
							"Primary linked tier upgrade did not reject its open linked storage menu without consuming the upgrade");
					return true;
				});
				closeStorageMenu();
				waitForClosedStorageMenu();
			}
			AutomationRuntime.runOnServer(player -> {
				ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
				require(upgrade.getItem()
						.onItemUseFirst(upgrade,
								new UseOnContext(player, InteractionHand.MAIN_HAND,
										new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()), Direction.UP, fixture.primaryPos(), false)))
						.consumesAction() && upgrade.isEmpty(), "Primary linked tier upgrade did not complete");
				return true;
			});
			if (!primaryMenu) {
				waitForClosedStorageMenu();
				waitForServerCondition("primary linked tier upgrade stale secondary menu closure", player -> player.containerMenu == player.inventoryMenu);
			}
			openStorageMenu(fixture.addedPos());
			waitForClientStorageMenu(fixture.addedPos(), ModBlocks.DIAMOND_BARREL.get().getNumberOfInventorySlots(), LinkedStorageEndpointRole.SECONDARY);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setGameMode(originalGameMode);
				clearArea(player.serverLevel(), fixture.primaryPos());
				restorePlayerState(player, fixture.playerState());
				return true;
			});
			waitForClientPlayerState(fixture.playerState());
		}
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
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
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
				return true;
			} finally {
				player.getInventory().setItem(0, originalFirstInventoryStack);
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedControllerClientOutlineRegression() {
		LinkedControllerOutlineFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedControllerOutlineFixture);
		try {
			waitForClientStorageBlock(fixture.primaryPos());
			waitForClientStorageBlock(fixture.secondaryPos());
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_CONTROLLER_OUTLINE_CONVERGENCE_SECONDS);
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
				return true;
			});
		}
	}

	private static LinkedControllerOutlineFixture setupLinkedControllerOutlineFixture(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 36);
		BlockPos primaryPos = controllerPos.east();
		BlockPos secondaryPos = primaryPos.east();
		clearArea(level, controllerPos);
		placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
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

	private static void waitForClientLimitedBarrelReloadProjection(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null
					&& Minecraft.getInstance().level.getBlockEntity(primaryPos) instanceof LimitedBarrelBlockEntity primary
					&& Minecraft.getInstance().level.getBlockEntity(secondaryPos) instanceof LimitedBarrelBlockEntity secondary
					&& primary.getLinkedStorageEndpointData() != null && secondary.getLinkedStorageEndpointData() != null
					&& primary.getLinkedStorageEndpointData().groupId().equals(groupId) && secondary.getLinkedStorageEndpointData().groupId().equals(groupId)
					&& hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Reloaded linked limited barrels did not synchronize their render projection to the client");
	}

	private static BlockPos getReloadPrimaryPos(ServerPlayer player) {
		return player.blockPosition().offset(0, 0, 48);
	}

	private static void waitForClientStorageBlock(BlockPos pos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof StorageBlockEntity)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Reloaded linked storage block did not synchronize to the client");
	}

	private static InteractionResult useLinkerAsPlayer(ServerPlayer player, ItemStack linker, BlockPos pos) {
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		return player.gameMode.useItemOn(player, player.serverLevel(), linker, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	private static InteractionResult useTierUpgrade(ServerPlayer player, BlockPos pos, ItemStack upgrade) {
		player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
		return upgrade.getItem().onItemUseFirst(upgrade,
				new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
	}

	private static PlayerState capturePlayerState(ServerPlayer player) {
		return new PlayerState(player.position(), player.getYRot(), player.getXRot(), player.getInventory().selected, player.getMainHandItem().copy());
	}

	private static void movePlayerNear(BlockPos pos, PlayerState originalState) {
		PlayerState nearbyState = new PlayerState(new Vec3(pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D), originalState.yRot(), originalState.xRot(),
				originalState.selectedSlot(), originalState.mainHandStack());
		AutomationRuntime.runOnServer(player -> {
			restorePlayerState(player, nearbyState);
			return true;
		});
		waitForClientPlayerState(nearbyState);
	}

	private static void restorePlayerState(ServerPlayer player, PlayerState state) {
		player.getInventory().selected = state.selectedSlot();
		player.setItemInHand(InteractionHand.MAIN_HAND, state.mainHandStack().copy());
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastFullState();
		player.connection.send(new ClientboundSetHeldSlotPacket(state.selectedSlot()));
		require(player.teleportTo(player.serverLevel(), state.position().x, state.position().y, state.position().z, Set.of(), state.yRot(), state.xRot(),
				false), "Could not restore player position and rotation for linked storage regression");
	}

	private static void waitForClientPlayerState(PlayerState state) {
		Vec3 standingClientPosition = state.position().add(0, -0.5D, 0);
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& (Minecraft.getInstance().player.position().distanceToSqr(state.position()) < 0.0001D
							|| Minecraft.getInstance().player.position().distanceToSqr(standingClientPosition) < 0.0001D)
					&& Math.abs(Minecraft.getInstance().player.getYRot() - state.yRot()) < 0.01F
					&& Math.abs(Minecraft.getInstance().player.getXRot() - state.xRot()) < 0.01F
					&& Minecraft.getInstance().player.getInventory().selected == state.selectedSlot()
					&& ItemStack.matches(Minecraft.getInstance().player.getMainHandItem(), state.mainHandStack()))) {
				return;
			}
			sleep(50);
		}
		String clientState = AutomationRuntime.runOnClient(() -> {
			if (Minecraft.getInstance().player == null) {
				return "missing";
			}
			return "position=" + Minecraft.getInstance().player.position() + ", yRot=" + Minecraft.getInstance().player.getYRot() + ", xRot="
					+ Minecraft.getInstance().player.getXRot() + ", selectedSlot=" + Minecraft.getInstance().player.getInventory().selected + ", mainHand="
					+ Minecraft.getInstance().player.getMainHandItem();
		});
		throw new IllegalStateException("Player state did not synchronize for linked storage regression: expected=" + state + ", standingClientPosition="
				+ standingClientPosition + ", actual=" + clientState);
	}

	private static void placeBlockAsPlayer(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		PlayerState playerState = capturePlayerState(player);
		try {
			require(player.teleportTo(level, pos.getX() + .5D, pos.getY() + 1.5D, pos.getZ() + .5D, Set.of(), 0, 0, false),
					"Could not position player for linked storage fixture at " + pos);
			level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
			player.setYRot(0);
			player.setXRot(0);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			InteractionResult result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
			require(result.consumesAction(), "Could not place linked storage fixture at " + pos + ": " + result + ", mode="
					+ player.gameMode.getGameModeForPlayer() + ", mayBuild=" + player.getAbilities().mayBuild + ", held=" + player.getMainHandItem());
		} finally {
			restorePlayerState(player, playerState);
		}
	}

	private static void placeBlock(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			throw new IllegalStateException("Regression fixture item is not placeable at " + pos);
		}
		level.setBlock(pos, blockItem.getBlock().defaultBlockState(), 3);
		blockItem.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), player, stack);
	}

	private static void clearArea(ServerLevel level, BlockPos anchor) {
		for (int x = -2; x <= 12; x++) {
			for (int y = -1; y <= 2; y++) {
				for (int z = -4; z <= 5; z++) {
					BlockPos pos = anchor.offset(x, y, z);
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
						storage.clearContent();
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		level.getEntitiesOfClass(ItemEntity.class, new AABB(anchor).inflate(12)).forEach(ItemEntity::discard);
	}

	private static BarrelBlockEntity getBarrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing barrel at " + pos));
	}

	private static ChestBlockEntity getChest(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing chest at " + pos));
	}

	private static ShulkerBoxBlockEntity getShulker(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing shulker at " + pos));
	}

	private static LimitedBarrelBlockEntity getLimitedBarrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing limited barrel at " + pos));
	}

	private static LinkedStorageEndpointData requireEndpoint(StorageBlockEntity storage, String name) {
		LinkedStorageEndpointData endpoint = storage.getLinkedStorageEndpointData();
		if (endpoint == null) {
			throw new IllegalStateException("Missing linked endpoint for " + name);
		}
		return endpoint;
	}

	private static boolean hasLimitedRenderProjection(LimitedBarrelBlockEntity barrel) {
		return barrel.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().stream()
				.anyMatch(displayItem -> displayItem.getItem().is(Items.DIAMOND) || displayItem.getItem().is(Items.EMERALD))
				&& barrel.getSlotCounts().stream().anyMatch(count -> count > 0) && barrel.getSlotFillLevels().stream().anyMatch(fill -> fill > 0F);
	}

	private static int count(StorageBlockEntity storage, Item item) {
		return count(storage.getStorageWrapper().getInventoryHandler(), item);
	}

	private static int count(IItemHandler inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			if (inventory.getStackInSlot(slot).is(item)) {
				count += inventory.getStackInSlot(slot).getCount();
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

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for linked storage client state", e);
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}

	private record LinkedStorageFixture(BlockPos anchor, BlockPos primaryPos, UUID groupId, LinkedStorageEndpointData primaryEndpoint, ItemStack carrier) {
	}

	private record PlayerState(Vec3 position, float yRot, float xRot, int selectedSlot, ItemStack mainHandStack) {
	}

	private record LinkedStorageMenuFixture(BlockPos anchor, BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, int inventorySlots, String title,
			PlayerState playerState) {
	}

	private record LinkedLimitedMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, PlayerState playerState) {
	}

	private record LinkedStackFixture(BlockPos pos, UUID groupId, int inventorySlot, ItemStack originalStack, PlayerState playerState) {
	}

	private record DroppedItemFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID entityId, long verifyTime, PlayerState playerState) {
	}

	private record DroppedPrimaryRenameFixture(BlockPos primaryPos, BlockPos secondaryPos, LinkedStorageEndpointData endpoint, String originalName,
			GameType originalGameMode, PlayerState playerState, ItemStack renamedPrimary, ItemStack renamedSecondary) {
	}

	private record LinkedChestFixture(BlockPos primaryPos, BlockPos addedPos, LinkedStorageEndpointData endpoint, PlayerState playerState) {
	}

	private record LinkedControllerOutlineFixture(BlockPos controllerPos, BlockPos primaryPos, BlockPos secondaryPos) {
	}
}
