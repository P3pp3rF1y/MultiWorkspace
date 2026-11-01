package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Opens only the synthetic vanilla menus used by the inventory-interaction regression. */
public final class OpenInventoryInteractionMenuAction extends TypedRegressionAction<OpenInventoryInteractionMenuAction.Arguments> {
	public record Arguments(VanillaMenuHandle.Kind kind, String fixture, String saveAs) {
	}

	public OpenInventoryInteractionMenuAction() {
		super("fixture.openInventoryInteractionMenu", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "fixture", "saveAs"), description);
		VanillaMenuHandle.Kind kind;
		try {
			kind = VanillaMenuHandle.Kind.valueOf(requireString(args, "kind", description).toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid vanilla menu kind on " + description, e);
		}
		String fixture = requireString(args, "fixture", description);
		if (!Set.of("transferToStorage", "transferToPlayer", "storageSort", "playerSort").contains(fixture)) {
			throw new IllegalArgumentException("Invalid inventory-interaction fixture on " + description);
		}
		if (kind == VanillaMenuHandle.Kind.CHEST && fixture.equals("playerSort")
				|| kind != VanillaMenuHandle.Kind.CHEST && !fixture.equals("playerSort")) {
			throw new IllegalArgumentException("Menu kind and fixture are incompatible on " + description);
		}
		return new Arguments(kind, fixture, RegressionValueStore.requireName(requireString(args, "saveAs", description)));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.VANILLA_MENU);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		@Nullable
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					player.closeContainer();
					player.getInventory().clearContent();
					if (arguments.fixture().equals("transferToStorage")) {
						player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
						player.getInventory().setItem(10, new ItemStack(Items.DIRT));
					} else if (arguments.fixture().equals("transferToPlayer")) {
						player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
					} else if (arguments.fixture().equals("playerSort")) {
						player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
						player.getInventory().setItem(10, new ItemStack(Items.COBBLESTONE, 2));
					}
					open(player);
					player.getInventory().setChanged();
					player.inventoryMenu.broadcastFullState();
					context.values().put(arguments.saveAs(), VanillaMenuHandle.class,
							new VanillaMenuHandle(arguments.kind(), player.containerMenu.containerId));
				});
			}
			return await(future);
		}

		private void open(ServerPlayer player) {
			if (arguments.kind() == VanillaMenuHandle.Kind.CHEST) {
				SimpleContainer storage = new SimpleContainer(27);
				storage.setItem(0, new ItemStack(Items.COBBLESTONE));
				if (arguments.fixture().equals("transferToPlayer")) {
					storage.setItem(1, new ItemStack(Items.DIRT));
				} else if (arguments.fixture().equals("storageSort")) {
					storage.setItem(5, new ItemStack(Items.COBBLESTONE, 2));
				}
				player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> ChestMenu.threeRows(id, inventory, storage),
						Component.literal("Inventory interaction regression")));
			} else if (arguments.kind() == VanillaMenuHandle.Kind.CRAFTING) {
				player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new CraftingMenu(id, inventory),
						Component.literal("Inventory interaction regression")));
			} else {
				player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new FurnaceMenu(id, inventory),
						Component.literal("Inventory interaction regression")));
			}
		}
	}
}
