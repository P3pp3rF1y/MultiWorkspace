package net.p3pp3rf1y.devclientautomation.regression;

import javax.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Platform-independent result semantics shared by status and every report consumer. */
public final class RegressionResultModel {
	private RegressionResultModel() {
	}

	static int priority(String outcome) {
		return switch (outcome) {
			case "idle", "running", "passed" -> 0;
			case "skipped" -> 1;
			case "notRun" -> 2;
			case "cancelled" -> 3;
			case "blocked" -> 4;
			case "failed" -> 5;
			case "error" -> 6;
			default -> throw new IllegalArgumentException("Unknown regression outcome " + outcome);
		};
	}

	public record Failure(String category, String message, String exceptionType, String stackTrace) {
		public static Failure message(String category, String message) {
			return new Failure(category, message, "", "");
		}

		public static Failure exception(String category, Throwable exception) {
			StringWriter stack = new StringWriter();
			exception.printStackTrace(new PrintWriter(stack));
			return new Failure(category, exception.toString(), exception.getClass().getName(), stack.toString());
		}
	}

	public record ReportCase(String testId, String classname, String role, String outcome, String category, String message, double durationMillis,
			String junitElement, Map<String, Object> diagnostics) {
		public static ReportCase of(String testId, String classname, String role, String outcome, String category, String message, double durationMillis,
				Map<String, Object> diagnostics) {
			String element = switch (outcome) {
				case "passed", "running", "idle" -> "";
				case "failed" -> "failure";
				case "error" -> "error";
				case "skipped", "blocked", "cancelled", "notRun" -> "skipped";
				default -> throw new IllegalArgumentException("Unknown regression outcome " + outcome);
			};
			return new ReportCase(testId, classname, role, outcome, category, message, durationMillis, element, Map.copyOf(diagnostics));
		}
	}

	static List<ReportCase> cases(String suiteId, String testId, String primaryOutcome, String primaryCategory, String message, double durationMillis,
			List<ConformanceResult> conformance, List<RuntimeConformanceResult> runtimeConformance, List<CleanupResult> cleanup, List<StepResult> steps) {
		return cases(suiteId, testId, primaryOutcome, primaryCategory, message, durationMillis, conformance, runtimeConformance, cleanup, steps, null,
				Map.of());
	}

	private static List<ReportCase> cases(String suiteId, String testId, String primaryOutcome, String primaryCategory, String message, double durationMillis,
			List<ConformanceResult> conformance, List<RuntimeConformanceResult> runtimeConformance, List<CleanupResult> cleanup, List<StepResult> steps,
			@Nullable Failure primaryFailure, Map<String, Object> diagnostics) {
		List<ReportCase> cases = new ArrayList<>();
		for (ConformanceResult result : conformance) {
			cases.add(ReportCase.of(result.id(), suiteId + ".conformance", "conformance", result.passed() ? "passed" : "error",
					result.passed() ? "" : "actionConformance", result.dependency() + ": " + result.detail(), 0, diagnostics));
		}
		for (RuntimeConformanceResult result : runtimeConformance) {
			cases.add(ReportCase.of(result.testId(), suiteId + ".runtimeConformance", "conformance", result.primaryOutcome(), result.category(),
					result.message(), result.durationMillis(), evidence(result.steps(), result.failure(), diagnostics)));
		}
		cases.add(ReportCase.of(testId, suiteId, "scenario", primaryOutcome, primaryCategory, message, durationMillis,
				evidence(steps, primaryFailure, diagnostics)));
		for (CleanupResult result : cleanup) {
			cases.add(ReportCase.of(result.testId() + ".cleanup", suiteId + ".cleanup", "cleanup", result.outcome(),
					result.outcome().equals("passed") ? "" : "cleanup", result.message(), result.durationMillis(),
					evidence(List.of(), result.failure(), diagnostics)));
		}
		return List.copyOf(cases);
	}

	private static Map<String, Object> evidence(List<StepResult> steps, @Nullable Failure failure, Map<String, Object> diagnostics) {
		Map<String, Object> evidence = new java.util.LinkedHashMap<>(diagnostics);
		evidence.put("steps", steps);
		if (failure != null) {
			evidence.put("failure", failure);
		}
		return Map.copyOf(evidence);
	}

	public record RuntimeIdentity(String loader, String minecraftVersion, long processId, Map<String, Object> controller) {
		public RuntimeIdentity {
			controller = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(controller));
		}
	}

	public record ReportContext(RuntimeIdentity runtimeIdentity, String rerunCommand, Map<String, String> artifacts) {
		public ReportContext {
			artifacts = Map.copyOf(artifacts);
		}
		public static ReportContext unknown() {
			return new ReportContext(new RuntimeIdentity("", "", 0, Map.of()), "", Map.of());
		}
	}

	public enum ControllerOutcome {
		ERROR, NOT_RUN
	}

	/** Used by the offline controller tool as well as runtime serialization: no frontend outcome/JUnit inference. */
	public static RunStatus controller(ControllerOutcome kind, String suiteId, String testId, String message, @Nullable Failure failure,
			Set<String> effectiveRequirements, ReportContext context) {
		String outcome = kind == ControllerOutcome.ERROR ? "error" : "notRun";
		Failure primary = kind == ControllerOutcome.ERROR ? failure : null;
		return new RunStatus(false, suiteId + "/" + testId, suiteId, testId, outcome, message, List.of(), List.of(), List.of(), Map.of(), 0, 0, List.of(),
				"preflight", "infrastructure", effectiveRequirements, List.of(), outcome, "infrastructure", primary, null, context.rerunCommand(), List.of(), 0,
				message, context.runtimeIdentity(), context.artifacts());
	}

	public record ConformanceResult(String id, String dependency, boolean passed, String detail) {
	}

	public record StepResult(String id, String action, String outcome, String message, double durationMillis, String revision, String category,
			RegressionStepProvenance provenance, Map<String, Object> expectedActual, @Nullable Failure failure) {
		public StepResult(String id, String action, String outcome, String message, double durationMillis) {
			this(id, action, outcome, message, durationMillis, "1", "", RegressionStepProvenance.direct(id), Map.of(), null);
		}
	}

	public record RuntimeConformanceResult(String testId, String outcome, String message, List<StepResult> steps, double durationMillis, String primaryOutcome,
			String category, @Nullable Failure failure) {
		public RuntimeConformanceResult(String testId, String outcome, String message, List<StepResult> steps, double durationMillis, String primaryOutcome,
				String category) {
			this(testId, outcome, message, steps, durationMillis, primaryOutcome, category, null);
		}
		public RuntimeConformanceResult(String testId, String outcome, String message, List<StepResult> steps, double durationMillis) {
			this(testId, outcome, message, steps, durationMillis, outcome, outcome.equals("passed") ? "" : "actionConformance", null);
		}
	}

	public record CleanupResult(String testId, String outcome, String message, double durationMillis, @Nullable Failure failure) {
		public CleanupResult(String testId, String outcome, String message, double durationMillis) {
			this(testId, outcome, message, durationMillis, null);
		}
		public CleanupResult(String testId, String outcome, String message) {
			this(testId, outcome, message, 0, null);
		}
	}

	public record RunStatus(boolean ok, String runId, String suiteId, String testId, String outcome, String message, List<ConformanceResult> conformance,
			List<StepResult> steps, List<RuntimeConformanceResult> runtimeConformance, Map<String, Object> diagnostics, double durationMillis,
			double testDurationMillis, List<CleanupResult> cleanup, String phase, String category, Set<String> effectiveRequirements,
			List<String> missingRequirements, String primaryOutcome, String primaryCategory, @Nullable Failure primaryFailure, @Nullable Failure cleanupFailure,
			String rerunCommand, List<ReportCase> reportCases, int outcomePriority, String primaryMessage, RuntimeIdentity runtimeIdentity,
			Map<String, String> artifacts) {
		public RunStatus {
			outcomePriority = priority(outcome);
			Map<String, Object> reportDiagnostics = new java.util.LinkedHashMap<>(diagnostics);
			reportDiagnostics.put("runtimeIdentity", runtimeIdentity);
			reportDiagnostics.put("artifacts", artifacts);
			reportDiagnostics.put("rerunCommand", rerunCommand);
			reportDiagnostics.put("effectiveRequirements", effectiveRequirements);
			reportDiagnostics.put("missingRequirements", missingRequirements);
			List<CleanupResult> cleanupEvidence = cleanup.stream()
					.map(result -> result.failure() == null && result.outcome().equals("error")
							? new CleanupResult(result.testId(), result.outcome(), result.message(), result.durationMillis(), cleanupFailure)
							: result)
					.toList();
			reportCases = cases(suiteId, testId, primaryOutcome, primaryCategory, primaryMessage, testDurationMillis, conformance, runtimeConformance,
					cleanupEvidence, steps, primaryFailure, reportDiagnostics);
		}
		public RunStatus(boolean ok, String runId, String suiteId, String testId, String outcome, String message, List<ConformanceResult> conformance,
				List<StepResult> steps, List<RuntimeConformanceResult> runtimeConformance, Map<String, Object> diagnostics, double durationMillis,
				double testDurationMillis, List<CleanupResult> cleanup, String phase, String category, Set<String> effectiveRequirements,
				List<String> missingRequirements, String primaryOutcome, String primaryCategory, @Nullable Failure primaryFailure,
				@Nullable Failure cleanupFailure, String rerunCommand, List<ReportCase> reportCases, int outcomePriority, String primaryMessage) {
			this(ok, runId, suiteId, testId, outcome, message, conformance, steps, runtimeConformance, diagnostics, durationMillis, testDurationMillis, cleanup,
					phase, category, effectiveRequirements, missingRequirements, primaryOutcome, primaryCategory, primaryFailure, cleanupFailure, rerunCommand,
					reportCases, outcomePriority, primaryMessage, ReportContext.unknown().runtimeIdentity(), Map.of());
		}
		public RunStatus(boolean ok, String runId, String suiteId, String testId, String outcome, String message, List<ConformanceResult> conformance,
				List<StepResult> steps, List<RuntimeConformanceResult> runtimeConformance, Map<String, Object> diagnostics, double durationMillis,
				double testDurationMillis, List<CleanupResult> cleanup, String phase) {
			this(ok, runId, suiteId, testId, outcome, message, conformance, steps, runtimeConformance, diagnostics, durationMillis, testDurationMillis, cleanup,
					phase, "", Set.of(), List.of(), outcome, "", null, null, "", List.of(), 0, message);
		}
		static RunStatus idle() {
			return new RunStatus(true, "", "", "", "idle", "No regression run has been requested", List.of(), List.of(), List.of(), Map.of(), 0, 0, List.of(),
					"idle");
		}
	}
}
