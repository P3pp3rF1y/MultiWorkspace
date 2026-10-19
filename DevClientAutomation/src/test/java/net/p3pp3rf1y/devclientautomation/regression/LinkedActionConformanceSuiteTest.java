package net.p3pp3rf1y.devclientautomation.regression;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedActionConformanceSuiteTest {
	@Test
	void parseConformanceSuiteRetainsDiagnosticCasesAndJournalOwnedMutationSequence() throws IOException {
		RegressionSuite suite;
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-action-conformance.json")) {
			assertTrue(stream != null);
			suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
		}

		assertEquals(List.of("empty-crafting-times-out-with-slot-diagnostics", "missing-and-out-of-range-entity-handles-fail-deterministically",
				"missing-mob-index-and-menu-fail-deterministically", "missing-controller-and-capabilities-fail-deterministically",
				"inventory-mutation-is-journal-owned", "qualification-and-cleanup-consumer"), suite.tests().stream().map(RegressionTest::id).toList());
		assertEquals(4, suite.tests().stream().filter(test -> test.steps().stream().anyMatch(step -> !step.expectFailure().isEmpty())).count());
		assertEquals(10, suite.tests().stream().flatMap(test -> test.steps().stream()).filter(step -> !step.expectFailure().isEmpty()).count());
		assertTrue(suite.tests().stream().allMatch(RegressionTest::prepareTestCell));
		RegressionTest consumer = suite.tests().getLast();
		assertEquals("assert.playerInventoryBaseline", consumer.steps().getFirst().action().action().metadata().id());
		RegressionTest mutation = suite.tests().get(4);
		assertEquals(
				List.of("assert.playerInventoryBaseline", "player.createMainHandItem", "player.setInventorySlot", "fixture.player.setGameMode",
						"player.openInventory", "menu.clickPlayerInventorySlot"),
				mutation.steps().stream().map(step -> step.action().action().metadata().id()).toList());
	}
}
