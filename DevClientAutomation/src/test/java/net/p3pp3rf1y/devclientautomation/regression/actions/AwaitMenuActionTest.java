package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AwaitMenuActionTest {
	@Test
	void usesDefaultAndExplicitTimeouts() {
		AwaitMenuAction action = new AwaitMenuAction();

		assertEquals(100,
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), AwaitMenuAction.Arguments.class).timeoutTicks());
		assertEquals(40, RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": 40}"), AwaitMenuAction.Arguments.class).timeoutTicks());
		assertEquals("backpackBlock",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), AwaitMenuAction.Arguments.class).kind());
		assertEquals("await.menu", action.metadata().id());
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client"), action.metadata().dependencies());
	}

	@Test
	void rejectsInvalidTimeout() {
		AwaitMenuAction action = new AwaitMenuAction();

		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": false}"));
	}
}
