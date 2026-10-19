package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertBackpackAutomationExtractableAction;
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
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertStorageResourcesAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertUnlinkedMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AwaitBackpackBlockAction;
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
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickStorageMenuSlotAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickStorageSettingsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CloseMenuAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ConfigureBackpackDepositAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ConfigureBackpackFilterAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ConfigureBackpackRestockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ConfigureStorageContentsFilterAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ControlJukeboxPlaybackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CopyPlayerInventoryItemAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateActionArgumentSchemas;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateBackpackInMainHandAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateLockToolAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateMainHandItemAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.DestroyMovingStorageAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.HoldRecoveredMovingStorageInnerAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LazyRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LinkHeldBackpackAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LinkTargetStorageBlockAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.OpenPlayerInventoryAction;
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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class RegressionActionRegistry {
	private final Map<String, RegressionAction> actions = new LinkedHashMap<>();

	public RegressionActionRegistry() {
		this(() -> instantiateCreateAction("net.p3pp3rf1y.devclientautomation.regression.actions.MountLinkedStorageAction"),
				() -> instantiateCreateAction("net.p3pp3rf1y.devclientautomation.regression.actions.SetMountedStorageSlotAction"),
				() -> instantiateCreateAction("net.p3pp3rf1y.devclientautomation.regression.actions.AssertMountedLinkedStorageAction"));
	}

	RegressionActionRegistry(Supplier<? extends RegressionAction> mountActionFactory, Supplier<? extends RegressionAction> setMountedSlotActionFactory,
			Supplier<? extends RegressionAction> assertMountedActionFactory) {
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
		registerCreateActions(mountActionFactory, setMountedSlotActionFactory, assertMountedActionFactory);
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
		registerLazyCreateAction("assert.createMountedLinkedStorage", assertMountedActionFactory, CreateActionArgumentSchemas::validateAssertMounted);
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
		register(new AssertMovingMenuPresentationAction());
		register(new ClickMenuSlotAction());
		register(new QuickMovePlayerInventorySlotAction());
		register(new QuickMoveBackpackStorageSlotAction());
		register(new CloseMenuAction());
		register(new AwaitMenuClosedAction());
	}

	private void registerCreateActions(Supplier<? extends RegressionAction> mountActionFactory,
			Supplier<? extends RegressionAction> setMountedSlotActionFactory, Supplier<? extends RegressionAction> assertMountedActionFactory) {
		registerLazyCreateAction("fixture.create.mountLinkedStorage", mountActionFactory, CreateActionArgumentSchemas::validateMount);
		registerLazyBackpackCreateAction("fixture.create.mountBackpack", "MountBackpackOnCartAction", CreateActionArgumentSchemas::validateMount);
		registerLazyBackpackCreateAction("create.disassembleBackpack", "DisassembleCartBackpackAction", CreateActionArgumentSchemas::validateDisassemble);
		registerLazyBackpackCreateAction("fixture.create.setMountedBackpackSlot", "SetMountedBackpackSlotAction", CreateActionArgumentSchemas::validateSetSlot);
		registerLazyCreateAction("fixture.create.setMountedSlot", setMountedSlotActionFactory, CreateActionArgumentSchemas::validateSetSlot);
	}

	private void registerLazyBackpackCreateAction(String actionId, String className, java.util.function.BiConsumer<JsonObject, String> argumentValidator) {
		register(new LazyRegressionAction(new RegressionActionMetadata(actionId, "1", Set.of("minecraft:client", "sophisticatedbackpackscreateintegration")),
				() -> instantiateCreateAction("net.p3pp3rf1y.devclientautomation.regression.actions." + className), argumentValidator));
	}

	private void registerLazyCreateAction(String actionId, Supplier<? extends RegressionAction> actionFactory,
			java.util.function.BiConsumer<JsonObject, String> argumentValidator) {
		register(new LazyRegressionAction(new RegressionActionMetadata(actionId, "1", Set.of("minecraft:client", "sophisticatedstoragecreateintegration")),
				actionFactory, argumentValidator));
	}

	static RegressionAction instantiateCreateAction(String className) {
		try {
			return Class.forName(className).asSubclass(RegressionAction.class).getDeclaredConstructor().newInstance();
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof LinkageError linkageError) {
				throw linkageError;
			}
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException("Could not load optional Create regression action " + className, e);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not load optional Create regression action " + className, e);
		}
	}

	private static RegressionAction lazyJeiAction() {
		return new LazyRegressionAction(new RegressionActionMetadata("recipeviewer.dropSettingsGhost", "1", Set.of("minecraft:client", "jei")),
				() -> instantiateCreateAction("net.p3pp3rf1y.devclientautomation.regression.actions.DropSettingsGhostAction"), (args, description) -> {
					CreateActionArgumentSchemas.validateOptionalSlot(args, description);
				});
	}

	private static RegressionAction lazyTierAction(String id, Set<String> dependencies, String implementation, String variant, Set<String> variants) {
		return new LazyRegressionAction(new RegressionActionMetadata(id, "2", dependencies),
				() -> instantiateCreateAction("net.p3pp3rf1y.devclientautomation.regression.actions." + implementation),
				(args, description) -> CreateActionArgumentSchemas.validateTierRecipe(args, description, variant, variants));
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
}
