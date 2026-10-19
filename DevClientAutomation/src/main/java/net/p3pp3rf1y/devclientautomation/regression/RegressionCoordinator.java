package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionExecution;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionMetadata;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionProgress;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionExecutionContext;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class RegressionCoordinator {
	private static final String SUITE_INDEX_RESOURCE = "/devclientautomation/regressions/index.json";
	private static final Set<String> RUNTIME_CAPABILITIES = Set.of("minecraft:client");
	private static final RegressionActionRegistry ACTIONS = new RegressionActionRegistry();
	@Nullable
	private static RegressionCoordinator instance;

	@Nullable
	private RunState run;

	private RegressionCoordinator() {
	}

	public static void init() {
		if (instance == null) {
			instance = new RegressionCoordinator();
			NeoForge.EVENT_BUS.addListener(instance::tick);
		}
	}

	public static RegressionCoordinator get() {
		if (instance == null) {
			throw new IllegalStateException("Regression coordinator is not initialized");
		}
		return instance;
	}

	public synchronized RunStatus start(String suiteId, String testId, String conformanceFailureAction) {
		return start(suiteId, testId, conformanceFailureAction, "", false);
	}

	public synchronized RunStatus start(String suiteId, String testId, String conformanceFailureAction, String actionFailureAfter, boolean cleanupFailure) {
		return start(suiteId, testId, conformanceFailureAction, actionFailureAfter, cleanupFailure, "");
	}

	public synchronized RunStatus start(String suiteId, String testId, String conformanceFailureAction, String actionFailureAfter, boolean cleanupFailure,
			String preparationToken) {
		if (run != null && !run.finished()) {
			throw new IllegalStateException("Regression run " + run.runId + " is already active");
		}
		RegressionSuite suite = loadSuites().get(suiteId);
		if (suite == null) {
			throw new IllegalArgumentException("No regression suite with id " + suiteId);
		}
		RegressionTest test = suite.tests().stream().filter(candidate -> candidate.id().equals(testId)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No regression test " + suiteId + "/" + testId));
		List<RegressionTest> runtimeConformanceTests = test.tags().contains("runtimeConformance")
				? List.of()
				: suite.tests().stream().filter(candidate -> candidate.tags().contains("runtimeConformance")).toList();
		List<ConformanceResult> conformance = qualify(suite, runtimeConformanceTests, test, conformanceFailureAction, RegressionCoordinator::isAvailable);
		String runId = suiteId + "/" + testId;
		if (conformance.stream().anyMatch(result -> !result.passed())) {
			String dependency = conformance.stream().filter(result -> !result.passed()).findFirst().orElseThrow().dependency();
			run = RunState.skipped(runId, suiteId, testId, conformance, "Missing action dependency " + dependency);
			return run.status();
		}
		run = RunState.running(runId, suiteId, test, runtimeConformanceTests, conformance, actionFailureAfter, cleanupFailure);
		run.preparationToken = preparationToken;
		return run.status();
	}

	public synchronized RunStatus status() {
		return run == null ? RunStatus.idle() : run.status();
	}

	public synchronized RunStatus cancel() {
		if (run == null || run.finished()) {
			throw new IllegalStateException("There is no active regression run to cancel");
		}
		run.cancelRequested = true;
		return run.status();
	}

	public Map<String, RegressionActionMetadata> actions() {
		return ACTIONS.all();
	}

	public Map<String, RegressionSuite> suites() {
		return loadSuites();
	}

	private void tick(ClientTickEvent.Post event) {
		RunState current;
		synchronized (this) {
			current = run;
		}
		if (current == null || current.finished()) {
			return;
		}
		synchronized (this) {
			try {
				current.tick();
			} catch (RuntimeException e) {
				current.fail(e.getMessage());
			}
		}
	}

	static Map<String, RegressionSuite> loadSuites() {
		try (InputStream stream = RegressionCoordinator.class.getResourceAsStream(SUITE_INDEX_RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Regression suite index resource is missing: " + SUITE_INDEX_RESOURCE);
			}
			JsonElement parsed = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
			if (!parsed.isJsonArray()) {
				throw new IllegalStateException("Regression suite index must be a JSON array");
			}
			Map<String, RegressionSuite> suites = new LinkedHashMap<>();
			for (JsonElement element : parsed.getAsJsonArray()) {
				if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
					throw new IllegalStateException("Regression suite index contains a non-string resource");
				}
				String resource = "/devclientautomation/regressions/" + element.getAsString();
				try (InputStream suiteStream = RegressionCoordinator.class.getResourceAsStream(resource)) {
					if (suiteStream == null) {
						throw new IllegalStateException("Regression suite resource is missing: " + resource);
					}
					RegressionSuite suite = RegressionSuiteParser.parse(new String(suiteStream.readAllBytes(), StandardCharsets.UTF_8), ACTIONS);
					if (suites.putIfAbsent(suite.suiteId(), suite) != null) {
						throw new IllegalStateException("Regression suite index contains duplicate suite id " + suite.suiteId());
					}
				}
			}
			return Collections.unmodifiableMap(new LinkedHashMap<>(suites));
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read regression suite resources", e);
		}
	}

	static List<ConformanceResult> qualify(RegressionSuite suite, List<RegressionTest> runtimeConformanceTests, RegressionTest test, String forcedFailure,
			Predicate<String> capabilityAvailable) {
		List<ConformanceResult> results = new ArrayList<>();
		for (String requirement : suite.requires()) {
			results.add(new ConformanceResult("suite-requirement-" + requirement, requirement, capabilityAvailable.test(requirement), "suite capability"));
		}
		for (RegressionTest qualifiedTest : runtimeConformanceTests) {
			qualifyActions(results, qualifiedTest, forcedFailure, capabilityAvailable);
		}
		qualifyActions(results, test, forcedFailure, capabilityAvailable);
		return results;
	}

	private static void qualifyActions(List<ConformanceResult> results, RegressionTest test, String forcedFailure, Predicate<String> capabilityAvailable) {
		for (RegressionStep step : executionSteps(test)) {
			RegressionActionMetadata metadata = step.action().action().metadata();
			for (String dependency : step.action().dependencies()) {
				boolean passed = capabilityAvailable.test(dependency) && !metadata.id().equals(forcedFailure);
				results.add(new ConformanceResult("action-" + test.id() + "-" + step.id() + "-" + dependency, dependency, passed,
						metadata.id() + " revision " + metadata.revision()));
			}
		}
	}

	private static boolean isAvailable(String requirement) {
		return RUNTIME_CAPABILITIES.contains(requirement) || ModList.get().isLoaded(requirement);
	}

	private static List<RegressionStep> executionSteps(RegressionTest test) {
		if (!test.prepareTestCell()) {
			return test.steps();
		}
		List<RegressionStep> steps = new ArrayList<>(ACTIONS.testCellSetup());
		steps.addAll(test.steps());
		return List.copyOf(steps);
	}

	private static final class RunState {
		private final String runId;
		private final String suiteId;
		private final RegressionTest test;
		private final List<RegressionTest> runtimeConformanceTests;
		private final List<ConformanceResult> conformance;
		private final List<StepResult> steps = new ArrayList<>();
		private final List<RuntimeConformanceResult> runtimeConformance = new ArrayList<>();
		private final Map<String, Object> diagnostics = new ConcurrentHashMap<>();
		private final List<CleanupResult> cleanup = new ArrayList<>();
		private final String actionFailureAfter;
		private final boolean forcedCleanupFailure;
		private final long startedNanos = System.nanoTime();
		private RegressionExecutionContext executionContext;
		private String preparationToken = "";
		@Nullable
		private RegressionTest activeTest;
		private List<RegressionStep> activeExecutionSteps = List.of();
		private final List<StepResult> activeSteps = new ArrayList<>();
		private boolean runningConformance;
		private int conformanceTestIndex;
		private int stepIndex;
		@Nullable
		private RegressionStep activeStep;
		@Nullable
		private RegressionActionExecution activeAction;
		@Nullable
		private CompletableFuture<Void> journalPreparation;
		@Nullable
		private CompletableFuture<Void> cleanupFuture;
		private boolean cancelRequested;
		@Nullable
		private Outcome terminalOutcome;
		private String primaryFailure = "";
		private Outcome outcome;
		private String message;
		private int deadlineTick = -1;
		private long activeTestStartedNanos;
		private long activeActionStartedNanos;
		private long selectedTestDurationNanos;
		private long finishedNanos;

		private RunState(String runId, String suiteId, RegressionTest test, List<RegressionTest> runtimeConformanceTests, List<ConformanceResult> conformance,
				Outcome outcome, String message, String actionFailureAfter, boolean forcedCleanupFailure) {
			this.runId = runId;
			this.suiteId = suiteId;
			this.test = test;
			this.runtimeConformanceTests = List.copyOf(runtimeConformanceTests);
			this.conformance = List.copyOf(conformance);
			this.outcome = outcome;
			this.message = message;
			this.actionFailureAfter = actionFailureAfter;
			this.forcedCleanupFailure = forcedCleanupFailure;
			executionContext = new RegressionExecutionContext(runId, diagnostics);
			if (outcome != Outcome.RUNNING) {
				finishedNanos = System.nanoTime();
			}
		}

		static RunState running(String runId, String suiteId, RegressionTest test, List<RegressionTest> runtimeConformanceTests,
				List<ConformanceResult> conformance, String actionFailureAfter, boolean forcedCleanupFailure) {
			return new RunState(runId, suiteId, test, runtimeConformanceTests, conformance, Outcome.RUNNING, "", actionFailureAfter, forcedCleanupFailure);
		}

		static RunState skipped(String runId, String suiteId, String testId, List<ConformanceResult> conformance, String message) {
			return new RunState(runId, suiteId, new RegressionTest(testId, Set.of(), 0, false, List.of()), List.of(), conformance, Outcome.SKIPPED, message, "",
					false);
		}

		boolean finished() {
			return outcome != Outcome.RUNNING;
		}

		void tick() {
			if (cleanupFuture != null) {
				pollCleanup();
				return;
			}
			if (cancelRequested) {
				terminate(Outcome.CANCELLED, "Regression run was cancelled");
				return;
			}
			if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
				return;
			}
			if (activeTest == null) {
				if (conformanceTestIndex < runtimeConformanceTests.size()) {
					beginTest(runtimeConformanceTests.get(conformanceTestIndex), true);
				} else {
					beginTest(test, false);
				}
			}
			if (journalPreparation == null) {
				journalPreparation = executionContext.onServer(executionContext::prepareJournal);
				return;
			}
			if (!journalPreparation.isDone()) {
				return;
			}
			try {
				journalPreparation.join();
			} catch (RuntimeException e) {
				fail("Could not prepare recovery journal: " + rootCause(e));
				return;
			}
			if (deadlineTick < 0) {
				deadlineTick = Minecraft.getInstance().player.tickCount + activeTest.timeoutTicks();
			} else if (Minecraft.getInstance().player.tickCount >= deadlineTick) {
				fail("Test exceeded timeoutTicks " + activeTest.timeoutTicks());
				return;
			}
			if (activeAction == null) {
				if (stepIndex >= activeExecutionSteps.size()) {
					completeActiveTest();
					return;
				}
				activeStep = activeExecutionSteps.get(stepIndex);
				activeActionStartedNanos = System.nanoTime();
				activeAction = activeStep.action().start(executionContext);
			}
			RegressionActionProgress progress;
			String actionFailure = "";
			try {
				progress = activeAction.tick();
				if (progress == RegressionActionProgress.FAILED) {
					actionFailure = activeAction.failure();
				}
			} catch (RuntimeException e) {
				progress = RegressionActionProgress.FAILED;
				actionFailure = rootCause(e);
			}
			if (!activeStep.expectFailure().isEmpty() && progress != RegressionActionProgress.WAITING) {
				if (progress != RegressionActionProgress.FAILED || !actionFailure.contains(activeStep.expectFailure())) {
					fail("Expected diagnostic '" + activeStep.expectFailure() + "', observed " + progress + ": " + actionFailure);
					return;
				}
				progress = RegressionActionProgress.PASSED;
			}
			if (progress == RegressionActionProgress.PASSED) {
				if (activeStep.action().action().metadata().id().equals(actionFailureAfter)) {
					fail("Injected action failure after " + actionFailureAfter);
					return;
				}
				activeSteps.add(new StepResult(activeStep.id(), activeStep.action().action().metadata().id(), "passed", actionFailure,
						elapsedMillis(activeActionStartedNanos, System.nanoTime())));
				activeStep = null;
				activeAction = null;
				activeActionStartedNanos = 0;
				stepIndex++;
			} else if (progress == RegressionActionProgress.FAILED) {
				fail(actionFailure);
			}
		}

		private void beginTest(RegressionTest nextTest, boolean conformanceTest) {
			executionContext = new RegressionExecutionContext(runId + "/" + nextTest.id(), diagnostics);
			executionContext.configurePlayerState(nextTest.playerState(), nextTest.checkpoint());
			executionContext.putDiagnostic("expectedPreparationToken", preparationToken);
			activeTest = nextTest;
			activeExecutionSteps = executionSteps(nextTest);
			activeTestStartedNanos = System.nanoTime();
			runningConformance = conformanceTest;
			activeSteps.clear();
			stepIndex = 0;
			deadlineTick = -1;
			activeStep = null;
			activeAction = null;
			activeActionStartedNanos = 0;
			journalPreparation = null;
		}

		private void completeActiveTest() {
			executionContext.markSuccessful();
			terminate(Outcome.PASSED, "All steps passed");
		}

		private void completeAfterCleanup(boolean cleaned, String cleanupMessage) {
			long now = System.nanoTime();
			cleanup.add(new CleanupResult(activeTest.id(), cleaned ? "passed" : "failed", cleanupMessage));
			Outcome result = cleaned ? terminalOutcome : Outcome.FAILED;
			String detail = cleaned ? primaryFailure : primaryFailure + (primaryFailure.isBlank() ? "" : "; ") + "Cleanup failed: " + cleanupMessage;
			if (runningConformance) {
				runtimeConformance.add(new RuntimeConformanceResult(activeTest.id(), result.serializedName(), detail, List.copyOf(activeSteps),
						elapsedMillis(activeTestStartedNanos, now)));
				if (result == Outcome.PASSED) {
					conformanceTestIndex++;
					activeTest = null;
					cleanupFuture = null;
					return;
				}
				detail = "Runtime conformance " + activeTest.id() + " failed: " + detail;
			} else {
				steps.addAll(activeSteps);
				selectedTestDurationNanos = now - activeTestStartedNanos;
			}
			finishedNanos = now;
			outcome = result;
			message = detail;
		}

		void fail(@Nullable String failure) {
			if (cleanupFuture != null) {
				return;
			}
			long now = System.nanoTime();
			if (activeStep != null) {
				activeSteps.add(new StepResult(activeStep.id(), activeStep.action().action().metadata().id(), "failed", failure,
						elapsedMillis(activeActionStartedNanos, now)));
			}
			terminate(Outcome.FAILED, failure == null || failure.isBlank() ? "Regression action failed" : failure);
		}

		private void terminate(Outcome desiredOutcome, String detail) {
			terminalOutcome = desiredOutcome;
			primaryFailure = detail;
			executionContext.minecraft().options.keyShift.setDown(false);
			activeAction = null;
			activeStep = null;
			if (activeTest == null) {
				outcome = desiredOutcome;
				message = detail;
				finishedNanos = System.nanoTime();
				return;
			}
			cleanupFuture = executionContext.onServer(player -> {
				player.setShiftKeyDown(false);
				if (forcedCleanupFailure) {
					throw new IllegalStateException("Injected cleanup failure");
				}
				executionContext.cleanupJournal(player);
			});
		}

		private void pollCleanup() {
			if (!cleanupFuture.isDone()) {
				return;
			}
			try {
				cleanupFuture.join();
				completeAfterCleanup(true, "World mutations restored");
			} catch (RuntimeException e) {
				completeAfterCleanup(false, rootCause(e));
			}
		}

		private static String rootCause(RuntimeException e) {
			return e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
		}

		RunStatus status() {
			long now = finishedNanos == 0 ? System.nanoTime() : finishedNanos;
			long testNanos = selectedTestDurationNanos;
			if (!runningConformance && activeTestStartedNanos != 0 && !finished()) {
				testNanos = now - activeTestStartedNanos;
			}
			return new RunStatus(outcome == Outcome.PASSED, runId, suiteId, test.id(), outcome.serializedName(), message, List.copyOf(conformance),
					List.copyOf(steps), List.copyOf(runtimeConformance), Map.copyOf(diagnostics), elapsedMillis(startedNanos, now), testNanos / 1_000_000.0,
					List.copyOf(cleanup), cleanupFuture != null && !cleanupFuture.isDone() ? "cleanup" : "execution");
		}
	}

	private static double elapsedMillis(long startNanos, long endNanos) {
		return startNanos == 0 ? 0 : (endNanos - startNanos) / 1_000_000.0;
	}

	private enum Outcome {
		RUNNING, PASSED, FAILED, SKIPPED, CANCELLED;
		String serializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public record ConformanceResult(String id, String dependency, boolean passed, String detail) {
	}

	public record StepResult(String id, String action, String outcome, String message, double durationMillis) {
	}

	public record RuntimeConformanceResult(String testId, String outcome, String message, List<StepResult> steps, double durationMillis) {
	}

	public record CleanupResult(String testId, String outcome, String message) {
	}

	public record RunStatus(boolean ok, String runId, String suiteId, String testId, String outcome, String message, List<ConformanceResult> conformance,
			List<StepResult> steps, List<RuntimeConformanceResult> runtimeConformance, Map<String, Object> diagnostics, double durationMillis,
			double testDurationMillis, List<CleanupResult> cleanup, String phase) {
		static RunStatus idle() {
			return new RunStatus(true, "", "", "", "idle", "No regression run has been requested", List.of(), List.of(), List.of(), Map.of(), 0, 0, List.of(),
					"idle");
		}
	}
}
