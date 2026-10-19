package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResetRegionActionTest {
	@Test
	void bindsNoArgumentsAndRejectsUnexpectedArguments() {
		ResetRegionAction action = new ResetRegionAction();

		assertSame(action, RegressionActionTestSupport.bind(action, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"unexpected\": true}"));
		RegressionActionTestSupport.assertMetadata(action, "fixture.region.reset", Set.of("minecraft:client"));
	}
}
