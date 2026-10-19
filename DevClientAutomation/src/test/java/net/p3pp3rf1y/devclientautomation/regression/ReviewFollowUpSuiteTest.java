package net.p3pp3rf1y.devclientautomation.regression;

import net.p3pp3rf1y.devclientautomation.regression.actions.PlayerStatePolicy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

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
		assertEquals(List.of("prepare-canonical-mob-and-stashed-items", "verify-canonical-mob-and-stashed-items-after-server-restart"),
				restart.tests().stream().map(RegressionTest::id).toList());
		assertEquals(List.of(PlayerStatePolicy.CHECKPOINT_PREPARE, PlayerStatePolicy.CHECKPOINT_VERIFY),
				restart.tests().stream().map(RegressionTest::playerState).toList());
		assertEquals(List.of("canonical-mob-items", "canonical-mob-items"), restart.tests().stream().map(RegressionTest::checkpoint).toList());
		assertEquals(List.of(true, false), restart.tests().stream().map(RegressionTest::prepareTestCell).toList());
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

		assertEquals(
				List.of("empty-crafting-times-out-with-slot-diagnostics", "missing-and-out-of-range-entity-handles-fail-deterministically",
						"missing-mob-index-and-menu-fail-deterministically", "missing-controller-and-capabilities-fail-deterministically",
						"inventory-mutation-is-journal-owned", "qualification-and-cleanup-consumer"),
				conformance.tests().stream().map(RegressionTest::id).toList());
		assertEquals(5, conformance.tests().stream().filter(test -> test.tags().contains("runtimeConformance")).count());
		assertEquals(
				List.of("Player crafting result did not appear; client slots=", "Regression value missing has not been bound",
						"outside player interaction range", "Backpack screen is not open", "Captured mob index is missing", "Fixture controller is missing",
						"No fluid capability at endpoint", "No energy capability at endpoint", "Storage inventory screen is not open",
						"Linked settings screen did not open with canonical categories"),
				conformance.tests().stream().flatMap(test -> test.steps().stream()).map(RegressionStep::expectFailure).filter(message -> !message.isEmpty())
						.toList());
		assertEquals(Set.of("isolation", "repeatability"), conformance.tests().getLast().tags());
	}
	@Test
	void parseRejectsIncompleteCheckpointAndAmbiguousExpectedFailure() {
		String test = "{\"schemaVersion\":2,\"suiteId\":\"test\",\"requires\":[],\"isolation\":\"testCell\",\"tests\":[{\"id\":\"case\",\"tags\":[],\"timeoutTicks\":20,%s\"steps\":[{\"id\":\"step\",\"action\":\"menu.close\"%s}]}]}";
		RegressionActionRegistry actions = new RegressionActionRegistry();

		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("\"playerState\":\"checkpointVerify\",", ""), actions));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("\"playerState\":\"unknown\",", ""), actions));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(test.formatted("", ",\"expectFailure\":\"\""), actions));
	}
}
