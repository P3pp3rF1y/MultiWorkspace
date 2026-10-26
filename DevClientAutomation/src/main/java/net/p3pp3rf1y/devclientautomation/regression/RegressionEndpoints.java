package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.Gson;
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
		return Collections.unmodifiableMap(new LinkedHashMap<>(suiteTests));
	}

	static String catalogJson(Map<String, RegressionActionMetadata> actions, Map<String, RegressionSuite> suites) {
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
					HttpJson.string(request, "preparationToken", ""), reportContext(request, exchange)));
		});
	}

	private static void status(HttpExchange exchange) throws IOException {
		HttpJson.requireMethod(exchange, "GET");
		HttpJson.sendJsonHandling(exchange, DevClientAutomation.getLogger(), () -> statusJson(RegressionCoordinator.get().status()));
	}

	static String statusJson(RegressionResultModel.RunStatus status) {
		return HttpJson.toJson(status);
	}

	private static RegressionResultModel.ReportContext reportContext(JsonObject request, HttpExchange exchange) {
		RegressionResultModel.ReportContext supplied = request.has("reportContext")
				? new Gson().fromJson(request.get("reportContext"), RegressionResultModel.ReportContext.class)
				: RegressionResultModel.ReportContext.unknown();
		String rerun = supplied.rerunCommand().isEmpty() ? rerunCommand(request, exchange.getLocalAddress().getPort()) : supplied.rerunCommand();
		return new RegressionResultModel.ReportContext(new RegressionResultModel.RuntimeIdentity(DevClientAutomation.LOADER,
				DevClientAutomation.MINECRAFT_VERSION, ProcessHandle.current().pid(), supplied.runtimeIdentity().controller()), rerun, supplied.artifacts());
	}

	static String rerunCommand(JsonObject request, int port) {
		StringBuilder command = new StringBuilder("pwsh -NoProfile -File scripts/dev-client/regression.ps1 -Command run");
		appendRerunOption(command, request, "suiteId", "Suite");
		appendRerunOption(command, request, "testId", "Test");
		command.append(" -BaseUrl 'http://127.0.0.1:").append(port).append("'");
		appendRerunOption(command, request, "conformanceFailureAction", "ConformanceFailureAction");
		appendRerunOption(command, request, "actionFailureAfter", "ActionFailureAfter");
		appendRerunOption(command, request, "preparationToken", "PreparationToken");
		if (HttpJson.bool(request, "cleanupFailure", false)) {
			command.append(" -CleanupFailure");
		}
		return command.toString();
	}

	private static void appendRerunOption(StringBuilder command, JsonObject request, String field, String option) {
		String value = HttpJson.string(request, field, "");
		if (!value.isEmpty()) {
			command.append(" -").append(option).append(" '").append(value.replace("'", "''")).append("'");
		}
	}

	private static void cancel(HttpExchange exchange) throws IOException {
		HttpJson.requireMethod(exchange, "POST");
		HttpJson.sendJsonHandling(exchange, DevClientAutomation.getLogger(), () -> HttpJson.toJson(RegressionCoordinator.get().cancel()));
	}
}
