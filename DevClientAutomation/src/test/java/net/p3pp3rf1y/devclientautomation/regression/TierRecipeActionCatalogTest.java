package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TierRecipeActionCatalogTest {
	@Test
	void playerInventorySlotAdvertisesItsProductionActionRevision() {
		RegressionActionRegistry actions = new RegressionActionRegistry();
		assertEquals("2", actions.require("player.setInventorySlot").metadata().revision());
	}
	@Test
	void tierRecipeProxiesAdvertiseTheProductionActionRevision() {
		RegressionActionRegistry actions = new RegressionActionRegistry();
		assertEquals("2", actions.require("assert.storageTierUpgradeRecipe").metadata().revision());
		assertEquals("2", actions.require("assert.movingStorageTierUpgradeRecipe").metadata().revision());
		assertEquals("2", actions.require("player.setInventorySlot").metadata().revision());
	}

	@Test
	void tierRecipeProxiesValidateArgumentsBeforeLoadingRecipeClasses() {
		RegressionActionRegistry actions = new RegressionActionRegistry();
		assertThrows(IllegalArgumentException.class, () -> actions.require("assert.storageTierUpgradeRecipe")
				.bind(JsonParser.parseString("{\"form\":\"single\",\"recipe\":\"shaped\",\"linkage\":\"primary\"}").getAsJsonObject(), "tier test"));
		assertThrows(IllegalArgumentException.class, () -> actions.require("assert.movingStorageTierUpgradeRecipe")
				.bind(JsonParser.parseString("{\"carrier\":\"boat\",\"recipe\":\"shapeless\",\"linkage\":\"invalid\"}").getAsJsonObject(), "tier test"));
	}
}
