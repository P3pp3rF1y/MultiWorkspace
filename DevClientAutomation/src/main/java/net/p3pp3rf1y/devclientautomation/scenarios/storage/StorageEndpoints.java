package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import static net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime.runOnClient;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.bool;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.integer;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class StorageEndpoints {
	private static final Logger LOGGER = LoggerFactory.getLogger("devclientautomation");

	private StorageEndpoints() {
	}

	public static void register(EndpointRegistry endpoints) {
		endpoints.register("/storage/controller-double-chest-regressions", StorageEndpoints::storageControllerDoubleChestRegressions);
		endpoints.register("/storage/controller-double-chest-tier-upgrade-regressions", StorageEndpoints::storageControllerDoubleChestTierUpgradeRegressions);
		endpoints.register("/storage/controller-filter-regressions", StorageEndpoints::storageControllerFilterRegressions);
		endpoints.register("/storage/simple-material-decoration-verification", StorageEndpoints::simpleMaterialDecorationVerification);
		endpoints.register("/storage/simple-material-render-setup", StorageEndpoints::simpleMaterialRenderSetup);
		endpoints.register("/storage/simple-material-overlay-comparison-setup", StorageEndpoints::simpleMaterialOverlayComparisonSetup);
		endpoints.register("/storage/item-display-preview/open", StorageEndpoints::openStorageItemDisplayPreview);
		endpoints.register("/storage/decoration-table-render-preview/open", StorageEndpoints::openDecorationTableRenderPreview);
		endpoints.register("/storage/decoration-table-render-preview/drag", StorageEndpoints::dragDecorationTableRenderPreview);
		endpoints.register("/storage/issue-23-reproduce", StorageEndpoints::reproduceStorageIssue23);
		endpoints.register("/storage/issue-23-status", StorageEndpoints::issue23Status);
		endpoints.register("/storage/issue-23-open-source", StorageEndpoints::openIssue23SourceStorage);
		endpoints.register("/storage/linked-storage-regression", StorageEndpoints::linkedStorageRegression);
		endpoints.register("/storage/linked-double-chest-lifecycle-regression", StorageEndpoints::linkedDoubleChestLifecycleRegression);
		endpoints.register("/storage/linked-limited-reload/setup", StorageEndpoints::setupLinkedLimitedReload);
		endpoints.register("/storage/linked-limited-reload/status", StorageEndpoints::linkedLimitedReloadStatus);
	}

	private static void storageControllerDoubleChestRegressions(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		boolean inspectOnly = bool(request, "inspectOnly", false);
		sendJsonHandling(exchange, LOGGER, () -> StorageControllerRegressions.runDoubleChestRegressions(inspectOnly));
	}

	private static void storageControllerDoubleChestTierUpgradeRegressions(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, StorageControllerRegressions::runDoubleChestTierUpgradeRegressions);
	}

	private static void storageControllerFilterRegressions(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String mode = string(request, "mode", "run");
		int runs = integer(request, "runs", 1);
		boolean profileCapacity = bool(request, "profileCapacity", false);
		sendJsonHandling(exchange, LOGGER, () -> StorageControllerRegressions.runFilterRegressions(mode, runs, profileCapacity));
	}

	private static void simpleMaterialDecorationVerification(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, SimpleMaterialScenarios::verifyDecoration);
	}

	private static void simpleMaterialRenderSetup(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, SimpleMaterialScenarios::setupRenderVerification);
	}

	private static void simpleMaterialOverlayComparisonSetup(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, SimpleMaterialScenarios::setupOverlayComparison);
	}

	private static void openStorageItemDisplayPreview(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String scenario = string(request, "scenario", "barrel_north");
		DisplaySide displaySide = DisplaySide.fromName(string(request, "displaySide", DisplaySide.FRONT.getSerializedName()));
		sendJsonHandling(exchange, LOGGER, () -> StoragePreviewScenarios.openItemDisplayPreview(scenario, displaySide));
	}

	private static void openDecorationTableRenderPreview(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		String itemName = string(readObject(exchange), "item", "storage_io");
		sendJsonHandling(exchange, LOGGER, () -> StoragePreviewScenarios.openDecorationTableRenderPreview(itemName));
	}

	private static void dragDecorationTableRenderPreview(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		double dragX = integer(request, "dragX", 0);
		double dragY = integer(request, "dragY", 0);
		sendJsonHandling(exchange, LOGGER, () -> runOnClient(() -> StoragePreviewScenarios.dragDecorationTableRenderPreview(dragX, dragY)));
	}

	private static void reproduceStorageIssue23(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, StoragePreviewScenarios.reproduceIssue23());
	}

	private static void issue23Status(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		sendJsonHandling(exchange, LOGGER, StoragePreviewScenarios.issue23Status());
	}

	private static void openIssue23SourceStorage(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, StoragePreviewScenarios.openIssue23SourceStorage());
	}

	private static void linkedStorageRegression(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, StorageLinkedStorageRegression::run);
	}

	private static void linkedDoubleChestLifecycleRegression(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, StorageLinkedStorageRegression::runLinkedDoubleChestLifecycleRegression);
	}

	private static void setupLinkedLimitedReload(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, StorageLinkedStorageRegression::setupLinkedLimitedBarrelReloadProjection);
	}

	private static void linkedLimitedReloadStatus(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		UUID groupId = UUID.fromString(string(readObject(exchange), "groupId", ""));
		sendJsonHandling(exchange, LOGGER, () -> StorageLinkedStorageRegression.linkedLimitedBarrelReloadProjectionStatus(groupId));
	}

}
