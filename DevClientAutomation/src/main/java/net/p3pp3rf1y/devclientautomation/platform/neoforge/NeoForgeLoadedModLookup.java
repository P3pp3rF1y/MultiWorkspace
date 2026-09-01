package net.p3pp3rf1y.devclientautomation.platform.neoforge;

import net.neoforged.fml.ModList;

public final class NeoForgeLoadedModLookup {
	private NeoForgeLoadedModLookup() {
	}

	public static boolean isLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}
}
