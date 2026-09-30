package net.p3pp3rf1y.devclientautomation.regression;

import net.p3pp3rf1y.devclientautomation.regression.actions.BoundRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.CreateBackpackInMainHandAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.SetBackpackStorageSlotAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionSuiteParserTest {
	private static final String VALID_SUITE = """
			{
			  "schemaVersion": 2,
			  "suiteId": "test-suite",
			  "requires": ["minecraft:client"],
			  "isolation": "testCell",
			  "tests": [{
			    "id": "test-case",
			    "tags": ["fast"],
			    "timeoutTicks": 20,
			    "steps": [{"id": "reset", "action": "fixture.region.reset"}]
			  }]
			}
			""";

	@Test
	void parsesSchemaV2WithStableSuiteAndTestIds() {
		RegressionSuite suite = RegressionSuiteParser.parse(VALID_SUITE, new RegressionActionRegistry());

		assertEquals(2, suite.schemaVersion());
		assertEquals("test-suite", suite.suiteId());
		assertEquals("test-case", suite.tests().getFirst().id());
		assertTrue(suite.tests().getFirst().prepareTestCell());
		assertEquals("fixture.region.reset", suite.tests().getFirst().steps().getFirst().action().action().metadata().id());
	}

	@Test
	void supportsTestCellPreparationOptOut() {
		String suiteJson = VALID_SUITE.replace("\"timeoutTicks\": 20", "\"timeoutTicks\": 20, \"prepareTestCell\": false");

		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, new RegressionActionRegistry());

		assertFalse(suite.tests().getFirst().prepareTestCell());
	}

	@Test
	void registersTheDefaultTestCellSetup() {
		RegressionActionRegistry registry = new RegressionActionRegistry();

		assertEquals(List.of("fixture.region.reset", "player.reset"),
				registry.testCellSetup().stream().map(step -> step.action().action().metadata().id()).toList());
	}

	@Test
	void bindsNamedArgumentsToTheActionInvocation() {
		String suiteJson = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"create\", \"action\": \"backpack.createInMainHand\", \"args\": {\"tier\": \"diamond\"}");

		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, registry);
		BoundRegressionAction<?> invocation = assertInstanceOf(BoundRegressionAction.class, suite.tests().getFirst().steps().getFirst().action());
		CreateBackpackInMainHandAction.Arguments arguments = assertInstanceOf(CreateBackpackInMainHandAction.Arguments.class, invocation.arguments());

		assertSame(registry.require("backpack.createInMainHand"), invocation.action());
		assertEquals("diamond", arguments.tier());
	}

	@Test
	void bindsEachBackpackStorageSlotIndependently() {
		String suiteJson = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"set-slot\", \"action\": \"backpack.setStorageSlot\", \"args\": {\"slot\": 2, \"item\": \"minecraft:emerald\", \"count\": 3}");

		RegressionSuite suite = RegressionSuiteParser.parse(suiteJson, new RegressionActionRegistry());
		BoundRegressionAction<?> invocation = assertInstanceOf(BoundRegressionAction.class, suite.tests().getFirst().steps().getFirst().action());
		SetBackpackStorageSlotAction.Arguments arguments = assertInstanceOf(SetBackpackStorageSlotAction.Arguments.class, invocation.arguments());

		assertEquals(2, arguments.slot());
		assertEquals("minecraft:emerald", arguments.item());
		assertEquals(3, arguments.count());
	}

	@Test
	void rejectsUnsupportedSchemaBeforeExecution() {
		String invalid = VALID_SUITE.replace("\"schemaVersion\": 2", "\"schemaVersion\": 3");

		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(invalid, new RegressionActionRegistry()));

		assertEquals("Unsupported regression schemaVersion 3; expected 2", error.getMessage());
	}

	@Test
	void rejectsDuplicateTestIds() {
		String invalid = """
				{
				  "schemaVersion": 2,
				  "suiteId": "test-suite",
				  "requires": ["minecraft:client"],
				  "isolation": "testCell",
				  "tests": [
				    {"id": "test-case", "tags": [], "timeoutTicks": 1, "steps": [{"id": "reset", "action": "fixture.region.reset"}]},
				    {"id": "test-case", "tags": [], "timeoutTicks": 1, "steps": [{"id": "reset", "action": "fixture.region.reset"}]}
				  ]
				}
				""";

		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(invalid, new RegressionActionRegistry()));

		assertEquals("Suite test-suite contains duplicate test id test-case", error.getMessage());
	}

	@Test
	void rejectsUnknownActionsAndWrongArgumentTypes() {
		String unknownAction = VALID_SUITE.replace("fixture.region.reset", "fixture.unknown");
		String badArgument = VALID_SUITE.replace("\"action\": \"fixture.region.reset\"",
				"\"action\": \"fixture.region.reset\", \"args\": {\"unexpected\": true}");

		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(unknownAction, new RegressionActionRegistry()));
		assertThrows(IllegalArgumentException.class, () -> RegressionSuiteParser.parse(badArgument, new RegressionActionRegistry()));
	}

	@Test
	void rejectsUnsupportedBackpackTiersBeforeExecution() {
		String invalidTier = VALID_SUITE.replace("\"id\": \"reset\", \"action\": \"fixture.region.reset\"",
				"\"id\": \"create\", \"action\": \"backpack.createInMainHand\", \"args\": {\"tier\": \"obsidian\"}");

		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> RegressionSuiteParser.parse(invalidTier, new RegressionActionRegistry()));

		assertEquals("Argument tier on step create must be one of leather, copper, iron, gold, diamond, netherite", error.getMessage());
	}
}
