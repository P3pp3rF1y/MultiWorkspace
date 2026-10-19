package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegressionEndpoints {
	private static final Gson GSON = new Gson();

	private RegressionEndpoints() {
	}

	public static void register(HttpServer server, Logger logger) {
		server.createContext("/regression/catalog", exchange -> catalog(exchange, logger));
		server.createContext("/regression/run", exchange -> run(exchange, logger));
		server.createContext("/regression/status", exchange -> status(exchange, logger));
		server.createContext("/regression/cancel", exchange -> cancel(exchange, logger));
	}

	private static void catalog(HttpExchange exchange, Logger logger) throws IOException {
		if (!requireMethod(exchange, "GET")) {
			return;
		}
		sendJsonHandling(exchange, logger, () -> catalogJson(RegressionCoordinator.get().actions(), RegressionCoordinator.get().suites()));
	}

	static Map<String, List<String>> suiteTests(Map<String, RegressionSuite> suites) {
		Map<String, List<String>> suiteTests = new LinkedHashMap<>();
		suites.forEach((id, suite) -> suiteTests.put(id, suite.tests().stream().map(RegressionTest::id).toList()));
		return Collections.unmodifiableMap(new LinkedHashMap<>(suiteTests));
	}

	static String catalogJson(Map<String, net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionMetadata> actions,
			Map<String, RegressionSuite> suites) {
		Map<String, List<String>> suiteTests = suiteTests(suites);
		Map<String, Object> catalog = new LinkedHashMap<>();
		catalog.put("actions", actions);
		catalog.put("suites", suiteTests.keySet());
		catalog.put("suiteTests", suiteTests);
		return GSON.toJson(catalog);
	}

	private static void run(HttpExchange exchange, Logger logger) throws IOException {
		if (!requireMethod(exchange, "POST")) {
			return;
		}
		sendJsonHandling(exchange, logger, () -> {
			JsonObject request = readObject(exchange);
			String suiteId = string(request, "suiteId");
			String testId = string(request, "testId");
			if (suiteId.isBlank() || testId.isBlank()) {
				throw new IllegalArgumentException("suiteId and testId are required");
			}
			return GSON.toJson(RegressionCoordinator.get().start(suiteId, testId, string(request, "conformanceFailureAction"),
					string(request, "actionFailureAfter"), bool(request, "cleanupFailure"), string(request, "preparationToken")));
		});
	}

	private static void status(HttpExchange exchange, Logger logger) throws IOException {
		if (!requireMethod(exchange, "GET")) {
			return;
		}
		sendJsonHandling(exchange, logger, () -> statusJson(RegressionCoordinator.get().status()));
	}

	static String statusJson(RegressionCoordinator.RunStatus status) {
		return GSON.toJson(status);
	}

	private static void cancel(HttpExchange exchange, Logger logger) throws IOException {
		if (!requireMethod(exchange, "POST")) {
			return;
		}
		sendJsonHandling(exchange, logger, () -> GSON.toJson(RegressionCoordinator.get().cancel()));
	}

	private static JsonObject readObject(HttpExchange exchange) throws IOException {
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		if (!JsonParser.parseString(body).isJsonObject()) {
			throw new IllegalArgumentException("Regression request must be a JSON object");
		}
		return JsonParser.parseString(body).getAsJsonObject();
	}

	static String string(JsonObject object, String name) {
		if (!object.has(name)) {
			return "";
		}
		if (!object.get(name).isJsonPrimitive() || !object.getAsJsonPrimitive(name).isString()) {
			throw new IllegalArgumentException(name + " must be a string");
		}
		return object.getAsJsonPrimitive(name).getAsString();
	}

	private static boolean bool(JsonObject object, String name) {
		if (!object.has(name)) {
			return false;
		}
		if (!object.get(name).isJsonPrimitive() || !object.getAsJsonPrimitive(name).isBoolean()) {
			throw new IllegalArgumentException(name + " must be a boolean");
		}
		return object.getAsJsonPrimitive(name).getAsBoolean();
	}

	private static boolean requireMethod(HttpExchange exchange, String method) throws IOException {
		if (method.equals(exchange.getRequestMethod())) {
			return true;
		}
		sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
		return false;
	}

	private static void sendJsonHandling(HttpExchange exchange, Logger logger, JsonSupplier jsonSupplier) throws IOException {
		try {
			sendJson(exchange, 200, jsonSupplier.get());
		} catch (RuntimeException e) {
			logger.error("Regression endpoint failed", e);
			sendJson(exchange, 200, GSON.toJson(Map.of("ok", false, "error", e.getMessage() == null ? "Regression endpoint failed" : e.getMessage())));
		}
	}

	private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
		byte[] response = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(status, response.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(response);
		}
	}

	@FunctionalInterface
	private interface JsonSupplier {
		String get() throws IOException;
	}
}
