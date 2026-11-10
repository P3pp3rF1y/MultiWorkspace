package net.p3pp3rf1y.devclientautomation.regression;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReliquaryResurrectionSuiteTest {
	@Test
	void suiteQualifiesWhenReliquaryIsAvailableAndFailsBeforeMutationWhenItIsMissing() {
		RegressionSuite suite = RegressionCoordinator.loadSuites().get("reliquary-resurrection");
		assertEquals(List.of("reliquary"), suite.requires().stream().toList());
		assertEquals(11, suite.tests().size());

		RegressionCoordinator available = new RegressionCoordinator(id -> true, () -> Map.of(suite.suiteId(), suite));
		RegressionCoordinator missing = new RegressionCoordinator(id -> !id.equals("reliquary"), () -> Map.of(suite.suiteId(), suite));

		assertEquals("running", available.start(suite.suiteId(), suite.tests().get(0).id(), "").outcome());
		var status = missing.start(suite.suiteId(), suite.tests().get(0).id(), "");
		assertEquals("skipped", status.outcome());
		assertEquals("expectedUnavailable", status.category());
		assertTrue(status.steps().isEmpty());
		assertTrue(status.cleanup().isEmpty());
	}
}
