package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.ClientLinkedStorageBackpackContents;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.LinkedStorageBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackItemDisplaySettingsPreviewProvider;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackSettingsScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerEndpointRecipe;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerTargetData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.IJukeboxPlaybackLocationProvider;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

public final class BackpackLinkedStorageRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);
	private static final int MUTATION_SLOT = 1;
	private static final int EXTERNAL_MUTATION_SLOT = 2;
	private static final int CARRIED_MUTATION_SLOT = 3;
	private static final int CLIENT_FEEDBACK_PRIMARY_SLOT = 0;
	private static final int CLIENT_FEEDBACK_LINKER_SLOT = 1;
	private static final int CLIENT_FEEDBACK_INTERACTION_SLOT = 2;
	private static final int CLIENT_FEEDBACK_STASH_SLOT = 9;
	private static final int CLIENT_FEEDBACK_STASH_COUNT = 13;
	private static final int CLIENT_FEEDBACK_TANK_SLOT = 0;
	private static final int INCEPTION_LINKED_CHILD_SLOT = 0;
	private static final int INCEPTION_LINKED_CHILD_MARKER_COUNT = 5;
	private static final int INCEPTION_MOVED_CHILD_INVENTORY_SLOT = 1;
	private static final Component PRIMARY_NAME = Component.literal("Primary Linked Storage");
	private static final Component SECONDARY_NAME = Component.literal("Secondary Linked Storage");
	private static final Component CLIENT_PRIMARY_NAME = Component.literal("Client Linked Title");
	private static final int PRIMARY_MAIN_COLOR = 0xFF225588;
	private static final int PRIMARY_ACCENT_COLOR = 0xFFE2A100;
	private static final int SECONDARY_MAIN_COLOR = 0xFF5C1F78;
	private static final int SECONDARY_ACCENT_COLOR = 0xFF18A8A8;

	private BackpackLinkedStorageRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackLinkedStorageRegression::run);
	}

	private static String run() {
		String result = AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::run);
		if (!result.startsWith("{\"ok\":true")) {
			return result;
		}

		boolean clientCanonicalStorageSize = false;
		boolean inceptionLinkedChildPersistence = false;
		boolean inceptionMovedLinkedChildDoesNotDuplicate = false;
		boolean linkedTankColumnSlotSync = false;
		boolean linkedStorageFeedbackUiSync = false;
		boolean linkedPrimaryTickDispatch = false;
		boolean clientCreativePlacementCopiesSecondaryEndpoint = false;
		boolean clientCraftTakeActivatesLinkerClaim = false;
		boolean clientCraftQuickMoveActivatesLinkerClaim = false;
		boolean clientCraftingUpgradeTakeActivatesLinkerClaim = false;
		boolean clientCraftingUpgradeQuickMoveActivatesLinkerClaim = false;
		boolean clientSecondaryCraftTakeActivatesEndpointClaim = false;
		boolean clientSecondaryCraftQuickMoveActivatesEndpointClaim = false;
		boolean clientCraftingUpgradeSecondaryTakeActivatesEndpointClaim = false;
		boolean clientCraftingUpgradeSecondaryQuickMoveActivatesEndpointClaim = false;
		boolean clientRejectedSecondaryCraftKeepsInputs = false;
		boolean clientFailedQuickCraftCanRetry = false;
		boolean clientPendingLinkerUseResolvesClaim = false;
		try {
			runClientCanonicalStorageSizeRegression();
			clientCanonicalStorageSize = true;
			runClientEndpointCraftFinalizationRegression(false);
			clientCraftTakeActivatesLinkerClaim = true;
			runClientEndpointCraftFinalizationRegression(true);
			clientCraftQuickMoveActivatesLinkerClaim = true;
			runClientCraftingUpgradeFinalizationRegression(false);
			clientCraftingUpgradeTakeActivatesLinkerClaim = true;
			runClientCraftingUpgradeFinalizationRegression(true);
			clientCraftingUpgradeQuickMoveActivatesLinkerClaim = true;
			runClientSecondaryCraftRegression(false);
			clientSecondaryCraftTakeActivatesEndpointClaim = true;
			runClientSecondaryCraftRegression(true);
			clientSecondaryCraftQuickMoveActivatesEndpointClaim = true;
			runClientCraftingUpgradeSecondaryCraftRegression(false);
			clientCraftingUpgradeSecondaryTakeActivatesEndpointClaim = true;
			runClientCraftingUpgradeSecondaryCraftRegression(true);
			clientCraftingUpgradeSecondaryQuickMoveActivatesEndpointClaim = true;
			runClientRejectedSecondaryCraftRegression();
			clientRejectedSecondaryCraftKeepsInputs = true;
			runClientFailedQuickCraftRetryRegression();
			clientFailedQuickCraftCanRetry = true;
			runClientPendingLinkerUseRegression();
			clientPendingLinkerUseResolvesClaim = true;
			runClientLinkedTankColumnSlotRegression();
			linkedTankColumnSlotSync = true;
			runClientLinkedStorageFeedbackRegression();
			linkedStorageFeedbackUiSync = true;
			runLinkedStorageTickRegression();
			linkedPrimaryTickDispatch = true;
			runClientCreativePlacementRegression();
			clientCreativePlacementCopiesSecondaryEndpoint = true;
			runClientInceptionLinkedChildPersistenceRegression();
			inceptionLinkedChildPersistence = true;
			inceptionMovedLinkedChildDoesNotDuplicate = true;
			return result.replace("\"ok\":true",
					"\"ok\":true,\"clientCanonicalStorageSize\":true,\"inceptionLinkedChildPersistence\":true,\"inceptionMovedLinkedChildDoesNotDuplicate\":true,\"linkedTankColumnSlotSync\":true,\"linkedStorageFeedbackUiSync\":true,\"linkedPrimaryTickDispatch\":true,\"clientCreativePlacementCopiesSecondaryEndpoint\":true,\"clientCraftTakeActivatesLinkerClaim\":true,\"clientCraftQuickMoveActivatesLinkerClaim\":true,\"clientCraftingUpgradeTakeActivatesLinkerClaim\":true,\"clientCraftingUpgradeQuickMoveActivatesLinkerClaim\":true,\"clientSecondaryCraftTakeActivatesEndpointClaim\":true,\"clientSecondaryCraftQuickMoveActivatesEndpointClaim\":true,\"clientCraftingUpgradeSecondaryTakeActivatesEndpointClaim\":true,\"clientCraftingUpgradeSecondaryQuickMoveActivatesEndpointClaim\":true,\"clientRejectedSecondaryCraftKeepsInputs\":true,\"clientFailedQuickCraftCanRetry\":true,\"clientPendingLinkerUseResolvesClaim\":true");
		} catch (RuntimeException e) {
			String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
			return result.replace("\"ok\":true",
					"\"ok\":false,\"clientCanonicalStorageSize\":" + clientCanonicalStorageSize + ",\"inceptionLinkedChildPersistence\":"
							+ inceptionLinkedChildPersistence + ",\"inceptionMovedLinkedChildDoesNotDuplicate\":" + inceptionMovedLinkedChildDoesNotDuplicate
							+ ",\"linkedTankColumnSlotSync\":" + linkedTankColumnSlotSync + ",\"linkedStorageFeedbackUiSync\":" + linkedStorageFeedbackUiSync
							+ ",\"linkedPrimaryTickDispatch\":" + linkedPrimaryTickDispatch + ",\"clientCreativePlacementCopiesSecondaryEndpoint\":"
							+ clientCreativePlacementCopiesSecondaryEndpoint + ",\"clientCraftTakeActivatesLinkerClaim\":" + clientCraftTakeActivatesLinkerClaim
							+ ",\"clientCraftQuickMoveActivatesLinkerClaim\":" + clientCraftQuickMoveActivatesLinkerClaim
							+ ",\"clientCraftingUpgradeTakeActivatesLinkerClaim\":" + clientCraftingUpgradeTakeActivatesLinkerClaim
							+ ",\"clientCraftingUpgradeQuickMoveActivatesLinkerClaim\":" + clientCraftingUpgradeQuickMoveActivatesLinkerClaim
							+ ",\"clientSecondaryCraftTakeActivatesEndpointClaim\":" + clientSecondaryCraftTakeActivatesEndpointClaim
							+ ",\"clientSecondaryCraftQuickMoveActivatesEndpointClaim\":" + clientSecondaryCraftQuickMoveActivatesEndpointClaim
							+ ",\"clientCraftingUpgradeSecondaryTakeActivatesEndpointClaim\":" + clientCraftingUpgradeSecondaryTakeActivatesEndpointClaim
							+ ",\"clientCraftingUpgradeSecondaryQuickMoveActivatesEndpointClaim\":"
							+ clientCraftingUpgradeSecondaryQuickMoveActivatesEndpointClaim + ",\"clientRejectedSecondaryCraftKeepsInputs\":"
							+ clientRejectedSecondaryCraftKeepsInputs + ",\"clientFailedQuickCraftCanRetry\":" + clientFailedQuickCraftCanRetry
							+ ",\"clientPendingLinkerUseResolvesClaim\":" + clientPendingLinkerUseResolvesClaim)
					.replace("\"error\":null", jsonProperty("error", error));
		}
	}

	private static void runClientInceptionLinkedChildPersistenceRegression() {
		InceptionLinkedChildFixture fixture = AutomationRuntime
				.runOnServer(BackpackLinkedStorageRegression::prepareClientInceptionLinkedChildPersistenceRegression);
		try {
			waitForClientInceptionLinkedChildFixture(fixture);
			openClientInceptionLinkedParent();
			waitForClientInceptionLinkedParentMenu(fixture);
			openClientInceptionLinkedChild(fixture);
			waitForClientInceptionLinkedChildMenu(fixture);
			openClientInceptionLinkedParentFromChild();
			waitForClientInceptionLinkedParentMenu(fixture);
			moveClientInceptionLinkedChildToPlayerInventory(fixture);
			waitForServerInceptionMovedLinkedChild(fixture);
			waitForClientInceptionMovedLinkedChild(fixture);
			closeClientInceptionLinkedParent();
			waitForClientInceptionInventoryMenu();
			openClientMovedInceptionLinkedChild(fixture);
			waitForClientMovedInceptionLinkedChildMenu(fixture);
			openClientMovedInceptionLinkedChildSettings(fixture);
			waitForClientMovedInceptionLinkedChildSettings(fixture);
			AutomationRuntime.runOnClient(() -> selectClientItemDisplaySlot(Items.NETHER_STAR));
			waitForServerInceptionMovedLinkedChildItemDisplay(fixture);
			waitForClientInceptionMovedLinkedChildItemDisplay(fixture);
			closeClientInceptionLinkedChildSettings();
			waitForClientInceptionInventoryMenu();
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
		ServerLevel level = (ServerLevel) player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);

		ItemStack child = new ItemStack(ModItems.GOLD_BACKPACK.get());
		IBackpackWrapper childWrapper = BackpackWrapper.fromStack(child);
		childWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.NETHER_STAR, INCEPTION_LINKED_CHILD_MARKER_COUNT));
		childWrapper.getInventoryHandler().saveInventory();
		childWrapper.onContentsUpdated();
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		assertTrue(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, child) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not link the Inception child Backpack");
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
		ItemStack persistedChild = parentWrapper.getInventoryHandler().getStackInSlot(INCEPTION_LINKED_CHILD_SLOT);
		assertInceptionLinkedChildState(level, persistedChild, childEndpoint, "after Inception initialization");

		player.getInventory().setItem(0, parent);
		player.getInventory().setSelectedSlot(0);
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastFullState();
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
							&& hasStack(canonical, 0, Items.NETHER_STAR, INCEPTION_LINKED_CHILD_MARKER_COUNT),
					"Inception linked child lost its canonical group or marker contents " + phase);
		} finally {
			close(canonical);
		}
	}

	private static void waitForClientInceptionLinkedChildFixture(InceptionLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getInventory().getSelectedSlot() == 0
							&& Minecraft.getInstance().player.getMainHandItem().is(ModItems.NETHERITE_BACKPACK.get())
							&& Minecraft.getInstance().player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT).isEmpty())) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the client Inception parent Backpack");
	}

	private static void openClientInceptionLinkedParent() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.getMainHandItem().is(ModItems.NETHERITE_BACKPACK.get())) {
				throw new IllegalStateException("Client Inception parent Backpack is unavailable");
			}
			assertTrue(minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND).consumesAction(),
					"Client Inception parent Backpack use did not consume the interaction");
			return true;
		});
	}

	private static void waitForClientInceptionLinkedParentMenu(InceptionLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK
					&& !(menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper) && fixture.childEndpoint().equals(menu.getStorageWrapper()
							.getInventoryHandler().getStackInSlot(INCEPTION_LINKED_CHILD_SLOT).get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the client Inception parent Backpack menu");
	}

	private static void openClientInceptionLinkedChild(InceptionLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen) || Minecraft.getInstance().player == null
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)
					|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
				throw new IllegalStateException("Client Inception parent Backpack menu is unavailable for child open");
			}
			Slot childSlot = menu.getSlot(INCEPTION_LINKED_CHILD_SLOT);
			assertTrue(fixture.childEndpoint().equals(childSlot.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Client Inception parent menu lost the linked child endpoint before open");
			ClientPacketDistributor.sendToServer(new BackpackOpenPayload(childSlot.index));
			return true;
		});
	}

	private static void waitForClientInceptionLinkedChildMenu(InceptionLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK
					&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
					&& fixture.childEndpoint().equals(menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& fixture.childEndpoint().groupId().equals(menu.getStorageWrapper().getContentsUuid().orElse(null))
					&& hasStack(menu.getStorageWrapper(), 0, Items.NETHER_STAR, INCEPTION_LINKED_CHILD_MARKER_COUNT))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the client Inception linked child Backpack menu");
	}

	private static void openClientInceptionLinkedParentFromChild() {
		AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen) || Minecraft.getInstance().player == null
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)
					|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_SUB_BACKPACK) {
				throw new IllegalStateException("Client Inception linked child Backpack menu is unavailable for parent open");
			}
			ClientPacketDistributor.sendToServer(new BackpackOpenPayload());
			return true;
		});
	}

	private static void moveClientInceptionLinkedChildToPlayerInventory(InceptionLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
					|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
				throw new IllegalStateException("Client Inception parent Backpack menu is unavailable for child move");
			}
			Slot childSlot = menu.getSlot(INCEPTION_LINKED_CHILD_SLOT);
			assertTrue(fixture.childEndpoint().equals(childSlot.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Client Inception parent menu lost the linked child endpoint before move");
			Slot playerSlot = getClientInceptionMovedChildPlayerSlot(menu);
			assertTrue(playerSlot.getItem().isEmpty(), "Client Inception child player inventory destination was occupied");
			minecraft.gameMode.handleContainerInput(menu.containerId, childSlot.index, 0, ContainerInput.PICKUP, minecraft.player);
			assertTrue(fixture.childEndpoint().equals(menu.getCarried().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Client Inception child move did not pick up the linked endpoint");
			minecraft.gameMode.handleContainerInput(menu.containerId, playerSlot.index, 0, ContainerInput.PICKUP, minecraft.player);
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

	private static void waitForServerInceptionMovedLinkedChild(InceptionLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> hasServerInceptionMovedLinkedChild(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the server Inception linked child move");
	}

	private static boolean hasServerInceptionMovedLinkedChild(ServerPlayer player, InceptionLinkedChildFixture fixture) {
		ItemStack parent = player.getInventory().getItem(0);
		ItemStack movedChild = player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT);
		return parent.is(ModItems.NETHERITE_BACKPACK.get())
				&& BackpackWrapper.fromStack(parent).getInventoryHandler().getStackInSlot(INCEPTION_LINKED_CHILD_SLOT).isEmpty()
				&& fixture.childEndpoint().equals(movedChild.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)) && player.getInventory().getNonEquipmentItems()
						.stream().filter(stack -> fixture.childEndpoint().equals(stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).count() == 1;
	}

	private static void waitForClientInceptionMovedLinkedChild(InceptionLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> hasClientInceptionMovedLinkedChild(fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the client Inception linked child move");
	}

	private static boolean hasClientInceptionMovedLinkedChild(InceptionLinkedChildFixture fixture) {
		if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)
				|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
			return false;
		}
		ItemStack movedChild = Minecraft.getInstance().player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT);
		return menu.getSlot(INCEPTION_LINKED_CHILD_SLOT).getItem().isEmpty()
				&& fixture.childEndpoint().equals(movedChild.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
				&& Minecraft.getInstance().player.getInventory().getNonEquipmentItems().stream()
						.filter(stack -> fixture.childEndpoint().equals(stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))).count() == 1;
	}

	private static void closeClientInceptionLinkedParent() {
		AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen screen) || Minecraft.getInstance().player == null
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)
					|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
				throw new IllegalStateException("Client Inception parent Backpack menu is unavailable for close");
			}
			screen.onClose();
			return true;
		});
	}

	private static void waitForClientInceptionInventoryMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> player.containerMenu == player.inventoryMenu) && AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.player != null && minecraft.player.containerMenu == minecraft.player.inventoryMenu
						&& !(minecraft.gui.screen() instanceof BackpackScreen) && !(minecraft.gui.screen() instanceof BackpackSettingsScreen);
			})) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the client Inception child player inventory");
	}

	private static void openClientMovedInceptionLinkedChild(InceptionLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.player.containerMenu != minecraft.player.inventoryMenu || !fixture.childEndpoint()
					.equals(minecraft.player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT).get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))) {
				throw new IllegalStateException("Client moved Inception linked child is unavailable in the player inventory");
			}
			ClientPacketDistributor.sendToServer(new BackpackOpenPayload(INCEPTION_MOVED_CHILD_INVENTORY_SLOT, "", PlayerInventoryProvider.MAIN_INVENTORY));
			return true;
		});
	}

	private static void waitForClientMovedInceptionLinkedChildMenu(InceptionLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK
					&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
					&& fixture.childEndpoint().equals(menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the moved Inception linked child Backpack menu");
	}

	private static void openClientMovedInceptionLinkedChildSettings(InceptionLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen screen) || Minecraft.getInstance().player == null
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)
					|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK
					|| !fixture.childEndpoint().equals(menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))) {
				throw new IllegalStateException("Client moved Inception linked child Backpack menu is unavailable for settings");
			}
			StorageSettingsTab settingsTab = findChild(screen, StorageSettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Client moved Inception linked child storage settings tab was unavailable"));
			assertTrue(settingsTab.mouseClicked(new MouseButtonEvent(settingsTab.getX() + 9, settingsTab.getY() + 12, new MouseButtonInfo(0, 0)), false),
					"Client moved Inception linked child storage settings tab did not handle the click");
			return true;
		});
	}

	private static void waitForClientMovedInceptionLinkedChildSettings(InceptionLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime
					.runOnServer(player -> player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
							&& settingsMenu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
							&& fixture.childEndpoint()
									.equals(settingsMenu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))
					&& AutomationRuntime
							.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackSettingsScreen && Minecraft.getInstance().player != null
									&& Minecraft.getInstance().player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
									&& settingsMenu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper && fixture.childEndpoint()
											.equals(settingsMenu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the moved Inception linked child settings screen");
	}

	private static void closeClientInceptionLinkedChildSettings() {
		AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackSettingsScreen screen)) {
				throw new IllegalStateException("Client moved Inception linked child settings screen was unavailable for close");
			}
			screen.onClose();
			return true;
		});
	}

	private static void waitForServerInceptionMovedLinkedChildItemDisplay(InceptionLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> hasServerInceptionMovedLinkedChildItemDisplay(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the server moved Inception linked child item display setting");
	}

	private static boolean hasServerInceptionMovedLinkedChildItemDisplay(ServerPlayer player, InceptionLinkedChildFixture fixture) {
		if (!hasServerInceptionMovedLinkedChild(player, fixture)) {
			return false;
		}
		IBackpackWrapper canonical = BackpackLinkedStorageResolver
				.resolveCanonicalHost((ServerLevel) player.level(), player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT))
				.orElseThrow(() -> new IllegalStateException("Moved Inception linked child did not resolve its canonical group after item display setting"));
		try {
			ItemDisplaySettingsCategory itemDisplaySettings = canonical.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
			return itemDisplaySettings.getSlots().contains(0) && itemDisplaySettings.getDisplaySide() == DisplaySide.FRONT;
		} finally {
			close(canonical);
		}
	}

	private static void waitForClientInceptionMovedLinkedChildItemDisplay(InceptionLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> hasClientInceptionMovedLinkedChildItemDisplay(fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the client moved Inception linked child item display setting");
	}

	private static boolean hasClientInceptionMovedLinkedChildItemDisplay(InceptionLinkedChildFixture fixture) {
		if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu)
				|| !(Minecraft.getInstance().gui.screen() instanceof BackpackSettingsScreen)
				|| !fixture.childEndpoint().equals(settingsMenu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))) {
			return false;
		}
		final boolean[] matches = {false};
		settingsMenu.forEachSettingsContainer((name, container) -> {
			if (ItemDisplaySettingsCategory.NAME.equals(name) && container instanceof ItemDisplaySettingsContainer itemDisplaySettings) {
				matches[0] = itemDisplaySettings.isSlotSelected(0) && itemDisplaySettings.getDisplaySide() == DisplaySide.FRONT;
			}
		});
		return matches[0];
	}

	private static void runClientEndpointCraftFinalizationRegression(boolean quickMove) {
		ClientEndpointCraftFixture fixture = AutomationRuntime.runOnServer(player -> prepareClientEndpointCraftFinalizationRegression(player, quickMove));
		try {
			waitForClientEndpointCraftFixture(fixture);
			AutomationRuntime.runOnClient(() -> {
				clickClientEndpointCraftResult(fixture);
				return true;
			});
			waitForServerEndpointCraftFinalization(fixture);
			waitForClientEndpointCraftFinalization(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea((ServerLevel) player.level(), fixture.craftingTablePos());
				return true;
			});
			waitForClientMenuClose(fixture.containerId(), "Ender Link crafting");
		}
	}

	private static ClientEndpointCraftFixture prepareClientEndpointCraftFinalizationRegression(ServerPlayer player, boolean quickMove) {
		ServerLevel level = (ServerLevel) player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos craftingTablePos = player.blockPosition().relative(player.getDirection(), 3);
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		clearArea(level, craftingTablePos);
		level.setBlockAndUpdate(craftingTablePos, Blocks.CRAFTING_TABLE.defaultBlockState());
		player.openMenu(new SimpleMenuProvider(
				(windowId, inventory, menuPlayer) -> new CraftingMenu(windowId, inventory, ContainerLevelAccess.create(level, craftingTablePos)),
				Component.literal("Ender Link craft regression")));
		if (!(player.containerMenu instanceof CraftingMenu menu)) {
			throw new IllegalStateException("Could not open Ender Link crafting menu");
		}
		menu.getSlot(1).set(new ItemStack(ENDER_LINKER.get()));
		menu.getSlot(2).set(new ItemStack(ModItems.GOLD_BACKPACK.get()));
		menu.broadcastChanges();
		return new ClientEndpointCraftFixture(menu.containerId, craftingTablePos, quickMove, originalGameMode);
	}

	private static void waitForClientEndpointCraftFixture(ClientEndpointCraftFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof CraftingScreen screen
					&& screen.getMenu().containerId == fixture.containerId() && hasBoundLinkerModel(screen.getMenu().getSlot(0).getItem())
					&& screen.getMenu().getSlot(0).getItem().has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client Ender Link crafting result");
	}

	private static void clickClientEndpointCraftResult(ClientEndpointCraftFixture fixture) {
		clickClientEndpointCraftResult(fixture, fixture.quickMove() ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
	}

	private static void clickClientEndpointCraftResult(ClientEndpointCraftFixture fixture, ContainerInput clickType) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null || !(minecraft.player.containerMenu instanceof CraftingMenu menu)
				|| menu.containerId != fixture.containerId()) {
			throw new IllegalStateException("Client Ender Link crafting menu is unavailable");
		}
		minecraft.gameMode.handleContainerInput(menu.containerId, 0, 0, clickType, minecraft.player);
	}

	private static void waitForServerEndpointCraftFinalization(ClientEndpointCraftFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> hasActivePendingLinkerCraft(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		String status = AutomationRuntime.runOnServer(player -> describeEndpointCraftStatus(player, fixture));
		throw new IllegalStateException("Timed out waiting for server " + fixture.description() + " Ender Link craft claim activation: " + status);
	}

	private static void waitForClientEndpointCraftFinalization(ClientEndpointCraftFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> hasPendingLinkerCraft(Minecraft.getInstance().player, fixture) && hasBoundLinkerModel(fixture.quickMove()
					? findCraftedLinker(Minecraft.getInstance().player.getInventory().getNonEquipmentItems())
					: Minecraft.getInstance().player.containerMenu.getCarried()))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client " + fixture.description() + " Ender Link craft claim activation");
	}

	private static boolean hasActivePendingLinkerCraft(ServerPlayer player, ClientEndpointCraftFixture fixture) {
		ItemStack linker = fixture.quickMove() ? findCraftedLinker(player.getInventory().getNonEquipmentItems()) : player.containerMenu.getCarried();
		ItemStack endpoint = player.containerMenu.getSlot(2).getItem();
		EnderLinkPendingCraftData pendingCraft = linker.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		LinkedStorageEndpointData endpointData = endpoint.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpointData == null || !hasBoundLinkerPresentation(linker)) {
			return false;
		}
		if (pendingCraft == null) {
			return linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET) != null;
		}
		return pendingCraft.claimId() != null && LinkedStorageGroupsSavedData.get((ServerLevel) player.level()).manager()
				.getActivePendingCraftClaim(pendingCraft.claimId()).filter(claim -> claim.groupId().equals(endpointData.groupId())).isPresent();
	}

	private static boolean hasPendingLinkerCraft(@Nullable Player player, ClientEndpointCraftFixture fixture) {
		if (player == null) {
			return false;
		}
		ItemStack linker = fixture.quickMove() ? findCraftedLinker(player.getInventory().getNonEquipmentItems()) : player.containerMenu.getCarried();
		ItemStack endpoint = player.containerMenu.getSlot(2).getItem();
		LinkedStorageEndpointData endpointData = endpoint.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		return endpointData != null && hasBoundLinkerPresentation(linker)
				&& (linker.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT) != null || linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET) != null);
	}

	private static String describeEndpointCraftStatus(ServerPlayer player, ClientEndpointCraftFixture fixture) {
		ItemStack linker = fixture.quickMove() ? findCraftedLinker(player.getInventory().getNonEquipmentItems()) : player.containerMenu.getCarried();
		ItemStack endpoint = player.containerMenu.getSlot(2).getItem();
		EnderLinkPendingCraftData pendingCraft = linker.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		return "linker=" + linker + ", endpoint=" + endpoint + ", pending=" + pendingCraft + ", activeClaim="
				+ (pendingCraft == null || pendingCraft.claimId() == null
						? null
						: LinkedStorageGroupsSavedData.get((ServerLevel) player.level()).manager().getActivePendingCraftClaim(pendingCraft.claimId())
								.orElse(null));
	}

	private static ItemStack findCraftedLinker(List<ItemStack> stacks) {
		return stacks.stream().filter(stack -> stack.is(ENDER_LINKER.get())).findFirst().orElse(ItemStack.EMPTY);
	}

	private static boolean hasBoundLinkerPresentation(ItemStack linker) {
		return EnderLinkerItem.hasBoundPresentation(linker);
	}

	private static boolean hasBoundLinkerModel(ItemStack linker) {
		return hasBoundLinkerPresentation(linker);
	}

	private static void runClientSecondaryCraftRegression(boolean quickMove) {
		ClientSecondaryCraftFixture fixture = AutomationRuntime.runOnServer(player -> prepareClientSecondaryCraftRegression(player, quickMove));
		try {
			waitForClientSecondaryCraftFixture(fixture);
			AutomationRuntime.runOnClient(() -> {
				clickClientSecondaryCraftResult(fixture);
				return true;
			});
			waitForServerSecondaryCraftActivation(fixture);
			waitForClientSecondaryCraftActivation(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea((ServerLevel) player.level(), fixture.craftingTablePos());
				return true;
			});
			waitForClientMenuClose(fixture.containerId(), "secondary Ender Link crafting");
		}
	}

	private static ClientSecondaryCraftFixture prepareClientSecondaryCraftRegression(ServerPlayer player, boolean quickMove) {
		ServerLevel level = (ServerLevel) player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos craftingTablePos = player.blockPosition().relative(player.getDirection(), 3);
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		clearArea(level, craftingTablePos);
		level.setBlockAndUpdate(craftingTablePos, Blocks.CRAFTING_TABLE.defaultBlockState());
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		if (!LinkedStorageService.link(level, linker, new ItemStack(ModItems.GOLD_BACKPACK.get()))) {
			throw new IllegalStateException("Could not prepare bound linker for secondary craft");
		}
		EnderLinkerTargetData target = linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET);
		if (target == null) {
			throw new IllegalStateException("Bound linker is missing its target group");
		}
		player.openMenu(new SimpleMenuProvider(
				(windowId, inventory, menuPlayer) -> new CraftingMenu(windowId, inventory, ContainerLevelAccess.create(level, craftingTablePos)),
				Component.literal("Ender Link secondary craft regression")));
		if (!(player.containerMenu instanceof CraftingMenu menu)) {
			throw new IllegalStateException("Could not open secondary Ender Link crafting menu");
		}
		menu.getSlot(1).set(linker);
		menu.getSlot(2).set(new ItemStack(ModItems.GOLD_BACKPACK.get()));
		menu.broadcastChanges();
		return new ClientSecondaryCraftFixture(menu.containerId, craftingTablePos, target.groupId(), quickMove, originalGameMode);
	}

	private static void waitForClientSecondaryCraftFixture(ClientSecondaryCraftFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof CraftingScreen screen
					&& screen.getMenu().containerId == fixture.containerId() && screen.getMenu().getSlot(0).getItem().is(ModItems.GOLD_BACKPACK.get())
					&& screen.getMenu().getSlot(0).getItem().has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client secondary Ender Link crafting result");
	}

	private static void clickClientSecondaryCraftResult(ClientSecondaryCraftFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null || !(minecraft.player.containerMenu instanceof CraftingMenu menu)
				|| menu.containerId != fixture.containerId()) {
			throw new IllegalStateException("Client secondary Ender Link crafting menu is unavailable");
		}
		minecraft.gameMode.handleContainerInput(menu.containerId, 0, 0, fixture.quickMove() ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP,
				minecraft.player);
	}

	private static void waitForServerSecondaryCraftActivation(ClientSecondaryCraftFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> hasActivatedSecondaryCraft(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for server " + fixture.description() + " secondary Ender Link craft activation");
	}

	private static void waitForClientSecondaryCraftActivation(ClientSecondaryCraftFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> hasDeliveredSecondaryCraft(Minecraft.getInstance().player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client " + fixture.description() + " secondary Ender Link craft activation");
	}

	private static boolean hasActivatedSecondaryCraft(@Nullable Player player, ClientSecondaryCraftFixture fixture) {
		if (player == null) {
			return false;
		}
		ItemStack secondary = fixture.quickMove() ? findCraftedBackpack(player.getInventory().getNonEquipmentItems()) : player.containerMenu.getCarried();
		LinkedStorageEndpointData endpoint = secondary.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		EnderLinkPendingCraftData pendingCraft = secondary.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		boolean activated = endpoint != null && fixture.groupId().equals(endpoint.groupId()) || pendingCraft != null && pendingCraft.claimId() != null
				&& player.level() instanceof ServerLevel level && LinkedStorageGroupsSavedData.get(level).manager()
						.getActivePendingCraftClaim(pendingCraft.claimId()).filter(claim -> fixture.groupId().equals(claim.groupId())).isPresent();
		return activated && player.containerMenu.getSlot(1).getItem().isEmpty() && player.containerMenu.getSlot(2).getItem().isEmpty();
	}

	private static boolean hasDeliveredSecondaryCraft(@Nullable Player player, ClientSecondaryCraftFixture fixture) {
		if (player == null) {
			return false;
		}
		ItemStack secondary = fixture.quickMove() ? findCraftedBackpack(player.getInventory().getNonEquipmentItems()) : player.containerMenu.getCarried();
		return secondary.is(ModItems.GOLD_BACKPACK.get())
				&& (secondary.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT) || secondary.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) != null);
	}

	private static ItemStack findCraftedBackpack(List<ItemStack> stacks) {
		return stacks.stream().filter(stack -> stack.is(ModItems.GOLD_BACKPACK.get())).findFirst().orElse(ItemStack.EMPTY);
	}

	private static void runClientRejectedSecondaryCraftRegression() {
		ClientRejectedSecondaryCraftFixture fixture = AutomationRuntime
				.runOnServer(BackpackLinkedStorageRegression::prepareClientRejectedSecondaryCraftRegression);
		try {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			do {
				if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof CraftingScreen screen
						&& screen.getMenu().containerId == fixture.containerId() && screen.getMenu().getSlot(0).getItem().isEmpty())) {
					break;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);
			if (!AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof CraftingScreen screen
					&& screen.getMenu().containerId == fixture.containerId() && screen.getMenu().getSlot(0).getItem().isEmpty())) {
				throw new IllegalStateException("Client exposed a result for an incompatible secondary Ender Link craft");
			}
			if (!AutomationRuntime.runOnServer(player -> hasRejectedSecondaryCraftInputs(player, fixture))) {
				throw new IllegalStateException("Incompatible secondary Ender Link craft changed an input");
			}
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea((ServerLevel) player.level(), fixture.craftingTablePos());
				return true;
			});
			waitForClientMenuClose(fixture.containerId(), "rejected Ender Link crafting");
		}
	}

	private static ClientRejectedSecondaryCraftFixture prepareClientRejectedSecondaryCraftRegression(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos craftingTablePos = player.blockPosition().relative(player.getDirection(), 3);
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		clearArea(level, craftingTablePos);
		level.setBlockAndUpdate(craftingTablePos, Blocks.CRAFTING_TABLE.defaultBlockState());
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		if (!LinkedStorageService.link(level, linker, new ItemStack(ModItems.GOLD_BACKPACK.get()))) {
			throw new IllegalStateException("Could not prepare bound linker for rejected secondary craft");
		}
		ItemStack candidate = new ItemStack(ModItems.GOLD_BACKPACK.get());
		candidate.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		candidate.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, ModItems.GOLD_BACKPACK.get().getNumberOfSlots());
		candidate.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, ModItems.GOLD_BACKPACK.get().getNumberOfUpgradeSlots());
		IBackpackWrapper candidateWrapper = BackpackWrapper.fromStack(candidate);
		candidateWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIRT));
		candidateWrapper.getInventoryHandler().saveInventory();
		player.openMenu(new SimpleMenuProvider(
				(windowId, inventory, menuPlayer) -> new CraftingMenu(windowId, inventory, ContainerLevelAccess.create(level, craftingTablePos)),
				Component.literal("Rejected Ender Link craft regression")));
		if (!(player.containerMenu instanceof CraftingMenu menu)) {
			throw new IllegalStateException("Could not open rejected Ender Link crafting menu");
		}
		menu.getSlot(1).set(linker);
		menu.getSlot(2).set(candidate);
		menu.broadcastChanges();
		return new ClientRejectedSecondaryCraftFixture(menu.containerId, craftingTablePos, originalGameMode);
	}

	private static boolean hasRejectedSecondaryCraftInputs(ServerPlayer player, ClientRejectedSecondaryCraftFixture fixture) {
		if (!(player.containerMenu instanceof CraftingMenu menu) || menu.containerId != fixture.containerId()) {
			return false;
		}
		ItemStack linker = menu.getSlot(1).getItem();
		ItemStack candidate = menu.getSlot(2).getItem();
		return linker.is(ENDER_LINKER.get()) && linker.has(ModCoreDataComponents.ENDER_LINKER_TARGET) && linker.getCount() == 1
				&& candidate.is(ModItems.GOLD_BACKPACK.get()) && !candidate.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
	}

	private static void runClientFailedQuickCraftRetryRegression() {
		ClientEndpointCraftFixture fixture = AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::prepareClientFailedQuickCraftRetryRegression);
		try {
			waitForClientEndpointCraftFixture(fixture);
			AutomationRuntime.runOnClient(() -> {
				clickClientEndpointCraftResult(fixture, ContainerInput.QUICK_MOVE);
				return true;
			});
			waitForUntransferredEndpointCraftResult(fixture);
			waitForClientEndpointCraftFixture(fixture);
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().getNonEquipmentItems().set(0, ItemStack.EMPTY);
				player.getInventory().setChanged();
				player.containerMenu.broadcastChanges();
				return true;
			});
			AutomationRuntime.runOnClient(() -> {
				clickClientEndpointCraftResult(fixture, ContainerInput.PICKUP);
				return true;
			});
			waitForServerEndpointCraftFinalization(fixture);
			waitForClientEndpointCraftFinalization(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea((ServerLevel) player.level(), fixture.craftingTablePos());
				return true;
			});
			waitForClientMenuClose(fixture.containerId(), "failed quick Ender Link crafting");
		}
	}

	private static ClientEndpointCraftFixture prepareClientFailedQuickCraftRetryRegression(ServerPlayer player) {
		ClientEndpointCraftFixture fixture = prepareClientEndpointCraftFinalizationRegression(player, false);
		for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
			player.getInventory().getNonEquipmentItems().set(slot, new ItemStack(Items.DIRT));
		}
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
		return fixture;
	}

	private static boolean hasUntransferredEndpointCraftResult(ServerPlayer player, ClientEndpointCraftFixture fixture) {
		if (!(player.containerMenu instanceof CraftingMenu menu) || menu.containerId != fixture.containerId()) {
			return false;
		}
		EnderLinkPendingCraftData pendingCraft = menu.getSlot(0).getItem().get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		return pendingCraft != null && pendingCraft.claimId() != null && !menu.getSlot(2).getItem().has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
	}

	private static void waitForUntransferredEndpointCraftResult(ClientEndpointCraftFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> hasUntransferredEndpointCraftResult(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Full-inventory quick craft unexpectedly transferred an Ender Link result");
	}

	private static void runClientPendingLinkerUseRegression() {
		ClientEndpointCraftFixture craftFixture = AutomationRuntime.runOnServer(player -> prepareClientEndpointCraftFinalizationRegression(player, false));
		ClientPendingLinkerUseFixture useFixture = null;
		try {
			waitForClientEndpointCraftFixture(craftFixture);
			AutomationRuntime.runOnClient(() -> {
				clickClientEndpointCraftResult(craftFixture);
				return true;
			});
			waitForServerEndpointCraftFinalization(craftFixture);
			useFixture = AutomationRuntime.runOnServer(player -> prepareClientPendingLinkerUse(player, craftFixture));
			waitForClientMenuClose(craftFixture.containerId(), "Ender Link crafting before linker use");
			ClientPendingLinkerUseFixture activeUseFixture = useFixture;
			waitForClientPendingLinkerUseFixture(activeUseFixture);
			AutomationRuntime.runOnClient(() -> {
				useClientPendingLinker(activeUseFixture);
				return true;
			});
			waitForServerPendingLinkerUse(activeUseFixture);
		} finally {
			ClientPendingLinkerUseFixture fixtureToClean = useFixture;
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(craftFixture.originalGameMode());
				clearArea((ServerLevel) player.level(), craftFixture.craftingTablePos());
				if (fixtureToClean != null) {
					clearArea((ServerLevel) player.level(), fixtureToClean.targetPos());
				}
				return true;
			});
		}
	}

	private static ClientPendingLinkerUseFixture prepareClientPendingLinkerUse(ServerPlayer player, ClientEndpointCraftFixture fixture) {
		if (!(player.containerMenu instanceof CraftingMenu menu)) {
			throw new IllegalStateException("Ender Link crafting menu closed before pending linker use");
		}
		ItemStack linker = menu.getCarried();
		LinkedStorageEndpointData primary = menu.getSlot(2).getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if ((!linker.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT) && !linker.has(ModCoreDataComponents.ENDER_LINKER_TARGET)) || primary == null) {
			throw new IllegalStateException("Ender Link craft did not produce a bound linker and primary endpoint");
		}
		BlockPos targetPos = fixture.craftingTablePos().east();
		ServerLevel level = (ServerLevel) player.level();
		clearArea(level, targetPos);
		level.setBlock(targetPos, ModBlocks.GOLD_BACKPACK.get().defaultBlockState(), 3);
		menu.setCarried(ItemStack.EMPTY);
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		player.closeContainer();
		player.getInventory().setChanged();
		return new ClientPendingLinkerUseFixture(targetPos, primary.groupId());
	}

	private static void waitForClientPendingLinkerUseFixture(ClientPendingLinkerUseFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().level != null
					&& hasBoundLinkerPresentation(Minecraft.getInstance().player.getMainHandItem())
					&& Minecraft.getInstance().level.getBlockEntity(fixture.targetPos()) instanceof BackpackBlockEntity)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client pending Linker use fixture");
	}

	private static void useClientPendingLinker(ClientPendingLinkerUseFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null) {
			throw new IllegalStateException("Client pending Linker use fixture is unavailable");
		}
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(fixture.targetPos()), Direction.UP, fixture.targetPos(), false);
		minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
	}

	private static void waitForServerPendingLinkerUse(ClientPendingLinkerUseFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> hasResolvedPendingLinkerUse(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for pending Linker use to resolve its crafted group");
	}

	private static boolean hasResolvedPendingLinkerUse(ServerPlayer player, ClientPendingLinkerUseFixture fixture) {
		if (!(((ServerLevel) player.level()).getBlockEntity(fixture.targetPos()) instanceof BackpackBlockEntity backpack)) {
			return false;
		}
		LinkedStorageEndpointData endpoint = backpack.getBackpackWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		return endpoint != null && endpoint.groupId().equals(fixture.groupId());
	}

	private static void runClientCraftingUpgradeFinalizationRegression(boolean quickMove) {
		ClientCraftingUpgradeBlockFixture blockFixture = AutomationRuntime
				.runOnServer(player -> prepareClientCraftingUpgradeFinalizationRegression(player, quickMove));
		ClientCraftingUpgradeFixture fixture = null;
		try {
			waitForClientCraftingUpgradeBlock(blockFixture);
			fixture = AutomationRuntime.runOnServer(player -> openClientCraftingUpgradeFinalizationRegression(player, blockFixture));
			ClientCraftingUpgradeFixture activeFixture = fixture;
			waitForClientCraftingUpgradeFixture(activeFixture);
			AutomationRuntime.runOnClient(() -> {
				clickClientCraftingUpgradeResult(activeFixture);
				return true;
			});
			waitForServerCraftingUpgradeFinalization(activeFixture);
			waitForClientCraftingUpgradeFinalization(activeFixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(blockFixture.originalGameMode());
				clearArea((ServerLevel) player.level(), blockFixture.position());
				return true;
			});
			if (fixture != null) {
				waitForClientMenuClose(fixture.containerId(), "Backpack crafting upgrade");
			}
		}
	}

	private static void runClientCraftingUpgradeSecondaryCraftRegression(boolean quickMove) {
		ClientCraftingUpgradeBlockFixture blockFixture = AutomationRuntime
				.runOnServer(player -> prepareClientCraftingUpgradeFinalizationRegression(player, quickMove, true));
		ClientCraftingUpgradeFixture fixture = null;
		try {
			waitForClientCraftingUpgradeBlock(blockFixture);
			fixture = AutomationRuntime.runOnServer(player -> openClientCraftingUpgradeFinalizationRegression(player, blockFixture));
			ClientCraftingUpgradeFixture activeFixture = fixture;
			waitForClientCraftingUpgradeFixture(activeFixture);
			AutomationRuntime.runOnClient(() -> {
				clickClientCraftingUpgradeResult(activeFixture);
				return true;
			});
			waitForServerCraftingUpgradeSecondaryActivation(activeFixture);
			waitForClientCraftingUpgradeSecondaryActivation(activeFixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(blockFixture.originalGameMode());
				clearArea((ServerLevel) player.level(), blockFixture.position());
				return true;
			});
			if (fixture != null) {
				waitForClientMenuClose(fixture.containerId(), "Backpack secondary crafting upgrade");
			}
		}
	}

	private static ClientCraftingUpgradeBlockFixture prepareClientCraftingUpgradeFinalizationRegression(ServerPlayer player, boolean quickMove) {
		return prepareClientCraftingUpgradeFinalizationRegression(player, quickMove, false);
	}

	private static ClientCraftingUpgradeBlockFixture prepareClientCraftingUpgradeFinalizationRegression(ServerPlayer player, boolean quickMove,
			boolean secondary) {
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		ServerLevel level = (ServerLevel) player.level();
		BlockPos position = player.blockPosition().relative(player.getDirection(), 2);
		clearArea(level, position);

		ItemStack backpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, ModItems.GOLD_BACKPACK.get().getNumberOfSlots());
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, ModItems.GOLD_BACKPACK.get().getNumberOfUpgradeSlots());
		IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
		backpackWrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
		backpackWrapper.getUpgradeHandler().saveInventory();
		backpackWrapper.setOpenTabId(0);
		backpackWrapper.onContentsUpdated();
		level.setBlock(position, ModBlocks.GOLD_BACKPACK.get().defaultBlockState(), 3);
		if (!(level.getBlockEntity(position) instanceof BackpackBlockEntity backpackBlockEntity)) {
			throw new IllegalStateException("Could not create the Backpack crafting upgrade regression block");
		}
		backpackBlockEntity.setBackpack(backpack);
		WorldHelper.notifyBlockUpdate(backpackBlockEntity);
		UUID backpackUuid = backpack.get(ModCoreDataComponents.STORAGE_UUID);
		if (backpackUuid == null) {
			throw new IllegalStateException("Backpack crafting upgrade regression backpack is missing its storage UUID");
		}
		PacketDistributor.sendToPlayer(player,
				new BackpackContentsPayload(backpackUuid, BackpackStorage.get().getOrCreateBackpackContents(backpackUuid).copy()));
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		UUID groupId = null;
		if (secondary) {
			if (!LinkedStorageService.link(level, linker, new ItemStack(ModItems.GOLD_BACKPACK.get()))) {
				throw new IllegalStateException("Could not prepare bound linker for Backpack secondary craft");
			}
			EnderLinkerTargetData target = linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET);
			if (target == null) {
				throw new IllegalStateException("Backpack secondary craft linker is missing its target group");
			}
			groupId = target.groupId();
		}
		return new ClientCraftingUpgradeBlockFixture(position, linker, groupId, secondary, quickMove, originalGameMode);
	}

	private static void waitForClientCraftingUpgradeBlock(ClientCraftingUpgradeBlockFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null
					&& Minecraft.getInstance().level.getBlockEntity(fixture.position()) instanceof BackpackBlockEntity backpack
					&& backpack.getBackpackWrapper().getUpgradeHandler().getStackInSlot(0).is(ModItems.CRAFTING_UPGRADE.get()))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client Backpack crafting upgrade block");
	}

	private static ClientCraftingUpgradeFixture openClientCraftingUpgradeFinalizationRegression(ServerPlayer player,
			ClientCraftingUpgradeBlockFixture fixture) {
		BackpackContext.Block context = new BackpackContext.Block(fixture.position());
		player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new BackpackContainer(windowId, menuPlayer, context),
				Component.literal("Ender Link crafting upgrade regression")), context::toBuffer);
		if (!(player.containerMenu instanceof BackpackContainer menu)
				|| !(menu.getUpgradeContainers().get(0) instanceof CraftingUpgradeContainer craftingContainer)) {
			throw new IllegalStateException("Could not open the Backpack crafting upgrade menu");
		}
		craftingContainer.getCraftMatrix().setItem(0, fixture.linker());
		craftingContainer.getCraftMatrix().setItem(1, new ItemStack(ModItems.GOLD_BACKPACK.get()));
		Slot resultSlot = craftingContainer.getSlots().get(9);
		if (menu.hasSomethingMessedWithStorage()) {
			throw new IllegalStateException(describeCraftingUpgradeMenuIntegrity(menu, player));
		}
		menu.broadcastFullState();
		return new ClientCraftingUpgradeFixture(menu.containerId, fixture.position(), fixture.groupId(), fixture.secondary(), fixture.quickMove(),
				fixture.originalGameMode());
	}

	private static void waitForClientCraftingUpgradeFixture(ClientCraftingUpgradeFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> {
				CraftingUpgradeContainer craftingContainer = getClientCraftingUpgrade(fixture);
				return craftingContainer != null && (fixture.secondary() || hasBoundLinkerModel(craftingContainer.getSlots().get(9).getItem()))
						&& craftingContainer.getSlots().get(9).getItem().has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
			})) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		String serverResult = AutomationRuntime.runOnServer(player -> {
			if (!(player.containerMenu instanceof BackpackContainer menu)
					|| !(menu.getUpgradeContainers().get(0) instanceof CraftingUpgradeContainer craftingContainer)) {
				return "menu unavailable";
			}
			return describeCraftingUpgradeMenuIntegrity(menu, player) + ", result=" + craftingContainer.getSlots().get(9).getItem() + ", grid="
					+ craftingContainer.getCraftMatrix().getItem(0) + ", " + craftingContainer.getCraftMatrix().getItem(1);
		});
		throw new IllegalStateException("Timed out waiting for client Backpack crafting upgrade result: " + serverResult);
	}

	private static String describeCraftingUpgradeMenuIntegrity(BackpackContainer menu, ServerPlayer player) {
		return "messedWithStorage=" + menu.hasSomethingMessedWithStorage() + ", contextWrapperChanged="
				+ (menu.getBackpackContext().getBackpackWrapper(player) != menu.getStorageWrapper()) + ", menuSlots=" + menu.getInventorySlotsSize()
				+ ", expectedSlots=" + (menu.getStorageWrapper().getInventoryHandler().size() + 36);
	}

	private static void clickClientCraftingUpgradeResult(ClientCraftingUpgradeFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
				|| menu.containerId != fixture.containerId()) {
			throw new IllegalStateException("Client Backpack crafting upgrade menu is unavailable");
		}
		if (!(menu.getUpgradeContainers().get(0) instanceof CraftingUpgradeContainer craftingContainer)) {
			throw new IllegalStateException("Client Backpack crafting upgrade is unavailable");
		}
		Slot resultSlot = craftingContainer.getSlots().get(9);
		minecraft.gameMode.handleContainerInput(menu.containerId, resultSlot.index, 0, fixture.quickMove() ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP,
				minecraft.player);
	}

	private static void waitForServerCraftingUpgradeFinalization(ClientCraftingUpgradeFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> hasActivePendingCraftingUpgradeLinker(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for server " + fixture.description() + " Backpack crafting upgrade claim activation");
	}

	private static void waitForClientCraftingUpgradeFinalization(ClientCraftingUpgradeFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> {
				if (!(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
					return false;
				}
				ItemStack linker = fixture.quickMove() ? findCraftingUpgradeLinker(Minecraft.getInstance().player, menu) : menu.getCarried();
				return hasPendingCraftingUpgradeLinker(Minecraft.getInstance().player, fixture) && hasBoundLinkerModel(linker);
			})) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client " + fixture.description() + " Backpack crafting upgrade claim activation");
	}

	private static void waitForClientMenuClose(int containerId, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().player == null || Minecraft.getInstance().player.containerMenu.containerId != containerId)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client " + description + " menu to close");
	}

	@Nullable
	private static CraftingUpgradeContainer getClientCraftingUpgrade(ClientCraftingUpgradeFixture fixture) {
		if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)
				|| menu.containerId != fixture.containerId()) {
			return null;
		}
		return menu.getUpgradeContainers().get(0) instanceof CraftingUpgradeContainer craftingContainer ? craftingContainer : null;
	}

	private static boolean hasActivePendingCraftingUpgradeLinker(ServerPlayer player, ClientCraftingUpgradeFixture fixture) {
		if (!(player.containerMenu instanceof BackpackContainer menu)
				|| !(menu.getUpgradeContainers().get(0) instanceof CraftingUpgradeContainer craftingContainer)) {
			return false;
		}
		ItemStack linker = fixture.quickMove() ? findCraftingUpgradeLinker(player, menu) : menu.getCarried();
		EnderLinkPendingCraftData pendingCraft = linker.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		LinkedStorageEndpointData endpointData = craftingContainer.getCraftMatrix().getItem(1).get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (!hasBoundLinkerPresentation(linker)) {
			return false;
		}
		if (pendingCraft == null) {
			return linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET) != null;
		}
		return endpointData != null && pendingCraft.claimId() != null && LinkedStorageGroupsSavedData.get((ServerLevel) player.level()).manager()
				.getActivePendingCraftClaim(pendingCraft.claimId()).filter(claim -> claim.groupId().equals(endpointData.groupId())).isPresent();
	}

	private static boolean hasPendingCraftingUpgradeLinker(@Nullable Player player, ClientCraftingUpgradeFixture fixture) {
		if (player == null || !(player.containerMenu instanceof BackpackContainer menu)
				|| !(menu.getUpgradeContainers().get(0) instanceof CraftingUpgradeContainer craftingContainer)) {
			return false;
		}
		ItemStack linker = fixture.quickMove() ? findCraftingUpgradeLinker(player, menu) : menu.getCarried();
		ItemStack endpoint = craftingContainer.getCraftMatrix().getItem(1);
		LinkedStorageEndpointData endpointData = endpoint.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		EnderLinkPendingCraftData pendingCraft = linker.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		return hasBoundLinkerPresentation(linker)
				&& (linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET) != null || endpointData != null && pendingCraft != null);
	}

	private static ItemStack findCraftingUpgradeLinker(Player player, BackpackContainer menu) {
		ItemStack playerLinker = findCraftedLinker(player.getInventory().getNonEquipmentItems());
		if (!playerLinker.isEmpty()) {
			return playerLinker;
		}
		for (int slot = 0; slot < menu.getStorageWrapper().getInventoryHandler().size(); slot++) {
			ItemStack storageStack = menu.getStorageWrapper().getInventoryHandler().getStackInSlot(slot);
			if (storageStack.is(ENDER_LINKER.get())) {
				return storageStack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void waitForServerCraftingUpgradeSecondaryActivation(ClientCraftingUpgradeFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> hasActivatedCraftingUpgradeSecondary(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for server " + fixture.description() + " Backpack secondary craft activation");
	}

	private static void waitForClientCraftingUpgradeSecondaryActivation(ClientCraftingUpgradeFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> hasDeliveredCraftingUpgradeSecondary(Minecraft.getInstance().player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client " + fixture.description() + " Backpack secondary craft activation");
	}

	private static boolean hasActivatedCraftingUpgradeSecondary(@Nullable Player player, ClientCraftingUpgradeFixture fixture) {
		if (player == null || fixture.groupId() == null || !(player.containerMenu instanceof BackpackContainer menu)
				|| !(menu.getUpgradeContainers().get(0) instanceof CraftingUpgradeContainer craftingContainer)) {
			return false;
		}
		ItemStack secondary = fixture.quickMove() ? findCraftingUpgradeBackpack(player, menu) : menu.getCarried();
		LinkedStorageEndpointData endpoint = secondary.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		EnderLinkPendingCraftData pendingCraft = secondary.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		boolean activated = endpoint != null && fixture.groupId().equals(endpoint.groupId()) || pendingCraft != null && pendingCraft.claimId() != null
				&& player.level() instanceof ServerLevel level && LinkedStorageGroupsSavedData.get(level).manager()
						.getActivePendingCraftClaim(pendingCraft.claimId()).filter(claim -> fixture.groupId().equals(claim.groupId())).isPresent();
		return activated && craftingContainer.getCraftMatrix().getItem(0).isEmpty() && craftingContainer.getCraftMatrix().getItem(1).isEmpty();
	}

	private static boolean hasDeliveredCraftingUpgradeSecondary(@Nullable Player player, ClientCraftingUpgradeFixture fixture) {
		if (player == null || !(player.containerMenu instanceof BackpackContainer menu)) {
			return false;
		}
		ItemStack secondary = fixture.quickMove() ? findCraftingUpgradeBackpack(player, menu) : menu.getCarried();
		return secondary.is(ModItems.GOLD_BACKPACK.get())
				&& (secondary.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT) || secondary.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) != null);
	}

	private static ItemStack findCraftingUpgradeBackpack(Player player, BackpackContainer menu) {
		ItemStack playerBackpack = findCraftedBackpack(player.getInventory().getNonEquipmentItems());
		if (!playerBackpack.isEmpty()) {
			return playerBackpack;
		}
		for (int slot = 0; slot < menu.getStorageWrapper().getInventoryHandler().size(); slot++) {
			ItemStack storageStack = menu.getStorageWrapper().getInventoryHandler().getStackInSlot(slot);
			if (storageStack.is(ModItems.GOLD_BACKPACK.get())) {
				return storageStack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void runClientCreativePlacementRegression() {
		ClientCreativePlacementFixture fixture = AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::prepareClientCreativePlacementRegression);
		try {
			waitForClientCreativePlacementFixture(fixture);
			AutomationRuntime.runOnClient(() -> {
				placeClientCreativeEndpoint(fixture);
				return true;
			});
			waitForServerCreativePlacement(fixture);
			waitForClientCreativePlacement(fixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.setShiftKeyDown(false);
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea((ServerLevel) player.level(), fixture.position());
				return true;
			});
		}
	}

	private static ClientCreativePlacementFixture prepareClientCreativePlacementRegression(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos position = player.blockPosition().relative(player.getDirection(), 3);
		clearArea(level, position);
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.CREATIVE);

		ItemStack primary = new ItemStack(ModItems.GOLD_BACKPACK.get());
		ItemStack secondary = new ItemStack(ModItems.GOLD_BACKPACK.get());
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		assertTrue(LinkedStorageService.link(level, linker, primary), "Could not link client creative-placement primary");
		assertTrue(LinkedStorageService.link(level, linker, secondary), "Could not link client creative-placement secondary");
		LinkedStorageEndpointData sourceEndpoint = requireEndpoint(secondary, "client creative-placement source");
		player.getInventory().setSelectedSlot(0);
		player.connection.send(new ClientboundSetHeldSlotPacket(0));
		player.setItemInHand(InteractionHand.MAIN_HAND, secondary);
		player.setShiftKeyDown(true);
		level.setBlockAndUpdate(position.below(), Blocks.DIRT.defaultBlockState());
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastFullState();
		return new ClientCreativePlacementFixture(position, sourceEndpoint, originalGameMode);
	}

	private static void waitForClientCreativePlacementFixture(ClientCreativePlacementFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().gameMode != null
					&& Minecraft.getInstance().level != null
					&& fixture.sourceEndpoint().equals(Minecraft.getInstance().player.getMainHandItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& Minecraft.getInstance().level.getBlockState(fixture.position().below()).is(Blocks.DIRT))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client creative-placement fixture");
	}

	private static void placeClientCreativeEndpoint(ClientCreativePlacementFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null) {
			throw new IllegalStateException("Client player/game mode is not available for creative placement");
		}
		minecraft.player.setShiftKeyDown(true);
		try {
			BlockPos supportPos = fixture.position().below();
			minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false));
		} finally {
			minecraft.player.setShiftKeyDown(false);
		}
	}

	private static void waitForServerCreativePlacement(ClientCreativePlacementFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean placed = AutomationRuntime.runOnServer(player -> {
				if (!(((ServerLevel) player.level()).getBlockEntity(fixture.position()) instanceof BackpackBlockEntity backpack)) {
					return false;
				}
				LinkedStorageEndpointData placedEndpoint = requireEndpoint(backpack.getBackpackWrapper().getBackpack(), "client creative placement");
				return fixture.sourceEndpoint().equals(player.getMainHandItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
						&& fixture.sourceEndpoint().groupId().equals(placedEndpoint.groupId())
						&& !fixture.sourceEndpoint().endpointId().equals(placedEndpoint.endpointId());
			});
			if (placed) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for server creative endpoint copy");
	}

	private static void waitForClientCreativePlacement(ClientCreativePlacementFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().level == null
						|| !(Minecraft.getInstance().level.getBlockEntity(fixture.position()) instanceof BackpackBlockEntity backpack)) {
					return false;
				}
				LinkedStorageEndpointData placedEndpoint = backpack.getBackpackWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
				return placedEndpoint != null && fixture.sourceEndpoint().groupId().equals(placedEndpoint.groupId())
						&& !fixture.sourceEndpoint().endpointId().equals(placedEndpoint.endpointId());
			})) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client-synchronized creative endpoint copy");
	}

	private static void runLinkedStorageTickRegression() {
		assertTickCase(LinkedStorageTickCase.CARRIED_PRIMARY, true);
		assertTickCase(LinkedStorageTickCase.CARRIED_SECONDARY_WITH_PRIMARY_IN_CHEST, false);
		assertTickCase(LinkedStorageTickCase.PLACED_PRIMARY, true);
		assertTickCase(LinkedStorageTickCase.PLACED_SECONDARY_WITH_PRIMARY_IN_CHEST, false);
	}

	private static void assertTickCase(LinkedStorageTickCase tickCase, boolean shouldRefill) {
		LinkedStorageTickFixture fixture = AutomationRuntime.runOnServer(player -> prepareLinkedStorageTickFixture(player, tickCase));
		try {
			sleep(1_000);
			boolean refilled = AutomationRuntime.runOnServer(player -> hasTickFixtureRefilled(player, fixture));
			if (refilled != shouldRefill) {
				throw new IllegalStateException(
						tickCase.description() + (shouldRefill ? " did not refill from its active primary" : " refilled despite its primary being inactive"));
			}
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea((ServerLevel) player.level(), fixture.primaryPos());
				clearArea((ServerLevel) player.level(), fixture.secondaryPos());
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				return true;
			});
		}
	}

	private static LinkedStorageTickFixture prepareLinkedStorageTickFixture(ServerPlayer player, LinkedStorageTickCase tickCase) {
		ServerLevel level = (ServerLevel) player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos primaryPos = player.blockPosition().relative(player.getDirection(), 3);
		BlockPos secondaryPos = primaryPos.east(2);
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		clearArea(level, primaryPos);
		clearArea(level, secondaryPos);

		ItemStack primary = configuredRefillBackpack();
		ItemStack secondary = new ItemStack(ModItems.BACKPACK.get());
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		assertTrue(LinkedStorageService.link(level, linker, primary), "Could not link tick-regression primary");
		assertTrue(LinkedStorageService.link(level, linker, secondary), "Could not link tick-regression secondary");

		if (tickCase.primaryInChest()) {
			level.setBlockAndUpdate(primaryPos, Blocks.CHEST.defaultBlockState());
			if (!(level.getBlockEntity(primaryPos) instanceof ChestBlockEntity chest)) {
				throw new IllegalStateException("Tick-regression primary chest was not created");
			}
			chest.setItem(0, primary);
			chest.setChanged();
		} else if (tickCase.primaryPlaced()) {
			placeBlockWithItem(level, player, primaryPos, primary);
		} else {
			player.getInventory().setItem(8, primary);
		}

		if (tickCase.secondaryPlaced()) {
			placeBlockWithItem(level, player, secondaryPos, secondary);
		} else {
			player.getInventory().setItem(9, secondary);
		}
		player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
		player.getInventory().setSelectedSlot(0);
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		return new LinkedStorageTickFixture(tickCase, primaryPos, secondaryPos, originalGameMode);
	}

	private static ItemStack configuredRefillBackpack() {
		ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 32));
		wrapper.getInventoryHandler().saveInventory();
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(ModItems.REFILL_UPGRADE.get()));
		RefillUpgradeWrapper refill = upgrades.getWrappersThatImplement(RefillUpgradeWrapper.class).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Tick-regression Refill upgrade was not initialized"));
		refill.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
		upgrades.saveInventory();
		return backpack;
	}

	private static boolean hasTickFixtureRefilled(ServerPlayer player, LinkedStorageTickFixture fixture) {
		ItemStack target = player.getInventory().getItem(0);
		if (!target.is(Items.DIAMOND)) {
			throw new IllegalStateException(fixture.tickCase().description() + " target slot no longer contains Diamonds");
		}
		return target.getCount() > 5;
	}

	private static String run(ServerPlayer player) {
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		ServerLevel level = (ServerLevel) player.level();
		BlockPos primaryPos = player.blockPosition().relative(player.getDirection(), 3);
		BlockPos secondaryPos = primaryPos.east(2);
		BlockPos creativePos = secondaryPos.east(2);
		BlockPos failedCreativePos = creativePos.east(2);
		BlockPos worldLinkPrimaryPos = failedCreativePos.east(2);
		BlockPos worldLinkSecondaryPos = worldLinkPrimaryPos.east(2);
		boolean primaryMigrated = false;
		boolean groupOwnerRecorded = false;
		boolean endpointAccessRecorded = false;
		boolean boundLinkerVisual = false;
		boolean existingEndpointBindsLinker = false;
		boolean invalidCraftRejected = false;
		boolean primaryTierUpgradePropagated = false;
		boolean secondaryTierUpgradeRejected = false;
		boolean differentTierEndpointAccepted = false;
		boolean placedEndpointsShare = false;
		boolean secondaryServerAccessPrimaryProcessingOnly = false;
		boolean placedCanonicalContentsRendered = false;
		boolean endpointLocalPresentationRetained = false;
		boolean placedMenuMutationSynced = false;
		boolean externalItemAutomationSynced = false;
		boolean externalFluidAutomationSynced = false;
		boolean externalEnergyAutomationSynced = false;
		boolean placedToCarriedSharing = false;
		boolean carriedToPlacedSharing = false;
		boolean replacementRetainedEndpointAndContents = false;
		boolean normalBreakPickupRetainedEndpointAndContents = false;
		boolean placedPrimaryLinked = false;
		boolean placedSecondaryLinked = false;
		boolean existingBlockBindsBlankLinker = false;
		boolean boundLinkerLeavesExistingBlockUnchanged = false;
		boolean jukeboxPlaybackAnchorsToPrimary = false;
		boolean creativePlacementCopiesSecondaryEndpoint = false;
		boolean failedCreativePlacementDoesNotRegisterEndpoint = false;
		@Nullable
		String error = null;
		try {
			player.closeContainer();
			player.getInventory().clearContent();
			clearArea(level, primaryPos);
			clearArea(level, secondaryPos);
			clearArea(level, creativePos);
			clearArea(level, failedCreativePos);
			clearArea(level, worldLinkPrimaryPos);
			clearArea(level, worldLinkSecondaryPos);
			player.setGameMode(GameType.SURVIVAL);

			BackpackItem backpackItem = ModItems.GOLD_BACKPACK.get();
			BackpackRegressionFixture.Fixture primaryFixture = BackpackRegressionFixture.create(backpackItem, PRIMARY_MAIN_COLOR, PRIMARY_ACCENT_COLOR);
			ItemStack primaryInput = primaryFixture.backpack();
			primaryInput.set(DataComponents.CUSTOM_NAME, PRIMARY_NAME);
			configureCanonicalItemDisplay(primaryInput);
			IBackpackWrapper primaryInputWrapper = BackpackWrapper.fromStack(primaryInput);
			primaryInputWrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
			primaryInputWrapper.getUpgradeHandler().setStackInSlot(2, new ItemStack(ModItems.BATTERY_UPGRADE.get()));
			primaryInputWrapper.getUpgradeHandler().saveInventory();

			EnderLinkerEndpointRecipe recipe = new EnderLinkerEndpointRecipe(CraftingBookCategory.MISC);
			ItemStack linker = new ItemStack(ENDER_LINKER.get());
			CraftingInput firstInput = CraftingInput.of(2, 1, List.of(linker, primaryInput));
			assertTrue(recipe.matches(firstInput, level), "Primary EnderLinker recipe did not match");
			ItemStack boundLinker = recipe.assemble(firstInput);
			EnderLinkerEndpointRecipe.issueCraftClaim(player, boundLinker);
			NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, boundLinker, new SimpleContainer(linker, primaryInput)));
			assertTrue(EnderLinkerEndpointRecipe.finalizePendingCraftLinker(level, boundLinker),
					"Primary linker claim was not activated by the crafting event");
			ItemStack primary = primaryInput;
			LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "primary");
			CompoundTag savedPrimaryGroup = getSavedGroup(level, primaryEndpoint.groupId());
			groupOwnerRecorded = player.getUUID().equals(savedPrimaryGroup.read("owner_id", UUIDUtil.CODEC).orElse(null));
			assertTrue(groupOwnerRecorded, "Primary endpoint group did not retain its creating player as owner");
			CompoundTag savedPrimaryEndpoint = getSavedEndpoint(savedPrimaryGroup, primaryEndpoint.endpointId());
			assertTrue(savedPrimaryEndpoint.read("last_opened_by", UUIDUtil.CODEC).isEmpty() && savedPrimaryEndpoint.getLongOr("last_opened_at", -1L) == -1L,
					"Primary endpoint unexpectedly recorded an opener before its menu was opened");
			IBackpackWrapper primaryFacade = resolve(level, primary, "primary");
			primaryMigrated = !primary.has(ModCoreDataComponents.STORAGE_UUID) && primaryFacade instanceof LinkedStorageBackpackWrapper
					&& primaryEndpoint.groupId().equals(primaryFacade.getContentsUuid().orElse(null)) && hasStack(primaryFacade, 0, Items.DIAMOND, 5)
					&& hasUpgrade(primaryFacade, ModItems.STACK_UPGRADE_STARTER_TIER.get(), ModItems.TANK_UPGRADE.get(), ModItems.BATTERY_UPGRADE.get());
			close(primaryFacade);
			assertTrue(primaryMigrated, "Primary endpoint did not migrate its canonical contents");

			EnderLinkerTargetData linkerTarget = boundLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET);
			assertTrue(!boundLinker.isEmpty() && linkerTarget != null && primaryEndpoint.groupId().equals(linkerTarget.groupId()),
					"Primary craft did not deliver a linker bound to the created group");
			boundLinkerVisual = hasBoundLinkerPresentation(boundLinker);
			assertTrue(boundLinkerVisual, "Primary craft did not mark the returned linker with its bound visual state");
			ItemStack blankLinker = new ItemStack(ENDER_LINKER.get());
			CraftingInput rebindInput = CraftingInput.of(2, 1, List.of(blankLinker, primary));
			assertTrue(recipe.matches(rebindInput, level), "Existing endpoint did not bind a blank linker");
			ItemStack reboundLinker = recipe.assemble(rebindInput);
			EnderLinkerEndpointRecipe.issueCraftClaim(player, reboundLinker);
			NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, reboundLinker, new SimpleContainer(blankLinker, primary)));
			assertTrue(EnderLinkerEndpointRecipe.finalizePendingCraftLinker(level, reboundLinker),
					"Existing endpoint linker claim was not activated by the crafting event");
			LinkedStorageEndpointData reboundEndpoint = requireEndpoint(primary, "rebound primary");
			existingEndpointBindsLinker = primaryEndpoint.groupId().equals(reboundEndpoint.groupId())
					&& primaryEndpoint.endpointId().equals(reboundEndpoint.endpointId()) && reboundLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET) != null
					&& primaryEndpoint.groupId().equals(reboundLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
			assertTrue(existingEndpointBindsLinker, "Existing endpoint craft did not preserve the endpoint and bind the blank linker");

			CraftingInput primaryUpgradeInput = tierUpgradeInput(primary, Items.DIAMOND);
			CraftingRecipe primaryUpgradeRecipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, primaryUpgradeInput, level)
					.orElseThrow(() -> new IllegalStateException("Primary linked Backpack tier upgrade recipe did not match")).value();
			assertTrue(primaryUpgradeRecipe instanceof BackpackUpgradeRecipe, "Primary linked Backpack tier upgrade selected an unexpected recipe");
			ItemStack upgradedPrimary = primaryUpgradeRecipe.assemble(primaryUpgradeInput);
			NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, upgradedPrimary, craftingContainer(primaryUpgradeInput)));
			IBackpackWrapper upgradedPrimaryFacade = resolve(level, upgradedPrimary, "upgraded primary");
			primaryTierUpgradePropagated = upgradedPrimary.is(ModItems.DIAMOND_BACKPACK.get())
					&& upgradedPrimary.getOrDefault(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, -1) == ModItems.DIAMOND_BACKPACK.get().getNumberOfSlots()
					&& upgradedPrimary.getOrDefault(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, -1) == ModItems.DIAMOND_BACKPACK.get()
							.getNumberOfUpgradeSlots()
					&& upgradedPrimaryFacade.getInventoryHandler().size() == ModItems.DIAMOND_BACKPACK.get().getNumberOfSlots()
					&& upgradedPrimaryFacade.getUpgradeHandler().size() == ModItems.DIAMOND_BACKPACK.get().getNumberOfUpgradeSlots();
			close(upgradedPrimaryFacade);
			assertTrue(primaryTierUpgradePropagated, "Primary tier upgrade did not update the canonical group capacity");
			primary = upgradedPrimary;

			ItemStack incompleteResult = recipe.assemble(CraftingInput.of(2, 1, List.of(new ItemStack(ENDER_LINKER.get()), new ItemStack(backpackItem))));
			NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, incompleteResult, new SimpleContainer(new ItemStack(ENDER_LINKER.get()))));
			invalidCraftRejected = BackpackLinkedStorageResolver.resolve(level, incompleteResult).isEmpty()
					&& !incompleteResult.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
			assertTrue(invalidCraftRejected, "Incomplete EnderLinker craft was treated as a live endpoint");

			placeBlockWithItem(level, player, primaryPos, primary);
			BackpackBlockEntity primaryPlaced = requirePlacedBackpack(level, primaryPos, "primary");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			BlockHitResult primaryHit = new BlockHitResult(Vec3.atCenterOf(primaryPos), Direction.UP, primaryPos, false);
			long primaryOpenedAt = level.getGameTime();
			player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, primaryHit);
			assertTrue(player.containerMenu instanceof BackpackContainer, "Placed linked backpack did not open a backpack menu");
			assertTrue(((BackpackContainer) player.containerMenu).getStorageWrapper() instanceof LinkedStorageBackpackWrapper,
					"Placed linked backpack menu did not use a linked storage facade");
			savedPrimaryEndpoint = getSavedEndpoint(getSavedGroup(level, primaryEndpoint.groupId()), primaryEndpoint.endpointId());
			endpointAccessRecorded = player.getUUID().equals(savedPrimaryEndpoint.read("last_opened_by", UUIDUtil.CODEC).orElse(null))
					&& savedPrimaryEndpoint.getLongOr("last_opened_at", -1L) == primaryOpenedAt;
			assertTrue(endpointAccessRecorded, "Opening the primary endpoint menu did not record the player and game time");
			player.closeContainer();

			ItemStack secondaryInput = new ItemStack(ModItems.BACKPACK.get());
			BackpackItem.setColors(secondaryInput, SECONDARY_MAIN_COLOR, SECONDARY_ACCENT_COLOR);
			secondaryInput.set(DataComponents.CUSTOM_NAME, SECONDARY_NAME);
			secondaryInput.set(ModCoreDataComponents.OPEN_TAB_ID, 2);
			CraftingInput secondInput = CraftingInput.of(2, 1, List.of(boundLinker, secondaryInput));
			assertTrue(recipe.matches(secondInput, level), "Secondary EnderLinker recipe did not match");
			ItemStack secondary = recipe.assemble(secondInput);
			EnderLinkerEndpointRecipe.issueCraftClaim(player, secondary);
			NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, secondary, new SimpleContainer(boundLinker, secondaryInput)));
			assertTrue(secondary.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) || EnderLinkerEndpointRecipe.finalizePendingCraftResult(level, secondary),
					"Secondary endpoint claim was not activated by the crafting event");
			boundLinker.shrink(1);
			assertTrue(boundLinker.isEmpty() && recipe.getRemainingItems(secondInput).get(0).isEmpty(), "Secondary craft did not consume the bound linker");
			LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "secondary");
			CraftingInput secondaryUpgradeInput = tierUpgradeInput(secondary, Items.COPPER_INGOT);
			secondaryTierUpgradeRejected = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, secondaryUpgradeInput, level).isEmpty();
			assertTrue(secondaryTierUpgradeRejected, "Secondary linked Backpack tier upgrade recipe was available");
			ItemStack placedPrimary = primaryPlaced.getBackpackWrapper().getBackpack();
			differentTierEndpointAccepted = placedPrimary.getItem() != secondary.getItem()
					&& placedPrimary.getOrDefault(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, -1) == secondary
							.getOrDefault(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, -1)
					&& placedPrimary.getOrDefault(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, -1) == secondary
							.getOrDefault(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, -1);
			assertTrue(differentTierEndpointAccepted,
					"Different-tier endpoint did not retain the primary group storage size. Primary inventory="
							+ placedPrimary.getOrDefault(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, -1) + ", secondary inventory="
							+ secondary.getOrDefault(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, -1) + ", primary upgrades="
							+ placedPrimary.getOrDefault(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, -1) + ", secondary upgrades="
							+ secondary.getOrDefault(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, -1));
			placeBlockWithItem(level, player, secondaryPos, secondary);
			BackpackBlockEntity secondaryPlaced = requirePlacedBackpack(level, secondaryPos, "secondary");

			IBackpackWrapper placedPrimaryFacade = resolve(level, primaryPlaced.getBackpackWrapper().getBackpack(), "placed primary");
			IBackpackWrapper placedSecondaryFacade = resolve(level, secondaryPlaced.getBackpackWrapper().getBackpack(), "placed secondary");
			placedEndpointsShare = primaryEndpoint.groupId().equals(secondaryEndpoint.groupId())
					&& !primaryEndpoint.endpointId().equals(secondaryEndpoint.endpointId())
					&& placedPrimaryFacade.getContentsUuid().equals(placedSecondaryFacade.getContentsUuid())
					&& placedPrimaryFacade.getInventoryHandler() == placedSecondaryFacade.getInventoryHandler()
					&& hasStack(placedPrimaryFacade, 0, Items.DIAMOND, 5) && hasStack(placedSecondaryFacade, 0, Items.DIAMOND, 5);
			close(placedPrimaryFacade);
			close(placedSecondaryFacade);
			assertTrue(placedEndpointsShare, "Placed endpoints did not share one canonical linked storage root");

			IBackpackWrapper secondaryCanonicalHost = BackpackLinkedStorageResolver
					.resolveCanonicalHost(level, secondaryPlaced.getBackpackWrapper().getBackpack())
					.orElseThrow(() -> new IllegalStateException("Secondary endpoint did not resolve its canonical host"));
			IBackpackWrapper secondaryFacade = resolve(level, secondaryPlaced.getBackpackWrapper().getBackpack(), "secondary server access");
			ResourceHandler<ItemResource> secondaryItems = Objects.requireNonNull(level.getCapability(Capabilities.Item.BLOCK, secondaryPos, Direction.UP),
					"Placed secondary backpack did not expose an item handler");
			ItemStack secondaryCapabilityStack = secondaryItems.getResource(0).toStack(secondaryItems.getAmountAsInt(0));
			secondaryServerAccessPrimaryProcessingOnly = secondaryEndpoint.groupId().equals(secondaryFacade.getContentsUuid().orElse(null))
					&& secondaryCanonicalHost.getInventoryHandler() == secondaryFacade.getInventoryHandler()
					&& hasStack(secondaryCanonicalHost, 0, Items.DIAMOND, 5) && secondaryCapabilityStack.is(Items.DIAMOND)
					&& secondaryCapabilityStack.getCount() == 5
					&& BackpackLinkedStorageResolver.resolvePrimaryCanonicalHost(level, secondaryPlaced.getBackpackWrapper().getBackpack()).isEmpty()
					&& BackpackLinkedStorageResolver.resolveForGlobalUpgradeProcessing(level,
							secondaryPlaced.getBackpackWrapper().getBackpack()) == IBackpackWrapper.Noop.INSTANCE;
			close(secondaryFacade);
			assertTrue(secondaryServerAccessPrimaryProcessingOnly,
					"Secondary endpoint did not provide canonical server access or was accepted for primary-only processing");

			endpointLocalPresentationRetained = hasEndpointPresentation(primaryPlaced.getBackpackWrapper().getBackpack(), primaryEndpoint, PRIMARY_MAIN_COLOR,
					PRIMARY_ACCENT_COLOR, PRIMARY_NAME, 1)
					&& hasEndpointPresentation(secondaryPlaced.getBackpackWrapper().getBackpack(), secondaryEndpoint, SECONDARY_MAIN_COLOR,
							SECONDARY_ACCENT_COLOR, SECONDARY_NAME, 2);
			assertTrue(endpointLocalPresentationRetained, "Placed endpoints did not retain endpoint-local colors, names, and tabs");

			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			BlockHitResult primaryMutationHit = new BlockHitResult(Vec3.atCenterOf(primaryPos), Direction.UP, primaryPos, false);
			player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, primaryMutationHit);
			assertTrue(player.containerMenu instanceof BackpackContainer, "Placed linked backpack did not open a backpack menu");
			BackpackContainer menu = (BackpackContainer) player.containerMenu;
			assertTrue(menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper, "Placed linked backpack menu did not use a linked storage facade");
			menu.getStorageWrapper().getInventoryHandler().setStackInSlot(MUTATION_SLOT, new ItemStack(Items.EMERALD, 3));
			menu.getStorageWrapper().getInventoryHandler().saveInventory();
			menu.getStorageWrapper().onContentsUpdated();
			menu.broadcastChanges();
			player.closeContainer();
			placedMenuMutationSynced = hasStack(secondaryPlaced.getBackpackWrapper(), MUTATION_SLOT, Items.EMERALD, 3);
			assertTrue(placedMenuMutationSynced, "Placed menu mutation was not visible through the other endpoint");

			ResourceHandler<ItemResource> primaryItems = Objects.requireNonNull(level.getCapability(Capabilities.Item.BLOCK, primaryPos, Direction.UP),
					"Placed primary backpack did not expose an item handler");
			int inserted;
			try (Transaction tx = Transaction.openRoot()) {
				inserted = primaryItems.insert(EXTERNAL_MUTATION_SLOT, ItemResource.of(Items.COPPER_INGOT), 4, tx);
				if (inserted > 0) {
					tx.commit();
				}
			}
			externalItemAutomationSynced = inserted == 4 && hasStack(secondaryPlaced.getBackpackWrapper(), EXTERNAL_MUTATION_SLOT, Items.COPPER_INGOT, 4);
			assertTrue(externalItemAutomationSynced, "External item automation did not synchronize between placed endpoints");

			ResourceHandler<FluidResource> primaryFluid = Objects.requireNonNull(level.getCapability(Capabilities.Fluid.BLOCK, primaryPos, Direction.UP),
					"Placed primary backpack did not expose a fluid handler");
			ResourceHandler<FluidResource> secondaryFluid = Objects.requireNonNull(level.getCapability(Capabilities.Fluid.BLOCK, secondaryPos, Direction.UP),
					"Placed secondary backpack did not expose a fluid handler");
			int filled;
			try (Transaction tx = Transaction.openRoot()) {
				filled = primaryFluid.insert(FluidResource.of(Fluids.WATER), 1000, tx);
				if (filled > 0) {
					tx.commit();
				}
			}
			externalFluidAutomationSynced = filled > 0 && secondaryFluid.size() > 0 && secondaryFluid.getResource(0).is(Fluids.WATER)
					&& secondaryFluid.getAmountAsInt(0) == filled;
			assertTrue(externalFluidAutomationSynced, "External fluid automation did not synchronize between placed endpoints");

			EnergyHandler secondaryEnergy = Objects.requireNonNull(level.getCapability(Capabilities.Energy.BLOCK, secondaryPos, Direction.UP),
					"Placed secondary backpack did not expose an energy storage");
			EnergyHandler primaryEnergy = Objects.requireNonNull(level.getCapability(Capabilities.Energy.BLOCK, primaryPos, Direction.UP),
					"Placed primary backpack did not expose an energy storage");
			int received = 0;
			while (secondaryEnergy.getAmountAsInt() <= secondaryEnergy.getCapacityAsInt() / 8) {
				int transferred;
				try (Transaction tx = Transaction.openRoot()) {
					transferred = secondaryEnergy.insert(Integer.MAX_VALUE, tx);
					if (transferred > 0) {
						tx.commit();
					}
				}
				if (transferred == 0) {
					break;
				}
				received += transferred;
			}
			int receivedEnergy = received;
			externalEnergyAutomationSynced = received > 0 && primaryEnergy.getAmountAsInt() == secondaryEnergy.getAmountAsInt()
					&& primaryEnergy.getAmountAsInt() == received;
			assertTrue(externalEnergyAutomationSynced, "External energy automation did not synchronize between placed endpoints");

			boolean primaryItemRendered = hasRenderedItem(primaryPlaced, Items.DIAMOND);
			boolean secondaryItemRendered = hasRenderedItem(secondaryPlaced, Items.DIAMOND);
			boolean primaryFluidRendered = hasRenderedFluid(primaryPlaced, Fluids.WATER);
			boolean secondaryFluidRendered = hasRenderedFluid(secondaryPlaced, Fluids.WATER);
			boolean primaryEnergyRendered = hasRenderedEnergy(primaryPlaced);
			boolean secondaryEnergyRendered = hasRenderedEnergy(secondaryPlaced);
			placedCanonicalContentsRendered = primaryItemRendered && secondaryItemRendered && primaryFluidRendered && secondaryFluidRendered
					&& primaryEnergyRendered && secondaryEnergyRendered;
			assertTrue(placedCanonicalContentsRendered,
					"Placed backpacks did not render the canonical contents at both endpoints. Item=" + primaryItemRendered + "/" + secondaryItemRendered
							+ ", fluid=" + primaryFluidRendered + "/" + secondaryFluidRendered + ", energy=" + primaryEnergyRendered + "/"
							+ secondaryEnergyRendered);

			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setShiftKeyDown(true);
			BlockHitResult secondaryHit = new BlockHitResult(Vec3.atCenterOf(secondaryPos), Direction.UP, secondaryPos, false);
			player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, secondaryHit);
			player.setShiftKeyDown(false);
			ItemStack pickedUp = player.getMainHandItem();
			IBackpackWrapper pickedUpFacade = resolve(level, pickedUp, "picked up secondary");
			placedToCarriedSharing = level.getBlockState(secondaryPos).isAir()
					&& hasEndpointPresentation(pickedUp, secondaryEndpoint, SECONDARY_MAIN_COLOR, SECONDARY_ACCENT_COLOR, SECONDARY_NAME, 2)
					&& hasCanonicalContentsBeforeCarriedMutation(pickedUpFacade)
					&& pickedUpFacade.getFluidHandler().map(handler -> handler.size() > 0 && handler.getResource(0).is(Fluids.WATER)).orElse(false)
					&& pickedUpFacade.getEnergyHandler().map(storage -> storage.getAmountAsInt() == receivedEnergy).orElse(false);
			assertTrue(placedToCarriedSharing, "Pickup did not preserve a carried endpoint's canonical contents");

			pickedUpFacade.getInventoryHandler().setStackInSlot(CARRIED_MUTATION_SLOT, new ItemStack(Items.LAPIS_LAZULI, 2));
			pickedUpFacade.getInventoryHandler().saveInventory();
			pickedUpFacade.onContentsUpdated();
			close(pickedUpFacade);
			carriedToPlacedSharing = hasStack(primaryPlaced.getBackpackWrapper(), CARRIED_MUTATION_SLOT, Items.LAPIS_LAZULI, 2);
			assertTrue(carriedToPlacedSharing, "Carried endpoint mutation was not visible through the placed endpoint");

			// Shift-pickup intentionally applies a short item cooldown. The fixture runs replacement in the same server task rather than after player ticks.
			player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(pickedUp));
			InteractionResult replacementPlacement = placeBlockWithItem(level, player, secondaryPos, pickedUp);
			assertTrue(replacementPlacement.consumesAction(),
					"Replacement endpoint placement did not consume the action: " + replacementPlacement + ", stack=" + pickedUp);
			secondaryPlaced = requirePlacedBackpack(level, secondaryPos, "replacement secondary");
			replacementRetainedEndpointAndContents = hasEndpointPresentation(secondaryPlaced.getBackpackWrapper().getBackpack(), secondaryEndpoint,
					SECONDARY_MAIN_COLOR, SECONDARY_ACCENT_COLOR, SECONDARY_NAME, 2) && hasCanonicalContents(secondaryPlaced.getBackpackWrapper());
			assertTrue(replacementRetainedEndpointAndContents, "Replacement placement did not retain endpoint identity and canonical contents");

			assertTrue(player.gameMode.destroyBlock(secondaryPos), "Normal secondary block break did not succeed");
			ItemStack brokenSecondary = ItemStack.EMPTY;
			for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class,
					new AABB(secondaryPos.getX() - 1, secondaryPos.getY() - 1, secondaryPos.getZ() - 1, secondaryPos.getX() + 2, secondaryPos.getY() + 2,
							secondaryPos.getZ() + 2),
					entity -> secondaryEndpoint.equals(entity.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))) {
				itemEntity.setPickUpDelay(0);
				itemEntity.playerTouch(player);
				break;
			}
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (secondaryEndpoint.equals(stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))) {
					brokenSecondary = stack;
					break;
				}
			}
			IBackpackWrapper brokenSecondaryFacade = resolve(level, brokenSecondary, "normally broken secondary");
			normalBreakPickupRetainedEndpointAndContents = level.getBlockState(secondaryPos).isAir()
					&& hasEndpointPresentation(brokenSecondary, secondaryEndpoint, SECONDARY_MAIN_COLOR, SECONDARY_ACCENT_COLOR, SECONDARY_NAME, 2)
					&& hasCanonicalContents(brokenSecondaryFacade)
					&& brokenSecondaryFacade.getFluidHandler().map(handler -> handler.size() > 0 && handler.getResource(0).is(Fluids.WATER)).orElse(false)
					&& brokenSecondaryFacade.getEnergyHandler().map(storage -> storage.getAmountAsInt() == receivedEnergy).orElse(false);
			close(brokenSecondaryFacade);
			assertTrue(normalBreakPickupRetainedEndpointAndContents,
					"Normal block break and pickup did not retain the secondary endpoint identity and canonical contents");

			ItemStack worldLinkPrimary = new ItemStack(ModItems.IRON_BACKPACK.get());
			IBackpackWrapper worldLinkPrimaryWrapper = BackpackWrapper.fromStack(worldLinkPrimary);
			worldLinkPrimaryWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.REDSTONE, 7));
			worldLinkPrimaryWrapper.getInventoryHandler().saveInventory();
			configureCanonicalItemDisplay(worldLinkPrimary);
			placeBlockWithItem(level, player, worldLinkPrimaryPos, worldLinkPrimary);
			ItemStack worldLinker = new ItemStack(ENDER_LINKER.get(), 2);
			useLinkerOnBlock(level, player, worldLinkPrimaryPos, worldLinker);
			BackpackBlockEntity worldLinkPrimaryPlaced = requirePlacedBackpack(level, worldLinkPrimaryPos, "world-linked primary");
			LinkedStorageEndpointData worldLinkPrimaryEndpoint = requireEndpoint(worldLinkPrimaryPlaced.getBackpackWrapper().getBackpack(),
					"world-linked primary");
			ItemStack worldBoundLinker = findBoundLinker(player, worldLinkPrimaryEndpoint.groupId(), ItemStack.EMPTY);
			EnderLinkerTargetData worldLinkTarget = worldBoundLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET);
			placedPrimaryLinked = worldLinker.getCount() == 1 && !worldLinker.has(ModCoreDataComponents.ENDER_LINKER_TARGET) && worldLinkTarget != null
					&& worldLinkTarget.groupId().equals(worldLinkPrimaryEndpoint.groupId())
					&& hasStack(worldLinkPrimaryPlaced.getBackpackWrapper(), 0, Items.REDSTONE, 7);
			assertTrue(placedPrimaryLinked, "Blank linker stack did not preserve its remaining blank linker and bind one primary");

			ItemStack worldLinkSecondary = new ItemStack(ModItems.GOLD_BACKPACK.get());
			placeBlockWithItem(level, player, worldLinkSecondaryPos, worldLinkSecondary);
			BackpackBlockEntity worldLinkSecondaryPlaced = requirePlacedBackpack(level, worldLinkSecondaryPos, "world-linked secondary");
			// Placement initializes empty slot padding; keep this fixture equivalent to a blank craft endpoint.
			worldLinkSecondaryPlaced.getBackpackWrapper().getContentsUuid().ifPresent(BackpackStorage.get()::removeBackpackContents);
			useLinkerOnBlock(level, player, worldLinkSecondaryPos, worldBoundLinker);
			LinkedStorageEndpointData worldLinkSecondaryEndpoint = requireEndpoint(worldLinkSecondaryPlaced.getBackpackWrapper().getBackpack(),
					"world-linked secondary");
			placedSecondaryLinked = worldBoundLinker.isEmpty() && worldLinkPrimaryEndpoint.groupId().equals(worldLinkSecondaryEndpoint.groupId())
					&& !worldLinkPrimaryEndpoint.endpointId().equals(worldLinkSecondaryEndpoint.endpointId())
					&& hasStack(worldLinkSecondaryPlaced.getBackpackWrapper(), 0, Items.REDSTONE, 7) && hasRenderedItem(worldLinkPrimaryPlaced, Items.REDSTONE)
					&& hasRenderedItem(worldLinkSecondaryPlaced, Items.REDSTONE);
			assertTrue(placedSecondaryLinked, "Bound linker did not bind the placed secondary backpack");

			IStorageWrapper worldLinkHost = BackpackLinkedStorageResolver
					.resolvePrimaryCanonicalHost(level, worldLinkPrimaryPlaced.getBackpackWrapper().getBackpack()).orElseThrow();
			jukeboxPlaybackAnchorsToPrimary = worldLinkHost instanceof IJukeboxPlaybackLocationProvider playbackLocationProvider
					&& playbackLocationProvider.getJukeboxPlaybackLocation(level)
							.filter(location -> worldLinkPrimaryPos.equals(location.blockPos()) && location.entity() == null).isPresent();
			assertTrue(jukeboxPlaybackAnchorsToPrimary, "Linked Storage Jukebox playback did not anchor to the primary block");

			ItemStack blankExistingLinker = new ItemStack(ENDER_LINKER.get());
			useLinkerOnBlock(level, player, worldLinkSecondaryPos, blankExistingLinker);
			existingBlockBindsBlankLinker = blankExistingLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET) != null
					&& worldLinkPrimaryEndpoint.groupId().equals(blankExistingLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId())
					&& worldLinkSecondaryEndpoint
							.equals(worldLinkSecondaryPlaced.getBackpackWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
			assertTrue(existingBlockBindsBlankLinker, "Blank linker did not bind to the existing placed linked storage group");

			EnderLinkerTargetData boundTargetBeforeClick = blankExistingLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET);
			useLinkerOnBlock(level, player, worldLinkSecondaryPos, blankExistingLinker);
			boundLinkerLeavesExistingBlockUnchanged = boundTargetBeforeClick.equals(blankExistingLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET))
					&& worldLinkSecondaryEndpoint
							.equals(worldLinkSecondaryPlaced.getBackpackWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
			assertTrue(boundLinkerLeavesExistingBlockUnchanged, "Bound linker changed an already linked placed endpoint");

			player.setGameMode(GameType.CREATIVE);
			ItemStack creativeEndpoint = brokenSecondary.copy();
			placeBlockWithItem(level, player, creativePos, creativeEndpoint);
			BackpackBlockEntity creativePlaced = requirePlacedBackpack(level, creativePos, "creative secondary");
			LinkedStorageEndpointData creativeEndpointData = requireEndpoint(creativePlaced.getBackpackWrapper().getBackpack(), "creative secondary");
			creativePlacementCopiesSecondaryEndpoint = secondaryEndpoint.equals(player.getMainHandItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& secondaryEndpoint.groupId().equals(creativeEndpointData.groupId())
					&& !secondaryEndpoint.endpointId().equals(creativeEndpointData.endpointId())
					&& hasEndpointPresentation(creativePlaced.getBackpackWrapper().getBackpack(), creativeEndpointData, SECONDARY_MAIN_COLOR,
							SECONDARY_ACCENT_COLOR, SECONDARY_NAME, 2)
					&& hasCanonicalContents(creativePlaced.getBackpackWrapper());
			assertTrue(creativePlacementCopiesSecondaryEndpoint, "Creative placement did not create a secondary linked endpoint");

			long revisionBeforeFailedCreativePlacement = LinkedStorageGroupsSavedData.get(level).manager().getRevision(secondaryEndpoint.groupId());
			level.setBlockAndUpdate(failedCreativePos, Blocks.OBSIDIAN.defaultBlockState());
			InteractionResult failedCreativePlacement = placeBlockWithItem(level, player, failedCreativePos, brokenSecondary.copy());
			failedCreativePlacementDoesNotRegisterEndpoint = !failedCreativePlacement.consumesAction()
					&& LinkedStorageGroupsSavedData.get(level).manager().getRevision(secondaryEndpoint.groupId()) == revisionBeforeFailedCreativePlacement;
			assertTrue(failedCreativePlacementDoesNotRegisterEndpoint, "Failed creative placement registered a linked endpoint");
		} catch (RuntimeException e) {
			error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		} finally {
			player.closeContainer();
			player.setShiftKeyDown(false);
			player.getInventory().clearContent();
			player.getInventory().setChanged();
			player.setGameMode(originalGameMode);
			clearArea(level, primaryPos);
			clearArea(level, secondaryPos);
			clearArea(level, creativePos);
			clearArea(level, failedCreativePos);
			clearArea(level, worldLinkPrimaryPos);
			clearArea(level, worldLinkSecondaryPos);
		}

		boolean passed = primaryMigrated && groupOwnerRecorded && endpointAccessRecorded && boundLinkerVisual && existingEndpointBindsLinker
				&& invalidCraftRejected && primaryTierUpgradePropagated && secondaryTierUpgradeRejected && differentTierEndpointAccepted && placedEndpointsShare
				&& secondaryServerAccessPrimaryProcessingOnly && placedCanonicalContentsRendered && endpointLocalPresentationRetained
				&& placedMenuMutationSynced && externalItemAutomationSynced && externalFluidAutomationSynced && externalEnergyAutomationSynced
				&& placedToCarriedSharing && carriedToPlacedSharing && replacementRetainedEndpointAndContents && normalBreakPickupRetainedEndpointAndContents
				&& placedPrimaryLinked && placedSecondaryLinked && existingBlockBindsBlankLinker && boundLinkerLeavesExistingBlockUnchanged
				&& jukeboxPlaybackAnchorsToPrimary && creativePlacementCopiesSecondaryEndpoint && failedCreativePlacementDoesNotRegisterEndpoint;
		return "{\"ok\":" + passed + ",\"primaryMigrated\":" + primaryMigrated + ",\"groupOwnerRecorded\":" + groupOwnerRecorded
				+ ",\"endpointAccessRecorded\":" + endpointAccessRecorded + ",\"boundLinkerVisual\":" + boundLinkerVisual + ",\"existingEndpointBindsLinker\":"
				+ existingEndpointBindsLinker + ",\"invalidCraftRejected\":" + invalidCraftRejected + ",\"primaryTierUpgradePropagated\":"
				+ primaryTierUpgradePropagated + ",\"secondaryTierUpgradeRejected\":" + secondaryTierUpgradeRejected + ",\"differentTierEndpointAccepted\":"
				+ differentTierEndpointAccepted + ",\"placedEndpointsShare\":" + placedEndpointsShare + ",\"secondaryServerAccessPrimaryProcessingOnly\":"
				+ secondaryServerAccessPrimaryProcessingOnly + ",\"placedCanonicalContentsRendered\":" + placedCanonicalContentsRendered
				+ ",\"endpointLocalPresentationRetained\":" + endpointLocalPresentationRetained + ",\"placedMenuMutationSynced\":" + placedMenuMutationSynced
				+ ",\"externalItemAutomationSynced\":" + externalItemAutomationSynced + ",\"externalFluidAutomationSynced\":" + externalFluidAutomationSynced
				+ ",\"externalEnergyAutomationSynced\":" + externalEnergyAutomationSynced + ",\"placedToCarriedSharing\":" + placedToCarriedSharing
				+ ",\"carriedToPlacedSharing\":" + carriedToPlacedSharing + ",\"replacementRetainedEndpointAndContents\":"
				+ replacementRetainedEndpointAndContents + ",\"normalBreakPickupRetainedEndpointAndContents\":" + normalBreakPickupRetainedEndpointAndContents
				+ ",\"placedPrimaryLinked\":" + placedPrimaryLinked + ",\"placedSecondaryLinked\":" + placedSecondaryLinked
				+ ",\"existingBlockBindsBlankLinker\":" + existingBlockBindsBlankLinker + ",\"boundLinkerLeavesExistingBlockUnchanged\":"
				+ boundLinkerLeavesExistingBlockUnchanged + ",\"jukeboxPlaybackAnchorsToPrimary\":" + jukeboxPlaybackAnchorsToPrimary
				+ ",\"creativePlacementCopiesSecondaryEndpoint\":" + creativePlacementCopiesSecondaryEndpoint
				+ ",\"failedCreativePlacementDoesNotRegisterEndpoint\":" + failedCreativePlacementDoesNotRegisterEndpoint + "," + jsonProperty("error", error)
				+ "}";
	}

	private static void runClientCanonicalStorageSizeRegression() {
		ClientStorageSizeFixture fixture = AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::prepareClientCanonicalStorageSizeRegression);
		try {
			AutomationRuntime.runOnClient(() -> setupClientCanonicalStorageSizeRegression(fixture));
			AutomationRuntime.runOnClient(() -> assertClientActivityParticleRoles(fixture));
			AutomationRuntime.runOnServer(player -> {
				openPlacedBackpack(player, fixture.diamondPrimaryPos());
				return true;
			});
			waitForClientLinkedMenu(ModItems.DIAMOND_BACKPACK.get().getNumberOfSlots(), Items.DIAMOND, CLIENT_PRIMARY_NAME, "primary first open");
			AutomationRuntime.runOnServer(player -> {
				ItemDisplaySettingsCategory itemDisplaySettings = requirePlacedBackpack((ServerLevel) player.level(), fixture.diamondPrimaryPos(),
						"storage size Diamond primary").getBackpackWrapper().getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
				itemDisplaySettings.selectSlot(0);
				itemDisplaySettings.setDisplaySide(DisplaySide.FRONT);
				itemDisplaySettings.itemsChanged();
				return true;
			});
			waitForClientRenderedItem(fixture.diamondPrimaryPos(), Items.DIAMOND, "primary display setting");
			waitForClientRenderedItem(fixture.diamondSecondaryPos(), Items.DIAMOND, "secondary display projection");
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				openPlacedBackpack(player, fixture.diamondPrimaryPos());
				return true;
			});
			waitForClientLinkedMenu(ModItems.DIAMOND_BACKPACK.get().getNumberOfSlots(), Items.DIAMOND, CLIENT_PRIMARY_NAME, "primary second open");
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				openPlacedBackpack(player, fixture.diamondSecondaryPos());
				return true;
			});
			waitForClientLinkedMenu(ModItems.DIAMOND_BACKPACK.get().getNumberOfSlots(), Items.DIAMOND, CLIENT_PRIMARY_NAME, "secondary second open");
			verifyClientCarriedSecondaryPickupSynchronizes(fixture.diamondSecondaryPos());
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				openPlacedBackpack(player, fixture.goldSecondaryPos());
				return true;
			});
			waitForClientLinkedMenu(ModItems.IRON_BACKPACK.get().getNumberOfSlots(), Items.REDSTONE,
					Component.translatable(ModItems.IRON_BACKPACK.get().getDescriptionId()), "Gold secondary with an Iron primary first open");
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				openPlacedBackpack(player, fixture.goldSecondaryPos());
				return true;
			});
			waitForClientLinkedMenu(ModItems.IRON_BACKPACK.get().getNumberOfSlots(), Items.REDSTONE,
					Component.translatable(ModItems.IRON_BACKPACK.get().getDescriptionId()), "Gold secondary with an Iron primary second open");
		} finally {
			AutomationRuntime.runOnClient(() -> clearClientCanonicalStorageSizeRegression(fixture));
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea((ServerLevel) player.level(), fixture.diamondPrimaryPos());
				clearArea((ServerLevel) player.level(), fixture.diamondSecondaryPos());
				clearArea((ServerLevel) player.level(), fixture.ironPrimaryPos());
				clearArea((ServerLevel) player.level(), fixture.goldSecondaryPos());
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				return true;
			});
		}
	}

	private static Boolean assertClientActivityParticleRoles(ClientStorageSizeFixture fixture) {
		if (Minecraft.getInstance().level == null) {
			throw new IllegalStateException("Client level is unavailable for Linked Storage activity particle validation");
		}
		BackpackBlockEntity primary = Minecraft.getInstance().level.getBlockEntity(fixture.diamondPrimaryPos(), ModBlocks.BACKPACK_TILE_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Client primary Linked Storage Backpack is unavailable"));
		BackpackBlockEntity secondary = Minecraft.getInstance().level.getBlockEntity(fixture.diamondSecondaryPos(), ModBlocks.BACKPACK_TILE_TYPE.get())
				.orElseThrow(() -> new IllegalStateException("Client secondary Linked Storage Backpack is unavailable"));
		assertTrue(BackpackItem.shouldRenderUpgradeActivity(primary.getBackpackWrapper().getBackpack()),
				"Primary Linked Storage Backpack activity particles were suppressed");
		assertTrue(!BackpackItem.shouldRenderUpgradeActivity(secondary.getBackpackWrapper().getBackpack()),
				"Secondary Linked Storage Backpack activity particles were not suppressed");
		return true;
	}

	private static void verifyClientCarriedSecondaryPickupSynchronizes(BlockPos endpointPos) {
		LinkedStorageEndpointData endpoint = AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setShiftKeyDown(true);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(endpointPos), Direction.UP, endpointPos, false);
			player.gameMode.useItemOn(player, (ServerLevel) player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
			player.setShiftKeyDown(false);
			ItemStack carried = player.getMainHandItem();
			LinkedStorageEndpointData carriedEndpoint = carried.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
			if (!(carried.getItem() instanceof BackpackItem) || carriedEndpoint == null) {
				throw new IllegalStateException("Linked secondary endpoint was not picked up into the main hand");
			}
			player.inventoryMenu.broadcastChanges();
			return carriedEndpoint;
		});

		waitForClientCarriedEndpoint(endpoint, "Linked secondary endpoint was not synchronized to the client main hand");
		AutomationRuntime.runOnServer(player -> {
			((BackpackItem) player.getMainHandItem().getItem()).use((ServerLevel) player.level(), player, InteractionHand.MAIN_HAND);
			return true;
		});
		waitForClientCarriedLinkedMenu(endpoint);

		AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = getClientLinkedCarriedBackpackMenu(endpoint);
			Slot sourceSlot = menu.getSlot(0);
			if (!sourceSlot.getItem().is(Items.DIAMOND)) {
				throw new IllegalStateException("Carried linked secondary source slot does not contain the expected Diamond before pickup");
			}
			clickClientSlot(sourceSlot);
			return true;
		});

		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean synchronizedPickup = AutomationRuntime.runOnClient(() -> {
				BackpackContainer menu = getClientLinkedCarriedBackpackMenu(endpoint);
				return menu.getSlot(0).getItem().isEmpty() && menu.getCarried().is(Items.DIAMOND);
			});
			if (synchronizedPickup) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted waiting for carried linked secondary pickup synchronization", e);
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Carried linked secondary source slot stayed desynchronized after picking up its Diamond");
	}

	private static void runClientLinkedStorageFeedbackRegression() {
		ClientFeedbackLinkedStorageFixture fixture = AutomationRuntime
				.runOnServer(BackpackLinkedStorageRegression::prepareClientLinkedStorageFeedbackRegression);
		try {
			waitForClientFeedbackLinker(fixture);
			AutomationRuntime.runOnClient(() -> {
				linkClientFeedbackPlacedEndpoint(fixture);
				return true;
			});
			waitForServerFeedbackPlacedEndpoint(fixture);
			waitForClientFeedbackPlacedEndpoint(fixture);
			AutomationRuntime.runOnServer(player -> selectClientFeedbackHotbarSlot(player, CLIENT_FEEDBACK_PRIMARY_SLOT));
			waitForClientFeedbackSelectedSlot(fixture.primaryEndpoint(), CLIENT_FEEDBACK_PRIMARY_SLOT, "primary selection");

			openClientFeedbackItemBackpack(fixture);
			waitForClientFeedbackMenu(fixture, false, false, "item primary initial open");
			waitForFeedbackState(fixture, false, false, false, true, "item primary initial profile");
			closeClientFeedbackMenu(false, "item primary before inventory stash");

			long stashRevision = AutomationRuntime
					.runOnServer(player -> LinkedStorageGroupsSavedData.get((ServerLevel) player.level()).manager().getRevision(fixture.groupId()));
			stashIntoClientFeedbackBackpack(fixture);
			waitForServerFeedbackStash(fixture, stashRevision);
			waitForClientFeedbackStash(fixture);

			openClientFeedbackItemBackpack(fixture);
			waitForClientFeedbackMenu(fixture, false, false, "item primary after inventory stash");
			waitForClientFeedbackStashView(fixture);
			waitForFeedbackState(fixture, false, false, false, true, "item primary stash reopen");
			toggleClientFeedbackTankUpgrade(fixture, false, "item primary insert");
			waitForFeedbackState(fixture, false, true, false, false, "item primary insert");
			roundTripClientFeedbackSettings();
			openClientFeedbackItemBackpack(fixture);
			waitForClientFeedbackMenu(fixture, false, true, "item primary settings reopen");
			waitForFeedbackState(fixture, false, true, false, true, "item primary settings round trip");
			toggleClientFeedbackTankUpgrade(fixture, false, "item primary remove");
			waitForFeedbackState(fixture, false, false, true, false, "item primary remove");
			closeClientFeedbackMenu(false, "item primary remove");
			openClientFeedbackItemBackpack(fixture);
			waitForClientFeedbackMenu(fixture, false, false, "item primary remove reopen");
			toggleClientFeedbackTankUpgrade(fixture, false, "item primary reinsert");
			waitForFeedbackState(fixture, false, true, false, false, "item primary reinsert");
			closeClientFeedbackMenu(false, "item primary after reinsert");

			AutomationRuntime.runOnServer(player -> selectClientFeedbackHotbarSlot(player, CLIENT_FEEDBACK_INTERACTION_SLOT));
			waitForClientFeedbackSelectedSlot(null, CLIENT_FEEDBACK_INTERACTION_SLOT, "placed endpoint interaction selection");
			openClientFeedbackPlacedBackpack(fixture);
			waitForClientFeedbackMenu(fixture, true, true, "placed secondary initial open");
			waitForFeedbackState(fixture, true, true, false, true, "placed secondary initial profile");
			toggleClientFeedbackTankUpgrade(fixture, true, "placed secondary remove");
			waitForFeedbackState(fixture, true, false, true, false, "placed secondary remove");
			closeClientFeedbackMenu(true, "placed secondary remove");
			openClientFeedbackPlacedBackpack(fixture);
			waitForClientFeedbackMenu(fixture, true, false, "placed secondary remove reopen");
			toggleClientFeedbackTankUpgrade(fixture, true, "placed secondary insert");
			waitForFeedbackState(fixture, true, true, false, false, "placed secondary insert");
			closeClientFeedbackMenu(true, "placed secondary insert");
			openClientFeedbackPlacedBackpack(fixture);
			waitForClientFeedbackMenu(fixture, true, true, "placed secondary final reopen");
			waitForFeedbackState(fixture, true, true, false, true, "placed secondary final profile");
		} finally {
			AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().gui.screen() instanceof BackpackScreen screen) {
					screen.onClose();
				}
				return true;
			});
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				clearArea((ServerLevel) player.level(), fixture.placedEndpointPos());
				return true;
			});
		}
	}

	private static ClientFeedbackLinkedStorageFixture prepareClientLinkedStorageFeedbackRegression(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		BlockPos placedEndpointPos = player.blockPosition().relative(player.getDirection(), 2);
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		clearArea(level, placedEndpointPos);

		ItemStack primary = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		primary.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, ModItems.DIAMOND_BACKPACK.get().getNumberOfSlots());
		primary.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, ModItems.DIAMOND_BACKPACK.get().getNumberOfUpgradeSlots());
		IBackpackWrapper ordinaryPrimary = BackpackWrapper.fromStack(primary);
		ordinaryPrimary.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
		ordinaryPrimary.getInventoryHandler().saveInventory();
		ordinaryPrimary.getUpgradeHandler().saveInventory();
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
		ItemStack placedPhysical = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		UUID placedStorageUuid = UUID.randomUUID();
		placedPhysical.set(ModCoreDataComponents.STORAGE_UUID, placedStorageUuid);
		placedEndpoint.setBackpack(placedPhysical);
		BackpackStorage.get().setBackpackContents(placedStorageUuid, new ContainerContents());
		player.getInventory().setItem(CLIENT_FEEDBACK_PRIMARY_SLOT, primary);
		player.getInventory().setItem(CLIENT_FEEDBACK_LINKER_SLOT, linker);
		player.getInventory().setItem(CLIENT_FEEDBACK_STASH_SLOT, new ItemStack(Items.EMERALD, CLIENT_FEEDBACK_STASH_COUNT));
		player.getInventory().setItem(CLIENT_FEEDBACK_STASH_SLOT + 1, new ItemStack(ModItems.TANK_UPGRADE.get()));
		player.getInventory().setSelectedSlot(CLIENT_FEEDBACK_LINKER_SLOT);
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		player.connection.send(new ClientboundSetHeldSlotPacket(CLIENT_FEEDBACK_LINKER_SLOT));
		return new ClientFeedbackLinkedStorageFixture(placedEndpointPos, primaryEndpoint, canonicalProfile, originalGameMode);
	}

	private static void waitForClientFeedbackLinker(ClientFeedbackLinkedStorageFixture fixture) {
		waitForClientFeedback(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().level != null
				&& Minecraft.getInstance().player.getInventory().getSelectedSlot() == CLIENT_FEEDBACK_LINKER_SLOT
				&& Minecraft.getInstance().player.getMainHandItem().is(ENDER_LINKER.get())
				&& fixture.primaryEndpoint().equals(
						Minecraft.getInstance().player.getInventory().getItem(CLIENT_FEEDBACK_PRIMARY_SLOT).get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
				&& Minecraft.getInstance().level.getBlockEntity(fixture.placedEndpointPos()) instanceof BackpackBlockEntity, "linked-storage fixture");
	}

	private static void linkClientFeedbackPlacedEndpoint(ClientFeedbackLinkedStorageFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.getMainHandItem().is(ENDER_LINKER.get())) {
			throw new IllegalStateException("Client feedback linker is unavailable");
		}
		EnderLinkerTargetData target = minecraft.player.getMainHandItem().get(ModCoreDataComponents.ENDER_LINKER_TARGET);
		assertTrue(target != null && target.groupId().equals(fixture.groupId()),
				"Client feedback linker target does not match the primary linked-storage group");
		assertTrue(
				minecraft.gameMode
						.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
								new BlockHitResult(Vec3.atCenterOf(fixture.placedEndpointPos()), Direction.UP, fixture.placedEndpointPos(), false))
						.consumesAction(),
				"Client feedback linker use did not consume the placed Backpack interaction");
	}

	private static void waitForServerFeedbackPlacedEndpoint(ClientFeedbackLinkedStorageFixture fixture) {
		waitForFeedback(() -> AutomationRuntime.runOnServer(player -> {
			LinkedStorageEndpointData secondaryEndpoint = requirePlacedBackpack((ServerLevel) player.level(), fixture.placedEndpointPos(),
					"feedback placed secondary").getBackpackWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
			return secondaryEndpoint != null && secondaryEndpoint.groupId().equals(fixture.groupId())
					&& !secondaryEndpoint.endpointId().equals(fixture.primaryEndpoint().endpointId());
		}), "server placed linked Backpack endpoint");
	}

	private static void waitForClientFeedbackPlacedEndpoint(ClientFeedbackLinkedStorageFixture fixture) {
		waitForClientFeedback(
				() -> Minecraft.getInstance().level != null
						&& Minecraft.getInstance().level.getBlockEntity(fixture.placedEndpointPos()) instanceof BackpackBlockEntity backpack
						&& backpack.getBackpackWrapper().getBackpack()
								.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) instanceof LinkedStorageEndpointData endpoint
						&& endpoint.groupId().equals(fixture.groupId()) && !endpoint.endpointId().equals(fixture.primaryEndpoint().endpointId()),
				"placed linked Backpack endpoint");
	}

	private static Boolean selectClientFeedbackHotbarSlot(ServerPlayer player, int slot) {
		player.getInventory().setSelectedSlot(slot);
		player.connection.send(new ClientboundSetHeldSlotPacket(slot));
		return true;
	}

	private static void waitForClientFeedbackSelectedSlot(@Nullable LinkedStorageEndpointData primaryEndpoint, int slot, String description) {
		waitForClientFeedback(
				() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getInventory().getSelectedSlot() == slot
						&& (primaryEndpoint == null
								|| primaryEndpoint.equals(Minecraft.getInstance().player.getMainHandItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))),
				description);
	}

	private static void openClientFeedbackItemBackpack(ClientFeedbackLinkedStorageFixture fixture) {
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

	private static void openClientFeedbackPlacedBackpack(ClientFeedbackLinkedStorageFixture fixture) {
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

	private static void waitForClientFeedbackMenu(ClientFeedbackLinkedStorageFixture fixture, boolean placed, boolean tankPresent, String description) {
		waitForClientFeedback(() -> getClientFeedbackMenuProfile(fixture, placed)
				.map(profile -> matchesCanonicalProfile(profile, fixture.canonicalProfile(), tankPresent)).orElse(false), description + " Backpack menu");
	}

	private static Optional<StorageProfile> getClientFeedbackMenuProfile(ClientFeedbackLinkedStorageFixture fixture, boolean placed) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.gui.screen() instanceof BackpackScreen) || minecraft.player == null
				|| !(minecraft.player.containerMenu instanceof BackpackContainer menu) || !(menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper)
				|| placed != menu.getBlockPosition().filter(fixture.placedEndpointPos()::equals).isPresent()) {
			return Optional.empty();
		}
		LinkedStorageEndpointData endpoint = menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null || !endpoint.groupId().equals(fixture.groupId())
				|| !placed && !endpoint.endpointId().equals(fixture.primaryEndpoint().endpointId())) {
			return Optional.empty();
		}
		return Optional.of(snapshotFeedbackProfile(menu.getStorageWrapper(), menu.getNumberOfStorageInventorySlots()));
	}

	private static void closeClientFeedbackMenu(boolean placed, String description) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.gui.screen() instanceof BackpackScreen screen) || minecraft.player == null
					|| !(minecraft.player.containerMenu instanceof BackpackContainer menu) || placed != menu.getBlockPosition().isPresent()) {
				throw new IllegalStateException("Client feedback " + description + " Backpack screen was not open");
			}
			screen.onClose();
			return true;
		});
		waitForClientFeedbackInventoryMenu(description);
	}

	private static void waitForClientFeedbackInventoryMenu(String description) {
		waitForFeedback(() -> AutomationRuntime.runOnServer(player -> player.containerMenu == player.inventoryMenu) && AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.player != null && minecraft.player.containerMenu == minecraft.player.inventoryMenu
					&& !(minecraft.gui.screen() instanceof BackpackScreen) && !(minecraft.gui.screen() instanceof BackpackSettingsScreen);
		}), description + " close");
	}

	private static void stashIntoClientFeedbackBackpack(ClientFeedbackLinkedStorageFixture fixture) {
		waitForClientFeedback(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
				return false;
			}
			InventoryMenu menu = minecraft.player.inventoryMenu;
			int primaryMenuSlot = inventoryMenuSlot(CLIENT_FEEDBACK_PRIMARY_SLOT);
			int stashMenuSlot = inventoryMenuSlot(CLIENT_FEEDBACK_STASH_SLOT);
			assertTrue(fixture.primaryEndpoint().equals(menu.getSlot(primaryMenuSlot).getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)),
					"Client feedback primary inventory stack was not the linked endpoint");
			assertTrue(
					menu.getSlot(stashMenuSlot).getItem().is(Items.EMERALD) && menu.getSlot(stashMenuSlot).getItem().getCount() == CLIENT_FEEDBACK_STASH_COUNT,
					"Client feedback stash stack was not synchronized");
			assertTrue(menu.getCarried().isEmpty(), "Client feedback inventory started with a carried stack");
			minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
			minecraft.gameMode.handleContainerInput(menu.containerId, stashMenuSlot, 0, ContainerInput.PICKUP, minecraft.player);
			assertTrue(menu.getCarried().is(Items.EMERALD) && menu.getCarried().getCount() == CLIENT_FEEDBACK_STASH_COUNT,
					"Client feedback inventory click did not pick up the stash stack");
			minecraft.gameMode.handleContainerInput(menu.containerId, primaryMenuSlot, 0, ContainerInput.PICKUP, minecraft.player);
			return true;
		}, "InventoryScreen same-slot stash click");
	}

	private static int inventoryMenuSlot(int inventorySlot) {
		return inventorySlot < 9 ? InventoryMenu.USE_ROW_SLOT_START + inventorySlot : InventoryMenu.INV_SLOT_START + inventorySlot - 9;
	}

	private static void waitForServerFeedbackStash(ClientFeedbackLinkedStorageFixture fixture, long revisionBeforeStash) {
		waitForFeedback(() -> AutomationRuntime.runOnServer(player -> {
			Optional<IBackpackWrapper> canonicalHost = BackpackLinkedStorageResolver.resolveCanonicalHost((ServerLevel) player.level(),
					player.getInventory().getItem(CLIENT_FEEDBACK_PRIMARY_SLOT));
			if (canonicalHost.isEmpty()) {
				return false;
			}
			IBackpackWrapper canonical = canonicalHost.orElseThrow();
			try {
				BackpackBlockEntity placed = requirePlacedBackpack((ServerLevel) player.level(), fixture.placedEndpointPos(), "feedback placed secondary");
				return countFeedbackItems(canonical, Items.EMERALD) == CLIENT_FEEDBACK_STASH_COUNT
						&& countFeedbackItems(placed.getBackpackWrapper(), Items.EMERALD) == CLIENT_FEEDBACK_STASH_COUNT
						&& countFeedbackPlayerItems(player, Items.EMERALD) == 0 && player.containerMenu.getCarried().isEmpty()
						&& LinkedStorageGroupsSavedData.get((ServerLevel) player.level()).manager().getRevision(fixture.groupId()) > revisionBeforeStash;
			} finally {
				close(canonical);
			}
		}), "server inventory stash");
	}

	private static void waitForClientFeedbackStash(ClientFeedbackLinkedStorageFixture fixture) {
		waitForClientFeedback(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.player != null && minecraft.gui.screen() instanceof InventoryScreen
					&& minecraft.player.containerMenu == minecraft.player.inventoryMenu
					&& fixture.primaryEndpoint()
							.equals(minecraft.player.inventoryMenu.getSlot(inventoryMenuSlot(CLIENT_FEEDBACK_PRIMARY_SLOT)).getItem()
									.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
					&& minecraft.player.inventoryMenu.getSlot(inventoryMenuSlot(CLIENT_FEEDBACK_STASH_SLOT)).getItem().isEmpty()
					&& minecraft.player.inventoryMenu.getCarried().isEmpty();
		}, "inventory stash");
	}

	private static void waitForClientFeedbackStashView(ClientFeedbackLinkedStorageFixture fixture) {
		waitForClientFeedback(() -> getClientFeedbackMenuProfile(fixture, false)
				.map(profile -> countFeedbackItems(((BackpackContainer) Minecraft.getInstance().player.containerMenu).getStorageWrapper(),
						Items.EMERALD) == CLIENT_FEEDBACK_STASH_COUNT)
				.orElse(false), "linked Backpack stash view");
	}

	private static void toggleClientFeedbackTankUpgrade(ClientFeedbackLinkedStorageFixture fixture, boolean placed, String description) {
		AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = requireClientFeedbackMenu(fixture, placed);
			assertTrue(menu.getCarried().isEmpty(), "Client feedback " + description + " started with a carried stack");
			if (menu.upgradeSlots.get(CLIENT_FEEDBACK_TANK_SLOT).getItem().is(ModItems.TANK_UPGRADE.get())) {
				clickClientSlot(menu.upgradeSlots.get(CLIENT_FEEDBACK_TANK_SLOT));
				return true;
			}
			clickClientSlot(findClientFeedbackTankInPlayerInventory(menu));
			assertTrue(menu.getCarried().is(ModItems.TANK_UPGRADE.get()), "Client feedback " + description + " did not carry the Tank upgrade");
			menu = requireClientFeedbackMenu(fixture, placed); // Picking up the Tank can refresh the linked menu layout.
			Slot upgradeSlot = menu.upgradeSlots.get(CLIENT_FEEDBACK_TANK_SLOT);
			assertTrue(upgradeSlot.getItem().isEmpty(), "Client feedback " + description + " upgrade slot was not empty after layout refresh");
			clickClientSlot(upgradeSlot);
			return true;
		});
	}

	private static BackpackContainer requireClientFeedbackMenu(ClientFeedbackLinkedStorageFixture fixture, boolean placed) {
		return getClientFeedbackMenuProfile(fixture, placed).map(profile -> (BackpackContainer) Minecraft.getInstance().player.containerMenu)
				.orElseThrow(() -> new IllegalStateException("Client feedback linked Backpack menu is unavailable"));
	}

	private static Slot findClientFeedbackTankInPlayerInventory(BackpackContainer menu) {
		for (int slotIndex = menu.getNumberOfStorageInventorySlots(); slotIndex < menu.getInventorySlotsSize(); slotIndex++) {
			Slot slot = menu.getSlot(slotIndex);
			if (slot.getItem().is(ModItems.TANK_UPGRADE.get())) {
				return slot;
			}
		}
		throw new IllegalStateException("Client feedback Tank upgrade was not returned to the player inventory");
	}

	private static void roundTripClientFeedbackSettings() {
		AutomationRuntime.runOnClient(() -> {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen screen)) {
				throw new IllegalStateException("Client feedback item Backpack screen was not open for settings");
			}
			StorageSettingsTab settingsTab = findChild(screen, StorageSettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Client feedback storage settings tab was unavailable"));
			assertTrue(settingsTab.mouseClicked(new MouseButtonEvent(settingsTab.getX() + 9, settingsTab.getY() + 12, new MouseButtonInfo(0, 0)), false),
					"Client feedback storage settings tab did not handle the click");
			return true;
		});
		waitForFeedback(() -> AutomationRuntime
				.runOnServer(player -> player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
						&& settingsMenu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper)
				&& AutomationRuntime
						.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackSettingsScreen && Minecraft.getInstance().player != null
								&& Minecraft.getInstance().player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
								&& settingsMenu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper),
				"Backpack settings screen");
		AutomationRuntime.runOnClient(() -> {
			((BackpackSettingsScreen) Minecraft.getInstance().gui.screen()).onClose();
			return true;
		});
		waitForClientFeedbackInventoryMenu("Backpack settings screen");
	}

	private static void waitForFeedbackState(ClientFeedbackLinkedStorageFixture fixture, boolean placed, boolean tankPresent, boolean tankCarried,
			boolean requireCurrentCache, String description) {
		waitForFeedback(() -> {
			FeedbackServerSnapshot server = AutomationRuntime.runOnServer(player -> getFeedbackServerSnapshot(player, fixture));
			Optional<FeedbackClientSnapshot> client = AutomationRuntime.runOnClient(() -> getFeedbackClientSnapshot(fixture, placed));
			return matchesFeedbackServerSnapshot(server, fixture, tankPresent, placed) && client
					.filter(snapshot -> matchesFeedbackClientSnapshot(snapshot, server, fixture, tankPresent, tankCarried, requireCurrentCache, placed))
					.isPresent();
		}, description);
	}

	private static FeedbackServerSnapshot getFeedbackServerSnapshot(ServerPlayer player, ClientFeedbackLinkedStorageFixture fixture) {
		ItemStack primary = player.getInventory().getItem(CLIENT_FEEDBACK_PRIMARY_SLOT);
		BackpackBlockEntity placed = requirePlacedBackpack((ServerLevel) player.level(), fixture.placedEndpointPos(), "feedback placed secondary");
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost((ServerLevel) player.level(), primary)
				.orElseThrow(() -> new IllegalStateException("Could not resolve the feedback canonical linked Backpack"));
		try {
			return new FeedbackServerSnapshot(snapshotFeedbackProfile(canonical), snapshotFeedbackProfile(new BackpackWrapper(primary)),
					snapshotFeedbackProfile(new BackpackWrapper(placed.getBackpackWrapper().getBackpack())),
					LinkedStorageGroupsSavedData.get((ServerLevel) player.level()).manager().getRevision(fixture.groupId()));
		} finally {
			close(canonical);
		}
	}

	private static Optional<FeedbackClientSnapshot> getFeedbackClientSnapshot(ClientFeedbackLinkedStorageFixture fixture, boolean placed) {
		Optional<StorageProfile> menuProfile = getClientFeedbackMenuProfile(fixture, placed);
		Minecraft minecraft = Minecraft.getInstance();
		if (menuProfile.isEmpty() || minecraft.player == null || minecraft.level == null
				|| !(minecraft.level.getBlockEntity(fixture.placedEndpointPos()) instanceof BackpackBlockEntity placedEndpoint)) {
			return Optional.empty();
		}
		return Optional.of(new FeedbackClientSnapshot(menuProfile.get(),
				snapshotFeedbackProfile(new BackpackWrapper(minecraft.player.getInventory().getItem(CLIENT_FEEDBACK_PRIMARY_SLOT))),
				snapshotFeedbackProfile(new BackpackWrapper(placedEndpoint.getBackpackWrapper().getBackpack())),
				ClientLinkedStorageBackpackContents.getRevision(fixture.groupId()).orElse(-1L),
				ClientLinkedStorageBackpackContents.getStorageSize(fixture.groupId()).map(ClientLinkedStorageBackpackContents.StorageSize::inventorySlots)
						.orElse(-1),
				ClientLinkedStorageBackpackContents.getStorageSize(fixture.groupId()).map(ClientLinkedStorageBackpackContents.StorageSize::upgradeSlots)
						.orElse(-1),
				ClientLinkedStorageBackpackContents.getColumnsTaken(fixture.groupId()).orElse(-1),
				minecraft.player.containerMenu.getCarried().is(ModItems.TANK_UPGRADE.get())));
	}

	private static StorageProfile snapshotFeedbackProfile(IBackpackWrapper wrapper) {
		return snapshotFeedbackProfile(wrapper, wrapper.getInventoryHandler().size());
	}

	private static StorageProfile snapshotFeedbackProfile(IBackpackWrapper wrapper, int visibleStorageSlots) {
		return new StorageProfile(visibleStorageSlots, wrapper.getInventoryHandler().size(), wrapper.getUpgradeHandler().size(), wrapper.getColumnsTaken(),
				wrapper.getNumberOfSlotRows(), wrapper.getUpgradeHandler().getStackInSlot(CLIENT_FEEDBACK_TANK_SLOT).is(ModItems.TANK_UPGRADE.get()),
				wrapper.getRenderDataHandler().getData().copy());
	}

	private static boolean matchesFeedbackServerSnapshot(FeedbackServerSnapshot snapshot, ClientFeedbackLinkedStorageFixture fixture, boolean tankPresent,
			boolean placed) {
		StorageProfile activePhysical = placed ? snapshot.placedPhysical() : snapshot.primaryPhysical();
		return matchesCanonicalProfile(snapshot.canonical(), fixture.canonicalProfile(), tankPresent)
				&& matchesPhysicalProfile(activePhysical, fixture.canonicalProfile(), tankPresent)
				&& activePhysical.renderData().equals(snapshot.canonical().renderData());
	}

	private static boolean matchesFeedbackClientSnapshot(FeedbackClientSnapshot snapshot, FeedbackServerSnapshot server,
			ClientFeedbackLinkedStorageFixture fixture, boolean tankPresent, boolean tankCarried, boolean requireCurrentCache, boolean placed) {
		CanonicalProfile canonicalProfile = fixture.canonicalProfile();
		StorageProfile activePhysical = placed ? snapshot.placedPhysical() : snapshot.primaryPhysical();
		StorageProfile activeServerPhysical = placed ? server.placedPhysical() : server.primaryPhysical();
		return matchesCanonicalProfile(snapshot.menuProfile(), canonicalProfile, tankPresent)
				&& matchesPhysicalProfile(activePhysical, canonicalProfile, tankPresent) && snapshot.tankCarried() == tankCarried
				&& (!requireCurrentCache
						|| snapshot.cachedRevision() >= server.revision() && snapshot.cachedInventorySlots() == canonicalProfile.baseStorageSlots()
								&& snapshot.cachedUpgradeSlots() == canonicalProfile.upgradeSlots()
								&& snapshot.cachedColumnsTaken() == expectedFeedbackColumns(canonicalProfile, tankPresent))
				&& snapshot.menuProfile().renderData().equals(server.canonical().renderData())
				&& activePhysical.renderData().equals(activeServerPhysical.renderData());
	}

	private static boolean matchesCanonicalProfile(StorageProfile profile, CanonicalProfile canonical, boolean tankPresent) {
		return matchesFeedbackLayout(profile, canonical, tankPresent) && profile.tankInUpgradeSlot() == tankPresent;
	}

	private static boolean matchesPhysicalProfile(StorageProfile profile, CanonicalProfile canonical, boolean tankPresent) {
		return matchesFeedbackLayout(profile, canonical, tankPresent) && !profile.tankInUpgradeSlot();
	}

	private static boolean matchesFeedbackLayout(StorageProfile profile, CanonicalProfile canonical, boolean tankPresent) {
		int columns = expectedFeedbackColumns(canonical, tankPresent);
		int slots = canonical.baseStorageSlots() - columns * canonical.rows();
		return profile.visibleStorageSlots() == slots && profile.inventoryHandlerSlots() == slots && profile.upgradeSlots() == canonical.upgradeSlots()
				&& profile.columnsTaken() == columns && profile.rows() == canonical.rows();
	}

	private static int expectedFeedbackColumns(CanonicalProfile canonical, boolean tankPresent) {
		return tankPresent ? canonical.tankColumns() : 0;
	}

	private static int countFeedbackItems(IBackpackWrapper backpack, Item item) {
		int count = 0;
		for (int slot = 0; slot < backpack.getInventoryHandler().size(); slot++) {
			ItemStack stack = backpack.getInventoryHandler().getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int countFeedbackPlayerItems(ServerPlayer player, Item item) {
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

	private static void waitForFeedback(java.util.function.BooleanSupplier condition, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (condition.getAsBoolean()) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client feedback " + description);
	}

	private static void waitForClientFeedback(java.util.function.BooleanSupplier condition, String description) {
		waitForFeedback(() -> AutomationRuntime.runOnClient(condition::getAsBoolean), description);
	}

	private static void runClientLinkedTankColumnSlotRegression() {
		ClientStorageSizeFixture fixture = AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::prepareClientCanonicalStorageSizeRegression);
		try {
			AutomationRuntime.runOnClient(() -> setupClientCanonicalStorageSizeRegression(fixture));
			AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::givePlayerTankUpgrade);
			runClientLinkedTankColumnSlotActions(fixture);
		} finally {
			AutomationRuntime.runOnClient(() -> clearClientCanonicalStorageSizeRegression(fixture));
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				clearArea((ServerLevel) player.level(), fixture.diamondPrimaryPos());
				clearArea((ServerLevel) player.level(), fixture.diamondSecondaryPos());
				clearArea((ServerLevel) player.level(), fixture.ironPrimaryPos());
				clearArea((ServerLevel) player.level(), fixture.goldSecondaryPos());
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				return true;
			});
		}
	}

	private static void runClientLinkedTankColumnSlotActions(ClientStorageSizeFixture fixture) {
		runClientLinkedTankUpgradeAction(fixture.diamondPrimaryPos(), fixture.diamondSecondaryPos(), 0, true, "primary add");
		runClientLinkedTankUpgradeAction(fixture.diamondPrimaryPos(), fixture.diamondSecondaryPos(), 0, false, "primary remove");
		runClientLinkedTankUpgradeAction(fixture.diamondSecondaryPos(), fixture.diamondPrimaryPos(), 0, true, "secondary add");
		runClientLinkedTankUpgradeAction(fixture.diamondSecondaryPos(), fixture.diamondPrimaryPos(), 0, false, "secondary remove");
		runClientLinkedTankUpgradeAction(fixture.diamondSecondaryPos(), fixture.diamondPrimaryPos(), 0, true, "secondary carried render add");
		verifyClientCarriedTankRender(fixture.diamondSecondaryPos());
	}

	private static void verifyClientCarriedTankRender(BlockPos endpointPos) {
		LinkedStorageEndpointData endpoint = AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setShiftKeyDown(true);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(endpointPos), Direction.UP, endpointPos, false);
			player.gameMode.useItemOn(player, (ServerLevel) player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
			player.setShiftKeyDown(false);
			ItemStack carried = player.getMainHandItem();
			LinkedStorageEndpointData carriedEndpoint = carried.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
			if (carriedEndpoint == null) {
				throw new IllegalStateException("Linked Tank endpoint was not picked up into the main hand");
			}
			player.inventoryMenu.broadcastChanges();
			return carriedEndpoint;
		});

		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean matches = AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().player == null) {
					return false;
				}
				ItemStack carried = Minecraft.getInstance().player.getMainHandItem();
				return endpoint.equals(carried.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
						&& !new BackpackWrapper(carried).getRenderDataHandler().getTankRenderData().isEmpty();
			});
			if (matches) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted waiting for carried linked Tank render", e);
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for carried linked Tank render");
	}

	private static void runClientLinkedTankUpgradeAction(BlockPos endpointPos, BlockPos otherEndpointPos, int upgradeSlot, boolean adding, String description) {
		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			openPlacedBackpack(player, endpointPos);
			return true;
		});
		waitForClientLinkedColumnMenu(endpointPos, description + " open");

		int playerMenuSlot = adding ? waitForClientTankUpgradeInPlayerMenu(endpointPos, description + " Tank upgrade sync") : -1;
		LinkedTankUpgradeClickExpectation expectation = AutomationRuntime
				.runOnClient(() -> clickClientLinkedTankUpgrade(endpointPos, upgradeSlot, adding, playerMenuSlot));
		waitForServerLinkedColumnState(endpointPos, upgradeSlot, expectation, adding, !adding, description + " authoritative endpoint");
		waitForClientLinkedColumnState(endpointPos, upgradeSlot, expectation, adding, !adding, description + " current endpoint");

		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			openPlacedBackpack(player, endpointPos);
			return true;
		});
		waitForClientLinkedColumnState(endpointPos, upgradeSlot, expectation, adding, false, description + " current endpoint reopen");

		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			openPlacedBackpack(player, otherEndpointPos);
			return true;
		});
		waitForClientLinkedColumnState(otherEndpointPos, upgradeSlot, expectation, adding, false, description + " other endpoint reopen");
	}

	private static Boolean givePlayerTankUpgrade(ServerPlayer player) {
		player.getInventory().setItem(0, new ItemStack(ModItems.TANK_UPGRADE.get()));
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		return true;
	}

	private static LinkedTankUpgradeClickExpectation clickClientLinkedTankUpgrade(BlockPos endpointPos, int upgradeSlot, boolean adding, int playerMenuSlot) {
		BackpackContainer menu = getClientLinkedBackpackMenu(endpointPos);
		if (upgradeSlot < 0 || upgradeSlot >= menu.upgradeSlots.size()) {
			throw new IllegalStateException("Linked backpack upgrade slot " + upgradeSlot + " is unavailable");
		}
		Slot slot = menu.upgradeSlots.get(upgradeSlot);
		ItemStack tankUpgrade;
		if (adding) {
			if (playerMenuSlot < 0 || playerMenuSlot >= menu.getInventorySlotsSize()) {
				throw new IllegalStateException("Linked backpack Tank upgrade player inventory slot is unavailable");
			}
			tankUpgrade = menu.getSlot(playerMenuSlot).getItem();
		} else {
			tankUpgrade = slot.getItem();
		}
		if (!tankUpgrade.is(ModItems.TANK_UPGRADE.get()) || !(tankUpgrade.getItem() instanceof IUpgradeItem<?> upgradeItem)) {
			throw new IllegalStateException("Linked backpack " + (adding ? "player inventory slot" : "upgrade slot") + " does not contain a Tank upgrade");
		}

		int columnsTaken = menu.getStorageWrapper().getColumnsTaken();
		int expectedColumnsTaken = columnsTaken + (adding ? upgradeItem.getInventoryColumnsTaken() : -upgradeItem.getInventoryColumnsTaken());
		int rows = menu.getStorageWrapper().getNumberOfSlotRows();
		int baseStorageSlots = menu.getStorageWrapper().getInventoryHandler().size() + columnsTaken * rows;
		int expectedStorageSlots = baseStorageSlots - expectedColumnsTaken * rows;
		BackpackScreen screen = (BackpackScreen) Minecraft.getInstance().gui.screen();
		if (adding) {
			clickClientSlot(screen, menu.getSlot(playerMenuSlot));
			if (!menu.getCarried().is(ModItems.TANK_UPGRADE.get())) {
				throw new IllegalStateException("Linked backpack Tank upgrade player inventory click did not pick up the upgrade");
			}
		}
		clickClientSlot(screen, slot);
		return new LinkedTankUpgradeClickExpectation(expectedColumnsTaken, expectedStorageSlots);
	}

	private static void clickClientSlot(Slot slot) {
		if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen screen)) {
			throw new IllegalStateException("Linked backpack client click is unavailable");
		}
		clickClientSlot(screen, slot);
	}

	private static void clickClientSlot(BackpackScreen screen, Slot slot) {
		double x = screen.getGuiLeft() + slot.x + 8.0;
		double y = screen.getGuiTop() + slot.y + 8.0;
		MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
		if (!screen.mouseClicked(event, false)) {
			throw new IllegalStateException("Linked backpack Tank upgrade slot click was not handled");
		}
		screen.mouseReleased(event);
	}

	private static BackpackContainer getClientLinkedBackpackMenu(BlockPos endpointPos) {
		if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			throw new IllegalStateException("Linked backpack screen is not open");
		}
		if (!menu.getBlockPosition().filter(endpointPos::equals).isPresent()) {
			throw new IllegalStateException("Linked backpack screen is open on an unexpected endpoint");
		}
		if (!(menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper)) {
			throw new IllegalStateException("Linked backpack screen does not use a Linked Storage wrapper");
		}
		return menu;
	}

	private static void waitForClientCarriedEndpoint(LinkedStorageEndpointData endpoint, String failureMessage) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean matches = AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& endpoint.equals(Minecraft.getInstance().player.getMainHandItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)));
			if (matches) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted waiting for carried linked endpoint synchronization", e);
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(failureMessage);
	}

	private static void waitForClientCarriedLinkedMenu(LinkedStorageEndpointData endpoint) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean matches = AutomationRuntime.runOnClient(() -> {
				if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen) || Minecraft.getInstance().player == null
						|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
					return false;
				}
				return menu.getBlockPosition().isEmpty() && menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
						&& endpoint.equals(menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
			});
			if (matches) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted waiting for carried linked backpack menu", e);
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Carried linked backpack screen did not open");
	}

	private static BackpackContainer getClientLinkedCarriedBackpackMenu(LinkedStorageEndpointData endpoint) {
		if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			throw new IllegalStateException("Carried linked backpack screen is not open");
		}
		if (menu.getBlockPosition().isPresent() || !(menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper)
				|| !endpoint.equals(menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))) {
			throw new IllegalStateException("Carried linked backpack screen opened an unexpected endpoint");
		}
		return menu;
	}

	private static ClientStorageSizeFixture prepareClientCanonicalStorageSizeRegression(ServerPlayer player) {
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		ServerLevel level = (ServerLevel) player.level();
		BlockPos fixtureOrigin = player.blockPosition().relative(player.getDirection(), 3);
		BlockPos diamondPrimaryPos = fixtureOrigin;
		BlockPos diamondSecondaryPos = fixtureOrigin.east(2);
		BlockPos ironPrimaryPos = fixtureOrigin.east(4);
		BlockPos goldSecondaryPos = fixtureOrigin.east(6);
		player.closeContainer();
		player.getInventory().clearContent();
		player.getInventory().setItem(0, new ItemStack(Items.NETHER_STAR));
		player.setGameMode(GameType.SURVIVAL);
		clearArea(level, diamondPrimaryPos);
		clearArea(level, diamondSecondaryPos);
		clearArea(level, ironPrimaryPos);
		clearArea(level, goldSecondaryPos);

		ItemStack diamondPrimary = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		diamondPrimary.set(DataComponents.CUSTOM_NAME, CLIENT_PRIMARY_NAME);
		IBackpackWrapper diamondPrimaryWrapper = BackpackWrapper.fromStack(diamondPrimary);
		diamondPrimaryWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
		diamondPrimaryWrapper.getInventoryHandler().saveInventory();
		ItemStack diamondLinker = new ItemStack(ENDER_LINKER.get());
		assertTrue(LinkedStorageService.link(level, diamondLinker, diamondPrimary), "Could not link the upgraded primary storage size fixture");
		placeFixtureBackpack(level, diamondPrimaryPos, diamondPrimary, "storage size Diamond primary");
		ItemStack diamondSecondary = new ItemStack(ModItems.BACKPACK.get());
		assertTrue(LinkedStorageService.link(level, diamondLinker, diamondSecondary), "Could not link the secondary display storage size fixture");
		placeFixtureBackpack(level, diamondSecondaryPos, diamondSecondary, "storage size Diamond secondary");

		ItemStack ironPrimary = new ItemStack(ModItems.IRON_BACKPACK.get());
		IBackpackWrapper ironPrimaryWrapper = BackpackWrapper.fromStack(ironPrimary);
		ironPrimaryWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.REDSTONE));
		ironPrimaryWrapper.getInventoryHandler().saveInventory();
		ItemStack ironLinker = new ItemStack(ENDER_LINKER.get());
		assertTrue(LinkedStorageService.link(level, ironLinker, ironPrimary), "Could not link the Iron primary storage size fixture");
		ItemStack goldSecondary = new ItemStack(ModItems.GOLD_BACKPACK.get());
		assertTrue(LinkedStorageService.link(level, ironLinker, goldSecondary), "Could not link the Gold secondary storage size fixture");
		placeFixtureBackpack(level, ironPrimaryPos, ironPrimary, "storage size Iron primary");
		placeFixtureBackpack(level, goldSecondaryPos, goldSecondary, "storage size Gold secondary");
		return new ClientStorageSizeFixture(diamondPrimaryPos, diamondSecondaryPos, ironPrimaryPos, goldSecondaryPos,
				requirePlacedBackpack(level, diamondPrimaryPos, "storage size Diamond primary").getBackpackWrapper().getBackpack().copy(),
				requirePlacedBackpack(level, diamondSecondaryPos, "storage size Diamond secondary").getBackpackWrapper().getBackpack().copy(),
				requirePlacedBackpack(level, ironPrimaryPos, "storage size Iron primary").getBackpackWrapper().getBackpack().copy(),
				requirePlacedBackpack(level, goldSecondaryPos, "storage size Gold secondary").getBackpackWrapper().getBackpack().copy(), originalGameMode);
	}

	private static Boolean setupClientCanonicalStorageSizeRegression(ClientStorageSizeFixture fixture) {
		if (Minecraft.getInstance().level == null) {
			throw new IllegalStateException("Client level is not available for the linked storage size fixture");
		}
		ClientLinkedStorageBackpackContents.clear();
		setClientBackpack(Minecraft.getInstance().level, fixture.diamondPrimaryPos(), fixture.diamondPrimary());
		setClientBackpack(Minecraft.getInstance().level, fixture.diamondSecondaryPos(), fixture.diamondSecondary());
		setClientBackpack(Minecraft.getInstance().level, fixture.ironPrimaryPos(), fixture.ironPrimary());
		setClientBackpack(Minecraft.getInstance().level, fixture.goldSecondaryPos(), fixture.goldSecondary());
		return true;
	}

	private static Boolean clearClientCanonicalStorageSizeRegression(ClientStorageSizeFixture fixture) {
		if (Minecraft.getInstance().level != null) {
			Minecraft.getInstance().level.setBlock(fixture.diamondPrimaryPos(), Blocks.AIR.defaultBlockState(), 3);
			Minecraft.getInstance().level.setBlock(fixture.diamondSecondaryPos(), Blocks.AIR.defaultBlockState(), 3);
			Minecraft.getInstance().level.setBlock(fixture.ironPrimaryPos(), Blocks.AIR.defaultBlockState(), 3);
			Minecraft.getInstance().level.setBlock(fixture.goldSecondaryPos(), Blocks.AIR.defaultBlockState(), 3);
		}
		ClientLinkedStorageBackpackContents.clear();
		return true;
	}

	private static void setClientBackpack(ClientLevel level, BlockPos pos, ItemStack backpack) {
		level.setBlock(pos, ModBlocks.BACKPACK.get().defaultBlockState(), 3);
		if (!(level.getBlockEntity(pos) instanceof BackpackBlockEntity backpackBlockEntity)) {
			throw new IllegalStateException("Client linked storage size fixture did not create a Backpack block entity");
		}
		backpackBlockEntity.setBackpack(backpack.copy());
	}

	private static void openPlacedBackpack(ServerPlayer player, BlockPos pos) {
		BackpackContext context = new BackpackContext.Block(pos);
		assertTrue(
				player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new BackpackContainer(windowId, menuPlayer, context),
						context.getDisplayName(player)), buffer -> context.toBuffer(buffer, player)).isPresent(),
				"Profile fixture did not open a Backpack menu");
	}

	private static void configureCanonicalItemDisplay(ItemStack backpack) {
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		ItemDisplaySettingsCategory itemDisplaySettings = wrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
		if (!itemDisplaySettings.getSlots().contains(0)) {
			itemDisplaySettings.selectSlot(0);
		}
		itemDisplaySettings.setDisplaySide(DisplaySide.FRONT);
		itemDisplaySettings.itemsChanged();
	}

	private static CraftingInput tierUpgradeInput(ItemStack backpack, Item material) {
		return CraftingInput.of(3, 3, List.of(new ItemStack(material), new ItemStack(material), new ItemStack(material), new ItemStack(material), backpack,
				new ItemStack(material), new ItemStack(material), new ItemStack(material), new ItemStack(material)));
	}

	private static SimpleContainer craftingContainer(CraftingInput input) {
		SimpleContainer container = new SimpleContainer(input.size());
		for (int slot = 0; slot < input.size(); slot++) {
			container.setItem(slot, input.getItem(slot));
		}
		return container;
	}

	private static IBackpackWrapper resolve(ServerLevel level, ItemStack stack, String name) {
		return BackpackLinkedStorageResolver.resolve(level, stack).orElseThrow(() -> new IllegalStateException(name + " endpoint did not resolve"));
	}

	private static BackpackBlockEntity requirePlacedBackpack(ServerLevel level, BlockPos pos, String name) {
		return level.getBlockEntity(pos, ModBlocks.BACKPACK_TILE_TYPE.get()).orElseThrow(() -> new IllegalStateException(
				name + " linked backpack block was not placed; state=" + level.getBlockState(pos) + ", blockEntity=" + level.getBlockEntity(pos)));
	}

	private static void placeFixtureBackpack(ServerLevel level, BlockPos pos, ItemStack backpack, String name) {
		level.setBlockAndUpdate(pos, ModBlocks.BACKPACK.get().defaultBlockState());
		BackpackBlockEntity blockEntity = requirePlacedBackpack(level, pos, name);
		blockEntity.setBackpack(backpack);
		blockEntity.refreshRenderState();
		blockEntity.tryToAddToController();
	}

	private static CompoundTag getSavedGroup(ServerLevel level, UUID groupId) {
		CompoundTag savedGroups = LinkedStorageGroupsSavedData.get(level).save();
		for (Tag groupTag : savedGroups.getListOrEmpty("groups")) {
			CompoundTag group = (CompoundTag) groupTag;
			if (groupId.equals(group.read("id", UUIDUtil.CODEC).orElse(null))) {
				return group;
			}
		}
		throw new IllegalStateException("Linked storage group was not persisted: " + groupId);
	}

	private static CompoundTag getSavedEndpoint(CompoundTag group, UUID endpointId) {
		for (Tag endpointTag : group.getListOrEmpty("endpoints")) {
			CompoundTag endpoint = (CompoundTag) endpointTag;
			if (endpointId.equals(endpoint.read("id", UUIDUtil.CODEC).orElse(null))) {
				return endpoint;
			}
		}
		throw new IllegalStateException("Linked storage endpoint was not persisted: " + endpointId);
	}

	private static LinkedStorageEndpointData requireEndpoint(ItemStack stack, String name) {
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null) {
			throw new IllegalStateException(name + " endpoint was not created by the crafting event");
		}
		return endpoint;
	}

	private static boolean hasEndpointPresentation(ItemStack stack, LinkedStorageEndpointData endpoint, int mainColor, int accentColor, Component name,
			int openTabId) {
		return Objects.equals(endpoint, stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)) && BackpackItem.getMainColor(stack) == mainColor
				&& BackpackItem.getAccentColor(stack) == accentColor && Objects.equals(name, stack.get(DataComponents.CUSTOM_NAME))
				&& Integer.valueOf(openTabId).equals(stack.get(ModCoreDataComponents.OPEN_TAB_ID));
	}

	private static boolean hasCanonicalContents(IBackpackWrapper backpack) {
		return hasCanonicalContentsBeforeCarriedMutation(backpack) && hasStack(backpack, CARRIED_MUTATION_SLOT, Items.LAPIS_LAZULI, 2);
	}

	private static boolean hasCanonicalContentsBeforeCarriedMutation(IBackpackWrapper backpack) {
		return hasStack(backpack, 0, Items.DIAMOND, 5) && hasStack(backpack, MUTATION_SLOT, Items.EMERALD, 3)
				&& hasStack(backpack, EXTERNAL_MUTATION_SLOT, Items.COPPER_INGOT, 4)
				&& hasUpgrade(backpack, ModItems.STACK_UPGRADE_STARTER_TIER.get(), ModItems.TANK_UPGRADE.get(), ModItems.BATTERY_UPGRADE.get());
	}

	private static boolean hasUpgrade(IBackpackWrapper backpack, Item... upgrades) {
		for (int slot = 0; slot < upgrades.length; slot++) {
			if (!backpack.getUpgradeHandler().getStackInSlot(slot).is(upgrades[slot])) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasStack(IBackpackWrapper backpack, int slot, Item item, int count) {
		ItemStack stack = backpack.getInventoryHandler().getStackInSlot(slot);
		return stack.is(item) && stack.getCount() == count;
	}

	private static ItemStack findBoundLinker(ServerPlayer player, UUID groupId, ItemStack excluded) {
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack != excluded && stack.getItem() == ENDER_LINKER.get()
					&& stack.get(ModCoreDataComponents.ENDER_LINKER_TARGET) instanceof EnderLinkerTargetData target && target.groupId().equals(groupId)) {
				return stack;
			}
		}
		throw new IllegalStateException("No bound linker was delivered to the player inventory");
	}

	private static boolean hasRenderedItem(BackpackBlockEntity backpack, Item item) {
		return backpack.getBackpackWrapper().getRenderDataHandler().getDisplayData().displayItems().stream()
				.anyMatch(displayItem -> displayItem.item().is(item));
	}

	private static boolean hasRenderedFluid(BackpackBlockEntity backpack, Fluid fluid) {
		return backpack.getBackpackWrapper().getRenderDataHandler().getTankRenderData().values().stream()
				.anyMatch(tank -> tank.getFluid().map(fluidStack -> fluidStack.is(fluid)).orElse(false));
	}

	private static boolean hasRenderedEnergy(BackpackBlockEntity backpack) {
		return backpack.getBackpackWrapper().getRenderDataHandler().getBatteryRenderData().map(battery -> battery.chargeRatio() > 0).orElse(false);
	}

	private static void close(IBackpackWrapper backpack) {
		if (backpack instanceof LinkedStorageBackpackWrapper linkedStorageBackpack) {
			linkedStorageBackpack.close();
		}
	}

	private static void clearArea(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
	}

	private static InteractionResult placeBlockWithItem(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		BlockPos supportPos = pos.below();
		level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
		player.setYRot(0);
		player.setXRot(0);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.setShiftKeyDown(true);
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false);
		InteractionResult result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitResult);
		player.setShiftKeyDown(false);
		return result;
	}

	private static void useLinkerOnBlock(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack linker) {
		player.setItemInHand(InteractionHand.MAIN_HAND, linker);
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		player.gameMode.useItemOn(player, level, linker, InteractionHand.MAIN_HAND, hitResult);
	}

	private static void waitForClientLinkedMenu(int expectedSlots, Item expectedItem, Component expectedTitle, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean matches = AutomationRuntime.runOnClient(() -> hasClientLinkedMenuState(expectedSlots, expectedItem, expectedTitle));
			if (matches) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted waiting for client " + description + " inventory slots", e);
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(
				"Timed out waiting for client " + description + "; " + AutomationRuntime.runOnClient(BackpackLinkedStorageRegression::getClientMenuSlotState));
	}

	private static void waitForClientLinkedColumnMenu(BlockPos endpointPos, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> hasClientLinkedMenuAt(endpointPos))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(
				"Timed out waiting for client " + description + "; " + AutomationRuntime.runOnClient(BackpackLinkedStorageRegression::getClientMenuSlotState));
	}

	private static int waitForClientTankUpgradeInPlayerMenu(BlockPos endpointPos, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			int playerMenuSlot = AutomationRuntime.runOnClient(() -> getClientTankUpgradePlayerMenuSlot(endpointPos));
			if (playerMenuSlot >= 0) {
				return playerMenuSlot;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client " + description + " in the open player inventory menu slot");
	}

	private static int getClientTankUpgradePlayerMenuSlot(BlockPos endpointPos) {
		if (!hasClientLinkedMenuAt(endpointPos)) {
			return -1;
		}
		BackpackContainer menu = (BackpackContainer) Minecraft.getInstance().player.containerMenu;
		for (int playerMenuSlot = menu.getNumberOfStorageInventorySlots(); playerMenuSlot < menu.getInventorySlotsSize(); playerMenuSlot++) {
			if (menu.getSlot(playerMenuSlot).getItem().is(ModItems.TANK_UPGRADE.get())) {
				return playerMenuSlot;
			}
		}
		return -1;
	}

	private static void waitForClientLinkedColumnState(BlockPos endpointPos, int upgradeSlot, LinkedTankUpgradeClickExpectation expectation,
			boolean tankInUpgradeSlot, boolean tankCarried, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		LinkedTankUpgradeState state = null;
		do {
			if (AutomationRuntime.runOnClient(() -> hasClientLinkedMenuAt(endpointPos))) {
				state = AutomationRuntime.runOnClient(() -> getClientLinkedTankUpgradeState(endpointPos, upgradeSlot));
				if (state.matches(expectation, tankInUpgradeSlot, tankCarried)) {
					return;
				}
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client " + description + "; expected=" + expectation + ", actual=" + state);
	}

	private static void waitForServerLinkedColumnState(BlockPos endpointPos, int upgradeSlot, LinkedTankUpgradeClickExpectation expectation,
			boolean tankInUpgradeSlot, boolean tankCarried, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		LinkedTankUpgradeState state = null;
		do {
			state = AutomationRuntime.runOnServer(player -> getServerLinkedTankUpgradeState(player, endpointPos, upgradeSlot));
			if (state != null && state.matches(expectation, tankInUpgradeSlot, tankCarried)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for server " + description + "; expected=" + expectation + ", actual=" + state);
	}

	private static boolean hasClientLinkedMenuAt(BlockPos endpointPos) {
		return Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
				&& menu.getBlockPosition().filter(endpointPos::equals).isPresent() && menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted waiting for client linked Backpack state", e);
		}
	}

	private static LinkedTankUpgradeState getClientLinkedTankUpgradeState(BlockPos endpointPos, int upgradeSlot) {
		BackpackContainer menu = getClientLinkedBackpackMenu(endpointPos);
		if (upgradeSlot < 0 || upgradeSlot >= menu.upgradeSlots.size()) {
			throw new IllegalStateException("Linked backpack upgrade slot " + upgradeSlot + " is unavailable");
		}
		return new LinkedTankUpgradeState(menu.getStorageWrapper().getColumnsTaken(), menu.getNumberOfStorageInventorySlots(),
				menu.getStorageWrapper().getInventoryHandler().size(), menu.upgradeSlots.get(upgradeSlot).getItem().is(ModItems.TANK_UPGRADE.get()),
				menu.getCarried().is(ModItems.TANK_UPGRADE.get()));
	}

	@Nullable
	private static LinkedTankUpgradeState getServerLinkedTankUpgradeState(ServerPlayer player, BlockPos endpointPos, int upgradeSlot) {
		IBackpackWrapper backpack = requirePlacedBackpack((ServerLevel) player.level(), endpointPos, "linked Tank endpoint").getBackpackWrapper();
		if (upgradeSlot < 0 || upgradeSlot >= backpack.getUpgradeHandler().size()) {
			return null;
		}
		return new LinkedTankUpgradeState(backpack.getColumnsTaken(), backpack.getInventoryHandler().size(), backpack.getInventoryHandler().size(),
				backpack.getUpgradeHandler().getStackInSlot(upgradeSlot).is(ModItems.TANK_UPGRADE.get()),
				player.containerMenu.getCarried().is(ModItems.TANK_UPGRADE.get()));
	}

	private static boolean hasClientLinkedMenuState(int expectedSlots, Item expectedItem, Component expectedTitle) {
		if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen screen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			return false;
		}
		ItemStack firstStorageStack = menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0);
		return menu.getStorageWrapper().getInventoryHandler().size() == expectedSlots && firstStorageStack.is(expectedItem)
				&& !firstStorageStack.is(Items.NETHER_STAR) && screen.getTitle().equals(expectedTitle);
	}

	private static void waitForClientRenderedItem(BlockPos pos, Item expectedItem, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean matches = AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null
					&& Minecraft.getInstance().level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack && hasRenderedItem(backpack, expectedItem));
			if (matches) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted waiting for client " + description + " render", e);
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client " + description + " render");
	}

	private static boolean selectClientItemDisplaySlot(Item expectedItem) {
		if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu)
				|| !(Minecraft.getInstance().gui.screen() instanceof BackpackSettingsScreen settingsScreen)) {
			throw new IllegalStateException("Client item display settings menu did not open");
		}
		final boolean[] selected = {false};
		settingsMenu.forEachSettingsContainer((name, container) -> {
			if (ItemDisplaySettingsCategory.NAME.equals(name) && container instanceof ItemDisplaySettingsContainer itemDisplaySettings) {
				itemDisplaySettings.selectSlot(0);
				itemDisplaySettings.setDisplaySide(DisplaySide.FRONT);
				selected[0] = true;
			}
		});
		if (!selected[0]) {
			throw new IllegalStateException("Client item display settings container was unavailable");
		}
		ItemStack previewStack = BackpackItemDisplaySettingsPreviewProvider.INSTANCE.getItemDisplaySettingsPreviewStack(settingsScreen, null, 0)
				.orElseThrow(() -> new IllegalStateException("Client item display preview stack was unavailable"));
		assertTrue(BackpackWrapper.fromStackNoCache(previewStack).getRenderDataHandler().getDisplayData().displayItems().stream()
				.anyMatch(displayItem -> displayItem.item().is(expectedItem)), "Client item display preview did not refresh selected slot");
		return true;
	}

	private static String getClientMenuSlotState() {
		if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen backpackScreen)) {
			return "screen=" + Minecraft.getInstance().gui.screen();
		}
		if (Minecraft.getInstance().player == null) {
			return "client player unavailable";
		}
		if (!(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			return "menu=" + Minecraft.getInstance().player.containerMenu;
		}
		return "title=" + backpackScreen.getTitle().getString() + ", inventorySlots=" + menu.getStorageWrapper().getInventoryHandler().size()
				+ ", upgradeSlots=" + menu.getStorageWrapper().getUpgradeHandler().size() + ", firstStorageStack="
				+ menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0);
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}

	private static String jsonProperty(String name, @Nullable String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private record ClientStorageSizeFixture(BlockPos diamondPrimaryPos, BlockPos diamondSecondaryPos, BlockPos ironPrimaryPos, BlockPos goldSecondaryPos,
			ItemStack diamondPrimary, ItemStack diamondSecondary, ItemStack ironPrimary, ItemStack goldSecondary, GameType originalGameMode) {
	}

	private record InceptionLinkedChildFixture(LinkedStorageEndpointData childEndpoint, GameType originalGameMode) {
	}

	private record ClientFeedbackLinkedStorageFixture(BlockPos placedEndpointPos, LinkedStorageEndpointData primaryEndpoint, CanonicalProfile canonicalProfile,
			GameType originalGameMode) {
		private UUID groupId() {
			return primaryEndpoint.groupId();
		}
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

	private record LinkedStorageTickFixture(LinkedStorageTickCase tickCase, BlockPos primaryPos, BlockPos secondaryPos, GameType originalGameMode) {
	}

	private record ClientCreativePlacementFixture(BlockPos position, LinkedStorageEndpointData sourceEndpoint, GameType originalGameMode) {
	}

	private record ClientEndpointCraftFixture(int containerId, BlockPos craftingTablePos, boolean quickMove, GameType originalGameMode) {
		private String description() {
			return quickMove ? "quick-move" : "normal-take";
		}
	}

	private record ClientSecondaryCraftFixture(int containerId, BlockPos craftingTablePos, UUID groupId, boolean quickMove, GameType originalGameMode) {
		private String description() {
			return quickMove ? "quick-move" : "normal-take";
		}
	}

	private record ClientRejectedSecondaryCraftFixture(int containerId, BlockPos craftingTablePos, GameType originalGameMode) {
	}

	private record ClientPendingLinkerUseFixture(BlockPos targetPos, UUID groupId) {
	}

	private record ClientCraftingUpgradeBlockFixture(BlockPos position, ItemStack linker, @Nullable UUID groupId, boolean secondary, boolean quickMove,
			GameType originalGameMode) {
	}

	private record ClientCraftingUpgradeFixture(int containerId, BlockPos position, @Nullable UUID groupId, boolean secondary, boolean quickMove,
			GameType originalGameMode) {
		private String description() {
			return quickMove ? "quick-move" : "normal-take";
		}
	}

	private enum LinkedStorageTickCase {
		CARRIED_PRIMARY("carried primary", false, false, false), CARRIED_SECONDARY_WITH_PRIMARY_IN_CHEST("carried secondary with primary in a chest", true,
				false, false), PLACED_PRIMARY("placed primary", false, true,
						false), PLACED_SECONDARY_WITH_PRIMARY_IN_CHEST("placed secondary with primary in a chest", true, false, true);

		private final String description;
		private final boolean primaryInChest;
		private final boolean primaryPlaced;
		private final boolean secondaryPlaced;

		LinkedStorageTickCase(String description, boolean primaryInChest, boolean primaryPlaced, boolean secondaryPlaced) {
			this.description = description;
			this.primaryInChest = primaryInChest;
			this.primaryPlaced = primaryPlaced;
			this.secondaryPlaced = secondaryPlaced;
		}

		private String description() {
			return description;
		}

		private boolean primaryInChest() {
			return primaryInChest;
		}

		private boolean primaryPlaced() {
			return primaryPlaced;
		}

		private boolean secondaryPlaced() {
			return secondaryPlaced;
		}
	}

	private record LinkedTankUpgradeClickExpectation(int columnsTaken, int storageSlots) {
	}

	private record LinkedTankUpgradeState(int columnsTaken, int storageSlots, int inventoryHandlerSlots, boolean tankInUpgradeSlot, boolean tankCarried) {
		private boolean matches(LinkedTankUpgradeClickExpectation expectation, boolean expectedTankInUpgradeSlot, boolean expectedTankCarried) {
			return columnsTaken == expectation.columnsTaken() && storageSlots == expectation.storageSlots()
					&& inventoryHandlerSlots == expectation.storageSlots() && tankInUpgradeSlot == expectedTankInUpgradeSlot
					&& tankCarried == expectedTankCarried;
		}
	}
}
