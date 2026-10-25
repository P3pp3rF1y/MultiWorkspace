package net.p3pp3rf1y.devclientautomation.demo;

import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.devclientautomation.input.BackgroundInputPolicy;
import net.p3pp3rf1y.devclientautomation.input.PresentationPolicy;
import org.lwjgl.glfw.GLFW;

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
		double[] x = new double[1];
		double[] y = new double[1];
		GLFW.glfwGetCursorPos(Minecraft.getInstance().getWindow().handle(), x, y);
		return new double[]{x[0], y[0]};
	}

	static void move(double x, double y) {
		GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().handle(), x, y);
	}

	public static void maximize() {
		requireStart();
		GLFW.glfwMaximizeWindow(Minecraft.getInstance().getWindow().handle());
	}
}
