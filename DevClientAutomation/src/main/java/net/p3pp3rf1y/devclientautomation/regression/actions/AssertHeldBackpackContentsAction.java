package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertHeldBackpackContentsAction extends TypedRegressionAction<AssertHeldBackpackContentsAction.Arguments> {
	public record Arguments(String item, int count, boolean filterUpgrade) {
	}

	public AssertHeldBackpackContentsAction() {
		super("assert.heldBackpackContents", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item", "count", "upgrade"), description);
		String upgrade = optionalString(args, "upgrade", "none", description);
		if (!Set.of("none", "filter").contains(upgrade)) {
			throw new IllegalArgumentException(description + ": unsupported backpack upgrade " + upgrade);
		}
		return new Arguments(requireString(args, "item", description), requirePositiveInt(args, "count", description), upgrade.equals("filter"));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> serverCheck;
			private boolean serverMatches;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				Item expected = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(arguments.item()))
						.orElseThrow(() -> new IllegalArgumentException("Unknown item " + arguments.item()));
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (serverCheck == null) {
					serverCheck = context.onServer(player -> serverMatches = matches(player.getMainHandItem(), expected, arguments));
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress checked = await(serverCheck);
				if (checked != RegressionActionProgress.PASSED) {
					return checked;
				}
				if (!serverMatches) {
					return fail("Picked-up backpack lost its iron tier or stored contents on the server");
				}
				if (matches(context.minecraft().player.getMainHandItem(), expected, arguments)) {
					return RegressionActionProgress.PASSED;
				}
				return context.minecraft().player.tickCount >= deadline
						? fail("Picked-up backpack tier or contents did not synchronize to the client")
						: RegressionActionProgress.WAITING;
			}
		};
	}

	private static boolean matches(ItemStack backpack, Item expected, Arguments arguments) {
		if (!backpack.is(ModItems.IRON_BACKPACK.get())) {
			return false;
		}
		BackpackWrapper wrapper = new BackpackWrapper(backpack);
		ItemStack contents = wrapper.getInventoryHandler().getStackInSlot(0);
		return contents.is(expected) && contents.getCount() == arguments.count()
				&& (!arguments.filterUpgrade() || wrapper.getUpgradeHandler().getStackInSlot(1).is(ModItems.FILTER_UPGRADE.get()));
	}
}
