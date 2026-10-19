package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertDoubleChestAction extends TypedRegressionAction<AssertDoubleChestAction.Arguments> {
	public record Arguments() {
	}

	public AssertDoubleChestAction() {
		super("assert.doubleChest", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ServerLevel level = player.serverLevel();
						BlockState first = level.getBlockState(context.target());
						if (!(first.getBlock() instanceof ChestBlock chest) || first.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
							throw new IllegalStateException("First ordinary chest did not join its neighbor");
						}
						BlockPos otherPos = context.target().relative(ChestBlock.getConnectedDirection(first));
						BlockState second = level.getBlockState(otherPos);
						if (second.getBlock() != chest || second.getValue(ChestBlock.TYPE) != first.getValue(ChestBlock.TYPE).getOpposite()
								|| second.getValue(ChestBlock.FACING) != first.getValue(ChestBlock.FACING)
								|| !otherPos.relative(ChestBlock.getConnectedDirection(second)).equals(context.target())) {
							throw new IllegalStateException("Ordinary chest blocks do not form a matching double chest");
						}
						if (!(level.getBlockEntity(context.target()) instanceof ChestBlockEntity firstEntity)
								|| !(level.getBlockEntity(otherPos) instanceof ChestBlockEntity secondEntity)
								|| !firstEntity.getMainPos().equals(secondEntity.getMainPos())
								|| !(level.getBlockEntity(firstEntity.getMainPos()) instanceof ChestBlockEntity main)
								|| main.getStorageWrapper().getInventoryHandler().getSlots() != chest.getNumberOfInventorySlots() * 2) {
							throw new IllegalStateException("Naturally joined chest did not acquire shared double capacity");
						}
					});
				}
				return await(future);
			}
		};
	}
}
