package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class BreakControllerLinkGroupsBarrelAction extends TypedRegressionAction<BreakControllerLinkGroupsBarrelAction.Arguments> {
	public record Arguments(String layout) {
	}

	public BreakControllerLinkGroupsBarrelAction() {
		super("fixture.controllerLinkGroups.breakBarrel", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("layout"), description);
		String layout = requireString(args, "layout", description);
		if (!layout.equals("shared") && !layout.equals("separate")) {
			throw new IllegalArgumentException("Argument layout on " + description + " must be shared or separate");
		}
		return new Arguments(layout);
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
					BlockPos pos = context.standingPosition().south(2).east(arguments.layout().equals("shared") ? 1 : 0);
					context.journal().recordBlock(player.level(), pos);
					if (!player.level().getBlockState(pos).is(ModBlocks.BARREL.get())) {
						throw new IllegalStateException("Barrel was not removed from the controller link groups fixture");
					}
					AABB bounds = new AABB(pos).inflate(1.5D);
					Set<UUID> before = player.level().getEntitiesOfClass(ItemEntity.class, bounds).stream().map(Entity::getUUID).collect(Collectors.toSet());
					boolean removed;
					try {
						removed = player.gameMode.destroyBlock(pos);
					} finally {
						context.journal().recordNewItemDrops(player.level(), bounds, before);
					}
					if (!removed) {
						throw new IllegalStateException("Barrel was not removed from the controller link groups fixture");
					}
				});
			}
			return await(future);
		}
	}
}
