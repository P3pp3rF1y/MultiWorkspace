package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.function.IntPredicate;

final class TransferStorageResourceSelector {
	private TransferStorageResourceSelector() {
	}

	static int firstNonEmptySlot(int slots, IntPredicate isNonEmpty) {
		for (int index = 0; index < slots; index++) {
			if (isNonEmpty.test(index)) {
				return index;
			}
		}
		return -1;
	}

	static void requireExpectedCount(int transferred, int expected) {
		if (transferred != expected) {
			throw new IllegalStateException("Transferred " + transferred + ", expected " + expected);
		}
	}
}
