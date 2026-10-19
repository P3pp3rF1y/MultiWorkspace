package net.p3pp3rf1y.devclientautomation.regression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TierRecipeActionCatalogTest {
	@Test
	void tierRecipeProxiesAdvertiseTheProductionActionRevision() {
		RegressionActionRegistry actions = new RegressionActionRegistry();
		assertEquals("2", actions.require("assert.storageTierUpgradeRecipe").metadata().revision());
		assertEquals("2", actions.require("assert.movingStorageTierUpgradeRecipe").metadata().revision());
	}

	@Test
	void playerInventorySlotAdvertisesItsProductionActionRevision() {
		RegressionActionRegistry actions = new RegressionActionRegistry();
		assertEquals("2", actions.require("player.setInventorySlot").metadata().revision());
	}
}
