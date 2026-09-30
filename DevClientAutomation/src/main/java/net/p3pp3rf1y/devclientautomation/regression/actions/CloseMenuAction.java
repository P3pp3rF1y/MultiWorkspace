package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

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
			if (minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
				minecraft.player.closeContainer();
			}
			return RegressionActionProgress.PASSED;
		}
	}
}
