package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.Set;

public final class OpenPlayerInventoryAction extends TypedRegressionAction<OpenPlayerInventoryAction.Arguments> {
	public record Arguments() {
	}

	public OpenPlayerInventoryAction() {
		super("player.openInventory", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(), description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (context.minecraft().gui.screen() != null || context.minecraft().player == null) {
					return fail("Player must be in the world with no screen open");
				}
				context.minecraft().gui.setScreen(new InventoryScreen(context.minecraft().player));
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
