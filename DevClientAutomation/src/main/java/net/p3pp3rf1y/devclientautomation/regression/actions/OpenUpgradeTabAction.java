package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.Tab;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public final class OpenUpgradeTabAction extends TypedRegressionAction<OpenUpgradeTabAction.Arguments> {
	public record Arguments(int upgrade) {
	}

	public OpenUpgradeTabAction() {
		super("menu.openUpgradeTab", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade"), description);
		return new Arguments(optionalNonNegativeInt(args, "upgrade", 0, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof StorageScreenBase<?> screen)) {
					return fail("Storage screen is not active for upgrade tab click");
				}
				var upgrades = screen.getMenu().getUpgradeContainers();
				UpgradeContainerBase<?, ?> upgrade = upgrades.get(arguments.upgrade());
				if (upgrade == null) {
					return fail("Menu has no upgrade " + arguments.upgrade());
				}
				if (!upgrade.isOpen()) {
					// UpgradeSettingsTabControl puts the storage-settings tab first, then the upgrades in menu order.
					int tabIndex = new ArrayList<>(upgrades.keySet()).indexOf(arguments.upgrade()) + 1;
					if (!(screen.getUpgradeSettingsControl().children().get(tabIndex) instanceof Tab tab)) {
						return fail("Upgrade tab is missing from the storage screen");
					}
					VirtualInput.click(screen, tab.getX() + 8, tab.getTopY() + 12, 0, 0);
				}
				observe(Map.of("upgrade", arguments.upgrade()), Map.of("open", upgrade.isOpen()));
				return upgrade.isOpen() ? RegressionActionProgress.PASSED : fail("Upgrade tab click did not open the requested tab");
			}
		};
	}
}
