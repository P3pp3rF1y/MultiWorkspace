package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Set;

public final class CaptureInventoryInteractionLayoutAction extends TypedRegressionAction<CaptureInventoryInteractionLayoutAction.Arguments> {
	public record Arguments(String saveAs) {
	}

	public CaptureInventoryInteractionLayoutAction() {
		super("menu.captureInventoryInteractionLayout", "1", Set.of("minecraft:client", "sophisticatedinventoryinteractions"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("saveAs"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "saveAs", description)));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.producing(arguments.saveAs(), InventoryInteractionLayoutProbe.Snapshot.class.getName());
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof AbstractContainerScreen<?> screen)) {
					return fail("A container screen is required");
				}
				context.values().put(arguments.saveAs(), InventoryInteractionLayoutProbe.Snapshot.class, InventoryInteractionLayoutProbe.capture(screen));
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
