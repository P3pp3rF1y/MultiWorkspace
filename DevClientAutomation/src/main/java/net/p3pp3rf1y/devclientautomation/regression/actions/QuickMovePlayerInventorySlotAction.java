package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;

import java.util.Set;

public final class QuickMovePlayerInventorySlotAction extends TypedRegressionAction<QuickMovePlayerInventorySlotAction.Arguments> {
	public record Arguments(int inventorySlot) {
	}

	public QuickMovePlayerInventorySlotAction() {
		super("menu.quickMovePlayerInventorySlot", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("inventorySlot"), description);
		int inventorySlot = requirePositiveInt(args, "inventorySlot", description);
		if (inventorySlot < 9 || inventorySlot > 35) {
			throw new IllegalArgumentException("Argument inventorySlot on " + description + " must be a main-inventory slot from 9 through 35");
		}
		return new Arguments(inventorySlot);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (!(minecraft.player.containerMenu instanceof BackpackContainer menu) || minecraft.gameMode == null) {
				return fail("Backpack menu is not active for quick move");
			}
			int menuSlot = menu.getNumberOfStorageInventorySlots() + arguments.inventorySlot() - 9;
			minecraft.gameMode.handleContainerInput(menu.containerId, menuSlot, 0, ContainerInput.QUICK_MOVE, minecraft.player);
			return RegressionActionProgress.PASSED;
		}
	}
}
