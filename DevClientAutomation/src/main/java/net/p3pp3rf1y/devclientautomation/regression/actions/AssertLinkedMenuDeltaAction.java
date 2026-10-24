package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstoragecreateintegration.common.MountedStorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertLinkedMenuDeltaAction extends TypedRegressionAction<AssertLinkedMenuDeltaAction.Arguments> {
	public record Arguments(MenuKind kind, String ref, String baseline, String item, int count, int slot) {
	}

	public AssertLinkedMenuDeltaAction() {
		super("assert.linkedMenuDelta", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "ref", "baseline", "item", "count", "slot"), description);
		MenuKind kind = MenuKind.parse(requireString(args, "kind", description));
		if (kind == MenuKind.BACKPACK_BLOCK) {
			throw new IllegalArgumentException("Argument kind on " + description + " must be moving, mounted, storageBlock or backpackItem");
		}
		return new Arguments(kind,
				kind == MenuKind.STORAGE_BLOCK || kind == MenuKind.BACKPACK_ITEM
						? ""
						: RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "baseline", description)), requireString(args, "item", description),
				requirePositiveInt(args, "count", description), optionalNonNegativeInt(args, "slot", 0, description));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		RegressionValueContract inputs = switch (arguments.kind()) {
			case MOVING -> RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE);
			case MOUNTED -> RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOUNTED_STORAGE);
			default -> RegressionValueContract.EMPTY;
		};
		return inputs.and(RegressionValueContract.consuming(arguments.baseline(), RegressionValueTypes.MENU_BASELINE));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
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

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private final CaptureLinkedMenuRevisionAction.Baseline baseline;
		private final Item item;
		private CompletableFuture<Void> serverAssertion;
		private boolean serverMatched;
		private int deadline = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
			baseline = context.values().require(arguments.baseline(), CaptureLinkedMenuRevisionAction.Baseline.class);
			item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(arguments.item()))
					.orElseThrow(() -> new IllegalArgumentException("Unknown asserted item " + arguments.item()));
		}

		@Override
		public RegressionActionProgress tick() {
			if (deadline < 0) {
				deadline = context.minecraft().player.tickCount + 100;
			}
			if (serverAssertion == null) {
				serverAssertion = context.onServer(player -> {
					StorageContainerMenuBase<?> menu;
					if (arguments.kind() == MenuKind.MOVING) {
						MovingStorageEntityHandle handle = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
						if (!(player.containerMenu instanceof MovingStorageContainerMenu<?> movingMenu)
								|| movingMenu.getStorageEntity().filter(entity -> entity.getUUID().equals(handle.id())).isEmpty()) {
							throw RegressionActionFailure.infrastructure("Expected moving menu closed on server");
						}
						menu = movingMenu;
					} else if (arguments.kind() == MenuKind.MOUNTED) {
						MountedLinkedStorageHandle handle = context.values().require(arguments.ref(), MountedLinkedStorageHandle.class);
						if (!(player.containerMenu instanceof MountedStorageContainerMenu mountedMenu)
								|| mountedMenu.getContraptionEntity().filter(entity -> entity.getUUID().equals(handle.entityId())).isEmpty()
								|| !mountedMenu.getLocalPos().equals(handle.localPos())) {
							throw RegressionActionFailure.infrastructure("Expected mounted menu closed on server");
						}
						menu = mountedMenu;
					} else if (arguments.kind() == MenuKind.STORAGE_BLOCK) {
						if (!(player.containerMenu instanceof StorageContainerMenu storageMenu)
								|| !storageMenu.getStorageBlockEntity().getBlockPos().equals(context.target())) {
							throw RegressionActionFailure.infrastructure("Expected storage block menu closed on server");
						}
						menu = storageMenu;
					} else {
						if (!(player.containerMenu instanceof BackpackContainer backpackMenu)
								|| backpackMenu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
							throw RegressionActionFailure.infrastructure("Expected held backpack menu closed on server");
						}
						menu = backpackMenu;
					}
					serverMatched = LinkedStorageGroupsSavedData.get(player.serverLevel()).manager().getRevision(baseline.groupId()) > baseline.revision()
							&& matches(menu);
				});
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress progress = await(serverAssertion);
			if (progress != RegressionActionProgress.PASSED) {
				return progress;
			}
			if (!serverMatched) {
				serverAssertion = null;
				return context.minecraft().player.tickCount >= deadline
						? failAssertion("Server linked group revision or menu slot did not change")
						: RegressionActionProgress.WAITING;
			}
			if (ClientLinkedStorageContents.getRevision(baseline.groupId()).orElse(-1L) != baseline.revision()) {
				return failAssertion("Client linked group received a full snapshot instead of only a menu slot update");
			}
			StorageContainerMenuBase<?> menu;
			if (arguments.kind() == MenuKind.MOVING) {
				MovingStorageEntityHandle handle = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
				if (!(context.minecraft().player.containerMenu instanceof MovingStorageContainerMenu<?> movingMenu)
						|| movingMenu.getStorageEntity().filter(entity -> entity.getUUID().equals(handle.id())).isEmpty()) {
					return fail("Expected moving client menu closed before slot synchronization");
				}
				menu = movingMenu;
			} else if (arguments.kind() == MenuKind.MOUNTED) {
				MountedLinkedStorageHandle handle = context.values().require(arguments.ref(), MountedLinkedStorageHandle.class);
				if (!(context.minecraft().player.containerMenu instanceof MountedStorageContainerMenu mountedMenu)
						|| mountedMenu.getContraptionEntity().filter(entity -> entity.getUUID().equals(handle.entityId())).isEmpty()
						|| !mountedMenu.getLocalPos().equals(handle.localPos())) {
					return fail("Expected mounted client menu closed before slot synchronization");
				}
				menu = mountedMenu;
			} else if (arguments.kind() == MenuKind.STORAGE_BLOCK) {
				if (!(context.minecraft().player.containerMenu instanceof StorageContainerMenu storageMenu)
						|| !storageMenu.getStorageBlockEntity().getBlockPos().equals(context.target())) {
					return fail("Expected storage block client menu closed before slot synchronization");
				}
				menu = storageMenu;
			} else {
				if (!(context.minecraft().player.containerMenu instanceof BackpackContainer backpackMenu)
						|| backpackMenu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
					return fail("Expected held backpack client menu closed before slot synchronization");
				}
				menu = backpackMenu;
			}
			return matches(menu)
					? RegressionActionProgress.PASSED
					: context.minecraft().player.tickCount >= deadline
							? failAssertion("Client menu slot did not receive the changed stack")
							: RegressionActionProgress.WAITING;
		}

		private boolean matches(StorageContainerMenuBase<?> menu) {
			ItemStack stack = menu.getSlot(arguments.slot()).getItem();
			return stack.is(item) && stack.getCount() == arguments.count();
		}
	}
}
