package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.LazyRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionInvocation;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateActionRegistryTest {
	@Test
	void indexedMovingSuiteParsesWithoutCreateAndKeepsMovingOnlyActionsEligible() throws IOException {
		RegressionSuite suite = RegressionSuiteParser.parse(movingLinkedStorageJson(), new RegressionActionRegistry());
		Set<String> movingCapabilities = Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion");
		boolean movingEligible = false;
		boolean mountedNeedsCreate = false;
		for (RegressionTest test : suite.tests()) {
			for (RegressionStep step : test.steps()) {
				RegressionActionInvocation action = step.action();
				if (action.action().metadata().id().equals("menu.captureLinkedRevision") && action.dependencies().contains("sophisticatedstorageinmotion")) {
					movingEligible = movingCapabilities.containsAll(action.dependencies());
				}
				if (action.action().metadata().id().startsWith("fixture.create.")
						|| action.action().metadata().id().equals("assert.createMountedLinkedStorage")) {
					mountedNeedsCreate |= !movingCapabilities.containsAll(action.dependencies());
				}
			}
		}
		assertTrue(movingEligible);
		assertTrue(mountedNeedsCreate);
	}

	@Test
	void createActionSchemasRejectInvalidArgumentsBeforeLazyFactoryInvocation() {
		RegressionActionRegistry registry = new RegressionActionRegistry();

		assertThrows(IllegalArgumentException.class,
				() -> registry.require("fixture.create.mountLinkedStorage").bind(JsonParser.parseString("{\"saveAs\":7}").getAsJsonObject(), "test action"));
		assertThrows(IllegalArgumentException.class, () -> registry.require("fixture.create.setMountedSlot")
				.bind(JsonParser.parseString("{\"ref\":\"mounted\",\"item\":\"minecraft:diamond\",\"count\":0}").getAsJsonObject(), "test action"));
		assertThrows(IllegalArgumentException.class, () -> registry.require("assert.createMountedLinkedStorage")
				.bind(JsonParser.parseString("{\"ref\":\"mounted\",\"checkPresentation\":\"true\"}").getAsJsonObject(), "test action"));
		assertInstanceOf(LazyRegressionAction.class, registry.require("fixture.create.mountLinkedStorage")
				.bind(JsonParser.parseString("{\"saveAs\":\"mounted\"}").getAsJsonObject(), "test action").action());
	}

	@Test
	void instantiateOptionalActionUnwrapsConstructorFailures() {
		LazyRegressionAction action = new LazyRegressionAction(new RegressionActionMetadata("test.optional", "1", Set.of("test")),
				() -> RegressionActionRegistry.instantiateOptionalAction(LinkageFailureAction.class.getName()), (args, description) -> {
				});

		IllegalStateException unavailable = assertThrows(IllegalStateException.class, () -> action.bind(new JsonObject(), "test").start(null));
		assertSame(LinkageFailureAction.FAILURE, unavailable.getCause());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionRegistry.instantiateOptionalAction(RuntimeFailureAction.class.getName()));
		assertThrows(AssertionError.class, () -> RegressionActionRegistry.instantiateOptionalAction(ErrorFailureAction.class.getName()));

		IllegalStateException reflectiveFailure = assertThrows(IllegalStateException.class,
				() -> RegressionActionRegistry.instantiateOptionalAction("missing.OptionalAction"));
		assertInstanceOf(ClassNotFoundException.class, reflectiveFailure.getCause());
	}

	private String movingLinkedStorageJson() throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/moving-linked-storage.json")) {
			assertTrue(stream != null);
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	public static final class LinkageFailureAction implements RegressionAction {
		private static final NoClassDefFoundError FAILURE = new NoClassDefFoundError("optional dependency");

		public LinkageFailureAction() {
			throw FAILURE;
		}

		@Override
		public RegressionActionMetadata metadata() {
			return new RegressionActionMetadata("test.optional", "1", Set.of("test"));
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
			return new RegressionActionMetadata("test.optional", "1", Set.of("test"));
		}

		@Override
		public RegressionActionInvocation bind(JsonObject args, String description) {
			throw new UnsupportedOperationException();
		}
	}

	public static final class ErrorFailureAction implements RegressionAction {
		public ErrorFailureAction() {
			throw new AssertionError("bad implementation");
		}

		@Override
		public RegressionActionMetadata metadata() {
			return new RegressionActionMetadata("test.optional", "1", Set.of("test"));
		}

		@Override
		public RegressionActionInvocation bind(JsonObject args, String description) {
			throw new UnsupportedOperationException();
		}
	}
}
