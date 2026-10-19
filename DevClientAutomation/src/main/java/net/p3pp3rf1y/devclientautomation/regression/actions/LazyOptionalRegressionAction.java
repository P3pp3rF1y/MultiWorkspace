package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.devclientautomation.regression.RegressionJsonNumbers;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.function.BiConsumer;

/** Defers optional integration class loading until an already-qualified action executes. */
public final class LazyOptionalRegressionAction implements RegressionAction {
	private final RegressionActionMetadata metadata;
	private final String implementationClass;
	private final BiConsumer<JsonObject, String> schema;

	public LazyOptionalRegressionAction(String id, String revision, Set<String> dependencies, String implementationClass,
			BiConsumer<JsonObject, String> schema) {
		metadata = new RegressionActionMetadata(id, revision, dependencies);
		this.implementationClass = implementationClass;
		this.schema = schema;
	}

	@Override
	public RegressionActionMetadata metadata() {
		return metadata;
	}

	@Override
	public RegressionActionInvocation bind(JsonObject args, String description) {
		schema.accept(args, description);
		return new Invocation(args.deepCopy());
	}

	private final class Invocation implements RegressionActionInvocation {
		private final JsonObject arguments;

		private Invocation(JsonObject arguments) {
			this.arguments = arguments;
		}

		@Override
		public RegressionAction action() {
			return LazyOptionalRegressionAction.this;
		}

		@Override
		public Set<String> dependencies() {
			return metadata.dependencies();
		}

		@Override
		public RegressionActionExecution start(RegressionExecutionContext context) {
			try {
				RegressionAction implementation = (RegressionAction) Class.forName(implementationClass).getConstructor().newInstance();
				if (!metadata.equals(implementation.metadata())) {
					throw new IllegalStateException("Optional implementation metadata does not match proxy for " + metadata.id());
				}
				return implementation.bind(arguments, metadata.id()).start(context);
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof LinkageError linkageError) {
					throw new IllegalStateException("Optional implementation for " + metadata.id() + " is unavailable", linkageError);
				}
				if (e.getCause() instanceof RuntimeException runtimeException) {
					throw runtimeException;
				}
				if (e.getCause() instanceof Error error) {
					throw error;
				}
				throw new IllegalStateException("Optional implementation for " + metadata.id() + " is unavailable", e);
			} catch (ReflectiveOperationException | LinkageError e) {
				throw new IllegalStateException("Optional implementation for " + metadata.id() + " is unavailable", e);
			}
		}
	}

	public static void requireOnly(JsonObject args, Set<String> names, String description) {
		for (String name : args.keySet()) {
			if (!names.contains(name)) {
				throw new IllegalArgumentException("Unsupported argument " + name + " on " + description);
			}
		}
	}

	public static void requireString(JsonObject args, String name, String description) {
		if (!args.has(name) || !args.get(name).isJsonPrimitive() || !args.getAsJsonPrimitive(name).isString()) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be a string");
		}
	}

	public static void requirePositiveInt(JsonObject args, String name, String description) {
		if (!args.has(name) || !args.get(name).isJsonPrimitive() || !args.getAsJsonPrimitive(name).isNumber() || integer(args, name, description) < 1) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be positive");
		}
	}

	public static void optionalNonNegativeInt(JsonObject args, String name, String description) {
		if (args.has(name) && (!args.get(name).isJsonPrimitive() || !args.getAsJsonPrimitive(name).isNumber() || integer(args, name, description) < 0)) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must not be negative");
		}
	}

	private static int integer(JsonObject args, String name, String description) {
		try {
			return RegressionJsonNumbers.integer(args.get(name));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be an integer", e);
		}
	}
}
