package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;

import java.util.Set;

public final class AwaitBackpackBlockAction extends TypedRegressionAction<AwaitBackpackBlockAction.Arguments> {
	public record Arguments(int timeoutTicks) {
	}

	public AwaitBackpackBlockAction() {
		super("await.backpackBlock", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("timeoutTicks"), description);
		return new Arguments(optionalPositiveInt(args, "timeoutTicks", 100, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private int deadline = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + arguments.timeoutTicks();
			}
			if (minecraft.level.getBlockEntity(context.target()) instanceof BackpackBlockEntity) {
				return RegressionActionProgress.PASSED;
			}
			if (minecraft.player.tickCount >= deadline) {
				return fail("Placed backpack did not synchronize to the client");
			}
			return RegressionActionProgress.WAITING;
		}
	}
}
