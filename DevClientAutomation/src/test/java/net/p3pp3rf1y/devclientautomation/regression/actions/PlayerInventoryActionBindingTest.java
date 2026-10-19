package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerInventoryActionBindingTest {
	@Test
	void playerInventoryActionsBindDefaultsAndRejectInvalidArguments() {
		SetPlayerGameModeAction.Arguments mode = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new SetPlayerGameModeAction(), "{\"mode\":\"creative\"}"), SetPlayerGameModeAction.Arguments.class);
		CopyPlayerInventoryItemAction.Arguments copy = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new CopyPlayerInventoryItemAction(), "{}"), CopyPlayerInventoryItemAction.Arguments.class);

		assertEquals("creative", mode.mode());
		assertEquals(0, copy.source());
		assertEquals(10, copy.target());
		RegressionActionTestSupport.bind(new AssertPlayerInventoryBaselineAction(), "{}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetPlayerGameModeAction(), "{\"mode\":\"other\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetPlayerGameModeAction(), "{\"mode\":\"survival\",\"extra\":1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new CopyPlayerInventoryItemAction(), "{\"source\":10,\"target\":10}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new CopyPlayerInventoryItemAction(), "{\"target\":36}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertPlayerInventoryBaselineAction(), "{\"extra\":true}"));
	}
}
