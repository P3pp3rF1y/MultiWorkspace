package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;

import java.util.Set;

public final class ClickSortControlAction extends TypedRegressionAction<ClickSortControlAction.Arguments> {
	public record Arguments(String control) {
	}

	public ClickSortControlAction() {
		super("menu.clickSortControl", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("control"), description);
		String control = requireString(args, "control", description);
		if (!Set.of("sort", "mode").contains(control)) {
			throw new IllegalArgumentException(description + ": control must be sort or mode");
		}
		return new Arguments(control);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().screen instanceof StorageScreenBase<?> screen)) {
					return fail("Storage inventory screen is not open");
				}
				var rectangle = screen.getSortButtonsRectangle();
				if (rectangle.isEmpty()) {
					return fail("Storage sort controls are hidden");
				}
				double x = rectangle.get().getX() + (arguments.control().equals("mode") ? 18 : 6);
				double y = rectangle.get().getY() + 6;
				screen.mouseMoved(x, y);
				MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
				screen.mouseClicked(event, false);
				screen.mouseReleased(event);
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
