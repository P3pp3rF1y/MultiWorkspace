package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Keeps optional-mod action classes out of registry construction while retaining their schema metadata.
 */
public final class LazyRegressionAction implements RegressionAction {
	private final RegressionActionMetadata metadata;
	private final Supplier<? extends RegressionAction> actionFactory;
	private final BiConsumer<JsonObject, String> argumentValidator;

	public LazyRegressionAction(RegressionActionMetadata metadata, Supplier<? extends RegressionAction> actionFactory,
			BiConsumer<JsonObject, String> argumentValidator) {
		this.metadata = Objects.requireNonNull(metadata);
		this.actionFactory = Objects.requireNonNull(actionFactory);
		this.argumentValidator = Objects.requireNonNull(argumentValidator);
	}

	@Override
	public RegressionActionMetadata metadata() {
		return metadata;
	}

	@Override
	public RegressionActionInvocation bind(JsonObject args, String description) {
		argumentValidator.accept(args, description);
		return new Invocation(args.deepCopy(), description);
	}

	private final class Invocation implements RegressionActionInvocation {
		private final JsonObject args;
		private final String description;

		private Invocation(JsonObject args, String description) {
			this.args = args;
			this.description = description;
		}

		@Override
		public RegressionAction action() {
			return LazyRegressionAction.this;
		}

		@Override
		public RegressionActionExecution start(RegressionExecutionContext context) {
			try {
				RegressionAction action = Objects.requireNonNull(actionFactory.get(), "actionFactory returned null");
				if (!metadata.equals(action.metadata())) {
					throw new IllegalStateException("Lazy regression action metadata does not match " + metadata.id());
				}
				return action.bind(args, description).start(context);
			} catch (LinkageError e) {
				throw new IllegalStateException("Regression action " + metadata.id() + " cannot start because its optional dependency is unavailable", e);
			}
		}
	}
}
