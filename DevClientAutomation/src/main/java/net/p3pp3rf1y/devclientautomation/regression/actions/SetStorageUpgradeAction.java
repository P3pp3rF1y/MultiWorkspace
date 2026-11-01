package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetStorageUpgradeAction extends TypedRegressionAction<SetStorageUpgradeAction.Arguments> {
	public record Arguments(String item, int slot, boolean enabled) {
	}
	public SetStorageUpgradeAction() {
		super("fixture.storage.setUpgrade", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item", "slot", "enabled"), description);
		return new Arguments(requireString(args, "item", description), optionalNonNegativeInt(args, "slot", 0, description),
				optionalBoolean(args, "enabled", true, description));
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null)
					future = context.onServer(player -> {
						if (!(player.serverLevel().getBlockEntity(context.target()) instanceof StorageBlockEntity storage))
							throw new IllegalStateException("Target is not a storage block");
						var upgrades = storage.getStorageWrapper().getUpgradeHandler();
						if (arguments.slot() >= upgrades.getSlots())
							throw new IllegalArgumentException("Storage upgrade slot " + arguments.slot() + " is outside the block upgrade inventory");
						Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown storage upgrade item " + arguments.item()));
						if (!(item instanceof IUpgradeItem<?>))
							throw new IllegalArgumentException("Storage fixture item is not an upgrade " + arguments.item());
						context.journal().recordBlock(player.serverLevel(), context.target());
						upgrades.setStackInSlot(arguments.slot(), new ItemStack(item));
						upgrades.getSlotWrappers().get(arguments.slot()).setEnabled(arguments.enabled());
						upgrades.saveInventory();
						storage.setChanged();
						player.serverLevel().sendBlockUpdated(context.target(), storage.getBlockState(), storage.getBlockState(), 3);
					});
				return await(future);
			}
		};
	}
}
