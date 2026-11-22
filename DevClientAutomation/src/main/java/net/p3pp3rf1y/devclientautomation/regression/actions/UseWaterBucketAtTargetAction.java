package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Aimed sneak-use so block interaction does not consume the bucket click. */
public final class UseWaterBucketAtTargetAction extends TypedRegressionAction<UseWaterBucketAtTargetAction.Arguments> {

	public record Arguments(int x, int y, int z, boolean drain) {
	}

	public UseWaterBucketAtTargetAction() {
		super("player.useWaterBucketAtTarget", "1", Set.of(CLIENT_CAPABILITY));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offset", "drain"), description);
		JsonObject position = new JsonObject();
		if (args.has("offset")) {
			position.add("offset", args.get("offset"));
		}
		var offset = new PlaceHeldBlockAction().parseArguments(position, description);
		return new Arguments(offset.x(), offset.y(), offset.z(), optionalBoolean(args, "drain", false, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private final BlockPos target = context.target().offset(arguments.x(), arguments.y(), arguments.z());
			private final Vec3 aim = Vec3.atCenterOf(target);
			@Nullable
			private CompletableFuture<Void> probe;
			private int deadline = -1;
			private boolean ready;
			private boolean clicked;
			private boolean serverExchanged;

			@Override
			public RegressionActionProgress tick() {
				var minecraft = context.minecraft();
				var player = minecraft.player;
				var before = arguments.drain() ? Items.BUCKET : Items.WATER_BUCKET;
				var after = arguments.drain() ? Items.WATER_BUCKET : Items.BUCKET;
				if (deadline < 0) {
					deadline = player.tickCount + 100;
				}
				if (player.tickCount >= deadline) {
					minecraft.options.keyShift.setDown(false);
					return failAssertion("Bucket use did not converge at " + target + " (clicked=" + clicked + ", held=" + player.getMainHandItem() + ")");
				}
				if (!clicked) {
					minecraft.options.keyShift.setDown(true);
					player.lookAt(EntityAnchorArgument.Anchor.EYES, aim);
					if (!player.getMainHandItem().is(before) || !(minecraft.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(target)) {
						return RegressionActionProgress.WAITING;
					}
				}
				if (probe == null) {
					probe = context.onServer(serverPlayer -> {
						if (clicked) {
							serverExchanged = serverPlayer.getMainHandItem().is(after);
							return;
						}
						ready = serverPlayer.isShiftKeyDown() && serverPlayer.getMainHandItem().is(before)
								&& serverPlayer.pick(serverPlayer.blockInteractionRange(), 0, arguments.drain()) instanceof BlockHitResult hit
								&& hit.getBlockPos().equals(target);
						if (ready) {
							for (BlockPos pos : BlockPos.betweenClosed(target.offset(-1, 0, -1), target.offset(1, 1, 1))) {
								context.journal().recordBlock(serverPlayer.serverLevel(), pos);
							}
						}
					});
				}
				RegressionActionProgress result = await(probe);
				if (result != RegressionActionProgress.PASSED) {
					if (result == RegressionActionProgress.FAILED) {
						minecraft.options.keyShift.setDown(false);
					}
					return result;
				}
				probe = null;
				if (!clicked && ready) {
					VirtualInput.pressMouseButton(1);
					VirtualInput.releaseMouseButton();
					clicked = true;
				}
				if (serverExchanged && player.getMainHandItem().is(after)) {
					minecraft.options.keyShift.setDown(false);
					return RegressionActionProgress.PASSED;
				}
				return RegressionActionProgress.WAITING;
			}
		};
	}
}
