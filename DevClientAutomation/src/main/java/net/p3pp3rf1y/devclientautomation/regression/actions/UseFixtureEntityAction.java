package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Set;
import java.util.UUID;

public final class UseFixtureEntityAction extends TypedRegressionAction<UseFixtureEntityAction.Arguments> {
	public record Arguments(String ref) {
	}
	public UseFixtureEntityAction() {
		super("player.sneakUseFixtureEntity", "1", Set.of("minecraft:client"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref"), description);
		return new Arguments(requireString(args, "ref", description));
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int deadline = -1;
			private int usedAt = -1;
			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = context.minecraft();
				if (usedAt >= 0) {
					return minecraft.player.tickCount - usedAt >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
				}
				if (deadline < 0) {
					deadline = minecraft.player.tickCount + 100;
				}
				UUID id = context.values().require(arguments.ref(), UUID.class);
				for (Entity entity : minecraft.level.entitiesForRendering()) {
					if (entity.getUUID().equals(id)) {
						if (!minecraft.player.isWithinEntityInteractionRange(entity, 0)) {
							return fail("Fixture entity is outside player interaction range");
						}
						Input previousInput = minecraft.player.input.keyPresses;
						Input sneakingInput = new Input(previousInput.forward(), previousInput.backward(), previousInput.left(), previousInput.right(),
								previousInput.jump(), true, previousInput.sprint());
						minecraft.player.input.keyPresses = sneakingInput;
						minecraft.player.connection.send(new ServerboundPlayerInputPacket(sneakingInput));
						try {
							InteractionResult result = minecraft.gameMode.interact(minecraft.player, entity, new EntityHitResult(entity),
									InteractionHand.MAIN_HAND);
							if (!result.consumesAction()) {
								minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
							}
						} finally {
							Input releasedInput = new Input(previousInput.forward(), previousInput.backward(), previousInput.left(), previousInput.right(),
									previousInput.jump(), false, previousInput.sprint());
							minecraft.player.input.keyPresses = releasedInput;
							minecraft.player.connection.send(new ServerboundPlayerInputPacket(releasedInput));
						}
						usedAt = minecraft.player.tickCount;
						return RegressionActionProgress.WAITING;
					}
				}
				return minecraft.player.tickCount >= deadline ? fail("Fixture entity did not synchronize: " + id) : RegressionActionProgress.WAITING;
			}
		};
	}
}
