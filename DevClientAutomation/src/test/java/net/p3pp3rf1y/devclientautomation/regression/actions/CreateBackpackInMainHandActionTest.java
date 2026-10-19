package net.p3pp3rf1y.devclientautomation.regression.actions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateBackpackInMainHandActionTest {
	@Test
	void bindsEverySupportedTier() {
		CreateBackpackInMainHandAction action = new CreateBackpackInMainHandAction();

		for (String tier : List.of("leather", "copper", "iron", "gold", "diamond", "netherite")) {
			CreateBackpackInMainHandAction.Arguments arguments = RegressionActionTestSupport
					.arguments(RegressionActionTestSupport.bind(action, "{\"tier\": \"" + tier + "\"}"), CreateBackpackInMainHandAction.Arguments.class);
			assertEquals(tier, arguments.tier());
		}
		RegressionActionTestSupport.assertMetadata(action, "backpack.createInMainHand", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Test
	void rejectsUnsupportedTier() {
		CreateBackpackInMainHandAction action = new CreateBackpackInMainHandAction();

		assertThrows(IllegalArgumentException.class, () -> RegressionActionTestSupport.bind(action, "{\"tier\": \"obsidian\"}"));
	}
}
