package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.bool;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.decimal;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.integer;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.longValue;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class BackpackEndpoints {
	private static final Logger LOGGER = LoggerFactory.getLogger("devclientautomation");

	private BackpackEndpoints() {
	}

	public static void register(EndpointRegistry endpoints) {
		endpoints.register("/backpack/setup", BackpackEndpoints::setupBackpacks);
		endpoints.register("/backpack/issue-1528-setup", BackpackEndpoints::setupIssue1528Backpacks);
		endpoints.register("/backpack/issue-1528-test", BackpackEndpoints::runIssue1528Test);
		endpoints.register("/backpack/issue-1528-status", BackpackEndpoints::issue1528Status);
		endpoints.register("/backpack/issue-1528-use-firework", BackpackEndpoints::useIssue1528Firework);
		endpoints.register("/backpack/issue-1528-refill-tick", BackpackEndpoints::issue1528RefillTick);
		endpoints.register("/backpack/stress", BackpackEndpoints::stressBackpacks);
		endpoints.register("/backpack/status", BackpackEndpoints::backpackStatus);
		endpoints.register("/backpack/open-main", BackpackEndpoints::openMainBackpack);
		endpoints.register("/backpack/open-nested", BackpackEndpoints::openNestedBackpack);
		endpoints.register("/backpack/empty", BackpackEndpoints::emptyBackpacks);
		endpoints.register("/backpack/clear-cache", BackpackEndpoints::clearBackpackCache);
		endpoints.register("/backpack/inception-magnet-persistence/setup", BackpackEndpoints::setupInceptionMagnetPersistence);
		endpoints.register("/backpack/inception-magnet-persistence/pickup", BackpackEndpoints::pickupWithInceptionMagnet);
		endpoints.register("/backpack/inception-magnet-persistence/status", BackpackEndpoints::inceptionMagnetPersistenceStatus);
		endpoints.register("/backpack/linked-storage-reload/setup", BackpackEndpoints::setupLinkedStorageReload);
		endpoints.register("/backpack/linked-storage-reload/status", BackpackEndpoints::linkedStorageReloadStatus);
		endpoints.register("/backpack/magnet-settings", BackpackEndpoints::changeMagnetSettings);
		endpoints.register("/backpack/move", BackpackEndpoints::moveBackpacks);
		endpoints.register("/backpack/spread-nested", BackpackEndpoints::spreadNestedBackpacks);
		endpoints.register("/backpack/fill-main-noise", BackpackEndpoints::fillMainBackpackNoise);
		endpoints.register("/backpack/magnet-pickup", BackpackEndpoints::changeMagnetPickup);
		endpoints.register("/backpack/seed", BackpackEndpoints::seedBackpack);
		endpoints.register("/backpack/bulk-drop", BackpackEndpoints::bulkDropFromNestedBackpack);
		endpoints.register("/backpack/column-upgrade-regressions", BackpackColumnUpgradeRegressions::handle);
		endpoints.register("/backpack/storage-gui-regressions", BackpackStorageGuiRegressions::handle);
		endpoints.register("/backpack/lifecycle-regression", BackpackLifecycleRegression::handle);
		endpoints.register("/backpack/linked-storage-regression", BackpackLinkedStorageRegression::handle);
		endpoints.register("/backpack/linked-storage-carrier-projection-regression",
				BackpackLinkedStorageRegression::handleCarrierRelocationAndNestedProjection);
		endpoints.register("/backpack/access-regression", BackpackAccessRegression::handle);
		endpoints.register("/backpack/curios-access-regression", BackpackAccessRegression::handleCurios);
		endpoints.register("/backpack/magnet-regression/setup", BackpackMagnetRegression::handleSetup);
		endpoints.register("/backpack/magnet-regression/status", BackpackMagnetRegression::handleStatus);
		endpoints.register("/backpack/pickup-regression", BackpackPickupRegression::handle);
		endpoints.register("/backpack/filter-regression", BackpackFilterRegression::handle);
		endpoints.register("/backpack/restock-regression", BackpackRestockRegression::handle);
		endpoints.register("/backpack/refill-regression/setup", BackpackRefillRegression::handleSetup);
		endpoints.register("/backpack/refill-regression/status", BackpackRefillRegression::handleStatus);
		endpoints.register("/backpack/gui-regression/run", BackpackGuiRegressionRun::handle);
		endpoints.register("/backpack/remote-upgrade-slot-regression", BackpackEndpoints::backpackRemoteUpgradeSlotRegression);
		endpoints.register("/backpack/dropped-items", BackpackEndpoints::droppedItemsStatus);
		endpoints.register("/backpack/clear-dropped-items", BackpackEndpoints::clearDroppedItems);
	}

	private static void setupBackpacks(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		boolean mainMagnet = bool(request, "mainMagnet", false);
		int redstoneCount = integer(request, "redstoneCount", 0);
		sendJsonHandling(exchange, LOGGER, () -> BackpackOperations.setupBackpacks(mainMagnet, redstoneCount));
	}

	private static void runIssue1528Test(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		int autosaveCount = integer(request, "autosaveCount", 2);
		long autosaveTimeoutMs = longValue(request, "autosaveTimeoutMs", 700_000L);
		boolean stressMagnetPickups = bool(request, "stressMagnetPickups", true);
		int stressStacks = integer(request, "stressStacks", 64);
		int stressCount = integer(request, "stressCount", 64);
		double stressRadius = decimal(request, "stressRadius", 2.5D);
		sendJsonHandling(exchange, LOGGER,
				() -> BackpackOperations.runIssue1528Test(autosaveCount, autosaveTimeoutMs, stressMagnetPickups, stressStacks, stressCount, stressRadius));
	}

	private static void setupIssue1528Backpacks(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::setupIssue1528BackpacksForInspection);
	}

	private static void issue1528Status(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::issue1528Status);
	}

	private static void useIssue1528Firework(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::useIssue1528Firework);
	}

	private static void issue1528RefillTick(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::tickIssue1528Refill);
	}

	private static void stressBackpacks(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		int stacks = integer(request, "stacks", 64);
		int count = integer(request, "count", 64);
		double radius = decimal(request, "radius", 2.5D);
		sendJsonHandling(exchange, LOGGER, () -> BackpackOperations.stressBackpacks(stacks, count, radius));
	}

	private static void backpackStatus(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::backpackStatus);
	}

	private static void openMainBackpack(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::openMainBackpack);
	}

	private static void openNestedBackpack(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		int nestedSlot = integer(readObject(exchange), "nestedSlot", 0);
		sendJsonHandling(exchange, LOGGER, () -> BackpackOperations.openNestedBackpack(nestedSlot));
	}

	private static void emptyBackpacks(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::emptyNestedBackpacks);
	}

	private static void clearBackpackCache(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::clearBackpackCache);
	}

	private static void setupInceptionMagnetPersistence(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::setupInceptionMagnetPersistence);
	}

	private static void pickupWithInceptionMagnet(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::pickupWithInceptionMagnet);
	}

	private static void inceptionMagnetPersistenceStatus(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::inceptionMagnetPersistenceStatus);
	}

	private static void setupLinkedStorageReload(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::setupLinkedStorageReload);
	}

	private static void linkedStorageReloadStatus(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::linkedStorageReloadStatus);
	}

	private static void changeMagnetSettings(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String target = string(request, "target", "all");
		ContentsFilterType filterType = ContentsFilterType.fromName(string(request, "filterType", "storage"));
		sendJsonHandling(exchange, LOGGER, () -> BackpackOperations.changeMagnetSettings(target, filterType));
	}

	private static void moveBackpacks(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String target = string(request, "target", "nested");
		boolean clearCache = bool(request, "clearCache", true);
		sendJsonHandling(exchange, LOGGER, () -> BackpackOperations.moveBackpacks(target, clearCache));
	}

	private static void spreadNestedBackpacks(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::spreadNestedBackpacks);
	}

	private static void fillMainBackpackNoise(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::fillMainBackpackNoise);
	}

	private static void changeMagnetPickup(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String target = string(request, "target", "nested");
		boolean pickupItems = bool(request, "pickupItems", false);
		sendJsonHandling(exchange, LOGGER, () -> BackpackOperations.changeMagnetPickup(target, pickupItems));
	}

	private static void seedBackpack(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		int nestedSlot = integer(request, "nestedSlot", 0);
		int count = integer(request, "count", 3_072);
		sendJsonHandling(exchange, LOGGER, () -> BackpackOperations.seedNestedBackpack(nestedSlot, count));
	}

	private static void bulkDropFromNestedBackpack(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		int nestedSlot = integer(request, "nestedSlot", 0);
		int maxStacks = integer(request, "maxStacks", 128);
		int pickupDelay = integer(request, "pickupDelay", 6_000);
		boolean clearCache = bool(request, "clearCache", false);
		sendJsonHandling(exchange, LOGGER, () -> BackpackOperations.bulkDropFromNestedBackpack(nestedSlot, maxStacks, pickupDelay, clearCache));
	}

	private static void backpackRemoteUpgradeSlotRegression(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackGuiRegressions::runRemoteUpgradeSlotRegression);
	}

	private static void droppedItemsStatus(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::droppedItemsStatus);
	}

	private static void clearDroppedItems(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackOperations::clearDroppedItems);
	}

}
