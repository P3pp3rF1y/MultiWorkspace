package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PlaceHeldBlockAction extends TypedRegressionAction<PlaceHeldBlockAction.Arguments> {
	static final String OFFSET_ARGUMENT = "offset";
	private static final String SNEAKING_ARGUMENT = "sneaking";

	public record Arguments(int x, int y, int z, boolean sneaking) {
	}

	public PlaceHeldBlockAction() {
		super("player.placeHeldBlock", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(OFFSET_ARGUMENT, SNEAKING_ARGUMENT), description);
		boolean sneaking = optionalBoolean(args, SNEAKING_ARGUMENT, false, description);
		if (!args.has(OFFSET_ARGUMENT)) {
			return new Arguments(0, 0, 0, sneaking);
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
		return new Arguments(coordinates[0], coordinates[1], coordinates[2], sneaking);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private int deadline = -1;
		private CompletableFuture<Void> snapshot;
		private CompletableFuture<Void> serverPlacement;
		private volatile boolean serverPlaced;
		private Block placedBlock;
		private BlockPos placedPosition;
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
			if (placedPosition != null) {
				if (serverPlacement == null) {
					serverPlacement = context.onServer(player -> serverPlaced = player.level().getBlockState(placedPosition).is(placedBlock));
				}
				if (!serverPlacement.isDone()) {
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress progress = await(serverPlacement);
				if (progress != RegressionActionProgress.PASSED || serverPlaced) {
					return progress;
				}
				serverPlacement = null;
				return minecraft.player.tickCount >= deadline
						? fail("Server did not place the held block at " + placedPosition)
						: RegressionActionProgress.WAITING;
			}
			if (minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
				minecraft.player.closeContainer();
				return RegressionActionProgress.WAITING;
			}
			if (minecraft.player.blockPosition().equals(context.standingPosition()) && minecraft.player.onGround()
					&& !minecraft.player.getMainHandItem().isEmpty() && minecraft.gameMode != null) {
				if (snapshot == null) {
					snapshot = context.onServer(player -> {
						int height = player.getMainHandItem().getOrDefault(ModDataComponents.TANK_HEIGHT, 1);
						for (int index = 0; index < height; index++) {
							context.journal().recordBlock(player.level(), context.target().offset(arguments.x(), arguments.y(), arguments.z()).above(index));
						}
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress recorded = await(snapshot);
				if (recorded != RegressionActionProgress.PASSED) {
					return recorded;
				}
				BlockPos support = context.target().offset(arguments.x(), arguments.y(), arguments.z()).below();
				if (!(minecraft.player.getMainHandItem().getItem() instanceof BlockItem blockItem)) {
					return fail("Held item is not a placeable block");
				}
				placedBlock = blockItem.getBlock();
				placedPosition = support.above();
				String heldBefore = minecraft.player.getMainHandItem().toString();
				Input previous = minecraft.player.input.keyPresses;
				if (arguments.sneaking()) {
					minecraft.player.input.keyPresses = new Input(previous.forward(), previous.backward(), previous.left(), previous.right(), previous.jump(),
							true, previous.sprint());
					minecraft.player.connection.send(new ServerboundPlayerInputPacket(minecraft.player.input.keyPresses));
				}
				InteractionResult result;
				try {
					result = minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
				} finally {
					if (arguments.sneaking()) {
						minecraft.player.input.keyPresses = previous;
						minecraft.player.connection.send(new ServerboundPlayerInputPacket(previous));
					}
				}
				observe(Map.of("heldBefore", heldBefore, "support", support.toString()),
						Map.of("interactionResult", result.toString(), "heldAfter", minecraft.player.getMainHandItem().toString()));
				return result.consumesAction() ? RegressionActionProgress.WAITING : RegressionActionProgress.PASSED;
			}
			return minecraft.player.tickCount >= deadline
					? fail("Client block-placement fixture did not synchronize at the supported test cell")
					: RegressionActionProgress.WAITING;
		}
	}
}
