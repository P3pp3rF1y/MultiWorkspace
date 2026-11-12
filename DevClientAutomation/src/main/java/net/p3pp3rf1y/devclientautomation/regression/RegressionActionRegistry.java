package net.p3pp3rf1y.devclientautomation.regression;

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
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertHeldBackpackStateAction;
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
import net.p3pp3rf1y.devclientautomation.regression.actions.CaptureHeldBackpackUuidAction;
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
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickVanillaMenuSlotAction;
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
import net.p3pp3rf1y.devclientautomation.regression.actions.SelectHotbarSlotAction;
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
	// Matches Identifier.parse's namespace and path character sets without loading Minecraft in the JVM-only schema binder.
	private static final Pattern RECIPE_RESOURCE_LOCATION = Pattern.compile("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+");
	private final Map<String, RegressionAction> actions = new LinkedHashMap<>();

	public RegressionActionRegistry() {
		register(lazyTankAction("assert.tank", "AssertTankAction",
				Set.of("offset", "present", "amount", "capacity", "height", "upgrades", "tier", "tankDrops", "droppedFluid", "droppedHeight", "droppedUpgrades",
						"menu", "emission", "light", "tabs", "switches", "cursorItem", "cursorCount", "containerSlot", "containerItem", "containerCount",
						"droppedFluidType"),
				(args, description) -> {
					validateOffset(args, description);
					for (String name : Set.of("amount", "capacity", "height", "upgrades", "tankDrops", "droppedFluid", "droppedHeight", "droppedUpgrades",
							"emission", "light", "tabs", "switches", "cursorCount", "containerSlot", "containerCount"))
						LazyOptionalRegressionAction.optionalNonNegativeInt(args, name, description);
					for (String name : Set.of("present", "menu"))
						validateBoolean(args, name, description);
					for (String name : Set.of("tier", "cursorItem", "containerItem", "droppedFluidType"))
						validateOptionalString(args, name, description);
					if (args.has("containerSlot") && args.get("containerSlot").getAsInt() > 3)
						throw new IllegalArgumentException("Tank container slot must be from 0 through 3");
				}));
		register(lazyTankAction("player.useTankSegment", "UseTankSegmentAction", Set.of("offset", "face"), (args, description) -> {
			validateOffset(args, description);
			validateFace(args, description);
		}));
		register(lazyTankAction("capability.tankFluid", "TankFluidTransferAction", Set.of("offset", "face", "fluid", "amount", "expected", "drain", "simulate"),
				(args, description) -> {
					validateOffset(args, description);
					validateFace(args, description);
					validateOptionalString(args, "fluid", description);
					LazyOptionalRegressionAction.requirePositiveInt(args, "amount", description);
					LazyOptionalRegressionAction.optionalNonNegativeInt(args, "expected", description);
					validateBoolean(args, "drain", description);
					validateBoolean(args, "simulate", description);
				}));
		register(lazyTankAction("fixture.tank.recoverDrop", "RecoverTankDropAction", Set.of(), (args, description) -> {
		}));
		register(lazyTankAction("world.removeTargetBlock", "RemoveTargetBlockAction", Set.of("offset", "mode"), (args, description) -> {
			validateOffset(args, description);
			validateOptionalString(args, "mode", description);
			if (args.has("mode") && !Set.of("destroy", "setAir", "explosion").contains(args.get("mode").getAsString()))
				throw new IllegalArgumentException("Invalid removal mode on " + description);
		}));
		register(lazyTankAction("fixture.tank.spawnExperience", "SpawnTankExperienceAction", Set.of("experience"), (args, description) -> {
			if (args.has("experience")) {
				LazyOptionalRegressionAction.requirePositiveInt(args, "experience", description);
				if (args.get("experience").getAsInt() > 100)
					throw new IllegalArgumentException("Experience exceeds the bounded fixture on " + description);
			}
		}));
		register(lazyTankAction("fixture.tank.reload", "ReloadTankAction", Set.of("offset", "sneaking"), (args, description) -> {
			validateOffset(args, description);
			validateBoolean(args, "sneaking", description);
		}));
		register(new LazyOptionalRegressionAction("diagnostic.screenshot", "1", Set.of(RegressionAction.CLIENT_CAPABILITY),
				"net.p3pp3rf1y.devclientautomation.regression.actions.CaptureScreenshotAction", (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of("name"), description);
					LazyOptionalRegressionAction.requireString(args, "name", description);
					if (!args.get("name").getAsString().matches("[a-z0-9_-]{1,80}"))
						throw new IllegalArgumentException("Invalid screenshot name on " + description);
				}));
		register(new LazyOptionalRegressionAction("menu.clickPumpControl", "1", Set.of(RegressionAction.CLIENT_CAPABILITY, "sophisticatedcore"),
				"net.p3pp3rf1y.devclientautomation.regression.actions.ClickPumpControlAction", (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of("upgrade", "control"), description);
					LazyOptionalRegressionAction.requireString(args, "control", description);
					if (!Set.of("input", "handlers", "world", "hand").contains(args.get("control").getAsString()))
						throw new IllegalArgumentException("Unknown pump control on " + description);
					LazyOptionalRegressionAction.optionalNonNegativeInt(args, "upgrade", description);
				}));
		register(lazyTankAction("menu.clickTankGauge", "ClickTankGaugeAction", Set.of(), (args, description) -> {
		}));
		register(new ClickSortControlAction());
		register(new AssertSortModeAction());
		register(new ClickStorageSettingsAction());
		register(new ClickSettingsControlAction());
		register(lazyJeiAction());
		register(lazyJeiTransferAction());
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
		register(new AssertPlayerHealthAction());
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
		register(new AssertPlacedBackpackContentsAction());
		register(new AssertBackpackAutomationExtractableAction());
		register(new AssertHeldBackpackContentsAction());
		register(new AssertHeldBackpackStateAction());
		register(new CaptureHeldBackpackUuidAction());
		register(new PickupPlacedBackpackAction());
		register(new BreakPlacedBackpackAction());
		register(new PlaceHeldBlockAction());
		register(new AwaitBackpackBlockAction());
		register(new AwaitStorageBlockAction());
		register(new FixtureCompressionAction());
		register(new AssertCompressionAction());
		register(new ExerciseCompressionAutomationAction());
		register(new ExerciseCompressionFrontAction());
		// The 26.3 plain JVM test classpath lacks NeoForge's transaction context; load the profile only in the client.
		register(new LazyOptionalRegressionAction("profile.storage.compression", "1", Set.of("minecraft:client", "sophisticatedstorage"),
				"net.p3pp3rf1y.devclientautomation.regression.actions.ProfileCompressionAction", (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of("worldBatch"), description);
					if (args.has("worldBatch") && (!args.get("worldBatch").isJsonPrimitive() || !args.getAsJsonPrimitive("worldBatch").isBoolean())) {
						throw new IllegalArgumentException("Argument worldBatch on " + description + " must be boolean");
					}
				}));
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
		register(new AssertHeldBackpackJukeboxPlaybackAction());
		register(new ControlHeldBackpackJukeboxPlaybackAction());
		register(new ObserveHeldBackpackJukeboxSoundAction());
		register(new KeepInventoryDeathRespawnAction());
		register(new LethalDamageAction());
		register(new SetHeldBackpackJukeboxDiscAction());
		register(new AssertLinkedJukeboxPlaybackAction());
		register(new AssertMovingStoragePresentationAction());
		register(new AssertUnlinkedMovingStorageAction());
		register(new ProbeForgedMovingStorageAction());
		register(new CaptureNestedMovingStorageAction());
		register(new AssertNestedMovingStorageIdentityAction());
		register(new AssertMovingStorageCompatibilityProfileAction());
		register(new SetStorageBlockSlotAction());
		register(new SetStorageUpgradeAction());
		register(new ClearMainHandAction());
		register(new SelectHotbarSlotAction());
		register(new UseHeldItemAction());
		register(new UseTargetBlockAction());
		register(new AwaitMenuAction());
		register(new AwaitVanillaMenuAction());
		register(new OpenInventoryInteractionMenuAction());
		register(new PressInventoryInteractionKeybindAction());
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
		register(new ClickVanillaMenuSlotAction());
		register(new ClickStorageMenuSlotAction());
		register(new ClickUpgradeSlotAction());
		register(new OpenUpgradeTabAction());
		register(new SetUpgradeTabAction());
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

	private static RegressionAction lazyTankAction(String id, String implementation, Set<String> names,
			java.util.function.BiConsumer<JsonObject, String> validate) {
		return new LazyOptionalRegressionAction(id, "1", Set.of(RegressionAction.CLIENT_CAPABILITY, "sophisticatedstorage"),
				"net.p3pp3rf1y.devclientautomation.regression.actions." + implementation, (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, names, description);
					validate.accept(args, description);
				});
	}

	private static void validateOptionalString(JsonObject args, String name, String description) {
		if (args.has(name))
			LazyOptionalRegressionAction.requireString(args, name, description);
	}

	private static void validateBoolean(JsonObject args, String name, String description) {
		if (args.has(name) && (!args.get(name).isJsonPrimitive() || !args.getAsJsonPrimitive(name).isBoolean()))
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be boolean");
	}

	private static void validateFace(JsonObject args, String description) {
		validateOptionalString(args, "face", description);
		if (args.has("face") && !Set.of("up", "down", "north", "south", "east", "west").contains(args.get("face").getAsString()))
			throw new IllegalArgumentException("Invalid face on " + description);
	}

	private static void validateOffset(JsonObject args, String description) {
		if (!args.has("offset"))
			return;
		if (!args.get("offset").isJsonArray() || args.getAsJsonArray("offset").size() != 3)
			throw new IllegalArgumentException("Argument offset on " + description + " must be an [x, y, z] array");
		for (var coordinate : args.getAsJsonArray("offset")) {
			if (!coordinate.isJsonPrimitive() || !coordinate.getAsJsonPrimitive().isNumber())
				throw new IllegalArgumentException("Argument offset on " + description + " must contain integers");
			try {
				Integer.parseInt(coordinate.getAsString());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Argument offset on " + description + " must contain integers", e);
			}
		}
	}

	private static RegressionAction lazyJeiAction() {
		return new LazyOptionalRegressionAction("recipeviewer.dropSettingsGhost", "1", Set.of("minecraft:client", "jei"),
				"net.p3pp3rf1y.devclientautomation.regression.actions.DropSettingsGhostAction", (args, description) -> {
					LazyOptionalRegressionAction.requireOnly(args, Set.of("slot"), description);
					LazyOptionalRegressionAction.optionalNonNegativeInt(args, "slot", description);
				});
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
