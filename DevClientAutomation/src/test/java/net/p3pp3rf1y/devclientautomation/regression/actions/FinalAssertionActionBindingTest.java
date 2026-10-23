package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinalAssertionActionBindingTest {
	@Test
	void doubleChestControllerAssertionBindsOptionalStrictMode() {
		AssertDoubleChestAction action = new AssertDoubleChestAction();
		assertEquals(new AssertDoubleChestAction.Arguments(false),
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), AssertDoubleChestAction.Arguments.class));
		assertEquals(new AssertDoubleChestAction.Arguments(true), RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"controller\":true}"), AssertDoubleChestAction.Arguments.class));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"controller\":1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"count\":1}"));
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage"), action.metadata().dependencies());
	}
}
