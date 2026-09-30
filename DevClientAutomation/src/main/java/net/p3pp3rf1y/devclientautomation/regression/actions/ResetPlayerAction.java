package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ResetPlayerAction extends TypedRegressionAction<ResetPlayerAction.Arguments> {
	public record Arguments() {
	}

	public ResetPlayerAction() {
		super("player.reset", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private CompletableFuture<Void> future;
		private int deadline = -1;

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					BlockPos target = context.standingPosition();
					player.closeContainer();
					player.teleportTo(player.level(), target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, Set.of(), 0.0F, 0.0F, false);
					player.setDeltaMovement(Vec3.ZERO);
					player.resetFallDistance();
					if (!player.onGround() || player.getDeltaMovement().lengthSqr() != 0.0D) {
						throw new IllegalStateException("Player reset did not leave the server player supported and stationary");
					}
				});
			}
			RegressionActionProgress reset = await(future);
			if (reset != RegressionActionProgress.PASSED) {
				return reset;
			}
			Minecraft minecraft = context.minecraft();
			BlockPos target = context.standingPosition();
			if (minecraft.player != null) {
				BlockPos clientPosition = minecraft.player.blockPosition();
				context.putDiagnostic("clientPlayerPosition", Map.of("x", clientPosition.getX(), "y", clientPosition.getY(), "z", clientPosition.getZ()));
				context.putDiagnostic("clientPlayerOnGround", minecraft.player.onGround());
				context.putDiagnostic("clientPlayerVelocity", minecraft.player.getDeltaMovement().lengthSqr());
				if (clientPosition.equals(target) && minecraft.player.onGround()) {
					minecraft.player.setDeltaMovement(Vec3.ZERO);
					minecraft.player.resetFallDistance();
					return RegressionActionProgress.PASSED;
				}
			}
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (minecraft.player.tickCount >= deadline) {
				return fail("Client player did not synchronize to the supported test cell");
			}
			return RegressionActionProgress.WAITING;
		}
	}
}
