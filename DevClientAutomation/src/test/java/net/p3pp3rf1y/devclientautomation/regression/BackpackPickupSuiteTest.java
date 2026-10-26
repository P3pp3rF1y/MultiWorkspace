package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.AssertInventoryCountAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.BoundRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.ItemEntityHandle;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionValueTypes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackPickupSuiteTest {
	@Test
	void parsePickupRetainsSixIndependentNaturalPickupPlansAndExactTypedIdentity() {
		RegressionSuite suite = RegressionCoordinator.loadSuites().get("backpack-pickup");

		assertEquals(
				List.of("basic-pickup-allows-diamond", "basic-pickup-blocks-redstone", "basic-pickup-full-backpack-falls-back-to-player",
						"advanced-pickup-allows-diamond", "advanced-pickup-blocks-redstone", "advanced-pickup-full-backpack-falls-back-to-player"),
				suite.tests().stream().map(RegressionTest::id).toList());
		for (RegressionTest test : suite.tests()) {
			RegressionPreflight.validateValues(test);
			assertTrue(test.requires().isEmpty());
			assertEquals("diamond-pickup-backpack", test.steps().getFirst().provenance().fixture());
			var spawn = test.steps().stream().filter(step -> step.id().equals("spawn")).findFirst().orElseThrow();
			var pickup = test.steps().stream().filter(step -> step.id().equals("natural-pickup")).findFirst().orElseThrow();
			assertEquals(RegressionValueTypes.ITEM_ENTITY, spawn.action().values().produces().getFirst().type());
			assertEquals(spawn.action().values().produces(), pickup.action().values().consumes());
			assertEquals("await.itemEntity", pickup.action().action().metadata().id());
			for (String id : List.of("settings", "backpack", "player")) {
				assertTrue(test.steps().stream().anyMatch(step -> step.id().equals(id) && step.action().action().metadata().id().startsWith("assert.")));
			}
		}
	}

	@Test
	void itemHandleRejectsWrongDimensionTypeItemAndCountWithoutEntitySearch() {
		ItemEntityHandle handle = new ItemEntityHandle(UUID.randomUUID(), "minecraft:overworld", "minecraft:item", "minecraft:diamond", 1);

		handle.validate("minecraft:overworld", "minecraft:item", "minecraft:diamond", 1);
		assertThrows(IllegalStateException.class, () -> handle.validate("minecraft:the_nether", "minecraft:item", "minecraft:diamond", 1));
		assertThrows(IllegalStateException.class, () -> handle.validate("minecraft:overworld", "minecraft:pig", "minecraft:diamond", 1));
		assertThrows(IllegalStateException.class, () -> handle.validate("minecraft:overworld", "minecraft:item", "minecraft:redstone", 1));
		assertThrows(IllegalStateException.class, () -> handle.validate("minecraft:overworld", "minecraft:item", "minecraft:diamond", 2));
	}

	@Test
	void bindInventoryRevisionTwoKeepsOldCountsAndStrictlyAddsClientPlayerAndFullness() {
		var action = new AssertInventoryCountAction();
		var old = (BoundRegressionAction<?>) action
				.bind(JsonParser.parseString("{\"target\":\"backpack\",\"item\":\"minecraft:diamond\",\"count\":0}").getAsJsonObject(), "old");
		var arguments = (AssertInventoryCountAction.Arguments) old.arguments();

		assertEquals("2", action.metadata().revision());
		assertEquals(false, arguments.client());
		assertEquals("", arguments.fullItem());
		for (String invalid : List.of("{\"target\":\"player\",\"item\":\"minecraft:diamond\",\"count\":0,\"fullItem\":\"minecraft:cobblestone\"}",
				"{\"target\":\"storage\",\"item\":\"minecraft:diamond\",\"count\":0,\"client\":true}",
				"{\"target\":\"player\",\"item\":\"minecraft:diamond\",\"count\":0,\"client\":\"true\"}")) {
			assertThrows(IllegalArgumentException.class, () -> action.bind(JsonParser.parseString(invalid).getAsJsonObject(), "invalid"));
		}
	}
}
