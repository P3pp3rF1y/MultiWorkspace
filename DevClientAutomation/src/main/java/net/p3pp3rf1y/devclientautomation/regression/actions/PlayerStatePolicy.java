package net.p3pp3rf1y.devclientautomation.regression.actions;

public enum PlayerStatePolicy {
	ISOLATED("isolated"), CHECKPOINT_PREPARE("checkpointPrepare"), CHECKPOINT_VERIFY("checkpointVerify");
	private final String serializedName;
	PlayerStatePolicy(String serializedName) {
		this.serializedName = serializedName;
	}
	public String serializedName() {
		return serializedName;
	}
	public static PlayerStatePolicy parse(String name) {
		return switch (name) {
			case "isolated" -> ISOLATED;
			case "checkpointPrepare" -> CHECKPOINT_PREPARE;
			case "checkpointVerify" -> CHECKPOINT_VERIFY;
			default -> throw new IllegalArgumentException("Unknown playerState " + name);
		};
	}
}
