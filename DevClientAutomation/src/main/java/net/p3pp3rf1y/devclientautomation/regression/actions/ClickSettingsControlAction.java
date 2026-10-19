package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.Tab;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsTab;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsTab;

import java.util.Set;

public final class ClickSettingsControlAction extends TypedRegressionAction<ClickSettingsControlAction.Arguments> {
	public enum Operation {
		MEMORY, DISPLAY, SLOT
	}
	public record Arguments(Operation operation, int slot) {
	}
	public ClickSettingsControlAction() {
		super("menu.clickSettingsControl", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("operation", "slot"), description);
		Operation operation = switch (requireString(args, "operation", description)) {
			case "memory" -> Operation.MEMORY;
			case "display" -> Operation.DISPLAY;
			case "slot" -> Operation.SLOT;
			default -> throw new IllegalArgumentException(description + " has an unknown settings operation");
		};
		return new Arguments(operation, optionalNonNegativeInt(args, "slot", 0, description));
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int ticks;
			@Override
			public RegressionActionProgress tick() {
				if (ticks++ == 0) {
					if (!(context.minecraft().screen instanceof SettingsScreen screen)) {
						return fail("Settings screen is not open");
					}
					int x;
					int y;
					if (arguments.operation() == Operation.SLOT) {
						Slot slot = screen.getMenu().getStorageInventorySlots().get(arguments.slot());
						x = screen.getGuiLeft() + slot.x + 8;
						y = screen.getGuiTop() + slot.y + 8;
					} else {
						Tab tab = screen.getSettingsTabControl().children().stream()
								.filter(child -> arguments.operation() == Operation.MEMORY
										? child instanceof MemorySettingsTab
										: child instanceof ItemDisplaySettingsTab)
								.map(Tab.class::cast).findFirst().orElseThrow(() -> new IllegalStateException("Settings tab is missing"));
						x = tab.getX() + 8;
						y = tab.getTopY() + 12;
					}
					screen.mouseMoved(x, y);
					MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
					screen.mouseClicked(event, false);
					screen.mouseReleased(event);
				}
				return ticks >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
			}
		};
	}
}
