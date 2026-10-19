package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferResourceSelectionTest {
	@Test
	void selectsTheFirstNonEmptyResource() {
		assertEquals(1, TransferResourceSelection.firstNonEmptyIndex(4, index -> index == 1 || index == 3));
	}

	@Test
	void reportsNoResourceWhenEverySlotIsEmpty() {
		assertEquals(-1, TransferResourceSelection.firstNonEmptyIndex(3, index -> false));
	}

	@Test
	void rejectsUnexpectedTransferredAmountsBeforeCommitCanOccur() {
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> TransferResourceSelection.requireExpectedTransfer(500, 1000));
		assertEquals("Transferred 500, expected 1000", exception.getMessage());
	}
}
