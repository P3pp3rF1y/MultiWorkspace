package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ResetPlayerAction extends TypedRegressionAction<ResetPlayerAction.Arguments> {
	private static final float CAMERA_YAW = -90.0F;
	private static final float CAMERA_PITCH = 20.0F;

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
		@Nullable
		private CompletableFuture<Void> future;
		private int deadline = -1;
		private boolean respawnRequested;

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			LocalPlayer clientPlayer = minecraft.player;
			if (clientPlayer == null) {
				return fail("Client player is unavailable while resetting the test cell");
			}
			if ((clientPlayer.isDeadOrDying() || minecraft.screen instanceof DeathScreen) && !respawnRequested) {
				clientPlayer.respawn();
				respawnRequested = true;
				context.putDiagnostic("resetPlayerRecovery", "requestedRespawn");
				return RegressionActionProgress.WAITING;
			}
			if (respawnRequested && (clientPlayer.isDeadOrDying() || minecraft.screen instanceof DeathScreen)) {
				return waitForRecovery();
			}
			if (future == null) {
				future = context.onServer(player -> {
					BlockPos target = context.standingPosition();
					player.closeContainer();
					player.setShiftKeyDown(false);
					player.teleportTo(player.serverLevel(), target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, Set.of(), CAMERA_YAW, CAMERA_PITCH);
					player.setDeltaMovement(Vec3.ZERO);
					player.resetFallDistance();
				});
			}
			RegressionActionProgress reset = await(future);
			if (reset != RegressionActionProgress.PASSED) {
				return reset;
			}
			BlockPos target = context.standingPosition();
			if (minecraft.player != null) {
				if (minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
					minecraft.player.closeContainer();
				}
				BlockPos clientPosition = minecraft.player.blockPosition();
				context.putDiagnostic("clientPlayerPosition", Map.of("x", clientPosition.getX(), "y", clientPosition.getY(), "z", clientPosition.getZ()));
				context.putDiagnostic("clientPlayerOnGround", minecraft.player.onGround());
				context.putDiagnostic("clientPlayerVelocity", minecraft.player.getDeltaMovement().lengthSqr());
				if (clientPosition.equals(target) && minecraft.player.onGround() && minecraft.player.containerMenu == minecraft.player.inventoryMenu
						&& minecraft.screen == null) {
					minecraft.player.setYRot(CAMERA_YAW);
					minecraft.player.setXRot(CAMERA_PITCH);
					minecraft.player.setDeltaMovement(Vec3.ZERO);
					minecraft.player.resetFallDistance();
					return RegressionActionProgress.PASSED;
				}
			}
			if (deadline < 0) {
				deadline = context.coordinatorTicks() + 100;
			}
			if (context.coordinatorTicks() >= deadline) {
				return fail("Client player did not synchronize to the supported test cell after 100 coordinator ticks: position="
						+ (minecraft.player == null ? "unavailable" : minecraft.player.blockPosition()) + ", onGround="
						+ (minecraft.player != null && minecraft.player.onGround()) + ", recovery=" + respawnRequested);
			}
			return RegressionActionProgress.WAITING;
		}

		private RegressionActionProgress waitForRecovery() {
			if (deadline < 0) {
				deadline = context.coordinatorTicks() + 100;
			}
			if (context.coordinatorTicks() >= deadline) {
				return fail("Client player remained dead while resetting the test cell after 100 coordinator ticks");
			}
			return RegressionActionProgress.WAITING;
		}
	}
}
