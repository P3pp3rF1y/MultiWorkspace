package net.p3pp3rf1y.devclientautomation.input;

import java.util.function.Supplier;

/** Minecraft-local coordinates and modifiers; never an operating-system input device. */
public final class VirtualInputState {
	private static final ThreadLocal<Integer> MODIFIERS = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> VIRTUAL_MOUSE_PRESS = new ThreadLocal<>();
	private static int heldMouseButton = -1;

	public static boolean virtualMousePress() {
		return Boolean.TRUE.equals(VIRTUAL_MOUSE_PRESS.get());
	}

	public static void withVirtualMousePress(Runnable operation) {
		Boolean previous = VIRTUAL_MOUSE_PRESS.get();
		VIRTUAL_MOUSE_PRESS.set(true);
		try {
			operation.run();
		} finally {
			if (previous == null) {
				VIRTUAL_MOUSE_PRESS.remove();
			} else {
				VIRTUAL_MOUSE_PRESS.set(previous);
			}
		}
	}

	public static int heldMouseButton() {
		return heldMouseButton;
	}

	public static void setHeldMouseButton(int button) {
		heldMouseButton = button;
	}

	private VirtualInputState() {
	}

	public static double toWindow(double guiCoordinate, int windowSize, int guiSize) {
		return guiCoordinate * windowSize / guiSize;
	}

	public static boolean modifier(int mask) {
		Integer modifiers = MODIFIERS.get();
		return modifiers != null && (modifiers & mask) != 0;
	}

	public static boolean active() {
		return MODIFIERS.get() != null;
	}

	public static <T> T withModifiers(int modifiers, Supplier<T> operation) {
		Integer previous = MODIFIERS.get();
		MODIFIERS.set(modifiers);
		try {
			return operation.get();
		} finally {
			if (previous == null) {
				MODIFIERS.remove();
			} else {
				MODIFIERS.set(previous);
			}
		}
	}
}
