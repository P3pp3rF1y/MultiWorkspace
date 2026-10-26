package net.p3pp3rf1y.devclientautomation.demo;

import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import net.p3pp3rf1y.devclientautomation.input.PresentationPolicy;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/** The sole native-input boundary, used only by explicitly selected presentation workflows. */
public final class PresentationNativeInput {
	private PresentationNativeInput() {
	}

	public static void requireStart() {
		if (BackgroundInputPolicy.enabled()) {
			throw new IllegalStateException("Native presentation requires a separate client launched without background input mode");
		}
		PresentationPolicy.requireStart(Boolean.getBoolean(PresentationPolicy.PROPERTY), Minecraft.getInstance().isWindowActive());
	}

	static double[] cursorPosition() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			FloatBuffer x = stack.mallocFloat(1);
			FloatBuffer y = stack.mallocFloat(1);
			SDLMouse.SDL_GetMouseState(x, y);
			return new double[]{x.get(0), y.get(0)};
		}
	}

	static void move(double x, double y) {
		SDLMouse.SDL_WarpMouseInWindow(Minecraft.getInstance().getWindow().handle(), (float) x, (float) y);
	}

	public static void maximize() {
		requireStart();
		SDLVideo.SDL_MaximizeWindow(Minecraft.getInstance().getWindow().handle());
	}
}
