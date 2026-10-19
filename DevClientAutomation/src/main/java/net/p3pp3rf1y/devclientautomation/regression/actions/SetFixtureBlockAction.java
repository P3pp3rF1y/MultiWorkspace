package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetFixtureBlockAction extends TypedRegressionAction<SetFixtureBlockAction.Arguments> {
	public record Arguments(String block, int x, int y, int z, String facing) {
	}

	public SetFixtureBlockAction() {
		super("fixture.block.set", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("block", "offset", "facing"), description);
		String block = requireString(args, "block", description);
		if (!args.has("offset") || !args.get("offset").isJsonArray()) {
			throw new IllegalArgumentException("Argument offset on " + description + " must be an [x, y, z] array");
		}
		JsonArray offset = args.getAsJsonArray("offset");
		if (offset.size() != 3) {
			throw new IllegalArgumentException("Argument offset on " + description + " must be an [x, y, z] array");
		}
		int x = integer(offset.get(0), description);
		int y = integer(offset.get(1), description);
		int z = integer(offset.get(2), description);
		String facing = optionalString(args, "facing", "", description);
		if (!facing.isEmpty() && !Set.of("north", "south", "east", "west", "up", "down").contains(facing)) {
			throw new IllegalArgumentException("Argument facing on " + description + " must be a direction");
		}
		return new Arguments(block, x, y, z, facing);
	}

	private static int integer(JsonElement value, String description) {
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException("Argument offset on " + description + " must contain integers");
		}
		try {
			return Integer.parseInt(value.getAsString());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Argument offset on " + description + " must contain integers", e);
		}
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		@Nullable
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(arguments.block()))
							.orElseThrow(() -> new IllegalArgumentException("Unknown fixture block " + arguments.block()));
					BlockState state = block.defaultBlockState();
					if (!arguments.facing().isEmpty()) {
						if (!state.hasProperty(BlockStateProperties.FACING)) {
							throw new IllegalArgumentException("Fixture block " + arguments.block() + " has no facing property");
						}
						state = state.setValue(BlockStateProperties.FACING, Direction.valueOf(arguments.facing().toUpperCase(Locale.ROOT)));
					}
					BlockPos position = context.standingPosition().south(2).offset(arguments.x(), arguments.y(), arguments.z());
					context.journal().recordBlock(player.serverLevel(), position);
					player.serverLevel().setBlockAndUpdate(position, state);
				});
			}
			return await(future);
		}
	}
}
