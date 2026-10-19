package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestartProvenanceTest {
	@Test
	void validateTokenRejectsMissingAndStalePreparationTokens() {
		String token = UUID.randomUUID().toString();

		assertDoesNotThrow(() -> RestartProvenance.validateToken(token, token));
		assertThrows(IllegalStateException.class, () -> RestartProvenance.validateToken(token, ""));
		assertThrows(IllegalStateException.class, () -> RestartProvenance.validateToken(token, UUID.randomUUID().toString()));
	}

	@Test
	void validateRejectsSameBootWrongWorldPlayerAndPreparation() {
		UUID player = UUID.randomUUID();
		UUID preparedBoot = UUID.randomUUID();
		UUID restartedBoot = UUID.randomUUID();

		assertDoesNotThrow(() -> RestartProvenance.validate("run", "run", "world", "world", player, player, preparedBoot, restartedBoot));
		assertThrows(IllegalStateException.class, () -> RestartProvenance.validate("run", "run", "world", "world", player, player, preparedBoot, preparedBoot));
		assertThrows(IllegalStateException.class,
				() -> RestartProvenance.validate("run", "other", "world", "world", player, player, preparedBoot, restartedBoot));
		assertThrows(IllegalStateException.class,
				() -> RestartProvenance.validate("run", "run", "world", "other", player, player, preparedBoot, restartedBoot));
		assertThrows(IllegalStateException.class,
				() -> RestartProvenance.validate("run", "run", "world", "world", player, UUID.randomUUID(), preparedBoot, restartedBoot));
	}
}
