package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.*;
import net.p3pp3rf1y.sophisticatedstorage.network.OpenStorageInventoryMessage;
import net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;

/** Covers controller indexing when linked storage endpoints join or restore as a double chest. */
public final class StorageLinkedStorageRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);
	private static final int LINKED_LIMITED_RELOAD_ITEM_COUNT = 23;

	private StorageLinkedStorageRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, StorageLinkedStorageRegression::run);
	}

	public static void handleLinkedLimitedReloadSetup(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, StorageLinkedStorageRegression::setupLinkedLimitedBarrelReloadProjection);
	}

	public static void handleLinkedLimitedReloadStatus(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		sendJsonHandling(exchange, LOGGER, () -> {
			JsonObject request = JsonParser.parseString(body).getAsJsonObject();
			return linkedLimitedBarrelReloadProjectionStatus(UUID.fromString(request.get("groupId").getAsString()),
					new BlockPos(request.get("primaryX").getAsInt(), request.get("primaryY").getAsInt(), request.get("primaryZ").getAsInt()));
		});
	}

	private static String run() {
		AutomationRuntime.runOnServer(player -> {
			moveToSafeFixtureHeight(player);
			return true;
		});
		runOrdinaryLinkedStorageRegression();
		runCanonicalStorageTypeInsertionRulesRegression();
		runLinkedControllerEndpointJoinDoesNotDuplicateCanonicalContentsRegression();
		runPlacedLinkedDoubleChestControllerRegression();
		runLinkedDoubleChestLifecycleRegression();
		runLinkedStorageMenuSnapshotRegression();
		runLinkedStorageMenuTransitionRegression();
		runLinkedLimitedBarrelMemorySyncRegression();
		runLinkedStorageStackTooltipRegression();
		runSecondaryDroppedPickupRegression();
		runLinkedPrimaryChestExpansionMenuRegression();
		runLinkedSecondaryChestSplitMenuClosureRegression();
		return "{\"ok\":true,\"ordinaryBarrelLinkingSharesCanonicalContents\":true,\"canonicalStorageTypeControlsInsertionRules\":true,"
				+ "\"linkedControllerEndpointJoinKeepsOneCanonicalContentIndex\":true," + "\"placedLinkedDoubleChestKeepsOneCanonicalContentIndex\":true,"
				+ "\"linkedControllerRoutesAndFansOutToolOperations\":true," + "\"controllerRestorationReconnectsLinkedStorage\":true,"
				+ "\"linkedPrimaryDoubleChestRestoresAsPaired\":true," + "\"linkedSecondaryDoubleChestSplitsToSingleEndpoint\":true,"
				+ "\"linkedStorageMenuReceivesCanonicalSnapshot\":true," + "\"linkedMenusExposeCanonicalTitleAndEndpointRoles\":true,"
				+ "\"linkedLimitedBarrelMemorySyncsToOpenMenu\":true," + "\"droppedEndpointTooltipUsesCanonicalClientCache\":true,"
				+ "\"secondarySuppressesMagnetPickupUntilItemReachesPrimary\":true," + "\"linkedPrimaryExpansionRetainsContentsAndReopens54Slots\":true,"
				+ "\"rootReplacementClosesStaleMenu\":true," + "\"linkedStorageMenuTransitionsRejectStaleActions\":true}";
	}

	private static String setupLinkedLimitedBarrelReloadProjection() {
		return AutomationRuntime.runOnServer(player -> {
			moveToSafeFixtureHeight(player);
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 4);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()), 0);
			placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()), 0);
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
			ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked limited-barrel reload fixture");
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

	private static String linkedLimitedBarrelReloadProjectionStatus(UUID groupId, BlockPos primaryPos) {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
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
			return null;
		});
		waitForClientLimitedBarrelReloadProjection(primaryPos);
		return "{\"ok\":true,\"clientDisplayItems\":true,\"clientCounts\":true,\"clientFillLevels\":true}";
	}

	private static void runOrdinaryLinkedStorageRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 64);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
				StorageBlockEntity primary = getStorage(level, primaryPos, "ordinary linked primary");
				StorageBlockEntity secondary = getStorage(level, secondaryPos, "ordinary linked secondary");
				require(primary.isLinkedStorageCandidate() && secondary.isLinkedStorageCandidate(), "Ordinary barrels were not linked-storage candidates");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not link ordinary barrels");
				IItemHandler secondaryCapability = secondary.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
						.orElseThrow(() -> new IllegalStateException("Linked secondary barrel did not expose an item capability"));
				ItemStack remainder = secondaryCapability.insertItem(1, new ItemStack(Items.EMERALD, 3), false);
				require(remainder.isEmpty() && count(primary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
						&& count(secondary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
						&& count(primary.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3,
						"Linked barrels did not expose one canonical inventory through the secondary capability");
				return null;
			} finally {
				clearArea(level, primaryPos);
			}
		});
	}

	private static void runCanonicalStorageTypeInsertionRulesRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 68);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			try {
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()), 0);
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
				StorageBlockEntity shulkerPrimary = getAnyStorage(level, primaryPos, "canonical shulker primary");
				StorageBlockEntity barrelSecondary = getStorage(level, secondaryPos, "physical barrel secondary");
				ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, shulkerPrimary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, barrelSecondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked storage with a canonical shulker and physical barrel secondary");
				IItemHandler barrelCapability = barrelSecondary.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
						.orElseThrow(() -> new IllegalStateException("Linked barrel secondary did not expose an item capability"));
				ItemStack rejected = barrelCapability.insertItem(0, new ItemStack(Items.SHULKER_BOX), false);
				require(rejected.is(Items.SHULKER_BOX) && rejected.getCount() == 1
						&& count(shulkerPrimary.getStorageWrapper().getInventoryHandler(), Items.SHULKER_BOX) == 0
						&& count(barrelCapability, Items.SHULKER_BOX) == 0, "Physical barrel secondary bypassed canonical shulker insertion rules");

				clearArea(level, primaryPos);
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()), 0);
				StorageBlockEntity barrelPrimary = getStorage(level, primaryPos, "canonical barrel primary");
				StorageBlockEntity shulkerSecondary = getAnyStorage(level, secondaryPos, "physical shulker secondary");
				linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, barrelPrimary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, shulkerSecondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked storage with a canonical barrel and physical shulker secondary");
				IItemHandler shulkerCapability = shulkerSecondary.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
						.orElseThrow(() -> new IllegalStateException("Linked shulker secondary did not expose an item capability"));
				ItemStack remainder = shulkerCapability.insertItem(0, new ItemStack(Items.SHULKER_BOX), false);
				require(remainder.isEmpty() && count(barrelPrimary.getStorageWrapper().getInventoryHandler(), Items.SHULKER_BOX) == 1
						&& count(shulkerCapability, Items.SHULKER_BOX) == 1,
						"Physical shulker secondary did not apply canonical barrel insertion rules exactly once");
				return null;
			} finally {
				clearArea(level, primaryPos);
			}
		});
	}

	private static void runLinkedStorageMenuSnapshotRegression() {
		PlayerLocation originalPlayerLocation = capturePlayerLocation();
		BlockPos primaryPos = AutomationRuntime.runOnServer(player -> player.blockPosition().offset(0, 0, 100));
		try {
			LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
				ServerLevel level = player.serverLevel();
				BlockPos secondaryPos = primaryPos.east(3);
				clearArea(level, primaryPos);
				placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()), 0);
				placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()), 0);
				StorageBlockEntity primary = getStorage(level, primaryPos, "linked storage menu primary");
				StorageBlockEntity secondary = getStorage(level, secondaryPos, "linked storage menu secondary");
				String title = "Automation Linked Storage";
				primary.setCustomName(Component.literal(title));
				ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked storage menu fixture");
				primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				primary.getStorageWrapper().getInventoryHandler().saveInventory();
				return new LinkedStorageMenuFixture(primaryPos, secondaryPos, requireEndpoint(primary, "linked storage menu primary").groupId(),
						primary.getStorageWrapper().getInventoryHandler().getSlots(), title);
			});
			movePlayerNearLinkedStorage(fixture.secondaryPos());
			LinkedStorageMenuState serverMenuState = openLinkedStorageMenu(fixture.secondaryPos());
			assertLinkedStorageMenuCanonicalSnapshot(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY, serverMenuState);
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY, serverMenuState);
			openLinkedStorageMenu(fixture.primaryPos());
			waitForClientLinkedStorageMenu(fixture, fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				try {
					clearArea(player.serverLevel(), primaryPos);
				} finally {
					restorePlayerLocation(player, originalPlayerLocation);
				}
				return null;
			});
		}
	}

	private static LinkedStorageMenuState openLinkedStorageMenu(BlockPos pos) {
		return AutomationRuntime.runOnServer(player -> {
			StorageBlockEntity storage = player.serverLevel().getBlockEntity(pos) instanceof StorageBlockEntity storageBlockEntity ? storageBlockEntity : null;
			require(storage != null, "Missing linked storage menu endpoint at " + pos);
			NetworkHooks.openScreen(player, new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new StorageContainerMenu(windowId, menuPlayer, pos),
					storage.getMenuDisplayName()), buffer -> StorageContainerMenu.writeMenuData(buffer, player, pos));
			if (!(player.containerMenu instanceof StorageContainerMenu menu)) {
				throw new IllegalStateException("Linked storage server menu did not open at " + pos);
			}
			return getLinkedStorageMenuState(menu);
		});
	}

	private static void openLinkedStorageChestMenu(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			ItemStack emptyHand = ItemStack.EMPTY;
			player.setItemInHand(InteractionHand.MAIN_HAND, emptyHand);
			require(player.gameMode.useItemOn(player, player.serverLevel(), emptyHand, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false)).consumesAction(),
					"Could not open the linked chest through its normal in-world interaction");
			require(player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().filter(pos::equals).isPresent(),
					"Linked chest did not open its ordinary storage menu");
			return null;
		});
	}

	private static void openLinkedStorageSettingsMenu(BlockPos pos) {
		AutomationRuntime.runOnClient(() -> {
			if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenu menu)
					|| menu.getBlockPosition().filter(pos::equals).isEmpty()) {
				throw new IllegalStateException("Linked storage inventory menu is missing before settings transition");
			}
			menu.openSettings();
			return null;
		});
	}

	private static void runLinkedStorageMenuTransitionRegression() {
		PlayerLocation originalPlayerLocation = capturePlayerLocation();
		LinkedStorageMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 104);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.DIAMOND_BARREL_ITEM.get()), 0);
			placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
			StorageBlockEntity primary = getStorage(level, primaryPos, "linked storage transition primary");
			StorageBlockEntity secondary = getStorage(level, secondaryPos, "linked storage transition secondary");
			ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked storage menu transition fixture");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			String title = "Linked Storage Transition";
			primary.setCustomName(Component.literal(title));
			return new LinkedStorageMenuFixture(primaryPos, secondaryPos, requireEndpoint(primary, "linked storage transition primary").groupId(),
					primary.getStorageWrapper().getInventoryHandler().getSlots(), title);
		});
		try {
			AutomationRuntime.runOnClient(() -> {
				StoragePacketHandler.INSTANCE.sendToServer(new OpenStorageInventoryMessage(fixture.secondaryPos()));
				return null;
			});
			long noMenuVerificationTime = AutomationRuntime.runOnServer(player -> player.serverLevel().getGameTime() + 2L);
			waitForServerCondition("no-menu linked storage inventory message rejection",
					player -> player.serverLevel().getGameTime() >= noMenuVerificationTime && player.containerMenu == player.inventoryMenu);
			movePlayerNearLinkedStorage(fixture.secondaryPos());
			openLinkedStorageMenu(fixture.secondaryPos());
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnClient(() -> {
				StoragePacketHandler.INSTANCE.sendToServer(new OpenStorageInventoryMessage(fixture.secondaryPos()));
				return null;
			});
			long wrongMenuVerificationTime = AutomationRuntime.runOnServer(player -> player.serverLevel().getGameTime() + 2L);
			waitForServerCondition("wrong-menu linked storage inventory message rejection",
					player -> player.serverLevel().getGameTime() >= wrongMenuVerificationTime && player.containerMenu instanceof StorageContainerMenu);
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			openLinkedStorageSettingsMenu(fixture.secondaryPos());
			waitForClientLinkedStorageSettingsMenu(fixture.secondaryPos(), fixture.inventorySlots(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnClient(() -> {
				StoragePacketHandler.INSTANCE.sendToServer(new OpenStorageInventoryMessage(fixture.primaryPos()));
				return null;
			});
			long stalePayloadVerificationTime = AutomationRuntime.runOnServer(player -> player.serverLevel().getGameTime() + 2L);
			waitForServerCondition("mismatched linked storage inventory message rejection",
					player -> player.serverLevel().getGameTime() >= stalePayloadVerificationTime
							&& player.containerMenu instanceof StorageSettingsContainerMenu menu && menu.getBlockPosition().equals(fixture.secondaryPos()));
			AutomationRuntime.runOnClient(() -> {
				StoragePacketHandler.INSTANCE.sendToServer(new OpenStorageInventoryMessage(fixture.secondaryPos()));
				return null;
			});
			waitForClientLinkedStorageMenu(fixture, fixture.secondaryPos(), LinkedStorageEndpointRole.SECONDARY);
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return null;
			});
			waitForServerCondition("linked storage opener cleanup after settings transition", player -> player.containerMenu == player.inventoryMenu
					&& !getStorage(player.serverLevel(), fixture.secondaryPos(), "transition secondary").isOpen());
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				try {
					clearArea(player.serverLevel(), fixture.primaryPos());
				} finally {
					restorePlayerLocation(player, originalPlayerLocation);
				}
				return null;
			});
		}
	}

	private static PlayerLocation capturePlayerLocation() {
		return AutomationRuntime.runOnServer(player -> new PlayerLocation(player.position(), player.getYRot(), player.getXRot()));
	}

	private static void movePlayerNearLinkedStorage(BlockPos pos) {
		AutomationRuntime.runOnServer(player -> {
			Vec3 target = Vec3.atCenterOf(pos).add(0, 1, 0);
			require(player.teleportTo(player.serverLevel(), target.x, target.y, target.z, Set.of(), player.getYRot(), player.getXRot()),
					"Could not move player into linked storage menu interaction range at " + pos);
			var chunk = player.serverLevel().getChunkAt(pos);
			player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, player.serverLevel().getLightEngine(), null, null));
			return null;
		});
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.player != null && minecraft.level != null && minecraft.player.position().distanceToSqr(Vec3.atCenterOf(pos)) < 4
						&& minecraft.level.getBlockEntity(pos) instanceof StorageBlockEntity;
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client did not synchronize the nearby linked storage fixture at " + pos);
	}

	private static void restorePlayerLocation(ServerPlayer player, PlayerLocation location) {
		require(player.teleportTo(player.serverLevel(), location.position().x, location.position().y, location.position().z, Set.of(), location.yRot(),
				location.xRot()), "Could not restore player location after linked storage menu regression");
	}

	private static void assertLinkedStorageMenuCanonicalSnapshot(LinkedStorageMenuFixture fixture, BlockPos expectedPos, LinkedStorageEndpointRole expectedRole,
			LinkedStorageMenuState state) {
		require(expectedPos.equals(state.position()) && fixture.title().equals(state.title()) && fixture.groupId().equals(state.groupId())
				&& fixture.inventorySlots() == state.slots() && expectedRole.name().equals(state.role()) && state.diamondCount() == 7,
				"Linked storage server menu did not expose the canonical snapshot: " + state);
	}

	private static LinkedStorageMenuState getLinkedStorageMenuState(StorageContainerMenu menu) {
		LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
		String role = menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
				? endpointProvider.getLinkedStorageEndpointRole().map(Enum::name).orElse("none")
				: "unlinked";
		return new LinkedStorageMenuState(menu.getBlockPosition().orElse(null), menu.getStorageBlockEntity().getMenuDisplayName().getString(),
				endpoint == null ? null : endpoint.groupId(), menu.getStorageWrapper().getInventoryHandler().getSlots(), role,
				count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND));
	}

	private static void waitForClientLinkedStorageMenu(LinkedStorageMenuFixture fixture, BlockPos expectedPos, LinkedStorageEndpointRole expectedRole) {
		waitForClientLinkedStorageMenu(fixture, expectedPos, expectedRole, null);
	}

	private static void waitForClientLinkedStorageMenu(LinkedStorageMenuFixture fixture, BlockPos expectedPos, LinkedStorageEndpointRole expectedRole,
			LinkedStorageMenuState serverMenuState) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof StorageContainerMenu menu)
						|| !screen.getTitle().getString().equals(fixture.title()) || menu.getBlockPosition().filter(expectedPos::equals).isEmpty()) {
					return false;
				}
				LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
				return endpoint != null && fixture.groupId().equals(endpoint.groupId())
						&& menu.getStorageWrapper().getInventoryHandler().getSlots() == fixture.inventorySlots()
						&& menu.getNumberOfStorageInventorySlots() == fixture.inventorySlots()
						&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
						&& endpointProvider.getLinkedStorageEndpoint().filter(wrapperEndpoint -> wrapperEndpoint.groupId().equals(fixture.groupId()))
								.isPresent()
						&& endpointProvider.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent()
						&& count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7;
			})) {
				return;
			}
			sleep(50);
		}
		String clientMenuState = AutomationRuntime.runOnClient(StorageLinkedStorageRegression::describeClientLinkedStorageMenu);
		throw new IllegalStateException("Linked storage menu did not receive the canonical snapshot, title, and " + expectedRole + " role at " + expectedPos
				+ "; server=" + serverMenuState + "; client=" + clientMenuState);
	}

	private static String describeClientLinkedStorageMenu() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
			return "screen=" + (minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName())
					+ ", position=none, title=none, group=none, slots=none, role=none, diamonds=none";
		}
		if (!(screen.getMenu() instanceof StorageContainerMenu menu)) {
			return "screen=" + screen.getClass().getSimpleName() + ", position=none, title='" + screen.getTitle().getString()
					+ "', group=none, slots=none, role=none, diamonds=none";
		}
		LinkedStorageEndpointData endpoint = menu.getStorageBlockEntity().getLinkedStorageEndpointData();
		String role = menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
				? endpointProvider.getLinkedStorageEndpointRole().map(Enum::name).orElse("none")
				: "unlinked";
		return "screen=" + screen.getClass().getSimpleName() + ", position=" + menu.getBlockPosition().map(Object::toString).orElse("none") + ", title='"
				+ screen.getTitle().getString() + "', group=" + (endpoint == null ? "none" : endpoint.groupId()) + ", slots="
				+ menu.getStorageWrapper().getInventoryHandler().getSlots() + ", role=" + role + ", diamonds="
				+ count(menu.getStorageWrapper().getInventoryHandler(), Items.DIAMOND);
	}

	private static void runLinkedControllerEndpointJoinDoesNotDuplicateCanonicalContentsRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			PlayerInventoryState originalInventoryState = capturePlayerInventoryState(player);
			int dyeSlot = originalInventoryState.selectedSlot() == 8 ? 7 : 8;
			ItemStack originalDyeSlot = player.getInventory().getItem(dyeSlot).copy();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 76);
			BlockPos existingEndpointPos = controllerPos.east();
			BlockPos joiningEndpointPos = existingEndpointPos.east();
			clearArea(level, controllerPos);
			try {
				placeBlockAsPlayer(level, player, existingEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
				placeBlockAsPlayer(level, player, joiningEndpointPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
				placeBlockAsPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()), 0);
				ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing controller for linked endpoint join regression"));
				StorageBlockEntity existingEndpoint = getStorage(level, existingEndpointPos, "existing controller endpoint");
				StorageBlockEntity joiningEndpoint = getStorage(level, joiningEndpointPos, "joining controller endpoint");
				int canonicalSlots = existingEndpoint.getStorageWrapper().getInventoryHandler().getSlots();
				require(controller.getStoragePositions().size() == 2 && controller.getSlots() == canonicalSlots * 2,
						"Controller did not register both unlinked barrels before linking");

				ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, existingEndpoint) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create controller-connected linked-storage group");
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, joiningEndpoint) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not add a controller-connected barrel to its existing linked-storage group");
				require(requireEndpoint(existingEndpoint, "existing controller endpoint").groupId()
						.equals(requireEndpoint(joiningEndpoint, "joining controller endpoint").groupId()),
						"Controller-connected endpoints did not join the same linked-storage group");
				existingEndpoint.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				existingEndpoint.getStorageWrapper().getInventoryHandler().saveInventory();

				ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
				require(controller.getStoragePositions().size() == 1 && controller.getSlots() == canonicalSlots && count(controller, Items.DIAMOND) == 7
						&& controller.getStackStorages(diamondKey).size() == 1,
						"Adding a controller-connected endpoint to an existing linked group duplicated canonical contents: positions="
								+ controller.getStoragePositions() + ", slots=" + controller.getSlots() + ", diamonds=" + count(controller, Items.DIAMOND)
								+ ", indexed=" + controller.getStackStorages(diamondKey));
				require(controller.insertItem(new ItemStack(Items.EMERALD, 3), false).isEmpty()
						&& count(joiningEndpoint.getStorageWrapper().getInventoryHandler(), Items.EMERALD) == 3,
						"Controller did not route items into the linked canonical inventory");
				ItemStackKey emeraldKey = ItemStackKey.of(new ItemStack(Items.EMERALD));
				Set<BlockPos> physicalEndpoints = Set.of(existingEndpointPos, joiningEndpointPos);
				require(controller.getStorageBlockPositions().equals(physicalEndpoints), "Controller did not expose both linked physical endpoint positions");
				require(controller.getHighlightStoragePositions(controller.getStackStorages(emeraldKey)).containsAll(physicalEndpoints),
						"Controller highlights did not expand to both linked physical endpoints");
				require(controller.getStorageTierUpgradePositions().equals(Set.of(existingEndpointPos)),
						"Controller tier candidates did not select only the linked primary endpoint");

				ItemStack paintbrush = new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModItems.PAINTBRUSH.get());
				int brushColor = 0xFFFF0000;
				int storedBrushColor = brushColor & 0x00FFFFFF;
				PaintbrushItem.setMainColor(paintbrush, brushColor);
				PaintbrushItem.setAccentColor(paintbrush, brushColor);
				PaintbrushItem.ItemRequirements requirements = PaintbrushItem.getItemRequirements(paintbrush, player, level, controllerPos)
						.orElseThrow(() -> new IllegalStateException("Linked controller paintbrush requirements were missing"));
				int requiredRedDyes = requirements.itemsPresent().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum()
						+ requirements.itemsMissing().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum();
				require(requiredRedDyes == 2, "Controller paintbrush requirements did not include both linked physical endpoints");
				player.getInventory().setItem(dyeSlot, new ItemStack(Items.RED_DYE, 2));
				player.getInventory().setChanged();
				player.setItemInHand(InteractionHand.MAIN_HAND, paintbrush);
				PaintbrushItem.ItemRequirements suppliedRequirements = PaintbrushItem.getItemRequirements(paintbrush, player, level, controllerPos)
						.orElseThrow(() -> new IllegalStateException("Linked controller paintbrush requirements disappeared after supplying dyes"));
				int suppliedRedDyes = suppliedRequirements.itemsPresent().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum();
				int missingRedDyes = suppliedRequirements.itemsMissing().stream().filter(stack -> stack.is(Items.RED_DYE)).mapToInt(ItemStack::getCount).sum();
				require(suppliedRedDyes == 2 && missingRedDyes == 0,
						"Controller paintbrush could not access supplied dyes: present=" + suppliedRedDyes + ", missing=" + missingRedDyes);
				int redDyesBeforePainting = countPlayerResource(player, Items.RED_DYE);
				PlayerLocation originalPlayerLocation = new PlayerLocation(player.position(), player.getYRot(), player.getXRot());
				InteractionResult paintResult;
				try {
					require(player.teleportTo(level, controllerPos.getX() + 0.5D, controllerPos.getY() + 1.0D, controllerPos.getZ() + 0.5D, Set.of(),
							player.getYRot(), player.getXRot()), "Could not move player near controller paintbrush fixture");
					paintResult = player.gameMode.useItemOn(player, level, paintbrush, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(controllerPos), Direction.UP, controllerPos, false));
				} finally {
					restorePlayerLocation(player, originalPlayerLocation);
				}
				int existingEndpointColor = existingEndpoint.getStorageWrapper().getMainColor();
				int joiningEndpointColor = joiningEndpoint.getStorageWrapper().getMainColor();
				int redDyesAfterPainting = countPlayerResource(player, Items.RED_DYE);
				require(paintResult == InteractionResult.SUCCESS && existingEndpointColor == storedBrushColor && joiningEndpointColor == storedBrushColor
						&& existingEndpoint.getStorageWrapper().getAccentColor() == storedBrushColor
						&& joiningEndpoint.getStorageWrapper().getAccentColor() == storedBrushColor,
						"Controller paintbrush did not color both linked physical endpoints: result=" + paintResult + ", expectedColor=" + storedBrushColor
								+ ", existingColor=" + existingEndpointColor + ", joiningColor=" + joiningEndpointColor + ", dyesBefore="
								+ redDyesBeforePainting + ", dyesAfter=" + redDyesAfterPainting);
				require(redDyesAfterPainting == redDyesBeforePainting - 2,
						"Controller paintbrush did not consume dye for both linked physical endpoints: before=" + redDyesBeforePainting + ", after="
								+ redDyesAfterPainting);

				controller.toggleLock();
				require(existingEndpoint.isLocked() && joiningEndpoint.isLocked(), "Controller lock did not fan out to linked endpoints");
				boolean initialLockVisibility = existingEndpoint.shouldShowLock();
				require(initialLockVisibility == joiningEndpoint.shouldShowLock(), "Linked endpoints started with different lock visibility");
				controller.toggleLockVisibility();
				require(existingEndpoint.shouldShowLock() == !initialLockVisibility && joiningEndpoint.shouldShowLock() == !initialLockVisibility,
						"Controller lock visibility did not fan out to linked endpoints");
				boolean initialTierVisibility = existingEndpoint.shouldShowTier();
				require(initialTierVisibility == joiningEndpoint.shouldShowTier(), "Linked endpoints started with different tier visibility");
				controller.toggleTierVisiblity();
				require(existingEndpoint.shouldShowTier() == !initialTierVisibility && joiningEndpoint.shouldShowTier() == !initialTierVisibility,
						"Controller tier visibility did not fan out to linked endpoints");
				boolean initialUpgradeVisibility = existingEndpoint.shouldShowUpgrades();
				require(initialUpgradeVisibility == joiningEndpoint.shouldShowUpgrades(), "Linked endpoints started with different upgrade visibility");
				controller.toggleUpgradesVisiblity();
				require(existingEndpoint.shouldShowUpgrades() == !initialUpgradeVisibility && joiningEndpoint.shouldShowUpgrades() == !initialUpgradeVisibility,
						"Controller upgrade visibility did not fan out to linked endpoints");
				controller.toggleLock();

				level.setBlock(controllerPos, Blocks.AIR.defaultBlockState(), 3);
				require(existingEndpoint.getControllerPos().isEmpty() && joiningEndpoint.getControllerPos().isEmpty(),
						"Removing the controller did not detach linked endpoints");
				placeBlockAsPlayer(level, player, controllerPos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()), 0);
				ControllerBlockEntity restoredController = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing restored controller"));
				restoredController.searchAndAddBoundables();
				require(existingEndpoint.getControllerPos().filter(controllerPos::equals).isPresent()
						&& joiningEndpoint.getControllerPos().filter(controllerPos::equals).isPresent() && restoredController.getStoragePositions().size() == 1
						&& restoredController.getStackStorages(diamondKey).size() == 1,
						"Restored controller did not reconnect the linked canonical storage index");
				return null;
			} finally {
				try {
					player.getInventory().setItem(dyeSlot, originalDyeSlot);
					restorePlayerInventoryState(player, originalInventoryState);
				} finally {
					clearArea(level, controllerPos);
				}
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
				placeChest(level, player, sourceLeftPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				placeChest(level, player, sourceRightPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				ChestBlockEntity sourceMainChest = getChest(level, sourceRightPos);
				require(sourceMainChest.isMainChest(), "Could not create the source linked double chest");
				ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get());
				require(useLinkerAsPlayer(player, linker, sourceMainChest.getBlockPos()).consumesAction(), "Could not create the linked double-chest group");
				LinkedStorageEndpointData endpoint = requireEndpoint(sourceMainChest, "source linked double chest");
				player.setGameMode(GameType.SURVIVAL);
				require(player.gameMode.destroyBlock(sourceLeftPos), "Player break did not remove the linked source double chest");
				player.setGameMode(originalGameMode);
				ItemStack linkedDoubleChestCarrier = level
						.getEntitiesOfClass(ItemEntity.class, new AABB(sourceLeftPos).inflate(2.0D),
								itemEntity -> itemEntity.getItem().is(ModBlocks.CHEST_ITEM.get())
										&& endpoint.equals(LinkedStorageStackData.getEndpoint(itemEntity.getItem())))
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
				player.setShiftKeyDown(true);
				try {
					placeChest(level, player, upperChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				} finally {
					player.setShiftKeyDown(false);
				}
				ChestBlockEntity upperChest = getChest(level, upperChestPos);
				require(useLinkerAsPlayer(player, linker, upperChest.getBlockPos()).consumesAction()
						&& endpoint.groupId().equals(requireEndpoint(upperChest, "upper linked chest").groupId()),
						"Could not add the upper chest to the placed double chest's linked group");
				placedMainChest.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				placedMainChest.getStorageWrapper().getInventoryHandler().saveInventory();
				ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));

				require(controller.getStoragePositions().size() == 1 && controller.getSlots() == 54 && count(controller, Items.DIAMOND) == 7
						&& controller.getStackStorages(diamondKey).size() == 1,
						"Placing a linked double chest next to a controller duplicated canonical contents after linking the upper chest: positions="
								+ controller.getStoragePositions() + ", slots=" + controller.getSlots() + ", diamonds=" + count(controller, Items.DIAMOND)
								+ ", indexed=" + controller.getStackStorages(diamondKey));
				ChestBlockEntity placedOtherChest = getChest(level,
						placedMainChest.getBlockPos().relative(ChestBlock.getConnectedDirection(placedMainChest.getBlockState())));
				BlockPos ordinaryStoragePos = controllerPos.south();
				placeBlockAsPlayer(level, player, ordinaryStoragePos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
				StorageBlockEntity ordinaryStorage = getStorage(level, ordinaryStoragePos, "ordinary controller storage");
				require(ordinaryStorage.getControllerPos().filter(controllerPos::equals).isPresent(),
						"Controller did not connect the ordinary storage beside the linked double chest");
				controller.toggleLock();
				require(placedMainChest.isLocked() && placedOtherChest.isLocked() && ordinaryStorage.isLocked(),
						"One controller lock toggle did not lock both double-chest halves and the ordinary connected storage");
				controller.toggleLock();
				require(!placedMainChest.isLocked() && !placedOtherChest.isLocked() && !ordinaryStorage.isLocked(),
						"One controller unlock toggle did not unlock both double-chest halves and the ordinary connected storage");
				return null;
			} finally {
				player.setGameMode(originalGameMode);
				clearArea(level, controllerPos);
			}
		});
	}

	private static void runLinkedDoubleChestLifecycleRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			GameType originalGameMode = player.gameMode.getGameModeForPlayer();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 112);
			BlockPos addedPrimaryPos = primaryPos.east();
			BlockPos secondaryPos = primaryPos.east(5);
			BlockPos addedSecondaryPos = secondaryPos.west();
			clearArea(level, primaryPos);
			try {
				player.setGameMode(GameType.CREATIVE);
				placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				ChestBlockEntity primary = getChest(level, primaryPos);
				ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 4);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked primary chest for lifecycle regression");
				LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "single linked primary chest");
				placeChest(level, player, addedPrimaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				List<ChestBlockEntity> primaryParts = List.of(getChest(level, primaryPos), getChest(level, addedPrimaryPos));
				ChestBlockEntity expandedPrimary = primaryParts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after expanding linked primary"));
				require(primaryParts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
						&& primaryEndpoint.equals(requireEndpoint(expandedPrimary, "expanded linked primary chest"))
						&& expandedPrimary.getStorageWrapper().getInventoryHandler().getSlots() == 54,
						"Adding a chest did not expand the linked primary into a double chest");

				player.setGameMode(GameType.SURVIVAL);
				require(player.gameMode.destroyBlock(expandedPrimary.getBlockPos()), "Player break did not remove the linked primary double chest");
				List<ItemEntity> primaryDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(primaryPos).inflate(1.5D),
						itemEntity -> itemEntity.getItem().is(ModBlocks.CHEST_ITEM.get()));
				require(level.getBlockState(primaryPos).isAir() && level.getBlockState(addedPrimaryPos).isAir() && primaryDrops.size() == 1
						&& ChestBlockItem.isDoubleChest(primaryDrops.get(0).getItem())
						&& primaryEndpoint.equals(LinkedStorageStackData.getEndpoint(primaryDrops.get(0).getItem())) && StorageBlockEntity
								.getLinkedStorageEndpointRole(primaryDrops.get(0).getItem()).filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent(),
						"Breaking a linked primary double chest did not drop one paired endpoint carrier");
				ItemStack carrier = primaryDrops.get(0).getItem().copy();
				primaryDrops.forEach(ItemEntity::discard);
				placeChest(level, player, primaryPos, carrier, 0);
				ChestBlockEntity restoredPart = getChest(level, primaryPos);
				BlockPos restoredOtherPos = primaryPos.relative(ChestBlock.getConnectedDirection(restoredPart.getBlockState()));
				ChestBlockEntity restoredPrimary = restoredPart.isMainChest() ? restoredPart : getChest(level, restoredOtherPos);
				require(level.getBlockState(restoredOtherPos).getValue(ChestBlock.TYPE) != ChestType.SINGLE
						&& primaryEndpoint.equals(requireEndpoint(restoredPrimary, "restored linked primary chest"))
						&& restoredPrimary.getStorageWrapper().getInventoryHandler().getSlots() == 54,
						"Linked primary double chest carrier did not restore as a paired endpoint");

				clearArea(level, primaryPos);
				player.setGameMode(GameType.CREATIVE);
				placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				placeChest(level, player, secondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				ChestBlockEntity linkedPrimary = getChest(level, primaryPos);
				ChestBlockEntity secondary = getChest(level, secondaryPos);
				ItemStack secondaryLinker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 4);
				require(LinkedStorageService.linkWithResult(level, player.getUUID(), secondaryLinker, linkedPrimary) == LinkedStorageService.LinkResult.SUCCESS
						&& LinkedStorageService.linkWithResult(level, player.getUUID(), secondaryLinker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create linked secondary chest for split regression");
				LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "linked secondary chest");
				List<ItemStack> canonicalContents = fillInventoryWithTestContents(linkedPrimary.getStorageWrapper().getInventoryHandler());
				linkedPrimary.getStorageWrapper().getInventoryHandler().saveInventory();
				placeChest(level, player, addedSecondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				ChestBlockEntity secondaryMain = getChest(level, secondaryPos).isMainChest()
						? getChest(level, secondaryPos)
						: getChest(level, addedSecondaryPos);
				require(secondaryMain.getBlockPos().equals(secondaryPos), "Original linked secondary chest did not become the double-chest main half");
				BlockPos remainingSecondaryPos = secondaryMain.getBlockPos().equals(secondaryPos) ? addedSecondaryPos : secondaryPos;
				PlayerInventoryState originalInventoryState = capturePlayerInventoryState(player);
				ItemStack tierUpgrade = new ItemStack(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get());
				InteractionResult tierUpgradeResult;
				try {
					player.setItemInHand(InteractionHand.MAIN_HAND, tierUpgrade);
					tierUpgradeResult = player.gameMode.useItemOn(player, level, tierUpgrade, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(remainingSecondaryPos), Direction.UP, remainingSecondaryPos, false));
				} finally {
					restorePlayerInventoryState(player, originalInventoryState);
				}
				require(tierUpgradeResult == InteractionResult.FAIL && tierUpgrade.getCount() == 1
						&& level.getBlockState(secondaryMain.getBlockPos()).is(ModBlocks.CHEST.get())
						&& level.getBlockState(remainingSecondaryPos).is(ModBlocks.CHEST.get())
						&& secondaryEndpoint.equals(requireEndpoint(secondaryMain, "linked secondary double-chest main after rejected tier upgrade")),
						"Tier upgrade on a linked secondary double-chest non-main half "
								+ "was not rejected without changing the main endpoint");
				player.setGameMode(GameType.SURVIVAL);
				require(player.gameMode.destroyBlock(secondaryMain.getBlockPos()), "Player break did not remove the linked secondary double chest half");
				List<ItemEntity> secondaryDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(secondaryMain.getBlockPos()).inflate(1.5D));
				ItemStack secondaryDrop = secondaryDrops.size() == 1 ? secondaryDrops.get(0).getItem().copy() : ItemStack.EMPTY;
				ChestBlockEntity remainingSecondary = getChest(level, remainingSecondaryPos);
				require(remainingSecondary.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& secondaryEndpoint.equals(requireEndpoint(remainingSecondary, "remaining linked secondary chest"))
						&& !remainingSecondary.hasCustomName()
						&& hasInventoryContents(linkedPrimary.getStorageWrapper().getInventoryHandler(), canonicalContents) && secondaryDrops.size() == 1
						&& !ChestBlockItem.isDoubleChest(secondaryDrop) && LinkedStorageStackData.getEndpoint(secondaryDrop) == null
						&& !secondaryDrop.hasCustomHoverName(), "Breaking a linked secondary main chest changed or dropped linked inventory contents");
				secondaryDrops.forEach(ItemEntity::discard);

				player.setGameMode(GameType.CREATIVE);
				BlockPos shiftedAddedSecondaryPos = remainingSecondaryPos.west();
				placeChest(level, player, shiftedAddedSecondaryPos, secondaryDrop, 0);
				ChestBlockEntity shiftedSecondaryPart = getChest(level, shiftedAddedSecondaryPos);
				ChestBlockEntity shiftedSecondaryMain = shiftedSecondaryPart.isMainChest()
						? shiftedSecondaryPart
						: getChest(level, shiftedSecondaryPart.getBlockPos().relative(ChestBlock.getConnectedDirection(shiftedSecondaryPart.getBlockState())));
				require(shiftedSecondaryMain.getBlockPos().equals(remainingSecondaryPos)
						&& secondaryEndpoint.equals(requireEndpoint(shiftedSecondaryMain, "shifted linked secondary chest"))
						&& !shiftedSecondaryMain.hasCustomName(), "Shifting the linked secondary double chest did not preserve an unnamed endpoint");

				player.setGameMode(GameType.SURVIVAL);
				require(player.gameMode.destroyBlock(remainingSecondaryPos), "Player break did not remove the shifted linked secondary main chest");
				List<ItemEntity> shiftedDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(remainingSecondaryPos).inflate(1.5D));
				ItemStack shiftedDrop = shiftedDrops.size() == 1 ? shiftedDrops.get(0).getItem().copy() : ItemStack.EMPTY;
				ChestBlockEntity shiftedRemainingSecondary = getChest(level, shiftedAddedSecondaryPos);
				require(shiftedRemainingSecondary.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& secondaryEndpoint.equals(requireEndpoint(shiftedRemainingSecondary, "remaining shifted linked secondary chest"))
						&& !shiftedRemainingSecondary.hasCustomName()
						&& hasInventoryContents(linkedPrimary.getStorageWrapper().getInventoryHandler(), canonicalContents) && shiftedDrops.size() == 1
						&& !ChestBlockItem.isDoubleChest(shiftedDrop) && LinkedStorageStackData.getEndpoint(shiftedDrop) == null
						&& !shiftedDrop.hasCustomHoverName(), "Breaking the shifted linked secondary main chest changed or dropped linked inventory contents");
				shiftedDrops.forEach(ItemEntity::discard);

				player.setGameMode(GameType.CREATIVE);
				placeChest(level, player, remainingSecondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				ChestBlockEntity restoredSecondaryPart = getChest(level, remainingSecondaryPos);
				ChestBlockEntity restoredSecondaryMain = restoredSecondaryPart.isMainChest()
						? restoredSecondaryPart
						: getChest(level,
								restoredSecondaryPart.getBlockPos().relative(ChestBlock.getConnectedDirection(restoredSecondaryPart.getBlockState())));
				BlockPos nonMainSecondaryPos = restoredSecondaryMain.getBlockPos().equals(remainingSecondaryPos)
						? shiftedAddedSecondaryPos
						: remainingSecondaryPos;
				player.setGameMode(GameType.SURVIVAL);
				require(player.gameMode.destroyBlock(nonMainSecondaryPos), "Player break did not remove the linked secondary non-main chest half");
				List<ItemEntity> nonMainDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(nonMainSecondaryPos).inflate(1.5D));
				require(restoredSecondaryMain.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE
						&& secondaryEndpoint.equals(requireEndpoint(restoredSecondaryMain, "remaining linked secondary chest after non-main break"))
						&& hasInventoryContents(linkedPrimary.getStorageWrapper().getInventoryHandler(), canonicalContents) && nonMainDrops.size() == 1
						&& !ChestBlockItem.isDoubleChest(nonMainDrops.get(0).getItem())
						&& LinkedStorageStackData.getEndpoint(nonMainDrops.get(0).getItem()) == null,
						"Breaking a linked secondary non-main chest changed or dropped linked inventory contents");
				return null;
			} finally {
				player.setGameMode(originalGameMode);
				clearArea(level, primaryPos);
			}
		});
	}

	private static void runLinkedLimitedBarrelMemorySyncRegression() {
		PlayerLocation originalPlayerLocation = capturePlayerLocation();
		PlayerInventoryState originalInventoryState = AutomationRuntime.runOnServer(StorageLinkedStorageRegression::capturePlayerInventoryState);
		BlockPos primaryPos = AutomationRuntime.runOnServer(player -> player.blockPosition().offset(0, 0, 124));
		BlockPos secondaryPos = primaryPos.east(2);
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			clearArea(level, primaryPos);
			placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get()), 0);
			placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()), 0);
			LimitedBarrelBlockEntity primary = getLimitedBarrel(level, primaryPos);
			LimitedBarrelBlockEntity secondary = getLimitedBarrel(level, secondaryPos);
			ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked limited barrels for memory synchronization regression");
			primary.toggleLock();
			return null;
		});
		try {
			movePlayerNearLinkedStorage(secondaryPos);
			openLinkedStorageMenu(secondaryPos);
			AutomationRuntime.runOnServer(player -> {
				LimitedBarrelBlockEntity primary = getLimitedBarrel(player.serverLevel(), primaryPos);
				LimitedBarrelBlockEntity secondary = getLimitedBarrel(player.serverLevel(), secondaryPos);
				ItemStack diamonds = new ItemStack(Items.DIAMOND, 7);
				player.setItemInHand(InteractionHand.MAIN_HAND, diamonds);
				require(primary.depositItem(player, InteractionHand.MAIN_HAND, diamonds, 0),
						"Locked linked limited barrel did not accept the memorized-slot deposit");
				MemorySettingsCategory primaryMemory = primary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				MemorySettingsCategory secondaryMemory = secondary.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				require(primaryMemory.getSlotFilterStack(0, false).filter(stack -> stack.is(Items.DIAMOND)).isPresent()
						&& secondaryMemory.getSlotFilterStack(0, false).filter(stack -> stack.is(Items.DIAMOND)).isPresent(),
						"Linked limited-barrel memory did not update through both canonical wrappers");
				return null;
			});
			waitForClientLinkedMemory(secondaryPos);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				try {
					clearArea(player.serverLevel(), primaryPos);
				} finally {
					try {
						restorePlayerLocation(player, originalPlayerLocation);
					} finally {
						restorePlayerInventoryState(player, originalInventoryState);
					}
				}
				return null;
			});
		}
	}

	private static void runLinkedPrimaryChestExpansionMenuRegression() {
		PlayerLocation originalPlayerLocation = capturePlayerLocation();
		LinkedPrimaryChestExpansionFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 136);
			BlockPos addedChestPos = primaryPos.west();
			clearArea(level, primaryPos);
			placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
			ChestBlockEntity primary = getChest(level, primaryPos);
			String title = "Expanded Linked Primary";
			primary.setCustomName(Component.literal(title));
			require(LinkedStorageService.linkWithResult(level, player.getUUID(),
					new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2), primary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked primary chest for menu expansion regression");
			primary.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			primary.getStorageWrapper().getInventoryHandler().saveInventory();
			return new LinkedPrimaryChestExpansionFixture(primaryPos, addedChestPos,
					requireEndpoint(primary, "linked primary chest before expansion").groupId(), title);
		});
		try {
			movePlayerNearLinkedStorage(fixture.primaryPos());
			openLinkedStorageChestMenu(fixture.primaryPos());
			waitForClientLinkedStorageMenu(new LinkedStorageMenuFixture(fixture.primaryPos(), fixture.primaryPos(), fixture.groupId(), 27, fixture.title()),
					fixture.primaryPos(), LinkedStorageEndpointRole.PRIMARY);
			openLinkedStorageSettingsMenu(fixture.primaryPos());
			waitForClientLinkedStorageSettingsMenu(fixture.primaryPos(), 27, LinkedStorageEndpointRole.PRIMARY);
			BlockPos expandedPrimaryPos = AutomationRuntime.runOnServer(player -> {
				require(player.containerMenu instanceof StorageSettingsContainerMenu, "Linked primary settings menu closed before expansion transfer");
				placeChest(player.serverLevel(), player, fixture.addedChestPos(), new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				List<ChestBlockEntity> parts = List.of(getChest(player.serverLevel(), fixture.primaryPos()),
						getChest(player.serverLevel(), fixture.addedChestPos()));
				ChestBlockEntity expandedPrimary = parts.stream().filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing main chest after linked primary expansion"));
				require(player.containerMenu == player.inventoryMenu, "Canonical root replacement left the stale settings menu open");
				require(parts.stream().filter(ChestBlockEntity::isMainChest).count() == 1
						&& expandedPrimary.getStorageWrapper().getInventoryHandler().getSlots() == 54
						&& count(expandedPrimary.getStorageWrapper().getInventoryHandler(), Items.DIAMOND) == 7
						&& requireEndpoint(expandedPrimary, "expanded linked primary chest").groupId().equals(fixture.groupId())
						&& expandedPrimary.getStorageWrapper().getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent(),
						"Linked primary expansion did not retain its endpoint, contents, and 54-slot canonical profile");
				return expandedPrimary.getBlockPos();
			});
			waitForClientMenuClosed();
			openLinkedStorageMenu(expandedPrimaryPos);
			waitForClientLinkedStorageMenu(new LinkedStorageMenuFixture(expandedPrimaryPos, expandedPrimaryPos, fixture.groupId(), 54, fixture.title()),
					expandedPrimaryPos, LinkedStorageEndpointRole.PRIMARY);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				try {
					clearArea(player.serverLevel(), fixture.primaryPos());
				} finally {
					restorePlayerLocation(player, originalPlayerLocation);
				}
				return null;
			});
		}
	}

	private static void runLinkedSecondaryChestSplitMenuClosureRegression() {
		PlayerLocation originalPlayerLocation = capturePlayerLocation();
		LinkedSecondaryChestSplitMenuFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 164);
			BlockPos secondaryPos = primaryPos.east(3);
			BlockPos addedChestPos = secondaryPos.east();
			clearArea(level, primaryPos);
			placeChest(level, player, primaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
			placeChest(level, player, secondaryPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
			ChestBlockEntity primary = getChest(level, primaryPos);
			ChestBlockEntity secondary = getChest(level, secondaryPos);
			ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked secondary chest split menu fixture");
			String title = "Linked Secondary Split";
			primary.setCustomName(Component.literal(title));
			placeChest(level, player, addedChestPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
			ChestBlockEntity main = List.of(getChest(level, secondaryPos), getChest(level, addedChestPos)).stream().filter(ChestBlockEntity::isMainChest)
					.findFirst().orElseThrow(() -> new IllegalStateException("Missing linked secondary double-chest main half"));
			return new LinkedSecondaryChestSplitMenuFixture(primaryPos, secondaryPos, addedChestPos, main.getBlockPos(),
					requireEndpoint(primary, "linked secondary split primary").groupId(), primary.getStorageWrapper().getInventoryHandler().getSlots(), title);
		});
		try {
			openLinkedSecondaryChestSplitMenu(fixture, false);
			BlockPos remainingPos = AutomationRuntime.runOnServer(player -> {
				if (!(player.containerMenu instanceof StorageContainerMenu staleMenu)) {
					throw new IllegalStateException("Linked secondary inventory menu closed before split transfer");
				}
				BlockPos remaining = fixture.mainPos().equals(fixture.secondaryPos()) ? fixture.addedChestPos() : fixture.secondaryPos();
				require(player.gameMode.destroyBlock(fixture.mainPos()), "Player break did not split the linked secondary double chest");
				require(player.containerMenu == player.inventoryMenu && player.containerMenu != staleMenu
						&& getChest(player.serverLevel(), remaining).getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE,
						"Linked secondary split did not close the stale inventory menu");
				return remaining;
			});
			waitForClientMenuClosed();
			BlockPos reopenedMainPos = AutomationRuntime.runOnServer(player -> {
				BlockPos removedPos = remainingPos.equals(fixture.secondaryPos()) ? fixture.addedChestPos() : fixture.secondaryPos();
				placeChest(player.serverLevel(), player, removedPos, new ItemStack(ModBlocks.CHEST_ITEM.get()), 0);
				return List.of(getChest(player.serverLevel(), remainingPos), getChest(player.serverLevel(), removedPos)).stream()
						.filter(ChestBlockEntity::isMainChest).findFirst()
						.orElseThrow(() -> new IllegalStateException("Missing linked secondary main chest before settings split transfer")).getBlockPos();
			});
			openLinkedSecondaryChestSplitMenu(fixture.withMainPos(reopenedMainPos), true);
			AutomationRuntime.runOnServer(player -> {
				if (!(player.containerMenu instanceof StorageSettingsContainerMenu staleMenu)) {
					throw new IllegalStateException("Linked secondary settings menu closed before split transfer");
				}
				BlockPos remaining = reopenedMainPos.equals(fixture.secondaryPos()) ? fixture.addedChestPos() : fixture.secondaryPos();
				require(player.gameMode.destroyBlock(reopenedMainPos), "Player break did not split the linked secondary double chest from settings");
				require(player.containerMenu == player.inventoryMenu && player.containerMenu != staleMenu
						&& getChest(player.serverLevel(), remaining).getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE,
						"Linked secondary split did not close the stale settings menu");
				return null;
			});
			waitForClientMenuClosed();
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				try {
					clearArea(player.serverLevel(), fixture.primaryPos());
				} finally {
					restorePlayerLocation(player, originalPlayerLocation);
				}
				return null;
			});
		}
	}

	private static void openLinkedSecondaryChestSplitMenu(LinkedSecondaryChestSplitMenuFixture fixture, boolean openSettings) {
		movePlayerNearLinkedStorage(fixture.mainPos());
		openLinkedStorageChestMenu(fixture.mainPos());
		waitForClientStorageMenu(fixture.mainPos());
		if (openSettings) {
			openLinkedStorageSettingsMenu(fixture.mainPos());
			waitForClientStorageSettingsMenu(fixture.mainPos());
		}
	}

	private static void runLinkedStorageStackTooltipRegression() {
		LinkedStorageStackTooltipFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 148);
			BlockPos secondaryPos = primaryPos.east(3);
			clearArea(level, primaryPos);
			placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
			placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
			StorageBlockEntity primary = getStorage(level, primaryPos, "dropped linked stack primary");
			ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker,
							getStorage(level, secondaryPos, "dropped linked stack secondary")) == LinkedStorageService.LinkResult.SUCCESS,
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
					&& StorageBlockEntity.getLinkedStorageEndpointData(drops.get(0).getItem()).filter(endpoint -> endpoint.groupId().equals(groupId))
							.isPresent()
					&& StorageBlockEntity.getLinkedStorageEndpointRole(drops.get(0).getItem()).filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent(),
					"Dropped linked storage stack did not retain its primary endpoint group and role");
			player.getInventory().setItem(inventorySlot, drops.get(0).getItem().copy());
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
				clearArea(player.serverLevel(), fixture.primaryPos());
				return null;
			});
		}
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
				if (!(stack.getItem() instanceof WoodStorageBlockItem storageItem)) {
					return false;
				}
				Object tooltip = storageItem.getTooltipImage(stack).orElse(null);
				return tooltip instanceof LinkedStorageTooltip linkedTooltip && linkedTooltip.role() == LinkedStorageEndpointRole.PRIMARY
						&& linkedTooltip.groupId().equals(fixture.groupId())
						&& new StackStorageWrapper(stack).getContentsUuid().filter(fixture.groupId()::equals).isPresent();
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
				if (minecraft.player == null || ClientLinkedStorageContents.getRevision(fixture.groupId()).isEmpty()) {
					return false;
				}
				return count(new StackStorageWrapper(minecraft.player.getInventory().getItem(fixture.inventorySlot())).getInventoryHandler(),
						Items.DIAMOND) == 7;
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Dropped linked stack tooltip did not receive the canonical contents snapshot");
	}

	private static void runSecondaryDroppedPickupRegression() {
		DroppedItemPickupFixture fixture = AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos primaryPos = player.blockPosition().offset(0, 0, 160);
			// Keep the secondary outside the Magnet upgrade's maximum range so this
			// verifies that only the primary endpoint performs global pickup work.
			BlockPos secondaryPos = primaryPos.east(21);
			Vec3 originalPosition = player.position();
			clearArea(level, primaryPos);
			clearArea(level, secondaryPos);
			placeBlockAsPlayer(level, player, primaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
			placeBlockAsPlayer(level, player, secondaryPos, new ItemStack(ModBlocks.BARREL_ITEM.get()), 0);
			StorageBlockEntity primary = getStorage(level, primaryPos, "magnet pickup primary");
			StorageBlockEntity secondary = getStorage(level, secondaryPos, "magnet pickup secondary");
			ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
			require(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS
					&& LinkedStorageService.linkWithResult(level, player.getUUID(), linker, secondary) == LinkedStorageService.LinkResult.SUCCESS,
					"Could not create linked storage for dropped-item pickup regression");
			UpgradeHandler upgrades = primary.getStorageWrapper().getUpgradeHandler();
			upgrades.setStackInSlot(0, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModItems.MAGNET_UPGRADE.get()));
			List<MagnetUpgradeWrapper> magnets = upgrades.getWrappersThatImplement(MagnetUpgradeWrapper.class);
			require(magnets.size() == 1, "Linked primary did not create exactly one Magnet upgrade wrapper");
			magnets.get(0).setPickupItems(true);
			upgrades.saveInventory();
			require(primary.getBlockState().getValue(StorageBlockBase.TICKING)
					&& primary.getStorageWrapper().getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent(),
					"Linked primary endpoint did not become the canonical ticking pickup endpoint");
			require(!secondary.getBlockState().getValue(StorageBlockBase.TICKING)
					&& secondary.getStorageWrapper().getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.SECONDARY::equals).isPresent(),
					"Linked secondary endpoint incorrectly retained global pickup ticking behavior");
			Vec3 dropPosition = Vec3.atCenterOf(secondaryPos).add(0, -0.25D, 0);
			ItemEntity droppedItem = new ItemEntity(level, dropPosition.x, dropPosition.y, dropPosition.z, new ItemStack(Items.ENDER_PEARL, 2));
			droppedItem.setDeltaMovement(Vec3.ZERO);
			droppedItem.setNoGravity(true);
			droppedItem.setDefaultPickUpDelay();
			require(level.addFreshEntity(droppedItem), "Could not spawn the linked secondary pickup regression item");
			return new DroppedItemPickupFixture(primaryPos, secondaryPos, Items.ENDER_PEARL, level.getGameTime() + 20, originalPosition);
		});
		try {
			waitForServerCondition("dropped item to remain on linked secondary endpoint",
					player -> player.serverLevel().getGameTime() >= fixture.verifyAfterGameTime()
							&& findDroppedItem(player.serverLevel(), fixture, fixture.secondaryPos()).filter(item -> item.getItem().getCount() == 2).isPresent()
							&& count(getStorage(player.serverLevel(), fixture.primaryPos(), "magnet pickup primary").getStorageWrapper().getInventoryHandler(),
									fixture.item()) == 0);
			AutomationRuntime.runOnServer(player -> {
				ItemEntity item = findDroppedItem(player.serverLevel(), fixture, fixture.secondaryPos())
						.orElseThrow(() -> new IllegalStateException("Dropped item disappeared from the linked secondary endpoint"));
				Vec3 primaryCenter = Vec3.atCenterOf(fixture.primaryPos()).add(0, -0.25D, 0);
				item.setPos(primaryCenter.x, primaryCenter.y, primaryCenter.z);
				return null;
			});
			waitForServerCondition("linked primary endpoint to pick up the dropped item",
					player -> findDroppedItem(player.serverLevel(), fixture, fixture.primaryPos()).isEmpty()
							&& count(getStorage(player.serverLevel(), fixture.primaryPos(), "magnet pickup primary").getStorageWrapper().getInventoryHandler(),
									fixture.item()) == 2);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.teleportTo(player.serverLevel(), fixture.originalPosition().x, fixture.originalPosition().y, fixture.originalPosition().z, Set.of(),
						player.getYRot(), player.getXRot());
				clearArea(player.serverLevel(), fixture.primaryPos());
				clearArea(player.serverLevel(), fixture.secondaryPos());
				return null;
			});
		}
	}

	private static Optional<ItemEntity> findDroppedItem(ServerLevel level, DroppedItemPickupFixture fixture, BlockPos center) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(5),
				itemEntity -> itemEntity.isAlive() && itemEntity.getItem().is(fixture.item())).stream().findFirst();
	}

	private static void waitForServerCondition(String description, java.util.function.Predicate<ServerPlayer> condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(condition::test)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static LimitedBarrelBlockEntity getLimitedBarrel(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Missing limited barrel at " + pos));
	}

	private static void waitForClientLimitedBarrelReloadProjection(BlockPos primaryPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				BlockPos secondaryPos = primaryPos.east(3);
				return minecraft.level != null && minecraft.level.getBlockEntity(primaryPos) instanceof LimitedBarrelBlockEntity primary
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
		return barrel.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().stream()
				.anyMatch(displayItem -> displayItem.getItem().is(Items.DIAMOND)) && barrel.getSlotCounts().contains(LINKED_LIMITED_RELOAD_ITEM_COUNT)
				&& barrel.getSlotFillLevels().stream().anyMatch(fillLevel -> fillLevel > 0F);
	}

	private static void waitForClientLinkedMemory(BlockPos secondaryPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> {
				if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof StorageContainerMenu menu)
						|| menu.getBlockPosition().filter(secondaryPos::equals).isEmpty()) {
					return false;
				}
				MemorySettingsCategory memory = menu.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
				return memory.getSlotFilterStack(0, false).filter(stack -> stack.is(Items.DIAMOND)).isPresent();
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked limited-barrel memory did not synchronize to the open secondary menu");
	}

	private static void waitForClientLinkedStorageSettingsMenu(BlockPos expectedPos, int expectedSlots, LinkedStorageEndpointRole expectedRole) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageSettingsContainerMenu menu
							&& menu.getBlockPosition().equals(expectedPos) && menu.getStorageWrapper().getInventoryHandler().getSlots() == expectedSlots
							&& menu.getStorageWrapper() instanceof ILinkedStorageEndpointProvider endpointProvider
							&& endpointProvider.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked storage settings menu did not open at " + expectedPos + " with " + expectedSlots + " slots");
	}

	private static void waitForClientStorageMenu(BlockPos expectedPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof StorageContainerMenu menu && menu.getBlockPosition().filter(expectedPos::equals).isPresent())) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked storage inventory menu did not open at " + expectedPos);
	}

	private static void waitForClientStorageSettingsMenu(BlockPos expectedPos) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageSettingsContainerMenu menu
							&& menu.getBlockPosition().equals(expectedPos))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Linked storage settings menu did not open at " + expectedPos);
	}

	private static void waitForClientMenuClosed() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(player -> player.containerMenu == player.inventoryMenu)
					&& AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen == null)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Client retained the stale linked-storage menu after canonical root replacement");
	}

	private static StorageBlockEntity getStorage(ServerLevel level, BlockPos pos, String name) {
		return level.getBlockEntity(pos, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get()).map(storage -> (StorageBlockEntity) storage)
				.orElseThrow(() -> new IllegalStateException("Missing barrel for " + name));
	}

	private static StorageBlockEntity getAnyStorage(ServerLevel level, BlockPos pos, String name) {
		if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
			return storage;
		}
		throw new IllegalStateException("Missing storage for " + name);
	}

	private static ChestBlockEntity getChest(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).orElseThrow(() -> new IllegalStateException("Missing chest at " + pos));
	}

	private static LinkedStorageEndpointData requireEndpoint(StorageBlockEntity storage, String name) {
		LinkedStorageEndpointData endpoint = storage.getLinkedStorageEndpointData();
		if (endpoint == null) {
			throw new IllegalStateException("Missing linked-storage endpoint data for " + name);
		}
		return endpoint;
	}

	private static InteractionResult useLinkerAsPlayer(ServerPlayer player, ItemStack linker, BlockPos pos) {
		PlayerInventoryState originalInventoryState = capturePlayerInventoryState(player);
		try {
			player.setItemInHand(InteractionHand.MAIN_HAND, linker);
			BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
			return player.gameMode.useItemOn(player, player.serverLevel(), linker, InteractionHand.MAIN_HAND, hitResult);
		} finally {
			restorePlayerInventoryState(player, originalInventoryState);
		}
	}

	private static void placeChest(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack, float yRot) {
		placeBlockAsPlayer(level, player, pos, stack, yRot);
	}

	private static void placeBlockAsPlayer(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack, float yRot) {
		Vec3 originalPosition = player.position();
		float originalYRot = player.getYRot();
		float originalXRot = player.getXRot();
		PlayerInventoryState originalInventoryState = capturePlayerInventoryState(player);
		try {
			if (!player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, Set.of(), yRot, 0)) {
				throw new IllegalStateException("Could not position player for linked storage regression");
			}
			BlockPos supportPos = pos.below();
			if (level.isEmptyBlock(supportPos)) {
				level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
			}
			player.setYRot(yRot);
			player.setXRot(0);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			InteractionResult result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false));
			require(result.consumesAction(), "Could not place regression fixture at " + pos + ": " + result);
		} finally {
			try {
				player.teleportTo(level, originalPosition.x, originalPosition.y, originalPosition.z, Set.of(), originalYRot, originalXRot);
			} finally {
				restorePlayerInventoryState(player, originalInventoryState);
			}
		}
	}

	private static void moveToSafeFixtureHeight(ServerPlayer player) {
		if (player.blockPosition().getY() >= 64) {
			return;
		}
		Vec3 position = player.position();
		if (!player.teleportTo(player.serverLevel(), position.x, 80, position.z, Set.of(), player.getYRot(), player.getXRot())) {
			throw new IllegalStateException("Could not move player to a safe linked-storage fixture height");
		}
	}

	private static PlayerInventoryState capturePlayerInventoryState(ServerPlayer player) {
		return new PlayerInventoryState(player.getInventory().selected, player.getItemInHand(InteractionHand.MAIN_HAND).copy());
	}

	private static void restorePlayerInventoryState(ServerPlayer player, PlayerInventoryState state) {
		player.getInventory().selected = state.selectedSlot();
		player.setItemInHand(InteractionHand.MAIN_HAND, state.mainHand().copy());
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		if (player.containerMenu != player.inventoryMenu) {
			player.containerMenu.broadcastChanges();
		}
		player.connection.send(new ClientboundSetCarriedItemPacket(state.selectedSlot()));
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

	private static int countPlayerResource(ServerPlayer player, Item item) {
		return InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player).stream().mapToInt(handler -> count(handler, item)).sum();
	}

	private static void clearArea(ServerLevel level, BlockPos center) {
		for (int x = -3; x <= 4; x++) {
			for (int y = -1; y <= 3; y++) {
				for (int z = -6; z <= 6; z++) {
					BlockPos pos = center.offset(x, y, z);
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
						storage.clearContent();
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(8.0D)).forEach(ItemEntity::discard);
	}

	private record LinkedStorageMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, UUID groupId, int inventorySlots, String title) {
	}

	private record LinkedStorageMenuState(BlockPos position, String title, UUID groupId, int slots, String role, int diamondCount) {
	}

	private record LinkedPrimaryChestExpansionFixture(BlockPos primaryPos, BlockPos addedChestPos, UUID groupId, String title) {
	}

	private record LinkedSecondaryChestSplitMenuFixture(BlockPos primaryPos, BlockPos secondaryPos, BlockPos addedChestPos, BlockPos mainPos, UUID groupId,
			int inventorySlots, String title) {
		private LinkedSecondaryChestSplitMenuFixture withMainPos(BlockPos newMainPos) {
			return new LinkedSecondaryChestSplitMenuFixture(primaryPos, secondaryPos, addedChestPos, newMainPos, groupId, inventorySlots, title);
		}
	}

	private record LinkedStorageStackTooltipFixture(BlockPos primaryPos, UUID groupId, int inventorySlot, ItemStack originalStack, GameType originalGameMode) {
	}

	private record DroppedItemPickupFixture(BlockPos primaryPos, BlockPos secondaryPos, Item item, long verifyAfterGameTime, Vec3 originalPosition) {
	}

	private record PlayerLocation(Vec3 position, float yRot, float xRot) {
	}

	private record PlayerInventoryState(int selectedSlot, ItemStack mainHand) {
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
}
