package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PlaceHeldBackpackAction extends TypedRegressionAction<PlaceHeldBackpackAction.Arguments> {
	public record Arguments(boolean useOffset, int x, int y, int z) {
	}

	public PlaceHeldBackpackAction() {
		super("backpack.placeHeld", "2", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offset", "offsetZ"), description);
		if (args.has("offset") && args.has("offsetZ")) {
			throw new IllegalArgumentException("Cannot combine offset and offsetZ on " + description);
		}
		if (args.has("offsetZ")) {
			return new Arguments(true, 0, 0, optionalNonNegativeInt(args, "offsetZ", 0, description));
		}
		if (!args.has("offset")) {
			return new Arguments(false, 0, 0, 0);
		}
		JsonObject positionArgs = args.deepCopy();
		positionArgs.addProperty("block", "minecraft:air");
		var position = new SetFixtureBlockAction().parseArguments(positionArgs, description);
		return new Arguments(true, position.x(), position.y(), position.z());
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final BlockPos target;
		private int deadline = -1;
		private CompletableFuture<Void> enableSneaking;
		private CompletableFuture<Void> snapshot;
		private CompletableFuture<Void> checkPlacement;
		private CompletableFuture<Void> disableSneaking;
		private boolean clientUseSent;
		private boolean serverPlaced;
		private boolean terminalFailureLatched;
		private String terminalFailure;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			target = arguments.useOffset() ? context.standingPosition().south(2).offset(arguments.x(), arguments.y(), arguments.z()) : context.target();
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (terminalFailureLatched) {
				return awaitSneakingDisable();
			}
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (enableSneaking == null) {
				enableSneaking = context.onServer(player -> player.setShiftKeyDown(true));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress sneaking = await(enableSneaking);
			if (sneaking != RegressionActionProgress.PASSED) {
				return beginTerminalFailure();
			}
			if (clientUseSent) {
				return awaitServerPlacement(minecraft);
			}
			if (snapshot == null) {
				snapshot = context.onServer(player -> context.journal().recordBlock(player.level(), target));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress recorded = await(snapshot);
			if (recorded != RegressionActionProgress.PASSED) {
				return beginTerminalFailure();
			}
			BlockPos support = target.below();
			if (minecraft.player != null && minecraft.gameMode != null && minecraft.level != null
					&& minecraft.player.blockPosition().equals(context.standingPosition()) && minecraft.player.onGround()
					&& minecraft.player.getMainHandItem().getItem() instanceof BackpackItem && minecraft.level.getBlockState(support).is(Blocks.STONE)) {
				try {
					minecraft.player.setShiftKeyDown(true);
					minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
				} catch (RuntimeException e) {
					return beginTerminalFailure(e.getMessage());
				} finally {
					minecraft.player.setShiftKeyDown(false);
				}
				clientUseSent = true;
				return RegressionActionProgress.WAITING;
			}
			if (minecraft.player.tickCount >= deadline) {
				return disableSneakingThenFail("Client backpack fixture did not synchronize before placement");
			}
			return RegressionActionProgress.WAITING;
		}

		private RegressionActionProgress awaitServerPlacement(Minecraft minecraft) {
			if (checkPlacement == null) {
				checkPlacement = context.onServer(player -> serverPlaced = player.level().getBlockEntity(target) instanceof BackpackBlockEntity);
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress checked = await(checkPlacement);
			if (checked != RegressionActionProgress.PASSED) {
				return beginTerminalFailure();
			}
			if (serverPlaced) {
				context.putDiagnostic("serverPlacement", "confirmed");
				if (disableSneaking == null) {
					disableSneaking = context.onServer(player -> player.setShiftKeyDown(false));
					return RegressionActionProgress.WAITING;
				}
				return await(disableSneaking);
			}
			checkPlacement = null;
			if (minecraft.player.tickCount >= deadline) {
				return disableSneakingThenFail("Server did not place the held backpack");
			}
			return RegressionActionProgress.WAITING;
		}

		private RegressionActionProgress disableSneakingThenFail(String message) {
			return beginTerminalFailure(message);
		}

		private RegressionActionProgress beginTerminalFailure() {
			return beginTerminalFailure(failure());
		}

		private RegressionActionProgress beginTerminalFailure(String message) {
			if (!terminalFailureLatched) {
				terminalFailure = message;
				terminalFailureLatched = true;
			}
			return awaitSneakingDisable();
		}

		private RegressionActionProgress awaitSneakingDisable() {
			if (disableSneaking == null) {
				disableSneaking = context.onServer(player -> player.setShiftKeyDown(false));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress disabled = await(disableSneaking);
			return disabled == RegressionActionProgress.PASSED ? fail(terminalFailure) : disabled;
		}
	}
}
