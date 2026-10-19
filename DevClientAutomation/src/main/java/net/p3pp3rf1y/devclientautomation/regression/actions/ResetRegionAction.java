package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

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
					BlockPos support = new BlockPos(256, 72, 256);
					player.level().getChunkAt(support);
					var unownedEntities = player.level().getEntities((Entity) null, new AABB(support.offset(-4, 0, -4)).expandTowards(9, 8, 9),
							entity -> !(entity instanceof Player));
					if (!unownedEntities.isEmpty()) {
						throw new IllegalStateException("Reusable regression cell contains an unowned entity: " + unownedEntities);
					}
					for (int x = -4; x <= 4; x++) {
						for (int z = -4; z <= 4; z++) {
							var state = player.level().getBlockState(support.offset(x, 0, z));
							if (!state.isAir() && !state.is(Blocks.STONE)) {
								throw new IllegalStateException("Reusable regression support overlaps another block at " + support.offset(x, 0, z));
							}
						}
					}
					for (int x = -3; x <= 3; x++) {
						for (int y = 1; y <= 6; y++) {
							for (int z = -3; z <= 3; z++) {
								if (!player.level().getBlockState(support.offset(x, y, z)).isAir()) {
									throw new IllegalStateException("Reusable regression cell is occupied at " + support.offset(x, y, z));
								}
							}
						}
					}
					for (int x = -4; x <= 4; x++) {
						for (int z = -4; z <= 4; z++) {
							BlockPos pos = support.offset(x, 0, z);
							if (player.level().getBlockState(pos).isAir()) {
								player.level().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
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
