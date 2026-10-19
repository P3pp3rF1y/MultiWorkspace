package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CreateLockToolAction extends TypedRegressionAction<CreateLockToolAction.Arguments> {
	public record Arguments() {
	}
	public CreateLockToolAction() {
		super("fixture.storage.createLockTool", "1", Set.of("minecraft:client", "sophisticatedstorage"));
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
						ItemStack tool = new ItemStack(ModItems.STORAGE_TOOL.get());
						StorageToolItem.cycleMode(tool, true);
						player.getInventory().setItem(player.getInventory().selected, tool);
						player.containerMenu.broadcastChanges();
					});
				}
				RegressionActionProgress progress = await(future);
				return progress == RegressionActionProgress.PASSED && (context.minecraft().player.getMainHandItem().getItem() != ModItems.STORAGE_TOOL.get()
						|| StorageToolItem.getMode(context.minecraft().player.getMainHandItem()) != StorageToolItem.Mode.LOCK)
								? RegressionActionProgress.WAITING
								: progress;
			}
		};
	}
}
