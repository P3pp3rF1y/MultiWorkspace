package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SetUpgradeTabActionTest {
	@Test
	void bindUsesDefaultUpgrade() {
		SetUpgradeTabAction action = new SetUpgradeTabAction();
		var bound = action.bind(JsonParser.parseString("{\"open\":true}").getAsJsonObject(), "set tab");
		assertNotNull(bound);
	}

	@Test
	void bindUsesExplicitUpgrade() {
		SetUpgradeTabAction action = new SetUpgradeTabAction();
		var bound = action.bind(JsonParser.parseString("{\"upgrade\":2,\"open\":false}").getAsJsonObject(), "set tab");
		assertNotNull(bound);
	}

	@Test
	void bindRejectsInvalidArguments() {
		SetUpgradeTabAction action = new SetUpgradeTabAction();
		Executable negativeUpgrade = () -> action.bind(JsonParser.parseString("{\"upgrade\":-1,\"open\":true}").getAsJsonObject(), "set tab");
		Executable nonBooleanOpen = () -> action.bind(JsonParser.parseString("{\"open\":1}").getAsJsonObject(), "set tab");
		assertThrows(IllegalArgumentException.class, negativeUpgrade);
		assertThrows(IllegalArgumentException.class, nonBooleanOpen);
	}
}
