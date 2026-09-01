package net.p3pp3rf1y.devclientautomation.platform.neoforge;

import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class NeoForgeModelDiagnostics {
	private NeoForgeModelDiagnostics() {
	}

	public static String particleName(BakedModel model) {
		return model.getParticleIcon(ModelData.EMPTY).contents().name().toString();
	}
}
