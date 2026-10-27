package net.p3pp3rf1y.devclientautomation.regression;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedStorageResourcesSuiteTest {
	private RegressionSuite suite(String name) throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/" + name + ".json")) {
			assertNotNull(stream);
			return RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
		}
	}

	@Test
	void parseRestartSuiteKeepsVerificationIndependentOfFixtureCreation() throws IOException {
		var tests = suite("linked-storage-restart").tests();
		assertEquals(2, tests.size());
		assertTrue(tests.get(0).prepareTestCell());
		assertFalse(tests.get(1).prepareTestCell());
		assertTrue(tests.get(1).steps().stream().noneMatch(step -> step.action().action().metadata().id().startsWith("fixture.")));
	}

	@Test
	void parseInceptionSuiteUsesNativeMenuInsertionForInstalledAndSameGroupChildren() throws IOException {
		var tests = suite("linked-storage-inception").tests();
		assertEquals(1, tests.size());
		List<String> actions = actions(tests.get(0));
		assertEquals(4, actions.stream().filter("menu.clickStorageSlot"::equals).count());
		assertTrue(actions.containsAll(Set.of("fixture.player.copyInventoryItem", "assert.linkedItemContents")));
		assertFalse(actions.contains("backpack.linkHeld"));
	}

	@Test
	void parseMobAndControllerSuitesKeepsPlayerInteractionsSeparateFromLifecycleFixtures() throws IOException {
		var mobs = suite("linked-storage-mob-catcher").tests();
		assertEquals(4, mobs.size());
		assertTrue(actions(mobs.get(0)).containsAll(Set.of("player.sneakUseFixtureEntity", "menu.clickCapturedMob", "assert.canonicalCapturedMobs")));
		for (int i = 1; i < 4; i++) {
			assertTrue(actions(mobs.get(i)).contains("player.useHeldItemOnTarget"));
			assertEquals(3, actions(mobs.get(i)).stream().filter("assert.canonicalCapturedMobs"::equals).count());
		}
		var controller = suite("linked-storage-controller-lifecycle").tests();
		assertEquals(1, controller.size());
		assertTrue(actions(controller.get(0)).containsAll(Set.of("player.useHeldItemOnTarget", "player.breakTargetBlock", "assert.controllerSharedGroup")));
		assertEquals(3, actions(controller.get(0)).stream().filter("fixture.controller.reload"::equals).count());
	}

	@Test
	void parseInteroperabilitySuiteUsesNativeCraftingAndCanonicalAssertionsInBothDirections() throws IOException {
		var interoperability = suite("linked-storage-interoperability").tests();
		assertEquals(2, interoperability.size());
		for (var test : interoperability) {
			assertTrue(actions(test).containsAll(Set.of("await.playerCraftingResult", "menu.clickPlayerInventorySlot", "assert.linkedItemContents")));
			assertFalse(actions(test).contains("backpack.linkHeld"));
			assertFalse(actions(test).contains("storage.linkBlock"));
		}
	}

	@Test
	void parseFluidLockSuiteUsesPlayerLinkingAndToolInteractionsForAllPlacedStorageKinds() throws IOException {
		var fluidLock = suite("linked-storage-fluid-lock").tests();
		assertEquals(3, fluidLock.size());
		for (var test : fluidLock) {
			assertTrue(actions(test).containsAll(Set.of("menu.clickPlayerInventorySlot", "fixture.storage.createLockTool", "player.useHeldItemOnTarget")));
			assertFalse(actions(test).contains("backpack.linkHeld"));
			assertFalse(actions(test).contains("storage.linkBlock"));
		}
	}

	@Test
	void parseIndexedMenuSuiteLinksThroughClientClicksRatherThanServiceShortcuts() throws IOException {
		var menu = suite("linked-storage-menu").tests();
		assertEquals(7, menu.size());
		for (var test : menu) {
			List<String> actions = actions(test);
			assertTrue(actions.contains("player.useHeldItemOnTarget"));
			assertTrue(actions.stream().noneMatch(Set.of("backpack.linkHeld", "storage.linkBlock", "player.selectHeldLinkedEndpoint")::contains));
			if (test.id().equals("double-chest-primary-backpack-secondary")) {
				assertEquals(2, actions.stream().filter("player.placeHeldBlock"::equals).count());
				assertEquals(3, actions.stream().filter("player.useHeldItemOnTarget"::equals).count());
				assertTrue(actions.containsAll(Set.of("assert.doubleChest", "backpack.placeHeld")));
				assertFalse(actions.contains("fixture.block.set"));
			} else {
				assertTrue(actions.contains("menu.clickPlayerInventorySlot"));
			}
		}
	}

	@Test
	void parseIndexedResourcesSuiteBindsRegisteredProductionInteractionsAndCapabilityProbes() throws IOException {
		var resources = suite("linked-storage-resources");
		assertEquals("linked-storage-resources", resources.suiteId());
		assertEquals(5, resources.tests().size());
		for (var test : resources.tests()) {
			assertTrue(actions(test).contains("assert.storageResources"));
			if (!test.id().equals("storage-main-has-no-resource-capabilities")) {
				assertTrue(actions(test).contains("capability.transferStorageResource"));
			}
		}
	}

	private List<String> actions(RegressionTest test) {
		return test.steps().stream().map(step -> step.action().action().metadata().id()).toList();
	}
}
