package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionExecution;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionMetadata;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionProgress;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionExecutionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RegressionCoordinator {
	private static final String SUITE_INDEX_RESOURCE = "/devclientautomation/regressions/index.json";
	private static final Set<String> RUNTIME_CAPABILITIES = Set.of("minecraft:client", "sophisticatedbackpacks", "sophisticatedstorage");
	private static final RegressionActionRegistry ACTIONS = new RegressionActionRegistry();
	private static RegressionCoordinator instance;

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
		List<ConformanceResult> conformance = qualify(suite, runtimeConformanceTests, test, conformanceFailureAction);
		String runId = suiteId + "/" + testId;
		if (conformance.stream().anyMatch(result -> !result.passed())) {
			String dependency = conformance.stream().filter(result -> !result.passed()).findFirst().orElseThrow().dependency();
			run = RunState.skipped(runId, suiteId, testId, conformance, "Missing action dependency " + dependency);
			return run.status();
		}
		run = RunState.running(runId, suiteId, test, runtimeConformanceTests, conformance);
		return run.status();
	}

	public synchronized RunStatus status() {
		return run == null ? RunStatus.idle() : run.status();
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
		try {
			current.tick();
		} catch (RuntimeException e) {
			current.fail(e.getMessage());
		}
	}

	private static Map<String, RegressionSuite> loadSuites() {
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
			return Map.copyOf(suites);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read regression suite resources", e);
		}
	}

	private static List<ConformanceResult> qualify(RegressionSuite suite, List<RegressionTest> runtimeConformanceTests, RegressionTest test,
			String forcedFailure) {
		List<ConformanceResult> results = new ArrayList<>();
		for (String requirement : suite.requires()) {
			results.add(new ConformanceResult("suite-requirement-" + requirement, requirement, RUNTIME_CAPABILITIES.contains(requirement), "suite capability"));
		}
		for (RegressionTest qualifiedTest : runtimeConformanceTests) {
			qualifyActions(results, qualifiedTest, forcedFailure);
		}
		qualifyActions(results, test, forcedFailure);
		return results;
	}

	private static void qualifyActions(List<ConformanceResult> results, RegressionTest test, String forcedFailure) {
		for (RegressionStep step : executionSteps(test)) {
			RegressionActionMetadata metadata = step.action().action().metadata();
			for (String dependency : metadata.dependencies()) {
				boolean passed = RUNTIME_CAPABILITIES.contains(dependency) && !metadata.id().equals(forcedFailure);
				results.add(new ConformanceResult("action-" + test.id() + "-" + step.id() + "-" + dependency, dependency, passed,
						metadata.id() + " revision " + metadata.revision()));
			}
		}
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
		private final Map<String, Object> diagnostics = new LinkedHashMap<>();
		private final RegressionExecutionContext executionContext;
		private RegressionTest activeTest;
		private List<RegressionStep> activeExecutionSteps = List.of();
		private final List<StepResult> activeSteps = new ArrayList<>();
		private boolean runningConformance;
		private int conformanceTestIndex;
		private int stepIndex;
		private RegressionStep activeStep;
		private RegressionActionExecution activeAction;
		private String outcome;
		private String message;
		private int deadlineTick = -1;

		private RunState(String runId, String suiteId, RegressionTest test, List<RegressionTest> runtimeConformanceTests, List<ConformanceResult> conformance,
				String outcome, String message) {
			this.runId = runId;
			this.suiteId = suiteId;
			this.test = test;
			this.runtimeConformanceTests = List.copyOf(runtimeConformanceTests);
			this.conformance = List.copyOf(conformance);
			this.outcome = outcome;
			this.message = message;
			executionContext = new RegressionExecutionContext(runId, diagnostics);
		}

		static RunState running(String runId, String suiteId, RegressionTest test, List<RegressionTest> runtimeConformanceTests,
				List<ConformanceResult> conformance) {
			return new RunState(runId, suiteId, test, runtimeConformanceTests, conformance, "running", "");
		}

		static RunState skipped(String runId, String suiteId, String testId, List<ConformanceResult> conformance, String message) {
			return new RunState(runId, suiteId, new RegressionTest(testId, Set.of(), 0, false, List.of()), List.of(), conformance, "skipped", message);
		}

		boolean finished() {
			return !"running".equals(outcome);
		}

		void tick() {
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
				activeAction = activeStep.action().start(executionContext);
			}
			RegressionActionProgress progress = activeAction.tick();
			if (progress == RegressionActionProgress.PASSED) {
				activeSteps.add(new StepResult(activeStep.id(), activeStep.action().action().metadata().id(), "passed", ""));
				activeStep = null;
				activeAction = null;
				stepIndex++;
			} else if (progress == RegressionActionProgress.FAILED) {
				fail(activeAction.failure());
			}
		}

		private void beginTest(RegressionTest nextTest, boolean conformanceTest) {
			activeTest = nextTest;
			activeExecutionSteps = executionSteps(nextTest);
			runningConformance = conformanceTest;
			activeSteps.clear();
			stepIndex = 0;
			deadlineTick = -1;
		}

		private void completeActiveTest() {
			if (runningConformance) {
				runtimeConformance.add(new RuntimeConformanceResult(activeTest.id(), "passed", "All steps passed", List.copyOf(activeSteps)));
				conformanceTestIndex++;
				activeTest = null;
				return;
			}
			steps.addAll(activeSteps);
			outcome = "passed";
			message = "All steps passed";
		}

		void fail(String failure) {
			if (activeAction != null) {
				activeSteps.add(new StepResult(activeStep.id(), activeStep.action().action().metadata().id(), "failed", failure));
			}
			if (runningConformance) {
				runtimeConformance.add(new RuntimeConformanceResult(activeTest.id(), "failed", failure, List.copyOf(activeSteps)));
				failure = "Runtime conformance " + activeTest.id() + " failed: " + failure;
			} else {
				steps.addAll(activeSteps);
			}
			outcome = "failed";
			message = failure == null || failure.isBlank() ? "Regression action failed" : failure;
		}

		RunStatus status() {
			return new RunStatus("passed".equals(outcome), runId, suiteId, test.id(), outcome, message, List.copyOf(conformance), List.copyOf(steps),
					List.copyOf(runtimeConformance), Map.copyOf(diagnostics));
		}
	}

	public record ConformanceResult(String id, String dependency, boolean passed, String detail) {
	}

	public record StepResult(String id, String action, String outcome, String message) {
	}

	public record RuntimeConformanceResult(String testId, String outcome, String message, List<StepResult> steps) {
	}

	public record RunStatus(boolean ok, String runId, String suiteId, String testId, String outcome, String message, List<ConformanceResult> conformance,
			List<StepResult> steps, List<RuntimeConformanceResult> runtimeConformance, Map<String, Object> diagnostics) {
		static RunStatus idle() {
			return new RunStatus(true, "", "", "", "idle", "No regression run has been requested", List.of(), List.of(), List.of(), Map.of());
		}
	}
}
