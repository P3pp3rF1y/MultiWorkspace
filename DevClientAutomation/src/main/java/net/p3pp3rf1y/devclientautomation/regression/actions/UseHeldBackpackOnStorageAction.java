package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Sends a real sneaking item-on-block interaction through the client game mode. */
public final class UseHeldBackpackOnStorageAction extends TypedRegressionAction<UseHeldBackpackOnStorageAction.Arguments> {
	public record Arguments() {
	}

	public UseHeldBackpackOnStorageAction() {
		super("backpack.useOnStorage", "2", Set.of("minecraft:client", "sophisticatedbackpacks", "sophisticatedstorage"));
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
		private CompletableFuture<Void> enableSneaking;
		private CompletableFuture<Void> disableSneaking;
		private CompletableFuture<Void> snapshotTarget;
		private int deadline = -1;
		private int sentAt;
		private boolean sent;
		private boolean terminalFailureLatched;
		private String terminalFailure;

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (terminalFailureLatched) {
				return disableSneakingThenFail();
			}
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (enableSneaking == null) {
				enableSneaking = context.onServer(player -> player.setShiftKeyDown(true));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress enabled = await(enableSneaking);
			if (enabled == RegressionActionProgress.WAITING) {
				return RegressionActionProgress.WAITING;
			}
			if (enabled != RegressionActionProgress.PASSED) {
				return terminalFail();
			}
			if (sent) {
				if (minecraft.player.tickCount < sentAt + 10) {
					return RegressionActionProgress.WAITING;
				}
				if (disableSneaking == null) {
					disableSneaking = context.onServer(player -> player.setShiftKeyDown(false));
					return RegressionActionProgress.WAITING;
				}
				return await(disableSneaking);
			}
			if (snapshotTarget == null) {
				snapshotTarget = context.onServer(player -> context.journal().recordBlock(player.level(), context.target()));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress snapshot = await(snapshotTarget);
			if (snapshot == RegressionActionProgress.WAITING) {
				return RegressionActionProgress.WAITING;
			}
			if (snapshot != RegressionActionProgress.PASSED) {
				return terminalFail();
			}
			if (minecraft.player.blockPosition().equals(context.standingPosition()) && minecraft.player.onGround()
					&& minecraft.player.getMainHandItem().getItem() instanceof BackpackItem
					&& (minecraft.level.getBlockEntity(context.target()) instanceof StorageBlockEntity
							|| minecraft.level.getBlockEntity(context.target()) instanceof ControllerBlockEntityBase)) {
				try {
					minecraft.player.setShiftKeyDown(true);
					minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(context.target()), Direction.UP, context.target(), false));
					sent = true;
					sentAt = minecraft.player.tickCount;
					return RegressionActionProgress.WAITING;
				} catch (RuntimeException e) {
					terminalFailure = e.getMessage();
					terminalFailureLatched = true;
					return disableSneakingThenFail();
				} finally {
					minecraft.player.setShiftKeyDown(false);
				}
			}
			if (minecraft.player.tickCount >= deadline) {
				terminalFailure = "Backpack or target storage did not synchronize before use";
				terminalFailureLatched = true;
				return disableSneakingThenFail();
			}
			return RegressionActionProgress.WAITING;
		}

		private RegressionActionProgress terminalFail() {
			terminalFailure = failure();
			terminalFailureLatched = true;
			return disableSneakingThenFail();
		}

		private RegressionActionProgress disableSneakingThenFail() {
			if (disableSneaking == null) {
				disableSneaking = context.onServer(player -> player.setShiftKeyDown(false));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress disabled = await(disableSneaking);
			return disabled == RegressionActionProgress.PASSED ? fail(terminalFailure) : disabled;
		}
	}
}
