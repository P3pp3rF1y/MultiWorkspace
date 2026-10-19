package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TierUpgradeRecipeActionBindingTest {
	@Test
	void storageRecipeProxyBindsEveryProductionDomainAndRejectsInvalidSchemas() {
		RegressionAction action = new RegressionActionRegistry().require("assert.storageTierUpgradeRecipe");
		assertInstanceOf(LazyRegressionAction.class, action);
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage"), action.metadata().dependencies());
		for (String form : Set.of("normal", "double")) {
			for (String recipe : Set.of("shaped", "shapeless")) {
				for (String linkage : Set.of("unlinked", "primary", "secondary")) {
					assertSame(action, bind(action, "{\"form\":\"" + form + "\",\"recipe\":\"" + recipe + "\",\"linkage\":\"" + linkage + "\"}").action());
				}
			}
		}
		assertInvalidVariants(action, "form", "normal", "single");
	}

	@Test
	void movingRecipeProxyBindsEveryProductionDomainAndRejectsInvalidSchemas() {
		RegressionAction action = new RegressionActionRegistry().require("assert.movingStorageTierUpgradeRecipe");
		assertInstanceOf(LazyRegressionAction.class, action);
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"), action.metadata().dependencies());
		for (String carrier : Set.of("boat", "minecart")) {
			for (String recipe : Set.of("shaped", "shapeless")) {
				for (String linkage : Set.of("unlinked", "primary", "secondary")) {
					assertDoesNotThrow(() -> bind(action, "{\"carrier\":\"" + carrier + "\",\"recipe\":\"" + recipe + "\",\"linkage\":\"" + linkage + "\"}"));
				}
			}
		}
		assertInvalidVariants(action, "carrier", "boat", "horse");
	}

	private static void assertInvalidVariants(RegressionAction action, String key, String valid, String invalid) {
		assertInvalid(action, "{}");
		assertInvalid(action, "{\"" + key + "\":\"" + valid + "\",\"recipe\":\"shaped\"}");
		assertInvalid(action, "{\"" + key + "\":\"" + valid + "\",\"linkage\":\"primary\"}");
		assertInvalid(action, "{\"recipe\":\"shaped\",\"linkage\":\"primary\"}");
		for (String field : Set.of(key, "recipe", "linkage")) {
			assertInvalid(action, "{\"" + key + "\":" + (field.equals(key) ? "true" : "\"" + valid + "\"") + ",\"recipe\":"
					+ (field.equals("recipe") ? "true" : "\"shaped\"") + ",\"linkage\":" + (field.equals("linkage") ? "true" : "\"primary\"") + "}");
		}
		for (String json : Set.of("{\"" + key + "\":\"" + invalid + "\",\"recipe\":\"shaped\",\"linkage\":\"primary\"}",
				"{\"" + key + "\":\"" + valid + "\",\"recipe\":\"grid\",\"linkage\":\"primary\"}",
				"{\"" + key + "\":\"" + valid + "\",\"recipe\":\"shaped\",\"linkage\":\"linked\"}",
				"{\"" + key + "\":\"" + valid + "\",\"recipe\":\"shaped\",\"linkage\":\"primary\",\"extra\":1}")) {
			assertInvalid(action, json);
		}
	}

	private static RegressionActionInvocation bind(RegressionAction action, String json) {
		return action.bind(JsonParser.parseString(json).getAsJsonObject(), "test");
	}

	private static void assertInvalid(RegressionAction action, String json) {
		assertThrows(IllegalArgumentException.class, () -> bind(action, json));
	}
}
