package net.p3pp3rf1y.devclientautomation.regression;

import net.p3pp3rf1y.devclientautomation.regression.actions.PlayerStatePolicy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ReviewFollowUpSuiteTest {
	private RegressionSuite suite(String resource) throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/" + resource + ".json")) {
			assertNotNull(stream);
			return RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
		}
	}
	@Test
	void parseResourcesUsesNativeLinkerClicksAndRestartRequiresExplicitCheckpointPolicy() throws IOException {
		RegressionSuite resources = suite("linked-storage-resources");
		RegressionSuite restart = suite("linked-storage-restart");

		for (RegressionTest test : resources.tests()) {
			assertTrue(test.steps().stream().noneMatch(step -> step.action().action().metadata().id().equals("backpack.linkHeld")
					|| step.action().action().metadata().id().equals("player.selectHeldLinkedEndpoint")));
		}
		assertEquals(PlayerStatePolicy.CHECKPOINT_PREPARE, restart.tests().get(0).playerState());
		assertEquals(PlayerStatePolicy.CHECKPOINT_VERIFY, restart.tests().get(1).playerState());
		assertEquals("canonical-mob-items", restart.tests().get(0).checkpoint());
		assertEquals("canonical-mob-items", restart.tests().get(1).checkpoint());
		assertTrue(restart.tests().get(0).prepareTestCell());
		assertFalse(restart.tests().get(1).prepareTestCell());
		assertEquals(restart.tests().get(0).checkpoint(), restart.tests().get(1).checkpoint());
	}
	@Test
	void parseMenuDeltaCasesUseNativeLinkerInteractionsWithoutObsoleteSelectionAction() throws IOException {
		RegressionSuite backpacks = suite("backpack-menu");
		RegressionSuite storage = suite("storage-menu");
		RegressionActionRegistry actions = new RegressionActionRegistry();

		RegressionTest backpackCase = backpacks.tests().stream().filter(test -> test.id().equals("linked-held-backpack-menu-receives-slot-delta")).findFirst()
				.orElseThrow();
		RegressionTest storageCase = storage.tests().stream().filter(test -> test.id().equals("placed-linked-block-open-menu-receives-live-slot-delta"))
				.findFirst().orElseThrow();

		assertTrue(backpackCase.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("menu.clickPlayerInventorySlot")));
		assertTrue(backpackCase.steps().stream().noneMatch(step -> step.action().action().metadata().id().equals("backpack.linkHeld")));
		assertTrue(storageCase.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("player.useHeldItemOnTarget")));
		assertTrue(storageCase.steps().stream().noneMatch(step -> step.action().action().metadata().id().equals("storage.linkBlock")));
		assertFalse(actions.all().containsKey("player.selectHeldLinkedEndpoint"));
	}

	@Test
	void parseConformanceCasesContainExpectedFailureDiagnosticsAndAnIndependentConsumer() throws IOException {
		RegressionSuite conformance = suite("linked-storage-action-conformance");

		assertEquals(5, conformance.tests().stream().filter(test -> test.tags().contains("runtimeConformance")).count());
		assertEquals(9, conformance.tests().stream().flatMap(test -> test.steps().stream()).filter(step -> !step.expectFailure().isEmpty()).count());
		assertFalse(conformance.tests().getLast().tags().contains("runtimeConformance"));
	}
	@Test
	void parseRejectsIncompleteCheckpointAndAmbiguousExpectedFailure() {
		String test = "{\"schemaVersion\":2,\"suiteId\":\"test\",\"requires\":[],\"isolation\":\"testCell\",\"tests\":[{\"id\":\"case\",\"tags\":[],\"timeoutTicks\":20,%s\"steps\":[{\"id\":\"step\",\"action\":\"menu.close\"%s}]}]}";
		RegressionActionRegistry actions = new RegressionActionRegistry();

		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("\"playerState\":\"checkpointVerify\",", ""), actions));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("\"checkpoint\":\"checkpoint\",", ""), actions));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(test.formatted("\"playerState\":\"checkpointVerify\",\"checkpoint\":\"checkpoint\",", ""), actions));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("\"playerState\":\"unknown\",", ""), actions));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("", ",\"expectFailure\":\"\""), actions));
	}
}
