package net.p3pp3rf1y.devclientautomation;

import com.google.gson.Gson;

public final class JsonUtil {
	private static final Gson GSON = new Gson();

	private JsonUtil() {
	}

	public static String property(String name, String value) {
		return GSON.toJson(name) + ':' + GSON.toJson(value);
	}

	public static String rawProperty(String name, String value) {
		return GSON.toJson(name) + ':' + value;
	}
}
