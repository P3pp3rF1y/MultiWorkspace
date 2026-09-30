package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;

import java.util.Set;

public final class QuickMoveBackpackStorageSlotAction extends TypedRegressionAction<QuickMoveBackpackStorageSlotAction.Arguments> {
	public record Arguments(int slot) {
	}

	public QuickMoveBackpackStorageSlotAction() {
		super("menu.quickMoveBackpackStorageSlot", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot"), description);
		return new Arguments(optionalNonNegativeInt(args, "slot", 0, description));
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
				return fail("Backpack menu is not active for storage quick move");
			}
			if (arguments.slot() >= menu.getNumberOfStorageInventorySlots()) {
				return fail("Backpack menu has no storage slot " + arguments.slot());
			}
			minecraft.gameMode.handleContainerInput(menu.containerId, arguments.slot(), 0, ContainerInput.QUICK_MOVE, minecraft.player);
			return RegressionActionProgress.PASSED;
		}
	}
}
