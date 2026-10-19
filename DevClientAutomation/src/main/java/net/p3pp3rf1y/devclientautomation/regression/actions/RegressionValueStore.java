package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Values produced by actions for use by later actions in the same test. Store identities, not live game objects. */
public final class RegressionValueStore {
	private final Map<String, Entry<?>> values = new HashMap<>();

	public static String requireName(String name) {
		if (name == null || !name.matches("[a-z][a-z0-9-]{0,63}")) {
			throw new IllegalArgumentException("Regression value name must be a lower-case identifier: " + name);
		}
		return name;
	}

	public synchronized <T> void put(String name, Class<T> type, T value) {
		requireName(name);
		requireType(type);
		Entry<T> entry = new Entry<>(type, type.cast(Objects.requireNonNull(value, "Regression value must not be null")));
		if (values.putIfAbsent(name, entry) != null) {
			throw new IllegalStateException("Regression value " + name + " is already bound");
		}
	}

	public synchronized <T> T require(String name, Class<T> type) {
		requireName(name);
		requireType(type);
		Entry<?> entry = values.get(name);
		if (entry == null) {
			throw new IllegalStateException("Regression value " + name + " has not been bound");
		}
		if (!type.isAssignableFrom(entry.type())) {
			throw new IllegalStateException("Regression value " + name + " has type " + entry.type().getName() + "; expected " + type.getName());
		}
		return type.cast(entry.value());
	}

	private static void requireType(Class<?> type) {
		if (type == null || type == Object.class || type.isPrimitive()) {
			throw new IllegalArgumentException("Regression values require a reference type more specific than Object");
		}
	}

	private record Entry<T>(Class<T> type, T value) {
	}
}
