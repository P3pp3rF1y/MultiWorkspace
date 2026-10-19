package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MountedLinkedStorageActionBindingTest {
	@Test
	void registeredCreateProxiesBindStrictSchemasWithoutLoadingImplementations() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionAction mountStorage = registry.require("fixture.create.mountLinkedStorage");
		RegressionAction mountBackpack = registry.require("fixture.create.mountBackpack");
		RegressionAction disassembleBackpack = registry.require("create.disassembleBackpack");
		RegressionAction setMountedBackpackSlot = registry.require("fixture.create.setMountedBackpackSlot");
		RegressionAction setMountedStorageSlot = registry.require("fixture.create.setMountedSlot");
		RegressionAction assertMountedStorage = registry.require("assert.createMountedLinkedStorage");
		for (RegressionAction action : Set.of(mountStorage, setMountedStorageSlot, assertMountedStorage)) {
			assertCreateMetadata(action, "sophisticatedstoragecreateintegration");
		}
		for (RegressionAction action : Set.of(mountBackpack, disassembleBackpack, setMountedBackpackSlot)) {
			assertCreateMetadata(action, "sophisticatedbackpackscreateintegration");
		}
		assertSame(mountStorage, bind(mountStorage, "{\"saveAs\":\"mounted\"}").action());
		assertSame(mountBackpack, bind(mountBackpack, "{\"saveAs\":\"mounted\"}").action());
		assertSame(disassembleBackpack, bind(disassembleBackpack, "{\"ref\":\"mounted\"}").action());
		assertSame(setMountedBackpackSlot, bind(setMountedBackpackSlot, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1}").action());
		assertSame(setMountedStorageSlot, bind(setMountedStorageSlot, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1}").action());
		assertSame(assertMountedStorage, bind(assertMountedStorage, "{\"ref\":\"mounted\"}").action());
		assertSame(assertMountedStorage, bind(assertMountedStorage, "{\"ref\":\"mounted\",\"count\":0,\"checkPresentation\":false}").action());
		for (RegressionAction action : Set.of(mountStorage, mountBackpack)) {
			assertInvalid(action, "{}");
			assertInvalid(action, "{\"saveAs\":1}");
			assertInvalidValueNames(action, "saveAs", "");
			assertInvalid(action, "{\"saveAs\":\"mounted\",\"extra\":true}");
		}
		assertInvalid(disassembleBackpack, "{}");
		assertInvalid(disassembleBackpack, "{\"ref\":1}");
		assertInvalidValueNames(disassembleBackpack, "ref", "");
		assertInvalid(disassembleBackpack, "{\"ref\":\"mounted\",\"extra\":true}");
		assertSlotSchemaIsStrict(setMountedBackpackSlot);
		assertSlotSchemaIsStrict(setMountedStorageSlot);
		for (String json : Set.of("{}", "{\"ref\":1}", "{\"ref\":\"mounted\",\"count\":1.5}", "{\"ref\":\"mounted\",\"count\":-1}",
				"{\"ref\":\"mounted\",\"checkPresentation\":\"false\"}", "{\"ref\":\"mounted\",\"extra\":true}")) {
			assertInvalid(assertMountedStorage, json);
		}
		assertInvalidValueNames(assertMountedStorage, "ref", "");
	}

	private static void assertCreateMetadata(RegressionAction action, String capability) {
		assertEquals("1", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", capability), action.metadata().dependencies());
	}

	private static void assertSlotSchemaIsStrict(RegressionAction action) {
		for (String json : Set.of("{}", "{\"item\":\"minecraft:emerald\",\"count\":1}", "{\"ref\":\"mounted\",\"count\":1}",
				"{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\"}", "{\"ref\":1,\"item\":\"minecraft:emerald\",\"count\":1}",
				"{\"ref\":\"mounted\",\"item\":1,\"count\":1}", "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":\"1\"}",
				"{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1.5}", "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":0}",
				"{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1,\"extra\":true}")) {
			assertInvalid(action, json);
		}
		assertInvalidValueNames(action, "ref", ",\"item\":\"minecraft:emerald\",\"count\":1");
	}

	private static void assertInvalidValueNames(RegressionAction action, String name, String suffix) {
		for (String value : Set.of("", "Mounted", "mounted.name")) {
			assertInvalid(action, "{\"" + name + "\":\"" + value + "\"" + suffix + "}");
		}
	}

	private static RegressionActionInvocation bind(RegressionAction action, String json) {
		return action.bind(JsonParser.parseString(json).getAsJsonObject(), "test");
	}

	private static void assertInvalid(RegressionAction action, String json) {
		assertThrows(IllegalArgumentException.class, () -> bind(action, json));
	}
}
