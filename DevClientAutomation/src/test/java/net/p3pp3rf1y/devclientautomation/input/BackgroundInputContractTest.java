package net.p3pp3rf1y.devclientautomation.input;

import net.p3pp3rf1y.devclientautomation.mixin.BackgroundMouseHandlerMixin;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.lang.reflect.Method;
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
	void nativeButtonCallbacksAreBlockedOnlyForBackgroundClients() throws Exception {
		String previous = System.getProperty(BackgroundInputPolicy.PROPERTY);
		try {
			Method onPress = BackgroundMouseHandlerMixin.class.getDeclaredMethod("preventNativeButtonInput", long.class, int.class, int.class, int.class,
					CallbackInfo.class);
			onPress.setAccessible(true);
			System.setProperty(BackgroundInputPolicy.PROPERTY, "true");
			CallbackInfo background = new CallbackInfo("onPress", true);
			BackgroundMouseHandlerMixin mixin = new BackgroundMouseHandlerMixin() {
			};
			onPress.invoke(mixin, 0L, 0, 1, 0, background);
			assertTrue(background.isCancelled());
			System.setProperty(BackgroundInputPolicy.PROPERTY, "false");
			CallbackInfo manual = new CallbackInfo("onPress", true);
			onPress.invoke(mixin, 0L, 0, 1, 0, manual);
			assertFalse(manual.isCancelled());
		} finally {
			if (previous == null) {
				System.clearProperty(BackgroundInputPolicy.PROPERTY);
			} else {
				System.setProperty(BackgroundInputPolicy.PROPERTY, previous);
			}
		}
	}
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
	void regressionActionsCannotBypassScreenInventoryClicks() throws IOException {
		Pattern bypass = Pattern.compile("\\bhandleInventoryMouseClick\\s*\\(|\\bServerboundContainerClickPacket\\b|\\.(?:clicked|quickMoveStack)\\s*\\(");
		try (var paths = Files.walk(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/regression/actions"))) {
			for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
				assertFalse(bypass.matcher(Files.readString(path)).find(), "Regression inventory click bypasses screen input: " + path);
			}
		}
	}

	@Test
	void ordinarySourcesCannotIntroduceNativeCursorOrWindowOperations() throws IOException {
		Pattern nativeOperation = Pattern.compile(
				"glfw(?:SetCursorPos|SetInputMode|FocusWindow|MaximizeWindow|RestoreWindow|SetWindow\\w*|ShowWindow|IconifyWindow)\\s*\\(|grabOrReleaseMouse\\s*\\(|new\\s+Robot\\s*\\(|(?:SetCursorPos|ClipCursor|SetForegroundWindow)\\s*\\(");
		Set<String> presentationCallers = Set.of("DemoMouseMotion.java", "DemoPlayback.java", "PresentationNativeInput.java", "ClientControlEndpoints.java",
				"DevClientAutomationClient.java");
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
}
