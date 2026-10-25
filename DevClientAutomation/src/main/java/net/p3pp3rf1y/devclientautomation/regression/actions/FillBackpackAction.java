package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Deterministic arrangement only; no pickup behavior or assertions. */
public final class FillBackpackAction extends TypedRegressionAction<String> {
	public FillBackpackAction() {
		super("backpack.fillStorage", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected String parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item"), description);
		return requireString(args, "item", description);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, String itemId) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.getMainHandItem().getItem() instanceof BackpackItem)) {
							throw new IllegalStateException("Main hand does not contain a backpack");
						}
						var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId))
								.orElseThrow(() -> new IllegalArgumentException("Unknown fill item " + itemId));
						var wrapper = BackpackWrapper.fromStack(player.getMainHandItem());
						var inventory = wrapper.getInventoryHandler();
						for (int slot = 0; slot < inventory.size(); slot++) {
							inventory.setStackInSlot(slot, new ItemStack(item, item.getDefaultMaxStackSize()));
						}
						inventory.saveInventory();
						player.getInventory().setChanged();
						player.inventoryMenu.broadcastFullState();
					});
				}
				return await(future);
			}
		};
	}
}
