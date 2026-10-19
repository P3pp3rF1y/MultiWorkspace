package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TierUpgradeRecipeActionBindingTest {
	@Test
	void storageRecipeProxyBindsEveryProductionDomainAndRejectsInvalidSchemas() {
		RegressionAction action = new RegressionActionRegistry().require("assert.storageTierUpgradeRecipe");
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage"), action.metadata().dependencies());
		for (String form : Set.of("normal", "double")) {
			for (String recipe : Set.of("shaped", "shapeless")) {
				for (String linkage : Set.of("unlinked", "primary", "secondary")) {
					assertSame(action, bind(action, "{\"form\":\"" + form + "\",\"recipe\":\"" + recipe + "\",\"linkage\":\"" + linkage + "\"}").action());
				}
			}
		}
		assertInvalid(action, "{}");
		assertInvalid(action, "{\"form\":\"normal\",\"recipe\":\"shaped\"}");
		assertInvalid(action, "{\"form\":\"normal\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"recipe\":\"shaped\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"form\":true,\"recipe\":\"shaped\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"form\":\"normal\",\"recipe\":true,\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"form\":\"normal\",\"recipe\":\"shaped\",\"linkage\":true}");
		assertInvalid(action, "{\"form\":\"single\",\"recipe\":\"shaped\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"form\":\"normal\",\"recipe\":\"grid\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"form\":\"normal\",\"recipe\":\"shaped\",\"linkage\":\"linked\"}");
		assertInvalid(action, "{\"form\":\"normal\",\"recipe\":\"shaped\",\"linkage\":\"primary\",\"extra\":1}");
	}

	@Test
	void movingRecipeProxyBindsEveryProductionDomainAndRejectsInvalidSchemas() {
		RegressionAction action = new RegressionActionRegistry().require("assert.movingStorageTierUpgradeRecipe");
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"), action.metadata().dependencies());
		for (String carrier : Set.of("boat", "minecart")) {
			for (String recipe : Set.of("shaped", "shapeless")) {
				for (String linkage : Set.of("unlinked", "primary", "secondary")) {
					assertDoesNotThrow(() -> bind(action, "{\"carrier\":\"" + carrier + "\",\"recipe\":\"" + recipe + "\",\"linkage\":\"" + linkage + "\"}"));
				}
			}
		}
		assertInvalid(action, "{}");
		assertInvalid(action, "{\"carrier\":\"boat\",\"recipe\":\"shaped\"}");
		assertInvalid(action, "{\"carrier\":\"boat\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"recipe\":\"shaped\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"carrier\":true,\"recipe\":\"shaped\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"carrier\":\"boat\",\"recipe\":true,\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"carrier\":\"boat\",\"recipe\":\"shaped\",\"linkage\":true}");
		assertInvalid(action, "{\"carrier\":\"horse\",\"recipe\":\"shaped\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"carrier\":\"boat\",\"recipe\":\"grid\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"carrier\":\"boat\",\"recipe\":\"shaped\",\"linkage\":\"linked\"}");
		assertInvalid(action, "{\"carrier\":\"boat\",\"recipe\":\"shaped\",\"linkage\":\"primary\",\"extra\":1}");
	}

	private static RegressionActionInvocation bind(RegressionAction action, String json) {
		return action.bind(JsonParser.parseString(json).getAsJsonObject(), "test");
	}

	private static void assertInvalid(RegressionAction action, String json) {
		assertThrows(IllegalArgumentException.class, () -> bind(action, json));
	}
	@Test
	void storageRecipeArgumentsAreStrictAndTyped() {
		AssertStorageTierUpgradeRecipeAction.Arguments arguments = RegressionActionTestSupport
				.arguments(
						RegressionActionTestSupport.bind(new AssertStorageTierUpgradeRecipeAction(),
								"{\"form\":\"double\",\"recipe\":\"shapeless\",\"linkage\":\"secondary\"}"),
						AssertStorageTierUpgradeRecipeAction.Arguments.class);
		assertEquals("double", arguments.form());
		assertEquals("shapeless", arguments.recipe());
		assertEquals("secondary", arguments.linkage());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertStorageTierUpgradeRecipeAction(),
				"{\"form\":\"single\",\"recipe\":\"shaped\",\"linkage\":\"unlinked\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertStorageTierUpgradeRecipeAction(),
				"{\"form\":\"normal\",\"recipe\":\"shaped\",\"linkage\":true}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertStorageTierUpgradeRecipeAction(),
				"{\"form\":\"normal\",\"recipe\":\"shaped\",\"linkage\":\"unlinked\",\"extra\":1}"));
	}

	@Test
	void movingRecipeArgumentsAreStrictAndTyped() {
		AssertMovingStorageTierUpgradeRecipeAction.Arguments arguments = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new AssertMovingStorageTierUpgradeRecipeAction(),
						"{\"carrier\":\"minecart\",\"recipe\":\"shaped\",\"linkage\":\"primary\"}"),
				AssertMovingStorageTierUpgradeRecipeAction.Arguments.class);
		assertEquals("minecart", arguments.carrier());
		assertEquals("shaped", arguments.recipe());
		assertEquals("primary", arguments.linkage());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertMovingStorageTierUpgradeRecipeAction(),
				"{\"carrier\":\"horse\",\"recipe\":\"shaped\",\"linkage\":\"unlinked\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertMovingStorageTierUpgradeRecipeAction(),
				"{\"carrier\":\"boat\",\"recipe\":\"grid\",\"linkage\":\"unlinked\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertMovingStorageTierUpgradeRecipeAction(), "{\"carrier\":\"boat\",\"recipe\":\"shaped\"}"));
	}
}
