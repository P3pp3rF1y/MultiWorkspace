package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.p3pp3rf1y.devclientautomation.regression.RegressionJsonNumbers;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ScanFixtureControllerAction extends TypedRegressionAction<ScanFixtureControllerAction.Arguments> {
	public record Arguments(int x, int y, int z) {
	}

	public ScanFixtureControllerAction() {
		super("fixture.controller.scanAdjacent", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offset"), description);
		if (!args.has("offset") || !args.get("offset").isJsonArray()) {
			throw new IllegalArgumentException("Argument offset on " + description + " must be an [x, y, z] array");
		}
		JsonArray offset = args.getAsJsonArray("offset");
		if (offset.size() != 3) {
			throw new IllegalArgumentException("Argument offset on " + description + " must be an [x, y, z] array");
		}
		int[] coordinates = new int[3];
		for (int index = 0; index < offset.size(); index++) {
			var element = offset.get(index);
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
				throw new IllegalArgumentException("Argument offset on " + description + " must contain integers");
			}
			try {
				coordinates[index] = RegressionJsonNumbers.integer(element);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Argument offset on " + description + " must contain integers", e);
			}
		}
		return new Arguments(coordinates[0], coordinates[1], coordinates[2]);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					BlockPos position = context.standingPosition().south(2).offset(arguments.x(), arguments.y(), arguments.z());
					if (!(player.level().getBlockEntity(position) instanceof ControllerBlockEntityBase controller)) {
						throw new IllegalStateException("No fixture controller at " + position);
					}
					controller.searchAndAddBoundables();
				});
			}
			return await(future);
		}
	}
}
