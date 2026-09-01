package net.p3pp3rf1y.devclientautomation.platform.neoforge;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;

public final class NeoForgeInventoryInteractionHooks {
	private NeoForgeInventoryInteractionHooks() {
	}

	public static boolean onScreenMouseClickedPre(Screen screen, double mouseX, double mouseY, int button) {
		return ClientHooks.onScreenMouseClickedPre(screen, mouseX, mouseY, button);
	}

	public static boolean onScreenKeyPressedPre(Screen screen, int keyCode, int scanCode, int modifiers) {
		return ClientHooks.onScreenKeyPressedPre(screen, keyCode, scanCode, modifiers);
	}
}
