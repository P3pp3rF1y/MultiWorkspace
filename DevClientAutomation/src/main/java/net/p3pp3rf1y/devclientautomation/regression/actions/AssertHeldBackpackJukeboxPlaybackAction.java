package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackRenderInfo;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeRenderData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.RepeatMode;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AssertHeldBackpackJukeboxPlaybackAction extends TypedRegressionAction<AssertHeldBackpackJukeboxPlaybackAction.Arguments> {
	public record Arguments(String saveAs, String baseline, boolean playing) {
	}

	public record Baseline(UUID contentsUuid, LinkedStorageEndpointStackState endpointState, boolean shuffle, RepeatMode repeatMode, List<ItemStack> discs,
			int soundReceiptCount) {
		Baseline withSoundReceiptCount(int receiptCount) {
			return new Baseline(contentsUuid, endpointState, shuffle, repeatMode, discs, receiptCount);
		}
	}

	private record PlaybackState(boolean contentsUuidMatches, boolean endpointStateMatches, boolean shuffleMatches, boolean repeatModeMatches,
			boolean discsMatch, boolean wrapperPlaying, boolean componentPlaying, boolean soundPlaying, boolean projectionPresent, boolean finishTimeFuture,
			boolean finishTimeAbsent, boolean activeDiscAbsent, long finishTime, long gameTime) {
	}

	private record ClientPlaybackState(boolean wrapperPlaying, boolean componentPlaying, boolean projectionPlaying, boolean cachedProjectionPlaying,
			boolean finishTimeFuture, boolean finishTimeAbsent, boolean activeDiscAbsent, long finishTime, long gameTime) {
	}

	public AssertHeldBackpackJukeboxPlaybackAction() {
		super("assert.heldBackpackJukeboxPlayback", "2", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("saveAs", "baseline", "playing"), description);
		if (!args.has("playing") || !args.get("playing").isJsonPrimitive() || !args.getAsJsonPrimitive("playing").isBoolean()) {
			throw new IllegalArgumentException("Argument playing on " + description + " must be a boolean");
		}
		String saveAs = optionalString(args, "saveAs", "", description);
		String baseline = optionalString(args, "baseline", "", description);
		if (saveAs.isBlank() == baseline.isBlank()) {
			throw new IllegalArgumentException("Exactly one of saveAs or baseline is required on " + description);
		}
		return new Arguments(saveAs.isBlank() ? "" : RegressionValueStore.requireName(saveAs),
				baseline.isBlank() ? "" : RegressionValueStore.requireName(baseline), args.get("playing").getAsBoolean());
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.saveAs().isEmpty()
				? RegressionValueContract.consuming(arguments.baseline(), RegressionValueTypes.HELD_BACKPACK_JUKEBOX_BASELINE)
				: RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.HELD_BACKPACK_JUKEBOX_BASELINE);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Runtime.Execution(context, arguments);
	}

	/** Isolated so schema-only binding does not link Minecraft runtime classes. */
	private static final class Runtime {
		private static final class Execution extends TickRegressionActionExecution {
			private final Arguments arguments;
			private final Baseline baseline;
			private Baseline capturedBaseline;
			private CompletableFuture<Void> serverAssertion;
			private boolean serverMatched;
			private PlaybackState serverState;
			private ClientPlaybackState clientState;
			private int deadline = -1;
			private long firstRemaining = -1;
			private int firstTick;

			private Execution(RegressionExecutionContext context, Arguments arguments) {
				super(context);
				this.arguments = arguments;
				baseline = arguments.baseline().isEmpty() ? null : context.values().require(arguments.baseline(), Baseline.class);
			}

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (serverAssertion == null) {
					serverAssertion = context.onServer(player -> {
						JukeboxUpgradeWrapper jukebox = jukebox(player.getMainHandItem());
						Baseline captured = baseline == null ? capturedBaseline : baseline;
						if (captured == null) {
							captured = capture(player.getMainHandItem(), jukebox);
							capturedBaseline = captured;
						}
						serverState = serverState(player, jukebox, captured);
						serverMatched = matchesServer(serverState, arguments.playing());
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress progress = await(serverAssertion);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				if (!serverMatched) {
					observe(expectedServerState(arguments.playing()), serverState);
					serverAssertion = null;
					return timedOut("Server held backpack jukebox state did not match playback contract");
				}
				clientState = clientState(jukebox(context.minecraft().player.getMainHandItem()));
				HeldBackpackJukeboxSoundObserver soundObserver = arguments.playing() ? context.heldBackpackJukeboxSoundObserver() : null;
				if (!matchesClient(clientState, soundObserver, baseline, arguments.playing())) {
					observe(expectedClientState(arguments.playing()),
							Map.of("state", clientState, "soundObserver", soundObserver == null ? Map.of("required", false) : soundObserver.diagnostics()));
					return timedOut("Client held backpack jukebox state, render projection, or expected sound receipt did not match playback contract");
				}
				if (!arguments.playing()) {
					return passed();
				}
				long remaining = clientState.finishTime() - context.minecraft().level.getGameTime();
				if (remaining <= 0) {
					return timedOut("Client held backpack jukebox has no positive disc finish time");
				}
				if (firstRemaining < 0) {
					firstRemaining = remaining;
					firstTick = context.minecraft().player.tickCount;
				}
				return context.minecraft().player.tickCount >= firstTick + 5 && remaining < firstRemaining
						? passed()
						: timedOut("Client held backpack jukebox finish time did not progress");
			}

			private RegressionActionProgress passed() {
				if (baseline == null) {
					context.values().put(arguments.saveAs(), Baseline.class,
							capturedBaseline.withSoundReceiptCount(context.heldBackpackJukeboxSoundObserver().receiptCount()));
				}
				return RegressionActionProgress.PASSED;
			}

			private RegressionActionProgress timedOut(String message) {
				return context.minecraft().player.tickCount >= deadline ? failAssertion(message) : RegressionActionProgress.WAITING;
			}
		}

		private static JukeboxUpgradeWrapper jukebox(ItemStack stack) {
			return BackpackWrapper.fromStack(stack).getUpgradeHandler().getTypeWrappers(JukeboxUpgradeItem.TYPE).stream().findFirst()
					.orElseThrow(() -> RegressionActionFailure.infrastructure("Held backpack has no jukebox upgrade"));
		}

		private static Baseline capture(ItemStack stack, JukeboxUpgradeWrapper jukebox) {
			LinkedStorageEndpointStackState endpointState = LinkedStorageStackLifecycle.classifyEndpoint(stack);
			if (endpointState == LinkedStorageEndpointStackState.ENDPOINT) {
				throw RegressionActionFailure.assertion("Held backpack jukebox baseline requires a non-endpoint stack");
			}
			UUID contentsUuid = BackpackWrapper.fromStack(stack).getContentsUuid()
					.orElseThrow(() -> RegressionActionFailure.assertion("Held backpack has no contents UUID"));
			List<ItemStack> discs = new ArrayList<>();
			for (int slot = 0; slot < jukebox.getDiscInventory().getSlots(); slot++) {
				discs.add(jukebox.getDiscInventory().getStackInSlot(slot).copy());
			}
			return new Baseline(contentsUuid, endpointState, jukebox.isShuffleEnabled(), jukebox.getRepeatMode(), List.copyOf(discs), -1);
		}

		private static PlaybackState serverState(ServerPlayer player, JukeboxUpgradeWrapper jukebox, Baseline baseline) {
			ItemStack upgrade = jukebox.getUpgradeStack();
			long finishTime = jukebox.getDiscFinishTime();
			ItemStack stack = player.getMainHandItem();
			return new PlaybackState(BackpackWrapper.fromStack(stack).getContentsUuid().filter(baseline.contentsUuid()::equals).isPresent(),
					LinkedStorageStackLifecycle.classifyEndpoint(stack) == baseline.endpointState(), jukebox.isShuffleEnabled() == baseline.shuffle(),
					jukebox.getRepeatMode() == baseline.repeatMode(), discsMatch(jukebox, baseline.discs()), jukebox.isPlaying(),
					upgrade.getOrDefault(ModCoreDataComponents.IS_PLAYING, false),
					ServerStorageSoundHandler.isStorageSoundPlayingNear(player.level(), player.position(), 1),
					BackpackRenderInfo.fromPhysicalStack(stack).getUpgradeRenderData(JukeboxUpgradeRenderData.TYPE).isPresent(),
					finishTime > player.level().getGameTime(), !upgrade.has(ModCoreDataComponents.DISC_FINISH_TIME),
					upgrade.getOrDefault(ModCoreDataComponents.DISC_SLOT_ACTIVE, -1) == -1, finishTime, player.level().getGameTime());
		}

		private static boolean matchesServer(PlaybackState state, boolean playing) {
			return state.contentsUuidMatches() && state.endpointStateMatches() && state.shuffleMatches() && state.repeatModeMatches() && state.discsMatch()
					&& state.wrapperPlaying() == playing && state.componentPlaying() == playing && state.soundPlaying() == playing
					&& (playing ? state.finishTimeFuture() : state.finishTimeAbsent() && state.activeDiscAbsent() && !state.projectionPresent());
		}

		private static Map<String, Boolean> expectedServerState(boolean playing) {
			return playing
					? Map.of("contentsUuidMatches", true, "endpointStateMatches", true, "shuffleMatches", true, "repeatModeMatches", true, "discsMatch", true,
							"wrapperPlaying", true, "componentPlaying", true, "soundPlaying", true, "finishTimeFuture", true)
					: Map.ofEntries(Map.entry("contentsUuidMatches", true), Map.entry("endpointStateMatches", true), Map.entry("shuffleMatches", true),
							Map.entry("repeatModeMatches", true), Map.entry("discsMatch", true), Map.entry("wrapperPlaying", false),
							Map.entry("componentPlaying", false), Map.entry("soundPlaying", false), Map.entry("projectionPresent", false),
							Map.entry("finishTimeAbsent", true), Map.entry("activeDiscAbsent", true));
		}

		private static ClientPlaybackState clientState(JukeboxUpgradeWrapper jukebox) {
			ItemStack upgrade = jukebox.getUpgradeStack();
			ItemStack mainHand = Minecraft.getInstance().player.getMainHandItem();
			boolean projection = BackpackRenderInfo.fromPhysicalStack(mainHand).getUpgradeRenderData(JukeboxUpgradeRenderData.TYPE)
					.map(JukeboxUpgradeRenderData::isPlaying).orElse(false);
			boolean cachedProjection = BackpackWrapper.fromStack(mainHand).getRenderInfo().getUpgradeRenderData(JukeboxUpgradeRenderData.TYPE)
					.map(JukeboxUpgradeRenderData::isPlaying).orElse(false);
			long finishTime = jukebox.getDiscFinishTime();
			return new ClientPlaybackState(jukebox.isPlaying(), upgrade.getOrDefault(ModCoreDataComponents.IS_PLAYING, false), projection, cachedProjection,
					finishTime > Minecraft.getInstance().level.getGameTime(), !upgrade.has(ModCoreDataComponents.DISC_FINISH_TIME),
					upgrade.getOrDefault(ModCoreDataComponents.DISC_SLOT_ACTIVE, -1) == -1, finishTime, Minecraft.getInstance().level.getGameTime());
		}

		private static boolean matchesClient(ClientPlaybackState state, HeldBackpackJukeboxSoundObserver soundObserver, Baseline baseline, boolean playing) {
			if (!playing) {
				return !state.projectionPlaying();
			}
			return state.wrapperPlaying() && state.componentPlaying() && state.projectionPlaying() && state.finishTimeFuture()
					&& soundObserver.hasReceiptSinceArming() && (baseline == null || soundObserver.receiptCount() > baseline.soundReceiptCount());
		}

		private static Map<String, Boolean> expectedClientState(boolean playing) {
			return playing
					? Map.of("wrapperPlaying", true, "componentPlaying", true, "projectionPlaying", true, "soundReceiptSinceArming", true, "finishTimeFuture",
							true)
					: Map.of("projectionPlaying", false);
		}

		private static boolean discsMatch(JukeboxUpgradeWrapper jukebox, List<ItemStack> expected) {
			List<ItemStack> actual = discs(jukebox);
			if (actual.size() != expected.size()) {
				return false;
			}
			for (int slot = 0; slot < actual.size(); slot++) {
				if (!ItemStack.matches(actual.get(slot), expected.get(slot))) {
					return false;
				}
			}
			return true;
		}

		private static List<ItemStack> discs(JukeboxUpgradeWrapper jukebox) {
			List<ItemStack> discs = new ArrayList<>();
			for (int slot = 0; slot < jukebox.getDiscInventory().getSlots(); slot++) {
				discs.add(jukebox.getDiscInventory().getStackInSlot(slot).copy());
			}
			return List.copyOf(discs);
		}
	}
}
