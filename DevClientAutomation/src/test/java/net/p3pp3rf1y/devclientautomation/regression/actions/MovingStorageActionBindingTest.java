package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovingStorageActionBindingTest {
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
	void createMountBindsImmutableHandleNamesAndRejectsUnboundTargets() {
		assertEquals("mounted",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new MountLinkedStorageAction(), "{\"saveAs\":\"mounted\"}"),
						MountLinkedStorageAction.Arguments.class).saveAs());
		assertEquals("mounted",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertMountedLinkedStorageAction(), "{\"ref\":\"mounted\",\"count\":7}"),
								AssertMountedLinkedStorageAction.Arguments.class)
						.ref());
		assertEquals(false,
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertMountedLinkedStorageAction(),
								"{\"ref\":\"mounted\",\"count\":7,\"checkPresentation\":false}"), AssertMountedLinkedStorageAction.Arguments.class)
						.checkPresentation());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new MountLinkedStorageAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new MountLinkedStorageAction(), "{\"saveAs\":\"mounted\",\"offsetX\":2}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertMountedLinkedStorageAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMountedLinkedStorageAction(), "{\"ref\":\"mounted\",\"checkPresentation\":\"false\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMovingStoragePackedAction(), "{\"ref\":\"primary\",\"packed\":\"true\"}"));
	}

	@Test
	void spawnAndTargetArgumentsRequireTypedNamesWithoutNearbyFallbacks() {
		SpawnMovingStorageAction.Arguments spawn = RegressionActionTestSupport.arguments(
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
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SpawnMovingStorageAction(), "{\"kind\":\"boat\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SpawnMovingStorageAction(), "{\"kind\":\"boat\",\"saveAs\":\"Bad Name\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new UseMovingStorageAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new UseMovingStorageAction(), "{\"ref\":\"primary\",\"offsetX\":4}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new RestoreMovingStorageAction(), "{\"ref\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ReloadMovingStorageAction(), "{\"offsetX\":2}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMovingStorageSlotAction(), "{\"item\":\"minecraft:diamond\",\"count\":1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMovingStorageUpgradeAction(), "{\"item\":\"minecraft:diamond\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ProbeMovingStoragePickupAction(), "{\"phase\":\"spawn\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ProbeMovingStoragePickupAction(), "{\"phase\":\"spawn\",\"ref\":\"secondary\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new DestroyMovingStorageAction(), "{\"ref\":\"primary\",\"drops\":\"false\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AwaitMenuAction(), "{\"kind\":\"moving\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMenuAction(), "{\"kind\":\"moving\",\"item\":\"minecraft:diamond\",\"count\":1}"));
	}

	@Test
	void presentationAndProbeActionsRequireTypedReferences() {
		assertEquals("boat",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertMovingMenuPresentationAction(), "{\"ref\":\"boat\",\"role\":\"primary\"}"),
								AssertMovingMenuPresentationAction.Arguments.class)
						.ref());
		assertEquals("primary", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new ProbeMovingStoragePickupAction(),
						"{\"phase\":\"spawn\",\"ref\":\"secondary\",\"primaryRef\":\"primary\"}"), ProbeMovingStoragePickupAction.Arguments.class)
				.primaryRef());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetMovingStoragePresentationAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingStoragePresentationAction(), "{\"primaryRef\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertLinkedStorageBlockAction(), "{\"presentation\":\"true\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertMovingMenuPresentationAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingMenuPresentationAction(), "{\"ref\":\"boat\",\"role\":\"main\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ProbePackedPrimaryAction(), "{\"phase\":\"paused\",\"primaryRef\":\"primary\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ProbeForgedMovingStorageAction(), "{}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertUnlinkedMovingStorageAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertLinkedMovingStorageAction(), "{\"ref\":\"primary\",\"primary\":\"true\"}"));
	}

	@Test
	void nestedIdentityAndCompatibilityActionsBindStrictly() {
		CaptureNestedMovingStorageAction.Arguments capture = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new CaptureNestedMovingStorageAction(), "{\"ref\":\"boat\",\"saveAs\":\"nested-copy\"}"),
				CaptureNestedMovingStorageAction.Arguments.class);
		assertEquals("boat", capture.ref());
		assertEquals("nested-copy", capture.saveAs());
		assertEquals("nested-copy",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(new AssertNestedMovingStorageIdentityAction(), "{\"ref\":\"nested-copy\"}"),
								AssertNestedMovingStorageIdentityAction.Arguments.class)
						.ref());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new CaptureNestedMovingStorageAction(), "{\"ref\":\"boat\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new CaptureNestedMovingStorageAction(), "{\"ref\":\"boat\",\"saveAs\":\"Nested Copy\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertNestedMovingStorageIdentityAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertNestedMovingStorageIdentityAction(), "{\"ref\":\"nested-copy\",\"copy\":true}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingStorageCompatibilityProfileAction(), "{\"ref\":\"boat\"}"));
	}

	@Test
	void linkedMenuSyncActionsRequireValidTargetsAndPlaybackControls() {
		assertEquals("opening", RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new CaptureLinkedMenuRevisionAction(), "{\"kind\":\"mounted\",\"ref\":\"mount\",\"saveAs\":\"opening\"}"),
				CaptureLinkedMenuRevisionAction.Arguments.class).saveAs());
		assertEquals("",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new CaptureLinkedMenuRevisionAction(), "{\"kind\":\"storageBlock\",\"saveAs\":\"opening\"}"),
						CaptureLinkedMenuRevisionAction.Arguments.class).ref());
		assertEquals(MenuKind.STORAGE_BLOCK,
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new AssertLinkedMenuDeltaAction(),
								"{\"kind\":\"storageBlock\",\"baseline\":\"opening\",\"item\":\"minecraft:emerald\",\"count\":9}"),
						AssertLinkedMenuDeltaAction.Arguments.class).kind());
		assertEquals(9,
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new AssertLinkedMenuDeltaAction(),
								"{\"kind\":\"moving\",\"ref\":\"boat\",\"baseline\":\"opening\",\"item\":\"minecraft:emerald\",\"count\":9}"),
						AssertLinkedMenuDeltaAction.Arguments.class).count());
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
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMountedStorageSlotAction(), "{\"ref\":\"mount\",\"count\":9}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedMenuSettingsAction(), "{\"ref\":\"boat\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetMovingJukeboxDiscAction(), "{\"ref\":\"boat\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ControlJukeboxPlaybackAction(), "{\"ref\":\"boat\",\"control\":\"pause\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertLinkedJukeboxPlaybackAction(),
				"{\"ref\":\"boat\",\"baseline\":\"opening\",\"playing\":\"true\"}"));
	}
}
