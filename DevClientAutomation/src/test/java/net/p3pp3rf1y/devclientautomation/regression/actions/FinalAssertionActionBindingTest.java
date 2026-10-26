package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinalAssertionActionBindingTest {
	private final RegressionActionRegistry registry = new RegressionActionRegistry();

	private Object bind(String id, String json) {
		return ((BoundRegressionAction<?>) RegressionActionTestSupport.bind(registry.require(id), json)).arguments();
	}

	@Test
	void canonicalAndNativeChestAssertions() {
		assertEquals(new AssertCanonicalBackpackTemplateAction.Arguments("sophisticatedbackpacks:backpack", "canonical"),
				bind("assert.canonicalBackpackTemplate", "{\"template\":\"sophisticatedbackpacks:backpack\"}"));
		assertEquals(new AssertCanonicalBackpackTemplateAction.Arguments("sophisticatedbackpacks:backpack", "absent"),
				bind("assert.canonicalBackpackTemplate", "{\"template\":\"sophisticatedbackpacks:backpack\",\"expectation\":\"absent\"}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.canonicalBackpackTemplate", "{\"template\":\"backpack\"}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.canonicalBackpackTemplate", "{\"template\":\"a:b\",\"expectation\":\"other\"}"));
		assertEquals(new AssertCanonicalCapturedMobsAction.Arguments(0, false), bind("assert.canonicalCapturedMobs", "{}"));
		assertEquals(new AssertCanonicalCapturedMobsAction.Arguments(2, true), bind("assert.canonicalCapturedMobs", "{\"count\":2,\"render\":true}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.canonicalCapturedMobs", "{\"render\":1}"));
		assertEquals(new AssertDoubleChestAction.Arguments(false), bind("assert.doubleChest", "{}"));
		assertEquals(new AssertDoubleChestAction.Arguments(true), bind("assert.doubleChest", "{\"controller\":true}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.doubleChest", "{\"controller\":1}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.doubleChest", "{\"count\":1}"));
		RegressionActionTestSupport.assertMetadata(registry.require("assert.canonicalBackpackTemplate"), "assert.canonicalBackpackTemplate",
				Set.of("minecraft:client", "sophisticatedbackpacks"));
		RegressionActionTestSupport.assertMetadata(registry.require("assert.canonicalCapturedMobs"), "assert.canonicalCapturedMobs",
				Set.of("minecraft:client", "sophisticatedbackpacks"));
		assertEquals("2", registry.require("assert.doubleChest").metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage"), registry.require("assert.doubleChest").metadata().dependencies());
	}

	@Test
	void linkedHostItemAndSettingsAssertions() {
		assertEquals(new AssertLinkedBackpackHostAction.Arguments(1, false, 0), bind("assert.linkedBackpackHost", "{}"));
		assertEquals(new AssertLinkedBackpackHostAction.Arguments(27, true, 2),
				bind("assert.linkedBackpackHost", "{\"minimumSlots\":27,\"placed\":true,\"offsetZ\":2}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.linkedBackpackHost", "{\"minimumSlots\":0}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.linkedBackpackHost", "{\"placed\":1}"));
		assertEquals(new AssertLinkedItemContentsAction.Arguments(0, 0), bind("assert.linkedItemContents", "{}"));
		assertEquals(new AssertLinkedItemContentsAction.Arguments(35, 12), bind("assert.linkedItemContents", "{\"slot\":35,\"count\":12}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.linkedItemContents", "{\"slot\":36}"));
		assertEquals(new AssertLinkedSettingsAction.Arguments(true), bind("assert.linkedSettings", "{\"backpackPrimary\":true}"));
		assertEquals(new AssertLinkedSettingsAction.Arguments(false), bind("assert.linkedSettings", "{\"backpackPrimary\":false}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.linkedSettings", "{}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.linkedSettings", "{\"backpackPrimary\":1}"));
		RegressionActionTestSupport.assertMetadata(registry.require("assert.linkedBackpackHost"), "assert.linkedBackpackHost",
				Set.of("minecraft:client", "sophisticatedbackpacks"));
		RegressionActionTestSupport.assertMetadata(registry.require("assert.linkedItemContents"), "assert.linkedItemContents",
				Set.of("minecraft:client", "sophisticatedcore"));
		RegressionActionTestSupport.assertMetadata(registry.require("assert.linkedSettings"), "assert.linkedSettings",
				Set.of("minecraft:client", "sophisticatedbackpacks", "sophisticatedcore"));
	}

	@Test
	void upgradeLayoutAndSelectionAssertions() {
		assertEquals(new AssertMenuUpgradeLayoutAction.Arguments(0, 0, false), bind("assert.menuUpgradeLayout", "{}"));
		assertEquals(new AssertMenuUpgradeLayoutAction.Arguments(2, 3, true),
				bind("assert.menuUpgradeLayout", "{\"columns\":2,\"controls\":3,\"contents\":true}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.menuUpgradeLayout", "{\"columns\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.menuUpgradeLayout", "{\"contents\":1}"));
		assertEquals(new AssertSettingsSelectionAction.Arguments(0, false), bind("assert.settingsSelection", "{\"display\":false}"));
		assertEquals(new AssertSettingsSelectionAction.Arguments(4, true), bind("assert.settingsSelection", "{\"slot\":4,\"display\":true}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.settingsSelection", "{}"));
		assertThrows(IllegalArgumentException.class, () -> bind("assert.settingsSelection", "{\"display\":true,\"slot\":-1}"));
		RegressionActionTestSupport.assertMetadata(registry.require("assert.menuUpgradeLayout"), "assert.menuUpgradeLayout",
				Set.of("minecraft:client", "sophisticatedcore"));
		assertEquals("2", registry.require("assert.settingsSelection").metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedcore"), registry.require("assert.settingsSelection").metadata().dependencies());
	}
}
