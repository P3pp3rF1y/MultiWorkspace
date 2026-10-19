package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterType;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Configures a fixture backpack; the deposit itself requires a real client interaction. */
public final class ConfigureBackpackDepositAction extends TypedRegressionAction<ConfigureBackpackDepositAction.Arguments> {
	public record Arguments(String item) {
	}

	public ConfigureBackpackDepositAction() {
		super("backpack.configureDeposit", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item"), description);
		return new Arguments(requireString(args, "item", description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack backpack = player.getMainHandItem();
						if (!(backpack.getItem() instanceof BackpackItem)) {
							throw new IllegalStateException("Main hand does not contain a backpack");
						}
						var item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown deposit filter item " + arguments.item()));
						BackpackWrapper wrapper = new BackpackWrapper(backpack);
						if (wrapper.getContentsUuid().isEmpty()) {
							wrapper.setContentsUuid(UUID.randomUUID());
						}
						wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.DEPOSIT_UPGRADE.get()));
						var deposits = wrapper.getUpgradeHandler().getWrappersThatImplement(DepositUpgradeWrapper.class);
						if (deposits.size() != 1) {
							throw new IllegalStateException("Expected exactly one deposit upgrade, found " + deposits.size());
						}
						var filter = deposits.get(0).getFilterLogic();
						filter.setDepositFilterType(DepositFilterType.ALLOW);
						filter.getFilterHandler().setStackInSlot(0, new ItemStack(item));
						wrapper.getUpgradeHandler().saveInventory();
						wrapper.onContentsNbtUpdated();
						// Recreate the older stack capability after its UUID-less handler was initialized.
						player.setItemInHand(InteractionHand.MAIN_HAND, backpack.copy());
						player.getInventory().setChanged();
						player.inventoryMenu.broadcastFullState();
					});
				}
				return await(future);
			}
		};
	}
}
