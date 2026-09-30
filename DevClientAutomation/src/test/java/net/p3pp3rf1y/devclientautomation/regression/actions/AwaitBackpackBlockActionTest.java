package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AwaitBackpackBlockActionTest {
	@Test
	void usesDefaultAndExplicitTimeouts() {
		AwaitBackpackBlockAction action = new AwaitBackpackBlockAction();

		assertEquals(100,
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), AwaitBackpackBlockAction.Arguments.class).timeoutTicks());
		assertEquals(40, RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": 40}"), AwaitBackpackBlockAction.Arguments.class).timeoutTicks());
		RegressionActionTestSupport.assertMetadata(action, "await.backpackBlock", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Test
	void rejectsInvalidTimeout() {
		AwaitBackpackBlockAction action = new AwaitBackpackBlockAction();

		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": 0}"));
	}
}
