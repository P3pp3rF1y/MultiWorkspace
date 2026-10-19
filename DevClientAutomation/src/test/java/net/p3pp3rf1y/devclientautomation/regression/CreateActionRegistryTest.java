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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateActionRegistryTest {
	@Test
	void createActionsExposeMetadataWithoutLoadingOptionalImplementations() {
		AtomicInteger factoryCalls = new AtomicInteger();
		RegressionActionRegistry registry = new RegressionActionRegistry(missingCreateAction(factoryCalls), missingCreateAction(factoryCalls),
				missingCreateAction(factoryCalls));

		assertEquals("1", registry.all().get("fixture.create.mountLinkedStorage").revision());
		assertEquals("1", registry.all().get("fixture.create.setMountedSlot").revision());
		assertEquals("1", registry.all().get("assert.createMountedLinkedStorage").revision());
		assertEquals(0, factoryCalls.get());
	}

	@Test
	void indexedMovingSuiteDefersMissingCreateImplementationsAndKeepsMovingActionsEligible() throws IOException {
		AtomicInteger factoryCalls = new AtomicInteger();
		RegressionActionRegistry registry = new RegressionActionRegistry(missingCreateAction(factoryCalls), missingCreateAction(factoryCalls),
				missingCreateAction(factoryCalls));
		RegressionSuite suite = RegressionSuiteParser.parse(movingLinkedStorageJson(), registry);

		assertEquals(0, factoryCalls.get());
		Set<String> movingCapabilities = Set.of("minecraft:client", "sophisticatedstorageinmotion");
		boolean movingEligible = false;
		boolean createSkipped = false;
		for (RegressionTest test : suite.tests()) {
			for (RegressionStep step : test.steps()) {
				RegressionActionInvocation action = step.action();
				Set<String> dependencies = action.dependencies();
				if (action.action().metadata().id().equals("menu.captureLinkedRevision") && dependencies.contains("sophisticatedstorageinmotion")) {
					movingEligible = movingCapabilities.containsAll(dependencies);
				}
				if (action.action().metadata().id().startsWith("fixture.create.")
						|| action.action().metadata().id().equals("assert.createMountedLinkedStorage")) {
					createSkipped |= !movingCapabilities.containsAll(dependencies);
				}
			}
		}
		assertEquals(true, movingEligible);
		assertEquals(true, createSkipped);
		assertEquals(0, factoryCalls.get());
	}

	@Test
	void createActionSchemasRejectInvalidArgumentsBeforeOptionalImplementationLoads() {
		AtomicInteger factoryCalls = new AtomicInteger();
		RegressionActionRegistry registry = new RegressionActionRegistry(missingCreateAction(factoryCalls), missingCreateAction(factoryCalls),
				missingCreateAction(factoryCalls));

		assertThrows(IllegalArgumentException.class,
				() -> registry.require("fixture.create.mountLinkedStorage").bind(JsonParser.parseString("{\"saveAs\":7}").getAsJsonObject(), "test action"));
		assertThrows(IllegalArgumentException.class, () -> registry.require("assert.createMountedLinkedStorage")
				.bind(JsonParser.parseString("{\"ref\":\"mounted\",\"checkPresentation\":\"true\"}").getAsJsonObject(), "test action"));
		assertEquals(0, factoryCalls.get());
	}

	@Test
	void instantiateCreateActionUnwrapsConstructorFailures() {
		LazyRegressionAction action = new LazyRegressionAction(new RegressionActionMetadata("test.optional", "1", Set.of("test")),
				() -> RegressionActionRegistry.instantiateCreateAction(LinkageFailureAction.class.getName()), (args, description) -> {
				});

		IllegalStateException unavailable = assertThrows(IllegalStateException.class, () -> action.bind(new JsonObject(), "test").start(null));
		assertSame(LinkageFailureAction.FAILURE, unavailable.getCause());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionRegistry.instantiateCreateAction(RuntimeFailureAction.class.getName()));
		assertThrows(AssertionError.class, () -> RegressionActionRegistry.instantiateCreateAction(ErrorFailureAction.class.getName()));

		IllegalStateException reflectiveFailure = assertThrows(IllegalStateException.class,
				() -> RegressionActionRegistry.instantiateCreateAction("missing.OptionalAction"));
		assertInstanceOf(ClassNotFoundException.class, reflectiveFailure.getCause());
	}

	private String movingLinkedStorageJson() throws IOException {
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/moving-linked-storage.json")) {
			assertEquals(false, stream == null);
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static Supplier<? extends RegressionAction> missingCreateAction(AtomicInteger factoryCalls) {
		return () -> {
			factoryCalls.incrementAndGet();
			throw new NoClassDefFoundError("com/simibubi/create/Create");
		};
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
