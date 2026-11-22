package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
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
	public record Arguments(boolean controller, @Nullable Boolean packed, @Nullable Boolean firstWaterlogged, @Nullable Boolean otherWaterlogged,
			boolean noDrops, @Nullable String upgrade) {
	}

	public AssertDoubleChestAction() {
		super("assert.doubleChest", "3", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("controller", "packed", "firstWaterlogged", "otherWaterlogged", "noDrops", "upgrade"), description);
		return new Arguments(optionalBoolean(args, "controller", false, description), optionalExpectation(args, "packed", description),
				optionalExpectation(args, "firstWaterlogged", description), optionalExpectation(args, "otherWaterlogged", description),
				optionalBoolean(args, "noDrops", false, description), args.has("upgrade") ? requireString(args, "upgrade", description) : null);
	}

	@Nullable
	private static Boolean optionalExpectation(JsonObject args, String key, String description) {
		return args.has(key) ? optionalBoolean(args, key, false, description) : null;
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
						ServerLevel level = player.serverLevel();
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
								|| main.getStorageWrapper().getInventoryHandler().getSlots() != chest.getNumberOfInventorySlots() * 2) {
							throw RegressionActionFailure.assertion("Naturally joined chest did not acquire shared double capacity");
						}
						if (arguments.packed() != null && (firstEntity.isPacked() != arguments.packed() || secondEntity.isPacked() != arguments.packed())) {
							throw RegressionActionFailure.assertion("Expected packed=" + arguments.packed() + " on both halves, got " + firstEntity.isPacked()
									+ " and " + secondEntity.isPacked());
						}
						assertWaterlogged("First", first, arguments.firstWaterlogged());
						assertWaterlogged("Other", second, arguments.otherWaterlogged());
						if (arguments.upgrade() != null) {
							var expected = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(arguments.upgrade()))
									.orElseThrow(() -> RegressionActionFailure.infrastructure("Unknown expected upgrade: " + arguments.upgrade()));
							var actual = main.getStorageWrapper().getUpgradeHandler().getStackInSlot(0);
							if (!actual.is(expected) || actual.getCount() != 1) {
								throw RegressionActionFailure.assertion("Expected one " + arguments.upgrade() + " in upgrade slot 0, got " + actual);
							}
						}
						if (arguments.noDrops()) {
							var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(context.target()).minmax(new AABB(otherPos)).inflate(1));
							if (!drops.isEmpty()) {
								throw RegressionActionFailure
										.assertion("Unexpected drops around the double chest: " + drops.stream().map(ItemEntity::getItem).toList());
							}
						}
						if (arguments.controller()) {
							BlockPos controllerPos = main.getControllerPos()
									.orElseThrow(() -> RegressionActionFailure.assertion("Double chest did not connect to its controller"));
							if (!(level.getBlockEntity(controllerPos) instanceof ControllerBlockEntityBase controller)
									|| !controller.getStoragePositions().equals(List.of(main.getBlockPos()))
									|| controller.getSlots() != main.getStorageWrapper().getInventoryForInputOutput().getSlots()) {
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
			private static void assertWaterlogged(String half, BlockState state, @Nullable Boolean expected) {
				if (expected != null && !expected.equals(state.getValue(ChestBlock.WATERLOGGED))) {
					throw RegressionActionFailure.assertion(half + " chest half expected waterlogged=" + expected + ", got " + state);
				}
			}
		};
	}

	private static Map<String, String> snapshot(ServerLevel level, BlockPos pos) {
		return Map.of("first", level.getBlockState(pos).toString(), "north", level.getBlockState(pos.north()).toString(), "south",
				level.getBlockState(pos.south()).toString(), "east", level.getBlockState(pos.east()).toString(), "west",
				level.getBlockState(pos.west()).toString());
	}
}
