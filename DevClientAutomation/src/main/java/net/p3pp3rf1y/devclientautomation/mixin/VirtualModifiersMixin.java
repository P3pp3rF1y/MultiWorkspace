package net.p3pp3rf1y.devclientautomation.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.devclientautomation.input.VirtualInputState;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class VirtualModifiersMixin {
	@Inject(method = "hasShiftDown", at = @At("HEAD"), cancellable = true)
	private static void virtualShift(CallbackInfoReturnable<Boolean> callback) {
		if (VirtualInputState.active()) {
			callback.setReturnValue(VirtualInputState.modifier(GLFW.GLFW_MOD_SHIFT));
		}
	}

	@Inject(method = "hasControlDown", at = @At("HEAD"), cancellable = true)
	private static void virtualControl(CallbackInfoReturnable<Boolean> callback) {
		if (VirtualInputState.active()) {
			callback.setReturnValue(VirtualInputState.modifier(GLFW.GLFW_MOD_CONTROL));
		}
	}

	@Inject(method = "hasAltDown", at = @At("HEAD"), cancellable = true)
	private static void virtualAlt(CallbackInfoReturnable<Boolean> callback) {
		if (VirtualInputState.active()) {
			callback.setReturnValue(VirtualInputState.modifier(GLFW.GLFW_MOD_ALT));
		}
	}
}
