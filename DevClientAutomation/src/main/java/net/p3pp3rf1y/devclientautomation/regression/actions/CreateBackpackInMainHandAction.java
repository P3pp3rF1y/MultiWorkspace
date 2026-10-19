package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CreateBackpackInMainHandAction extends TypedRegressionAction<CreateBackpackInMainHandAction.Arguments> {
	public record Arguments(String tier) {
	}
	public CreateBackpackInMainHandAction() {
		super("backpack.createInMainHand", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("tier"), description);
		String tier = requireString(args, "tier", description);
		if (!Set.of("leather", "copper", "iron", "gold", "diamond", "netherite").contains(tier)) {
			throw new IllegalArgumentException("Argument tier on " + description + " must be one of leather, copper, iron, gold, diamond, netherite");
		}
		return new Arguments(tier);
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
					player.getInventory().selected = 0;
					player.setItemInHand(InteractionHand.MAIN_HAND, switch (arguments.tier()) {
						case "leather" -> new ItemStack(ModItems.BACKPACK.get());
						case "copper" -> new ItemStack(ModItems.COPPER_BACKPACK.get());
						case "iron" -> new ItemStack(ModItems.IRON_BACKPACK.get());
						case "gold" -> new ItemStack(ModItems.GOLD_BACKPACK.get());
						case "diamond" -> new ItemStack(ModItems.DIAMOND_BACKPACK.get());
						case "netherite" -> new ItemStack(ModItems.NETHERITE_BACKPACK.get());
						default -> throw new IllegalStateException();
					});
					player.getInventory().setChanged();
					player.inventoryMenu.broadcastFullState();
					player.connection.send(new ClientboundSetCarriedItemPacket(0));
				});
			}
			return await(future);
		}
	}
}
