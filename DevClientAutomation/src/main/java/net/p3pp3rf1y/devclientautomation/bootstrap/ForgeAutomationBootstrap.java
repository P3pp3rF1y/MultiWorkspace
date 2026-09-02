package net.p3pp3rf1y.devclientautomation.bootstrap;

import net.p3pp3rf1y.devclientautomation.DevClientAutomationClient;

/** Forge 47 client bootstrap kept separate from scenario implementations. */
public final class ForgeAutomationBootstrap {
	private ForgeAutomationBootstrap() {
	}

	public static void init() {
		DevClientAutomationClient.init();
	}
}
