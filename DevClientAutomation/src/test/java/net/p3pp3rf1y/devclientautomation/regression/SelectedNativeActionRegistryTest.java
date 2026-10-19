package net.p3pp3rf1y.devclientautomation.regression;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectedNativeActionRegistryTest {
	// Frozen source manifest: selectedRegressionInventory.requiredActionIds (97 scenario + two automatic setup IDs).
	static final Set<String> SELECTED = Arrays.stream(("""
			assert.backpackAutomationExtractable assert.canonicalBackpackTemplate assert.canonicalCapturedMobs
			assert.controllerLinkGroups assert.controllerLinkLine assert.controllerSharedGroup assert.createMountedLinkedStorage
			assert.doubleChest assert.heldBackpackContents assert.heldBackpackFilter assert.linkedBackpackHost
			assert.linkedItemContents assert.linkedJukeboxPlayback assert.linkedMenuDelta assert.linkedMenuSettings
			assert.linkedSettings assert.linkedStorageBlock assert.menu assert.menuUpgradeLayout assert.movingForgedRejected
			assert.movingLinked assert.movingMenuPresentation assert.movingNestedLinkedIdentity assert.movingPresentation
			assert.movingStorageCompatibilityProfile assert.movingUnlinked assert.placedBackpackContents
			assert.playerInventoryBaseline assert.settingsSelection assert.storageResources await.backpackBlock await.menu
			await.menuClosed await.playerCraftingResult await.storageBlock backpack.breakAndAssertDrop
			backpack.configureFilter backpack.createInMainHand backpack.pickupPlaced backpack.placeHeld backpack.templateCommand
			backpack.setStorageSlot capability.storageResourceCache capability.transferStorageResource fixture.storage.contentsFilter
			create.disassembleBackpack fixture.backpack.setUpgrade fixture.block.set fixture.controller.reload
			fixture.controller.scanAdjacent fixture.controllerLinkGroups.breakBarrel fixture.controllerLinkLine.breakFirstBarrel
			fixture.create.mountBackpack fixture.create.mountLinkedStorage fixture.create.setMountedBackpackSlot
			fixture.create.setMountedSlot fixture.entity.spawnPig fixture.linked.replaceRoot
			fixture.moving.captureNestedLinkedCarrier fixture.moving.holdRecoveredInner fixture.moving.reload
			fixture.moving.restoreDrop fixture.moving.setJukeboxDisc fixture.moving.spawn fixture.player.copyInventoryItem
			fixture.player.setGameMode fixture.region.reset fixture.storage.createLockTool menu.captureLinkedRevision
			menu.clickCapturedMob menu.clickPlayerInventorySlot menu.clickSettingsControl menu.clickSlot
			menu.clickStorageSettings menu.clickStorageSlot menu.close menu.jukeboxPlayback
			menu.quickMoveBackpackStorageSlot menu.quickMovePlayerInventorySlot moving.destroy moving.probePackedPrimary moving.probePickup
			moving.setPacked moving.setPresentation moving.setSlot moving.setUpgrade player.breakTargetBlock
			player.clearMainHand player.createMainHandItem player.openInventory player.placeHeldBlock player.reset
			player.sendCommand player.setInventorySlot player.sneakUseFixtureEntity player.useControllerLinkTool
			player.useHeldItem player.useHeldItemOnTarget player.useMovingStorage player.useTargetBlock
			recipeviewer.dropSettingsGhost storage.setBlockSlot
			""").trim().split("\\s+")).collect(Collectors.toUnmodifiableSet());

	@Test
	void everyFrozenSelectedActionIdIsBoundWithoutRemovingUnselectedActions() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertEquals(102, SELECTED.size(), "Frozen manifest action ID inventory drifted");
		assertTrue(registry.all().keySet().containsAll(SELECTED),
				() -> "Missing selected IDs: " + SELECTED.stream().filter(id -> !registry.all().containsKey(id)).sorted().toList());
		assertEquals(SELECTED, registry.all().keySet().stream().filter(SELECTED::contains).collect(Collectors.toSet()));
		SELECTED.forEach(id -> assertEquals(id, registry.require(id).metadata().id()));
		assertEquals("fixture.region.reset", registry.testCellSetup().get(0).action().action().metadata().id());
		assertEquals("player.reset", registry.testCellSetup().get(1).action().action().metadata().id());
	}
}
