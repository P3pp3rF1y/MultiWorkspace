package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinalAssertionActionBindingTest {
	@Test
	void doubleChestControllerAssertionBindsOptionalStrictMode() {
		AssertDoubleChestAction action = new AssertDoubleChestAction();
		assertEquals(new AssertDoubleChestAction.Arguments(false, null, null, null, false, null),
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), AssertDoubleChestAction.Arguments.class));
		assertEquals(new AssertDoubleChestAction.Arguments(true, null, null, null, false, null), RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"controller\":true}"), AssertDoubleChestAction.Arguments.class));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"controller\":1}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"count\":1}"));
		assertEquals(new AssertDoubleChestAction.Arguments(false, true, false, true, true, "sophisticatedstorage:stack_upgrade_tier_1"),
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action,
						"{\"packed\":true,\"firstWaterlogged\":false,\"otherWaterlogged\":true,\"noDrops\":true,\"upgrade\":\"sophisticatedstorage:stack_upgrade_tier_1\"}"),
						AssertDoubleChestAction.Arguments.class));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"packed\":\"true\"}"));
		assertEquals("3", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage"), action.metadata().dependencies());
	}

	@Test
	void aimedBucketUseBindsOffsetAndDrainMode() {
		var action = new UseWaterBucketAtTargetAction();
		assertEquals(new UseWaterBucketAtTargetAction.Arguments(0, 0, 0, false),
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{}"), UseWaterBucketAtTargetAction.Arguments.class));
		assertEquals(new UseWaterBucketAtTargetAction.Arguments(0, 0, 1, true), RegressionActionTestSupport
				.arguments(RegressionActionTestSupport.bind(action, "{\"offset\":[0,0,1],\"drain\":true}"), UseWaterBucketAtTargetAction.Arguments.class));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"offset\":[0,1]}"));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"drain\":1}"));
	}

	@Test
	void packedDropRequiresExpectedContents() {
		var action = new AssertPackedDoubleChestDropAction();
		var valid = "{\"item\":\"minecraft:diamond\",\"count\":7,\"upgrade\":\"sophisticatedstorage:stack_upgrade_tier_1\"}";
		assertEquals(new AssertPackedDoubleChestDropAction.Arguments("minecraft:diamond", 7, "sophisticatedstorage:stack_upgrade_tier_1"),
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, valid), AssertPackedDoubleChestDropAction.Arguments.class));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, valid.replace(":7", ":0")));
		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"count\":2}"));
	}
}
