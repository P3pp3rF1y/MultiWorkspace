package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ResetRegionAction extends TypedRegressionAction<ResetRegionAction.Arguments> {
	public record Arguments() {
	}

	public ResetRegionAction() {
		super("fixture.region.reset", "1", Set.of("minecraft:client"));
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

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					BlockPos current = player.blockPosition();
					int hash = context.runId().hashCode();
					BlockPos support = new BlockPos(current.getX() + Math.floorMod(hash, 5) * 16 + 16, current.getY() - 1,
							current.getZ() + Math.floorMod(hash >>> 8, 5) * 16 + 16);
					for (int x = -4; x <= 4; x++) {
						for (int z = -4; z <= 4; z++) {
							player.level().setBlockAndUpdate(support.offset(x, 0, z), Blocks.STONE.defaultBlockState());
						}
					}
					for (int x = -3; x <= 3; x++) {
						for (int y = 1; y <= 6; y++) {
							for (int z = -3; z <= 3; z++) {
								player.level().removeBlock(support.offset(x, y, z), false);
							}
						}
					}
					context.setSupport(support);
				});
			}
			return await(future);
		}
	}
}
