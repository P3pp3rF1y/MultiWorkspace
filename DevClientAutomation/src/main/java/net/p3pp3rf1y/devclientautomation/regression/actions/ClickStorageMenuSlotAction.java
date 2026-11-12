package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.Map;
import java.util.Set;

public final class ClickStorageMenuSlotAction extends TypedRegressionAction<ClickStorageMenuSlotAction.Arguments> {
	private static final String AREA_ARGUMENT = "area";
	private static final String SLOT_ARGUMENT = "slot";
	private static final String CLICK_ARGUMENT = "click";
	private static final String STORAGE_AREA = "storage";
	private static final String PLAYER_AREA = "player";
	private static final String UPGRADE_AREA = "upgrade";
	private static final String EXTRA_AREA = "extra";
	private static final String QUICK_MOVE_CLICK = "quickMove";

	public record Arguments(String area, int slot, String click) {
	}

	public ClickStorageMenuSlotAction() {
		super("menu.clickStorageSlot", "1", Set.of(CLIENT_CAPABILITY, "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(AREA_ARGUMENT, SLOT_ARGUMENT, CLICK_ARGUMENT), description);
		String area = requireString(args, AREA_ARGUMENT, description);
		String click = requireString(args, CLICK_ARGUMENT, description);
		if (!Set.of(STORAGE_AREA, PLAYER_AREA, UPGRADE_AREA, EXTRA_AREA).contains(area) || !Set.of("pickup", QUICK_MOVE_CLICK).contains(click)) {
			throw new IllegalArgumentException("Unsupported storage menu area or click on " + description);
		}
		int slot = optionalNonNegativeInt(args, SLOT_ARGUMENT, 0, description);
		if (area.equals(PLAYER_AREA) && slot > 35) {
			throw new IllegalArgumentException("Player slot must be from 0 through 35 on " + description);
		}
		return new Arguments(area, slot, click);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().screen instanceof StorageScreenBase<?> screen)
						|| !(context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu) || screen.getMenu() != menu) {
					return fail("Storage menu is not active");
				}
				int slot = switch (arguments.area()) {
					case PLAYER_AREA -> menu.getNumberOfStorageInventorySlots() + (arguments.slot() < 9 ? 27 + arguments.slot() : arguments.slot() - 9);
					case UPGRADE_AREA -> menu.getInventorySlotsSize() + arguments.slot();
					case EXTRA_AREA -> menu.getNumberOfStorageInventorySlots() + 36 + arguments.slot();
					default -> arguments.slot();
				};
				if (arguments.area().equals(UPGRADE_AREA) && arguments.slot() >= menu.getNumberOfUpgradeSlots()
						|| arguments.area().equals(STORAGE_AREA) && arguments.slot() >= menu.getNumberOfStorageInventorySlots()
						|| arguments.area().equals(EXTRA_AREA) && arguments.slot() >= menu.getExtraSlots().size()) {
					return fail("Storage menu has no requested slot");
				}
				String slotBefore = menu.getSlot(slot).getItem().toString();
				String cursorBefore = menu.getCarried().toString();
				Slot target = menu.getSlot(slot);
				VirtualInput.moveToSlot(screen, slot);
				if (!VirtualInput.click(screen, screen.getGuiLeft() + target.x + 8, screen.getGuiTop() + target.y + 8, 0,
						arguments.click().equals(QUICK_MOVE_CLICK) ? 1 : 0)) {
					return fail("Storage slot click was not handled by the screen");
				}
				observe(Map.of("menuId", menu.containerId, SLOT_ARGUMENT, slot, AREA_ARGUMENT, arguments.area(), "inventorySlot", arguments.slot(),
						CLICK_ARGUMENT, arguments.click(), "slotBefore", slotBefore, "cursorBefore", cursorBefore),
						Map.of("slotAfter", menu.getSlot(slot).getItem().toString(), "cursorAfter", menu.getCarried().toString()));
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
