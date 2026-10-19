package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CopyPlayerInventoryItemAction extends TypedRegressionAction<CopyPlayerInventoryItemAction.Arguments> {
	public record Arguments(int source, int target) {
	}

	public CopyPlayerInventoryItemAction() {
		super("fixture.player.copyInventoryItem", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("source", "target"), description);
		int source = optionalNonNegativeInt(args, "source", 0, description);
		int target = optionalNonNegativeInt(args, "target", 10, description);
		if (source > 35 || target > 35 || source == target) {
			throw new IllegalArgumentException(description + " requires distinct inventory slots between 0 and 35");
		}
		return new Arguments(source, target);
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
						player.getInventory().setItem(arguments.target(), player.getInventory().getItem(arguments.source()).copy());
						player.inventoryMenu.broadcastFullState();
					});
				}
				return await(future);
			}
		};
	}
}
