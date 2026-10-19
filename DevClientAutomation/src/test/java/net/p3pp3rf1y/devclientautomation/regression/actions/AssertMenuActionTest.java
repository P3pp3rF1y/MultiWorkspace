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
		assertEquals(false, arguments.playerInventory());
		assertEquals(0, arguments.stableTicks());
		assertEquals(null, arguments.ref());
		assertEquals("assert.menu", action.metadata().id());
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client"), action.metadata().dependencies());
	}

	@Test
	void rejectsInvalidArguments() {
		AssertMenuAction action = new AssertMenuAction();

		assertEquals(0, RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": 0}"), AssertMenuAction.Arguments.class)
				.count());
		AssertMenuAction.Arguments playerSlot = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(action, "{\"item\":\"minecraft:diamond\",\"count\":1,\"playerInventory\":true,\"stableTicks\":3}"),
				AssertMenuAction.Arguments.class);
		assertEquals(true, playerSlot.playerInventory());
		assertEquals(3, playerSlot.stableTicks());
		assertEquals(null, playerSlot.ref());
		assertEquals("moving",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(action, "{\"kind\":\"moving\",\"ref\":\"boat\",\"item\":\"minecraft:diamond\",\"count\":1}"),
						AssertMenuAction.Arguments.class).kind());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": -1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\":\"minecraft:diamond\",\"stableTicks\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"kind\":\"moving\",\"item\":\"minecraft:diamond\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"ref\":\"boat\",\"item\":\"minecraft:diamond\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"slot\": 0, \"item\": \"minecraft:diamond\", \"count\": 1, \"extra\": true}"));
	}

	@Test
	void movingMenusRequireExactReferenceWhileRetainingExtendedArguments() {
		AssertMenuAction.Arguments arguments = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new AssertMenuAction(),
						"{\"kind\":\"moving\",\"item\":\"minecraft:diamond\",\"count\":0,\"playerInventory\":true,\"stableTicks\":3,\"ref\":\"primary\"}"),
				AssertMenuAction.Arguments.class);

		assertEquals("primary", arguments.ref());
		assertEquals(0, arguments.count());
		assertEquals(true, arguments.playerInventory());
		assertEquals(3, arguments.stableTicks());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMenuAction(), "{\"kind\":\"moving\",\"item\":\"minecraft:diamond\",\"count\":1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMenuAction(), "{\"item\":\"minecraft:diamond\",\"count\":1,\"ref\":\"primary\"}"));
	}
}
