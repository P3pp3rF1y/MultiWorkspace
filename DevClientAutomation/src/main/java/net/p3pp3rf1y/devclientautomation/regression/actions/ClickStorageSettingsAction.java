package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;

import java.util.Set;
import java.util.UUID;

public final class ClickStorageSettingsAction extends TypedRegressionAction<ClickStorageSettingsAction.Arguments> {
	public record Arguments() {
	}

	public ClickStorageSettingsAction() {
		super("menu.clickStorageSettings", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(), description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private boolean clicked;

			@Override
			public RegressionActionProgress tick() {
				if (!clicked) {
					if (!(context.minecraft().screen instanceof StorageScreenBase<?> screen)) {
						return fail("Storage inventory screen is not open");
					}
					context.values().put("settings-group", UUID.class, screen.getMenu().getLinkedStorageGroupId().orElseThrow());
					double x = screen.getUpgradeSettingsControl().getX() + 8;
					double y = screen.getUpgradeSettingsControl().getY() + 12;
					screen.mouseMoved(x, y);
					MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
					screen.mouseClicked(event, false);
					screen.mouseReleased(event);
					clicked = true;
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
