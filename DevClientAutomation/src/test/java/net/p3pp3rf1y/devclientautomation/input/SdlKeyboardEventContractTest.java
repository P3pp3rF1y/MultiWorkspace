package net.p3pp3rf1y.devclientautomation.input;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdlKeyboardEventContractTest {
	@Test
	void screenKeyUsesPhysicalScancodeAndDerivedVirtualKeycodeForNavigation() throws IOException {
		String endpoint = Files.readString(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/scenarios/core/ClientControlEndpoints.java"));
		assertTrue(endpoint.contains("new KeyEvent(keyCode, SDLKeyboard.SDL_GetKeyFromScancode(keyCode, (short) 0, false), modifiers)"));
		assertTrue(endpoint.contains("case \"TAB\" -> InputConstants.KEY_TAB"));
		assertTrue(endpoint.contains("case \"UP\" -> InputConstants.KEY_UP"));
		assertTrue(endpoint.contains("case \"DOWN\" -> InputConstants.KEY_DOWN"));
		assertTrue(endpoint.contains("case \"ENTER\", \"RETURN\" -> InputConstants.KEY_RETURN"));
		assertFalse(endpoint.contains("new KeyEvent(keyCode, 0, modifiers)"));
	}

	@Test
	void syntheticSiaKeyCarriesAltInModifiersAndPressReleaseInAction() throws IOException {
		String demo = Files.readString(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/demo/DemoCommand.java"));
		for (String action : new String[]{"PRESS", "RELEASE"}) {
			assertTrue(demo.contains("new InputEvent.Key(new KeyEvent(key.getValue(), 0, SDLKeycode.SDL_KMOD_ALT), InputConstants." + action + ")"));
		}
		assertFalse(demo.contains("new InputEvent.Key(new KeyEvent(key.getValue(), 0, 1), SDLKeycode.SDL_KMOD_ALT)"));
		assertFalse(demo.contains("new InputEvent.Key(new KeyEvent(key.getValue(), 0, 0), SDLKeycode.SDL_KMOD_ALT)"));
	}
}
