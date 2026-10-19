package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static net.p3pp3rf1y.devclientautomation.regression.actions.RegressionActionTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigureStorageContentsFilterActionTest {
	@Test
	void contentsFilterRequiresExactTypedArguments() {
		ConfigureStorageContentsFilterAction action = new ConfigureStorageContentsFilterAction();
		var bound = RegressionActionTestSupport.bind(action, "{\"offset\":[-1,0,2],\"item\":\"minecraft:diamond\",\"match\":\"mod\"}");
		assertSame(action, bound.action());
		assertEquals(new ConfigureStorageContentsFilterAction.Arguments(-1, 0, 2, "minecraft:diamond", ConfigureStorageContentsFilterAction.MatchMode.MOD),
				RegressionActionTestSupport.arguments(bound, ConfigureStorageContentsFilterAction.Arguments.class));
		assertEquals(ConfigureStorageContentsFilterAction.MatchMode.ITEM,
				RegressionActionTestSupport
						.arguments(RegressionActionTestSupport.bind(action, "{\"offset\":[0,0,0],\"item\":\"minecraft:diamond\",\"match\":\"item\"}"),
								ConfigureStorageContentsFilterAction.Arguments.class)
						.match());
		for (String invalid : new String[]{"{}", "{\"offset\":[0,0,0],\"item\":\"minecraft:diamond\"}",
				"{\"offset\":[0,0,0],\"item\":\"minecraft:diamond\",\"match\":\"tags\"}",
				"{\"offset\":[0,0.5,0],\"item\":\"minecraft:diamond\",\"match\":\"item\"}", "{\"offset\":[0,0,0],\"item\":true,\"match\":\"item\"}",
				"{\"offset\":[0,0,0],\"item\":\"minecraft:diamond\",\"match\":\"item\",\"extra\":true}"}) {
			assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, invalid));
		}
		RegressionActionTestSupport.assertMetadata(action, "fixture.storage.contentsFilter", java.util.Set.of("minecraft:client", "sophisticatedstorage"));
	}
	@Test
	void bindAcceptsExactOffsetAndModOrItemMatching() {
		var action = new ConfigureStorageContentsFilterAction();
		assertMetadata(action, "fixture.storage.contentsFilter", Set.of("minecraft:client", "sophisticatedstorage"));
		var mod = (ConfigureStorageContentsFilterAction.Arguments) bind(action, "{\"offset\":[1,0,-2],\"item\":\"minecraft:stone\",\"match\":\"mod\"}")
				.arguments();
		assertEquals(new ConfigureStorageContentsFilterAction.Arguments(1, 0, -2, "minecraft:stone", ConfigureStorageContentsFilterAction.MatchMode.MOD), mod);
		var item = (ConfigureStorageContentsFilterAction.Arguments) bind(action, "{\"offset\":[1,0,-2],\"item\":\"minecraft:stone\",\"match\":\"item\"}")
				.arguments();
		assertEquals(ConfigureStorageContentsFilterAction.MatchMode.ITEM, item.match());
	}

	@Test
	void bindRejectsInvalidModesOffsetsItemsAndExtraFields() {
		var action = new ConfigureStorageContentsFilterAction();
		for (String invalid : new String[]{"{\"offset\":[1,0,-2],\"item\":\"minecraft:stone\",\"match\":\"tag\"}",
				"{\"offset\":[1,0.5,-2],\"item\":\"minecraft:stone\",\"match\":\"mod\"}", "{\"offset\":[1,0],\"item\":\"minecraft:stone\",\"match\":\"mod\"}",
				"{\"item\":\"minecraft:stone\",\"match\":\"mod\"}", "{\"offset\":[1,0,-2],\"item\":4,\"match\":\"mod\"}",
				"{\"offset\":[1,0,-2],\"item\":\"minecraft:stone\",\"match\":\"mod\",\"void\":true}"}) {
			assertThrows(IllegalArgumentException.class, () -> bind(action, invalid));
		}
	}
}
