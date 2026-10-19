package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.sdl.SDLScancode;

import java.util.Set;

public final class CloseMenuAction extends TypedRegressionAction<CloseMenuAction.Arguments> {
	public record Arguments() {
	}

	public CloseMenuAction() {
		super("menu.close", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (minecraft.gui.screen() != null) {
				if (!minecraft.gui.screen().keyPressed(new KeyEvent(SDLScancode.SDL_SCANCODE_ESCAPE, 0, 0)) && minecraft.gui.screen() != null) {
					minecraft.gui.screen().onClose();
				}
			}
			return RegressionActionProgress.PASSED;
		}
	}
}
