package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Bridges compiled linked-menu assertions to the optional Create mounted-menu implementation. */
final class MountedLinkedStorageMenuAccess {
	private static final String HANDLE_CLASS = "net.p3pp3rf1y.devclientautomation.regression.actions.MountedLinkedStorageHandle";
	private static final String MENU_CLASS = "net.p3pp3rf1y.sophisticatedstoragecreateintegration.common.MountedStorageContainerMenu";
	private static final String SCREEN_CLASS = "net.p3pp3rf1y.sophisticatedstoragecreateintegration.client.MountedStorageScreen";

	private MountedLinkedStorageMenuAccess() {
	}

	static UUID groupId(RegressionExecutionContext context, String ref) {
		try {
			return ((LinkedStorageEndpointData) invoke(handle(context, ref), "endpoint")).groupId();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Mounted linked-storage handle does not expose its endpoint", e);
		}
	}

	static boolean isMountedMenu(RegressionExecutionContext context, String ref, Object menu, Object screen) {
		return isInstance(SCREEN_CLASS, screen) && hasMountedIdentity(context, ref, menu);
	}

	static StorageContainerMenuBase<?> requireMenu(RegressionExecutionContext context, String ref, Object menu) {
		if (!hasMountedIdentity(context, ref, menu) || !(menu instanceof StorageContainerMenuBase<?> storageMenu)) {
			throw new IllegalStateException("Expected mounted menu closed before slot synchronization");
		}
		return storageMenu;
	}

	private static boolean hasMountedIdentity(RegressionExecutionContext context, String ref, Object menu) {
		if (!isInstance(MENU_CLASS, menu)) {
			return false;
		}
		try {
			Object handle = handle(context, ref);
			Object contraptionEntity = invoke(menu, "getContraptionEntity");
			if (!(contraptionEntity instanceof Optional<?> entities) || entities.isEmpty()) {
				return false;
			}
			return invoke(entities.orElseThrow(), "getUUID").equals(invoke(handle, "entityId"))
					&& invoke(menu, "getLocalPos").equals(invoke(handle, "localPos"));
		} catch (ReflectiveOperationException e) {
			return false;
		}
	}

	private static boolean isInstance(String className, Object value) {
		try {
			return value != null && Class.forName(className).isInstance(value);
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private static Object handle(RegressionExecutionContext context, String ref) {
		try {
			return require(context, ref, Class.forName(HANDLE_CLASS));
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Mounted linked-storage action implementation is unavailable", e);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object require(RegressionExecutionContext context, String ref, Class<?> type) {
		return context.values().require(ref, (Class) type);
	}

	private static Object invoke(Object target, String name) throws ReflectiveOperationException {
		Method method = target.getClass().getMethod(name);
		return method.invoke(target);
	}
}
