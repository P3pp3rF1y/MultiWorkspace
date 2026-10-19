package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TierUpgradeRecipeActionBindingTest {
	@Test
	void bindStorageRecipeProxyValidatesWithoutLoadingItsImplementation() {
		RegressionAction action = new RegressionActionRegistry().require("assert.storageTierUpgradeRecipe");
		assertInstanceOf(LazyOptionalRegressionAction.class, action);
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage"), action.metadata().dependencies());
		for (String form : Set.of("normal", "double"))
			for (String recipe : Set.of("shaped", "shapeless"))
				for (String linkage : Set.of("unlinked", "primary", "secondary"))
					assertSame(action, bind(action, "{\"form\":\"" + form + "\",\"recipe\":\"" + recipe + "\",\"linkage\":\"" + linkage + "\"}").action());
		assertSchemaRejectsInvalid(action, "form", "normal", "single");
	}

	@Test
	void bindMovingRecipeProxyValidatesWithoutLoadingItsImplementation() {
		RegressionAction action = new RegressionActionRegistry().require("assert.movingStorageTierUpgradeRecipe");
		assertInstanceOf(LazyOptionalRegressionAction.class, action);
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"), action.metadata().dependencies());
		for (String carrier : Set.of("boat", "minecart"))
			for (String recipe : Set.of("shaped", "shapeless"))
				for (String linkage : Set.of("unlinked", "primary", "secondary"))
					assertSame(action,
							bind(action, "{\"carrier\":\"" + carrier + "\",\"recipe\":\"" + recipe + "\",\"linkage\":\"" + linkage + "\"}").action());
		assertSchemaRejectsInvalid(action, "carrier", "boat", "horse");
	}

	private static void assertSchemaRejectsInvalid(RegressionAction action, String field, String valid, String invalid) {
		String full = "{\"" + field + "\":\"" + valid + "\",\"recipe\":\"shaped\",\"linkage\":\"primary\"";
		for (String json : List.of("{}", "{\"" + field + "\":\"" + valid + "\",\"recipe\":\"shaped\"}",
				"{\"" + field + "\":\"" + valid + "\",\"linkage\":\"primary\"}", "{\"recipe\":\"shaped\",\"linkage\":\"primary\"}",
				"{\"" + field + "\":true,\"recipe\":\"shaped\",\"linkage\":\"primary\"}",
				"{\"" + field + "\":\"" + valid + "\",\"recipe\":true,\"linkage\":\"primary\"}",
				"{\"" + field + "\":\"" + valid + "\",\"recipe\":\"shaped\",\"linkage\":true}", full.replace("\"" + valid + "\"", "\"" + invalid + "\"") + "}",
				full.replace("shaped", "grid") + "}", full.replace("primary", "linked") + "}", full + ",\"extra\":1}")) {
			assertThrows(IllegalArgumentException.class, () -> bind(action, json), json);
		}
	}

	private static RegressionActionInvocation bind(RegressionAction action, String json) {
		return action.bind(JsonParser.parseString(json).getAsJsonObject(), "test");
	}
}
