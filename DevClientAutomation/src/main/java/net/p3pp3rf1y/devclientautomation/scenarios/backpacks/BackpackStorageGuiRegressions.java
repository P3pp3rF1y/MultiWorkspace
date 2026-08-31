package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
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
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class BackpackStorageGuiRegressions {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);

	private BackpackStorageGuiRegressions() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requirePost(exchange);
		try {
			sendJson(exchange, run());
		} catch (RuntimeException e) {
			LOGGER.error("Automation endpoint failed", e);
			sendJson(exchange, "{\"ok\":false," + jsonProperty("error", e.getMessage()) + "}");
		}
	}

	private static String run() {
		StorageGuiRegressionSuite suite = loadSuite();
		List<StorageGuiRegressionResult> results = new ArrayList<>();
		for (StorageGuiRegressionScenario scenario : suite.scenarios()) {
			results.add(runScenario(scenario));
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
		return json.append("]}").toString();
	}

	private static StorageGuiRegressionResult runScenario(StorageGuiRegressionScenario scenario) {
		try {
			resetState();
			AutomationRuntime.runOnServer(player -> setupPlacedBackpack(player, scenario));
			AutomationRuntime.runOnClient(() -> setupClientPlacedBackpack(scenario));
			AutomationRuntime.runOnServer(player -> openPlacedBackpack(player, scenario));
			waitForOpenPlacedBackpackMenu();

			Map<String, List<StorageGuiSlotSnapshot>> slotSnapshots = new HashMap<>();
			for (StorageGuiAction action : scenario.actions()) {
				runAction(scenario, action, slotSnapshots);
			}

			StorageGuiRegressionState state = AutomationRuntime.runOnClient(BackpackStorageGuiRegressions::getState);
			return new StorageGuiRegressionResult(scenario.name(), true, scenario.actions().length, state.storageSlots(), state.menuSlots(),
					state.upgradeSlots(), null);
		} catch (RuntimeException e) {
			StorageGuiRegressionState state = AutomationRuntime.runOnClient(BackpackStorageGuiRegressions::getStateSafely);
			return new StorageGuiRegressionResult(scenario.name(), false, scenario.actions().length, state.storageSlots(), state.menuSlots(),
					state.upgradeSlots(), e.getMessage());
		}
	}

	private static void runAction(StorageGuiRegressionScenario scenario, StorageGuiAction action, Map<String, List<StorageGuiSlotSnapshot>> slotSnapshots) {
		validateActionInputs(action);
		switch (action.type()) {
			case "assertMenuSlotLayout" -> AutomationRuntime.runOnClient(() -> assertMenuSlotLayout(scenario));
			case "assertProtectedStorageSlots" -> AutomationRuntime.runOnClient(() -> assertProtectedStorageSlots(scenario, action.slots()));
			case "assertTrashSlotIndexCompatibility" -> AutomationRuntime.runOnClient(() -> assertTrashSlotIndexCompatibility(action.slots()));
			case "assertScreenFindSlots" -> AutomationRuntime.runOnClient(() -> assertScreenFindSlots(action.slots()));
			case "assertSlotRefsFind" -> AutomationRuntime.runOnClient(() -> assertSlotRefsFind(action.slotRefs()));
			case "assertSlotRefsNotFind" -> AutomationRuntime.runOnClient(() -> assertSlotRefsNotFind(action.slotRefs()));
			case "assertUpgradeTabSlots" -> AutomationRuntime.runOnClient(() -> assertUpgradeTabSlots(action.slotRefs()));
			case "assertMobCatcherSlots" -> AutomationRuntime.runOnClient(() -> assertMobCatcherSlots(scenario));
			case "assertCarriedEmpty" -> AutomationRuntime.runOnClient(BackpackStorageGuiRegressions::assertCarriedEmpty);
			case "assertCarriedStack" -> AutomationRuntime.runOnClient(() -> assertCarriedStack(action));
			case "assertSlotContents" -> AutomationRuntime.runOnClient(() -> assertSlotContents(action));
			case "snapshotSlotContents" -> slotSnapshots.put(action.snapshot(), AutomationRuntime.runOnClient(() -> snapshotSlotContents(action)));
			case "assertSlotContentsUnchanged" -> AutomationRuntime.runOnClient(() -> assertSlotContentsUnchanged(action, slotSnapshots));
			case "assertColumnState" -> AutomationRuntime.runOnClient(() -> assertColumnState(action));
			case "setCarriedStack" -> setCarriedStack(action);
			case "clickSlot" -> AutomationRuntime.runOnClient(() -> clickSlot(scenario, action));
			case "shiftClickSlot" -> AutomationRuntime.runOnClient(() -> shiftClickSlot(scenario, action));
			case "pickupAllSlot" -> AutomationRuntime.runOnClient(() -> pickupAllSlot(scenario, action));
			case "hotbarSwapSlot" -> AutomationRuntime.runOnClient(() -> hotbarSwapSlot(scenario, action));
			case "throwSlot" -> AutomationRuntime.runOnClient(() -> throwSlot(scenario, action));
			case "scrollStorage" -> AutomationRuntime.runOnClient(() -> scrollStorage(action));
			case "dragCarriedStack" -> dragCarriedStack(scenario, action);
			case "clickColumnUpgrade" -> runColumnUpgradeAction(action);
			default -> throw new IllegalArgumentException("Unknown storage GUI regression action " + action.type());
		}
	}

	private static void validateActionInputs(StorageGuiAction action) {
		switch (action.type()) {
			case "assertProtectedStorageSlots", "assertTrashSlotIndexCompatibility", "assertScreenFindSlots" ->
				requireNonEmpty(action.slots(), "slots", action.type());
			case "assertSlotRefsFind", "assertSlotRefsNotFind", "assertUpgradeTabSlots", "snapshotSlotContents", "dragCarriedStack" ->
				requireNonEmpty(action.slotRefs(), "slots", action.type());
			case "assertSlotContents", "clickSlot", "shiftClickSlot", "pickupAllSlot", "hotbarSwapSlot", "throwSlot" ->
				Objects.requireNonNull(action.slot(), action.type() + " requires a slot");
			default -> {
			}
		}
	}

	private static void requireNonEmpty(Object[] values, String key, String actionType) {
		if (values.length == 0) {
			throw new IllegalArgumentException(actionType + " requires " + key);
		}
	}

	private static void requireNonEmpty(int[] values, String key, String actionType) {
		if (values.length == 0) {
			throw new IllegalArgumentException(actionType + " requires " + key);
		}
	}

	private static Boolean assertMenuSlotLayout(StorageGuiRegressionScenario scenario) {
		String error = getMenuSlotLayoutError(getOpenPlacedBackpackMenu());
		if (error != null) {
			throw new IllegalStateException(scenario.name() + ": " + error);
		}
		return true;
	}

	private static String getMenuSlotLayoutError(BackpackContainer menu) {
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
			if (slotId < menu.getNumberOfStorageInventorySlots() && slot.getContainerSlot() != slotId) {
				return "storage menu slot " + slotId + " points to backing slot " + slot.getContainerSlot();
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
		BackpackScreen screen = getOpenBackpackScreen();
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
		BackpackScreen screen = getOpenBackpackScreen();
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		for (SlotRef slotRef : slotRefs) {
			Slot slot = resolveSlot(menu, slotRef);
			Slot foundSlot = screen.findSlot(getSlotCenterX(screen, slot), getSlotCenterY(screen, slot));
			if (foundSlot != slot) {
				throw new IllegalStateException("findSlot did not return " + slotRef + "; found=" + (foundSlot == null ? "null" : foundSlot.index));
			}
		}
		return true;
	}

	private static Boolean assertSlotRefsNotFind(SlotRef[] slotRefs) {
		BackpackScreen screen = getOpenBackpackScreen();
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		for (SlotRef slotRef : slotRefs) {
			Slot slot = resolveSlot(menu, slotRef);
			if (screen.findSlot(getSlotCenterX(screen, slot), getSlotCenterY(screen, slot)) == slot) {
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
			Slot slot = resolveSlot(menu, slotRef);
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

	private static Boolean assertCarriedEmpty() {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		if (!menu.getCarried().isEmpty()) {
			throw new IllegalStateException("Storage GUI cursor is not empty: " + menu.getCarried());
		}
		return true;
	}

	private static Boolean assertCarriedStack(StorageGuiAction action) {
		assertStackMatches("carried stack", getOpenPlacedBackpackMenu().getCarried(), action.item(), action.count());
		return true;
	}

	private static Boolean assertSlotContents(StorageGuiAction action) {
		Slot slot = resolveSlot(getOpenPlacedBackpackMenu(), action.slot());
		assertStackMatches(action.slot().toString(), slot.getItem(), action.item(), action.count());
		return true;
	}

	private static List<StorageGuiSlotSnapshot> snapshotSlotContents(StorageGuiAction action) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		return Arrays.stream(action.slotRefs()).map(slotRef -> new StorageGuiSlotSnapshot(slotRef, resolveSlot(menu, slotRef).getItem().copy())).toList();
	}

	private static Boolean assertSlotContentsUnchanged(StorageGuiAction action, Map<String, List<StorageGuiSlotSnapshot>> slotSnapshots) {
		List<StorageGuiSlotSnapshot> snapshots = slotSnapshots.get(action.snapshot());
		if (snapshots == null) {
			throw new IllegalStateException("Missing storage GUI slot snapshot " + action.snapshot());
		}
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		for (StorageGuiSlotSnapshot snapshot : snapshots) {
			ItemStack currentStack = resolveSlot(menu, snapshot.slotRef()).getItem();
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

	private static Boolean assertColumnState(StorageGuiAction action) {
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

	private static Boolean clickSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		clickSlot(resolveSlot(getOpenPlacedBackpackMenu(), action.slot()), action.button());
		assertMenuSlotLayout(scenario);
		return true;
	}

	private static Boolean shiftClickSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null) {
			throw new IllegalStateException("Client player/gameMode is not available");
		}
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		minecraft.gameMode.handleInventoryMouseClick(menu.containerId, resolveSlot(menu, action.slot()).index, 0, ClickType.QUICK_MOVE, minecraft.player);
		assertMenuSlotLayout(scenario);
		return true;
	}

	private static Boolean pickupAllSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		handleClickType(scenario, action, 0, ClickType.PICKUP_ALL);
		return true;
	}

	private static Boolean hotbarSwapSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		handleClickType(scenario, action, action.hotbarSlot(), ClickType.SWAP);
		return true;
	}

	private static Boolean throwSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		handleClickType(scenario, action, action.button(), ClickType.THROW);
		return true;
	}

	private static void handleClickType(StorageGuiRegressionScenario scenario, StorageGuiAction action, int data, ClickType clickType) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null) {
			throw new IllegalStateException("Client player/gameMode is not available");
		}
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		minecraft.gameMode.handleInventoryMouseClick(menu.containerId, resolveSlot(menu, action.slot()).index, data, clickType, minecraft.player);
		assertMenuSlotLayout(scenario);
	}

	private static void setCarriedStack(StorageGuiAction action) {
		ItemStack stack = action.item().map(item -> new ItemStack(item, action.count())).orElse(ItemStack.EMPTY);
		AutomationRuntime.runOnServer(player -> setServerCarriedStack(player, stack));
		AutomationRuntime.runOnClient(() -> setClientCarriedStack(stack));
	}

	private static Boolean scrollStorage(StorageGuiAction action) {
		BackpackScreen screen = getOpenBackpackScreen();
		int steps = action.count() == 0 ? 1 : Math.abs(action.count());
		double scrollY = action.count() < 0 ? 1.0 : -1.0;
		for (int i = 0; i < steps; i++) {
			screen.mouseScrolled(screen.getGuiLeft() + 20.0, screen.getGuiTop() + 30.0, 0.0, scrollY);
		}
		return true;
	}

	private static void dragCarriedStack(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		Item item = action.item().orElseThrow(() -> new IllegalArgumentException("Drag action needs an item"));
		AutomationRuntime.runOnServer(player -> setServerCarriedStack(player, item, action.count()));
		AutomationRuntime.runOnClient(() -> setClientCarriedStack(item, action.count()));
		AutomationRuntime.runOnClient(() -> dragCarriedStackOnScreen(scenario, action));
	}

	private static Boolean dragCarriedStackOnScreen(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
		BackpackScreen screen = getOpenBackpackScreen();
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		if (action.slotRefs().length == 0) {
			throw new IllegalArgumentException("Drag action needs slots");
		}
		Slot firstSlot = resolveSlot(menu, action.slotRefs()[0]);
		double previousX = getSlotCenterX(screen, firstSlot);
		double previousY = getSlotCenterY(screen, firstSlot);
		if (!screen.mouseClicked(previousX, previousY, action.button())) {
			throw new IllegalStateException("Backpack drag start was not handled");
		}
		for (SlotRef slotRef : action.slotRefs()) {
			Slot slot = resolveSlot(menu, slotRef);
			double x = getSlotCenterX(screen, slot);
			double y = getSlotCenterY(screen, slot);
			if (!screen.mouseDragged(x, y, action.button(), x - previousX, y - previousY)) {
				throw new IllegalStateException("Backpack drag was not handled");
			}
			previousX = x;
			previousY = y;
		}
		screen.mouseReleased(previousX, previousY, action.button());
		assertMenuSlotLayout(scenario);
		return true;
	}

	private static void runColumnUpgradeAction(StorageGuiAction action) {
		if ("insert".equals(action.operation())) {
			Item item = action.item().orElseThrow(() -> new IllegalArgumentException("Column upgrade insert action needs an item"));
			AutomationRuntime.runOnServer(player -> setServerCarriedStack(player, item));
			AutomationRuntime.runOnClient(() -> setClientCarriedStack(item));
		}

		StorageGuiColumnUpgradeExpectation expectation = AutomationRuntime.runOnClient(() -> clickColumnUpgrade(action));
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		StorageGuiColumnUpgradeState state;
		do {
			state = AutomationRuntime.runOnClient(() -> getColumnUpgradeState(action.upgradeSlot()));
			if (state.matches(expectation) && AutomationRuntime.runOnClient(() -> getMenuSlotLayoutError(getOpenPlacedBackpackMenu()) == null)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for column upgrade " + action.operation() + " sync; expected=" + expectation + " actual=" + state);
	}

	private static Boolean setServerCarriedStack(ServerPlayer player, Item item) {
		return setServerCarriedStack(player, item, 1);
	}

	private static Boolean setServerCarriedStack(ServerPlayer player, Item item, int count) {
		return setServerCarriedStack(player, new ItemStack(item, count));
	}

	private static Boolean setServerCarriedStack(ServerPlayer player, ItemStack stack) {
		if (!(player.containerMenu instanceof BackpackContainer menu)) {
			throw new IllegalStateException("Backpack menu is not open on server");
		}
		menu.setCarried(stack.copy());
		return true;
	}

	private static Boolean setClientCarriedStack(Item item) {
		return setClientCarriedStack(item, 1);
	}

	private static Boolean setClientCarriedStack(Item item, int count) {
		return setClientCarriedStack(new ItemStack(item, count));
	}

	private static Boolean setClientCarriedStack(ItemStack stack) {
		getOpenPlacedBackpackMenu().setCarried(stack.copy());
		return true;
	}

	private static Slot resolveSlot(BackpackContainer menu, SlotRef slotRef) {
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

	private static double getSlotCenterX(BackpackScreen screen, Slot slot) {
		return screen.getGuiLeft() + slot.x + 8.0;
	}

	private static double getSlotCenterY(BackpackScreen screen, Slot slot) {
		return screen.getGuiTop() + slot.y + 8.0;
	}

	private static StorageGuiColumnUpgradeExpectation clickColumnUpgrade(StorageGuiAction action) {
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
		int handlerSlots = menu.getStorageWrapper().getInventoryHandler().getSlots();
		int baseColumns = handlerSlots <= 81 ? 9 : 12;
		int baseStorageSlots = handlerSlots / rows == baseColumns ? handlerSlots : handlerSlots + beforeColumnsTaken * rows;
		int expectedStorageSlots = baseStorageSlots - expectedColumnsTaken * rows;

		clickSlot(slot, 0);
		return new StorageGuiColumnUpgradeExpectation(expectedColumnsTaken, expectedStorageSlots, "remove".equals(action.operation()),
				"insert".equals(action.operation()));
	}

	private static StorageGuiColumnUpgradeState getColumnUpgradeState(int upgradeSlot) {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		if (upgradeSlot < 0 || upgradeSlot >= menu.upgradeSlots.size()) {
			throw new IllegalStateException("Invalid upgrade slot " + upgradeSlot);
		}
		return new StorageGuiColumnUpgradeState(menu.getStorageWrapper().getColumnsTaken(), menu.getNumberOfStorageInventorySlots(),
				menu.upgradeSlots.get(upgradeSlot).getItem().isEmpty(), menu.getCarried().isEmpty());
	}

	private static StorageGuiRegressionState getState() {
		BackpackContainer menu = getOpenPlacedBackpackMenu();
		return new StorageGuiRegressionState(menu.getNumberOfStorageInventorySlots(), menu.slots.size(), menu.upgradeSlots.size());
	}

	private static StorageGuiRegressionState getStateSafely() {
		try {
			if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu) {
				return new StorageGuiRegressionState(menu.getNumberOfStorageInventorySlots(), menu.slots.size(), menu.upgradeSlots.size());
			}
		} catch (RuntimeException ignored) {
			// Return an empty state below so the regression response still explains the failure.
		}
		return new StorageGuiRegressionState(0, 0, 0);
	}

	private static Boolean setupPlacedBackpack(ServerPlayer player, StorageGuiRegressionScenario scenario) {
		ServerLevel level = player.serverLevel();
		BlockPos pos = getRegressionBackpackPos(player);
		applyPlayerContents(player, scenario.playerContents());
		level.setBlock(pos, ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, player.getDirection().getOpposite()), 3);
		BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(level, pos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Failed to place storage GUI regression backpack block"));
		backpackBlockEntity.setBackpack(createBackpack(scenario));
		return true;
	}

	private static Boolean setupClientPlacedBackpack(StorageGuiRegressionScenario scenario) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			throw new IllegalStateException("Client level/player is not available");
		}
		applyPlayerContents(minecraft.player, scenario.playerContents());
		BlockPos pos = getRegressionBackpackPos(minecraft.player);
		minecraft.level.setBlock(pos,
				ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, minecraft.player.getDirection().getOpposite()), 3);
		BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(minecraft.level, pos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Failed to create client storage GUI regression backpack block"));
		backpackBlockEntity.setBackpack(createBackpack(scenario));
		return true;
	}

	private static Boolean openPlacedBackpack(ServerPlayer player, StorageGuiRegressionScenario scenario) {
		BackpackContext.Block backpackContext = new BackpackContext.Block(getRegressionBackpackPos(player));
		player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
				Component.literal("Storage GUI Regression: " + scenario.name())), backpackContext::toBuffer);
		return true;
	}

	private static ItemStack createBackpack(StorageGuiRegressionScenario scenario) {
		ItemStack backpack = scenario.inventorySlots() > 81 ? new ItemStack(ModItems.DIAMOND_BACKPACK.get()) : new ItemStack(ModItems.GOLD_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, scenario.inventorySlots());
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, scenario.upgradeSlots());
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
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
					new CapturedMob(UUID.randomUUID(), ResourceLocation.parse(capturedMob.entityType()), new CompoundTag(), capturedMob.slot(),
							capturedMob.width(), capturedMob.height(), capturedMob.width() * capturedMob.height(), false, capturedMob.entityType(), 10, 10));
		}
		if (scenario.openTab() >= 0) {
			wrapper.setOpenTabId(scenario.openTab());
		}
		wrapper.setColumnsTaken(scenario.columnsTaken(), false);
		wrapper.onContentsNbtUpdated();
		return backpack;
	}

	private static void applyPlayerContents(Player player, SlotStackSpec[] playerContents) {
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

	private static void resetState() {
		AutomationRuntime.runOnServer(player -> {
			player.containerMenu.setCarried(ItemStack.EMPTY);
			player.closeContainer();
			player.getInventory().setChanged();
			return true;
		});
		AutomationRuntime.runOnClient(() -> {
			if (Minecraft.getInstance().player != null) {
				Minecraft.getInstance().player.containerMenu.setCarried(ItemStack.EMPTY);
			}
			return true;
		});
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		long closedSince = 0;
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen == null
					&& (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer)))) {
				if (closedSince == 0) {
					closedSince = System.nanoTime();
				} else if (System.nanoTime() - closedSince >= TimeUnit.MILLISECONDS.toNanos(250)) {
					return;
				}
			} else {
				closedSince = 0;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out resetting placed backpack GUI state");
	}

	private static void waitForOpenPlacedBackpackMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu && menu.getBlockPosition().isPresent())) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for placed backpack screen to open");
	}

	private static BackpackContainer getOpenPlacedBackpackMenu() {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			throw new IllegalStateException("Placed backpack screen is not open");
		}
		if (menu.getBlockPosition().isEmpty()) {
			throw new IllegalStateException("Open backpack is not a placed backpack");
		}
		return menu;
	}

	private static BackpackScreen getOpenBackpackScreen() {
		if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
			throw new IllegalStateException("Backpack screen is not open");
		}
		return screen;
	}

	private static BlockPos getRegressionBackpackPos(LivingEntity player) {
		return player.blockPosition().relative(player.getDirection(), 2);
	}

	private static void clickSlot(Slot slot, int button) {
		BackpackScreen screen = getOpenBackpackScreen();
		double x = getSlotCenterX(screen, slot);
		double y = getSlotCenterY(screen, slot);
		if (!screen.mouseClicked(x, y, button)) {
			throw new IllegalStateException("Backpack upgrade slot click was not handled");
		}
		screen.mouseReleased(x, y, button);
	}

	private static StorageGuiRegressionSuite loadSuite() {
		try (InputStream inputStream = BackpackStorageGuiRegressions.class.getResourceAsStream("/devclientautomation/storage_gui_regressions.json")) {
			if (inputStream == null) {
				throw new IllegalStateException("Missing storage GUI regression definitions");
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
			List<StorageGuiRegressionScenario> scenarios = new ArrayList<>();
			for (JsonElement scenarioElement : root.getAsJsonArray("scenarios")) {
				JsonObject scenario = scenarioElement.getAsJsonObject();
				scenarios.add(new StorageGuiRegressionScenario(scenario.get("name").getAsString(), scenario.get("inventorySlots").getAsInt(),
						scenario.has("upgradeSlots") ? scenario.get("upgradeSlots").getAsInt() : 3,
						scenario.has("columnsTaken") ? scenario.get("columnsTaken").getAsInt() : 0,
						scenario.has("openTab") ? scenario.get("openTab").getAsInt() : -1, getSlotStackSpecs(scenario, "contents"),
						getSlotStackSpecs(scenario, "playerContents"), getSlotStackSpecs(scenario, "upgradeContents"), getIntArray(scenario, "noSortSlots"),
						getIntArray(scenario, "memorySlots"), getCapturedMobs(scenario), getActions(scenario)));
			}
			if (scenarios.isEmpty()) {
				throw new IllegalArgumentException("Storage GUI regression suite must contain at least one scenario");
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

	private static StorageGuiAction[] getActions(JsonObject scenario) {
		JsonArray elements = scenario.getAsJsonArray("actions");
		if (elements.isEmpty()) {
			throw new IllegalArgumentException("Storage GUI regression scenario requires actions");
		}
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
		if (!json.has(key)) {
			return null;
		}
		if (!json.get(key).isJsonObject()) {
			throw new IllegalArgumentException("Expected " + key + " to be an object");
		}
		return getSlotRef(json.getAsJsonObject(key));
	}

	private static SlotRef[] getSlotRefs(JsonObject json, String key) {
		if (!json.has(key)) {
			return new SlotRef[0];
		}
		if (!json.get(key).isJsonArray()) {
			throw new IllegalArgumentException("Expected " + key + " to be an array");
		}
		JsonArray elements = json.getAsJsonArray(key);
		if (elements.isEmpty() || elements.get(0).isJsonPrimitive()) {
			for (JsonElement element : elements) {
				if (!element.isJsonPrimitive()) {
					throw new IllegalArgumentException("Expected " + key + " entries to use one representation");
				}
			}
			return new SlotRef[0];
		}
		SlotRef[] slotRefs = new SlotRef[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			if (!elements.get(i).isJsonObject()) {
				throw new IllegalArgumentException("Expected " + key + " entries to be objects");
			}
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
		if (!json.get(key).isJsonArray()) {
			throw new IllegalArgumentException("Expected " + key + " to be an array");
		}
		JsonArray elements = json.getAsJsonArray(key);
		if (elements.isEmpty() || elements.get(0).isJsonObject()) {
			for (JsonElement element : elements) {
				if (!element.isJsonObject()) {
					throw new IllegalArgumentException("Expected " + key + " entries to use one representation");
				}
			}
			return new int[0];
		}
		int[] values = new int[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			if (!elements.get(i).isJsonPrimitive()) {
				throw new IllegalArgumentException("Expected " + key + " entries to be integers");
			}
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
		ResourceLocation itemId = ResourceLocation.parse(itemName);
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

	private static void requirePost(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			byte[] response = "{\"error\":\"Method not allowed\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(405, response.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(response);
			}
			throw new IllegalStateException("Method not allowed");
		}
	}

	private static void sendJson(HttpExchange exchange, String json) throws IOException {
		byte[] response = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(200, response.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(response);
		}
	}

	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":"
				+ (value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"");
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
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

	private record CapturedMobSpec(int slot, int width, int height, String entityType) {
	}
}
