package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SpawnItemEntityAction extends TypedRegressionAction<SpawnItemEntityAction.Arguments> {
	public record Arguments(String saveAs, String item, int count, int offsetX, int pickupDelay) {
	}

	public SpawnItemEntityAction() {
		super("fixture.itemEntity.spawn", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("saveAs", "item", "count", "offsetX", "pickupDelay"), description);
		int count = optionalPositiveInt(args, "count", 1, description);
		int offset = optionalNonNegativeInt(args, "offsetX", 0, description);
		int delay = optionalNonNegativeInt(args, "pickupDelay", 0, description);
		if (count > 64 || offset > 3 || delay > 32767) {
			throw new IllegalArgumentException("Item entity count/offset/delay outside fixture bounds on " + description);
		}
		return new Arguments(RegressionValueStore.requireName(requireString(args, "saveAs", description)), requireString(args, "item", description), count,
				offset, delay);
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.ITEM_ENTITY);
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
						var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown spawned item " + arguments.item()));
						ItemEntity entity = new ItemEntity(player.serverLevel(), player.getX() + arguments.offsetX(), player.getY() + 0.25, player.getZ(),
								new ItemStack(item, arguments.count()), 0, 0, 0);
						entity.setPickUpDelay(arguments.pickupDelay());
						context.journal().recordCreatedEntity(entity);
						if (!player.serverLevel().addFreshEntity(entity)) {
							throw new IllegalStateException("Unable to spawn item entity " + entity.getUUID());
						}
						ItemEntityHandle handle = new ItemEntityHandle(entity.getUUID(), player.serverLevel().dimension().location().toString(),
								BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), arguments.item(), arguments.count());
						context.values().put(arguments.saveAs(), ItemEntityHandle.class, handle);
						context.putDiagnostic("itemEntity-" + arguments.saveAs(), Map.of("identity", handle, "state", "spawned", "lastPosition",
								Map.of("x", entity.getX(), "y", entity.getY(), "z", entity.getZ())));
					});
				}
				return await(future);
			}
		};
	}
}
