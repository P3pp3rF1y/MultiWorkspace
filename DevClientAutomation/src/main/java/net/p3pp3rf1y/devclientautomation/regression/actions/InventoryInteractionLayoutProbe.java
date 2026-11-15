package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only observation of the injected widgets; all mutations use production input. */
final class InventoryInteractionLayoutProbe {
	private InventoryInteractionLayoutProbe() {
	}

	record Point(int x, int y) {
	}
	record Snapshot(String screenClass, Map<String, Point> widgets, boolean editing) {
	}

	static Snapshot capture(AbstractContainerScreen<?> screen) {
		try {
			Class<?> handler = Class.forName("net.p3pp3rf1y.sophisticatedinventoryinteractions.client.ClientEventHandler");
			Object injector = field(handler, null, "SCREEN_INTERACTION_INJECTOR");
			Map<String, Point> widgets = new LinkedHashMap<>();
			Object state = state(injector, "states", screen);
			add(widgets, screen, state, "sortContainerButton", "container-sort");
			add(widgets, screen, state, "sortByButton", "sort-by");
			add(widgets, screen, state, "searchBox", "search");
			add(widgets, screen, state, "noResultsLabel", "no-results");
			add(widgets, screen, state, "transferToContainerButton", "deposit");
			add(widgets, screen, state, "transferToPlayerButton", "withdraw");
			add(widgets, screen, state(injector, "playerOnlyStates", screen), "sortPlayerButton", "player-sort");
			add(widgets, screen, state(injector, "sophisticatedStates", screen), "sortPlayerButton", "player-sort");
			if (screen instanceof StorageScreenBase<?> nativeScreen) {
				nativeScreen.getTransferToInventoryButtonPosition().ifPresent(position -> {
					widgets.put("withdraw", new Point(position.x() - screen.getGuiLeft(), position.y() - screen.getGuiTop()));
					widgets.put("deposit", new Point(position.x() - 12 - screen.getGuiLeft(), position.y() - screen.getGuiTop()));
				});
			}
			Object editor = field(injector.getClass(), injector, "layoutEditor");
			boolean editing = field(editor.getClass(), editor, "activeScreen") == screen;
			return new Snapshot(screen.getClass().getName(), Map.copyOf(widgets), editing);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot observe SII layout", e);
		}
	}

	private static Object state(Object injector, String map, AbstractContainerScreen<?> screen) throws ReflectiveOperationException {
		return ((Map<?, ?>) field(injector.getClass(), injector, map)).get(screen);
	}

	private static void add(Map<String, Point> widgets, AbstractContainerScreen<?> screen, @Nullable Object state, String field, String name)
			throws ReflectiveOperationException {
		if (state != null && field(state.getClass(), state, field) instanceof WidgetBase widget) {
			widgets.put(name, new Point(widget.getX() - screen.getGuiLeft(), widget.getY() - screen.getGuiTop()));
		}
	}

	private static Object field(Class<?> type, @Nullable Object instance, String name) throws ReflectiveOperationException {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(instance);
	}
}
