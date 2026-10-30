package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageBreakActionBindingTest {
	@Test
	void parseArgumentsBreakDefaultsAndOffsetBindStrictly() {
		var action = new BreakTargetBlockAction();
		assertEquals(new BreakTargetBlockAction.Arguments(0, 0, 0, true), action.parseArguments(json("{}"), "test"));
		assertEquals(new BreakTargetBlockAction.Arguments(0, 0, 1, false), action.parseArguments(json("{\"offset\":[0,0,1],\"broken\":false}"), "test"));
		for (String invalid : new String[]{"{\"broken\":\"false\"}", "{\"offset\":[0,1]}", "{\"offset\":[0,0,1.5]}", "{\"extra\":true}"}) {
			assertThrows(IllegalArgumentException.class, () -> action.parseArguments(json(invalid), "test"));
		}
	}

	@Test
	void parseArgumentsFillSlotsRejectsAmbiguousAndInvalidFixtures() {
		var action = new SetStorageBlockSlotAction();
		assertEquals(new SetStorageBlockSlotAction.Arguments(0, "minecraft:diamond", 64, true),
				action.parseArguments(json("{\"item\":\"minecraft:diamond\",\"count\":64,\"allSlots\":true}"), "test"));
		for (String invalid : new String[]{"{\"slot\":0,\"allSlots\":true}", "{\"allSlots\":\"true\"}", "{\"count\":0}", "{\"count\":1.5}"}) {
			assertThrows(IllegalArgumentException.class, () -> action.parseArguments(json(invalid), "test"));
		}
	}

	private static com.google.gson.JsonObject json(String value) {
		return JsonParser.parseString(value).getAsJsonObject();
	}
}
