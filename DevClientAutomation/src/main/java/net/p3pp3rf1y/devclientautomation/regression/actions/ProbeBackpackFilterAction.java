package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Exercises the backpack's production input/output handler; expected observations live in the suite. */
public final class ProbeBackpackFilterAction extends TypedRegressionAction<ProbeBackpackFilterAction.Arguments> {
	public record Arguments(String operation, String item, int slot, int returnedCount) {
	}

	public ProbeBackpackFilterAction() {
		super("backpack.probeFilterIO", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("operation", "item", "slot", "returnedCount"), description);
		String operation = requireString(args, "operation", description);
		String item = requireString(args, "item", description);
		int returnedCount = optionalNonNegativeInt(args, "returnedCount", -1, description);
		int slot = optionalNonNegativeInt(args, "slot", 0, description);
		if (!Set.of("input", "output").contains(operation) || !Set.of("minecraft:diamond", "minecraft:redstone").contains(item) || !args.has("returnedCount")
				|| returnedCount > 1 || (operation.equals("output") && !args.has("slot"))) {
			throw new IllegalArgumentException("Invalid filter IO probe on " + description);
		}
		return new Arguments(operation, item, slot, returnedCount);
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
					ItemStack backpack = player.getMainHandItem();
					if (!(backpack.getItem() instanceof BackpackItem)) {
						throw new IllegalStateException("Main hand does not contain a backpack");
					}
					BackpackWrapper wrapper = new BackpackWrapper(backpack);
					Item item = arguments.item().equals("minecraft:diamond") ? Items.DIAMOND : Items.REDSTONE;
					int returned;
					try (Transaction transaction = Transaction.openRoot()) {
						returned = arguments.operation().equals("input")
								? 1 - wrapper.getInventoryForInputOutput().insert(ItemResource.of(new ItemStack(item)), 1, transaction)
								: wrapper.getInventoryForInputOutput().extract(arguments.slot(), ItemResource.of(new ItemStack(item)), 1, transaction);
						transaction.commit();
					}
					if (returned != arguments.returnedCount()) {
						throw new IllegalStateException("Filter IO " + arguments.operation() + " of " + arguments.item() + " returned " + returned
								+ ", expected count " + arguments.returnedCount());
					}
					wrapper.getInventoryHandler().saveInventory();
					wrapper.onContentsUpdated();
					player.getInventory().setChanged();
					player.inventoryMenu.broadcastFullState();
				});
			}
			return await(future);
		}
	}
}
