package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.ClientLinkedStorageBackpackContents;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.LinkedStorageBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackSettingsScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerTargetData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

final class BackpackLinkedStorageRegression {
	private static final int PRIMARY_SLOT = 0;
	private static final int LINKER_SLOT = 1;
	private static final int INTERACTION_SLOT = 2;
	private static final int STASH_SLOT = 9;
	private static final int STASH_COUNT = 13;
	private static final int TEST_ITEM_SLOT = STASH_SLOT + 2;
	private static final int TEST_STORAGE_SOURCE_SLOT = 1;
	private static final int TEST_STORAGE_TARGET_SLOT = 3;
	private static final int TANK_SLOT = 1;
	private static final int INCEPTION_LINKED_CHILD_SLOT = 0;
	private static final int INCEPTION_LINKED_CHILD_MARKER_COUNT = 5;
	private static final int INCEPTION_MOVED_CHILD_INVENTORY_SLOT = 1;

	private BackpackLinkedStorageRegression() {
	}

	static void handle(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			send(exchange, "{\"ok\":false,\"error\":\"Method not allowed\"}");
			return;
		}
		try {
			run();
			send(exchange, "{\"ok\":true,\"linkedStorageFeedbackUiSync\":true,\"linkedCarrierRelocation\":true,\"nestedLinkedChild\":true}");
		} catch (RuntimeException e) {
			send(exchange, "{\"ok\":false,\"error\":" + quote(e.getMessage()) + "}");
		}
	}

	static void handleInceptionLinkedChild(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			send(exchange, "{\"ok\":false,\"error\":\"Method not allowed\"}");
			return;
		}
		try {
			runClientInceptionLinkedChildPersistenceRegression();
			send(exchange, "{\"ok\":true,\"inceptionLinkedChildPersistence\":true,\"inceptionMovedLinkedChildDoesNotDuplicate\":true}");
		} catch (RuntimeException e) {
			send(exchange, "{\"ok\":false,\"error\":" + quote(e.getMessage()) + "}");
		}
	}

	private static void run() {
		runPreLinkedTankLayoutRegression();
		runLinkedCarrierRelocationRegression();
		runNestedLinkedChildRegression();
		Fixture fixture = AutomationRuntime.runOnServer(player -> setup(player, false));
		try {
			waitForClientLinker(fixture);
			AutomationRuntime.runOnClient(() -> {
				linkPlacedEndpoint(fixture);
				return true;
			});
			waitForServerPlacedEndpoint(fixture);
			waitForClientPlacedEndpoint(fixture);
			AutomationRuntime.runOnServer(player -> selectHotbarSlot(player, PRIMARY_SLOT));
			waitForClientSelectedSlot(fixture.primaryEndpoint(), PRIMARY_SLOT, "primary selection");

			openItemBackpack(fixture);
			waitForMenu(fixture, false, false, "item primary initial open");
			waitForState(fixture, false, false, false, true, "item primary initial profile");
			int firstItemChangeTick = moveTestItemIntoLinkedStorage(fixture);
			waitForServerTestItem(fixture);
			waitForClientTestItem(fixture, firstItemChangeTick);
			closeMenu(false, "item primary before inventory stash");

			long stashRevision = AutomationRuntime
					.runOnServer(player -> LinkedStorageGroupsSavedData.get(player.level()).manager().getRevision(fixture.groupId()));
			stashIntoBackpack(fixture);
			waitForServerStash(fixture, stashRevision);
			waitForClientStash(fixture);

			openItemBackpack(fixture);
			waitForMenu(fixture, false, false, "item primary after inventory stash");
			waitForStashView(fixture);
			waitForState(fixture, false, false, false, true, "item primary stash reopen");
			toggleTankUpgrade(fixture, false, "item primary insert");
			waitForState(fixture, false, true, false, false, "item primary insert");
			int tankItemChangeTick = moveTestItemWithTank(fixture);
			waitForServerTestItemWithTank(fixture);
			waitForClientTestItemWithTank(fixture, tankItemChangeTick);
			roundTripSettings();
			openItemBackpack(fixture);
			waitForMenu(fixture, false, true, "item primary settings reopen");
			waitForState(fixture, false, true, false, true, "item primary settings round trip");
			toggleTankUpgrade(fixture, false, "item primary remove");
			waitForState(fixture, false, false, true, false, "item primary remove");
			closeMenu(false, "item primary remove");
			openItemBackpack(fixture);
			waitForMenu(fixture, false, false, "item primary remove reopen");
			toggleTankUpgrade(fixture, false, "item primary reinsert");
			waitForState(fixture, false, true, false, false, "item primary reinsert");
			closeMenu(false, "item primary after reinsert");

			AutomationRuntime.runOnServer(player -> selectHotbarSlot(player, INTERACTION_SLOT));
			waitForClientSelectedSlot(null, INTERACTION_SLOT, "placed endpoint interaction selection");
			openPlacedBackpack(fixture);
			waitForMenu(fixture, true, true, "placed secondary initial open");
			waitForState(fixture, true, true, false, true, "placed secondary initial profile");
			toggleTankUpgrade(fixture, true, "placed secondary remove");
			waitForState(fixture, true, false, true, false, "placed secondary remove");
			closeMenu(true, "placed secondary remove");
			openPlacedBackpack(fixture);
			waitForMenu(fixture, true, false, "placed secondary remove reopen");
			toggleTankUpgrade(fixture, true, "placed secondary insert");
			waitForState(fixture, true, true, false, false, "placed secondary insert");
			closeMenu(true, "placed secondary insert");
			openPlacedBackpack(fixture);
			waitForMenu(fixture, true, true, "placed secondary final reopen");
			waitForState(fixture, true, true, false, true, "placed secondary final profile");
		} finally {
			AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().screen instanceof BackpackScreen screen) {
					screen.onClose();
				}
				return true;
			});
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea(player.level(), fixture.placedEndpointPos());
				return true;
			});
		}
	}

	private static void runClientInceptionLinkedChildPersistenceRegression() {
		InceptionLinkedChildFixture fixture = AutomationRuntime
				.runOnServer(BackpackLinkedStorageRegression::prepareClientInceptionLinkedChildPersistenceRegression);
		try {
			waitForClient(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().is(ModItems.NETHERITE_BACKPACK.get()),
					"client Inception parent Backpack");
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				assertTrue(
						minecraft.player != null && minecraft.gameMode != null
								&& minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND).consumesAction(),
						"Client Inception parent Backpack use did not consume the interaction");
				return true;
			});
			waitForClient(() -> isClientInceptionParentMenu(fixture), "client Inception parent Backpack menu");
			AutomationRuntime.runOnClient(() -> {
				BackpackContainer menu = requireClientInceptionMenu(BackpackContext.ContextType.ITEM_BACKPACK, "child open");
				assertTrue(
						fixture.childEndpoint().equals(menu.getSlot(INCEPTION_LINKED_CHILD_SLOT).getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
						"Client Inception parent menu lost the linked child endpoint before open");
				ClientPacketDistributor.sendToServer(new BackpackOpenPayload(INCEPTION_LINKED_CHILD_SLOT));
				return true;
			});
			waitForClient(() -> isClientInceptionChildMenu(fixture), "client Inception linked child Backpack menu");
			AutomationRuntime.runOnClient(() -> {
				ClientPacketDistributor.sendToServer(new BackpackOpenPayload());
				return true;
			});
			waitForClient(() -> isClientInceptionParentMenu(fixture), "client Inception parent Backpack return menu");
			moveClientInceptionLinkedChildToPlayerInventory(fixture);
			waitFor(() -> AutomationRuntime.runOnServer(player -> hasServerInceptionMovedLinkedChild(player, fixture))
					&& AutomationRuntime.runOnClient(() -> hasClientInceptionMovedLinkedChild(fixture)), "moved Inception linked child endpoint");
			AutomationRuntime.runOnClient(() -> {
				if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
					throw new IllegalStateException("Client Inception parent Backpack screen is unavailable for close");
				}
				screen.onClose();
				return true;
			});
			waitForInventoryMenu("client Inception child player inventory");
			AutomationRuntime.runOnClient(() -> {
				ClientPacketDistributor.sendToServer(new BackpackOpenPayload(INCEPTION_MOVED_CHILD_INVENTORY_SLOT, "", PlayerInventoryProvider.MAIN_INVENTORY));
				return true;
			});
			waitForClient(() -> isClientMovedInceptionLinkedChildMenu(fixture), "moved Inception linked child Backpack menu");
			openClientInceptionLinkedChildSettings(fixture);
			waitForClient(() -> isClientInceptionLinkedChildSettings(fixture), "moved Inception linked child settings menu");
			selectClientInceptionLinkedChildDisplaySlot();
			waitFor(() -> AutomationRuntime.runOnServer(player -> hasServerInceptionMovedLinkedChildItemDisplay(player, fixture))
					&& AutomationRuntime.runOnClient(() -> hasClientInceptionLinkedChildItemDisplay(fixture)),
					"moved Inception linked child item display setting");
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				return true;
			});
		}
	}

	private static InceptionLinkedChildFixture prepareClientInceptionLinkedChildPersistenceRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);

		ItemStack child = new ItemStack(ModItems.GOLD_BACKPACK.get());
		IBackpackWrapper childWrapper = BackpackWrapper.fromStack(child);
		childWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.NETHER_STAR, INCEPTION_LINKED_CHILD_MARKER_COUNT));
		childWrapper.getInventoryHandler().saveInventory();
		childWrapper.onContentsUpdated();
		assertTrue(LinkedStorageService.link(level, new ItemStack(ENDER_LINKER.get()), child), "Could not link the Inception child Backpack");
		LinkedStorageEndpointData childEndpoint = requireEndpoint(child, "Inception linked child");
		assertInceptionLinkedChildState(level, child, childEndpoint, "before insertion");

		ItemStack parent = new ItemStack(ModItems.NETHERITE_BACKPACK.get());
		IBackpackWrapper parentWrapper = BackpackWrapper.fromStack(parent);
		parentWrapper.getInventoryHandler();
		parentWrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.INCEPTION_UPGRADE.get()));
		parentWrapper.getUpgradeHandler().saveInventory();
		parentWrapper.getInventoryHandler().setStackInSlot(INCEPTION_LINKED_CHILD_SLOT, child);
		parentWrapper.getInventoryHandler().saveInventory();
		assertTrue(!parent.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT), "Inception parent unexpectedly became a linked endpoint");

		parentWrapper.getInventoryForUpgradeProcessing().size();
		assertInceptionLinkedChildState(level, parentWrapper.getInventoryHandler().getStackInSlot(INCEPTION_LINKED_CHILD_SLOT), childEndpoint,
				"after Inception initialization");
		player.getInventory().setItem(0, parent);
		player.getInventory().setSelectedSlot(0);
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		player.connection.send(new ClientboundSetHeldSlotPacket(0));
		return new InceptionLinkedChildFixture(childEndpoint, originalGameMode);
	}

	private static void assertInceptionLinkedChildState(ServerLevel level, ItemStack child, LinkedStorageEndpointData endpoint, String phase) {
		assertTrue(endpoint.equals(child.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)), "Inception linked child lost its endpoint " + phase);
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(level, child)
				.orElseThrow(() -> new IllegalStateException("Inception linked child did not resolve its canonical group " + phase));
		try {
			assertTrue(
					endpoint.groupId().equals(canonical.getContentsUuid().orElse(null))
							&& countItems(canonical, Items.NETHER_STAR) == INCEPTION_LINKED_CHILD_MARKER_COUNT,
					"Inception linked child lost its canonical group or marker contents " + phase);
		} finally {
			close(canonical);
		}
	}

	private static boolean isClientInceptionParentMenu(InceptionLinkedChildFixture fixture) {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			return false;
		}
		return menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK
				&& !(menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper) && fixture.childEndpoint().equals(menu.getStorageWrapper()
						.getInventoryHandler().getStackInSlot(INCEPTION_LINKED_CHILD_SLOT).get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
	}

	private static boolean isClientInceptionChildMenu(InceptionLinkedChildFixture fixture) {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			return false;
		}
		return menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK
				&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
				&& fixture.childEndpoint().equals(menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
				&& fixture.childEndpoint().groupId().equals(menu.getStorageWrapper().getContentsUuid().orElse(null))
				&& countItems(menu.getStorageWrapper(), Items.NETHER_STAR) == INCEPTION_LINKED_CHILD_MARKER_COUNT;
	}

	private static BackpackContainer requireClientInceptionMenu(BackpackContext.ContextType type, String action) {
		if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().screen instanceof BackpackScreen)
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu) || menu.getBackpackContext().getType() != type) {
			throw new IllegalStateException("Client Inception Backpack menu is unavailable for " + action);
		}
		return menu;
	}

	private static void moveClientInceptionLinkedChildToPlayerInventory(InceptionLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = requireClientInceptionMenu(BackpackContext.ContextType.ITEM_BACKPACK, "child move");
			Slot childSlot = menu.getSlot(INCEPTION_LINKED_CHILD_SLOT);
			assertTrue(fixture.childEndpoint().equals(childSlot.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Client Inception parent menu lost the linked child endpoint before move");
			Slot playerSlot = getClientInceptionMovedChildPlayerSlot(menu);
			assertTrue(playerSlot.getItem().isEmpty(), "Client Inception child player inventory destination was occupied");
			BackpackScreen screen = (BackpackScreen) Minecraft.getInstance().screen;
			clickSlot(screen, childSlot);
			assertTrue(fixture.childEndpoint().equals(menu.getCarried().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Client Inception child move did not pick up the linked endpoint");
			clickSlot(screen, playerSlot);
			return true;
		});
	}

	private static Slot getClientInceptionMovedChildPlayerSlot(BackpackContainer menu) {
		for (int slotIndex = menu.getNumberOfStorageInventorySlots(); slotIndex < menu.getInventorySlotsSize(); slotIndex++) {
			Slot slot = menu.getSlot(slotIndex);
			if (slot.getContainerSlot() == INCEPTION_MOVED_CHILD_INVENTORY_SLOT) {
				return slot;
			}
		}
		throw new IllegalStateException("Client Inception child player inventory slot is unavailable");
	}

	private static boolean hasServerInceptionMovedLinkedChild(ServerPlayer player, InceptionLinkedChildFixture fixture) {
		ItemStack parent = player.getInventory().getItem(0);
		ItemStack movedChild = player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT);
		return parent.is(ModItems.NETHERITE_BACKPACK.get())
				&& BackpackWrapper.fromStack(parent).getInventoryHandler().getStackInSlot(INCEPTION_LINKED_CHILD_SLOT).isEmpty()
				&& fixture.childEndpoint().equals(movedChild.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)) && player.getInventory().getNonEquipmentItems()
						.stream().filter(stack -> fixture.childEndpoint().equals(stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).count() == 1;
	}

	private static boolean hasClientInceptionMovedLinkedChild(InceptionLinkedChildFixture fixture) {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)
				|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
			return false;
		}
		ItemStack movedChild = Minecraft.getInstance().player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT);
		return menu.getSlot(INCEPTION_LINKED_CHILD_SLOT).getItem().isEmpty()
				&& fixture.childEndpoint().equals(movedChild.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
				&& Minecraft.getInstance().player.getInventory().getNonEquipmentItems().stream()
						.filter(stack -> fixture.childEndpoint().equals(stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).count() == 1;
	}

	private static boolean isClientMovedInceptionLinkedChildMenu(InceptionLinkedChildFixture fixture) {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			return false;
		}
		return menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK
				&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
				&& fixture.childEndpoint().equals(menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
	}

	private static void openClientInceptionLinkedChildSettings(InceptionLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen) || !isClientMovedInceptionLinkedChildMenu(fixture)) {
				throw new IllegalStateException("Client moved Inception linked child Backpack menu is unavailable for settings");
			}
			StorageSettingsTab settingsTab = findChild(screen, StorageSettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Client moved Inception linked child storage settings tab was unavailable"));
			assertTrue(settingsTab.mouseClicked(new MouseButtonEvent(settingsTab.getX() + 9, settingsTab.getY() + 12, new MouseButtonInfo(0, 0)), false),
					"Client moved Inception linked child storage settings tab did not handle the click");
			return true;
		});
	}

	private static boolean isClientInceptionLinkedChildSettings(InceptionLinkedChildFixture fixture) {
		return Minecraft.getInstance().screen instanceof BackpackSettingsScreen && Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.containerMenu instanceof BackpackSettingsContainerMenu menu
				&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
				&& fixture.childEndpoint().equals(menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
	}

	private static void selectClientInceptionLinkedChildDisplaySlot() {
		AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().screen instanceof BackpackSettingsScreen screen) || Minecraft.getInstance().player == null
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackSettingsContainerMenu menu)) {
				throw new IllegalStateException("Client moved Inception linked child settings screen is unavailable for item display selection");
			}
			ItemDisplaySettingsTab settingsTab = findChild(screen, ItemDisplaySettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Client moved Inception linked child item display settings tab was unavailable"));
			assertTrue(screen.mouseClicked(new MouseButtonEvent(settingsTab.getX() + 9, settingsTab.getY() + 12, new MouseButtonInfo(0, 0)), false),
					"Client moved Inception linked child item display settings tab did not handle the click");
			Slot storageSlot = menu.getStorageInventorySlots().getFirst();
			assertTrue(screen.mouseClicked(
					new MouseButtonEvent(screen.getLeftX() + storageSlot.x + 8, screen.getTopY() + storageSlot.y + 8, new MouseButtonInfo(0, 0)), false),
					"Client moved Inception linked child item display storage slot click was not handled");
			return true;
		});
	}

	private static boolean hasServerInceptionMovedLinkedChildItemDisplay(ServerPlayer player, InceptionLinkedChildFixture fixture) {
		if (!hasServerInceptionMovedLinkedChild(player, fixture)) {
			return false;
		}
		IBackpackWrapper canonical = BackpackLinkedStorageResolver
				.resolveCanonicalHost(player.level(), player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT)).orElseThrow();
		try {
			return canonical.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).getSlots().contains(0)
					&& canonical.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).getDisplaySide() == DisplaySide.FRONT;
		} finally {
			close(canonical);
		}
	}

	private static boolean hasClientInceptionLinkedChildItemDisplay(InceptionLinkedChildFixture fixture) {
		if (!isClientInceptionLinkedChildSettings(fixture)) {
			return false;
		}
		BackpackSettingsContainerMenu settingsMenu = (BackpackSettingsContainerMenu) Minecraft.getInstance().player.containerMenu;
		boolean[] matches = {false};
		settingsMenu.forEachSettingsContainer((name, container) -> {
			if (ItemDisplaySettingsCategory.NAME.equals(name) && container instanceof ItemDisplaySettingsContainer itemDisplaySettings) {
				matches[0] = itemDisplaySettings.isSlotSelected(0) && itemDisplaySettings.getDisplaySide() == DisplaySide.FRONT;
			}
		});
		return matches[0];
	}

	private static void runLinkedCarrierRelocationRegression() {
		Fixture fixture = AutomationRuntime.runOnServer(player -> setup(player, true));
		try {
			waitForClientLinker(fixture);
			AutomationRuntime.runOnServer(player -> selectHotbarSlot(player, PRIMARY_SLOT));
			waitForClientSelectedSlot(fixture.primaryEndpoint(), PRIMARY_SLOT, "carrier relocation primary selection");
			openItemBackpack(fixture);
			waitForMenu(fixture, false, true, "carrier relocation initial open");
			int initialMoveTick = moveTestItemIntoLinkedStorage(fixture);
			waitForServerTestItem(fixture);
			waitForClientTestItem(fixture, initialMoveTick);
			closeMenu(false, "carrier relocation initial close");

			moveLinkedCarrier(PRIMARY_SLOT, INTERACTION_SLOT, fixture.primaryEndpoint());
			waitForServer(
					player -> player.getInventory().getItem(PRIMARY_SLOT).isEmpty() && fixture.primaryEndpoint()
							.equals(player.getInventory().getItem(INTERACTION_SLOT).get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"server carrier relocation");
			AutomationRuntime.runOnServer(player -> selectHotbarSlot(player, INTERACTION_SLOT));
			waitForClientSelectedSlot(fixture.primaryEndpoint(), INTERACTION_SLOT, "carrier relocation selection");
			openItemBackpack(fixture);
			waitForMenu(fixture, false, true, "carrier relocation reopen");

			TankStorageSlots slots = AutomationRuntime.runOnClient(() -> {
				BackpackContainer menu = requireMenu(fixture, false);
				return new TankStorageSlots(menu.getSlot(TEST_STORAGE_SOURCE_SLOT), menu.getSlot(TEST_STORAGE_TARGET_SLOT));
			});
			for (int move = 0; move < 4; move++) {
				boolean itemInTarget = move % 2 == 0;
				int moveTick = moveRelocatedTankStorageItem(fixture, slots, itemInTarget);
				waitForRelocatedServerTankStorageItem(fixture, itemInTarget);
				waitForRelocatedClientTankStorageItem(fixture, slots, itemInTarget, moveTick);
			}
		} finally {
			AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().screen instanceof BackpackScreen screen) {
					screen.onClose();
				}
				return true;
			});
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea(player.level(), fixture.placedEndpointPos());
				return true;
			});
		}
	}

	private static void moveLinkedCarrier(int sourceInventorySlot, int targetInventorySlot, LinkedStorageEndpointData endpoint) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
				throw new IllegalStateException("Carrier relocation requires the client inventory menu");
			}
			InventoryMenu menu = minecraft.player.inventoryMenu;
			int sourceMenuSlot = inventoryMenuSlot(sourceInventorySlot);
			int targetMenuSlot = inventoryMenuSlot(targetInventorySlot);
			minecraft.setScreen(new InventoryScreen(minecraft.player));
			assertTrue(endpoint.equals(menu.getSlot(sourceMenuSlot).getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Carrier relocation source is not the linked Backpack endpoint");
			assertTrue(menu.getSlot(targetMenuSlot).getItem().isEmpty(), "Carrier relocation target hotbar slot is not empty");
			minecraft.gameMode.handleInventoryMouseClick(menu.containerId, sourceMenuSlot, 0, ClickType.PICKUP, minecraft.player);
			minecraft.gameMode.handleInventoryMouseClick(menu.containerId, targetMenuSlot, 0, ClickType.PICKUP, minecraft.player);
			assertTrue(
					menu.getSlot(sourceMenuSlot).getItem().isEmpty() && menu.getCarried().isEmpty()
							&& endpoint.equals(menu.getSlot(targetMenuSlot).getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Carrier relocation did not immediately move the linked Backpack endpoint");
			return true;
		});
	}

	private static int moveRelocatedTankStorageItem(Fixture fixture, TankStorageSlots slots, boolean itemInTarget) {
		return AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = requireMenu(fixture, false);
			assertTankStorageSlotIdentities(menu, slots);
			Slot source = itemInTarget ? slots.source() : slots.target();
			Slot target = itemInTarget ? slots.target() : slots.source();
			assertTrue(source.getItem().is(Items.NETHER_STAR) && target.getItem().isEmpty(), "Carrier relocation storage move started with wrong contents");
			clickSlot((BackpackScreen) Minecraft.getInstance().screen, source);
			clickSlot((BackpackScreen) Minecraft.getInstance().screen, target);
			assertTankStorageSlotIdentities(requireMenu(fixture, false), slots);
			assertTrue(target.getItem().is(Items.NETHER_STAR) && source.getItem().isEmpty() && menu.getCarried().isEmpty(),
					"Carrier relocation storage move did not immediately update the client");
			return Minecraft.getInstance().player.tickCount;
		});
	}

	private static void assertTankStorageSlotIdentities(BackpackContainer menu, TankStorageSlots slots) {
		assertTrue(menu.getSlot(TEST_STORAGE_SOURCE_SLOT) == slots.source() && menu.getSlot(TEST_STORAGE_TARGET_SLOT) == slots.target(),
				"Carrier relocation replaced Tank storage Slot instances");
	}

	private static void waitForRelocatedServerTankStorageItem(Fixture fixture, boolean itemInTarget) {
		waitForServer(player -> {
			IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.level(), player.getInventory().getItem(INTERACTION_SLOT))
					.orElseThrow();
			try {
				ItemStack item = canonical.getInventoryHandler().getStackInSlot(itemInTarget ? TEST_STORAGE_TARGET_SLOT : TEST_STORAGE_SOURCE_SLOT);
				ItemStack empty = canonical.getInventoryHandler().getStackInSlot(itemInTarget ? TEST_STORAGE_SOURCE_SLOT : TEST_STORAGE_TARGET_SLOT);
				return item.is(Items.NETHER_STAR) && empty.isEmpty();
			} finally {
				close(canonical);
			}
		}, "server relocated Tank storage move");
	}

	private static void waitForRelocatedClientTankStorageItem(Fixture fixture, TankStorageSlots slots, boolean itemInTarget, int moveTick) {
		waitForClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.player.tickCount < moveTick + 2) {
				return false;
			}
			BackpackContainer menu = requireMenu(fixture, false);
			assertTankStorageSlotIdentities(menu, slots);
			Slot item = itemInTarget ? slots.target() : slots.source();
			Slot empty = itemInTarget ? slots.source() : slots.target();
			return item.getItem().is(Items.NETHER_STAR) && empty.getItem().isEmpty() && menu.getCarried().isEmpty()
					&& menu.getStorageWrapper().getColumnsTaken() == fixture.canonicalProfile().tankColumns()
					&& ClientLinkedStorageBackpackContents.getColumnsTaken(fixture.groupId()).orElse(-1) == fixture.canonicalProfile().tankColumns();
		}, "client relocated Tank storage move");
	}

	private static void runNestedLinkedChildRegression() {
		NestedLinkedChildFixture fixture = AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::prepareNestedLinkedChildRegression);
		try {
			waitForNestedParentClientFixture(fixture);
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				assertTrue(minecraft.player != null && minecraft.gameMode != null && minecraft.player.getMainHandItem().isEmpty(),
						"Nested linked child parent interaction hand is unavailable");
				assertTrue(
						minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
								new BlockHitResult(Vec3.atCenterOf(fixture.parentPos()), Direction.UP, fixture.parentPos(), false)).consumesAction(),
						"Nested linked child parent Backpack did not open through the client interaction");
				return true;
			});
			waitForNestedParentMenu(fixture);
			AutomationRuntime.runOnClient(() -> {
				ClientPacketDistributor.sendToServer(new BackpackOpenPayload(0));
				return true;
			});
			waitForNestedLinkedChildMenu(fixture);
			insertNestedChildTankUpgrade();
			waitForNestedChildTankAndProjection(fixture);
			openAndConfigureNestedChildDisplay();
			waitForNestedChildDisplayProjection(fixture);
		} finally {
			AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().screen != null && Minecraft.getInstance().player != null) {
					Minecraft.getInstance().player.closeContainer();
				}
				return true;
			});
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea(player.level(), fixture.parentPos());
				return true;
			});
		}
	}

	private static NestedLinkedChildFixture prepareNestedLinkedChildRegression(ServerPlayer player) {
		ServerLevel level = player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos parentPos = player.blockPosition().relative(player.getDirection(), 4);
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		clearArea(level, parentPos);
		ItemStack child = new ItemStack(ModItems.GOLD_BACKPACK.get());
		IBackpackWrapper childWrapper = BackpackWrapper.fromStack(child);
		childWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
		childWrapper.getInventoryHandler().saveInventory();
		assertTrue(LinkedStorageService.link(level, new ItemStack(ENDER_LINKER.get()), child), "Could not link nested child Backpack");
		LinkedStorageEndpointData endpoint = requireEndpoint(child, "nested child");
		ItemStack parent = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		parent.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		IBackpackWrapper parentWrapper = BackpackWrapper.fromStack(parent);
		parentWrapper.getInventoryHandler().setStackInSlot(0, child);
		parentWrapper.getInventoryHandler().saveInventory();
		level.setBlockAndUpdate(parentPos, ModBlocks.DIAMOND_BACKPACK.get().defaultBlockState());
		requirePlacedBackpack(level, parentPos, "nested linked child parent").setBackpack(parent);
		player.getInventory().setItem(PRIMARY_SLOT, new ItemStack(ModItems.TANK_UPGRADE.get()));
		player.getInventory().setSelectedSlot(LINKER_SLOT);
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		player.connection.send(new ClientboundSetHeldSlotPacket(LINKER_SLOT));
		return new NestedLinkedChildFixture(parentPos, endpoint, originalGameMode);
	}

	private static void waitForNestedParentClientFixture(NestedLinkedChildFixture fixture) {
		waitForClient(
				() -> Minecraft.getInstance().player != null && Minecraft.getInstance().level != null
						&& Minecraft.getInstance().player.getMainHandItem().isEmpty()
						&& Minecraft.getInstance().level.getBlockEntity(fixture.parentPos()) instanceof BackpackBlockEntity,
				"nested linked child parent fixture");
	}

	private static void waitForNestedParentMenu(NestedLinkedChildFixture fixture) {
		waitForClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
				&& menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_BACKPACK
				&& menu.getBlockPosition().filter(fixture.parentPos()::equals).isPresent(), "nested linked child parent menu");
	}

	private static void waitForNestedLinkedChildMenu(NestedLinkedChildFixture fixture) {
		waitFor(() -> AutomationRuntime
				.runOnServer(player -> player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_SUB_BACKPACK
						&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper)
				&& AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_SUB_BACKPACK
						&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
						&& fixture.endpoint().equals(menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))),
				"nested linked child facade menu");
	}

	private static void insertNestedChildTankUpgrade() {
		AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = (BackpackContainer) Minecraft.getInstance().player.containerMenu;
			Slot playerTank = null;
			for (int slot = menu.getNumberOfStorageInventorySlots(); slot < menu.getInventorySlotsSize(); slot++) {
				if (menu.getSlot(slot).getItem().is(ModItems.TANK_UPGRADE.get())) {
					playerTank = menu.getSlot(slot);
					break;
				}
			}
			if (playerTank == null) {
				throw new IllegalStateException("Nested linked child Tank upgrade was not synchronized to the player inventory");
			}
			clickSlot((BackpackScreen) Minecraft.getInstance().screen, playerTank);
			clickSlot((BackpackScreen) Minecraft.getInstance().screen, menu.upgradeSlots.get(0));
			return true;
		});
	}

	private static void waitForNestedChildTankAndProjection(NestedLinkedChildFixture fixture) {
		waitFor(() -> AutomationRuntime.runOnServer(player -> {
			BackpackBlockEntity parent = requirePlacedBackpack(player.level(), fixture.parentPos(), "nested linked child parent");
			ItemStack physicalChild = parent.getBackpackWrapper().getInventoryHandler().getStackInSlot(0);
			IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.level(), physicalChild).orElseThrow();
			try {
				RenderData renderData = physicalChild.get(ModCoreDataComponents.RENDER_DATA);
				return fixture.endpoint().equals(physicalChild.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
						&& canonical.getUpgradeHandler().getStackInSlot(0).is(ModItems.TANK_UPGRADE.get()) && renderData != null
						&& renderData.equals(canonical.getRenderDataHandler().getData()) && physicalChild.getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0) > 0;
			} finally {
				close(canonical);
			}
		}) && AutomationRuntime.runOnClient(() -> {
			BackpackBlockEntity parent = (BackpackBlockEntity) Minecraft.getInstance().level.getBlockEntity(fixture.parentPos());
			ItemStack physicalChild = parent.getBackpackWrapper().getInventoryHandler().getStackInSlot(0);
			return physicalChild.getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0) > 0 && physicalChild.get(ModCoreDataComponents.RENDER_DATA) != null;
		}), "nested linked child Tank projection");
	}

	private static void openAndConfigureNestedChildDisplay() {
		AutomationRuntime.runOnClient(() -> {
			BackpackScreen screen = (BackpackScreen) Minecraft.getInstance().screen;
			StorageSettingsTab settingsTab = findChild(screen, StorageSettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Nested linked child settings tab is unavailable"));
			assertTrue(settingsTab.mouseClicked(new MouseButtonEvent(settingsTab.getX() + 9, settingsTab.getY() + 12, new MouseButtonInfo(0, 0)), false),
					"Nested linked child settings tab did not handle the client click");
			return true;
		});
		waitForClient(() -> Minecraft.getInstance().screen instanceof BackpackSettingsScreen && Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.containerMenu instanceof BackpackSettingsContainerMenu menu
				&& menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_SUB_BACKPACK, "nested linked child settings menu");
		AutomationRuntime.runOnClient(() -> {
			BackpackSettingsScreen screen = (BackpackSettingsScreen) Minecraft.getInstance().screen;
			BackpackSettingsContainerMenu menu = (BackpackSettingsContainerMenu) Minecraft.getInstance().player.containerMenu;
			ItemDisplaySettingsTab settingsTab = findChild(screen, ItemDisplaySettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Nested linked child item display settings tab is unavailable"));
			assertTrue(screen.mouseClicked(new MouseButtonEvent(settingsTab.getX() + 9, settingsTab.getY() + 12, new MouseButtonInfo(0, 0)), false),
					"Nested linked child item display settings tab did not handle the client click");
			Slot storageSlot = menu.getStorageInventorySlots().getFirst();
			assertTrue(screen.mouseClicked(
					new MouseButtonEvent(screen.getLeftX() + storageSlot.x + 8, screen.getTopY() + storageSlot.y + 8, new MouseButtonInfo(0, 0)), false),
					"Nested linked child item display settings did not handle the storage slot click");
			return true;
		});
	}

	private static void waitForNestedChildDisplayProjection(NestedLinkedChildFixture fixture) {
		waitFor(() -> AutomationRuntime.runOnServer(player -> {
			BackpackBlockEntity parent = requirePlacedBackpack(player.level(), fixture.parentPos(), "nested linked child parent");
			ItemStack physicalChild = parent.getBackpackWrapper().getInventoryHandler().getStackInSlot(0);
			IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.level(), physicalChild).orElseThrow();
			try {
				RenderData renderData = physicalChild.get(ModCoreDataComponents.RENDER_DATA);
				return canonical.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).getSlots().contains(0) && renderData != null
						&& renderData.equals(canonical.getRenderDataHandler().getData()) && !renderData.display().displayItems().isEmpty();
			} finally {
				close(canonical);
			}
		}) && AutomationRuntime.runOnClient(() -> {
			BackpackBlockEntity parent = (BackpackBlockEntity) Minecraft.getInstance().level.getBlockEntity(fixture.parentPos());
			RenderData renderData = parent.getBackpackWrapper().getInventoryHandler().getStackInSlot(0).get(ModCoreDataComponents.RENDER_DATA);
			return renderData != null && renderData.display().displayItems().stream().anyMatch(displayItem -> displayItem.item().is(Items.DIAMOND));
		}), "nested linked child display projection");
	}

	private static void runPreLinkedTankLayoutRegression() {
		Fixture fixture = AutomationRuntime.runOnServer(player -> setup(player, true));
		try {
			waitForClientLinker(fixture);
			AutomationRuntime.runOnServer(player -> selectHotbarSlot(player, PRIMARY_SLOT));
			waitForClientSelectedSlot(fixture.primaryEndpoint(), PRIMARY_SLOT, "pre-linked Tank primary selection");
			openItemBackpack(fixture);
			waitForMenu(fixture, false, true, "pre-linked Tank item primary open");
			waitForState(fixture, false, true, false, true, "pre-linked Tank item primary layout");
		} finally {
			AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().screen instanceof BackpackScreen screen) {
					screen.onClose();
				}
				return true;
			});
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea(player.level(), fixture.placedEndpointPos());
				return true;
			});
		}
	}

	private static Fixture setup(ServerPlayer player, boolean preLinkedTank) {
		ServerLevel level = player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos placedEndpointPos = player.blockPosition().relative(player.getDirection(), 2);
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		clearArea(level, placedEndpointPos);

		ItemStack primary = new ItemStack(ModItems.GOLD_BACKPACK.get());
		IBackpackWrapper ordinaryPrimary = new BackpackWrapper(primary);
		ordinaryPrimary.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
		if (preLinkedTank) {
			ordinaryPrimary.getUpgradeHandler().setStackInSlot(TANK_SLOT, new ItemStack(ModItems.TANK_UPGRADE.get()));
			ordinaryPrimary.setColumnsTaken(((IUpgradeItem<?>) ModItems.TANK_UPGRADE.get()).getInventoryColumnsTaken(), false);
			ordinaryPrimary.getUpgradeHandler().saveInventory();
		}
		ordinaryPrimary.getInventoryHandler().saveInventory();
		ordinaryPrimary.onContentsUpdated();
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		assertTrue(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not link the initialized item Backpack primary");
		LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "feedback item primary");
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(level, primary)
				.orElseThrow(() -> new IllegalStateException("Could not resolve the feedback canonical Backpack profile"));
		CanonicalProfile canonicalProfile;
		try {
			canonicalProfile = new CanonicalProfile(canonical.getInventoryHandler().size() + canonical.getColumnsTaken() * canonical.getNumberOfSlotRows(),
					canonical.getUpgradeHandler().size(), ((IUpgradeItem<?>) ModItems.TANK_UPGRADE.get()).getInventoryColumnsTaken(),
					canonical.getNumberOfSlotRows());
		} finally {
			close(canonical);
		}

		level.setBlockAndUpdate(placedEndpointPos, ModBlocks.GOLD_BACKPACK.get().defaultBlockState());
		BackpackBlockEntity placedEndpoint = requirePlacedBackpack(level, placedEndpointPos, "feedback placed secondary");
		ItemStack placedPhysical = new ItemStack(ModItems.GOLD_BACKPACK.get());
		UUID placedStorageUuid = UUID.randomUUID();
		placedPhysical.set(ModCoreDataComponents.STORAGE_UUID, placedStorageUuid);
		placedEndpoint.setBackpack(placedPhysical);
		BackpackStorage.get().setBackpackContents(placedStorageUuid, new ContainerContents());
		player.getInventory().setItem(PRIMARY_SLOT, primary);
		player.getInventory().setItem(LINKER_SLOT, linker);
		player.getInventory().setItem(STASH_SLOT, new ItemStack(Items.EMERALD, STASH_COUNT));
		player.getInventory().setItem(STASH_SLOT + 1, new ItemStack(ModItems.TANK_UPGRADE.get()));
		player.getInventory().setItem(TEST_ITEM_SLOT, new ItemStack(Items.NETHER_STAR));
		player.getInventory().setSelectedSlot(LINKER_SLOT);
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		player.connection.send(new ClientboundSetHeldSlotPacket(LINKER_SLOT));
		return new Fixture(placedEndpointPos, primaryEndpoint, canonicalProfile, originalGameMode);
	}

	private static void waitForClientLinker(Fixture fixture) {
		waitForClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().level != null
				&& Minecraft.getInstance().player.getInventory().getSelectedSlot() == LINKER_SLOT
				&& Minecraft.getInstance().player.getMainHandItem().is(ENDER_LINKER.get())
				&& fixture.primaryEndpoint()
						.equals(Minecraft.getInstance().player.getInventory().getItem(PRIMARY_SLOT).get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
				&& Minecraft.getInstance().level.getBlockEntity(fixture.placedEndpointPos()) instanceof BackpackBlockEntity, "linked-storage fixture");
	}

	private static void linkPlacedEndpoint(Fixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.getMainHandItem().is(ENDER_LINKER.get())) {
			throw new IllegalStateException("Client feedback linker is unavailable");
		}
		EnderLinkerTargetData target = minecraft.player.getMainHandItem().get(ModCoreDataComponents.ENDER_LINKER_TARGET);
		assertTrue(target != null && target.groupId().equals(fixture.groupId()),
				"Client feedback linker target does not match the primary linked-storage group");
		InteractionResult result = minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(fixture.placedEndpointPos()), Direction.UP, fixture.placedEndpointPos(), false));
		assertTrue(result.consumesAction(), "Client feedback linker use did not consume the placed Backpack interaction");
	}

	private static void waitForServerPlacedEndpoint(Fixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> hasServerPlacedEndpoint(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for server placed linked Backpack endpoint");
	}

	private static boolean hasServerPlacedEndpoint(ServerPlayer player, Fixture fixture) {
		LinkedStorageEndpointData endpoint = requirePlacedBackpack(player.level(), fixture.placedEndpointPos(), "feedback placed secondary")
				.getBackpackWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		return endpoint != null && endpoint.groupId().equals(fixture.groupId()) && !endpoint.endpointId().equals(fixture.primaryEndpoint().endpointId());
	}

	private static void waitForClientPlacedEndpoint(Fixture fixture) {
		waitForClient(
				() -> Minecraft.getInstance().level != null
						&& Minecraft.getInstance().level.getBlockEntity(fixture.placedEndpointPos()) instanceof BackpackBlockEntity backpack
						&& backpack.getBackpackWrapper().getBackpack()
								.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) instanceof LinkedStorageEndpointData endpoint
						&& endpoint.groupId().equals(fixture.groupId()) && !endpoint.endpointId().equals(fixture.primaryEndpoint().endpointId()),
				"client placed linked Backpack endpoint");
	}

	private static Boolean selectHotbarSlot(ServerPlayer player, int slot) {
		player.getInventory().setSelectedSlot(slot);
		player.connection.send(new ClientboundSetHeldSlotPacket(slot));
		return true;
	}

	private static void waitForClientSelectedSlot(LinkedStorageEndpointData primaryEndpoint, int slot, String description) {
		waitForClient(
				() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getInventory().getSelectedSlot() == slot
						&& (primaryEndpoint == null
								|| primaryEndpoint.equals(Minecraft.getInstance().player.getMainHandItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))),
				description);
	}

	private static void openItemBackpack(Fixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null
					|| !fixture.primaryEndpoint().equals(minecraft.player.getMainHandItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))) {
				throw new IllegalStateException("Client feedback item Backpack primary is unavailable");
			}
			assertTrue(minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND).consumesAction(),
					"Client feedback item Backpack use did not consume the interaction");
			return true;
		});
	}

	private static void openPlacedBackpack(Fixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.getMainHandItem().isEmpty()) {
				throw new IllegalStateException("Client feedback placed Backpack interaction hand is unavailable");
			}
			assertTrue(minecraft.gameMode
					.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(fixture.placedEndpointPos()), Direction.UP, fixture.placedEndpointPos(), false))
					.consumesAction(), "Client feedback placed Backpack use did not consume the interaction");
			return true;
		});
	}

	private static void waitForMenu(Fixture fixture, boolean placed, boolean tankPresent, String description) {
		waitForClient(() -> getMenuProfile(fixture, placed).map(profile -> matchesCanonical(profile, fixture.canonicalProfile(), tankPresent)).orElse(false),
				description);
	}

	private static Optional<StorageProfile> getMenuProfile(Fixture fixture, boolean placed) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof BackpackScreen) || minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
				|| !(menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper)
				|| placed != menu.getBlockPosition().filter(fixture.placedEndpointPos()::equals).isPresent()) {
			return Optional.empty();
		}
		LinkedStorageEndpointData endpoint = menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null || !endpoint.groupId().equals(fixture.groupId())
				|| !placed && !endpoint.endpointId().equals(fixture.primaryEndpoint().endpointId())) {
			return Optional.empty();
		}
		return Optional.of(snapshot(menu.getStorageWrapper(), menu.getNumberOfStorageInventorySlots()));
	}

	private static void closeMenu(boolean placed, String description) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof BackpackScreen screen) || minecraft.player == null
					|| !(minecraft.player.containerMenu instanceof BackpackContainer menu) || placed != menu.getBlockPosition().isPresent()) {
				throw new IllegalStateException("Client feedback " + description + " Backpack screen was not open");
			}
			screen.onClose();
			return true;
		});
		waitForInventoryMenu(description);
	}

	private static void stashIntoBackpack(Fixture fixture) {
		waitForClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
				return false;
			}
			InventoryMenu menu = minecraft.player.inventoryMenu;
			int primaryMenuSlot = inventoryMenuSlot(PRIMARY_SLOT);
			int stashMenuSlot = inventoryMenuSlot(STASH_SLOT);
			assertTrue(fixture.primaryEndpoint().equals(menu.getSlot(primaryMenuSlot).getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Client feedback primary inventory stack was not the linked endpoint");
			assertTrue(menu.getSlot(stashMenuSlot).getItem().is(Items.EMERALD) && menu.getSlot(stashMenuSlot).getItem().getCount() == STASH_COUNT,
					"Client feedback stash stack was not synchronized");
			assertTrue(menu.getCarried().isEmpty(), "Client feedback inventory started with a carried stack");
			minecraft.setScreen(new InventoryScreen(minecraft.player));
			minecraft.gameMode.handleInventoryMouseClick(menu.containerId, stashMenuSlot, 0, ClickType.PICKUP, minecraft.player);
			assertTrue(menu.getCarried().is(Items.EMERALD) && menu.getCarried().getCount() == STASH_COUNT,
					"Client feedback inventory click did not pick up the stash stack");
			minecraft.gameMode.handleInventoryMouseClick(menu.containerId, primaryMenuSlot, 1, ClickType.PICKUP, minecraft.player);
			return true;
		}, "InventoryScreen stash click");
	}

	private static int inventoryMenuSlot(int inventorySlot) {
		return inventorySlot < 9 ? InventoryMenu.USE_ROW_SLOT_START + inventorySlot : InventoryMenu.INV_SLOT_START + inventorySlot - 9;
	}

	private static void waitForServerStash(Fixture fixture, long revisionBeforeStash) {
		waitForServer(player -> {
			IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.level(), player.getInventory().getItem(PRIMARY_SLOT))
					.orElseThrow();
			try {
				BackpackBlockEntity placed = requirePlacedBackpack(player.level(), fixture.placedEndpointPos(), "feedback placed secondary");
				return countItems(canonical, Items.EMERALD) == STASH_COUNT && countItems(placed.getBackpackWrapper(), Items.EMERALD) == STASH_COUNT
						&& countPlayerItems(player, Items.EMERALD) == 0 && player.containerMenu.getCarried().isEmpty()
						&& LinkedStorageGroupsSavedData.get(player.level()).manager().getRevision(fixture.groupId()) > revisionBeforeStash;
			} finally {
				close(canonical);
			}
		}, "server inventory stash");
	}

	private static void waitForClientStash(Fixture fixture) {
		waitForClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.player != null && minecraft.screen instanceof InventoryScreen && minecraft.player.containerMenu == minecraft.player.inventoryMenu
					&& fixture.primaryEndpoint()
							.equals(minecraft.player.inventoryMenu.getSlot(inventoryMenuSlot(PRIMARY_SLOT)).getItem()
									.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& minecraft.player.inventoryMenu.getSlot(inventoryMenuSlot(STASH_SLOT)).getItem().isEmpty()
					&& minecraft.player.inventoryMenu.getCarried().isEmpty();
		}, "client inventory stash");
	}

	private static void waitForStashView(Fixture fixture) {
		waitForClient(() -> getMenuProfile(fixture, false).map(
				profile -> countItems(((BackpackContainer) Minecraft.getInstance().player.containerMenu).getStorageWrapper(), Items.EMERALD) == STASH_COUNT)
				.orElse(false), "linked Backpack stash view");
	}

	private static int moveTestItemIntoLinkedStorage(Fixture fixture) {
		return AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			BackpackContainer menu = requireMenu(fixture, false);
			Slot targetSlot = menu.getSlot(TEST_STORAGE_SOURCE_SLOT);
			assertTrue(targetSlot.getItem().isEmpty(), "Client feedback test linked storage slot was not empty");
			for (int slotIndex = menu.getNumberOfStorageInventorySlots(); slotIndex < menu.getInventorySlotsSize(); slotIndex++) {
				Slot playerSlot = menu.getSlot(slotIndex);
				if (playerSlot.getItem().is(Items.NETHER_STAR)) {
					BackpackScreen screen = (BackpackScreen) minecraft.screen;
					clickSlot(screen, playerSlot);
					assertTrue(menu.getCarried().is(Items.NETHER_STAR) && menu.getCarried().getCount() == 1,
							"Client feedback test item click did not pick up the Nether Star");
					clickSlot((BackpackScreen) minecraft.screen, targetSlot);
					assertTrue(targetSlot.getItem().is(Items.NETHER_STAR) && targetSlot.getItem().getCount() == 1 && menu.getCarried().isEmpty(),
							"Client feedback first linked-storage item change did not remain visible");
					return minecraft.player.tickCount;
				}
			}
			throw new IllegalStateException("Client feedback test item was not available in the Backpack player inventory");
		});
	}

	private static void waitForServerTestItem(Fixture fixture) {
		waitForServer(player -> {
			IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.level(), player.getInventory().getItem(PRIMARY_SLOT))
					.orElseThrow();
			try {
				ItemStack target = canonical.getInventoryHandler().getStackInSlot(TEST_STORAGE_SOURCE_SLOT);
				return target.is(Items.NETHER_STAR) && target.getCount() == 1 && countItems(canonical, Items.NETHER_STAR) == 1
						&& countPlayerItems(player, Items.NETHER_STAR) == 0 && player.containerMenu.getCarried().isEmpty();
			} finally {
				close(canonical);
			}
		}, "server first linked-storage item change");
	}

	private static void waitForClientTestItem(Fixture fixture, int firstItemChangeTick) {
		waitForClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.player.tickCount < firstItemChangeTick + 2 || getMenuProfile(fixture, false).isEmpty()) {
				return false;
			}
			BackpackContainer menu = (BackpackContainer) minecraft.player.containerMenu;
			Slot target = menu.getSlot(TEST_STORAGE_SOURCE_SLOT);
			return target.getItem().is(Items.NETHER_STAR) && target.getItem().getCount() == 1 && menu.getCarried().isEmpty();
		}, "client first linked-storage item change");
	}

	private static int moveTestItemWithTank(Fixture fixture) {
		return AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			BackpackContainer menu = requireMenu(fixture, false);
			Slot sourceSlot = menu.getSlot(TEST_STORAGE_SOURCE_SLOT);
			Slot targetSlot = menu.getSlot(TEST_STORAGE_TARGET_SLOT);
			assertTrue(sourceSlot.getItem().is(Items.NETHER_STAR) && sourceSlot.getItem().getCount() == 1,
					"Client feedback Tank test source slot did not contain the Nether Star");
			assertTrue(targetSlot.getItem().isEmpty(), "Client feedback Tank test target linked storage slot was not empty");
			BackpackScreen screen = (BackpackScreen) minecraft.screen;
			clickSlot(screen, sourceSlot);
			assertTrue(menu.getCarried().is(Items.NETHER_STAR) && menu.getCarried().getCount() == 1,
					"Client feedback Tank test item click did not pick up the Nether Star");
			clickSlot((BackpackScreen) minecraft.screen, targetSlot);
			assertTrue(targetSlot.getItem().is(Items.NETHER_STAR) && targetSlot.getItem().getCount() == 1 && menu.getCarried().isEmpty(),
					"Client feedback Tank test linked-storage item change did not remain visible");
			return minecraft.player.tickCount;
		});
	}

	private static void waitForServerTestItemWithTank(Fixture fixture) {
		waitForServer(player -> {
			IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.level(), player.getInventory().getItem(PRIMARY_SLOT))
					.orElseThrow();
			try {
				ItemStack target = canonical.getInventoryHandler().getStackInSlot(TEST_STORAGE_TARGET_SLOT);
				return target.is(Items.NETHER_STAR) && target.getCount() == 1 && countItems(canonical, Items.NETHER_STAR) == 1
						&& countPlayerItems(player, Items.NETHER_STAR) == 0 && player.containerMenu.getCarried().isEmpty();
			} finally {
				close(canonical);
			}
		}, "server Tank linked-storage item change");
	}

	private static void waitForClientTestItemWithTank(Fixture fixture, int tankItemChangeTick) {
		waitForClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.player.tickCount < tankItemChangeTick + 2
					|| getMenuProfile(fixture, false).filter(profile -> matchesCanonical(profile, fixture.canonicalProfile(), true)).isEmpty()
					|| ClientLinkedStorageBackpackContents.getColumnsTaken(fixture.groupId()).orElse(-1) != fixture.canonicalProfile().tankColumns()) {
				return false;
			}
			BackpackContainer menu = (BackpackContainer) minecraft.player.containerMenu;
			Slot target = menu.getSlot(TEST_STORAGE_TARGET_SLOT);
			return target.getItem().is(Items.NETHER_STAR) && target.getItem().getCount() == 1 && menu.getCarried().isEmpty();
		}, "client Tank linked-storage item change");
	}

	private static void toggleTankUpgrade(Fixture fixture, boolean placed, String description) {
		AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = requireMenu(fixture, placed);
			Slot upgradeSlot = menu.upgradeSlots.get(TANK_SLOT);
			BackpackScreen screen = (BackpackScreen) Minecraft.getInstance().screen;
			assertTrue(menu.getCarried().isEmpty(), "Client feedback " + description + " started with a carried stack");
			if (upgradeSlot.getItem().is(ModItems.TANK_UPGRADE.get())) {
				clickSlot(screen, upgradeSlot);
				return true;
			}
			for (int slotIndex = menu.getNumberOfStorageInventorySlots(); slotIndex < menu.getInventorySlotsSize(); slotIndex++) {
				Slot slot = menu.getSlot(slotIndex);
				if (slot.getItem().is(ModItems.TANK_UPGRADE.get())) {
					clickSlot(screen, slot);
					menu = requireMenu(fixture, placed);
					assertTrue(menu.getCarried().is(ModItems.TANK_UPGRADE.get()), "Client feedback " + description + " did not carry the Tank upgrade");
					clickSlot((BackpackScreen) Minecraft.getInstance().screen, menu.upgradeSlots.get(TANK_SLOT));
					return true;
				}
			}
			throw new IllegalStateException("Client feedback Tank upgrade was not returned to the player inventory");
		});
	}

	private static BackpackContainer requireMenu(Fixture fixture, boolean placed) {
		return getMenuProfile(fixture, placed).map(profile -> (BackpackContainer) Minecraft.getInstance().player.containerMenu)
				.orElseThrow(() -> new IllegalStateException("Client feedback linked Backpack menu is unavailable"));
	}

	private static void roundTripSettings() {
		AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
				throw new IllegalStateException("Client feedback item Backpack screen was not open for settings");
			}
			StorageSettingsTab tab = findChild(screen, StorageSettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Client feedback storage settings tab was unavailable"));
			assertTrue(tab.mouseClicked(new MouseButtonEvent(tab.getX() + 9, tab.getY() + 12, new MouseButtonInfo(0, 0)), false),
					"Client feedback storage settings tab did not handle the click");
			return true;
		});
		waitFor(() -> AutomationRuntime
				.runOnServer(player -> player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
						&& settingsMenu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper)
				&& AutomationRuntime
						.runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackSettingsScreen && Minecraft.getInstance().player != null
								&& Minecraft.getInstance().player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
								&& settingsMenu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper),
				"Backpack settings screen");
		AutomationRuntime.runOnClient(() -> {
			((BackpackSettingsScreen) Minecraft.getInstance().screen).onClose();
			return true;
		});
		waitForInventoryMenu("Backpack settings screen");
	}

	private static void waitForState(Fixture fixture, boolean placed, boolean tankPresent, boolean tankCarried, boolean requireCurrentCache,
			String description) {
		waitFor(() -> {
			FeedbackServerSnapshot server = AutomationRuntime.runOnServer(player -> getServerSnapshot(player, fixture));
			Optional<FeedbackClientSnapshot> client = AutomationRuntime.runOnClient(() -> getClientSnapshot(fixture, placed));
			return matchesServerSnapshot(server, fixture, placed, tankPresent) && client
					.filter(snapshot -> matchesClientSnapshot(snapshot, server, fixture, placed, tankPresent, tankCarried, requireCurrentCache)).isPresent();
		}, description);
	}

	private static FeedbackServerSnapshot getServerSnapshot(ServerPlayer player, Fixture fixture) {
		ItemStack primary = player.getInventory().getItem(PRIMARY_SLOT);
		BackpackBlockEntity placed = requirePlacedBackpack(player.level(), fixture.placedEndpointPos(), "feedback placed secondary");
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.level(), primary)
				.orElseThrow(() -> new IllegalStateException("Could not resolve the feedback canonical linked Backpack"));
		try {
			return new FeedbackServerSnapshot(snapshot(canonical), snapshot(new BackpackWrapper(primary)),
					snapshot(new BackpackWrapper(placed.getBackpackWrapper().getBackpack())),
					LinkedStorageGroupsSavedData.get(player.level()).manager().getRevision(fixture.groupId()));
		} finally {
			close(canonical);
		}
	}

	private static Optional<FeedbackClientSnapshot> getClientSnapshot(Fixture fixture, boolean placed) {
		Optional<StorageProfile> menuProfile = getMenuProfile(fixture, placed);
		Minecraft minecraft = Minecraft.getInstance();
		if (menuProfile.isEmpty() || minecraft.player == null || minecraft.level == null
				|| !(minecraft.level.getBlockEntity(fixture.placedEndpointPos()) instanceof BackpackBlockEntity endpoint)) {
			return Optional.empty();
		}
		return Optional.of(new FeedbackClientSnapshot(menuProfile.get(), snapshot(new BackpackWrapper(minecraft.player.getInventory().getItem(PRIMARY_SLOT))),
				snapshot(new BackpackWrapper(endpoint.getBackpackWrapper().getBackpack())),
				ClientLinkedStorageBackpackContents.getRevision(fixture.groupId()).orElse(-1L),
				ClientLinkedStorageBackpackContents.getStorageSize(fixture.groupId()).map(ClientLinkedStorageBackpackContents.StorageSize::inventorySlots)
						.orElse(-1),
				ClientLinkedStorageBackpackContents.getStorageSize(fixture.groupId()).map(ClientLinkedStorageBackpackContents.StorageSize::upgradeSlots)
						.orElse(-1),
				ClientLinkedStorageBackpackContents.getColumnsTaken(fixture.groupId()).orElse(-1),
				((BackpackContainer) minecraft.player.containerMenu).getCarried().is(ModItems.TANK_UPGRADE.get())));
	}

	private static boolean matchesServerSnapshot(FeedbackServerSnapshot snapshot, Fixture fixture, boolean placed, boolean tankPresent) {
		StorageProfile physical = placed ? snapshot.placedPhysical() : snapshot.primaryPhysical();
		return matchesCanonical(snapshot.canonical(), fixture.canonicalProfile(), tankPresent)
				&& matchesPhysical(physical, fixture.canonicalProfile(), tankPresent) && physical.renderData().equals(snapshot.canonical().renderData());
	}

	private static boolean matchesClientSnapshot(FeedbackClientSnapshot snapshot, FeedbackServerSnapshot server, Fixture fixture, boolean placed,
			boolean tankPresent, boolean tankCarried, boolean requireCurrentCache) {
		StorageProfile physical = placed ? snapshot.placedPhysical() : snapshot.primaryPhysical();
		StorageProfile serverPhysical = placed ? server.placedPhysical() : server.primaryPhysical();
		CanonicalProfile canonical = fixture.canonicalProfile();
		return matchesCanonical(snapshot.menuProfile(), canonical, tankPresent) && matchesPhysical(physical, canonical, tankPresent)
				&& snapshot.tankCarried() == tankCarried
				&& (!requireCurrentCache || snapshot.cachedRevision() >= server.revision() && snapshot.cachedInventorySlots() == canonical.baseStorageSlots()
						&& snapshot.cachedUpgradeSlots() == canonical.upgradeSlots()
						&& snapshot.cachedColumnsTaken() == expectedColumns(canonical, tankPresent))
				&& snapshot.menuProfile().renderData().equals(server.canonical().renderData()) && physical.renderData().equals(serverPhysical.renderData());
	}

	private static boolean matchesCanonical(StorageProfile profile, CanonicalProfile canonical, boolean tankPresent) {
		return matchesLayout(profile, canonical, tankPresent) && profile.tankInUpgradeSlot() == tankPresent;
	}

	private static boolean matchesPhysical(StorageProfile profile, CanonicalProfile canonical, boolean tankPresent) {
		return matchesLayout(profile, canonical, tankPresent) && !profile.tankInUpgradeSlot();
	}

	private static boolean matchesLayout(StorageProfile profile, CanonicalProfile canonical, boolean tankPresent) {
		int columns = expectedColumns(canonical, tankPresent);
		int slots = canonical.baseStorageSlots() - columns * canonical.rows();
		return profile.visibleStorageSlots() == slots && profile.inventoryHandlerSlots() == slots && profile.upgradeSlots() == canonical.upgradeSlots()
				&& profile.columnsTaken() == columns && profile.rows() == canonical.rows();
	}

	private static int expectedColumns(CanonicalProfile canonical, boolean tankPresent) {
		return tankPresent ? canonical.tankColumns() : 0;
	}

	private static StorageProfile snapshot(IBackpackWrapper wrapper) {
		return snapshot(wrapper, wrapper.getInventoryHandler().size());
	}

	private static StorageProfile snapshot(IBackpackWrapper wrapper, int visibleStorageSlots) {
		return new StorageProfile(visibleStorageSlots, wrapper.getInventoryHandler().size(), wrapper.getUpgradeHandler().size(), wrapper.getColumnsTaken(),
				wrapper.getNumberOfSlotRows(), wrapper.getUpgradeHandler().getStackInSlot(TANK_SLOT).is(ModItems.TANK_UPGRADE.get()),
				wrapper.getRenderDataHandler().getData().copy());
	}

	private static void clickSlot(BackpackScreen screen, Slot slot) {
		double x = screen.getGuiLeft() + slot.x + 8.0;
		double y = screen.getGuiTop() + slot.y + 8.0;
		MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
		if (!screen.mouseClicked(event, false)) {
			throw new IllegalStateException("Linked Backpack slot click was not handled");
		}
		screen.mouseReleased(event);
	}

	private static BackpackBlockEntity requirePlacedBackpack(ServerLevel level, BlockPos pos, String name) {
		return level.getBlockEntity(pos, ModBlocks.BACKPACK_TILE_TYPE.get())
				.orElseThrow(() -> new IllegalStateException(name + " linked Backpack was not placed"));
	}

	private static void clearArea(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
	}

	private static void close(IBackpackWrapper backpack) {
		if (backpack instanceof LinkedStorageBackpackWrapper linkedStorageBackpack) {
			linkedStorageBackpack.close();
		}
	}

	private static int countItems(IBackpackWrapper backpack, Item item) {
		int count = 0;
		for (int slot = 0; slot < backpack.getInventoryHandler().size(); slot++) {
			ItemStack stack = backpack.getInventoryHandler().getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int countPlayerItems(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static <T extends GuiEventListener> Optional<T> findChild(GuiEventListener parent, Class<T> childClass) {
		if (childClass.isInstance(parent)) {
			return Optional.of(childClass.cast(parent));
		}
		if (parent instanceof ContainerEventHandler container) {
			for (GuiEventListener child : container.children()) {
				Optional<T> found = findChild(child, childClass);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}

	private static LinkedStorageEndpointData requireEndpoint(ItemStack stack, String name) {
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null) {
			throw new IllegalStateException(name + " endpoint was not created");
		}
		return endpoint;
	}

	private static void waitForInventoryMenu(String description) {
		waitFor(() -> AutomationRuntime.runOnServer(player -> player.containerMenu == player.inventoryMenu) && AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.player != null && minecraft.player.containerMenu == minecraft.player.inventoryMenu && !(minecraft.screen instanceof BackpackScreen)
					&& !(minecraft.screen instanceof BackpackSettingsScreen);
		}), description + " close");
	}

	private static void waitForServer(java.util.function.Function<ServerPlayer, Boolean> condition, String description) {
		waitFor(() -> AutomationRuntime.runOnServer(condition), description);
	}

	private static void waitForClient(java.util.function.BooleanSupplier condition, String description) {
		waitFor(() -> AutomationRuntime.runOnClient(condition::getAsBoolean), description);
	}

	private static void waitFor(java.util.function.BooleanSupplier condition, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (condition.getAsBoolean()) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client feedback " + description);
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted waiting for client feedback linked Backpack state", e);
		}
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}

	private static String quote(String value) {
		return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
	}

	private static void send(HttpExchange exchange, String response) throws IOException {
		byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(bytes);
		}
	}

	private record Fixture(BlockPos placedEndpointPos, LinkedStorageEndpointData primaryEndpoint, CanonicalProfile canonicalProfile,
			GameType originalGameMode) {
		private UUID groupId() {
			return primaryEndpoint.groupId();
		}
	}

	private record NestedLinkedChildFixture(BlockPos parentPos, LinkedStorageEndpointData endpoint, GameType originalGameMode) {
	}

	private record InceptionLinkedChildFixture(LinkedStorageEndpointData childEndpoint, GameType originalGameMode) {
	}

	private record TankStorageSlots(Slot source, Slot target) {
	}

	private record CanonicalProfile(int baseStorageSlots, int upgradeSlots, int tankColumns, int rows) {
	}

	private record StorageProfile(int visibleStorageSlots, int inventoryHandlerSlots, int upgradeSlots, int columnsTaken, int rows, boolean tankInUpgradeSlot,
			RenderData renderData) {
	}

	private record FeedbackServerSnapshot(StorageProfile canonical, StorageProfile primaryPhysical, StorageProfile placedPhysical, long revision) {
	}

	private record FeedbackClientSnapshot(StorageProfile menuProfile, StorageProfile primaryPhysical, StorageProfile placedPhysical, long cachedRevision,
			int cachedInventorySlots, int cachedUpgradeSlots, int cachedColumnsTaken, boolean tankCarried) {
	}
}
