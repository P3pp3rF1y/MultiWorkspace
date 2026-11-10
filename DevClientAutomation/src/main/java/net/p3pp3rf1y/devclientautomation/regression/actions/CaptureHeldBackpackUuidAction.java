package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Captures the initialized held backpack identity after independently observing both sides. */
public final class CaptureHeldBackpackUuidAction extends TypedRegressionAction<CaptureHeldBackpackUuidAction.Arguments> {
	public record Arguments(String saveAs, int timeoutTicks) {
	}

	public CaptureHeldBackpackUuidAction() {
		super("backpack.captureHeldUuid", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("saveAs", "timeoutTicks"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "saveAs", description)),
				optionalPositiveInt(args, "timeoutTicks", 100, description));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.UUID);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> server;
			private UUID serverUuid;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0)
					deadline = context.minecraft().player.tickCount + arguments.timeoutTicks();
				if (server == null) {
					server = context.onServer(player -> serverUuid = uuid(player.getMainHandItem()));
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress result = await(server);
				if (result != RegressionActionProgress.PASSED)
					return result;
				UUID client = uuid(context.minecraft().player.getMainHandItem());
				if (client.equals(serverUuid)) {
					context.values().put(arguments.saveAs(), UUID.class, client);
					return RegressionActionProgress.PASSED;
				}
				if (context.minecraft().player.tickCount >= deadline)
					return failAssertion("Held backpack UUID did not converge: server=" + serverUuid + "; client=" + client);
				server = null;
				return RegressionActionProgress.WAITING;
			}
		};
	}

	private static UUID uuid(ItemStack stack) {
		return BackpackWrapper.fromStack(stack).getContentsUuid().orElseThrow(() -> new IllegalStateException("Held backpack has no contents UUID"));
	}
}
