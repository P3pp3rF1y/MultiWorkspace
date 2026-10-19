package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.Set;

public final class UseMovingStorageAction extends TypedRegressionAction<UseMovingStorageAction.Arguments> {
	public record Arguments(String ref) {
	}

	public UseMovingStorageAction() {
		super("player.useMovingStorage", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)));
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int deadline = -1;
			private int interactionTick = -1;
			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = context.minecraft();
				if (interactionTick >= 0) {
					if (minecraft.player.tickCount - interactionTick < 4) {
						return RegressionActionProgress.WAITING;
					}
					minecraft.options.keyShift.setDown(false);
					return RegressionActionProgress.PASSED;
				}
				if (deadline < 0) {
					deadline = minecraft.player.tickCount + 100;
				}
				if (minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
					minecraft.player.closeContainer();
					return RegressionActionProgress.WAITING;
				}
				if (minecraft.player.getMainHandItem().isEmpty() && !minecraft.player.isSecondaryUseActive()) {
					minecraft.options.keyShift.setDown(true);
					return RegressionActionProgress.WAITING;
				}
				if (minecraft.gameMode != null) {
					try {
						Entity entity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).findClient(minecraft);
						if (entity != null) {
							minecraft.gameMode.interact(minecraft.player, entity, InteractionHand.MAIN_HAND);
							interactionTick = minecraft.player.tickCount;
							return RegressionActionProgress.WAITING;
						}
					} catch (RuntimeException e) {
						releaseShift(minecraft);
						return fail(e.getMessage() == null ? e.toString() : e.getMessage());
					}
				}
				if (minecraft.player.tickCount >= deadline) {
					releaseShift(minecraft);
					return fail("Moving entity " + arguments.ref() + " did not synchronize to the client");
				}
				return RegressionActionProgress.WAITING;
			}

			private void releaseShift(Minecraft minecraft) {
				minecraft.options.keyShift.setDown(false);
			}
		};
	}
}
