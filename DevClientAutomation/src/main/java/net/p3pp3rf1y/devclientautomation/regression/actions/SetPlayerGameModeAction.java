package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetPlayerGameModeAction extends TypedRegressionAction<SetPlayerGameModeAction.Arguments> {
	public record Arguments(String mode) {
	}

	public SetPlayerGameModeAction() {
		super("fixture.player.setGameMode", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("mode"), description);
		String mode = requireString(args, "mode", description);
		if (!Set.of("survival", "creative").contains(mode)) {
			throw new IllegalArgumentException("Expected survival or creative on " + description);
		}
		return new Arguments(mode);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> player.setGameMode(arguments.mode().equals("survival") ? GameType.SURVIVAL : GameType.CREATIVE));
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				return context.minecraft().gameMode.getPlayerMode().getName().equals(arguments.mode())
						? RegressionActionProgress.PASSED
						: RegressionActionProgress.WAITING;
			}
		};
	}
}
