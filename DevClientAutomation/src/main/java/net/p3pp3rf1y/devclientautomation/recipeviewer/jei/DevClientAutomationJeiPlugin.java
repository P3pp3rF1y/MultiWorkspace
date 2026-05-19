package net.p3pp3rf1y.devclientautomation.recipeviewer.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;

@SuppressWarnings("unused")
@JeiPlugin
public class DevClientAutomationJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = new ResourceLocation(DevClientAutomation.MOD_ID, "automation");

    @Override
    public ResourceLocation getPluginUid() {
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
