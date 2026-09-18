package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointProvider;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSnapshotProfile;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkedStorageHostWrapper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
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

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

public final class StorageLinkedStorageRegression {
	private static final int LINKED_LIMITED_RELOAD_ITEM_COUNT = 23;
	private static final int LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT = 7;

	private StorageLinkedStorageRegression() {
	}

	public static String run() {
		try {
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runOrdinaryLinkedStorage);
			runClientSecondaryUpgradeRenderingRegression();
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedShulkerStashRegression);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runCreativeEndpointPlacementRegression);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runEndpointUnloadReloadRegression);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLegacyControllerStorageKeysMigrationRegression);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::restoreLinkedStorageEndpointRejectsUnregisteredEndpoint);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerNonListenerRemovalIndexesRegression);
			runDroppedPrimaryRenameRegression();
			runLinkedStorageMenuTransitionRegression();
			runLinkedUpgradeSwitchRefreshRegression();
			runTierUpgradeMenuInvalidationRegression();
			runComponentlessBarrelItemModelRegression();
			runPhysicalEndpointMenuLedgerRegression();
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runCanonicalStorageTypeInsertionRules);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runStorageFamilyCompatibilityAndCarrierSchema);
			runLinkedStorageMenuCanonicalSnapshotRegression();
			runLinkedPrimaryChestExpansionRegression();
			runSecondaryMemorySyncRegression();
			runLinkedStorageStackTooltipRegression();
			runDroppedItemPickupRegression();
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerCanonicalContents);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedDoubleChestControllerCanonicalContents);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runControllerDoubleChestLockFanoutRegression);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedDoubleChestLifecycle);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerTierCandidateAndPaintRegression);
			runClientControllerTopologyUpdateRegression();
			return "{\"ok\":true,\"ordinaryBarrelLinkingSharesCanonicalContents\":true,\"clientSecondarySuppressesUpgradeRendering\":true,\"linkedShulkerStashUsesCanonicalHostAndPrimaryRules\":true,\"creativePlacementCreatesSecondary\":true,\"endpointUnloadReloadReattachesCanonicalState\":true,\"legacyControllerStorageKeysMigrate\":true,\"unregisteredEndpointFallsBackToLocalMenuAndDoesNotRestore\":true,\"linkedControllerNonListenerRemovalPreservesRoutingIndexes\":true,\"droppedPrimaryNameSynchronizesBeforePlacement\":true,\"componentlessBarrelItemModelDoesNotInheritPreviousRenderState\":true,\"linkedStorageMenuTransitionsRejectStaleActions\":true,\"linkedUpgradeSwitchesRefreshInOpenMenu\":true,\"tierUpgradeClosesStaleLinkedMenus\":true,\"physicalEndpointMenusRecordOpenedEndpoint\":true,\"canonicalStorageTypeControlsInsertionRules\":true,\"limitedStorageFamilyCompatibilityAndCarrierSchema\":true,\"linkedStorageMenuCanonicalSnapshot\":true,\"linkedControllerEndpointJoinKeepsOneCanonicalContentIndex\":true,"
					+ "\"linkedDoubleChestControllerUsesOneCanonicalContentIndex\":true,\"linkedControllerRoutesAndFansOutToolOperations\":true,"
					+ "\"controllerLockUnlocksDoubleChestAndBarrelExactlyOnce\":true,\"clientControllerTopologyUpdatesNaturally\":true,"
					+ "\"controllerRestorationReconnectsLinkedStorage\":true,\"linkedPrimaryExpandsAndClosesStaleMenu\":true,"
					+ "\"secondaryMemorySynchronizes\":true,\"linkedStorageTooltipUsesCanonicalCache\":true,"
					+ "\"secondarySkipsDroppedItemPickup\":true,\"linkedDoubleChestLifecyclePreservesEndpoints\":true,"
					+ "\"linkedControllerPhysicalHighlightsTierPaintAndVisibility\":true}";
		} catch (RuntimeException e) {
			return "{\"ok\":false,\"error\":" + jsonString(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()) + "}";
		}
	}

	public static String setupLinkedLimitedBarrelReloadProjection() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = (ServerLevel) player.level();
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
			ServerLevel level = (ServerLevel) player.level();
			LimitedBarrelBlockEntity primary = limited(level, limitedPrimaryPos(player));
			LimitedBarrelBlockEntity secondary = limited(level, limitedPrimaryPos(player).east(3));
			require(endpoint(primary, "reloaded limited primary").groupId().equals(groupId)
					&& endpoint(secondary, "reloaded limited secondary").groupId().equals(groupId),
					"Reloaded limited barrels do not belong to the expected linked-storage group");
			require(hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary),
					"Reloaded linked limited barrels did not restore server render projection");
			WorldHelper.notifyBlockUpdate(primary);
			WorldHelper.notifyBlockUpdate(secondary);
			return true;
		});
		waitForClientLimitedBarrelReloadProjection();
		return "{\"ok\":true,\"clientDisplayItems\":true,\"clientCounts\":true,\"clientFillLevels\":true}";
	}

	private static Boolean runOrdinaryLinkedStorage(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
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
		if (capability == null)
			return 0;
		try (Transaction transaction = Transaction.openRoot()) {
			long inserted = capability.insert(ItemResource.of(stack), stack.getCount(), transaction);
			transaction.commit();
			return inserted;
		}
	}

	private static Boolean runLinkedShulkerStashRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos shulkerPos = player.blockPosition().offset(0, 0, 28);
		BlockPos barrelPos = shulkerPos.east(3);
		clearArea(level, shulkerPos);
		try {
			place(level, player, shulkerPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			place(level, player, barrelPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			ShulkerBoxBlockEntity shulker = WorldHelper.getBlockEntity(level, shulkerPos, ShulkerBoxBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing linked shulker primary"));
			BarrelBlockEntity barrel = barrel(level, barrelPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, shulker) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, barrel) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not link Shulker-primary stash fixture");
			ItemStack carrier = new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
			shulker.copyLinkedStorageEndpointTo(carrier);
			ShulkerBoxItem shulkerItem = (ShulkerBoxItem) carrier.getItem();
			try (Transaction transaction = Transaction.openRoot()) {
				require(shulkerItem.stash(level.registryAccess(), carrier, ItemResource.of(new ItemStack(Items.DIAMOND)), 7, transaction) == 7,
						"Linked Shulker carrier did not stash into canonical contents");
				transaction.commit();
			}
			require(count(shulker.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
					&& count(barrel.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
					"Linked Shulker carrier stash did not fan out canonical contents");
			try (Transaction transaction = Transaction.openRoot()) {
				require(shulkerItem.stash(level.registryAccess(), carrier, ItemResource.of(new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get())), 1,
						transaction) == 0, "Shulker-primary linked carrier accepted a nested Shulker");
			}
			return true;
		} finally {
			clearArea(level, shulkerPos);
		}
	}

	private static Boolean runCreativeEndpointPlacementRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos sourcePos = player.blockPosition().offset(0, 0, 32);
		BlockPos creativePos = sourcePos.east(3);
		GameType originalGameType = player.gameMode.getGameModeForPlayer();
		clearArea(level, sourcePos);
		try {
			player.setGameMode(GameType.SURVIVAL);
			place(level, player, sourcePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity source = barrel(level, sourcePos);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
					source) == LinkedStorageService.LinkResult.SUCCESS, "Could not create creative placement endpoint fixture");
			LinkedStorageEndpointData sourceEndpoint = endpoint(source, "creative placement source");
			level.destroyBlock(sourcePos, true, player);
			ItemStack endpointDrop = level
					.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos).inflate(1),
							entity -> sourceEndpoint.equals(entity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
					.stream().findFirst().map(entity -> entity.getItem().copy())
					.orElseThrow(() -> new IllegalStateException("Missing linked endpoint drop for creative placement"));
			level.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos).inflate(1)).forEach(ItemEntity::discard);
			player.setGameMode(GameType.CREATIVE);
			placeWithPlayer(level, player, creativePos, endpointDrop);
			LinkedStorageEndpointData creativeEndpoint = endpoint(barrel(level, creativePos), "creative placed endpoint");
			require(creativeEndpoint.groupId().equals(sourceEndpoint.groupId()) && !creativeEndpoint.endpointId().equals(sourceEndpoint.endpointId())
					&& sourceEndpoint.equals(endpointDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Creative placement did not preserve carrier ownership while creating a fresh secondary endpoint");
			return true;
		} finally {
			player.setGameMode(originalGameType);
			clearArea(level, sourcePos);
		}
	}

	private static Boolean runEndpointUnloadReloadRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 36);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos);
		try {
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link unload/reload endpoint fixture");
			LinkedStorageEndpointData primaryEndpoint = endpoint(primary, "unload/reload primary");
			LinkedStorageEndpointData secondaryEndpoint = endpoint(secondary, "unload/reload secondary");
			CompoundTag secondaryData = new CompoundTag();
			secondaryData = ValueIOHelper.collectOutputToTag(level.registryAccess(), secondary::saveAdditional);
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
					"Reloaded linked secondary did not recover its identity and canonical contents");
			CompoundTag primaryData = new CompoundTag();
			primaryData = ValueIOHelper.collectOutputToTag(level.registryAccess(), primary::saveAdditional);
			primary.onChunkUnloaded();
			level.removeBlockEntity(primaryPos);
			BarrelBlockEntity reloadedPrimary = new BarrelBlockEntity(primaryPos, level.getBlockState(primaryPos));
			reloadedPrimary.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), primaryData));
			level.setBlockEntity(reloadedPrimary);
			reloadedPrimary.onLoad();
			reloadedSecondary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.AMETHYST_SHARD, 4));
			reloadedSecondary.getStorageWrapper().getInventoryHandler().saveInventory();
			require(primaryEndpoint.equals(endpoint(reloadedPrimary, "reloaded primary"))
					&& count(reloadedPrimary.getStorageWrapper().getInventoryHandler(), Items.AMETHYST_SHARD) == 4,
					"Reloaded linked primary did not recover its identity and canonical contents");
			return true;
		} finally {
			clearArea(level, primaryPos);
		}
	}

	private static void runClientSecondaryUpgradeRenderingRegression() {
		LinkedRenderRoleFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 8);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link client upgrade render role fixture");
			if (!primary.shouldShowUpgrades()) {
				primary.toggleUpgradesVisiblity();
			}
			return new LinkedRenderRoleFixture(primaryPos, secondaryPos, endpoint(primary, "client render primary").groupId());
		});
		try {
			waitForClientLinkedStorageEndpoint(fixture.groupId(), fixture.primaryPos());
			waitForClientLinkedStorageEndpoint(fixture.groupId(), fixture.secondaryPos());
			Boolean rendersExpectedRoles = AutomationRuntime.runOnClient(() -> {
				StorageBlockEntity primary = (StorageBlockEntity) Minecraft.getInstance().level.getBlockEntity(fixture.primaryPos());
				StorageBlockEntity secondary = (StorageBlockEntity) Minecraft.getInstance().level.getBlockEntity(fixture.secondaryPos());
				return !secondary.shouldRenderUpgrades()
						&& secondary.getStorageWrapper().getLinkedStorageEndpointRole().filter(role -> role == LinkedStorageEndpointRole.SECONDARY).isPresent();
			});
			require(rendersExpectedRoles, "Client synchronized linked-storage role did not suppress secondary upgrade rendering");
		} finally {
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.level(), fixture.primaryPos());
				return "";
			});
		}
	}

	private static Boolean runLegacyControllerStorageKeysMigrationRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 156);
		BlockPos firstStoragePos = controllerPos.east();
		BlockPos secondStoragePos = firstStoragePos.east();
		clearArea(level, controllerPos);
		try {
			place(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			place(level, player, firstStoragePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondStoragePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
					.orElseThrow(() -> new IllegalStateException("Missing controller before legacy storage key migration"));
			barrel(level, secondStoragePos).getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
			barrel(level, secondStoragePos).getStorageWrapper().getInventoryHandler().saveInventory();
			CompoundTag legacyData = controller.getUpdateTag(level.registryAccess());
			legacyData.remove("storageKeys");
			legacyData.remove("storageMemberKeys");
			level.removeBlockEntity(controllerPos);
			ControllerBlockEntity migratedController = new ControllerBlockEntity(controllerPos, level.getBlockState(controllerPos));
			migratedController.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), legacyData));
			level.setBlockEntity(migratedController);
			migratedController.onLoad();
			require(migratedController.getStoragePositions().equals(List.of(firstStoragePos, secondStoragePos))
					&& migratedController.getStackStorages(ItemStackKey.of(new ItemStack(Items.DIAMOND))).contains(secondStoragePos),
					"Absent storageKeys did not rebuild controller routing indexes from legacy storagePositions");
			CompoundTag explicitEmptyKeysData = legacyData.copy();
			explicitEmptyKeysData.put("storageKeys", new ListTag());
			ControllerBlockEntity explicitEmptyKeysController = new ControllerBlockEntity(controllerPos, level.getBlockState(controllerPos));
			explicitEmptyKeysController.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), explicitEmptyKeysData));
			require(explicitEmptyKeysController.getStoragePositions().isEmpty(), "Present empty storageKeys incorrectly migrated legacy storagePositions");
			return true;
		} finally {
			clearArea(level, controllerPos);
		}
	}

	private static Boolean restoreLinkedStorageEndpointRejectsUnregisteredEndpoint(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 40);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos);
		try {
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			primary.setCustomName(Component.literal("Canonical linked group"));
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not create unregistered endpoint fixture");
			require(secondary.getMenuDisplayName().getString().equals("Canonical linked group")
					&& !secondary.getMenuDisplayName().equals(secondary.getDisplayName()),
					"Unregistered endpoint fixture did not expose a canonical title distinct from its local title");
			LinkedStorageEndpointData secondaryEndpoint = endpoint(secondary, "unregistered endpoint secondary");
			ItemStack carrier = new ItemStack(ModBlocks.BARREL_ITEM.get());
			secondary.copyLinkedStorageEndpointTo(carrier);
			var manager = LinkedStorageGroupsSavedData.get(level).manager();
			require(manager.resolveVirtualHost(secondaryEndpoint.groupId()).isPresent()
					&& manager.unregisterEndpoint(secondaryEndpoint.groupId(), secondaryEndpoint.endpointId())
					&& manager.resolveVirtualHost(secondaryEndpoint.groupId()).isPresent()
					&& !manager.isEndpointMember(secondaryEndpoint.groupId(), secondaryEndpoint.endpointId()),
					"Unregistered endpoint fixture did not retain its group host while removing the endpoint membership");
			require(secondary.getMenuDisplayName().equals(secondary.getDisplayName()), "Unregistered endpoint exposed the canonical linked storage menu title");
			FriendlyByteBuf menuData = new FriendlyByteBuf(Unpooled.buffer());
			secondary.writeLinkedStorageMenuData(menuData);
			require(!menuData.readBoolean(), "Unregistered endpoint wrote a linked storage menu snapshot");

			secondary.restoreLinkedStorageEndpoint(level, carrier);

			require(!secondary.isLinkedStorage() && secondary.getLinkedStorageEndpointData() == null,
					"Unregistered endpoint restored against its remaining group host");
			return true;
		} finally {
			clearArea(level, primaryPos);
		}
	}

	private static Boolean runLinkedControllerNonListenerRemovalIndexesRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 40);
		BlockPos primaryPos = controllerPos.east();
		BlockPos secondaryPos = primaryPos.east();
		clearArea(level, controllerPos);
		try {
			placeWithPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeWithPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link non-listener controller fixture");
			placeWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			ControllerBlockEntity controller = controller(level, controllerPos);
			require(controller.getStoragePositions().equals(List.of(primaryPos)), "Controller did not select the linked primary as its listener");
			level.destroyBlock(secondaryPos, false, player);
			require(controller.getStoragePositions().equals(List.of(primaryPos)), "Destroying a linked non-listener changed the surviving controller group");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(controller.hasMatchingStack(diamondKey) && controller.getStackStorages(diamondKey).equals(List.of(primaryPos)),
					"Surviving linked listener did not update the controller actual-item index");
			MemorySettingsCategory memory = primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.EMERALD));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			memory.selectSlots(1, 2);
			ItemStackKey emeraldKey = ItemStackKey.of(new ItemStack(Items.EMERALD));
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(1, ItemStack.EMPTY);
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			require(controller.hasMatchingItem(Items.EMERALD) && controller.getItemStorages(emeraldKey).equals(List.of(primaryPos)),
					"Surviving linked listener did not update the controller memory index");
			UpgradeHandler upgrades = primary.getStorageWrapper().getUpgradeHandler();
			upgrades.setStackInSlot(0, new ItemStack(ModItems.ADVANCED_FILTER_UPGRADE.get()));
			FilterUpgradeWrapper filter = upgrades.getWrappersThatImplement(FilterUpgradeWrapper.class).stream().findFirst()
					.orElseThrow(() -> new IllegalStateException("Missing controller survival Advanced Filter Upgrade wrapper"));
			filter.setDirection(net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction.INPUT);
			filter.getFilterLogic().setDepositFilterType(ContentsFilterType.ALLOW);
			filter.getFilterLogic().setPrimaryMatch(PrimaryMatch.ITEM);
			filter.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
			upgrades.saveInventory();
			primary.getStorageWrapper().refreshInventoryForInputOutput();
			ItemStackKey goldKey = ItemStackKey.of(new ItemStack(Items.GOLD_INGOT));
			require(controller.hasMatchingFilter(new ItemStack(Items.GOLD_INGOT)) && controller.getEmptyTargetSlotStorages(goldKey).contains(primaryPos),
					"Surviving linked listener did not update the controller Gold filter or accepting empty-target index");
			return true;
		} finally {
			clearArea(level, controllerPos);
		}
	}

	private static void runDroppedPrimaryRenameRegression() {
		DroppedPrimaryRenameCleanup cleanup = AutomationRuntime.runOnServer(player -> new DroppedPrimaryRenameCleanup(player.blockPosition().offset(0, 0, 44),
				player.getMainHandItem().copy(), player.getYRot(), player.getXRot(), player.position()));
		RuntimeException originalFailure = null;
		try {
			DroppedPrimaryRenameFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupDroppedPrimaryRenameRegression);
			AutomationRuntime.runOnServer(player -> {
				ensureFixtureSupportPlatform((ServerLevel) player.level(), fixture.secondaryPos().south(2));
				Vec3 interactionPosition = fixture.secondaryPos().south(2).getCenter();
				require(player.teleportTo((ServerLevel) player.level(), interactionPosition.x, interactionPosition.y, interactionPosition.z, Set.of(),
						player.getYRot(), player.getXRot(), false), "Could not move player beside dropped-primary rename secondary");
				return true;
			});
			waitForClientPlayerPosition(fixture.secondaryPos().south(2).getCenter());
			waitForClientLinkedStorageEndpoint(fixture.secondaryEndpoint(), fixture.secondaryPos(), () -> describeDroppedPrimaryRenameClientState(fixture));
			openDroppedPrimaryRenameStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			waitForDroppedPrimaryRenameStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedStorageMenu();
			waitForClientLinkedStorageEndpoint(fixture.primaryEndpoint(), fixture.primaryPos(), () -> describeDroppedPrimaryRenameClientState(fixture));
			openDroppedPrimaryRenameStorageMenu(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			waitForDroppedPrimaryRenameStorageMenu(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedStorageMenu();
			AutomationRuntime.runOnServer(player -> finalizeDroppedPrimaryRenameRegression(player, fixture));
		} catch (RuntimeException e) {
			originalFailure = e;
			throw e;
		} finally {
			RuntimeException cleanupFailure = null;
			try {
				AutomationRuntime.runOnServer(player -> {
					player.closeContainer();
					return true;
				});
				waitForClosedStorageMenu();
			} catch (RuntimeException e) {
				cleanupFailure = e;
			}
			try {
				AutomationRuntime.runOnServer(player -> {
					ServerLevel level = player.level();
					player.setItemInHand(InteractionHand.MAIN_HAND, cleanup.originalMainHand());
					player.inventoryMenu.broadcastChanges();
					player.setYRot(cleanup.originalYRot());
					player.setXRot(cleanup.originalXRot());
					require(player.teleportTo(level, cleanup.originalPosition().x, cleanup.originalPosition().y, cleanup.originalPosition().z, Set.of(),
							cleanup.originalYRot(), cleanup.originalXRot(), false),
							"Could not restore player position after dropped-primary rename regression");
					return true;
				});
			} catch (RuntimeException e) {
				if (cleanupFailure != null) {
					cleanupFailure.addSuppressed(e);
				} else {
					cleanupFailure = e;
				}
			}
			try {
				AutomationRuntime.runOnServer(player -> {
					clearArea((ServerLevel) player.level(), cleanup.primaryPos());
					return true;
				});
			} catch (RuntimeException e) {
				if (cleanupFailure != null) {
					cleanupFailure.addSuppressed(e);
				} else {
					cleanupFailure = e;
				}
			}
			if (cleanupFailure != null) {
				if (originalFailure != null) {
					originalFailure.addSuppressed(cleanupFailure);
				} else {
					throw cleanupFailure;
				}
			}
		}
	}

	private static DroppedPrimaryRenameFixture setupDroppedPrimaryRenameRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 44);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos);
		placeWithPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeWithPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		BarrelBlockEntity primary = barrel(level, primaryPos);
		require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link dropped-primary rename fixture");
		LinkedStorageEndpointData primaryEndpoint = endpoint(primary, "rename primary");
		LinkedStorageEndpointData secondaryEndpoint = endpoint(barrel(level, secondaryPos), "rename secondary");
		level.destroyBlock(primaryPos, true, player);
		level.destroyBlock(secondaryPos, true, player);
		ItemStack primaryCarrier = level
				.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos).inflate(1),
						entity -> primaryEndpoint.equals(entity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
				.stream().findFirst().map(entity -> entity.getItem().copy()).orElseThrow(() -> new IllegalStateException("Missing dropped primary carrier"));
		ItemStack secondaryCarrier = level
				.getEntitiesOfClass(ItemEntity.class, new AABB(secondaryPos).inflate(1),
						entity -> secondaryEndpoint.equals(entity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
				.stream().findFirst().map(entity -> entity.getItem().copy()).orElseThrow(() -> new IllegalStateException("Missing dropped secondary carrier"));
		require(!primaryCarrier.has(ModCoreDataComponents.STORAGE_UUID)
				&& StorageBlockEntity.getLinkedStorageEndpointRole(primaryCarrier).filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent(),
				"Dropped linked primary did not retain endpoint-only primary identity");
		String originalCanonicalName = LinkedStorageGroupsSavedData.get(level).manager().resolveVirtualHost(primaryEndpoint.groupId())
				.flatMap(net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost::getLinkedStorageDisplayName).map(Component::getString)
				.orElse("");
		String secondaryName = "Secondary name must not propagate";
		secondaryCarrier.set(DataComponents.CUSTOM_NAME, Component.literal(secondaryName));
		player.setItemInHand(InteractionHand.MAIN_HAND, secondaryCarrier);
		player.getInventory().tick();
		require(LinkedStorageGroupsSavedData.get(level).manager().resolveVirtualHost(primaryEndpoint.groupId())
				.flatMap(net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost::getLinkedStorageDisplayName).map(Component::getString)
				.orElse("").equals(originalCanonicalName), "Secondary carrier name incorrectly propagated to the canonical linked group");
		String renamed = "Renamed linked primary";
		primaryCarrier.set(DataComponents.CUSTOM_NAME, Component.literal(renamed));
		player.setItemInHand(InteractionHand.MAIN_HAND, primaryCarrier);
		player.getInventory().tick();
		require(LinkedStorageGroupsSavedData.get(level).manager().resolveVirtualHost(primaryEndpoint.groupId())
				.flatMap(net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost::getLinkedStorageDisplayName).map(Component::getString)
				.filter(renamed::equals).isPresent(), "Primary carrier inventory tick did not propagate the canonical linked group title");
		level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos).inflate(1)).forEach(ItemEntity::discard);
		placeWithPlayer(level, player, secondaryPos, secondaryCarrier);
		placeWithPlayer(level, player, primaryPos, primaryCarrier);
		BarrelBlockEntity restored = barrel(level, primaryPos);
		BarrelBlockEntity restoredSecondary = barrel(level, secondaryPos);
		require(primaryEndpoint.equals(endpoint(restored, "renamed restored primary")) && restored.getDisplayName().getString().equals(renamed)
				&& secondaryEndpoint.equals(endpoint(restoredSecondary, "renamed restored secondary")),
				"Replaced renamed primary did not preserve its canonical name and endpoint identity");
		return new DroppedPrimaryRenameFixture(primaryPos, secondaryPos, primaryEndpoint.groupId(), primaryEndpoint, secondaryEndpoint, renamed);
	}

	private static void openDroppedPrimaryRenameStorageMenu(DroppedPrimaryRenameFixture fixture, BlockPos pos, LinkedStorageEndpointRole expectedRole) {
		AutomationRuntime.runOnServer(player -> {
			openStorageWithPlayer(player, pos);
			require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(pos))
					&& fixture.endpointAt(pos).equals(menu.getStorageBlockEntity().getLinkedStorageEndpointData())
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
					&& provider.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent()
					&& menu.getStorageBlockEntity().getMenuDisplayName().getString().equals(fixture.renamed()),
					"Replaced " + expectedRole.name().toLowerCase() + " menu did not retain its role and canonical title");
			return true;
		});
	}

	private static void waitForDroppedPrimaryRenameStorageMenu(DroppedPrimaryRenameFixture fixture, BlockPos pos, LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(pos))
					&& fixture.endpointAt(pos).equals(menu.getStorageBlockEntity().getLinkedStorageEndpointData())
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
					&& provider.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent()
					&& screen.getTitle().getString().equals(fixture.renamed())
					&& ClientLinkedStorageContents.getGroupName(fixture.groupId()).filter(Component.literal(fixture.renamed())::equals).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for exact renamed " + expectedRole.name().toLowerCase() + " linked client Storage menu: "
				+ describeDroppedPrimaryRenameClientState(fixture));
	}

	private static Boolean finalizeDroppedPrimaryRenameRegression(ServerPlayer player, DroppedPrimaryRenameFixture fixture) {
		ServerLevel level = player.level();
		level.destroyBlock(fixture.primaryPos(), true, player);
		ItemStack renamedRedrop = level
				.getEntitiesOfClass(ItemEntity.class, new AABB(fixture.primaryPos()).inflate(1),
						entity -> fixture.primaryEndpoint().equals(entity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
				.stream().findFirst().map(entity -> entity.getItem().copy())
				.orElseThrow(() -> new IllegalStateException("Missing re-dropped renamed primary carrier"));
		require(renamedRedrop.get(DataComponents.CUSTOM_NAME) instanceof Component name && name.getString().equals(fixture.renamed()),
				"Re-dropped primary carrier did not preserve its synchronized canonical title");
		return true;
	}

	private record DroppedPrimaryRenameCleanup(BlockPos primaryPos, ItemStack originalMainHand, float originalYRot, float originalXRot, Vec3 originalPosition) {
	}

	private record DroppedPrimaryRenameFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, LinkedStorageEndpointData primaryEndpoint,
			LinkedStorageEndpointData secondaryEndpoint, String renamed) {
		private LinkedStorageEndpointData endpointAt(BlockPos pos) {
			if (pos.equals(primaryPos))
				return primaryEndpoint;
			if (pos.equals(secondaryPos))
				return secondaryEndpoint;
			throw new IllegalArgumentException("No dropped-primary rename endpoint at " + pos);
		}
	}

	private static void runComponentlessBarrelItemModelRegression() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			ItemStack componentless = new ItemStack(ModBlocks.BARREL_ITEM.get());
			TrackingItemStackRenderState expected = renderBarrelItemModel(minecraft, componentless);
			Object expectedIdentity = expected.getModelIdentity();
			ItemStack tinted = new ItemStack(ModBlocks.BARREL_ITEM.get());
			StorageBlockItem tintedItem = (StorageBlockItem) tinted.getItem();
			tintedItem.setMainColor(tinted, 0xFF00FF);
			tintedItem.setAccentColor(tinted, 0x00FFFF);
			TrackingItemStackRenderState tintedState = renderBarrelItemModel(minecraft, tinted);
			TrackingItemStackRenderState resolved = renderBarrelItemModel(minecraft, componentless);
			require(!expected.isEmpty() && !tintedState.isEmpty() && !resolved.isEmpty() && !expectedIdentity.equals(tintedState.getModelIdentity())
					&& expectedIdentity.equals(resolved.getModelIdentity()), "Componentless barrel item inherited tinted model identity");
			return true;
		});
	}

	private static void runLinkedStorageMenuTransitionRegression() {
		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			return true;
		});
		waitForClosedStorageMenu();
		MenuTransitionFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedStorageMenuTransitionRegression);
		try {
			AutomationRuntime.runOnServer(player -> {
				Vec3 interactionPosition = fixture.secondaryPos().south(2).getCenter();
				require(player.teleportTo((ServerLevel) player.level(), interactionPosition.x, interactionPosition.y, interactionPosition.z, Set.of(),
						player.getYRot(), player.getXRot(), false), "Could not move player beside linked storage transition secondary");
				return true;
			});
			waitForClientPlayerPosition(fixture.secondaryPos().south(2).getCenter());
			waitForClientLinkedStorageEndpoint(fixture.groupId(), fixture.primaryPos());
			waitForClientLinkedStorageEndpoint(fixture.groupId(), fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				openStorageWithPlayer(player, fixture.secondaryPos());
				return true;
			});
			waitForClientStorageMenu(fixture);
			AutomationRuntime.runOnServer(player -> {
				if (!(player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Linked storage inventory menu did not open before stale action test");
				}
				CompoundTag staleAction = new CompoundTag();
				staleAction.putString("action", "openSettings");
				staleAction.putInt("sourceContainerId", menu.containerId + 1);
				menu.handlePacket(staleAction);
				require(player.containerMenu == menu, "Stale linked-storage settings action replaced the active endpoint menu");
				return true;
			});
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				require(minecraft.screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu,
						"Client linked Storage menu was not active before settings transition");
				StorageContainerMenu menu = (StorageContainerMenu) ((AbstractContainerScreen<?>) minecraft.screen).getMenu();
				require(menu.getBlockPosition().equals(Optional.of(fixture.secondaryPos())),
						"Client linked Storage menu was not active at the secondary endpoint before settings transition");
				menu.openSettings();
				return true;
			});
			waitForClientSettingsMenu(fixture);
			AutomationRuntime.runOnClient(() -> {
				ClientPacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.primaryPos()));
				return true;
			});
			long stalePayloadVerificationTime = AutomationRuntime.runOnServer(player -> ((ServerLevel) player.level()).getGameTime() + 2L);
			waitForServerCondition(player -> ((ServerLevel) player.level()).getGameTime() >= stalePayloadVerificationTime
					&& player.containerMenu instanceof StorageSettingsContainerMenu settingsMenu
					&& settingsMenu.getBlockPosition().equals(fixture.secondaryPos()));
			waitForClientSettingsMenu(fixture);
			AutomationRuntime.runOnClient(() -> {
				ClientPacketDistributor.sendToServer(new OpenStorageInventoryPayload(fixture.secondaryPos()));
				return true;
			});
			waitForClientStorageMenu(fixture);
			AutomationRuntime.runOnServer(player -> {
				BarrelBlockEntity secondary = barrel((ServerLevel) player.level(), fixture.secondaryPos());
				require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(fixture.secondaryPos()))
						&& menu.getStorageWrapper() == secondary.getStorageWrapper()
						&& menu.getStorageWrapper().getInventoryHandler().size() == fixture.expectedSlots()
						&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT
						&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
						&& provider.getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.SECONDARY::equals).isPresent() && secondary.isOpen(),
						"Linked settings transition did not restore the canonical secondary storage menu and opener");
				player.closeContainer();
				return true;
			});
			waitForClosedStorageMenu();
			waitForServerCondition(player -> !barrel((ServerLevel) player.level(), fixture.secondaryPos()).isOpen());
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalMainHand());
				player.setYRot(fixture.originalYRot());
				player.setXRot(fixture.originalXRot());
				player.teleportTo((ServerLevel) player.level(), fixture.originalPosition().x, fixture.originalPosition().y, fixture.originalPosition().z,
						Set.of(), fixture.originalYRot(), fixture.originalXRot(), false);
				clearArea((ServerLevel) player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static MenuTransitionFixture setupLinkedStorageMenuTransitionRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 80);
		BlockPos secondaryPos = primaryPos.east(3);
		ItemStack originalMainHand = player.getMainHandItem().copy();
		float originalYRot = player.getYRot();
		float originalXRot = player.getXRot();
		Vec3 originalPosition = player.position();
		clearArea(level, primaryPos);
		try {
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link stale settings-menu fixture");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			return new MenuTransitionFixture(primaryPos, secondaryPos, endpoint(primary, "settings transition primary").groupId(),
					primary.getStorageWrapper().getInventoryHandler().size(), originalMainHand, originalYRot, originalXRot, originalPosition);
		} catch (RuntimeException e) {
			player.closeContainer();
			player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
			player.setYRot(originalYRot);
			player.setXRot(originalXRot);
			player.teleportTo(level, originalPosition.x, originalPosition.y, originalPosition.z, Set.of(), originalYRot, originalXRot, false);
			clearArea(level, primaryPos);
			throw e;
		}
	}

	private static void waitForClientPlayerPosition(Vec3 expectedPosition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.position().distanceToSqr(expectedPosition) < 0.25D)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for client player to reach linked storage transition fixture");
	}

	private static void waitForClientStorageMenu(MenuTransitionFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(fixture.secondaryPos()))
					&& menu.getStorageBlockEntity().getLinkedStorageEndpointData() != null
					&& menu.getStorageBlockEntity().getLinkedStorageEndpointData().groupId().equals(fixture.groupId())
					&& menu.getStorageWrapper().getInventoryHandler().size() == fixture.expectedSlots()
					&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
					&& provider.getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.SECONDARY::equals).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for exact canonical secondary linked client Storage menu before settings transition");
	}

	private static void waitForClientSettingsMenu(MenuTransitionFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof StorageSettingsContainerMenu menu && menu.getBlockPosition().equals(fixture.secondaryPos())
					&& menu.getStorageWrapper().getInventoryHandler().size() == fixture.expectedSlots()
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
					&& provider.getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.SECONDARY::equals).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for exact linked client Storage settings menu");
	}

	private record MenuTransitionFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, int expectedSlots, ItemStack originalMainHand,
			float originalYRot, float originalXRot, Vec3 originalPosition) {
	}

	private static void runLinkedUpgradeSwitchRefreshRegression() {
		UpgradeRefreshFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedUpgradeSwitchRefreshRegression);
		try {
			waitForClientLinkedStorageEndpoint(fixture.groupId(), fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				StorageBlockEntity secondary = barrel((ServerLevel) player.level(), fixture.secondaryPos());
				openStorageWithPlayer(player, fixture.secondaryPos());
				require(player.containerMenu instanceof StorageContainerMenu menu && menu.getStorageWrapper() == secondary.getStorageWrapper(),
						"Linked endpoint menu did not retain its physical facade before upgrade refresh");
				return true;
			});
			waitForClientUpgradeRefreshMenu(fixture);
			UpgradeRefreshClientBaseline baseline = AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				require(minecraft.player != null && minecraft.gameMode != null && minecraft.screen instanceof AbstractContainerScreen<?>
						&& ((AbstractContainerScreen<?>) minecraft.screen).getMenu() instanceof StorageContainerMenu,
						"Client linked upgrade-refresh menu was not active");
				StorageContainerMenu menu = (StorageContainerMenu) ((AbstractContainerScreen<?>) minecraft.screen).getMenu();
				int playerUpgradeSlot = -1;
				for (int slot = 0; slot < menu.slots.size(); slot++) {
					if (menu.getSlot(slot).container == minecraft.player.getInventory() && menu.getSlot(slot).getContainerSlot() == fixture.playerUpgradeSlot()
							&& menu.getSlot(slot).getItem().is(ModItems.ADVANCED_FILTER_UPGRADE.get())) {
						playerUpgradeSlot = slot;
						break;
					}
				}
				require(playerUpgradeSlot >= 0, "Client linked menu did not contain the Advanced Filter Upgrade in the expected player inventory slot");
				UpgradeRefreshClientBaseline beforeClick = new UpgradeRefreshClientBaseline(
						ClientLinkedStorageContents.getRevision(fixture.groupId())
								.orElseThrow(() -> new IllegalStateException("Missing client linked revision before upgrade click")),
						menu.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).copy(), menu.getUpgradeEnabled(0),
						menu.getStorageWrapper().getSettingsHandler().getSettingsData().hashCode());
				require(beforeClick.upgradeStack().isEmpty(), "Upgrade-refresh client baseline already contains an installed upgrade");
				minecraft.gameMode.handleContainerInput(menu.containerId, playerUpgradeSlot, 0, ContainerInput.PICKUP, minecraft.player);
				minecraft.gameMode.handleContainerInput(menu.containerId, menu.getFirstUpgradeSlot(), 0, ContainerInput.PICKUP, minecraft.player);
				return beforeClick;
			});
			waitForClientUpgradeRefreshApplied(fixture, baseline);
			AutomationRuntime.runOnServer(player -> {
				if (!(player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Server linked upgrade-refresh menu closed before verification");
				}
				require(menu.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).is(ModItems.ADVANCED_FILTER_UPGRADE.get()),
						"Client upgrade-slot click did not update canonical server upgrade contents");
				return true;
			});
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().setItem(fixture.playerUpgradeSlot(), fixture.originalPlayerUpgradeStack());
				player.inventoryMenu.broadcastChanges();
				clearArea((ServerLevel) player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static UpgradeRefreshFixture setupLinkedUpgradeSwitchRefreshRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 3);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos);
		try {
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link upgrade-refresh fixture");
			int playerUpgradeSlot = player.getInventory().getSelectedSlot() == 0 ? 1 : 0;
			ItemStack originalPlayerUpgradeStack = player.getInventory().getItem(playerUpgradeSlot).copy();
			player.getInventory().setItem(playerUpgradeSlot, new ItemStack(ModItems.ADVANCED_FILTER_UPGRADE.get()));
			player.inventoryMenu.broadcastChanges();
			return new UpgradeRefreshFixture(primaryPos, secondaryPos, endpoint(primary, "upgrade refresh primary").groupId(), playerUpgradeSlot,
					originalPlayerUpgradeStack);
		} catch (RuntimeException e) {
			player.closeContainer();
			clearArea(level, primaryPos);
			throw e;
		}
	}

	private static void waitForClientUpgradeRefreshMenu(UpgradeRefreshFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu menu
							&& menu.getBlockPosition().equals(Optional.of(fixture.secondaryPos())) && Minecraft.getInstance().player != null
							&& menu.slots.stream().anyMatch(slot -> slot.container == Minecraft.getInstance().player.getInventory()
									&& slot.getContainerSlot() == fixture.playerUpgradeSlot() && slot.getItem().is(ModItems.ADVANCED_FILTER_UPGRADE.get())))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for ordinary linked client upgrade-refresh menu");
	}

	private static void waitForClientUpgradeRefreshApplied(UpgradeRefreshFixture fixture, UpgradeRefreshClientBaseline baseline) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof StorageContainerMenu menu && menu.getBlockPosition().equals(Optional.of(fixture.secondaryPos()))
					&& menu.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).is(ModItems.ADVANCED_FILTER_UPGRADE.get()) && menu.getUpgradeEnabled(0)
					&& ClientLinkedStorageContents.getRevision(fixture.groupId()).orElse(-1L) > baseline.revision()
					&& (!ItemStack.matches(baseline.upgradeStack(), menu.getStorageWrapper().getUpgradeHandler().getStackInSlot(0))
							|| baseline.upgradeEnabled() != menu.getUpgradeEnabled(0)
							|| baseline.settingsHash() != menu.getStorageWrapper().getSettingsHandler().getSettingsData().hashCode()))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for client upgrade controls and linked snapshot refresh");
	}

	private record UpgradeRefreshClientBaseline(long revision, ItemStack upgradeStack, boolean upgradeEnabled, int settingsHash) {
	}

	private static void runTierUpgradeMenuInvalidationRegression() {
		runTierUpgradeMenuInvalidationRegression(LinkedStorageEndpointRole.PRIMARY);
		runTierUpgradeMenuInvalidationRegression(LinkedStorageEndpointRole.SECONDARY);
	}

	private static void runTierUpgradeMenuInvalidationRegression(LinkedStorageEndpointRole openMenuRole) {
		TierUpgradeMenuInvalidationFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupTierUpgradeMenuInvalidationFixture);
		RuntimeException originalFailure = null;
		try {
			BlockPos openMenuPos = openMenuRole == LinkedStorageEndpointRole.PRIMARY ? fixture.primaryPos() : fixture.secondaryPos();
			AutomationRuntime.runOnServer(player -> {
				Vec3 interactionPosition = fixture.primaryPos().south(2).getCenter();
				require(player.teleportTo((ServerLevel) player.level(), interactionPosition.x, interactionPosition.y, interactionPosition.z, Set.of(),
						player.getYRot(), player.getXRot(), false), "Could not move player beside tier-menu invalidation primary");
				return true;
			});
			waitForClientPlayerPosition(fixture.primaryPos().south(2).getCenter());
			waitForClientLinkedStorageEndpoint(fixture.initialMenuFixture(), openMenuPos);
			openLinkedStorageMenuCanonicalSnapshot(fixture.initialMenuFixture(), openMenuPos, openMenuRole);
			waitForLinkedStorageMenuCanonicalSnapshot(fixture.initialMenuFixture(), openMenuPos, openMenuRole);
			if (openMenuRole == LinkedStorageEndpointRole.PRIMARY) {
				AutomationRuntime.runOnServer(player -> rejectOpenPrimaryTierUpgrade(player, fixture));
				AutomationRuntime.runOnServer(player -> {
					player.closeContainer();
					return true;
				});
				waitForClosedStorageMenu();
			}
			LinkedStorageMenuSnapshotFixture upgradedMenuFixture = AutomationRuntime
					.runOnServer(player -> upgradeTierAndCaptureMenuFixture(player, fixture, openMenuRole));
			waitForClosedStorageMenu();
			waitForClientLinkedStorageEndpoint(upgradedMenuFixture, fixture.secondaryPos());
			openLinkedStorageMenuCanonicalSnapshot(upgradedMenuFixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			waitForLinkedStorageMenuCanonicalSnapshot(upgradedMenuFixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedStorageMenu();
		} catch (RuntimeException e) {
			originalFailure = e;
			throw e;
		} finally {
			try {
				AutomationRuntime.runOnServer(player -> {
					ServerLevel level = player.level();
					player.closeContainer();
					player.setGameMode(fixture.originalGameType());
					require(player.teleportTo(level, fixture.originalPosition().x, fixture.originalPosition().y, fixture.originalPosition().z, Set.of(),
							player.getYRot(), player.getXRot(), false), "Could not restore player position after tier-menu invalidation regression");
					return true;
				});
				waitForClosedStorageMenu();
				AutomationRuntime.runOnServer(player -> {
					clearArea(player.level(), fixture.primaryPos());
					return true;
				});
			} catch (RuntimeException cleanupFailure) {
				if (originalFailure != null) {
					originalFailure.addSuppressed(cleanupFailure);
				} else {
					throw cleanupFailure;
				}
			}
		}
	}

	private static TierUpgradeMenuInvalidationFixture setupTierUpgradeMenuInvalidationFixture(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 60);
		BlockPos secondaryPos = primaryPos.east(3);
		GameType originalGameType = player.gameMode.getGameModeForPlayer();
		Vec3 originalPosition = player.position();
		clearArea(level, primaryPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeWithPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeWithPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link tier-menu invalidation fixture");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			LinkedStorageMenuSnapshotFixture initialMenuFixture = new LinkedStorageMenuSnapshotFixture(primaryPos, secondaryPos,
					endpoint(primary, "tier-menu invalidation primary").groupId(), tierUpgradeMenuProfile(primary));
			player.setGameMode(GameType.SURVIVAL);
			return new TierUpgradeMenuInvalidationFixture(primaryPos, secondaryPos, initialMenuFixture, originalGameType, originalPosition);
		} catch (RuntimeException e) {
			player.closeContainer();
			player.setGameMode(originalGameType);
			clearArea(level, primaryPos);
			throw e;
		}
	}

	private static Boolean rejectOpenPrimaryTierUpgrade(ServerPlayer player, TierUpgradeMenuInvalidationFixture fixture) {
		ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
		BarrelBlockEntity primary = barrel(player.level(), fixture.primaryPos());
		require(player.containerMenu instanceof StorageContainerMenu menu && menu.getStorageBlockEntity() == primary && primary.isOpen(),
				"Primary linked tier-upgrade rejection did not retain the active physical-primary menu");
		player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()), Direction.UP, fixture.primaryPos(), false));
		require(player.level().getBlockState(fixture.primaryPos()).is(ModBlocks.BARREL.get())
				&& player.getMainHandItem().is(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get()) && player.getMainHandItem().getCount() == 1
				&& player.containerMenu instanceof StorageContainerMenu menu && menu.getStorageBlockEntity() == primary && primary.isOpen(),
				"Primary linked tier upgrade did not reject an open primary menu");
		return true;
	}

	private static LinkedStorageMenuSnapshotFixture upgradeTierAndCaptureMenuFixture(ServerPlayer player, TierUpgradeMenuInvalidationFixture fixture,
			LinkedStorageEndpointRole openMenuRole) {
		ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
		InteractionResult result = player.gameMode.useItemOn(player, player.level(), upgrade, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()), Direction.UP, fixture.primaryPos(), false));
		require(result.consumesAction() && !(player.containerMenu instanceof StorageContainerMenu),
				"Primary tier upgrade did not invalidate the open linked " + openMenuRole.name().toLowerCase() + " menu");
		BarrelBlockEntity upgradedPrimary = barrel(player.level(), fixture.primaryPos());
		return new LinkedStorageMenuSnapshotFixture(fixture.primaryPos(), fixture.secondaryPos(), fixture.initialMenuFixture().groupId(),
				tierUpgradeMenuProfile(upgradedPrimary));
	}

	private static LinkedStorageSnapshotProfile tierUpgradeMenuProfile(BarrelBlockEntity primary) {
		return new LinkedStorageSnapshotProfile(primary.getMenuDisplayName(), primary.getStorageWrapper().getInventoryHandler().size(),
				primary.getStorageWrapper().getUpgradeHandler().size(), primary.getStorageWrapper().getColumnsTaken());
	}

	private static TrackingItemStackRenderState renderBarrelItemModel(Minecraft minecraft, ItemStack stack) {
		TrackingItemStackRenderState state = new TrackingItemStackRenderState();
		minecraft.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, minecraft.level, minecraft.player, 0);
		return state;
	}

	private static void runPhysicalEndpointMenuLedgerRegression() {
		PhysicalEndpointMenuLedgerFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupPhysicalEndpointMenuLedgerRegression);
		RuntimeException originalFailure = null;
		try {
			waitForClientLinkedStorageEndpoint(fixture.barrelEndpoint().groupId(), fixture.barrelSecondaryPos());
			AutomationRuntime.runOnServer(player -> openAndVerifyPhysicalEndpointMenu(player, fixture.barrelSecondaryPos(), fixture.barrelEndpoint(),
					StorageContainerMenu.class, "Ordinary barrel"));
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedStorageMenu();

			waitForClientLinkedStorageEndpoint(fixture.limitedEndpoint().groupId(), fixture.limitedSecondaryPos());
			AutomationRuntime.runOnServer(player -> openAndVerifyPhysicalEndpointMenu(player, fixture.limitedSecondaryPos(), fixture.limitedEndpoint(),
					LimitedBarrelContainerMenu.class, "Limited barrel"));
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedStorageMenu();
		} catch (RuntimeException e) {
			originalFailure = e;
			throw e;
		} finally {
			try {
				AutomationRuntime.runOnServer(player -> {
					player.closeContainer();
					return true;
				});
				waitForClosedStorageMenu();
				AutomationRuntime.runOnServer(player -> {
					clearArea((ServerLevel) player.level(), fixture.barrelPrimaryPos());
					return true;
				});
			} catch (RuntimeException cleanupFailure) {
				if (originalFailure != null) {
					originalFailure.addSuppressed(cleanupFailure);
				} else {
					throw cleanupFailure;
				}
			}
		}
	}

	private static PhysicalEndpointMenuLedgerFixture setupPhysicalEndpointMenuLedgerRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos barrelPrimaryPos = player.blockPosition().offset(0, 0, 24);
		BlockPos barrelSecondaryPos = barrelPrimaryPos.east(3);
		BlockPos limitedPrimaryPos = barrelPrimaryPos.east(6);
		BlockPos limitedSecondaryPos = limitedPrimaryPos.east(3);
		clearArea(level, barrelPrimaryPos);
		try {
			placeWithPlayer(level, player, barrelPrimaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeWithPlayer(level, player, barrelSecondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity barrelPrimary = barrel(level, barrelPrimaryPos);
			require(linkWithPlayer(player, barrelPrimary, barrelPrimaryPos, barrelSecondaryPos), "Could not link ordinary barrel menu endpoints");
			StorageBlockEntity barrelSecondary = WorldHelper.getBlockEntity(level, barrelSecondaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing ordinary barrel menu endpoint"));
			LinkedStorageEndpointData barrelEndpoint = endpoint(barrelSecondary, "ordinary barrel menu endpoint");

			placeWithPlayer(level, player, limitedPrimaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			placeWithPlayer(level, player, limitedSecondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity limitedPrimary = limited(level, limitedPrimaryPos);
			require(linkWithPlayer(player, limitedPrimary, limitedPrimaryPos, limitedSecondaryPos), "Could not link limited barrel menu endpoints");
			StorageBlockEntity limitedSecondary = WorldHelper.getBlockEntity(level, limitedSecondaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing limited barrel menu endpoint"));
			LinkedStorageEndpointData limitedEndpoint = endpoint(limitedSecondary, "limited barrel menu endpoint");
			return new PhysicalEndpointMenuLedgerFixture(barrelPrimaryPos, barrelSecondaryPos, barrelEndpoint, limitedSecondaryPos, limitedEndpoint);
		} catch (RuntimeException e) {
			clearArea(level, barrelPrimaryPos);
			throw e;
		}
	}

	private static Boolean openAndVerifyPhysicalEndpointMenu(ServerPlayer player, BlockPos secondaryPos, LinkedStorageEndpointData endpoint,
			Class<? extends StorageContainerMenu> menuType, String storageType) {
		ServerLevel level = player.level();
		StorageBlockEntity secondary = WorldHelper.getBlockEntity(level, secondaryPos, StorageBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Missing " + storageType.toLowerCase() + " menu endpoint"));
		require(endpoint.equals(endpoint(secondary, storageType.toLowerCase() + " menu endpoint")),
				storageType + " endpoint identity changed before opening its menu");
		requireEndpointOpening(findEndpointOpening(level, endpoint), null, -1L, storageType + " endpoint was already marked open");
		long openedAt = level.getGameTime();
		openStorageWithPlayer(player, secondaryPos);
		requireActivePhysicalEndpointMenu(player, menuType, secondary, endpoint, storageType);
		requireEndpointOpening(findEndpointOpening(level, endpoint), player.getUUID(), openedAt,
				storageType + " linked Storage provider did not record its selected physical endpoint");
		return true;
	}

	private static void requireActivePhysicalEndpointMenu(ServerPlayer player, Class<? extends StorageContainerMenu> menuType,
			StorageBlockEntity selectedEndpoint, LinkedStorageEndpointData endpoint, String storageType) {
		require(menuType.isInstance(player.containerMenu), storageType + " provider did not leave its server menu active");
		StorageContainerMenu menu = (StorageContainerMenu) player.containerMenu;
		require(menu.getStorageWrapper() == selectedEndpoint.getStorageWrapper(), storageType + " menu did not retain the physical endpoint facade");
		require(menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
				&& endpointProvider.getLinkedStorageEndpoint().filter(endpoint::equals).isPresent()
				&& endpointProvider.getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.SECONDARY::equals).isPresent(),
				storageType + " menu physical facade did not retain its secondary endpoint identity and role");
	}

	private static void requireEndpointOpening(EndpointOpening opening, UUID expectedPlayerId, long expectedOpenedAt, String message) {
		require(java.util.Objects.equals(opening.playerId(), expectedPlayerId) && opening.openedAt() == expectedOpenedAt, message);
	}

	private static EndpointOpening findEndpointOpening(ServerLevel level, LinkedStorageEndpointData endpoint) {
		for (Tag groupValue : LinkedStorageGroupsSavedData.get(level).save().getListOrEmpty("groups")) {
			if (!(groupValue instanceof CompoundTag group) || group.read("id", UUIDUtil.CODEC).filter(endpoint.groupId()::equals).isEmpty()) {
				continue;
			}
			for (Tag endpointValue : group.getListOrEmpty("endpoints")) {
				if (endpointValue instanceof CompoundTag endpointTag
						&& endpointTag.read("id", UUIDUtil.CODEC).filter(endpoint.endpointId()::equals).isPresent()) {
					return new EndpointOpening(endpointTag.read("last_opened_by", UUIDUtil.CODEC).orElse(null), endpointTag.getLongOr("last_opened_at", -1L));
				}
			}
		}
		throw new IllegalStateException("Missing linked-storage endpoint ledger for group " + endpoint.groupId() + " endpoint " + endpoint.endpointId());
	}

	private record EndpointOpening(UUID playerId, long openedAt) {
	}

	private record PhysicalEndpointMenuLedgerFixture(BlockPos barrelPrimaryPos, BlockPos barrelSecondaryPos, LinkedStorageEndpointData barrelEndpoint,
			BlockPos limitedSecondaryPos, LinkedStorageEndpointData limitedEndpoint) {
	}

	private record UpgradeRefreshFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, int playerUpgradeSlot,
			ItemStack originalPlayerUpgradeStack) {
	}

	private static Boolean runStorageFamilyCompatibilityAndCarrierSchema(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos limitedOnePos = player.blockPosition().offset(0, 0, 16);
		BlockPos limitedCopperOnePos = limitedOnePos.east(3);
		BlockPos limitedTwoPos = limitedCopperOnePos.east(3);
		BlockPos standardPos = limitedTwoPos.east(3);
		clearArea(level, limitedOnePos);
		try {
			place(level, player, limitedOnePos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			place(level, player, limitedCopperOnePos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			place(level, player, limitedTwoPos, new ItemStack(ModBlocks.LIMITED_BARREL_2_ITEM.get()));
			place(level, player, standardPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			LimitedBarrelBlockEntity limitedOne = limited(level, limitedOnePos);
			LimitedBarrelBlockEntity limitedCopperOne = limited(level, limitedCopperOnePos);
			LimitedBarrelBlockEntity limitedTwo = limited(level, limitedTwoPos);
			BarrelBlockEntity standardBarrel = barrel(level, standardPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 4);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, limitedOne) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, limitedCopperOne) == LinkedStorageService.LinkResult.SUCCESS,
					"Limited barrels with the same slot family did not interlink across tiers");
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, limitedTwo) == LinkedStorageService.LinkResult.INCOMPATIBLE_ENDPOINT
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker,
							standardBarrel) == LinkedStorageService.LinkResult.INCOMPATIBLE_ENDPOINT
					&& !limitedTwo.isLinkedStorage() && !standardBarrel.isLinkedStorage(),
					"Limited barrels accepted a different slot family or a standard barrel");
			require(StorageLinkedStorageHostWrapper.createVirtualCarrier(limitedOne).contains("renderInfo"),
					"Linked storage virtual carrier did not preserve renderInfo");
			return true;
		} finally {
			clearArea(level, limitedOnePos);
		}
	}

	private static void runLinkedStorageMenuCanonicalSnapshotRegression() {
		LinkedStorageMenuSnapshotFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupLinkedStorageMenuSnapshotFixture);
		try {
			waitForClientLinkedStorageEndpoint(fixture, fixture.secondaryPos());
			openLinkedStorageMenuCanonicalSnapshot(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			waitForLinkedStorageMenuCanonicalSnapshot(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return "";
			});
			waitForClientLinkedStorageEndpoint(fixture, fixture.primaryPos());
			openLinkedStorageMenuCanonicalSnapshot(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			waitForLinkedStorageMenuCanonicalSnapshot(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), fixture.primaryPos());
				return "";
			});
		}
	}

	private static LinkedStorageMenuSnapshotFixture setupLinkedStorageMenuSnapshotFixture(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 8);
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
					"Could not create the linked storage menu snapshot fixture");
			primary.setCustomName(Component.literal("Linked storage snapshot profile"));
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			LinkedStorageSnapshotProfile profile = new LinkedStorageSnapshotProfile(primary.getDisplayName(),
					primary.getStorageWrapper().getInventoryHandler().size(), primary.getStorageWrapper().getUpgradeHandler().size(),
					primary.getStorageWrapper().getColumnsTaken());
			return new LinkedStorageMenuSnapshotFixture(primaryPos, secondaryPos, endpoint(primary, "linked storage menu primary").groupId(), profile);
		} catch (RuntimeException e) {
			clearArea(level, primaryPos);
			throw e;
		}
	}

	private static void openLinkedStorageMenuCanonicalSnapshot(LinkedStorageMenuSnapshotFixture fixture, BlockPos pos, LinkedStorageEndpointRole expectedRole) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			ItemStack originalMainHand = player.getMainHandItem().copy();
			InteractionResult interactionResult;
			try {
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				interactionResult = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
			} finally {
				player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
			}
			require(interactionResult.consumesAction(), "Linked storage endpoint did not consume the block interaction: " + interactionResult);
			if (!(player.containerMenu instanceof StorageContainerMenu menu)) {
				throw new IllegalStateException("Linked storage menu snapshot fixture did not open a storage menu");
			}
			LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
			boolean primary = endpoint != null
					&& LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId());
			require(menu.getBlockPosition().equals(Optional.of(pos)) && endpoint != null && endpoint.groupId().equals(fixture.groupId())
					&& primary == (expectedRole == LinkedStorageEndpointRole.PRIMARY)
					&& menu.getStorageWrapper().getInventoryHandler().size() == fixture.profile().inventorySlots()
					&& menu.getNumberOfStorageInventorySlots() == fixture.profile().inventorySlots()
					&& menu.getNumberOfUpgradeSlots() == fixture.profile().upgradeSlots() && menu.getColumnsTaken() == fixture.profile().columnsTaken()
					&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT,
					"Linked storage server menu did not expose the canonical snapshot");
			return "";
		});
	}

	private static Boolean runLinkedControllerCanonicalContents(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
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
			require(controller.getStoragePositions().size() == 2 && controller.getSlots(0) + controller.getSlots(1) == slots * 2,
					"Controller did not register both unlinked barrels before linking");
			require(linkWithPlayer(player, existing, existingPos, joiningPos), "Could not create controller-connected linked barrel group");
			existing.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			existing.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(controller.getStoragePositions().size() == 1 && controller.getSlots(0) == slots && controller.getStackStorages(diamondKey).size() == 1,
					"Controller duplicated canonical contents when a connected endpoint joined a linked group");
			require(controller.getStorageBlockPositions().containsAll(Set.of(existingPos, joiningPos))
					&& controller.getHighlightStoragePositions(controller.getStackStorages(diamondKey)).containsAll(Set.of(existingPos, joiningPos)),
					"Controller physical positions or highlights omitted a linked endpoint");
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
			placeWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
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

	private static Boolean runLinkedDoubleChestControllerCanonicalContents(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
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
					&& controller.getStoragePositions().contains(primaryMain) && controller.getSlots(0) == 54
					&& controller.getStackStorages(diamondKey).size() == 1, "Linked double chests did not share one canonical controller storage");
			return true;
		} finally {
			player.setGameMode(gameType);
			clearArea(level, controllerPos);
		}
	}

	private static Boolean runControllerDoubleChestLockFanoutRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType gameType = player.gameMode.getGameModeForPlayer();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 4);
		BlockPos chestLeftPos = controllerPos.east();
		BlockPos chestMainPos = chestLeftPos.east();
		BlockPos barrelPos = controllerPos.south();
		clearArea(level, controllerPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			placeWithPlayer(level, player, chestLeftPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeWithPlayer(level, player, chestMainPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeWithPlayer(level, player, barrelPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			ControllerBlockEntity controller = controller(level, controllerPos);
			require(isDoubleChest(level, chestLeftPos, chestMainPos)
					&& controller.getStorageBlockPositions().containsAll(Set.of(chestLeftPos, chestMainPos, barrelPos)),
					"Controller lock fanout fixture did not register both double-chest halves and the barrel");
			require(!chest(level, chestLeftPos).isLocked() && !chest(level, chestMainPos).isLocked() && !barrel(level, barrelPos).isLocked(),
					"Controller lock fanout fixture was not initially unlocked");

			controller.toggleLock();
			require(chest(level, chestLeftPos).isLocked() && chest(level, chestMainPos).isLocked() && barrel(level, barrelPos).isLocked(),
					"Controller lock did not toggle the double chest and barrel exactly once");

			controller.toggleLock();
			require(!chest(level, chestLeftPos).isLocked() && !chest(level, chestMainPos).isLocked() && !barrel(level, barrelPos).isLocked(),
					"Controller unlock did not toggle the double chest and barrel exactly once");
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
			require(isDoubleChest(level, primaryLeft, primaryMain) && primary.canUpgradeStorageTier(level)
					&& primaryEndpoint.equals(endpoint(primary, "primary double chest after endpoint transfer"))
					&& primary.getStorageWrapper().getInventoryHandler().size() == 54,
					"Primary linked endpoint did not transfer and expand during double-chest formation");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();

			player.setGameMode(GameType.SURVIVAL);
			clearDroppedItems(level, primaryMain);
			require(player.gameMode.destroyBlock(primaryMain), "Primary linked double-chest destruction failed");
			ItemStack droppedDoubleChest = findAndRemoveDroppedChest(level, primaryMain);
			require(level.getBlockState(primaryLeft).isAir() && level.getBlockState(primaryMain).isAir() && ChestBlockItem.isDoubleChest(droppedDoubleChest)
					&& primaryEndpoint.equals(droppedDoubleChest.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& Boolean.TRUE.equals(droppedDoubleChest.get(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT))
					&& !hasDroppedItem(level, primaryMain, Items.DIAMOND),
					"Primary linked double-chest removal did not preserve one marked endpoint drop without duplicate contents");
			placeWithPlayer(level, player, primaryLeft, droppedDoubleChest);
			BlockPos restoredPrimaryLeft = primaryLeft.west();
			require(isDoubleChest(level, restoredPrimaryLeft, primaryLeft), "Deferred linked double-chest placement did not recreate the expected pair");
			ChestBlockEntity restoredPrimary = chest(level, primaryLeft);
			ItemStack upgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
			InteractionResult upgradeResult = player.gameMode.useItemOn(player, level, upgrade, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(secondaryLeft), Direction.UP, secondaryLeft, false));
			require(upgradeResult == InteractionResult.FAIL && player.getMainHandItem().getCount() == 1
					&& level.getBlockState(secondaryLeft).is(ModBlocks.CHEST.get()) && level.getBlockState(secondaryMain).is(ModBlocks.CHEST.get())
					&& chest(level, secondaryMain).isMainChest()
					&& secondaryEndpoint.equals(endpoint(chest(level, secondaryMain), "main secondary chest after rejected tier upgrade")),
					"Tier upgrade from a linked secondary double-chest half "
							+ "was not rejected without changing the main endpoint");
			require(restoredPrimary.canUpgradeStorageTier(level) && primaryEndpoint.equals(endpoint(restoredPrimary, "restored primary double chest"))
					&& restoredPrimary.getStorageWrapper().getInventoryHandler().size() == 54
					&& count(restoredPrimary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
					"Deferred linked double-chest item restoration did not restore the primary endpoint and contents");

			player.setGameMode(GameType.CREATIVE);
			placeWithPlayer(level, player, secondaryPeer, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeWithPlayer(level, player, secondaryLeft, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity secondarySingle = chest(level, secondaryLeft);
			require(linkWithPlayer(player, barrel(level, secondaryPeer), secondaryPeer, secondaryLeft),
					"Could not link the secondary chest before double-chest formation");
			LinkedStorageEndpointData secondaryEndpoint = endpoint(secondarySingle, "secondary chest before double-chest formation");
			placeWithPlayer(level, player, secondaryMain, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity secondary = chest(level, secondaryMain);
			require(isDoubleChest(level, secondaryLeft, secondaryMain) && !secondary.canUpgradeStorageTier(level)
					&& secondaryEndpoint.equals(endpoint(secondary, "secondary double chest after endpoint transfer")),
					"Secondary linked endpoint did not transfer during double-chest formation");
			level.setBlock(secondaryMain, Blocks.BARREL.defaultBlockState(), 3);
			require(level.getBlockState(secondaryLeft).is(ModBlocks.CHEST.get())
					&& level.getBlockState(secondaryLeft).getValue(ChestBlock.TYPE) == ChestType.SINGLE
					&& secondaryEndpoint.equals(endpoint(chest(level, secondaryLeft), "split secondary linked chest")),
					"Replacing a linked secondary double-chest half did not split and retain its endpoint");
			runLinkedSecondaryChestCustomNameTransferRegression(level, player, primaryLeft.south(6));
			runLinkedSecondaryChestBreakRegression(level, player, primaryLeft.east(4).south(6), Direction.WEST, false);
			return true;
		} finally {
			player.setGameMode(gameType);
			clearArea(level, primaryLeft);
		}
	}

	private static void runLinkedSecondaryChestCustomNameTransferRegression(ServerLevel level, ServerPlayer player, BlockPos linkedPrimaryPos) {
		BlockPos originalSecondaryPos = linkedPrimaryPos.east(5);
		BlockPos firstSurvivorPos = originalSecondaryPos.west();
		BlockPos secondSurvivorPos = firstSurvivorPos.west();
		placeWithPlayer(level, player, linkedPrimaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeWithPlayer(level, player, originalSecondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
		BarrelBlockEntity linkedPrimary = barrel(level, linkedPrimaryPos);
		require(linkWithPlayer(player, linkedPrimary, linkedPrimaryPos, originalSecondaryPos),
				"Could not link the unnamed secondary chest before custom-name transfer regression");
		InventoryHandler canonicalInventory = linkedPrimary.getStorageWrapper().getInventoryHandler();
		fillInventoryWithTestContents(canonicalInventory);
		List<ItemStack> canonicalContents = new ArrayList<>();
		for (int slot = 0; slot < canonicalInventory.size(); slot++) {
			canonicalContents.add(canonicalInventory.getStackInSlot(slot).copy());
		}
		canonicalInventory.saveInventory();
		LinkedStorageEndpointData endpoint = endpoint(chest(level, originalSecondaryPos), "unnamed secondary chest before custom-name transfer regression");

		placeWithPlayer(level, player, firstSurvivorPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
		ChestBlockEntity originalSecondary = chest(level, originalSecondaryPos);
		require(isDoubleChest(level, firstSurvivorPos, originalSecondaryPos) && originalSecondary.isMainChest()
				&& endpoint.equals(endpoint(originalSecondary, "unnamed secondary main chest before break")) && originalSecondary.getCustomName() == null,
				"Adding a west chest did not make the unnamed linked secondary the double-chest main part");

		player.setGameMode(GameType.SURVIVAL);
		clearDroppedItems(level, originalSecondaryPos);
		require(player.gameMode.destroyBlock(originalSecondaryPos), "Unnamed linked secondary main-chest destruction failed");
		List<ItemEntity> firstDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(originalSecondaryPos).inflate(1));
		require(firstDrops.size() == 1, "Breaking the unnamed linked secondary main chest produced " + firstDrops.size() + " drops instead of one");
		ItemStack firstDrop = firstDrops.getFirst().getItem().copy();
		firstDrops.forEach(ItemEntity::discard);
		ChestBlockEntity firstSurvivor = chest(level, firstSurvivorPos);
		require(firstSurvivor.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
				&& endpoint.equals(endpoint(firstSurvivor, "first unnamed secondary survivor"))
				&& hasInventoryContents(firstSurvivor.getStorageWrapper().getInventoryHandler(), canonicalContents) && firstSurvivor.getCustomName() == null
				&& !ChestBlockItem.isDoubleChest(firstDrop) && firstDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
				&& !firstDrop.has(DataComponents.CUSTOM_NAME),
				"Breaking the unnamed linked secondary main chest changed its survivor or marked the ordinary chest drop");

		placeWithPlayer(level, player, secondSurvivorPos, firstDrop);
		ChestBlockEntity shiftedMain = chest(level, firstSurvivorPos);
		require(isDoubleChest(level, secondSurvivorPos, firstSurvivorPos) && shiftedMain.isMainChest()
				&& endpoint.equals(endpoint(shiftedMain, "shifted unnamed secondary main chest"))
				&& hasInventoryContents(shiftedMain.getStorageWrapper().getInventoryHandler(), canonicalContents) && shiftedMain.getCustomName() == null,
				"Placing the ordinary drop west of the survivor changed the shifted linked secondary chest name or contents");

		clearDroppedItems(level, firstSurvivorPos);
		require(player.gameMode.destroyBlock(firstSurvivorPos), "Shifted unnamed linked secondary main-chest destruction failed");
		List<ItemEntity> secondDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(firstSurvivorPos).inflate(1));
		require(secondDrops.size() == 1, "Breaking the shifted unnamed linked secondary main chest produced " + secondDrops.size() + " drops instead of one");
		ItemStack secondDrop = secondDrops.getFirst().getItem().copy();
		secondDrops.forEach(ItemEntity::discard);
		ChestBlockEntity secondSurvivor = chest(level, secondSurvivorPos);
		require(secondSurvivor.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
				&& endpoint.equals(endpoint(secondSurvivor, "second unnamed secondary survivor"))
				&& hasInventoryContents(secondSurvivor.getStorageWrapper().getInventoryHandler(), canonicalContents) && secondSurvivor.getCustomName() == null
				&& !ChestBlockItem.isDoubleChest(secondDrop) && secondDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
				&& !secondDrop.has(DataComponents.CUSTOM_NAME),
				"Breaking the shifted unnamed linked secondary main chest changed its survivor or marked the ordinary chest drop");

		clearDroppedItems(level, secondSurvivorPos);
		require(player.gameMode.destroyBlock(secondSurvivorPos), "Unnamed linked secondary endpoint cleanup destruction failed");
		clearDroppedItems(level, linkedPrimaryPos);
		require(player.gameMode.destroyBlock(linkedPrimaryPos), "Unnamed linked primary cleanup destruction failed");
		clearDroppedItems(level, linkedPrimaryPos);
		player.setGameMode(GameType.CREATIVE);
	}

	private static void runLinkedSecondaryChestBreakRegression(ServerLevel level, ServerPlayer player, BlockPos secondaryPos, Direction addedChestDirection,
			boolean breakMainChest) {
		BlockPos peerPos = secondaryPos.south(3);
		BlockPos addedPos = secondaryPos.relative(addedChestDirection);
		placeWithPlayer(level, player, peerPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeWithPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
		BarrelBlockEntity linkedPrimary = barrel(level, peerPos);
		require(linkWithPlayer(player, linkedPrimary, peerPos, secondaryPos), "Could not link the secondary chest before double-chest break regression");
		InventoryHandler canonicalInventory = linkedPrimary.getStorageWrapper().getInventoryHandler();
		fillInventoryWithTestContents(canonicalInventory);
		List<ItemStack> canonicalContents = new ArrayList<>();
		for (int slot = 0; slot < canonicalInventory.size(); slot++) {
			canonicalContents.add(canonicalInventory.getStackInSlot(slot).copy());
		}
		canonicalInventory.saveInventory();
		LinkedStorageEndpointData endpoint = endpoint(chest(level, secondaryPos), "secondary chest before double-chest break regression");
		placeWithPlayer(level, player, addedPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
		ChestBlockEntity main = chest(level, secondaryPos).isMainChest() ? chest(level, secondaryPos) : chest(level, addedPos);
		BlockPos brokenPos = breakMainChest ? main.getBlockPos() : main.getBlockPos().equals(secondaryPos) ? addedPos : secondaryPos;
		BlockPos remainingPos = brokenPos.equals(secondaryPos) ? addedPos : secondaryPos;
		player.setGameMode(GameType.SURVIVAL);
		clearDroppedItems(level, brokenPos);
		require(player.gameMode.destroyBlock(brokenPos), "Secondary linked double-chest destruction failed");
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(brokenPos).inflate(1.5D));
		ItemStack brokenDrop = drops.size() == 1 ? drops.getFirst().getItem().copy() : ItemStack.EMPTY;
		drops.forEach(ItemEntity::discard);
		ChestBlockEntity remaining = chest(level, remainingPos);
		require(remaining.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
				&& endpoint.equals(endpoint(remaining, "remaining secondary chest after double-chest break")) && !ChestBlockItem.isDoubleChest(brokenDrop)
				&& brokenDrop.is(ModBlocks.CHEST_ITEM.get()) && brokenDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
				&& hasInventoryContents(linkedPrimary.getStorageWrapper().getInventoryHandler(), canonicalContents),
				"Breaking a linked secondary double-chest half changed linked contents or did not leave one ordinary chest drop");
		clearDroppedItems(level, remainingPos);
		require(player.gameMode.destroyBlock(remainingPos), "Remaining linked secondary chest destruction failed");
		ItemStack remainingDrop = findAndRemoveDroppedChest(level, remainingPos);
		require(!ChestBlockItem.isDoubleChest(remainingDrop) && endpoint.equals(remainingDrop.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
				"Breaking the remaining linked secondary chest did not preserve its endpoint drop");
		player.setGameMode(GameType.CREATIVE);
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

	private static void runClientControllerTopologyUpdateRegression() {
		ControllerTopologyFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 20);
			BlockPos storagePos = controllerPos.east();
			clearArea(level, controllerPos);
			placeWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			return new ControllerTopologyFixture(controllerPos, storagePos);
		});
		try {
			waitForClientControllerTopology(fixture, false);
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				placeWithPlayer(level, player, fixture.storagePos(), new ItemStack(ModBlocks.BARREL_ITEM.get()));
				require(controller(level, fixture.controllerPos()).getStorageBlockPositions().contains(fixture.storagePos()),
						"Server controller did not register newly placed adjacent storage");
				return true;
			});
			waitForClientControllerTopology(fixture, true);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				clearArea(player.level(), fixture.controllerPos());
				return true;
			});
		}
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

	private static void waitForClientControllerTopology(ControllerTopologyFixture fixture, boolean containsStorage) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.level != null && minecraft.level.getBlockEntity(fixture.controllerPos()) instanceof ControllerBlockEntity controller
						&& controller.getStorageBlockPositions().contains(fixture.storagePos()) == containsStorage;
			}))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for client controller topology at " + fixture.controllerPos() + " to "
				+ (containsStorage ? "include " : "exclude ") + fixture.storagePos());
	}

	private static void waitForLinkedStorageMenuCanonicalSnapshot(LinkedStorageMenuSnapshotFixture fixture, BlockPos pos,
			LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof StorageContainerMenu menu))
					return false;
				LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
				return menu.getBlockPosition().equals(Optional.of(pos)) && endpoint != null && endpoint.groupId().equals(fixture.groupId())
						&& screen.getTitle().getString().equals(fixture.profile().groupName().getString())
						&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
						&& endpointProvider.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent()
						&& menu.getStorageWrapper().getInventoryHandler().size() == fixture.profile().inventorySlots()
						&& menu.getStorageWrapper().getUpgradeHandler().size() == fixture.profile().upgradeSlots()
						&& menu.getStorageWrapper().getColumnsTaken() == fixture.profile().columnsTaken()
						&& menu.getNumberOfStorageInventorySlots() == fixture.profile().inventorySlots()
						&& menu.getNumberOfUpgradeSlots() == fixture.profile().upgradeSlots() && menu.getColumnsTaken() == fixture.profile().columnsTaken()
						&& ClientLinkedStorageContents.getGroupName(fixture.groupId()).filter(fixture.profile().groupName()::equals).isPresent()
						&& ClientLinkedStorageContents.getInventorySlots(fixture.groupId()).orElse(-1) == fixture.profile().inventorySlots()
						&& ClientLinkedStorageContents.getUpgradeSlots(fixture.groupId()).orElse(-1) == fixture.profile().upgradeSlots()
						&& ClientLinkedStorageContents.getColumnsTaken(fixture.groupId()).orElse(-1) == fixture.profile().columnsTaken()
						&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == LINKED_STORAGE_MENU_SNAPSHOT_DIAMOND_COUNT;
			}))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for the client linked storage menu canonical snapshot for group " + fixture.groupId() + ": "
				+ describeClientLinkedStorageMenu(fixture, pos));
	}

	private static void waitForClientLinkedStorageEndpoint(LinkedStorageMenuSnapshotFixture fixture, BlockPos pos) {
		waitForClientLinkedStorageEndpoint(fixture.groupId(), pos, () -> describeClientLinkedStorageMenu(fixture, pos));
	}

	private static void waitForClientLinkedStorageEndpoint(UUID groupId, BlockPos pos) {
		waitForClientLinkedStorageEndpoint(groupId, pos, () -> "position=" + pos + ", group=" + groupId);
	}

	private static void waitForClientLinkedStorageEndpoint(UUID groupId, BlockPos pos, java.util.function.Supplier<String> diagnostic) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof StorageBlockEntity storage
						&& storage.getLinkedStorageEndpointData() != null && storage.getLinkedStorageEndpointData().groupId().equals(groupId);
			}))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for linked endpoint client block entity at " + pos + ": " + diagnostic.get());
	}

	private static void waitForClientLinkedStorageEndpoint(LinkedStorageEndpointData expectedEndpoint, BlockPos pos,
			java.util.function.Supplier<String> diagnostic) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof StorageBlockEntity storage
						&& expectedEndpoint.equals(storage.getLinkedStorageEndpointData());
			}))
				return;
			sleep(50);
		}
		throw new IllegalStateException(
				"Timed out waiting for linked endpoint client block entity at " + pos + " with endpoint " + expectedEndpoint + ": " + diagnostic.get());
	}

	private static String describeDroppedPrimaryRenameClientState(DroppedPrimaryRenameFixture fixture) {
		try {
			return AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				String screenName = minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName();
				String title = minecraft.screen instanceof AbstractContainerScreen<?> screen ? screen.getTitle().getString() : "n/a";
				String menuEndpoint = minecraft.screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu menu
						? String.valueOf(menu.getStorageBlockEntity().getLinkedStorageEndpointData())
						: "n/a";
				String secondaryEndpoint = minecraft.level != null
						&& minecraft.level.getBlockEntity(fixture.secondaryPos()) instanceof StorageBlockEntity storage
								? String.valueOf(storage.getLinkedStorageEndpointData())
								: "missing";
				String primaryEndpoint = minecraft.level != null && minecraft.level.getBlockEntity(fixture.primaryPos()) instanceof StorageBlockEntity storage
						? String.valueOf(storage.getLinkedStorageEndpointData())
						: "missing";
				String cache = ClientLinkedStorageContents.getGroupName(fixture.groupId()).map(Component::getString).orElse("missing");
				return "screen=" + screenName + ", title=" + title + ", menuEndpoint=" + menuEndpoint + ", primaryEndpoint=" + primaryEndpoint
						+ ", secondaryEndpoint=" + secondaryEndpoint + ", cacheName=" + cache + ", player="
						+ (minecraft.player == null ? "null" : minecraft.player.position());
			});
		} catch (RuntimeException e) {
			return "client diagnostics unavailable: " + e;
		}
	}

	private static String describeClientLinkedStorageMenu(LinkedStorageMenuSnapshotFixture fixture, BlockPos pos) {
		try {
			return AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				String screenName = minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName();
				String title = minecraft.screen instanceof AbstractContainerScreen<?> screen ? screen.getTitle().getString() : "n/a";
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
				if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof StorageContainerMenu menu)) {
					return "screen=" + screenName + ", title=" + title + ", player=" + (minecraft.player == null ? "null" : minecraft.player.position())
							+ ", clientBlockEntity=" + clientBlockEntity + ", cache={" + cache + "}";
				}
				LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
				String role = menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider provider
						? provider.getLinkedStorageEndpointRole().map(Enum::name).orElse("missing")
						: "no-provider";
				return "screen=" + screenName + ", title=" + title + ", titleComponent=" + screen.getTitle() + ", expectedComponent="
						+ fixture.profile().groupName() + ", player=" + minecraft.player.position() + ", menuPos=" + menu.getBlockPosition() + ", endpoint="
						+ endpoint + ", role=" + role + ", wrapper={inventorySlots=" + menu.getStorageWrapper().getInventoryHandler().size() + ", upgradeSlots="
						+ menu.getStorageWrapper().getUpgradeHandler().size() + ", columns=" + menu.getStorageWrapper().getColumnsTaken() + ", diamonds="
						+ count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) + "}, menu={inventorySlots="
						+ menu.getNumberOfStorageInventorySlots() + ", upgradeSlots=" + menu.getNumberOfUpgradeSlots() + ", columns=" + menu.getColumnsTaken()
						+ "}, clientBlockEntity=" + clientBlockEntity + ", cache={" + cache + "}";
			});
		} catch (RuntimeException e) {
			return "client diagnostics unavailable: " + e;
		}
	}

	private static boolean hasLimitedRenderProjection(LimitedBarrelBlockEntity barrel) {
		return barrel.getStorageWrapper().getRenderDataHandler().getDisplayData().displayItems().stream()
				.anyMatch(item -> item.item() != null && item.item().is(Items.DIAMOND)) && barrel.getSlotCounts().contains(LINKED_LIMITED_RELOAD_ITEM_COUNT)
				&& barrel.getSlotFillLevels().stream().anyMatch(level -> level > 0F);
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

	private static void ensureFixtureSupportPlatform(ServerLevel level, BlockPos position) {
		level.setBlock(position.below(), Blocks.DIRT.defaultBlockState(), 3);
	}

	private static void openStorageWithPlayer(ServerPlayer player, BlockPos pos) {
		ItemStack originalMainHand = player.getMainHandItem().copy();
		try {
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			require(player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)).consumesAction(),
					"Player storage interaction did not consume at " + pos);
		} finally {
			player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
		}
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

	private static boolean isDoubleChest(ServerLevel level, BlockPos left, BlockPos main) {
		return level.getBlockState(left).is(ModBlocks.CHEST.get()) && level.getBlockState(main).is(ModBlocks.CHEST.get())
				&& level.getBlockState(left).getValue(ChestBlock.TYPE) == ChestType.LEFT
				&& level.getBlockState(main).getValue(ChestBlock.TYPE) == ChestType.RIGHT
				&& level.getBlockEntity(left, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(chest -> chest.getMainPos().equals(main)).orElse(false)
				&& level.getBlockEntity(main, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(chest -> chest.getMainPos().equals(main)).orElse(false);
	}

	private static ItemStack findAndRemoveDroppedChest(ServerLevel level, BlockPos pos) {
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
				entity -> entity.getItem().is(ModBlocks.CHEST_ITEM.get()));
		require(drops.size() == 1, "Chest break produced " + drops.size() + " chest drops instead of exactly one");
		ItemStack stack = drops.getFirst().getItem().copy();
		drops.forEach(ItemEntity::discard);
		return stack;
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

	private static void clearArea(ServerLevel level, BlockPos center) {
		level.getEntitiesOfClass(ItemEntity.class, AABB.encapsulatingFullBlocks(center.offset(-2, 0, -2), center.offset(9, 2, 10)))
				.forEach(ItemEntity::discard);
		for (int x = -2; x <= 9; x++)
			for (int y = -1; y <= 2; y++)
				for (int z = -2; z <= 10; z++) {
					BlockPos pos = center.offset(x, y, z);
					// Keep the automation world's Y=72 platform intact beneath each fixture.
					if (pos.getY() == 72)
						continue;
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage)
						storage.clearContent();
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
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

	private record LinkedStorageMenuSnapshotFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, LinkedStorageSnapshotProfile profile) {
	}

	private record TierUpgradeMenuInvalidationFixture(BlockPos primaryPos, BlockPos secondaryPos, LinkedStorageMenuSnapshotFixture initialMenuFixture,
			GameType originalGameType, Vec3 originalPosition) {
	}

	private record ControllerTopologyFixture(BlockPos controllerPos, BlockPos storagePos) {
	}

	private static void runLinkedPrimaryChestExpansionRegression() {
		ExpansionFixture initialFixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos pos = player.blockPosition().offset(0, 0, 3);
			clearArea(level, pos);
			placeWithPlayer(level, player, pos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity primary = chest(level, pos);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get()),
					primary) == LinkedStorageService.LinkResult.SUCCESS, "Could not link primary chest for expansion");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			LinkedStorageEndpointData endpoint = endpoint(primary, "expansion primary");
			require(LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()),
					"Expansion fixture endpoint was not the authoritative linked primary");
			return new ExpansionFixture(pos, endpoint.groupId(), profile(primary));
		});
		try {
			LinkedStorageMenuSnapshotFixture initialMenuFixture = new LinkedStorageMenuSnapshotFixture(initialFixture.primaryPos(), initialFixture.primaryPos(),
					initialFixture.groupId(), initialFixture.profile());
			waitForClientLinkedStorageEndpoint(initialMenuFixture, initialFixture.primaryPos());
			openLinkedStorageMenuCanonicalSnapshot(initialMenuFixture, initialFixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			waitForLinkedStorageMenuCanonicalSnapshot(initialMenuFixture, initialFixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			ExpansionFixture fixture = AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				require(player.containerMenu instanceof StorageContainerMenu, "Linked primary menu closed before chest expansion");
				StorageContainerMenu staleMenu = (StorageContainerMenu) player.containerMenu;
				BlockPos addedPos = initialFixture.primaryPos().east();
				placeWithPlayer(level, player, addedPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				require(player.containerMenu == player.inventoryMenu && player.containerMenu != staleMenu, "Expanding linked primary did not close stale menu");
				ChestBlockEntity originalPart = chest(level, initialFixture.primaryPos());
				ChestBlockEntity addedPart = chest(level, addedPos);
				List<ChestBlockEntity> endpointHolders = List.of(originalPart, addedPart).stream().filter(
						part -> part.getLinkedStorageEndpointData() != null && part.getLinkedStorageEndpointData().groupId().equals(initialFixture.groupId()))
						.toList();
				require(endpointHolders.size() == 1 && endpointHolders.getFirst().isMainChest(),
						"Expanded linked chest did not preserve exactly one endpoint on its main part");
				ChestBlockEntity expanded = endpointHolders.getFirst();
				LinkedStorageEndpointData endpoint = endpoint(expanded, "expanded primary");
				require(LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())
						&& expanded.getStorageWrapper().getInventoryHandler().size() == 54
						&& count(expanded.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7,
						"Expanded linked primary did not preserve role, capacity, and contents");
				return new ExpansionFixture(expanded.getBlockPos(), endpoint.groupId(), profile(expanded));
			});
			waitForClosedStorageMenu();
			waitForClientExpandedLinkedStorageEndpoint(fixture);
			AutomationRuntime.runOnServer(player -> {
				ChestBlockEntity expanded = chest(player.level(), fixture.primaryPos());
				player.openMenu(new SophisticatedMenuProvider((id, inventory, menuPlayer) -> new StorageContainerMenu(id, menuPlayer, fixture.primaryPos()),
						expanded.getMenuDisplayName(), false), buffer -> StorageContainerMenu.writeMenuData(buffer, player, fixture.primaryPos()));
				return true;
			});
			waitForExpandedClientMenu(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), initialFixture.primaryPos());
				return true;
			});
		}
	}

	private static void waitForClientExpandedLinkedStorageEndpoint(ExpansionFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		int consecutiveMatches = 0;
		while (System.nanoTime() < deadline) {
			boolean matches = AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.level != null && minecraft.level.getBlockEntity(fixture.primaryPos()) instanceof ChestBlockEntity chest && chest.isMainChest()
						&& chest.getLinkedStorageEndpointData() != null && chest.getLinkedStorageEndpointData().groupId().equals(fixture.groupId());
			});
			if (matches && ++consecutiveMatches >= 3) {
				return;
			}
			if (!matches) {
				consecutiveMatches = 0;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for stable expanded linked primary client block entity at " + fixture.primaryPos() + " for group "
				+ fixture.groupId() + ": " + describeClientChest(fixture.primaryPos()) + ", west=" + describeClientChest(fixture.primaryPos().west())
				+ ", east=" + describeClientChest(fixture.primaryPos().east()));
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
			clearArea(level, primaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity primary = limited(level, primaryPos);
			LimitedBarrelBlockEntity secondary = limited(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create memory fixture");
			ItemStack originalHand = player.getMainHandItem().copy();
			float originalYRot = player.getYRot();
			float originalXRot = player.getXRot();
			Vec3 originalPosition = player.position();
			LinkedStorageEndpointData endpoint = secondary.getLinkedStorageEndpointData();
			require(endpoint != null, "Linked limited secondary lost its endpoint before memory deposit");
			primary.toggleLock();
			return new MemoryFixture(primaryPos, secondaryPos, endpoint.groupId(), originalHand, originalYRot, originalXRot, originalPosition);
		});
		try {
			waitForClientLinkedStorageEndpoint(fixture.groupId(), fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				LimitedBarrelBlockEntity primary = limited(level, fixture.primaryPos());
				BlockState state = level.getBlockState(fixture.primaryPos());
				require(state.getBlock() instanceof LimitedBarrelBlock, "Missing ordinary limited barrel before memory regression interaction");
				LimitedBarrelBlock block = (LimitedBarrelBlock) state.getBlock();
				Direction facing = block.getFacing(state);
				int diamondCount = 23;
				ItemStack diamonds = new ItemStack(Items.DIAMOND, diamondCount);
				player.setItemInHand(InteractionHand.MAIN_HAND, diamonds);
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()).add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(.5)),
						facing, fixture.primaryPos(), false);
				require(player.gameMode.useItemOn(player, level, diamonds, InteractionHand.MAIN_HAND, hit).consumesAction(),
						"Could not insert an item into a locked linked limited-barrel empty slot through its in-world interaction");
				Vec3 originalPosition = player.position();
				float originalYRot = player.getYRot();
				float originalXRot = player.getXRot();
				try {
					require(player.teleportTo(level, Vec3.atCenterOf(fixture.primaryPos()).x + facing.getStepX() * 2,
							Vec3.atCenterOf(fixture.primaryPos()).y - 1.62, Vec3.atCenterOf(fixture.primaryPos()).z + facing.getStepZ() * 2, Set.of(),
							player.getYRot(), player.getXRot(), false), "Could not position player to remove limited-barrel items");
					player.setYRot(facing.getOpposite().toYRot());
					player.setYHeadRot(facing.getOpposite().toYRot());
					player.setXRot(0);
					for (int removed = 0; removed < diamondCount; removed++) {
						require(block.tryToTakeItem(state, level, fixture.primaryPos(), player),
								"Could not remove an item from a locked linked limited barrel through its in-world interaction");
					}
				} finally {
					player.setYRot(originalYRot);
					player.setYHeadRot(originalYRot);
					player.setXRot(originalXRot);
					player.teleportTo(level, originalPosition.x, originalPosition.y, originalPosition.z, Set.of(), originalYRot, originalXRot, false);
				}
				require(primary.getStorageWrapper().getInventoryHandler().getStackInSlot(0).isEmpty(),
						"In-world interaction did not remove the linked limited barrel contents");
				require(primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotFilterStack(0, false)
						.filter(stack -> stack.is(Items.DIAMOND)).isPresent(), "Locked linked limited barrel did not memorize the inserted item");
				require(LinkedStorageGroupsSavedData.get(level).manager().resolveContents(fixture.groupId())
						.orElseThrow(() -> new IllegalStateException("Linked limited barrel group contents disappeared")).contents().settings()
						.equals(primary.getStorageWrapper().getSettingsHandler().getSettingsData()),
						"Linked limited barrel canonical contents did not retain the memorized-slot settings");
				return true;
			});
			AutomationRuntime.runOnServer(player -> {
				require(player.teleportTo(player.level(), fixture.primaryPos().getCenter().x, fixture.primaryPos().getCenter().y,
						fixture.primaryPos().getCenter().z, Set.of(), player.getYRot(), player.getXRot(), false),
						"Could not position player to open linked limited barrel");
				return true;
			});
			waitForClientPlayerPosition(fixture.primaryPos().getCenter());
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				ItemStack emptyHand = ItemStack.EMPTY;
				player.setItemInHand(InteractionHand.MAIN_HAND, emptyHand);
				require(player.gameMode
						.useItemOn(player, level, emptyHand, InteractionHand.MAIN_HAND,
								new BlockHitResult(Vec3.atCenterOf(fixture.primaryPos()).add(0, .5, 0), Direction.UP, fixture.primaryPos(), false))
						.consumesAction() && player.containerMenu instanceof LimitedBarrelContainerMenu,
						"Could not open the linked Limited Barrel I through its normal in-world interaction");
				return true;
			});
			waitForClientMemoryMenu(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalHand());
				player.setYRot(fixture.originalYRot());
				player.setYHeadRot(fixture.originalYRot());
				player.setXRot(fixture.originalXRot());
				player.teleportTo(player.level(), fixture.originalPosition().x, fixture.originalPosition().y, fixture.originalPosition().z, Set.of(),
						fixture.originalYRot(), fixture.originalXRot(), false);
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void waitForClientMemoryMenu(MemoryFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof LimitedBarrelContainerMenu menu && menu.getBlockPosition().equals(Optional.of(fixture.primaryPos()))
					&& Optional.ofNullable(menu.getStorageBlockEntity().getLinkedStorageEndpointData())
							.filter(endpoint -> endpoint.groupId().equals(fixture.groupId())).isPresent()
					&& menu.getStorageWrapper().getInventoryHandler().size() == 1 && screen.getTitle().getString().equals("Limited Barrel I")
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
					&& endpointProvider.getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent()
					&& menu.getMemorizedStackInSlot(0).filter(stack -> stack.is(Items.DIAMOND)).isPresent()))
				return;
			sleep(50);
		}
		throw new IllegalStateException("Primary linked limited-barrel menu did not synchronize canonical Diamond memory: position=" + fixture.primaryPos()
				+ ", group=" + fixture.groupId());
	}

	private static void runLinkedStorageStackTooltipRegression() {
		TooltipFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos pos = player.blockPosition().offset(0, 0, 44);
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
				ItemStack carrier = Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot());
				require(StorageItemClient.getTooltipImage(carrier) instanceof LinkedStorageTooltip tooltip
						&& tooltip.role() == LinkedStorageEndpointRole.PRIMARY && tooltip.groupId().equals(fixture.endpoint().groupId()),
						"Production linked tooltip did not expose the PRIMARY role and canonical group");
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
			throw new IllegalStateException("Linked carrier tooltip cache did not resolve canonical contents: " + describeClientTooltipCache(fixture));
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().setItem(fixture.inventorySlot(), fixture.originalStack());
				player.inventoryMenu.broadcastChanges();
				clearArea(player.level(), fixture.pos());
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
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 56);
			BlockPos secondaryPos = primaryPos.east(21);
			clearArea(level, primaryPos);
			clearArea(level, secondaryPos);
			clearPickupEntities(level, primaryPos, secondaryPos);
			place(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			place(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = barrel(level, primaryPos);
			BarrelBlockEntity secondary = barrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create pickup fixture");
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
				int canonicalPearls = count(barrel(player.level(), fixture.primaryPos()).getStorageWrapper().getInventoryHandler(), Items.ENDER_PEARL);
				require(items.size() == 2 && canonicalPearls == 0,
						"Secondary pickup suppression did not retain exactly two entities and zero canonical items: entities="
								+ items.stream().map(item -> item.getUUID() + "@" + item.position() + "x" + item.getItem().getCount()).toList()
								+ ", expectedIds=" + fixture.itemIds() + ", canonical=" + canonicalPearls);
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
				clearPickupEntities(player.level(), fixture.primaryPos(), fixture.secondaryPos());
				clearArea(player.level(), fixture.primaryPos());
				clearArea(player.level(), fixture.secondaryPos());
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
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().player != null
							&& fixture.endpoint()
									.equals(Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot())
											.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
							&& StorageBlockEntity.getLinkedStorageEndpointRole(Minecraft.getInstance().player.getInventory().getItem(fixture.inventorySlot()))
									.filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent()))
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

	private static void waitForExpandedClientMenu(ExpansionFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageContainerMenu menu
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
							&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7))
				return;
			sleep(50);
		}
		LinkedStorageMenuSnapshotFixture diagnosticFixture = new LinkedStorageMenuSnapshotFixture(fixture.primaryPos(), fixture.primaryPos(), fixture.groupId(),
				fixture.profile());
		throw new IllegalStateException("Expanded linked primary client menu did not expose its 54-slot canonical snapshot: "
				+ describeClientLinkedStorageMenu(diagnosticFixture, fixture.primaryPos()));
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
		throw new IllegalStateException("Stale linked storage menu remained open on the server or client");
	}

	private record MemoryFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, ItemStack originalHand, float originalYRot, float originalXRot,
			Vec3 originalPosition) {
	}

	private record TooltipFixture(BlockPos pos, LinkedStorageEndpointData endpoint, int inventorySlot, ItemStack originalStack) {
	}

	private record ExpansionFixture(BlockPos primaryPos, UUID groupId, LinkedStorageSnapshotProfile profile) {
	}

	private record LinkedRenderRoleFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId) {
	}

	private record PickupFixture(BlockPos primaryPos, BlockPos secondaryPos, List<UUID> itemIds, long verifyTime) {
	}
}
