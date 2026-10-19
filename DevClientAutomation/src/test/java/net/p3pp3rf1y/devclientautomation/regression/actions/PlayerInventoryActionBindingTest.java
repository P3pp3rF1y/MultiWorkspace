package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerInventoryActionBindingTest {
	@Test
	void playerInventoryActionsBindDefaultsAndRejectInvalidArguments() {
		SetPlayerGameModeAction.Arguments mode = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new SetPlayerGameModeAction(), "{\"mode\":\"creative\"}"), SetPlayerGameModeAction.Arguments.class);
		CopyPlayerInventoryItemAction.Arguments copy = RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new CopyPlayerInventoryItemAction(), "{}"), CopyPlayerInventoryItemAction.Arguments.class);
		assertEquals("creative", mode.mode());
		assertEquals(0, copy.source());
		assertEquals(10, copy.target());
		RegressionActionTestSupport.bind(new AssertPlayerInventoryBaselineAction(), "{}");
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetPlayerGameModeAction(), "{\"mode\":\"other\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetPlayerGameModeAction(), "{\"mode\":\"survival\",\"extra\":1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new CopyPlayerInventoryItemAction(), "{\"source\":10,\"target\":10}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new CopyPlayerInventoryItemAction(), "{\"target\":36}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new AssertPlayerInventoryBaselineAction(), "{\"extra\":true}"));
	}
	@Test
	void selectedPlayerActionsAreRegisteredWithStrictArguments() {
		RegressionActionRegistry registry = new RegressionActionRegistry();
		RegressionAction mode = registry.require("fixture.player.setGameMode");
		RegressionAction copy = registry.require("fixture.player.copyInventoryItem");
		RegressionAction baseline = registry.require("assert.playerInventoryBaseline");
		assertSame(mode, RegressionActionTestSupport.bind(mode, "{\"mode\":\"survival\"}").action());
		assertSame(mode, RegressionActionTestSupport.bind(mode, "{\"mode\":\"creative\"}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(mode, "{\"mode\":\"other\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(mode, "{\"mode\":\"survival\",\"extra\":1}"));
		assertSame(copy, RegressionActionTestSupport.bind(copy, "{}").action());
		assertSame(copy, RegressionActionTestSupport.bind(copy, "{\"source\":0,\"target\":35}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(copy, "{\"source\":10,\"target\":10}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(copy, "{\"target\":36}"));
		assertSame(baseline, RegressionActionTestSupport.bind(baseline, "{}").action());
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(baseline, "{\"extra\":true}"));
		RegressionActionTestSupport.assertMetadata(mode, "fixture.player.setGameMode", Set.of("minecraft:client"));
		RegressionActionTestSupport.assertMetadata(copy, "fixture.player.copyInventoryItem", Set.of("minecraft:client"));
		RegressionActionTestSupport.assertMetadata(baseline, "assert.playerInventoryBaseline", Set.of("minecraft:client"));
	}
}
