package net.p3pp3rf1y.devclientautomation.regression.actions;

public interface RegressionActionInvocation {
	RegressionAction action();

	RegressionActionExecution start(RegressionExecutionContext context);
}
