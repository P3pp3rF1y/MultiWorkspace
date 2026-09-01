package net.p3pp3rf1y.devclientautomation.platform.neoforge;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

public final class NeoForgeModelDiagnostics {
	private NeoForgeModelDiagnostics() {
	}

	public static String particleName(ItemStackRenderState state) {
		TextureAtlasSprite particle = state.pickParticleIcon(RandomSource.create(42));
		return particle == null ? null : particle.contents().name().toString();
	}
}
