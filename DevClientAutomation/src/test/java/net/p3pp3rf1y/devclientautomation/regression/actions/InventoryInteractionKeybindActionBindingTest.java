package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryInteractionKeybindActionBindingTest {
	@Test
	void keybindRequiresAnExplicitSupportedOperationAndSortHover() {
		PressInventoryInteractionKeybindAction action = new PressInventoryInteractionKeybindAction();

		var vanilla = RegressionActionTestSupport.bind(action,
				"{\"operation\":\"sort\",\"target\":\"player\",\"slot\":9,\"modifier\":\"none\",\"ref\":\"menu\"}");
		assertEquals("sort", RegressionActionTestSupport.arguments(vanilla, PressInventoryInteractionKeybindAction.Arguments.class).operation());
		assertEquals(List.of(new RegressionValueContract.Value("menu", RegressionValueTypes.VANILLA_MENU)), vanilla.values().consumes());
		assertEquals(Set.of("minecraft:client", "sophisticatedinventoryinteractions"), vanilla.dependencies());
		var backpack = RegressionActionTestSupport.bind(action, "{\"operation\":\"transferToPlayer\",\"modifier\":\"shift\"}");
		assertEquals(Set.of("minecraft:client", "sophisticatedbackpacks", "sophisticatedinventoryinteractions"), backpack.dependencies());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"operation\":\"sort\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"operation\":\"transferToPlayer\",\"slot\":0}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"operation\":\"sort\",\"target\":\"playerOnly\",\"slot\":0}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"operation\":\"sort\",\"target\":\"container\",\"slot\":0,\"modifier\":\"control\"}"));
	}

	@Test
	void vanillaFixtureAndAwaitUseTypedImmutableHandles() {
		OpenInventoryInteractionMenuAction open = new OpenInventoryInteractionMenuAction();
		AwaitVanillaMenuAction await = new AwaitVanillaMenuAction();

		var opened = RegressionActionTestSupport.bind(open, "{\"kind\":\"chest\",\"fixture\":\"storageSort\",\"saveAs\":\"opened\"}");
		var awaited = RegressionActionTestSupport.bind(await, "{\"ref\":\"opened\",\"saveAs\":\"menu\",\"timeoutTicks\":40}");
		assertEquals("opened", RegressionActionTestSupport.arguments(opened, OpenInventoryInteractionMenuAction.Arguments.class).saveAs());
		assertEquals(40, RegressionActionTestSupport.arguments(awaited, AwaitVanillaMenuAction.Arguments.class).timeoutTicks());
		assertEquals(List.of(new RegressionValueContract.Value("opened", RegressionValueTypes.VANILLA_MENU)), opened.values().produces());
		assertEquals(opened.values().produces(), awaited.values().consumes());
		assertEquals(List.of(new RegressionValueContract.Value("menu", RegressionValueTypes.VANILLA_MENU)), awaited.values().produces());
		assertTrue(VanillaMenuHandle.class.isRecord());
		assertEquals(List.of("kind", "containerId"),
				Arrays.stream(VanillaMenuHandle.class.getRecordComponents()).map(RecordComponent::getName).toList());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(open, "{\"kind\":\"furnace\",\"fixture\":\"storageSort\",\"saveAs\":\"menu\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(open, "{\"kind\":\"unknown\",\"fixture\":\"playerSort\",\"saveAs\":\"menu\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(await, "{\"ref\":\"opened\",\"saveAs\":\"menu\",\"timeoutTicks\":0}"));
	}

	@Test
	void inventoryAssertionsRequireExactItemRegionAndHandleContracts() {
		AssertInventoryInteractionCountAction action = new AssertInventoryInteractionCountAction();
		var container = RegressionActionTestSupport.bind(action,
				"{\"target\":\"container\",\"ref\":\"menu\",\"item\":\"minecraft:cobblestone\",\"count\":3,\"stacks\":1}");
		var backpack = RegressionActionTestSupport.bind(action, "{\"target\":\"backpack\",\"item\":\"minecraft:cobblestone\",\"count\":3}");

		assertEquals(List.of(new RegressionValueContract.Value("menu", RegressionValueTypes.VANILLA_MENU)), container.values().consumes());
		assertEquals(Set.of("minecraft:client", "sophisticatedbackpacks"), backpack.dependencies());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"target\":\"container\",\"item\":\"minecraft:cobblestone\",\"count\":0}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"target\":\"player\",\"ref\":\"menu\",\"item\":\"minecraft:cobblestone\",\"count\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action,
				"{\"target\":\"craftingRegion\",\"ref\":\"menu\",\"item\":\"minecraft:cobblestone\",\"count\":0}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"target\":\"furnaceRegion\",\"ref\":\"menu\",\"count\":1}"));
	}
}
