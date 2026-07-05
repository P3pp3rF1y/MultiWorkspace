package net.p3pp3rf1y.devclientautomation.recipeviewer;

public interface RecipeViewerAutomation {
	String id();

	String stateJson();

	String searchJson(String query);

	String openJson(String requestJson);

	String queryJson(String requestJson);

	default String transferJson(String requestJson) {
		return "{\"ok\":false,\"error\":\"Recipe transfer automation is not supported for this viewer\"}";
	}
}
