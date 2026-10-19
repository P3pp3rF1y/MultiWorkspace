package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

import java.util.Set;
import java.util.regex.Pattern;

public final class BackpackTemplateCommandAction extends TypedRegressionAction<BackpackTemplateCommandAction.Arguments> {
	private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	public record Arguments(String operation, String template, boolean override) {
	}

	public BackpackTemplateCommandAction() {
		super("backpack.templateCommand", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("operation", "template", "override"), description);
		String operation = requireString(args, "operation", description);
		if (!operation.equals("create") && !operation.equals("delete")) {
			throw new IllegalArgumentException("Argument operation on " + description + " must be create or delete");
		}
		String template = requireString(args, "template", description);
		if (!RESOURCE_LOCATION.matcher(template).matches()) {
			throw new IllegalArgumentException("Argument template on " + description + " must be a namespaced resource location");
		}
		if (operation.equals("delete") && args.has("override")) {
			throw new IllegalArgumentException("Argument override on " + description + " is legal only for create");
		}
		return new Arguments(operation, template, optionalBoolean(args, "override", false, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int ticks;

			@Override
			public RegressionActionProgress tick() {
				if (ticks++ == 0) {
					String command = "sophisticatedbackpacks template " + arguments.operation() + " " + arguments.template()
							+ (arguments.operation().equals("create") ? " " + arguments.override() : "");
					context.minecraft().player.connection.sendCommand(command);
				}
				return ticks >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
			}
		};
	}
}
