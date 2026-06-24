package net.p3pp3rf1y.devclientautomation.recipeviewer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Optional;

public final class RecipeViewerRequest {
	private RecipeViewerRequest() {
	}

	public static JsonObject parse(String body) {
		if (body == null || body.isBlank()) {
			return new JsonObject();
		}
		return JsonParser.parseString(body).getAsJsonObject();
	}

	public static int limit(JsonObject request, int defaultLimit) {
		return request.has("limit") ? request.get("limit").getAsInt() : defaultLimit;
	}

	public static String mode(JsonObject request, String defaultMode) {
		return request.has("mode") ? request.get("mode").getAsString() : defaultMode;
	}

	public static Optional<JsonObject> focus(JsonObject request) {
		return request.has("focus") && request.get("focus").isJsonObject() ? Optional.of(request.getAsJsonObject("focus")) : Optional.empty();
	}

	public static String itemId(JsonObject request) {
		return focus(request).filter(focus -> focus.has("item")).map(focus -> focus.get("item").getAsString())
				.orElseGet(() -> request.has("item") ? request.get("item").getAsString() : "");
	}
}
