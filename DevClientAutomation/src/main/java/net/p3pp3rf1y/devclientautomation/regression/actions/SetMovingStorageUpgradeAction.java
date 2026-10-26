package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetMovingStorageUpgradeAction extends TypedRegressionAction<SetMovingStorageUpgradeAction.Arguments> {
	public record Arguments(String item, String ref) {
	}

	public SetMovingStorageUpgradeAction() {
		super("moving.setUpgrade", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item", "ref"), description);
		return new Arguments(requireString(args, "item", description), RegressionValueStore.requireName(requireString(args, "ref", description)));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
							.orElseThrow(() -> new IllegalArgumentException("Unknown upgrade " + arguments.item()));
					IMovingStorageEntity moving = (IMovingStorageEntity) context.values().require(arguments.ref(), MovingStorageEntityHandle.class)
							.requireServer(player);
					moving.getStorageHolder().getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(item));
				});
			}
			return await(future);
		}
	}
}
