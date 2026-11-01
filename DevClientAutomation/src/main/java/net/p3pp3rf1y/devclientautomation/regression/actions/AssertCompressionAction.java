package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertCompressionAction extends TypedRegressionAction<AssertCompressionAction.Arguments> {
	public record Arguments(int multiple, int missing) {
	}
	public AssertCompressionAction() {
		super("assert.storage.compression", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("multiple", "missing"), description);
		return new Arguments(optionalNonNegativeInt(args, "multiple", 90, description), optionalNonNegativeInt(args, "missing", 0, description));
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.serverLevel().getBlockEntity(context.target()) instanceof StorageBlockEntity storage)) {
							throw RegressionActionFailure.infrastructure("Missing compression storage");
						}
						var inventory = storage.getStorageWrapper().getInventoryHandler();
						long total = assertContents(inventory);
						long expected = (long) inventory.getBaseStackLimit(new ItemStack(Items.IRON_BLOCK)) * arguments.multiple() - arguments.missing();
						observe(Map.of("total", expected), Map.of("total", total));
						if (total != expected) {
							throw RegressionActionFailure.assertion("Compression conservation: " + expectedActual());
						}
					});
				}
				return await(future);
			}
		};
	}

	static long assertContents(InventoryHandler inventory) {
		List<Item> items = List.of(Items.IRON_BLOCK, Items.IRON_INGOT, Items.IRON_NUGGET);
		long calculated = 0;
		for (int slot = 0; slot < 3; slot++) {
			int raw = inventory.getSlotStack(slot).getCount();
			calculated = calculated * (slot == 0 ? 1 : 9) + raw;
			ItemStack stack = inventory.getStackInSlot(slot);
			if (!stack.is(items.get(slot)) || stack.getCount() != calculated) {
				throw RegressionActionFailure.assertion("Incorrect calculated compression slot " + slot + ": " + stack + " expected " + calculated);
			}
			ItemStackKey key = ItemStackKey.of(stack);
			boolean full = calculated >= inventory.getStackLimit(slot, stack);
			if (inventory.getSlotTracker().getFullStacks().contains(key) != full || inventory.getSlotTracker().getPartialStacks().contains(key) == full) {
				throw RegressionActionFailure.assertion("Incorrect calculated tracker classification in slot " + slot);
			}
			if (slot > 0 && raw >= 9 && inventory.getSlotStack(slot - 1).getCount() < inventory.getBaseStackLimit(new ItemStack(items.get(slot - 1)))) {
				throw RegressionActionFailure.assertion("Unsettled compression carry in slot " + slot);
			}
		}
		return calculated;
	}
}
