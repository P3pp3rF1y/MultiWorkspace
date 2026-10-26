package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ConfigureBackpackPickupAction extends TypedRegressionAction<ConfigureBackpackPickupAction.Arguments> {
	public record Arguments(String upgrade, String filterMode, String filterItem) {
	}

	public ConfigureBackpackPickupAction() {
		super("backpack.configurePickup", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade", "filterMode", "filterItem"), description);
		String upgrade = requireString(args, "upgrade", description);
		String mode = requireString(args, "filterMode", description);
		String item = requireString(args, "filterItem", description);
		if (!Set.of("basic", "advanced").contains(upgrade) || !Set.of("allow", "block").contains(mode)) {
			throw new IllegalArgumentException("Expected basic/advanced pickup and allow/block filter on " + description);
		}
		return new Arguments(upgrade, mode, item);
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
						ItemStack backpack = player.getMainHandItem();
						if (!(backpack.getItem() instanceof BackpackItem)) {
							throw new IllegalStateException("Main hand does not contain a backpack");
						}
						if (!backpack.has(ModCoreDataComponents.STORAGE_UUID)) {
							backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
						}
						var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.filterItem()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown filter item " + arguments.filterItem()));
						var wrapper = BackpackWrapper.fromStack(backpack);
						wrapper.getUpgradeHandler().setStackInSlot(1,
								new ItemStack(arguments.upgrade().equals("basic") ? ModItems.PICKUP_UPGRADE.get() : ModItems.ADVANCED_PICKUP_UPGRADE.get()));
						var pickups = wrapper.getUpgradeHandler().getWrappersThatImplement(PickupUpgradeWrapper.class);
						if (pickups.size() != 1) {
							throw new IllegalStateException("Expected exactly one pickup upgrade, found " + pickups.size());
						}
						var logic = pickups.getFirst().getFilterLogic();
						logic.setDepositFilterType(arguments.filterMode().equals("allow") ? ContentsFilterType.ALLOW : ContentsFilterType.BLOCK);
						logic.getFilterHandler().setStackInSlot(0, new ItemStack(item));
						wrapper.getUpgradeHandler().saveInventory();
						player.getInventory().setChanged();
						player.inventoryMenu.broadcastFullState();
					});
				}
				return await(future);
			}
		};
	}
}
