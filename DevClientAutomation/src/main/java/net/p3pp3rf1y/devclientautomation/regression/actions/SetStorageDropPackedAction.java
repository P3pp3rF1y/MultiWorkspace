package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetStorageDropPackedAction extends TypedRegressionAction<SetStorageDropPackedAction.Arguments> {
	public record Arguments(boolean enabled) {
	}

	public SetStorageDropPackedAction() {
		super("fixture.storage.dropPacked", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("enabled"), description);
		return new Arguments(optionalBoolean(args, "enabled", true, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> context.journal().setStorageDropPacked(arguments.enabled()));
				}
				return await(future);
			}
		};
	}
}
