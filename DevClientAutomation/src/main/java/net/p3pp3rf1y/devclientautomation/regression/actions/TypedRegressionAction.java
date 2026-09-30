package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

import java.util.Set;

public abstract class TypedRegressionAction<A> implements RegressionAction {
	private final RegressionActionMetadata metadata;

	protected TypedRegressionAction(String id, String revision, Set<String> dependencies) {
		metadata = new RegressionActionMetadata(id, revision, dependencies);
	}

	@Override
	public final RegressionActionMetadata metadata() {
		return metadata;
	}

	@Override
	public final RegressionActionInvocation bind(JsonObject args, String description) {
		return new BoundRegressionAction<>(this, parseArguments(args, description));
	}

	protected abstract A parseArguments(JsonObject args, String description);

	protected abstract RegressionActionExecution start(RegressionExecutionContext context, A arguments);

	protected static void requireNoArguments(JsonObject args, String description) {
		if (!args.isEmpty()) {
			throw new IllegalArgumentException("Action on " + description + " does not accept arguments");
		}
	}

	protected static void requireOnly(JsonObject args, Set<String> names, String description) {
		for (String name : args.keySet()) {
			if (!names.contains(name)) {
				throw new IllegalArgumentException("Unsupported argument " + name + " on " + description);
			}
		}
	}

	protected static String requireString(JsonObject args, String name, String description) {
		if (!args.has(name) || !args.get(name).isJsonPrimitive() || !args.get(name).getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be a string");
		}
		return args.get(name).getAsString();
	}

	protected static String optionalString(JsonObject args, String name, String defaultValue, String description) {
		return args.has(name) ? requireString(args, name, description) : defaultValue;
	}

	protected static int requirePositiveInt(JsonObject args, String name, String description) {
		if (!args.has(name) || !args.get(name).isJsonPrimitive() || !args.get(name).getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be numeric");
		}
		int value = args.get(name).getAsInt();
		if (value < 1) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be positive");
		}
		return value;
	}

	protected static int optionalPositiveInt(JsonObject args, String name, int defaultValue, String description) {
		return args.has(name) ? requirePositiveInt(args, name, description) : defaultValue;
	}

	protected static int optionalNonNegativeInt(JsonObject args, String name, int defaultValue, String description) {
		if (!args.has(name)) {
			return defaultValue;
		}
		if (!args.get(name).isJsonPrimitive() || !args.get(name).getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be numeric");
		}
		int value = args.get(name).getAsInt();
		if (value < 0) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must not be negative");
		}
		return value;
	}

}
