package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AwaitMenuClosedAction extends TypedRegressionAction<AwaitMenuClosedAction.Arguments> {
	public record Arguments(int timeoutTicks) {
	}

	public AwaitMenuClosedAction() {
		super("await.menuClosed", "1", Set.of("minecraft:client"));
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
		private CompletableFuture<Void> serverClosed;
		private volatile boolean serverMenuClosed;

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
			if (minecraft.player.containerMenu == minecraft.player.inventoryMenu && minecraft.gui.screen() == null) {
				if (serverClosed == null) {
					serverClosed = context.onServer(player -> serverMenuClosed = player.containerMenu == player.inventoryMenu);
				}
				if (serverClosed.isDone()) {
					RegressionActionProgress progress = await(serverClosed);
					if (progress != RegressionActionProgress.PASSED || serverMenuClosed) {
						return progress;
					}
					serverClosed = null;
				}
				return minecraft.player.tickCount >= deadline ? fail("Server menu did not acknowledge close") : RegressionActionProgress.WAITING;
			}
			return minecraft.player.tickCount >= deadline ? fail("Menu close was not acknowledged by client and server") : RegressionActionProgress.WAITING;
		}
	}
}
