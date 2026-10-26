package net.p3pp3rf1y.devclientautomation.regression;

import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionValueContract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Pure checks only: no execution contexts, optional implementations, journals, or game objects. */
final class RegressionPreflight {
	private RegressionPreflight() {
	}

	static List<String> missing(Set<String> requirements, Predicate<String> loaded) {
		return requirements.stream().filter(requirement -> !loaded.test(requirement)).sorted().toList();
	}

	static void validateValues(RegressionTest test) {
		Map<String, String> produced = new LinkedHashMap<>();
		for (RegressionStep step : test.steps()) {
			RegressionValueContract contract = step.action().values();
			for (RegressionValueContract.Value input : contract.consumes()) {
				String actual = produced.get(input.name());
				if (!input.type().equals(actual)) {
					throw new IllegalArgumentException("Value preflight " + test.id() + "/" + step.id() + " " + step.provenance() + ": " + input.name()
							+ " requires earlier " + input.type() + ", observed " + (actual == null ? "no producer" : actual));
				}
			}
			for (RegressionValueContract.Value output : contract.produces()) {
				if (produced.putIfAbsent(output.name(), output.type()) != null) {
					throw new IllegalArgumentException(
							"Value preflight " + test.id() + "/" + step.id() + " " + step.provenance() + ": duplicate producer for " + output.name());
				}
			}
		}
	}
}
