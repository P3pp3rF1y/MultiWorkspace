package net.p3pp3rf1y.devclientautomation.regression.actions;

/** Immutable identity for a synthetic vanilla menu; deliberately contains no live menu or container. */
public record VanillaMenuHandle(Kind kind, int containerId) {
	public enum Kind {
		CHEST, CRAFTING, FURNACE, SMITHING
	}
}
