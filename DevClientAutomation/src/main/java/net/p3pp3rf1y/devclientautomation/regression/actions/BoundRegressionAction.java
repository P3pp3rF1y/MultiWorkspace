package net.p3pp3rf1y.devclientautomation.regression.actions;

public record BoundRegressionAction<A>(TypedRegressionAction<A> action, A arguments) implements RegressionActionInvocation {
	@Override
	public RegressionActionExecution start(RegressionExecutionContext context) {
		return action.start(context, arguments);
	}
}
