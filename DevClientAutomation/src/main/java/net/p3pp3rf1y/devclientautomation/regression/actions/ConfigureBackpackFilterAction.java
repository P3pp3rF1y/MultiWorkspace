package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ConfigureBackpackFilterAction extends TypedRegressionAction<ConfigureBackpackFilterAction.Arguments> {
	public record Arguments(String upgrade, String operation) {
	}

	public ConfigureBackpackFilterAction() {
		super("backpack.configureFilter", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade", "operation"), description);
		String upgrade = requireString(args, "upgrade", description);
		String operation = requireString(args, "operation", description);
		if (!Set.of("basic", "advanced").contains(upgrade) || !Set.of("input", "output").contains(operation)) {
			throw new IllegalArgumentException("Filter upgrade and operation on " + description + " must be basic/advanced and input/output");
		}
		return new Arguments(upgrade, operation);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		@Nullable
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
					if (!backpack.has(ModCoreDataComponents.STORAGE_UUID)) {
						backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
					}
					IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
					FilterUpgradeItem upgrade = arguments.upgrade().equals("basic") ? ModItems.FILTER_UPGRADE.get() : ModItems.ADVANCED_FILTER_UPGRADE.get();
					wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(upgrade));
					List<FilterUpgradeWrapper> filters = wrapper.getUpgradeHandler().getWrappersThatImplement(FilterUpgradeWrapper.class);
					if (filters.size() != 1) {
						throw new IllegalStateException("Expected exactly one filter upgrade, found " + filters.size());
					}
					FilterUpgradeWrapper filter = filters.getFirst();
					boolean input = arguments.operation().equals("input");
					filter.setDirection(input ? Direction.INPUT : Direction.OUTPUT);
					filter.getFilterLogic().setDepositFilterType(input ? ContentsFilterType.ALLOW : ContentsFilterType.BLOCK);
					filter.getFilterLogic().getFilterHandler().setStackInSlot(upgrade.getFilterSlotCount() - 1,
							new ItemStack(input ? Items.DIAMOND : Items.REDSTONE));
					wrapper.getUpgradeHandler().saveInventory();
					wrapper.refreshInventoryForInputOutput();
					wrapper.onContentsNbtUpdated();
					player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
					player.getInventory().setChanged();
					player.inventoryMenu.broadcastFullState();
				});
			}
			return await(future);
		}
	}
}
