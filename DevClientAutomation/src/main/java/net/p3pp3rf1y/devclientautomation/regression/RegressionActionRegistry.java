package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertBackpackAutomationExtractableAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertBackpackPickupAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertCanonicalBackpackTemplateAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertCanonicalCapturedMobsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertControllerLinkGroupsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertControllerLinkLineAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertControllerSharedGroupAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertDoubleChestAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertHeldBackpackContentsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertHeldBackpackFilterAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertInventoryCountAction;
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
import net.p3pp3rf1y.devclientautomation.regression.actions.ControlJukeboxPlaybackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CopyPlayerInventoryItemAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateBackpackInMainHandAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateLockToolAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateMainHandItemAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.DestroyMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.FillBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.HoldRecoveredMovingStorageInnerAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LazyOptionalRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LinkHeldBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LinkTargetStorageBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.OpenPlayerInventoryAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.OpenUpgradeTabAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.PickupPlacedBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.PlaceHeldBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.PlaceHeldBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbeBackpackFilterAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbeForgedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbeMovingStoragePickupAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ProbePackedPrimaryAction;
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
import net.p3pp3rf1y.devclientautomation.regression.actions.SetHeldBackpackUpgradeAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingJukeboxDiscAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStoragePackedAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStoragePresentationAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStorageSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetMovingStorageUpgradeAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetPlayerGameModeAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetPlayerInventorySlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetStorageBlockSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SpawnFixturePigAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SpawnItemEntityAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SpawnMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.StorageResourceCacheAction;
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

public final class RegressionActionRegistry {
	private final Map<String, RegressionAction> actions = new LinkedHashMap<>();

	public RegressionActionRegistry() {
		register(new ClickSortControlAction());
		register(new AssertSortModeAction());
		register(new ClickStorageSettingsAction());
		register(new ClickSettingsControlAction());
		register(lazyJeiAction());
		register(new AssertSettingsSelectionAction());
		register(new AssertLinkedSettingsAction());
		register(new SpawnFixturePigAction());
		register(new UseFixtureEntityAction());
		register(new AssertCanonicalCapturedMobsAction());
		register(new ClickCapturedMobAction());
		register(new UseHeldItemOnTargetAction());
		register(new StorageResourceCacheAction());
		register(new ReplaceLinkedStorageRootAction());
		register(new SetHeldBackpackUpgradeAction());
		register(new TransferStorageResourceAction());
		register(new AssertStorageResourcesAction());
		register(lazyTierAction("assert.storageTierUpgradeRecipe", Set.of("minecraft:client", "sophisticatedstorage"), "AssertStorageTierUpgradeRecipeAction",
				"form", Set.of("normal", "double")));
		register(lazyTierAction("assert.movingStorageTierUpgradeRecipe", Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"),
				"AssertMovingStorageTierUpgradeRecipeAction", "carrier", Set.of("boat", "minecart")));
		register(new ResetRegionAction());
		register(new ResetPlayerAction());
		register(new AssertPlayerInventoryBaselineAction());
		register(new CreateBackpackInMainHandAction());
		register(new BackpackTemplateCommandAction());
		register(new CreateMainHandItemAction());
		register(new CopyPlayerInventoryItemAction());
		register(new BreakTargetBlockAction());
		register(new SetBackpackStorageSlotAction());
		register(new LinkHeldBackpackAction());
		register(new LinkTargetStorageBlockAction());
		register(new ConfigureBackpackFilterAction());
		register(new ProbeBackpackFilterAction());
		register(new AssertHeldBackpackFilterAction());
		register(new ConfigureBackpackRestockAction());
		register(new ConfigureBackpackDepositAction());
		register(new UseHeldBackpackOnStorageAction());
		register(new AssertInventoryCountAction());
		register(new ConfigureBackpackPickupAction());
		register(new FillBackpackAction());
		register(new SpawnItemEntityAction());
		register(new AwaitItemEntityAction());
		register(new AssertBackpackPickupAction());
		register(new AssertRestockBackpackAction());
		register(new AssertRestockBarrelAction());
		register(new SetPlayerInventorySlotAction());
		register(new PlaceHeldBackpackAction());
		register(new AssertPlacedBackpackContentsAction());
		register(new AssertBackpackAutomationExtractableAction());
		register(new AssertHeldBackpackContentsAction());
		register(new PickupPlacedBackpackAction());
		register(new BreakPlacedBackpackAction());
		register(new PlaceHeldBlockAction());
		register(new AwaitBackpackBlockAction());
		register(new AwaitStorageBlockAction());
		register(new ConfigureStorageContentsFilterAction());
		register(new SetFixtureBlockAction());
		register(new ScanFixtureControllerAction());
		register(new BreakControllerLinkLineBarrelAction());
		register(new BreakControllerLinkGroupsBarrelAction());
		register(new UseControllerLinkToolAction());
		register(new AssertControllerLinkLineAction());
		register(new AssertControllerLinkGroupsAction());
		register(new SpawnMovingStorageAction());
		register(new DestroyMovingStorageAction());
		register(new RestoreMovingStorageAction());
		register(new ReloadMovingStorageAction());
		register(new HoldRecoveredMovingStorageInnerAction());
		register(lazyCreateAction("fixture.create.mountLinkedStorage", "sophisticatedstoragecreateintegration", "MountLinkedStorageAction", Set.of("saveAs"),
				Set.of("saveAs"), Set.of(), Set.of(),
				args -> RegressionValueContract.producing(args.get("saveAs").getAsString(), RegressionValueTypes.MOUNTED_STORAGE)));
		register(lazyCreateAction("fixture.create.mountBackpack", "sophisticatedbackpackscreateintegration", "MountBackpackOnCartAction", Set.of("saveAs"),
				Set.of("saveAs"), Set.of(), Set.of(), args -> RegressionValueContract.producing(args.get("saveAs").getAsString(), RegressionValueTypes.UUID)));
		register(lazyCreateAction("create.disassembleBackpack", "sophisticatedbackpackscreateintegration", "DisassembleCartBackpackAction", Set.of("ref"),
				Set.of("ref"), Set.of(), Set.of(), args -> RegressionValueContract.consuming(args.get("ref").getAsString(), RegressionValueTypes.UUID)));
		register(lazyCreateAction("fixture.create.setMountedBackpackSlot", "sophisticatedbackpackscreateintegration", "SetMountedBackpackSlotAction",
				Set.of("ref", "item", "count"), Set.of("ref", "item"), Set.of("count"), Set.of(),
				args -> RegressionValueContract.consuming(args.get("ref").getAsString(), RegressionValueTypes.UUID)));
		register(lazyCreateAction("fixture.create.setMountedSlot", "sophisticatedstoragecreateintegration", "SetMountedStorageSlotAction",
				Set.of("ref", "item", "count"), Set.of("ref", "item"), Set.of("count"), Set.of(),
				args -> RegressionValueContract.consuming(args.get("ref").getAsString(), RegressionValueTypes.MOUNTED_STORAGE)));
		register(new SetMovingStorageSlotAction());
		register(new SetMovingJukeboxDiscAction());
		register(new SetMovingStorageUpgradeAction());
		register(new SetMovingStoragePackedAction());
		register(new SetMovingStoragePresentationAction());
		register(new ProbeMovingStoragePickupAction());
		register(new ProbePackedPrimaryAction());
		register(new UseMovingStorageAction());
		register(new AssertLinkedMovingStorageAction());
		register(new AssertLinkedStorageBlockAction());
		register(new LazyOptionalRegressionAction("assert.createMountedLinkedStorage", "1", Set.of("minecraft:client", "sophisticatedstoragecreateintegration"),
				"net.p3pp3rf1y.devclientautomation.regression.actions.AssertMountedLinkedStorageAction", (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of("ref", "count", "checkPresentation"), description);
					LazyOptionalRegressionAction.requireString(args, "ref", description);
					RegressionValueStore.requireName(args.get("ref").getAsString());
					LazyOptionalRegressionAction.optionalNonNegativeInt(args, "count", description);
					if (args.has("checkPresentation")
							&& (!args.get("checkPresentation").isJsonPrimitive() || !args.getAsJsonPrimitive("checkPresentation").isBoolean())) {
						throw new IllegalArgumentException("Argument checkPresentation on " + description + " must be boolean");
					}
				}, args -> RegressionValueContract.consuming(args.get("ref").getAsString(), RegressionValueTypes.MOUNTED_STORAGE)));
		register(new CaptureLinkedMenuRevisionAction());
		register(new AssertLinkedMenuDeltaAction());
		register(new AssertLinkedMenuSettingsAction());
		register(new ControlJukeboxPlaybackAction());
		register(new AssertLinkedJukeboxPlaybackAction());
		register(new AssertMovingStoragePresentationAction());
		register(new AssertUnlinkedMovingStorageAction());
		register(new ProbeForgedMovingStorageAction());
		register(new CaptureNestedMovingStorageAction());
		register(new AssertNestedMovingStorageIdentityAction());
		register(new AssertMovingStorageCompatibilityProfileAction());
		register(new SetStorageBlockSlotAction());
		register(new ClearMainHandAction());
		register(new UseHeldItemAction());
		register(new UseTargetBlockAction());
		register(new AwaitMenuAction());
		register(new AssertMenuAction());
		register(new AssertMenuUpgradeLayoutAction());
		register(new AssertLinkedBackpackHostAction());
		register(new AssertLinkedItemContentsAction());
		register(new AssertCanonicalBackpackTemplateAction());
		register(new SendPlayerCommandAction());
		register(new AssertControllerSharedGroupAction());
		register(new ReloadFixtureControllerAction());
		register(new ReloadChunkBlockEntitiesAction());
		register(new AwaitPlayerCraftingResultAction());
		register(new AssertDoubleChestAction());
		register(new CreateLockToolAction());
		register(new OpenPlayerInventoryAction());
		register(new SetPlayerGameModeAction());
		register(new ClickPlayerInventorySlotAction());
		register(new ClickStorageMenuSlotAction());
		register(new ClickUpgradeSlotAction());
		register(new OpenUpgradeTabAction());
		register(new SetCraftingRefillAction());
		register(new AssertUpgradeSlotAction());
		register(new AssertMovingMenuPresentationAction());
		register(new ClickMenuSlotAction());
		register(new QuickMovePlayerInventorySlotAction());
		register(new QuickMoveBackpackStorageSlotAction());
		register(new CloseMenuAction());
		register(new AwaitMenuClosedAction());
	}

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

	private static RegressionAction lazyCreateAction(String id, String capability, String implementation, Set<String> names, Set<String> strings,
			Set<String> positiveInts, Set<String> nonNegativeInts, Function<JsonObject, RegressionValueContract> values) {
		return new LazyOptionalRegressionAction(id, "1", Set.of("minecraft:client", capability),
				"net.p3pp3rf1y.devclientautomation.regression.actions." + implementation, (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, names, description);
					strings.forEach(name -> requireCreateString(args, name, description));
					positiveInts.forEach(name -> LazyOptionalRegressionAction.requirePositiveInt(args, name, description));
					nonNegativeInts.forEach(name -> LazyOptionalRegressionAction.optionalNonNegativeInt(args, name, description));
				}, values);
	}

	private static void requireCreateString(JsonObject args, String name, String description) {
		LazyOptionalRegressionAction.requireString(args, name, description);
		if (name.equals("ref") || name.equals("saveAs")) {
			RegressionValueStore.requireName(args.get(name).getAsString());
		}
	}

	private static RegressionAction lazyTierAction(String id, Set<String> dependencies, String implementation, String variant, Set<String> variants) {
		return new LazyOptionalRegressionAction(id, "2", dependencies, "net.p3pp3rf1y.devclientautomation.regression.actions." + implementation,
				(args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of(variant, "recipe", "linkage"), description);
					LazyOptionalRegressionAction.requireString(args, variant, description);
					String value = args.get(variant).getAsString();
					if (!variants.contains(value)) {
						throw new IllegalArgumentException("Argument " + variant + " on " + description + " must be one of " + variants);
					}
					LazyOptionalRegressionAction.requireString(args, "recipe", description);
					LazyOptionalRegressionAction.requireString(args, "linkage", description);
					String recipe = args.get("recipe").getAsString();
					String linkage = args.get("linkage").getAsString();
					if (!Set.of("shaped", "shapeless").contains(recipe) || !Set.of("unlinked", "primary", "secondary").contains(linkage)) {
						throw new IllegalArgumentException("Invalid tier upgrade recipe arguments on " + description);
					}
				});
	}

}
