package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CreateMainHandItemAction extends TypedRegressionAction<CreateMainHandItemAction.Arguments> {
	public record Arguments(String item, int count) {
	}

	public CreateMainHandItemAction() {
		super("player.createMainHandItem", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item", "count"), description);
		return new Arguments(requireString(args, "item", description), optionalPositiveInt(args, "count", 1, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
							.orElseThrow(() -> new IllegalArgumentException("Unknown main-hand item " + arguments.item()));
					player.getInventory().setSelectedSlot(0);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, arguments.count()));
					player.getInventory().setChanged();
					player.inventoryMenu.broadcastFullState();
					player.connection.send(new ClientboundSetHeldSlotPacket(0));
				});
			}
			return await(future);
		}
	}
}
