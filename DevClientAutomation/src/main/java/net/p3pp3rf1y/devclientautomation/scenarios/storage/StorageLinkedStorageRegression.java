package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
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
import java.util.function.Function;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;
import static net.p3pp3rf1y.sophisticatedstorage.init.ModItems.MAGNET_UPGRADE;

public final class StorageLinkedStorageRegression {
	private static final int LINKED_LIMITED_RELOAD_ITEM_COUNT = 23;

	private StorageLinkedStorageRegression() {
	}

	public static String run() {
		try {
			ensurePlayerAlive();
			BlockPos safePos = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::movePlayerToSafeFixturePosition);
			waitForLivingPlayer();
			waitForClientPlayerPosition(safePos);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runOrdinaryLinkedStorage);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runCanonicalStorageTypeInsertionRules);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedControllerCanonicalContents);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runControllerAdjacentLinkedEndpointPlacementRegression);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedDoubleChestControllerCanonicalContents);
			AutomationRuntime.runOnServer(StorageLinkedStorageRegression::runLinkedDoubleChestLifecycle);
			runLinkedStorageMenuSnapshotRegression();
			runLinkedStorageMenuTransitionRegression();
			runLinkedLimitedBarrelMemorySyncRegression();
			runLinkedStorageStackTooltipRegression();
			runSecondaryDroppedPickupRegression();
			runLinkedPrimaryChestExpansionMenuRegression();
			runLinkedSecondaryChestSplitMenuClosureRegression();
			return "{\"ok\":true,\"ordinaryBarrelLinkingSharesCanonicalContents\":true," + "\"canonicalStorageTypeControlsInsertionRules\":true,"
					+ "\"linkedControllerEndpointJoinKeepsOneCanonicalContentIndex\":true,"
					+ "\"controllerAdjacentLinkedEndpointPlacementRestoresMembership\":true,"
					+ "\"linkedDoubleChestControllerUsesOneCanonicalContentIndex\":true," + "\"linkedControllerRoutesAndFansOutToolOperations\":true,"
					+ "\"controllerRestorationReconnectsLinkedStorage\":true," + "\"linkedPrimaryDoubleChestRestoresAsPaired\":true,"
					+ "\"linkedSecondaryDoubleChestSplitsToSingleEndpoint\":true," + "\"linkedStorageMenuReceivesCanonicalSnapshot\":true,"
					+ "\"linkedMenusExposeCanonicalTitleAndEndpointRoles\":true," + "\"linkedPrimaryExpansionRetainsContentsAndReopens54Slots\":true,"
					+ "\"rootReplacementClosesStaleMenu\":true," + "\"linkedLimitedBarrelMemorySyncsToOpenMenu\":true,"
					+ "\"linkedStorageMenuTransitionsRejectStaleActions\":true," + "\"linkedSecondarySplitClosesStaleMenus\":true,"
					+ "\"linkedDroppedStackUsesCanonicalTooltipSnapshot\":true," + "\"secondarySkipsDuplicateDroppedPickup\":true,"
					+ "\"controllerPhysicalMembersDriveHighlightsAndVisibility\":true}";
		} catch (RuntimeException e) {
			return "{\"ok\":false,\"error\":" + jsonString(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()) + "}";
		}
	}

	private static void ensurePlayerAlive() {
		boolean respawnRequested = AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null && (minecraft.player.getHealth() <= 0 || minecraft.screen instanceof DeathScreen)) {
				minecraft.player.respawn();
				return true;
			}
			return false;
		});
		if (!respawnRequested) {
			return;
		}
		waitForLivingPlayer();
	}

	private static void waitForLivingPlayer() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getHealth() > 0
					&& !(Minecraft.getInstance().screen instanceof DeathScreen))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for the automation player to respawn");
	}

	private static BlockPos movePlayerToSafeFixturePosition(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos respawnPos = level.getRespawnData().pos();
		int safeY = Math.max(64, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, respawnPos.getX(), respawnPos.getZ()));
		BlockPos safePos = new BlockPos(respawnPos.getX(), safeY, respawnPos.getZ());
		for (int x = -4; x <= 10; x++) {
			for (int z = -4; z <= 14; z++) {
				level.setBlock(safePos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
		level.setBlock(safePos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(safePos.above(), Blocks.AIR.defaultBlockState(), 3);
		player.setGameMode(GameType.CREATIVE);
		SectionPos previousSection = player.getLastSectionPos();
		player.connection.teleport(safePos.getCenter().x, safePos.getY(), safePos.getCenter().z, player.getYRot(), player.getXRot());
		player.setLastSectionPos(previousSection);
		level.getChunkSource().move(player);
		return safePos;
	}

	private static void waitForClientPlayerPosition(BlockPos expectedPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.blockPosition().closerThan(expectedPos, 2))) {
				return;
			}
			sleep(50);
		}
		String clientState = AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return "player=" + (minecraft.player == null ? null : minecraft.player.blockPosition()) + ", level=" + minecraft.level + ", loaded="
					+ (minecraft.level != null && minecraft.level.isLoaded(expectedPos));
		});
		throw new IllegalStateException("Automation client did not reach the safe fixture position " + expectedPos + ": " + clientState);
	}

	private static void runLinkedStorageMenuSnapshotRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			Vec3 originalPosition = player.position();
			float originalYRot = player.getYRot();
			float originalXRot = player.getXRot();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 3);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			placeBlockWithPlayer(level, player, primaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()));
			placeBlockWithPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			String title = "Automation Linked Storage";
			primary.setCustomName(Component.literal(title));
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked storage menu fixture");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			synchronizeClientChunk(player, primaryPos);
			return new LinkedStorageMenuFixture(primaryPos, secondaryPos, requireEndpoint(primary, "linked storage menu primary").groupId(),
					primary.getStorageWrapper().getInventoryHandler().size(), title, originalPosition, originalYRot, originalXRot);
		});
		try {
			movePlayerNear(fixture.secondaryPos());
			waitForClientStorageBlockEntity(fixture.secondaryPos());
			openLinkedStorageMenu(fixture.secondaryPos());
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			movePlayerNear(fixture.primaryPos());
			openLinkedStorageMenu(fixture.primaryPos());
			waitForClientLinkedStorageMenu(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
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

	private static void waitForClientLinkedStorageMenu(LinkedStorageMenuFixture fixture, BlockPos expectedPos, LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getTitle().getString().equals(fixture.title())
							&& screen.getMenu() instanceof StorageContainerMenu menu && menu.getBlockPosition().filter(expectedPos::equals).isPresent()
							&& fixture.groupId().equals(menu.getStorageBlockEntity().getLinkedStorageEndpointData().groupId())
							&& menu.getStorageWrapper().getInventoryHandler().size() == fixture.inventorySlots()
							&& menu.getNumberOfStorageInventorySlots() == fixture.inventorySlots()
							&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
							&& endpointProvider.getLinkedStorageEndpoint().filter(endpoint -> endpoint.groupId().equals(fixture.groupId())).isPresent()
							&& endpointProvider.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent()
							&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7)) {
				return;
			}
			sleep(50);
		}
		String clientState = AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
				return "screen=" + Minecraft.getInstance().screen;
			}
			if (!(screen.getMenu() instanceof StorageContainerMenu menu)) {
				return "title=" + screen.getTitle().getString() + ", menu=" + screen.getMenu().getClass().getName();
			}
			if (!(menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider)) {
				return "title=" + screen.getTitle().getString() + ", wrapper=" + menu.getStorageWrapper().getClass().getName();
			}
			return "title=" + screen.getTitle().getString() + ", pos=" + menu.getBlockPosition() + ", blockEndpoint="
					+ menu.getStorageBlockEntity().getLinkedStorageEndpointData() + ", wrapperEndpoint=" + endpointProvider.getLinkedStorageEndpoint()
					+ ", role=" + endpointProvider.getLinkedStorageEndpointRole() + ", handlerSlots=" + menu.getStorageWrapper().getInventoryHandler().size()
					+ ", menuSlots=" + menu.getNumberOfStorageInventorySlots() + ", diamonds="
					+ count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND);
		});
		throw new IllegalStateException("Linked storage menu did not receive the canonical snapshot, title, and " + expectedRole + " role at " + expectedPos
				+ " with expected group " + fixture.groupId() + " and " + fixture.inventorySlots() + " slots: " + clientState);
	}

	private static void runLinkedStorageMenuTransitionRegression() {
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 9);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
			String title = "Linked Storage Transition";
			primary.setCustomName(Component.literal(title));
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked storage menu transition fixture");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			synchronizeClientChunk(player, primaryPos);
			return new LinkedStorageMenuFixture(primaryPos, secondaryPos, requireEndpoint(primary, "menu transition primary").groupId(),
					primary.getStorageWrapper().getInventoryHandler().size(), title);
		});
		try {
			sendOpenStorageInventoryPayload(fixture.secondaryPos());
			long noMenuVerificationTime = AutomationRuntime.runOnServer(player -> player.level().getGameTime() + 2L);
			waitForServerCondition("no-menu linked storage inventory payload rejection",
					player -> player.level().getGameTime() >= noMenuVerificationTime && player.containerMenu == player.inventoryMenu);

			movePlayerNear(fixture.secondaryPos());
			waitForClientStorageBlockEntity(fixture.secondaryPos());
			openLinkedStorageMenu(fixture.secondaryPos());
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			sendOpenStorageInventoryPayload(fixture.secondaryPos());
			long wrongMenuVerificationTime = AutomationRuntime.runOnServer(player -> player.level().getGameTime() + 2L);
			waitForServerCondition("wrong-menu linked storage inventory payload rejection",
					player -> player.level().getGameTime() >= wrongMenuVerificationTime && player.containerMenu instanceof StorageContainerMenu);
			AutomationRuntime.runOnServer(player -> {
				if (!(player.containerMenu instanceof StorageContainerMenu menu)) {
					throw new IllegalStateException("Linked storage inventory menu did not open before stale action test");
				}
				CompoundTag staleAction = new CompoundTag();
				staleAction.putString("action", "openSettings");
				staleAction.putInt("sourceContainerId", menu.containerId + 1);
				menu.handlePacket(staleAction);
				require(player.containerMenu == menu, "Stale linked storage settings action replaced the active menu");
				return true;
			});
			AutomationRuntime.runOnServer(player -> {
				StorageContainerMenu menu = (StorageContainerMenu) player.containerMenu;
				BlockPos invalidPosition = fixture.secondaryPos().east(10);
				player.teleportTo(player.level(), invalidPosition.getCenter().x, invalidPosition.getY(), invalidPosition.getCenter().z, Set.of(),
						player.getYRot(), player.getXRot(), false);
				menu.openSettings();
				require(player.containerMenu == menu, "Invalid linked storage menu opened settings");
				return true;
			});
			movePlayerNear(fixture.secondaryPos());

			openLinkedStorageSettingsMenu(fixture.secondaryPos());
			waitForClientLinkedStorageSettingsMenu(fixture.secondaryPos(), fixture.inventorySlots(), LinkedStorageEndpointRole.SECONDARY);
			sendOpenStorageInventoryPayload(fixture.primaryPos());
			long mismatchVerificationTime = AutomationRuntime.runOnServer(player -> player.level().getGameTime() + 2L);
			waitForServerCondition("mismatched linked storage inventory payload rejection",
					player -> player.level().getGameTime() >= mismatchVerificationTime
							&& player.containerMenu instanceof StorageSettingsContainerMenu settingsMenu
							&& settingsMenu.getBlockPosition().equals(fixture.secondaryPos()));

			sendOpenStorageInventoryPayload(fixture.secondaryPos());
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClientStorageMenuClosed();
			waitForServerCondition("linked storage opener cleanup after settings transition",
					player -> !getBarrel(player.level(), fixture.secondaryPos()).isOpen());
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void runLinkedPrimaryChestExpansionMenuRegression() {
		LinkedPrimaryChestExpansionFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 3);
			BlockPos addedChestPos = primaryPos.west();
			clearArea(level, primaryPos);
			placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity primary = getChest(level, primaryPos);
			String title = "Expanded Linked Primary";
			primary.setCustomName(Component.literal(title));
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), new ItemStack(ENDER_LINKER.get(), 2),
					primary) == LinkedStorageService.LinkResult.SUCCESS, "Could not create linked primary chest for menu expansion regression");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			synchronizeClientChunk(player, primaryPos);
			return new LinkedPrimaryChestExpansionFixture(primaryPos, addedChestPos,
					requireEndpoint(primary, "linked primary chest before expansion").groupId(), title);
		});
		try {
			waitForClientStorageBlockEntity(fixture.primaryPos());
			openLinkedStorageChestMenu(fixture.primaryPos());
			waitForClientLinkedStorageMenu(new LinkedStorageMenuFixture(fixture.primaryPos(), fixture.primaryPos(), fixture.groupId(), 27, fixture.title()),
					fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			openLinkedStorageSettingsMenu(fixture.primaryPos());
			waitForClientLinkedStorageSettingsMenu(fixture.primaryPos(), 27, LinkedStorageEndpointRole.PRIMARY);
			BlockPos expandedPrimaryPos = AutomationRuntime.runOnServer(player -> {
				placeChest(player.level(), player, fixture.addedChestPos(), new ItemStack(ModBlocks.CHEST_ITEM.get()));
				List<ChestBlockEntity> parts = List.of(getChest(player.level(), fixture.primaryPos()), getChest(player.level(), fixture.addedChestPos()));
				ChestBlockEntity expandedPrimary = parts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after linked primary expansion"));
				require(player.containerMenu == player.inventoryMenu, "Canonical root replacement left the stale 27-slot settings menu open");
				require(parts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
						&& expandedPrimary.getStorageWrapper().getInventoryHandler().size() == 54
						&& count(expandedPrimary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
						&& requireEndpoint(expandedPrimary, "expanded linked primary chest").groupId().equals(fixture.groupId())
						&& expandedPrimary.getStorageWrapper().getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent(),
						"Linked primary expansion did not retain its endpoint, contents, and 54-slot canonical profile");
				synchronizeClientChunk(player, expandedPrimary.getBlockPos());
				return expandedPrimary.getBlockPos();
			});
			waitForClientStorageMenuClosed();
			waitForClientStorageBlockEntity(expandedPrimaryPos);
			openLinkedStorageMenu(expandedPrimaryPos);
			waitForClientLinkedStorageMenu(new LinkedStorageMenuFixture(expandedPrimaryPos, expandedPrimaryPos, fixture.groupId(), 54, fixture.title()),
					expandedPrimaryPos, LinkedStorageEndpointRole.PRIMARY);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void runLinkedLimitedBarrelMemorySyncRegression() {
		LinkedLimitedBarrelFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 2);
			BlockPos secondaryPos = primaryPos.east(2);
			clearArea(level, primaryPos);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked limited barrels for memory synchronization regression");
			primary.toggleLock();
			synchronizeClientChunk(player, primaryPos);
			return new LinkedLimitedBarrelFixture(primaryPos, secondaryPos, requireEndpoint(primary, "linked limited-barrel primary").groupId(),
					player.getItemInHand(InteractionHand.MAIN_HAND).copy());
		});
		try {
			movePlayerNear(fixture.secondaryPos());
			waitForClientStorageBlockEntity(fixture.secondaryPos());
			openLimitedBarrelMenu(fixture.secondaryPos());
			AutomationRuntime.runOnServer(player -> {
				LimitedBarrelBlockEntity primary = getLimitedBarrel(player.level(), fixture.primaryPos());
				LimitedBarrelBlockEntity secondary = getLimitedBarrel(player.level(), fixture.secondaryPos());
				ItemStack diamonds = new ItemStack(Items.DIAMOND, 7);
				player.setItemInHand(InteractionHand.MAIN_HAND, diamonds);
				require(primary.depositItem(player, InteractionHand.MAIN_HAND, diamonds, 0),
						"Locked linked limited barrel did not accept the memorized-slot deposit");
				MemorySettingsCategory primaryMemory = primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				MemorySettingsCategory secondaryMemory = secondary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				require(primaryMemory.getSlotFilterStack(0, false).filter(stack -> stack.is(Items.DIAMOND)).isPresent()
						&& secondaryMemory.getSlotFilterStack(0, false).filter(stack -> stack.is(Items.DIAMOND)).isPresent(),
						"Linked limited-barrel memory did not update through both canonical wrappers");
				return true;
			});
			waitForClientLinkedMemory(fixture.secondaryPos());
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, fixture.originalMainHand());
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void runLinkedStorageStackTooltipRegression() {
		LinkedStorageStackTooltipFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 6);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity primary = getBarrel(level, primaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker,
							getBarrel(level, secondaryPos)) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked storage for dropped-stack tooltip regression");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			UUID groupId = requireEndpoint(primary, "dropped linked stack primary").groupId();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			int inventorySlot = 8;
			ItemStack originalStack = player.getInventory().getItem(inventorySlot).copy();
			player.setGameMode(GameType.SURVIVAL);
			require(player.gameMode.destroyBlock(primaryPos), "Player break did not drop the linked storage tooltip carrier");
			List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos).inflate(1.5D),
					itemEntity -> itemEntity.getItem().is(ModBlocks.BARREL_ITEM.get()));
			require(drops.size() == 1
					&& StorageBlockEntity.getLinkedStorageEndpointData(drops.getFirst().getItem()).filter(endpoint -> endpoint.groupId().equals(groupId))
							.isPresent()
					&& StorageBlockEntity.getLinkedStorageEndpointRole(drops.getFirst().getItem()).filter(LinkedStorageEndpointRole.PRIMARY::equals)
							.isPresent(),
					"Dropped linked storage stack did not retain its primary endpoint group and role");
			player.getInventory().setItem(inventorySlot, drops.getFirst().getItem().copy());
			drops.forEach(ItemEntity::discard);
			player.inventoryMenu.broadcastChanges();
			return new LinkedStorageStackTooltipFixture(primaryPos, groupId, inventorySlot, originalStack, originalGameMode);
		});
		try {
			waitForClientLinkedStorageTooltip(fixture);
			waitForClientLinkedStorageTooltipContents(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().setItem(fixture.inventorySlot(), fixture.originalStack());
				player.inventoryMenu.broadcastChanges();
				player.setGameMode(fixture.originalGameMode());
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void runSecondaryDroppedPickupRegression() {
		DroppedItemPickupFixture fixture = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::setupDroppedItemPickupFixture);
		try {
			waitForServerCondition("dropped item to remain on linked secondary endpoint",
					player -> player.level().getGameTime() >= fixture.verifyAfterGameTime()
							&& findDroppedItem(player.level(), fixture, fixture.secondaryPos()).filter(item -> item.getItem().getCount() == 2).isPresent()
							&& count(getBarrel(player.level(), fixture.primaryPos()).getStorageWrapper().getInventoryHandler(), fixture.item()) == 0);
			AutomationRuntime.runOnServer(player -> {
				ItemEntity item = findDroppedItem(player.level(), fixture, fixture.secondaryPos())
						.orElseThrow(() -> new IllegalStateException("Dropped item disappeared from the linked secondary endpoint"));
				item.setPos(fixture.primaryPos().getCenter().add(0, -0.25D, 0));
				return true;
			});
			waitForServerCondition("linked primary endpoint to pick up the dropped item",
					player -> findDroppedItem(player.level(), fixture, fixture.primaryPos()).isEmpty()
							&& count(getBarrel(player.level(), fixture.primaryPos()).getStorageWrapper().getInventoryHandler(), fixture.item()) == 2);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.teleportTo(player.level(), fixture.originalPosition().x, fixture.originalPosition().y, fixture.originalPosition().z, Set.of(),
						player.getYRot(), player.getXRot(), true);
				clearArea(player.level(), fixture.primaryPos());
				clearArea(player.level(), fixture.secondaryPos());
				return true;
			});
		}
	}

	public static String setupLinkedLimitedBarrelReloadProjection() {
		return AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = getLinkedLimitedReloadPrimaryPos(player);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()));
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
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
					+ "\",\"secondaryEndpointId\":\"" + secondaryEndpoint.endpointId() + "\",\"primaryX\":" + primaryPos.getX() + ",\"primaryY\":"
					+ primaryPos.getY() + ",\"primaryZ\":" + primaryPos.getZ() + "}";
		});
	}

	public static String linkedLimitedBarrelReloadProjectionStatus(UUID groupId, BlockPos primaryPos) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos secondaryPos = primaryPos.east(3);
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
			require(requireEndpoint(primary, "reloaded limited primary").groupId().equals(groupId)
					&& requireEndpoint(secondary, "reloaded limited secondary").groupId().equals(groupId),
					"Reloaded limited barrels do not belong to the expected linked-storage group");
			require(hasLimitedRenderProjection(primary) && hasLimitedRenderProjection(secondary),
					"Reloaded linked limited barrels did not restore server render projection");
			WorldHelper.notifyBlockUpdate(primary);
			WorldHelper.notifyBlockUpdate(secondary);
			return true;
		});
		waitForClientLimitedBarrelReloadProjection(primaryPos);
		return "{\"ok\":true,\"clientDisplayItems\":true,\"clientCounts\":true,\"clientFillLevels\":true}";
	}

	private static Boolean runOrdinaryLinkedStorage(ServerPlayer player) {
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
			ResourceHandler<ItemResource> secondaryCapability = level.getCapability(Capabilities.Item.BLOCK, secondaryPos, Direction.UP);
			int inserted;
			try (Transaction transaction = Transaction.openRoot()) {
				inserted = secondaryCapability == null ? 0 : secondaryCapability.insert(ItemResource.of(new ItemStack(Items.EMERALD)), 3, transaction);
				transaction.commit();
			}
			require(inserted == 3 && count(primary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
					&& count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
					&& count(primary.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3,
					"Linked barrel endpoints did not expose one canonical inventory through capabilities");
			return true;
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, primaryPos);
		}
	}

	private static Boolean runCanonicalStorageTypeInsertionRules(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 4);
		BlockPos secondaryPos = primaryPos.east(3);
		clearArea(level, primaryPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			StorageBlockEntity primary = WorldHelper.getBlockEntity(level, primaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing canonical shulker endpoint"));
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link canonical shulker to barrel endpoint");
			require(insertThroughCapability(level, secondaryPos, new ItemStack(Items.SHULKER_BOX)) == 0,
					"Physical barrel endpoint bypassed the canonical shulker insertion restriction");

			clearArea(level, primaryPos);
			placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			primary = WorldHelper.getBlockEntity(level, primaryPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing canonical barrel endpoint"));
			require(linkWithPlayer(player, primary, primaryPos, secondaryPos), "Could not link canonical barrel to shulker endpoint");
			require(insertThroughCapability(level, secondaryPos, new ItemStack(Items.SHULKER_BOX)) == 1,
					"Physical shulker endpoint overrode the canonical barrel insertion rule");
			return true;
		} finally {
			player.setGameMode(originalGameMode);
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
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		ItemStack originalDyeSlot = player.getInventory().getItem(8).copy();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, 4);
		BlockPos existingEndpointPos = controllerPos.east();
		BlockPos joiningEndpointPos = existingEndpointPos.east();
		clearArea(level, controllerPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeBlockWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			placeBlockWithPlayer(level, player, existingEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			placeBlockWithPlayer(level, player, joiningEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			ControllerBlockEntity controller = getController(level, controllerPos);
			BarrelBlockEntity existingEndpoint = getBarrel(level, existingEndpointPos);
			BarrelBlockEntity joiningEndpoint = getBarrel(level, joiningEndpointPos);
			int canonicalSlots = existingEndpoint.getStorageWrapper().getInventoryHandler().size();
			require(controller.getStoragePositions().size() == 2 && controller.size() == canonicalSlots * 2,
					"Controller did not register both unlinked barrels before linking");
			require(linkWithPlayer(player, existingEndpoint, existingEndpointPos, joiningEndpointPos),
					"Could not create controller-connected linked barrel group");
			existingEndpoint.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			existingEndpoint.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(controller.getStoragePositions().size() == 1 && controller.size() == canonicalSlots && controller.getStackStorages(diamondKey).size() == 1,
					"Controller duplicated canonical contents when a connected endpoint joined a linked group");
			try (Transaction transaction = Transaction.openRoot()) {
				require(controller.insert(ItemResource.of(new ItemStack(Items.EMERALD)), 3, transaction) == 3,
						"Controller did not route items to the linked canonical contents");
				transaction.commit();
			}
			require(count(joiningEndpoint.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3,
					"Controller-routed items were not visible through the linked endpoint");
			ItemStackKey emeraldKey = ItemStackKey.of(new ItemStack(Items.EMERALD));
			Set<BlockPos> physicalEndpoints = Set.of(existingEndpointPos, joiningEndpointPos);
			require(controller.getStorageBlockPositions().equals(physicalEndpoints), "Controller physical storage positions did not expand the linked group");
			require(controller.getHighlightStoragePositions(controller.getStackStorages(emeraldKey)).containsAll(physicalEndpoints),
					"Controller matching-stack highlights did not expand to every linked endpoint");
			require(controller.getStorageTierUpgradePositions().equals(Set.of(existingEndpointPos)),
					"Controller tier candidates did not select only the linked primary endpoint");
			ItemStack paintbrush = new ItemStack(ModItems.PAINTBRUSH.get());
			PaintbrushItem.setMainColor(paintbrush, 0xFFFF0000);
			PaintbrushItem.ItemRequirements requirements = PaintbrushItem.getItemRequirements(paintbrush, player, level, controllerPos)
					.orElseThrow(() -> new IllegalStateException("Linked controller paintbrush requirements were missing"));
			int requiredRedDyes = requirements.itemsPresent().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum()
					+ requirements.itemsMissing().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum();
			require(requiredRedDyes == 2, "Controller paintbrush requirements did not include both linked physical endpoints");
			player.getInventory().setItem(8, new ItemStack(Items.RED_DYE, 2));
			require(paintbrush.getItem().onItemUseFirst(paintbrush,
					new UseOnContext(player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false))) == InteractionResult.SUCCESS
					&& existingEndpoint.getStorageWrapper().getMainColor() == 0xFFFF0000 && joiningEndpoint.getStorageWrapper().getMainColor() == 0xFFFF0000,
					"Controller paintbrush did not color every linked physical endpoint");
			controller.toggleLock();
			require(existingEndpoint.isLocked() && joiningEndpoint.isLocked(), "Controller lock did not fan out to linked members");
			boolean initialLockVisibility = existingEndpoint.shouldShowLock();
			require(initialLockVisibility == joiningEndpoint.shouldShowLock(), "Linked endpoints started with different lock visibility");
			controller.toggleLockVisibility();
			require(existingEndpoint.shouldShowLock() == !initialLockVisibility && joiningEndpoint.shouldShowLock() == !initialLockVisibility,
					"Controller lock visibility did not fan out to every linked endpoint");
			boolean initialTierVisibility = existingEndpoint.shouldShowTier();
			require(initialTierVisibility == joiningEndpoint.shouldShowTier(), "Linked endpoints started with different tier visibility");
			controller.toggleTierVisiblity();
			require(existingEndpoint.shouldShowTier() == !initialTierVisibility && joiningEndpoint.shouldShowTier() == !initialTierVisibility,
					"Controller tier visibility did not fan out to every linked endpoint");
			boolean initialUpgradeVisibility = existingEndpoint.shouldShowUpgrades();
			require(initialUpgradeVisibility == joiningEndpoint.shouldShowUpgrades(), "Linked endpoints started with different upgrade visibility");
			controller.toggleUpgradesVisiblity();
			require(existingEndpoint.shouldShowUpgrades() == !initialUpgradeVisibility && joiningEndpoint.shouldShowUpgrades() == !initialUpgradeVisibility,
					"Controller upgrade visibility did not fan out to every linked endpoint");
			controller.toggleLock();
			level.setBlock(controllerPos, Blocks.AIR.defaultBlockState(), 3);
			require(existingEndpoint.getControllerPos().isEmpty() && joiningEndpoint.getControllerPos().isEmpty(),
					"Removing the controller did not detach linked storage members");
			placeBlock(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			ControllerBlockEntity restoredController = getController(level, controllerPos);
			restoredController.searchAndAddBoundables();
			require(existingEndpoint.getControllerPos().filter(controllerPos::equals).isPresent()
					&& joiningEndpoint.getControllerPos().filter(controllerPos::equals).isPresent() && restoredController.getStoragePositions().size() == 1
					&& restoredController.getStackStorages(diamondKey).size() == 1, "Restored controller did not reconnect the linked canonical storage index");
			return true;
		} finally {
			player.getInventory().setItem(8, originalDyeSlot);
			player.setGameMode(originalGameMode);
			clearArea(level, controllerPos);
		}
	}

	private static Boolean runControllerAdjacentLinkedEndpointPlacementRegression(ServerPlayer player) {
		runControllerAdjacentLinkedEndpointPlacementRegression(player, ModBlocks.BARREL_ITEM.get(), "wood storage", false);
		runControllerAdjacentLinkedEndpointPlacementRegression(player, ModBlocks.SHULKER_BOX_ITEM.get(), "shulker box", true);
		return true;
	}

	private static void runControllerAdjacentLinkedEndpointPlacementRegression(ServerPlayer player, Item endpointItem, String endpointName,
			boolean creativePlacement) {
		ServerLevel level = player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		ItemStack originalMainHand = player.getMainHandItem().copy();
		BlockPos controllerPos = player.blockPosition().offset(0, 0, creativePlacement ? 12 : 4);
		BlockPos restoredEndpointPos = controllerPos.east();
		BlockPos remoteEndpointPos = restoredEndpointPos.east();
		clearArea(level, controllerPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeBlockWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			placeBlockWithPlayer(level, player, restoredEndpointPos, new ItemStack(endpointItem));
			placeBlockWithPlayer(level, player, remoteEndpointPos, new ItemStack(endpointItem));
			ControllerBlockEntity controller = getController(level, controllerPos);
			StorageBlockEntity endpoint = WorldHelper.getBlockEntity(level, restoredEndpointPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing controller-adjacent linked " + endpointName + " endpoint"));
			StorageBlockEntity remoteEndpoint = WorldHelper.getBlockEntity(level, remoteEndpointPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing remote linked " + endpointName + " endpoint"));
			require(linkWithPlayer(player, endpoint, restoredEndpointPos, remoteEndpointPos),
					"Could not create controller-adjacent linked " + endpointName + " group");
			LinkedStorageEndpointData endpointIdentity = requireEndpoint(endpoint, "controller-adjacent linked " + endpointName);
			LinkedStorageEndpointData remoteIdentity = requireEndpoint(remoteEndpoint, "remote linked " + endpointName);
			require(endpointIdentity.groupId().equals(remoteIdentity.groupId()) && !endpointIdentity.endpointId().equals(remoteIdentity.endpointId())
					&& controller.getStoragePositions().size() == 1
					&& controller.getStorageBlockPositions().equals(Set.of(restoredEndpointPos, remoteEndpointPos)),
					"Controller did not initially register the linked " + endpointName + " endpoints");

			player.setGameMode(GameType.SURVIVAL);
			require(player.gameMode.destroyBlock(restoredEndpointPos), "Player break did not remove the controller-adjacent linked " + endpointName);
			ItemStack endpointCarrier = level
					.getEntitiesOfClass(ItemEntity.class, new AABB(restoredEndpointPos).inflate(1.5D),
							itemEntity -> itemEntity.getItem().is(endpointItem)
									&& endpointIdentity.equals(itemEntity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
					.stream().findFirst().map(itemEntity -> itemEntity.getItem().copy()).orElseThrow(
							() -> new IllegalStateException("Breaking the controller-adjacent linked " + endpointName + " did not preserve its endpoint item"));
			level.getEntitiesOfClass(ItemEntity.class, new AABB(restoredEndpointPos).inflate(1.5D)).forEach(ItemEntity::discard);
			if (creativePlacement) {
				player.setGameMode(GameType.CREATIVE);
			}
			placeBlockWithPlayer(level, player, restoredEndpointPos, endpointCarrier);
			StorageBlockEntity restoredEndpoint = WorldHelper.getBlockEntity(level, restoredEndpointPos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing restored controller-adjacent linked " + endpointName));
			LinkedStorageEndpointData restoredIdentity = requireEndpoint(restoredEndpoint, "restored controller-adjacent linked " + endpointName);
			boolean identityRestored = creativePlacement
					? restoredIdentity.groupId().equals(endpointIdentity.groupId()) && !restoredIdentity.endpointId().equals(endpointIdentity.endpointId())
					: restoredIdentity.equals(endpointIdentity);
			require(identityRestored && remoteIdentity.equals(requireEndpoint(remoteEndpoint, "remote linked " + endpointName + " after placement"))
					&& restoredEndpoint.getControllerPos().filter(controllerPos::equals).isPresent()
					&& remoteEndpoint.getControllerPos().filter(controllerPos::equals).isPresent() && controller.getStoragePositions().size() == 1
					&& controller.getStorageBlockPositions().equals(Set.of(restoredEndpointPos, remoteEndpointPos)),
					"Restored controller-adjacent linked " + endpointName + " did not restore endpoint identity and controller membership");
			try (Transaction transaction = Transaction.openRoot()) {
				require(controller.insert(ItemResource.of(new ItemStack(Items.EMERALD)), 3, transaction) == 3,
						"Controller did not route into the restored linked " + endpointName + " group");
				transaction.commit();
			}
			require(count(restoredEndpoint.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3
					&& count(remoteEndpoint.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3,
					"Restored controller-adjacent linked " + endpointName + " did not retain canonical controller routing");
		} finally {
			player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
			player.setGameMode(originalGameMode);
			clearArea(level, controllerPos);
		}
	}

	private static Boolean runLinkedDoubleChestControllerCanonicalContents(ServerPlayer player) {
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
			placeBlockWithPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			placeBlockWithPlayer(level, player, primaryLeftChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeBlockWithPlayer(level, player, primaryMainChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeBlockWithPlayer(level, player, secondaryLeftChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeBlockWithPlayer(level, player, secondaryMainChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ControllerBlockEntity controller = getController(level, controllerPos);
			ChestBlockEntity primary = getChest(level, primaryMainChestPos);
			ChestBlockEntity secondary = getChest(level, secondaryMainChestPos);
			require(isDoubleChest(level, primaryLeftChestPos, primaryMainChestPos) && isDoubleChest(level, secondaryLeftChestPos, secondaryMainChestPos),
					"Real-player placement did not create both linked-storage double chests");
			require(linkWithPlayer(player, primary, primaryMainChestPos, secondaryMainChestPos), "Could not link player-placed double chests");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
			require(count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7 && controller.getStoragePositions().size() == 1
					&& controller.getStoragePositions().contains(primaryMainChestPos) && controller.size() == 54
					&& controller.getStackStorages(diamondKey).size() == 1, "Linked double chests did not share one canonical controller storage");
			BlockPos ordinaryStoragePos = controllerPos.west();
			placeBlockWithPlayer(level, player, ordinaryStoragePos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
			BarrelBlockEntity ordinaryStorage = getBarrel(level, ordinaryStoragePos);
			ChestBlockEntity primaryOtherHalf = getChest(level, primaryLeftChestPos);
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
			player.setGameMode(originalGameMode);
			clearArea(level, controllerPos);
		}
	}

	private static Boolean runLinkedDoubleChestLifecycle(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 12);
		BlockPos addedChestPos = primaryPos.east();
		BlockPos secondaryPos = primaryPos.east(5);
		BlockPos secondaryAddedChestPos = secondaryPos.west();
		clearArea(level, primaryPos);
		try {
			player.setGameMode(GameType.CREATIVE);
			placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity primary = getChest(level, primaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 4);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked primary chest for lifecycle regression");
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "single linked primary chest");
			placeChest(level, player, addedChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			List<ChestBlockEntity> primaryParts = List.of(getChest(level, primaryPos), getChest(level, addedChestPos));
			ChestBlockEntity expandedPrimary = primaryParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Missing main chest after expanding linked primary"));
			require(primaryParts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
					&& primaryParts.stream().anyMatch(chest -> chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
					&& expandedPrimary.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT
					&& primaryEndpoint.equals(requireEndpoint(expandedPrimary, "expanded linked primary chest"))
					&& expandedPrimary.getStorageWrapper().getInventoryHandler().size() == 54,
					"Adding a chest did not expand the linked primary into a double chest");

			player.setGameMode(GameType.SURVIVAL);
			require(player.gameMode.destroyBlock(expandedPrimary.getBlockPos()), "Player break did not remove the linked primary double chest");
			List<ItemEntity> primaryDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos).inflate(1.5D),
					itemEntity -> itemEntity.getItem().is(ModBlocks.CHEST_ITEM.get()));
			require(level.getBlockState(primaryPos).isAir() && level.getBlockState(addedChestPos).isAir() && primaryDrops.size() == 1
					&& ChestBlockItem.isDoubleChest(primaryDrops.getFirst().getItem())
					&& primaryEndpoint.equals(primaryDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& StorageBlockEntity.getLinkedStorageEndpointRole(primaryDrops.getFirst().getItem())
							.filter(role -> role == LinkedStorageEndpointRole.PRIMARY).isPresent(),
					"Breaking a linked primary double chest did not drop one paired endpoint carrier");
			ItemStack carrier = primaryDrops.getFirst().getItem().copy();
			primaryDrops.forEach(ItemEntity::discard);
			placeChest(level, player, primaryPos, carrier);
			ChestBlockEntity restoredPrimary = getChest(level, primaryPos);
			BlockPos restoredOtherPos = primaryPos.relative(ChestBlock.getConnectedDirection(restoredPrimary.getBlockState()));
			restoredPrimary = restoredPrimary.isMainChest() ? restoredPrimary : getChest(level, restoredOtherPos);
			require(level.getBlockState(restoredOtherPos).getValue(ChestBlock.TYPE) != ChestType.SINGLE
					&& primaryEndpoint.equals(requireEndpoint(restoredPrimary, "restored linked primary chest"))
					&& restoredPrimary.getStorageWrapper().getInventoryHandler().size() == 54,
					"Linked primary double chest carrier did not restore as a paired endpoint");

			clearArea(level, primaryPos);
			player.setGameMode(GameType.CREATIVE);
			placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeChest(level, player, secondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity linkedPrimary = getChest(level, primaryPos);
			ChestBlockEntity secondary = getChest(level, secondaryPos);
			ItemStack secondaryLinker = new ItemStack(ENDER_LINKER.get(), 4);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), secondaryLinker, linkedPrimary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), secondaryLinker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked secondary chest for split regression");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "linked secondary chest");
			List<ItemStack> canonicalContents = fillInventoryWithTestContents(linkedPrimary.getStorageWrapper().getInventoryHandler());
			linkedPrimary.getStorageWrapper().getInventoryHandler().saveInventory();
			placeChest(level, player, secondaryAddedChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity secondaryMain = getChest(level, secondaryPos);
			require(secondaryMain.isMainChest(), "Original linked secondary chest did not become the main chest");
			player.setGameMode(GameType.SURVIVAL);
			ItemStack nonMainTierUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
			require(useLinkerAsPlayer(player, nonMainTierUpgrade, secondaryAddedChestPos) == InteractionResult.FAIL,
					"Linked secondary double-chest non-main half accepted a tier upgrade");
			require(nonMainTierUpgrade.getCount() == 1 && level.getBlockState(secondaryPos).is(ModBlocks.CHEST.get())
					&& level.getBlockState(secondaryAddedChestPos).is(ModBlocks.CHEST.get())
					&& secondaryEndpoint
							.equals(requireEndpoint(getChest(level, secondaryMain.getBlockPos()), "linked secondary main after rejected tier upgrade")),
					"Rejected linked secondary double-chest tier upgrade changed a chest or its endpoint");
			require(player.gameMode.destroyBlock(secondaryMain.getBlockPos()), "Player break did not remove the linked secondary double chest half");
			List<ItemEntity> secondaryDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(secondaryMain.getBlockPos()).inflate(1.5D));
			ChestBlockEntity remainingSecondary = getChest(level, secondaryAddedChestPos);
			require(remainingSecondary.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
					&& secondaryEndpoint.equals(requireEndpoint(remainingSecondary, "remaining linked secondary chest"))
					&& hasInventoryContents(remainingSecondary.getStorageWrapper().getInventoryHandler(), canonicalContents)
					&& remainingSecondary.getCustomName() == null && secondaryDrops.size() == 1
					&& !ChestBlockItem.isDoubleChest(secondaryDrops.getFirst().getItem())
					&& secondaryDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
					&& secondaryDrops.getFirst().getItem().get(DataComponents.CUSTOM_NAME) == null,
					"Breaking the linked secondary main chest changed or dropped linked inventory contents: remainingType="
							+ remainingSecondary.getBlockState().getValue(ChestBlock.TYPE) + ", remainingEndpoint="
							+ requireEndpoint(remainingSecondary, "remaining linked secondary chest diagnostics") + ", expectedEndpoint=" + secondaryEndpoint
							+ ", drops=" + secondaryDrops.stream().map(drop -> "double=" + ChestBlockItem.isDoubleChest(drop.getItem()) + ", endpoint="
									+ drop.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)).toList());
			ItemStack secondaryDrop = secondaryDrops.getFirst().getItem().copy();
			secondaryDrops.forEach(ItemEntity::discard);

			placeChest(level, player, secondaryAddedChestPos.west(), secondaryDrop);
			ChestBlockEntity shiftedSecondaryMain = getChest(level, secondaryAddedChestPos);
			require(shiftedSecondaryMain.isMainChest() && shiftedSecondaryMain.getCustomName() == null,
					"Shifted linked secondary double chest did not retain the unnamed main endpoint");
			require(player.gameMode.destroyBlock(shiftedSecondaryMain.getBlockPos()), "Player break did not remove the shifted linked secondary main chest");
			List<ItemEntity> shiftedSecondaryDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(shiftedSecondaryMain.getBlockPos()).inflate(1.5D));
			ChestBlockEntity shiftedSecondarySurvivor = getChest(level, secondaryAddedChestPos.west());
			require(shiftedSecondarySurvivor.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
					&& secondaryEndpoint.equals(requireEndpoint(shiftedSecondarySurvivor, "shifted remaining linked secondary chest"))
					&& hasInventoryContents(shiftedSecondarySurvivor.getStorageWrapper().getInventoryHandler(), canonicalContents)
					&& shiftedSecondarySurvivor.getCustomName() == null && shiftedSecondaryDrops.size() == 1
					&& !ChestBlockItem.isDoubleChest(shiftedSecondaryDrops.getFirst().getItem())
					&& shiftedSecondaryDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
					&& shiftedSecondaryDrops.getFirst().getItem().get(DataComponents.CUSTOM_NAME) == null,
					"Breaking the shifted linked secondary main chest changed or dropped linked inventory contents");
			shiftedSecondaryDrops.forEach(ItemEntity::discard);

			clearArea(level, primaryPos);
			player.setGameMode(GameType.CREATIVE);
			placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeChest(level, player, secondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			linkedPrimary = getChest(level, primaryPos);
			secondary = getChest(level, secondaryPos);
			secondaryLinker = new ItemStack(ENDER_LINKER.get(), 4);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), secondaryLinker, linkedPrimary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), secondaryLinker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked secondary chest for reverse split regression");
			secondaryEndpoint = requireEndpoint(secondary, "linked secondary chest for reverse split regression");
			canonicalContents = fillInventoryWithTestContents(linkedPrimary.getStorageWrapper().getInventoryHandler());
			linkedPrimary.getStorageWrapper().getInventoryHandler().saveInventory();
			placeChest(level, player, secondaryPos.east(), new ItemStack(ModBlocks.CHEST_ITEM.get()));
			List<ChestBlockEntity> reformedSecondaryParts = List.of(getChest(level, secondaryPos), getChest(level, secondaryPos.east()));
			ChestBlockEntity reformedSecondaryMain = reformedSecondaryParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Missing main chest for reverse linked secondary split regression"));
			BlockPos reformedNonMainPos = reformedSecondaryParts.stream().filter(chest -> !chest.isMainChest()).findFirst()
					.orElseThrow(() -> new IllegalStateException("Missing non-main chest for reverse linked secondary split regression")).getBlockPos();
			player.setGameMode(GameType.SURVIVAL);
			require(player.gameMode.destroyBlock(reformedNonMainPos), "Player break did not remove the linked secondary non-main chest");
			List<ItemEntity> reformedDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(reformedNonMainPos).inflate(1.5D));
			require(reformedSecondaryMain.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
					&& secondaryEndpoint.equals(reformedSecondaryMain.getLinkedStorageEndpointData())
					&& hasInventoryContents(linkedPrimary.getStorageWrapper().getInventoryHandler(), canonicalContents) && reformedDrops.size() == 1
					&& !ChestBlockItem.isDoubleChest(reformedDrops.getFirst().getItem())
					&& reformedDrops.getFirst().getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null,
					"Breaking the linked secondary non-main chest changed or dropped linked inventory contents");
			return true;
		} finally {
			player.setGameMode(originalGameMode);
			clearArea(level, primaryPos);
		}
	}

	private static void waitForClientLimitedBarrelReloadProjection(BlockPos primaryPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.level == null || minecraft.player == null) {
					return false;
				}
				BlockPos secondaryPos = primaryPos.east(3);
				return minecraft.level.getBlockEntity(primaryPos) instanceof LimitedBarrelBlockEntity primary
						&& minecraft.level.getBlockEntity(secondaryPos) instanceof LimitedBarrelBlockEntity secondary && hasLimitedRenderProjection(primary)
						&& hasLimitedRenderProjection(secondary);
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Reloaded linked limited barrels did not synchronize display items, counts, and fill levels to the client");
	}

	private static boolean hasLimitedRenderProjection(LimitedBarrelBlockEntity barrel) {
		return barrel.getStorageWrapper().getRenderDataHandler().getDisplayData().displayItems().stream()
				.anyMatch(displayItem -> displayItem.item().is(Items.DIAMOND)) && barrel.getSlotCounts().contains(LINKED_LIMITED_RELOAD_ITEM_COUNT)
				&& barrel.getSlotFillLevels().stream().anyMatch(fillLevel -> fillLevel > 0F);
	}

	private static BlockPos getLinkedLimitedReloadPrimaryPos(ServerPlayer player) {
		return player.blockPosition().offset(0, 0, 4);
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
		Vec3 originalPosition = player.position();
		float originalYRot = player.getYRot();
		float originalXRot = player.getXRot();
		try {
			player.teleportTo(level, pos.getCenter().x, pos.getY() + 1.5D, pos.getCenter().z, Set.of(), 0, 0, true);
			placeBlockWithPlayer(level, player, pos, stack);
		} finally {
			player.teleportTo(level, originalPosition.x, originalPosition.y, originalPosition.z, Set.of(), originalYRot, originalXRot, true);
		}
	}

	private static boolean linkWithPlayer(ServerPlayer player, StorageBlockEntity primary, BlockPos primaryPos, BlockPos secondaryPos) {
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
	}

	private static net.minecraft.world.InteractionResult useLinkerAsPlayer(ServerPlayer player, ItemStack linker, BlockPos pos) {
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		return player.gameMode.useItemOn(player, player.level(), linker, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	private static void openLinkedStorageMenu(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			StorageBlockEntity storage = WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Missing linked storage menu endpoint at " + pos));
			player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new StorageContainerMenu(windowId, menuPlayer, pos),
					storage.getMenuDisplayName()), buffer -> StorageContainerMenu.writeMenuData(buffer, player, pos));
			require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().filter(pos::equals).isPresent(),
					"Linked storage server menu did not open at " + pos);
			return true;
		});
	}

	private static void runLinkedSecondaryChestSplitMenuClosureRegression() {
		LinkedSecondaryChestSplitMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 9);
			BlockPos secondaryPos = primaryPos.east(3);
			BlockPos addedChestPos = secondaryPos.east();
			Vec3 originalPosition = player.position();
			clearArea(level, primaryPos);
			placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			placeChest(level, player, secondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity primary = getChest(level, primaryPos);
			ChestBlockEntity secondary = getChest(level, secondaryPos);
			ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked secondary chest for split menu closure regression");
			String groupName = "Secondary Split Transfer";
			primary.setCustomName(Component.literal(groupName));
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			synchronizeClientChunk(player, primaryPos);
			placeChest(level, player, addedChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
			ChestBlockEntity secondaryMain = List.of(getChest(level, secondaryPos), getChest(level, addedChestPos)).stream()
					.filter(ChestBlockEntity::isMainChest).findFirst()
					.orElseThrow(() -> new IllegalStateException("Missing linked secondary main chest before split menu closure regression"));
			return new LinkedSecondaryChestSplitMenuFixture(primaryPos, secondaryPos, addedChestPos, secondaryMain.getBlockPos(),
					primary.getStorageWrapper().getInventoryHandler().size(), requireEndpoint(primary, "linked secondary split primary").groupId(), groupName,
					originalPosition);
		});
		try {
			openLinkedSecondaryChestSplitMenu(fixture, false);
			BlockPos remainingPos = AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
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
			waitForClientStorageMenuClosed();

			BlockPos reopenedMainPos = AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				BlockPos removedPos = remainingPos.equals(fixture.secondaryPos()) ? fixture.addedChestPos() : fixture.secondaryPos();
				placeChest(level, player, removedPos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
				return List.of(getChest(level, remainingPos), getChest(level, removedPos)).stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing linked secondary main chest before settings split transfer")).getBlockPos();
			});
			openLinkedSecondaryChestSplitMenu(fixture.withMainPos(reopenedMainPos), true);
			AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.level();
				if (!(player.containerMenu instanceof StorageSettingsContainerMenu staleMenu)) {
					throw new IllegalStateException("Linked secondary settings menu closed before main-half split transfer");
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
				return true;
			});
			waitForClientStorageMenuClosed();
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.teleportTo(player.level(), fixture.originalPosition().x, fixture.originalPosition().y, fixture.originalPosition().z, Set.of(),
						player.getYRot(), player.getXRot(), false);
				clearArea(player.level(), fixture.primaryPos());
				return true;
			});
		}
	}

	private static void openLinkedStorageChestMenu(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			ItemStack emptyHand = ItemStack.EMPTY;
			player.setItemInHand(InteractionHand.MAIN_HAND, emptyHand);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
			require(player.gameMode.useItemOn(player, player.level(), emptyHand, InteractionHand.MAIN_HAND, hit).consumesAction(),
					"Could not open the linked chest through its normal in-world interaction");
			require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().filter(pos::equals).isPresent(),
					"Linked chest did not open its ordinary storage menu");
			return true;
		});
	}

	private static void openLinkedStorageSettingsMenu(BlockPos pos) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || !(minecraft.player.containerMenu instanceof StorageContainerMenu menu)
					|| menu.getBlockPosition().filter(pos::equals).isEmpty()) {
				throw new IllegalStateException("Linked storage inventory menu is missing before settings transition");
			}
			menu.openSettings();
			return true;
		});
	}

	private static void openLinkedSecondaryChestSplitMenu(LinkedSecondaryChestSplitMenuFixture fixture, boolean openSettings) {
		movePlayerNear(fixture.mainPos());
		waitForClientStorageBlockEntity(fixture.mainPos());
		openLinkedStorageChestMenu(fixture.mainPos());
		waitForClientLinkedStorageMenu(
				new LinkedStorageMenuFixture(fixture.mainPos(), fixture.mainPos(), fixture.groupId(), fixture.slots(), fixture.groupName()), fixture.mainPos(),
				LinkedStorageEndpointRole.SECONDARY);
		if (openSettings) {
			openLinkedStorageSettingsMenu(fixture.mainPos());
			waitForClientLinkedStorageSettingsMenu(fixture.mainPos(), fixture.slots(), LinkedStorageEndpointRole.SECONDARY);
		}
	}

	private static void sendOpenStorageInventoryPayload(BlockPos pos) {
		AutomationRuntime.runOnClient(() -> {
			ClientPacketDistributor.sendToServer(new OpenStorageInventoryPayload(pos));
			return true;
		});
	}

	private static void openLimitedBarrelMenu(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.level();
			if (!(level.getBlockState(pos).getBlock() instanceof LimitedBarrelBlock)) {
				throw new IllegalStateException("Expected a limited barrel before opening its regression menu at " + pos);
			}
			ItemStack emptyHand = ItemStack.EMPTY;
			player.setItemInHand(InteractionHand.MAIN_HAND, emptyHand);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
			require(player.gameMode.useItemOn(player, level, emptyHand, InteractionHand.MAIN_HAND, hit).consumesAction(),
					"Could not open the linked limited barrel through its normal in-world interaction");
			require(player.containerMenu instanceof LimitedBarrelContainerMenu, "Linked limited barrel did not open its dedicated server menu");
			return true;
		});
	}

	private static void movePlayerNear(BlockPos pos) {
		BlockPos playerPos = AutomationRuntime.runOnServer(player -> {
			BlockPos target = pos.south();
			player.teleportTo(player.level(), target.getCenter().x, target.getY(), target.getCenter().z, Set.of(), player.getYRot(), player.getXRot(), false);
			synchronizeClientChunk(player, pos);
			return target;
		});
		waitForClientPlayerPosition(playerPos);
	}

	private static void synchronizeClientChunk(ServerPlayer player, BlockPos pos) {
		var chunk = player.level().getChunkAt(pos);
		player.connection.send(chunk.getAuxLightManager(chunk.getPos())
				.sendLightDataTo(new ClientboundLevelChunkWithLightPacket(chunk, player.level().getLightEngine(), null, null)));
	}

	private static void waitForClientStorageBlockEntity(BlockPos pos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof StorageBlockEntity)) {
				return;
			}
			sleep(50);
		}
		String clientState = AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.level == null) {
				return "level=null";
			}
			return "player=" + (minecraft.player == null ? null : minecraft.player.blockPosition()) + ", loaded=" + minecraft.level.isLoaded(pos) + ", state="
					+ minecraft.level.getBlockState(pos) + ", blockEntity=" + minecraft.level.getBlockEntity(pos);
		});
		throw new IllegalStateException("Storage block entity did not synchronize to the client at " + pos + ": " + clientState);
	}

	private static void waitForClientStorageMenuClosed() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
					|| !(screen.getMenu() instanceof StorageContainerMenu) && !(screen.getMenu() instanceof StorageSettingsContainerMenu))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Canonical root replacement did not close the stale client storage menu");
	}

	private static void waitForClientLinkedStorageSettingsMenu(BlockPos expectedPos, int expectedSlots, LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof StorageSettingsContainerMenu menu && menu.getBlockPosition().equals(expectedPos)
					&& menu.getStorageWrapper().getInventoryHandler().size() == expectedSlots
					&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
					&& endpointProvider.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked storage settings menu did not open at " + expectedPos + " with the expected " + expectedRole + " endpoint");
	}

	private static void waitForClientLinkedMemory(BlockPos expectedPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof StorageContainerMenu menu && menu.getBlockPosition().filter(expectedPos::equals).isPresent()
					&& menu.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotFilterStack(0, false)
							.filter(stack -> stack.is(Items.DIAMOND)).isPresent())) {
				return;
			}
			sleep(50);
		}
		String clientState = AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof StorageContainerMenu menu)) {
				return "screen=" + minecraft.screen;
			}
			LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
			return "endpoint=" + endpoint + ", revision=" + (endpoint == null ? null : ClientLinkedStorageContents.getRevision(endpoint.groupId()))
					+ ", memory=" + menu.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotFilterStack(0, false);
		});
		throw new IllegalStateException("Linked limited-barrel memory settings did not synchronize to the open client menu: " + clientState);
	}

	private static void waitForClientLinkedStorageTooltip(LinkedStorageStackTooltipFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null || minecraft.level == null) {
					return false;
				}
				ItemStack stack = minecraft.player.getInventory().getItem(fixture.inventorySlot());
				if (!(StorageItemClient.getTooltipImage(stack) instanceof LinkedStorageTooltip tooltip) || tooltip.role() != LinkedStorageEndpointRole.PRIMARY
						|| !tooltip.groupId().equals(fixture.groupId())) {
					return false;
				}
				return StackStorageWrapper.fromStack(minecraft.level.registryAccess(), stack).getContentsUuid().filter(fixture.groupId()::equals).isPresent();
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Dropped linked stack did not expose its primary role, group, and canonical tooltip path");
	}

	private static void waitForClientLinkedStorageTooltipContents(LinkedStorageStackTooltipFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null || minecraft.level == null || ClientLinkedStorageContents.getRevision(fixture.groupId()).isEmpty()) {
					return false;
				}
				StackStorageWrapper wrapper = StackStorageWrapper.fromStack(minecraft.level.registryAccess(),
						minecraft.player.getInventory().getItem(fixture.inventorySlot()));
				return count(wrapper.getInventoryHandler(), Items.DIAMOND) == 7;
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Dropped linked stack tooltip did not receive the canonical contents snapshot");
	}

	private static DroppedItemPickupFixture setupDroppedItemPickupFixture(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos primaryPos = player.blockPosition().offset(0, 0, 12);
		BlockPos secondaryPos = primaryPos.east(12);
		Vec3 originalPosition = player.position();
		clearArea(level, primaryPos);
		clearArea(level, secondaryPos);
		player.teleportTo(level, primaryPos.getCenter().x, primaryPos.getY(), primaryPos.getCenter().z - 10, Set.of(), player.getYRot(), player.getXRot(),
				true);
		placeBlock(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		placeBlock(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()));
		BarrelBlockEntity primary = getBarrel(level, primaryPos);
		BarrelBlockEntity secondary = getBarrel(level, secondaryPos);
		ItemStack linker = new ItemStack(ENDER_LINKER.get(), 2);
		require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
				&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not create linked storage for dropped-item pickup regression");
		configureMagnet(primary);
		require(primary.getBlockState().getValue(StorageBlockBase.TICKING)
				&& primary.getStorageWrapper().getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent(),
				"Linked primary endpoint did not become the canonical ticking pickup endpoint");
		require(!secondary.getBlockState().getValue(StorageBlockBase.TICKING)
				&& secondary.getStorageWrapper().getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.SECONDARY::equals).isPresent(),
				"Linked secondary endpoint incorrectly retained global pickup ticking behavior");
		Vec3 dropPosition = secondaryPos.getCenter().add(0, -0.25D, 0);
		ItemEntity droppedItem = new ItemEntity(level, dropPosition.x, dropPosition.y, dropPosition.z, new ItemStack(Items.ENDER_PEARL, 2));
		droppedItem.setDeltaMovement(Vec3.ZERO);
		droppedItem.setNoGravity(true);
		droppedItem.setDefaultPickUpDelay();
		require(level.addFreshEntity(droppedItem), "Could not spawn the linked secondary pickup regression item");
		return new DroppedItemPickupFixture(primaryPos, secondaryPos, Items.ENDER_PEARL, level.getGameTime() + 20, originalPosition);
	}

	private static void configureMagnet(BarrelBlockEntity primary) {
		UpgradeHandler upgrades = primary.getStorageWrapper().getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(MAGNET_UPGRADE.get()));
		List<MagnetUpgradeWrapper> magnets = upgrades.getWrappersThatImplement(MagnetUpgradeWrapper.class);
		require(magnets.size() == 1, "Linked primary did not create exactly one Magnet upgrade wrapper");
		magnets.getFirst().setPickupItems(true);
		upgrades.saveInventory();
	}

	private static Optional<ItemEntity> findDroppedItem(ServerLevel level, DroppedItemPickupFixture fixture, BlockPos center) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(5),
				itemEntity -> itemEntity.isAlive() && itemEntity.getItem().is(fixture.item())).stream().findFirst();
	}

	private static void waitForServerCondition(String description, Function<ServerPlayer, Boolean> condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(condition)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static int count(InventoryHandler handler, Item item) {
		int count = 0;
		for (int slot = 0; slot < handler.size(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
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
					if (y == -1) {
						level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
						continue;
					}
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
						storage.clearContent();
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(12.0D)).forEach(ItemEntity::discard);
	}

	private record LinkedStorageMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, int inventorySlots, String title, Vec3 originalPosition,
			float originalYRot, float originalXRot) {
		private LinkedStorageMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, int inventorySlots, String title) {
			this(primaryPos, secondaryPos, groupId, inventorySlots, title, Vec3.ZERO, 0, 0);
		}
	}

	private record LinkedPrimaryChestExpansionFixture(BlockPos primaryPos, BlockPos addedChestPos, UUID groupId, String title) {
	}

	private record LinkedSecondaryChestSplitMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, BlockPos addedChestPos, BlockPos mainPos, int slots,
			UUID groupId, String groupName, Vec3 originalPosition) {
		private LinkedSecondaryChestSplitMenuFixture withMainPos(BlockPos newMainPos) {
			return new LinkedSecondaryChestSplitMenuFixture(primaryPos, secondaryPos, addedChestPos, newMainPos, slots, groupId, groupName, originalPosition);
		}
	}

	private record LinkedLimitedBarrelFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, ItemStack originalMainHand) {
	}

	private record LinkedStorageStackTooltipFixture(BlockPos primaryPos, UUID groupId, int inventorySlot, ItemStack originalStack, GameType originalGameMode) {
	}

	private record DroppedItemPickupFixture(BlockPos primaryPos, BlockPos secondaryPos, Item item, long verifyAfterGameTime, Vec3 originalPosition) {
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
}
