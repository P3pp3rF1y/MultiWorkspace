package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PlaceHeldBackpackAction extends TypedRegressionAction<PlaceHeldBackpackAction.Arguments> {
	public record Arguments() {
	}

	public PlaceHeldBackpackAction() {
		super("backpack.placeHeld", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
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
		private int deadline = -1;
		private CompletableFuture<Void> enableSneaking;
		private CompletableFuture<Void> checkPlacement;
		private CompletableFuture<Void> disableSneaking;
		private boolean clientUseSent;
		private boolean serverPlaced;

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (enableSneaking == null) {
				enableSneaking = context.onServer(player -> player.setShiftKeyDown(true));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress sneaking = await(enableSneaking);
			if (sneaking != RegressionActionProgress.PASSED) {
				return sneaking;
			}
			if (clientUseSent) {
				return awaitServerPlacement(minecraft);
			}
			BlockPos support = context.target().below();
			if (minecraft.player != null && minecraft.gameMode != null && minecraft.level != null
					&& minecraft.player.blockPosition().equals(context.standingPosition()) && minecraft.player.onGround()
					&& minecraft.player.getMainHandItem().getItem() instanceof BackpackItem && minecraft.level.getBlockState(support).is(Blocks.STONE)) {
				minecraft.player.setShiftKeyDown(true);
				try {
					minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
				} finally {
					minecraft.player.setShiftKeyDown(false);
				}
				clientUseSent = true;
				return RegressionActionProgress.WAITING;
			}
			if (minecraft.player.tickCount >= deadline) {
				return disableSneakingThenFail("Client backpack fixture did not synchronize before placement");
			}
			return RegressionActionProgress.WAITING;
		}

		private RegressionActionProgress awaitServerPlacement(Minecraft minecraft) {
			if (checkPlacement == null) {
				checkPlacement = context.onServer(player -> serverPlaced = player.level().getBlockEntity(context.target()) instanceof BackpackBlockEntity);
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress checked = await(checkPlacement);
			if (checked != RegressionActionProgress.PASSED) {
				return checked;
			}
			if (serverPlaced) {
				context.putDiagnostic("serverPlacement", "confirmed");
				if (disableSneaking == null) {
					disableSneaking = context.onServer(player -> player.setShiftKeyDown(false));
					return RegressionActionProgress.WAITING;
				}
				return await(disableSneaking);
			}
			checkPlacement = null;
			if (minecraft.player.tickCount >= deadline) {
				return disableSneakingThenFail("Server did not place the held backpack");
			}
			return RegressionActionProgress.WAITING;
		}

		private RegressionActionProgress disableSneakingThenFail(String message) {
			if (disableSneaking == null) {
				disableSneaking = context.onServer(player -> player.setShiftKeyDown(false));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress disabled = await(disableSneaking);
			return disabled == RegressionActionProgress.PASSED ? fail(message) : disabled;
		}
	}
}
