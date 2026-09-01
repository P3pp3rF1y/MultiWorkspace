package net.p3pp3rf1y.devclientautomation.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

public final class HttpJson {
	private static final Gson GSON = new Gson();

	private HttpJson() {
	}

	public static String readBody(HttpExchange exchange) throws IOException {
		return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}

	public static JsonObject readObject(HttpExchange exchange) throws IOException {
		String body = readBody(exchange);
		if (body.isBlank()) {
			return new JsonObject();
		}
		JsonElement element = JsonParser.parseString(body);
		if (!element.isJsonObject()) {
			throw new IllegalArgumentException("Expected a JSON object");
		}
		return element.getAsJsonObject();
	}

	public static void requireMethod(HttpExchange exchange, String method) throws IOException {
		if (!method.equals(exchange.getRequestMethod())) {
			JsonObject responseBody = new JsonObject();
			responseBody.addProperty("error", "Method not allowed");
			byte[] response = GSON.toJson(responseBody).getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(405, response.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(response);
			}
			throw new IllegalStateException("Method not allowed");
		}
	}

	public static void sendJson(HttpExchange exchange, String json) throws IOException {
		byte[] response = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(200, response.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(response);
		}
	}

	public static void sendJsonHandling(HttpExchange exchange, Logger logger, Supplier<String> jsonSupplier) throws IOException {
		try {
			sendJson(exchange, jsonSupplier.get());
		} catch (RuntimeException e) {
			logger.error("Automation endpoint failed", e);
			sendJson(exchange, GSON.toJson(errorResponse(e.getMessage())));
		}
	}

	public static void sendJson(HttpExchange exchange, Object response) throws IOException {
		sendJson(exchange, GSON.toJson(response));
	}

	public static String string(JsonObject request, String name, String defaultValue) {
		return value(request, name).map(JsonElement::getAsString).orElse(defaultValue);
	}

	public static boolean bool(JsonObject request, String name, boolean defaultValue) {
		return value(request, name).map(JsonElement::getAsBoolean).orElse(defaultValue);
	}

	public static int integer(JsonObject request, String name, int defaultValue) {
		return value(request, name).map(JsonElement::getAsInt).orElse(defaultValue);
	}

	public static double decimal(JsonObject request, String name, double defaultValue) {
		return value(request, name).map(JsonElement::getAsDouble).orElse(defaultValue);
	}

	public static long longValue(JsonObject request, String name, long defaultValue) {
		return value(request, name).map(JsonElement::getAsLong).orElse(defaultValue);
	}

	public static String jsonProperty(String name, String value) {
		return GSON.toJson(name) + ':' + GSON.toJson(value);
	}

	public static String errorJson(String error) {
		return GSON.toJson(errorResponse(error));
	}

	private static Optional<JsonElement> value(JsonObject request, String name) {
		return request.has(name) && !request.get(name).isJsonNull() ? Optional.of(request.get(name)) : Optional.empty();
	}

	private static JsonObject errorResponse(String error) {
		JsonObject response = new JsonObject();
		response.addProperty("ok", false);
		response.addProperty("error", error);
		return response;
	}
}
