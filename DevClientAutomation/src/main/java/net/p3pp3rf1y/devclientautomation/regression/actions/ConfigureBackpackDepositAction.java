package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterType;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Fixture configuration; transferring items remains a separate real client interaction. */
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
			private @Nullable CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack backpack = player.getMainHandItem();
						if (!(backpack.getItem() instanceof BackpackItem)) {
							throw new IllegalStateException("Main hand does not contain a backpack");
						}
						var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown deposit filter item " + arguments.item()));
						if (!backpack.has(ModCoreDataComponents.STORAGE_UUID)) {
							backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
						}
						BackpackWrapper wrapper = new BackpackWrapper(backpack);
						wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.DEPOSIT_UPGRADE.get()));
						var deposits = wrapper.getUpgradeHandler().getWrappersThatImplement(DepositUpgradeWrapper.class);
						if (deposits.size() != 1) {
							throw new IllegalStateException("Expected exactly one deposit upgrade, found " + deposits.size());
						}
						var filter = deposits.getFirst().getFilterLogic();
						filter.setDepositFilterType(DepositFilterType.ALLOW);
						filter.getFilterHandler().setStackInSlot(0, new ItemStack(item));
						wrapper.getUpgradeHandler().saveInventory();
						wrapper.onContentsUpdated();
						player.getInventory().setChanged();
						player.inventoryMenu.broadcastFullState();
					});
				}
				return await(future);
			}
		};
	}
}
