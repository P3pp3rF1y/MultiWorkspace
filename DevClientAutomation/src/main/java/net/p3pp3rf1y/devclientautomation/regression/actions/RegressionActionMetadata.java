package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.Set;

public record RegressionActionMetadata(String id, String revision, Set<String> dependencies) {
}
