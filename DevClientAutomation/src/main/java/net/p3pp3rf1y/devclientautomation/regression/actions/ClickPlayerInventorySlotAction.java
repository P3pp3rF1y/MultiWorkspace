package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.Slot;

import java.util.Set;

public final class ClickPlayerInventorySlotAction extends TypedRegressionAction<ClickPlayerInventorySlotAction.Arguments> {
	public record Arguments(int slot, int button, String area) {
	}

	public ClickPlayerInventorySlotAction() {
		super("menu.clickPlayerInventorySlot", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "button", "area"), description);
		String area = optionalString(args, "area", "inventory", description);
		int slot = optionalNonNegativeInt(args, "slot", 0, description);
		int button = optionalNonNegativeInt(args, "button", 0, description);
		int maxSlot = switch (area) {
			case "inventory" -> 35;
			case "crafting" -> 3;
			case "result" -> 0;
			default -> throw new IllegalArgumentException("Unknown inventory area on " + description);
		};
		if (slot > maxSlot || button > 1) {
			throw new IllegalArgumentException("Invalid slot or mouse button on " + description);
		}
		return new Arguments(slot, button, area);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof InventoryScreen screen)) {
					return fail("Player inventory screen is not active");
				}
				Slot slot = arguments.area().equals("inventory")
						? screen.getMenu().slots.stream()
								.filter(candidate -> candidate.container == context.minecraft().player.getInventory()
										&& candidate.getSlotIndex() == arguments.slot())
								.findFirst().orElseThrow()
						: screen.getMenu().getSlot(arguments.area().equals("result") ? 0 : arguments.slot() + 1);
				double x = screen.getGuiLeft() + slot.x + 8;
				double y = screen.getGuiTop() + slot.y + 8;
				screen.mouseMoved(x, y);
				MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(arguments.button(), 0));
				screen.mouseClicked(event, false);
				screen.mouseReleased(event);
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
