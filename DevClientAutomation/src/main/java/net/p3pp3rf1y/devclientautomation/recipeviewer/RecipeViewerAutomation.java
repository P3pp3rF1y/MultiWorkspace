package net.p3pp3rf1y.devclientautomation.recipeviewer;

public interface RecipeViewerAutomation {
    String id();

    String stateJson();

    String searchJson(String query);

    String openJson(String requestJson);

    String queryJson(String requestJson);

    default String statsJson() {
        return "{\"ok\":false,\"error\":\"Recipe viewer stats are not supported for this viewer\"}";
    }
}
