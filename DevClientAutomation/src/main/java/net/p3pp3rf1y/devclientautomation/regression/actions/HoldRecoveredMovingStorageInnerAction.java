package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
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
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack outer = player.getInventory().getItem(1);
						ItemStack inner = MovingStorageItem.getStorageItem(outer);
						if (inner.isEmpty() || LinkedStorageStackData.getEndpoint(inner) == null || NBTHelper.hasTag(inner, StorageWrapper.UUID_TAG)) {
							throw new IllegalStateException("Recovered moving item must contain one linked inner carrier");
						}
						player.getInventory().setItem(player.getInventory().selected, inner.copy());
						player.getInventory().setItem(1, ItemStack.EMPTY);
					});
				}
				return await(future);
			}
		};
	}
}
