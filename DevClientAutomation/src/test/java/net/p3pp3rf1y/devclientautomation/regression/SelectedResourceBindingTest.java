package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectedResourceBindingTest {
	private static final String ROOT = "/devclientautomation/regressions/";
	private static final String[] SELECTED = ("""
			backpack-create-disassembly backpack-menu controller-link controller-manual-deposit linked-storage-action-conformance
			linked-storage-controller-lifecycle linked-storage-fluid-lock linked-storage-inception
			linked-storage-interoperability linked-storage-menu linked-storage-mob-catcher
			linked-storage-resources linked-storage-restart linked-storage-settings moving-linked-storage storage-menu
			""").trim().split("\\s+");

	@Test
	void selectedCatalogRetainsAllUnselectedIndexEntriesAndCases() throws IOException {
		var index = JsonParser.parseString(resource("index.json")).getAsJsonArray();
		Set<String> entries = new LinkedHashSet<>();
		index.forEach(entry -> entries.add(entry.getAsString()));
		assertEquals(index.size(), entries.size(), "Duplicate index entries");
		Arrays.stream(SELECTED).forEach(id -> assertTrue(entries.contains(id + ".json"), id));
		assertEquals(
				Set.of("backpack-filters.json", "backpack-restock.json", "backpack-pickup.json", "item-entity-action-conformance.json",
						"controller-deposit.json", "tier-upgrade-recipes.json", "vanilla-double-chest-upgrade.json"),
				entries.stream().filter(entry -> Arrays.stream(SELECTED).noneMatch(id -> entry.equals(id + ".json")))
						.collect(java.util.stream.Collectors.toSet()));
		assertEquals(23, entries.size());
		assertEquals(9, JsonParser.parseString(resource("backpack-menu.json")).getAsJsonObject().getAsJsonArray("tests").size());
		assertEquals(2, JsonParser.parseString(resource("storage-menu.json")).getAsJsonObject().getAsJsonArray("tests").size());
	}

	@Test
	void allFrozenSelectedResourcesBindWithExactAuthoredAndExpandedInventory() throws IOException {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		int cases = 0;
		int fixtures = 0;
		int authored = 0;
		int expanded = 0;
		Set<String> selectedCases = new HashSet<>();
		Set<String> usedActions = new HashSet<>();
		for (String id : SELECTED) {
			JsonObject json = JsonParser.parseString(resource(id + ".json")).getAsJsonObject();
			RegressionSuite suite = RegressionSuiteParser.parse(json, registry);
			assertEquals(id, suite.suiteId());
			fixtures += json.has("fixtures") ? json.getAsJsonObject("fixtures").size() : 0;
			Set<String> shared = switch (id) {
				case "backpack-menu" -> Set.of("linked-held-backpack-menu-receives-slot-delta");
				case "storage-menu" -> Set.of("placed-linked-block-open-menu-receives-live-slot-delta");
				default -> null;
			};
			for (var test : json.getAsJsonArray("tests")) {
				String testId = test.getAsJsonObject().get("id").getAsString();
				if (shared != null && !shared.contains(testId)) {
					continue;
				}
				cases++;
				authored += test.getAsJsonObject().getAsJsonArray("steps").size();
				assertTrue(selectedCases.add(id + "/" + testId), id + "/" + testId);
				RegressionTest bound = suite.tests().stream().filter(candidate -> candidate.id().equals(testId)).findFirst().orElseThrow();
				expanded += bound.steps().size();
				bound.steps().forEach(step -> {
					String actionId = step.action().action().metadata().id();
					assertEquals(actionId, registry.require(actionId).metadata().id());
					usedActions.add(actionId);
				});
			}
			if (shared != null) {
				assertTrue(suite.tests().stream().anyMatch(test -> shared.contains(test.id())), id);
			} else {
				assertEquals(json.getAsJsonArray("tests").size(), suite.tests().size(), id);
			}
		}
		assertEquals(16, SELECTED.length);
		assertEquals(cases, selectedCases.size());
		assertTrue(cases >= 60);
		assertTrue(fixtures >= 9);
		assertTrue(authored >= 900);
		assertTrue(expanded >= authored);
		registry.testCellSetup().forEach(step -> usedActions.add(step.action().action().metadata().id()));
		assertTrue(registry.all().keySet().containsAll(usedActions), "Selected resources used an unregistered action");
		assertTrue(usedActions.contains("fixture.storage.contentsFilter"), "The manual-deposit source contract must bind its contents-filter action");
	}

	private String resource(String name) throws IOException {
		try (var stream = getClass().getResourceAsStream(ROOT + name)) {
			assertNotNull(stream, name);
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
