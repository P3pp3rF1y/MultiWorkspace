package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class BreakPlacedBackpackAction extends TypedRegressionAction<BreakPlacedBackpackAction.Arguments> {
	public record Arguments(int count) {
	}

	public BreakPlacedBackpackAction() {
		super("backpack.breakAndAssertDrop", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("count"), description);
		return new Arguments(requirePositiveInt(args, "count", description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ServerLevel level = player.serverLevel();
						BlockPos pos = context.target();
						if (!(level.getBlockState(pos).getBlock() instanceof BackpackBlock)) {
							throw new IllegalStateException("Backpack break requires a placed backpack");
						}
						context.journal().recordBlock(level, pos);
						AABB bounds = new AABB(pos).inflate(1.5D);
						Set<UUID> before = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(Entity::getUUID).collect(Collectors.toSet());
						GameType originalMode = player.gameMode.getGameModeForPlayer();
						try {
							player.setGameMode(GameType.SURVIVAL);
							if (!player.gameMode.destroyBlock(pos)) {
								throw new IllegalStateException("Player could not break the restored backpack");
							}
						} finally {
							try {
								level.getEntitiesOfClass(ItemEntity.class, bounds, entity -> !before.contains(entity.getUUID()))
										.forEach(context.journal()::recordCreatedEntity);
							} finally {
								player.setGameMode(originalMode);
							}
						}
						List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, bounds, entity -> !before.contains(entity.getUUID()));
						if (drops.size() != 1) {
							throw new IllegalStateException("Expected one backpack drop after break, found " + drops.size());
						}
						ItemStack stack = drops.get(0).getItem();
						IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
						ItemStack contents = wrapper.getInventoryHandler().getStackInSlot(0);
						Item upgrade = wrapper.getUpgradeHandler().getStackInSlot(1).getItem();
						context.putDiagnostic("brokenBackpackItem", stack.getItem().toString());
						context.putDiagnostic("brokenBackpackSlot", contents.getCount() + " " + contents.getItem());
						context.putDiagnostic("brokenBackpackUpgrade", upgrade.toString());
						if (!stack.is(ModItems.IRON_BACKPACK.get()) || !contents.is(Items.DIAMOND) || contents.getCount() != arguments.count()
								|| upgrade != ModItems.FILTER_UPGRADE.get()) {
							throw new IllegalStateException("Broken backpack lost its iron tier, stored diamonds or filter upgrade");
						}
					});
				}
				return await(future);
			}
		};
	}
}
