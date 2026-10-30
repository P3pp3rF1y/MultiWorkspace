package net.p3pp3rf1y.devclientautomation.mixin;

import net.minecraft.client.MouseHandler;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import net.p3pp3rf1y.devclientautomation.input.VirtualInputState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Installed before the first screen transition, including world loading and cleanup. */
@Mixin(MouseHandler.class)
public abstract class BackgroundMouseHandlerMixin {
	/** Logical capture for held world input, without changing the native cursor mode. */
	@Inject(method = "isMouseGrabbed", at = @At("HEAD"), cancellable = true)
	private void reportVirtualCapture(CallbackInfoReturnable<Boolean> callback) {
		if (BackgroundInputPolicy.enabled() && VirtualInputState.heldMouseButton() >= 0) {
			callback.setReturnValue(true);
		}
	}

	@Inject(method = {"grabMouse", "releaseMouse"}, at = @At("HEAD"), cancellable = true)
	private void preventNativeCursorModeChange(CallbackInfo callback) {
		if (BackgroundInputPolicy.enabled()) {
			callback.cancel();
		}
	}

	@Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
	private void preserveVirtualCoordinates(long window, double x, double y, double xRelative, double yRelative, CallbackInfo callback) {
		if (BackgroundInputPolicy.enabled()) {
			callback.cancel();
		}
	}

	/** 26.3 resynchronizes from SDL after every screen transition and GUI resize. */
	@Inject(method = "resyncMousePosition", at = @At("HEAD"), cancellable = true)
	private void preventNativePositionResync(CallbackInfo callback) {
		if (BackgroundInputPolicy.enabled()) {
			callback.cancel();
		}
	}
}
