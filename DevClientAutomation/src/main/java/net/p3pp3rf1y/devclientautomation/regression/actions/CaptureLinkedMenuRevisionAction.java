package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstoragecreateintegration.common.MountedStorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;

import java.util.Set;
import java.util.UUID;

public final class CaptureLinkedMenuRevisionAction extends TypedRegressionAction<CaptureLinkedMenuRevisionAction.Arguments> {
	public record Arguments(MenuKind kind, String ref, String saveAs) {
	}

	public record Baseline(UUID groupId, long revision) {
	}

	public CaptureLinkedMenuRevisionAction() {
		super("menu.captureLinkedRevision", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "ref", "saveAs"), description);
		MenuKind kind = MenuKind.parse(requireString(args, "kind", description));
		if (kind == MenuKind.BACKPACK_BLOCK) {
			throw new IllegalArgumentException("Argument kind on " + description + " must be moving, mounted, storageBlock or backpackItem");
		}
		return new Arguments(kind,
				kind == MenuKind.STORAGE_BLOCK || kind == MenuKind.BACKPACK_ITEM
						? ""
						: RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "saveAs", description)));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				UUID groupId;
				if (arguments.kind() == MenuKind.MOVING) {
					MovingStorageEntityHandle handle = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
					if (!(context.minecraft().player.containerMenu instanceof MovingStorageContainerMenu<?> menu)
							|| menu.getStorageEntity().filter(entity -> entity.getUUID().equals(handle.id())).isEmpty()) {
						return fail("Expected linked moving menu is not open");
					}
					groupId = menu.getStorageEntity().orElseThrow().getStorageItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId();
				} else if (arguments.kind() == MenuKind.MOUNTED) {
					MountedLinkedStorageHandle handle = context.values().require(arguments.ref(), MountedLinkedStorageHandle.class);
					if (!(context.minecraft().player.containerMenu instanceof MountedStorageContainerMenu menu)
							|| menu.getContraptionEntity().filter(entity -> entity.getUUID().equals(handle.entityId())).isEmpty()
							|| !menu.getLocalPos().equals(handle.localPos())) {
						return fail("Expected linked mounted menu is not open");
					}
					groupId = handle.endpoint().groupId();
				} else if (arguments.kind() == MenuKind.STORAGE_BLOCK) {
					if (!(context.minecraft().player.containerMenu instanceof StorageContainerMenu menu)
							|| !menu.getStorageBlockEntity().getBlockPos().equals(context.target())
							|| menu.getStorageBlockEntity().getLinkedStorageEndpointData() == null) {
						return fail("Expected linked storage block menu is not open");
					}
					groupId = menu.getStorageBlockEntity().getLinkedStorageEndpointData().groupId();
				} else {
					if (!(context.minecraft().player.containerMenu instanceof BackpackContainer menu)
							|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK
							|| menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null) {
						return fail("Expected linked held backpack menu is not open");
					}
					groupId = menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId();
				}
				long revision = ClientLinkedStorageContents.getRevision(groupId)
						.orElseThrow(() -> new IllegalStateException("Linked menu has no opening group snapshot"));
				context.values().put(arguments.saveAs(), Baseline.class, new Baseline(groupId, revision));
				return RegressionActionProgress.PASSED;
			}
		};
	}

	@Override
	protected Set<String> dependencies(Arguments arguments) {
		return switch (arguments.kind()) {
			case MOVING -> Set.of("minecraft:client", "sophisticatedstorageinmotion");
			case MOUNTED -> Set.of("minecraft:client", "sophisticatedstoragecreateintegration");
			case STORAGE_BLOCK -> Set.of("minecraft:client", "sophisticatedstorage");
			case BACKPACK_ITEM -> Set.of("minecraft:client", "sophisticatedbackpacks");
			case BACKPACK_BLOCK -> throw new IllegalArgumentException("Unsupported linked menu kind");
		};
	}
}
