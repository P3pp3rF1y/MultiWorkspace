package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.neoforged.neoforge.client.ClientHooks;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public final class InventoryInteractionLayoutControlAction extends TypedRegressionAction<InventoryInteractionLayoutControlAction.Arguments> {
	public record Arguments(String control) {
	}

	public InventoryInteractionLayoutControlAction() {
		super("input.inventoryInteractionLayoutControl", "1", Set.of("minecraft:client", "sophisticatedinventoryinteractions"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("control"), description);
		String control = requireString(args, "control", description);
		if (!Set.of("reset-container", "reset-player", "cancel", "reinit", "escape").contains(control)) {
			throw new IllegalArgumentException("Unknown editor control on " + description);
		}
		return new Arguments(control);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof AbstractContainerScreen<?> screen)) {
					return fail("A container screen is required");
				}
				if (arguments.control().equals("reinit")) {
					screen.resize(screen.width, screen.height);
					return RegressionActionProgress.PASSED;
				}
				if (arguments.control().equals("escape")) {
					KeyEvent escape = new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0);
					if (!ClientHooks.onScreenKeyPressedPre(screen, escape)) {
						screen.keyPressed(escape);
					}
					return RegressionActionProgress.PASSED;
				}
				if (!InventoryInteractionLayoutProbe.capture(screen).editing()) {
					return fail("Layout editor is not active");
				}
				int index = arguments.control().equals("reset-container") ? 0 : arguments.control().equals("reset-player") ? 1 : 2;
				int width = Math.min(112, (screen.width - 8) / 3);
				return VirtualInput.click(screen, (screen.width - width * 3) / 2.0 + width * index + width / 2.0, 26, 0, 0)
						? RegressionActionProgress.PASSED
						: fail("Editor toolbar did not consume input");
			}
		};
	}
}
