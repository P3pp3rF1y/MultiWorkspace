package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertPlayerInventoryBaselineAction extends TypedRegressionAction<AssertPlayerInventoryBaselineAction.Arguments> {
	public record Arguments() {
	}
	public AssertPlayerInventoryBaselineAction() {
		super("assert.playerInventoryBaseline", "1", Set.of("minecraft:client"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(), description);
		return new Arguments();
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						if (!player.getInventory().isEmpty() || player.getInventory().selected != 0 || !player.inventoryMenu.getCarried().isEmpty()) {
							throw RegressionActionFailure.assertion("Player inventory baseline is not empty");
						}
						for (int slot = 1; slot <= 4; slot++) {
							if (player.inventoryMenu.getSlot(slot).hasItem()) {
								throw RegressionActionFailure.assertion("Crafting baseline is not empty");
							}
						}
					});
				}
				return await(future);
			}
		};
	}
}
