package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SetStorageUpgradeActionTest {
	@Test
	void bindUsesDefaultSlotAndEnabledState() {
		SetStorageUpgradeAction action = new SetStorageUpgradeAction();

		var bound = action.bind(JsonParser.parseString("{\"item\":\"sophisticatedstorage:crafting_upgrade\"}").getAsJsonObject(), "storage upgrade");

		assertNotNull(bound);
	}

	@Test
	void bindRejectsInvalidArguments() {
		SetStorageUpgradeAction action = new SetStorageUpgradeAction();

		Executable missingItem = () -> action.bind(JsonParser.parseString("{\"slot\":0}").getAsJsonObject(), "storage upgrade");
		Executable negativeSlot = () -> action.bind(JsonParser.parseString("{\"item\":\"x:y\",\"slot\":-1}").getAsJsonObject(), "storage upgrade");
		Executable nonBooleanEnabled = () -> action.bind(JsonParser.parseString("{\"item\":\"x:y\",\"enabled\":\"true\"}").getAsJsonObject(),
				"storage upgrade");

		assertThrows(IllegalArgumentException.class, missingItem);
		assertThrows(IllegalArgumentException.class, negativeSlot);
		assertThrows(IllegalArgumentException.class, nonBooleanEnabled);
	}
}
