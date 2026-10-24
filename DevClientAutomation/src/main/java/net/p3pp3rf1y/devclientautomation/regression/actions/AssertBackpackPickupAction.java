package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Independent persisted configuration observation through fresh server and synchronized menu wrappers. */
public final class AssertBackpackPickupAction extends TypedRegressionAction<AssertBackpackPickupAction.Arguments> {
	public record Arguments(String upgrade, String filterMode, String filterItem) {
	}

	public AssertBackpackPickupAction() {
		super("assert.backpackPickupConfiguration", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade", "filterMode", "filterItem"), description);
		String upgrade = requireString(args, "upgrade", description);
		String mode = requireString(args, "filterMode", description);
		if (!Set.of("basic", "advanced").contains(upgrade) || !Set.of("allow", "block").contains(mode)) {
			throw new IllegalArgumentException("Invalid pickup configuration expectation on " + description);
		}
		return new Arguments(upgrade, mode, requireString(args, "filterItem", description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> server;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (!context.isExpectedMenu("backpackItem")) {
					return fail("Expected synchronized held-backpack menu");
				}
				if (server == null) {
					deadline = context.minecraft().player.tickCount + 100;
					server = context.onServer(player -> {
						var actual = snapshot(new BackpackWrapper(player.getMainHandItem()));
						observe(expected(arguments), actual);
						context.putDiagnostic("serverPickupConfiguration", actual);
						if (!actual.equals(expected(arguments))) {
							throw RegressionActionFailure.assertion("Persisted server pickup settings expected " + expected(arguments) + ", actual " + actual);
						}
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress checked = await(server);
				if (checked != RegressionActionProgress.PASSED) {
					return checked;
				}
				var actual = snapshot(((BackpackContainer) context.minecraft().player.containerMenu).getStorageWrapper());
				context.putDiagnostic("clientPickupConfiguration", actual);
				observe(expected(arguments), actual);
				if (actual.equals(expected(arguments))) {
					return RegressionActionProgress.PASSED;
				}
				return context.minecraft().player.tickCount >= deadline
						? failAssertion("Persisted client pickup settings did not synchronize: " + actual)
						: RegressionActionProgress.WAITING;
			}
		};
	}

	private static Map<String, Object> expected(Arguments arguments) {
		return Map.of("upgrade", arguments.upgrade(), "filterMode", arguments.filterMode(), "filterItem", arguments.filterItem(), "filterCount", 1);
	}

	private static Map<String, Object> snapshot(IBackpackWrapper wrapper) {
		var pickups = wrapper.getUpgradeHandler().getWrappersThatImplement(PickupUpgradeWrapper.class);
		if (pickups.size() != 1) {
			return Map.of("pickupUpgrades", pickups.size());
		}
		var upgrade = wrapper.getUpgradeHandler().getStackInSlot(1);
		String type = upgrade.is(ModItems.PICKUP_UPGRADE.get()) ? "basic" : upgrade.is(ModItems.ADVANCED_PICKUP_UPGRADE.get()) ? "advanced" : "unexpected";
		var logic = pickups.get(0).getFilterLogic();
		var filter = logic.getFilterHandler().getStackInSlot(0);
		return Map.of("upgrade", type, "filterMode",
				logic.getFilterType() == ContentsFilterType.ALLOW ? "allow" : logic.getFilterType() == ContentsFilterType.BLOCK ? "block" : "unexpected",
				"filterItem", BuiltInRegistries.ITEM.getKey(filter.getItem()).toString(), "filterCount", filter.getCount());
	}
}
