package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResetPlayerActionTest {
	@Test
	void bindsNoArgumentsAndRejectsUnexpectedArguments() {
		ResetPlayerAction action = new ResetPlayerAction();

		assertSame(action, RegressionActionTestSupport.bind(action, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"unexpected\": true}"));
		RegressionActionTestSupport.assertMetadata(action, "player.reset", Set.of("minecraft:client"));
	}
}
