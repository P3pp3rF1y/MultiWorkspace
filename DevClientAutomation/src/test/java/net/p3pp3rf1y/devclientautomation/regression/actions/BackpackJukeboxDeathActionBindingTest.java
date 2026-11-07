package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackJukeboxDeathActionBindingTest {
	@Test
	void heldJukeboxBaselineReservationsUsePublishedBinaryType() {
		String binaryType = "net.p3pp3rf1y.devclientautomation.regression.actions.AssertHeldBackpackJukeboxPlaybackAction$Baseline";
		assertEquals(binaryType, RegressionValueTypes.HELD_BACKPACK_JUKEBOX_BASELINE);
		var producer = RegressionActionTestSupport.bind(new AssertHeldBackpackJukeboxPlaybackAction(), "{\"saveAs\":\"beforedeath\",\"playing\":true}");
		var consumer = RegressionActionTestSupport.bind(new AssertHeldBackpackJukeboxPlaybackAction(), "{\"baseline\":\"beforedeath\",\"playing\":false}");
		assertEquals(List.of(new RegressionValueContract.Value("beforedeath", binaryType)), producer.values().produces());
		assertEquals(producer.values().produces(), consumer.values().consumes());
		RegressionValueStore store = new RegressionValueStore();
		store.reserveAll(producer.values().produces());
		assertThrows(IllegalStateException.class, () -> store.put("beforedeath", String.class, "wrong type"));
	}

	@Test
	void heldJukeboxDeathActionsBindStrictArgumentsAndRegister() {
		SetHeldBackpackJukeboxDiscAction.Arguments disc = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new SetHeldBackpackJukeboxDiscAction(),
						"{\"item\":\"minecraft:music_disc_13\",\"slot\":1,\"repeat\":\"one\",\"shuffle\":false}"),
				SetHeldBackpackJukeboxDiscAction.Arguments.class);
		assertEquals("minecraft:music_disc_13", disc.item());
		assertEquals(1, disc.slot());
		assertEquals("one", disc.repeat());
		assertEquals(false, disc.shuffle());
		assertEquals("play", RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new ControlHeldBackpackJukeboxPlaybackAction(), "{\"control\":\"play\"}"), String.class));
		assertEquals("minecraft:music_disc_13", RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new ObserveHeldBackpackJukeboxSoundAction(), "{\"item\":\"minecraft:music_disc_13\"}"), String.class));
		AssertHeldBackpackJukeboxPlaybackAction.Arguments assertion = RegressionActionTestSupport.arguments(
				RegressionActionTestSupport.bind(new AssertHeldBackpackJukeboxPlaybackAction(), "{\"saveAs\":\"beforedeath\",\"playing\":true}"),
				AssertHeldBackpackJukeboxPlaybackAction.Arguments.class);
		assertEquals("beforedeath", assertion.saveAs());
		assertEquals("beforedeath",
				RegressionActionTestSupport.arguments(
						RegressionActionTestSupport.bind(new AssertHeldBackpackJukeboxPlaybackAction(), "{\"baseline\":\"beforedeath\",\"playing\":false}"),
						AssertHeldBackpackJukeboxPlaybackAction.Arguments.class).baseline());
		assertEquals(0, RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(new KeepInventoryDeathRespawnAction(), "{}"), Integer.class));
		assertEquals(30, RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(new KeepInventoryDeathRespawnAction(), "{\"respawnDelayTicks\":30}"), Integer.class));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetHeldBackpackJukeboxDiscAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new ControlHeldBackpackJukeboxPlaybackAction(), "{\"control\":\"pause\"}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new ObserveHeldBackpackJukeboxSoundAction(), "{}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetHeldBackpackJukeboxDiscAction(), "{\"item\":\"minecraft:music_disc_13\",\"slot\":-1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new SetHeldBackpackJukeboxDiscAction(),
				"{\"item\":\"minecraft:music_disc_13\",\"repeat\":\"invalid\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new SetHeldBackpackJukeboxDiscAction(), "{\"item\":\"minecraft:music_disc_13\",\"shuffle\":\"false\"}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new AssertHeldBackpackJukeboxPlaybackAction(), "{\"playing\":true}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new KeepInventoryDeathRespawnAction(), "{\"respawnDelayTicks\":-1}"));
		assertThrows(IllegalArgumentException.class,
				() -> RegressionActionTestSupport.bind(new KeepInventoryDeathRespawnAction(), "{\"respawnDelayTicks\":1.5}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(new KeepInventoryDeathRespawnAction(), "{\"extra\":true}"));
		RegressionActionRegistry registry = new RegressionActionRegistry();
		assertEquals("player.keepInventoryDeathRespawn", registry.require("player.keepInventoryDeathRespawn").metadata().id());
		assertEquals("menu.heldBackpackJukeboxPlayback", registry.require("menu.heldBackpackJukeboxPlayback").metadata().id());
		assertEquals("observe.heldBackpackJukeboxSound", registry.require("observe.heldBackpackJukeboxSound").metadata().id());
	}

	@Test
	void legacyRenderProjectionAndRespawnReplacementRetainDirectChecks() throws IOException {
		Path actions = Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/regression/actions");
		String assertion = Files.readString(actions.resolve("AssertHeldBackpackJukeboxPlaybackAction.java"));
		String death = Files.readString(actions.resolve("KeepInventoryDeathRespawnAction.java"));
		assertTrue(assertion.contains("wrapper(mainHand).getRenderInfo().getUpgradeRenderData(JukeboxUpgradeRenderData.TYPE)"));
		assertTrue(assertion.contains("wrapper(stack).getRenderInfo().getUpgradeRenderData(JukeboxUpgradeRenderData.TYPE)"));
		assertTrue(death.contains("replacement != oldPlayer && !replacement.isDeadOrDying()"));
		assertTrue(death.contains("replacement.containerMenu == replacement.inventoryMenu && minecraft.screen == null"));
	}

	@Test
	void resetPlayerRequestsRespawnOnceAndBoundsRecoveryByCoordinatorTicks() throws IOException {
		String reset = Files.readString(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/regression/actions/ResetPlayerAction.java"));
		assertTrue(reset.contains("private boolean respawnRequested;"));
		assertTrue(reset.indexOf("deadline = context.coordinatorTicks() + 100;") < reset.indexOf("clientPlayer.respawn();"));
		assertTrue(reset.contains("minecraft.screen instanceof DeathScreen) && !respawnRequested"));
		assertTrue(reset.contains("context.putDiagnostic(\"resetPlayerRecovery\", \"requestedRespawn\")"));
		assertTrue(reset.contains("return waitForRecovery();"));
		assertTrue(reset.contains("Client player remained dead while resetting the test cell after 100 coordinator ticks"));
	}
}
