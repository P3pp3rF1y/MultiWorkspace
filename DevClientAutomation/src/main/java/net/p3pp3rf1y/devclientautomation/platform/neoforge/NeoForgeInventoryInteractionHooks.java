package net.p3pp3rf1y.devclientautomation.platform.neoforge;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.neoforged.neoforge.client.ClientHooks;

public final class NeoForgeInventoryInteractionHooks {
	private NeoForgeInventoryInteractionHooks() {
	}

	public static boolean onScreenMouseClickedPre(Screen screen, double mouseX, double mouseY, int button) {
		return ClientHooks.onScreenMouseClickedPre(screen, new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
	}

	public static boolean onScreenKeyPressedPre(Screen screen, int keyCode, int scanCode, int modifiers) {
		return ClientHooks.onScreenKeyPressedPre(screen, new KeyEvent(keyCode, scanCode, modifiers));
	}
}
