package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.function.IntPredicate;

final class TransferResourceSelection {
	private TransferResourceSelection() {
	}

	static int firstNonEmptyIndex(int size, IntPredicate hasResource) {
		for (int index = 0; index < size; index++) {
			if (hasResource.test(index)) {
				return index;
			}
		}
		return -1;
	}

	static void requireExpectedTransfer(int transferred, int expected) {
		if (transferred != expected) {
			throw new IllegalStateException("Transferred " + transferred + ", expected " + expected);
		}
	}
}
