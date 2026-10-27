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

	public static String string(JsonObject request, String name, String defaultValue) {
		if (!request.has(name)) {
			return defaultValue;
		}
		JsonElement value = request.get(name);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException(name + " must be a string");
		}
		return value.getAsString();
	}

	public static void requireMethod(HttpExchange exchange, String method) throws IOException {
		if (!method.equals(exchange.getRequestMethod())) {
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			byte[] response = "{\"ok\":false,\"error\":\"Method not allowed\"}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(405, response.length);
			try (OutputStream output = exchange.getResponseBody()) {
				output.write(response);
			}
			throw new IllegalStateException("Method not allowed");
		}
	}

	public static void sendJsonHandling(HttpExchange exchange, Logger logger, Supplier<String> response) throws IOException {
		try {
			sendJson(exchange, response.get());
		} catch (RuntimeException e) {
			logger.error("Automation endpoint failed", e);
			sendJson(exchange, GSON.toJson(new ErrorResponse(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())));
		}
	}

	public static void sendJson(HttpExchange exchange, String response) throws IOException {
		byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(bytes);
		}
	}

	private record ErrorResponse(boolean ok, String error) {
		private ErrorResponse(String error) {
			this(false, error);
		}
	}
}
