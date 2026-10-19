package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AwaitStorageBlockAction extends TypedRegressionAction<AwaitStorageBlockAction.Arguments> {
	public record Arguments(int timeoutTicks) {
	}

	public AwaitStorageBlockAction() {
		super("await.storageBlock", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("timeoutTicks"), description);
		return new Arguments(optionalPositiveInt(args, "timeoutTicks", 100, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private int deadline = -1;
		private CompletableFuture<Void> serverBlock;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + arguments.timeoutTicks();
			}
			if (minecraft.level.getBlockEntity(context.target()) instanceof StorageBlockEntity) {
				if (serverBlock == null) {
					serverBlock = context.onServer(player -> {
						if (!(player.level().getBlockEntity(context.target()) instanceof StorageBlockEntity)) {
							throw new IllegalStateException("Server did not place a storage block at the target");
						}
					});
				}
				return await(serverBlock);
			}
			return minecraft.player.tickCount >= deadline ? fail("Placed storage block did not synchronize to the client") : RegressionActionProgress.WAITING;
		}
	}
}
