package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.LazyRegressionAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MountedLinkedStorageActionBindingTest {
	@Test
	void bindsMountedActionsWithSourceSchemasWithoutLinkingUnavailableCreateClasses() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertInstanceOf(LazyRegressionAction.class, registry.require("fixture.create.mountLinkedStorage")
				.bind(JsonParser.parseString("{\"saveAs\":\"mounted\"}").getAsJsonObject(), "test").action());
		assertInstanceOf(LazyRegressionAction.class, registry.require("fixture.create.setMountedSlot")
				.bind(JsonParser.parseString("{\"ref\":\"mounted\",\"item\":\"minecraft:diamond\",\"count\":1}").getAsJsonObject(), "test").action());
		assertInstanceOf(LazyRegressionAction.class, registry.require("assert.createMountedLinkedStorage")
				.bind(JsonParser.parseString("{\"ref\":\"mounted\",\"count\":1,\"checkPresentation\":false}").getAsJsonObject(), "test").action());
	}

	@Test
	void mountedAssertionRetainsSourceDefaultsWithoutCreateLoading() {
		RegressionActionRegistry registry = new RegressionActionRegistry();

		assertInstanceOf(LazyRegressionAction.class,
				registry.require("assert.createMountedLinkedStorage").bind(JsonParser.parseString("{\"ref\":\"mounted\"}").getAsJsonObject(), "test").action());
	}

	@Test
	void mountedActionSchemasRejectInvalidSourceArgumentsBeforeCreateLoading() {
		RegressionActionRegistry registry = new RegressionActionRegistry();

		assertThrows(IllegalArgumentException.class,
				() -> registry.require("fixture.create.mountLinkedStorage").bind(JsonParser.parseString("{\"saveAs\":\"Mounted\"}").getAsJsonObject(), "test"));
		assertThrows(IllegalArgumentException.class, () -> registry.require("fixture.create.setMountedSlot")
				.bind(JsonParser.parseString("{\"ref\":\"mounted\",\"item\":\"minecraft:diamond\",\"count\":0}").getAsJsonObject(), "test"));
		assertThrows(IllegalArgumentException.class, () -> registry.require("assert.createMountedLinkedStorage")
				.bind(JsonParser.parseString("{\"ref\":\"mounted\",\"checkPresentation\":\"false\"}").getAsJsonObject(), "test"));
	}

	@Test
	void mountedActionSchemasRejectUnexpectedSourceArgumentsBeforeCreateLoading() {
		RegressionActionRegistry registry = new RegressionActionRegistry();

		assertThrows(IllegalArgumentException.class, () -> registry.require("fixture.create.mountLinkedStorage")
				.bind(JsonParser.parseString("{\"saveAs\":\"mounted\",\"extra\":true}").getAsJsonObject(), "test"));
	}
}
