package net.p3pp3rf1y.devclientautomation.regression.actions;

public interface RegressionActionExecution {
	RegressionActionProgress tick();

	String failure();
}
