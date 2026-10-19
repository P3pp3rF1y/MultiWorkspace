package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InteroperabilityActionContractTest {
	@Test
	void bindBackpackUpgradeFixtureRequiresTypedEnabledStateAndInventorySlot() {
		assertDoesNotThrow(() -> new BreakTargetBlockAction().bind(JsonParser.parseString("{}").getAsJsonObject(), "break"));
		assertThrows(IllegalArgumentException.class,
				() -> new BreakTargetBlockAction().bind(JsonParser.parseString("{\"unknown\":true}").getAsJsonObject(), "break"));
		assertDoesNotThrow(() -> new CopyPlayerInventoryItemAction().bind(JsonParser.parseString("{\"source\":0,\"target\":10}").getAsJsonObject(), "copy"));
		assertThrows(IllegalArgumentException.class,
				() -> new CopyPlayerInventoryItemAction().bind(JsonParser.parseString("{\"source\":10,\"target\":10}").getAsJsonObject(), "copy"));
		assertDoesNotThrow(() -> new SetHeldBackpackUpgradeAction().bind(
				JsonParser.parseString("{\"item\":\"sophisticatedbackpacks:inception_upgrade\",\"inventorySlot\":10,\"enabled\":false}").getAsJsonObject(),
				"upgrade"));
		assertThrows(IllegalArgumentException.class, () -> new SetHeldBackpackUpgradeAction()
				.bind(JsonParser.parseString("{\"item\":\"test:upgrade\",\"inventorySlot\":36}").getAsJsonObject(), "upgrade"));
		assertThrows(IllegalArgumentException.class, () -> new SetHeldBackpackUpgradeAction()
				.bind(JsonParser.parseString("{\"item\":\"test:upgrade\",\"enabled\":\"false\"}").getAsJsonObject(), "upgrade"));
	}

	@Test
	void bindMobInteractionsRequireReferencesAndRejectInvalidCounts() {
		assertDoesNotThrow(() -> new SpawnFixturePigAction().bind(JsonParser.parseString("{\"saveAs\":\"pig\"}").getAsJsonObject(), "pig"));
		assertDoesNotThrow(() -> new UseFixtureEntityAction().bind(JsonParser.parseString("{\"ref\":\"pig\"}").getAsJsonObject(), "use"));
		assertDoesNotThrow(
				() -> new AssertCanonicalCapturedMobsAction().bind(JsonParser.parseString("{\"count\":1,\"render\":true}").getAsJsonObject(), "assert"));
		assertThrows(IllegalArgumentException.class,
				() -> new AssertCanonicalCapturedMobsAction().bind(JsonParser.parseString("{\"render\":\"true\"}").getAsJsonObject(), "assert"));
		assertThrows(IllegalArgumentException.class, () -> new SpawnFixturePigAction().bind(JsonParser.parseString("{}").getAsJsonObject(), "pig"));
		assertThrows(IllegalArgumentException.class, () -> new UseFixtureEntityAction().bind(JsonParser.parseString("{\"ref\":1}").getAsJsonObject(), "use"));
		assertThrows(IllegalArgumentException.class,
				() -> new AssertCanonicalCapturedMobsAction().bind(JsonParser.parseString("{\"count\":-1}").getAsJsonObject(), "assert"));
		assertThrows(IllegalArgumentException.class,
				() -> new ClickCapturedMobAction().bind(JsonParser.parseString("{\"index\":-1}").getAsJsonObject(), "click"));
	}

	@Test
	void bindTemplateCommandsRejectMalformedNamesAndUnknownExpectations() {
		assertDoesNotThrow(
				() -> new SendPlayerCommandAction().bind(JsonParser.parseString("{\"command\":\"sbp template list\"}").getAsJsonObject(), "command"));
		assertDoesNotThrow(
				() -> new AssertCanonicalBackpackTemplateAction().bind(JsonParser.parseString("{\"template\":\"test:template\"}").getAsJsonObject(), "assert"));
		assertThrows(IllegalArgumentException.class,
				() -> new AssertCanonicalBackpackTemplateAction().bind(JsonParser.parseString("{\"template\":\"Invalid Name\"}").getAsJsonObject(), "assert"));
		assertThrows(IllegalArgumentException.class, () -> new AssertCanonicalBackpackTemplateAction()
				.bind(JsonParser.parseString("{\"template\":\"test:template\",\"expectation\":\"other\"}").getAsJsonObject(), "assert"));
	}
}
