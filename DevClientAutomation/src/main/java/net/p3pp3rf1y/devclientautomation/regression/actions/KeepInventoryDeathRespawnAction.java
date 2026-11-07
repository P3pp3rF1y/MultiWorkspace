package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class KeepInventoryDeathRespawnAction extends TypedRegressionAction<Integer> {
	private static final int SETTLED_COORDINATOR_TICKS = 2;

	public KeepInventoryDeathRespawnAction() {
		super("player.keepInventoryDeathRespawn", "3", Set.of("minecraft:client"));
	}

	@Override
	protected Integer parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("respawnDelayTicks"), description);
		return optionalNonNegativeInt(args, "respawnDelayTicks", 0, description);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Integer respawnDelayTicks) {
		return new TickRegressionActionExecution(context) {
			private State state = State.KILL;
			private CompletableFuture<Void> death;
			private CompletableFuture<Boolean> serverLoaded;
			private LocalPlayer oldPlayer;
			private int delayInvocations;
			private int settledInvocations;
			private boolean deathObserved;

			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = context.minecraft();
				if (state == State.KILL) {
					oldPlayer = minecraft.player;
					if (oldPlayer == null) {
						return fail("Client player is unavailable before keep-inventory death");
					}
					death = context.onServer(player -> {
						var level = player.level();
						context.journal().recordDeathRespawnRules(level);
						level.getGameRules().set(GameRules.KEEP_INVENTORY, true, level.getServer());
						level.getGameRules().set(GameRules.IMMEDIATE_RESPAWN, false, level.getServer());
						player.kill(level);
					});
					transition(State.AWAIT_DEATH);
					return RegressionActionProgress.WAITING;
				}
				if (state == State.AWAIT_DEATH) {
					RegressionActionProgress progress = await(death);
					if (progress != RegressionActionProgress.PASSED) {
						return progress;
					}
					if (!deathObserved(minecraft)) {
						return RegressionActionProgress.WAITING;
					}
					transition(State.DELAY);
				}
				if (state == State.DELAY) {
					if (delayInvocations++ < respawnDelayTicks) {
						putState();
						return RegressionActionProgress.WAITING;
					}
					oldPlayer.respawn();
					transition(State.AWAIT_REPLACEMENT);
					return RegressionActionProgress.WAITING;
				}
				if (state == State.AWAIT_REPLACEMENT) {
					return awaitReplacement(minecraft);
				}
				return RegressionActionProgress.PASSED;
			}

			private boolean deathObserved(Minecraft minecraft) {
				deathObserved = oldPlayer != null && (oldPlayer.isDeadOrDying() || oldPlayer.getHealth() <= 0.0F || minecraft.screen instanceof DeathScreen);
				if (deathObserved) {
					putState();
				}
				return deathObserved;
			}

			private RegressionActionProgress awaitReplacement(Minecraft minecraft) {
				if (serverLoaded == null) {
					serverLoaded = context.onServerResult(player -> player.connection.hasClientLoaded());
					return RegressionActionProgress.WAITING;
				}
				if (!serverLoaded.isDone()) {
					return RegressionActionProgress.WAITING;
				}
				boolean serverReady;
				try {
					serverReady = serverLoaded.join();
				} catch (RuntimeException e) {
					return fail("Could not observe server client-loaded state after respawn: " + e.getMessage());
				}
				serverLoaded = null;
				LocalPlayer replacement = minecraft.player;
				boolean clientReady = replacement != null && replacement != oldPlayer && !replacement.isDeadOrDying() && replacement.getHealth() > 0.0F
						&& !(minecraft.screen instanceof DeathScreen) && minecraft.getConnection() != null && minecraft.getConnection().hasClientLoaded()
						&& replacement.containerMenu == replacement.inventoryMenu && minecraft.screen == null;
				if (clientReady && serverReady) {
					settledInvocations++;
					if (settledInvocations >= SETTLED_COORDINATOR_TICKS) {
						transition(State.COMPLETE);
						return RegressionActionProgress.PASSED;
					}
				} else {
					settledInvocations = 0;
				}
				putState();
				return RegressionActionProgress.WAITING;
			}

			private void transition(State next) {
				state = next;
				putState();
			}

			private void putState() {
				context.putDiagnostic("keepInventoryDeathRespawn",
						Map.of("state", state.name(), "deathObserved", deathObserved, "oldPlayerId", oldPlayer == null ? "" : oldPlayer.getUUID().toString(),
								"requestedDelayTicks", respawnDelayTicks, "delayCoordinatorInvocations", delayInvocations, "settledCoordinatorInvocations",
								settledInvocations, "coordinatorTicks", context.coordinatorTicks()));
			}
		};
	}

	private enum State {
		KILL, AWAIT_DEATH, DELAY, AWAIT_REPLACEMENT, COMPLETE
	}
}
