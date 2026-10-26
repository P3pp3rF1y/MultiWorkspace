package net.p3pp3rf1y.devclientautomation.mixin;

import com.mojang.blaze3d.platform.TextInputManager;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Text widgets may update native IME state during unattended screen transitions. */
@Mixin(TextInputManager.class)
public abstract class BackgroundTextInputManagerMixin {
	@Inject(method = "setTextInputArea", at = @At("HEAD"), cancellable = true)
	private void preventNativePreeditRectangle(int x0, int y0, int x1, int y1, CallbackInfo callback) {
		if (BackgroundInputPolicy.enabled()) {
			callback.cancel();
		}
	}

	@Inject(method = "startTextInput", at = @At("HEAD"), cancellable = true)
	private void preventNativeStartTextInput(Object owner, CallbackInfo callback) {
		if (BackgroundInputPolicy.enabled()) {
			callback.cancel();
		}
	}

	@Inject(method = "stopTextInput()V", at = @At("HEAD"), cancellable = true)
	private void preventNativeStopTextInput(CallbackInfo callback) {
		if (BackgroundInputPolicy.enabled()) {
			callback.cancel();
		}
	}
}
