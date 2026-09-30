package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

public interface RegressionAction {
	RegressionActionMetadata metadata();

	RegressionActionInvocation bind(JsonObject args, String description);
}
