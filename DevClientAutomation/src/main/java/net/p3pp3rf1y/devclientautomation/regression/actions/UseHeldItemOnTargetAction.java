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
	public record Arguments(boolean useFixtureOffset, int x, int y, int z) {
	}

	record TargetCoordinates(int x, int y, int z) {
	}
	public UseHeldItemOnTargetAction() {
		super("player.useHeldItemOnTarget", "2", Set.of("minecraft:client"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offset", "offsetZ"), description);
		if (args.has("offset") && args.has("offsetZ")) {
			throw new IllegalArgumentException("Cannot combine offset and offsetZ on " + description);
		}
		if (args.has("offsetZ")) {
			return new Arguments(false, 0, 0, optionalNonNegativeInt(args, "offsetZ", 0, description));
		}
		if (!args.has("offset")) {
			return new Arguments(false, 0, 0, 0);
		}
		JsonObject positionArgs = args.deepCopy();
		positionArgs.addProperty("block", "minecraft:air");
		var position = new SetFixtureBlockAction().parseArguments(positionArgs, description);
		return new Arguments(true, position.x(), position.y(), position.z());
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int interactionTick = -1;
			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = context.minecraft();
				if (interactionTick < 0) {
					BlockPos target = resolveTarget(context.target(), context.standingPosition().south(2), arguments);
					minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
					interactionTick = minecraft.player.tickCount;
				}
				return minecraft.player.tickCount - interactionTick >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
			}
		};
	}

	static BlockPos resolveTarget(BlockPos target, BlockPos fixtureOrigin, Arguments arguments) {
		TargetCoordinates coordinates = resolveTargetCoordinates(target.getX(), target.getY(), target.getZ(), fixtureOrigin.getX(), fixtureOrigin.getY(),
				fixtureOrigin.getZ(), arguments);
		return new BlockPos(coordinates.x(), coordinates.y(), coordinates.z());
	}

	static TargetCoordinates resolveTargetCoordinates(int targetX, int targetY, int targetZ, int fixtureOriginX, int fixtureOriginY, int fixtureOriginZ,
			Arguments arguments) {
		return arguments.useFixtureOffset()
				? new TargetCoordinates(fixtureOriginX + arguments.x(), fixtureOriginY + arguments.y(), fixtureOriginZ + arguments.z())
				: new TargetCoordinates(targetX, targetY, targetZ + arguments.z());
	}
}
