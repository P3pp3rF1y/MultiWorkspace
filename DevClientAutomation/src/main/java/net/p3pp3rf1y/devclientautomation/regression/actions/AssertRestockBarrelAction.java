package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Reads the block inventory directly on the server and its synchronized storage menu on the client. */
public final class AssertRestockBarrelAction extends TypedRegressionAction<AssertRestockBarrelAction.Arguments> {
	public record Arguments(int diamonds, int redstone) {
	}

	public AssertRestockBarrelAction() {
		super("assert.restockBarrel", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("diamonds", "redstone"), description);
		if (!args.has("diamonds") || !args.has("redstone")) {
			throw new IllegalArgumentException("Restock barrel assertion requires both counts on " + description);
		}
		return new Arguments(optionalNonNegativeInt(args, "diamonds", 0, description), optionalNonNegativeInt(args, "redstone", 0, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> serverCheck;
		private boolean serverMatched;
		private int deadline = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (deadline < 0) {
				deadline = context.minecraft().player.tickCount + 100;
			}
			if (serverCheck == null) {
				serverCheck = context
						.onServer(player -> serverMatched = player.serverLevel().getBlockEntity(context.target()) instanceof StorageBlockEntity storage
								&& matches(storage.getStorageWrapper().getInventoryHandler(), arguments));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress checked = await(serverCheck);
			if (checked != RegressionActionProgress.PASSED) {
				return checked;
			}
			if (!serverMatched) {
				serverCheck = null;
				return context.minecraft().player.tickCount >= deadline
						? failAssertion("Server barrel contents did not match restock expectation")
						: RegressionActionProgress.WAITING;
			}
			if (context.isExpectedMenu("storageBlock") && matches((StorageContainerMenuBase<?>) context.minecraft().player.containerMenu, arguments)) {
				return RegressionActionProgress.PASSED;
			}
			return context.minecraft().player.tickCount >= deadline
					? failAssertion("Client barrel menu did not synchronize restock contents")
					: RegressionActionProgress.WAITING;
		}
	}

	private static boolean matches(InventoryHandler inventory, Arguments expected) {
		int diamonds = 0;
		int redstone = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.is(Items.DIAMOND)) {
				diamonds += stack.getCount();
			} else if (stack.is(Items.REDSTONE)) {
				redstone += stack.getCount();
			} else if (!stack.isEmpty()) {
				return false;
			}
		}
		return diamonds == expected.diamonds() && redstone == expected.redstone();
	}

	private static boolean matches(StorageContainerMenuBase<?> menu, Arguments expected) {
		int diamonds = 0;
		int redstone = 0;
		for (int slot = 0; slot < menu.getNumberOfStorageInventorySlots(); slot++) {
			ItemStack stack = menu.getSlot(slot).getItem();
			if (stack.is(Items.DIAMOND)) {
				diamonds += stack.getCount();
			} else if (stack.is(Items.REDSTONE)) {
				redstone += stack.getCount();
			} else if (!stack.isEmpty()) {
				return false;
			}
		}
		return diamonds == expected.diamonds() && redstone == expected.redstone();
	}
}
