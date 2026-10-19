package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.BoundRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.LazyRegressionAction;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionInvocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MovingStorageActionRegistryTest {
	@Test
	void bindsNestedIdentityAndTierRecipeActionsByIndividualId() {
		RegressionActionRegistry registry = new RegressionActionRegistry();

		assertBound(registry, "fixture.moving.captureNestedLinkedCarrier", "{\"ref\":\"primary\",\"saveAs\":\"nested\"}");
		assertBound(registry, "assert.movingNestedLinkedIdentity", "{\"ref\":\"nested\"}");
		assertLazyBound(registry, "assert.storageTierUpgradeRecipe", "{\"form\":\"normal\",\"recipe\":\"shaped\",\"linkage\":\"unlinked\"}");
		assertLazyBound(registry, "assert.movingStorageTierUpgradeRecipe", "{\"carrier\":\"boat\",\"recipe\":\"shaped\",\"linkage\":\"unlinked\"}");
	}

	private static void assertBound(RegressionActionRegistry registry, String id, String arguments) {
		assertInstanceOf(BoundRegressionAction.class, registry.require(id).bind(JsonParser.parseString(arguments).getAsJsonObject(), "test action"));
	}

	private static void assertLazyBound(RegressionActionRegistry registry, String id, String arguments) {
		RegressionActionInvocation invocation = registry.require(id).bind(JsonParser.parseString(arguments).getAsJsonObject(), "test action");
		assertInstanceOf(LazyRegressionAction.class, invocation.action());
	}
}
