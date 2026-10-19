package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class UseHeldItemOnTargetAction extends TypedRegressionAction<UseHeldItemOnTargetAction.Arguments> {
	public record Arguments(int offsetZ) {
	}
	public UseHeldItemOnTargetAction() {
		super("player.useHeldItemOnTarget", "1", Set.of("minecraft:client"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offsetZ"), description);
		return new Arguments(optionalNonNegativeInt(args, "offsetZ", 0, description));
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int interactionTick = -1;
			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = context.minecraft();
				if (interactionTick < 0) {
					BlockPos target = context.target().south(arguments.offsetZ());
					minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
					interactionTick = minecraft.player.tickCount;
				}
				return minecraft.player.tickCount - interactionTick >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
			}
		};
	}
}
