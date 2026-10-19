package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControllerDepositActionBindingTest {
	@Test
	void depositRequiresOnlyAnItem() {
		var action = new ConfigureBackpackDepositAction();
		assertSame(action, RegressionActionTestSupport.bind(action, "{\"item\":\"minecraft:diamond\"}").action());
		for (String invalid : new String[]{"{}", "{\"item\":1}", "{\"item\":\"minecraft:diamond\",\"extra\":true}"}) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, invalid));
		}
		RegressionActionTestSupport.assertMetadata(action, "backpack.configureDeposit", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Test
	void countAcceptsZeroAndRejectsInvalidCountsAndTargets() {
		var action = new AssertInventoryCountAction();
		assertSame(action, RegressionActionTestSupport.bind(action, "{\"target\":\"backpack\",\"item\":\"minecraft:diamond\",\"count\":0}").action());
		assertSame(action,
				RegressionActionTestSupport.bind(action, "{\"target\":\"storage\",\"offset\":[-1,0,-1],\"item\":\"minecraft:diamond\",\"count\":7}").action());
		for (String count : new String[]{"-1", "0.5", "\"0\"", "null"}) {
			assertThrows(IllegalArgumentException.class,
					() -> RegressionActionTestSupport.bind(action, "{\"target\":\"backpack\",\"item\":\"minecraft:diamond\",\"count\":" + count + "}"));
		}
		for (String invalid : new String[]{"{}", "{\"target\":\"unknown\",\"item\":\"minecraft:diamond\",\"count\":0}",
				"{\"target\":\"storage\",\"item\":\"minecraft:diamond\",\"count\":0}",
				"{\"target\":\"backpack\",\"item\":\"minecraft:diamond\",\"count\":0,\"offset\":[0,0,0]}",
				"{\"target\":\"backpack\",\"item\":\"minecraft:diamond\",\"count\":0,\"extra\":true}"}) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, invalid));
		}
	}

	@Test
	void reloadRequiresAnExactBlockOffset() {
		var action = new ReloadChunkBlockEntitiesAction();
		assertSame(action, RegressionActionTestSupport.bind(action, "{\"offset\":[-1,0,-1]}").action());
		for (String invalid : new String[]{"{}", "{\"offset\":[0]}", "{\"offset\":[0,0.5,0]}", "{\"offset\":[0,0,0],\"extra\":true}"}) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, invalid));
		}
		assertEquals(new ReloadChunkBlockEntitiesAction.Arguments(-1, 0, -1, true),
				RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, "{\"offset\":[-1,0,-1],\"assertControllerEmpty\":true}"),
						ReloadChunkBlockEntitiesAction.Arguments.class));
		for (String invalid : new String[]{"null", "\"true\"", "1"}) {
			assertThrows(IllegalArgumentException.class,
					() -> RegressionActionTestSupport.bind(action, "{\"offset\":[-1,0,-1],\"assertControllerEmpty\":" + invalid + "}"));
		}
		assertEquals("fixture.chunk.reloadBlockEntities", action.metadata().id());
		assertEquals("2", action.metadata().revision());
		assertEquals(Set.of("minecraft:client", "sophisticatedstorage"), action.metadata().dependencies());
	}
}
