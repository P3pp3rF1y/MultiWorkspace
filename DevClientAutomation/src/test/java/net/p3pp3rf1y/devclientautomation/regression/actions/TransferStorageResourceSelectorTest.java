package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferStorageResourceSelectorTest {
	@Test
	void selectsFirstNonEmptySlotOrNoSlot() {
		assertEquals(1, TransferStorageResourceSelector.firstNonEmptySlot(4, index -> index == 1 || index == 3));
		assertEquals(-1, TransferStorageResourceSelector.firstNonEmptySlot(3, index -> false));
	}

	@Test
	void requiresExactExpectedTransferCount() {
		TransferStorageResourceSelector.requireExpectedCount(24, 24);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> TransferStorageResourceSelector.requireExpectedCount(23, 24));
		assertEquals("Transferred 23, expected 24", exception.getMessage());
	}
}
