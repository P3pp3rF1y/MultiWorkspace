package net.p3pp3rf1y.devclientautomation.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.ClientHooks;
import net.p3pp3rf1y.devclientautomation.mixin.ContainerScreenAccess;
import net.p3pp3rf1y.devclientautomation.mixin.MouseHandlerAccess;

/** Client-thread input through production screen handlers, independent of window focus. */
public final class VirtualInput {
	private VirtualInput() {
	}

	public static void move(double x, double y) {
		Minecraft minecraft = Minecraft.getInstance();
		MouseHandlerAccess mouse = (MouseHandlerAccess) minecraft.mouseHandler;
		mouse.setVirtualX(VirtualInputState.toWindow(x, minecraft.getWindow().getScreenWidth(), minecraft.getWindow().getGuiScaledWidth()));
		mouse.setVirtualY(VirtualInputState.toWindow(y, minecraft.getWindow().getScreenHeight(), minecraft.getWindow().getGuiScaledHeight()));
		if (minecraft.screen != null) {
			minecraft.screen.mouseMoved(x, y);
			if (minecraft.screen instanceof AbstractContainerScreen<?> container) {
				ContainerScreenAccess access = (ContainerScreenAccess) container;
				access.setVirtualHoveredSlot(access.findVirtualSlot(x, y));
			}
		}
	}

	public static void clearHover() {
		move(-100, -100);
	}

	public static void moveToSlot(AbstractContainerScreen<?> screen, int menuSlot) {
		Slot slot = screen.getMenu().getSlot(menuSlot);
		move(screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
		if (screen.getSlotUnderMouse() != slot) {
			throw new IllegalStateException("Virtual input did not hit menu slot " + menuSlot);
		}
	}

	public static boolean click(Screen screen, double x, double y, int button, int modifiers) {
		move(x, y);
		return VirtualInputState.withModifiers(modifiers, () -> {
			screen.afterMouseAction();
			boolean handled = ClientHooks.onScreenMouseClickedPre(screen, x, y, button);
			if (!handled) {
				handled = ClientHooks.onScreenMouseClickedPost(screen, x, y, button, screen.mouseClicked(x, y, button));
			}
			if (!ClientHooks.onScreenMouseReleasedPre(screen, x, y, button)) {
				ClientHooks.onScreenMouseReleasedPost(screen, x, y, button, screen.mouseReleased(x, y, button));
			}
			return handled;
		});
	}
}
