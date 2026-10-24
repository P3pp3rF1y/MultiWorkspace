package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;
import net.p3pp3rf1y.sophisticatedstoragecreateintegration.storage.MountedSophisticatedStorage;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetMountedStorageSlotAction extends TypedRegressionAction<SetMountedStorageSlotAction.Arguments> {
	public record Arguments(String ref, String item, int count) {
	}

	public SetMountedStorageSlotAction() {
		super("fixture.create.setMountedSlot", "1", Set.of("minecraft:client", "sophisticatedstoragecreateintegration"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "item", "count"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)), requireString(args, "item", description),
				requirePositiveInt(args, "count", description));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOUNTED_STORAGE);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						MountedLinkedStorageHandle handle = context.values().require(arguments.ref(), MountedLinkedStorageHandle.class);
						if (!(ContraptionHelper.getMountedStorage(handle.requireServer(player),
								handle.localPos()) instanceof MountedSophisticatedStorage storage)) {
							throw new IllegalStateException("Expected mounted sophisticated storage is missing");
						}
						Item item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown item " + arguments.item()));
						storage.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(item, arguments.count()));
					});
				}
				return await(future);
			}
		};
	}
}
