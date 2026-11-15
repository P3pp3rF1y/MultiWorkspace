package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.p3pp3rf1y.devclientautomation.regression.RegressionJsonNumbers;
import net.p3pp3rf1y.devclientautomation.regression.actions.InventoryInteractionLayoutProbe.Point;
import net.p3pp3rf1y.devclientautomation.regression.actions.InventoryInteractionLayoutProbe.Snapshot;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public final class AssertInventoryInteractionLayoutAction extends TypedRegressionAction<AssertInventoryInteractionLayoutAction.Arguments> {
	public record Arguments(boolean container, boolean transfers, boolean playerSort, boolean editing, @Nullable String ref, int containerX, int containerY,
			int playerX, int playerY) {
	}

	public AssertInventoryInteractionLayoutAction() {
		super("assert.inventoryInteractionLayout", "1", Set.of("minecraft:client", "sophisticatedinventoryinteractions"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("container", "transfers", "playerSort", "editing", "ref", "containerX", "containerY", "playerX", "playerY"), description);
		return new Arguments(optionalBoolean(args, "container", false, description), optionalBoolean(args, "transfers", false, description),
				optionalBoolean(args, "playerSort", false, description), optionalBoolean(args, "editing", false, description),
				args.has("ref") ? RegressionValueStore.requireName(requireString(args, "ref", description)) : null,
				args.has("containerX") ? RegressionJsonNumbers.integer(args.get("containerX")) : 0,
				args.has("containerY") ? RegressionJsonNumbers.integer(args.get("containerY")) : 0,
				args.has("playerX") ? RegressionJsonNumbers.integer(args.get("playerX")) : 0,
				args.has("playerY") ? RegressionJsonNumbers.integer(args.get("playerY")) : 0);
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.ref() == null ? RegressionValueContract.EMPTY : RegressionValueContract.consuming(arguments.ref(), Snapshot.class.getName());
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof AbstractContainerScreen<?> screen)) {
					return fail("A container screen is required");
				}
				Snapshot actual = InventoryInteractionLayoutProbe.capture(screen);
				context.putDiagnostic("inventoryInteractionLayout", actual);
				if (actual.editing() != arguments.editing() || actual.widgets().containsKey("container-sort") != arguments.container()
						|| actual.widgets().containsKey("deposit") != arguments.transfers() || actual.widgets().containsKey("withdraw") != arguments.transfers()
						|| actual.widgets().containsKey("player-sort") != arguments.playerSort()) {
					return fail("Unexpected visibility or editor state: " + actual);
				}
				if (arguments.ref() != null) {
					Snapshot before = context.values().require(arguments.ref(), Snapshot.class);
					if (!before.screenClass().equals(actual.screenClass()) || !before.widgets().keySet().equals(actual.widgets().keySet())) {
						return fail("Layout identity/widgets differ from baseline");
					}
					for (Map.Entry<String, Point> widget : before.widgets().entrySet()) {
						boolean player = Set.of("player-sort", "deposit", "withdraw").contains(widget.getKey());
						boolean container = !player && !widget.getKey().equals("no-results");
						int deltaX = player ? arguments.playerX() : container ? arguments.containerX() : 0;
						int deltaY = player ? arguments.playerY() : container ? arguments.containerY() : 0;
						Point expected = new Point(widget.getValue().x() + deltaX, widget.getValue().y() + deltaY);
						if (!expected.equals(actual.widgets().get(widget.getKey()))) {
							return fail(widget.getKey() + " expected " + expected + " but was " + actual.widgets().get(widget.getKey()));
						}
					}
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
