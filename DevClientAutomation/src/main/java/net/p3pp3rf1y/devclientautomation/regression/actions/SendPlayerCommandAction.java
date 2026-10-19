package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

import java.util.Set;

public final class SendPlayerCommandAction extends TypedRegressionAction<SendPlayerCommandAction.Arguments> {
	public record Arguments(String command) {
	}

	public SendPlayerCommandAction() {
		super("player.sendCommand", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("command"), description);
		String command = requireString(args, "command", description);
		if (command.isBlank() || command.startsWith("/") || command.contains("\n")) {
			throw new IllegalArgumentException("Command must be one nonempty command without a leading slash");
		}
		return new Arguments(command);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int ticks;

			@Override
			public RegressionActionProgress tick() {
				if (ticks++ == 0) {
					context.minecraft().player.connection.sendCommand(arguments.command());
				}
				return ticks >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
			}
		};
	}
}
