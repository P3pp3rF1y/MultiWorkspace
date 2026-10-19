package net.p3pp3rf1y.devclientautomation.bridge;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpJsonTest {
	@Test
	void readObjectRejectsBlankMalformedAndNonObjectRequestBodies() {
		assertThrows(IllegalArgumentException.class, () -> HttpJson.readObject(" "));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.readObject("{"));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.readObject("[]"));

		assertEquals("value", HttpJson.readObject("{\"value\":\"value\"}").get("value").getAsString());
	}

	@Test
	void requestFieldsRejectCoercionAndExplicitNull() {
		JsonObject request = new JsonObject();
		request.addProperty("text", "value");
		request.addProperty("enabled", true);

		assertEquals("value", HttpJson.string(request, "text", "default"));
		assertEquals(true, HttpJson.bool(request, "enabled", false));
		assertEquals("default", HttpJson.string(request, "missing", "default"));

		request.addProperty("text", true);
		assertThrows(IllegalArgumentException.class, () -> HttpJson.string(request, "text", "default"));
		request.add("text", JsonNull.INSTANCE);
		assertThrows(IllegalArgumentException.class, () -> HttpJson.string(request, "text", "default"));
		request.addProperty("enabled", "true");
		assertThrows(IllegalArgumentException.class, () -> HttpJson.bool(request, "enabled", false));
		request.add("enabled", JsonNull.INSTANCE);
		assertThrows(IllegalArgumentException.class, () -> HttpJson.bool(request, "enabled", false));
	}
}
