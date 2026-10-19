package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.Set;

public final class AwaitPlayerCraftingResultAction extends TypedRegressionAction<AwaitPlayerCraftingResultAction.Arguments> {
	public record Arguments(int timeoutTicks) {
	}

	public AwaitPlayerCraftingResultAction() {
		super("await.playerCraftingResult", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("timeoutTicks"), description);
		return new Arguments(optionalPositiveInt(args, "timeoutTicks", 100, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (context.minecraft().player == null) {
					return fail("Client player is unavailable");
				}
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + arguments.timeoutTicks();
				}
				if (context.minecraft().screen instanceof InventoryScreen screen && screen.getMenu().getSlot(0).hasItem()) {
					return RegressionActionProgress.PASSED;
				}
				if (context.minecraft().player.tickCount >= deadline) {
					return fail("Player crafting result did not appear; client slots=" + context.minecraft().player.inventoryMenu.getItems());
				}
				return RegressionActionProgress.WAITING;
			}
		};
	}
}
