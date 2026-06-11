package net.p3pp3rf1y.devclientautomation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(DevClientAutomation.MOD_ID)
public class DevClientAutomation {
	public static final String MOD_ID = "devclientautomation";

	public DevClientAutomation() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			DevClientAutomationClient.init();
		}
	}
}
