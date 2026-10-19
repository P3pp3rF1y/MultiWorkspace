package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceHeldBackpackActionTest {
	@Test
	void bindsNoArgumentsAndRejectsUnexpectedArguments() {
		PlaceHeldBackpackAction action = new PlaceHeldBackpackAction();
		assertSame(action, RegressionActionTestSupport.bind(action, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": 20}"));
		assertEquals("backpack.placeHeld", action.metadata().id());
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedbackpacks"), action.metadata().dependencies());
	}

	@Test
	void bindsExactPlacementOffset() {
		PlaceHeldBackpackAction action = new PlaceHeldBackpackAction();
		assertEquals(new PlaceHeldBackpackAction.Arguments(true, -1, 0, -1), RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"offset\":[-1,0,-1]}"), PlaceHeldBackpackAction.Arguments.class));
		assertEquals(new PlaceHeldBackpackAction.Arguments(false, 0, 0, 0),
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), PlaceHeldBackpackAction.Arguments.class));
		for (String invalid : new String[]{"{\"offset\":null}", "{\"offset\":[1,2]}", "{\"offset\":[1.5,0,0]}", "{\"offset\":[\"1\",0,0]}"}) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, invalid));
		}
	}

	@Test
	void bindsExactPlacementOffsetAndRetainsLegacyOffsetZ() {
		PlaceHeldBackpackAction action = new PlaceHeldBackpackAction();
		assertEquals(new PlaceHeldBackpackAction.Arguments(true, 0, 0, 1), RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"offset\":[0,0,1]}"), PlaceHeldBackpackAction.Arguments.class));
		assertEquals(new PlaceHeldBackpackAction.Arguments(true, 0, 0, 1),
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{\"offsetZ\":1}"), PlaceHeldBackpackAction.Arguments.class));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"offset\":[0,0,1],\"offsetZ\":1}"));
	}
}
