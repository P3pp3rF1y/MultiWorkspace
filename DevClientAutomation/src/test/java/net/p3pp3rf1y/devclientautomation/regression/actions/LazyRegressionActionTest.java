package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LazyRegressionActionTest {
	@Test
	void linkageErrorsAreReportedAsUnavailableButRuntimeFailuresPropagate() {
		IllegalStateException unavailable = assertThrows(IllegalStateException.class, () -> proxy(() -> {
			throw new NoClassDefFoundError("optional dependency");
		}).bind(new JsonObject(), "test").start(null));
		assertEquals("Regression action test.lazy cannot start because its optional dependency is unavailable", unavailable.getMessage());
		assertInstanceOf(NoClassDefFoundError.class, unavailable.getCause());
		assertEquals("bad implementation", assertThrows(IllegalArgumentException.class, () -> proxy(() -> {
			throw new IllegalArgumentException("bad implementation");
		}).bind(new JsonObject(), "test").start(null)).getMessage());
	}

	@Test
	void delegateMetadataMustExactlyMatchTheProxyBeforeStarting() {
		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> proxy(() -> new StubAction(new RegressionActionMetadata("test.lazy", "1", Set.of("other")))).bind(new JsonObject(), "test").start(null));
		assertEquals("Lazy regression action metadata does not match test.lazy", failure.getMessage());
	}

	@Test
	void bindDoesNotInvokeTheOptionalActionFactory() {
		AtomicBoolean invoked = new AtomicBoolean();
		LazyRegressionAction action = proxy(() -> {
			invoked.set(true);
			throw new AssertionError("factory must not run during bind");
		});

		RegressionActionInvocation invocation = action.bind(new JsonObject(), "test");

		assertSame(action, invocation.action());
		assertFalse(invoked.get());
	}

	private static LazyRegressionAction proxy(java.util.function.Supplier<? extends RegressionAction> factory) {
		return new LazyRegressionAction(new RegressionActionMetadata("test.lazy", "1", Set.of("test")), factory, (args, description) -> {
		});
	}

	private record StubAction(RegressionActionMetadata metadata) implements RegressionAction {
		@Override
		public RegressionActionInvocation bind(JsonObject args, String description) {
			throw new UnsupportedOperationException();
		}
	}
}
