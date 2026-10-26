package net.p3pp3rf1y.devclientautomation.input;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundInputContractTest {
	@Test
	void backgroundInputPolicyDefaultsToManualAndRequiresExplicitLaunchOptIn() {
		String previous = System.getProperty(BackgroundInputPolicy.PROPERTY);
		try {
			System.clearProperty(BackgroundInputPolicy.PROPERTY);
			assertFalse(BackgroundInputPolicy.enabled());
			System.setProperty(BackgroundInputPolicy.PROPERTY, "true");
			assertTrue(BackgroundInputPolicy.enabled());
			System.setProperty(BackgroundInputPolicy.PROPERTY, "false");
			assertFalse(BackgroundInputPolicy.enabled());
		} finally {
			if (previous == null) {
				System.clearProperty(BackgroundInputPolicy.PROPERTY);
			} else {
				System.setProperty(BackgroundInputPolicy.PROPERTY, previous);
			}
		}
	}

	@Test
	void virtualInputStateScalesCoordinatesAndRestoresModifiersAfterFailure() {
		assertEquals(198.0, VirtualInputState.toWindow(66, 1200, 400));
		assertEquals(-300.0, VirtualInputState.toWindow(-100, 1200, 400));
		assertFalse(VirtualInputState.modifier(1));
		assertFalse(VirtualInputState.active());

		assertThrows(IllegalStateException.class, () -> VirtualInputState.withModifiers(1, () -> {
			assertTrue(VirtualInputState.active());
			assertTrue(VirtualInputState.modifier(1));
			VirtualInputState.withModifiers(2, () -> {
				assertFalse(VirtualInputState.modifier(1));
				assertTrue(VirtualInputState.modifier(2));
				return null;
			});
			assertTrue(VirtualInputState.modifier(1));
			throw new IllegalStateException("callback failure");
		}));

		assertFalse(VirtualInputState.modifier(1));
		assertFalse(VirtualInputState.active());
		VirtualInputState.withModifiers(0, () -> {
			assertTrue(VirtualInputState.active());
			assertFalse(VirtualInputState.modifier(1));
			return null;
		});
		assertFalse(VirtualInputState.active());
	}

	@Test
	void presentationPolicyRejectsOrdinaryAndUnfocusedStartsWithoutWaiting() {
		assertThrows(IllegalStateException.class, () -> PresentationPolicy.requireStart(false, true));
		assertThrows(IllegalStateException.class, () -> PresentationPolicy.requireStart(false, false));
		assertThrows(IllegalStateException.class, () -> PresentationPolicy.requireStart(true, false));
		assertDoesNotThrow(() -> PresentationPolicy.requireStart(true, true));
	}

	@Test
	void ordinarySourcesCannotIntroduceNativeCursorOrWindowOperations() throws IOException {
		Pattern nativeOperation = Pattern.compile(
				"(?:glfw(?:SetCursorPos|SetInputMode|FocusWindow|MaximizeWindow|RestoreWindow|SetWindow\\w*|ShowWindow|IconifyWindow)|SDL_(?:WarpMouseInWindow|SetCursor|MaximizeWindow|RestoreWindow|SetWindow\\w*|ShowWindow|HideWindow))\\s*\\(|grabOrReleaseMouse\\s*\\(|new\\s+Robot\\s*\\(|(?:SetCursorPos|ClipCursor|SetForegroundWindow)\\s*\\(");
		Set<String> presentationCallers = Set.of("DemoMouseMotion.java", "DemoPlayback.java", "PresentationNativeInput.java", "ClientControlEndpoints.java");
		try (var paths = Files.walk(Path.of("src/main/java"))) {
			for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
				String text = Files.readString(path);
				if (!path.getFileName().toString().equals("PresentationNativeInput.java")) {
					assertFalse(nativeOperation.matcher(text).find(), "Native input outside presentation boundary: " + path);
				}
				if (text.contains("PresentationNativeInput.")) {
					assertTrue(presentationCallers.contains(path.getFileName().toString()), "Presentation leaked into regression: " + path);
				}
			}
		}
		try (var paths = Files.walk(Path.of("../scripts/dev-client"))) {
			for (Path path : paths.filter(p -> p.toString().endsWith(".ps1")).toList()) {
				String text = Files.readString(path);
				assertFalse(text.contains("/window/maximize") || text.contains("MaximizeClient") || text.contains("-Maximize"),
						"Native window option in ordinary runner: " + path);
			}
		}
	}

	@Test
	void backgroundModeSuppressesNativeCursorAndImeWritesAtMinecraftBoundaries() throws IOException {
		String mixins = Files.readString(Path.of("src/main/resources/devclientautomation.mixins.json"));
		for (String name : Set.of("BackgroundMouseHandlerMixin", "BackgroundCursorTypeMixin", "BackgroundTextInputManagerMixin")) {
			assertTrue(mixins.contains("\"" + name + "\""), "Missing background input mixin: " + name);
		}
		String cursor = Files.readString(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/mixin/BackgroundCursorTypeMixin.java"));
		String mouse = Files.readString(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/mixin/BackgroundMouseHandlerMixin.java"));
		String textInput = Files.readString(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/mixin/BackgroundTextInputManagerMixin.java"));
		assertTrue(cursor.contains("@Inject(method = \"select\", at = @At(\"HEAD\"), cancellable = true)"));
		assertTrue(mouse.contains("double xRelative, double yRelative, CallbackInfo callback"));
		assertTrue(mouse.contains("@Inject(method = \"resyncMousePosition\", at = @At(\"HEAD\"), cancellable = true)"));
		for (String method : Set.of("setTextInputArea", "startTextInput")) {
			assertTrue(textInput.contains("@Inject(method = \"" + method + "\", at = @At(\"HEAD\"), cancellable = true)"));
		}
		assertTrue(textInput.contains("@Inject(method = \"stopTextInput()V\", at = @At(\"HEAD\"), cancellable = true)"));
		assertEquals(1, cursor.split("if \\(BackgroundInputPolicy.enabled\\(\\)\\) \\{\\s*callback.cancel\\(\\);", -1).length - 1);
		assertEquals(3, mouse.split("if \\(BackgroundInputPolicy.enabled\\(\\)\\) \\{\\s*callback.cancel\\(\\);", -1).length - 1);
		assertEquals(3, textInput.split("if \\(BackgroundInputPolicy.enabled\\(\\)\\) \\{\\s*callback.cancel\\(\\);", -1).length - 1);
	}
}
