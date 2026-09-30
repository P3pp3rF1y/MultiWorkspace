package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionFoundationContractTest {
	@Test
	void heldUseAndBlockPlacementRejectArguments() {
		assertNoArguments(new UseHeldItemAction(), "player.useHeldItem");
		assertNoArguments(new PlaceHeldBlockAction(), "player.placeHeldBlock");
		assertNoArguments(new CloseMenuAction(), "menu.close");
	}

	@Test
	void mainHandItemBindsDefaultCountAndRejectsWrongTypes() {
		CreateMainHandItemAction action = new CreateMainHandItemAction();
		CreateMainHandItemAction.Arguments arguments = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"item\": \"sophisticatedstorage:barrel\"}"), CreateMainHandItemAction.Arguments.class);
		assertEquals("sophisticatedstorage:barrel", arguments.item());
		assertEquals(1, arguments.count());
		RegressionActionTestSupport.assertMetadata(action, "player.createMainHandItem", Set.of("minecraft:client"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": false}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:stone\", \"count\": 0}"));
	}

	@Test
	void storageBlockWaitBindsTimeoutAndRejectsUnknownArguments() {
		AwaitStorageBlockAction action = new AwaitStorageBlockAction();
		assertEquals(100,
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), AwaitStorageBlockAction.Arguments.class).timeoutTicks());
		assertEquals(20, RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": 20}"), AwaitStorageBlockAction.Arguments.class).timeoutTicks());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": true}"));
	}

	@Test
	void storageSlotBindsDefaultAndRejectsInvalidValues() {
		SetStorageBlockSlotAction action = new SetStorageBlockSlotAction();
		SetStorageBlockSlotAction.Arguments arguments = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": 7}"), SetStorageBlockSlotAction.Arguments.class);
		assertEquals(0, arguments.slot());
		assertEquals("minecraft:diamond", arguments.item());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"slot\": -1, \"item\": \"minecraft:diamond\", \"count\": 1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": \"one\"}"));
	}

	@Test
	void playerInventorySlotRequiresValidFixtureValues() {
		SetPlayerInventorySlotAction action = new SetPlayerInventorySlotAction();
		SetPlayerInventorySlotAction.Arguments arguments = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(action, "{\"slot\": 9, \"item\": \"minecraft:stick\", \"count\": 1}"),
				SetPlayerInventorySlotAction.Arguments.class);
		assertEquals(9, arguments.slot());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"slot\": 0, \"item\": \"minecraft:stick\", \"count\": 1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"slot\": 9, \"item\": 1, \"count\": 1}"));
	}

	@Test
	void menuCloseWaitBindsTimeoutAndRejectsInvalidValues() {
		AwaitMenuClosedAction action = new AwaitMenuClosedAction();
		assertEquals(100,
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), AwaitMenuClosedAction.Arguments.class).timeoutTicks());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": 0}"));
	}

	@Test
	void clickAndQuickMoveUseStableSlotSemantics() {
		ClickMenuSlotAction click = new ClickMenuSlotAction();
		assertEquals(0, RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(click, "{}"), ClickMenuSlotAction.Arguments.class).slot());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(click, "{\"slot\": false}"));

		QuickMovePlayerInventorySlotAction quickMove = new QuickMovePlayerInventorySlotAction();
		assertEquals(9,
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(quickMove, "{\"inventorySlot\": 9}"), QuickMovePlayerInventorySlotAction.Arguments.class)
						.inventorySlot());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(quickMove, "{\"inventorySlot\": 8}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(quickMove, "{\"inventorySlot\": \"9\"}"));
	}

	private static void assertNoArguments(RegressionAction action, String id) {
		RegressionActionTestSupport.bind(action, "{}");
		RegressionActionTestSupport.assertMetadata(action, id, Set.of("minecraft:client"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"unexpected\": true}"));
	}
}
