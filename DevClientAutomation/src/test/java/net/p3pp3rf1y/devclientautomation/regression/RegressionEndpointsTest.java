package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegressionEndpointsTest {
	@Test
	void stringDefaultsOnlyWhenFieldIsAbsent() {
		assertEquals("", RegressionEndpoints.string(JsonParser.parseString("{}").getAsJsonObject(), "value"));
		assertThrows(IllegalArgumentException.class, () -> RegressionEndpoints.string(JsonParser.parseString("{\"value\": null}").getAsJsonObject(), "value"));
	}

	@Test
	void stringRejectsNonStringValues() {
		assertThrows(IllegalArgumentException.class, () -> RegressionEndpoints.string(JsonParser.parseString("{\"value\": false}").getAsJsonObject(), "value"));
	}

	@Test
	void readObjectRejectsNonObjectHttpBodies() throws ReflectiveOperationException {
		Method readObject = RegressionEndpoints.class.getDeclaredMethod("readObject", HttpExchange.class);
		readObject.setAccessible(true);

		for (String body : new String[]{"[]", "null", "\"text\""}) {
			InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> readObject.invoke(null, exchange(body)));
			assertEquals(IllegalArgumentException.class, exception.getCause().getClass());
			assertEquals("Regression request must be a JSON object", exception.getCause().getMessage());
		}
	}

	private static HttpExchange exchange(String body) {
		return new HttpExchange() {
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
				return URI.create("/regression/run");
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
				return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
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
		};
	}
}
