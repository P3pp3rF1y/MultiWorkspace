package net.p3pp3rf1y.devclientautomation.regression.actions;

/** JVM-loadable keys for existing action values; actions may declare further binary type names. */
public final class RegressionValueTypes {
	private static final String PACKAGE = "net.p3pp3rf1y.devclientautomation.regression.actions.";
	public static final String UUID = "java.util.UUID";
	public static final String ITEM_STACK = "net.minecraft.world.item.ItemStack";
	public static final String MOVING_STORAGE = PACKAGE + "MovingStorageEntityHandle";
	public static final String MOUNTED_STORAGE = PACKAGE + "MountedLinkedStorageHandle";
	public static final String MENU_BASELINE = PACKAGE + "CaptureLinkedMenuRevisionAction$Baseline";
	public static final String NESTED_STORAGE = PACKAGE + "NestedMovingStorageSnapshot";
	public static final String RESOURCE_PROBE = PACKAGE + "StorageResourceCacheAction$Probe";
	public static final String ITEM_ENTITY = PACKAGE + "ItemEntityHandle";

	private RegressionValueTypes() {
	}
}
