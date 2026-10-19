package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PickupPlacedBackpackAction extends TypedRegressionAction<PickupPlacedBackpackAction.Arguments> {
	public record Arguments() {
	}

	public PickupPlacedBackpackAction() {
		super("backpack.pickupPlaced", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						BlockState state = player.serverLevel().getBlockState(context.target());
						if (!(state.getBlock() instanceof BackpackBlock backpackBlock) || !player.getMainHandItem().isEmpty()) {
							throw new IllegalStateException("Backpack pickup requires a placed backpack and an empty main hand");
						}
						context.journal().recordBlock(player.serverLevel(), context.target());
						player.setShiftKeyDown(true);
						try {
							InteractionResult result = backpackBlock.use(state, player.serverLevel(), context.target(), player, InteractionHand.MAIN_HAND,
									new BlockHitResult(Vec3.atCenterOf(context.target()), Direction.UP, context.target(), false));
							if (result != InteractionResult.SUCCESS || !player.serverLevel().getBlockState(context.target()).isAir()) {
								throw new IllegalStateException("Sneak-use did not pick up the restored backpack");
							}
						} finally {
							player.setShiftKeyDown(false);
						}
					});
				}
				return await(future);
			}
		};
	}
}
