package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;

public interface RegressionAction {
	String CLIENT_CAPABILITY = "minecraft:client";
	RegressionActionMetadata metadata();

	RegressionActionInvocation bind(JsonObject args, String description);
}
