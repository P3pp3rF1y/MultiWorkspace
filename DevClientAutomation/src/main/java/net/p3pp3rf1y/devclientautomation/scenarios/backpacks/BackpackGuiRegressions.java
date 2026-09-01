package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public final class BackpackGuiRegressions {
	private BackpackGuiRegressions() {
	}

	public static String runRemoteUpgradeSlotRegression() {
		return AutomationRuntime.runOnClient(() -> {
			if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
				return "{\"ok\":false,\"error\":\"Player does not have a storage menu open\"}";
			}
			if (menu.getNumberOfUpgradeSlots() < 2) {
				return "{\"ok\":false,\"error\":\"Storage menu needs at least two upgrade slots\"}";
			}

			int logicalUpgradeSlot = menu.getFirstUpgradeSlot() + 1;
			menu.setRemoteSlot(logicalUpgradeSlot, menu.getSlot(logicalUpgradeSlot).getItem().copy());
			return "{\"ok\":true,\"slot\":" + logicalUpgradeSlot + "}";
		});
	}
}
