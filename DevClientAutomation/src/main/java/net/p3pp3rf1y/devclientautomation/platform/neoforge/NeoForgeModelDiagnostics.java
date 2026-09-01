package net.p3pp3rf1y.devclientautomation.platform.neoforge;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;

public final class NeoForgeModelDiagnostics {
	private NeoForgeModelDiagnostics() {
	}

	public static String particleName(ItemStackRenderState state) {
		Material.Baked particle = state.pickParticleMaterial(RandomSource.create(42));
		return particle == null ? null : particle.sprite().contents().name().toString();
	}
}
