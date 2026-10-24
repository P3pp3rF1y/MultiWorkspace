package net.p3pp3rf1y.devclientautomation.regression;

import net.p3pp3rf1y.devclientautomation.regression.actions.PlayerStatePolicy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewFollowUpSuiteTest {
	private RegressionSuite suite(String resource) throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/" + resource + ".json")) {
			assertNotNull(stream);
			return RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
		}
	}

	@Test
	void parseResourcesUsesNativeLinkerClicksAndRestartRequiresExplicitCheckpointPolicy() throws IOException {
		var resources = suite("linked-storage-resources");
		var restart = suite("linked-storage-restart");
		for (var test : resources.tests()) {
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
		var backpack = suite("backpack-menu").tests().stream().filter(test -> test.id().equals("linked-held-backpack-menu-receives-slot-delta")).findFirst()
				.orElseThrow();
		var storage = suite("storage-menu").tests().stream().filter(test -> test.id().equals("placed-linked-block-open-menu-receives-live-slot-delta"))
				.findFirst().orElseThrow();
		assertTrue(backpack.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("menu.clickPlayerInventorySlot")));
		assertTrue(backpack.steps().stream().noneMatch(step -> step.action().action().metadata().id().equals("backpack.linkHeld")));
		assertTrue(storage.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("player.useHeldItemOnTarget")));
		assertTrue(storage.steps().stream().noneMatch(step -> step.action().action().metadata().id().equals("storage.linkBlock")));
		assertFalse(new RegressionActionRegistry().all().containsKey("player.selectHeldLinkedEndpoint"));
	}

	@Test
	void parseConformanceCasesContainExpectedFailureDiagnosticsAndAnIndependentConsumer() throws IOException {
		var tests = suite("linked-storage-action-conformance").tests();
		assertEquals(5, tests.stream().filter(test -> test.tags().contains("runtimeConformance")).count());
		assertEquals(9, tests.stream().flatMap(test -> test.steps().stream()).filter(step -> !step.expectFailure().isEmpty()).count());
		assertFalse(tests.get(tests.size() - 1).tags().contains("runtimeConformance"));
	}

	@Test
	void parseRejectsIncompleteCheckpointAndAmbiguousExpectedFailure() {
		String test = "{\"schemaVersion\":2,\"suiteId\":\"test\",\"requires\":[],\"isolation\":\"testCell\",\"tests\":[{\"id\":\"case\",\"tags\":[],\"timeoutTicks\":20,%s\"steps\":[{\"id\":\"step\",\"action\":\"menu.close\"%s}]}]}";
		var actions = new RegressionActionRegistry();
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("\"playerState\":\"unknown\",", ""), actions));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("\"playerState\":\"checkpointVerify\",", ""), actions));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("\"checkpoint\":\"checkpoint\",", ""), actions));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(test.formatted("\"playerState\":\"checkpointVerify\",\"checkpoint\":\"checkpoint\",", ""), actions));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("", ",\"expectFailure\":\"\""), actions));
	}
}
