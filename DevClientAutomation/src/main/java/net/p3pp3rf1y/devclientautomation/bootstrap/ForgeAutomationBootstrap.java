package net.p3pp3rf1y.devclientautomation.bootstrap;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.DevClientAutomationClient;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import net.p3pp3rf1y.devclientautomation.mixin.SoundEngineAccess;
import net.p3pp3rf1y.devclientautomation.mixin.SoundManagerAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Forge 47 client bootstrap kept separate from scenario implementations. */
public final class ForgeAutomationBootstrap {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);
	private static boolean startupAudioApplied;

	private ForgeAutomationBootstrap() {
	}

	public static void init() {
		DevClientAutomationClient.init();
		MinecraftForge.EVENT_BUS.addListener(ForgeAutomationBootstrap::clientTick);
	}

	private static void clientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || startupAudioApplied) {
			return;
		}
		if (BackgroundInputPolicy.enabled()) {
			// Forge can tick before the asynchronous resource reload has initialized audio.
			if (!((SoundEngineAccess) (Object) ((SoundManagerAccess) Minecraft.getInstance().getSoundManager()).getSoundEngine()).isLoaded()) {
				return;
			}
			// Mute the live master gain once, leaving the user's volume preference and options file unchanged.
			Minecraft.getInstance().getSoundManager().updateSourceVolume(SoundSource.MASTER, 0.0F);
			LOGGER.info("Muted background regression audio for this launch (master gain 0; options unchanged)");
		}
		startupAudioApplied = true;
	}
}
