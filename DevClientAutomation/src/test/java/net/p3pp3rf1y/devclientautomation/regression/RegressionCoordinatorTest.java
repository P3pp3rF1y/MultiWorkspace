package net.p3pp3rf1y.devclientautomation.regression;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionCoordinatorTest {
	@Test
	void qualifyKeepsMovingTestsRunnableAndSkipsOnlyMountedCreateTestsWithoutCreateIntegration() throws IOException {
		RegressionSuite suite;
		try (var stream = getClass().getResourceAsStream("/devclientautomation/regressions/moving-linked-storage.json")) {
			assertTrue(stream != null);
			suite = RegressionSuiteParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new RegressionActionRegistry());
		}

		Set<String> capabilities = Set.of("minecraft:client", "sophisticatedstorageinmotion", "sophisticatedstorage");
		RegressionTest movingTest = test(suite, "boat-link-through-entity-interaction");
		RegressionTest mountedTest = test(suite, "mounted-open-menu-receives-live-slot-delta");

		List<RegressionResultModel.ConformanceResult> moving = RegressionCoordinator.qualify(suite, movingTest, "", capabilities::contains);
		List<RegressionResultModel.ConformanceResult> mounted = RegressionCoordinator.qualify(suite, mountedTest, "", capabilities::contains);

		assertTrue(moving.stream().allMatch(RegressionResultModel.ConformanceResult::passed));
		assertTrue(mounted.stream().anyMatch(result -> result.dependency().equals("sophisticatedstoragecreateintegration") && !result.passed()));
		assertFalse(mounted.stream().anyMatch(result -> result.dependency().equals("sophisticatedstorageinmotion") && !result.passed()));
	}

	private static RegressionTest test(RegressionSuite suite, String testId) {
		return suite.tests().stream().filter(test -> test.id().equals(testId)).findFirst().orElseThrow();
	}
}
