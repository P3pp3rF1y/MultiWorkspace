package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionResultModelTest {
	@Test
	void casesPreservePrimaryAndCleanupOutcomesAndExportSerializerGoldens() throws IOException {
		List<Golden> goldens = new ArrayList<>();
		goldens.add(golden("pass", "passed", "", "", 1, 0, 0, 0));
		goldens.add(golden("assertion", "failed", "assertion", "", 1, 1, 0, 0));
		goldens.add(golden("infrastructure", "error", "infrastructure", "", 1, 0, 1, 0));
		goldens.add(golden("unexpected", "error", "unexpectedException", "", 1, 0, 1, 0));
		goldens.add(golden("preflight", "error", "preflight", "", 1, 0, 1, 0));
		goldens.add(golden("timeout", "error", "timeout", "passed", 2, 0, 1, 0));
		goldens.add(golden("unavailable", "skipped", "expectedUnavailable", "", 1, 0, 0, 1));
		goldens.add(golden("blocked", "blocked", "dependencyBlocked", "", 1, 0, 0, 1));
		goldens.add(golden("cancelled", "cancelled", "cancellation", "", 1, 0, 0, 1));
		goldens.add(golden("notRun", "notRun", "infrastructure", "", 1, 0, 0, 1));
		goldens.add(golden("pass-cleanup-pass", "passed", "", "passed", 2, 0, 0, 0));
		goldens.add(golden("pass-cleanup-error", "passed", "", "error", 2, 0, 1, 0));
		goldens.add(golden("assertion-cleanup-error", "failed", "assertion", "error", 2, 1, 1, 0));
		goldens.add(golden("cancelled-cleanup", "cancelled", "cancellation", "passed", 2, 0, 0, 1));
		goldens.add(golden("cancelled-cleanup-error", "cancelled", "cancellation", "error", 2, 0, 1, 1));
		List<RegressionResultModel.ReportCase> root = RegressionResultModel.cases("suite", "case", "blocked", "dependencyBlocked",
				"blocked by probe revision 1", 0, List.of(new RegressionResultModel.ConformanceResult("probe", "owned", false, "probe revision 1")), List.of(),
				List.of(), List.of());
		goldens.add(new Golden("conformance-root", Map.of("suiteId", "suite", "durationMillis", 0, "reportCases", root), 2, 0, 1, 1));

		for (Golden golden : goldens) {
			@SuppressWarnings("unchecked")
			List<RegressionResultModel.ReportCase> cases = (List<RegressionResultModel.ReportCase>) golden.result().get("reportCases");
			assertEquals(golden.tests(), cases.size(), golden.name());
			assertEquals(golden.failures(), cases.stream().filter(c -> c.junitElement().equals("failure")).count(), golden.name());
			assertEquals(golden.errors(), cases.stream().filter(c -> c.junitElement().equals("error")).count(), golden.name());
			assertEquals(golden.skipped(), cases.stream().filter(c -> c.junitElement().equals("skipped")).count(), golden.name());
			assertEquals(1, cases.stream().filter(c -> c.role().equals("scenario")).count(), golden.name());
		}
		Path directory = Path.of("build/reports/regression-result-model");
		Files.createDirectories(directory);
		Files.writeString(directory.resolve("goldens.json"), new GsonBuilder().setPrettyPrinting().create().toJson(goldens));
	}

	@Test
	void failureRetainsExceptionTypeStackAndCause() {
		var failure = RegressionResultModel.Failure.exception("unexpectedException", new IllegalStateException("outer", new IllegalArgumentException("inner")));

		assertEquals("java.lang.IllegalStateException", failure.exceptionType());
		assertTrue(failure.stackTrace().contains("Caused by: java.lang.IllegalArgumentException: inner"));
	}

	@Test
	void statusSerializationRetainsPrimaryOutcomeMessageAndSeparateCleanupError() {
		for (String primary : List.of("passed", "failed", "cancelled")) {
			String category = primary.equals("failed") ? "assertion" : primary.equals("cancelled") ? "cancellation" : "";
			var cleanupFailure = RegressionResultModel.Failure.exception("cleanup", new IllegalStateException("cleanup exception"));
			var status = new RegressionResultModel.RunStatus(false, "run", "suite", "case", "error", "primary; cleanup exception", List.of(), List.of(),
					List.of(), Map.of(), 3, 1, List.of(new RegressionResultModel.CleanupResult("case", "error", "cleanup exception", 2)), "cleanup", "cleanup",
					Set.of("jei"), List.of(), primary, category, primary.equals("failed") ? RegressionResultModel.Failure.message(category, "primary") : null,
					cleanupFailure, "pwsh -NoProfile -File scripts/dev-client/regression.ps1 -Command run -Suite suite -Test case", List.of(), 0, "primary");

			var json = JsonParser.parseString(RegressionEndpoints.statusJson(status)).getAsJsonObject();
			assertEquals("error", json.get("outcome").getAsString());
			assertEquals(false, json.get("ok").getAsBoolean());
			assertEquals(primary, json.get("primaryOutcome").getAsString());
			assertEquals("primary", json.get("primaryMessage").getAsString());
			assertTrue(json.getAsJsonObject("cleanupFailure").get("stackTrace").getAsString().contains("cleanup exception"));
			assertEquals(2, json.getAsJsonArray("reportCases").size());
			assertEquals("primary", json.getAsJsonArray("reportCases").get(0).getAsJsonObject().get("message").getAsString());
			assertEquals(primary, json.getAsJsonArray("reportCases").get(0).getAsJsonObject().get("outcome").getAsString());
			assertEquals("error", json.getAsJsonArray("reportCases").get(1).getAsJsonObject().get("junitElement").getAsString());
			assertTrue(json.getAsJsonArray("reportCases").get(1).getAsJsonObject().getAsJsonObject("diagnostics").getAsJsonObject("failure").get("stackTrace")
					.getAsString().contains("cleanup exception"));
		}
	}

	@Test
	void offlineControllerFactoryUsesCanonicalFieldsIdentityAndJUnitMapping() throws IOException {
		var context = new RegressionResultModel.ReportContext(
				new RegressionResultModel.RuntimeIdentity("forge", "1.20.1", 123, Map.of("buildIdentity", "candidate-sha")), "pwsh exact rerun --with-options",
				Map.of("json", "case.json", "junit", "case.xml"));
		var failure = RegressionResultModel.Failure.exception("infrastructure", new IllegalStateException("offline controller cause"));
		List<RegressionResultModel.RunStatus> statuses = new ArrayList<>();
		for (var kind : RegressionResultModel.ControllerOutcome.values()) {
			var status = RegressionResultTool
					.create(new RegressionResultTool.Request(kind, "suite", "case", "controller message", failure, Set.of("jei"), context));
			var json = JsonParser.parseString(RegressionEndpoints.statusJson(status)).getAsJsonObject();

			assertEquals(context.runtimeIdentity(), status.runtimeIdentity());
			assertEquals("suite/case", status.runId());
			assertEquals(context.artifacts(), status.artifacts());
			assertEquals(context.rerunCommand(), status.rerunCommand());
			assertEquals(Set.of("jei"), status.effectiveRequirements());
			assertEquals(kind == RegressionResultModel.ControllerOutcome.ERROR ? "error" : "notRun", status.outcome());
			assertEquals(kind == RegressionResultModel.ControllerOutcome.ERROR ? "error" : "skipped", status.reportCases().get(0).junitElement());
			assertTrue(json.has("primaryCategory") && json.has("primaryMessage") && json.has("missingRequirements"));
			assertTrue(status.reportCases().get(0).diagnostics().containsKey("runtimeIdentity"));
			if (kind == RegressionResultModel.ControllerOutcome.ERROR) {
				assertEquals(failure, status.reportCases().get(0).diagnostics().get("failure"));
			}
			statuses.add(status);
		}
		Path directory = Path.of("build/reports/regression-result-model");
		Files.createDirectories(directory);
		Files.writeString(directory.resolve("controller.json"), new GsonBuilder().setPrettyPrinting().create().toJson(statuses));
	}

	private static Golden golden(String name, String primary, String category, String cleanup, int tests, int failures, int errors, int skipped) {
		var provenance = new RegressionStepProvenance("fixture", "arrangement", "prepare", "member", "prepare-member");
		var step = new RegressionResultModel.StepResult("prepare-member", "assert.example", primary, "<expected> & actual", 1, "2", category, provenance,
				Map.of("expected", 1, "actual", 2), null);
		var cases = RegressionResultModel.cases("suite", "case", primary, category, "<expected> & actual", 1, List.of(), List.of(),
				cleanup.isEmpty() ? List.of() : List.of(new RegressionResultModel.CleanupResult("case", cleanup, "cleanup & evidence", 2)), List.of(step));
		return new Golden(name, Map.of("suiteId", "suite", "durationMillis", 3, "reportCases", cases), tests, failures, errors, skipped);
	}

	private record Golden(String name, Map<String, Object> result, int tests, int failures, int errors, int skipped) {
	}
}
