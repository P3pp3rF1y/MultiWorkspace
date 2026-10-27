package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;

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
			if (!(minecraft.gui.screen() instanceof StorageScreenBase<?> screen) || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
					|| screen.getMenu() != menu) {
				return fail("Backpack menu is not active for storage quick move");
			}
			if (arguments.slot() >= menu.getNumberOfStorageInventorySlots()) {
				return fail("Backpack menu has no storage slot " + arguments.slot());
			}
			Slot slot = menu.getSlot(arguments.slot());
			VirtualInput.moveToSlot(screen, slot.index);
			if (!VirtualInput.click(screen, screen.getLeftPos() + slot.x + 8, screen.getTopPos() + slot.y + 8, InputConstants.MOUSE_BUTTON_LEFT,
					InputConstants.MOD_SHIFT)) {
				return fail("Backpack quick-move click was not handled by the screen");
			}
			return RegressionActionProgress.PASSED;
		}
	}
}
