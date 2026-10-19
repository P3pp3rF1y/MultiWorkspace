package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

import java.util.Set;

/** Retains strict suite binding and capability reporting without linking the unavailable 26.1 Create binary. */
public final class UnavailableCreateAction extends TypedRegressionAction<JsonObject> {
	private final String schema;

	public UnavailableCreateAction(String id, String dependency, String schema) {
		super(id, "1", Set.of("minecraft:client", dependency));
		this.schema = schema;
	}

	@Override
	protected JsonObject parseArguments(JsonObject args, String description) {
		switch (schema) {
			case "mount" -> {
				requireOnly(args, Set.of("saveAs"), description);
				RegressionValueStore.requireName(requireString(args, "saveAs", description));
			}
			case "disassemble" -> {
				requireOnly(args, Set.of("ref"), description);
				RegressionValueStore.requireName(requireString(args, "ref", description));
			}
			case "slot" -> {
				requireOnly(args, Set.of("ref", "item", "count"), description);
				RegressionValueStore.requireName(requireString(args, "ref", description));
				requireString(args, "item", description);
				requirePositiveInt(args, "count", description);
			}
			case "assert" -> {
				requireOnly(args, Set.of("ref", "count", "checkPresentation"), description);
				RegressionValueStore.requireName(requireString(args, "ref", description));
				optionalNonNegativeInt(args, "count", 0, description);
				if (args.has("checkPresentation")
						&& (!args.get("checkPresentation").isJsonPrimitive() || !args.get("checkPresentation").getAsJsonPrimitive().isBoolean())) {
					throw new IllegalArgumentException("Argument checkPresentation on " + description + " must be boolean");
				}
			}
			default -> throw new IllegalArgumentException("Unknown Create action schema " + schema);
		}
		return args.deepCopy();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, JsonObject arguments) {
		throw new IllegalStateException("Create regression action " + metadata().id() + " is unavailable on 26.1");
	}
}
