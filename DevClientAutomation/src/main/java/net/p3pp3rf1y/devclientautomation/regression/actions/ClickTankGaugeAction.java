package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.TankScreen;

import java.util.Map;
import java.util.Set;

public final class ClickTankGaugeAction extends TypedRegressionAction<Boolean> {
	public ClickTankGaugeAction() {
		super("menu.clickTankGauge", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}

	@Override
	protected Boolean parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(), description);
		return true;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Boolean arguments) {
		return new TickRegressionActionExecution(context) {
			private int ticks;

			@Override
			public RegressionActionProgress tick() {
				if (ticks++ == 0) {
					if (!(context.minecraft().screen instanceof TankScreen screen)) {
						return fail("Native tank screen is not active");
					}
					VirtualInput.click(screen, screen.getGuiLeft() + 16, screen.getGuiTop() + 50, 0, 0);
					observe(Map.of("gauge", true), Map.of("clicked", true));
				}
				return ticks >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
			}
		};
	}
}
