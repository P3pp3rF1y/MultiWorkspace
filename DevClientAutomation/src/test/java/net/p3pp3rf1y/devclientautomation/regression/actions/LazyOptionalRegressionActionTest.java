package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LazyOptionalRegressionActionTest {
	@Test
	void linkageErrorsAreReportedAsUnavailableButRuntimeFailuresPropagate() {
		IllegalStateException unavailable = assertThrows(IllegalStateException.class,
				() -> proxy(LinkageFailureAction.class).bind(new JsonObject(), "test").start(null));
		assertEquals("Optional implementation for test.lazy is unavailable", unavailable.getMessage());
		assertThrows(IllegalArgumentException.class, () -> proxy(RuntimeFailureAction.class).bind(new JsonObject(), "test").start(null));
	}

	@Test
	void delegateMetadataMustExactlyMatchTheProxyBeforeStarting() {
		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> proxy(MismatchedMetadataAction.class).bind(new JsonObject(), "test").start(null));
		assertEquals("Optional implementation metadata does not match proxy for test.lazy", failure.getMessage());
	}

	private static LazyOptionalRegressionAction proxy(Class<?> implementation) {
		return new LazyOptionalRegressionAction("test.lazy", "1", Set.of("test"), implementation.getName(), (args, description) -> {
		});
	}

	public static final class LinkageFailureAction implements RegressionAction {
		public LinkageFailureAction() {
			throw new NoClassDefFoundError("optional dependency");
		}
		@Override
		public RegressionActionMetadata metadata() {
			return new RegressionActionMetadata("test.lazy", "1", Set.of("test"));
		}
		@Override
		public RegressionActionInvocation bind(JsonObject args, String description) {
			throw new UnsupportedOperationException();
		}
	}

	public static final class RuntimeFailureAction implements RegressionAction {
		public RuntimeFailureAction() {
			throw new IllegalArgumentException("bad implementation");
		}
		@Override
		public RegressionActionMetadata metadata() {
			return new RegressionActionMetadata("test.lazy", "1", Set.of("test"));
		}
		@Override
		public RegressionActionInvocation bind(JsonObject args, String description) {
			throw new UnsupportedOperationException();
		}
	}

	public static final class MismatchedMetadataAction implements RegressionAction {
		public MismatchedMetadataAction() {
		}
		@Override
		public RegressionActionMetadata metadata() {
			return new RegressionActionMetadata("other", "1", Set.of("test"));
		}
		@Override
		public RegressionActionInvocation bind(JsonObject args, String description) {
			throw new UnsupportedOperationException();
		}
	}
}
