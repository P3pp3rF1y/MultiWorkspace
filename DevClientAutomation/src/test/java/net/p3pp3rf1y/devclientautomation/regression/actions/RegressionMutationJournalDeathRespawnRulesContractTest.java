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

		assertTrue(journal.contains("entry.putBoolean(\"keepInventory\", level.getGameRules().get(GameRules.KEEP_INVENTORY))"));
		assertTrue(journal.contains("entry.putBoolean(\"immediateRespawn\", level.getGameRules().get(GameRules.IMMEDIATE_RESPAWN))"));
		assertTrue(journal.contains("level.getGameRules().set(GameRules.KEEP_INVENTORY, entry.getBooleanOr(\"keepInventory\", false), level.getServer())"));
		assertTrue(
				journal.contains("level.getGameRules().set(GameRules.IMMEDIATE_RESPAWN, entry.getBooleanOr(\"immediateRespawn\", false), level.getServer())"));
	}
}
