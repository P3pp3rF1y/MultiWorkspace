package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.*;
import net.p3pp3rf1y.devclientautomation.regression.actions.BoundRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateBackpackInMainHandAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LazyRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.PlayerStatePolicy;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionInvocation;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetBackpackStorageSlotAction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionSuiteParserTest {
	@Test
	void indexedMovingSuiteConcreteReferencesFollowTheirPublishers() throws IOException {
		RegressionSuite suite = RegressionSuiteParser.parse(movingLinkedStorageJson(), new RegressionActionRegistry());
		for (RegressionTest test : suite.tests()) {
			Set<String> published = new HashSet<>();
			for (RegressionStep step : test.steps()) {
				if (!(step.action() instanceof BoundRegressionAction<?> bound)) {
					continue; // Optional Create actions remain unloaded during binding.
				}
				Object arguments = bound.arguments();
				String ref = switch (arguments) {
					case null -> null;
					case SpawnMovingStorageAction.Arguments spawn -> {
						assertTrue(published.add(spawn.saveAs()), test.id() + "/" + step.id());
						yield null;
					}
					case RestoreMovingStorageAction.Arguments restore -> {
						assertTrue(published.add(restore.saveAs()), test.id() + "/" + step.id());
						yield restore.ref();
					}
					case CaptureNestedMovingStorageAction.Arguments capture -> {
						assertTrue(published.contains(capture.ref()), test.id() + "/" + step.id());
						assertTrue(published.add(capture.saveAs()), test.id() + "/" + step.id());
						yield null;
					}
					case AssertNestedMovingStorageIdentityAction.Arguments nested -> nested.ref();
					case UseMovingStorageAction.Arguments use -> use.ref();
					case AssertLinkedMovingStorageAction.Arguments linked -> linked.ref();
					case AssertUnlinkedMovingStorageAction.Arguments unlinked -> unlinked.ref();
					case DestroyMovingStorageAction.Arguments destroy -> destroy.ref();
					case ReloadMovingStorageAction.Arguments reload -> reload.ref();
					case SetMovingStorageSlotAction.Arguments slot -> slot.ref();
					case SetMovingStorageUpgradeAction.Arguments upgrade -> upgrade.ref();
					case SetMovingStoragePackedAction.Arguments packed -> packed.ref();
					case String presentation when bound.action() instanceof SetMovingStoragePresentationAction -> presentation;
					case AssertMovingStoragePresentationAction.Arguments presentation -> presentation.primaryRef();
					case AssertMovingMenuPresentationAction.Arguments menuPresentation -> menuPresentation.ref();
					case ProbeMovingStoragePickupAction.Arguments pickup -> pickup.ref();
					case ProbePackedPrimaryAction.Arguments packed -> packed.primaryRef();
					case ProbeForgedMovingStorageAction.Arguments forged -> forged.ref();
					case CaptureLinkedMenuRevisionAction.Arguments revision when revision.kind().equals("moving") -> revision.ref();
					case AssertLinkedMenuDeltaAction.Arguments delta when delta.kind().equals("moving") -> delta.ref();
					case CaptureLinkedMenuRevisionAction.Arguments revision -> {
						assertEquals("mounted", revision.kind(), test.id() + "/" + step.id());
						yield null;
					}
					case AssertLinkedMenuDeltaAction.Arguments delta -> {
						assertEquals("mounted", delta.kind(), test.id() + "/" + step.id());
						yield null;
					}
					case AssertLinkedMenuSettingsAction.Arguments settings -> settings.ref();
					case SetMovingJukeboxDiscAction.Arguments disc -> disc.ref();
					case ControlJukeboxPlaybackAction.Arguments playback -> playback.ref();
					case AssertLinkedJukeboxPlaybackAction.Arguments playback -> playback.ref();
					case AwaitMenuAction.Arguments menu when menu.kind().equals("moving") -> menu.ref();
					case AssertMenuAction.Arguments menu when menu.kind().equals("moving") -> menu.ref();
					case AwaitMenuAction.Arguments menu -> {
						assertEquals("storageBlock", menu.kind(), test.id() + "/" + step.id());
						yield null;
					}
					case AssertMenuAction.Arguments menu -> {
						assertEquals("storageBlock", menu.kind(), test.id() + "/" + step.id());
						yield null;
					}
					default -> {
						assertTrue(
								Set.of("player.createMainHandItem", "player.clearMainHand", "fixture.moving.holdRecoveredInner", "player.placeHeldBlock",
										"player.useTargetBlock", "await.storageBlock", "assert.linkedStorageBlock", "menu.close", "await.menuClosed",
										"assert.movingStorageCompatibilityProfile").contains(bound.action().metadata().id()),
								"Unexpected moving-suite arguments " + arguments.getClass().getName() + " on " + test.id() + "/" + step.id());
						yield null;
					}
				};
				if (ref != null)
					assertTrue(published.contains(ref), test.id() + "/" + step.id() + " refers to unknown " + ref);
				if (arguments instanceof ProbeMovingStoragePickupAction.Arguments pickup) {
					assertTrue(published.contains(pickup.primaryRef()), test.id() + "/" + step.id() + " refers to unknown primary " + pickup.primaryRef());
				}
				if (arguments instanceof ProbePackedPrimaryAction.Arguments packed) {
					assertTrue(published.contains(packed.secondaryRef()),
							test.id() + "/" + step.id() + " refers to unknown secondary " + packed.secondaryRef());
				}
				if (arguments instanceof AssertMovingStoragePresentationAction.Arguments presentation) {
					assertTrue(published.contains(presentation.secondaryRef()),
							test.id() + "/" + step.id() + " refers to unknown secondary " + presentation.secondaryRef());
				}
			}
		}
	}
	@Test
	void indexedResourcesRetainCanonicalSuiteTestFixtureAndBackpackOrder() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/index.json")) {
			assertTrue(stream != null);
			JsonArray index = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonArray();
			List<String> indexedPaths = index.asList().stream().map(entry -> entry.getAsString()).toList();
			assertEquals(List.of("backpack-menu.json", "backpack-filters.json", "backpack-restock.json", "backpack-create-disassembly.json",
					"storage-menu.json", "moving-linked-storage.json", "tier-upgrade-recipes.json", "controller-link.json", "controller-deposit.json",
					"controller-manual-deposit.json", "linked-storage-action-conformance.json", "linked-storage-resources.json", "linked-storage-menu.json",
					"linked-storage-controller-lifecycle.json", "linked-storage-restart.json", "linked-storage-inception.json",
					"linked-storage-fluid-lock.json", "linked-storage-interoperability.json", "linked-storage-mob-catcher.json",
					"linked-storage-settings.json"), indexedPaths);
			assertEquals(20, indexedPaths.size());
			assertEquals(indexedPaths.size(), new HashSet<>(indexedPaths).size());
			Path resourceDirectory = Path.of("src", "main", "resources", "devclientautomation", "regressions");
			try (var resources = Files.list(resourceDirectory)) {
				List<String> physicalJson = resources.filter(path -> path.getFileName().toString().endsWith(".json")).map(path -> path.getFileName().toString())
						.sorted().toList();
				Set<String> expectedJson = new HashSet<>(indexedPaths);
				expectedJson.add("index.json");
				assertEquals(21, physicalJson.size());
				assertEquals(expectedJson, new HashSet<>(physicalJson));
			}
			int testCount = 0;
			int fixtureCount = 0;
			for (String indexedPath : indexedPaths) {
				try (InputStream suiteStream = getClass().getResourceAsStream("/devclientautomation/regressions/" + indexedPath)) {
					assertTrue(suiteStream != null, indexedPath);
					JsonObject suite = JsonParser.parseString(new String(suiteStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
					assertEquals(indexedPath.substring(0, indexedPath.length() - ".json".length()), suite.get("suiteId").getAsString());
					RegressionSuiteParser.parse(suite, new RegressionActionRegistry());
					testCount += suite.getAsJsonArray("tests").size();
					if (suite.has("fixtures"))
						fixtureCount += suite.getAsJsonObject("fixtures").size();
					if (indexedPath.equals("backpack-menu.json")) {
						assertEquals(
								List.of("backpack-actions-execute-with-valid-fixture", "placed-backpack-opens-synchronized-menu",
										"held-backpack-opens-synchronized-item-menu", "placed-backpack-menu-click-and-quick-move-stay-synchronized",
										"linked-held-backpack-opens-with-enchanted-book", "linked-placed-backpack-opens-with-enchanted-book",
										"linked-held-backpack-quick-move-stays-synchronized", "linked-placed-backpack-quick-move-stays-synchronized",
										"linked-held-backpack-menu-receives-slot-delta"),
								suite.getAsJsonArray("tests").asList().stream().map(test -> test.getAsJsonObject().get("id").getAsString()).toList());
					}
				}
			}
			assertEquals(98, testCount);
			assertEquals(16, fixtureCount);
		}
	}

	@Test
	void indexedMovingSuiteRetainsExactlyTwentyFiveTestsAndThreeHundredFourExpandedSteps() throws IOException {
		RegressionSuite suite = RegressionSuiteParser.parse(movingLinkedStorageJson(), new RegressionActionRegistry());
		assertEquals(25, suite.tests().size());
		assertEquals(304, suite.tests().stream().mapToInt(test -> test.steps().size()).sum());
	}

	@Test
	void indexedMovingSuiteMountedProxyReferencesFollowAuthoredSaveAs() throws IOException {
		String json = movingLinkedStorageJson();
		RegressionSuiteParser.parse(json, new RegressionActionRegistry());
		JsonObject resource = JsonParser.parseString(json).getAsJsonObject();
		Set<String> mountedReferenceActions = Set.of("assert.createMountedLinkedStorage", "fixture.create.setMountedSlot");
		for (var test : resource.getAsJsonArray("tests")) {
			List<JsonObject> steps = expandRawSteps(resource, test.getAsJsonObject().getAsJsonArray("steps"));
			Set<String> mountedNames = steps.stream().filter(step -> "fixture.create.mountLinkedStorage".equals(step.get("action").getAsString()))
					.map(step -> step.getAsJsonObject("args").get("saveAs").getAsString()).collect(java.util.stream.Collectors.toSet());
			Set<String> published = new HashSet<>();
			for (JsonObject step : steps) {
				String action = step.get("action").getAsString();
				JsonObject arguments = step.has("args") ? step.getAsJsonObject("args") : new JsonObject();
				if ("fixture.create.mountLinkedStorage".equals(action)) {
					assertTrue(published.add(arguments.get("saveAs").getAsString()),
							test.getAsJsonObject().get("id").getAsString() + "/" + step.get("id").getAsString());
					continue;
				}
				boolean mountedMenu = arguments.has("kind") && "mounted".equals(arguments.get("kind").getAsString());
				if (mountedReferenceActions.contains(action) || mountedMenu) {
					assertTrue(arguments.has("ref"), action + " must author a mounted ref");
					String ref = arguments.get("ref").getAsString();
					assertTrue(mountedNames.contains(ref), action + " refers to a name not authored by a mounted saveAs");
					assertTrue(published.contains(ref), test.getAsJsonObject().get("id").getAsString() + "/" + step.get("id").getAsString());
				}
			}
		}
	}

	private static List<JsonObject> expandRawSteps(JsonObject resource, JsonArray steps) {
		List<JsonObject> expanded = new java.util.ArrayList<>();
		for (var step : steps) {
			JsonObject definition = step.getAsJsonObject();
			if (definition.has("fixture"))
				expanded.addAll(expandRawSteps(resource, resource.getAsJsonObject("fixtures").getAsJsonArray(definition.get("fixture").getAsString())));
			else
				expanded.add(definition);
		}
		return expanded;
	}

	@Test
	void indexedMovingSuiteMutationRejectsUnknownActionsAndInvalidCounts() throws IOException {
		String json = movingLinkedStorageJson();
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(json.replaceFirst("moving\\.setSlot", "moving.unknown"), registry));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(json.replaceFirst("\\\"count\\\": 7", "\"count\": 0"), registry));
	}

	@Test
	void rejectsFractionalSchemaAndActionIntegersBeforeExecution() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(VALID_SUITE.replace("\"schemaVersion\": 2", "\"schemaVersion\": 2.5"), registry));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(VALID_SUITE.replace("\"timeoutTicks\": 20", "\"timeoutTicks\": 20.5"), registry));
		String slot = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"set-slot\", \"action\": \"backpack.setStorageSlot\", \"args\": {\"slot\": 1.5, \"item\": \"minecraft:emerald\", \"count\": 1}");
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(slot, registry));
		String count = slot.replace("\"slot\": 1.5", "\"slot\": 1").replace("\"count\": 1", "\"count\": 1.5");
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(count, registry));
		String optional = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"assert\", \"action\": \"assert.menu\", \"args\": {\"kind\": \"backpackBlock\", \"item\": \"minecraft:air\", \"slot\": 1.5}");
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(optional, registry));
	}

	@Test
	void rejectsMalformedUnusedFixturesBeforeExecution() {
		String fixture = "\"fixtures\": {\"unused\": [{\"id\": \"fixture-step\", \"action\": \"fixture.region.reset\"}]},";
		String suite = VALID_SUITE.replace("\"tests\":", fixture + "\"tests\":");
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(suite.replaceFirst("fixture.region.reset", "fixture.unknown"), registry));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser
				.parse(suite.replaceFirst("\"action\": \"fixture.region.reset\"", "\"action\": \"fixture.region.reset\", \"unsupported\": true"), registry));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(
						suite.replaceFirst("\"action\": \"fixture.region.reset\"", "\"action\": \"fixture.region.reset\", \"args\": {\"unexpected\": true}"),
						registry));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser
				.parse(suite.replaceFirst("\"action\": \"fixture.region.reset\"", "\"action\": \"fixture.region.reset\", \"expectFailure\": 1"), registry));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser
				.parse(suite.replaceFirst("\"action\": \"fixture.region.reset\"", "\"action\": \"fixture.region.reset\", \"expectFailure\": \"\""), registry));
		String invalidCreateSaveAs = VALID_SUITE.replace("\"tests\":",
				"\"fixtures\": {\"unused\": [{\"id\": \"mount\", \"action\": \"fixture.create.mountLinkedStorage\", \"args\": {\"saveAs\": \"Bad Name\"}}]}, \"tests\":");
		String invalidCreateRef = VALID_SUITE.replace("\"tests\":",
				"\"fixtures\": {\"unused\": [{\"id\": \"assert-mounted\", \"action\": \"assert.createMountedLinkedStorage\", \"args\": {\"ref\": \"Bad Name\"}}]}, \"tests\":");
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(invalidCreateSaveAs, registry));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(invalidCreateRef, registry));
	}
	@Test
	void allManifestSelectedActionIdsAreRegistered() {
		String[] selected = """
				assert.backpackAutomationExtractable assert.canonicalBackpackTemplate assert.canonicalCapturedMobs
				assert.controllerLinkGroups assert.controllerLinkLine assert.controllerSharedGroup assert.createMountedLinkedStorage
				assert.doubleChest assert.heldBackpackContents assert.heldBackpackFilter assert.linkedBackpackHost
				assert.linkedItemContents assert.linkedJukeboxPlayback assert.linkedMenuDelta assert.linkedMenuSettings
				assert.linkedSettings assert.linkedStorageBlock assert.menu assert.menuUpgradeLayout assert.movingForgedRejected
				assert.movingLinked assert.movingMenuPresentation assert.movingNestedLinkedIdentity assert.movingPresentation
				assert.movingStorageCompatibilityProfile assert.movingUnlinked assert.placedBackpackContents
				assert.playerInventoryBaseline assert.settingsSelection assert.storageResources await.backpackBlock await.menu
				await.menuClosed await.playerCraftingResult await.storageBlock backpack.breakAndAssertDrop
				backpack.configureFilter backpack.createInMainHand backpack.pickupPlaced backpack.placeHeld backpack.setStorageSlot
				capability.storageResourceCache capability.transferStorageResource create.disassembleBackpack
				fixture.backpack.setUpgrade fixture.block.set fixture.controller.reload fixture.controller.scanAdjacent
				fixture.controllerLinkGroups.breakBarrel fixture.controllerLinkLine.breakFirstBarrel fixture.create.mountBackpack
				fixture.create.mountLinkedStorage fixture.create.setMountedBackpackSlot fixture.create.setMountedSlot
				fixture.entity.spawnPig fixture.linked.replaceRoot fixture.moving.captureNestedLinkedCarrier
				fixture.moving.holdRecoveredInner fixture.moving.reload fixture.moving.restoreDrop fixture.moving.setJukeboxDisc
				fixture.moving.spawn fixture.player.copyInventoryItem fixture.player.setGameMode fixture.region.reset
				fixture.storage.createLockTool menu.captureLinkedRevision menu.clickCapturedMob menu.clickPlayerInventorySlot
				menu.clickSettingsControl menu.clickSlot menu.clickStorageSettings menu.clickStorageSlot menu.close
				menu.jukeboxPlayback menu.quickMovePlayerInventorySlot moving.destroy moving.probePackedPrimary
				moving.probePickup moving.setPacked moving.setPresentation moving.setSlot moving.setUpgrade
				player.breakTargetBlock player.clearMainHand player.createMainHandItem player.openInventory player.placeHeldBlock
				player.reset player.sendCommand player.setInventorySlot player.sneakUseFixtureEntity
				player.useControllerLinkTool player.useHeldItem player.useHeldItemOnTarget player.useMovingStorage
				player.useTargetBlock recipeviewer.dropSettingsGhost storage.setBlockSlot
				""".trim().split("\\s+");
		assertEquals(99, selected.length);
		RegressionActionRegistry registry = new RegressionActionRegistry();
		for (String id : selected) {
			assertEquals(id, registry.require(id).metadata().id());
		}
	}

	@Test
	void selectedSourceScenariosRetainExactAuthoredAndExpandedActionCounts() throws IOException {
		List<String> selected = List.of("backpack-menu.json", "storage-menu.json", "backpack-create-disassembly.json", "moving-linked-storage.json",
				"controller-link.json", "linked-storage-resources.json", "linked-storage-menu.json", "linked-storage-fluid-lock.json",
				"linked-storage-interoperability.json", "linked-storage-controller-lifecycle.json", "linked-storage-mob-catcher.json",
				"linked-storage-inception.json", "linked-storage-restart.json", "linked-storage-action-conformance.json", "linked-storage-settings.json");
		RegressionActionRegistry registry = new RegressionActionRegistry();
		int scenarios = 0;
		int authored = 0;
		int expanded = 0;
		try (var index = getClass().getResourceAsStream("/devclientautomation/regressions/index.json")) {
			assertTrue(index != null);
			Set<String> indexed = new HashSet<>();
			for (var entry : JsonParser.parseString(new String(index.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonArray()) {
				indexed.add(entry.getAsString());
			}
			assertEquals(20, indexed.size());
			assertTrue(indexed.containsAll(selected));
		}
		for (String resource : selected) {
			try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/" + resource)) {
				assertTrue(stream != null, resource);
				JsonObject raw = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
				RegressionSuite suite = RegressionSuiteParser.parse(raw, registry);
				for (var rawTest : raw.getAsJsonArray("tests")) {
					String id = rawTest.getAsJsonObject().get("id").getAsString();
					if ((resource.equals("backpack-menu.json") && !id.equals("linked-held-backpack-menu-receives-slot-delta"))
							|| (resource.equals("storage-menu.json") && !id.equals("placed-linked-block-open-menu-receives-live-slot-delta"))) {
						continue;
					}
					scenarios++;
					authored += rawTest.getAsJsonObject().getAsJsonArray("steps").size();
					String selectedId = id;
					expanded += suite.tests().stream().filter(test -> test.id().equals(selectedId)).findFirst().orElseThrow().steps().size();
				}
			}
		}
		assertEquals(72, scenarios);
		assertEquals(900, authored);
		assertEquals(1478, expanded);
	}

	@Test
	void indexedBackpackCreateDisassemblySuiteBindsBothRecoveryPaths() throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/backpack-create-disassembly.json")) {
			assertTrue(stream != null);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			assertEquals("backpack-create-disassembly", suite.suiteId());
			assertEquals(List.of("cart-disassembly-preserves-iron-backpack-contents", "cart-disassembly-preserves-iron-backpack-break-drop"),
					suite.tests().stream().map(RegressionTest::id).toList());
			for (RegressionTest test : suite.tests()) {
				assertTrue(test.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("fixture.create.mountBackpack")));
				assertTrue(test.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("create.disassembleBackpack")));
			}
			assertTrue(suite.tests().get(1).steps().stream()
					.anyMatch(step -> step.action().action().metadata().id().equals("fixture.create.setMountedBackpackSlot")));
			assertEquals("backpack.breakAndAssertDrop", suite.tests().get(1).steps().getLast().action().action().metadata().id());
		}
	}

	@Test
	void indexedControllerSuiteExpandsSharedFixtureIntoOrderedBlockSteps() throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/controller-link.json")) {
			assertTrue(stream != null);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			assertEquals(3, suite.tests().size());
			assertEquals("prepare-line-first-barrel", suite.tests().getFirst().steps().getFirst().id());
			assertEquals("fixture.block.set", suite.tests().getFirst().steps().getFirst().action().action().metadata().id());
			assertEquals("prepare-shared-group-first-barrel", suite.tests().get(1).steps().getFirst().id());
			assertEquals("add-bridge", suite.tests().get(1).steps().get(8).id());
			assertEquals("hold-storage-tool", suite.tests().get(2).steps().get(8).id());
		}
	}

	@Test
	void tierUpgradeRecipeMatrixParsesAllTwentyFourCells() throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/tier-upgrade-recipes.json")) {
			assertTrue(stream != null);
			String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			RegressionSuite suite = RegressionSuiteParser.parse(json, new RegressionActionRegistry());
			JsonArray tests = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("tests");
			assertEquals("tier-upgrade-recipes", suite.suiteId());
			assertEquals(2, suite.tests().size());
			assertEquals(24, suite.tests().stream().mapToInt(test -> test.steps().size()).sum());
			assertTierRecipeCells(tests.get(0).getAsJsonObject().getAsJsonArray("steps"), "assert.storageTierUpgradeRecipe", "form",
					List.of("normal", "double"));
			assertTierRecipeCells(tests.get(1).getAsJsonObject().getAsJsonArray("steps"), "assert.movingStorageTierUpgradeRecipe", "carrier",
					List.of("boat", "minecart"));
		}
	}

	private static void assertTierRecipeCells(JsonArray steps, String actionId, String variant, List<String> variants) {
		List<String> expectedCells = variants.stream().flatMap(value -> List.of("shaped", "shapeless").stream()
				.flatMap(recipe -> List.of("unlinked", "primary", "secondary").stream().map(linkage -> value + "-" + recipe + "-" + linkage))).toList();
		List<String> actualCells = steps.asList().stream().map(step -> {
			JsonObject definition = step.getAsJsonObject();
			assertEquals(actionId, definition.get("action").getAsString());
			JsonObject arguments = definition.getAsJsonObject("args");
			String cell = arguments.get(variant).getAsString() + "-" + arguments.get("recipe").getAsString() + "-" + arguments.get("linkage").getAsString();
			assertEquals(cell, definition.get("id").getAsString());
			return cell;
		}).toList();
		assertEquals(expectedCells, actualCells);
		assertEquals(actualCells.size(), new HashSet<>(actualCells).size());
	}

	@Test
	void indexedMovingSuiteBindsRegisteredActionsAndOrderedReferences() throws IOException {
		String suiteJson = movingLinkedStorageJson();
		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, registry);

		assertEquals("moving-linked-storage", suite.suiteId());
		assertEquals(25, suite.tests().size());
		assertEquals(304, suite.tests().stream().mapToInt(test -> test.steps().size()).sum());
		for (var test : suite.tests()) {
			for (var step : test.steps()) {
				RegressionActionInvocation invocation = step.action();
				if (invocation.action() instanceof LazyRegressionAction) {
					continue;
				}
				BoundRegressionAction<?> bound = assertInstanceOf(BoundRegressionAction.class, invocation);
				assertSame(registry.require(bound.action().metadata().id()), bound.action());
			}
		}
		for (var test : JsonParser.parseString(suiteJson).getAsJsonObject().getAsJsonArray("tests")) {
			Set<String> published = new HashSet<>();
			for (var step : test.getAsJsonObject().getAsJsonArray("steps")) {
				JsonObject args = step.getAsJsonObject().has("args") ? step.getAsJsonObject().getAsJsonObject("args") : new JsonObject();
				for (String key : List.of("ref", "primaryRef", "secondaryRef", "baseline")) {
					if (args.has(key)) {
						assertTrue(published.contains(args.get(key).getAsString()),
								step.getAsJsonObject().get("id").getAsString() + " refers to unknown " + args.get(key).getAsString());
					}
				}
				if (args.has("saveAs")) {
					assertTrue(published.add(args.get("saveAs").getAsString()),
							step.getAsJsonObject().get("id").getAsString() + " republishes " + args.get("saveAs").getAsString());
				}
			}
		}
	}

	@Test
	void indexedMovingSuiteRejectsUnknownActionsAndInvalidArguments() throws IOException {
		String suiteJson = movingLinkedStorageJson();

		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser
				.parse(suiteJson.replaceFirst("fixture\\.moving\\.spawn", "fixture.moving.unknown"), new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(suiteJson.replaceFirst("\\\"count\\\": 7", "\\\"count\\\": \\\"seven\\\""), new RegressionActionRegistry()));
	}

	private String movingLinkedStorageJson() throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/moving-linked-storage.json")) {
			assertTrue(stream != null);
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	void fixtureReferencesRejectUnknownNestedAndDuplicateExpandedSteps() {
		String suite = """
				{
				  "schemaVersion": 2,
				  "suiteId": "fixture-test",
				  "requires": ["minecraft:client"],
				  "isolation": "testCell",
				  "fixtures": {"base": [{"id": "block", "action": "fixture.block.set", "args": {"block": "minecraft:stone", "offset": [0, 0, 0]}}]},
				  "tests": [{"id": "test", "tags": [], "timeoutTicks": 20, "steps": [{"id": "setup", "fixture": "base"}]}]
				}
				""";
		assertEquals("setup-block", RegressionSuiteParser.parse(suite, new RegressionActionRegistry()).tests().getFirst().steps().getFirst().id());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(suite.replace("\"fixture\": \"base\"", "\"fixture\": \"missing\""), new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser
				.parse(suite.replace("\"id\": \"block\", \"action\"", "\"id\": \"block\", \"fixture\": \"base\", \"action\""), new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(
						suite.replace("[{\"id\": \"setup\", \"fixture\": \"base\"}]",
								"[{\"id\": \"setup\", \"fixture\": \"base\"}, {\"id\": \"setup-block\", \"action\": \"fixture.region.reset\"}]"),
						new RegressionActionRegistry()));
	}

	private static final String VALID_SUITE = """
			{
			  "schemaVersion": 2,
			  "suiteId": "test-suite",
			  "requires": ["minecraft:client"],
			  "isolation": "testCell",
			  "tests": [{
			    "id": "test-case",
			    "tags": ["fast"],
			    "timeoutTicks": 20,
			    "steps": [{"id": "reset", "action": "fixture.region.reset"}]
			  }]
			}
			""";

	@Test
	void parsesSchemaV2WithStableSuiteAndTestIds() {
		RegressionSuite suite = RegressionSuiteParser.parse(VALID_SUITE, new RegressionActionRegistry());

		assertEquals(2, suite.schemaVersion());
		assertEquals("test-suite", suite.suiteId());
		assertEquals("test-case", suite.tests().getFirst().id());
		assertTrue(suite.tests().getFirst().prepareTestCell());
		assertEquals("fixture.region.reset", suite.tests().getFirst().steps().getFirst().action().action().metadata().id());
	}

	@Test
	void supportsTestCellPreparationOptOut() {
		String suiteJson = VALID_SUITE.replace("\"timeoutTicks\": 20", "\"timeoutTicks\": 20, \"prepareTestCell\": false");

		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, new RegressionActionRegistry());

		assertFalse(suite.tests().getFirst().prepareTestCell());
	}

	@Test
	void checkpointPhasesAndExpectedFailuresRequireValidSchema() {
		String preparation = VALID_SUITE
				.replace("\"timeoutTicks\": 20", "\"timeoutTicks\": 20, \"playerState\": \"checkpointPrepare\", \"checkpoint\": \"restart-a\"")
				.replace("\"action\": \"fixture.region.reset\"", "\"action\": \"fixture.region.reset\", \"expectFailure\": \"expected diagnostic\"");
		RegressionTest prepared = RegressionSuiteParser.parse(preparation, new RegressionActionRegistry()).tests().getFirst();
		assertEquals(PlayerStatePolicy.CHECKPOINT_PREPARE, prepared.playerState());
		assertEquals("restart-a", prepared.checkpoint());
		assertEquals("expected diagnostic", prepared.steps().getFirst().expectFailure());
		String verification = preparation.replace("checkpointPrepare", "checkpointVerify").replace("\"timeoutTicks\": 20",
				"\"timeoutTicks\": 20, \"prepareTestCell\": false");
		assertEquals(PlayerStatePolicy.CHECKPOINT_VERIFY,
				RegressionSuiteParser.parse(verification, new RegressionActionRegistry()).tests().getFirst().playerState());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(preparation.replace("checkpointPrepare", "checkpointVerify"), new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(preparation.replace("checkpointPrepare", "unknown"), new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser
				.parse(preparation.replace("\"checkpoint\": \"restart-a\"", "\"checkpoint\": \"\""), new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser
				.parse(preparation.replace("\"expectFailure\": \"expected diagnostic\"", "\"expectFailure\": \"\""), new RegressionActionRegistry()));
	}

	@Test
	void registersTheDefaultTestCellSetup() {
		RegressionActionRegistry registry = new RegressionActionRegistry();

		assertEquals(List.of("fixture.region.reset", "player.reset"),
				registry.testCellSetup().stream().map(step -> step.action().action().metadata().id()).toList());
	}

	@Test
	void registersEachMovingAssertionAndProbeAction() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		List<String> actionIds = List.of("assert.movingLinked", "assert.movingUnlinked", "assert.movingPresentation", "assert.movingMenuPresentation",
				"assert.movingStorageCompatibilityProfile", "assert.movingForgedRejected", "moving.probePickup", "moving.probePackedPrimary",
				"assert.linkedStorageBlock", "fixture.moving.setJukeboxDisc", "menu.captureLinkedRevision", "assert.linkedMenuDelta",
				"assert.linkedMenuSettings", "menu.jukeboxPlayback", "assert.linkedJukeboxPlayback", "fixture.moving.captureNestedLinkedCarrier",
				"assert.movingNestedLinkedIdentity", "assert.storageTierUpgradeRecipe", "assert.movingStorageTierUpgradeRecipe");

		assertEquals(actionIds, actionIds.stream().filter(registry.all()::containsKey).toList());
	}

	@Test
	void bindsNamedArgumentsToTheActionInvocation() {
		String suiteJson = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"create\", \"action\": \"backpack.createInMainHand\", \"args\": {\"tier\": \"diamond\"}");

		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, registry);
		BoundRegressionAction<?> invocation = assertInstanceOf(BoundRegressionAction.class, suite.tests().getFirst().steps().getFirst().action());
		CreateBackpackInMainHandAction.Arguments arguments = assertInstanceOf(CreateBackpackInMainHandAction.Arguments.class, invocation.arguments());

		assertSame(registry.require("backpack.createInMainHand"), invocation.action());
		assertEquals("diamond", arguments.tier());
	}

	@Test
	void bindsEachBackpackStorageSlotIndependently() {
		String suiteJson = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"set-slot\", \"action\": \"backpack.setStorageSlot\", \"args\": {\"slot\": 2, \"item\": \"minecraft:emerald\", \"count\": 3}");

		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, new RegressionActionRegistry());
		BoundRegressionAction<?> invocation = assertInstanceOf(BoundRegressionAction.class, suite.tests().getFirst().steps().getFirst().action());
		SetBackpackStorageSlotAction.Arguments arguments = assertInstanceOf(SetBackpackStorageSlotAction.Arguments.class, invocation.arguments());

		assertEquals(2, arguments.slot());
		assertEquals("minecraft:emerald", arguments.item());
		assertEquals(3, arguments.count());
	}

	@Test
	void rejectsUnsupportedSchemaBeforeExecution() {
		String invalid = VALID_SUITE.replace("\"schemaVersion\": 2", "\"schemaVersion\": 3");

		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(invalid, new RegressionActionRegistry()));

		assertEquals("Unsupported regression schemaVersion 3; expected 2", error.getMessage());
	}

	@Test
	void rejectsDuplicateTestIds() {
		String invalid = """
				{
				  "schemaVersion": 2,
				  "suiteId": "test-suite",
				  "requires": ["minecraft:client"],
				  "isolation": "testCell",
				  "tests": [
				    {"id": "test-case", "tags": [], "timeoutTicks": 1, "steps": [{"id": "reset", "action": "fixture.region.reset"}]},
				    {"id": "test-case", "tags": [], "timeoutTicks": 1, "steps": [{"id": "reset", "action": "fixture.region.reset"}]}
				  ]
				}
				""";

		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(invalid, new RegressionActionRegistry()));

		assertEquals("Suite test-suite contains duplicate test id test-case", error.getMessage());
	}

	@Test
	void rejectsUnknownActionsAndWrongArgumentTypes() {
		String unknownAction = VALID_SUITE.replace("fixture.region.reset", "fixture.unknown");
		String badArgument = VALID_SUITE.replace("\"action\": \"fixture.region.reset\"",
				"\"action\": \"fixture.region.reset\", \"args\": {\"unexpected\": true}");

		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(unknownAction, new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(badArgument, new RegressionActionRegistry()));
	}

	@Test
	void rejectsUnsupportedBackpackTiersBeforeExecution() {
		String invalidTier = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"create\", \"action\": \"backpack.createInMainHand\", \"args\": {\"tier\": \"obsidian\"}");

		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(invalidTier, new RegressionActionRegistry()));

		assertEquals("Argument tier on step create must be one of leather, copper, iron, gold, diamond, netherite", error.getMessage());
	}
}
