package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

import javax.annotation.Nullable;

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
						List<ItemEntity> drops = player.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(context.target()).inflate(6),
								entity -> entity.getItem().getItem() instanceof TankBlockItem);
						if (drops.size() > 1)
							throw RegressionActionFailure.infrastructure("Expected exactly one recoverable tank drop, found " + drops.size());
						String origin;
						if (drops.size() == 1) {
							ItemEntity drop = drops.get(0);
							context.journal().recordExistingEntity(drop);
							player.getInventory().setItem(player.getInventory().selected, drop.getItem().copy());
							drop.discard();
							origin = "world";
						} else {
							int foundSlot = -1;
							for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
								ItemStack stack = player.getInventory().items.get(slot);
								if (stack.getItem() instanceof TankBlockItem) {
									if (foundSlot >= 0 || stack.getCount() != 1)
										throw RegressionActionFailure.infrastructure("Expected one picked-up tank stack, found multiple or stacked tanks");
									foundSlot = slot;
								}
							}
							if (foundSlot < 0)
								throw RegressionActionFailure.infrastructure("Expected one recoverable tank drop or picked-up tank stack; found neither");
							ItemStack recovered = player.getInventory().items.get(foundSlot).copy();
							player.getInventory().setItem(foundSlot, ItemStack.EMPTY);
							player.getInventory().setItem(player.getInventory().selected, recovered);
							origin = "player-inventory";
						}
						player.inventoryMenu.broadcastChanges();
						observe(Map.of("recoverableTanks", 1), Map.of("held", player.getMainHandItem().toString(), "origin", origin));
					});
				}
				return await(future);
			}
		};
	}
}
