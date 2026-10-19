package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssertStorageTierUpgradeRecipeActionTest {
	@Test
	void doubleShapelessUsesTheRegisteredRecipeIdAndTwoNetheriteIngredients() {
		assertEquals("double_netherite_chest", StorageTierUpgradeRecipeContract.recipePath("double", "shapeless"));
		assertEquals(2, StorageTierUpgradeRecipeContract.requiredNetheriteIngots("double", "shapeless"));
		assertEquals("sophisticatedstorage:double_netherite_chest",
				AssertStorageTierUpgradeRecipeAction.recipeId(new AssertStorageTierUpgradeRecipeAction.Arguments("double", "shapeless", "primary")).toString());
	}
}
