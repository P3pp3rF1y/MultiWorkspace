package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertLinkedSettingsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertSettingsSelectionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickSettingsControlAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ClickStorageSettingsAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.DropSettingsGhostAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionInvocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LinkedSettingsSuiteTest {
	@Test
	void parseSettingsSuiteUsesNativeLinkingAndSettingsClicksForBothFamiliesAndMovingEndpoints() throws IOException {
		String json;
		try (InputStream stream = getClass().getResourceAsStream("/devclientautomation/regressions/linked-storage-settings.json")) {
			assertNotNull(stream);
			json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
		RegressionSuite suite = RegressionSuiteParser.parse(json, new RegressionActionRegistry());
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
	void bindSettingsEditsRejectInvalidOperationsSlotsAndUntypedSelection() {
		ClickSettingsControlAction click = new ClickSettingsControlAction();
		DropSettingsGhostAction drop = new DropSettingsGhostAction();
		AssertSettingsSelectionAction assertion = new AssertSettingsSelectionAction();
		RegressionActionInvocation clickArguments = click.bind(JsonParser.parseString("{\"operation\":\"memory\"}").getAsJsonObject(), "click");
		RegressionActionInvocation dropArguments = drop.bind(JsonParser.parseString("{}").getAsJsonObject(), "drop");
		RegressionActionInvocation assertArguments = assertion.bind(JsonParser.parseString("{\"display\":true}").getAsJsonObject(), "assert");
		assertNotNull(clickArguments);
		assertNotNull(dropArguments);
		assertNotNull(assertArguments);
		assertThrows(IllegalArgumentException.class, () -> click.bind(JsonParser.parseString("{\"operation\":\"other\"}").getAsJsonObject(), "click"));
		assertThrows(IllegalArgumentException.class, () -> drop.bind(JsonParser.parseString("{\"slot\":-1}").getAsJsonObject(), "drop"));
		assertThrows(IllegalArgumentException.class, () -> assertion.bind(JsonParser.parseString("{\"display\":\"true\"}").getAsJsonObject(), "assert"));
	}

	@Test
	void bindSettingsActionsRejectUnknownArgumentsAndRequireTypedPrimaryFamily() {
		ClickStorageSettingsAction click = new ClickStorageSettingsAction();
		AssertLinkedSettingsAction assertion = new AssertLinkedSettingsAction();
		RegressionActionInvocation clickArguments = click.bind(JsonParser.parseString("{}").getAsJsonObject(), "click");
		RegressionActionInvocation assertionArguments = assertion.bind(JsonParser.parseString("{\"backpackPrimary\":true}").getAsJsonObject(), "assert");
		assertNotNull(clickArguments);
		assertNotNull(assertionArguments);
		assertThrows(IllegalArgumentException.class, () -> click.bind(JsonParser.parseString("{\"unexpected\":1}").getAsJsonObject(), "click"));
		assertThrows(IllegalArgumentException.class, () -> assertion.bind(JsonParser.parseString("{}").getAsJsonObject(), "assert"));
		assertThrows(IllegalArgumentException.class,
				() -> assertion.bind(JsonParser.parseString("{\"backpackPrimary\":\"true\"}").getAsJsonObject(), "assert"));
	}
}
