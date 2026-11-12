package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PlaceHeldBlockAction extends TypedRegressionAction<PlaceHeldBlockAction.Arguments> {
	static final String OFFSET_ARGUMENT = "offset";
	private static final String SNEAKING_ARGUMENT = "sneaking";

	public record Arguments(int x, int y, int z, boolean sneaking) {
	}

	public PlaceHeldBlockAction() {
		super("player.placeHeldBlock", "1", Set.of(CLIENT_CAPABILITY));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(OFFSET_ARGUMENT, SNEAKING_ARGUMENT), description);
		boolean sneaking = optionalBoolean(args, SNEAKING_ARGUMENT, false, description);
		if (!args.has(OFFSET_ARGUMENT)) {
			return new Arguments(0, 0, 0, sneaking);
		}
		if (!args.get(OFFSET_ARGUMENT).isJsonArray() || args.getAsJsonArray(OFFSET_ARGUMENT).size() != 3) {
			throw new IllegalArgumentException("Argument offset on " + description + " must be an [x, y, z] array");
		}
		JsonArray offset = args.getAsJsonArray(OFFSET_ARGUMENT);
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
		return new Arguments(coordinates[0], coordinates[1], coordinates[2], sneaking);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private int deadline = -1;
		@Nullable
		private CompletableFuture<Void> snapshotTarget;
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
			if (minecraft.player.blockPosition().equals(context.standingPosition()) && minecraft.player.onGround()
					&& !minecraft.player.getMainHandItem().isEmpty() && minecraft.gameMode != null) {
				if (snapshotTarget == null) {
					snapshotTarget = context.onServer(player -> {
						int height = player.getMainHandItem().getOrDefault(ModDataComponents.TANK_HEIGHT, 1);
						for (int index = 0; index < height; index++)
							context.journal().recordBlock(player.serverLevel(),
									context.target().offset(arguments.x(), arguments.y(), arguments.z()).above(index));
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress snapshot = await(snapshotTarget);
				if (snapshot != RegressionActionProgress.PASSED) {
					return snapshot;
				}
				BlockPos support = context.target().offset(arguments.x(), arguments.y(), arguments.z()).below();
				String heldBefore = minecraft.player.getMainHandItem().toString();
				InteractionResult result;
				if (arguments.sneaking()) {
					minecraft.player.input.shiftKeyDown = true;
					minecraft.player.connection
							.send(new ServerboundPlayerCommandPacket(minecraft.player, ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
				}
				try {
					result = minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
				} finally {
					if (arguments.sneaking()) {
						minecraft.player.input.shiftKeyDown = false;
						minecraft.player.connection
								.send(new ServerboundPlayerCommandPacket(minecraft.player, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
					}
				}
				observe(Map.of("heldBefore", heldBefore, "support", support.toString()),
						Map.of("interactionResult", result.toString(), "heldAfter", minecraft.player.getMainHandItem().toString()));
				return RegressionActionProgress.PASSED;
			}
			return minecraft.player.tickCount >= deadline
					? fail("Client block-placement fixture did not synchronize at the supported test cell")
					: RegressionActionProgress.WAITING;
		}
	}
}
