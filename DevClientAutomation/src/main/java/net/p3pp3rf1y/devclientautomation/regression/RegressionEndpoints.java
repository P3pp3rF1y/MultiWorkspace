package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import net.p3pp3rf1y.devclientautomation.bridge.HttpJson;

import java.io.IOException;
import java.util.Map;

public final class RegressionEndpoints {
	private RegressionEndpoints() {
	}

	public static void register(EndpointRegistry endpoints) {
		endpoints.register("/regression/catalog", RegressionEndpoints::catalog);
		endpoints.register("/regression/run", RegressionEndpoints::run);
		endpoints.register("/regression/status", RegressionEndpoints::status);
	}

	private static void catalog(HttpExchange exchange) throws IOException {
		HttpJson.requireMethod(exchange, "GET");
		HttpJson.sendJsonHandling(exchange, DevClientAutomation.getLogger(),
				() -> HttpJson.toJson(Map.of("actions", RegressionCoordinator.get().actions(), "suites", RegressionCoordinator.get().suites().keySet())));
	}

	private static void run(HttpExchange exchange) throws IOException {
		HttpJson.requireMethod(exchange, "POST");
		JsonObject request = HttpJson.readObject(exchange);
		HttpJson.sendJsonHandling(exchange, DevClientAutomation.getLogger(), () -> {
			String suiteId = HttpJson.string(request, "suiteId", "");
			String testId = HttpJson.string(request, "testId", "");
			if (suiteId.isBlank() || testId.isBlank()) {
				throw new IllegalArgumentException("suiteId and testId are required");
			}
			return HttpJson.toJson(RegressionCoordinator.get().start(suiteId, testId, HttpJson.string(request, "conformanceFailureAction", "")));
		});
	}

	private static void status(HttpExchange exchange) throws IOException {
		HttpJson.requireMethod(exchange, "GET");
		HttpJson.sendJsonHandling(exchange, DevClientAutomation.getLogger(), () -> HttpJson.toJson(RegressionCoordinator.get().status()));
	}
}
