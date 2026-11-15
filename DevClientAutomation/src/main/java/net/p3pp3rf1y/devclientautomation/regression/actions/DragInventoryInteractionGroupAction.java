package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.neoforged.neoforge.client.ClientHooks;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.devclientautomation.regression.RegressionJsonNumbers;
import net.p3pp3rf1y.devclientautomation.regression.actions.InventoryInteractionLayoutProbe.Point;
import net.p3pp3rf1y.devclientautomation.regression.actions.InventoryInteractionLayoutProbe.Snapshot;

import java.util.Set;

public final class DragInventoryInteractionGroupAction extends TypedRegressionAction<DragInventoryInteractionGroupAction.Arguments> {
	public record Arguments(String group, int x, int y) {
	}

	public DragInventoryInteractionGroupAction() {
		super("input.dragInventoryInteractionGroup", "1", Set.of("minecraft:client", "sophisticatedinventoryinteractions"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("group", "x", "y"), description);
		String group = requireString(args, "group", description);
		if (!Set.of("container", "player").contains(group)) {
			throw new IllegalArgumentException("Unknown layout group on " + description);
		}
		return new Arguments(group, RegressionJsonNumbers.integer(args.get("x")), RegressionJsonNumbers.integer(args.get("y")));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().screen instanceof AbstractContainerScreen<?> screen)) {
					return fail("A container screen is required");
				}
				Snapshot snapshot = InventoryInteractionLayoutProbe.capture(screen);
				if (!snapshot.editing()) {
					return fail("Layout editor is not active");
				}
				Point point = snapshot.widgets().get(arguments.group().equals("container") ? "container-sort" : "deposit");
				if (point == null && arguments.group().equals("player")) {
					point = snapshot.widgets().get("player-sort");
				}
				if (point == null) {
					return fail("Layout group has no visible controls");
				}
				double x = screen.getGuiLeft() + point.x() + 6;
				double y = screen.getGuiTop() + point.y() + 6;
				VirtualInput.move(x, y);
				boolean pressed = ClientHooks.onScreenMouseClickedPre(screen, new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
				boolean dragged = ClientHooks.onScreenMouseDragPre(screen,
						new MouseButtonEvent(x + arguments.x(), y + arguments.y(), new MouseButtonInfo(0, 0)), arguments.x(), arguments.y());
				boolean released = ClientHooks.onScreenMouseReleasedPre(screen,
						new MouseButtonEvent(x + arguments.x(), y + arguments.y(), new MouseButtonInfo(0, 0)));
				VirtualInput.move(x + arguments.x(), y + arguments.y());
				return pressed && dragged && released ? RegressionActionProgress.PASSED : fail("Editor did not consume drag input");
			}
		};
	}
}
