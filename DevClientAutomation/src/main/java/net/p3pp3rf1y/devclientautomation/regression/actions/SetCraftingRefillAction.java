package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeTab;

import java.util.ArrayList;
import java.util.Set;

public final class SetCraftingRefillAction extends TypedRegressionAction<SetCraftingRefillAction.Arguments> {
	public record Arguments(int upgrade, boolean enabled) {
	}

	public SetCraftingRefillAction() {
		super("menu.setCraftingRefill", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade", "enabled"), description);
		if (!args.has("enabled") || !args.get("enabled").isJsonPrimitive() || !args.getAsJsonPrimitive("enabled").isBoolean()) {
			throw new IllegalArgumentException(description + " enabled must be a boolean");
		}
		return new Arguments(optionalNonNegativeInt(args, "upgrade", 0, description), args.get("enabled").getAsBoolean());
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof StorageScreenBase<?> screen)
						|| !(context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu) || screen.getMenu() != menu
						|| !(menu.getUpgradeContainers().get(arguments.upgrade()) instanceof CraftingUpgradeContainer crafting)) {
					return fail("Crafting upgrade is not active at upgrade " + arguments.upgrade());
				}
				if (!crafting.isOpen()) {
					return fail("Crafting upgrade tab is not open");
				}
				if (crafting.shouldRefillCraftingGrid() == arguments.enabled()) {
					return RegressionActionProgress.PASSED;
				}
				int tabIndex = new ArrayList<>(menu.getUpgradeContainers().keySet()).indexOf(arguments.upgrade()) + 1;
				if (!(screen.getUpgradeSettingsControl().children().get(tabIndex) instanceof CraftingUpgradeTab tab)) {
					return fail("Crafting upgrade tab is missing");
				}
				// CraftingUpgradeTab places the refill toggle at (x + 21, y + 24); hit its center.
				if (!VirtualInput.click(screen, tab.getX() + 29, tab.getTopY() + 32, InputConstants.MOUSE_BUTTON_LEFT, 0)
						|| crafting.shouldRefillCraftingGrid() != arguments.enabled()) {
					return fail("Crafting refill toggle was not changed by the screen");
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
