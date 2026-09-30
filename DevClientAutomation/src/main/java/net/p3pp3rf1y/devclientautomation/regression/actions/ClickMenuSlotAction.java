package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;

import java.util.Set;

public final class ClickMenuSlotAction extends TypedRegressionAction<ClickMenuSlotAction.Arguments> {
	public record Arguments(int slot) {
	}

	public ClickMenuSlotAction() {
		super("menu.clickSlot", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
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
			if (!(minecraft.gui.screen() instanceof BackpackScreen screen) || !(minecraft.player.containerMenu instanceof BackpackContainer menu)) {
				return fail("Backpack menu is not active for physical slot click");
			}
			if (arguments.slot() >= menu.getNumberOfStorageInventorySlots()) {
				return fail("Backpack menu has no storage slot " + arguments.slot());
			}
			Slot slot = menu.getSlot(arguments.slot());
			double x = screen.getGuiLeft() + slot.x + 8;
			double y = screen.getGuiTop() + slot.y + 8;
			MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
			if (!screen.mouseClicked(event, false)) {
				return fail("Backpack menu did not handle physical slot click");
			}
			screen.mouseReleased(event);
			return RegressionActionProgress.PASSED;
		}
	}
}
