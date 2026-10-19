package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionRunnerContractTest {
	@Test
	void catalogMapsStrictlyParsedSuiteTestsForTheOfficialRunner() throws IOException {
		var suites = new LinkedHashMap<String, RegressionSuite>();
		for (String resource : List.of("linked-storage-interoperability", "linked-storage-restart")) {
			try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/" + resource + ".json")) {
				assertTrue(stream != null);
				var suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
				suites.put(suite.suiteId(), suite);
			}
		}
		var suiteTests = RegressionEndpoints.suiteTests(suites);
		assertEquals(suites.keySet(), suiteTests.keySet());
		for (var entry : suites.entrySet()) {
			assertEquals(entry.getValue().tests().stream().map(RegressionTest::id).toList(), suiteTests.get(entry.getKey()));
		}
		JsonObject catalog = JsonParser.parseString(RegressionEndpoints.catalogJson(new RegressionActionRegistry().all(), suites)).getAsJsonObject();
		assertTrue(catalog.has("actions"));
		assertEquals(suiteTests.get("linked-storage-interoperability").size(),
				catalog.getAsJsonObject("suiteTests").getAsJsonArray("linked-storage-interoperability").size());
	}

	@Test
	void statusSerializationExposesRunSelectedTestAndStepTimingsIncludingFailedCleanup() {
		var passed = new RegressionCoordinator.StepResult("first", "menu.close", "passed", "", 1.25);
		var failed = new RegressionCoordinator.StepResult("second", "menu.close", "failed", "failure", 2.5);
		var conformance = new RegressionCoordinator.RuntimeConformanceResult("probe", "passed", "", List.of(passed), 3.75);
		var cleanup = new RegressionCoordinator.CleanupResult("case", "failed", "Cleanup failed");
		var status = new RegressionCoordinator.RunStatus(false, "suite/case", "suite", "case", "failed", "Cleanup failed", List.of(), List.of(failed),
				List.of(conformance), Map.of(), 10.5, 4.25, List.of(cleanup), "execution");
		JsonObject json = JsonParser.parseString(RegressionEndpoints.statusJson(status)).getAsJsonObject();
		assertEquals(10.5, json.get("durationMillis").getAsDouble());
		assertEquals(4.25, json.get("testDurationMillis").getAsDouble());
		assertEquals(2.5, json.getAsJsonArray("steps").get(0).getAsJsonObject().get("durationMillis").getAsDouble());
		assertEquals(3.75, json.getAsJsonArray("runtimeConformance").get(0).getAsJsonObject().get("durationMillis").getAsDouble());
		assertEquals(1.25, json.getAsJsonArray("runtimeConformance").get(0).getAsJsonObject().getAsJsonArray("steps").get(0).getAsJsonObject()
				.get("durationMillis").getAsDouble());
		assertEquals("failed", json.getAsJsonArray("cleanup").get(0).getAsJsonObject().get("outcome").getAsString());
		assertTrue(json.has("phase"));
	}

	@Test
	void idleStatusHasRunnerTimingFields() {
		JsonObject json = JsonParser.parseString(RegressionEndpoints.statusJson(RegressionCoordinator.RunStatus.idle())).getAsJsonObject();
		assertEquals(0, json.get("durationMillis").getAsDouble());
		assertEquals(0, json.get("testDurationMillis").getAsDouble());
	}
}
