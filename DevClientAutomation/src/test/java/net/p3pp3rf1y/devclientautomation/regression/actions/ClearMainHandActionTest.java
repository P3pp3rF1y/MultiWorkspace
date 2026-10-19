package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClearMainHandActionTest {
	@Test
	void bindsNoArgumentsAndRejectsUnexpectedArguments() {
		ClearMainHandAction action = new ClearMainHandAction();

		assertSame(action, RegressionActionTestSupport.bind(action, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"closeMenu\": true}"));
		RegressionActionTestSupport.assertMetadata(action, "player.clearMainHand", Set.of("minecraft:client"));
	}
}
