package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Asserts the linked-primary tier-upgrade result remains marked before it is quick moved. */
public final class AssertLinkedBackpackTierUpgradeResultAction extends TypedRegressionAction<AssertLinkedBackpackTierUpgradeResultAction.Arguments> {
	public record Arguments(String tier) {
	}

	public AssertLinkedBackpackTierUpgradeResultAction() {
		super("assert.linkedBackpackTierUpgradeResult", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
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
			private CompletableFuture<Void> server;

			@Override
			public RegressionActionProgress tick() {
				boolean expectedScreen = arguments.tier().equals("diamond") && context.minecraft().gui.screen() instanceof CraftingScreen
						|| arguments.tier().equals("netherite") && context.minecraft().gui.screen() instanceof SmithingScreen;
				if (!expectedScreen) {
					return fail("Expected " + arguments.tier() + " tier-upgrade result in its real workstation menu");
				}
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) context.minecraft().gui.screen();
				int resultSlot = arguments.tier().equals("diamond") ? 0 : 3;
				if (!isMarkedTarget(screen.getMenu().getSlot(resultSlot).getItem(), arguments.tier())) {
					return failAssertion("Workstation result is not a marked linked " + arguments.tier() + " backpack");
				}
				if (server == null) {
					server = context.onServer(player -> {
						AbstractContainerMenu menu = player.containerMenu;
						if (menu.containerId != screen.getMenu().containerId || !matchesMenu(menu, arguments.tier())
								|| !isMarkedTarget(menu.getSlot(resultSlot).getItem(), arguments.tier())) {
							throw RegressionActionFailure.assertion("Server workstation result is not a marked linked " + arguments.tier() + " backpack");
						}
					});
					return RegressionActionProgress.WAITING;
				}
				return await(server);
			}
		};
	}

	private static boolean matchesMenu(AbstractContainerMenu menu, String tier) {
		return tier.equals("diamond") ? menu instanceof CraftingMenu : menu instanceof SmithingMenu;
	}

	static boolean isMarkedTarget(ItemStack stack, String tier) {
		Item expected = BuiltInRegistries.ITEM.getValue(Identifier.parse("sophisticatedbackpacks:" + tier + "_backpack"));
		return stack.is(expected) && stack.has(ModDataComponents.TIER_UPGRADE_OPERATION_ID.get());
	}
}
