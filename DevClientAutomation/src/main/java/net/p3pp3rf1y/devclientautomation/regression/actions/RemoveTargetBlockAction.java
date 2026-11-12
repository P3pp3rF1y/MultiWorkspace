package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class RemoveTargetBlockAction extends TypedRegressionAction<RemoveTargetBlockAction.Arguments> {
	private static final String MODE_ARGUMENT = "mode";
	private static final String DESTROY_MODE = "destroy";
	private static final String SET_AIR_MODE = "setAir";
	private static final String EXPLOSION_MODE = "explosion";

	public record Arguments(int x, int y, int z, String mode) {
	}

	public RemoveTargetBlockAction() {
		super("world.removeTargetBlock", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(PlaceHeldBlockAction.OFFSET_ARGUMENT, MODE_ARGUMENT), description);
		JsonObject position = new JsonObject();
		if (args.has(PlaceHeldBlockAction.OFFSET_ARGUMENT))
			position.add(PlaceHeldBlockAction.OFFSET_ARGUMENT, args.get(PlaceHeldBlockAction.OFFSET_ARGUMENT));
		PlaceHeldBlockAction.Arguments offset = new PlaceHeldBlockAction().parseArguments(position, description);
		String mode = optionalString(args, MODE_ARGUMENT, DESTROY_MODE, description);
		if (!Set.of(DESTROY_MODE, SET_AIR_MODE, EXPLOSION_MODE).contains(mode))
			throw new IllegalArgumentException("Invalid removal mode on " + description);
		return new Arguments(offset.x(), offset.y(), offset.z(), mode);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments args) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ServerLevel level = player.serverLevel();
						BlockPos pos = context.target().offset(args.x(), args.y(), args.z());
						context.journal().recordBlock(level, pos);
						if (level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
							tank.getPrimary().ifPresent(primary -> {
								for (int index = 0; index < primary.getHeight(); index++)
									context.journal().recordBlock(level, primary.getBlockPos().above(index));
							});
						}
						AABB bounds = new AABB(pos).inflate(6);
						Set<UUID> previousDrops = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID)
								.collect(Collectors.toSet());
						switch (args.mode()) {
							case SET_AIR_MODE -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
							case DESTROY_MODE -> level.destroyBlock(pos, true);
							case EXPLOSION_MODE -> new Explosion(level, null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 1, false,
									Explosion.BlockInteraction.DESTROY_WITH_DECAY, List.of(pos)).finalizeExplosion(false);
							default -> throw new IllegalStateException(args.mode());
						}
						context.journal().recordNewItemDrops(level, bounds, previousDrops);
						observe(Map.of(MODE_ARGUMENT, args.mode()), Map.of("air", level.getBlockState(pos).isAir()));
					});
				}
				return await(future);
			}
		};
	}
}
