package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertLinkedMovingStorageAction extends TypedRegressionAction<AssertLinkedMovingStorageAction.Arguments> {
	public record Arguments(String ref, boolean primary, int count, String hostItem, int handCount) {
	}
	public AssertLinkedMovingStorageAction() {
		super("assert.movingLinked", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "primary", "count", "hostItem", "handCount"), description);
		if (args.has("primary") && (!args.get("primary").isJsonPrimitive() || !args.get("primary").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument primary on " + description + " must be a boolean");
		}
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)),
				args.has("primary") && args.get("primary").getAsBoolean(), optionalNonNegativeInt(args, "count", 0, description),
				optionalString(args, "hostItem", "", description), args.has("handCount") ? optionalNonNegativeInt(args, "handCount", 0, description) : -1);
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
						var inner = moving.getStorageItem();
						var endpoint = LinkedStorageStackData.getEndpoint(inner);
						var manager = LinkedStorageGroupsSavedData.get(player.serverLevel()).manager();
						if (endpoint == null || NBTHelper.hasTag(inner, StorageWrapper.UUID_TAG)
								|| !manager.isEndpointMember(endpoint.groupId(), endpoint.endpointId())) {
							throw new IllegalStateException(
									"Moving carrier does not have exactly one registered linked identity without ordinary storage UUID");
						}
						if (arguments.primary() != manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())) {
							throw new IllegalStateException("Moving endpoint has unexpected primary role");
						}
						if (!arguments.hostItem().isEmpty() && arguments.primary()
								&& !BuiltInRegistries.ITEM.getKey(inner.getItem()).equals(new ResourceLocation(arguments.hostItem()))) {
							throw new IllegalStateException("Primary carrier has unexpected tier");
						}
						if (arguments.handCount() >= 0 && player.getMainHandItem().getCount() != arguments.handCount()) {
							throw new IllegalStateException("Upgrade consumed an unexpected number of items");
						}
						if (context.getDiagnostic("linkedGroup") instanceof String groupId && !groupId.equals(endpoint.groupId().toString())) {
							throw new IllegalStateException("Moving endpoints do not share a group");
						}
						int count = moving.getStorageHolder().getStorageWrapper().getInventoryHandler().getStackInSlot(0).getCount();
						if (count != arguments.count()) {
							throw new IllegalStateException("Canonical moving slot 0 has " + count + " items, expected " + arguments.count());
						}
						context.putDiagnostic("linkedGroup", endpoint.groupId().toString());
						context.putDiagnostic("linkedEndpoint", endpoint.endpointId().toString());
					});
				}
				return await(future);
			}
		};
	}
}
