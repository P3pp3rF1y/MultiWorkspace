package net.p3pp3rf1y.devclientautomation.regression;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedActionConformanceSuiteTest {
	@Test
	void parseConformanceSuiteRetainsDiagnosticCasesAndJournalOwnedMutationSequence() throws IOException {
		RegressionSuite suite;
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-action-conformance.json")) {
			assertNotNull(stream);
			suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
		}
		assertEquals(List.of("empty-crafting-times-out-with-slot-diagnostics", "missing-and-out-of-range-entity-handles-fail-deterministically",
				"missing-mob-index-and-menu-fail-deterministically", "missing-controller-and-capabilities-fail-deterministically",
				"inventory-mutation-is-journal-owned", "qualification-and-cleanup-consumer"), suite.tests().stream().map(RegressionTest::id).toList());
		assertEquals(4, suite.tests().stream().filter(test -> test.steps().stream().anyMatch(step -> !step.expectFailure().isEmpty())).count());
		assertEquals(9, suite.tests().stream().flatMap(test -> test.steps().stream()).filter(step -> !step.expectFailure().isEmpty()).count());
		assertTrue(suite.tests().stream().allMatch(RegressionTest::prepareTestCell));
		assertEquals("assert.playerInventoryBaseline", suite.tests().get(suite.tests().size() - 1).steps().get(0).action().action().metadata().id());
		assertEquals(
				List.of("assert.playerInventoryBaseline", "player.createMainHandItem", "player.setInventorySlot", "fixture.player.setGameMode",
						"player.openInventory", "menu.clickPlayerInventorySlot"),
				suite.tests().get(4).steps().stream().map(step -> step.action().action().metadata().id()).toList());
	}

	@Test
	void allSixSelectedCasesBindStrictExpectedFailuresAndOwnedCleanup() throws IOException {
		RegressionSuite suite;
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-action-conformance.json")) {
			assertNotNull(stream);
			suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
		}
		assertEquals(6, suite.tests().size());
		assertEquals(4, suite.tests().stream().filter(test -> test.steps().stream().anyMatch(step -> !step.expectFailure().isEmpty())).count());
		assertTrue(suite.tests().stream().allMatch(RegressionTest::prepareTestCell));
		assertEquals("qualification-and-cleanup-consumer", suite.tests().get(5).id());
		assertEquals("assert.playerInventoryBaseline", suite.tests().get(5).steps().get(0).action().action().metadata().id());
	}
}
