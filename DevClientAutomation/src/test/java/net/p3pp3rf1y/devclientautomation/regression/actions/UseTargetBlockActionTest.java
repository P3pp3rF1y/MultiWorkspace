package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UseTargetBlockActionTest {
	@Test
	void bindsNoArgumentsAndRejectsUnexpectedArguments() {
		UseTargetBlockAction action = new UseTargetBlockAction();

		assertSame(action, RegressionActionTestSupport.bind(action, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"hand\": \"main\"}"));
		RegressionActionTestSupport.assertMetadata(action, "player.useTargetBlock", Set.of("minecraft:client"));
	}
}
