package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ClickPlayerInventorySlotAction extends TypedRegressionAction<ClickPlayerInventorySlotAction.Arguments> {
	public record Arguments(int slot, int button, String area, boolean awaitServer, int timeoutTicks) {
	}

	public ClickPlayerInventorySlotAction() {
		super("menu.clickPlayerInventorySlot", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "button", "area", "awaitServer", "timeoutTicks"), description);
		String area = optionalString(args, "area", "inventory", description);
		int slot = optionalNonNegativeInt(args, "slot", 0, description);
		int button = optionalNonNegativeInt(args, "button", 0, description);
		int maxSlot = switch (area) {
			case "inventory" -> 35;
			case "crafting" -> 3;
			case "result" -> 0;
			default -> throw new IllegalArgumentException("Unknown inventory area on " + description);
		};
		if (slot > maxSlot || button > 1) {
			throw new IllegalArgumentException("Invalid slot or mouse button on " + description);
		}
		return new Arguments(slot, button, area, optionalBoolean(args, "awaitServer", false, description),
				optionalPositiveInt(args, "timeoutTicks", 40, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private boolean clicked;
			private int menuSlot;
			private ItemStack predictedCarried;
			private ItemStack predictedSlot;
			private ItemStack beforeCarried;
			private ItemStack beforeSlot;
			private ItemStack observedCarried;
			private ItemStack observedSlot;
			private CompletableFuture<Void> confirmation;
			private int deadline;

			@Override
			public RegressionActionProgress tick() {
				if (clicked) {
					if (!(context.minecraft().gui.screen() instanceof InventoryScreen screen) || context.minecraft().player == null) {
						return fail("Player inventory screen closed while awaiting server click");
					}
					if (confirmation == null) {
						confirmation = context.onServer(player -> {
							observedCarried = player.inventoryMenu.getCarried().copy();
							observedSlot = player.inventoryMenu.getSlot(menuSlot).getItem().copy();
						});
						return RegressionActionProgress.WAITING;
					}
					if (!confirmation.isDone()) {
						if (context.minecraft().player.tickCount >= deadline) {
							return failAssertion(
									"Server inventory click " + arguments.area() + "/" + arguments.slot() + " timed out waiting for server observation after "
											+ arguments.timeoutTicks() + " ticks; predicted cursor=" + predictedCarried + ", slot=" + predictedSlot);
						}
						return RegressionActionProgress.WAITING;
					}
					RegressionActionProgress completed = await(confirmation);
					if (completed != RegressionActionProgress.PASSED) {
						return completed;
					}
					ItemStack clientCarried = screen.getMenu().getCarried();
					ItemStack clientSlot = screen.getMenu().getSlot(menuSlot).getItem();
					boolean carriedChanged = !ItemStack.matches(beforeCarried, predictedCarried);
					boolean slotChanged = !ItemStack.matches(beforeSlot, predictedSlot);
					boolean predictedIdentity = (!carriedChanged || predictedCarried.isEmpty()
							|| ItemStack.isSameItem(predictedCarried, observedCarried) && predictedCarried.getCount() == observedCarried.getCount())
							&& (!slotChanged || predictedSlot.isEmpty()
									|| ItemStack.isSameItem(predictedSlot, observedSlot) && predictedSlot.getCount() == observedSlot.getCount());
					if (predictedIdentity && ItemStack.matches(clientCarried, observedCarried) && ItemStack.matches(clientSlot, observedSlot)) {
						return RegressionActionProgress.PASSED;
					}
					if (context.minecraft().player.tickCount >= deadline) {
						return failAssertion("Server inventory click " + arguments.area() + "/" + arguments.slot() + " did not converge in "
								+ arguments.timeoutTicks() + " ticks; predicted cursor=" + predictedCarried + ", slot=" + predictedSlot + "; client cursor="
								+ clientCarried + ", slot=" + clientSlot + "; server cursor=" + observedCarried + ", slot=" + observedSlot);
					}
					confirmation = null;
					return RegressionActionProgress.WAITING;
				}
				if (!(context.minecraft().gui.screen() instanceof InventoryScreen screen)) {
					return fail("Player inventory screen is not active");
				}
				Slot slot = arguments.area().equals("inventory")
						? screen.getMenu().slots.stream()
								.filter(candidate -> candidate.container == context.minecraft().player.getInventory()
										&& candidate.getSlotIndex() == arguments.slot())
								.findFirst().orElseThrow()
						: screen.getMenu().getSlot(arguments.area().equals("result") ? 0 : arguments.slot() + 1);
				if (arguments.awaitServer()) {
					menuSlot = slot.index;
					beforeCarried = screen.getMenu().getCarried().copy();
					beforeSlot = slot.getItem().copy();
				}
				double x = screen.getLeftPos() + slot.x + 8;
				double y = screen.getTopPos() + slot.y + 8;
				screen.mouseMoved(x, y);
				MouseButtonEvent event = new MouseButtonEvent(x, y,
						new MouseButtonInfo(arguments.button() == 0 ? InputConstants.MOUSE_BUTTON_LEFT : InputConstants.MOUSE_BUTTON_RIGHT, 0));
				screen.mouseClicked(event, false);
				screen.mouseReleased(event);
				if (arguments.awaitServer()) {
					predictedCarried = screen.getMenu().getCarried().copy();
					predictedSlot = slot.getItem().copy();
					if (ItemStack.matches(beforeCarried, predictedCarried) && ItemStack.matches(beforeSlot, predictedSlot)) {
						return failAssertion("Inventory click produced no client change at " + arguments.area() + "/" + arguments.slot());
					}
					clicked = true;
					deadline = context.minecraft().player.tickCount + arguments.timeoutTicks();
					return RegressionActionProgress.WAITING;
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
