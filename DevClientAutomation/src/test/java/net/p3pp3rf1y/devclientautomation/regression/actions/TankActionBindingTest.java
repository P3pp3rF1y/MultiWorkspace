package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TankActionBindingTest {
	@ParameterizedTest
	@MethodSource("validBindings")
	void bindAcceptsKnownTankActionArguments(Binding params) {
		RegressionAction action = params.action();

		BoundRegressionAction<?> bound = RegressionActionTestSupport.bind(action, params.json());

		assertNotNull(bound);
	}

	@ParameterizedTest
	@MethodSource("invalidBindings")
	void bindRejectsUnknownOrMalformedTankActionArguments(Binding params) {
		RegressionAction action = params.action();

		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, params.json()));
	}

	@Test
	void bindTankFluidPreservesRelativeTargetExpectedAmountAndSimulation() {
		var action = new TankFluidTransferAction();
		String json = "{\"offset\":[0,4,0],\"face\":\"north\",\"amount\":1000,\"expected\":0,\"simulate\":true}";

		TankFluidTransferAction.Arguments args = RegressionActionTestSupport.arguments(RegressionActionTestSupport.bind(action, json),
				TankFluidTransferAction.Arguments.class);

		assertEquals(4, args.y());
		assertEquals(0, args.expected());
		assertTrue(args.simulate());
	}

	private static Stream<Binding> validBindings() {
		return Stream.of(new Binding(new ClickTankGaugeAction(), "{}"),
				new Binding(new ClickStorageMenuSlotAction(), "{\"area\":\"extra\",\"slot\":2,\"click\":\"quickMove\"}"),
				new Binding(new AssertTankAction(),
						"{\"cursorItem\":\"minecraft:bucket\",\"containerSlot\":3,\"containerItem\":\"minecraft:experience_bottle\"}"),
				new Binding(new ClickPumpControlAction(), "{\"control\":\"input\"}"),
				new Binding(new ClickPumpControlAction(), "{\"control\":\"hand\",\"upgrade\":1}"),
				new Binding(new CaptureScreenshotAction(), "{\"name\":\"tank-menu\"}"), new Binding(new SpawnTankExperienceAction(), "{\"experience\":5}"),
				new Binding(new RemoveTargetBlockAction(), "{\"offset\":[0,1,0],\"mode\":\"explosion\"}"), new Binding(new RecoverTankDropAction(), "{}"),
				new Binding(new ReloadTankAction(), "{\"offset\":[0,2,0]}"),
				new Binding(new AssertTankAction(), "{\"present\":false,\"tankDrops\":1,\"droppedHeight\":5,\"droppedUpgrades\":2}"),
				new Binding(new AssertTankAction(), "{\"droppedFluid\":20000,\"droppedFluidType\":\"minecraft:water\"}"),
				new Binding(new UseTankSegmentAction(), "{\"offset\":[0,2,0],\"face\":\"west\"}"));
	}

	private static Stream<Binding> invalidBindings() {
		return Stream.of(new Binding(new ClickTankGaugeAction(), "{\"fill\":true}"), new Binding(new AssertTankAction(), "{\"containerSlot\":4}"),
				new Binding(new ClickPumpControlAction(), "{\"control\":\"unknown\"}"),
				new Binding(new ClickPumpControlAction(), "{\"control\":\"world\",\"upgrade\":-1}"),
				new Binding(new CaptureScreenshotAction(), "{\"name\":\"../escape\"}"), new Binding(new CaptureScreenshotAction(), "{}"),
				new Binding(new SpawnTankExperienceAction(), "{\"experience\":0}"), new Binding(new SpawnTankExperienceAction(), "{\"experience\":101}"),
				new Binding(new SpawnTankExperienceAction(), "{\"amount\":5}"), new Binding(new RemoveTargetBlockAction(), "{\"mode\":\"erase\"}"),
				new Binding(new RemoveTargetBlockAction(), "{\"offset\":[0,1]}"), new Binding(new RecoverTankDropAction(), "{\"fluid\":1000}"),
				new Binding(new ReloadTankAction(), "{\"offset\":[0,1]}"), new Binding(new TankFluidTransferAction(), "{\"amount\":0}"),
				new Binding(new TankFluidTransferAction(), "{\"amount\":1,\"expected\":-1}"),
				new Binding(new TankFluidTransferAction(), "{\"amount\":1,\"face\":\"diagonal\"}"),
				new Binding(new TankFluidTransferAction(), "{\"amount\":1,\"simulate\":\"true\"}"), new Binding(new AssertTankAction(), "{\"amount\":-1}"),
				new Binding(new AssertTankAction(), "{\"present\":\"false\"}"), new Binding(new AssertTankAction(), "{\"droppedFluidType\":1000}"),
				new Binding(new UseTankSegmentAction(), "{\"offset\":[0,1]}"), new Binding(new UseTankSegmentAction(), "{\"face\":\"diagonal\"}"),
				new Binding(new UseTankSegmentAction(), "{\"pack\":true}"));
	}

	private record Binding(RegressionAction action, String json) {
	}
}
