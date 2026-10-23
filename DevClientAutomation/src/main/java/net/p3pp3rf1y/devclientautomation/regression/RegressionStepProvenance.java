package net.p3pp3rf1y.devclientautomation.regression;

/** Immutable source identity retained alongside the existing flattened execution ID. */
public record RegressionStepProvenance(String origin, String fixture, String referenceId, String memberId, String expandedId) {
	public static RegressionStepProvenance direct(String id) {
		return new RegressionStepProvenance("direct", "", "", "", id);
	}
}
