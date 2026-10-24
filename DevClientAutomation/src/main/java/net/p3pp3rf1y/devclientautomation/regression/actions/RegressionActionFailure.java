package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.concurrent.CompletionException;

/** An action-owned classification, preserved across server futures and action start/tick boundaries. */
public final class RegressionActionFailure extends RuntimeException {
	public enum Kind {
		ASSERTION("assertion", "failed"), INFRASTRUCTURE("infrastructure", "error"), TIMEOUT("timeout", "error"), UNEXPECTED_EXCEPTION("unexpectedException",
				"error");

		private final String category;
		private final String outcome;

		Kind(String category, String outcome) {
			this.category = category;
			this.outcome = outcome;
		}

		public String category() {
			return category;
		}
		public String outcome() {
			return outcome;
		}
	}

	private final Kind kind;

	public RegressionActionFailure(Kind kind, String message) {
		super(message);
		this.kind = kind;
	}

	public RegressionActionFailure(Kind kind, String message, Throwable cause) {
		super(message, cause);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	public static RegressionActionFailure assertion(String message) {
		return new RegressionActionFailure(Kind.ASSERTION, message);
	}

	public static RegressionActionFailure infrastructure(String message) {
		return new RegressionActionFailure(Kind.INFRASTRUCTURE, message);
	}

	public static Kind classify(Throwable exception) {
		Throwable cause = exception;
		while (cause instanceof CompletionException && cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause instanceof RegressionActionFailure failure ? failure.kind() : Kind.UNEXPECTED_EXCEPTION;
	}
}
