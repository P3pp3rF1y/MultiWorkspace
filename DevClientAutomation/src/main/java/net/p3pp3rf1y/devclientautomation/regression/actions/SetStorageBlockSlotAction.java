package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetStorageBlockSlotAction extends TypedRegressionAction<SetStorageBlockSlotAction.Arguments> {
	public record Arguments(int slot, String item, int count) {
	}

	public SetStorageBlockSlotAction() {
		super("storage.setBlockSlot", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "item", "count"), description);
		return new Arguments(optionalNonNegativeInt(args, "slot", 0, description), requireString(args, "item", description),
				requirePositiveInt(args, "count", description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		@Nullable
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					if (!(player.serverLevel().getBlockEntity(context.target()) instanceof StorageBlockEntity storage)) {
						throw new IllegalStateException("Target is not a storage block");
					}
					if (arguments.slot() >= storage.getStorageWrapper().getInventoryHandler().getSlots()) {
						throw new IllegalArgumentException("Storage slot " + arguments.slot() + " is outside the block inventory");
					}
					Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(arguments.item()))
							.orElseThrow(() -> new IllegalArgumentException("Unknown storage item " + arguments.item()));
					context.journal().recordBlock(player.serverLevel(), context.target());
					storage.getStorageWrapper().getInventoryHandler().setStackInSlot(arguments.slot(), new ItemStack(item, arguments.count()));
					storage.getStorageWrapper().getInventoryHandler().saveInventory();
					storage.setChanged();
					player.serverLevel().sendBlockUpdated(context.target(), storage.getBlockState(), storage.getBlockState(), 3);
				});
			}
			return await(future);
		}
	}
}
