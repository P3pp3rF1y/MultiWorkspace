package net.p3pp3rf1y.devclientautomation.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.devclientautomation.input.VirtualInputState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies virtual modifiers only while an automation input operation is active. */
@Mixin(Minecraft.class)
public abstract class VirtualModifiersMixin {
	@Inject(method = "hasShiftDown", at = @At("HEAD"), cancellable = true)
	private void virtualShift(CallbackInfoReturnable<Boolean> callback) {
		if (VirtualInputState.active()) {
			callback.setReturnValue(VirtualInputState.modifier(InputConstants.MOD_SHIFT));
		}
	}

	@Inject(method = "hasControlDown", at = @At("HEAD"), cancellable = true)
	private void virtualControl(CallbackInfoReturnable<Boolean> callback) {
		if (VirtualInputState.active()) {
			callback.setReturnValue(VirtualInputState.modifier(InputConstants.MOD_CONTROL));
		}
	}

	@Inject(method = "hasAltDown", at = @At("HEAD"), cancellable = true)
	private void virtualAlt(CallbackInfoReturnable<Boolean> callback) {
		if (VirtualInputState.active()) {
			callback.setReturnValue(VirtualInputState.modifier(InputConstants.MOD_ALT));
		}
	}
}
