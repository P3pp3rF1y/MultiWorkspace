package net.p3pp3rf1y.devclientautomation.regression.actions;

public enum MenuKind {
	BACKPACK_BLOCK, BACKPACK_ITEM, STORAGE_BLOCK, MOVING, MOUNTED;
	public static MenuKind parse(String name) {
		return switch (name) {
			case "backpackBlock" -> BACKPACK_BLOCK;
			case "backpackItem" -> BACKPACK_ITEM;
			case "storageBlock" -> STORAGE_BLOCK;
			case "moving" -> MOVING;
			case "mounted" -> MOUNTED;
			default -> throw new IllegalArgumentException("Argument kind must be backpackBlock, backpackItem, storageBlock, or moving: " + name);
		};
	}
}
