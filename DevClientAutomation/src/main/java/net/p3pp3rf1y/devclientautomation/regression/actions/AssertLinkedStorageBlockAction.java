package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertLinkedStorageBlockAction extends TypedRegressionAction<AssertLinkedStorageBlockAction.Arguments> {
	public record Arguments(int count, boolean presentation) {
	}
	public AssertLinkedStorageBlockAction() {
		super("assert.linkedStorageBlock", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("count", "presentation"), description);
		if (args.has("presentation") && (!args.get("presentation").isJsonPrimitive() || !args.get("presentation").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument presentation on " + description + " must be a boolean");
		}
		return new Arguments(optionalNonNegativeInt(args, "count", 0, description), args.has("presentation") && args.get("presentation").getAsBoolean());
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}
	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> future;
		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}
		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					ServerLevel level = (ServerLevel) player.level();
					if (!(level.getBlockEntity(context.target()) instanceof StorageBlockEntity block) || !block.isLinkedStorage()) {
						throw new IllegalStateException("Expected a placed linked storage endpoint");
					}
					var endpoint = block.getLinkedStorageEndpointData();
					var manager = LinkedStorageGroupsSavedData.get(level).manager();
					context.putDiagnostic("placedEndpoint", String.valueOf(endpoint));
					context.putDiagnostic("placedEndpointRegistered", endpoint != null && manager.isEndpointMember(endpoint.groupId(), endpoint.endpointId()));
					if (endpoint == null || !manager.isEndpointMember(endpoint.groupId(), endpoint.endpointId())
							|| !endpoint.groupId().toString().equals(context.getDiagnostic("linkedGroup"))) {
						throw new IllegalStateException("Placed storage lost its registered moving endpoint identity");
					}
					if (block.getStorageWrapper().getInventoryHandler().getStackInSlot(0).getCount() != arguments.count()) {
						throw new IllegalStateException("Placed storage did not resolve canonical moving contents");
					}
					if (arguments.presentation() && !AssertMovingStoragePresentationAction.hasPresentation(block.getStorageWrapper())) {
						throw new IllegalStateException("Placed storage did not resolve canonical moving upgrades, memory and display settings");
					}
				});
			}
			return await(future);
		}
	}
}
