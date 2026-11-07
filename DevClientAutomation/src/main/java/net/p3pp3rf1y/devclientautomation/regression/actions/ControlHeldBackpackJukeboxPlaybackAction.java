package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeTab;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ControlHeldBackpackJukeboxPlaybackAction extends TypedRegressionAction<String> {
	public ControlHeldBackpackJukeboxPlaybackAction() {
		super("menu.heldBackpackJukeboxPlayback", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected String parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("control"), description);
		String control = requireString(args, "control", description);
		if (!Set.of("play", "stop").contains(control)) {
			throw new IllegalArgumentException("Argument control on " + description + " must be play or stop");
		}
		return control;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, String control) {
		return new TickRegressionActionExecution(context) {
			private boolean clicked;

			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().player.containerMenu instanceof BackpackContainer menu)) {
					return fail("Expected held Backpack menu is not open for jukebox control");
				}
				JukeboxUpgradeContainer jukebox = menu.getUpgradeContainers().values().stream().filter(JukeboxUpgradeContainer.class::isInstance)
						.map(JukeboxUpgradeContainer.class::cast).findFirst()
						.orElseThrow(() -> new IllegalStateException("Open Backpack menu has no jukebox upgrade tab"));
				if (!(context.minecraft().screen instanceof StorageScreenBase<?> screen)) {
					return fail("Backpack screen is not active for jukebox control");
				}
				JukeboxUpgradeTab tab = screen.getUpgradeSettingsControl().children().stream().filter(JukeboxUpgradeTab.class::isInstance)
						.map(JukeboxUpgradeTab.class::cast).findFirst()
						.orElseThrow(() -> new IllegalStateException("Backpack screen has no jukebox upgrade tab"));
				if (!jukebox.isOpen()) {
					VirtualInput.click(screen, tab.getX() + 8, tab.getTopY() + 12, InputConstants.MOUSE_BUTTON_LEFT, 0);
					return RegressionActionProgress.WAITING;
				}
				if (clicked) {
					return RegressionActionProgress.PASSED;
				}
				if (control.equals("play")) {
					context.heldBackpackJukeboxSoundObserver().arm();
				}
				List<Button> buttons = tab.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
						.filter(button -> !(button instanceof ToggleButton<?>)).sorted(Comparator.comparingInt(Button::getX)).toList();
				int buttonIndex = control.equals("play") ? buttons.size() == 2 ? 1 : 2 : buttons.size() == 2 ? 0 : 1;
				if (buttonIndex < 0 || buttonIndex >= buttons.size()) {
					return fail("Jukebox " + control + " button is not available in the active tab");
				}
				Button button = buttons.get(buttonIndex);
				if (!VirtualInput.click(screen, button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0,
						InputConstants.MOUSE_BUTTON_LEFT, 0)) {
					return fail("Jukebox " + control + " button did not handle virtual click");
				}
				clicked = true;
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
