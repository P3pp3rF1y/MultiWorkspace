package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RegressionActionFailureTest {
	@Test
	void explicitKindsSurviveDirectAndAsynchronousFailureWithoutActionNameInference() {
		for (RegressionActionFailure.Kind kind : RegressionActionFailure.Kind.values()) {
			var failure = new RegressionActionFailure(kind, "same action, different cause");
			var execution = new FutureExecution(CompletableFuture.failedFuture(failure));

			assertEquals(kind, RegressionActionFailure.classify(failure));
			assertEquals(RegressionActionProgress.FAILED, execution.tick());
			assertEquals(kind, execution.failureKind());
			assertSame(failure, execution.failureException().getCause());
			assertEquals(kind, RegressionActionFailure.classify(new CompletionException(new CompletionException(failure))));
		}
	}

	@Test
	void untypedExceptionsStayUnexpectedEvenWhenTheyAreIllegalStateExceptions() {
		var failure = new IllegalStateException("not a declared assertion");
		var execution = new FutureExecution(CompletableFuture.failedFuture(failure));

		assertEquals(RegressionActionFailure.Kind.UNEXPECTED_EXCEPTION, RegressionActionFailure.classify(failure));
		assertEquals(RegressionActionProgress.FAILED, execution.tick());
		assertEquals(RegressionActionFailure.Kind.UNEXPECTED_EXCEPTION, execution.failureKind());
	}

	private static final class FutureExecution extends TickRegressionActionExecution {
		private final CompletableFuture<Void> future;
		private FutureExecution(CompletableFuture<Void> future) {
			super(null);
			this.future = future;
		}
		@Override
		public RegressionActionProgress tick() {
			return await(future);
		}
	}
}
