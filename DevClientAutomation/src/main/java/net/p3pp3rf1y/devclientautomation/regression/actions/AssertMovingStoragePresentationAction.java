package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertMovingStoragePresentationAction extends TypedRegressionAction<AssertMovingStoragePresentationAction.Arguments> {
	public record Arguments(String primaryRef, String secondaryRef) {
	}

	public AssertMovingStoragePresentationAction() {
		super("assert.movingPresentation", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("primaryRef", "secondaryRef"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "primaryRef", description)),
				RegressionValueStore.requireName(requireString(args, "secondaryRef", description)));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> serverAssertion;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (serverAssertion == null) {
					serverAssertion = context.onServer(player -> {
						IMovingStorageEntity primary = (IMovingStorageEntity) context.values().require(arguments.primaryRef(), MovingStorageEntityHandle.class)
								.requireServer(player);
						IMovingStorageEntity secondary = (IMovingStorageEntity) context.values()
								.require(arguments.secondaryRef(), MovingStorageEntityHandle.class).requireServer(player);
						verify(primary.getStorageHolder().getStorageWrapper());
						verify(secondary.getStorageHolder().getStorageWrapper());
						var projected = NBTHelper.getCompound(secondary.getStorageItem(), StorageWrapper.RENDER_INFO_TAG);
						if (projected.isEmpty() || !projected.get().equals(primary.getStorageHolder().getStorageWrapper().getRenderInfo().getNbt())) {
							throw new IllegalStateException("Secondary moving carrier did not project canonical display state");
						}
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress server = await(serverAssertion);
				if (server != RegressionActionProgress.PASSED) {
					return server;
				}
				var secondary = context.values().require(arguments.secondaryRef(), MovingStorageEntityHandle.class).findClient(context.minecraft());
				if (secondary instanceof IMovingStorageEntity moving && context.minecraft().player.containerMenu instanceof MovingStorageContainerMenu<?> menu
						&& menu.getStorageEntity().filter(entity -> entity == secondary).isPresent()) {
					IStorageWrapper wrapper = menu.getStorageWrapper();
					if (hasPresentation(wrapper) && NBTHelper.hasTag(moving.getStorageItem(), StorageWrapper.RENDER_INFO_TAG)) {
						return RegressionActionProgress.PASSED;
					}
				}
				return context.minecraft().player.tickCount >= deadline
						? fail("Client secondary did not synchronize upgrades, settings and display")
						: RegressionActionProgress.WAITING;
			}
		};
	}

	private static void verify(IStorageWrapper wrapper) {
		if (!hasPresentation(wrapper)) {
			throw new IllegalStateException("Moving endpoint does not see canonical upgrade, memory and display settings");
		}
	}

	static boolean hasPresentation(IStorageWrapper wrapper) {
		return BuiltInRegistries.ITEM.getKey(wrapper.getUpgradeHandler().getStackInSlot(0).getItem())
				.equals(new ResourceLocation("sophisticatedstorage", "pickup_upgrade"))
				&& wrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).isSlotSelected(0)
				&& wrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).getSlots().contains(0);
	}
}
