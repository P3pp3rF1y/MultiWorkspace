package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionMutationJournalDeathRespawnRulesContractTest {
	@Test
	void recordDeathRespawnRulesSnapshotsAndRestoresBothRules() throws IOException {
		String journal = Files.readString(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/regression/actions/RegressionMutationJournal.java"));

		assertTrue(journal.contains("entry.putBoolean(\"keepInventory\", level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY))"));
		assertTrue(journal.contains("entry.putBoolean(\"immediateRespawn\", level.getGameRules().getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN))"));
		assertTrue(journal.contains("level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(entry.getBoolean(\"keepInventory\"), level.getServer())"));
		assertTrue(journal
				.contains("level.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(entry.getBoolean(\"immediateRespawn\"), level.getServer())"));
	}

	@Test
	void legacyCheckpointWithoutConditionsDoesNotResetHealthOrEffects() throws IOException {
		String journal = Files.readString(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/regression/actions/RegressionMutationJournal.java"));

		assertTrue(journal.contains("checkpoint.contains(\"originalPlayerConditions\", 10)"));
		assertTrue(journal.contains("? checkpoint.getCompound(\"originalPlayerConditions\").copy()"));
		assertTrue(journal.contains(": null;"));
		assertTrue(journal.contains("if (snapshot == null)"));
		assertTrue(journal.contains("restorePlayerConditions(player, playerConditionSnapshot)"));
	}
}
