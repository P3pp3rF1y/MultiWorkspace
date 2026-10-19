package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.actions.PlayerStatePolicy;
import net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionInvocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RegressionSuiteParser {
	private static final Set<String> SUITE_KEYS = Set.of("schemaVersion", "suiteId", "requires", "isolation", "fixtures", "tests");
	private static final Set<String> TEST_KEYS = Set.of("id", "tags", "timeoutTicks", "prepareTestCell", "playerState", "checkpoint", "steps");
	private static final Set<String> STEP_KEYS = Set.of("id", "action", "args", "expectFailure");
	private static final Set<String> FIXTURE_REFERENCE_KEYS = Set.of("id", "fixture");
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
		return new RegressionSuite(schemaVersion, suiteId, requires, isolation, parseTests(tests, suiteId, parseFixtures(suite, registry), registry));
	}

	private static Map<String, List<JsonObject>> parseFixtures(RegressionJsonObject suite, RegressionActionRegistry registry) {
		Map<String, List<JsonObject>> fixtures = new LinkedHashMap<>();
		for (var entry : suite.optionalObject("fixtures").entrySet()) {
			String name = entry.getKey();
			if (!name.matches("[a-z0-9][a-z0-9-]{0,63}")) {
				throw new IllegalArgumentException("Invalid fixture name " + name);
			}
			if (!entry.getValue().isJsonArray() || entry.getValue().getAsJsonArray().isEmpty()) {
				throw new IllegalArgumentException("Fixture " + name + " must be a nonempty array of steps");
			}
			List<JsonObject> steps = new ArrayList<>();
			for (JsonElement element : entry.getValue().getAsJsonArray()) {
				if (!element.isJsonObject()) {
					throw new IllegalArgumentException("Fixture " + name + " contains a non-object step");
				}
				steps.add(element.getAsJsonObject());
			}
			validateFixture(name, steps, registry);
			fixtures.put(name, steps);
		}
		return fixtures;
	}

	private static void validateFixture(String fixtureName, List<JsonObject> fixtureSteps, RegressionActionRegistry registry) {
		List<RegressionStep> validatedSteps = new ArrayList<>();
		Set<String> stepIds = new LinkedHashSet<>();
		for (JsonObject fixtureStep : fixtureSteps) {
			RegressionJsonObject definition = new RegressionJsonObject(fixtureStep, "fixture " + fixtureName + " step", STEP_KEYS);
			addActionStep(validatedSteps, stepIds, "fixture " + fixtureName, definition.id("id"), definition, registry);
		}
	}

	private static List<RegressionTest> parseTests(List<JsonObject> tests, String suiteId, Map<String, List<JsonObject>> fixtures,
			RegressionActionRegistry registry) {
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
			PlayerStatePolicy playerState = testObject.has("playerState") ? PlayerStatePolicy.parse(test.string("playerState")) : PlayerStatePolicy.ISOLATED;
			String checkpoint = testObject.has("checkpoint") ? test.id("checkpoint") : "";
			if (playerState == PlayerStatePolicy.ISOLATED != checkpoint.isEmpty() || (playerState == PlayerStatePolicy.CHECKPOINT_VERIFY && prepareTestCell)) {
				throw new IllegalArgumentException("Checkpoint phases require a checkpoint id; verification must disable test-cell preparation");
			}
			if (timeoutTicks < 1) {
				throw new IllegalArgumentException("Test " + testId + " timeoutTicks must be positive");
			}
			List<JsonObject> steps = test.objectArray("steps", "Test " + testId + " contains a non-object step");
			if (steps.isEmpty()) {
				throw new IllegalArgumentException("Test " + testId + " must define at least one step");
			}
			parsedTests.add(
					new RegressionTest(testId, tags, timeoutTicks, prepareTestCell, playerState, checkpoint, parseSteps(steps, testId, fixtures, registry)));
		}
		return parsedTests;
	}

	private static List<RegressionStep> parseSteps(List<JsonObject> steps, String testId, Map<String, List<JsonObject>> fixtures,
			RegressionActionRegistry registry) {
		List<RegressionStep> parsedSteps = new ArrayList<>();
		Set<String> stepIds = new LinkedHashSet<>();
		for (JsonObject stepObject : steps) {
			boolean fixtureReference = stepObject.has("fixture");
			RegressionJsonObject step = new RegressionJsonObject(stepObject, "step", fixtureReference ? FIXTURE_REFERENCE_KEYS : STEP_KEYS);
			String stepId = step.id("id");
			if (fixtureReference) {
				String fixtureName = step.string("fixture");
				List<JsonObject> fixtureSteps = fixtures.get(fixtureName);
				if (fixtureSteps == null) {
					throw new IllegalArgumentException("Test " + testId + " references unknown fixture " + fixtureName);
				}
				for (JsonObject fixtureStep : fixtureSteps) {
					RegressionJsonObject definition = new RegressionJsonObject(fixtureStep, "fixture " + fixtureName + " step", STEP_KEYS);
					addActionStep(parsedSteps, stepIds, testId, stepId + "-" + definition.id("id"), definition, registry);
				}
			} else {
				addActionStep(parsedSteps, stepIds, testId, stepId, step, registry);
			}
		}
		return parsedSteps;
	}

	private static void addActionStep(List<RegressionStep> steps, Set<String> stepIds, String testId, String stepId, RegressionJsonObject definition,
			RegressionActionRegistry registry) {
		if (!stepIds.add(stepId)) {
			throw new IllegalArgumentException("Test " + testId + " contains duplicate step id " + stepId);
		}
		RegressionJsonObject step = definition.withDescription("step " + stepId);
		String expectedFailure = definition.has("expectFailure") ? step.string("expectFailure") : "";
		if (definition.has("expectFailure") && expectedFailure.isBlank()) {
			throw new IllegalArgumentException("Expected failure diagnostic must not be blank");
		}
		steps.add(new RegressionStep(stepId, registry.require(step.string("action")).bind(step.optionalObject("args"), "step " + stepId), expectedFailure));
	}
}
record RegressionSuite(int schemaVersion, String suiteId, Set<String> requires, String isolation, List<RegressionTest> tests) {
}
record RegressionTest(String id, Set<String> tags, int timeoutTicks, boolean prepareTestCell, PlayerStatePolicy playerState, String checkpoint,
		List<RegressionStep> steps) {
	RegressionTest(String id, Set<String> tags, int timeoutTicks, boolean prepareTestCell, List<RegressionStep> steps) {
		this(id, tags, timeoutTicks, prepareTestCell, PlayerStatePolicy.ISOLATED, "", steps);
	}
}
record RegressionStep(String id, RegressionActionInvocation action, String expectFailure) {
	RegressionStep(String id, RegressionActionInvocation action) {
		this(id, action, "");
	}
}
