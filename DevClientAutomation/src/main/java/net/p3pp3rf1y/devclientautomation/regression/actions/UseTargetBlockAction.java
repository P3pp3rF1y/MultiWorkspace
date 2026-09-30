package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class UseTargetBlockAction extends TypedRegressionAction<UseTargetBlockAction.Arguments> {
	public record Arguments() {
	}

	public UseTargetBlockAction() {
		super("player.useTargetBlock", "1", Set.of("minecraft:client"));
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
		private int deadline;

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (deadline == 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (minecraft.player.getMainHandItem().isEmpty()) {
				BlockPos target = context.target();
				context.putDiagnostic("clientDistance", minecraft.player.position().distanceTo(Vec3.atCenterOf(target)));
				minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
				return RegressionActionProgress.PASSED;
			}
			if (minecraft.player.tickCount >= deadline) {
				return fail("Client hand did not become empty before target-block use");
			}
			return RegressionActionProgress.WAITING;
		}
	}
}
