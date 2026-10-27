package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetHeldBackpackUpgradeAction extends TypedRegressionAction<SetHeldBackpackUpgradeAction.Arguments> {
	public record Arguments(int slot, String item, int inventorySlot, boolean enabled) {
	}
	public SetHeldBackpackUpgradeAction() {
		super("fixture.backpack.setUpgrade", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "item", "inventorySlot", "enabled"), description);
		int inventorySlot = optionalNonNegativeInt(args, "inventorySlot", 0, description);
		if (inventorySlot > 35) {
			throw new IllegalArgumentException(description + " inventorySlot must be between 0 and 35");
		}
		boolean enabled = true;
		if (args.has("enabled")) {
			if (!args.get("enabled").isJsonPrimitive() || !args.getAsJsonPrimitive("enabled").isBoolean()) {
				throw new IllegalArgumentException(description + " enabled must be a boolean");
			}
			enabled = args.get("enabled").getAsBoolean();
		}
		return new Arguments(optionalNonNegativeInt(args, "slot", 0, description), requireString(args, "item", description), inventorySlot, enabled);
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack backpack = player.getInventory().getItem(arguments.inventorySlot());
						if (!(backpack.getItem() instanceof BackpackItem)) {
							throw new IllegalStateException("Inventory slot does not contain a backpack");
						}
						Item item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(arguments.item())).orElseThrow();
						IBackpackWrapper wrapper = BackpackLinkedStorageResolver.resolveOrCreate(player.serverLevel(), backpack);
						wrapper.getInventoryHandler();
						wrapper.getUpgradeHandler().setStackInSlot(arguments.slot(), new ItemStack(item));
						wrapper.getUpgradeHandler().getSlotWrappers().get(arguments.slot()).setEnabled(arguments.enabled());
						player.getInventory().setItem(arguments.inventorySlot(), backpack.copy());
						player.getInventory().setChanged();
						player.inventoryMenu.broadcastFullState();
					});
				}
				return await(future);
			}
		};
	}
}
