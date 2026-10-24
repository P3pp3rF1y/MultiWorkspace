package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.Set;

public record BoundRegressionAction<A>(TypedRegressionAction<A> action, A arguments) implements RegressionActionInvocation {
	@Override
	public RegressionActionExecution start(RegressionExecutionContext context) {
		context.values().reserveAll(values().produces());
		return action.start(context, arguments);
	}

	@Override
	public RegressionValueContract values() {
		return action.values(arguments);
	}

	@Override
	public Set<String> dependencies() {
		return action.dependencies(arguments);
	}
}
