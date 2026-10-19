package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScanFixtureControllerActionTest {
	@Test
	void parseArgumentsAcceptsOnlyExactIntegersWithinIntRange() {
		ScanFixtureControllerAction action = new ScanFixtureControllerAction();

		ScanFixtureControllerAction.Arguments arguments = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"offset\":[-1,0,2147483647]}"), ScanFixtureControllerAction.Arguments.class);

		assertEquals(-1, arguments.x());
		assertEquals(0, arguments.y());
		assertEquals(Integer.MAX_VALUE, arguments.z());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"offset\":[2147483648,0,0]}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"offset\":[1.0,0,0]}"));
	}
}
