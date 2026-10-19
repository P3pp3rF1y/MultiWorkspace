package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetPlayerInventorySlotActionTest {
	@Test
	void slotZeroIsRequiredAndClearMatchingIsOptional() {
		SetPlayerInventorySlotAction action = new SetPlayerInventorySlotAction();
		SetPlayerInventorySlotAction.Arguments zero = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(action, "{\"slot\":0,\"item\":\"minecraft:diamond\",\"count\":1}"),
				SetPlayerInventorySlotAction.Arguments.class);
		assertEquals(0, zero.slot());
		assertFalse(zero.clearMatching());
		assertEquals("2", action.metadata().revision());
		SetPlayerInventorySlotAction.Arguments clearing = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(action, "{\"slot\":1,\"item\":\"minecraft:diamond\",\"count\":1,\"clearMatching\":true}"),
				SetPlayerInventorySlotAction.Arguments.class);
		assertTrue(clearing.clearMatching());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\":\"minecraft:diamond\",\"count\":1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"slot\":-1,\"item\":\"minecraft:diamond\",\"count\":1}"));
	}
}
