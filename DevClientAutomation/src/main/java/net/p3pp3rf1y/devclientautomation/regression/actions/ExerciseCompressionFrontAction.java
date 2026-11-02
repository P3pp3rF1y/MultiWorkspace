package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Front withdrawal and deposit through ordinary mouse/key input, including server-side ray picking. */
public final class ExerciseCompressionFrontAction extends TypedRegressionAction<ExerciseCompressionFrontAction.Arguments> {
	public record Arguments(boolean stack) {
	}

	public ExerciseCompressionFrontAction() {
		super("exercise.storage.compressionFront", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("stack"), description);
		return new Arguments(optionalBoolean(args, "stack", false, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			private long before;
			private Vec3 aim;
			private Direction facing;
			private boolean prepared;
			private boolean withdrawing;
			private boolean withdrawn;
			private boolean depositing;
			private boolean complete;
			private boolean shiftReady;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = Minecraft.getInstance();
				if (deadline < 0) {
					deadline = minecraft.player.tickCount + 150;
				}
				if (minecraft.player.tickCount >= deadline) {
					minecraft.options.keyShift.setDown(false);
					return failAssertion("Front compression cycle did not converge: withdrawn=" + withdrawn + ", depositing=" + depositing);
				}
				if (!prepared) {
					if (future == null) {
						future = context.onServer(player -> {
							if (!(player.serverLevel().getBlockEntity(context.target()) instanceof StorageBlockEntity storage)
									|| !(storage.getBlockState().getBlock() instanceof LimitedBarrelBlock barrel) || !player.getMainHandItem().isEmpty()) {
								throw RegressionActionFailure.infrastructure("Front cycle requires a limited barrel and empty selected hand");
							}
							before = AssertCompressionAction.assertContents(storage.getStorageWrapper().getInventoryHandler());
							facing = barrel.getFacing(storage.getBlockState());
							if (facing.getAxis().isVertical()) {
								throw RegressionActionFailure.infrastructure("Front fixture requires a horizontal facing");
							}
							aim = Vec3.atCenterOf(context.target()).add(facing.getStepX() * 0.501 + facing.getStepZ() * 0.25, -0.25,
									facing.getStepZ() * 0.501 - facing.getStepX() * 0.25);
						});
					}
					RegressionActionProgress result = await(future);
					if (result != RegressionActionProgress.PASSED) {
						return result;
					}
					prepared = true;
					future = null;
				}
				minecraft.options.keyShift.setDown(arguments.stack() && !withdrawn);
				minecraft.player.lookAt(EntityAnchorArgument.Anchor.EYES, aim);
				if (!(minecraft.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(context.target()) || hit.getDirection() != facing) {
					return RegressionActionProgress.WAITING;
				}
				if (future == null) {
					future = context.onServer(player -> {
						shiftReady = player.isShiftKeyDown() == (arguments.stack() && !withdrawn)
								&& player.pick(player.getBlockReach(), 0, false) instanceof BlockHitResult serverHit
								&& serverHit.getBlockPos().equals(context.target()) && serverHit.getDirection() == facing;
						StorageBlockEntity storage = (StorageBlockEntity) player.serverLevel().getBlockEntity(context.target());
						long total = AssertCompressionAction.assertContents(storage.getStorageWrapper().getInventoryHandler());
						int amount = arguments.stack() ? 64 : 1;
						if (withdrawing && !withdrawn && total == before - amount) {
							if (!player.getMainHandItem().is(Items.IRON_NUGGET) || player.getMainHandItem().getCount() != amount) {
								throw RegressionActionFailure.assertion("Front withdrawal did not reach the selected player slot");
							}
							withdrawn = true;
						}
						complete = depositing && total == before && player.getMainHandItem().isEmpty();
					});
				}
				RegressionActionProgress result = await(future);
				if (result != RegressionActionProgress.PASSED) {
					return result;
				}
				future = null;
				if (complete) {
					context.putDiagnostic("compressionFront" + (arguments.stack() ? "Stack" : "Single"),
							"mouse withdrawal/deposit conserved contents and calculated indexes");
					return RegressionActionProgress.PASSED;
				}
				if (!shiftReady) {
					return RegressionActionProgress.WAITING;
				}
				if (!withdrawing) {
					VirtualInput.pressMouseButton(0);
					VirtualInput.releaseMouseButton();
					withdrawing = true;
				} else if (withdrawn && !depositing && !minecraft.player.getMainHandItem().isEmpty() && !minecraft.player.isShiftKeyDown()) {
					VirtualInput.pressMouseButton(1);
					VirtualInput.releaseMouseButton();
					depositing = true;
				}
				return RegressionActionProgress.WAITING;
			}
		};
	}
}
