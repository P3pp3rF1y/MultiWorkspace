package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;

import java.util.Set;
import java.util.UUID;

public final class CaptureLinkedMenuRevisionAction extends TypedRegressionAction<CaptureLinkedMenuRevisionAction.Arguments> {
	public record Arguments(String kind, String ref, String saveAs) {
	}

	public record Baseline(UUID groupId, long revision) {
	}

	public CaptureLinkedMenuRevisionAction() {
		super("menu.captureLinkedRevision", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "ref", "saveAs"), description);
		String kind = requireString(args, "kind", description);
		if (!Set.of("moving", "mounted", "storageBlock", "backpackItem").contains(kind)) {
			throw new IllegalArgumentException("Argument kind on " + description + " must be moving, mounted, storageBlock or backpackItem");
		}
		return new Arguments(kind,
				Set.of("storageBlock", "backpackItem").contains(kind) ? "" : RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "saveAs", description)));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				UUID groupId;
				if (arguments.kind().equals("moving")) {
					MovingStorageEntityHandle handle = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
					if (!(context.minecraft().player.containerMenu instanceof MovingStorageContainerMenu<?> menu)
							|| menu.getStorageEntity().filter(entity -> entity.getUUID().equals(handle.id())).isEmpty()
							|| !context.isExpectedMenu("moving", arguments.ref())) {
						return fail("Expected linked moving menu is not open");
					}
					groupId = menu.getStorageEntity().orElseThrow().getStorageItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId();
				} else if (arguments.kind().equals("mounted")) {
					if (!MountedLinkedStorageMenuAccess.isMountedMenu(context, arguments.ref(), context.minecraft().player.containerMenu,
							context.minecraft().gui.screen())) {
						return fail("Expected linked mounted menu is not open");
					}
					groupId = MountedLinkedStorageMenuAccess.groupId(context, arguments.ref());
				} else if (arguments.kind().equals("storageBlock")) {
					if (!(context.minecraft().player.containerMenu instanceof StorageContainerMenu menu)
							|| !menu.getStorageBlockEntity().getBlockPos().equals(context.target())
							|| menu.getStorageBlockEntity().getLinkedStorageEndpointData() == null || !context.isExpectedMenu("storageBlock")) {
						return fail("Expected linked storage block menu is not open");
					}
					groupId = menu.getStorageBlockEntity().getLinkedStorageEndpointData().groupId();
				} else {
					if (!(context.minecraft().player.containerMenu instanceof BackpackContainer menu)
							|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK
							|| menu.getStorageWrapper().getBackpack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) == null
							|| !context.isExpectedMenu("backpackItem")) {
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
		return Set.of("minecraft:client", switch (arguments.kind()) {
			case "moving" -> "sophisticatedstorageinmotion";
			case "mounted" -> "sophisticatedstoragecreateintegration";
			case "storageBlock" -> "sophisticatedstorage";
			case "backpackItem" -> "sophisticatedbackpacks";
			default -> throw new IllegalStateException("Unsupported linked menu kind " + arguments.kind());
		});
	}
}
