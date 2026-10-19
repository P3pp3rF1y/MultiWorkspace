package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class BreakTargetBlockAction extends TypedRegressionAction<BreakTargetBlockAction.Arguments> {
	public record Arguments() {
	}

	public BreakTargetBlockAction() {
		super("player.breakTargetBlock", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(), description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			private boolean started;
			private int ticks;

			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = Minecraft.getInstance();
				if (!started) {
					if (minecraft.gameMode == null || !minecraft.gameMode.startDestroyBlock(context.target(), Direction.UP)) {
						throw new IllegalStateException("Player could not start breaking target block");
					}
					started = true;
				}
				if (++ticks < 4) {
					return RegressionActionProgress.WAITING;
				}
				if (future == null) {
					future = context.onServer(player -> {
						if (!player.serverLevel().getBlockState(context.target()).isAir()) {
							throw new IllegalStateException("Target block was not broken");
						}
					});
				}
				return await(future);
			}
		};
	}
}
