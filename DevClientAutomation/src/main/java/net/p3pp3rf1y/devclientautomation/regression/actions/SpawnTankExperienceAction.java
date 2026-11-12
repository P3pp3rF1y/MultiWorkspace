package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ExperienceOrb;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SpawnTankExperienceAction extends TypedRegressionAction<Integer> {
	public SpawnTankExperienceAction() {
		super("fixture.tank.spawnExperience", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}

	@Override
	protected Integer parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("experience"), description);
		int experience = optionalPositiveInt(args, "experience", 1, description);
		if (experience > 100) {
			throw new IllegalArgumentException("Experience exceeds the bounded fixture on " + description);
		}
		return experience;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Integer experience) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						BlockPos target = context.target();
						ExperienceOrb orb = new ExperienceOrb(player.level(), target.getX() + .5, target.getY() + 1.25, target.getZ() + .5, experience);
						orb.setDeltaMovement(0, 0, 0);
						context.journal().recordCreatedEntity(orb);
						if (!player.level().addFreshEntity(orb)) {
							throw new IllegalStateException("Unable to spawn XP fixture");
						}
					});
				}
				return await(future);
			}
		};
	}
}
