package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageResourcesActionContractTest {
	@Test
	void bindCraftingInteractionsRejectInvalidAreasSlotsAndTimeouts() {
		bind(new SendPlayerCommandAction(), "{\"command\":\"sbp template list\"}");
		bind(new AssertCanonicalBackpackTemplateAction(), "{\"template\":\"devclientautomation:canonical_export\"}");
		invalid(new SendPlayerCommandAction(), "{\"command\":\"/sbp template list\"}");
		invalid(new AssertCanonicalBackpackTemplateAction(), "{\"template\":\"devclientautomation:canonical_export\",\"expectation\":\"other\"}");
		bind(new ReloadFixtureControllerAction(), "{}");
		bind(new AssertControllerSharedGroupAction(), "{}");
		invalid(new ReloadFixtureControllerAction(), "{\"offsetX\":-1}");
		invalid(new AssertControllerSharedGroupAction(), "{\"members\":0}");
		bind(new ClickPlayerInventorySlotAction(), "{\"area\":\"crafting\",\"slot\":3}");
		bind(new ClickPlayerInventorySlotAction(), "{\"area\":\"result\",\"slot\":0}");
		bind(new AwaitPlayerCraftingResultAction(), "{}");
		bind(new AssertLinkedItemContentsAction(), "{\"slot\":10,\"count\":7}");
		invalid(new ClickPlayerInventorySlotAction(), "{\"area\":\"crafting\",\"slot\":4}");
		invalid(new ClickPlayerInventorySlotAction(), "{\"area\":\"result\",\"slot\":1}");
		invalid(new ClickPlayerInventorySlotAction(), "{\"area\":\"other\"}");
		invalid(new AwaitPlayerCraftingResultAction(), "{\"timeoutTicks\":0}");
		invalid(new AssertLinkedItemContentsAction(), "{\"count\":-1}");
	}

	@Test
	void bindFluidLockTransfersRequireTypedExpectationsSimulationAndFluid() {
		bind(new TransferStorageResourceAction(), "{\"operation\":\"fill\",\"amount\":1000,\"expected\":0,\"simulate\":true,\"fluid\":\"lava\"}");
		bind(new CreateLockToolAction(), "{}");
		invalid(new TransferStorageResourceAction(), "{\"operation\":\"fill\",\"amount\":1000,\"expected\":-1}");
		invalid(new TransferStorageResourceAction(), "{\"operation\":\"fill\",\"amount\":1000,\"simulate\":\"true\"}");
		invalid(new TransferStorageResourceAction(), "{\"operation\":\"fill\",\"amount\":1000,\"fluid\":\"other\"}");
		invalid(new CreateLockToolAction(), "{\"toggle\":true}");
	}

	@Test
	void bindItemFluidTransfersRequireAnUnambiguousInventoryTarget() {
		TransferStorageResourceAction action = new TransferStorageResourceAction();
		assertEquals(0,
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{\"operation\":\"fill\",\"amount\":1000,\"inventorySlot\":0}"),
						TransferStorageResourceAction.Arguments.class).inventorySlot());
		invalid(action, "{\"operation\":\"fill\",\"amount\":1000,\"inventorySlot\":36}");
		invalid(action, "{\"operation\":\"fill\",\"amount\":1000,\"inventorySlot\":-1}");
		invalid(action, "{\"operation\":\"fill\",\"amount\":1000,\"inventorySlot\":0,\"ref\":\"boat\"}");
		invalid(action, "{\"operation\":\"receive\",\"amount\":1000,\"inventorySlot\":0}");
	}

	@Test
	void bindPhysicalLinkedBackpackFluidTransfersRejectInvalidArguments() {
		TransferLinkedBackpackFluidAction action = new TransferLinkedBackpackFluidAction();
		bind(action, "{\"slot\":8,\"operation\":\"fill\",\"amount\":250,\"expected\":250,\"canonicalAmount\":750}");
		invalid(action, "{\"slot\":36,\"operation\":\"fill\",\"amount\":250,\"canonicalAmount\":750}");
		invalid(action, "{\"operation\":\"extract\",\"amount\":250,\"canonicalAmount\":750}");
		invalid(action, "{\"operation\":\"fill\",\"amount\":0,\"canonicalAmount\":750}");
		invalid(action, "{\"operation\":\"fill\",\"amount\":250,\"canonicalAmount\":-1}");
		invalid(action, "{\"operation\":\"fill\",\"amount\":250,\"canonicalAmount\":750,\"unknown\":true}");
	}

	@Test
	void bindLinkedPumpFluidTransferRejectsInvalidArguments() {
		TransferLinkedPumpFluidAction action = new TransferLinkedPumpFluidAction();
		bind(action, "{\"hostSlot\":8,\"direction\":\"input\",\"held\":\"otherGroup\"}");
		bind(action, "{\"direction\":\"output\",\"held\":\"bucket\"}");
		invalid(action, "{\"hostSlot\":36,\"direction\":\"input\",\"held\":\"bucket\"}");
		invalid(action, "{\"direction\":\"fill\",\"held\":\"bucket\"}");
		invalid(action, "{\"direction\":\"input\",\"held\":\"sameGroup\"}");
		invalid(action, "{\"direction\":\"input\",\"held\":\"bucket\",\"unknown\":true}");
	}

	@Test
	void bindPlayerInventoryInteractionsRejectInvalidButtonsSlotsAndUnknownArguments() {
		bind(new OpenPlayerInventoryAction(), "{}");
		bind(new ClickPlayerInventorySlotAction(), "{\"slot\":9,\"button\":1}");
		bind(new SetPlayerGameModeAction(), "{\"mode\":\"survival\"}");
		invalid(new OpenPlayerInventoryAction(), "{\"unknown\":true}");
		invalid(new ClickPlayerInventorySlotAction(), "{\"slot\":36}");
		invalid(new ClickPlayerInventorySlotAction(), "{\"button\":2}");
		invalid(new SetPlayerGameModeAction(), "{\"mode\":\"other\"}");
	}

	@Test
	void bindReverseLinkActionsRejectInvalidReferencesAndFixtureTypes() {
		LinkHeldBackpackAction held = new LinkHeldBackpackAction();
		LinkTargetStorageBlockAction target = new LinkTargetStorageBlockAction();
		assertEquals("linker", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(held, "{\"linkerRef\":\"linker\"}"), LinkHeldBackpackAction.Arguments.class).linkerRef());
		assertEquals("linker", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(target, "{\"saveAs\":\"linker\"}"), LinkTargetStorageBlockAction.Arguments.class).saveAs());
		assertEquals("",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(held, "{}"), LinkHeldBackpackAction.Arguments.class).linkerRef());
		assertEquals("",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(target, "{}"), LinkTargetStorageBlockAction.Arguments.class).saveAs());
		bind(new SetFixtureBlockAction(), "{\"block\":\"sophisticatedstorage:chest\",\"offset\":[2,0,-2]}");
		bind(new AssertLinkedBackpackHostAction(), "{\"minimumSlots\":54}");
		bind(new AssertLinkedBackpackHostAction(), "{\"minimumSlots\":54,\"placed\":true,\"offsetZ\":1}");
		bind(new PlaceHeldBackpackAction(), "{\"offsetZ\":1}");
		bind(new UseHeldItemOnTargetAction(), "{\"offsetZ\":1}");
		invalid(held, "{\"linkerRef\":1}");
		invalid(target, "{\"saveAs\":true}");
		invalid(held, "{\"unknown\":true}");
		invalid(target, "{\"unknown\":true}");
		invalid(new AssertLinkedBackpackHostAction(), "{\"minimumSlots\":0}");
		invalid(new AssertLinkedBackpackHostAction(), "{\"placed\":\"true\"}");
		invalid(new PlaceHeldBackpackAction(), "{\"offsetZ\":-1}");
		invalid(new UseHeldItemOnTargetAction(), "{\"offsetZ\":\"1\"}");
		invalid(new SetFixtureBlockAction(), "{\"block\":\"sophisticatedstorage:chest\",\"offset\":[2,0,-2],\"doubleChest\":true}");
	}

	@Test
	void bindMenuUpgradeLayoutRejectsInvalidCountsAndUnknownArguments() {
		AssertMenuUpgradeLayoutAction action = new AssertMenuUpgradeLayoutAction();
		AssertMenuUpgradeLayoutAction.Arguments args = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"columns\":4,\"controls\":2}"), AssertMenuUpgradeLayoutAction.Arguments.class);
		assertEquals(4, args.columns());
		assertEquals(2, args.controls());
		assertFalse(args.contents());
		invalid(action, "{\"contents\":\"true\"}");
		invalid(action, "{\"columns\":-1,\"controls\":2}");
		invalid(action, "{\"columns\":4,\"controls\":\"2\"}");
		invalid(action, "{\"columns\":4,\"controls\":2,\"unknown\":true}");
	}

	@Test
	void bindStorageMenuClickRejectsInvalidAreaClickAndSlot() {
		ClickStorageMenuSlotAction action = new ClickStorageMenuSlotAction();
		bind(action, "{\"area\":\"upgrade\",\"slot\":0,\"click\":\"quickMove\"}");
		invalid(action, "{\"area\":\"other\",\"click\":\"pickup\"}");
		invalid(action, "{\"area\":\"upgrade\",\"click\":\"copy\"}");
		invalid(action, "{\"area\":\"player\",\"slot\":36,\"click\":\"pickup\"}");
	}

	@Test
	void bindCacheAndRootReplacementRejectAmbiguousInputs() {
		StorageResourceCacheAction cache = new StorageResourceCacheAction();
		ReplaceLinkedStorageRootAction root = new ReplaceLinkedStorageRootAction();
		bind(cache, "{\"operation\":\"capture\",\"ref\":\"resources\"}");
		bind(root, "{}");
		invalid(cache, "{\"operation\":\"copy\",\"ref\":\"resources\"}");
		invalid(cache, "{\"operation\":\"assert\",\"ref\":\"resources\",\"replaced\":\"true\"}");
		invalid(root, "{\"unknown\":true}");
		invalid(new UseHeldItemOnTargetAction(), "{\"unknown\":true}");
	}

	@Test
	void bindRejectsUnknownOperationsAndInvalidTransferAmounts() {
		TransferStorageResourceAction action = new TransferStorageResourceAction();
		TransferStorageResourceAction.Arguments args = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"operation\":\"fill\",\"amount\":1000}"), TransferStorageResourceAction.Arguments.class);
		assertEquals(1000, args.amount());
		assertEquals("", args.ref());
		invalid(action, "{\"operation\":\"copy\",\"amount\":1000}");
		invalid(action, "{\"operation\":\"fill\",\"amount\":0}");
		invalid(action, "{\"operation\":\"fill\",\"amount\":true}");
	}

	@Test
	void bindResourceAssertionUsesStrictPresenceAndAmounts() {
		AssertStorageResourcesAction action = new AssertStorageResourcesAction();
		AssertStorageResourcesAction.Arguments args = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"),
				AssertStorageResourcesAction.Arguments.class);
		assertTrue(args.present());
		assertEquals(0, args.fluid());
		invalid(action, "{\"present\":\"false\"}");
		invalid(action, "{\"energy\":-1}");
		invalid(action, "{\"unknown\":true}");
	}

	@Test
	void bindUpgradeRejectsInvalidArguments() {
		SetHeldBackpackUpgradeAction action = new SetHeldBackpackUpgradeAction();
		bind(action, "{\"item\":\"sophisticatedbackpacks:tank_upgrade\"}");
		invalid(action, "{\"slot\":-1,\"item\":\"minecraft:air\"}");
	}

	private static void bind(RegressionAction action, String json) {
		RegressionActionTestSupport.bind(action, json);
	}

	private static void invalid(RegressionAction action, String json) {
		assertThrows(IllegalArgumentException.class, () -> bind(action, json));
	}
}
