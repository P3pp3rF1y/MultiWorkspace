package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ConfigureBackpackRestockAction extends TypedRegressionAction<ConfigureBackpackRestockAction.Arguments> {
	public record Arguments(String upgrade, String mode) {
	}

	public ConfigureBackpackRestockAction() {
		super("backpack.configureRestock", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade", "mode"), description);
		String upgrade = requireString(args, "upgrade", description);
		String mode = requireString(args, "mode", description);
		if (!Set.of("basic", "advanced").contains(upgrade) || !Set.of("allow", "block", "empty").contains(mode)) {
			throw new IllegalArgumentException("Invalid restock upgrade or mode on " + description);
		}
		return new Arguments(upgrade, mode);
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
					BackpackWrapper wrapper = new BackpackWrapper(backpack);
					RestockUpgradeItem upgrade = arguments.upgrade().equals("basic") ? ModItems.RESTOCK_UPGRADE.get() : ModItems.ADVANCED_RESTOCK_UPGRADE.get();
					wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(upgrade));
					List<RestockUpgradeWrapper> restocks = wrapper.getUpgradeHandler().getWrappersThatImplement(RestockUpgradeWrapper.class);
					if (restocks.size() != 1) {
						throw new IllegalStateException("Expected exactly one restock upgrade, found " + restocks.size());
					}
					RestockUpgradeWrapper restock = restocks.getFirst();
					boolean block = arguments.mode().equals("block");
					restock.getFilterLogic().setDepositFilterType(block ? ContentsFilterType.BLOCK : ContentsFilterType.ALLOW);
					restock.getFilterLogic().getFilterHandler().setStackInSlot(upgrade.getFilterSlotCount() - 1,
							new ItemStack(block ? Items.REDSTONE : Items.DIAMOND));
					wrapper.getUpgradeHandler().saveInventory();
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
