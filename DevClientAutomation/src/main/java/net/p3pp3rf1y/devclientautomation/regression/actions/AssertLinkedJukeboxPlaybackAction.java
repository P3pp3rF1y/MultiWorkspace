package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertLinkedJukeboxPlaybackAction extends TypedRegressionAction<AssertLinkedJukeboxPlaybackAction.Arguments> {
	public record Arguments(String ref, String baseline, boolean playing) {
	}

	public AssertLinkedJukeboxPlaybackAction() {
		super("assert.linkedJukeboxPlayback", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "baseline", "playing"), description);
		if (!args.has("playing") || !args.get("playing").isJsonPrimitive() || !args.get("playing").getAsJsonPrimitive().isBoolean()) {
			throw new IllegalArgumentException("Argument playing on " + description + " must be boolean");
		}
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "baseline", description)), args.get("playing").getAsBoolean());
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE)
				.and(RegressionValueContract.consuming(arguments.baseline(), RegressionValueTypes.MENU_BASELINE));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private final CaptureLinkedMenuRevisionAction.Baseline baseline;
		private CompletableFuture<Void> serverAssertion;
		private boolean serverMatched;
		private int deadline = -1;
		private long firstRemaining = -1;
		private int firstTick;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
			baseline = context.values().require(arguments.baseline(), CaptureLinkedMenuRevisionAction.Baseline.class);
		}

		@Override
		public RegressionActionProgress tick() {
			if (deadline < 0) {
				deadline = context.minecraft().player.tickCount + 100;
			}
			if (serverAssertion == null) {
				serverAssertion = context.onServer(player -> {
					MovingStorageEntityHandle handle = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
					if (!(player.containerMenu instanceof MovingStorageContainerMenu<?> menu)
							|| menu.getStorageEntity().filter(entity -> entity.getUUID().equals(handle.id())).isEmpty()) {
						throw new IllegalStateException("Expected moving jukebox menu closed on server");
					}
					JukeboxUpgradeContainer jukebox = findJukebox(menu);
					serverMatched = LinkedStorageGroupsSavedData.get(player.level()).manager().getRevision(baseline.groupId()) > baseline.revision()
							&& jukebox.getUpgradeWrapper().isPlaying() == arguments.playing()
							&& (!arguments.playing() || jukebox.getDiscFinishTime() > player.level().getGameTime());
				});
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress progress = await(serverAssertion);
			if (progress != RegressionActionProgress.PASSED) {
				return progress;
			}
			if (!serverMatched) {
				serverAssertion = null;
				return context.minecraft().player.tickCount >= deadline
						? fail("Server jukebox playback state or linked revision did not update")
						: RegressionActionProgress.WAITING;
			}
			if (ClientLinkedStorageContents.getRevision(baseline.groupId()).orElse(-1L) != baseline.revision()) {
				return fail("Jukebox state arrived through a full linked group snapshot");
			}
			MovingStorageEntityHandle handle = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
			if (!(context.minecraft().player.containerMenu instanceof MovingStorageContainerMenu<?> menu)
					|| menu.getStorageEntity().filter(entity -> entity.getUUID().equals(handle.id())).isEmpty()) {
				return fail("Moving jukebox client menu closed before synchronization");
			}
			JukeboxUpgradeContainer jukebox = findJukebox(menu);
			boolean playing = jukebox.getUpgradeWrapper().isPlaying();
			boolean stackPlaying = menu.getSlot(menu.getFirstUpgradeSlot()).getItem().getOrDefault(ModCoreDataComponents.IS_PLAYING, false);
			if (playing != arguments.playing() || stackPlaying != arguments.playing()) {
				boolean handlerPlaying = menu.getStorageWrapper().getUpgradeHandler().getTypeWrappers(JukeboxUpgradeItem.TYPE).stream().findFirst()
						.orElseThrow().isPlaying();
				return context.minecraft().player.tickCount >= deadline
						? fail("Jukebox client playback state mismatched: wrapper=" + playing + ", upgrade slot=" + stackPlaying + ", handler=" + handlerPlaying
								+ ", expected=" + arguments.playing())
						: RegressionActionProgress.WAITING;
			}
			if (!arguments.playing()) {
				return RegressionActionProgress.PASSED;
			}
			long remaining = jukebox.getDiscFinishTime() - context.minecraft().level.getGameTime();
			if (remaining <= 0) {
				return fail("Jukebox client upgrade has no positive disc finish time");
			}
			if (firstRemaining < 0) {
				firstRemaining = remaining;
				firstTick = context.minecraft().player.tickCount;
			}
			return context.minecraft().player.tickCount >= firstTick + 5 && remaining < firstRemaining
					? RegressionActionProgress.PASSED
					: context.minecraft().player.tickCount >= deadline
							? fail("Jukebox client finish-time progress did not advance")
							: RegressionActionProgress.WAITING;
		}

		private static JukeboxUpgradeContainer findJukebox(MovingStorageContainerMenu<?> menu) {
			return menu.getUpgradeContainers().values().stream().filter(JukeboxUpgradeContainer.class::isInstance).map(JukeboxUpgradeContainer.class::cast)
					.findFirst().orElseThrow(() -> new IllegalStateException("Moving menu has no jukebox upgrade container"));
		}
	}
}
