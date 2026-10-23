package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
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
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.producing("settings-group", RegressionValueTypes.UUID);
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
					screen.mouseClicked(x, y, 0);
					screen.mouseReleased(x, y, 0);
					clicked = true;
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
