package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Selects a hotbar slot through its configured key binding and awaits client/server agreement. */
public final class SelectHotbarSlotAction extends TypedRegressionAction<SelectHotbarSlotAction.Arguments> {
	public record Arguments(int slot) {
	}

	public SelectHotbarSlotAction() {
		super("player.selectHotbarSlot", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot"), description);
		int slot = requireNonNegativeInt(args, "slot", description);
		if (slot > 8) {
			throw new IllegalArgumentException("Argument slot on " + description + " must be between 0 and 8");
		}
		return new Arguments(slot);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int deadline = -1;
			private boolean dispatched;
			private volatile boolean serverSelected;
			private volatile int serverSlot = -1;
			private CompletableFuture<Void> server;

			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = context.minecraft();
				if (deadline < 0) {
					deadline = minecraft.player.tickCount + 100;
				}
				if (!dispatched) {
					VirtualInput.pressKey(minecraft.options.keyHotbarSlots[arguments.slot()]);
					dispatched = true;
				}
				if (minecraft.player.getInventory().getSelectedSlot() == arguments.slot()) {
					if (server == null) {
						server = context.onServer(player -> {
							serverSlot = player.getInventory().getSelectedSlot();
							serverSelected = serverSlot == arguments.slot();
						});
						return RegressionActionProgress.WAITING;
					}
					RegressionActionProgress result = await(server);
					if (result != RegressionActionProgress.PASSED) {
						return result;
					}
					if (serverSelected) {
						return RegressionActionProgress.PASSED;
					}
					server = null;
				}
				if (minecraft.player.tickCount >= deadline) {
					context.putDiagnostic("selectedHotbarSlot.client", minecraft.player.getInventory().getSelectedSlot());
					context.putDiagnostic("selectedHotbarSlot.server", serverSlot < 0 ? "not observed" : serverSlot);
					return fail("Hotbar slot did not synchronize to " + arguments.slot() + " within 100 ticks");
				}
				return RegressionActionProgress.WAITING;
			}
		};
	}
}
