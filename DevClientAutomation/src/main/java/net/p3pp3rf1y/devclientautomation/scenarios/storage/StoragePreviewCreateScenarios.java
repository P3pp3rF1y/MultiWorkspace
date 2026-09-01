package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;

import java.util.function.Supplier;

/**
 * Keeps incompatible Create automation separate from regular storage previews.
 */
final class StoragePreviewCreateScenarios {
	private static final String TARGET_ERROR = "Create preview automation is unavailable on this target because its Create binary is incompatible";

	private StoragePreviewCreateScenarios() {
	}

	static String openItemDisplayPreview(String scenario, DisplaySide displaySide) {
		return "{\"ok\":false,\"scenario\":\"" + scenario + "\",\"displaySide\":\"" + displaySide.getSerializedName() + "\",\"error\":\"" + TARGET_ERROR
				+ "\"}";
	}

	static Supplier<String> reproduceIssue23() {
		return StoragePreviewCreateScenarios::unavailableResult;
	}

	static Supplier<String> issue23Status() {
		return StoragePreviewCreateScenarios::unavailableResult;
	}

	static Supplier<String> openIssue23SourceStorage() {
		return StoragePreviewCreateScenarios::unavailableResult;
	}

	private static String unavailableResult() {
		return "{\"ok\":false,\"error\":\"" + TARGET_ERROR + "\"}";
	}
}
