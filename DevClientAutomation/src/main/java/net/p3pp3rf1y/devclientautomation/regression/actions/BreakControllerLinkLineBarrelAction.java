package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class BreakControllerLinkLineBarrelAction extends TypedRegressionAction<BreakControllerLinkLineBarrelAction.Arguments> {
	public record Arguments() {
	}

	public BreakControllerLinkLineBarrelAction() {
		super("fixture.controllerLinkLine.breakFirstBarrel", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					BlockPos first = context.standingPosition().south(2).west(3);
					AABB bounds = new AABB(first).inflate(3);
					var before = player.level().getEntitiesOfClass(ItemEntity.class, bounds).stream().map(Entity::getUUID).collect(Collectors.toSet());
					context.journal().recordBlock(player.level(), first);
					try {
						if (!player.level().getBlockState(first).is(ModBlocks.BARREL.get()) || !player.gameMode.destroyBlock(first)) {
							throw new IllegalStateException("First barrel was not removed from the controller link fixture");
						}
					} finally {
						context.journal().recordNewItemDrops(player.level(), bounds, before);
					}
				});
			}
			return await(future);
		}
	}
}
