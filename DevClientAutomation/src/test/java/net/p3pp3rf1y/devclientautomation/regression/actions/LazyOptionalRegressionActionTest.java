package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LazyOptionalRegressionActionTest {
	@Test
	void constructorLinkageAndRuntimeFailuresAreInfrastructure() {
		for (Class<?> implementation : new Class<?>[]{LinkageFailureAction.class, RuntimeFailureAction.class}) {
			RegressionActionFailure failure = assertThrows(RegressionActionFailure.class,
					() -> proxy(implementation).bind(new JsonObject(), "test").start(null));

			assertEquals(RegressionActionFailure.Kind.INFRASTRUCTURE, RegressionActionFailure.classify(failure));
			assertEquals("Optional implementation for test.lazy could not initialize", failure.getMessage());
			assertEquals(implementation == LinkageFailureAction.class ? NoClassDefFoundError.class : IllegalArgumentException.class,
					failure.getCause().getClass());
		}
	}

	@Test
	void delegateMetadataMustExactlyMatchTheProxyBeforeStarting() {
		RegressionActionFailure failure = assertThrows(RegressionActionFailure.class,
				() -> proxy(MismatchedMetadataAction.class).bind(new JsonObject(), "test").start(null));
		assertEquals("Optional implementation metadata does not match proxy for test.lazy", failure.getMessage());
		assertEquals(RegressionActionFailure.Kind.INFRASTRUCTURE, failure.kind());
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
