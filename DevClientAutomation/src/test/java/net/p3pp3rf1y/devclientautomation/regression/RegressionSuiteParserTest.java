package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedJukeboxPlaybackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedMenuDeltaAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedMenuSettingsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertMenuAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertMovingMenuPresentationAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertMovingStoragePresentationAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertNestedMovingStorageIdentityAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertUnlinkedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AwaitMenuAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.BoundRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CaptureLinkedMenuRevisionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CaptureNestedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ControlJukeboxPlaybackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateBackpackInMainHandAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.DestroyMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.MenuKind;
import net.p3pp3rf1y.devclientautomation.regression.actions.PlayerStatePolicy;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbeForgedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbeMovingStoragePickupAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbePackedPrimaryAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ReloadMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RestoreMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetBackpackStorageSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingJukeboxDiscAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStoragePackedAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStoragePresentationAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStorageSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStorageUpgradeAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SpawnMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.UseMovingStorageAction;
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
			try (var resources = Files.list(Path.of("src", "main", "resources", "devclientautomation", "regressions"))) {
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
					assertTrue(suiteStream != null, () -> indexedPath);
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
	void tierUpgradeRecipeMatrixParsesAllTwentyFourCells() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/tier-upgrade-recipes.json")) {
			assertTrue(stream != null);
			String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			RegressionSuite suite = RegressionSuiteParser.parse(json, new RegressionActionRegistry());
			JsonArray tests = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("tests");
			assertEquals(2, suite.tests().size());
			assertEquals(24, suite.tests().stream().mapToInt(test -> test.steps().size()).sum());
			assertTierRecipeCells(tests.get(0).getAsJsonObject().getAsJsonArray("steps"), "assert.storageTierUpgradeRecipe", "form",
					List.of("normal", "double"));
			assertTierRecipeCells(tests.get(1).getAsJsonObject().getAsJsonArray("steps"), "assert.movingStorageTierUpgradeRecipe", "carrier",
					List.of("boat", "minecart"));
		}
	}

	private static void assertTierRecipeCells(JsonArray steps, String actionId, String variant, List<String> variants) {
		List<String> expected = variants.stream().flatMap(value -> List.of("shaped", "shapeless").stream()
				.flatMap(recipe -> List.of("unlinked", "primary", "secondary").stream().map(linkage -> value + "-" + recipe + "-" + linkage))).toList();
		List<String> actual = steps.asList().stream().map(step -> {
			JsonObject definition = step.getAsJsonObject();
			assertEquals(actionId, definition.get("action").getAsString());
			JsonObject args = definition.getAsJsonObject("args");
			String cell = args.get(variant).getAsString() + "-" + args.get("recipe").getAsString() + "-" + args.get("linkage").getAsString();
			assertEquals(cell, definition.get("id").getAsString());
			return cell;
		}).toList();
		assertEquals(expected, actual);
		assertEquals(actual.size(), new HashSet<>(actual).size());
	}

	@Test
	void indexedBackpackCreateDisassemblySuiteBindsBothRecoveryPaths() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/backpack-create-disassembly.json")) {
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
			List<RegressionStep> steps = suite.tests().get(1).steps();
			assertEquals("backpack.breakAndAssertDrop", steps.get(steps.size() - 1).action().action().metadata().id());
		}
	}

	@Test
	void indexedMovingSuiteMountedProxyReferencesFollowAuthoredSaveAs() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/moving-linked-storage.json")) {
			assertTrue(stream != null);
			String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
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
	}

	private static List<JsonObject> expandRawSteps(JsonObject resource, JsonArray steps) {
		List<JsonObject> expanded = new java.util.ArrayList<>();
		for (var step : steps) {
			JsonObject definition = step.getAsJsonObject();
			if (definition.has("fixture")) {
				expanded.addAll(expandRawSteps(resource, resource.getAsJsonObject("fixtures").getAsJsonArray(definition.get("fixture").getAsString())));
			} else {
				expanded.add(definition);
			}
		}
		return expanded;
	}

	@Test
	void indexedMovingSuiteMutationRejectsUnknownActionsAndInvalidCounts() throws IOException {
		String json;
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/moving-linked-storage.json")) {
			assertTrue(stream != null);
			json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(json.replaceFirst("moving\\.setSlot", "moving.unknown"), registry));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(json.replaceFirst("\\\"count\\\": 7", "\"count\": 0"), registry));
	}

	@Test
	void indexedMovingSuiteRetainsExactlyTwentyFiveTestsAndThreeHundredFourExpandedSteps() throws IOException {
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/moving-linked-storage.json")) {
			assertTrue(stream != null);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			assertEquals(25, suite.tests().size());
			assertEquals(304, suite.tests().stream().mapToInt(test -> test.steps().size()).sum());
		}
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
		String count = slot.replace("1.5", "1").replace("\"count\": 1", "\"count\": 1.5");
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
		String invalidSave = VALID_SUITE.replace("\"tests\":",
				"\"fixtures\": {\"unused\": [{\"id\": \"mount\", \"action\": \"fixture.create.mountLinkedStorage\", \"args\": {\"saveAs\": \"Bad Name\"}}]}, \"tests\":");
		String invalidRef = VALID_SUITE.replace("\"tests\":",
				"\"fixtures\": {\"unused\": [{\"id\": \"assert-mounted\", \"action\": \"assert.createMountedLinkedStorage\", \"args\": {\"ref\": \"Bad Name\"}}]}, \"tests\":");
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(invalidSave, registry));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(invalidRef, registry));
	}

	@Test
	void checkpointPhasesAndExpectedFailuresBindWithoutWeakeningStrictSchema() {
		String prepare = VALID_SUITE
				.replace("\"timeoutTicks\": 20", "\"timeoutTicks\": 20, \"playerState\": \"checkpointPrepare\", \"checkpoint\": \"canonical-mob-items\"")
				.replace("\"action\": \"fixture.region.reset\"", "\"action\": \"fixture.region.reset\", \"expectFailure\": \"expected diagnostic\"");
		RegressionTest test = RegressionSuiteParser.parse(prepare, new RegressionActionRegistry()).tests().get(0);
		assertEquals(PlayerStatePolicy.CHECKPOINT_PREPARE, test.playerState());
		assertEquals("canonical-mob-items", test.checkpoint());
		assertEquals("expected diagnostic", test.steps().get(0).expectFailure());
		String verify = prepare.replace("checkpointPrepare", "checkpointVerify").replace("\"timeoutTicks\": 20",
				"\"timeoutTicks\": 20, \"prepareTestCell\": false");
		assertEquals(PlayerStatePolicy.CHECKPOINT_VERIFY, RegressionSuiteParser.parse(verify, new RegressionActionRegistry()).tests().get(0).playerState());
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(prepare.replace("canonical-mob-items", ""), new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(prepare.replace("expected diagnostic", ""), new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(prepare.replace("checkpointPrepare", "checkpointVerify"), new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(prepare.replace("checkpointPrepare", "unrecognized"), new RegressionActionRegistry()));
	}

	@Test
	void indexedControllerSuiteExpandsSharedFixtureIntoOrderedBlockSteps() throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/controller-link.json")) {
			assertTrue(stream != null);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			assertEquals(3, suite.tests().size());
			assertEquals("prepare-line-first-barrel", suite.tests().get(0).steps().get(0).id());
			assertEquals("fixture.block.set", suite.tests().get(0).steps().get(0).action().action().metadata().id());
			assertEquals("prepare-shared-group-first-barrel", suite.tests().get(1).steps().get(0).id());
			assertEquals("add-bridge", suite.tests().get(1).steps().get(8).id());
			assertEquals("hold-storage-tool", suite.tests().get(2).steps().get(8).id());
		}
	}

	@Test
	void indexedMovingLinkedSuiteBindsLiveMenuAndJukeboxContracts() throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/moving-linked-storage.json")) {
			assertTrue(stream != null);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			assertTrue(suite.tests().stream().anyMatch(test -> test.id().equals("moving-primary-open-menu-receives-live-slot-delta")));
			assertTrue(suite.tests().stream().anyMatch(test -> test.id().equals("mounted-open-menu-receives-live-slot-delta")));
			assertTrue(suite.tests().stream().flatMap(test -> test.steps().stream())
					.anyMatch(step -> step.action().action().metadata().id().equals("assert.linkedJukeboxPlayback")));
		}
	}

	@Test
	void indexedMovingSuiteConcreteReferencesFollowTheirPublishers() throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/moving-linked-storage.json")) {
			assertTrue(stream != null);
			RegressionSuite suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
			for (RegressionTest test : suite.tests()) {
				Set<String> published = new HashSet<>();
				for (RegressionStep step : test.steps()) {
					if (!(step.action() instanceof BoundRegressionAction<?> bound)) {
						continue; // Optional Create actions deliberately remain unloaded during binding.
					}
					Object arguments = bound.arguments();
					String ref = null;
					if (arguments instanceof SpawnMovingStorageAction.Arguments spawn) {
						assertTrue(published.add(spawn.saveAs()), test.id() + "/" + step.id());
					} else if (arguments instanceof RestoreMovingStorageAction.Arguments restore) {
						assertTrue(published.add(restore.saveAs()), test.id() + "/" + step.id());
						ref = restore.ref();
					} else if (arguments instanceof CaptureNestedMovingStorageAction.Arguments capture) {
						ref = capture.ref();
						assertTrue(published.contains(ref), test.id() + "/" + step.id());
						assertTrue(published.add(capture.saveAs()), test.id() + "/" + step.id());
					} else if (arguments instanceof AssertNestedMovingStorageIdentityAction.Arguments nested) {
						ref = nested.ref();
					} else if (arguments instanceof UseMovingStorageAction.Arguments use) {
						ref = use.ref();
					} else if (arguments instanceof AssertLinkedMovingStorageAction.Arguments linked) {
						ref = linked.ref();
					} else if (bound.action() instanceof AssertUnlinkedMovingStorageAction && arguments instanceof String unlinked) {
						ref = unlinked;
					} else if (arguments instanceof DestroyMovingStorageAction.Arguments destroy) {
						ref = destroy.ref();
					} else if (arguments instanceof ReloadMovingStorageAction.Arguments reload) {
						ref = reload.ref();
					} else if (arguments instanceof SetMovingStorageSlotAction.Arguments slot) {
						ref = slot.ref();
					} else if (arguments instanceof SetMovingStorageUpgradeAction.Arguments upgrade) {
						ref = upgrade.ref();
					} else if (arguments instanceof SetMovingStoragePackedAction.Arguments packed) {
						ref = packed.ref();
					} else if (arguments instanceof String presentation && bound.action() instanceof SetMovingStoragePresentationAction) {
						ref = presentation;
					} else if (arguments instanceof AssertMovingStoragePresentationAction.Arguments presentation) {
						ref = presentation.primaryRef();
					} else if (arguments instanceof AssertMovingMenuPresentationAction.Arguments menuPresentation) {
						ref = menuPresentation.ref();
					} else if (arguments instanceof ProbeMovingStoragePickupAction.Arguments pickup) {
						ref = pickup.ref();
					} else if (arguments instanceof ProbePackedPrimaryAction.Arguments packed) {
						ref = packed.primaryRef();
					} else if (arguments instanceof ProbeForgedMovingStorageAction.Arguments forged) {
						ref = forged.ref();
					} else if (arguments instanceof CaptureLinkedMenuRevisionAction.Arguments revision && revision.kind() == MenuKind.MOVING) {
						ref = revision.ref();
					} else if (arguments instanceof AssertLinkedMenuDeltaAction.Arguments delta && delta.kind() == MenuKind.MOVING) {
						ref = delta.ref();
					} else if (arguments instanceof AssertLinkedMenuSettingsAction.Arguments settings) {
						ref = settings.ref();
					} else if (arguments instanceof SetMovingJukeboxDiscAction.Arguments disc) {
						ref = disc.ref();
					} else if (arguments instanceof ControlJukeboxPlaybackAction.Arguments playback) {
						ref = playback.ref();
					} else if (arguments instanceof AssertLinkedJukeboxPlaybackAction.Arguments playback) {
						ref = playback.ref();
					} else if (arguments instanceof AwaitMenuAction.Arguments menu && menu.kind().equals("moving")) {
						ref = menu.ref();
					} else if (arguments instanceof AssertMenuAction.Arguments menu && menu.kind() == MenuKind.MOVING) {
						ref = menu.ref();
					}
					if (ref != null) {
						assertTrue(published.contains(ref), test.id() + "/" + step.id() + " refers to unknown " + ref);
					}
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
		assertEquals("setup-block", RegressionSuiteParser.parse(suite, new RegressionActionRegistry()).tests().get(0).steps().get(0).id());
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
		assertEquals("test-case", suite.tests().get(0).id());
		assertTrue(suite.tests().get(0).prepareTestCell());
		assertEquals("fixture.region.reset", suite.tests().get(0).steps().get(0).action().action().metadata().id());
	}

	@Test
	void supportsTestCellPreparationOptOut() {
		String suiteJson = VALID_SUITE.replace("\"timeoutTicks\": 20", "\"timeoutTicks\": 20, \"prepareTestCell\": false");

		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, new RegressionActionRegistry());

		assertFalse(suite.tests().get(0).prepareTestCell());
	}

	@Test
	void registersTheDefaultTestCellSetup() {
		RegressionActionRegistry registry = new RegressionActionRegistry();

		assertEquals(List.of("fixture.region.reset", "player.reset"),
				registry.testCellSetup().stream().map(step -> step.action().action().metadata().id()).toList());
	}

	@Test
	void bindsNamedArgumentsToTheActionInvocation() {
		String suiteJson = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"create\", \"action\": \"backpack.createInMainHand\", \"args\": {\"tier\": \"diamond\"}");

		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, registry);
		BoundRegressionAction<?> invocation = assertInstanceOf(BoundRegressionAction.class, suite.tests().get(0).steps().get(0).action());
		CreateBackpackInMainHandAction.Arguments arguments = assertInstanceOf(CreateBackpackInMainHandAction.Arguments.class, invocation.arguments());

		assertSame(registry.require("backpack.createInMainHand"), invocation.action());
		assertEquals("diamond", arguments.tier());
	}

	@Test
	void bindsEachBackpackStorageSlotIndependently() {
		String suiteJson = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"set-slot\", \"action\": \"backpack.setStorageSlot\", \"args\": {\"slot\": 2, \"item\": \"minecraft:emerald\", \"count\": 3}");

		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, new RegressionActionRegistry());
		BoundRegressionAction<?> invocation = assertInstanceOf(BoundRegressionAction.class, suite.tests().get(0).steps().get(0).action());
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
