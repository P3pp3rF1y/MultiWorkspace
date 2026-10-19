package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DropSettingsGhostActionTest {
	@Test
	void bindsConcreteJeiSettingsDropOnlyWithTypedSlot() {
		RegressionAction action = new DropSettingsGhostAction();

		assertEquals("1", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "jei"), action.metadata().dependencies());
		assertNotNull(action.bind(JsonParser.parseString("{}").getAsJsonObject(), "drop"));
		assertNotNull(action.bind(JsonParser.parseString("{\"slot\":2}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> action.bind(JsonParser.parseString("{\"slot\":-1}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> action.bind(JsonParser.parseString("{\"slot\":2.5}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> action.bind(JsonParser.parseString("{\"slot\":\"2\"}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> action.bind(JsonParser.parseString("{\"extra\":true}").getAsJsonObject(), "drop"));
	}
}
