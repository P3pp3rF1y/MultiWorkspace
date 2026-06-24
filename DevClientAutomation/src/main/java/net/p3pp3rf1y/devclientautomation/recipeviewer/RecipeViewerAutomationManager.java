package net.p3pp3rf1y.devclientautomation.recipeviewer;

import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.devclientautomation.JsonUtil;
import net.p3pp3rf1y.devclientautomation.recipeviewer.emi.EmiRecipeViewerAutomation;
import net.p3pp3rf1y.devclientautomation.recipeviewer.jei.JeiRecipeViewerAutomation;
import net.p3pp3rf1y.devclientautomation.recipeviewer.rei.ReiRecipeViewerAutomation;

import java.util.Optional;

public final class RecipeViewerAutomationManager {
	private RecipeViewerAutomationManager() {
	}

	public static String stateJson() {
		Optional<RecipeViewerAutomation> viewer = activeViewer();
		if (viewer.isEmpty()) {
			return noViewerJson();
		}
		return viewer.get().stateJson();
	}

	public static String searchJson(String query) {
		Optional<RecipeViewerAutomation> viewer = activeViewer();
		if (viewer.isEmpty()) {
			return noViewerJson();
		}
		return viewer.get().searchJson(query);
	}

	public static String openJson(String requestJson) {
		Optional<RecipeViewerAutomation> viewer = activeViewer();
		if (viewer.isEmpty()) {
			return noViewerJson();
		}
		return viewer.get().openJson(requestJson);
	}

	public static String queryJson(String requestJson) {
		Optional<RecipeViewerAutomation> viewer = activeViewer();
		if (viewer.isEmpty()) {
			return noViewerJson();
		}
		return viewer.get().queryJson(requestJson);
	}

	public static String statsJson() {
		Optional<RecipeViewerAutomation> viewer = activeViewer();
		if (viewer.isEmpty()) {
			return noViewerJson();
		}
		return viewer.get().statsJson();
	}

	private static Optional<RecipeViewerAutomation> activeViewer() {
		if (ModList.get().isLoaded("emi")) {
			return Optional.of(new EmiRecipeViewerAutomation());
		}
		if (ModList.get().isLoaded("roughlyenoughitems")) {
			return Optional.of(new ReiRecipeViewerAutomation());
		}
		if (ModList.get().isLoaded("jei")) {
			return Optional.of(new JeiRecipeViewerAutomation());
		}
		return Optional.empty();
	}

	private static String noViewerJson() {
		return "{\"ok\":false," + JsonUtil.property("error", "No supported recipe viewer is loaded") + "}";
	}
}
