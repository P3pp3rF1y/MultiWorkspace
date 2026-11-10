package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asserts that the player remains alive with at least the requested health. */
public final class AssertPlayerHealthAction extends TypedRegressionAction<Integer> {
	public AssertPlayerHealthAction() {
		super("assert.playerHealth", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Integer parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("minimum"), description);
		return optionalPositiveInt(args, "minimum", 1, description);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Integer minimum) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> serverHealth;
			private float expected;
			private UUID expectedPlayer;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (serverHealth == null) {
					serverHealth = context.onServer(player -> {
						float actual = player.getHealth();
						observe(Map.of("minimum", minimum), Map.of("health", actual));
						if (player.isDeadOrDying() || actual < minimum) {
							throw RegressionActionFailure.assertion("Server player is not alive with required health: minimum " + minimum + ", health " + actual
									+ ", deadOrDying " + player.isDeadOrDying());
						}
						expected = actual;
						expectedPlayer = player.getUUID();
					});
					deadline = context.coordinatorTicks() + 100;
				}
				RegressionActionProgress progress = await(serverHealth);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				if (context.minecraft().player == null) {
					return context.coordinatorTicks() >= deadline
							? failAssertion("Client player is unavailable while synchronizing server health " + expected)
							: RegressionActionProgress.WAITING;
				}
				Player clientPlayer = context.minecraft().player;
				if (!clientPlayer.getUUID().equals(expectedPlayer)) {
					return context.coordinatorTicks() >= deadline
							? failAssertion("Client player is stale while synchronizing server health " + expected + ": expected " + expectedPlayer
									+ ", actual " + clientPlayer.getUUID())
							: RegressionActionProgress.WAITING;
				}
				float actual = clientPlayer.getHealth();
				observe(Map.of("minimum", minimum, "serverHealth", expected), Map.of("health", actual));
				if (!clientPlayer.isDeadOrDying() && actual >= minimum && Math.abs(actual - expected) < 0.01F) {
					return RegressionActionProgress.PASSED;
				}
				return context.coordinatorTicks() >= deadline
						? failAssertion("Client player health did not synchronize: expected " + expected + ", actual " + actual + ", deadOrDying "
								+ clientPlayer.isDeadOrDying())
						: RegressionActionProgress.WAITING;
			}
		};
	}
}
