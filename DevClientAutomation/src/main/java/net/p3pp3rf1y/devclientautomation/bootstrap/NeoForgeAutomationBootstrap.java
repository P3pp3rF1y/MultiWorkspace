package net.p3pp3rf1y.devclientautomation.bootstrap;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.demo.DemoCommand;
import net.p3pp3rf1y.devclientautomation.scenarios.backpacks.BackpackLinkedStoragePerformanceRegression;

@Mod(value = DevClientAutomation.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeAutomationBootstrap {
	public NeoForgeAutomationBootstrap(IEventBus modBus) {
		modBus.addListener(this::clientSetup);
		DemoCommand.init();
		BackpackLinkedStoragePerformanceRegression.init();
	}

	private void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(DevClientAutomation::start);
	}
}
