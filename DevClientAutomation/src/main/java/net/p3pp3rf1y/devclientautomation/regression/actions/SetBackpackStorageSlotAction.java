package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetBackpackStorageSlotAction extends TypedRegressionAction<SetBackpackStorageSlotAction.Arguments> {
	public record Arguments(int slot, String item, int count, String enchantment, int enchantmentLevel) {
	}

	public SetBackpackStorageSlotAction() {
		super("backpack.setStorageSlot", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "item", "count", "enchantment", "enchantmentLevel"), description);
		if (args.has("enchantmentLevel") && !args.has("enchantment")) {
			throw new IllegalArgumentException("Argument enchantmentLevel on " + description + " requires enchantment");
		}
		return new Arguments(optionalNonNegativeInt(args, "slot", 0, description), requireString(args, "item", description),
				requirePositiveInt(args, "count", description), args.has("enchantment") ? requireString(args, "enchantment", description) : "",
				args.has("enchantmentLevel") ? requirePositiveInt(args, "enchantmentLevel", description) : 1);
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
					ItemStack backpack = player.getMainHandItem();
					if (!(backpack.getItem() instanceof BackpackItem)) {
						throw new IllegalStateException("Main hand does not contain a backpack");
					}
					BackpackWrapper wrapper = new BackpackWrapper(backpack);
					if (arguments.slot() >= wrapper.getInventoryHandler().size()) {
						throw new IllegalArgumentException("Storage slot " + arguments.slot() + " is outside the backpack inventory");
					}
					ItemStack stack = new ItemStack(itemForId(arguments.item()), arguments.count());
					if (!arguments.enchantment().isEmpty()) {
						ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(arguments.enchantment()));
						stack.enchant(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), arguments.enchantmentLevel());
					}
					wrapper.getInventoryHandler().setStackInSlot(arguments.slot(), stack);
					wrapper.getInventoryHandler().saveInventory();
					wrapper.onContentsUpdated();
					player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
					player.getInventory().setChanged();
					player.inventoryMenu.broadcastFullState();
				});
			}
			return await(future);
		}

		private static Item itemForId(String itemId) {
			return BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId))
					.orElseThrow(() -> new IllegalArgumentException("Unknown backpack storage item " + itemId));
		}
	}
}
