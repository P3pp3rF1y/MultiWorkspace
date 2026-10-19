package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertLinkedMenuDeltaAction extends TypedRegressionAction<AssertLinkedMenuDeltaAction.Arguments> {
	public record Arguments(String kind, String ref, String baseline, String item, int count, int slot) {
	}

	public AssertLinkedMenuDeltaAction() {
		super("assert.linkedMenuDelta", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "ref", "baseline", "item", "count", "slot"), description);
		String kind = requireString(args, "kind", description);
		if (!Set.of("moving", "mounted", "storageBlock", "backpackItem").contains(kind)) {
			throw new IllegalArgumentException("Argument kind on " + description + " must be moving, mounted, storageBlock or backpackItem");
		}
		return new Arguments(kind,
				Set.of("storageBlock", "backpackItem").contains(kind) ? "" : RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "baseline", description)), requireString(args, "item", description),
				requirePositiveInt(args, "count", description), optionalNonNegativeInt(args, "slot", 0, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
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
			item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
					.orElseThrow(() -> new IllegalArgumentException("Unknown asserted item " + arguments.item()));
		}

		@Override
		public RegressionActionProgress tick() {
			if (deadline < 0) {
				deadline = context.minecraft().player.tickCount + 100;
			}
			if (serverAssertion == null) {
				serverAssertion = context.onServer(player -> {
					StorageContainerMenuBase<?> menu = requireMenu(player);
					serverMatched = LinkedStorageGroupsSavedData.get(player.level()).manager().getRevision(baseline.groupId()) > baseline.revision()
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
						? fail("Server linked group revision or menu slot did not change")
						: RegressionActionProgress.WAITING;
			}
			if (ClientLinkedStorageContents.getRevision(baseline.groupId()).orElse(-1L) != baseline.revision()) {
				return fail("Client linked group received a full snapshot instead of only a menu slot update");
			}
			try {
				return matches(requireMenu(context.minecraft().player))
						? RegressionActionProgress.PASSED
						: context.minecraft().player.tickCount >= deadline
								? fail("Client menu slot did not receive the changed stack")
								: RegressionActionProgress.WAITING;
			} catch (IllegalStateException e) {
				return fail(e.getMessage());
			}
		}

		private StorageContainerMenuBase<?> requireMenu(net.minecraft.world.entity.player.Player player) {
			if (arguments.kind().equals("moving")) {
				MovingStorageEntityHandle handle = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
				if (player.containerMenu instanceof MovingStorageContainerMenu<?> menu
						&& menu.getStorageEntity().filter(entity -> entity.getUUID().equals(handle.id())).isPresent()) {
					return menu;
				}
			} else if (arguments.kind().equals("mounted")) {
				return MountedLinkedStorageMenuAccess.requireMenu(context, arguments.ref(), player.containerMenu);
			} else if (arguments.kind().equals("storageBlock")) {
				if (player.containerMenu instanceof StorageContainerMenu menu && menu.getStorageBlockEntity().getBlockPos().equals(context.target())) {
					return menu;
				}
			} else if (player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK) {
				return menu;
			}
			throw new IllegalStateException("Expected " + arguments.kind() + " menu closed before slot synchronization");
		}

		private boolean matches(StorageContainerMenuBase<?> menu) {
			ItemStack stack = menu.getSlot(arguments.slot()).getItem();
			return stack.is(item) && stack.getCount() == arguments.count();
		}
	}
}
