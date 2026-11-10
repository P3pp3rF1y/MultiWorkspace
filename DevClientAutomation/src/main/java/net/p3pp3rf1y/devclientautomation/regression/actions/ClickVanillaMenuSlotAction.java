package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Clicks a real crafting or smithing screen slot through its production hit testing. */
public final class ClickVanillaMenuSlotAction extends TypedRegressionAction<ClickVanillaMenuSlotAction.Arguments> {
	public record Arguments(int slot, int button) {
	}

	public ClickVanillaMenuSlotAction() {
		super("menu.clickVanillaSlot", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "button"), description);
		int button = optionalNonNegativeInt(args, "button", 0, description);
		if (button > 1) {
			throw new IllegalArgumentException("button on " + description + " must be 0 or 1");
		}
		return new Arguments(requireNonNegativeInt(args, "slot", description), button);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> server;
			private int containerId = -1;
			private boolean clicked;
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof AbstractContainerScreen<?> screen)
						|| !(screen instanceof CraftingScreen || screen instanceof SmithingScreen)) {
					return fail("A real crafting or smithing screen is not active");
				}
				if (arguments.slot() >= screen.getMenu().slots.size()) {
					return fail("Vanilla menu slot " + arguments.slot() + " is outside the active menu");
				}
				if (!clicked) {
					Slot slot = screen.getMenu().getSlot(arguments.slot());
					VirtualInput.moveToSlot(screen, slot.index);
					if (!VirtualInput.click(screen, screen.getLeftPos() + slot.x + 8, screen.getTopPos() + slot.y + 8,
							arguments.button() == 0 ? InputConstants.MOUSE_BUTTON_LEFT : InputConstants.MOUSE_BUTTON_RIGHT, 0)) {
						return fail("Vanilla menu slot click was not handled by the screen");
					}
					containerId = screen.getMenu().containerId;
					clicked = true;
					server = context.onServer(player -> {
						if (player.containerMenu.containerId != containerId)
							throw RegressionActionFailure.infrastructure("Vanilla menu closed before click synchronization");
					});
					return RegressionActionProgress.WAITING;
				}
				return await(server);
			}
		};
	}
}
