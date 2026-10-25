package net.p3pp3rf1y.devclientautomation.input;

import java.util.function.Supplier;

/** Minecraft-local coordinates and modifiers; never an operating-system input device. */
public final class VirtualInputState {
	private static final ThreadLocal<Integer> MODIFIERS = new ThreadLocal<>();

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
