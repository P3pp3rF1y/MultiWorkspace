package net.p3pp3rf1y.devclientautomation.input;

public final class PresentationPolicy {
	public static final String PROPERTY = "devclientautomation.presentation";

	private PresentationPolicy() {
	}

	public static void requireStart(boolean explicitlyEnabled, boolean windowActive) {
		if (!explicitlyEnabled) {
			throw new IllegalStateException("Native presentation requires explicit -D" + PROPERTY + "=true; ordinary regressions use virtual input");
		}
		if (!windowActive) {
			throw new IllegalStateException("Native presentation must start with the Minecraft window already in the foreground");
		}
	}
}
