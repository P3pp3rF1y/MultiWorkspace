package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Observes only the published UUID on the server thread. Normal world ticks perform pickup. */
public final class AwaitItemEntityAction extends TypedRegressionAction<AwaitItemEntityAction.Arguments> {
	public record Arguments(String ref, String state, int timeoutTicks) {
	}

	public AwaitItemEntityAction() {
		super("await.itemEntity", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "state", "timeoutTicks"), description);
		String state = requireString(args, "state", description);
		if (!Set.of("present", "absent").contains(state)) {
			throw new IllegalArgumentException("Expected present/absent item entity state on " + description);
		}
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)), state,
				optionalPositiveInt(args, "timeoutTicks", 100, description));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.ITEM_ENTITY);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> observation;
			private boolean matched;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + arguments.timeoutTicks();
				}
				if (observation == null) {
					observation = context.onServer(player -> {
						ItemEntityHandle handle = context.values().require(arguments.ref(), ItemEntityHandle.class);
						String dimension = player.level().dimension().identifier().toString();
						if (!dimension.equals(handle.dimension())) {
							throw new IllegalStateException(
									"Item entity " + handle.uuid() + " belongs to " + handle.dimension() + ", player is in " + dimension);
						}
						var entity = player.level().getEntity(handle.uuid());
						boolean absent = entity == null || entity.isRemoved() || entity instanceof ItemEntity itemEntity && itemEntity.getItem().isEmpty();
						Object previous = context.getDiagnostic("itemEntity-" + arguments.ref());
						Object lastPosition = previous instanceof Map<?, ?> snapshot ? snapshot.get("lastPosition") : Map.of();
						if (!absent) {
							if (!(entity instanceof ItemEntity itemEntity)) {
								throw new IllegalStateException("UUID " + handle.uuid() + " is not an item entity: " + entity.getType());
							}
							handle.validate(dimension, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
									BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()).toString(), itemEntity.getItem().getCount());
							lastPosition = Map.of("x", entity.getX(), "y", entity.getY(), "z", entity.getZ());
						}
						context.putDiagnostic("itemEntity-" + arguments.ref(), Map.of("identity", handle, "state", absent ? "absent" : "present",
								"lastPosition", lastPosition == null ? Map.of() : lastPosition));
						matched = arguments.state().equals("absent") == absent;
						observe(arguments.state(), absent ? "absent" : "present");
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress result = await(observation);
				if (result != RegressionActionProgress.PASSED) {
					return result;
				}
				if (matched) {
					return RegressionActionProgress.PASSED;
				}
				if (context.minecraft().player.tickCount >= deadline) {
					return fail(RegressionActionFailure.Kind.TIMEOUT,
							"Exact item entity " + arguments.ref() + " did not become " + arguments.state() + " within " + arguments.timeoutTicks() + " ticks");
				}
				observation = null;
				return RegressionActionProgress.WAITING;
			}
		};
	}
}
