package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.Map;
import java.util.Set;

public final class ClickStorageMenuSlotAction extends TypedRegressionAction<ClickStorageMenuSlotAction.Arguments> {
	public record Arguments(String area, int slot, String click) {
	}

	public ClickStorageMenuSlotAction() {
		super("menu.clickStorageSlot", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("area", "slot", "click"), description);
		String area = requireString(args, "area", description);
		String click = requireString(args, "click", description);
		if (!Set.of("storage", "player", "upgrade").contains(area) || !Set.of("pickup", "quickMove").contains(click)) {
			throw new IllegalArgumentException("Unsupported storage menu area or click on " + description);
		}
		int slot = optionalNonNegativeInt(args, "slot", 0, description);
		if (area.equals("player") && slot > 35) {
			throw new IllegalArgumentException("Player slot must be from 0 through 35 on " + description);
		}
		return new Arguments(area, slot, click);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof StorageScreenBase<?> screen)
						|| !(context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu) || screen.getMenu() != menu) {
					return fail("Storage menu is not active");
				}
				int slot = switch (arguments.area()) {
					case "player" -> menu.getNumberOfStorageInventorySlots() + (arguments.slot() < 9 ? 27 + arguments.slot() : arguments.slot() - 9);
					case "upgrade" -> menu.getInventorySlotsSize() + arguments.slot();
					default -> arguments.slot();
				};
				if (arguments.area().equals("upgrade") && arguments.slot() >= menu.getNumberOfUpgradeSlots()
						|| arguments.area().equals("storage") && arguments.slot() >= menu.getNumberOfStorageInventorySlots()) {
					return fail("Storage menu has no requested slot");
				}
				String slotBefore = menu.getSlot(slot).getItem().toString();
				String cursorBefore = menu.getCarried().toString();
				Slot target = menu.getSlot(slot);
				VirtualInput.moveToSlot(screen, slot);
				if (!VirtualInput.click(screen, screen.getLeftPos() + target.x + 8, screen.getTopPos() + target.y + 8, InputConstants.MOUSE_BUTTON_LEFT,
						arguments.click().equals("quickMove") ? InputConstants.MOD_SHIFT : 0)) {
					return fail("Storage slot click was not handled by the screen");
				}
				observe(Map.of("menuId", menu.containerId, "slot", slot, "area", arguments.area(), "inventorySlot", arguments.slot(), "click",
						arguments.click(), "slotBefore", slotBefore, "cursorBefore", cursorBefore),
						Map.of("slotAfter", menu.getSlot(slot).getItem().toString(), "cursorAfter", menu.getCarried().toString()));
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
