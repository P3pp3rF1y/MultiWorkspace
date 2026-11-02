package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Tick-bounded client/server synchronization for an immutable vanilla menu identity. */
public final class AwaitVanillaMenuAction extends TypedRegressionAction<AwaitVanillaMenuAction.Arguments> {
	public record Arguments(String ref, String saveAs, int timeoutTicks) {
	}

	public AwaitVanillaMenuAction() {
		super("await.vanillaMenu", "2", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "saveAs", "timeoutTicks"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "saveAs", description)), optionalPositiveInt(args, "timeoutTicks", 100, description));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.VANILLA_MENU)
				.and(RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.VANILLA_MENU));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int deadline = -1;
			@Nullable
			private CompletableFuture<Void> server;

			@Override
			public RegressionActionProgress tick() {
				VanillaMenuHandle handle = context.values().require(arguments.ref(), VanillaMenuHandle.class);
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + arguments.timeoutTicks();
				}
				if (matchesClient(handle)) {
					if (server == null) {
						server = context.onServer(player -> {
							if (player.containerMenu.containerId != handle.containerId() || !matchesMenu(player.containerMenu, handle.kind())) {
								throw RegressionActionFailure.infrastructure("Server menu does not match vanilla menu handle");
							}
						});
						return RegressionActionProgress.WAITING;
					}
					RegressionActionProgress result = await(server);
					if (result == RegressionActionProgress.PASSED) {
						context.values().put(arguments.saveAs(), VanillaMenuHandle.class, handle);
					}
					return result;
				}
				if (context.minecraft().player.tickCount >= deadline) {
					context.putDiagnostic("vanillaMenu",
							context.minecraft().gui.screen() == null ? "none" : context.minecraft().gui.screen().getClass().getName());
					return fail(RegressionActionFailure.Kind.TIMEOUT,
							"Vanilla menu did not synchronize to handle within " + arguments.timeoutTicks() + " ticks");
				}
				return RegressionActionProgress.WAITING;
			}

			private boolean matchesClient(VanillaMenuHandle handle) {
				if (!(context.minecraft().gui.screen() instanceof AbstractContainerScreen<?> screen) || screen.getMenu().containerId != handle.containerId()) {
					return false;
				}
				return switch (handle.kind()) {
					case CHEST -> screen.getMenu() instanceof ChestMenu;
					case CRAFTING -> screen instanceof CraftingScreen && screen.getMenu() instanceof CraftingMenu;
					case FURNACE -> screen instanceof FurnaceScreen && screen.getMenu() instanceof FurnaceMenu;
				};
			}
		};
	}

	private static boolean matchesMenu(AbstractContainerMenu menu, VanillaMenuHandle.Kind kind) {
		return switch (kind) {
			case CHEST -> menu instanceof ChestMenu;
			case CRAFTING -> menu instanceof CraftingMenu;
			case FURNACE -> menu instanceof FurnaceMenu;
		};
	}
}
