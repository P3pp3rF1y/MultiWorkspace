package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class HoldRecoveredMovingStorageInnerAction extends TypedRegressionAction<HoldRecoveredMovingStorageInnerAction.Arguments> {
	public record Arguments() {
	}

	public HoldRecoveredMovingStorageInnerAction() {
		super("fixture.moving.holdRecoveredInner", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					ItemStack outer = player.getInventory().getItem(1);
					ItemStack inner = MovingStorageItem.getStorageItem(outer);
					if (inner.isEmpty() || inner.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null || inner.has(ModCoreDataComponents.STORAGE_UUID)) {
						throw new IllegalStateException("Recovered moving item must contain one linked inner carrier");
					}
					player.getInventory().setSelectedItem(inner.copy());
					player.getInventory().setItem(1, ItemStack.EMPTY);
				});
			}
			return await(future);
		}
	}
}
