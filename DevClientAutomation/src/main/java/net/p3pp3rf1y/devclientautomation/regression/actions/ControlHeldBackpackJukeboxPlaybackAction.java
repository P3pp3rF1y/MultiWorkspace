package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeTab;

import java.util.List;
import java.util.Map;
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
						.filter(button -> !(button instanceof ToggleButton<?>)).toList();
				int buttonX = tab.getX() + (tab instanceof JukeboxUpgradeTab.Advanced ? control.equals("play") ? 39 : 21 : control.equals("play") ? 21 : 3);
				List<Button> candidates = buttons.stream().filter(button -> button.getX() == buttonX).distinct().toList();
				context.putDiagnostic(
						"heldJukeboxGuiButtons", Map
								.of("tab", tab.getClass().getName(), "tabIdentity", System.identityHashCode(tab), "menuId", menu.containerId, "childCount",
										tab.children().size(), "buttonCount", buttons.size(), "buttons", buttons
												.stream().map(button -> button.getClass().getName() + "@" + System.identityHashCode(button) + " ["
														+ button.getX() + "," + button.getY() + " " + button.getWidth() + "x" + button.getHeight() + "]")
												.toList()));
				if (candidates.size() != 1) {
					return fail("Jukebox " + control + " has " + candidates.size() + " distinct buttons at the production tab position " + buttonX);
				}
				Button button = candidates.get(0);
				double clickX = button.getX() + button.getWidth() / 2.0;
				double clickY = button.getY() + button.getHeight() / 2.0;
				GuiEventListener screenHit = screen.getChildAt(clickX, clickY).orElse(null);
				GuiEventListener controlHit = screen.getUpgradeSettingsControl().getChildAt(clickX, clickY).orElse(null);
				GuiEventListener tabHit = tab.getChildAt(clickX, clickY).orElse(null);
				context.putDiagnostic("heldJukeboxGuiHit",
						Map.of("point", clickX + "," + clickY, "screenHit", widget(screenHit), "settingsHit", widget(controlHit), "tabHit", widget(tabHit),
								"selected", widget(button), "clientPlayingBefore", jukebox.getUpgradeWrapper().isPlaying(), "menuId", menu.containerId,
								"clientPlayerLevelIsClient", context.minecraft().player.level().isClientSide(), "jukeboxPlayerLevelIsClient",
								jukebox.getPlayer().level().isClientSide(), "jukeboxUpgradeContainerId", jukebox.getUpgradeContainerId()));
				if (screenHit != screen.getUpgradeSettingsControl() || controlHit != tab || tabHit != button) {
					return fail("Jukebox " + control + " virtual click would miss its production tab button");
				}
				boolean handled = VirtualInput.click(screen, clickX, clickY, InputConstants.MOUSE_BUTTON_LEFT, 0);
				context.putDiagnostic("heldJukeboxGuiClick", Map.of("handledByScreen", handled, "clientPlayingAfter", jukebox.getUpgradeWrapper().isPlaying(),
						"menuIdAfter", context.minecraft().player.containerMenu.containerId, "sameScreen", context.minecraft().screen == screen));
				if (!handled) {
					return fail("Jukebox " + control + " button did not handle virtual click");
				}
				clicked = true;
				return RegressionActionProgress.PASSED;
			}
		};
	}

	private static String widget(GuiEventListener widget) {
		return widget == null ? "none" : widget.getClass().getName() + "@" + System.identityHashCode(widget);
	}
}
