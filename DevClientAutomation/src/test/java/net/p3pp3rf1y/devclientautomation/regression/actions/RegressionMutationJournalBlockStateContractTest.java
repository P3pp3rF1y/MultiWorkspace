package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionMutationJournalBlockStateContractTest {
	@Test
	void retainedBlockSnapshotUsesMinecraft263StateIdForValidationAndRestore() throws IOException {
		String journal = Files.readString(Path.of("src/main/java/net/p3pp3rf1y/devclientautomation/regression/actions/RegressionMutationJournal.java"));
		assertTrue(journal.contains("entry.put(\"state\", NbtUtils.writeBlockState(level.getBlockState(pos)))"));
		assertTrue(journal.contains("CompoundTag originalState = entry.getCompoundOrEmpty(\"state\");"));
		assertTrue(journal.contains("String blockId = originalState.getStringOr(\"id\", \"\");"));
		assertFalse(journal.contains("originalState.getStringOr(\"Name\", \"\")"));
		assertTrue(journal.contains("NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), originalState)"));
		assertTrue(journal.contains("read(path).finish(player, null, false)"));
		assertTrue(journal.contains("if (!failures.isEmpty())"));
		assertTrue(journal.contains("if (clearJournal)"));
	}
}
