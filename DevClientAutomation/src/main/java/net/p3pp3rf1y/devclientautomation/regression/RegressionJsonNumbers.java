package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonElement;

/** Strict JSON integer conversion shared by regression schema readers. */
public final class RegressionJsonNumbers {
	private RegressionJsonNumbers() {
	}

	public static int integer(JsonElement value) {
		return Integer.parseInt(value.getAsString());
	}
}
