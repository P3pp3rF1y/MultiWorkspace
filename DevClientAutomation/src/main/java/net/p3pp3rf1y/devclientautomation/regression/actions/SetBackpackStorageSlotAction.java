package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
		if (args.has("enchantmentLevel") && !args.has("enchantment"))
			throw new IllegalArgumentException("Argument enchantmentLevel on " + description + " requires enchantment");
		return new Arguments(optionalNonNegativeInt(args, "slot", 0, description), requireString(args, "item", description),
				requirePositiveInt(args, "count", description), args.has("enchantment") ? requireString(args, "enchantment", description) : null,
				optionalPositiveInt(args, "enchantmentLevel", 1, description));
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
					var wrapper = BackpackWrapper.fromStack(backpack);
					if (arguments.slot() >= wrapper.getInventoryHandler().size()) {
						throw new IllegalArgumentException("Storage slot " + arguments.slot() + " is outside the backpack inventory");
					}
					ItemStack contents = new ItemStack(itemForId(arguments.item()), arguments.count());
					if (arguments.enchantment() != null) {
						var enchantment = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
								.get(ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(arguments.enchantment())))
								.orElseThrow(() -> new IllegalArgumentException("Unknown enchantment " + arguments.enchantment()));
						ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(
								contents.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
						enchantments.set(enchantment, arguments.enchantmentLevel());
						contents.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
					}
					wrapper.getInventoryHandler().setStackInSlot(arguments.slot(), contents);
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
