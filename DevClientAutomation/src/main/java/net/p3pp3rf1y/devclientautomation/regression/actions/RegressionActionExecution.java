package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.Map;

public interface RegressionActionExecution {
	default RegressionActionFailure.Kind failureKind() {
		return RegressionActionFailure.Kind.INFRASTRUCTURE;
	}
	RegressionActionProgress tick();

	String failure();

	default Throwable failureException() {
		return null;
	}

	default Map<String, Object> expectedActual() {
		return Map.of();
	}
}
