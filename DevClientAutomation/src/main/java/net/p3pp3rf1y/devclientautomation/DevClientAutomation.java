package net.p3pp3rf1y.devclientautomation;

import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationBridge;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import net.p3pp3rf1y.devclientautomation.scenarios.backpacks.BackpackEndpoints;
import net.p3pp3rf1y.devclientautomation.scenarios.core.ClientControlEndpoints;
import net.p3pp3rf1y.devclientautomation.scenarios.core.WorldAutomationEndpoints;
import net.p3pp3rf1y.devclientautomation.scenarios.inventory.InventoryIntegrationEndpoints;
import net.p3pp3rf1y.devclientautomation.scenarios.model.ModelEndpoints;
import net.p3pp3rf1y.devclientautomation.scenarios.recipeviewer.RecipeViewerEndpoints;
import net.p3pp3rf1y.devclientautomation.scenarios.storage.StorageEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class DevClientAutomation {
	public static final String MOD_ID = "devclientautomation";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static AutomationServer server;

	private DevClientAutomation() {
	}

	public static void start() {
		if (server == null) {
			server = new AutomationServer();
			server.start();
		}
	}

	private static class AutomationServer {
		private final AutomationBridge bridge = new AutomationBridge(LOGGER);

		void start() {
			bridge.start(this::registerEndpoints, Minecraft.getInstance().gameDirectory.toPath());
		}

		private void registerEndpoints(EndpointRegistry endpoints) {
			endpoints.register("/capabilities", this::capabilities);
			ClientControlEndpoints.register(endpoints);
			WorldAutomationEndpoints.register(endpoints);
			InventoryIntegrationEndpoints.register(endpoints);
			BackpackEndpoints.register(endpoints);
			StorageEndpoints.register(endpoints);
			ModelEndpoints.register(endpoints);
			RecipeViewerEndpoints.register(endpoints);
		}

		private void capabilities(HttpExchange exchange) throws IOException {
			if (!"GET".equals(exchange.getRequestMethod())) {
				byte[] response = "{\"error\":\"Method not allowed\"}".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
				exchange.sendResponseHeaders(405, response.length);
				try (OutputStream outputStream = exchange.getResponseBody()) {
					outputStream.write(response);
				}
				throw new IllegalStateException("Method not allowed");
			}
			byte[] response = ("{\"ok\":true,\"protocolVersion\":1,\"loader\":\"neoforge\",\"minecraftVersion\":\"1.21.8\","
					+ "\"features\":[\"state\",\"world-load\",\"screenshot\",\"recipe-viewer\",\"backpacks\",\"storage\",\"inventory-interactions\"]}")
					.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(response);
			}
		}
	}
}
