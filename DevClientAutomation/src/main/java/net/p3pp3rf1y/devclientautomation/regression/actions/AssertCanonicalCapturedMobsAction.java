package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AssertCanonicalCapturedMobsAction extends TypedRegressionAction<AssertCanonicalCapturedMobsAction.Arguments> {
	public record Arguments(int count, boolean render) {
	}
	public AssertCanonicalCapturedMobsAction() {
		super("assert.canonicalCapturedMobs", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("count", "render"), description);
		if (args.has("render") && (!args.get("render").isJsonPrimitive() || !args.getAsJsonPrimitive("render").isBoolean())) {
			throw new IllegalArgumentException(description + " render must be boolean");
		}
		return new Arguments(optionalNonNegativeInt(args, "count", 0, description), args.has("render") && args.get("render").getAsBoolean());
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						UUID groupId = player.containerMenu instanceof StorageContainerMenuBase<?> menu && menu.getLinkedStorageGroupId().isPresent()
								? menu.getLinkedStorageGroupId().orElseThrow()
								: Optional.ofNullable(player.getMainHandItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)).orElseThrow().groupId();
						IBackpackWrapper wrapper = (IBackpackWrapper) LinkedStorageGroupsSavedData.get(player.serverLevel()).manager()
								.resolveVirtualHost(groupId).orElseThrow();
						if (MobCatcherStorage.getCapturedMobs(wrapper).size() != arguments.count()) {
							throw new IllegalStateException("Canonical captured mob count differs: actual=" + MobCatcherStorage.getCapturedMobs(wrapper).size()
									+ ", wrapper=" + wrapper.getClass().getName() + ", upgrades=" + wrapper.getUpgradeHandler().getStackInSlot(0) + ", player="
									+ player.position() + ", catcher=" + MobCatcherHandler.getBestUpgrade(wrapper).isPresent() + ", primaryProcessor="
									+ BackpackLinkedStorageResolver.resolveForGlobalUpgradeProcessing(player.serverLevel(), player.getMainHandItem()).getClass()
											.getName());
						}
						if (BackpackStorage.get().getBackpackContents(groupId).isPresent()) {
							throw new IllegalStateException("Mob data leaked into ordinary group-UUID backend");
						}
					});
				}
				RegressionActionProgress result = await(future);
				if (result != RegressionActionProgress.PASSED) {
					return result;
				}
				if (context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu
						&& menu.getStorageWrapper() instanceof IBackpackWrapper wrapper
						&& MobCatcherStorage.getCapturedMobs(wrapper).size() != arguments.count()) {
					return fail("Open client menu did not refresh captured mobs");
				}
				if (arguments.render()) {
					if (!(context.minecraft().screen instanceof StorageScreenBase<?> screen)) {
						return fail("Storage screen is not open for captured mob rendering");
					}
					Optional<IBackpackWrapper> host = screen.getMenu().getStorageWrapper().getUpgradeHandler().getTypeWrappers(MobCatcherUpgradeItem.TYPE)
							.stream().map(MobCatcherUpgradeWrapper::getBackpackWrapper).flatMap(Optional::stream).findFirst();
					if (host.isEmpty() || MobCatcherStorage.getCapturedMobs(host.get()).size() != arguments.count()) {
						return fail("Secondary menu did not expose canonical captured mobs");
					}
					Map<UpgradeType<?>, UpgradeInventoryControlBase> controls = UpgradeGuiManager.getStorageUpgradeInventoryControls(screen);
					for (CapturedMob mob : MobCatcherStorage.getCapturedMobs(host.get())) {
						if (controls.values().stream().noneMatch(control -> control.replacesSlotRender(mob.slot()))) {
							return fail("Captured mob renderer does not replace its occupied slot");
						}
					}
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
