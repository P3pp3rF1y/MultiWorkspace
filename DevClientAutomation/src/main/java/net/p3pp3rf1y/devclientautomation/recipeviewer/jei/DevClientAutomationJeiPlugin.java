package net.p3pp3rf1y.devclientautomation.recipeviewer.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;

@SuppressWarnings("unused")
@JeiPlugin
public class DevClientAutomationJeiPlugin implements IModPlugin {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(DevClientAutomation.MOD_ID, "automation");

	@Override
	public Identifier getPluginUid() {
		return ID;
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		JeiRecipeViewerAutomation.setRuntime(jeiRuntime);
	}

	@Override
	public void onRuntimeUnavailable() {
		JeiRecipeViewerAutomation.setRuntime(null);
	}
}
