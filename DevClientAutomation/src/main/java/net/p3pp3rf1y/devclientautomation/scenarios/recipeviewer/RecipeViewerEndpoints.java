package net.p3pp3rf1y.devclientautomation.scenarios.recipeviewer;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import net.p3pp3rf1y.devclientautomation.recipeviewer.RecipeViewerAutomationManager;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.errorJson;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readBody;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJson;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class RecipeViewerEndpoints {
	private static final Logger LOGGER = LoggerFactory.getLogger("devclientautomation");

	private RecipeViewerEndpoints() {
	}

	public static void register(EndpointRegistry endpoints) {
		endpoints.register("/recipe-viewer/state", RecipeViewerEndpoints::state);
		endpoints.register("/recipe-viewer/search", RecipeViewerEndpoints::search);
		endpoints.register("/recipe-viewer/open", RecipeViewerEndpoints::open);
		endpoints.register("/recipe-viewer/query", RecipeViewerEndpoints::query);
		endpoints.register("/recipe-viewer/backpack-crafting-transfer", RecipeViewerEndpoints::backpackCraftingTransfer);
		endpoints.register("/recipe-viewer/stats", RecipeViewerEndpoints::stats);
	}

	private static void state(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		sendJson(exchange, AutomationRuntime.runOnClient(RecipeViewerAutomationManager::stateJson));
	}

	private static void search(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String query = string(request, "query", "");
		sendJson(exchange, AutomationRuntime.runOnClient(() -> RecipeViewerAutomationManager.searchJson(query)));
	}

	private static void open(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		String body = readBody(exchange);
		sendJson(exchange, AutomationRuntime.runOnClient(() -> RecipeViewerAutomationManager.openJson(body)));
	}

	private static void query(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		String body = readBody(exchange);
		try {
			sendJson(exchange, AutomationRuntime.runOnClient(() -> RecipeViewerAutomationManager.queryJson(body)));
		} catch (RuntimeException e) {
			LOGGER.error("Recipe viewer query failed", e);
			sendJson(exchange, errorJson(e.getMessage()));
		}
	}

	private static void backpackCraftingTransfer(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		String body = readBody(exchange);
		sendJsonHandling(exchange, LOGGER, () -> {
			AutomationRuntime.runOnServer(RecipeViewerEndpoints::setupBackpackCraftingTransferRegression);
			AutomationRuntime.runOnClient(RecipeViewerEndpoints::setupClientBackpackCraftingTransferRegression);
			waitForClientCraftingTransferBackpack();
			AutomationRuntime.runOnServer(RecipeViewerEndpoints::openParentBackpackRegression);
			waitForOpenParentBackpackMenu();
			return AutomationRuntime.runOnClient(() -> RecipeViewerAutomationManager.transferJson(body));
		});
	}

	private static void stats(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		try {
			sendJson(exchange, AutomationRuntime.runOnClient(RecipeViewerAutomationManager::statsJson));
		} catch (RuntimeException e) {
			LOGGER.error("Recipe viewer stats failed", e);
			sendJson(exchange, errorJson(e.getMessage()));
		}
	}

	private static String setupBackpackCraftingTransferRegression(ServerPlayer player) {
		player.getInventory().clearContent();
		player.getInventory().setItem(0, createCraftingTransferRegressionBackpack());
		for (int slot = 1; slot <= 4; slot++) {
			player.getInventory().setItem(slot, new ItemStack(Items.OAK_PLANKS));
		}
		player.getInventory().setSelectedSlot(0);
		player.getInventory().setChanged();
		return "{\"ok\":true}";
	}

	private static Boolean setupClientBackpackCraftingTransferRegression() {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			throw new IllegalStateException("Client player is not available");
		}
		player.getInventory().clearContent();
		player.getInventory().setItem(0, createCraftingTransferRegressionBackpack());
		for (int slot = 1; slot <= 4; slot++) {
			player.getInventory().setItem(slot, new ItemStack(Items.OAK_PLANKS));
		}
		player.getInventory().setSelectedSlot(0);
		player.getInventory().setChanged();
		return true;
	}

	private static void waitForClientCraftingTransferBackpack() {
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		do {
			if (AutomationRuntime.runOnClient(() -> {
				Player player = Minecraft.getInstance().player;
				if (player == null) {
					return false;
				}
				ItemStack backpack = player.getInventory().getItem(0);
				return backpack.getItem() instanceof BackpackItem
						&& BackpackWrapper.fromStack(backpack).getUpgradeHandler().getStackInSlot(0).is(ModItems.CRAFTING_UPGRADE.get());
			})) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);

		throw new IllegalStateException("Timed out waiting for client inventory slot 0 to contain crafting upgrade backpack");
	}

	private static ItemStack createCraftingTransferRegressionBackpack() {
		ItemStack backpack = createBackpackStack();
		IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
		backpackWrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
		backpackWrapper.getUpgradeHandler().saveInventory();
		backpackWrapper.onContentsNbtUpdated();
		return backpack;
	}

	private static Boolean openParentBackpackRegression(ServerPlayer player) {
		BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
		player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
				Component.literal("Parent Backpack Regression")), backpackContext::toBuffer);
		return true;
	}

	private static void waitForOpenParentBackpackMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for parent backpack screen to open");
	}

	private static ItemStack createBackpackStack() {
		ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 80);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);
		return backpack;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
