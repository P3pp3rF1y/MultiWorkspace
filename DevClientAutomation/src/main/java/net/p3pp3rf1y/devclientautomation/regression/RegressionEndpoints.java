package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
		server.createContext("/regression/probe-background-attack", exchange -> probeBackgroundAttack(exchange, logger));
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
		Map<String, Object> requirements = new LinkedHashMap<>();
		suites.forEach((id, suite) -> {
			Map<String, Object> tests = new LinkedHashMap<>();
			suite.tests().forEach(test -> tests.put(test.id(), Map.of("effectiveRequirements", test.requires().stream().sorted().toList())));
			requirements.put(id, tests);
		});
		catalog.put("testMetadata", requirements);
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
			return GSON.toJson(
					RegressionCoordinator.get().start(suiteId, testId, string(request, "conformanceFailureAction"), string(request, "actionFailureAfter"),
							bool(request, "cleanupFailure"), string(request, "preparationToken"), reportContext(request, exchange)));
		});
	}

	private static void status(HttpExchange exchange, Logger logger) throws IOException {
		if (!requireMethod(exchange, "GET")) {
			return;
		}
		sendJsonHandling(exchange, logger, () -> statusJson(RegressionCoordinator.get().status()));
	}

	static String statusJson(RegressionResultModel.RunStatus status) {
		return GSON.toJson(status);
	}

	private static RegressionResultModel.ReportContext reportContext(JsonObject request, HttpExchange exchange) {
		RegressionResultModel.ReportContext supplied = request.has("reportContext")
				? GSON.fromJson(request.get("reportContext"), RegressionResultModel.ReportContext.class)
				: RegressionResultModel.ReportContext.unknown();
		String rerun = supplied.rerunCommand().isEmpty() ? rerunCommand(request, exchange.getLocalAddress().getPort()) : supplied.rerunCommand();
		return new RegressionResultModel.ReportContext(
				new RegressionResultModel.RuntimeIdentity("forge", "1.20.1", ProcessHandle.current().pid(), supplied.runtimeIdentity().controller()), rerun,
				supplied.artifacts());
	}

	static String rerunCommand(JsonObject request, int port) {
		StringBuilder command = new StringBuilder("pwsh -NoProfile -File scripts/dev-client/regression.ps1 -Command run");
		appendRerunOption(command, request, "suiteId", "Suite");
		appendRerunOption(command, request, "testId", "Test");
		command.append(" -BaseUrl 'http://127.0.0.1:").append(port).append("'");
		appendRerunOption(command, request, "conformanceFailureAction", "ConformanceFailureAction");
		appendRerunOption(command, request, "actionFailureAfter", "ActionFailureAfter");
		appendRerunOption(command, request, "preparationToken", "PreparationToken");
		if (bool(request, "cleanupFailure")) {
			command.append(" -CleanupFailure");
		}
		return command.toString();
	}

	private static void appendRerunOption(StringBuilder command, JsonObject request, String field, String option) {
		String value = string(request, field);
		if (!value.isEmpty()) {
			command.append(" -").append(option).append(" '").append(value.replace("'", "''")).append("'");
		}
	}

	private static void cancel(HttpExchange exchange, Logger logger) throws IOException {
		if (!requireMethod(exchange, "POST")) {
			return;
		}
		sendJsonHandling(exchange, logger, () -> GSON.toJson(RegressionCoordinator.get().cancel()));
	}

	/** Explicit, loopback-only dev-client diagnostic; not a regression action or world mutation. */
	private static void probeBackgroundAttack(HttpExchange exchange, Logger logger) throws IOException {
		if (!requireMethod(exchange, "POST")) {
			return;
		}
		sendJsonHandling(exchange, logger, () -> {
			if (!BackgroundInputPolicy.enabled()) {
				throw new IllegalStateException("Background attack probe requires a background regression client");
			}
			String mode = string(readObject(exchange), "mode");
			if (mode.equals("stones")) {
				Object cell = AutomationRuntime.runOnServer(player -> {
					int stones = 0;
					for (int x = 252; x <= 260; x++) {
						for (int z = 252; z <= 260; z++) {
							if (player.serverLevel().getBlockState(new BlockPos(x, 72, z)).is(Blocks.STONE)) {
								stones++;
							}
						}
					}
					return Map.of("stones", stones, "creative", player.isCreative(), "x", player.getX(), "y", player.getY(), "z", player.getZ(), "yaw",
							player.getYRot(), "pitch", player.getXRot());
				});
				return GSON.toJson(cell);
			}
			return GSON.toJson(AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null || minecraft.screen == null && (mode.equals("native") || mode.equals("armStart"))) {
					throw new IllegalStateException("Probe requires a loaded player and open screen to prevent vanilla consuming the seeded click");
				}
				if (mode.equals("native")) {
					try {
						Method press = minecraft.mouseHandler.getClass().getDeclaredMethod("onPress", long.class, int.class, int.class, int.class);
						press.setAccessible(true);
						press.invoke(minecraft.mouseHandler, minecraft.getWindow().getWindow(), 0, 1, 0);
						press.invoke(minecraft.mouseHandler, minecraft.getWindow().getWindow(), 0, 0, 0);
					} catch (ReflectiveOperationException e) {
						throw new IllegalStateException("Unable to invoke native mouse callback", e);
					}
				} else if (mode.equals("armStart")) {
					KeyMapping.click(minecraft.options.keyAttack.getKey());
					minecraft.options.keyAttack.setDown(true);
					boolean armed = rawAttackDown(minecraft);
					RegressionCoordinator.get().start("linked-storage-action-conformance", "empty-crafting-times-out-with-slot-diagnostics", "");
					return Map.of("mode", mode, "armed", armed);
				} else if (mode.equals("armCancel")) {
					return Map.of("mode", mode, "armed", RegressionCoordinator.get().requestCancellationWithQueuedAttackProbe());
				} else if (!mode.equals("inspect")) {
					throw new IllegalArgumentException("Unknown background attack probe mode " + mode);
				}
				int clicks = 0;
				if (mode.equals("inspect") || mode.equals("native")) {
					while (minecraft.options.keyAttack.consumeClick()) {
						clicks++;
					}
				}
				return Map.of("mode", mode, "attackDown", rawAttackDown(minecraft), "queuedClicks", clicks, "screen",
						minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName());
			}));
		});
	}

	private static boolean rawAttackDown(Minecraft minecraft) {
		try {
			Field down = KeyMapping.class.getDeclaredField("isDown");
			down.setAccessible(true);
			return down.getBoolean(minecraft.options.keyAttack);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to inspect raw attack key state", e);
		}
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
