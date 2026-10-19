package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.Set;

public interface RegressionActionInvocation {
	RegressionAction action();

	Set<String> dependencies();

	RegressionActionExecution start(RegressionExecutionContext context);
}
