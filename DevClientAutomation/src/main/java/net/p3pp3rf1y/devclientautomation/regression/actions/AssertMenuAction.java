package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertMenuAction extends TypedRegressionAction<AssertMenuAction.Arguments> {
	public record Arguments(String kind, int slot, String item, int count, boolean playerInventory, int stableTicks) {
	}

	public AssertMenuAction() {
		super("assert.menu", "2", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "slot", "item", "count", "playerInventory", "stableTicks"), description);
		String kind = optionalString(args, "kind", "backpackBlock", description);
		if (!Set.of("backpackBlock", "backpackItem", "storageBlock").contains(kind)) {
			throw new IllegalArgumentException("Argument kind on " + description + " must be backpackBlock, backpackItem, or storageBlock");
		}
		boolean playerInventory = args.has("playerInventory") && args.get("playerInventory").isJsonPrimitive()
				&& args.get("playerInventory").getAsJsonPrimitive().isBoolean() && args.get("playerInventory").getAsBoolean();
		if (args.has("playerInventory") && (!args.get("playerInventory").isJsonPrimitive() || !args.get("playerInventory").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument playerInventory on " + description + " must be a boolean");
		}
		return new Arguments(kind, optionalNonNegativeInt(args, "slot", 0, description), requireString(args, "item", description),
				optionalNonNegativeInt(args, "count", 1, description), playerInventory, optionalNonNegativeInt(args, "stableTicks", 0, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> serverAssertion;
		private boolean serverMatched;
		private String serverInventory;
		private int deadline = -1;
		private int matchedSince = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (serverAssertion == null) {
				serverAssertion = context.onServer(player -> {
					StorageContainerMenuBase<?> menu = (StorageContainerMenuBase<?>) player.containerMenu;
					serverMatched = matchesStack(menu);
					if (!serverMatched && arguments.playerInventory()) {
						serverInventory = describeInventory(menu);
					}
				});
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress server = await(serverAssertion);
			if (server != RegressionActionProgress.PASSED) {
				return server;
			}
			if (!serverMatched) {
				serverAssertion = null;
				matchedSince = -1;
				return minecraft.player.tickCount >= deadline
						? fail("Server menu did not receive the expected item stack" + (serverInventory == null ? "" : "; inventory=" + serverInventory))
						: RegressionActionProgress.WAITING;
			}
			if (!context.isExpectedMenu(arguments.kind())) {
				return minecraft.player.tickCount >= deadline
						? fail("Expected " + arguments.kind() + " menu closed before client synchronization")
						: RegressionActionProgress.WAITING;
			}
			if (!matchesStack((StorageContainerMenuBase<?>) minecraft.player.containerMenu)) {
				matchedSince = -1;
				return minecraft.player.tickCount >= deadline
						? fail("Client menu did not synchronize the expected item stack")
						: RegressionActionProgress.WAITING;
			}
			if (matchedSince < 0) {
				matchedSince = minecraft.player.tickCount;
			}
			if (minecraft.player.tickCount - matchedSince >= arguments.stableTicks()) {
				return RegressionActionProgress.PASSED;
			}
			serverAssertion = null;
			return minecraft.player.tickCount >= deadline ? fail("Menu items did not remain synchronized long enough") : RegressionActionProgress.WAITING;
		}

		private boolean matchesStack(StorageContainerMenuBase<?> menu) {
			if (arguments.playerInventory()) {
				for (int i = menu.getNumberOfStorageInventorySlots(); i < menu.getInventorySlotsSize(); i++) {
					if (matchesStack(menu.getSlot(i).getItem())) {
						return true;
					}
				}
				return false;
			}
			if (arguments.slot() >= menu.getNumberOfStorageInventorySlots()) {
				throw new IllegalStateException(
						"Menu has " + menu.getNumberOfStorageInventorySlots() + " storage slots; cannot assert slot " + arguments.slot());
			}
			return matchesStack(menu.getSlot(arguments.slot()).getItem());
		}

		private boolean matchesStack(ItemStack stack) {
			return arguments.count() == 0 ? stack.isEmpty() : stack.is(itemForId(arguments.item())) && stack.getCount() == arguments.count();
		}

		private static String describeInventory(StorageContainerMenuBase<?> menu) {
			StringBuilder result = new StringBuilder();
			for (int i = menu.getNumberOfStorageInventorySlots(); i < menu.getInventorySlotsSize(); i++) {
				ItemStack stack = menu.getSlot(i).getItem();
				if (!stack.isEmpty()) {
					result.append(i).append('=').append(stack).append(' ');
				}
			}
			return result.toString();
		}

		private static Item itemForId(String itemId) {
			return BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId))
					.orElseThrow(() -> new IllegalArgumentException("Unknown asserted item " + itemId));
		}
	}
}
