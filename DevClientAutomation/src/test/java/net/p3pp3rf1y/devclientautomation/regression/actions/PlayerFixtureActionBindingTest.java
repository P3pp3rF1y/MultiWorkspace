package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		UseHeldItemOnTargetAction.Arguments useHeldItem = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offset\":[3,0,-2]}"), UseHeldItemOnTargetAction.Arguments.class);
		assertTrue(useHeldItem.useFixtureOffset());
		assertEquals(3, useHeldItem.x());
		assertEquals(-2, useHeldItem.z());
		UseHeldItemOnTargetAction.Arguments offsetZ = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":1}"), UseHeldItemOnTargetAction.Arguments.class);
		assertFalse(offsetZ.useFixtureOffset());
		UseHeldItemOnTargetAction.Arguments noOffset = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{}"), UseHeldItemOnTargetAction.Arguments.class);
		assertResolvedTarget(noOffset, 10, 20, 30);
		assertResolvedTarget(offsetZ, 10, 20, 31);
		assertResolvedTarget(useHeldItem, 11, 20, 30);
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
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offset\":[3,0,-2],\"offsetZ\":1}"));
	}

	private static void assertResolvedTarget(UseHeldItemOnTargetAction.Arguments arguments, int expectedX, int expectedY, int expectedZ) {
		UseHeldItemOnTargetAction.TargetCoordinates resolved = UseHeldItemOnTargetAction.resolveTargetCoordinates(10, 20, 30, 8, 20, 32, arguments);
		assertEquals(expectedX, resolved.x());
		assertEquals(expectedY, resolved.y());
		assertEquals(expectedZ, resolved.z());
	}
	@Test
	void bindSelectedSourcePlayerAndFixtureActions() {
		RegressionActionTestSupport.bind(new BreakTargetBlockAction(), "{}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new BreakTargetBlockAction(), "{\"unknown\":true}"));
		RegressionActionTestSupport.bind(new SpawnFixturePigAction(), "{\"saveAs\":\"pig\"}");
		RegressionActionTestSupport.bind(new UseFixtureEntityAction(), "{\"ref\":\"pig\"}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SpawnFixturePigAction(), "{}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseFixtureEntityAction(), "{\"ref\":1}"));
		RegressionActionTestSupport.bind(new SendPlayerCommandAction(), "{\"command\":\"sbp template list\"}");
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SendPlayerCommandAction(), "{\"command\":\"/sbp template list\"}"));
		RegressionActionTestSupport.bind(new AwaitPlayerCraftingResultAction(), "{}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AwaitPlayerCraftingResultAction(), "{\"timeoutTicks\":0}"));
		RegressionActionTestSupport.bind(new CreateLockToolAction(), "{}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new CreateLockToolAction(), "{\"toggle\":true}"));
		RegressionActionTestSupport.bind(new OpenPlayerInventoryAction(), "{}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new OpenPlayerInventoryAction(), "{\"unknown\":true}"));
		RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":1}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":\"1\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"unknown\":true}"));
	}

	@Test
	void registryIncludesExactlyTheEightNewlyAssignedBindings() {
		Map<String, RegressionActionMetadata> actions = new RegressionActionRegistry().all();
		for (String id : new String[]{"await.playerCraftingResult", "player.breakTargetBlock", "player.openInventory", "player.sendCommand",
				"player.sneakUseFixtureEntity", "player.useHeldItemOnTarget", "fixture.entity.spawnPig", "fixture.storage.createLockTool"}) {
			assertTrue(actions.containsKey(id), id);
		}
	}
}
