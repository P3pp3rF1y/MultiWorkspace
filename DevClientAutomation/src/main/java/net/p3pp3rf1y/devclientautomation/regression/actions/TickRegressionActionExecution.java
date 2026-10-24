package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class TickRegressionActionExecution implements RegressionActionExecution {
	protected final RegressionExecutionContext context;
	private String failure = "";
	private RegressionActionFailure.Kind failureKind = RegressionActionFailure.Kind.INFRASTRUCTURE;
	private Throwable exception;
	private Map<String, Object> comparison = Map.of();

	protected TickRegressionActionExecution(RegressionExecutionContext context) {
		this.context = context;
	}

	@Override
	public final String failure() {
		return failure;
	}

	@Override
	public final RegressionActionFailure.Kind failureKind() {
		return failureKind;
	}

	@Override
	public final Throwable failureException() {
		return exception;
	}

	@Override
	public final Map<String, Object> expectedActual() {
		return comparison;
	}

	protected final void observe(Object expected, Object actual) {
		comparison = Map.of("expected", expected, "actual", actual);
	}

	protected final RegressionActionProgress await(CompletableFuture<Void> future) {
		if (!future.isDone()) {
			return RegressionActionProgress.WAITING;
		}
		try {
			future.join();
			return RegressionActionProgress.PASSED;
		} catch (java.util.concurrent.CompletionException e) {
			exception = e;
			failureKind = RegressionActionFailure.classify(e);
			failure = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
			return RegressionActionProgress.FAILED;
		}
	}

	protected final RegressionActionProgress fail(String message) {
		failure = message;
		return RegressionActionProgress.FAILED;
	}

	protected final RegressionActionProgress fail(RegressionActionFailure.Kind kind, String message) {
		failureKind = kind;
		return fail(message);
	}

	protected final RegressionActionProgress failAssertion(String message) {
		return fail(RegressionActionFailure.Kind.ASSERTION, message);
	}
}
