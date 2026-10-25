package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.Set;

public interface RegressionActionInvocation {
	RegressionAction action();

	RegressionActionExecution start(RegressionExecutionContext context);

	default RegressionValueContract values() {
		return RegressionValueContract.EMPTY;
	}

	default Set<String> dependencies() {
		return action().metadata().dependencies();
	}
}
