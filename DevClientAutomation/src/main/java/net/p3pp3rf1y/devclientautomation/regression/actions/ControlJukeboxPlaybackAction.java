package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;

import java.util.Set;

public final class ControlJukeboxPlaybackAction extends TypedRegressionAction<ControlJukeboxPlaybackAction.Arguments> {
	public record Arguments(String ref, String control) {
	}

	public ControlJukeboxPlaybackAction() {
		super("menu.jukeboxPlayback", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "control"), description);
		String control = requireString(args, "control", description);
		if (!Set.of("play", "stop").contains(control)) {
			throw new IllegalArgumentException("Argument control on " + description + " must be play or stop");
		}
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)), control);
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				MovingStorageEntityHandle handle = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
				if (!(context.minecraft().player.containerMenu instanceof MovingStorageContainerMenu<?> menu)
						|| menu.getStorageEntity().filter(entity -> entity.getUUID().equals(handle.id())).isEmpty()) {
					return fail("Expected moving menu is not open for jukebox control");
				}
				JukeboxUpgradeContainer jukebox = menu.getUpgradeContainers().values().stream().filter(JukeboxUpgradeContainer.class::isInstance)
						.map(JukeboxUpgradeContainer.class::cast).findFirst()
						.orElseThrow(() -> new IllegalStateException("Open moving menu has no jukebox upgrade tab"));
				if (arguments.control().equals("play")) {
					jukebox.play();
				} else {
					jukebox.stop();
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
