package net.p3pp3rf1y.devclientautomation.scenarios.inventory;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.devclientautomation.platform.neoforge.NeoForgeInventoryInteractionHooks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime.runOnClient;
import static net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime.runOnServer;

public final class InventoryInteractionsKeybindRegression {
	private InventoryInteractionsKeybindRegression() {
	}

	public static String run() {
		InventoryInteractionKeyMappings originalMappings = runOnClient(InventoryInteractionsKeybindRegression::configureInventoryInteractionKeyMappings);
		List<String> cases = new ArrayList<>();
		try {
			runVanillaTransferKeybindRegression(true, false);
			cases.add("vanillaTransferToStorageFiltered");
			runVanillaTransferKeybindRegression(true, true);
			cases.add("vanillaTransferToStorageAll");
			runVanillaTransferKeybindRegression(false, false);
			cases.add("vanillaTransferToPlayerFiltered");
			runVanillaTransferKeybindRegression(false, true);
			cases.add("vanillaTransferToPlayerAll");
			runBackpackTransferKeybindRegression(true, false);
			cases.add("backpackTransferToStorageFiltered");
			runBackpackTransferKeybindRegression(true, true);
			cases.add("backpackTransferToStorageAll");
			runBackpackTransferKeybindRegression(false, false);
			cases.add("backpackTransferToPlayerFiltered");
			runBackpackTransferKeybindRegression(false, true);
			cases.add("backpackTransferToPlayerAll");
			runVanillaSortKeybindRegression();
			cases.add("vanillaSort");
			runPlayerInventorySortKeybindRegression();
			cases.add("playerInventorySort");
			runCraftingPlayerInventorySortKeybindRegression();
			cases.add("craftingPlayerInventorySort");
			runFurnacePlayerInventorySortKeybindRegression();
			cases.add("furnacePlayerInventorySort");
			runBackpackSortKeybindRegression();
			cases.add("backpackSort");
			return "{\"ok\":true,\"cases\":[\"" + String.join("\",\"", cases) + "\"]}";
		} finally {
			runOnClient(() -> {
				restoreInventoryInteractionKeyMappings(originalMappings);
				return null;
			});
			runOnServer(player -> {
				player.closeContainer();
				return null;
			});
		}
	}

	private static InventoryInteractionKeyMappings configureInventoryInteractionKeyMappings() {
		KeyMapping sortKeybind = ClientEventHandler.SORT_KEYBIND;
		KeyMapping transferToStorageKeybind = ClientEventHandler.TRANSFER_TO_STORAGE_KEYBIND;
		KeyMapping transferToInventoryKeybind = ClientEventHandler.TRANSFER_TO_INVENTORY_KEYBIND;
		InventoryInteractionKeyMappings originalMappings = new InventoryInteractionKeyMappings(sortKeybind.getKey(), transferToStorageKeybind.getKey(),
				transferToInventoryKeybind.getKey());
		sortKeybind.setKey(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_MIDDLE));
		transferToStorageKeybind.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET));
		transferToInventoryKeybind.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET));
		KeyMapping.resetMapping();
		return originalMappings;
	}

	private static void restoreInventoryInteractionKeyMappings(InventoryInteractionKeyMappings originalMappings) {
		ClientEventHandler.SORT_KEYBIND.setKey(originalMappings.sort());
		ClientEventHandler.TRANSFER_TO_STORAGE_KEYBIND.setKey(originalMappings.transferToStorage());
		ClientEventHandler.TRANSFER_TO_INVENTORY_KEYBIND.setKey(originalMappings.transferToInventory());
		KeyMapping.resetMapping();
	}

	private static void runVanillaTransferKeybindRegression(boolean toStorage, boolean shift) {
		int containerId = runOnServer(player -> {
			prepareVanillaTransferKeybindRegression(player, toStorage);
			return player.containerMenu.containerId;
		});
		waitForClientScreen("vanilla chest", () -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
				&& screen.getMenu() instanceof ChestMenu && screen.getMenu().containerId == containerId);
		requireHandled(pressTransferKeybind(toStorage, shift), "Vanilla transfer keybind was not handled");
		waitForServerCondition("vanilla transfer", player -> vanillaTransferMatches(player, toStorage, shift));
	}

	private static void prepareVanillaTransferKeybindRegression(ServerPlayer player, boolean toStorage) {
		player.closeContainer();
		player.getInventory().clearContent();
		SimpleContainer container = new SimpleContainer(27);
		container.setItem(0, new ItemStack(Items.COBBLESTONE));
		if (toStorage) {
			player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
			player.getInventory().setItem(10, new ItemStack(Items.DIRT));
		} else {
			container.setItem(1, new ItemStack(Items.DIRT));
			player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
		}
		player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> ChestMenu.threeRows(windowId, inventory, container),
				Component.literal("Inventory interaction regression")));
	}

	private static boolean vanillaTransferMatches(ServerPlayer player, boolean toStorage, boolean shift) {
		if (!(player.containerMenu instanceof ChestMenu menu)) {
			return false;
		}
		SimpleContainer container = (SimpleContainer) menu.getContainer();
		return transferMatches(countItems(container, Items.COBBLESTONE), countItems(container, Items.DIRT), countItems(player, Items.COBBLESTONE),
				countItems(player, Items.DIRT), toStorage, shift);
	}

	private static void runBackpackTransferKeybindRegression(boolean toStorage, boolean shift) {
		runOnServer(player -> {
			prepareBackpackTransferKeybindRegression(player, toStorage);
			return null;
		});
		waitForClientBackpackInHotbar();
		int containerId = runOnServer(player -> {
			openMainBackpack(player);
			return player.containerMenu.containerId;
		});
		waitForClientScreen("backpack", () -> Minecraft.getInstance().screen instanceof BackpackScreen screen && screen.getMenu().containerId == containerId);
		requireHandled(pressTransferKeybind(toStorage, shift), "Backpack transfer keybind was not handled");
		try {
			waitForServerCondition("backpack transfer", player -> backpackTransferMatches(player, toStorage, shift));
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage() + ": " + runOnServer(InventoryInteractionsKeybindRegression::backpackTransferState), e);
		}
	}

	private static void prepareBackpackTransferKeybindRegression(ServerPlayer player, boolean toStorage) {
		player.closeContainer();
		player.getInventory().clearContent();
		ItemStack backpack = createBackpackStack();
		InventoryHandler inventory = BackpackWrapper.fromStack(backpack).getInventoryHandler();
		inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
		if (!toStorage) {
			inventory.setStackInSlot(1, new ItemStack(Items.DIRT));
		}
		inventory.saveInventory();
		player.getInventory().setItem(0, backpack);
		player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
		if (toStorage) {
			player.getInventory().setItem(10, new ItemStack(Items.DIRT));
		}
		player.getInventory().setChanged();
	}

	private static boolean backpackTransferMatches(ServerPlayer player, boolean toStorage, boolean shift) {
		InventoryHandler inventory = getMainBackpackWrapper(player).getInventoryHandler();
		return transferMatches(countItems(inventory, Items.COBBLESTONE), countItems(inventory, Items.DIRT), countItems(player, Items.COBBLESTONE),
				countItems(player, Items.DIRT), toStorage, shift);
	}

	private static String backpackTransferState(ServerPlayer player) {
		InventoryHandler inventory = getMainBackpackWrapper(player).getInventoryHandler();
		return "storage cobblestone=" + countItems(inventory, Items.COBBLESTONE) + ", dirt=" + countItems(inventory, Items.DIRT) + "; player cobblestone="
				+ countItems(player, Items.COBBLESTONE) + ", dirt=" + countItems(player, Items.DIRT);
	}

	private static boolean transferMatches(int containerCobblestone, int containerDirt, int playerCobblestone, int playerDirt, boolean toStorage,
			boolean shift) {
		if (toStorage) {
			return containerCobblestone == 2 && containerDirt == (shift ? 1 : 0) && playerCobblestone == 0 && playerDirt == (shift ? 0 : 1);
		}
		return containerCobblestone == 0 && containerDirt == (shift ? 0 : 1) && playerCobblestone == 2 && playerDirt == (shift ? 1 : 0);
	}

	private static void runVanillaSortKeybindRegression() {
		int containerId = runOnServer(player -> {
			player.closeContainer();
			player.getInventory().clearContent();
			SimpleContainer container = new SimpleContainer(27);
			container.setItem(0, new ItemStack(Items.COBBLESTONE));
			container.setItem(5, new ItemStack(Items.COBBLESTONE, 2));
			player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> ChestMenu.threeRows(windowId, inventory, container),
					Component.literal("Inventory interaction regression")));
			return player.containerMenu.containerId;
		});
		waitForClientScreen("vanilla chest", () -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
				&& screen.getMenu() instanceof ChestMenu && screen.getMenu().containerId == containerId);
		requireHandled(pressSortKeybind(0), "Vanilla sort keybind was not handled");
		try {
			waitForServerCondition("vanilla sort", player -> {
				if (!(player.containerMenu instanceof ChestMenu menu)) {
					return false;
				}
				SimpleContainer container = (SimpleContainer) menu.getContainer();
				return countItems(container, Items.COBBLESTONE) == 3 && countStacks(container, Items.COBBLESTONE) == 1;
			});
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage() + ": " + runOnServer(InventoryInteractionsKeybindRegression::vanillaSortState), e);
		}
	}

	private static String vanillaSortState(ServerPlayer player) {
		if (!(player.containerMenu instanceof ChestMenu menu)) {
			return "no chest menu";
		}
		SimpleContainer container = (SimpleContainer) menu.getContainer();
		return "cobblestone=" + countItems(container, Items.COBBLESTONE) + ", stacks=" + countStacks(container, Items.COBBLESTONE);
	}

	private static void runPlayerInventorySortKeybindRegression() {
		runOnServer(player -> {
			preparePlayerInventorySortKeybindRegression(player);
			return null;
		});
		boolean handled = runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null) {
				throw new IllegalStateException("Client player is not loaded");
			}
			InventoryScreen screen = new InventoryScreen(minecraft.player);
			minecraft.setScreen(screen);
			return NeoForgeInventoryInteractionHooks.onScreenMouseClickedPre(screen, 0, 0, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
		});
		requireHandled(handled, "Player inventory sort keybind was not handled");
		waitForPlayerInventorySort("player inventory sort");
	}

	private static void runCraftingPlayerInventorySortKeybindRegression() {
		int containerId = runOnServer(player -> {
			preparePlayerInventorySortKeybindRegression(player);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new CraftingMenu(windowId, inventory),
					Component.literal("Inventory interaction regression")));
			return player.containerMenu.containerId;
		});
		waitForClientScreen("crafting table",
				() -> Minecraft.getInstance().screen instanceof CraftingScreen screen && screen.getMenu().containerId == containerId);
		requireHandled(pressSortKeybind(10), "Crafting-table player inventory sort keybind was not handled");
		waitForPlayerInventorySort("crafting-table player inventory sort");
	}

	private static void runFurnacePlayerInventorySortKeybindRegression() {
		int containerId = runOnServer(player -> {
			preparePlayerInventorySortKeybindRegression(player);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new FurnaceMenu(windowId, inventory),
					Component.literal("Inventory interaction regression")));
			return player.containerMenu.containerId;
		});
		waitForClientScreen("furnace", () -> Minecraft.getInstance().screen instanceof FurnaceScreen screen && screen.getMenu().containerId == containerId);
		requireHandled(pressSortKeybind(3), "Furnace player inventory sort keybind was not handled");
		waitForPlayerInventorySort("furnace player inventory sort");
	}

	private static void preparePlayerInventorySortKeybindRegression(ServerPlayer player) {
		player.closeContainer();
		player.getInventory().clearContent();
		player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
		player.getInventory().setItem(10, new ItemStack(Items.COBBLESTONE, 2));
		player.getInventory().setChanged();
	}

	private static void waitForPlayerInventorySort(String description) {
		waitForServerCondition(description, player -> countItems(player, Items.COBBLESTONE) == 3 && countStacks(player, Items.COBBLESTONE) == 1);
	}

	private static void runBackpackSortKeybindRegression() {
		runOnServer(player -> {
			player.closeContainer();
			player.getInventory().clearContent();
			ItemStack backpack = createBackpackStack();
			InventoryHandler inventory = BackpackWrapper.fromStack(backpack).getInventoryHandler();
			inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
			inventory.setStackInSlot(5, new ItemStack(Items.COBBLESTONE, 2));
			inventory.saveInventory();
			player.getInventory().setItem(0, backpack);
			player.getInventory().setChanged();
			return null;
		});
		waitForClientBackpackInHotbar();
		int containerId = runOnServer(player -> {
			openMainBackpack(player);
			return player.containerMenu.containerId;
		});
		waitForClientScreen("backpack", () -> Minecraft.getInstance().screen instanceof BackpackScreen screen && screen.getMenu().containerId == containerId);
		requireHandled(pressSortKeybind(0), "Backpack sort keybind was not handled");
		waitForServerCondition("backpack sort", player -> {
			InventoryHandler inventory = getMainBackpackWrapper(player).getInventoryHandler();
			return countItems(inventory, Items.COBBLESTONE) == 3 && countStacks(inventory, Items.COBBLESTONE) == 1;
		});
	}

	private static boolean pressTransferKeybind(boolean toStorage, boolean shift) {
		int keyCode = toStorage ? GLFW.GLFW_KEY_LEFT_BRACKET : GLFW.GLFW_KEY_RIGHT_BRACKET;
		return postKeyPressed(keyCode, shift ? GLFW.GLFW_MOD_SHIFT : 0);
	}

	private static boolean postKeyPressed(int keyCode, int modifiers) {
		return runOnClient(() -> {
			Screen screen = Minecraft.getInstance().screen;
			if (screen == null) {
				throw new IllegalStateException("No screen is open for the transfer keybind");
			}
			return NeoForgeInventoryInteractionHooks.onScreenKeyPressedPre(screen, keyCode, 0, modifiers);
		});
	}

	private static boolean pressSortKeybind(int menuSlot) {
		return runOnClient(() -> {
			if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> containerScreen)) {
				throw new IllegalStateException("No container screen is open for the sort keybind");
			}
			if (menuSlot >= 0) {
				moveToSlot(menuSlot);
				setHoveredSlot(containerScreen, menuSlot);
			}
			return NeoForgeInventoryInteractionHooks.onScreenMouseClickedPre(containerScreen, 0, 0, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
		});
	}

	private static void waitForClientScreen(String description, BooleanSupplier condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (runOnClient(condition::getAsBoolean)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static void waitForServerCondition(String description, Function<ServerPlayer, Boolean> condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (runOnServer(condition)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static void requireHandled(boolean handled, String message) {
		if (!handled) {
			throw new IllegalStateException(message);
		}
	}

	private static void moveToSlot(int menuSlot) {
		Minecraft minecraft = Minecraft.getInstance();
		AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) minecraft.screen;
		Slot slot = containerScreen.getMenu().slots.get(menuSlot);
		int x = containerScreen.getGuiLeft() + slot.x + 8;
		int y = containerScreen.getGuiTop() + slot.y + 8;
		double scale = minecraft.getWindow().getGuiScale();
		GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), x * scale, y * scale);
		containerScreen.mouseMoved(x, y);
	}

	private static void setHoveredSlot(AbstractContainerScreen<?> screen, int menuSlot) {
		try {
			Field hoveredSlotField = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
			hoveredSlotField.setAccessible(true);
			hoveredSlotField.set(screen, screen.getMenu().slots.get(menuSlot));
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to set the hovered container slot", e);
		}
	}

	private static void waitForClientBackpackInHotbar() {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
		while (System.nanoTime() < deadline) {
			if (runOnClient(() -> Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.getInventory().getItem(0).getItem() instanceof BackpackItem)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for client backpack in hotbar slot 0");
	}

	private static void openMainBackpack(ServerPlayer player) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem backpackItem)) {
			throw new IllegalStateException("No backpack in player inventory slot 0");
		}
		player.getInventory().setSelectedSlot(0);
		backpackItem.use(player.level(), player, InteractionHand.MAIN_HAND);
	}

	private static ItemStack createBackpackStack() {
		ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 80);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);
		return backpack;
	}

	private static IBackpackWrapper getMainBackpackWrapper(ServerPlayer player) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("No backpack in player inventory slot 0");
		}
		return BackpackWrapper.fromStack(mainBackpack);
	}

	private static int countItems(SimpleContainer container, Item item) {
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int countItems(ServerPlayer player, Item item) {
		return player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	private static int countItems(InventoryHandler inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int countStacks(SimpleContainer container, Item item) {
		int stacks = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (container.getItem(slot).is(item)) {
				stacks++;
			}
		}
		return stacks;
	}

	private static int countStacks(ServerPlayer player, Item item) {
		return (int) player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(item)).count();
	}

	private static int countStacks(InventoryHandler inventory, Item item) {
		int stacks = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			if (inventory.getStackInSlot(slot).is(item)) {
				stacks++;
			}
		}
		return stacks;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record InventoryInteractionKeyMappings(InputConstants.Key sort, InputConstants.Key transferToStorage, InputConstants.Key transferToInventory) {
	}
}
