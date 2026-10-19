package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.LazyRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionInvocation;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedJvmContractTest {
	private static final String CANONICAL_ACTION_METADATA = """
			assert.backpackAutomationExtractable|1|minecraft:client,sophisticatedbackpacks
			assert.canonicalBackpackTemplate|1|minecraft:client,sophisticatedbackpacks
			assert.canonicalCapturedMobs|1|minecraft:client,sophisticatedbackpacks
			assert.controllerLinkGroups|1|minecraft:client,sophisticatedstorage
			assert.controllerLinkLine|1|minecraft:client,sophisticatedstorage
			assert.controllerSharedGroup|1|minecraft:client,sophisticatedstorage
			assert.createMountedLinkedStorage|1|minecraft:client,sophisticatedstoragecreateintegration
			assert.doubleChest|1|minecraft:client,sophisticatedstorage
			assert.heldBackpackContents|1|minecraft:client,sophisticatedbackpacks
			assert.heldBackpackFilter|1|minecraft:client,sophisticatedbackpacks
			assert.inventoryCount|1|minecraft:client,sophisticatedbackpacks,sophisticatedstorage
			assert.linkedBackpackHost|1|minecraft:client,sophisticatedbackpacks
			assert.linkedItemContents|1|minecraft:client,sophisticatedcore
			assert.linkedJukeboxPlayback|1|minecraft:client,sophisticatedstorageinmotion
			assert.linkedMenuDelta|1|minecraft:client
			assert.linkedMenuSettings|1|minecraft:client,sophisticatedstorageinmotion
			assert.linkedSettings|1|minecraft:client,sophisticatedbackpacks,sophisticatedcore
			assert.linkedStorageBlock|1|minecraft:client,sophisticatedstorage
			assert.menu|2|minecraft:client
			assert.menuUpgradeLayout|1|minecraft:client,sophisticatedcore
			assert.movingForgedRejected|1|minecraft:client,sophisticatedstorageinmotion
			assert.movingLinked|1|minecraft:client,sophisticatedstorageinmotion
			assert.movingMenuPresentation|1|minecraft:client,sophisticatedstorageinmotion
			assert.movingNestedLinkedIdentity|1|minecraft:client,sophisticatedstorageinmotion
			assert.movingPresentation|1|minecraft:client,sophisticatedstorageinmotion
			assert.movingStorageCompatibilityProfile|1|minecraft:client,sophisticatedstorageinmotion
			assert.movingStorageTierUpgradeRecipe|2|minecraft:client,sophisticatedstorage,sophisticatedstorageinmotion
			assert.movingUnlinked|1|minecraft:client,sophisticatedstorageinmotion
			assert.placedBackpackContents|1|minecraft:client,sophisticatedbackpacks
			assert.playerInventoryBaseline|1|minecraft:client
			assert.restockBackpack|1|minecraft:client,sophisticatedbackpacks
			assert.restockBarrel|1|minecraft:client,sophisticatedstorage
			assert.settingsSelection|1|minecraft:client,sophisticatedcore
			assert.storageResources|1|minecraft:client,sophisticatedstorage,sophisticatedstorageinmotion
			assert.storageTierUpgradeRecipe|2|minecraft:client,sophisticatedstorage
			await.backpackBlock|1|minecraft:client,sophisticatedbackpacks
			await.menu|2|minecraft:client
			await.menuClosed|1|minecraft:client
			await.playerCraftingResult|1|minecraft:client
			await.storageBlock|1|minecraft:client,sophisticatedstorage
			backpack.breakAndAssertDrop|1|minecraft:client,sophisticatedbackpacks
			backpack.configureDeposit|1|minecraft:client,sophisticatedbackpacks
			backpack.configureFilter|1|minecraft:client,sophisticatedbackpacks
			backpack.configureRestock|1|minecraft:client,sophisticatedbackpacks
			backpack.createInMainHand|1|minecraft:client,sophisticatedbackpacks
			backpack.linkHeld|1|minecraft:client,sophisticatedbackpacks
			backpack.pickupPlaced|1|minecraft:client,sophisticatedbackpacks
			backpack.placeHeld|2|minecraft:client,sophisticatedbackpacks
			backpack.probeFilterIO|1|minecraft:client,sophisticatedbackpacks
			backpack.setStorageSlot|1|minecraft:client,sophisticatedbackpacks
			backpack.templateCommand|1|minecraft:client,sophisticatedbackpacks
			backpack.useOnStorage|2|minecraft:client,sophisticatedbackpacks,sophisticatedstorage
			capability.storageResourceCache|1|minecraft:client,sophisticatedstorage
			capability.transferStorageResource|1|minecraft:client,sophisticatedstorage,sophisticatedstorageinmotion
			create.disassembleBackpack|1|minecraft:client,sophisticatedbackpackscreateintegration
			fixture.backpack.setUpgrade|1|minecraft:client,sophisticatedbackpacks
			fixture.block.set|1|minecraft:client
			fixture.chunk.reloadBlockEntities|2|minecraft:client,sophisticatedstorage
			fixture.controller.reload|1|minecraft:client,sophisticatedstorage
			fixture.controller.scanAdjacent|1|minecraft:client
			fixture.controllerLinkGroups.breakBarrel|1|minecraft:client,sophisticatedstorage
			fixture.controllerLinkLine.breakFirstBarrel|1|minecraft:client,sophisticatedstorage
			fixture.create.mountBackpack|1|minecraft:client,sophisticatedbackpackscreateintegration
			fixture.create.mountLinkedStorage|1|minecraft:client,sophisticatedstoragecreateintegration
			fixture.create.setMountedBackpackSlot|1|minecraft:client,sophisticatedbackpackscreateintegration
			fixture.create.setMountedSlot|1|minecraft:client,sophisticatedstoragecreateintegration
			fixture.entity.spawnPig|1|minecraft:client
			fixture.linked.replaceRoot|1|minecraft:client,sophisticatedcore
			fixture.moving.captureNestedLinkedCarrier|1|minecraft:client,sophisticatedstorageinmotion
			fixture.moving.holdRecoveredInner|1|minecraft:client,sophisticatedstorageinmotion
			fixture.moving.reload|1|minecraft:client,sophisticatedstorageinmotion
			fixture.moving.restoreDrop|1|minecraft:client,sophisticatedstorageinmotion
			fixture.moving.setJukeboxDisc|1|minecraft:client,sophisticatedstorageinmotion
			fixture.moving.spawn|1|minecraft:client,sophisticatedstorageinmotion
			fixture.player.copyInventoryItem|1|minecraft:client
			fixture.player.setGameMode|1|minecraft:client
			fixture.region.reset|1|minecraft:client
			fixture.storage.contentsFilter|1|minecraft:client,sophisticatedstorage
			fixture.storage.createLockTool|1|minecraft:client,sophisticatedstorage
			menu.captureLinkedRevision|1|minecraft:client
			menu.clickCapturedMob|1|minecraft:client,sophisticatedbackpacks
			menu.clickPlayerInventorySlot|1|minecraft:client
			menu.clickSettingsControl|1|minecraft:client,sophisticatedcore
			menu.clickSlot|1|minecraft:client,sophisticatedbackpacks
			menu.clickStorageSettings|1|minecraft:client,sophisticatedcore
			menu.clickStorageSlot|1|minecraft:client,sophisticatedcore
			menu.close|1|minecraft:client
			menu.jukeboxPlayback|1|minecraft:client,sophisticatedstorageinmotion
			menu.quickMoveBackpackStorageSlot|1|minecraft:client,sophisticatedbackpacks
			menu.quickMovePlayerInventorySlot|1|minecraft:client,sophisticatedbackpacks
			moving.destroy|1|minecraft:client,sophisticatedstorageinmotion
			moving.probePackedPrimary|1|minecraft:client,sophisticatedstorageinmotion
			moving.probePickup|1|minecraft:client,sophisticatedstorageinmotion
			moving.setPacked|1|minecraft:client,sophisticatedstorageinmotion
			moving.setPresentation|1|minecraft:client,sophisticatedstorageinmotion
			moving.setSlot|1|minecraft:client,sophisticatedstorageinmotion
			moving.setUpgrade|1|minecraft:client,sophisticatedstorageinmotion
			player.breakTargetBlock|1|minecraft:client
			player.clearMainHand|1|minecraft:client
			player.createMainHandItem|1|minecraft:client
			player.openInventory|1|minecraft:client
			player.placeHeldBlock|1|minecraft:client
			player.reset|1|minecraft:client
			player.sendCommand|1|minecraft:client
			player.setInventorySlot|2|minecraft:client
			player.sneakUseFixtureEntity|1|minecraft:client
			player.useControllerLinkTool|1|minecraft:client,sophisticatedstorage
			player.useHeldItem|1|minecraft:client
			player.useHeldItemOnTarget|1|minecraft:client
			player.useMovingStorage|1|minecraft:client,sophisticatedstorageinmotion
			player.useTargetBlock|1|minecraft:client
			recipeviewer.dropSettingsGhost|1|jei,minecraft:client
			storage.linkBlock|1|minecraft:client,sophisticatedstorage
			storage.setBlockSlot|1|minecraft:client,sophisticatedstorage
			""";
	private static final String CANONICAL_ACTION_ORDER = """
			menu.clickStorageSettings
			menu.clickSettingsControl
			recipeviewer.dropSettingsGhost
			assert.settingsSelection
			assert.linkedSettings
			fixture.entity.spawnPig
			player.sneakUseFixtureEntity
			assert.canonicalCapturedMobs
			menu.clickCapturedMob
			player.useHeldItemOnTarget
			capability.storageResourceCache
			fixture.linked.replaceRoot
			fixture.backpack.setUpgrade
			capability.transferStorageResource
			assert.storageResources
			assert.storageTierUpgradeRecipe
			assert.movingStorageTierUpgradeRecipe
			fixture.region.reset
			player.reset
			assert.playerInventoryBaseline
			backpack.createInMainHand
			backpack.templateCommand
			player.createMainHandItem
			fixture.player.copyInventoryItem
			player.breakTargetBlock
			backpack.setStorageSlot
			backpack.linkHeld
			storage.linkBlock
			backpack.configureFilter
			backpack.probeFilterIO
			assert.heldBackpackFilter
			backpack.configureRestock
			backpack.configureDeposit
			backpack.useOnStorage
			assert.inventoryCount
			assert.restockBackpack
			assert.restockBarrel
			player.setInventorySlot
			backpack.placeHeld
			assert.placedBackpackContents
			assert.backpackAutomationExtractable
			assert.heldBackpackContents
			backpack.pickupPlaced
			backpack.breakAndAssertDrop
			player.placeHeldBlock
			await.backpackBlock
			await.storageBlock
			fixture.storage.contentsFilter
			fixture.block.set
			fixture.controller.scanAdjacent
			fixture.controllerLinkLine.breakFirstBarrel
			fixture.controllerLinkGroups.breakBarrel
			player.useControllerLinkTool
			assert.controllerLinkLine
			assert.controllerLinkGroups
			fixture.moving.spawn
			moving.destroy
			fixture.moving.restoreDrop
			fixture.moving.reload
			fixture.moving.holdRecoveredInner
			fixture.create.mountLinkedStorage
			fixture.create.mountBackpack
			create.disassembleBackpack
			fixture.create.setMountedBackpackSlot
			fixture.create.setMountedSlot
			moving.setSlot
			fixture.moving.setJukeboxDisc
			moving.setUpgrade
			moving.setPacked
			moving.setPresentation
			moving.probePickup
			moving.probePackedPrimary
			player.useMovingStorage
			assert.movingLinked
			assert.linkedStorageBlock
			assert.createMountedLinkedStorage
			menu.captureLinkedRevision
			assert.linkedMenuDelta
			assert.linkedMenuSettings
			menu.jukeboxPlayback
			assert.linkedJukeboxPlayback
			assert.movingPresentation
			assert.movingUnlinked
			assert.movingForgedRejected
			fixture.moving.captureNestedLinkedCarrier
			assert.movingNestedLinkedIdentity
			assert.movingStorageCompatibilityProfile
			storage.setBlockSlot
			player.clearMainHand
			player.useHeldItem
			player.useTargetBlock
			await.menu
			assert.menu
			assert.menuUpgradeLayout
			assert.linkedBackpackHost
			assert.linkedItemContents
			assert.canonicalBackpackTemplate
			player.sendCommand
			assert.controllerSharedGroup
			fixture.controller.reload
			fixture.chunk.reloadBlockEntities
			await.playerCraftingResult
			assert.doubleChest
			fixture.storage.createLockTool
			player.openInventory
			fixture.player.setGameMode
			menu.clickPlayerInventorySlot
			menu.clickStorageSlot
			assert.movingMenuPresentation
			menu.clickSlot
			menu.quickMovePlayerInventorySlot
			menu.quickMoveBackpackStorageSlot
			menu.close
			await.menuClosed
			""";
	private static final String CANONICAL_SUITE_TESTS = """
			backpack-menu:backpack-actions-execute-with-valid-fixture,placed-backpack-opens-synchronized-menu,held-backpack-opens-synchronized-item-menu,placed-backpack-menu-click-and-quick-move-stay-synchronized,linked-held-backpack-opens-with-enchanted-book,linked-placed-backpack-opens-with-enchanted-book,linked-held-backpack-quick-move-stays-synchronized,linked-placed-backpack-quick-move-stays-synchronized,linked-held-backpack-menu-receives-slot-delta
			backpack-filters:basic-filter-input-allow-diamond-block-redstone,advanced-filter-input-allow-diamond-block-redstone,basic-filter-output-block-redstone-allow-diamond,advanced-filter-output-block-redstone-allow-diamond
			backpack-restock:basic-restock-allow-diamond,advanced-restock-allow-diamond,basic-restock-block-redstone,advanced-restock-block-redstone,basic-restock-empty-barrel,advanced-restock-empty-barrel
			backpack-create-disassembly:cart-disassembly-preserves-iron-backpack-contents,cart-disassembly-preserves-iron-backpack-break-drop
			storage-menu:place-and-open-barrel-synchronized-menu,placed-linked-block-open-menu-receives-live-slot-delta
			moving-linked-storage:boat-link-through-entity-interaction,minecart-link-through-entity-interaction,primary-tier-migrates-once-secondary-rejected,recoverable-boat-destruction-reopens-same-group,recoverable-minecart-destruction-reopens-same-group,horse-attach-and-drop-preserves-linked-carrier,nonrecoverable-secondary-detaches-without-erasing-group,removed-primary-pauses-authority-but-secondary-remains-usable,carried-primary-pauses-authority-but-secondary-remains-usable,forged-endpoint-and-tooltip-request-denied,limited-barrel-moving-links-with-compatible-profile,only-active-primary-runs-canonical-pickup-upgrade,packed-primary-pauses-pickup-secondary-menu-stays-usable,recovered-moving-carrier-places-original-linked-block,create-cart-mount-opens-original-linked-group,limited-barrel-mount-opens-linked-group,secondary-minecart-sees-boat-canonical-contents,serialized-moving-entity-reloads-same-canonical-group,moving-endpoints-share-upgrade-settings-and-display,removed-endpoint-invalidates-open-menu,moving-primary-open-menu-receives-live-slot-delta,moving-secondary-open-menu-receives-primary-slot-delta,mounted-open-menu-receives-live-slot-delta,linked-moving-jukebox-gui-play-stop-syncs-upgrade-slot,linked-carrier-copy-preserves-identity-and-limited-profile
			tier-upgrade-recipes:storage-tier-upgrade-recipe-linkage-matrix,moving-storage-tier-upgrade-recipe-linkage-matrix
			controller-link:breaking-first-barrel-preserves-storage-link,two-links-survive-shared-group-split,two-separate-groups-remain-linked
			controller-deposit:deposit-actions-conformance,deposit-after-chest-chunk-lifecycle,deposit-after-placed-backpack-chunk-lifecycle
			controller-manual-deposit:contents-item-filter-manual-deposit-conformance,contents-mod-filter-double-click-preserves-items
			linked-storage-action-conformance:empty-crafting-times-out-with-slot-diagnostics,missing-and-out-of-range-entity-handles-fail-deterministically,missing-mob-index-and-menu-fail-deterministically,missing-controller-and-capabilities-fail-deterministically,inventory-mutation-is-journal-owned,qualification-and-cleanup-consumer
			linked-storage-resources:placed-backpack-canonical-resources,boat-minecart-share-resources-after-reload,horse-backpack-resources,storage-main-has-no-resource-capabilities
			linked-storage-menu:double-chest-primary-backpack-secondary,barrel-primary-backpack-secondary,chest-primary-backpack-secondary,shulker-primary-backpack-secondary,backpack-primary-barrel-shows-tank-and-battery-controls,backpack-primary-chest-upgrade-layout-stays-synchronized,backpack-primary-shulker-upgrade-layout-stays-synchronized
			linked-storage-controller-lifecycle:shared-group-retains-all-visual-members-across-controller-reload
			linked-storage-restart:prepare-canonical-mob-and-stashed-items,verify-canonical-mob-and-stashed-items-after-server-restart
			linked-storage-inception:canonical-installed-inception-rejects-disabled-and-enabled-child
			linked-storage-fluid-lock:locked-barrel-filters-canonical-fluid,locked-chest-filters-canonical-fluid,locked-shulker-filters-canonical-fluid
			linked-storage-interoperability:linked-shulker-stashes-into-backpack-canonical-inventory,storage-primary-crafts-backpack-secondary
			linked-storage-mob-catcher:canonical-capture-and-menu-release,captured-mob-shows-in-barrel-secondary,captured-mob-shows-in-chest-secondary,captured-mob-shows-in-shulker-secondary
			linked-storage-settings:settings-edit-actions-reject-missing-screen,barrel-primary-backpack-settings,chest-primary-backpack-settings,shulker-primary-backpack-settings,backpack-primary-barrel-settings,backpack-primary-chest-settings,backpack-primary-shulker-settings,backpack-primary-boat-settings,backpack-primary-minecart-settings,backpack-primary-llama-settings
			""";

	@Test
	void registryMatchesCanonicalActionMetadata() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		String actual = registry.all().values().stream().sorted(Comparator.comparing(metadata -> metadata.id()))
				.map(metadata -> metadata.id() + "|" + metadata.revision() + "|" + String.join(",", metadata.dependencies().stream().sorted().toList()))
				.reduce((first, second) -> first + "\n" + second).orElseThrow();
		assertEquals(114, CANONICAL_ACTION_METADATA.lines().count());
		assertEquals(CANONICAL_ACTION_METADATA.strip(), actual);
		assertEquals(CANONICAL_ACTION_ORDER.strip(), String.join("\n", registry.all().keySet()));
	}

	@Test
	void boundInvocationsExposeDynamicDependenciesAndValidateRegisteredOptionalProxies() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		Map<String, String> expectedDependencies = Map.of("moving", "sophisticatedstorageinmotion", "mounted", "sophisticatedstoragecreateintegration",
				"storageBlock", "sophisticatedstorage", "backpackItem", "sophisticatedbackpacks");
		for (String actionId : List.of("menu.captureLinkedRevision", "assert.linkedMenuDelta")) {
			for (Map.Entry<String, String> entry : expectedDependencies.entrySet()) {
				JsonObject arguments = linkedMenuArguments(entry.getKey(), actionId);
				RegressionActionInvocation invocation = registry.require(actionId).bind(arguments, actionId + " " + entry.getKey());
				assertEquals(Set.of("minecraft:client", entry.getValue()), invocation.dependencies());
			}
		}
		Map<String, String> optionalBindings = Map.ofEntries(Map.entry("recipeviewer.dropSettingsGhost", "{}"),
				Map.entry("assert.storageTierUpgradeRecipe", "{\"form\":\"normal\",\"recipe\":\"shaped\",\"linkage\":\"unlinked\"}"),
				Map.entry("assert.movingStorageTierUpgradeRecipe", "{\"carrier\":\"boat\",\"recipe\":\"shaped\",\"linkage\":\"unlinked\"}"),
				Map.entry("fixture.create.mountLinkedStorage", "{\"saveAs\":\"mounted\"}"),
				Map.entry("fixture.create.mountBackpack", "{\"saveAs\":\"mounted\"}"), Map.entry("create.disassembleBackpack", "{\"ref\":\"mounted\"}"),
				Map.entry("fixture.create.setMountedBackpackSlot", "{\"ref\":\"mounted\",\"item\":\"minecraft:stone\",\"count\":1}"),
				Map.entry("fixture.create.setMountedSlot", "{\"ref\":\"mounted\",\"item\":\"minecraft:stone\",\"count\":1}"),
				Map.entry("assert.createMountedLinkedStorage", "{\"ref\":\"mounted\",\"count\":1,\"checkPresentation\":true}"));
		optionalBindings.forEach((actionId, arguments) -> {
			if (!actionId.equals("assert.storageTierUpgradeRecipe") && !actionId.equals("assert.movingStorageTierUpgradeRecipe")) {
				assertTrue(registry.require(actionId) instanceof LazyRegressionAction);
			}
			assertDoesNotThrow(() -> registry.require(actionId).bind(json(arguments), actionId));
		});
	}

	@Test
	void catalogMapsStrictlyParsedSuiteTestsForTheOfficialRunner() {
		Map<String, RegressionSuite> suites = RegressionCoordinator.loadSuites();
		JsonObject catalog = JsonParser.parseString(RegressionEndpoints.catalogJson(new RegressionActionRegistry().all(), suites)).getAsJsonObject();
		Map<String, List<String>> expectedSuiteTests = suiteTests(CANONICAL_SUITE_TESTS);
		JsonObject actions = catalog.getAsJsonObject("actions");
		assertEquals(expectedSuiteTests.keySet().stream().toList(),
				catalog.getAsJsonArray("suites").asList().stream().map(element -> element.getAsString()).toList());
		assertEquals(expectedSuiteTests, suiteTests(catalog.getAsJsonObject("suiteTests")));
		assertEquals(List.of(CANONICAL_ACTION_ORDER.strip().split("\\R")), actions.keySet().stream().toList());
		assertEquals(114, actions.size());
		for (String entry : CANONICAL_ACTION_METADATA.strip().split("\\R")) {
			String[] metadata = entry.split("\\|", 3);
			JsonObject serialized = actions.getAsJsonObject(metadata[0]);
			assertEquals(metadata[1], serialized.get("revision").getAsString());
			assertEquals(List.of(metadata[2].split(",")),
					serialized.getAsJsonArray("dependencies").asList().stream().map(element -> element.getAsString()).sorted().toList());
		}
	}

	private static Map<String, List<String>> suiteTests(String contract) {
		Map<String, List<String>> suites = new LinkedHashMap<>();
		for (String line : contract.strip().split("\\R")) {
			String[] suite = line.split(":", 2);
			suites.put(suite[0], List.of(suite[1].split(",")));
		}
		return suites;
	}

	private static Map<String, List<String>> suiteTests(JsonObject suites) {
		Map<String, List<String>> names = new LinkedHashMap<>();
		suites.entrySet().forEach(
				entry -> names.put(entry.getKey(), entry.getValue().getAsJsonArray().asList().stream().map(element -> element.getAsString()).toList()));
		return names;
	}

	@Test
	void statusEndpointSerializationExposesDtoTimingsAndFailedCleanup() {
		RegressionCoordinator.StepResult passed = new RegressionCoordinator.StepResult("first", "menu.close", "passed", "", 1.25);
		RegressionCoordinator.StepResult failed = new RegressionCoordinator.StepResult("second", "menu.close", "failed", "failure", 2.5);
		RegressionCoordinator.RuntimeConformanceResult conformance = new RegressionCoordinator.RuntimeConformanceResult("probe", "passed", "", List.of(passed),
				3.75);
		RegressionCoordinator.CleanupResult cleanup = new RegressionCoordinator.CleanupResult("case", "failed", "Cleanup failed");
		RegressionCoordinator.RunStatus status = new RegressionCoordinator.RunStatus(false, "suite/case", "suite", "case", "failed", "Cleanup failed",
				List.of(), List.of(failed), List.of(conformance), Map.of(), 10.5, 4.25, List.of(cleanup), "execution");
		JsonObject result = JsonParser.parseString(RegressionEndpoints.statusJson(status)).getAsJsonObject();
		assertEquals(10.5, result.get("durationMillis").getAsDouble());
		assertEquals(4.25, result.get("testDurationMillis").getAsDouble());
		assertEquals(2.5, result.getAsJsonArray("steps").get(0).getAsJsonObject().get("durationMillis").getAsDouble());
		assertEquals(3.75, result.getAsJsonArray("runtimeConformance").get(0).getAsJsonObject().get("durationMillis").getAsDouble());
		assertEquals(1.25, result.getAsJsonArray("runtimeConformance").get(0).getAsJsonObject().getAsJsonArray("steps").get(0).getAsJsonObject()
				.get("durationMillis").getAsDouble());
		assertEquals("failed", result.getAsJsonArray("cleanup").get(0).getAsJsonObject().get("outcome").getAsString());
		assertTrue(result.has("phase"));
	}

	@Test
	void idleStatusEndpointSerializationHasRunnerTimingFields() {
		JsonObject result = JsonParser.parseString(RegressionEndpoints.statusJson(RegressionCoordinator.RunStatus.idle())).getAsJsonObject();
		assertEquals(0, result.get("durationMillis").getAsDouble());
		assertEquals(0, result.get("testDurationMillis").getAsDouble());
	}

	@Test
	void qualifyKeepsMovingTestsRunnableAndSkipsOnlyMountedCreateTestsWithoutCreateIntegration() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionTest moving = test("moving",
				registry.require("menu.captureLinkedRevision").bind(json("{\"kind\":\"moving\",\"ref\":\"moving\",\"saveAs\":\"baseline\"}"), "moving"));
		RegressionTest mounted = test("mounted",
				registry.require("menu.captureLinkedRevision").bind(json("{\"kind\":\"mounted\",\"ref\":\"mounted\",\"saveAs\":\"baseline\"}"), "mounted"));
		RegressionSuite suite = new RegressionSuite(2, "linked", Set.of("minecraft:client"), "testCell", List.of(moving, mounted));
		List<RegressionCoordinator.ConformanceResult> movingResults = RegressionCoordinator.qualify(suite, List.of(), moving, "",
				dependency -> !dependency.equals("sophisticatedstoragecreateintegration"));
		List<RegressionCoordinator.ConformanceResult> mountedResults = RegressionCoordinator.qualify(suite, List.of(), mounted, "",
				dependency -> !dependency.equals("sophisticatedstoragecreateintegration"));
		assertTrue(movingResults.stream().allMatch(RegressionCoordinator.ConformanceResult::passed));
		assertFalse(mountedResults.stream().allMatch(RegressionCoordinator.ConformanceResult::passed));
		assertEquals(List.of("sophisticatedstoragecreateintegration"),
				mountedResults.stream().filter(result -> !result.passed()).map(RegressionCoordinator.ConformanceResult::dependency).toList());
	}

	private static JsonObject linkedMenuArguments(String kind, String actionId) {
		String common = "\"kind\":\"" + kind + "\"" + (kind.equals("moving") || kind.equals("mounted") ? ",\"ref\":\"linked\"" : "");
		return actionId.equals("menu.captureLinkedRevision")
				? json("{" + common + ",\"saveAs\":\"baseline\"}")
				: json("{" + common + ",\"baseline\":\"baseline\",\"item\":\"minecraft:stone\",\"count\":1}");
	}

	private static RegressionTest test(String id, RegressionActionInvocation invocation) {
		return new RegressionTest(id, Set.of(), 1, false, List.of(new RegressionStep("step", invocation)));
	}

	private static JsonObject json(String value) {
		return JsonParser.parseString(value).getAsJsonObject();
	}
}
