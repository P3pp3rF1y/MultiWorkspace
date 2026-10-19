package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControllerLinkLineActionBindingTest {
	@Test
	void fixturePrimitivesAndBreakRejectUnexpectedArguments() {
		SetFixtureBlockAction setBlock = new SetFixtureBlockAction();
		ScanFixtureControllerAction scan = new ScanFixtureControllerAction();
		BreakControllerLinkLineBarrelAction breakBarrel = new BreakControllerLinkLineBarrelAction();

		assertSame(setBlock, RegressionActionTestSupport
				.bind(setBlock, "{\"block\": \"sophisticatedstorage:storage_link\", \"offset\": [1, 0, 0], \"facing\": \"west\"}").action());
		assertSame(scan, RegressionActionTestSupport.bind(scan, "{\"offset\": [-2, 0, 0]}").action());
		assertSame(breakBarrel, RegressionActionTestSupport.bind(breakBarrel, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(setBlock, "{\"offset\": [0, 0, 0]}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(setBlock, "{\"block\": \"minecraft:stone\", \"offset\": [0, 0.5, 0]}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(scan, "{\"offset\": [0]}"));
		assertThrows(NumberFormatException.class, () -> RegressionActionTestSupport.bind(scan, "{\"offset\": [2147483648, 0, 0]}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(breakBarrel, "{\"offset\": 1}"));
		RegressionActionTestSupport.assertMetadata(setBlock, "fixture.block.set", Set.of("minecraft:client"));
		RegressionActionTestSupport.assertMetadata(scan, "fixture.controller.scanAdjacent", Set.of("minecraft:client"));
		RegressionActionTestSupport.assertMetadata(breakBarrel, "fixture.controllerLinkLine.breakFirstBarrel",
				Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Test
	void useToolRequiresValidTarget() {
		UseControllerLinkToolAction action = new UseControllerLinkToolAction();

		assertSame(action, RegressionActionTestSupport.bind(action, "{\"target\": \"controller\"}").action());
		assertSame(action, RegressionActionTestSupport.bind(action, "{\"target\": \"link\"}").action());
		assertSame(action, RegressionActionTestSupport.bind(action, "{\"target\": \"groupController\"}").action());
		assertSame(action, RegressionActionTestSupport.bind(action, "{\"target\": \"firstGroupLink\"}").action());
		assertSame(action, RegressionActionTestSupport.bind(action, "{\"target\": \"secondGroupLink\"}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"target\": \"barrel\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"target\": 1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"target\": \"link\", \"offset\": 1}"));
		RegressionActionTestSupport.assertMetadata(action, "player.useControllerLinkTool", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Test
	void groupActionsRequireLayoutAndPhase() {
		BreakControllerLinkGroupsBarrelAction breakBarrel = new BreakControllerLinkGroupsBarrelAction();
		AssertControllerLinkGroupsAction assertGroups = new AssertControllerLinkGroupsAction();

		for (String layout : new String[]{"shared", "separate"}) {
			assertSame(breakBarrel, RegressionActionTestSupport.bind(breakBarrel, "{\"layout\": \"" + layout + "\"}").action());
			for (String phase : new String[]{"beforeBreak", "afterBreak"}) {
				assertSame(assertGroups,
						RegressionActionTestSupport.bind(assertGroups, "{\"layout\": \"" + layout + "\", \"phase\": \"" + phase + "\"}").action());
			}
		}
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(breakBarrel, "{\"layout\": \"other\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(assertGroups, "{\"layout\": \"shared\", \"phase\": \"other\"}"));
		RegressionActionTestSupport.assertMetadata(breakBarrel, "fixture.controllerLinkGroups.breakBarrel", Set.of("minecraft:client", "sophisticatedstorage"));
		RegressionActionTestSupport.assertMetadata(assertGroups, "assert.controllerLinkGroups", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Test
	void assertLineRequiresValidPhase() {
		AssertControllerLinkLineAction action = new AssertControllerLinkLineAction();

		assertSame(action, RegressionActionTestSupport.bind(action, "{\"phase\": \"beforeBreak\"}").action());
		assertSame(action, RegressionActionTestSupport.bind(action, "{\"phase\": \"afterBreak\"}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"phase\": \"other\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"phase\": false}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"phase\": \"afterBreak\", \"offset\": 1}"));
		RegressionActionTestSupport.assertMetadata(action, "assert.controllerLinkLine", Set.of("minecraft:client", "sophisticatedstorage"));
	}
}
