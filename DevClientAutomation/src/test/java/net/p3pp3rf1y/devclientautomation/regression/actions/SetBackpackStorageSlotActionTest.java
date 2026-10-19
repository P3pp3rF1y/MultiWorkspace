package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SetBackpackStorageSlotActionTest {
	@Test
	void usesDefaultStorageSlot() {
		SetBackpackStorageSlotAction action = new SetBackpackStorageSlotAction();

		SetBackpackStorageSlotAction.Arguments arguments = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": 7}"), SetBackpackStorageSlotAction.Arguments.class);

		assertEquals(0, arguments.slot());
		assertEquals("minecraft:diamond", arguments.item());
		assertEquals(7, arguments.count());
		RegressionActionTestSupport.assertMetadata(action, "backpack.setStorageSlot", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Test
	void bindsExplicitStorageSlot() {
		SetBackpackStorageSlotAction action = new SetBackpackStorageSlotAction();

		SetBackpackStorageSlotAction.Arguments arguments = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(action, "{\"slot\": 3, \"item\": \"minecraft:emerald\", \"count\": 2}"),
				SetBackpackStorageSlotAction.Arguments.class);

		assertEquals(3, arguments.slot());
		assertEquals("minecraft:emerald", arguments.item());
		assertEquals(2, arguments.count());
	}

	@Test
	void rejectsInvalidSlotAndCount() {
		SetBackpackStorageSlotAction action = new SetBackpackStorageSlotAction();

		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"slot\": -1, \"item\": \"minecraft:diamond\", \"count\": 1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": 0}"));
	}
}
