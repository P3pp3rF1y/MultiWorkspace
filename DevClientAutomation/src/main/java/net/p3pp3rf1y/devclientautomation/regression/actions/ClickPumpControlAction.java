package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeTab;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public final class ClickPumpControlAction extends TypedRegressionAction<ClickPumpControlAction.Arguments> {
	private static final String UPGRADE_ARGUMENT = "upgrade";
	private static final String CONTROL_ARGUMENT = "control";
	private static final String INPUT_CONTROL = "input";
	private static final String HANDLERS_CONTROL = "handlers";
	private static final String WORLD_CONTROL = "world";

	public record Arguments(int upgrade, String control) {
	}

	public ClickPumpControlAction() {
		super("menu.clickPumpControl", "1", Set.of(CLIENT_CAPABILITY, SophisticatedCore.MOD_ID));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(UPGRADE_ARGUMENT, CONTROL_ARGUMENT), description);
		String control = requireString(args, CONTROL_ARGUMENT, description);
		if (!Set.of(INPUT_CONTROL, HANDLERS_CONTROL, WORLD_CONTROL, "hand").contains(control)) {
			throw new IllegalArgumentException("Unknown pump control on " + description);
		}
		return new Arguments(optionalNonNegativeInt(args, UPGRADE_ARGUMENT, 0, description), control);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int ticks;

			@Override
			public RegressionActionProgress tick() {
				if (ticks++ == 0) {
					if (!(context.minecraft().gui.screen() instanceof StorageScreenBase<?> screen)) {
						return fail("Storage screen is not active");
					}
					Map<Integer, UpgradeContainerBase<?, ?>> upgrades = screen.getMenu().getUpgradeContainers();
					UpgradeContainerBase<?, ?> upgrade = upgrades.get(arguments.upgrade());
					if (upgrade == null || !upgrade.isOpen()) {
						return fail("Requested pump tab is not open");
					}
					int settingsTabs = screen.getMenu().getStorageWrapper().supportsItemContents() ? 1 : 0;
					int index = new ArrayList<>(upgrades.keySet()).indexOf(arguments.upgrade()) + settingsTabs;
					if (!(screen.getUpgradeSettingsControl().children().get(index) instanceof PumpUpgradeTab tab)
							|| !arguments.control().equals(INPUT_CONTROL) && !(tab instanceof PumpUpgradeTab.Advanced)) {
						return fail("Requested control is unavailable on this upgrade");
					}
					int offset = switch (arguments.control()) {
						case INPUT_CONTROL -> 3;
						case HANDLERS_CONTROL -> 21;
						case WORLD_CONTROL -> 39;
						default -> 57;
					};
					VirtualInput.click(screen, tab.getX() + offset + 8, tab.getTopY() + 32, InputConstants.MOUSE_BUTTON_LEFT, 0);
					observe(Map.of(UPGRADE_ARGUMENT, arguments.upgrade(), CONTROL_ARGUMENT, arguments.control()), Map.of("clicked", true));
				}
				return ticks >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
			}
		};
	}
}
