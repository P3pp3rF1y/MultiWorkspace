package net.p3pp3rf1y.devclientautomation.scenarios.inventory;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.bool;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.integer;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.jsonProperty;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class InventoryIntegrationEndpoints {
	private static final Logger LOGGER = LoggerFactory.getLogger("devclientautomation");

	private InventoryIntegrationEndpoints() {
	}

	public static void register(EndpointRegistry endpoints) {
		endpoints.register("/invtweaks/sort", InventoryIntegrationEndpoints::inventoryTweaksSort);
		endpoints.register("/inventory-interactions/keybind-regression", InventoryIntegrationEndpoints::inventoryInteractionsKeybindRegression);
		endpoints.register("/inventoryessentials/drop-by-type", InventoryIntegrationEndpoints::inventoryEssentialsDropByType);
	}

	private static void inventoryTweaksSort(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		boolean playerInventory = bool(request, "playerInventory", false);
		String screenName = string(request, "screenName", "net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen");
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(player -> inventoryTweaksSort(player, playerInventory, screenName)));
	}

	private static void inventoryInteractionsKeybindRegression(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, InventoryInteractionsKeybindRegression::run);
	}

	private static void inventoryEssentialsDropByType(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		int menuSlot = integer(request, "menuSlot", -1);
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnClient(() -> inventoryEssentialsDropByType(menuSlot)));
	}

	private static String inventoryTweaksSort(ServerPlayer player, boolean playerInventory, String screenName) {
		try {
			Class<?> sortingClass = Class.forName("invtweaks.util.Sorting");
			Method executeSort = sortingClass.getMethod("executeSort", Player.class, boolean.class, String.class);
			executeSort.invoke(null, player, playerInventory, screenName);
			return "{\"ok\":true,\"playerInventory\":" + playerInventory + "," + jsonProperty("serverMenu", player.containerMenu.getClass().getName()) + "}";
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to invoke Inventory Tweaks sort", e);
		}
	}

	private static String inventoryEssentialsDropByType(int menuSlot) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return "{\"ok\":false,\"error\":\"No container screen is open\"}";
		}
		if (menuSlot < 0 || menuSlot >= containerScreen.getMenu().slots.size()) {
			return "{\"ok\":false,\"error\":\"Invalid menu slot\"}";
		}
		try {
			Class<?> clientClass = Class.forName("net.blay09.mods.inventoryessentials.client.InventoryEssentialsClient");
			Class<?> controlsClass = Class.forName("net.blay09.mods.inventoryessentials.client.InventoryControls");
			Method getInventoryControls = clientClass.getMethod("getInventoryControls", Screen.class);
			Method dropByType = controlsClass.getMethod("dropByType", AbstractContainerScreen.class, Slot.class);
			Object controls = getInventoryControls.invoke(null, containerScreen);
			Slot slot = containerScreen.getMenu().slots.get(menuSlot);
			boolean handled = (Boolean) dropByType.invoke(controls, containerScreen, slot);
			return "{\"ok\":" + handled + ",\"handled\":" + handled + ",\"menuSlot\":" + menuSlot + ","
					+ jsonProperty("controls", controls.getClass().getName()) + "}";
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to invoke Inventory Essentials dropByType", e);
		}
	}

}
