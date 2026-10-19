package net.p3pp3rf1y.devclientautomation.bridge;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpJsonTest {
	@Test
	void requestBodiesAcceptOnlyBlankOrJsonObjects() {
		assertEquals(0, HttpJson.parseObject(" \t\n").size());
		assertThrows(RuntimeException.class, () -> HttpJson.parseObject("{malformed"));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.parseObject("[]"));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.parseObject("null"));
	}

	@Test
	void requestFieldsDefaultOnlyWhenAbsentAndRejectWrongOrNullPrimitiveTypes() {
		JsonObject request = new JsonObject();
		assertEquals("default", HttpJson.string(request, "text", "default"));
		assertEquals(false, HttpJson.bool(request, "enabled", false));
		assertEquals(7, HttpJson.integer(request, "count", 7));
		assertEquals(1.5D, HttpJson.decimal(request, "ratio", 1.5D));
		assertEquals(9L, HttpJson.longValue(request, "ticks", 9L));

		request.addProperty("text", true);
		request.addProperty("enabled", "true");
		request.addProperty("count", "7");
		request.addProperty("ratio", false);
		request.addProperty("ticks", "9");
		assertThrows(IllegalArgumentException.class, () -> HttpJson.string(request, "text", "default"));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.bool(request, "enabled", false));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.integer(request, "count", 7));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.decimal(request, "ratio", 1.5D));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.longValue(request, "ticks", 9L));

		request.add("text", JsonNull.INSTANCE);
		request.add("enabled", JsonNull.INSTANCE);
		request.add("count", JsonNull.INSTANCE);
		request.add("ratio", JsonNull.INSTANCE);
		request.add("ticks", JsonNull.INSTANCE);
		assertThrows(IllegalArgumentException.class, () -> HttpJson.string(request, "text", "default"));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.bool(request, "enabled", false));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.integer(request, "count", 7));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.decimal(request, "ratio", 1.5D));
		assertThrows(IllegalArgumentException.class, () -> HttpJson.longValue(request, "ticks", 9L));
	}
}
