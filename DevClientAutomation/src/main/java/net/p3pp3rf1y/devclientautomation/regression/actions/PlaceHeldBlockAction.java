package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class PlaceHeldBlockAction extends TypedRegressionAction<PlaceHeldBlockAction.Arguments> {
	public record Arguments() {
	}

	public PlaceHeldBlockAction() {
		super("player.placeHeldBlock", "1", Set.of("minecraft:client"));
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

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
				minecraft.player.closeContainer();
				return RegressionActionProgress.WAITING;
			}
			if (minecraft.player.blockPosition().equals(context.standingPosition()) && minecraft.player.onGround()
					&& !minecraft.player.getMainHandItem().isEmpty() && minecraft.gameMode != null) {
				BlockPos support = context.target().below();
				minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
				return RegressionActionProgress.PASSED;
			}
			return minecraft.player.tickCount >= deadline
					? fail("Client block-placement fixture did not synchronize at the supported test cell")
					: RegressionActionProgress.WAITING;
		}
	}
}
