package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CaptureNestedMovingStorageAction extends TypedRegressionAction<CaptureNestedMovingStorageAction.Arguments> {
	public record Arguments(String ref, String saveAs) {
	}

	public CaptureNestedMovingStorageAction() {
		super("fixture.moving.captureNestedLinkedCarrier", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "saveAs"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "saveAs", description)));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE)
				.and(RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.NESTED_STORAGE));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						IMovingStorageEntity moving = (IMovingStorageEntity) context.values().require(arguments.ref(), MovingStorageEntityHandle.class)
								.requireServer(player);
						ItemStack inner = moving.getStorageItem();
						var endpoint = inner.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
						if (endpoint == null) {
							throw new IllegalStateException("Cannot nest an unlinked moving carrier");
						}
						ItemStack outer = MovingStorageItem.createWithStorage(new ItemStack(ModItems.STORAGE_BOAT.get()), inner).copy();
						context.values().put(arguments.saveAs(), NestedMovingStorageSnapshot.class, NestedMovingStorageSnapshot.of(outer, endpoint.toString()));
					});
				}
				return await(future);
			}
		};
	}
}
