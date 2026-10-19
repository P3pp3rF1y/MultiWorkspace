package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertLinkedItemContentsAction extends TypedRegressionAction<AssertLinkedItemContentsAction.Arguments> {
	public record Arguments(int slot, int count) {
	}

	public AssertLinkedItemContentsAction() {
		super("assert.linkedItemContents", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "count"), description);
		int slot = optionalNonNegativeInt(args, "slot", 0, description);
		if (slot > 35) {
			throw new IllegalArgumentException("Inventory slot must be between 0 and 35 on " + description);
		}
		return new Arguments(slot, optionalNonNegativeInt(args, "count", 0, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack stack = player.getInventory().getItem(arguments.slot());
						LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
						if (endpoint == null || stack.has(ModCoreDataComponents.STORAGE_UUID) || stack.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT)) {
							List<String> inventory = new ArrayList<>();
							for (int slot = 0; slot < 36; slot++) {
								ItemStack found = player.getInventory().getItem(slot);
								if (!found.isEmpty()) {
									inventory.add(slot + "=" + found + " " + found.getComponentsPatch());
								}
							}
							throw new IllegalStateException("Expected a finalized linked item without an ordinary backend at " + arguments.slot() + ": " + stack
									+ ", endpoint=" + endpoint + ", ordinary=" + stack.get(ModCoreDataComponents.STORAGE_UUID) + ", pending="
									+ stack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT) + ", inventory=" + inventory + ", carried="
									+ player.containerMenu.getCarried() + ", menu=" + player.containerMenu.getClass().getSimpleName() + ", craftingSlots="
									+ player.containerMenu.getItems().subList(0, Math.min(5, player.containerMenu.getItems().size())));
						}
						IStorageWrapper host = (IStorageWrapper) LinkedStorageGroupsSavedData.get(player.level()).manager().resolveVirtualHost(endpoint, false)
								.orElseThrow();
						int count = 0;
						for (int slot = 0; slot < host.getInventoryHandler().size(); slot++) {
							count += host.getInventoryHandler().getStackInSlot(slot).getCount();
						}
						if (count != arguments.count()) {
							List<String> inventory = new ArrayList<>();
							for (int slot = 0; slot < 36; slot++) {
								ItemStack found = player.getInventory().getItem(slot);
								if (!found.isEmpty()) {
									inventory.add(slot + "=" + found + " " + found.getComponentsPatch());
								}
							}
							List<String> upgrades = new ArrayList<>();
							for (int slot = 0; slot < host.getUpgradeHandler().size(); slot++) {
								ItemStack upgrade = host.getUpgradeHandler().getStackInSlot(slot);
								upgrades.add(slot + "=" + upgrade + " " + upgrade.getComponentsPatch());
							}
							throw new IllegalStateException("Expected canonical item count " + arguments.count() + ", got " + count + ", inventory=" + inventory
									+ ", canonicalUpgrades=" + upgrades + ", menu=" + player.containerMenu.getClass().getSimpleName());
						}
					});
				}
				return await(future);
			}
		};
	}
}
