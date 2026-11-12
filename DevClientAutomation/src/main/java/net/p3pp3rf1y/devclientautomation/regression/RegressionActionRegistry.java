package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertBackpackAutomationExtractableAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertBackpackPickupAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertCanonicalBackpackTemplateAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertCanonicalCapturedMobsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertCompressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertControllerLinkGroupsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertControllerLinkLineAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertControllerSharedGroupAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertDoubleChestAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertHeldBackpackContentsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertHeldBackpackFilterAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertHeldBackpackJukeboxPlaybackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertInventoryCountAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertInventoryInteractionCountAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedBackpackHostAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedItemContentsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedJukeboxPlaybackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedMenuDeltaAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedMenuSettingsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedSettingsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedStorageBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertMenuAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertMenuUpgradeLayoutAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertMovingMenuPresentationAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertMovingStorageCompatibilityProfileAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertMovingStoragePresentationAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertNestedMovingStorageIdentityAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertPlacedBackpackContentsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertPlayerHealthAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertPlayerInventoryBaselineAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertRestockBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertRestockBarrelAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertSettingsSelectionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertSortModeAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertStorageResourcesAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertUnlinkedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertUpgradeSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AwaitBackpackBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AwaitItemEntityAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AwaitMenuAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AwaitMenuClosedAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AwaitPlayerCraftingResultAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AwaitStorageBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AwaitVanillaMenuAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.BackpackTemplateCommandAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.BreakControllerLinkGroupsBarrelAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.BreakControllerLinkLineBarrelAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.BreakPlacedBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.BreakTargetBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CaptureLinkedMenuRevisionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CaptureNestedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClearMainHandAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickCapturedMobAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickMenuSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickPlayerInventorySlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickSettingsControlAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickSortControlAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickStorageMenuSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickStorageSettingsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickUpgradeSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CloseMenuAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ConfigureBackpackDepositAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ConfigureBackpackFilterAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ConfigureBackpackPickupAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ConfigureBackpackRestockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ConfigureStorageContentsFilterAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ControlHeldBackpackJukeboxPlaybackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ControlJukeboxPlaybackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CopyPlayerInventoryItemAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateBackpackInMainHandAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateLockToolAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateMainHandItemAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.DestroyMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ExerciseCompressionAutomationAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ExerciseCompressionFrontAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.FillBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.FixtureCompressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.HoldRecoveredMovingStorageInnerAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.KeepInventoryDeathRespawnAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LazyOptionalRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LethalDamageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LinkHeldBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LinkTargetStorageBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ObserveHeldBackpackJukeboxSoundAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.OpenInventoryInteractionMenuAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.OpenPlayerInventoryAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.OpenUpgradeTabAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.PickupPlacedBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.PlaceHeldBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.PlaceHeldBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.PressInventoryInteractionKeybindAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbeBackpackFilterAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbeForgedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbeMovingStoragePickupAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbePackedPrimaryAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProfileCompressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.QuickMoveBackpackStorageSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.QuickMovePlayerInventorySlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionMetadata;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionValueContract;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionValueStore;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionValueTypes;
import net.p3pp3rf1y.devclientautomation.regression.actions.ReloadChunkBlockEntitiesAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ReloadFixtureControllerAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ReloadMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ReplaceLinkedStorageRootAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ResetPlayerAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ResetRegionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RestoreMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ScanFixtureControllerAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SendPlayerCommandAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetBackpackStorageSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetCraftingRefillAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetFixtureBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetHeldBackpackJukeboxDiscAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetHeldBackpackUpgradeAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingJukeboxDiscAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStoragePackedAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStoragePresentationAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStorageSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStorageUpgradeAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetPlayerGameModeAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetPlayerInventorySlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetStorageBlockSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetStorageUpgradeAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetUpgradeTabAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SpawnFixturePigAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SpawnItemEntityAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SpawnMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.StorageResourceCacheAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.TransferLinkedBackpackFluidAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.TransferLinkedPumpFluidAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.TransferStorageResourceAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.UseControllerLinkToolAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.UseFixtureEntityAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.UseHeldBackpackOnStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.UseHeldItemAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.UseHeldItemOnTargetAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.UseMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.UseTargetBlockAction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class RegressionActionRegistry {
	private static final Pattern RECIPE_RESOURCE_LOCATION = Pattern.compile("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+");
	private final Map<String, RegressionAction> actions = new LinkedHashMap<>();
	public RegressionActionRegistry() {
		register(lazyTankAction("assert.tank", "AssertTankAction"));
		register(lazyTankAction("player.useTankSegment", "UseTankSegmentAction"));
		register(lazyTankAction("capability.tankFluid", "TankFluidTransferAction"));
		register(lazyTankAction("fixture.tank.recoverDrop", "RecoverTankDropAction"));
		register(lazyTankAction("world.removeTargetBlock", "RemoveTargetBlockAction"));
		register(lazyTankAction("fixture.tank.spawnExperience", "SpawnTankExperienceAction"));
		register(lazyTankAction("fixture.tank.reload", "ReloadTankAction"));
		register(lazyTankAction("diagnostic.screenshot", "CaptureScreenshotAction"));
		register(lazyTankAction("menu.clickPumpControl", "ClickPumpControlAction"));
		register(lazyTankAction("menu.clickTankGauge", "ClickTankGaugeAction"));
		register(new ClickSortControlAction());
		register(new AssertSortModeAction());
		register(new ClickSettingsControlAction());
		register(lazyJeiAction());
		register(lazyJeiTransferAction());
		register(new AssertSettingsSelectionAction());
		register(new AssertDoubleChestAction());
		register(new ClickStorageMenuSlotAction());
		register(new ClickUpgradeSlotAction());
		register(new OpenUpgradeTabAction());
		register(new SetUpgradeTabAction());
		register(new SetCraftingRefillAction());
		register(new AssertUpgradeSlotAction());
		register(new ClickStorageSettingsAction());
		register(new ResetRegionAction());
		register(new ResetPlayerAction());
		register(new KeepInventoryDeathRespawnAction());
		register(new LethalDamageAction());
		register(new AssertPlayerHealthAction());
		register(new AssertPlayerInventoryBaselineAction());
		register(new CreateBackpackInMainHandAction());
		register(new BackpackTemplateCommandAction());
		register(new CreateMainHandItemAction());
		register(new CopyPlayerInventoryItemAction());
		register(new SendPlayerCommandAction());
		register(new OpenPlayerInventoryAction());
		register(new SetPlayerGameModeAction());
		register(new AwaitPlayerCraftingResultAction());
		register(new ClickPlayerInventorySlotAction());
		register(new SetBackpackStorageSlotAction());
		register(new ConfigureBackpackFilterAction());
		register(new ConfigureBackpackDepositAction());
		register(new ProbeBackpackFilterAction());
		register(new AssertHeldBackpackFilterAction());
		register(new AssertHeldBackpackContentsAction());
		register(new ConfigureBackpackRestockAction());
		register(new UseHeldBackpackOnStorageAction());
		register(new AssertInventoryCountAction());
		register(new AssertInventoryInteractionCountAction());
		register(new ConfigureBackpackPickupAction());
		register(new FillBackpackAction());
		register(new SpawnItemEntityAction());
		register(new AwaitItemEntityAction());
		register(new AssertBackpackPickupAction());
		register(new AssertRestockBackpackAction());
		register(new AssertRestockBarrelAction());
		register(new SetPlayerInventorySlotAction());
		register(new PlaceHeldBackpackAction());
		register(new PickupPlacedBackpackAction());
		register(new BreakPlacedBackpackAction());
		register(new PlaceHeldBlockAction());
		register(new AwaitBackpackBlockAction());
		register(new AwaitStorageBlockAction());
		register(new ConfigureStorageContentsFilterAction());
		register(new SetStorageBlockSlotAction());
		register(new FixtureCompressionAction());
		register(new AssertCompressionAction());
		register(new ProfileCompressionAction());
		register(new ExerciseCompressionAutomationAction());
		register(new ExerciseCompressionFrontAction());
		register(new SetStorageUpgradeAction());
		register(new ClearMainHandAction());
		register(new UseHeldItemAction());
		register(new UseTargetBlockAction());
		register(new AwaitMenuAction());
		register(new AwaitVanillaMenuAction());
		register(new OpenInventoryInteractionMenuAction());
		register(new PressInventoryInteractionKeybindAction());
		register(new AssertMenuAction());
		register(new ClickMenuSlotAction());
		register(new QuickMovePlayerInventorySlotAction());
		register(new QuickMoveBackpackStorageSlotAction());
		register(new CloseMenuAction());
		register(new AwaitMenuClosedAction());
		register(new SetFixtureBlockAction());
		register(new ScanFixtureControllerAction());
		register(new ReloadChunkBlockEntitiesAction());
		register(new BreakControllerLinkLineBarrelAction());
		register(new BreakControllerLinkGroupsBarrelAction());
		register(new UseControllerLinkToolAction());
		register(new AssertControllerLinkLineAction());
		register(new AssertControllerLinkGroupsAction());
		register(new SpawnMovingStorageAction());
		register(new DestroyMovingStorageAction());
		register(new RestoreMovingStorageAction());
		register(new ReloadMovingStorageAction());
		register(lazyCreateAction("fixture.create.mountLinkedStorage", "sophisticatedstoragecreateintegration", "MountLinkedStorageAction", Set.of("saveAs"),
				Set.of("saveAs"), Set.of(), Set.of(),
				args -> RegressionValueContract.producing(args.get("saveAs").getAsString(), RegressionValueTypes.MOUNTED_STORAGE)));
		register(lazyCreateAction("fixture.create.mountBackpack", "sophisticatedbackpackscreateintegration", "MountBackpackOnCartAction", Set.of("saveAs"),
				Set.of("saveAs"), Set.of(), Set.of(), args -> RegressionValueContract.producing(args.get("saveAs").getAsString(), RegressionValueTypes.UUID)));
		register(lazyCreateAction("create.disassembleBackpack", "sophisticatedbackpackscreateintegration", "DisassembleCartBackpackAction", Set.of("ref"),
				Set.of("ref"), Set.of(), Set.of(), args -> RegressionValueContract.consuming(args.get("ref").getAsString(), RegressionValueTypes.UUID)));
		register(new AssertPlacedBackpackContentsAction());
		register(new AssertBackpackAutomationExtractableAction());
		register(lazyCreateAction("fixture.create.setMountedSlot", "sophisticatedstoragecreateintegration", "SetMountedStorageSlotAction",
				Set.of("ref", "item", "count"), Set.of("ref", "item"), Set.of("count"), Set.of(),
				args -> RegressionValueContract.consuming(args.get("ref").getAsString(), RegressionValueTypes.MOUNTED_STORAGE)));
		register(lazyCreateAction("fixture.create.setMountedBackpackSlot", "sophisticatedbackpackscreateintegration", "SetMountedBackpackSlotAction",
				Set.of("ref", "item", "count"), Set.of("ref", "item"), Set.of("count"), Set.of(),
				args -> RegressionValueContract.consuming(args.get("ref").getAsString(), RegressionValueTypes.UUID)));
		register(new SetMovingStorageSlotAction());
		register(new SetMovingJukeboxDiscAction());
		register(new SetMovingStorageUpgradeAction());
		register(new UseMovingStorageAction());
		register(new AssertLinkedMovingStorageAction());
		register(new CaptureNestedMovingStorageAction());
		register(new AssertNestedMovingStorageIdentityAction());
		register(new AssertMovingStorageCompatibilityProfileAction());
		register(new LazyOptionalRegressionAction("assert.createMountedLinkedStorage", "1", Set.of("minecraft:client", "sophisticatedstoragecreateintegration"),
				"net.p3pp3rf1y.devclientautomation.regression.actions.AssertMountedLinkedStorageAction", (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of("ref", "count", "checkPresentation"), description);
					LazyOptionalRegressionAction.requireString(args, "ref", description);
					RegressionValueStore.requireName(args.get("ref").getAsString());
					LazyOptionalRegressionAction.optionalNonNegativeInt(args, "count", description);
					if (args.has("checkPresentation")
							&& (!args.get("checkPresentation").isJsonPrimitive() || !args.getAsJsonPrimitive("checkPresentation").isBoolean()))
						throw new IllegalArgumentException("Argument checkPresentation on " + description + " must be boolean");
				}, args -> RegressionValueContract.consuming(args.get("ref").getAsString(), RegressionValueTypes.MOUNTED_STORAGE)));
		register(new AssertUnlinkedMovingStorageAction());
		register(new SetMovingStoragePackedAction());
		register(new ProbePackedPrimaryAction());
		register(new ProbeMovingStoragePickupAction());
		register(new ProbeForgedMovingStorageAction());
		register(new SetMovingStoragePresentationAction());
		register(new AssertMovingStoragePresentationAction());
		register(new AssertMovingMenuPresentationAction());
		register(new AssertLinkedStorageBlockAction());
		register(new CaptureLinkedMenuRevisionAction());
		register(new AssertLinkedMenuDeltaAction());
		register(new AssertLinkedMenuSettingsAction());
		register(new ControlJukeboxPlaybackAction());
		register(new ObserveHeldBackpackJukeboxSoundAction());
		register(new ControlHeldBackpackJukeboxPlaybackAction());
		register(new AssertHeldBackpackJukeboxPlaybackAction());
		register(new AssertLinkedJukeboxPlaybackAction());
		register(new HoldRecoveredMovingStorageInnerAction());
		register(lazyTierAction("assert.storageTierUpgradeRecipe", Set.of("minecraft:client", "sophisticatedstorage"), "AssertStorageTierUpgradeRecipeAction",
				"form", Set.of("normal", "double")));
		register(lazyTierAction("assert.movingStorageTierUpgradeRecipe", Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"),
				"AssertMovingStorageTierUpgradeRecipeAction", "carrier", Set.of("boat", "minecart")));
		register(new AssertCanonicalBackpackTemplateAction());
		register(new AssertCanonicalCapturedMobsAction());
		register(new AssertControllerSharedGroupAction());
		register(new AssertLinkedBackpackHostAction());
		register(new AssertLinkedItemContentsAction());
		register(new AssertLinkedSettingsAction());
		register(new AssertMenuUpgradeLayoutAction());
		register(new AssertStorageResourcesAction());
		register(new BreakTargetBlockAction());
		register(new ClickCapturedMobAction());
		register(new CreateLockToolAction());
		register(new LinkHeldBackpackAction());
		register(new LinkTargetStorageBlockAction());
		register(new ReloadFixtureControllerAction());
		register(new ReplaceLinkedStorageRootAction());
		register(new SetHeldBackpackUpgradeAction());
		register(new SetHeldBackpackJukeboxDiscAction());
		register(new SpawnFixturePigAction());
		register(new StorageResourceCacheAction());
		register(new TransferStorageResourceAction());
		register(new TransferLinkedBackpackFluidAction());
		register(new TransferLinkedPumpFluidAction());
		register(new UseFixtureEntityAction());
		register(new UseHeldItemOnTargetAction());
		// The registry is presented in the frozen catalog order, independently of registration groups.
		Map<String, RegressionAction> ordered = new LinkedHashMap<>();
		for (String id : CATALOG_ORDER.strip().split("\\R")) {
			RegressionAction action = actions.get(id);
			if (action == null || ordered.put(id, action) != null) {
				throw new IllegalStateException("Invalid regression catalog action " + id);
			}
		}
		if (ordered.size() != actions.size()) {
			throw new IllegalStateException("Regression catalog action order is incomplete");
		}
		actions.clear();
		actions.putAll(ordered);
	}
	private static final String CATALOG_ORDER = """
			assert.tank
			player.useTankSegment
			capability.tankFluid
			fixture.tank.recoverDrop
			world.removeTargetBlock
			fixture.tank.spawnExperience
			fixture.tank.reload
			diagnostic.screenshot
			menu.clickPumpControl
			menu.clickTankGauge
			menu.clickSortControl
			assert.sortMode
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
			fixture.backpack.setHeldJukeboxDisc
			fixture.storage.setUpgrade
			capability.transferStorageResource
			capability.transferLinkedBackpackFluid
			capability.transferLinkedPumpFluid
			assert.storageResources
			assert.storageTierUpgradeRecipe
			assert.movingStorageTierUpgradeRecipe
			fixture.region.reset
			player.reset
			player.keepInventoryDeathRespawn
			player.lethalDamage
			assert.playerHealth
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
			assert.inventoryInteractionCount
			backpack.configurePickup
			backpack.fillStorage
			fixture.itemEntity.spawn
			await.itemEntity
			assert.backpackPickupConfiguration
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
			fixture.storage.compression
			assert.storage.compression
			profile.storage.compression
			exercise.storage.compressionAutomation
			exercise.storage.compressionFront
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
			observe.heldBackpackJukeboxSound
			menu.heldBackpackJukeboxPlayback
			assert.heldBackpackJukeboxPlayback
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
			await.vanillaMenu
			fixture.openInventoryInteractionMenu
			input.inventoryInteractionKeybind
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
			menu.clickUpgradeSlot
			menu.openUpgradeTab
			menu.setUpgradeTab
			menu.setCraftingRefill
			assert.upgradeSlot
			assert.movingMenuPresentation
			menu.clickSlot
			menu.quickMovePlayerInventorySlot
			menu.quickMoveBackpackStorageSlot
			menu.close
			await.menuClosed
			recipeviewer.transferRecipe
			""";
	public RegressionAction require(String actionId) {
		RegressionAction action = actions.get(actionId);
		if (action == null) {
			throw new IllegalArgumentException("Unknown regression action " + actionId);
		}
		return action;
	}
	public Map<String, RegressionActionMetadata> all() {
		Map<String, RegressionActionMetadata> metadata = new LinkedHashMap<>();
		actions.forEach((id, action) -> metadata.put(id, action.metadata()));
		return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
	}
	public List<RegressionStep> testCellSetup() {
		return List.of(setupStep("prepare-region", "fixture.region.reset"), setupStep("prepare-player", "player.reset"));
	}
	private RegressionStep setupStep(String stepId, String actionId) {
		return new RegressionStep(stepId, require(actionId).bind(new JsonObject(), "automatic " + stepId));
	}
	private void register(RegressionAction action) {
		actions.put(action.metadata().id(), action);
	}
	private static RegressionAction lazyJeiAction() {
		return new LazyOptionalRegressionAction("recipeviewer.dropSettingsGhost", "1", Set.of("minecraft:client", "jei"),
				"net.p3pp3rf1y.devclientautomation.regression.actions.DropSettingsGhostAction", (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of("slot"), description);
					LazyOptionalRegressionAction.optionalNonNegativeInt(args, "slot", description);
				});
	}

	private static RegressionAction lazyTankAction(String id, String implementation) {
		Set<String> dependencies = switch (id) {
			case "diagnostic.screenshot" -> Set.of("minecraft:client");
			case "menu.clickPumpControl" -> Set.of("minecraft:client", "sophisticatedcore");
			default -> Set.of("minecraft:client", "sophisticatedstorage");
		};
		return new LazyOptionalRegressionAction(id, "1", dependencies, "net.p3pp3rf1y.devclientautomation.regression.actions." + implementation,
				(args, description) -> validateTankArguments(id, args, description));
	}

	private static void validateTankArguments(String id, JsonObject args, String description) {
		switch (id) {
			case "assert.tank" -> {
				LazyOptionalRegressionAction.requireOnly(args,
						Set.of("offset", "present", "amount", "capacity", "height", "upgrades", "tier", "tankDrops", "droppedFluid", "droppedHeight",
								"droppedUpgrades", "menu", "emission", "light", "tabs", "switches", "cursorItem", "cursorCount", "containerSlot",
								"containerItem", "containerCount", "droppedFluidType"),
						description);
				validateOffset(args, description);
				for (String name : Set.of("amount", "capacity", "height", "upgrades", "tankDrops", "droppedFluid", "droppedHeight", "droppedUpgrades",
						"emission", "light", "tabs", "switches", "cursorCount", "containerCount", "containerSlot")) {
					optionalTankInteger(args, name, false, description);
				}
				if (args.has("containerSlot") && RegressionJsonNumbers.integer(args.get("containerSlot")) > 3)
					throw new IllegalArgumentException("Tank container slot must be from 0 through 3");
				for (String name : Set.of("tier", "cursorItem", "containerItem", "droppedFluidType"))
					optionalTankString(args, name, description);
				for (String name : Set.of("present", "menu"))
					optionalTankBoolean(args, name, description);
			}
			case "player.useTankSegment", "capability.tankFluid" -> {
				LazyOptionalRegressionAction.requireOnly(args,
						id.equals("capability.tankFluid")
								? Set.of("offset", "face", "fluid", "amount", "expected", "drain", "simulate")
								: Set.of("offset", "face"),
						description);
				validateOffset(args, description);
				String face = args.has("face") ? tankString(args, "face", description) : "up";
				if (!Set.of("up", "down", "north", "south", "east", "west").contains(face))
					throw new IllegalArgumentException("Invalid face on " + description);
				if (id.equals("capability.tankFluid")) {
					LazyOptionalRegressionAction.requirePositiveInt(args, "amount", description);
					optionalTankInteger(args, "expected", false, description);
					if (args.has("fluid") && !RECIPE_RESOURCE_LOCATION.matcher(tankString(args, "fluid", description)).matches())
						throw new IllegalArgumentException("Invalid fluid on " + description);
					optionalTankBoolean(args, "drain", description);
					optionalTankBoolean(args, "simulate", description);
				}
			}
			case "fixture.tank.recoverDrop", "menu.clickTankGauge" -> LazyOptionalRegressionAction.requireOnly(args, Set.of(), description);
			case "world.removeTargetBlock" -> {
				LazyOptionalRegressionAction.requireOnly(args, Set.of("offset", "mode"), description);
				validateOffset(args, description);
				String mode = args.has("mode") ? tankString(args, "mode", description) : "destroy";
				if (!Set.of("destroy", "setAir", "explosion").contains(mode))
					throw new IllegalArgumentException("Invalid removal mode on " + description);
			}
			case "fixture.tank.spawnExperience" -> {
				LazyOptionalRegressionAction.requireOnly(args, Set.of("experience"), description);
				optionalTankInteger(args, "experience", true, description);
				if (args.has("experience") && RegressionJsonNumbers.integer(args.get("experience")) > 100)
					throw new IllegalArgumentException("Experience exceeds the bounded fixture on " + description);
			}
			case "fixture.tank.reload" -> {
				LazyOptionalRegressionAction.requireOnly(args, Set.of("offset", "sneaking"), description);
				validateOffset(args, description);
				optionalTankBoolean(args, "sneaking", description);
			}
			case "diagnostic.screenshot" -> {
				LazyOptionalRegressionAction.requireOnly(args, Set.of("name"), description);
				if (!tankString(args, "name", description).matches("[a-z0-9_-]{1,80}"))
					throw new IllegalArgumentException("Invalid screenshot name on " + description);
			}
			case "menu.clickPumpControl" -> {
				LazyOptionalRegressionAction.requireOnly(args, Set.of("control", "upgrade"), description);
				if (!Set.of("input", "handlers", "world", "hand").contains(tankString(args, "control", description)))
					throw new IllegalArgumentException("Unknown pump control on " + description);
				optionalTankInteger(args, "upgrade", false, description);
			}
			default -> throw new IllegalArgumentException("Unknown regression action " + id);
		}
	}

	private static String tankString(JsonObject args, String name, String description) {
		LazyOptionalRegressionAction.requireString(args, name, description);
		return args.get(name).getAsString();
	}
	private static void optionalTankString(JsonObject args, String name, String description) {
		if (args.has(name))
			tankString(args, name, description);
	}
	private static void optionalTankInteger(JsonObject args, String name, boolean positive, String description) {
		if (args.has(name)) {
			if (positive)
				LazyOptionalRegressionAction.requirePositiveInt(args, name, description);
			else
				LazyOptionalRegressionAction.optionalNonNegativeInt(args, name, description);
		}
	}
	private static void optionalTankBoolean(JsonObject args, String name, String description) {
		if (args.has(name) && (!args.get(name).isJsonPrimitive() || !args.getAsJsonPrimitive(name).isBoolean()))
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be boolean");
	}
	private static void validateOffset(JsonObject args, String description) {
		if (!args.has("offset"))
			return;
		if (!args.get("offset").isJsonArray())
			throw new IllegalArgumentException("Argument offset on " + description + " must be an [x, y, z] array");
		JsonArray offset = args.getAsJsonArray("offset");
		if (offset.size() != 3)
			throw new IllegalArgumentException("Argument offset on " + description + " must be an [x, y, z] array");
		for (int index = 0; index < 3; index++) {
			if (!offset.get(index).isJsonPrimitive() || !offset.get(index).getAsJsonPrimitive().isNumber())
				throw new IllegalArgumentException("Argument offset on " + description + " must contain integers");
			try {
				RegressionJsonNumbers.integer(offset.get(index));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Argument offset on " + description + " must contain integers", e);
			}
		}
	}
	private static RegressionAction lazyJeiTransferAction() {
		return new LazyOptionalRegressionAction("recipeviewer.transferRecipe", "1", Set.of("minecraft:client", "jei"),
				"net.p3pp3rf1y.devclientautomation.regression.actions.TransferRecipeAction", (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of("recipeResourceID", "maxTransfer"), description);
					LazyOptionalRegressionAction.requireString(args, "recipeResourceID", description);
					if (!RECIPE_RESOURCE_LOCATION.matcher(args.get("recipeResourceID").getAsString()).matches())
						throw new IllegalArgumentException("Argument recipeResourceID on " + description + " must be a resource location");
					if (args.has("maxTransfer") && (!args.get("maxTransfer").isJsonPrimitive() || !args.getAsJsonPrimitive("maxTransfer").isBoolean()))
						throw new IllegalArgumentException("Argument maxTransfer on " + description + " must be boolean");
				});
	}
	private static RegressionAction lazyCreateAction(String id, String capability, String implementation, Set<String> names, Set<String> strings,
			Set<String> positiveInts, Set<String> nonNegativeInts, Function<JsonObject, RegressionValueContract> values) {
		return new LazyOptionalRegressionAction(id, "1", Set.of("minecraft:client", capability),
				"net.p3pp3rf1y.devclientautomation.regression.actions." + implementation, (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, names, description);
					strings.forEach(name -> {
						LazyOptionalRegressionAction.requireString(args, name, description);
						if (name.equals("ref") || name.equals("saveAs")) {
							RegressionValueStore.requireName(args.get(name).getAsString());
						}
					});
					positiveInts.forEach(name -> LazyOptionalRegressionAction.requirePositiveInt(args, name, description));
					nonNegativeInts.forEach(name -> LazyOptionalRegressionAction.optionalNonNegativeInt(args, name, description));
				}, values);
	}
	private static RegressionAction lazyTierAction(String id, Set<String> dependencies, String implementation, String variant, Set<String> variants) {
		return new LazyOptionalRegressionAction(id, "2", dependencies, "net.p3pp3rf1y.devclientautomation.regression.actions." + implementation,
				(args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of(variant, "recipe", "linkage"), description);
					LazyOptionalRegressionAction.requireString(args, variant, description);
					String value = args.get(variant).getAsString();
					if (!variants.contains(value))
						throw new IllegalArgumentException("Argument " + variant + " on " + description + " must be one of " + variants);
					LazyOptionalRegressionAction.requireString(args, "recipe", description);
					LazyOptionalRegressionAction.requireString(args, "linkage", description);
					String recipe = args.get("recipe").getAsString();
					String linkage = args.get("linkage").getAsString();
					if (!Set.of("shaped", "shapeless").contains(recipe) || !Set.of("unlinked", "primary", "secondary").contains(linkage))
						throw new IllegalArgumentException("Invalid tier upgrade recipe arguments on " + description);
				});
	}
}
