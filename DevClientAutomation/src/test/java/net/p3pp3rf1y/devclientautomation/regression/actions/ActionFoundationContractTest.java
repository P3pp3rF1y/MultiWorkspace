package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionFoundationContractTest {
	@Test
	void blockPlacementBindsRelativeTargetsAndRejectsMalformedCoordinates() {
		var action = new PlaceHeldBlockAction();
		PlaceHeldBlockAction.Arguments defaultTarget = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"),
				PlaceHeldBlockAction.Arguments.class);
		PlaceHeldBlockAction.Arguments adjacentTarget = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{\"offset\":[0,0,-1]}"),
				PlaceHeldBlockAction.Arguments.class);
		assertEquals(0, defaultTarget.z());
		assertEquals(-1, adjacentTarget.z());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"offset\":[0,0]}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"offset\":[0,0,0.5]}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"offset\":[0,0,\"-1\"]}"));
		RegressionActionTestSupport.bind(new AssertDoubleChestAction(), "{}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertDoubleChestAction(), "{\"forceJoin\":true}"));
	}
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
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:stone\", \"count\": 1.5}"));
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
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"slot\": 0.5, \"item\": \"minecraft:diamond\", \"count\": 1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": \"one\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:diamond\", \"count\": 1.5}"));
	}

	@Test
	void playerInventorySlotRequiresValidFixtureValues() {
		SetPlayerInventorySlotAction action = new SetPlayerInventorySlotAction();
		SetPlayerInventorySlotAction.Arguments arguments = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(action, "{\"slot\": 9, \"item\": \"minecraft:stick\", \"count\": 1}"),
				SetPlayerInventorySlotAction.Arguments.class);
		assertEquals(9, arguments.slot());
		assertEquals("2", action.metadata().revision());
		assertEquals(false,
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{\"slot\": 0, \"item\": \"minecraft:stick\", \"count\": 1}"),
						SetPlayerInventorySlotAction.Arguments.class).clearMatching());
		assertEquals(true,
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(action, "{\"slot\": 0, \"item\": \"minecraft:stick\", \"count\": 1, \"clearMatching\": true}"),
						SetPlayerInventorySlotAction.Arguments.class).clearMatching());
		assertEquals(0,
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(action, "{\"slot\": 0, \"item\": \"minecraft:stick\", \"count\": 1, \"clearMatching\": true}"),
						SetPlayerInventorySlotAction.Arguments.class).slot());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"item\": \"minecraft:stick\", \"count\": 1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"slot\": -1, \"item\": \"minecraft:stick\", \"count\": 1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"slot\": 9, \"item\": 1, \"count\": 1}"));
		for (String json : java.util.List.of("{\"slot\":9,\"item\":\"minecraft:stick\"}", "{\"slot\":9,\"item\":\"minecraft:stick\",\"count\":0}",
				"{\"slot\":9,\"item\":\"minecraft:stick\",\"count\":true}", "{\"slot\":9.5,\"item\":\"minecraft:stick\",\"count\":1}",
				"{\"slot\":9,\"item\":\"minecraft:stick\",\"count\":1.5}", "{\"slot\":9,\"item\":\"minecraft:stick\",\"count\":1,\"clearMatching\":\"true\"}",
				"{\"slot\":9,\"item\":\"minecraft:stick\",\"count\":1,\"extra\":true}")) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, json));
		}
	}

	@Test
	void storageBlockLinkBindsOptionalSavedLinkerName() {
		LinkTargetStorageBlockAction action = new LinkTargetStorageBlockAction();
		assertEquals("",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), LinkTargetStorageBlockAction.Arguments.class).saveAs());
		assertEquals("linker", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"saveAs\":\"linker\"}"), LinkTargetStorageBlockAction.Arguments.class).saveAs());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"saveAs\":1}"));
	}

	@Test
	void menuCloseWaitBindsTimeoutAndRejectsInvalidValues() {
		AwaitMenuClosedAction action = new AwaitMenuClosedAction();
		assertEquals(100,
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), AwaitMenuClosedAction.Arguments.class).timeoutTicks());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"timeoutTicks\": 0}"));
		for (String json : java.util.List.of("{\"timeoutTicks\":\"100\"}", "{\"timeoutTicks\":1.5}", "{\"timeoutTicks\":1,\"extra\":true}")) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, json));
		}
	}

	@Test
	void clickAndQuickMoveUseStableSlotSemantics() {
		ClickMenuSlotAction click = new ClickMenuSlotAction();
		assertEquals(0, RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(click, "{}"), ClickMenuSlotAction.Arguments.class).slot());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(click, "{\"slot\": false}"));
		for (String json : java.util.List.of("{\"slot\":-1}", "{\"slot\":0.5}", "{\"slot\":0,\"extra\":true}")) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(click, json));
		}

		QuickMovePlayerInventorySlotAction quickMove = new QuickMovePlayerInventorySlotAction();
		assertEquals(9,
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(quickMove, "{\"inventorySlot\": 9}"), QuickMovePlayerInventorySlotAction.Arguments.class)
						.inventorySlot());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(quickMove, "{\"inventorySlot\": 8}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(quickMove, "{\"inventorySlot\": \"9\"}"));
		for (String json : java.util.List.of("{\"inventorySlot\":9.5}", "{\"inventorySlot\":36}", "{\"inventorySlot\":9,\"extra\":true}")) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(quickMove, json));
		}
	}

	private static void assertNoArguments(RegressionAction action, String id) {
		RegressionActionTestSupport.bind(action, "{}");
		RegressionActionTestSupport.assertMetadata(action, id, Set.of("minecraft:client"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"unexpected\": true}"));
	}
}
