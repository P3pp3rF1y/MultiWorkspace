package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkedStorageHostWrapper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorageinmotion.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertMovingStorageCompatibilityProfileAction extends TypedRegressionAction<Void> {
	public AssertMovingStorageCompatibilityProfileAction() {
		super("assert.movingStorageCompatibilityProfile", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Void parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return null;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Void arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						String placed = StorageLinkedStorageHostWrapper
								.getCompatibilityKey(new LimitedBarrelBlockEntity(BlockPos.ZERO, ModBlocks.LIMITED_BARREL_2.get().defaultBlockState()));
						String moving = StorageLinkedStorageHostWrapper.getCompatibilityKey(MovingStorageItem.getStorageItem(MovingStorageItem
								.createWithStorage(new ItemStack(ModItems.STORAGE_BOAT.get()), new ItemStack(ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get()))));
						String item = StorageLinkedStorageHostWrapper.getCompatibilityKey(new ItemStack(ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get()));
						if (!placed.equals(moving) || !placed.equals(item)) {
							throw new IllegalStateException("Equivalent two-slot limited barrel compatibility profiles differ");
						}
						if (placed.equals(StorageLinkedStorageHostWrapper.getCompatibilityKey(new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get())))
								|| placed.equals(StorageLinkedStorageHostWrapper.getCompatibilityKey(new ItemStack(ModBlocks.BARREL_ITEM.get())))) {
							throw new IllegalStateException("Limited barrel compatibility profile matches an incompatible storage type");
						}
					});
				}
				return await(future);
			}
		};
	}
}
