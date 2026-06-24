package net.p3pp3rf1y.devclientautomation;

import java.util.Optional;

public final class JsonUtil {
	private JsonUtil() {
	}

	public static String property(String name, String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escape(value) + "\"");
	}

	public static String rawProperty(String name, String value) {
		return "\"" + name + "\":" + value;
	}

	public static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	public static Optional<String> extractString(String json, String key) {
		String prefix = "\"" + key + "\"";
		int keyIndex = json.indexOf(prefix);
		if (keyIndex < 0) {
			return Optional.empty();
		}
		int colonIndex = json.indexOf(':', keyIndex + prefix.length());
		int startQuoteIndex = json.indexOf('"', colonIndex + 1);
		if (colonIndex < 0 || startQuoteIndex < 0) {
			return Optional.empty();
		}
		StringBuilder value = new StringBuilder();
		boolean escaped = false;
		for (int i = startQuoteIndex + 1; i < json.length(); i++) {
			char c = json.charAt(i);
			if (escaped) {
				value.append(c);
				escaped = false;
			} else if (c == '\\') {
				escaped = true;
			} else if (c == '"') {
				return Optional.of(value.toString());
			} else {
				value.append(c);
			}
		}
		return Optional.empty();
	}

	public static Optional<Boolean> extractBoolean(String json, String key) {
		return extractRawValue(json, key).map(value -> Boolean.parseBoolean(value.toLowerCase(java.util.Locale.ROOT)));
	}

	public static Optional<Integer> extractInt(String json, String key) {
		return extractRawValue(json, key).map(Integer::parseInt);
	}

	public static Optional<Long> extractLong(String json, String key) {
		return extractRawValue(json, key).map(Long::parseLong);
	}

	private static Optional<String> extractRawValue(String json, String key) {
		String prefix = "\"" + key + "\"";
		int keyIndex = json.indexOf(prefix);
		if (keyIndex < 0) {
			return Optional.empty();
		}
		int colonIndex = json.indexOf(':', keyIndex + prefix.length());
		if (colonIndex < 0) {
			return Optional.empty();
		}
		int start = colonIndex + 1;
		while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
			start++;
		}
		int end = start;
		while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
			end++;
		}
		return Optional.of(json.substring(start, end).trim());
	}
}
