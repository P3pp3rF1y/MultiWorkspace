package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class AssertDoubleChestAction extends TypedRegressionAction<AssertDoubleChestAction.Arguments> {
	public record Arguments(boolean controller) {
	}

	public AssertDoubleChestAction() {
		super("assert.doubleChest", "2", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("controller"), description);
		return new Arguments(optionalBoolean(args, "controller", false, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private final Map<String, Object> observations = new ConcurrentHashMap<>();
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ServerLevel level = player.level();
						BlockState first = level.getBlockState(context.target());
						observations.put("atAssertion", snapshot(level, context.target()));
						if (!(first.getBlock() instanceof ChestBlock chest) || first.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
							throw RegressionActionFailure.assertion("First ordinary chest did not join its neighbor");
						}
						BlockPos otherPos = context.target().relative(ChestBlock.getConnectedDirection(first));
						BlockState second = level.getBlockState(otherPos);
						if (second.getBlock() != chest || second.getValue(ChestBlock.TYPE) != first.getValue(ChestBlock.TYPE).getOpposite()
								|| second.getValue(ChestBlock.FACING) != first.getValue(ChestBlock.FACING)
								|| !otherPos.relative(ChestBlock.getConnectedDirection(second)).equals(context.target())) {
							throw RegressionActionFailure.assertion("Ordinary chest blocks do not form a matching double chest");
						}
						if (!(level.getBlockEntity(context.target()) instanceof ChestBlockEntity firstEntity)
								|| !(level.getBlockEntity(otherPos) instanceof ChestBlockEntity secondEntity)
								|| !firstEntity.getMainPos().equals(secondEntity.getMainPos())
								|| !(level.getBlockEntity(firstEntity.getMainPos()) instanceof ChestBlockEntity main)
								|| main.getStorageWrapper().getInventoryHandler().size() != chest.getNumberOfInventorySlots() * 2) {
							throw RegressionActionFailure.assertion("Naturally joined chest did not acquire shared double capacity");
						}
						if (arguments.controller()) {
							BlockPos controllerPos = main.getControllerPos()
									.orElseThrow(() -> RegressionActionFailure.assertion("Double chest did not connect to its controller"));
							if (!(level.getBlockEntity(controllerPos) instanceof ControllerBlockEntityBase controller)
									|| !controller.getStoragePositions().equals(List.of(main.getBlockPos()))
									|| controller.size() != main.getStorageWrapper().getInventoryForInputOutput().size()) {
								throw RegressionActionFailure.assertion("Controller did not expose exactly the shared double-chest inventory");
							}
						}
					});
				}
				RegressionActionProgress result = await(future);
				if (result == RegressionActionProgress.FAILED && !observations.isEmpty()) {
					observe("joined double chest", observations);
				}
				return result;
			}
		};
	}

	private static Map<String, String> snapshot(ServerLevel level, BlockPos pos) {
		return Map.of("first", level.getBlockState(pos).toString(), "north", level.getBlockState(pos.north()).toString(), "south",
				level.getBlockState(pos.south()).toString(), "east", level.getBlockState(pos.east()).toString(), "west",
				level.getBlockState(pos.west()).toString());
	}
}
