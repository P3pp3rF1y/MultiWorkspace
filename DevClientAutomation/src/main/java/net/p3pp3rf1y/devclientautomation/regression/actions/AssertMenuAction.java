package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertMenuAction extends TypedRegressionAction<AssertMenuAction.Arguments> {
	public record Arguments(MenuKind kind, int slot, String item, int count, boolean playerInventory, int stableTicks, @Nullable String ref) {
	}

	public AssertMenuAction() {
		super("assert.menu", "2", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "slot", "item", "count", "playerInventory", "stableTicks", "ref"), description);
		MenuKind kind = MenuKind.parse(optionalString(args, "kind", "backpackBlock", description));
		if (kind == MenuKind.MOUNTED) {
			throw new IllegalArgumentException("Argument kind on " + description + " must be backpackBlock, backpackItem, or storageBlock");
		}
		if (kind == MenuKind.MOVING != args.has("ref")) {
			throw new IllegalArgumentException("Argument ref on " + description + " is required exactly for moving menus");
		}
		return new Arguments(kind, optionalNonNegativeInt(args, "slot", 0, description), requireString(args, "item", description),
				optionalNonNegativeInt(args, "count", 1, description), optionalBoolean(args, "playerInventory", false, description),
				optionalNonNegativeInt(args, "stableTicks", 0, description),
				kind == MenuKind.MOVING ? RegressionValueStore.requireName(requireString(args, "ref", description)) : null);
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.kind() == MenuKind.MOVING
				? RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE)
				: RegressionValueContract.EMPTY;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		@Nullable
		private CompletableFuture<Void> serverAssertion;
		private boolean serverMatched;
		private int deadline = -1;
		private int stableSince = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (serverAssertion == null) {
				serverAssertion = context
						.onServer(player -> serverMatched = AwaitMenuAction.matchesServerMenu(player, arguments.kind(), arguments.ref(), context)
								&& player.containerMenu instanceof StorageContainerMenuBase<?> menu && matchesStack(menu));
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress server = await(serverAssertion);
			if (server != RegressionActionProgress.PASSED) {
				return server;
			}
			if (!serverMatched) {
				serverAssertion = null;
				stableSince = -1;
				return minecraft.player.tickCount >= deadline
						? failAssertion("Server menu did not receive the expected item stack")
						: RegressionActionProgress.WAITING;
			}
			if (!context.isExpectedMenu(arguments.kind(), arguments.ref())) {
				stableSince = -1;
				serverAssertion = null;
				return minecraft.player.tickCount >= deadline
						? fail("Expected " + arguments.kind() + " menu closed before client synchronization")
						: RegressionActionProgress.WAITING;
			}
			if (!matchesStack((StorageContainerMenuBase<?>) minecraft.player.containerMenu)) {
				stableSince = -1;
				serverAssertion = null;
				return minecraft.player.tickCount >= deadline
						? failAssertion("Client menu did not synchronize the expected item stack")
						: RegressionActionProgress.WAITING;
			}
			if (stableSince < 0) {
				stableSince = minecraft.player.tickCount;
			}
			if (minecraft.player.tickCount - stableSince >= arguments.stableTicks()) {
				return RegressionActionProgress.PASSED;
			}
			serverAssertion = null;
			return RegressionActionProgress.WAITING;
		}

		private boolean matchesStack(StorageContainerMenuBase<?> menu) {
			if (!arguments.playerInventory() && arguments.slot() >= menu.getNumberOfStorageInventorySlots()) {
				throw RegressionActionFailure
						.infrastructure("Menu has " + menu.getNumberOfStorageInventorySlots() + " storage slots; cannot assert slot " + arguments.slot());
			}
			ItemStack stack;
			if (arguments.playerInventory()) {
				stack = menu.slots.stream().filter(slot -> slot.container instanceof Inventory && slot.getContainerSlot() == arguments.slot()).findFirst()
						.map(Slot::getItem).orElseThrow(() -> RegressionActionFailure.infrastructure("Menu has no player inventory slot " + arguments.slot()));
			} else {
				stack = menu.getSlot(arguments.slot()).getItem();
			}
			return arguments.count() == 0 ? stack.isEmpty() : stack.is(itemForId(arguments.item())) && stack.getCount() == arguments.count();
		}

		private static Item itemForId(String itemId) {
			return BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId))
					.orElseThrow(() -> new IllegalArgumentException("Unknown asserted item " + itemId));
		}
	}
}
