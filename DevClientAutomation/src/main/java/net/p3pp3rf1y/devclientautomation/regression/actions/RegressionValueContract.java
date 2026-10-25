package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.List;

/** Bound, action-owned value flow. Type names are JVM binary names, never class-loaded during preflight. */
public record RegressionValueContract(List<Value> consumes, List<Value> produces) {
	public static final RegressionValueContract EMPTY = new RegressionValueContract(List.of(), List.of());

	public RegressionValueContract {
		consumes = List.copyOf(consumes);
		produces = List.copyOf(produces);
	}

	public static RegressionValueContract consuming(String name, String type) {
		return new RegressionValueContract(List.of(new Value(name, type)), List.of());
	}

	public static RegressionValueContract producing(String name, String type) {
		return new RegressionValueContract(List.of(), List.of(new Value(name, type)));
	}

	public RegressionValueContract and(RegressionValueContract other) {
		return new RegressionValueContract(java.util.stream.Stream.concat(consumes.stream(), other.consumes.stream()).toList(),
				java.util.stream.Stream.concat(produces.stream(), other.produces.stream()).toList());
	}

	public record Value(String name, String type) {
		public Value {
			RegressionValueStore.requireName(name);
			if (type == null || type.isBlank() || type.equals("java.lang.Object")) {
				throw new IllegalArgumentException("A specific value type is required");
			}
		}
	}
}
