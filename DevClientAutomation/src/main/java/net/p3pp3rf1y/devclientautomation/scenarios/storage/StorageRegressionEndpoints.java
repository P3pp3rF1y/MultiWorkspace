package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;

public final class StorageRegressionEndpoints {
	private StorageRegressionEndpoints() {
	}

	public static void runLinkedStorage(HttpExchange exchange) throws IOException {
		run(exchange, request -> StorageLinkedStorageRegression.run());
	}

	public static void setupLinkedLimitedReload(HttpExchange exchange) throws IOException {
		run(exchange, request -> StorageLinkedStorageRegression.setupLinkedLimitedBarrelReloadProjection());
	}

	public static void linkedLimitedReloadStatus(HttpExchange exchange) throws IOException {
		run(exchange, request -> StorageLinkedStorageRegression.linkedLimitedBarrelReloadProjectionStatus(UUID.fromString(request.get("groupId").getAsString()),
				new BlockPos(request.get("primaryX").getAsInt(), request.get("primaryY").getAsInt(), request.get("primaryZ").getAsInt())));
	}

	private static void run(HttpExchange exchange, Function<JsonObject, String> action) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			send(exchange, "{\"ok\":false,\"error\":\"Method not allowed\"}");
			return;
		}
		try {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			send(exchange, action.apply(body.isBlank() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject()));
		} catch (RuntimeException e) {
			send(exchange, "{\"ok\":false,\"error\":" + quote(e.getMessage()) + "}");
		}
	}

	private static void send(HttpExchange exchange, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(bytes);
		}
	}

	private static String quote(String value) {
		return "\"" + (value == null ? "Unknown error" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")) + "\"";
	}
}
