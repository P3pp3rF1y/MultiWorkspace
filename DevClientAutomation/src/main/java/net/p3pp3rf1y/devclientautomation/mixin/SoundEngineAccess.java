package net.p3pp3rf1y.devclientautomation.mixin;

import com.mojang.blaze3d.audio.Listener;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface SoundEngineAccess {
	@Accessor("loaded")
	boolean isLoaded();

	@Accessor("listener")
	Listener getListener();
}
