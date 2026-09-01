package net.p3pp3rf1y.devclientautomation.scenarios.model;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.integer;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class ModelEndpoints {
	private static final Logger LOGGER = LoggerFactory.getLogger("devclientautomation");

	private ModelEndpoints() {
	}

	public static void register(EndpointRegistry endpoints) {
		endpoints.register("/model/item-diagnostics", ModelEndpoints::itemModelDiagnostics);
		endpoints.register("/model/hotbar-item-diagnostics", ModelEndpoints::hotbarItemModelDiagnostics);
	}

	private static void itemModelDiagnostics(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String itemName = string(request, "item", "sophisticatedstorage:controller");
		sendJsonHandling(exchange, LOGGER, () -> ItemModelDiagnostics.forItem(itemName));
	}

	private static void hotbarItemModelDiagnostics(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		int slot = integer(request, "slot", 0);
		sendJsonHandling(exchange, LOGGER, () -> ItemModelDiagnostics.forHotbarSlot(slot));
	}

}
