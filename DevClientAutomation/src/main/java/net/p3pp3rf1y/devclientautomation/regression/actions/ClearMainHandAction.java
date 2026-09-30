package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ClearMainHandAction extends TypedRegressionAction<ClearMainHandAction.Arguments> {
	public record Arguments() {
	}

	public ClearMainHandAction() {
		super("player.clearMainHand", "1", Set.of("minecraft:client"));
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
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					player.getInventory().setChanged();
					player.inventoryMenu.broadcastFullState();
					player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
				});
			}
			return await(future);
		}
	}
}
