package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferRecipeActionTest {
	@Test
	void bindUsesDefaultSingleTransfer() {
		TransferRecipeAction action = new TransferRecipeAction();

		var bound = action.bind(JsonParser.parseString("{\"recipeResourceID\":\"minecraft:stick\"}").getAsJsonObject(), "recipe transfer");

		assertNotNull(bound);
	}

	@Test
	void bindUsesExplicitMaxTransfer() {
		TransferRecipeAction action = new TransferRecipeAction();

		var bound = action.bind(JsonParser.parseString("{\"recipeResourceID\":\"minecraft:stick\",\"maxTransfer\":true}").getAsJsonObject(), "recipe transfer");

		assertNotNull(bound);
	}

	@Test
	void bindRejectsMalformedArguments() {
		TransferRecipeAction action = new TransferRecipeAction();

		Executable malformedResourceId = () -> action.bind(JsonParser.parseString("{\"recipeResourceID\":\"not a resource id\"}").getAsJsonObject(),
				"recipe transfer");
		Executable unknownArgument = () -> action.bind(JsonParser.parseString("{\"recipeResourceID\":\"minecraft:stick\",\"extra\":true}").getAsJsonObject(),
				"recipe transfer");

		assertThrows(IllegalArgumentException.class, malformedResourceId);
		assertThrows(IllegalArgumentException.class, unknownArgument);
	}
}
