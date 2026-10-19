package net.p3pp3rf1y.devclientautomation.bridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpJsonTest {
	@Test
	void requestFieldsRequireJsonStringsAndBooleansWithoutCoercion() {
		JsonObject request = new JsonObject();
		request.addProperty("text", "value");
		request.addProperty("enabled", true);

		assertEquals("value", HttpJson.string(request, "text", "default"));
		assertEquals(true, HttpJson.bool(request, "enabled", false));
		assertEquals("default", HttpJson.string(request, "missing", "default"));
		assertEquals(false, HttpJson.bool(request, "missing", false));

		request.addProperty("text", true);
		assertThrows(IllegalArgumentException.class, () -> HttpJson.string(request, "text", "default"));
		request.add("text", com.google.gson.JsonNull.INSTANCE);
		assertThrows(IllegalArgumentException.class, () -> HttpJson.string(request, "text", "default"));
		request.addProperty("enabled", "true");
		assertThrows(IllegalArgumentException.class, () -> HttpJson.bool(request, "enabled", false));
	}

	@Test
	void readObjectParsesObjectsAndRejectsMalformedOrNonObjectBodies() throws IOException {
		JsonObject request = HttpJson.readObject(exchange("{\"name\":\"value\"}"));

		assertEquals("value", request.get("name").getAsString());
		assertThrows(JsonSyntaxException.class, () -> HttpJson.readObject(exchange("{")));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.readObject(exchange("[]")));
	}

	@Test
	void readObjectTreatsBlankBodiesAsEmptyObjectsForEndpointFieldValidation() throws IOException {
		JsonObject request = HttpJson.readObject(exchange(" \t\n"));

		assertTrue(request.isEmpty());
		assertThrows(IllegalArgumentException.class, () -> requireRunFields(request));
	}

	private static void requireRunFields(JsonObject request) {
		String suiteId = HttpJson.string(request, "suiteId", "");
		String testId = HttpJson.string(request, "testId", "");
		if (suiteId.isBlank() || testId.isBlank()) {
			throw new IllegalArgumentException("suiteId and testId are required");
		}
	}

	private static HttpExchange exchange(String body) {
		return new TestHttpExchange(body);
	}

	private static final class TestHttpExchange extends HttpExchange {
		private final Headers requestHeaders = new Headers();
		private final Headers responseHeaders = new Headers();
		private final InputStream requestBody;
		private final OutputStream responseBody = new ByteArrayOutputStream();
		private final Map<String, Object> attributes = new HashMap<>();

		private TestHttpExchange(String body) {
			requestBody = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public Headers getRequestHeaders() {
			return requestHeaders;
		}

		@Override
		public Headers getResponseHeaders() {
			return responseHeaders;
		}

		@Override
		public URI getRequestURI() {
			return URI.create("/");
		}

		@Override
		public String getRequestMethod() {
			return "POST";
		}

		@Override
		public HttpContext getHttpContext() {
			return null;
		}

		@Override
		public void close() {
		}

		@Override
		public InputStream getRequestBody() {
			return requestBody;
		}

		@Override
		public OutputStream getResponseBody() {
			return responseBody;
		}

		@Override
		public void sendResponseHeaders(int responseCode, long responseLength) {
		}

		@Override
		public InetSocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public int getResponseCode() {
			return 0;
		}

		@Override
		public InetSocketAddress getLocalAddress() {
			return null;
		}

		@Override
		public String getProtocol() {
			return "HTTP/1.1";
		}

		@Override
		public Object getAttribute(String name) {
			return attributes.get(name);
		}

		@Override
		public void setAttribute(String name, Object value) {
			attributes.put(name, value);
		}

		@Override
		public void setStreams(InputStream input, OutputStream output) {
			throw new UnsupportedOperationException();
		}

		@Override
		public HttpPrincipal getPrincipal() {
			return null;
		}
	}
}
