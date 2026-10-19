package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertLinkedMenuSettingsAction extends TypedRegressionAction<AssertLinkedMenuSettingsAction.Arguments> {
	public record Arguments(String ref, String baseline) {
	}

	public AssertLinkedMenuSettingsAction() {
		super("assert.linkedMenuSettings", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "baseline"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "baseline", description)));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private final CaptureLinkedMenuRevisionAction.Baseline baseline = context.values().require(arguments.baseline(),
					CaptureLinkedMenuRevisionAction.Baseline.class);
			private CompletableFuture<Void> serverAssertion;
			private CompoundTag expectedSettings;
			private int deadline = -1;

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
							throw new IllegalStateException("Expected linked moving settings menu closed on server");
						}
						expectedSettings = menu.getStorageWrapper().getSettingsHandler().getNbt().copy();
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress progress = await(serverAssertion);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				if (expectedSettings.isEmpty()) {
					return fail("Server linked settings mutation produced no settings");
				}
				if (ClientLinkedStorageContents.getRevision(baseline.groupId()).orElse(-1L) != baseline.revision()) {
					return fail("Linked settings change sent a full inventory snapshot");
				}
				MovingStorageEntityHandle handle = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
				if (!(context.minecraft().player.containerMenu instanceof MovingStorageContainerMenu<?> menu)
						|| menu.getStorageEntity().filter(entity -> entity.getUUID().equals(handle.id())).isEmpty()) {
					return fail("Moving client menu closed before linked settings arrived");
				}
				return expectedSettings.equals(menu.getStorageWrapper().getSettingsHandler().getNbt())
						? RegressionActionProgress.PASSED
						: context.minecraft().player.tickCount >= deadline
								? fail("Client linked menu settings did not synchronize")
								: RegressionActionProgress.WAITING;
			}
		};
	}
}
