package net.p3pp3rf1y.devclientautomation.demo;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public class DemoMouseMotion {
	private static Motion motion;
	private static DelayedCompletion delayedCompletion;
	private static boolean initialized = false;

	private DemoMouseMotion() {
	}

	public static void init() {
		if (!initialized) {
			NeoForge.EVENT_BUS.register(DemoMouseMotion.class);
			initialized = true;
		}
	}

	public static boolean isMoving() {
		return motion != null || delayedCompletion != null;
	}

	public static void moveTo(double targetX, double targetY, int ticks, Runnable onComplete) {
		moveTo(targetX, targetY, ticks, 0, onComplete);
	}

	public static void moveTo(double targetX, double targetY, int ticks, int completionDelayTicks, Runnable onComplete) {
		Minecraft minecraft = Minecraft.getInstance();
		double[] cursorX = new double[1];
		double[] cursorY = new double[1];
		double scale = minecraft.getWindow().getGuiScale();
		GLFW.glfwGetCursorPos(minecraft.getWindow().handle(), cursorX, cursorY);
		double startX = cursorX[0] / scale;
		double startY = cursorY[0] / scale;
		delayedCompletion = null;
		motion = new Motion(startX, startY, targetX, targetY, Math.max(1, ticks), 0, Math.max(0, completionDelayTicks), onComplete);
		setCursor(minecraft, startX, startY);
	}

	public static void setCursor(Minecraft minecraft, double x, double y) {
		double scale = minecraft.getWindow().getGuiScale();
		GLFW.glfwSetCursorPos(minecraft.getWindow().handle(), x * scale, y * scale);
		if (minecraft.gui.screen() != null) {
			minecraft.gui.screen().mouseMoved(x, y);
		}
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		if (delayedCompletion != null) {
			DelayedCompletion current = delayedCompletion;
			int remainingTicks = current.remainingTicks() - 1;
			if (remainingTicks <= 0) {
				delayedCompletion = null;
				current.onComplete().run();
			} else {
				delayedCompletion = new DelayedCompletion(remainingTicks, current.onComplete());
			}
			return;
		}

		if (motion == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Motion current = motion;
		int nextTick = current.elapsedTicks() + 1;
		double progress = Math.min(1D, nextTick / (double) current.durationTicks());
		double eased = easeInOutCubic(progress);
		double x = lerp(current.startX(), current.targetX(), eased);
		double y = lerp(current.startY(), current.targetY(), eased);
		setCursor(minecraft, x, y);

		if (progress >= 1D) {
			motion = null;
			if (current.completionDelayTicks() > 0) {
				delayedCompletion = new DelayedCompletion(current.completionDelayTicks(), current.onComplete());
			} else {
				current.onComplete().run();
			}
		} else {
			motion = new Motion(current.startX(), current.startY(), current.targetX(), current.targetY(), current.durationTicks(), nextTick,
					current.completionDelayTicks(), current.onComplete());
		}
	}

	private static double easeInOutCubic(double value) {
		return value < 0.5D ? 4D * value * value * value : 1D - Math.pow(-2D * value + 2D, 3D) / 2D;
	}

	private static double lerp(double start, double end, double progress) {
		return start + (end - start) * progress;
	}

	private record Motion(double startX, double startY, double targetX, double targetY, int durationTicks, int elapsedTicks, int completionDelayTicks,
			Runnable onComplete) {
	}

	private record DelayedCompletion(int remainingTicks, Runnable onComplete) {
	}
}
