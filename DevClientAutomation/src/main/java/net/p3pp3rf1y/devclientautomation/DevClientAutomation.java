package net.p3pp3rf1y.devclientautomation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.devclientautomation.bootstrap.ForgeAutomationBootstrap;

@Mod(DevClientAutomation.MOD_ID)
public class DevClientAutomation {
	public static final String MOD_ID = "devclientautomation";

	public DevClientAutomation() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			ForgeAutomationBootstrap.init();
		}
	}
}
