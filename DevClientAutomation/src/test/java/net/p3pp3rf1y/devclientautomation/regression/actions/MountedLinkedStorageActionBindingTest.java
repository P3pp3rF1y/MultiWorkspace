package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MountedLinkedStorageActionBindingTest {
	@Test
	void registeredCreateProxiesBindStrictSchemasWithoutLoadingImplementations() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionAction mountStorage = registry.require("fixture.create.mountLinkedStorage");
		RegressionAction mountBackpack = registry.require("fixture.create.mountBackpack");
		RegressionAction disassemble = registry.require("create.disassembleBackpack");
		RegressionAction backpackSlot = registry.require("fixture.create.setMountedBackpackSlot");
		RegressionAction storageSlot = registry.require("fixture.create.setMountedSlot");
		RegressionAction assertion = registry.require("assert.createMountedLinkedStorage");
		for (RegressionAction action : new RegressionAction[]{mountStorage, mountBackpack, disassemble, backpackSlot, storageSlot, assertion}) {
			assertInstanceOf(UnavailableCreateAction.class, action);
			assertEquals("1", action.metadata().revision());
			String capability = Set.of(mountBackpack, disassemble, backpackSlot).contains(action)
					? "sophisticatedbackpackscreateintegration"
					: "sophisticatedstoragecreateintegration";
			assertEquals(Set.of("minecraft:client", capability), action.metadata().dependencies());
		}
		for (RegressionAction action : new RegressionAction[]{mountStorage, mountBackpack}) {
			assertSame(action, bind(action, "{\"saveAs\":\"mounted\"}").action());
			assertInvalid(action, "{}");
			assertInvalid(action, "{\"saveAs\":1}");
			assertInvalidNames(action, "saveAs", "");
			assertInvalid(action, "{\"saveAs\":\"mounted\",\"extra\":true}");
		}
		assertSame(disassemble, bind(disassemble, "{\"ref\":\"mounted\"}").action());
		assertInvalid(disassemble, "{}");
		assertInvalid(disassemble, "{\"ref\":1}");
		assertInvalidNames(disassemble, "ref", "");
		assertInvalid(disassemble, "{\"ref\":\"mounted\",\"extra\":true}");
		for (RegressionAction action : new RegressionAction[]{backpackSlot, storageSlot}) {
			assertSame(action, bind(action, "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1}").action());
			for (String invalid : new String[]{"{}", "{\"item\":\"minecraft:emerald\",\"count\":1}", "{\"ref\":\"mounted\",\"count\":1}",
					"{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\"}", "{\"ref\":1,\"item\":\"minecraft:emerald\",\"count\":1}",
					"{\"ref\":\"mounted\",\"item\":1,\"count\":1}", "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":\"1\"}",
					"{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1.5}", "{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":0}",
					"{\"ref\":\"mounted\",\"item\":\"minecraft:emerald\",\"count\":1,\"extra\":true}"})
				assertInvalid(action, invalid);
			assertInvalidNames(action, "ref", ",\"item\":\"minecraft:emerald\",\"count\":1");
		}
		assertSame(assertion, bind(assertion, "{\"ref\":\"mounted\"}").action());
		assertSame(assertion, bind(assertion, "{\"ref\":\"mounted\",\"count\":0,\"checkPresentation\":false}").action());
		for (String invalid : new String[]{"{}", "{\"ref\":1}", "{\"ref\":\"mounted\",\"count\":1.5}", "{\"ref\":\"mounted\",\"count\":-1}",
				"{\"ref\":\"mounted\",\"checkPresentation\":\"false\"}", "{\"ref\":\"mounted\",\"extra\":true}"})
			assertInvalid(assertion, invalid);
		assertInvalidNames(assertion, "ref", "");
	}

	private static void assertInvalidNames(RegressionAction action, String name, String suffix) {
		for (String invalid : new String[]{"", "Mounted", "mounted.name"})
			assertInvalid(action, "{\"" + name + "\":\"" + invalid + "\"" + suffix + "}");
	}
	private static RegressionActionInvocation bind(RegressionAction action, String json) {
		return RegressionActionTestSupport.bind(action, json);
	}
	private static void assertInvalid(RegressionAction action, String json) {
		assertThrows(IllegalArgumentException.class, () -> bind(action, json), json);
	}
	@Test
	void mountedLinkedStorageActionsBindExactReferences() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertEquals("mounted",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(registry.require("fixture.create.mountLinkedStorage"), "{\"saveAs\":\"mounted\"}"),
								com.google.gson.JsonObject.class)
						.get("saveAs").getAsString());
		var assertion = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(registry.require("assert.createMountedLinkedStorage"),
				"{\"ref\":\"mounted\",\"count\":2,\"checkPresentation\":false}"), com.google.gson.JsonObject.class);
		assertEquals("mounted", assertion.get("ref").getAsString());
		assertEquals(2, assertion.get("count").getAsInt());
		assertEquals(false, assertion.get("checkPresentation").getAsBoolean());
		assertEquals("mounted",
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(registry.require("fixture.create.setMountedSlot"),
								"{\"ref\":\"mounted\",\"item\":\"minecraft:diamond\",\"count\":1}"), com.google.gson.JsonObject.class)
						.get("ref").getAsString());
	}

	@Test
	void mountedLinkedStorageActionsRejectUnboundArguments() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(registry.require("fixture.create.mountLinkedStorage"), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(registry.require("fixture.create.mountLinkedStorage"), "{\"saveAs\":\"mounted\",\"ref\":\"other\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(registry.require("assert.createMountedLinkedStorage"), "{\"count\":1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(registry.require("assert.createMountedLinkedStorage"),
				"{\"ref\":\"mounted\",\"checkPresentation\":\"false\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(registry.require("fixture.create.setMountedSlot"), "{\"ref\":\"mounted\",\"count\":1}"));
	}
}
