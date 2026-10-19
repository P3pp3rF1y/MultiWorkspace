package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LazyRegressionActionTest {
	private static final RegressionActionMetadata METADATA = new RegressionActionMetadata("test.lazy", "2", Set.of("test"));

	@Test
	void linkageErrorsAreReportedAsUnavailableButRuntimeFailuresPropagate() {
		IllegalStateException unavailable = start(() -> {
			throw new NoClassDefFoundError("optional dependency");
		});
		assertInstanceOf(NoClassDefFoundError.class, unavailable.getCause());
		assertEquals("bad implementation", assertThrows(IllegalArgumentException.class, () -> new LazyRegressionAction(METADATA, () -> {
			throw new IllegalArgumentException("bad implementation");
		}, (args, description) -> {
		}).bind(new JsonObject(), "test").start(null)).getMessage());
	}

	@Test
	void delegateMetadataMustExactlyMatchTheProxyBeforeStarting() {
		IllegalStateException mismatch = start(() -> new RegressionAction() {
			@Override
			public RegressionActionMetadata metadata() {
				return new RegressionActionMetadata("other", "2", Set.of("test"));
			}
			@Override
			public RegressionActionInvocation bind(JsonObject args, String description) {
				throw new UnsupportedOperationException();
			}
		});
		assertEquals("Lazy regression action metadata does not match test.lazy", mismatch.getMessage());
	}

	@Test
	void startReportsDirectLinkageErrorsAsUnavailableOptionalDependencies() {
		assertInstanceOf(NoClassDefFoundError.class, start(() -> {
			throw new NoClassDefFoundError("optional dependency");
		}).getCause());
	}

	@Test
	void delegatedMetadataMustMatchAndOtherFailuresAreNotHidden() {
		IllegalStateException mismatch = start(() -> new RegressionAction() {
			@Override
			public RegressionActionMetadata metadata() {
				return new RegressionActionMetadata("other", "2", Set.of("test"));
			}
			@Override
			public RegressionActionInvocation bind(JsonObject args, String description) {
				throw new UnsupportedOperationException();
			}
		});
		assertEquals("Lazy regression action metadata does not match test.lazy", mismatch.getMessage());
		assertEquals("bad implementation", start(() -> {
			throw new IllegalStateException("bad implementation");
		}).getMessage());
	}

	private static IllegalStateException start(Supplier<? extends RegressionAction> factory) {
		return assertThrows(IllegalStateException.class, () -> new LazyRegressionAction(METADATA, factory, (args, description) -> {
		}).bind(new JsonObject(), "test").start(null));
	}
}
