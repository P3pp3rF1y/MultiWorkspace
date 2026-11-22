package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Clicks a real crafting or smithing screen slot through its production hit testing. */
public final class ClickVanillaMenuSlotAction extends TypedRegressionAction<ClickVanillaMenuSlotAction.Arguments> {
	public record Arguments(int slot, int button, boolean quickMove) {
	}

	public ClickVanillaMenuSlotAction() {
		super("menu.clickVanillaSlot", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "button", "quickMove"), description);
		int button = optionalNonNegativeInt(args, "button", InputConstants.MOUSE_BUTTON_LEFT, description);
		if (button != InputConstants.MOUSE_BUTTON_LEFT) {
			throw new IllegalArgumentException("button on " + description + " must be the left mouse button");
		}
		return new Arguments(requireNonNegativeInt(args, "slot", description), button, optionalBoolean(args, "quickMove", false, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> before;
			private CompletableFuture<Void> after;
			private int containerId = -1;
			private boolean crafting;
			private boolean clicked;

			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof CraftingScreen) && !(context.minecraft().gui.screen() instanceof SmithingScreen)) {
					return fail("A real crafting or smithing screen is not active");
				}
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) context.minecraft().gui.screen();
				if (arguments.slot() >= screen.getMenu().slots.size()) {
					return fail("Vanilla menu slot " + arguments.slot() + " is outside the active menu");
				}
				if (containerId < 0) {
					containerId = screen.getMenu().containerId;
					crafting = screen instanceof CraftingScreen;
				}
				if (screen.getMenu().containerId != containerId || crafting != (screen instanceof CraftingScreen)) {
					return fail("Vanilla menu identity changed before slot click");
				}
				if (before == null) {
					before = context.onServer(player -> requireMatchingServerMenu(player.containerMenu));
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress beforeResult = await(before);
				if (beforeResult != RegressionActionProgress.PASSED) {
					return beforeResult;
				}
				if (!clicked) {
					Slot slot = screen.getMenu().getSlot(arguments.slot());
					VirtualInput.moveToSlot(screen, slot.index);
					if (!VirtualInput.click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8, arguments.button(),
							arguments.quickMove() ? GLFW.GLFW_MOD_SHIFT : 0)) {
						return fail("Vanilla menu slot click was not handled by the screen");
					}
					if (context.minecraft().gui.screen() != screen || screen.getMenu().containerId != containerId) {
						return fail("Vanilla menu identity changed during slot click");
					}
					clicked = true;
					after = context.onServer(player -> requireMatchingServerMenu(player.containerMenu));
					return RegressionActionProgress.WAITING;
				}
				return await(after);
			}

			private void requireMatchingServerMenu(AbstractContainerMenu menu) {
				if (menu.containerId != containerId || (crafting && !(menu instanceof CraftingMenu)) || (!crafting && !(menu instanceof SmithingMenu))) {
					throw RegressionActionFailure.infrastructure("Server menu does not match vanilla menu identity");
				}
			}
		};
	}
}
