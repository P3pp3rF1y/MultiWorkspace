package net.p3pp3rf1y.devclientautomation.regression.actions;

import javax.annotation.Nullable;

import java.util.Map;

public interface RegressionActionExecution {
	default RegressionActionFailure.Kind failureKind() {
		return RegressionActionFailure.Kind.INFRASTRUCTURE;
	}
	RegressionActionProgress tick();

	String failure();

	@Nullable
	default Throwable failureException() {
		return null;
	}

	default Map<String, Object> expectedActual() {
		return Map.of();
	}
}
