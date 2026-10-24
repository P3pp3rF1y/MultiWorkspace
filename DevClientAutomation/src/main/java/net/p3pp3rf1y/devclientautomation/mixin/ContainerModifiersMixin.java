package net.p3pp3rf1y.devclientautomation.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.p3pp3rf1y.devclientautomation.input.VirtualInputState;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerModifiersMixin {
	// Both container click handlers poll left/right Shift directly instead of Screen.hasShiftDown().
	@Redirect(method = {"mouseClicked", "mouseReleased"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(JI)Z"))
	private boolean virtualShift(long window, int key) {
		return VirtualInputState.active() ? VirtualInputState.modifier(GLFW.GLFW_MOD_SHIFT) : InputConstants.isKeyDown(window, key);
	}
}
