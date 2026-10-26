package net.p3pp3rf1y.devclientautomation.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorType;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** GUI extraction can select a native cursor even when mouse grab/release is suppressed. */
@Mixin(CursorType.class)
public abstract class BackgroundCursorTypeMixin {
	@Inject(method = "select", at = @At("HEAD"), cancellable = true)
	private void preventNativeCursorSelection(Window window, CallbackInfo callback) {
		if (BackgroundInputPolicy.enabled()) {
			callback.cancel();
		}
	}
}
