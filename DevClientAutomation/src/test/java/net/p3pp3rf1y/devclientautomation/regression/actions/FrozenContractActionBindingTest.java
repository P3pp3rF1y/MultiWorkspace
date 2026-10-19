package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrozenContractActionBindingTest {
	@Test
	void linksCanShareTheStoredLinkerAndStorageFilterUsesFrozenSchema() {
		LinkHeldBackpackAction held = new LinkHeldBackpackAction();
		assertEquals("linker", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(held, "{\"linkerRef\":\"linker\"}"), LinkHeldBackpackAction.Arguments.class).linkerRef());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(held, "{\"unexpected\":true}"));

		ConfigureStorageContentsFilterAction filter = new ConfigureStorageContentsFilterAction();
		assertEquals(new ConfigureStorageContentsFilterAction.Arguments(-1, 0, 2, "minecraft:diamond", ConfigureStorageContentsFilterAction.MatchMode.MOD),
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(filter, "{\"offset\":[-1,0,2],\"item\":\"minecraft:diamond\",\"match\":\"mod\"}"),
						ConfigureStorageContentsFilterAction.Arguments.class));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(filter, "{\"offset\":[0,0,0],\"item\":\"minecraft:diamond\",\"match\":\"tag\"}"));
	}

	@Test
	void registryExposesFrozenActionsAndKeepsJeiLazy() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage"), registry.require("storage.linkBlock").metadata().dependencies());
		assertEquals("1", registry.require("fixture.storage.contentsFilter").metadata().revision());
		assertEquals("1", registry.require("backpack.templateCommand").metadata().revision());
		assertTrue(registry.require("recipeviewer.dropSettingsGhost") instanceof LazyRegressionAction);
	}
}
