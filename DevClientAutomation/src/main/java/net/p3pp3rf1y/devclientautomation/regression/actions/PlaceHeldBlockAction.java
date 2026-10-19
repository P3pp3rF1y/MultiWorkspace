package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PlaceHeldBlockAction extends TypedRegressionAction<PlaceHeldBlockAction.Arguments> {
	public record Arguments(int x, int y, int z) {
	}

	public PlaceHeldBlockAction() {
		super("player.placeHeldBlock", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offset"), description);
		if (!args.has("offset")) {
			return new Arguments(0, 0, 0);
		}
		if (!args.get("offset").isJsonArray() || args.getAsJsonArray("offset").size() != 3) {
			throw new IllegalArgumentException("Argument offset on " + description + " must be an [x, y, z] array");
		}
		JsonArray offset = args.getAsJsonArray("offset");
		int[] coordinates = new int[3];
		for (int i = 0; i < coordinates.length; i++) {
			if (!offset.get(i).isJsonPrimitive() || !offset.get(i).getAsJsonPrimitive().isNumber()) {
				throw new IllegalArgumentException("Argument offset on " + description + " must contain integers");
			}
			try {
				coordinates[i] = Integer.parseInt(offset.get(i).getAsString());
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
		private int deadline = -1;
		private CompletableFuture<Void> snapshot;
		private final Arguments arguments;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
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
			if (snapshot == null) {
				snapshot = context.onServer(
						player -> context.journal().recordBlock(player.level(), context.target().offset(arguments.x(), arguments.y(), arguments.z())));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress snapped = await(snapshot);
			if (snapped != RegressionActionProgress.PASSED) {
				return snapped;
			}
			if (minecraft.player.blockPosition().equals(context.standingPosition()) && minecraft.player.onGround()
					&& !minecraft.player.getMainHandItem().isEmpty() && minecraft.gameMode != null) {
				BlockPos support = context.target().offset(arguments.x(), arguments.y(), arguments.z()).below();
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
