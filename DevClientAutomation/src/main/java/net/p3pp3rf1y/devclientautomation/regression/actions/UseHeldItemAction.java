package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

import java.util.Set;

public final class UseHeldItemAction extends TypedRegressionAction<UseHeldItemAction.Arguments> {
	public record Arguments() {
	}

	public UseHeldItemAction() {
		super("player.useHeldItem", "1", Set.of("minecraft:client"));
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
		private int deadline = -1;

		private Execution(RegressionExecutionContext context) {
			super(context);
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
				minecraft.player.closeContainer();
				return RegressionActionProgress.WAITING;
			}
			if (!minecraft.player.getMainHandItem().isEmpty() && minecraft.gameMode != null) {
				minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
				return RegressionActionProgress.PASSED;
			}
			return minecraft.player.tickCount >= deadline ? fail("Client held item did not synchronize before item use") : RegressionActionProgress.WAITING;
		}
	}
}
