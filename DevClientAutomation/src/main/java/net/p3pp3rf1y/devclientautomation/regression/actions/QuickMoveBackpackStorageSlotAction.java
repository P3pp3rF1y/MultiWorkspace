package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
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
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = context.minecraft();
				if (!(minecraft.screen instanceof StorageScreenBase<?> screen) || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
						|| screen.getMenu() != menu)
					return fail("Backpack menu is not active for quick move");
				if (arguments.slot() >= menu.getNumberOfStorageInventorySlots())
					return fail("Backpack storage slot " + arguments.slot() + " is outside the menu");
				Slot slot = menu.getSlot(arguments.slot());
				VirtualInput.moveToSlot(screen, slot.index);
				if (!VirtualInput.click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8, 0, 1)) {
					return fail("Backpack quick-move click was not handled by the screen");
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
