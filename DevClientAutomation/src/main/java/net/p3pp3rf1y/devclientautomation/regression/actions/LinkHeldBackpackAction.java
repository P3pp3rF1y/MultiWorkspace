package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

public final class LinkHeldBackpackAction extends TypedRegressionAction<Void> {
	public LinkHeldBackpackAction() {
		super("backpack.linkHeld", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Void parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return null;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Void arguments) {
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
					ItemStack backpack = player.getMainHandItem();
					if (!(backpack.getItem() instanceof BackpackItem)) {
						throw new IllegalStateException("Main hand does not contain a backpack to link");
					}
					LinkedStorageService.LinkResult result = LinkedStorageService.linkWithResult((ServerLevel) player.level(), player.getUUID(),
							new ItemStack(ENDER_LINKER.get()), backpack);
					if (result != LinkedStorageService.LinkResult.SUCCESS) {
						throw new IllegalStateException("Could not link held backpack: " + result);
					}
					player.getInventory().setChanged();
					player.inventoryMenu.broadcastFullState();
				});
			}
			return await(future);
		}
	}
}
