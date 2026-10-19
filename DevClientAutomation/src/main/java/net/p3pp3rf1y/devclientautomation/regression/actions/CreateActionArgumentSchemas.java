package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.devclientautomation.regression.RegressionJsonNumbers;

import java.util.Set;

/** Create-free argument schemas for optional Create actions. */
public final class CreateActionArgumentSchemas {
	private CreateActionArgumentSchemas() {
	}

	public static void validateMount(JsonObject args, String description) {
		requireOnly(args, Set.of("saveAs"), description);
		RegressionValueStore.requireName(requireString(args, "saveAs", description));
	}

	public static void validateDisassemble(JsonObject args, String description) {
		requireOnly(args, Set.of("ref"), description);
		RegressionValueStore.requireName(requireString(args, "ref", description));
	}

	public static void validateSetSlot(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "item", "count"), description);
		RegressionValueStore.requireName(requireString(args, "ref", description));
		requireString(args, "item", description);
		requirePositiveInt(args, "count", description);
	}

	public static void validateAssertMounted(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "count", "checkPresentation"), description);
		RegressionValueStore.requireName(requireString(args, "ref", description));
		optionalNonNegativeInt(args, "count", 0, description);
		if (args.has("checkPresentation")
				&& (!args.get("checkPresentation").isJsonPrimitive() || !args.get("checkPresentation").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument checkPresentation on " + description + " must be boolean");
		}
	}

	private static void requireOnly(JsonObject args, Set<String> names, String description) {
		for (String name : args.keySet()) {
			if (!names.contains(name)) {
				throw new IllegalArgumentException("Unsupported argument " + name + " on " + description);
			}
		}
	}

	private static String requireString(JsonObject args, String name, String description) {
		if (!args.has(name) || !args.get(name).isJsonPrimitive() || !args.get(name).getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be a string");
		}
		return args.get(name).getAsString();
	}

	private static int requirePositiveInt(JsonObject args, String name, String description) {
		int value = requireInt(args, name, description);
		if (value < 1) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be positive");
		}
		return value;
	}

	private static int optionalNonNegativeInt(JsonObject args, String name, int defaultValue, String description) {
		if (!args.has(name)) {
			return defaultValue;
		}
		int value = requireInt(args, name, description);
		if (value < 0) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must not be negative");
		}
		return value;
	}

	private static int requireInt(JsonObject args, String name, String description) {
		if (!args.has(name) || !args.get(name).isJsonPrimitive() || !args.get(name).getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be numeric");
		}
		try {
			return RegressionJsonNumbers.integer(args.get(name));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be an integer", e);
		}
	}
}
