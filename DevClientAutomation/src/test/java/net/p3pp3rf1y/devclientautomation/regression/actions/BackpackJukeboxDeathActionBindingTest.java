package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackpackJukeboxDeathActionBindingTest {
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

}
