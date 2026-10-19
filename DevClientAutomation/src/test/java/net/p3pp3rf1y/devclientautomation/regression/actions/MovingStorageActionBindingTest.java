package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovingStorageActionBindingTest {
	@Test
	void spawnAndTargetArgumentsRequireTypedNamesWithoutNearbyFallbacks() {
		var spawn = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new SpawnMovingStorageAction(), "{\"kind\":\"boat\",\"saveAs\":\"primary\"}"),
				SpawnMovingStorageAction.Arguments.class);
		assertEquals("primary", spawn.saveAs());
		assertEquals(2, spawn.offsetX());
		assertEquals("primary", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new UseMovingStorageAction(), "{\"ref\":\"primary\"}"), UseMovingStorageAction.Arguments.class)
				.ref());
		assertEquals("secondary",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new RestoreMovingStorageAction(), "{\"ref\":\"primary\",\"saveAs\":\"secondary\"}"),
								RestoreMovingStorageAction.Arguments.class)
						.saveAs());
		assertEquals("primary", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new ProbeMovingStoragePickupAction(),
						"{\"phase\":\"spawn\",\"ref\":\"secondary\",\"primaryRef\":\"primary\"}"), ProbeMovingStoragePickupAction.Arguments.class)
				.primaryRef());
		for (Object[] invalid : new Object[][]{{new SpawnMovingStorageAction(), "{\"kind\":\"boat\"}"},
				{new SpawnMovingStorageAction(), "{\"kind\":\"boat\",\"saveAs\":\"Bad Name\"}"}, {new UseMovingStorageAction(), "{}"},
				{new UseMovingStorageAction(), "{\"ref\":\"primary\",\"offsetX\":4}"}, {new RestoreMovingStorageAction(), "{\"ref\":\"primary\"}"},
				{new ReloadMovingStorageAction(), "{\"offsetX\":2}"}, {new SetMovingStorageSlotAction(), "{\"item\":\"minecraft:diamond\",\"count\":1}"},
				{new SetMovingStorageUpgradeAction(), "{\"item\":\"minecraft:diamond\"}"}, {new ProbeMovingStoragePickupAction(), "{\"phase\":\"spawn\"}"},
				{new ProbeMovingStoragePickupAction(), "{\"phase\":\"spawn\",\"ref\":\"secondary\"}"}, {new ProbeForgedMovingStorageAction(), "{}"},
				{new AssertUnlinkedMovingStorageAction(), "{}"}, {new DestroyMovingStorageAction(), "{\"ref\":\"primary\",\"drops\":\"false\"}"},
				{new AssertLinkedMovingStorageAction(), "{\"ref\":\"primary\",\"primary\":\"true\"}"}, {new AwaitMenuAction(), "{\"kind\":\"moving\"}"},
				{new AssertMenuAction(), "{\"kind\":\"moving\",\"item\":\"minecraft:diamond\",\"count\":1}"}}) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind((RegressionAction) invalid[0], (String) invalid[1]));
		}
	}

	@Test
	void linkedMenuSyncActionsRequireValidTargetsAndPlaybackControls() {
		RegressionAction mountedSlot = new net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry().require("fixture.create.setMountedSlot");
		assertEquals("opening", RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new CaptureLinkedMenuRevisionAction(), "{\"kind\":\"mounted\",\"ref\":\"mount\",\"saveAs\":\"opening\"}"),
				CaptureLinkedMenuRevisionAction.Arguments.class).saveAs());
		assertEquals("",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new CaptureLinkedMenuRevisionAction(), "{\"kind\":\"storageBlock\",\"saveAs\":\"opening\"}"),
						CaptureLinkedMenuRevisionAction.Arguments.class).ref());
		assertEquals("storageBlock",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new AssertLinkedMenuDeltaAction(),
								"{\"kind\":\"storageBlock\",\"baseline\":\"opening\",\"item\":\"minecraft:emerald\",\"count\":9}"),
						AssertLinkedMenuDeltaAction.Arguments.class).kind());
		assertEquals(9,
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new AssertLinkedMenuDeltaAction(),
								"{\"kind\":\"moving\",\"ref\":\"boat\",\"baseline\":\"opening\",\"item\":\"minecraft:emerald\",\"count\":9}"),
						AssertLinkedMenuDeltaAction.Arguments.class).count());
		assertEquals(mountedSlot, mountedSlot
				.bind(com.google.gson.JsonParser.parseString("{\"ref\":\"mount\",\"item\":\"minecraft:emerald\",\"count\":9}").getAsJsonObject(), "test")
				.action());
		assertEquals("boat",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertLinkedMenuSettingsAction(), "{\"ref\":\"boat\",\"baseline\":\"opening\"}"),
								AssertLinkedMenuSettingsAction.Arguments.class)
						.ref());
		assertEquals("minecraft:music_disc_13",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new SetMovingJukeboxDiscAction(), "{\"ref\":\"boat\",\"item\":\"minecraft:music_disc_13\"}"),
						SetMovingJukeboxDiscAction.Arguments.class).item());
		assertEquals("stop",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new ControlJukeboxPlaybackAction(), "{\"ref\":\"boat\",\"control\":\"stop\"}"),
								ControlJukeboxPlaybackAction.Arguments.class)
						.control());
		assertEquals(true, RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new AssertLinkedJukeboxPlaybackAction(), "{\"ref\":\"boat\",\"baseline\":\"opening\",\"playing\":true}"),
				AssertLinkedJukeboxPlaybackAction.Arguments.class).playing());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new CaptureLinkedMenuRevisionAction(), "{\"kind\":\"block\",\"ref\":\"boat\",\"saveAs\":\"opening\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedMenuDeltaAction(),
				"{\"kind\":\"moving\",\"ref\":\"boat\",\"baseline\":\"opening\",\"item\":\"minecraft:emerald\",\"count\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(mountedSlot, "{\"ref\":\"mount\",\"count\":9}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedMenuSettingsAction(), "{\"ref\":\"boat\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetMovingJukeboxDiscAction(), "{\"ref\":\"boat\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ControlJukeboxPlaybackAction(), "{\"ref\":\"boat\",\"control\":\"pause\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedJukeboxPlaybackAction(),
				"{\"ref\":\"boat\",\"baseline\":\"opening\",\"playing\":\"true\"}"));
	}
	@Test
	void movingActionsBindDefaultsNestedIdentityCompatibilityAndPresentationContracts() {
		assertEquals(true, RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new DestroyMovingStorageAction(), "{\"ref\":\"primary\"}"),
				DestroyMovingStorageAction.Arguments.class).drops());
		assertEquals("primary",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new ReloadMovingStorageAction(), "{\"ref\":\"primary\"}"),
						ReloadMovingStorageAction.Arguments.class).ref());
		assertEquals("primary",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new SetMovingStorageUpgradeAction(), "{\"item\":\"minecraft:diamond\",\"ref\":\"primary\"}"),
						SetMovingStorageUpgradeAction.Arguments.class).ref());
		assertEquals("nested",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new CaptureNestedMovingStorageAction(), "{\"ref\":\"primary\",\"saveAs\":\"nested\"}"),
								CaptureNestedMovingStorageAction.Arguments.class)
						.saveAs());
		assertEquals("nested",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new AssertNestedMovingStorageIdentityAction(), "{\"ref\":\"nested\"}"),
						AssertNestedMovingStorageIdentityAction.Arguments.class).ref());
		RegressionActionTestSupport.bind(new AssertMovingStorageCompatibilityProfileAction(), "{}");
		assertEquals("primary", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new SetMovingStoragePresentationAction(), "{\"ref\":\"primary\"}"), String.class));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new DestroyMovingStorageAction(), "{\"ref\":\"primary\",\"drops\":\"false\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new CaptureNestedMovingStorageAction(), "{\"ref\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertNestedMovingStorageIdentityAction(), "{\"ref\":\"nested\",\"copy\":true}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingStorageCompatibilityProfileAction(), "{\"ref\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMovingStoragePresentationAction(), "{\"ref\":\"primary\",\"extra\":true}"));
	}

	@Test
	void movingPresentationActionsRejectInvalidArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMovingStoragePackedAction(), "{\"ref\":\"primary\",\"packed\":\"true\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ProbePackedPrimaryAction(), "{\"phase\":\"paused\",\"primaryRef\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetMovingStoragePresentationAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingStoragePresentationAction(), "{\"primaryRef\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertLinkedStorageBlockAction(), "{\"presentation\":\"true\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertMovingMenuPresentationAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingMenuPresentationAction(), "{\"ref\":\"boat\",\"role\":\"main\"}"));
		assertEquals("boat",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertMovingMenuPresentationAction(), "{\"ref\":\"boat\",\"role\":\"primary\"}"),
								AssertMovingMenuPresentationAction.Arguments.class)
						.ref());
	}
	@Test
	void movingActionsBindExactReferences() {
		SpawnMovingStorageAction.Arguments spawn = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new SpawnMovingStorageAction(), "{\"kind\":\"boat\",\"saveAs\":\"primary\"}"),
				SpawnMovingStorageAction.Arguments.class);
		assertEquals("primary", spawn.saveAs());
		assertEquals(2, spawn.offsetX());
		assertEquals("primary", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new UseMovingStorageAction(), "{\"ref\":\"primary\"}"), UseMovingStorageAction.Arguments.class)
				.ref());
		assertEquals("primary",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new SetMovingStorageSlotAction(), "{\"item\":\"minecraft:diamond\",\"count\":1,\"ref\":\"primary\"}"),
						SetMovingStorageSlotAction.Arguments.class).ref());
		assertEquals("primary",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new SetMovingStorageUpgradeAction(), "{\"item\":\"minecraft:diamond\",\"ref\":\"primary\"}"),
						SetMovingStorageUpgradeAction.Arguments.class).ref());
		assertEquals(false,
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new SetMovingStoragePackedAction(), "{\"ref\":\"primary\",\"packed\":false}"),
								SetMovingStoragePackedAction.Arguments.class)
						.packed());
		assertEquals("primary",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new AssertLinkedMovingStorageAction(), "{\"ref\":\"primary\"}"),
						AssertLinkedMovingStorageAction.Arguments.class).ref());
		assertEquals("primary",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new AssertUnlinkedMovingStorageAction(), "{\"ref\":\"primary\"}"),
						AssertUnlinkedMovingStorageAction.Arguments.class).ref());
		assertEquals(true, RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new DestroyMovingStorageAction(), "{\"ref\":\"primary\"}"),
				DestroyMovingStorageAction.Arguments.class).drops());
		assertEquals(false,
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new DestroyMovingStorageAction(), "{\"ref\":\"primary\",\"drops\":false}"),
								DestroyMovingStorageAction.Arguments.class)
						.drops());
		RestoreMovingStorageAction.Arguments restore = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new RestoreMovingStorageAction(), "{\"ref\":\"primary\",\"saveAs\":\"restored\"}"),
				RestoreMovingStorageAction.Arguments.class);
		assertEquals("restored", restore.saveAs());
		assertEquals(2, restore.offsetX());
		assertEquals("primary",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new ReloadMovingStorageAction(), "{\"ref\":\"primary\"}"),
						ReloadMovingStorageAction.Arguments.class).ref());
		assertEquals("primary", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new SetMovingStoragePresentationAction(), "{\"ref\":\"primary\"}"), String.class));
		assertEquals("secondary",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertMovingStoragePresentationAction(),
								"{\"primaryRef\":\"primary\",\"secondaryRef\":\"secondary\"}"), AssertMovingStoragePresentationAction.Arguments.class)
						.secondaryRef());
		assertEquals("secondary",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertMovingMenuPresentationAction(), "{\"ref\":\"secondary\",\"role\":\"secondary\"}"),
								AssertMovingMenuPresentationAction.Arguments.class)
						.role());
		assertEquals("primary",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new ProbeForgedMovingStorageAction(), "{\"ref\":\"primary\"}"),
						ProbeForgedMovingStorageAction.Arguments.class).ref());
		assertEquals("primary", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new ProbeMovingStoragePickupAction(),
						"{\"phase\":\"spawn\",\"ref\":\"secondary\",\"primaryRef\":\"primary\"}"), ProbeMovingStoragePickupAction.Arguments.class)
				.primaryRef());
		assertEquals("resumed", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new ProbePackedPrimaryAction(),
						"{\"phase\":\"resumed\",\"primaryRef\":\"primary\",\"secondaryRef\":\"secondary\"}"), ProbePackedPrimaryAction.Arguments.class)
				.phase());
		assertEquals(true,
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertLinkedStorageBlockAction(), "{\"count\":7,\"presentation\":true}"),
								AssertLinkedStorageBlockAction.Arguments.class)
						.presentation());
		assertEquals("baseline",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new CaptureLinkedMenuRevisionAction(), "{\"kind\":\"backpackItem\",\"saveAs\":\"baseline\"}"),
						CaptureLinkedMenuRevisionAction.Arguments.class).saveAs());
		assertEquals(0,
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new AssertLinkedMenuDeltaAction(),
								"{\"kind\":\"mounted\",\"ref\":\"mounted\",\"baseline\":\"baseline\",\"item\":\"minecraft:diamond\",\"count\":1}"),
						AssertLinkedMenuDeltaAction.Arguments.class).slot());
		assertEquals("baseline",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertLinkedMenuSettingsAction(), "{\"ref\":\"primary\",\"baseline\":\"baseline\"}"),
								AssertLinkedMenuSettingsAction.Arguments.class)
						.baseline());
		assertEquals("minecraft:music_disc_13",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new SetMovingJukeboxDiscAction(), "{\"ref\":\"primary\",\"item\":\"minecraft:music_disc_13\"}"),
						SetMovingJukeboxDiscAction.Arguments.class).item());
		assertEquals("stop",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new ControlJukeboxPlaybackAction(), "{\"ref\":\"primary\",\"control\":\"stop\"}"),
								ControlJukeboxPlaybackAction.Arguments.class)
						.control());
		assertEquals(true,
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertLinkedJukeboxPlaybackAction(),
								"{\"ref\":\"primary\",\"baseline\":\"baseline\",\"playing\":true}"), AssertLinkedJukeboxPlaybackAction.Arguments.class)
						.playing());
		assertEquals("nested",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new CaptureNestedMovingStorageAction(), "{\"ref\":\"primary\",\"saveAs\":\"nested\"}"),
								CaptureNestedMovingStorageAction.Arguments.class)
						.saveAs());
		assertEquals("nested",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new AssertNestedMovingStorageIdentityAction(), "{\"ref\":\"nested\"}"),
						AssertNestedMovingStorageIdentityAction.Arguments.class).ref());
		RegressionActionTestSupport.bind(new AssertMovingStorageCompatibilityProfileAction(), "{}");
		assertEquals("double", RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new AssertStorageTierUpgradeRecipeAction(),
				"{\"form\":\"double\",\"recipe\":\"shaped\",\"linkage\":\"primary\"}"), AssertStorageTierUpgradeRecipeAction.Arguments.class).form());
		assertEquals("minecart",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new AssertMovingStorageTierUpgradeRecipeAction(),
								"{\"carrier\":\"minecart\",\"recipe\":\"shapeless\",\"linkage\":\"secondary\"}"),
						AssertMovingStorageTierUpgradeRecipeAction.Arguments.class).carrier());
	}

	@Test
	void movingActionsRejectMissingOrInvalidReferences() {
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseMovingStorageAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new UseMovingStorageAction(), "{\"ref\":\"primary\",\"offsetX\":4}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMovingStorageSlotAction(), "{\"item\":\"minecraft:diamond\",\"count\":1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMovingStorageUpgradeAction(), "{\"item\":\"minecraft:diamond\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMovingStoragePackedAction(), "{\"ref\":\"primary\",\"packed\":\"true\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertLinkedMovingStorageAction(), "{\"ref\":\"primary\",\"primary\":\"true\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertUnlinkedMovingStorageAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new HoldRecoveredMovingStorageInnerAction(), "{\"ref\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new DestroyMovingStorageAction(), "{\"ref\":\"primary\",\"drops\":\"false\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new RestoreMovingStorageAction(), "{\"ref\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new RestoreMovingStorageAction(), "{\"ref\":\"primary\",\"saveAs\":\"restored\",\"offsetX\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ReloadMovingStorageAction(), "{\"offsetX\":2}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetMovingStoragePresentationAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingStoragePresentationAction(), "{\"primaryRef\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingMenuPresentationAction(), "{\"ref\":\"primary\",\"role\":\"main\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ProbeForgedMovingStorageAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ProbeMovingStoragePickupAction(), "{\"phase\":\"spawn\",\"ref\":\"secondary\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ProbePackedPrimaryAction(),
				"{\"phase\":\"paused\",\"primaryRef\":\"primary\",\"secondaryRef\":\"secondary\",\"extra\":true}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertLinkedStorageBlockAction(), "{\"presentation\":\"true\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new CaptureLinkedMenuRevisionAction(), "{\"kind\":\"moving\",\"saveAs\":\"baseline\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new CaptureLinkedMenuRevisionAction(),
				"{\"kind\":\"backpackItem\",\"saveAs\":\"baseline\",\"extra\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedMenuDeltaAction(),
				"{\"kind\":\"moving\",\"ref\":\"primary\",\"baseline\":\"baseline\",\"item\":\"minecraft:diamond\",\"count\":0}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedMenuSettingsAction(), "{\"ref\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetMovingJukeboxDiscAction(),
				"{\"ref\":\"primary\",\"item\":\"minecraft:music_disc_13\",\"extra\":true}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ControlJukeboxPlaybackAction(), "{\"ref\":\"primary\",\"control\":\"pause\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedJukeboxPlaybackAction(),
				"{\"ref\":\"primary\",\"baseline\":\"baseline\",\"playing\":\"true\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new CaptureNestedMovingStorageAction(), "{\"ref\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertNestedMovingStorageIdentityAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingStorageCompatibilityProfileAction(), "{\"ref\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertStorageTierUpgradeRecipeAction(),
				"{\"form\":\"single\",\"recipe\":\"shaped\",\"linkage\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertMovingStorageTierUpgradeRecipeAction(),
				"{\"carrier\":\"boat\",\"recipe\":\"shaped\",\"linkage\":\"other\"}"));
	}
}
