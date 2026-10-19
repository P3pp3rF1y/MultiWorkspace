package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.Set;

public record BoundRegressionAction<A>(TypedRegressionAction<A> action, A arguments) implements RegressionActionInvocation {
	@Override
	public RegressionActionExecution start(RegressionExecutionContext context) {
		return action.start(context, arguments);
	}

	@Override
	public Set<String> dependencies() {
		return action.dependencies(arguments);
	}
}
