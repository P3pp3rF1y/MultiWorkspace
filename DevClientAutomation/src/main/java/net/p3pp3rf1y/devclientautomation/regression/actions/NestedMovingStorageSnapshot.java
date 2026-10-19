package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.minecraft.nbt.CompoundTag;

/** Immutable serialized nested carrier state shared between fixture and assertion actions. */
public final class NestedMovingStorageSnapshot {
	private final CompoundTag outer;
	private final String endpoint;

	private NestedMovingStorageSnapshot(CompoundTag outer, String endpoint) {
		this.outer = outer.copy();
		this.endpoint = endpoint;
	}

	public static NestedMovingStorageSnapshot of(CompoundTag outer, String endpoint) {
		return new NestedMovingStorageSnapshot(outer, endpoint);
	}

	public CompoundTag outerCopy() {
		return outer.copy();
	}

	public String endpoint() {
		return endpoint;
	}
}
