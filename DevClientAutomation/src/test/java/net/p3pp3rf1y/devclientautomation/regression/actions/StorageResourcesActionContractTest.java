package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageResourcesActionContractTest {
	@Test
	void bindItemFluidTransfersRequireAnUnambiguousInventoryTarget() {
		TransferStorageResourceAction action = new TransferStorageResourceAction();
		TransferStorageResourceAction.Arguments args = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(action, "{\"operation\":\"fill\",\"amount\":1000,\"inventorySlot\":0}"),
				TransferStorageResourceAction.Arguments.class);
		assertEquals(0, args.inventorySlot());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"operation\":\"fill\",\"amount\":1000,\"inventorySlot\":36}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"operation\":\"fill\",\"amount\":1000,\"inventorySlot\":-1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"operation\":\"fill\",\"amount\":1000,\"inventorySlot\":0,\"ref\":\"boat\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(action, "{\"operation\":\"receive\",\"amount\":1000,\"inventorySlot\":0}"));
	}

	@Test
	void bindReverseLinkActionsRejectInvalidReferencesAndFixtureTypes() {
		LinkHeldBackpackAction heldBackpack = new LinkHeldBackpackAction();
		LinkTargetStorageBlockAction targetStorage = new LinkTargetStorageBlockAction();
		LinkHeldBackpackAction.Arguments heldArguments = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(heldBackpack, "{\"linkerRef\":\"linker\"}"), LinkHeldBackpackAction.Arguments.class);
		LinkTargetStorageBlockAction.Arguments targetArguments = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(targetStorage, "{\"saveAs\":\"linker\"}"), LinkTargetStorageBlockAction.Arguments.class);
		assertEquals("linker", heldArguments.linkerRef());
		assertEquals("linker", targetArguments.saveAs());
		assertEquals("", RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(heldBackpack, "{}"), LinkHeldBackpackAction.Arguments.class)
				.linkerRef());
		assertEquals("", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(targetStorage, "{}"), LinkTargetStorageBlockAction.Arguments.class).saveAs());
		RegressionActionTestSupport.bind(new SetFixtureBlockAction(), "{\"block\":\"sophisticatedstorage:chest\",\"offset\":[2,0,-2]}");
		RegressionActionTestSupport.bind(new AssertLinkedBackpackHostAction(), "{\"minimumSlots\":54}");
		RegressionActionTestSupport.bind(new AssertLinkedBackpackHostAction(), "{\"minimumSlots\":54,\"placed\":true,\"offsetZ\":1}");
		RegressionActionTestSupport.bind(new PlaceHeldBackpackAction(), "{\"offsetZ\":1}");
		RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":1}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(heldBackpack, "{\"linkerRef\":1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(targetStorage, "{\"saveAs\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(heldBackpack, "{\"unknown\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(targetStorage, "{\"unknown\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedBackpackHostAction(), "{\"minimumSlots\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedBackpackHostAction(), "{\"placed\":\"true\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new PlaceHeldBackpackAction(), "{\"offsetZ\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":\"1\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetFixtureBlockAction(),
				"{\"block\":\"sophisticatedstorage:chest\",\"offset\":[2,0,-2],\"doubleChest\":true}"));
	}
	@Test
	void bindCraftingInteractionsRejectInvalidAreasSlotsAndTimeouts() {
		RegressionActionTestSupport.bind(new SendPlayerCommandAction(), "{\"command\":\"sbp template list\"}");
		RegressionActionTestSupport.bind(new AssertCanonicalBackpackTemplateAction(), "{\"template\":\"devclientautomation:canonical_export\"}");
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SendPlayerCommandAction(), "{\"command\":\"/sbp template list\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertCanonicalBackpackTemplateAction(),
				"{\"template\":\"devclientautomation:canonical_export\",\"expectation\":\"other\"}"));
		RegressionActionTestSupport.bind(new ReloadFixtureControllerAction(), "{}");
		RegressionActionTestSupport.bind(new AssertControllerSharedGroupAction(), "{}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ReloadFixtureControllerAction(), "{\"offsetX\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertControllerSharedGroupAction(), "{\"members\":0}"));
		RegressionActionTestSupport.bind(new ClickPlayerInventorySlotAction(), "{\"area\":\"crafting\",\"slot\":3}");
		RegressionActionTestSupport.bind(new ClickPlayerInventorySlotAction(), "{\"area\":\"result\",\"slot\":0}");
		RegressionActionTestSupport.bind(new AwaitPlayerCraftingResultAction(), "{}");
		RegressionActionTestSupport.bind(new AssertLinkedItemContentsAction(), "{\"slot\":10,\"count\":7}");

		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ClickPlayerInventorySlotAction(), "{\"area\":\"crafting\",\"slot\":4}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ClickPlayerInventorySlotAction(), "{\"area\":\"result\",\"slot\":1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ClickPlayerInventorySlotAction(), "{\"area\":\"other\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AwaitPlayerCraftingResultAction(), "{\"timeoutTicks\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedItemContentsAction(), "{\"count\":-1}"));
	}

	@Test
	void bindFluidLockTransfersRequireTypedExpectationsSimulationAndFluid() {
		RegressionActionTestSupport.bind(new TransferStorageResourceAction(),
				"{\"operation\":\"fill\",\"amount\":1000,\"expected\":0,\"simulate\":true,\"fluid\":\"lava\"}");
		RegressionActionTestSupport.bind(new CreateLockToolAction(), "{}");
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new TransferStorageResourceAction(), "{\"operation\":\"fill\",\"amount\":1000,\"expected\":-1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new TransferStorageResourceAction(), "{\"operation\":\"fill\",\"amount\":1000,\"simulate\":\"true\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new TransferStorageResourceAction(), "{\"operation\":\"fill\",\"amount\":1000,\"fluid\":\"other\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new CreateLockToolAction(), "{\"toggle\":true}"));
	}

	@Test
	void bindPlayerInventoryInteractionsRejectInvalidButtonsSlotsAndUnknownArguments() {
		RegressionActionTestSupport.bind(new OpenPlayerInventoryAction(), "{}");
		RegressionActionTestSupport.bind(new ClickPlayerInventorySlotAction(), "{\"slot\":9,\"button\":1}");
		RegressionActionTestSupport.bind(new SetPlayerGameModeAction(), "{\"mode\":\"survival\"}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new OpenPlayerInventoryAction(), "{\"unknown\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ClickPlayerInventorySlotAction(), "{\"slot\":36}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ClickPlayerInventorySlotAction(), "{\"button\":2}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetPlayerGameModeAction(), "{\"mode\":\"other\"}"));
	}

	@Test
	void bindSelectedPlacementAndHostActionsRejectInvalidArguments() {
		RegressionActionTestSupport.bind(new SetFixtureBlockAction(), "{\"block\":\"sophisticatedstorage:chest\",\"offset\":[2,0,-2]}");
		RegressionActionTestSupport.bind(new AssertLinkedBackpackHostAction(), "{\"minimumSlots\":54}");
		RegressionActionTestSupport.bind(new AssertLinkedBackpackHostAction(), "{\"minimumSlots\":54,\"placed\":true,\"offsetZ\":1}");
		RegressionActionTestSupport.bind(new PlaceHeldBackpackAction(), "{\"offsetZ\":1}");
		RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":1}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedBackpackHostAction(), "{\"minimumSlots\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedBackpackHostAction(), "{\"placed\":\"true\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new PlaceHeldBackpackAction(), "{\"offsetZ\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":\"1\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetFixtureBlockAction(),
				"{\"block\":\"sophisticatedstorage:chest\",\"offset\":[2,0,-2],\"doubleChest\":true}"));
	}

	@Test
	void bindMenuUpgradeLayoutRejectsInvalidCountsAndUnknownArguments() {
		AssertMenuUpgradeLayoutAction action = new AssertMenuUpgradeLayoutAction();
		AssertMenuUpgradeLayoutAction.Arguments args = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"columns\":4,\"controls\":2}"), AssertMenuUpgradeLayoutAction.Arguments.class);
		assertEquals(4, args.columns());
		assertEquals(2, args.controls());
		assertFalse(args.contents());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"contents\":\"true\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"columns\":-1,\"controls\":2}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"columns\":4,\"controls\":\"2\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"columns\":4,\"controls\":2,\"unknown\":true}"));
	}

	@Test
	void bindStorageMenuClickRejectsInvalidAreaClickAndSlot() {
		ClickStorageMenuSlotAction action = new ClickStorageMenuSlotAction();
		RegressionActionTestSupport.bind(action, "{\"area\":\"upgrade\",\"slot\":0,\"click\":\"quickMove\"}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"area\":\"other\",\"click\":\"pickup\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"area\":\"upgrade\",\"click\":\"copy\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"area\":\"player\",\"slot\":36,\"click\":\"pickup\"}"));
	}

	@Test
	void bindCacheAndRootReplacementRejectAmbiguousInputs() {
		StorageResourceCacheAction cache = new StorageResourceCacheAction();
		ReplaceLinkedStorageRootAction root = new ReplaceLinkedStorageRootAction();
		RegressionActionTestSupport.bind(cache, "{\"operation\":\"capture\",\"ref\":\"resources\"}");
		RegressionActionTestSupport.bind(root, "{}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(cache, "{\"operation\":\"copy\",\"ref\":\"resources\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(cache, "{\"operation\":\"assert\",\"ref\":\"resources\",\"replaced\":\"true\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(root, "{\"unknown\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseHeldItemOnTargetAction(), "{\"unknown\":true}"));
	}

	@Test
	void bindRejectsUnknownOperationsAndInvalidTransferAmounts() {
		TransferStorageResourceAction action = new TransferStorageResourceAction();
		TransferStorageResourceAction.Arguments args = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"operation\":\"fill\",\"amount\":1000}"), TransferStorageResourceAction.Arguments.class);
		assertEquals(1000, args.amount());
		assertEquals("", args.ref());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"operation\":\"copy\",\"amount\":1000}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"operation\":\"fill\",\"amount\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"operation\":\"fill\",\"amount\":true}"));
	}

	@Test
	void bindResourceAssertionUsesStrictPresenceAndAmounts() {
		AssertStorageResourcesAction action = new AssertStorageResourcesAction();
		AssertStorageResourcesAction.Arguments args = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"),
				AssertStorageResourcesAction.Arguments.class);
		assertTrue(args.present());
		assertEquals(0, args.fluid());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"present\":\"false\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"energy\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"unknown\":true}"));
	}

	@Test
	void bindUpgradeRejectsInvalidArguments() {
		SetHeldBackpackUpgradeAction upgrade = new SetHeldBackpackUpgradeAction();
		RegressionActionTestSupport.bind(upgrade, "{\"item\":\"sophisticatedbackpacks:tank_upgrade\"}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(upgrade, "{\"slot\":-1,\"item\":\"minecraft:air\"}"));
	}
}
