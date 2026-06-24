package net.p3pp3rf1y.devclientautomation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

final class StorageGuiRegressionRunner {
	private static final Duration TASK_TIMEOUT = Duration.ofSeconds(10);

	private StorageGuiRegressionRunner() {
	}

	static String run() {
		StorageGuiRegressionSuite suite = loadStorageGuiRegressionSuite();
		List<StorageGuiRegressionResult> results = new ArrayList<>();
		for (StorageGuiRegressionScenario scenario : suite.scenarios()) {
			results.add(runStorageGuiRegressionScenario(scenario));
		}

		long failed = results.stream().filter(result -> !result.passed()).count();
		StringBuilder json = new StringBuilder("{\"ok\":").append(failed == 0).append(",\"total\":").append(results.size()).append(",\"failed\":")
				.append(failed).append(",\"results\":[");
		for (int i = 0; i < results.size(); i++) {
			if (i > 0) {
				json.append(',');
			}
			StorageGuiRegressionResult result = results.get(i);
			json.append('{').append(jsonProperty("name", result.name())).append(",\"passed\":").append(result.passed()).append(",\"actions\":")
					.append(result.actions()).append(",\"storageSlots\":").append(result.storageSlots()).append(",\"menuSlots\":").append(result.menuSlots())
					.append(",\"upgradeSlots\":").append(result.upgradeSlots()).append(',').append(jsonProperty("error", result.error())).append('}');
		}
		json.append("]}");

		return json.toString();
	}

	private static StorageGuiRegressionResult runStorageGuiRegressionScenario(StorageGuiRegressionScenario scenario) {
		try {
			resetBackpackGuiRegressionState();
			runOnServer(player -> setupPlacedBackpackStorageGuiRegression(player, scenario));
			runOnClient(() -> setupClientPlacedBackpackStorageGuiRegression(scenario));
			runOnServer(player -> openPlacedBackpackStorageGuiRegression(player, scenario));
			waitForOpenPlacedBackpackMenu();

			Map<String, List<StorageGuiSlotSnapshot>> slotSnapshots = new HashMap<>();
			for (StorageGuiAction action : scenario.actions()) {
				runStorageGuiAction(scenario, action, slotSnapshots);
			}

			StorageGuiRegressionState state = runOnClient(StorageGuiRegressionRunner::getStorageGuiRegressionState);
			return new StorageGuiRegressionResult(scenario.name(), true, scenario.actions().length, state.storageSlots(), state.menuSlots(),
					state.upgradeSlots(), null);
		} catch (RuntimeException e) {
			StorageGuiRegressionState state = runOnClient(StorageGuiRegressionRunner::getStorageGuiRegressionStateSafely);
			return new StorageGuiRegressionResult(scenario.name(), false, scenario.actions().length, state.storageSlots(), state.menuSlots(),
					state.upgradeSlots(), e.getMessage());
		}
	}

	private static void runStorageGuiAction(StorageGuiRegressionScenario scenario, StorageGuiAction action,
			Map<String, List<StorageGuiSlotSnapshot>> slotSnapshots) {
		switch (action.type()) {
			case "assertMenuSlotLayout" -> runOnClient(() -> assertStorageGuiMenuSlotLayout(scenario));
			case "assertProtectedStorageSlots" -> runOnClient(() -> assertProtectedStorageSlots(scenario, action.slots()));
			case "assertTrashSlotIndexCompatibility" -> runOnClient(() -> assertTrashSlotIndexCompatibility(action.slots()));
			case "assertScreenFindSlots" -> runOnClient(() -> assertScreenFindSlots(action.slots()));
			case "assertSlotRefsFind" -> runOnClient(() -> assertSlotRefsFind(action.slotRefs()));
			case "assertSlotRefsNotFind" -> runOnClient(() -> assertSlotRefsNotFind(action.slotRefs()));
			case "assertUpgradeTabSlots" -> runOnClient(() -> assertUpgradeTabSlots(action.slotRefs()));
			case "assertMobCatcherSlots" -> runOnClient(() -> assertMobCatcherSlots(scenario));
			case "assertCarriedEmpty" -> runOnClient(StorageGuiRegressionRunner::assertStorageGuiCarriedEmpty);
			case "assertCarriedStack" -> runOnClient(() -> assertStorageGuiCarriedStack(action));
			case "assertSlotContents" -> runOnClient(() -> assertStorageGuiSlotContents(action));
			case "snapshotSlotContents" -> slotSnapshots.put(action.snapshot(), runOnClient(() -> snapshotStorageGuiSlotContents(action)));
			case "assertSlotContentsUnchanged" -> runOnClient(() -> assertStorageGuiSlotContentsUnchanged(action, slotSnapshots));
			case "assertColumnState" -> runOnClient(() -> assertStorageGuiColumnState(action));
			case "setCarriedStack" -> runStorageGuiSetCarriedStack(action);
			case "clickSlot" -> runOnClient(() -> clickStorageGuiSlot(scenario, action));
			case "shiftClickSlot" -> runOnClient(() -> shiftClickStorageGuiSlot(scenario, action));
			case "pickupAllSlot" -> runOnClient(() -> pickupAllStorageGuiSlot(scenario, action));
			case "hotbarSwapSlot" -> runOnClient(() -> hotbarSwapStorageGuiSlot(scenario, action));
			case "throwSlot" -> runOnClient(() -> throwStorageGuiSlot(scenario, action));
			case "scrollStorage" -> runOnClient(() -> scrollStorageGui(action));
			case "dragCarriedStack" -> runStorageGuiDragCarriedStack(scenario, action);
			case "clickColumnUpgrade" -> runStorageGuiColumnUpgradeAction(action);
			default -> throw new IllegalArgumentException("Unknown storage GUI regression action " + action.type());
		}
	}

	private static Boolean assertStorageGuiMenuSlotLayout(StorageGuiRegressionScenario scenario) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		String error = getStorageGuiMenuSlotLayoutError(menu);
		if (error != null) {
			throw new IllegalStateException(scenario.name() + ": " + error);
		}
		return true;
	}

	private static String getStorageGuiMenuSlotLayoutError(BackpackContainer menu) {
		if (menu.getInventorySlotsSize() != menu.slots.size()) {
			return "inventory slot size does not match menu.slots size";
		}
		int expectedInventorySlots = menu.getNumberOfStorageInventorySlots() + StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS;
		if (menu.getInventorySlotsSize() != expectedInventorySlots) {
			return "inventory slot size mismatch expected=" + expectedInventorySlots + " actual=" + menu.getInventorySlotsSize();
		}
		if (menu.getFirstUpgradeSlot() != menu.slots.size()) {
			return "first upgrade slot does not start after menu.slots";
		}
		if (menu.getTotalSlotsNumber() != menu.slots.size() + menu.upgradeSlots.size()) {
			return "total slot count does not equal inventory plus upgrade slots";
		}

		for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
			Slot slot = menu.slots.get(slotId);
			if (slot.index != slotId) {
				return "menu slot " + slotId + " has Slot.index " + slot.index;
			}
			if (menu.getSlot(slotId) != slot) {
				return "getSlot(" + slotId + ") does not return menu.slots entry";
			}
			if (slotId < menu.getNumberOfStorageInventorySlots() && slot.getSlotIndex() != slotId) {
				return "storage menu slot " + slotId + " points to backing slot " + slot.getSlotIndex();
			}
		}

		for (int upgradeSlot = 0; upgradeSlot < menu.upgradeSlots.size(); upgradeSlot++) {
			int logicalSlot = menu.getFirstUpgradeSlot() + upgradeSlot;
			Slot slot = menu.upgradeSlots.get(upgradeSlot);
			if (slot.index != logicalSlot) {
				return "upgrade slot " + upgradeSlot + " has Slot.index " + slot.index + " expected " + logicalSlot;
			}
			if (menu.getSlot(logicalSlot) != slot) {
				return "getSlot(" + logicalSlot + ") does not return upgrade slot";
			}
		}

		return null;
	}

	private static Boolean assertProtectedStorageSlots(StorageGuiRegressionScenario scenario, int[] slots) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		NoSortSettingsCategory noSortSettings = menu.getStorageWrapper().getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class);
		MemorySettingsCategory memorySettings = menu.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
		for (int slotId : slots) {
			assertStorageSlotPresent(menu, slotId);
			if (contains(scenario.noSortSlots(), slotId) && !noSortSettings.isSlotSelected(slotId)) {
				throw new IllegalStateException("No Sort slot " + slotId + " was not selected");
			}
			if (contains(scenario.memorySlots(), slotId) && !memorySettings.isSlotSelected(slotId)) {
				throw new IllegalStateException("Memory slot " + slotId + " was not selected");
			}
		}
		return true;
	}

	private static Boolean assertTrashSlotIndexCompatibility(int[] slots) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		for (int slotId : slots) {
			Slot slot = assertStorageSlotPresent(menu, slotId);
			if (slot.index < 0 || slot.index >= menu.slots.size()) {
				throw new IllegalStateException("Slot " + slotId + " has index outside menu.slots: " + slot.index);
			}
			Slot slotByIndex = menu.slots.get(slot.index);
			if (slotByIndex != slot) {
				throw new IllegalStateException("menu.slots.get(Slot.index) did not resolve storage slot " + slotId);
			}
			if (!ItemStack.matches(slot.getItem(), slotByIndex.getItem())) {
				throw new IllegalStateException("menu.slots.get(Slot.index) item mismatch for storage slot " + slotId);
			}
		}
		return true;
	}

	private static Boolean assertScreenFindSlots(int[] slots) {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
			throw new IllegalStateException("Backpack screen is not open");
		}
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		for (int slotId : slots) {
			Slot slot = assertStorageSlotPresent(menu, slotId);
			Slot foundSlot = screen.findSlot(getSlotCenterX(screen, slot), getSlotCenterY(screen, slot));
			if (foundSlot != slot) {
				throw new IllegalStateException("findSlot did not return storage slot " + slotId + "; found=" + (foundSlot == null ? "null" : foundSlot.index));
			}
		}
		return true;
	}

	private static Boolean assertSlotRefsFind(SlotRef[] slotRefs) {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
			throw new IllegalStateException("Backpack screen is not open");
		}
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		for (SlotRef slotRef : slotRefs) {
			Slot slot = resolveStorageGuiSlot(menu, slotRef);
			Slot foundSlot = screen.findSlot(getSlotCenterX(screen, slot), getSlotCenterY(screen, slot));
			if (foundSlot != slot) {
				throw new IllegalStateException("findSlot did not return " + slotRef + "; found=" + (foundSlot == null ? "null" : foundSlot.index));
			}
		}
		return true;
	}

	private static Boolean assertSlotRefsNotFind(SlotRef[] slotRefs) {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
			throw new IllegalStateException("Backpack screen is not open");
		}
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		for (SlotRef slotRef : slotRefs) {
			Slot slot = resolveStorageGuiSlot(menu, slotRef);
			Slot foundSlot = screen.findSlot(getSlotCenterX(screen, slot), getSlotCenterY(screen, slot));
			if (foundSlot == slot) {
				throw new IllegalStateException("findSlot unexpectedly returned hidden/inactive " + slotRef);
			}
		}
		return true;
	}

	private static Boolean assertUpgradeTabSlots(SlotRef[] slotRefs) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		for (SlotRef slotRef : slotRefs) {
			if (!"upgradeTab".equals(slotRef.area())) {
				throw new IllegalArgumentException("assertUpgradeTabSlots requires upgradeTab refs");
			}
			Slot slot = resolveStorageGuiSlot(menu, slotRef);
			if (slot.index < menu.getFirstUpgradeSlot()) {
				throw new IllegalStateException("Upgrade tab slot " + slotRef + " is not in logical upgrade range: " + slot.index);
			}
			if (menu.getSlot(slot.index) != slot) {
				throw new IllegalStateException("getSlot(" + slot.index + ") does not return upgrade tab slot " + slotRef);
			}
		}
		return true;
	}

	private static Boolean assertMobCatcherSlots(StorageGuiRegressionScenario scenario) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		List<CapturedMob> capturedMobs = MobCatcherStorage.getCapturedMobs(menu.getStorageWrapper());
		if (capturedMobs.size() != scenario.capturedMobs().length) {
			throw new IllegalStateException("Captured mob count mismatch expected=" + scenario.capturedMobs().length + " actual=" + capturedMobs.size());
		}
		int columns = MobCatcherStorage.getColumns(menu.getStorageWrapper());
		for (CapturedMob capturedMob : capturedMobs) {
			for (int y = 0; y < capturedMob.height(); y++) {
				for (int x = 0; x < capturedMob.width(); x++) {
					assertStorageSlotPresent(menu, capturedMob.slot() + x + y * columns);
				}
			}
		}
		return true;
	}

	private static Boolean assertStorageGuiCarriedEmpty() {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		if (!menu.getCarried().isEmpty()) {
			throw new IllegalStateException("Storage GUI cursor is not empty: " + menu.getCarried());
		}
		return true;
	}

	private static Boolean assertStorageGuiCarriedStack(StorageGuiAction action) {
		assertStackMatches("carried stack", getOpenPlacedBackpackMenu().getCarried(), action.item(), action.count());
		return true;
	}

	private static Boolean assertStorageGuiSlotContents(StorageGuiAction action) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		Slot slot = resolveStorageGuiSlot(menu, action.slot());
		assertStackMatches(action.slot().toString(), slot.getItem(), action.item(), action.count());
		return true;
	}

	private static List<StorageGuiSlotSnapshot> snapshotStorageGuiSlotContents(StorageGuiAction action) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		return Arrays.stream(action.slotRefs()).map(slotRef -> new StorageGuiSlotSnapshot(slotRef, resolveStorageGuiSlot(menu, slotRef).getItem().copy()))
				.toList();
	}

	private static Boolean assertStorageGuiSlotContentsUnchanged(StorageGuiAction action, Map<String, List<StorageGuiSlotSnapshot>> slotSnapshots) {
		List<StorageGuiSlotSnapshot> snapshots = slotSnapshots.get(action.snapshot());
		if (snapshots == null) {
			throw new IllegalStateException("Missing storage GUI slot snapshot " + action.snapshot());
		}
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		for (StorageGuiSlotSnapshot snapshot : snapshots) {
			ItemStack currentStack = resolveStorageGuiSlot(menu, snapshot.slotRef()).getItem();
			if (!ItemStack.matches(snapshot.stack(), currentStack)) {
				throw new IllegalStateException("Slot " + snapshot.slotRef() + " changed after snapshot " + action.snapshot() + "; expected=" + snapshot.stack()
						+ " actual=" + currentStack);
			}
		}
		return true;
	}

	private static void assertStackMatches(String name, ItemStack stack, Optional<Item> expectedItem, int expectedCount) {
		if (expectedItem.isEmpty()) {
			if (!stack.isEmpty()) {
				throw new IllegalStateException(name + " expected empty but was " + stack);
			}
			return;
		}
		if (stack.isEmpty()) {
			throw new IllegalStateException(name + " expected " + expectedItem.get() + " but was empty");
		}
		if (stack.getItem() != expectedItem.get()) {
			throw new IllegalStateException(name + " expected item " + expectedItem.get() + " but was " + stack.getItem());
		}
		if (expectedCount >= 0 && stack.getCount() != expectedCount) {
			throw new IllegalStateException(name + " expected count " + expectedCount + " but was " + stack.getCount());
		}
	}

	private static Boolean assertStorageGuiColumnState(StorageGuiAction action) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		if (action.expectedColumnsTaken() >= 0 && menu.getStorageWrapper().getColumnsTaken() != action.expectedColumnsTaken()) {
			throw new IllegalStateException(
					"Expected columnsTaken=" + action.expectedColumnsTaken() + " but was " + menu.getStorageWrapper().getColumnsTaken());
		}
		if (action.expectedStorageSlots() >= 0 && menu.getNumberOfStorageInventorySlots() != action.expectedStorageSlots()) {
			throw new IllegalStateException("Expected storageSlots=" + action.expectedStorageSlots() + " but was " + menu.getNumberOfStorageInventorySlots());
		}
		return true;
	}

	private static Slot assertStorageSlotPresent(BackpackContainer menu, int slotId) {
		if (slotId < 0 || slotId >= menu.getNumberOfStorageInventorySlots()) {
			throw new IllegalStateException("Storage slot " + slotId + " is outside storage slot count " + menu.getNumberOfStorageInventorySlots());
		}
		if (slotId >= menu.slots.size()) {
			throw new IllegalStateException("Storage slot " + slotId + " is outside menu.slots size " + menu.slots.size());
		}
		Slot slot = menu.getSlot(slotId);
		if (menu.slots.get(slotId) != slot) {
			throw new IllegalStateException("Storage slot " + slotId + " is not present at matching menu.slots index");
		}
		return slot;
	}

	private static Boolean clickStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		Slot slot = resolveStorageGuiSlot(menu, action.slot());
		clickSlot(Minecraft.getInstance().screen, slot, action.button());
		assertStorageGuiMenuSlotLayout(scenario);
		return true;
	}

	private static Boolean shiftClickStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		handleStorageGuiClickType(scenario, action, 0, ContainerInput.QUICK_MOVE);
		return true;
	}

	private static Boolean pickupAllStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		handleStorageGuiClickType(scenario, action, 0, ContainerInput.PICKUP_ALL);
		return true;
	}

	private static Boolean hotbarSwapStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		handleStorageGuiClickType(scenario, action, action.hotbarSlot(), ContainerInput.SWAP);
		return true;
	}

	private static Boolean throwStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		handleStorageGuiClickType(scenario, action, action.button(), ContainerInput.THROW);
		return true;
	}

	private static void handleStorageGuiClickType(StorageGuiRegressionScenario scenario, StorageGuiAction action, int data, ContainerInput clickType) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof StorageScreenBase<?> storageScreen)) {
			throw new IllegalStateException("Backpack storage screen is not open");
		}
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		Slot slot = resolveStorageGuiSlot(menu, action.slot());
		invokeInventoryMouseClick(storageScreen, slot.index, data, clickType);
		assertStorageGuiMenuSlotLayout(scenario);
	}

	private static void invokeInventoryMouseClick(StorageScreenBase<?> storageScreen, int slot, int button, ContainerInput clickType) {
		try {
			Method handleInventoryMouseClick = StorageScreenBase.class.getDeclaredMethod("handleInventoryMouseClick", int.class, int.class,
					ContainerInput.class);
			handleInventoryMouseClick.setAccessible(true);
			handleInventoryMouseClick.invoke(storageScreen, slot, button, clickType);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to invoke storage GUI click: " + e.getMessage(), e);
		}
	}

	private static void runStorageGuiSetCarriedStack(StorageGuiAction action) {
		ItemStack stack = action.item().map(item -> new ItemStack(item, action.count())).orElse(ItemStack.EMPTY);
		runOnServer(player -> setStorageGuiCarriedStack(player, stack));
		runOnClient(() -> setStorageGuiClientCarriedStack(stack));
	}

	private static Boolean scrollStorageGui(StorageGuiAction action) {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
			throw new IllegalStateException("Backpack screen is not open");
		}
		double x = screen.getGuiLeft() + 20.0;
		double y = screen.getGuiTop() + 30.0;
		int steps = action.count() == 0 ? 1 : Math.abs(action.count());
		double scrollY = action.count() < 0 ? 1.0 : -1.0;
		for (int i = 0; i < steps; i++) {
			screen.mouseScrolled(x, y, 0.0, scrollY);
		}
		return true;
	}

	private static void runStorageGuiDragCarriedStack(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		Item item = action.item().orElseThrow(() -> new IllegalArgumentException("Drag action needs an item"));
		runOnServer(player -> setStorageGuiCarriedStack(player, item, action.count()));
		runOnClient(() -> setStorageGuiClientCarriedStack(item, action.count()));
		runOnClient(() -> dragStorageGuiCarriedStack(scenario, action));
	}

	private static Boolean dragStorageGuiCarriedStack(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
			throw new IllegalStateException("Backpack screen is not open");
		}
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		if (action.slotRefs().length == 0) {
			throw new IllegalArgumentException("Drag action needs slots");
		}
		Slot firstSlot = resolveStorageGuiSlot(menu, action.slotRefs()[0]);
		double previousX = getSlotCenterX(screen, firstSlot);
		double previousY = getSlotCenterY(screen, firstSlot);
		screen.mouseClicked(new MouseButtonEvent(previousX, previousY, new MouseButtonInfo(action.button(), 0)), false);
		for (SlotRef slotRef : action.slotRefs()) {
			Slot slot = resolveStorageGuiSlot(menu, slotRef);
			double x = getSlotCenterX(screen, slot);
			double y = getSlotCenterY(screen, slot);
			screen.mouseDragged(new MouseButtonEvent(x, y, new MouseButtonInfo(action.button(), 0)), x - previousX, y - previousY);
			previousX = x;
			previousY = y;
		}
		screen.mouseReleased(new MouseButtonEvent(previousX, previousY, new MouseButtonInfo(action.button(), 0)));
		assertStorageGuiMenuSlotLayout(scenario);
		return true;
	}

	private static void runStorageGuiColumnUpgradeAction(StorageGuiAction action) {
		if ("insert".equals(action.operation())) {
			Item item = action.item().orElseThrow(() -> new IllegalArgumentException("Column upgrade insert action needs an item"));
			runOnServer(player -> setStorageGuiCarriedStack(player, item));
			runOnClient(() -> setStorageGuiClientCarriedStack(item));
		}

		StorageGuiColumnUpgradeExpectation expectation = runOnClient(() -> clickStorageGuiColumnUpgrade(action));
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		StorageGuiColumnUpgradeState state;
		do {
			state = runOnClient(() -> getStorageGuiColumnUpgradeState(action.upgradeSlot()));
			if (state.matches(expectation) && runOnClient(() -> getStorageGuiMenuSlotLayoutError(getOpenPlacedBackpackMenu()) == null)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);

		throw new IllegalStateException("Timed out waiting for column upgrade " + action.operation() + " sync; expected=" + expectation + " actual=" + state);
	}

	private static Boolean setStorageGuiCarriedStack(ServerPlayer player, Item item) {
		return setStorageGuiCarriedStack(player, item, 1);
	}

	private static Boolean setStorageGuiCarriedStack(ServerPlayer player, Item item, int count) {
		return setStorageGuiCarriedStack(player, new ItemStack(item, count));
	}

	private static Boolean setStorageGuiCarriedStack(ServerPlayer player, ItemStack stack) {
		if (!(player.containerMenu instanceof BackpackContainer menu)) {
			throw new IllegalStateException("Backpack menu is not open on server");
		}
		menu.setCarried(stack.copy());
		return true;
	}

	private static Boolean setStorageGuiClientCarriedStack(Item item) {
		return setStorageGuiClientCarriedStack(item, 1);
	}

	private static Boolean setStorageGuiClientCarriedStack(Item item, int count) {
		return setStorageGuiClientCarriedStack(new ItemStack(item, count));
	}

	private static Boolean setStorageGuiClientCarriedStack(ItemStack stack) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		menu.setCarried(stack.copy());
		return true;
	}

	private static Slot resolveStorageGuiSlot(BackpackContainer menu, SlotRef slotRef) {
		return switch (slotRef.area()) {
			case "storage" -> assertStorageSlotPresent(menu, slotRef.index());
			case "player" -> resolvePlayerSlot(menu, slotRef.index());
			case "upgrade" -> resolveUpgradeSlot(menu, slotRef.index());
			case "upgradeTab" -> resolveUpgradeTabSlot(menu, slotRef.upgradeSlot(), slotRef.index());
			default -> throw new IllegalArgumentException("Unknown storage GUI slot area " + slotRef.area());
		};
	}

	private static Slot resolvePlayerSlot(BackpackContainer menu, int playerSlotIndex) {
		int slotId = menu.getNumberOfStorageInventorySlots() + playerSlotIndex;
		if (playerSlotIndex < 0 || slotId >= menu.getInventorySlotsSize()) {
			throw new IllegalStateException("Player menu slot " + playerSlotIndex + " is outside inventory slot range");
		}
		return menu.getSlot(slotId);
	}

	private static Slot resolveUpgradeSlot(BackpackContainer menu, int upgradeSlot) {
		if (upgradeSlot < 0 || upgradeSlot >= menu.getNumberOfUpgradeSlots()) {
			throw new IllegalStateException("Upgrade slot " + upgradeSlot + " is outside upgrade slot range " + menu.getNumberOfUpgradeSlots());
		}
		return menu.upgradeSlots.get(upgradeSlot);
	}

	private static Slot resolveUpgradeTabSlot(BackpackContainer menu, int upgradeSlot, int tabSlot) {
		UpgradeContainerBase<?, ?> container = menu.getUpgradeContainers().get(upgradeSlot);
		if (container == null) {
			throw new IllegalStateException("Upgrade slot " + upgradeSlot + " does not have an upgrade container");
		}
		if (tabSlot < 0 || tabSlot >= container.getSlots().size()) {
			throw new IllegalStateException("Upgrade tab slot " + tabSlot + " is outside tab slot range " + container.getSlots().size());
		}
		return container.getSlots().get(tabSlot);
	}

	private static double getSlotCenterX(AbstractContainerScreen<?> screen, Slot slot) {
		return screen.getGuiLeft() + slot.x + 8.0;
	}

	private static double getSlotCenterY(AbstractContainerScreen<?> screen, Slot slot) {
		return screen.getGuiTop() + slot.y + 8.0;
	}

	private static StorageGuiColumnUpgradeExpectation clickStorageGuiColumnUpgrade(StorageGuiAction action) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		if (action.upgradeSlot() < 0 || action.upgradeSlot() >= menu.upgradeSlots.size()) {
			throw new IllegalStateException("Invalid upgrade slot " + action.upgradeSlot());
		}
		Slot slot = menu.upgradeSlots.get(action.upgradeSlot());
		ItemStack upgradeStack = "insert".equals(action.operation()) ? menu.getCarried() : slot.getItem();
		if (upgradeStack.isEmpty() || !(upgradeStack.getItem() instanceof IUpgradeItem<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
			throw new IllegalStateException("Column upgrade action needs a column-taking upgrade stack");
		}

		int beforeColumnsTaken = menu.getStorageWrapper().getColumnsTaken();
		int columnsChange = upgradeItem.getInventoryColumnsTaken();
		int expectedColumnsTaken = switch (action.operation()) {
			case "insert" -> beforeColumnsTaken + columnsChange;
			case "remove" -> beforeColumnsTaken - columnsChange;
			default -> throw new IllegalArgumentException("Unknown column upgrade operation " + action.operation());
		};
		int rows = menu.getStorageWrapper().getNumberOfSlotRows();
		int handlerSlots = menu.getStorageWrapper().getInventoryHandler().size();
		int baseColumns = handlerSlots <= 81 ? 9 : 12;
		int baseStorageSlots = handlerSlots / rows == baseColumns ? handlerSlots : handlerSlots + beforeColumnsTaken * rows;
		int expectedStorageSlots = baseStorageSlots - expectedColumnsTaken * rows;

		if (!(Minecraft.getInstance().screen instanceof StorageScreenBase<?> storageScreen)) {
			throw new IllegalStateException("Backpack storage screen is not open");
		}
		invokeInventoryMouseClick(storageScreen, slot.index, 0, ContainerInput.PICKUP);
		return new StorageGuiColumnUpgradeExpectation(expectedColumnsTaken, expectedStorageSlots, "remove".equals(action.operation()),
				"insert".equals(action.operation()));
	}

	private static StorageGuiColumnUpgradeState getStorageGuiColumnUpgradeState(int upgradeSlot) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		if (upgradeSlot < 0 || upgradeSlot >= menu.upgradeSlots.size()) {
			throw new IllegalStateException("Invalid upgrade slot " + upgradeSlot);
		}
		return new StorageGuiColumnUpgradeState(menu.getStorageWrapper().getColumnsTaken(), menu.getNumberOfStorageInventorySlots(),
				menu.upgradeSlots.get(upgradeSlot).getItem().isEmpty(), menu.getCarried().isEmpty());
	}

	private static StorageGuiRegressionState getStorageGuiRegressionState() {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		return new StorageGuiRegressionState(menu.getNumberOfStorageInventorySlots(), menu.slots.size(), menu.upgradeSlots.size());
	}

	private static StorageGuiRegressionState getStorageGuiRegressionStateSafely() {
		try {
			if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu) {
				return new StorageGuiRegressionState(menu.getNumberOfStorageInventorySlots(), menu.slots.size(), menu.upgradeSlots.size());
			}
		} catch (RuntimeException ignored) {
			// Return an empty state below so the regression response still explains the failure.
		}
		return new StorageGuiRegressionState(0, 0, 0);
	}

	private static Boolean setupPlacedBackpackStorageGuiRegression(ServerPlayer player, StorageGuiRegressionScenario scenario) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos pos = getRegressionBackpackPos(player);
		applyStorageGuiPlayerContents(player, scenario.playerContents());
		level.setBlock(pos, ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, player.getDirection().getOpposite()), 3);
		BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(level, pos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Failed to place storage GUI regression backpack block"));
		backpackBlockEntity.setBackpack(createStorageGuiRegressionBackpack(scenario));
		return true;
	}

	private static Boolean setupClientPlacedBackpackStorageGuiRegression(StorageGuiRegressionScenario scenario) {
		if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
			throw new IllegalStateException("Client level/player is not available");
		}
		applyStorageGuiPlayerContents(Minecraft.getInstance().player, scenario.playerContents());
		BlockPos pos = getRegressionBackpackPos(Minecraft.getInstance().player);
		Minecraft.getInstance().level.setBlock(pos,
				ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, Minecraft.getInstance().player.getDirection().getOpposite()),
				3);
		BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Failed to create client storage GUI regression backpack block"));
		backpackBlockEntity.setBackpack(createStorageGuiRegressionBackpack(scenario));
		return true;
	}

	private static Boolean openPlacedBackpackStorageGuiRegression(ServerPlayer player, StorageGuiRegressionScenario scenario) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos pos = getRegressionBackpackPos(player);
		BackpackContext.Block backpackContext = new BackpackContext.Block(pos);
		player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
				Component.literal("Storage GUI Regression: " + scenario.name())), backpackContext::toBuffer);
		level.gameEvent(player, GameEvent.CONTAINER_OPEN, pos);
		return true;
	}

	private static ItemStack createStorageGuiRegressionBackpack(StorageGuiRegressionScenario scenario) {
		ItemStack backpack = scenario.inventorySlots() > 81 ? new ItemStack(ModItems.DIAMOND_BACKPACK.get()) : new ItemStack(ModItems.GOLD_BACKPACK.get());
		IBackpackWrapper wrapper = BackpackWrapper.fromStackNoCache(backpack);
		wrapper.setSlotNumbers(scenario.inventorySlots(), scenario.upgradeSlots());
		InventoryHandler inventory = wrapper.getInventoryHandler();
		for (SlotStackSpec content : scenario.contents()) {
			inventory.setStackInSlot(content.slot(), new ItemStack(content.item(), content.count()));
		}
		inventory.saveInventory();
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		for (SlotStackSpec content : scenario.upgradeContents()) {
			upgrades.setStackInSlot(content.slot(), new ItemStack(content.item(), content.count()));
		}
		upgrades.saveInventory();
		applyProtectedSlots(wrapper, scenario.noSortSlots(), scenario.memorySlots());
		for (CapturedMobSpec capturedMob : scenario.capturedMobs()) {
			MobCatcherStorage.addCapturedMob(wrapper,
					new CapturedMob(UUID.randomUUID(), Identifier.parse(capturedMob.entityType()), new CompoundTag(), capturedMob.slot(), capturedMob.width(),
							capturedMob.height(), capturedMob.width() * capturedMob.height(), false, capturedMob.entityType(), 10, 10));
		}
		if (scenario.openTab() >= 0) {
			wrapper.setOpenTabId(scenario.openTab());
		}
		wrapper.setColumnsTaken(scenario.columnsTaken(), false);
		wrapper.onContentsUpdated();
		return backpack;
	}

	private static void applyStorageGuiPlayerContents(Player player, SlotStackSpec[] playerContents) {
		player.getInventory().clearContent();
		for (SlotStackSpec content : playerContents) {
			player.getInventory().setItem(content.slot(), new ItemStack(content.item(), content.count()));
		}
		player.getInventory().setChanged();
	}

	private static void applyProtectedSlots(IBackpackWrapper wrapper, int[] noSortSlots, int[] memorySlots) {
		NoSortSettingsCategory noSortSettings = wrapper.getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class);
		for (int slot : noSortSlots) {
			noSortSettings.selectSlot(slot);
		}
		MemorySettingsCategory memorySettings = wrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
		for (int slot : memorySlots) {
			memorySettings.selectSlot(slot);
		}
	}

	private static StorageGuiRegressionSuite loadStorageGuiRegressionSuite() {
		try (InputStream inputStream = StorageGuiRegressionRunner.class.getResourceAsStream("/devclientautomation/storage_gui_regressions.json")) {
			if (inputStream == null) {
				throw new IllegalStateException("Missing storage GUI regression definitions");
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonArray scenarioElements = root.getAsJsonArray("scenarios");
			List<StorageGuiRegressionScenario> scenarios = new ArrayList<>();
			for (JsonElement scenarioElement : scenarioElements) {
				JsonObject scenario = scenarioElement.getAsJsonObject();
				scenarios.add(new StorageGuiRegressionScenario(scenario.get("name").getAsString(), scenario.get("inventorySlots").getAsInt(),
						scenario.has("upgradeSlots") ? scenario.get("upgradeSlots").getAsInt() : 3,
						scenario.has("columnsTaken") ? scenario.get("columnsTaken").getAsInt() : 0,
						scenario.has("openTab") ? scenario.get("openTab").getAsInt() : -1, getSlotStackSpecs(scenario, "contents"),
						getSlotStackSpecs(scenario, "playerContents"), getSlotStackSpecs(scenario, "upgradeContents"), getIntArray(scenario, "noSortSlots"),
						getIntArray(scenario, "memorySlots"), getCapturedMobs(scenario), getStorageGuiActions(scenario)));
			}
			return new StorageGuiRegressionSuite(scenarios);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read storage GUI regression definitions", e);
		}
	}

	private static SlotStackSpec[] getSlotStackSpecs(JsonObject scenario, String key) {
		if (!scenario.has(key)) {
			return new SlotStackSpec[0];
		}
		JsonArray elements = scenario.getAsJsonArray(key);
		SlotStackSpec[] specs = new SlotStackSpec[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			JsonObject element = elements.get(i).getAsJsonObject();
			specs[i] = new SlotStackSpec(element.get("slot").getAsInt(), getItem(element.get("item").getAsString()),
					element.has("count") ? element.get("count").getAsInt() : 1);
		}
		return specs;
	}

	private static StorageGuiAction[] getStorageGuiActions(JsonObject scenario) {
		JsonArray elements = scenario.getAsJsonArray("actions");
		StorageGuiAction[] actions = new StorageGuiAction[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			JsonObject element = elements.get(i).getAsJsonObject();
			actions[i] = new StorageGuiAction(element.get("type").getAsString(), element.has("snapshot") ? element.get("snapshot").getAsString() : "",
					getIntArray(element, "slots"), getSlotRefs(element, "slots"), getSlotRef(element, "slot"), getButton(element),
					element.has("operation") ? element.get("operation").getAsString() : "",
					element.has("upgradeSlot") ? element.get("upgradeSlot").getAsInt() : -1,
					element.has("item") ? Optional.of(getItem(element.get("item").getAsString())) : Optional.empty(),
					element.has("count") ? element.get("count").getAsInt() : 1, element.has("hotbarSlot") ? element.get("hotbarSlot").getAsInt() : 0,
					element.has("expectedColumnsTaken") ? element.get("expectedColumnsTaken").getAsInt() : -1,
					element.has("expectedStorageSlots") ? element.get("expectedStorageSlots").getAsInt() : -1);
		}
		return actions;
	}

	private static int getButton(JsonObject element) {
		if (!element.has("button")) {
			return 0;
		}
		return switch (element.get("button").getAsString()) {
			case "left" -> 0;
			case "right" -> 1;
			default -> throw new IllegalArgumentException("Unknown mouse button " + element.get("button").getAsString());
		};
	}

	private static SlotRef getSlotRef(JsonObject json, String key) {
		if (!json.has(key) || !json.get(key).isJsonObject()) {
			return SlotRef.storage(0);
		}
		return getSlotRef(json.getAsJsonObject(key));
	}

	private static SlotRef[] getSlotRefs(JsonObject json, String key) {
		if (!json.has(key) || !json.get(key).isJsonArray()) {
			return new SlotRef[0];
		}
		JsonArray elements = json.getAsJsonArray(key);
		if (elements.isEmpty() || elements.get(0).isJsonPrimitive()) {
			return new SlotRef[0];
		}
		SlotRef[] slotRefs = new SlotRef[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			slotRefs[i] = getSlotRef(elements.get(i).getAsJsonObject());
		}
		return slotRefs;
	}

	private static SlotRef getSlotRef(JsonObject json) {
		return new SlotRef(json.has("area") ? json.get("area").getAsString() : "storage", json.get("index").getAsInt(),
				json.has("upgradeSlot") ? json.get("upgradeSlot").getAsInt() : -1);
	}

	private static int[] getIntArray(JsonObject json, String key) {
		if (!json.has(key)) {
			return new int[0];
		}
		JsonArray elements = json.getAsJsonArray(key);
		if (!elements.isEmpty() && elements.get(0).isJsonObject()) {
			return new int[0];
		}
		int[] values = new int[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			values[i] = elements.get(i).getAsInt();
		}
		return values;
	}

	private static CapturedMobSpec[] getCapturedMobs(JsonObject scenario) {
		if (!scenario.has("capturedMobs")) {
			return new CapturedMobSpec[0];
		}
		JsonArray capturedMobs = scenario.getAsJsonArray("capturedMobs");
		CapturedMobSpec[] mobs = new CapturedMobSpec[capturedMobs.size()];
		for (int i = 0; i < capturedMobs.size(); i++) {
			JsonObject capturedMob = capturedMobs.get(i).getAsJsonObject();
			mobs[i] = new CapturedMobSpec(capturedMob.get("slot").getAsInt(), capturedMob.get("width").getAsInt(), capturedMob.get("height").getAsInt(),
					capturedMob.has("entityType") ? capturedMob.get("entityType").getAsString() : "minecraft:pig");
		}
		return mobs;
	}

	private static Item getItem(String itemName) {
		Identifier itemId = Identifier.parse(itemName);
		return BuiltInRegistries.ITEM.getOptional(itemId).orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemId));
	}

	private static boolean contains(int[] values, int expected) {
		for (int value : values) {
			if (value == expected) {
				return true;
			}
		}
		return false;
	}

	private static void resetBackpackGuiRegressionState() {
		runOnServer(player -> {
			player.containerMenu.setCarried(ItemStack.EMPTY);
			player.closeContainer();
			player.getInventory().setChanged();
			return true;
		});
		runOnClient(() -> {
			if (Minecraft.getInstance().player != null) {
				Minecraft.getInstance().player.containerMenu.setCarried(ItemStack.EMPTY);
			}
			Minecraft.getInstance().setScreen(null);
			return true;
		});
		sleep(100);
	}

	private static void waitForOpenPlacedBackpackMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (runOnClient(() -> {
				if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || Minecraft.getInstance().player == null
						|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
					return false;
				}
				return menu.getBlockPosition().isPresent();
			})) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Placed backpack screen did not open");
	}

	private static BackpackContainer getOpenPlacedBackpackMenu() {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || Minecraft.getInstance().player == null
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			throw new IllegalStateException("Placed backpack screen is not open");
		}
		if (menu.getBlockPosition().isEmpty()) {
			throw new IllegalStateException("Open backpack is not a placed backpack");
		}
		return menu;
	}

	private static BlockPos getRegressionBackpackPos(LivingEntity player) {
		return player.blockPosition().relative(player.getDirection(), 2);
	}

	private static void clickSlot(Screen screen, Slot slot, int button) {
		int leftPos = getScreenIntField(screen, "leftPos");
		int topPos = getScreenIntField(screen, "topPos");
		double x = leftPos + slot.x + 8.0;
		double y = topPos + slot.y + 8.0;
		MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
		if (!screen.mouseClicked(event, false)) {
			throw new IllegalStateException("Storage GUI slot click was not handled for slot " + slot.index);
		}
		screen.mouseReleased(event);
	}

	private static int getScreenIntField(Screen screen, String fieldName) {
		Class<?> screenClass = screen.getClass();
		while (screenClass != null) {
			try {
				Field field = screenClass.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field.getInt(screen);
			} catch (NoSuchFieldException e) {
				screenClass = screenClass.getSuperclass();
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Unable to read screen field " + fieldName, e);
			}
		}
		throw new IllegalStateException("Unable to find screen field " + fieldName);
	}

	private static <T> T runOnClient(Supplier<T> supplier) {
		CompletableFuture<T> future = new CompletableFuture<>();
		Minecraft.getInstance().execute(() -> {
			try {
				future.complete(supplier.get());
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
		try {
			return future.get(TASK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for client task", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Failed to run client task: " + e.getCause(), e);
		} catch (TimeoutException e) {
			throw new IllegalStateException("Failed to run client task", e);
		}
	}

	private static <T> T runOnServer(Function<ServerPlayer, T> function) {
		ServerTaskContext context = runOnClient(() -> {
			MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
			if (server == null) {
				throw new IllegalStateException("Singleplayer server is not loaded");
			}
			if (Minecraft.getInstance().player == null) {
				throw new IllegalStateException("Client player is not loaded");
			}
			return new ServerTaskContext(server, Minecraft.getInstance().player.getUUID());
		});

		CompletableFuture<T> future = new CompletableFuture<>();
		context.server().execute(() -> {
			try {
				ServerPlayer player = context.server().getPlayerList().getPlayer(context.playerUuid());
				if (player == null) {
					throw new IllegalStateException("Server player is not loaded");
				}
				future.complete(function.apply(player));
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
		try {
			return future.get(TASK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for server task", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Failed to run server task: " + e.getCause(), e);
		} catch (TimeoutException e) {
			throw new IllegalStateException("Failed to run server task", e);
		}
	}

	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record ServerTaskContext(MinecraftServer server, UUID playerUuid) {
	}

	private record StorageGuiRegressionSuite(List<StorageGuiRegressionScenario> scenarios) {
	}

	private record StorageGuiRegressionScenario(String name, int inventorySlots, int upgradeSlots, int columnsTaken, int openTab, SlotStackSpec[] contents,
			SlotStackSpec[] playerContents, SlotStackSpec[] upgradeContents, int[] noSortSlots, int[] memorySlots, CapturedMobSpec[] capturedMobs,
			StorageGuiAction[] actions) {
	}

	private record SlotStackSpec(int slot, Item item, int count) {
	}

	private record StorageGuiAction(String type, String snapshot, int[] slots, SlotRef[] slotRefs, SlotRef slot, int button, String operation, int upgradeSlot,
			Optional<Item> item, int count, int hotbarSlot, int expectedColumnsTaken, int expectedStorageSlots) {
	}

	private record SlotRef(String area, int index, int upgradeSlot) {
		private static SlotRef storage(int index) {
			return new SlotRef("storage", index, -1);
		}
	}

	private record StorageGuiSlotSnapshot(SlotRef slotRef, ItemStack stack) {
	}

	private record CapturedMobSpec(int slot, int width, int height, String entityType) {
	}

	private record StorageGuiRegressionResult(String name, boolean passed, int actions, int storageSlots, int menuSlots, int upgradeSlots, String error) {
	}

	private record StorageGuiRegressionState(int storageSlots, int menuSlots, int upgradeSlots) {
	}

	private record StorageGuiColumnUpgradeExpectation(int expectedColumnsTaken, int expectedStorageSlots, boolean upgradeSlotEmpty, boolean carriedEmpty) {
	}

	private record StorageGuiColumnUpgradeState(int columnsTaken, int storageSlots, boolean upgradeSlotEmpty, boolean carriedEmpty) {
		private boolean matches(StorageGuiColumnUpgradeExpectation expectation) {
			return columnsTaken == expectation.expectedColumnsTaken() && storageSlots == expectation.expectedStorageSlots()
					&& upgradeSlotEmpty == expectation.upgradeSlotEmpty() && carriedEmpty == expectation.carriedEmpty();
		}
	}
}
