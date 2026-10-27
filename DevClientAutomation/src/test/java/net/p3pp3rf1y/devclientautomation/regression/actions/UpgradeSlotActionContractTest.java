package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpgradeSlotActionContractTest {
	@Test
	void bindsExactUpgradeTabAndRejectsInvalidArguments() {
		OpenUpgradeTabAction action = new OpenUpgradeTabAction();
		var defaults = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), OpenUpgradeTabAction.Arguments.class);
		assertEquals(0, defaults.upgrade());
		RegressionActionTestSupport.bind(action, "{\"upgrade\":1}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"upgrade\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"unknown\":1}"));
	}

	@Test
	void bindsExplicitRefillSettingAndRejectsInvalidArguments() {
		SetCraftingRefillAction action = new SetCraftingRefillAction();
		var defaults = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{\"enabled\":true}"),
				SetCraftingRefillAction.Arguments.class);
		assertEquals(0, defaults.upgrade());
		assertEquals(true, defaults.enabled());
		RegressionActionTestSupport.bind(action, "{\"upgrade\":1,\"enabled\":false}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"enabled\":\"true\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"upgrade\":-1,\"enabled\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"enabled\":true,\"unknown\":1}"));
	}

	@Test
	void bindsExactUpgradeAndSlotAndRejectsInvalidTargets() {
		ClickUpgradeSlotAction action = new ClickUpgradeSlotAction();
		var defaults = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), ClickUpgradeSlotAction.Arguments.class);
		assertEquals(0, defaults.upgrade());
		assertEquals(0, defaults.slot());
		assertEquals("pickup", defaults.click());
		assertEquals(0, defaults.button());
		RegressionActionTestSupport.bind(action, "{\"upgrade\":1,\"slot\":9}");
		RegressionActionTestSupport.bind(action, "{\"slot\":9,\"click\":\"quickMove\",\"button\":1}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"upgrade\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"slot\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"click\":\"other\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"button\":2}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"unknown\":1}"));
	}

	@Test
	void bindsEmptyAndNonemptyExpectationsWithBoundedWaits() {
		AssertUpgradeSlotAction action = new AssertUpgradeSlotAction();
		var defaults = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{\"item\":\"minecraft:oak_planks\"}"),
				AssertUpgradeSlotAction.Arguments.class);
		assertEquals(1, defaults.count());
		assertEquals(100, defaults.timeoutTicks());
		RegressionActionTestSupport.bind(action, "{\"slot\":9,\"item\":\"minecraft:air\",\"count\":0,\"timeoutTicks\":5}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\":\"minecraft:air\",\"count\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\":\"minecraft:air\",\"timeoutTicks\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\":\"minecraft:air\",\"unknown\":1}"));
	}
}
