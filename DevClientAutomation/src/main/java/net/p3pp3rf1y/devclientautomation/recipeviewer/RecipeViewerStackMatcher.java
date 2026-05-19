package net.p3pp3rf1y.devclientautomation.recipeviewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class RecipeViewerStackMatcher {
    private RecipeViewerStackMatcher() {}

    public static boolean matches(ItemStack stack, JsonObject selector) {
        if (selector.has("item") && !RecipeJsonUtil.itemId(stack).equals(selector.get("item").getAsString())) {
            return false;
        }
        if (!selector.has("match") || !selector.get("match").isJsonObject()) {
            return true;
        }

        JsonObject stackJson = JsonParser.parseString(RecipeJsonUtil.itemStackJson(stack)).getAsJsonObject();
        JsonObject match = selector.getAsJsonObject("match");
        for (Map.Entry<String, JsonElement> entry : match.entrySet()) {
            JsonElement actual = getPath(stackJson, entry.getKey());
            if (actual == null || !matchesValue(actual, entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static JsonElement getPath(JsonObject object, String path) {
        String[] segments = path.split("\\.");
        JsonElement current = object;
        for (String segment : segments) {
            if (!current.isJsonObject() || !current.getAsJsonObject().has(segment)) {
                return null;
            }
            current = current.getAsJsonObject().get(segment);
        }
        return current;
    }

    private static boolean matchesValue(JsonElement actual, JsonElement expected) {
        if (expected.isJsonPrimitive() && expected.getAsJsonPrimitive().isString()) {
            String expectedString = expected.getAsString();
            return actual.isJsonPrimitive() && actual.getAsString().equals(expectedString) || actual.toString().equals(expectedString);
        }
        return actual.equals(expected);
    }
}
