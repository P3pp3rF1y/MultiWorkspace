package net.p3pp3rf1y.devclientautomation.regression.actions;

final class StorageTierUpgradeRecipeContract {
	private StorageTierUpgradeRecipeContract() {
	}

	static String recipePath(String form, String recipe) {
		return (form.equals("double") ? "double_" : "") + (recipe.equals("shaped") ? "diamond_chest" : "netherite_chest");
	}

	static int requiredNetheriteIngots(String form, String recipe) {
		return recipe.equals("shapeless") && form.equals("double") ? 2 : 1;
	}
}
