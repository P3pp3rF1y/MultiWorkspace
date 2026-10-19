package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
		assertCreateMetadata(mountStorage, "sophisticatedstoragecreateintegration");
		assertCreateMetadata(mountBackpack, "sophisticatedbackpackscreateintegration");
		assertCreateMetadata(disassembleBackpack, "sophisticatedbackpackscreateintegration");
		assertCreateMetadata(setMountedBackpackSlot, "sophisticatedbackpackscreateintegration");
		assertCreateMetadata(setMountedStorageSlot, "sophisticatedstoragecreateintegration");
		assertCreateMetadata(assertMountedStorage, "sophisticatedstoragecreateintegration");
		assertSame(mountStorage, bind(mountStorage, "{\"saveAs\":\"mounted\"}").action());
		assertSame(mountBackpack, bind(mountBackpack, "{\"saveAs\":\"mounted\"}").action());
		assertSame(disassembleBackpack, bind(disassembleBackpack, "{\"ref\":\"mounted\"}").action());
		assertSame(setMountedBackpackSlot, bind(setMountedBackpackSlot, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1}").action());
		assertSame(setMountedStorageSlot, bind(setMountedStorageSlot, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1}").action());
		assertSame(assertMountedStorage, bind(assertMountedStorage, "{\"ref\":\"mounted\"}").action());
		assertSame(assertMountedStorage, bind(assertMountedStorage, "{\"ref\":\"mounted\",\"count\":0,\"checkPresentation\":false}").action());
		assertInvalid(mountStorage, "{}");
		assertInvalid(mountStorage, "{\"saveAs\":1}");
		assertInvalidValueNames(mountStorage, "saveAs", "");
		assertInvalid(mountStorage, "{\"saveAs\":\"mounted\",\"extra\":true}");
		assertInvalid(mountBackpack, "{}");
		assertInvalid(mountBackpack, "{\"saveAs\":1}");
		assertInvalidValueNames(mountBackpack, "saveAs", "");
		assertInvalid(mountBackpack, "{\"saveAs\":\"mounted\",\"extra\":true}");
		assertInvalid(disassembleBackpack, "{}");
		assertInvalid(disassembleBackpack, "{\"ref\":1}");
		assertInvalidValueNames(disassembleBackpack, "ref", "");
		assertInvalid(disassembleBackpack, "{\"ref\":\"mounted\",\"extra\":true}");
		assertSlotSchemaIsStrict(setMountedBackpackSlot);
		assertSlotSchemaIsStrict(setMountedStorageSlot);
		assertInvalid(assertMountedStorage, "{}");
		assertInvalid(assertMountedStorage, "{\"ref\":1}");
		assertInvalidValueNames(assertMountedStorage, "ref", "");
		assertInvalid(assertMountedStorage, "{\"ref\":\"mounted\",\"count\":1.5}");
		assertInvalid(assertMountedStorage, "{\"ref\":\"mounted\",\"count\":-1}");
		assertInvalid(assertMountedStorage, "{\"ref\":\"mounted\",\"checkPresentation\":\"false\"}");
		assertInvalid(assertMountedStorage, "{\"ref\":\"mounted\",\"extra\":true}");
	}

	private static void assertCreateMetadata(RegressionAction action, String capability) {
		assertEquals("1", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", capability), action.metadata().dependencies());
	}

	private static void assertSlotSchemaIsStrict(RegressionAction action) {
		assertInvalid(action, "{}");
		assertInvalid(action, "{\"item\":\"minecraft:emerald\",\"count\":1}");
		assertInvalid(action, "{\"ref\":\"mounted\",\"count\":1}");
		assertInvalid(action, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\"}");
		assertInvalid(action, "{\"ref\":1,\"item\":\"minecraft:emerald\",\"count\":1}");
		assertInvalidValueNames(action, "ref", ",\"item\":\"minecraft:emerald\",\"count\":1");
		assertInvalid(action, "{\"ref\":\"mounted\",\"item\":1,\"count\":1}");
		assertInvalid(action, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":\"1\"}");
		assertInvalid(action, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1.5}");
		assertInvalid(action, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":0}");
		assertInvalid(action, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1,\"extra\":true}");
	}

	private static void assertInvalidValueNames(RegressionAction action, String name, String suffix) {
		assertInvalid(action, "{\"" + name + "\":\"\"" + suffix + "}");
		assertInvalid(action, "{\"" + name + "\":\"Mounted\"" + suffix + "}");
		assertInvalid(action, "{\"" + name + "\":\"mounted.name\"" + suffix + "}");
	}

	private static RegressionActionInvocation bind(RegressionAction action, String json) {
		return action.bind(JsonParser.parseString(json).getAsJsonObject(), "test");
	}

	private static void assertInvalid(RegressionAction action, String json) {
		assertThrows(IllegalArgumentException.class, () -> bind(action, json));
	}
	@Test
	void mountedLinkedStorageActionsBindExactReferences() {
		assertEquals("mounted",
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new MountLinkedStorageAction(), "{\"saveAs\":\"mounted\"}"),
						MountLinkedStorageAction.Arguments.class).saveAs());
		AssertMountedLinkedStorageAction.Arguments assertion = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new AssertMountedLinkedStorageAction(), "{\"ref\":\"mounted\",\"count\":2,\"checkPresentation\":false}"),
				AssertMountedLinkedStorageAction.Arguments.class);
		assertEquals("mounted", assertion.ref());
		assertEquals(2, assertion.count());
		assertFalse(assertion.checkPresentation());
		assertEquals("mounted",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new SetMountedStorageSlotAction(), "{\"ref\":\"mounted\",\"item\":\"minecraft:diamond\",\"count\":1}"),
						SetMountedStorageSlotAction.Arguments.class).ref());
	}

	@Test
	void mountedLinkedStorageActionsRejectUnboundArguments() {
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new MountLinkedStorageAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new MountLinkedStorageAction(), "{\"saveAs\":\"mounted\",\"ref\":\"other\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertMountedLinkedStorageAction(), "{\"count\":1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMountedLinkedStorageAction(), "{\"ref\":\"mounted\",\"checkPresentation\":\"false\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetMountedStorageSlotAction(), "{\"ref\":\"mounted\",\"count\":1}"));
	}
}
