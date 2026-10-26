package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.Set;

public interface RegressionActionInvocation {
	RegressionAction action();

	Set<String> dependencies();

	default RegressionValueContract values() {
		return RegressionValueContract.EMPTY;
	}

	RegressionActionExecution start(RegressionExecutionContext context);
}
