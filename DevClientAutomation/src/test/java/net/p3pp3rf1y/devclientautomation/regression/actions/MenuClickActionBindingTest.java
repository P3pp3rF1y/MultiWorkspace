package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuClickActionBindingTest {
	@Test
	void clickCapturedMobBindsIndexStrictly() {
		RegressionAction action = new RegressionActionRegistry().require("menu.clickCapturedMob");
		assertSame(action, RegressionActionTestSupport.bind(action, "{}").action());
		assertSame(action, RegressionActionTestSupport.bind(action, "{\"index\":2}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"index\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"other\":1}"));
		RegressionActionTestSupport.assertMetadata(action, "menu.clickCapturedMob", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Test
	void clickPlayerInventorySlotBindsEachAreaAndRejectsInvalidSlots() {
		RegressionAction action = new RegressionActionRegistry().require("menu.clickPlayerInventorySlot");
		for (String args : new String[]{"{}", "{\"area\":\"inventory\",\"slot\":35,\"button\":1}", "{\"area\":\"crafting\",\"slot\":3}",
				"{\"area\":\"result\"}"}) {
			assertSame(action, RegressionActionTestSupport.bind(action, args).action());
		}
		for (String args : new String[]{"{\"area\":\"other\"}", "{\"area\":\"result\",\"slot\":1}", "{\"area\":\"crafting\",\"slot\":4}", "{\"slot\":36}",
				"{\"button\":2}", "{\"extra\":0}"}) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, args));
		}
		RegressionActionTestSupport.assertMetadata(action, "menu.clickPlayerInventorySlot", Set.of("minecraft:client"));
	}

	@Test
	void clickSettingsControlsAndGhostDropRetainSourceArgumentContract() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionAction control = registry.require("menu.clickSettingsControl");
		RegressionAction settings = registry.require("menu.clickStorageSettings");
		RegressionAction drop = registry.require("recipeviewer.dropSettingsGhost");
		for (String args : new String[]{"{\"operation\":\"memory\"}", "{\"operation\":\"display\"}", "{\"operation\":\"slot\",\"slot\":1}"}) {
			assertSame(control, RegressionActionTestSupport.bind(control, args).action());
		}
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(control, "{\"operation\":\"other\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(control, "{\"operation\":\"slot\",\"slot\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(control, "{}"));
		assertSame(settings, RegressionActionTestSupport.bind(settings, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(settings, "{\"unexpected\":1}"));
		assertSame(drop, drop.bind(JsonParser.parseString("{}").getAsJsonObject(), "test").action());
		assertSame(drop, drop.bind(JsonParser.parseString("{\"slot\":1}").getAsJsonObject(), "test").action());
		assertThrows(IllegalArgumentException.class, () -> drop.bind(JsonParser.parseString("{\"slot\":-1}").getAsJsonObject(), "test"));
		assertThrows(IllegalArgumentException.class, () -> drop.bind(JsonParser.parseString("{\"extra\":true}").getAsJsonObject(), "test"));
		RegressionActionTestSupport.assertMetadata(control, "menu.clickSettingsControl", Set.of("minecraft:client", "sophisticatedcore"));
		RegressionActionTestSupport.assertMetadata(settings, "menu.clickStorageSettings", Set.of("minecraft:client", "sophisticatedcore"));
		RegressionActionTestSupport.assertMetadata(drop, "recipeviewer.dropSettingsGhost", Set.of("minecraft:client", "jei"));
	}

	@Test
	void clickStorageSlotBindsRealMenuAreasAndClickModes() {
		RegressionAction action = new RegressionActionRegistry().require("menu.clickStorageSlot");
		for (String args : new String[]{"{\"area\":\"storage\",\"click\":\"pickup\"}", "{\"area\":\"player\",\"slot\":35,\"click\":\"quickMove\"}",
				"{\"area\":\"upgrade\",\"slot\":1,\"click\":\"pickup\"}"}) {
			assertSame(action, RegressionActionTestSupport.bind(action, args).action());
		}
		for (String args : new String[]{"{}", "{\"area\":\"other\",\"click\":\"pickup\"}", "{\"area\":\"storage\",\"click\":\"other\"}",
				"{\"area\":\"player\",\"slot\":36,\"click\":\"pickup\"}", "{\"area\":\"upgrade\",\"slot\":-1,\"click\":\"pickup\"}",
				"{\"area\":\"storage\",\"click\":\"pickup\",\"extra\":1}"}) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, args));
		}
		RegressionActionTestSupport.assertMetadata(action, "menu.clickStorageSlot", Set.of("minecraft:client", "sophisticatedcore"));
	}
}
