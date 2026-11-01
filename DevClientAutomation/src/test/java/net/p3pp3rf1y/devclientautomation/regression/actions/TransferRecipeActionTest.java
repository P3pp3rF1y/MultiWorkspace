package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferRecipeActionTest {
	@Test
	void bindUsesDefaultSingleTransfer() {
		var action = new RegressionActionRegistry().require("recipeviewer.transferRecipe");

		var bound = action.bind(JsonParser.parseString("{\"recipeResourceID\":\"minecraft:stick\"}").getAsJsonObject(), "recipe transfer");

		assertNotNull(bound);
		assertSame(action, bound.action());
	}

	@Test
	void bindUsesExplicitMaxTransfer() {
		var action = new RegressionActionRegistry().require("recipeviewer.transferRecipe");

		var bound = action.bind(JsonParser.parseString("{\"recipeResourceID\":\"minecraft:stick\",\"maxTransfer\":true}").getAsJsonObject(), "recipe transfer");

		assertNotNull(bound);
		assertSame(action, bound.action());
	}

	@Test
	void bindRejectsMalformedArguments() {
		var action = new RegressionActionRegistry().require("recipeviewer.transferRecipe");

		Executable malformedResourceId = () -> action.bind(JsonParser.parseString("{\"recipeResourceID\":\"not a resource id\"}").getAsJsonObject(),
				"recipe transfer");
		Executable invalidResourceType = () -> action.bind(JsonParser.parseString("{\"recipeResourceID\":3}").getAsJsonObject(), "recipe transfer");
		Executable unknownArgument = () -> action.bind(JsonParser.parseString("{\"recipeResourceID\":\"minecraft:stick\",\"extra\":true}").getAsJsonObject(),
				"recipe transfer");
		Executable invalidMaxTransfer = () -> action
				.bind(JsonParser.parseString("{\"recipeResourceID\":\"minecraft:stick\",\"maxTransfer\":1}").getAsJsonObject(), "recipe transfer");

		assertThrows(IllegalArgumentException.class, malformedResourceId);
		assertThrows(IllegalArgumentException.class, invalidResourceType);
		assertThrows(IllegalArgumentException.class, unknownArgument);
		assertThrows(IllegalArgumentException.class, invalidMaxTransfer);
	}
}
