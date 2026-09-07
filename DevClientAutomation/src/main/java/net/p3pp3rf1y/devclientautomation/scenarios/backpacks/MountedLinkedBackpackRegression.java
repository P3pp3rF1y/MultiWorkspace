package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssembleRailType;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackRenderInfo;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.LinkedStorageBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpackscreateintegration.backpack.MountedSophisticatedBackpack;
import net.p3pp3rf1y.sophisticatedbackpackscreateintegration.client.MountedBackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpackscreateintegration.common.MountedBackpackContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class MountedLinkedBackpackRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);
	private static final int MAIN_COLOR = 0xFF225588;
	private static final int ACCENT_COLOR = 0xFFE2A100;
	private static final int TANK_UPGRADE_SLOT = 0;
	private static final int CLIENT_SYNC_TIMEOUT_SECONDS = 15;
	private static final PlayerInventoryMarker[] ORDINARY_PLAYER_MARKERS = {new PlayerInventoryMarker(1, Items.REDSTONE),
			new PlayerInventoryMarker(14, Items.LAPIS_LAZULI), new PlayerInventoryMarker(35, Items.ENDER_PEARL)};

	private MountedLinkedBackpackRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, MountedLinkedBackpackRegression::run);
	}

	public static void handleOrdinary(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, MountedLinkedBackpackRegression::runOrdinary);
	}

	public static void setupReload(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, () -> {
			Fixture fixture = AutomationRuntime.runOnServer(MountedLinkedBackpackRegression::setupFixture);
			JsonObject result = new JsonObject();
			result.addProperty("ok", true);
			result.addProperty("groupId", fixture.groupId().toString());
			result.addProperty("mainColor", MAIN_COLOR);
			result.addProperty("accentColor", ACCENT_COLOR);
			return result.toString();
		});
	}

	public static void reloadStatus(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		UUID groupId = UUID.fromString(string(request, "groupId", ""));
		sendJsonHandling(exchange, LOGGER, () -> reloadStatus(groupId));
	}

	private static String run() {
		Fixture fixture = AutomationRuntime.runOnServer(MountedLinkedBackpackRegression::setupFixture);
		try {
			waitForClientMountedBackpack(fixture, 0, "initial linked mounted Backpack");
			AutomationRuntime.runOnServer(player -> {
				openMountedBackpack(player, fixture);
				giveTankUpgrade(player);
				return true;
			});
			waitForClientMountedMenu(fixture, 0, "initial mounted Backpack menu");
			installClientTankUpgrade(fixture);
			int tankColumns = tankColumns();
			waitForServerColumns(fixture, tankColumns, "mounted Tank insert");
			waitForClientMountedMenu(fixture, tankColumns, "mounted Tank insert");
			removeClientTankUpgrade(fixture);
			waitForServerColumns(fixture, 0, "mounted Tank remove");
			waitForClientMountedMenu(fixture, 0, "mounted Tank remove");

			CompoundTag expectedRenderInfo = AutomationRuntime.runOnServer(player -> updateRemoteEndpoint(player, fixture));
			waitForServerColumns(fixture, tankColumns, "remote Tank insert");
			waitForClientMountedMenu(fixture, tankColumns, "remote Tank insert");
			waitForClientProjection(fixture, expectedRenderInfo, "remote display projection");

			JsonObject result = new JsonObject();
			result.addProperty("ok", true);
			result.addProperty("linkInPlace", true);
			result.addProperty("mountedMenuColumns", true);
			result.addProperty("remoteColumns", true);
			result.addProperty("remoteDisplayProjection", true);
			return result.toString();
		} finally {
			cleanupFixture(fixture);
		}
	}

	private static String runOrdinary() {
		Fixture fixture = AutomationRuntime.runOnServer(MountedLinkedBackpackRegression::setupOrdinaryFixture);
		try {
			waitForClientOrdinaryMountedBackpack(fixture, 0, "initial ordinary mounted Backpack");
			AutomationRuntime.runOnServer(player -> {
				openMountedBackpack(player, fixture);
				giveTankUpgrade(player);
				return true;
			});
			waitForClientOrdinaryMountedMenu(fixture, 0, "initial ordinary mounted Backpack menu");
			installClientTankUpgrade(fixture);
			int tankColumns = tankColumns();
			waitForServerColumns(fixture, tankColumns, "ordinary mounted Tank insert");
			waitForClientOrdinaryMountedMenu(fixture, tankColumns, "ordinary mounted Tank insert");
			removeClientTankUpgrade(fixture);
			waitForServerColumns(fixture, 0, "ordinary mounted Tank remove");
			waitForClientOrdinaryMountedMenu(fixture, 0, "ordinary mounted Tank remove");

			JsonObject result = new JsonObject();
			result.addProperty("ok", true);
			result.addProperty("mountedMenuColumns", true);
			return result.toString();
		} finally {
			cleanupFixture(fixture);
		}
	}

	private static void cleanupFixture(Fixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			if (Minecraft.getInstance().screen instanceof MountedBackpackScreen screen) {
				screen.onClose();
			}
			return true;
		});
		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			clearFixture(player.serverLevel(), fixture.origin());
			player.getInventory().clearContent();
			player.getInventory().setChanged();
			player.setGameMode(fixture.originalGameMode());
			return true;
		});
	}

	private static String reloadStatus(UUID groupId) {
		MountedBackpackReloadStatus serverStatus = AutomationRuntime.runOnServer(player -> findReloadStatus(player, groupId));
		waitForClientReloadProjection(serverStatus, "reloaded mounted Backpack projection");
		JsonObject result = new JsonObject();
		result.addProperty("ok", true);
		result.addProperty("groupId", groupId.toString());
		result.addProperty("mainColor", serverStatus.mainColor());
		result.addProperty("accentColor", serverStatus.accentColor());
		result.addProperty("hasDisplayItem", serverStatus.hasDisplayItem());
		return result.toString();
	}

	private static Fixture setupFixture(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos origin = player.blockPosition().relative(player.getDirection(), 6);
		BlockPos peerPos = origin.east(4);
		BlockPos assemblerPos = origin;
		BlockPos backpackPos = assemblerPos.above();
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		clearFixture(level, origin);

		ItemStack peer = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		IBackpackWrapper peerWrapper = BackpackWrapper.fromStack(peer);
		peerWrapper.setColors(MAIN_COLOR, ACCENT_COLOR);
		peerWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
		configureItemDisplay(peerWrapper, DisplaySide.FRONT);
		peerWrapper.getInventoryHandler().saveInventory();
		ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get());
		assertTrue(LinkedStorageService.link(level, linker, peer), "Could not create mounted linked Backpack group");
		LinkedStorageEndpointData peerEndpoint = requireEndpoint(peer, "linked peer");
		placeBackpack(level, peerPos, peer, "linked peer");

		level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
		level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
				.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
		level.setBlock(backpackPos, ModBlocks.DIAMOND_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, Direction.NORTH), 3);
		BackpackBlockEntity backpack = requireBackpack(level, backpackPos, "unlinked contraption Backpack");
		ItemStack mountedStack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		IBackpackWrapper mountedWrapper = BackpackWrapper.fromStack(mountedStack);
		mountedWrapper.setColors(MAIN_COLOR, ACCENT_COLOR);
		backpack.setBackpack(mountedStack);
		backpack.setChanged();

		AbstractContraptionEntity contraption = assembleCartContraption(level, assemblerPos);
		BlockPos localPos = findMountedBackpackPos(contraption);
		MountedSophisticatedBackpack mounted = requireMountedBackpack(contraption, localPos);
		mounted.initEntityLevelAndPositions(contraption, localPos, level, contraption.position());
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		assertTrue(contraption.handlePlayerInteraction(player, localPos, Direction.UP, InteractionHand.MAIN_HAND),
				"Ender Linker interaction with mounted Backpack was not handled");
		assertTrue(peerEndpoint.groupId().equals(requireEndpoint(mounted.getStorageStack(), "mounted Backpack after linking").groupId()),
				"Mounted Backpack did not join the linked group");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.getInventory().setChanged();
		return new Fixture(origin, peerPos, contraption.getId(), localPos, peerEndpoint.groupId(), originalGameMode);
	}

	private static Fixture setupOrdinaryFixture(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos origin = player.blockPosition().relative(player.getDirection(), 6);
		BlockPos assemblerPos = origin;
		BlockPos backpackPos = assemblerPos.above();
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		clearFixture(level, origin);

		level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
		level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
				.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
		level.setBlock(backpackPos, ModBlocks.DIAMOND_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, Direction.NORTH), 3);
		BackpackBlockEntity backpack = requireBackpack(level, backpackPos, "ordinary contraption Backpack");
		ItemStack mountedStack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		mountedStack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		IBackpackWrapper mountedWrapper = BackpackWrapper.fromStack(mountedStack);
		mountedWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
		mountedWrapper.getInventoryHandler().saveInventory();
		backpack.setBackpack(mountedStack);
		backpack.setChanged();

		AbstractContraptionEntity contraption = assembleCartContraption(level, assemblerPos);
		BlockPos localPos = findMountedBackpackPos(contraption);
		MountedSophisticatedBackpack mounted = requireMountedBackpack(contraption, localPos);
		mounted.initEntityLevelAndPositions(contraption, localPos, level, contraption.position());
		return new Fixture(origin, BlockPos.ZERO, contraption.getId(), localPos, null, originalGameMode);
	}

	private static CompoundTag updateRemoteEndpoint(ServerPlayer player, Fixture fixture) {
		BackpackBlockEntity peer = requireBackpack(player.serverLevel(), fixture.peerPos(), "remote linked peer");
		IBackpackWrapper wrapper = BackpackLinkedStorageResolver.resolveOrCreate(player.serverLevel(), peer.getBackpackWrapper().getBackpack());
		try {
			wrapper.getUpgradeHandler().setStackInSlot(TANK_UPGRADE_SLOT, new ItemStack(ModItems.TANK_UPGRADE.get()));
			wrapper.setColumnsTaken(tankColumns(), true);
			wrapper.getUpgradeHandler().saveInventory();
			wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.EMERALD));
			wrapper.getInventoryHandler().saveInventory();
			configureItemDisplay(wrapper, DisplaySide.LEFT);
			return wrapper.getRenderInfo().getNbt().copy();
		} finally {
			if (wrapper instanceof LinkedStorageBackpackWrapper linkedStorageBackpackWrapper) {
				linkedStorageBackpackWrapper.close();
			}
		}
	}

	private static MountedBackpackReloadStatus findReloadStatus(ServerPlayer player, UUID groupId) {
		for (AbstractContraptionEntity contraption : player.serverLevel().getEntitiesOfClass(AbstractContraptionEntity.class,
				player.getBoundingBox().inflate(128))) {
			for (BlockPos localPos : ContraptionHelper.getMountedItemStorages(contraption).keySet()) {
				if (ContraptionHelper.getMountedStorage(contraption, localPos) instanceof MountedSophisticatedBackpack mounted
						&& groupId.equals(requireEndpoint(mounted.getStorageStack(), "reloaded mounted Backpack").groupId())) {
					ItemStack stack = mounted.getStorageStack();
					return new MountedBackpackReloadStatus(contraption.getId(), localPos,
							stack.getOrDefault(ModCoreDataComponents.MAIN_COLOR, BackpackWrapper.DEFAULT_MAIN_COLOR),
							stack.getOrDefault(ModCoreDataComponents.ACCENT_COLOR, BackpackWrapper.DEFAULT_ACCENT_COLOR), hasDisplayItem(stack, Items.DIAMOND));
				}
			}
		}
		throw new IllegalStateException("Could not find the reloaded linked mounted Backpack");
	}

	private static void openMountedBackpack(ServerPlayer player, Fixture fixture) {
		Entity entity = player.serverLevel().getEntity(fixture.contraptionEntityId());
		if (!(entity instanceof AbstractContraptionEntity contraption)) {
			throw new IllegalStateException("Mounted Backpack contraption is unavailable");
		}
		assertTrue(contraption.handlePlayerInteraction(player, fixture.localPos(), Direction.UP, InteractionHand.MAIN_HAND),
				"Mounted Backpack interaction did not open its menu");
	}

	private static void giveTankUpgrade(ServerPlayer player) {
		player.getInventory().setItem(0, new ItemStack(ModItems.TANK_UPGRADE.get()));
		for (PlayerInventoryMarker marker : ORDINARY_PLAYER_MARKERS) {
			player.getInventory().setItem(marker.slotIndex(), new ItemStack(marker.item()));
		}
		player.getInventory().setChanged();
		player.containerMenu.broadcastFullState();
	}

	private static void installClientTankUpgrade(Fixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			MountedBackpackContainerMenu menu = requireMountedMenu(fixture);
			Slot playerTankSlot = findTankInPlayerInventory(menu);
			clickSlot((MountedBackpackScreen) Minecraft.getInstance().screen, playerTankSlot);
			menu = requireMountedMenu(fixture);
			assertTrue(menu.getCarried().is(ModItems.TANK_UPGRADE.get()), "Mounted Backpack Tank upgrade was not carried");
			clickSlot((MountedBackpackScreen) Minecraft.getInstance().screen, menu.upgradeSlots.get(TANK_UPGRADE_SLOT));
			return true;
		});
	}

	private static void removeClientTankUpgrade(Fixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			MountedBackpackContainerMenu menu = requireMountedMenu(fixture);
			Slot upgradeSlot = menu.upgradeSlots.get(TANK_UPGRADE_SLOT);
			assertTrue(upgradeSlot.getItem().is(ModItems.TANK_UPGRADE.get()), "Mounted Backpack Tank upgrade is missing before removal");
			clickSlot((MountedBackpackScreen) Minecraft.getInstance().screen, upgradeSlot);
			return true;
		});
	}

	private static MountedBackpackContainerMenu requireMountedMenu(Fixture fixture) {
		if (Minecraft.getInstance().screen instanceof MountedBackpackScreen && Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.containerMenu instanceof MountedBackpackContainerMenu menu
				&& menu.getContext().getContraptionEntityId() == fixture.contraptionEntityId() && menu.getContext().getLocalPos().equals(fixture.localPos())) {
			return menu;
		}
		throw new IllegalStateException("Mounted Backpack menu is unavailable");
	}

	private static void waitForClientMountedBackpack(Fixture fixture, int columnsTaken, String description) {
		waitForClient(() -> getClientMountedBackpack(fixture).map(mounted -> {
			LinkedStorageEndpointData endpoint = mounted.getStorageStack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
			return endpoint != null && fixture.groupId().equals(endpoint.groupId())
					&& mounted.getStorageStack().getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0) == columnsTaken;
		}).orElse(false), description);
	}

	private static void waitForClientMountedMenu(Fixture fixture, int columnsTaken, String description) {
		waitForClient(() -> {
			if (!(Minecraft.getInstance().screen instanceof MountedBackpackScreen) || Minecraft.getInstance().player == null
					|| !(Minecraft.getInstance().player.containerMenu instanceof MountedBackpackContainerMenu menu)) {
				return false;
			}
			return menu.getContext().getContraptionEntityId() == fixture.contraptionEntityId() && menu.getContext().getLocalPos().equals(fixture.localPos())
					&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper && menu.getStorageWrapper().getColumnsTaken() == columnsTaken
					&& getClientMountedBackpack(fixture)
							.map(mounted -> mounted.getStorageStack().getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0) == columnsTaken).orElse(false);
		}, description);
	}

	private static void waitForClientOrdinaryMountedBackpack(Fixture fixture, int columnsTaken, String description) {
		waitForClient(() -> getClientMountedBackpack(fixture).map(mounted -> !mounted.getStorageStack().has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)
				&& mounted.getStorageStack().getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0) == columnsTaken).orElse(false), description);
	}

	private static void waitForClientOrdinaryMountedMenu(Fixture fixture, int columnsTaken, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_SYNC_TIMEOUT_SECONDS);
		do {
			if (AutomationRuntime.runOnClient(() -> hasClientOrdinaryMountedMenuState(fixture, columnsTaken))) {
				return;
			}
			sleep();
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(
				"Timed out waiting for client " + description + ": " + AutomationRuntime.runOnClient(() -> getClientOrdinaryMountedMenuState(fixture)));
	}

	private static boolean hasClientOrdinaryMountedMenuState(Fixture fixture, int columnsTaken) {
		if (!(Minecraft.getInstance().screen instanceof MountedBackpackScreen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof MountedBackpackContainerMenu menu)) {
			return false;
		}
		return menu.getContext().getContraptionEntityId() == fixture.contraptionEntityId()
				&& menu.getContext().getLocalPos().equals(fixture.localPos()) && !(menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper)
				&& menu.getStorageWrapper().getColumnsTaken() == columnsTaken && getClientMountedBackpack(fixture)
						.map(mounted -> mounted.getStorageStack().getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0) == columnsTaken).orElse(false)
				&& hasOrdinaryPlayerInventoryMarkers(menu);
	}

	private static String getClientOrdinaryMountedMenuState(Fixture fixture) {
		if (!(Minecraft.getInstance().screen instanceof MountedBackpackScreen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof MountedBackpackContainerMenu menu)) {
			return "mounted menu is unavailable";
		}

		StringBuilder markerState = new StringBuilder();
		for (PlayerInventoryMarker marker : ORDINARY_PLAYER_MARKERS) {
			ItemStack inventoryStack = Minecraft.getInstance().player.getInventory().getItem(marker.slotIndex());
			String menuStack = menu.slots.stream()
					.filter(slot -> slot.container == Minecraft.getInstance().player.getInventory() && slot.getContainerSlot() == marker.slotIndex())
					.findFirst().map(slot -> slot.getItem().toString()).orElse("missing");
			if (!markerState.isEmpty()) {
				markerState.append(", ");
			}
			markerState.append(marker.slotIndex()).append(" inventory=").append(inventoryStack).append(" menu=").append(menuStack);
		}
		String mountedState = getClientMountedBackpack(fixture)
				.map(mounted -> "columns=" + mounted.getStorageStack().getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0) + ", sameWrapper="
						+ (mounted.getStorageWrapper() == menu.getStorageWrapper()))
				.orElse("unavailable");
		return "menu columns=" + menu.getStorageWrapper().getColumnsTaken() + ", mounted=" + mountedState + ", markers=" + markerState;
	}

	private static boolean hasOrdinaryPlayerInventoryMarkers(MountedBackpackContainerMenu menu) {
		if (Minecraft.getInstance().player == null) {
			return false;
		}

		for (PlayerInventoryMarker marker : ORDINARY_PLAYER_MARKERS) {
			if (!Minecraft.getInstance().player.getInventory().getItem(marker.slotIndex()).is(marker.item())
					|| menu.slots.stream().noneMatch(slot -> slot.container == Minecraft.getInstance().player.getInventory()
							&& slot.getContainerSlot() == marker.slotIndex() && slot.getItem().is(marker.item()))) {
				return false;
			}
		}
		return true;
	}

	private static void waitForServerColumns(Fixture fixture, int columnsTaken, String description) {
		waitForServer(player -> {
			MountedSophisticatedBackpack mounted = requireMountedBackpack(player.serverLevel(), fixture);
			return mounted.getStorageWrapper().getColumnsTaken() == columnsTaken
					&& mounted.getStorageStack().getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0) == columnsTaken;
		}, description);
	}

	private static void waitForClientProjection(Fixture fixture, CompoundTag expectedRenderInfo, String description) {
		waitForClient(() -> getClientRenderedBackpack(fixture)
				.map(backpack -> backpack.getBackpackWrapper().getBackpack().getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0) == tankColumns()
						&& backpack.getBackpackWrapper().getRenderInfo().getNbt().equals(expectedRenderInfo)
						&& hasDisplayItem(backpack.getBackpackWrapper().getBackpack(), Items.EMERALD))
				.orElse(false), description);
	}

	private static void waitForClientReloadProjection(MountedBackpackReloadStatus status, String description) {
		Fixture fixture = new Fixture(BlockPos.ZERO, BlockPos.ZERO, status.contraptionEntityId(), status.localPos(), null, GameType.SURVIVAL);
		waitForClient(
				() -> getClientRenderedBackpack(fixture).map(backpack -> backpack.getBackpackWrapper().getMainColor() == status.mainColor()
						&& backpack.getBackpackWrapper().getAccentColor() == status.accentColor()
						&& (!status.hasDisplayItem() || hasDisplayItem(backpack.getBackpackWrapper().getBackpack(), Items.DIAMOND))).orElse(false),
				description);
	}

	private static void waitForServer(java.util.function.Function<ServerPlayer, Boolean> condition, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(condition)) {
				return;
			}
			sleep();
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for server " + description);
	}

	private static void waitForClient(java.util.function.BooleanSupplier condition, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLIENT_SYNC_TIMEOUT_SECONDS);
		do {
			if (AutomationRuntime.runOnClient(condition::getAsBoolean)) {
				return;
			}
			sleep();
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client " + description);
	}

	private static void sleep() {
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for mounted Backpack synchronization", e);
		}
	}

	private static java.util.Optional<MountedSophisticatedBackpack> getClientMountedBackpack(Fixture fixture) {
		if (!(Minecraft.getInstance().level != null
				&& Minecraft.getInstance().level.getEntity(fixture.contraptionEntityId()) instanceof AbstractContraptionEntity contraption)) {
			return java.util.Optional.empty();
		}
		return java.util.Optional.ofNullable(ContraptionHelper.getMountedStorage(contraption, fixture.localPos()))
				.filter(MountedSophisticatedBackpack.class::isInstance).map(MountedSophisticatedBackpack.class::cast);
	}

	private static java.util.Optional<BackpackBlockEntity> getClientRenderedBackpack(Fixture fixture) {
		if (!(Minecraft.getInstance().level != null
				&& Minecraft.getInstance().level.getEntity(fixture.contraptionEntityId()) instanceof AbstractContraptionEntity contraption)) {
			return java.util.Optional.empty();
		}
		return java.util.Optional.ofNullable(contraption.getContraption().getBlockEntityClientSide(fixture.localPos()))
				.filter(BackpackBlockEntity.class::isInstance).map(BackpackBlockEntity.class::cast);
	}

	private static MountedSophisticatedBackpack requireMountedBackpack(ServerLevel level, Fixture fixture) {
		Entity entity = level.getEntity(fixture.contraptionEntityId());
		if (!(entity instanceof AbstractContraptionEntity contraption)) {
			throw new IllegalStateException("Mounted Backpack contraption is unavailable");
		}
		return requireMountedBackpack(contraption, fixture.localPos());
	}

	private static MountedSophisticatedBackpack requireMountedBackpack(AbstractContraptionEntity contraption, BlockPos localPos) {
		if (ContraptionHelper.getMountedStorage(contraption, localPos) instanceof MountedSophisticatedBackpack mounted) {
			return mounted;
		}
		throw new IllegalStateException("Create contraption does not contain a mounted Backpack at " + localPos);
	}

	private static BlockPos findMountedBackpackPos(AbstractContraptionEntity contraption) {
		return ContraptionHelper.getMountedItemStorages(contraption).keySet().stream()
				.filter(localPos -> ContraptionHelper.getMountedStorage(contraption, localPos) instanceof MountedSophisticatedBackpack).findFirst()
				.orElseThrow(() -> new IllegalStateException("Create cart did not mount the Backpack"));
	}

	private static AbstractContraptionEntity assembleCartContraption(ServerLevel level, BlockPos assemblerPos) {
		Minecart cart = new Minecart(level, assemblerPos.getX() + 0.5D, assemblerPos.getY(), assemblerPos.getZ() + 0.5D);
		level.addFreshEntity(cart);
		CartAssemblerBlockEntity assembler = WorldHelper.getBlockEntity(level, assemblerPos, CartAssemblerBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Create cart assembler block entity is missing"));
		assembler.tryAssemble(cart);
		return cart.getPassengers().stream().filter(AbstractContraptionEntity.class::isInstance).map(AbstractContraptionEntity.class::cast).findFirst()
				.orElseThrow(() -> new IllegalStateException("Create cart assembler did not create a contraption"));
	}

	private static void placeBackpack(ServerLevel level, BlockPos pos, ItemStack stack, String name) {
		level.setBlockAndUpdate(pos, ModBlocks.DIAMOND_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, Direction.NORTH));
		BackpackBlockEntity backpack = requireBackpack(level, pos, name);
		backpack.setBackpack(stack);
		backpack.refreshRenderState();
	}

	private static BackpackBlockEntity requireBackpack(ServerLevel level, BlockPos pos, String name) {
		return WorldHelper.getBlockEntity(level, pos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException(name + " Backpack block entity is missing"));
	}

	private static void configureItemDisplay(IBackpackWrapper wrapper, DisplaySide side) {
		ItemDisplaySettingsCategory display = wrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
		if (!display.getSlots().contains(0)) {
			display.selectSlot(0);
		}
		display.setDisplaySide(side);
		display.itemsChanged();
	}

	private static Slot findTankInPlayerInventory(MountedBackpackContainerMenu menu) {
		for (int slotIndex = menu.getNumberOfStorageInventorySlots(); slotIndex < menu.getInventorySlotsSize(); slotIndex++) {
			Slot slot = menu.getSlot(slotIndex);
			if (slot.getItem().is(ModItems.TANK_UPGRADE.get())) {
				return slot;
			}
		}
		throw new IllegalStateException("Mounted Backpack Tank upgrade is missing from player inventory");
	}

	private static void clickSlot(MountedBackpackScreen screen, Slot slot) {
		double x = screen.getGuiLeft() + slot.x + 8.0;
		double y = screen.getGuiTop() + slot.y + 8.0;
		assertTrue(screen.mouseClicked(x, y, 0), "Mounted Backpack slot click was not handled");
		screen.mouseReleased(x, y, 0);
	}

	private static LinkedStorageEndpointData requireEndpoint(ItemStack stack, String name) {
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null) {
			throw new IllegalStateException(name + " is not a linked storage endpoint");
		}
		return endpoint;
	}

	private static boolean hasDisplayItem(ItemStack backpack, net.minecraft.world.item.Item item) {
		return BackpackRenderInfo.fromPhysicalStack(backpack).getItemDisplayRenderInfo().getDisplayItems().stream()
				.anyMatch(displayItem -> displayItem.getItem().is(item));
	}

	private static int tankColumns() {
		return ((IUpgradeItem<?>) ModItems.TANK_UPGRADE.get()).getInventoryColumnsTaken();
	}

	private static void clearFixture(ServerLevel level, BlockPos origin) {
		level.getEntitiesOfClass(Entity.class, new AABB(origin).inflate(10), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
		for (int x = -2; x <= 8; x++) {
			for (int y = -2; y <= 4; y++) {
				for (int z = -3; z <= 3; z++) {
					level.setBlock(origin.offset(x, y, z), y == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static void assertTrue(boolean value, String message) {
		if (!value) {
			throw new IllegalStateException(message);
		}
	}

	private record Fixture(BlockPos origin, BlockPos peerPos, int contraptionEntityId, BlockPos localPos, UUID groupId, GameType originalGameMode) {
	}

	private record MountedBackpackReloadStatus(int contraptionEntityId, BlockPos localPos, int mainColor, int accentColor, boolean hasDisplayItem) {
	}

	private record PlayerInventoryMarker(int slotIndex, Item item) {
	}
}
