package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VanillaWorkstationActionBindingTest {
	@Test
	void workstationAwaitCapturesRealSmithingHandlesAndRejectsAmbiguousSources() {
		AwaitVanillaMenuAction action = new AwaitVanillaMenuAction();

		var invocation = RegressionActionTestSupport.bind(action, "{\"kind\":\"smithing\",\"saveAs\":\"smithing\",\"timeoutTicks\":40}");
		var arguments = RegressionActionTestSupport.arguments(invocation, AwaitVanillaMenuAction.Arguments.class);
		assertEquals(VanillaMenuHandle.Kind.SMITHING, arguments.kind());
		assertEquals(List.of(new RegressionValueContract.Value("smithing", RegressionValueTypes.VANILLA_MENU)), invocation.values().produces());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"kind\":\"crafting\",\"ref\":\"old\",\"saveAs\":\"new\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"kind\":\"anvil\",\"saveAs\":\"new\"}"));
	}

	@Test
	void vanillaSlotClickAndHeldBagObservationsHaveStrictPortableArguments() {
		ClickVanillaMenuSlotAction click = new ClickVanillaMenuSlotAction();
		CaptureHeldBackpackUuidAction capture = new CaptureHeldBackpackUuidAction();
		AssertHeldBackpackStateAction state = new AssertHeldBackpackStateAction();

		assertEquals(Set.of("minecraft:client"), RegressionActionTestSupport.bind(click, "{\"slot\":3,\"button\":1}").dependencies());
		var captured = RegressionActionTestSupport.bind(capture, "{\"saveAs\":\"original\"}");
		assertEquals(List.of(new RegressionValueContract.Value("original", RegressionValueTypes.UUID)), captured.values().produces());
		var asserted = RegressionActionTestSupport.bind(state,
				"{\"uuid\":\"original\",\"tier\":\"diamond\",\"storageSlots\":108,\"upgradeSlots\":5,\"contents\":[{\"slot\":0,\"item\":\"minecraft:emerald\",\"count\":7}],\"upgrade\":\"sophisticatedbackpacks:stack_upgrade_tier_2\",\"playerSlot\":8,\"playerItem\":\"minecraft:cobblestone\",\"playerCount\":13}");
		assertEquals(List.of(new RegressionValueContract.Value("original", RegressionValueTypes.UUID)), asserted.values().consumes());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(click, "{\"slot\":0,\"button\":2}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(capture, "{\"saveAs\":\"uuid\",\"extra\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(state, "{\"uuid\":\"original\",\"tier\":\"diamond\"}"));
		String base = "{\"uuid\":\"original\",\"tier\":\"diamond\",\"storageSlots\":108,\"upgradeSlots\":5,\"contents\":[{\"slot\":%d,\"item\":\"minecraft:emerald\",\"count\":7}],\"upgradeSlot\":%d,\"upgrade\":\"sophisticatedbackpacks:stack_upgrade_tier_2\",\"playerSlot\":%d,\"playerItem\":\"minecraft:cobblestone\",\"playerCount\":13}";
		RegressionActionTestSupport.bind(state, base.formatted(107, 4, 35));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(state, base.formatted(108, 4, 35)));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(state, base.formatted(107, 5, 35)));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(state, base.formatted(107, 4, 36)));
	}

	@Test
	void hotbarSelectionUsesAConfiguredSlotAndRejectsInvalidArguments() {
		SelectHotbarSlotAction action = new SelectHotbarSlotAction();

		assertEquals(1,
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{\"slot\":1}"), SelectHotbarSlotAction.Arguments.class).slot());
		assertEquals(Set.of("minecraft:client"), RegressionActionTestSupport.bind(action, "{\"slot\":0}").dependencies());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"slot\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"slot\":9}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"slot\":1.5}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"slot\":1,\"extra\":true}"));
	}

	@Test
	void heldBackpackStateReportsNoScreenDeterministically() {
		assertEquals("screen expected=StorageScreenBase actual=<none>", AssertHeldBackpackStateAction.screenDiagnostic(null));
	}
}
