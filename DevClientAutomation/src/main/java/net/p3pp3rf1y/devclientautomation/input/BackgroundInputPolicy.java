package net.p3pp3rf1y.devclientautomation.input;

/** Launch-time opt-in: ordinary workspace clients retain normal mouse and focus behavior. */
public final class BackgroundInputPolicy {
	public static final String PROPERTY = "devclientautomation.backgroundInput";

	private BackgroundInputPolicy() {
	}

	public static boolean enabled() {
		return Boolean.getBoolean(PROPERTY);
	}
}
