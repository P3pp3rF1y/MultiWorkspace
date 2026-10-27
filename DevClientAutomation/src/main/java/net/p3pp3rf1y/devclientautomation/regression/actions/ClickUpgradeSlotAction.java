package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import java.util.Map;
import java.util.Set;

public final class ClickUpgradeSlotAction extends TypedRegressionAction<ClickUpgradeSlotAction.Arguments> {
	public record Arguments(int upgrade, int slot, String click, int button) {
	}

	public ClickUpgradeSlotAction() {
		super("menu.clickUpgradeSlot", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade", "slot", "click", "button"), description);
		String click = optionalString(args, "click", "pickup", description);
		int button = optionalNonNegativeInt(args, "button", 0, description);
		if (!Set.of("pickup", "quickMove").contains(click) || button > 1) {
			throw new IllegalArgumentException("Unsupported upgrade slot click or button on " + description);
		}
		return new Arguments(optionalNonNegativeInt(args, "upgrade", 0, description), optionalNonNegativeInt(args, "slot", 0, description), click, button);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().gui.screen() instanceof StorageScreenBase<?> screen)
						|| !(context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu) || screen.getMenu() != menu) {
					return fail("Storage menu is not active for upgrade slot click");
				}
				UpgradeContainerBase<?, ?> upgrade = menu.getUpgradeContainers().get(arguments.upgrade());
				if (upgrade == null || arguments.slot() >= upgrade.getSlots().size()) {
					return fail("Menu has no upgrade " + arguments.upgrade() + " slot " + arguments.slot());
				}
				Slot slot = upgrade.getSlots().get(arguments.slot());
				if (!upgrade.isOpen()) {
					return fail("Upgrade tab is not open for slot click");
				}
				String before = slot.getItem().toString();
				VirtualInput.moveToSlot(screen, slot.index);
				if (!VirtualInput.click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8, arguments.button(),
						arguments.click().equals("quickMove") ? 1 : 0)) {
					return fail("Upgrade slot click was not handled by the storage screen");
				}
				observe(Map.of("upgrade", arguments.upgrade(), "slot", arguments.slot(), "menuSlot", slot.index, "before", before, "click", arguments.click(),
						"button", arguments.button()), Map.of("after", slot.getItem().toString(), "cursor", menu.getCarried().toString()));
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
