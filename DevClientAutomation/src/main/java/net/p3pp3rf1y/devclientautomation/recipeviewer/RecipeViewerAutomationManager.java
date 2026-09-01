package net.p3pp3rf1y.devclientautomation.recipeviewer;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.devclientautomation.platform.neoforge.NeoForgeLoadedModLookup;
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

	public static String transferJson(String requestJson) {
		Optional<RecipeViewerAutomation> viewer = activeViewer();
		if (viewer.isEmpty()) {
			return noViewerJson();
		}
		return viewer.get().transferJson(requestJson);
	}

	public static String statsJson() {
		Optional<RecipeViewerAutomation> viewer = activeViewer();
		if (viewer.isEmpty()) {
			return noViewerJson();
		}
		if (viewer.get() instanceof EmiRecipeViewerAutomation emi) {
			return emi.statsJson();
		}
		return viewer.get().stateJson();
	}

	private static Optional<RecipeViewerAutomation> activeViewer() {
		if (NeoForgeLoadedModLookup.isLoaded("emi")) {
			return Optional.of(new EmiRecipeViewerAutomation());
		}
		if (NeoForgeLoadedModLookup.isLoaded("roughlyenoughitems")) {
			return Optional.of(new ReiRecipeViewerAutomation());
		}
		if (NeoForgeLoadedModLookup.isLoaded("jei")) {
			return Optional.of(new JeiRecipeViewerAutomation());
		}
		return Optional.empty();
	}

	private static String noViewerJson() {
		JsonObject response = new JsonObject();
		response.addProperty("ok", false);
		response.addProperty("error", "No supported recipe viewer is loaded");
		return response.toString();
	}
}
