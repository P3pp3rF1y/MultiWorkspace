package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SpawnFixturePigAction extends TypedRegressionAction<SpawnFixturePigAction.Arguments> {
	public record Arguments(String saveAs, int offsetX) {
	}

	public SpawnFixturePigAction() {
		super("fixture.entity.spawnPig", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("saveAs", "offsetX"), description);
		return new Arguments(requireString(args, "saveAs", description), optionalNonNegativeInt(args, "offsetX", 0, description));
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
						Pig pig = EntityType.PIG.create(player.level(), EntitySpawnReason.COMMAND);
						if (pig == null) {
							throw new IllegalStateException("Unable to create fixture pig");
						}
						BlockPos pos = context.target().west().east(arguments.offsetX());
						pig.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
						pig.setNoAi(true);
						context.journal().recordCreatedEntity(pig);
						if (!player.level().addFreshEntity(pig)) {
							throw new IllegalStateException("Unable to spawn fixture pig");
						}
						context.values().put(arguments.saveAs(), UUID.class, pig.getUUID());
					});
				}
				return await(future);
			}
		};
	}
}
