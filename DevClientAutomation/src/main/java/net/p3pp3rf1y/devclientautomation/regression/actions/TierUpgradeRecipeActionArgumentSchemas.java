package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

import java.util.Set;

/** Argument schemas retained when optional tier recipe actions are unavailable. */
public final class TierUpgradeRecipeActionArgumentSchemas {
	private TierUpgradeRecipeActionArgumentSchemas() {
	}

	public static void validateStorage(JsonObject args, String description) {
		validate(args, "form", Set.of("normal", "double"), description);
	}

	public static void validateMovingStorage(JsonObject args, String description) {
		validate(args, "carrier", Set.of("boat", "minecart"), description);
	}

	private static void validate(JsonObject args, String variantName, Set<String> variants, String description) {
		requireOnly(args, Set.of(variantName, "recipe", "linkage"), description);
		require(args, variantName, variants, description);
		require(args, "recipe", Set.of("shaped", "shapeless"), description);
		require(args, "linkage", Set.of("unlinked", "primary", "secondary"), description);
	}

	private static void requireOnly(JsonObject args, Set<String> names, String description) {
		for (String name : args.keySet()) {
			if (!names.contains(name)) {
				throw new IllegalArgumentException("Unsupported argument " + name + " on " + description);
			}
		}
	}

	private static void require(JsonObject args, String name, Set<String> values, String description) {
		if (!args.has(name) || !args.get(name).isJsonPrimitive() || !args.getAsJsonPrimitive(name).isString()) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be a string");
		}
		String value = args.get(name).getAsString();
		if (!values.contains(value)) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be one of " + values);
		}
	}
}
