package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class RecoverTankDropAction extends TypedRegressionAction<Boolean> {
	public RecoverTankDropAction() {
		super("fixture.tank.recoverDrop", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}

	@Override
	protected Boolean parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(), description);
		return true;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Boolean args) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class, new AABB(context.target()).inflate(6),
								entity -> entity.getItem().getItem() instanceof TankBlockItem);
						if (drops.size() != 1)
							throw RegressionActionFailure.infrastructure("Expected exactly one recoverable tank drop, found " + drops.size());
						ItemEntity drop = drops.getFirst();
						context.journal().recordExistingEntity(drop);
						player.getInventory().setItem(player.getInventory().getSelectedSlot(), drop.getItem().copy());
						drop.discard();
						player.inventoryMenu.broadcastChanges();
						observe(Map.of("drops", 1), Map.of("held", player.getMainHandItem().toString()));
					});
				}
				return await(future);
			}
		};
	}
}
