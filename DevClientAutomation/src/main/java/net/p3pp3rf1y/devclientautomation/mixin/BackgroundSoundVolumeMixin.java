package net.p3pp3rf1y.devclientautomation.mixin;

import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class BackgroundSoundVolumeMixin {
	/** Mute every category at the common final-volume boundary, including music after gain updates. */
	@Inject(method = "getFinalSoundSourceVolume", at = @At("RETURN"), cancellable = true)
	private void muteBackgroundSound(SoundSource source, CallbackInfoReturnable<Float> result) {
		if (BackgroundInputPolicy.enabled()) {
			result.setReturnValue(0.0F);
		}
	}
}
