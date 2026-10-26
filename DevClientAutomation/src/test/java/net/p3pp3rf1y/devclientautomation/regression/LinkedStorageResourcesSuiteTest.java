package net.p3pp3rf1y.devclientautomation.regression;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedStorageResourcesSuiteTest {
	@Test
	void parseRestartSuiteKeepsVerificationIndependentOfFixtureCreation() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-restart.json")) {
			assertNotNull(stream);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			assertEquals(2, suite.tests().size());
			assertTrue(suite.tests().getFirst().prepareTestCell());
			assertFalse(suite.tests().getLast().prepareTestCell());
			assertTrue(suite.tests().getLast().steps().stream().noneMatch(step -> step.action().action().metadata().id().startsWith("fixture.")));
		}
	}

	@Test
	void parseInceptionSuiteUsesNativeMenuInsertionForInstalledAndSameGroupChildren() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-inception.json")) {
			assertNotNull(stream);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			List<String> actions = suite.tests().getFirst().steps().stream().map(step -> step.action().action().metadata().id()).toList();
			assertEquals(1, suite.tests().size());
			assertEquals(4, actions.stream().filter("menu.clickStorageSlot"::equals).count());
			assertTrue(actions.contains("fixture.player.copyInventoryItem"));
			assertTrue(actions.contains("assert.linkedItemContents"));
			assertFalse(actions.contains("backpack.linkHeld"));
		}
	}

	@Test
	void parseMobAndControllerSuitesKeepsPlayerInteractionsSeparateFromLifecycleFixtures() throws IOException {
		for (String name : new String[]{"linked-storage-mob-catcher", "linked-storage-controller-lifecycle"}) {
			try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/" + name + ".json")) {
				assertNotNull(stream);
				RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
				List<String> actions = suite.tests().getFirst().steps().stream().map(step -> step.action().action().metadata().id()).toList();
				if (name.equals("linked-storage-mob-catcher")) {
					assertEquals(4, suite.tests().size());
					assertTrue(actions.contains("player.sneakUseFixtureEntity"));
					assertTrue(actions.contains("menu.clickCapturedMob"));
					assertTrue(actions.contains("assert.canonicalCapturedMobs"));
					for (RegressionTest test : suite.tests().subList(1, 4)) {
						assertTrue(test.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("player.useHeldItemOnTarget")));
						assertEquals(3,
								test.steps().stream().filter(step -> step.action().action().metadata().id().equals("assert.canonicalCapturedMobs")).count());
					}
				} else {
					assertEquals(1, suite.tests().size());
					assertTrue(actions.contains("player.useHeldItemOnTarget"));
					assertEquals(3, actions.stream().filter("fixture.controller.reload"::equals).count());
					assertTrue(actions.contains("player.breakTargetBlock"));
					assertTrue(actions.contains("assert.controllerSharedGroup"));
				}
			}
		}
	}

	@Test
	void parseInteroperabilitySuiteUsesNativeCraftingAndCanonicalAssertionsInBothDirections() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-interoperability.json")) {
			assertNotNull(stream);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			assertEquals(2, suite.tests().size());
			for (RegressionTest test : suite.tests()) {
				List<String> actions = test.steps().stream().map(step -> step.action().action().metadata().id()).toList();
				assertTrue(actions.contains("await.playerCraftingResult"));
				assertTrue(actions.contains("menu.clickPlayerInventorySlot"));
				assertTrue(actions.contains("assert.linkedItemContents"));
				assertFalse(actions.contains("backpack.linkHeld"));
				assertFalse(actions.contains("storage.linkBlock"));
			}
		}
	}

	@Test
	void parseFluidLockSuiteUsesPlayerLinkingAndToolInteractionsForAllPlacedStorageKinds() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-fluid-lock.json")) {
			assertNotNull(stream);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			assertEquals(3, suite.tests().size());
			for (RegressionTest test : suite.tests()) {
				List<String> actions = test.steps().stream().map(step -> step.action().action().metadata().id()).toList();
				assertTrue(actions.contains("menu.clickPlayerInventorySlot"));
				assertTrue(actions.contains("fixture.storage.createLockTool"));
				assertTrue(actions.contains("player.useHeldItemOnTarget"));
				assertFalse(actions.contains("backpack.linkHeld"));
				assertFalse(actions.contains("storage.linkBlock"));
			}
		}
	}

	@Test
	void parseIndexedMenuSuiteLinksThroughClientClicksRatherThanServiceShortcuts() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-menu.json")) {
			assertNotNull(stream);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			Set<String> shortcuts = Set.of("backpack.linkHeld", "storage.linkBlock", "player.selectHeldLinkedEndpoint");
			assertEquals(10, suite.tests().size());
			for (RegressionTest test : suite.tests()) {
				List<String> actions = test.steps().stream().map(step -> step.action().action().metadata().id()).toList();
				assertTrue(actions.stream().noneMatch(shortcuts::contains));
				if (test.id().equals("sort-actions-reject-missing-menu")) {
					assertEquals(List.of("menu.clickSortControl", "assert.sortMode"), actions);
					continue;
				}
				assertTrue(actions.contains("player.useHeldItemOnTarget"));
				if (test.id().equals("double-chest-primary-backpack-secondary")) {
					assertEquals(2, actions.stream().filter("player.placeHeldBlock"::equals).count());
					assertTrue(actions.contains("assert.doubleChest"));
					assertFalse(actions.contains("fixture.block.set"));
					assertTrue(actions.contains("backpack.placeHeld"));
					assertEquals(3, actions.stream().filter("player.useHeldItemOnTarget"::equals).count());
				} else if (test.id().equals("linked-barrels-retain-sort-mode-on-reopen")) {
					assertTrue(actions.contains("menu.clickSortControl"));
					assertTrue(actions.contains("assert.sortMode"));
				} else {
					assertTrue(actions.contains("menu.clickPlayerInventorySlot"));
				}
			}
		}
	}

	@Test
	void parseIndexedResourcesSuiteBindsRegisteredProductionInteractionsAndCapabilityProbes() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-resources.json")) {
			assertNotNull(stream);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			assertEquals("linked-storage-resources", suite.suiteId());
			assertEquals(4, suite.tests().size());
			for (RegressionTest test : suite.tests()) {
				assertTrue(test.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("assert.storageResources")));
				if (!test.id().equals("storage-main-has-no-resource-capabilities")) {
					assertTrue(test.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("capability.transferStorageResource")));
				}
			}
		}
	}
}
