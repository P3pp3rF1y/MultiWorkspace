package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackpackMigrationActionBindingTest {
	@Test
	void filterContractsRejectUnknownOperationsAndAmbiguousExpectedResults() {
		var configure = new ConfigureBackpackFilterAction();
		assertEquals("advanced",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(configure, "{\"upgrade\":\"advanced\",\"operation\":\"output\"}"),
						ConfigureBackpackFilterAction.Arguments.class).upgrade());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(configure, "{\"upgrade\":\"basic\",\"operation\":\"both\"}"));
		var probe = new ProbeBackpackFilterAction();
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(probe, "{\"operation\":\"output\",\"item\":\"minecraft:diamond\",\"returnedCount\":1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(probe, "{\"operation\":\"input\",\"item\":\"minecraft:diamond\"}"));
		var observe = new AssertHeldBackpackFilterAction();
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(observe, "{\"upgrade\":\"basic\",\"operation\":\"input\",\"item\":\"minecraft:air\",\"count\":1}"));
	}

	@Test
	void restockContractsRequireExplicitObservations() {
		var configure = new ConfigureBackpackRestockAction();
		assertEquals("empty",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(configure, "{\"upgrade\":\"advanced\",\"mode\":\"empty\"}"),
						ConfigureBackpackRestockAction.Arguments.class).mode());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(configure, "{\"upgrade\":\"basic\",\"mode\":\"insert\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseHeldBackpackOnStorageAction(), "{\"target\":\"nearby\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertRestockBackpackAction(), "{\"upgrade\":\"basic\",\"mode\":\"allow\",\"diamonds\":5}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertRestockBarrelAction(), "{\"diamonds\":0,\"redstone\":-1}"));
	}
}
