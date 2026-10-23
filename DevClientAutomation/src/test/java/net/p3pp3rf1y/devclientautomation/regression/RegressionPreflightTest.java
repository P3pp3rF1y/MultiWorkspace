package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionValueContract;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionValueTypes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionPreflightTest {
	@Test
	void startMissingExternalModTerminatesBeforeConformanceSetupJournalAndCleanup() {
		RegressionCoordinator coordinator = new RegressionCoordinator(id -> !id.equals("jei"));

		RegressionResultModel.RunStatus status = coordinator.start("linked-storage-settings", "barrel-primary-backpack-settings", "");

		assertEquals("skipped", status.outcome());
		assertEquals("expectedUnavailable", status.category());
		assertEquals(List.of("jei"), status.missingRequirements());
		assertEquals(Set.of("jei"), status.effectiveRequirements());
		assertTrue(status.steps().isEmpty());
		assertTrue(status.conformance().isEmpty());
		assertTrue(status.runtimeConformance().isEmpty());
		assertTrue(status.cleanup().isEmpty());
		assertTrue(status.diagnostics().isEmpty());
		assertEquals("skipped", coordinator.status().outcome());
	}

	@Test
	void startMissingOwnedModuleIsInfrastructureErrorWithNoMutation() {
		RegressionCoordinator coordinator = new RegressionCoordinator(id -> !id.equals("sophisticatedbackpacks"));

		RegressionResultModel.RunStatus status = coordinator.start("backpack-filters", "basic-filter-input-allow-diamond-block-redstone", "");

		assertEquals("error", status.outcome());
		assertEquals("infrastructure", status.category());
		assertTrue(status.steps().isEmpty());
		assertTrue(status.cleanup().isEmpty());
		assertTrue(status.diagnostics().isEmpty());
	}
	@Test
	void requiresUnionsSuiteAndTestRequirementsAndCatalogExposesThem() {
		RegressionSuite suite = parse("[\"jei\"]", "[\"create\",\"jei\"]", "{\"id\":\"close\",\"action\":\"menu.close\"}");

		assertEquals(Set.of("jei", "create"), suite.tests().getFirst().requires());
		assertEquals(List.of("create"), RegressionPreflight.missing(suite.tests().getFirst().requires(), "jei"::equals));
		String catalog = RegressionEndpoints.catalogJson(new RegressionActionRegistry().all(), java.util.Map.of("test", suite));
		assertEquals(2, JsonParser.parseString(catalog).getAsJsonObject().getAsJsonObject("testMetadata").getAsJsonObject("test").getAsJsonObject("case")
				.getAsJsonArray("effectiveRequirements").size());
	}

	@Test
	void startRejectsInvalidExpandedValuePlansBeforeLoadingMinecraftOrCreatingJournal() {
		String spawn = "{\"id\":\"spawn\",\"action\":\"fixture.entity.spawnPig\",\"args\":{\"saveAs\":\"target\"}}";
		String consume = "{\"id\":\"use\",\"action\":\"player.sneakUseFixtureEntity\",\"args\":{\"ref\":\"target\"}}";
		String wrongType = "{\"id\":\"moving\",\"action\":\"player.useMovingStorage\",\"args\":{\"ref\":\"target\"}}";
		for (String steps : List.of(consume, consume + "," + spawn, spawn + "," + wrongType, spawn + "," + spawn.replace("\"spawn\"", "\"second\""))) {
			RegressionSuite suite = parse("[]", "[]", steps);
			RegressionCoordinator coordinator = new RegressionCoordinator(id -> true, () -> java.util.Map.of("test", suite));

			var status = coordinator.start("test", "case", "");

			assertEquals("error", status.outcome());
			assertEquals("preflight", status.category());
			assertEquals("preflight", status.phase());
			assertTrue(status.steps().isEmpty() && status.conformance().isEmpty() && status.runtimeConformance().isEmpty());
			assertTrue(status.cleanup().isEmpty() && status.diagnostics().isEmpty());
		}
	}

	@Test
	void requiresRejectsMalformedDuplicateBlankAndOwnedRequirementsAtBothScopes() {
		for (String invalid : List.of("null", "{}", "\"jei\"", "[1]", "[\"\"]", "[\"jei\",\"jei\"]", "[\"minecraft:client\"]", "[\"sophisticatedbackpacks\"]",
				"[\"bad-id\"]", "[\" JEI \"]")) {
			assertThrows(IllegalArgumentException.class, () -> parse(invalid, "[]", "{\"id\":\"close\",\"action\":\"menu.close\"}"), invalid);
			assertThrows(IllegalArgumentException.class, () -> parse("[]", invalid, "{\"id\":\"close\",\"action\":\"menu.close\"}"), invalid);
		}
		assertEquals(Set.of(), parse("[]", "[]", "{\"id\":\"close\",\"action\":\"menu.close\"}").tests().getFirst().requires());
	}

	@Test
	void validateValuesRejectsMissingOutOfOrderDuplicateAndWrongTypeWithoutStartingActions() {
		String spawn = "{\"id\":\"spawn\",\"action\":\"fixture.entity.spawnPig\",\"args\":{\"saveAs\":\"target\"}}";
		String consume = "{\"id\":\"use\",\"action\":\"player.sneakUseFixtureEntity\",\"args\":{\"ref\":\"target\"}}";
		String otherType = "{\"id\":\"moving\",\"action\":\"player.useMovingStorage\",\"args\":{\"ref\":\"target\"}}";

		assertThrows(IllegalArgumentException.class, () -> validate(consume));
		assertThrows(IllegalArgumentException.class, () -> validate(consume + "," + spawn));
		assertThrows(IllegalArgumentException.class, () -> validate(spawn + "," + spawn.replace("\"spawn\"", "\"second\"")));
		assertThrows(IllegalArgumentException.class, () -> validate(spawn + "," + otherType));
		assertDoesNotThrow(() -> validate(spawn + "," + consume));
		assertDoesNotThrow(() -> validate(spawn + "," + consume));
		assertEquals(RegressionValueTypes.UUID, new RegressionActionRegistry().require("fixture.create.mountBackpack")
				.bind(JsonParser.parseString("{\"saveAs\":\"target\"}").getAsJsonObject(), "lazy").values().produces().getFirst().type());
		assertDoesNotThrow(() -> new RegressionValueContract.Value("third-type", "external.unloaded.Type"));
	}

	@Test
	void fixturesRetainStructuredProvenanceAndRepeatedFixedOutputCollides() {
		String json = """
				{"schemaVersion":2,"suiteId":"test","requires":[],"isolation":"testCell",
				"fixtures":{"pig":[{"id":"spawn","action":"fixture.entity.spawnPig","args":{"saveAs":"target"}}]},
				"tests":[{"id":"case","tags":[],"timeoutTicks":20,"steps":[{"id":"first","fixture":"pig"},{"id":"second","fixture":"pig"}]}]}
				""";
		RegressionTest test = RegressionSuiteParser.parse(json, new RegressionActionRegistry()).tests().getFirst();

		assertEquals(new RegressionStepProvenance("fixture", "pig", "first", "spawn", "first-spawn"), test.steps().getFirst().provenance());
		String failure = assertThrows(IllegalArgumentException.class, () -> RegressionPreflight.validateValues(test)).getMessage();
		assertTrue(failure.contains("second-spawn"));
		assertTrue(failure.contains("fixture=pig"));
		assertTrue(failure.contains("duplicate producer"));
	}

	@Test
	void indexedPlansHaveIndependentValidValueNamespaces() {
		RegressionCoordinator.loadSuites().values().forEach(
				suite -> suite.tests().forEach(test -> assertDoesNotThrow(() -> RegressionPreflight.validateValues(test), suite.suiteId() + "/" + test.id())));
	}

	private static void validate(String steps) {
		RegressionPreflight.validateValues(parse("[]", "[]", steps).tests().getFirst());
	}

	private static RegressionSuite parse(String suiteRequires, String testRequires, String steps) {
		return RegressionSuiteParser.parse("{\"schemaVersion\":2,\"suiteId\":\"test\",\"requires\":" + suiteRequires
				+ ",\"isolation\":\"testCell\",\"tests\":[{\"id\":\"case\",\"requires\":" + testRequires + ",\"tags\":[],\"timeoutTicks\":20,\"steps\":["
				+ steps + "]}]}", new RegressionActionRegistry());
	}
}
