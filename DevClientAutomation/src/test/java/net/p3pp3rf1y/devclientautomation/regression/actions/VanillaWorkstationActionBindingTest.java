package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VanillaWorkstationActionBindingTest {
	@Test
	void clickVanillaMenuSlotBindsLeftClickAndQuickMoveDefaults() {
		ClickVanillaMenuSlotAction action = new ClickVanillaMenuSlotAction();

		var defaultClick = RegressionActionTestSupport.bind(action, "{\"slot\":0}");
		var quickMove = RegressionActionTestSupport.bind(action, "{\"slot\":3,\"quickMove\":true}");

		assertEquals(0, RegressionActionTestSupport.arguments(defaultClick, ClickVanillaMenuSlotAction.Arguments.class).button());
		assertEquals(false, RegressionActionTestSupport.arguments(defaultClick, ClickVanillaMenuSlotAction.Arguments.class).quickMove());
		assertEquals(true, RegressionActionTestSupport.arguments(quickMove, ClickVanillaMenuSlotAction.Arguments.class).quickMove());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"slot\":0,\"button\":1}"));
	}

	@Test
	void selectHotbarSlotBindsConfiguredHotbarRange() {
		SelectHotbarSlotAction action = new SelectHotbarSlotAction();

		var invocation = RegressionActionTestSupport.bind(action, "{\"slot\":8}");

		assertEquals(8, RegressionActionTestSupport.arguments(invocation, SelectHotbarSlotAction.Arguments.class).slot());
		assertEquals(Set.of("minecraft:client"), invocation.dependencies());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"slot\":9}"));
	}

	@Test
	void linkedBackpackTierUpgradeAssertionsBindOnlyCoveredTiers() {
		AssertLinkedBackpackTierUpgradeResultAction result = new AssertLinkedBackpackTierUpgradeResultAction();
		AssertLinkedBackpackTierUpgradeDeliveryAction delivery = new AssertLinkedBackpackTierUpgradeDeliveryAction();

		assertEquals("diamond", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(result, "{\"tier\":\"diamond\"}"), AssertLinkedBackpackTierUpgradeResultAction.Arguments.class)
				.tier());
		assertEquals("netherite", RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(delivery, "{\"tier\":\"netherite\"}"),
				AssertLinkedBackpackTierUpgradeDeliveryAction.Arguments.class).tier());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(result, "{\"tier\":\"gold\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(delivery, "{\"tier\":\"gold\"}"));
	}
}
