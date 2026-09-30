package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssertMenuActionTest {
	@Test
	void usesDefaultSlot() {
		AssertMenuAction action = new AssertMenuAction();

		AssertMenuAction.Arguments arguments = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": 7}"), AssertMenuAction.Arguments.class);

		assertEquals(0, arguments.slot());
		assertEquals("backpackBlock", arguments.kind());
		assertEquals("minecraft:diamond", arguments.item());
		assertEquals(7, arguments.count());
		assertEquals("assert.menu", action.metadata().id());
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client"), action.metadata().dependencies());
	}

	@Test
	void rejectsInvalidArguments() {
		AssertMenuAction action = new AssertMenuAction();

		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": -1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"slot\": 0, \"item\": \"minecraft:diamond\", \"count\": 1, \"extra\": true}"));
	}
}
