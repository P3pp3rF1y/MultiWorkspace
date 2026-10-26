package net.p3pp3rf1y.devclientautomation.mixin;

import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class BackgroundGameRendererMixin {
	/** Suppress only the automatic focus-loss pause; do not alter focus or user options. */
	@Redirect(method = "pauseIfInactive", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pauseGame(Z)V"))
	private void preventAutomaticFocusPause(Minecraft minecraft, boolean pauseOnly) {
		if (!BackgroundInputPolicy.enabled()) {
			minecraft.pauseGame(pauseOnly);
		}
	}
}
