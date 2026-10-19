package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.concurrent.CompletableFuture;

public abstract class TickRegressionActionExecution implements RegressionActionExecution {
	protected final RegressionExecutionContext context;
	private String failure = "";

	protected TickRegressionActionExecution(RegressionExecutionContext context) {
		this.context = context;
	}

	@Override
	public final String failure() {
		return failure;
	}

	protected final RegressionActionProgress await(CompletableFuture<Void> future) {
		if (!future.isDone()) {
			return RegressionActionProgress.WAITING;
		}
		try {
			future.join();
			return RegressionActionProgress.PASSED;
		} catch (RuntimeException e) {
			failure = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
			return RegressionActionProgress.FAILED;
		}
	}

	protected final RegressionActionProgress fail(String message) {
		failure = message;
		return RegressionActionProgress.FAILED;
	}
}
