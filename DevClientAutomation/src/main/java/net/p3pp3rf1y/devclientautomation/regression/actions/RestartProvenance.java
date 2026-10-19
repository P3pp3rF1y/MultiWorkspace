package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.UUID;

/** Automation checkpoint identity; independent of linked-storage production persistence. */
public final class RestartProvenance {
	public static void validateToken(String actual, String expected) {
		if (expected.isBlank() || !actual.equals(expected)) {
			throw new IllegalStateException("Restart checkpoint preparation token does not match the requested preparation run");
		}
	}

	private RestartProvenance() {
	}

	public static void validate(String preparedCheckpoint, String requestedCheckpoint, String preparedWorld, String currentWorld, UUID preparedPlayer,
			UUID currentPlayer, UUID preparedBoot, UUID currentBoot) {
		if (!preparedCheckpoint.equals(requestedCheckpoint) || !preparedWorld.equals(currentWorld) || !preparedPlayer.equals(currentPlayer)) {
			throw new IllegalStateException("Restart checkpoint does not belong to this preparation/world/player");
		}
		if (preparedBoot.equals(currentBoot)) {
			throw new IllegalStateException("Restart verification requires a different server boot");
		}
	}
}
