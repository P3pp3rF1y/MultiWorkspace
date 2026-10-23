package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Independently reads the reloaded held item on the server and synchronized client. */
public final class AssertHeldBackpackFilterAction extends TypedRegressionAction<AssertHeldBackpackFilterAction.Arguments> {
	public record Arguments(String upgrade, String operation, int slot, String item, int count) {
	}

	public AssertHeldBackpackFilterAction() {
		super("assert.heldBackpackFilter", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade", "operation", "slot", "item", "count"), description);
		String upgrade = requireString(args, "upgrade", description);
		String operation = requireString(args, "operation", description);
		String item = requireString(args, "item", description);
		int count = optionalNonNegativeInt(args, "count", -1, description);
		if (!Set.of("basic", "advanced").contains(upgrade) || !Set.of("input", "output").contains(operation)
				|| !Set.of("minecraft:diamond", "minecraft:redstone", "minecraft:air").contains(item) || !args.has("count") || count > 1
				|| count == 0 != item.equals("minecraft:air")) {
			throw new IllegalArgumentException("Invalid held backpack filter assertion on " + description);
		}
		return new Arguments(upgrade, operation, optionalNonNegativeInt(args, "slot", 0, description), item, count);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		@Nullable
		private CompletableFuture<Void> serverCheck;
		private boolean serverMatched;
		private int deadline = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (!context.isExpectedMenu(MenuKind.BACKPACK_ITEM, null)) {
				return fail("Expected held-backpack item menu is not open");
			}
			if (deadline < 0) {
				deadline = context.minecraft().player.tickCount + 100;
			}
			if (serverCheck == null) {
				serverCheck = context.onServer(player -> serverMatched = matches(player.getMainHandItem(), arguments));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress checked = await(serverCheck);
			if (checked != RegressionActionProgress.PASSED) {
				return checked;
			}
			if (!serverMatched) {
				return failAssertion("Server held backpack filter configuration or storage slot did not match the suite expectation");
			}
			if (matches(((BackpackContainer) context.minecraft().player.containerMenu).getStorageWrapper(), arguments)) {
				return RegressionActionProgress.PASSED;
			}
			return context.minecraft().player.tickCount >= deadline
					? failAssertion("Client held backpack filter configuration or storage slot did not synchronize")
					: RegressionActionProgress.WAITING;
		}
	}

	private static boolean matches(ItemStack backpack, Arguments expected) {
		if (!(backpack.getItem() instanceof BackpackItem)) {
			return false;
		}
		return matches(new BackpackWrapper(backpack), expected);
	}

	private static boolean matches(IBackpackWrapper wrapper, Arguments expected) {
		List<FilterUpgradeWrapper> filters = wrapper.getUpgradeHandler().getWrappersThatImplement(FilterUpgradeWrapper.class);
		if (filters.size() != 1 || expected.slot() >= wrapper.getInventoryHandler().getSlots()) {
			return false;
		}
		FilterUpgradeWrapper filter = filters.getFirst();
		boolean input = expected.operation().equals("input");
		int filterSlots = expected.upgrade().equals("basic")
				? ModItems.FILTER_UPGRADE.get().getFilterSlotCount()
				: ModItems.ADVANCED_FILTER_UPGRADE.get().getFilterSlotCount();
		Item filterItem = input ? Items.DIAMOND : Items.REDSTONE;
		ItemStack observed = wrapper.getInventoryHandler().getStackInSlot(expected.slot());
		Item item = switch (expected.item()) {
			case "minecraft:diamond" -> Items.DIAMOND;
			case "minecraft:redstone" -> Items.REDSTONE;
			default -> Items.AIR;
		};
		return filter.getDirection() == (input ? Direction.INPUT : Direction.OUTPUT)
				&& filter.getFilterLogic().getFilterType() == (input ? ContentsFilterType.ALLOW : ContentsFilterType.BLOCK)
				&& filter.getFilterLogic().getFilterHandler().getSlots() == filterSlots
				&& filter.getFilterLogic().getFilterHandler().getStackInSlot(filterSlots - 1).is(filterItem) && observed.getCount() == expected.count()
				&& (expected.count() == 0 ? observed.isEmpty() : observed.is(item));
	}
}
