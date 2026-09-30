package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionInvocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RegressionSuiteParser {
	private static final Set<String> SUITE_KEYS = Set.of("schemaVersion", "suiteId", "requires", "isolation", "tests");
	private static final Set<String> TEST_KEYS = Set.of("id", "tags", "timeoutTicks", "prepareTestCell", "steps");
	private static final Set<String> STEP_KEYS = Set.of("id", "action", "args");

	private RegressionSuiteParser() {
	}

	public static RegressionSuite parse(String json, RegressionActionRegistry registry) {
		JsonElement parsed = JsonParser.parseString(json);
		if (!parsed.isJsonObject()) {
			throw new IllegalArgumentException("Regression suite must be a JSON object");
		}
		return parse(parsed.getAsJsonObject(), registry);
	}

	public static RegressionSuite parse(JsonObject root, RegressionActionRegistry registry) {
		RegressionJsonObject suite = new RegressionJsonObject(root, "suite", SUITE_KEYS);
		int schemaVersion = suite.integer("schemaVersion");
		if (schemaVersion != 2) {
			throw new IllegalArgumentException("Unsupported regression schemaVersion " + schemaVersion + "; expected 2");
		}
		String suiteId = suite.id("suiteId");
		Set<String> requires = suite.stringSet("requires");
		String isolation = suite.string("isolation");
		if (!"testCell".equals(isolation)) {
			throw new IllegalArgumentException("Suite " + suiteId + " must use isolation testCell");
		}
		List<JsonObject> tests = suite.objectArray("tests", "Suite " + suiteId + " contains a non-object test");
		if (tests.isEmpty()) {
			throw new IllegalArgumentException("Suite " + suiteId + " must define at least one test");
		}
		return new RegressionSuite(schemaVersion, suiteId, requires, isolation, parseTests(tests, suiteId, registry));
	}

	private static List<RegressionTest> parseTests(List<JsonObject> tests, String suiteId, RegressionActionRegistry registry) {
		List<RegressionTest> parsedTests = new ArrayList<>();
		Set<String> testIds = new LinkedHashSet<>();
		for (JsonObject testObject : tests) {
			RegressionJsonObject test = new RegressionJsonObject(testObject, "test", TEST_KEYS);
			String testId = test.id("id");
			if (!testIds.add(testId)) {
				throw new IllegalArgumentException("Suite " + suiteId + " contains duplicate test id " + testId);
			}
			test = test.withDescription("test " + testId);
			Set<String> tags = test.stringSet("tags");
			int timeoutTicks = test.integer("timeoutTicks");
			boolean prepareTestCell = test.optionalBoolean("prepareTestCell", true);
			if (timeoutTicks < 1) {
				throw new IllegalArgumentException("Test " + testId + " timeoutTicks must be positive");
			}
			List<JsonObject> steps = test.objectArray("steps", "Test " + testId + " contains a non-object step");
			if (steps.isEmpty()) {
				throw new IllegalArgumentException("Test " + testId + " must define at least one step");
			}
			parsedTests.add(new RegressionTest(testId, tags, timeoutTicks, prepareTestCell, parseSteps(steps, testId, registry)));
		}
		return parsedTests;
	}

	private static List<RegressionStep> parseSteps(List<JsonObject> steps, String testId, RegressionActionRegistry registry) {
		List<RegressionStep> parsedSteps = new ArrayList<>();
		Set<String> stepIds = new LinkedHashSet<>();
		for (JsonObject stepObject : steps) {
			RegressionJsonObject step = new RegressionJsonObject(stepObject, "step", STEP_KEYS);
			String stepId = step.id("id");
			if (!stepIds.add(stepId)) {
				throw new IllegalArgumentException("Test " + testId + " contains duplicate step id " + stepId);
			}
			step = step.withDescription("step " + stepId);
			String actionId = step.string("action");
			parsedSteps.add(new RegressionStep(stepId, registry.require(actionId).bind(step.optionalObject("args"), "step " + stepId)));
		}
		return parsedSteps;
	}
}

record RegressionSuite(int schemaVersion, String suiteId, Set<String> requires, String isolation, List<RegressionTest> tests) {
}

record RegressionTest(String id, Set<String> tags, int timeoutTicks, boolean prepareTestCell, List<RegressionStep> steps) {
}

record RegressionStep(String id, RegressionActionInvocation action) {
}
