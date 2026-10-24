package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertUnlinkedMovingStorageAction extends TypedRegressionAction<String> {
	public AssertUnlinkedMovingStorageAction() {
		super("assert.movingUnlinked", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}
	@Override
	protected String parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref"), description);
		return RegressionValueStore.requireName(requireString(args, "ref", description));
	}
	@Override
	protected RegressionValueContract values(String ref) {
		return RegressionValueContract.consuming(ref, RegressionValueTypes.MOVING_STORAGE);
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, String ref) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						IMovingStorageEntity moving = (IMovingStorageEntity) context.values().require(ref, MovingStorageEntityHandle.class)
								.requireServer(player);
						if (LinkedStorageStackData.getEndpoint(moving.getStorageItem()) != null) {
							throw RegressionActionFailure.assertion("Unsupported moving storage unexpectedly acquired a linked endpoint");
						}
					});
				}
				return await(future);
			}
		};
	}
}
