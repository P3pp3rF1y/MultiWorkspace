package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerFixtureActionBindingTest {
	@Test
	void playerAndFixtureActionsBindTypedArgumentsAndRejectInvalidValues() {
		RegressionActionTestSupport.bind(new BreakTargetBlockAction(), "{}");
		SpawnFixturePigAction.Arguments pig = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new SpawnFixturePigAction(), "{\"saveAs\":\"pig\",\"offsetX\":2}"), SpawnFixturePigAction.Arguments.class);
		assertEquals("pig", pig.saveAs());
		assertEquals(2, pig.offsetX());
		assertEquals("pig", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new UseFixtureEntityAction(), "{\"ref\":\"pig\"}"), UseFixtureEntityAction.Arguments.class).ref());
		RegressionActionTestSupport.bind(new SendPlayerCommandAction(), "{\"command\":\"sbp template list\"}");
		RegressionActionTestSupport.bind(new AwaitPlayerCraftingResultAction(), "{}");
		RegressionActionTestSupport.bind(new CreateLockToolAction(), "{}");
		RegressionActionTestSupport.bind(new OpenPlayerInventoryAction(), "{}");
		RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":1}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new BreakTargetBlockAction(), "{\"unknown\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SpawnFixturePigAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SpawnFixturePigAction(), "{\"saveAs\":\"pig\",\"offsetX\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseFixtureEntityAction(), "{\"ref\":1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SendPlayerCommandAction(), "{\"command\":\"/sbp template list\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AwaitPlayerCraftingResultAction(), "{\"timeoutTicks\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new CreateLockToolAction(), "{\"toggle\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new OpenPlayerInventoryAction(), "{\"unknown\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":\"1\"}"));
	}
}
