package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
		RegressionActionTestSupport.assertMetadata(action, "fixture.storage.contentsFilter", Set.of("minecraft:client", "sophisticatedstorage"));
	}
}
