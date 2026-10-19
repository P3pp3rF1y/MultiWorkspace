package net.p3pp3rf1y.devclientautomation.regression.actions;
import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
public final class SetMovingStoragePackedAction extends TypedRegressionAction<SetMovingStoragePackedAction.Arguments> {
	public record Arguments(String ref, boolean packed) {
	}
	public SetMovingStoragePackedAction() {
		super("moving.setPacked", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "packed"), description);
		if (!args.has("packed") || !args.get("packed").isJsonPrimitive() || !args.get("packed").getAsJsonPrimitive().isBoolean())
			throw new IllegalArgumentException("Argument packed on " + description + " must be a boolean");
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)), args.get("packed").getAsBoolean());
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
			if (future == null)
				future = context.onServer(player -> {
					IMovingStorageEntity moving = (IMovingStorageEntity) context.values().require(arguments.ref(), MovingStorageEntityHandle.class)
							.requireServer(player);
					var stack = moving.getStorageItem().copy();
					var endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
					if (endpoint == null
							|| !LinkedStorageGroupsSavedData.get(player.level()).manager().isEndpointMember(endpoint.groupId(), endpoint.endpointId()))
						throw new IllegalStateException("Only a registered linked endpoint can be packed by this fixture");
					WoodStorageBlockItem.setPacked(stack, arguments.packed());
					moving.setStorageItem(stack);
					if (moving.getStorageItem().has(ModCoreDataComponents.STORAGE_UUID)
							|| WoodStorageBlockItem.isPacked(moving.getStorageItem()) != arguments.packed())
						throw new IllegalStateException("Moving linked endpoint failed to update its packed state without an ordinary sidecar");
				});
			return await(future);
		}
	}
}
