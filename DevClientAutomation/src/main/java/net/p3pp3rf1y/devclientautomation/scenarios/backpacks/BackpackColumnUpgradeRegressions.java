package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitResult;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitter;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BackpackColumnUpgradeRegressions {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);

	private BackpackColumnUpgradeRegressions() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requirePost(exchange);
		try {
			sendJson(exchange, AutomationRuntime.runOnServer(BackpackColumnUpgradeRegressions::run));
		} catch (RuntimeException e) {
			LOGGER.error("Automation endpoint failed", e);
			sendJson(exchange, "{\"ok\":false," + jsonProperty("error", e.getMessage()) + "}");
		}
	}

	private static String run(ServerPlayer player) {
		ColumnUpgradeRegressionSuite suite = loadSuite();
		List<ColumnUpgradeRegressionResult> results = new ArrayList<>();
		for (ColumnUpgradeRegressionScenario scenario : suite.scenarios()) {
			results.add(runScenario(scenario, suite.stackGenerator()));
		}

		long failed = results.stream().filter(result -> !result.passed()).count();
		StringBuilder json = new StringBuilder("{\"ok\":").append(failed == 0).append(",\"total\":").append(results.size()).append(",\"failed\":")
				.append(failed).append(",\"results\":[");
		for (int i = 0; i < results.size(); i++) {
			if (i > 0) {
				json.append(',');
			}
			ColumnUpgradeRegressionResult result = results.get(i);
			json.append('{').append(jsonProperty("name", result.name())).append(",\"passed\":").append(result.passed()).append(",\"expectedFits\":")
					.append(result.expectedFits()).append(",\"actualFits\":").append(result.actualFits()).append(",\"beforeStacks\":")
					.append(result.beforeStacks()).append(",\"afterStacks\":").append(result.afterStacks()).append(',')
					.append(jsonProperty("error", result.error())).append('}');
		}
		player.getInventory().setChanged();
		return json.append("]}").toString();
	}

	private static ColumnUpgradeRegressionSuite loadSuite() {
		try (InputStream inputStream = BackpackColumnUpgradeRegressions.class
				.getResourceAsStream("/devclientautomation/backpack_column_upgrade_regressions.json")) {
			if (inputStream == null) {
				throw new IllegalStateException("Missing backpack column upgrade regression definitions");
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
			ColumnUpgradeStackGenerator stackGenerator = getStackGenerator(root.getAsJsonObject("stackGenerator"));
			List<ColumnUpgradeRegressionScenario> scenarios = new ArrayList<>();
			for (JsonElement scenarioElement : root.getAsJsonArray("scenarios")) {
				JsonObject scenario = scenarioElement.getAsJsonObject();
				ResourceLocation upgradeName = ResourceLocation.parse(scenario.get("upgrade").getAsString());
				Item upgradeItem = BuiltInRegistries.ITEM.getOptional(upgradeName)
						.orElseThrow(() -> new IllegalArgumentException("Unknown upgrade " + upgradeName));
				scenarios.add(new ColumnUpgradeRegressionScenario(scenario.get("name").getAsString(), scenario.get("inventorySlots").getAsInt(), upgradeItem,
						getOccupiedSlots(scenario), getIntArray(scenario, "noSortSlots"), getIntArray(scenario, "memorySlots"),
						getIntArray(scenario, "stableSlots"), getCapturedMobs(scenario), getIntArray(scenario, "expectedCapturedMobSlots"),
						scenario.has("operation") ? scenario.get("operation").getAsString() : "insert", scenario.get("expectedFits").getAsBoolean()));
			}
			if (scenarios.isEmpty()) {
				throw new IllegalArgumentException("Backpack column upgrade regression suite must contain at least one scenario");
			}
			return new ColumnUpgradeRegressionSuite(stackGenerator, scenarios);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read backpack column upgrade regression definitions", e);
		}
	}

	private static ColumnUpgradeStackGenerator getStackGenerator(JsonObject stackGenerator) {
		List<Item> items = new ArrayList<>();
		for (JsonElement itemElement : stackGenerator.getAsJsonArray("items")) {
			ResourceLocation itemName = ResourceLocation.parse(itemElement.getAsString());
			items.add(BuiltInRegistries.ITEM.getOptional(itemName).orElseThrow(() -> new IllegalArgumentException("Unknown stack item " + itemName)));
		}
		JsonObject countSequence = stackGenerator.getAsJsonObject("countSequence");
		return new ColumnUpgradeStackGenerator(items, countSequence.get("start").getAsInt(), countSequence.get("max").getAsInt());
	}

	private static int[] getOccupiedSlots(JsonObject scenario) {
		if (scenario.has("occupiedSlots")) {
			return getIntArray(scenario, "occupiedSlots");
		}
		if (scenario.has("firstSlots")) {
			int[] slots = new int[scenario.get("firstSlots").getAsInt()];
			for (int slot = 0; slot < slots.length; slot++) {
				slots[slot] = slot;
			}
			return slots;
		}
		if (scenario.has("occupiedColumns")) {
			JsonObject occupiedColumns = scenario.getAsJsonObject("occupiedColumns");
			int rows = occupiedColumns.get("rows").getAsInt();
			int columns = occupiedColumns.get("columns").getAsInt();
			int[] slots = new int[rows * occupiedColumns.get("occupiedColumns").getAsInt()];
			int index = 0;
			for (int row = 0; row < rows; row++) {
				for (int column = 0; column < slots.length / rows; column++) {
					slots[index++] = row * columns + column;
				}
			}
			return slots;
		}
		throw new IllegalArgumentException("Scenario " + scenario.get("name").getAsString() + " does not define occupied slots");
	}

	private static int[] getIntArray(JsonObject json, String key) {
		if (!json.has(key)) {
			return new int[0];
		}
		JsonArray elements = json.getAsJsonArray(key);
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

	private static ColumnUpgradeRegressionResult runScenario(ColumnUpgradeRegressionScenario scenario, ColumnUpgradeStackGenerator stackGenerator) {
		ItemStack backpack = createBackpack(scenario.inventorySlots());
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.setSlotNumbers(scenario.inventorySlots(), 5);
		if (scenario.operation().equals("remove")) {
			wrapper.setColumnsTaken(getUpgradeColumnsTaken(scenario.upgradeItem()), false);
		}
		InventoryHandler inventory = wrapper.getInventoryHandler();
		for (int i = 0; i < scenario.occupiedSlots().length; i++) {
			inventory.setStackInSlot(scenario.occupiedSlots()[i],
					new ItemStack(stackGenerator.items().get(i % stackGenerator.items().size()), stackGenerator.getCount(i)));
		}
		applyProtectedSlots(wrapper, scenario.noSortSlots(), scenario.memorySlots());
		addCapturedMobs(wrapper, scenario.capturedMobs());
		inventory.saveInventory();

		Map<String, Integer> beforeStacks = snapshotStacks(inventory);
		Map<Integer, String> beforeProtectedStacks = snapshotSlotStacks(inventory, scenario.protectedSlots());
		Map<Integer, String> beforeStableStacks = snapshotSlotStacks(inventory, scenario.stableSlots());
		Map<String, String> beforeProtectedSettings = snapshotProtectedSettings(wrapper, scenario);
		Map<UUID, String> beforeCapturedMobs = snapshotCapturedMobs(wrapper);
		ColumnUpgradeSimulationResult simulationResult = simulateOperation(backpack, wrapper, scenario.upgradeItem(), scenario.operation());
		wrapper = BackpackWrapper.fromStack(backpack);
		Map<String, Integer> afterStacks = snapshotStacks(wrapper.getInventoryHandler());
		if (simulationResult.fits() != scenario.expectedFits()) {
			return result(scenario, simulationResult.fits(), beforeStacks, afterStacks, "fit result mismatch");
		}
		if (!beforeProtectedStacks.equals(snapshotSlotStacks(wrapper.getInventoryHandler(), scenario.protectedSlots()))) {
			return result(scenario, simulationResult.fits(), beforeStacks, afterStacks, "protected slot stack changed");
		}
		if (!beforeProtectedSettings.equals(snapshotProtectedSettings(wrapper, scenario))) {
			return result(scenario, simulationResult.fits(), beforeStacks, afterStacks, "protected slot settings changed");
		}
		if (!beforeStableStacks.equals(snapshotSlotStacks(wrapper.getInventoryHandler(), scenario.stableSlots()))) {
			return result(scenario, simulationResult.fits(), beforeStacks, afterStacks, "stable slot stack changed");
		}
		Map<UUID, String> afterCapturedMobs = snapshotCapturedMobs(wrapper);
		if (scenario.expectedFits()) {
			Optional<String> layoutError = capturedMobLayoutError(wrapper);
			if (layoutError.isPresent()) {
				return result(scenario, true, beforeStacks, afterStacks, layoutError.get());
			}
			if (scenario.expectedCapturedMobSlots().length > 0 && !capturedMobSlotsMatch(wrapper, scenario.expectedCapturedMobSlots())) {
				return result(scenario, true, beforeStacks, afterStacks,
						"captured mob slots mismatch expected=" + Arrays.toString(scenario.expectedCapturedMobSlots()) + " actual=" + afterCapturedMobs);
			}
			if (!beforeStacks.equals(afterStacks)) {
				return result(scenario, true, beforeStacks, afterStacks, "stack snapshot changed");
			}
			if (scenario.expectedCapturedMobSlots().length == 0 && !beforeCapturedMobs.equals(afterCapturedMobs)) {
				return result(scenario, true, beforeStacks, afterStacks, "captured mobs changed");
			}
		} else if (!beforeStacks.equals(afterStacks) || !beforeCapturedMobs.equals(afterCapturedMobs)) {
			return result(scenario, false, beforeStacks, afterStacks, "blocked insertion mutated stacks");
		}
		return result(scenario, simulationResult.fits(), beforeStacks, afterStacks, null);
	}

	private static ColumnUpgradeRegressionResult result(ColumnUpgradeRegressionScenario scenario, boolean actualFits, Map<String, Integer> beforeStacks,
			Map<String, Integer> afterStacks, String error) {
		return new ColumnUpgradeRegressionResult(scenario.name(), error == null, scenario.expectedFits(), actualFits, beforeStacks.size(), afterStacks.size(),
				error);
	}

	private static ItemStack createBackpack(int inventorySlots) {
		ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, inventorySlots);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);
		return backpack;
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

	private static ColumnUpgradeSimulationResult simulateOperation(ItemStack backpack, IBackpackWrapper wrapper, Item upgradeItem, String operation) {
		int currentColumnsTaken = wrapper.getColumnsTaken();
		int columnsTaken = getUpgradeColumnsTaken(upgradeItem);
		int targetColumnsTaken = switch (operation) {
			case "insert" -> currentColumnsTaken + columnsTaken;
			case "remove" -> currentColumnsTaken - columnsTaken;
			default -> throw new IllegalArgumentException("Unknown column upgrade regression operation " + operation);
		};
		int rows = wrapper.getNumberOfSlotRows();
		int baseSlots = wrapper.getInventoryHandler().getSlots() + currentColumnsTaken * rows;
		int baseColumns = baseSlots <= 81 ? 9 : 12;
		int currentColumns = baseColumns - currentColumnsTaken;
		int targetColumns = baseColumns - targetColumnsTaken;
		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(wrapper.getInventoryLayoutParts(currentColumns, targetColumns),
				baseSlots - targetColumnsTaken * rows, targetColumns, targetColumnsTaken < currentColumnsTaken);
		if (!fitResult.fits()) {
			return new ColumnUpgradeSimulationResult(false);
		}
		if (targetColumnsTaken > currentColumnsTaken) {
			wrapper.applyInventoryLayout(fitResult, targetColumns);
		}
		wrapper.setColumnsTaken(targetColumnsTaken, false);
		wrapper.onContentsNbtUpdated();
		wrapper.applyInventoryLayout(fitResult, targetColumns);
		wrapper.onContentsNbtUpdated();
		wrapper.getUpgradeHandler().setStackInSlot(0, operation.equals("insert") ? new ItemStack(upgradeItem) : ItemStack.EMPTY);
		wrapper.getUpgradeHandler().saveInventory();
		wrapper.getInventoryHandler().saveInventory();
		BackpackWrapper.fromStack(backpack).getInventoryHandler().saveInventory();
		return new ColumnUpgradeSimulationResult(true);
	}

	private static int getUpgradeColumnsTaken(Item upgradeItem) {
		if (!(upgradeItem instanceof IUpgradeItem<?> upgrade)) {
			throw new IllegalArgumentException("Item is not an upgrade: " + BuiltInRegistries.ITEM.getKey(upgradeItem));
		}
		return upgrade.getInventoryColumnsTaken();
	}

	private static void addCapturedMobs(IBackpackWrapper wrapper, CapturedMobSpec[] capturedMobs) {
		if (capturedMobs.length == 0) {
			return;
		}
		wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
		wrapper.getUpgradeHandler().saveInventory();
		for (int i = 0; i < capturedMobs.length; i++) {
			CapturedMobSpec capturedMob = capturedMobs[i];
			MobCatcherStorage.addCapturedMob(wrapper,
					new CapturedMob(new UUID(0, i + 1), ResourceLocation.parse(capturedMob.entityType()), new CompoundTag(), capturedMob.slot(),
							capturedMob.width(), capturedMob.height(), capturedMob.width() * capturedMob.height(), false, capturedMob.entityType(), 10, 10));
		}
	}

	private static Map<String, Integer> snapshotStacks(InventoryHandler inventory) {
		Map<String, Integer> stacks = new HashMap<>();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (!stack.isEmpty()) {
				stacks.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()) + ":" + stack.getCount(), 1, Integer::sum);
			}
		}
		return stacks;
	}

	private static Map<Integer, String> snapshotSlotStacks(InventoryHandler inventory, int[] slots) {
		Map<Integer, String> stacks = new HashMap<>();
		for (int slot : slots) {
			if (slot < inventory.getSlots() && !inventory.getStackInSlot(slot).isEmpty()) {
				ItemStack stack = inventory.getStackInSlot(slot);
				stacks.put(slot, BuiltInRegistries.ITEM.getKey(stack.getItem()) + ":" + stack.getCount());
			}
		}
		return stacks;
	}

	private static Map<String, String> snapshotProtectedSettings(IBackpackWrapper wrapper, ColumnUpgradeRegressionScenario scenario) {
		Map<String, String> settings = new HashMap<>();
		NoSortSettingsCategory noSortSettings = wrapper.getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class);
		for (int slot : scenario.noSortSlots()) {
			settings.put("noSort:" + slot, String.valueOf(noSortSettings.getNoSortSlots().contains(slot)));
		}
		MemorySettingsCategory memorySettings = wrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
		for (int slot : scenario.memorySlots()) {
			settings.put("memory:" + slot, String.valueOf(memorySettings.isSlotSelected(slot)));
		}
		return settings;
	}

	private static Map<UUID, String> snapshotCapturedMobs(IBackpackWrapper wrapper) {
		Map<UUID, String> capturedMobs = new HashMap<>();
		for (CapturedMob capturedMob : MobCatcherStorage.getCapturedMobs(wrapper)) {
			capturedMobs.put(capturedMob.id(), capturedMob.slot() + ":" + capturedMob.width() + "x" + capturedMob.height());
		}
		return capturedMobs;
	}

	private static boolean capturedMobSlotsMatch(IBackpackWrapper wrapper, int[] expectedSlots) {
		List<CapturedMob> capturedMobs = MobCatcherStorage.getCapturedMobs(wrapper);
		if (capturedMobs.size() != expectedSlots.length) {
			return false;
		}
		for (int i = 0; i < expectedSlots.length; i++) {
			if (capturedMobs.get(i).slot() != expectedSlots[i]) {
				return false;
			}
		}
		return true;
	}

	private static Optional<String> capturedMobLayoutError(IBackpackWrapper wrapper) {
		List<CapturedMob> capturedMobs = MobCatcherStorage.getCapturedMobs(wrapper);
		if (capturedMobs.isEmpty()) {
			return Optional.empty();
		}
		int columns = MobCatcherStorage.getColumns(wrapper);
		int inventorySlots = wrapper.getInventoryHandler().getSlots();
		Set<Integer> occupiedSlots = new HashSet<>();
		for (CapturedMob capturedMob : capturedMobs) {
			if (capturedMob.slot() % columns + capturedMob.width() > columns) {
				return Optional.of("captured mob crosses row at slot " + capturedMob.slot());
			}
			for (int y = 0; y < capturedMob.height(); y++) {
				for (int x = 0; x < capturedMob.width(); x++) {
					int slot = capturedMob.slot() + y * columns + x;
					if (slot >= inventorySlots) {
						return Optional.of("captured mob outside inventory at slot " + slot);
					}
					if (!occupiedSlots.add(slot)) {
						return Optional.of("captured mobs overlap at slot " + slot);
					}
					if (!wrapper.getInventoryHandler().getStackInSlot(slot).isEmpty()) {
						return Optional.of("captured mob overlaps stack at slot " + slot);
					}
				}
			}
		}
		return Optional.empty();
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

	private record ColumnUpgradeRegressionSuite(ColumnUpgradeStackGenerator stackGenerator, List<ColumnUpgradeRegressionScenario> scenarios) {
	}

	private record ColumnUpgradeStackGenerator(List<Item> items, int countStart, int countMax) {
		private int getCount(int stackIndex) {
			return countStart + stackIndex % (countMax - countStart + 1);
		}
	}

	private record ColumnUpgradeRegressionScenario(String name, int inventorySlots, Item upgradeItem, int[] occupiedSlots, int[] noSortSlots, int[] memorySlots,
			int[] stableSlots, CapturedMobSpec[] capturedMobs, int[] expectedCapturedMobSlots, String operation, boolean expectedFits) {
		private int[] protectedSlots() {
			int[] slots = new int[noSortSlots.length + memorySlots.length];
			System.arraycopy(noSortSlots, 0, slots, 0, noSortSlots.length);
			System.arraycopy(memorySlots, 0, slots, noSortSlots.length, memorySlots.length);
			return slots;
		}
	}

	private record CapturedMobSpec(int slot, int width, int height, String entityType) {
	}

	private record ColumnUpgradeRegressionResult(String name, boolean passed, boolean expectedFits, boolean actualFits, int beforeStacks, int afterStacks,
			String error) {
	}

	private record ColumnUpgradeSimulationResult(boolean fits) {
	}
}
