package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Verifies the saved restock filter and aggregate contents in both authoritative and opened-client wrappers. */
public final class AssertRestockBackpackAction extends TypedRegressionAction<AssertRestockBackpackAction.Arguments> {
	public record Arguments(String upgrade, String mode, int diamonds, int redstone) {
	}

	public AssertRestockBackpackAction() {
		super("assert.restockBackpack", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade", "mode", "diamonds", "redstone"), description);
		String upgrade = requireString(args, "upgrade", description);
		String mode = requireString(args, "mode", description);
		if (!Set.of("basic", "advanced").contains(upgrade) || !Set.of("allow", "block", "empty").contains(mode) || !args.has("diamonds")
				|| !args.has("redstone")) {
			throw new IllegalArgumentException("Invalid restock backpack assertion on " + description);
		}
		return new Arguments(upgrade, mode, optionalNonNegativeInt(args, "diamonds", 0, description), optionalNonNegativeInt(args, "redstone", 0, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> serverCheck;
		private boolean serverMatched;
		private int deadline = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (deadline < 0) {
				deadline = context.minecraft().player.tickCount + 100;
			}
			if (serverCheck == null) {
				serverCheck = context.onServer(player -> serverMatched = player.getMainHandItem().getItem() instanceof BackpackItem
						&& matches(new BackpackWrapper(player.getMainHandItem()), arguments));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress checked = await(serverCheck);
			if (checked != RegressionActionProgress.PASSED) {
				return checked;
			}
			if (!serverMatched) {
				serverCheck = null;
				return context.minecraft().player.tickCount >= deadline
						? fail("Server restock backpack did not match expected settings and contents")
						: RegressionActionProgress.WAITING;
			}
			if (context.isExpectedMenu("backpackItem")
					&& matches(((BackpackContainer) context.minecraft().player.containerMenu).getStorageWrapper(), arguments)) {
				return RegressionActionProgress.PASSED;
			}
			return context.minecraft().player.tickCount >= deadline
					? fail("Client restock backpack menu did not synchronize")
					: RegressionActionProgress.WAITING;
		}
	}

	private static boolean matches(IBackpackWrapper wrapper, Arguments expected) {
		var restocks = wrapper.getUpgradeHandler().getWrappersThatImplement(RestockUpgradeWrapper.class);
		if (restocks.size() != 1) {
			return false;
		}
		int slots = expected.upgrade().equals("basic")
				? ModItems.RESTOCK_UPGRADE.get().getFilterSlotCount()
				: ModItems.ADVANCED_RESTOCK_UPGRADE.get().getFilterSlotCount();
		boolean block = expected.mode().equals("block");
		RestockUpgradeWrapper restock = restocks.get(0);
		InventoryHandler inventory = wrapper.getInventoryHandler();
		int diamonds = 0;
		int redstone = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.is(Items.DIAMOND)) {
				diamonds += stack.getCount();
			} else if (stack.is(Items.REDSTONE)) {
				redstone += stack.getCount();
			} else if (!stack.isEmpty()) {
				return false;
			}
		}
		return restock.getFilterLogic().getFilterType() == (block ? ContentsFilterType.BLOCK : ContentsFilterType.ALLOW)
				&& restock.getFilterLogic().getFilterHandler().getSlots() == slots
				&& restock.getFilterLogic().getFilterHandler().getStackInSlot(slots - 1).is(block ? Items.REDSTONE : Items.DIAMOND)
				&& diamonds == expected.diamonds() && redstone == expected.redstone();
	}
}
