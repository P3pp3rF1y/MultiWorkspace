package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Applies a fatal, ordinary damage source to the server player. */
public final class LethalDamageAction extends TypedRegressionAction<String> {
	public LethalDamageAction() {
		super("player.lethalDamage", "1", Set.of("minecraft:client"));
	}

	@Override
	protected String parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("source"), description);
		String source = requireString(args, "source", description);
		if (!Set.of("fire", "wither", "generic").contains(source)) {
			throw new IllegalArgumentException("Argument source on " + description + " must be fire, wither, or generic");
		}
		return source;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, String source) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			private boolean accepted;
			private boolean awaitingSpawnProtection;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.coordinatorTicks() + 100;
				}
				if (future == null) {
					future = context.onServer(player -> {
						float healthBefore = player.getHealth();
						DamageSource damageSource = switch (source) {
							case "fire" -> player.level().damageSources().inFire();
							case "wither" -> player.level().damageSources().wither();
							case "generic" -> player.level().damageSources().generic();
							default -> throw new IllegalStateException("Unexpected lethal damage source " + source);
						};
						// ServerPlayer starts with 60 ticks of spawn protection; wait for its server ticks
						// rather than repeating an attack that could already have triggered a rescue.
						if (player.tickCount < 60 && !player.getAbilities().invulnerable && !player.isInvulnerableTo(damageSource)) {
							awaitingSpawnProtection = true;
							return;
						}
						accepted = player.hurt(damageSource, Float.MAX_VALUE);
						if (!accepted) {
							throw RegressionActionFailure
									.assertion("Lethal " + source + " damage was not accepted: game mode " + player.gameMode.getGameModeForPlayer().getName()
											+ ", health before " + healthBefore + ", health after " + player.getHealth());
						}
					});
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED) {
					return progress == RegressionActionProgress.WAITING && context.coordinatorTicks() >= deadline
							? failAssertion("Lethal " + source + " damage observation did not complete within 100 coordinator ticks")
							: progress;
				}
				if (awaitingSpawnProtection) {
					if (context.coordinatorTicks() >= deadline) {
						return failAssertion("Spawn protection did not expire within 100 coordinator ticks before lethal " + source + " damage");
					}
					awaitingSpawnProtection = false;
					future = null;
					return RegressionActionProgress.WAITING;
				}
				return accepted ? RegressionActionProgress.PASSED : failAssertion("Lethal " + source + " damage was not accepted");
			}
		};
	}
}
