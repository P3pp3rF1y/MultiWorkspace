package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedSettingsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertSettingsSelectionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickSettingsControlAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickStorageSettingsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.DropSettingsGhostAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionAction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedSettingsSuiteTest {
	@Test
	void parseSettingsSuiteUsesNativeLinkingAndSettingsClicksForBothFamiliesAndMovingEndpoints() throws IOException {
		RegressionSuite suite;
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-settings.json")) {
			assertNotNull(stream);
			suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
		}
		assertEquals(10, suite.tests().size());
		assertEquals(1, suite.tests().stream().filter(test -> test.tags().contains("runtimeConformance")).count());
		for (RegressionTest test : suite.tests().stream().filter(test -> !test.tags().contains("runtimeConformance")).toList()) {
			assertTrue(test.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("menu.clickStorageSettings")));
			assertTrue(test.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("assert.linkedSettings")));
			assertTrue(test.steps().stream().anyMatch(step -> step.action().action().metadata().id().equals("recipeviewer.dropSettingsGhost")));
			assertEquals(3, test.steps().stream().filter(step -> step.action().action().metadata().id().equals("assert.settingsSelection")).count());
			assertTrue(test.steps().stream().noneMatch(step -> step.action().action().metadata().id().equals("backpack.linkHeld")
					|| step.action().action().metadata().id().equals("storage.linkBlock")));
		}
	}

	@Test
	void strictSettingsArgumentsRejectMalformedEdits() {
		ClickSettingsControlAction click = new ClickSettingsControlAction();
		DropSettingsGhostAction drop = new DropSettingsGhostAction();
		AssertSettingsSelectionAction selection = new AssertSettingsSelectionAction();
		ClickStorageSettingsAction tab = new ClickStorageSettingsAction();
		AssertLinkedSettingsAction linked = new AssertLinkedSettingsAction();
		assertNotNull(click.bind(JsonParser.parseString("{\"operation\":\"memory\"}").getAsJsonObject(), "click"));
		assertNotNull(drop.bind(JsonParser.parseString("{}").getAsJsonObject(), "drop"));
		assertNotNull(selection.bind(JsonParser.parseString("{\"display\":true}").getAsJsonObject(), "selection"));
		assertNotNull(tab.bind(JsonParser.parseString("{}").getAsJsonObject(), "tab"));
		assertNotNull(linked.bind(JsonParser.parseString("{\"backpackPrimary\":true}").getAsJsonObject(), "linked"));
		assertThrows(IllegalArgumentException.class, () -> click.bind(JsonParser.parseString("{\"operation\":\"other\"}").getAsJsonObject(), "click"));
		assertThrows(IllegalArgumentException.class, () -> drop.bind(JsonParser.parseString("{\"slot\":-1}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> selection.bind(JsonParser.parseString("{\"display\":\"true\"}").getAsJsonObject(), "selection"));
		assertThrows(IllegalArgumentException.class, () -> tab.bind(JsonParser.parseString("{\"unexpected\":1}").getAsJsonObject(), "tab"));
		assertThrows(IllegalArgumentException.class, () -> linked.bind(JsonParser.parseString("{}").getAsJsonObject(), "linked"));
		assertThrows(IllegalArgumentException.class, () -> linked.bind(JsonParser.parseString("{\"backpackPrimary\":\"true\"}").getAsJsonObject(), "linked"));
	}

	@Test
	void bindSettingsEditsRejectInvalidOperationsSlotsAndUntypedSelection() {
		ClickSettingsControlAction click = new ClickSettingsControlAction();
		RegressionAction drop = new RegressionActionRegistry().require("recipeviewer.dropSettingsGhost");
		AssertSettingsSelectionAction selection = new AssertSettingsSelectionAction();
		assertNotNull(click.bind(JsonParser.parseString("{\"operation\":\"memory\"}").getAsJsonObject(), "click"));
		assertNotNull(drop.bind(JsonParser.parseString("{}").getAsJsonObject(), "drop"));
		assertNotNull(drop.bind(JsonParser.parseString("{\"slot\":0}").getAsJsonObject(), "drop"));
		assertNotNull(selection.bind(JsonParser.parseString("{\"display\":true}").getAsJsonObject(), "selection"));
		assertThrows(IllegalArgumentException.class, () -> click.bind(JsonParser.parseString("{\"operation\":\"other\"}").getAsJsonObject(), "click"));
		assertThrows(IllegalArgumentException.class, () -> drop.bind(JsonParser.parseString("{\"slot\":-1}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> drop.bind(JsonParser.parseString("{\"slot\":0.5}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> drop.bind(JsonParser.parseString("{\"slot\":\"0\"}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> drop.bind(JsonParser.parseString("{\"unexpected\":1}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> selection.bind(JsonParser.parseString("{\"display\":\"true\"}").getAsJsonObject(), "selection"));
	}

	@Test
	void bindSettingsActionsRejectUnknownArgumentsAndRequireTypedPrimaryFamily() {
		ClickStorageSettingsAction click = new ClickStorageSettingsAction();
		AssertLinkedSettingsAction assertion = new AssertLinkedSettingsAction();
		assertNotNull(click.bind(JsonParser.parseString("{}").getAsJsonObject(), "click"));
		assertNotNull(assertion.bind(JsonParser.parseString("{\"backpackPrimary\":true}").getAsJsonObject(), "assert"));
		assertThrows(IllegalArgumentException.class, () -> click.bind(JsonParser.parseString("{\"unexpected\":1}").getAsJsonObject(), "click"));
		assertThrows(IllegalArgumentException.class, () -> assertion.bind(JsonParser.parseString("{}").getAsJsonObject(), "assert"));
		assertThrows(IllegalArgumentException.class,
				() -> assertion.bind(JsonParser.parseString("{\"backpackPrimary\":\"true\"}").getAsJsonObject(), "assert"));
	}
}
