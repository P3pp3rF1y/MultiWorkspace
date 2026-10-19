package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.minecraft.world.item.ItemStack;

/** Immutable copied carrier state shared between nested-moving-storage assertions. */
public final class NestedMovingStorageSnapshot {
	private final ItemStack outer;
	private final String endpoint;
	private NestedMovingStorageSnapshot(ItemStack outer, String endpoint) {
		this.outer = outer.copy();
		this.endpoint = endpoint;
	}
	public static NestedMovingStorageSnapshot of(ItemStack outer, String endpoint) {
		return new NestedMovingStorageSnapshot(outer, endpoint);
	}
	public ItemStack outerCopy() {
		return outer.copy();
	}
	public String endpoint() {
		return endpoint;
	}
}
