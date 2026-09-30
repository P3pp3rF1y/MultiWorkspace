package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class RegressionJsonObject {
	private final JsonObject object;
	private final String description;

	RegressionJsonObject(JsonObject object, String description, Set<String> permittedKeys) {
		this.object = object;
		this.description = description;
		for (String key : object.keySet()) {
			if (!permittedKeys.contains(key)) {
				throw new IllegalArgumentException("Unsupported " + description + " property " + key);
			}
		}
	}

	RegressionJsonObject withDescription(String newDescription) {
		return new RegressionJsonObject(object, newDescription, object.keySet());
	}

	String id(String name) {
		String value = string(name);
		if (!value.matches("[a-z0-9][a-z0-9-]{0,63}")) {
			throw new IllegalArgumentException("Invalid " + description + " " + name + " " + value);
		}
		return value;
	}

	String string(String name) {
		if (!object.has(name) || !object.get(name).isJsonPrimitive() || !object.get(name).getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException("Missing string " + name + " on " + description);
		}
		return object.get(name).getAsString();
	}

	int integer(String name) {
		if (!object.has(name) || !object.get(name).isJsonPrimitive() || !object.get(name).getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException("Missing numeric " + name + " on " + description);
		}
		return object.get(name).getAsInt();
	}

	boolean optionalBoolean(String name, boolean defaultValue) {
		if (!object.has(name)) {
			return defaultValue;
		}
		if (!object.get(name).isJsonPrimitive() || !object.get(name).getAsJsonPrimitive().isBoolean()) {
			throw new IllegalArgumentException("Expected boolean " + name + " on " + description);
		}
		return object.get(name).getAsBoolean();
	}

	JsonObject optionalObject(String name) {
		if (!object.has(name)) {
			return new JsonObject();
		}
		if (!object.get(name).isJsonObject()) {
			throw new IllegalArgumentException("Expected object " + name + " on " + description);
		}
		return object.getAsJsonObject(name);
	}

	List<JsonObject> objectArray(String name, String invalidElementMessage) {
		JsonArray values = array(name);
		List<JsonObject> result = new ArrayList<>();
		for (JsonElement value : values) {
			if (!value.isJsonObject()) {
				throw new IllegalArgumentException(invalidElementMessage);
			}
			result.add(value.getAsJsonObject());
		}
		return result;
	}

	Set<String> stringSet(String name) {
		Set<String> result = new LinkedHashSet<>();
		for (JsonElement value : array(name)) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || !result.add(value.getAsString())) {
				throw new IllegalArgumentException(description + " " + name + " must contain unique strings");
			}
		}
		return Set.copyOf(result);
	}

	private JsonArray array(String name) {
		if (!object.has(name) || !object.get(name).isJsonArray()) {
			throw new IllegalArgumentException("Missing array " + name + " on " + description);
		}
		return object.getAsJsonArray(name);
	}
}
