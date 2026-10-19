package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import net.p3pp3rf1y.devclientautomation.bridge.HttpJson;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionMetadata;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegressionEndpoints {
	private RegressionEndpoints() {
	}

	public static void register(EndpointRegistry endpoints) {
		endpoints.register("/regression/catalog", RegressionEndpoints::catalog);
		endpoints.register("/regression/run", RegressionEndpoints::run);
		endpoints.register("/regression/status", RegressionEndpoints::status);
		endpoints.register("/regression/cancel", RegressionEndpoints::cancel);
	}

	private static void catalog(HttpExchange exchange) throws IOException {
		HttpJson.requireMethod(exchange, "GET");
		HttpJson.sendJsonHandling(exchange, DevClientAutomation.getLogger(),
				() -> catalogJson(RegressionCoordinator.get().actions(), RegressionCoordinator.get().suites()));
	}

	static Map<String, List<String>> suiteTests(Map<String, RegressionSuite> suites) {
		Map<String, List<String>> suiteTests = new LinkedHashMap<>();
		suites.forEach((id, suite) -> suiteTests.put(id, suite.tests().stream().map(RegressionTest::id).toList()));
		return Collections.unmodifiableMap(suiteTests);
	}

	static String catalogJson(Map<String, RegressionActionMetadata> actions, Map<String, RegressionSuite> suites) {
		Map<String, List<String>> suiteTests = suiteTests(suites);
		Map<String, Object> catalog = new LinkedHashMap<>();
		catalog.put("actions", actions);
		catalog.put("suites", suiteTests.keySet());
		catalog.put("suiteTests", suiteTests);
		return HttpJson.toJson(catalog);
	}

	private static void run(HttpExchange exchange) throws IOException {
		HttpJson.requireMethod(exchange, "POST");
		HttpJson.sendJsonHandling(exchange, DevClientAutomation.getLogger(), () -> {
			JsonObject request;
			try {
				request = HttpJson.readObject(exchange);
			} catch (IOException e) {
				throw new IllegalStateException("Could not read regression request", e);
			}
			String suiteId = HttpJson.string(request, "suiteId", "");
			String testId = HttpJson.string(request, "testId", "");
			if (suiteId.isBlank() || testId.isBlank()) {
				throw new IllegalArgumentException("suiteId and testId are required");
			}
			return HttpJson.toJson(RegressionCoordinator.get().start(suiteId, testId, HttpJson.string(request, "conformanceFailureAction", ""),
					HttpJson.string(request, "actionFailureAfter", ""), HttpJson.bool(request, "cleanupFailure", false),
					HttpJson.string(request, "preparationToken", "")));
		});
	}

	private static void status(HttpExchange exchange) throws IOException {
		HttpJson.requireMethod(exchange, "GET");
		HttpJson.sendJsonHandling(exchange, DevClientAutomation.getLogger(), () -> statusJson(RegressionCoordinator.get().status()));
	}

	static String statusJson(RegressionCoordinator.RunStatus status) {
		return HttpJson.toJson(status);
	}

	private static void cancel(HttpExchange exchange) throws IOException {
		HttpJson.requireMethod(exchange, "POST");
		HttpJson.sendJsonHandling(exchange, DevClientAutomation.getLogger(), () -> HttpJson.toJson(RegressionCoordinator.get().cancel()));
	}
}
