package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.devclientautomation.input.VirtualInputState;
import net.p3pp3rf1y.devclientautomation.platform.neoforge.NeoForgeInventoryInteractionHooks;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

import java.util.Set;

/** Atomic production-hook keybind press with deterministic mapping and modifier restoration. */
public final class PressInventoryInteractionKeybindAction extends TypedRegressionAction<PressInventoryInteractionKeybindAction.Arguments> {
	public record Arguments(String operation, boolean shift, @Nullable String target, int slot, @Nullable String ref) {
	}

	public PressInventoryInteractionKeybindAction() {
		super("input.inventoryInteractionKeybind", "2", Set.of("minecraft:client", "sophisticatedinventoryinteractions"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("operation", "modifier", "target", "slot", "ref"), description);
		String operation = requireString(args, "operation", description);
		if (!Set.of("transferToContainer", "transferToPlayer", "sort").contains(operation)) {
			throw new IllegalArgumentException("Invalid inventory interaction operation on " + description);
		}
		String modifier = optionalString(args, "modifier", "none", description);
		if (!Set.of("none", "shift").contains(modifier)) {
			throw new IllegalArgumentException("Modifier must be none or shift on " + description);
		}
		if (!operation.equals("sort")) {
			if (args.has("target") || args.has("slot")) {
				throw new IllegalArgumentException("Transfer operations cannot specify target or slot on " + description);
			}
			return new Arguments(operation, modifier.equals("shift"), null, -1,
					args.has("ref") ? RegressionValueStore.requireName(requireString(args, "ref", description)) : null);
		}
		String target = requireString(args, "target", description);
		if (!Set.of("container", "player", "playerOnly").contains(target)) {
			throw new IllegalArgumentException("Invalid sort target on " + description);
		}
		if (target.equals("playerOnly") != !args.has("slot")) {
			throw new IllegalArgumentException("Sort slot is required exactly for container/player targets on " + description);
		}
		if (target.equals("playerOnly") && args.has("ref")) {
			throw new IllegalArgumentException("playerOnly sort cannot use a menu handle on " + description);
		}
		return new Arguments(operation, modifier.equals("shift"), target, target.equals("playerOnly") ? -1 : requireNonNegativeInt(args, "slot", description),
				args.has("ref") ? RegressionValueStore.requireName(requireString(args, "ref", description)) : null);
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.ref() == null ? RegressionValueContract.EMPTY : RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.VANILLA_MENU);
	}

	@Override
	protected Set<String> dependencies(Arguments arguments) {
		return arguments.ref() == null && !"playerOnly".equals(arguments.target())
				? Set.of("minecraft:client", "sophisticatedbackpacks", "sophisticatedinventoryinteractions")
				: metadata().dependencies();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().screen instanceof AbstractContainerScreen<?> screen)) {
					return fail("A live container screen is required");
				}
				if ("playerOnly".equals(arguments.target())) {
					if (!(screen instanceof InventoryScreen) || !(screen.getMenu() instanceof InventoryMenu)) {
						return fail("playerOnly sort requires the exact InventoryScreen and InventoryMenu");
					}
				} else if (arguments.ref() != null) {
					VanillaMenuHandle handle = context.values().require(arguments.ref(), VanillaMenuHandle.class);
					if (!matchesVanilla(screen, handle)) {
						return fail("Client screen does not match synthetic menu handle");
					}
				} else if (!(screen instanceof BackpackScreen) || !(screen.getMenu() instanceof BackpackContainer)) {
					return fail("An unhandled inventory interaction requires the exact backpack screen/menu");
				}
				if (arguments.operation().equals("sort") && !arguments.target().equals("playerOnly")) {
					if (arguments.slot() >= screen.getMenu().slots.size()) {
						return fail("Sort hover slot is outside menu");
					}
					VirtualInput.moveToSlot(screen, arguments.slot());
					Slot slot = screen.getMenu().getSlot(arguments.slot());
					boolean playerMain = slot.container == context.minecraft().player.getInventory() && slot.getContainerSlot() >= 0
							&& slot.getContainerSlot() <= 35;
					if (arguments.target().equals("player") != playerMain) {
						return fail("Sort hover does not belong to the requested menu region");
					}
				}
				return invoke(screen);
			}

			private RegressionActionProgress invoke(AbstractContainerScreen<?> screen) {
				KeyMapping sort = ClientEventHandler.SORT_KEYBIND;
				KeyMapping toContainer = ClientEventHandler.TRANSFER_TO_STORAGE_KEYBIND;
				KeyMapping toPlayer = ClientEventHandler.TRANSFER_TO_INVENTORY_KEYBIND;
				var oldSort = sort.getKey();
				var oldContainer = toContainer.getKey();
				var oldPlayer = toPlayer.getKey();
				int modifiers = arguments.shift() ? GLFW.GLFW_MOD_SHIFT : 0;
				RegressionActionProgress result;
				try {
					sort.setKey(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_MIDDLE));
					toContainer.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET));
					toPlayer.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET));
					KeyMapping.resetMapping();
					boolean handled = VirtualInputState.withModifiers(modifiers, () -> invokeHook(screen, modifiers));
					result = handled ? RegressionActionProgress.PASSED : fail("Production inventory interaction hook did not handle " + arguments.operation());
				} finally {
					sort.setKey(oldSort);
					toContainer.setKey(oldContainer);
					toPlayer.setKey(oldPlayer);
					KeyMapping.resetMapping();
				}
				if (!sort.getKey().equals(oldSort) || !toContainer.getKey().equals(oldContainer) || !toPlayer.getKey().equals(oldPlayer)) {
					return fail("Inventory interaction key mappings were not restored");
				}
				if (VirtualInputState.active()) {
					return fail("Virtual input modifier state leaked from inventory interaction hook");
				}
				return result;
			}

			private boolean invokeHook(AbstractContainerScreen<?> screen, int modifiers) {
				if (arguments.operation().equals("sort")) {
					return NeoForgeInventoryInteractionHooks.onScreenMouseClickedPre(screen, 0, 0, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
				}
				int key = arguments.operation().equals("transferToContainer") ? GLFW.GLFW_KEY_LEFT_BRACKET : GLFW.GLFW_KEY_RIGHT_BRACKET;
				return NeoForgeInventoryInteractionHooks.onScreenKeyPressedPre(screen, key, 0, modifiers);
			}
		};
	}

	private static boolean matchesVanilla(AbstractContainerScreen<?> screen, VanillaMenuHandle handle) {
		return screen.getMenu().containerId == handle.containerId() && switch (handle.kind()) {
			case CHEST -> screen.getMenu() instanceof ChestMenu;
			case CRAFTING -> screen instanceof CraftingScreen && screen.getMenu() instanceof CraftingMenu;
			case FURNACE -> screen instanceof FurnaceScreen && screen.getMenu() instanceof FurnaceMenu;
		};
	}
}
