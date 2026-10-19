package net.p3pp3rf1y.devclientautomation.bridge;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpJsonTest {
	@Test
	void readObjectRejectsBlankMalformedAndNonObjectEnvelopes() {
		assertThrows(IllegalArgumentException.class, () -> HttpJson.readObject(new TestExchange("  \t")));
		assertThrows(JsonParseException.class, () -> HttpJson.readObject(new TestExchange("{")));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.readObject(new TestExchange("[]")));
	}

	@Test
	void requestFieldsRequireExactPrimitiveTypesAndRejectExplicitNull() {
		JsonObject request = new JsonObject();
		request.addProperty("text", "value");
		request.addProperty("enabled", true);
		request.addProperty("whole", 12);
		request.addProperty("decimal", 1.25D);
		request.addProperty("large", 1234567890123L);

		assertEquals("value", HttpJson.string(request, "text", "default"));
		assertEquals(true, HttpJson.bool(request, "enabled", false));
		assertEquals(12, HttpJson.integer(request, "whole", 0));
		assertEquals(1.25D, HttpJson.decimal(request, "decimal", 0D));
		assertEquals(1234567890123L, HttpJson.longValue(request, "large", 0L));
		assertEquals("default", HttpJson.string(request, "missing", "default"));
		assertEquals(false, HttpJson.bool(request, "missing", false));

		request.addProperty("text", true);
		request.addProperty("enabled", "true");
		request.addProperty("whole", "12");
		request.addProperty("decimal", false);
		request.add("large", JsonNull.INSTANCE);
		assertThrows(IllegalArgumentException.class, () -> HttpJson.string(request, "text", "default"));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.bool(request, "enabled", false));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.integer(request, "whole", 0));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.decimal(request, "decimal", 0D));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.longValue(request, "large", 0L));
	}

	private static final class TestExchange extends HttpExchange {
		private final InputStream requestBody;

		private TestExchange(String requestBody) {
			this.requestBody = new ByteArrayInputStream(requestBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}

		@Override
		public Headers getRequestHeaders() {
			return new Headers();
		}

		@Override
		public Headers getResponseHeaders() {
			return new Headers();
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
			return OutputStream.nullOutputStream();
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
			return null;
		}

		@Override
		public void setAttribute(String name, Object value) {
		}

		@Override
		public void setStreams(InputStream input, OutputStream output) {
		}

		@Override
		public HttpPrincipal getPrincipal() {
			return null;
		}
	}
}
