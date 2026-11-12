package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonParser;
import net.p3pp3rf1y.devclientautomation.regression.RegressionActionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TankActionBindingTest {
	private static final RegressionActionRegistry REGISTRY = new RegressionActionRegistry();

	@ParameterizedTest
	@MethodSource("validBindings")
	void bindAcceptsKnownTankActionArguments(Binding params) {
		RegressionAction action = REGISTRY.require(params.id());
		if (!params.id().equals("menu.clickStorageSlot")) {
			assertInstanceOf(LazyOptionalRegressionAction.class, action);
		}
		assertNotNull(action.bind(JsonParser.parseString(params.json()).getAsJsonObject(), params.id()));
	}

	@ParameterizedTest
	@MethodSource("invalidBindings")
	void bindRejectsUnknownOrMalformedTankActionArguments(Binding params) {
		RegressionAction action = REGISTRY.require(params.id());
		assertThrows(IllegalArgumentException.class, () -> action.bind(JsonParser.parseString(params.json()).getAsJsonObject(), params.id()));
	}

	@Test
	void bindTankFluidPreservesRelativeTargetExpectedAmountAndSimulation() {
		String json = "{\"offset\":[0,4,0],\"face\":\"north\",\"amount\":1000,\"expected\":0,\"simulate\":true}";
		var invocation = REGISTRY.require("capability.tankFluid").bind(JsonParser.parseString(json).getAsJsonObject(), "tank fluid");
		assertEquals("capability.tankFluid", invocation.action().metadata().id());
		assertEquals("1", invocation.action().metadata().revision());
		assertTrue(invocation.dependencies().contains("sophisticatedstorage"));
	}

	private static Stream<Binding> validBindings() {
		return Stream.of(new Binding("menu.clickTankGauge", "{}"),
				new Binding("menu.clickStorageSlot", "{\"area\":\"extra\",\"slot\":2,\"click\":\"quickMove\"}"),
				new Binding("assert.tank", "{\"cursorItem\":\"minecraft:bucket\",\"containerSlot\":3,\"containerItem\":\"minecraft:experience_bottle\"}"),
				new Binding("menu.clickPumpControl", "{\"control\":\"input\"}"), new Binding("menu.clickPumpControl", "{\"control\":\"hand\",\"upgrade\":1}"),
				new Binding("diagnostic.screenshot", "{\"name\":\"tank-menu\"}"), new Binding("fixture.tank.spawnExperience", "{\"experience\":5}"),
				new Binding("world.removeTargetBlock", "{\"offset\":[0,1,0],\"mode\":\"explosion\"}"), new Binding("fixture.tank.recoverDrop", "{}"),
				new Binding("fixture.tank.reload", "{\"offset\":[0,2,0]}"), new Binding("fixture.tank.reload", "{\"offset\":[0,2,0],\"sneaking\":true}"),
				new Binding("assert.tank", "{\"present\":false,\"tankDrops\":1,\"droppedHeight\":5,\"droppedUpgrades\":2}"),
				new Binding("assert.tank", "{\"droppedFluid\":20000,\"droppedFluidType\":\"minecraft:water\"}"),
				new Binding("player.useTankSegment", "{\"offset\":[0,2,0],\"face\":\"west\"}"),
				new Binding("capability.tankFluid", "{\"offset\":[0,4,0],\"face\":\"north\",\"amount\":1000,\"expected\":0,\"simulate\":true}"));
	}

	private static Stream<Binding> invalidBindings() {
		return Stream.of(new Binding("menu.clickTankGauge", "{\"fill\":true}"), new Binding("assert.tank", "{\"containerSlot\":4}"),
				new Binding("menu.clickPumpControl", "{\"control\":\"unknown\"}"),
				new Binding("menu.clickPumpControl", "{\"control\":\"world\",\"upgrade\":-1}"),
				new Binding("diagnostic.screenshot", "{\"name\":\"../escape\"}"), new Binding("diagnostic.screenshot", "{}"),
				new Binding("fixture.tank.spawnExperience", "{\"experience\":0}"), new Binding("fixture.tank.spawnExperience", "{\"experience\":101}"),
				new Binding("fixture.tank.spawnExperience", "{\"amount\":5}"), new Binding("world.removeTargetBlock", "{\"mode\":\"erase\"}"),
				new Binding("world.removeTargetBlock", "{\"offset\":[0,1]}"), new Binding("fixture.tank.recoverDrop", "{\"fluid\":1000}"),
				new Binding("fixture.tank.reload", "{\"offset\":[0,1]}"), new Binding("capability.tankFluid", "{\"amount\":0}"),
				new Binding("capability.tankFluid", "{\"amount\":1,\"expected\":-1}"),
				new Binding("capability.tankFluid", "{\"amount\":1,\"face\":\"diagonal\"}"),
				new Binding("capability.tankFluid", "{\"amount\":1,\"simulate\":\"true\"}"),
				new Binding("capability.tankFluid", "{\"amount\":1,\"fluid\":\"not a resource id\"}"),
				new Binding("capability.tankFluid", "{\"amount\":1,\"fluid\":3}"), new Binding("assert.tank", "{\"amount\":-1}"),
				new Binding("assert.tank", "{\"present\":\"false\"}"), new Binding("assert.tank", "{\"droppedFluidType\":1000}"),
				new Binding("player.useTankSegment", "{\"offset\":[0,1]}"), new Binding("player.useTankSegment", "{\"face\":\"diagonal\"}"),
				new Binding("player.useTankSegment", "{\"pack\":true}"));
	}

	private record Binding(String id, String json) {
	}
}
