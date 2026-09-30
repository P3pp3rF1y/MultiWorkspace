package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceHeldBackpackActionTest {
	@Test
	void bindsNoArgumentsAndRejectsUnexpectedArguments() {
		PlaceHeldBackpackAction action = new PlaceHeldBackpackAction();

		assertSame(action, RegressionActionTestSupport.bind(action, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": 20}"));
		RegressionActionTestSupport.assertMetadata(action, "backpack.placeHeld", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}
}
