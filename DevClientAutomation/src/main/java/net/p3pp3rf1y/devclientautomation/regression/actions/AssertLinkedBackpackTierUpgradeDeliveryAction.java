package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Asserts quick move delivered one completed linked-primary tier-upgrade backpack. */
public final class AssertLinkedBackpackTierUpgradeDeliveryAction extends TypedRegressionAction<AssertLinkedBackpackTierUpgradeDeliveryAction.Arguments> {
	public record Arguments(String tier) {
	}

	public AssertLinkedBackpackTierUpgradeDeliveryAction() {
		super("assert.linkedBackpackTierUpgradeDelivery", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("tier"), description);
		String tier = requireString(args, "tier", description);
		if (!Set.of("diamond", "netherite").contains(tier)) {
			throw new IllegalArgumentException("Argument tier on " + description + " must be diamond or netherite");
		}
		return new Arguments(tier);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int deadline = -1;
			private CompletableFuture<Void> delivery;
			private Delivery observed;

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (delivery == null) {
					delivery = context.onServer(player -> observed = findDelivery(player.getInventory().getNonEquipmentItems(), arguments.tier()));
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress result = await(delivery);
				if (result != RegressionActionProgress.PASSED) {
					return result;
				}
				if (observed.matches()) {
					return RegressionActionProgress.PASSED;
				}
				if (context.minecraft().player.tickCount >= deadline) {
					return failAssertion("Quick move did not deliver exactly one completed linked " + arguments.tier() + " backpack: " + observed);
				}
				delivery = null;
				return RegressionActionProgress.WAITING;
			}
		};
	}

	private static Delivery findDelivery(Iterable<ItemStack> stacks, String tier) {
		Item expected = BuiltInRegistries.ITEM.getValue(Identifier.parse("sophisticatedbackpacks:" + tier + "_backpack"));
		int stacksFound = 0;
		int count = 0;
		boolean markersRemoved = true;
		for (ItemStack stack : stacks) {
			if (stack.is(expected)) {
				stacksFound++;
				count += stack.getCount();
				markersRemoved &= !stack.has(ModDataComponents.TIER_UPGRADE_OPERATION_ID.get())
						&& !stack.has(ModDataComponents.TIER_UPGRADE_SOURCE_COLUMNS.get());
			}
		}
		return new Delivery(stacksFound, count, markersRemoved);
	}

	private record Delivery(int stacks, int count, boolean markersRemoved) {
		private boolean matches() {
			return stacks == 1 && count == 1 && markersRemoved;
		}
	}
}
