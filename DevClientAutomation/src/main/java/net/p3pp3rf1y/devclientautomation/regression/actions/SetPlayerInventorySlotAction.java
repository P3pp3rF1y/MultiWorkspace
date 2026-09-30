package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetPlayerInventorySlotAction extends TypedRegressionAction<SetPlayerInventorySlotAction.Arguments> {
	public record Arguments(int slot, String item, int count, boolean clearMatching) {
	}

	public SetPlayerInventorySlotAction() {
		super("player.setInventorySlot", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "item", "count", "clearMatching"), description);
		if (args.has("clearMatching") && (!args.get("clearMatching").isJsonPrimitive() || !args.get("clearMatching").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument clearMatching on " + description + " must be a boolean");
		}
		boolean clearMatching = args.has("clearMatching") && args.get("clearMatching").getAsBoolean();
		return new Arguments(requirePositiveInt(args, "slot", description), requireString(args, "item", description),
				requirePositiveInt(args, "count", description), clearMatching);
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
					if (arguments.slot() >= player.getInventory().getContainerSize()) {
						throw new IllegalArgumentException("Player inventory slot " + arguments.slot() + " is outside the inventory");
					}
					Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
							.orElseThrow(() -> new IllegalArgumentException("Unknown player inventory item " + arguments.item()));
					if (arguments.clearMatching()) {
						for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
							if (player.getInventory().getItem(slot).is(item)) {
								player.getInventory().setItem(slot, ItemStack.EMPTY);
							}
						}
					}
					player.getInventory().setItem(arguments.slot(), new ItemStack(item, arguments.count()));
					player.getInventory().setChanged();
					player.inventoryMenu.broadcastFullState();
				});
			}
			return await(future);
		}
	}
}
