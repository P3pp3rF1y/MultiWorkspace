package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class RegressionActionTestSupport {
	private RegressionActionTestSupport() {
	}

	static BoundRegressionAction<?> bind(RegressionAction action, String json) {
		JsonObject arguments = JsonParser.parseString(json).getAsJsonObject();
		return assertInstanceOf(BoundRegressionAction.class, action.bind(arguments, "test action"));
	}

	static <A> A arguments(BoundRegressionAction<?> invocation, Class<A> type) {
		return type.cast(invocation.arguments());
	}

	static void assertMetadata(RegressionAction action, String id, Set<String> dependencies) {
		assertEquals(id, action.metadata().id());
		assertEquals("1", action.metadata().revision());
		assertEquals(dependencies, action.metadata().dependencies());
	}
}
