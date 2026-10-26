package net.p3pp3rf1y.devclientautomation.bootstrap;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.demo.DemoCommand;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import net.p3pp3rf1y.devclientautomation.regression.RegressionCoordinator;
import net.p3pp3rf1y.devclientautomation.scenarios.backpacks.BackpackLinkedStoragePerformanceRegression;

@Mod(value = DevClientAutomation.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeAutomationBootstrap {
	private boolean startupAudioApplied;

	public NeoForgeAutomationBootstrap(IEventBus modBus) {
		modBus.addListener(this::clientSetup);
		NeoForge.EVENT_BUS.addListener(this::clientTick);
		DemoCommand.init();
		BackpackLinkedStoragePerformanceRegression.init();
		RegressionCoordinator.init();
	}

	private void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(DevClientAutomation::start);
	}

	private void clientTick(ClientTickEvent.Post event) {
		if (startupAudioApplied) {
			return;
		}
		startupAudioApplied = true;
		if (BackgroundInputPolicy.enabled()) {
			// Recalculate existing channels through the background-only final-volume boundary.
			Minecraft.getInstance().getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
			DevClientAutomation.getLogger().info("Muted background regression audio for this launch (all effective category volumes 0; options unchanged)");
		}
	}
}
