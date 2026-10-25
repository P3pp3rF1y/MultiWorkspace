package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProbePackedPrimaryAction extends TypedRegressionAction<ProbePackedPrimaryAction.Arguments> {
	public record Arguments(String phase, String primaryRef, String secondaryRef) {
	}

	public ProbePackedPrimaryAction() {
		super("moving.probePackedPrimary", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("phase", "primaryRef", "secondaryRef"), description);
		String phase = requireString(args, "phase", description);
		if (!phase.equals("paused") && !phase.equals("resumed")) {
			throw new IllegalArgumentException(description + ": phase must be paused or resumed");
		}
		return new Arguments(phase, RegressionValueStore.requireName(requireString(args, "primaryRef", description)),
				RegressionValueStore.requireName(requireString(args, "secondaryRef", description)));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.primaryRef(), RegressionValueTypes.MOVING_STORAGE)
				.and(RegressionValueContract.consuming(arguments.secondaryRef(), RegressionValueTypes.MOVING_STORAGE));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> spawn;
		private CompletableFuture<Void> verify;
		private int startTick = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (startTick < 0) {
				startTick = context.minecraft().player.tickCount;
			}
			if (spawn == null) {
				spawn = context.onServer(player -> {
					var primary = context.values().require(arguments.primaryRef(), MovingStorageEntityHandle.class).requireServer(player);
					ServerLevel level = (ServerLevel) player.level();
					ItemEntity item = new ItemEntity(level, primary.getX(), primary.getY() + 0.2, primary.getZ(), new ItemStack(Items.EMERALD));
					item.setDeltaMovement(0, 0, 0);
					context.journal().recordCreatedEntity(item);
					if (!level.addFreshEntity(item)) {
						throw new IllegalStateException("Could not spawn packed-primary pickup probe");
					}
					context.putDiagnostic("packedPickupItem", item.getUUID().toString());
				});
			}
			RegressionActionProgress spawned = await(spawn);
			if (spawned != RegressionActionProgress.PASSED) {
				return spawned;
			}
			if (context.minecraft().player.tickCount - startTick < 25) {
				return RegressionActionProgress.WAITING;
			}
			if (verify == null) {
				verify = context.onServer(player -> {
					var primary = context.values().require(arguments.primaryRef(), MovingStorageEntityHandle.class).requireServer(player);
					IMovingStorageEntity secondary = (IMovingStorageEntity) context.values().require(arguments.secondaryRef(), MovingStorageEntityHandle.class)
							.requireServer(player);
					var probe = ((ServerLevel) player.level()).getEntity(UUID.fromString((String) context.getDiagnostic("packedPickupItem")));
					int count = secondary.getStorageHolder().getStorageWrapper().getInventoryHandler().getStackInSlot(0).getCount();
					if (arguments.phase().equals("paused") && (probe == null || !probe.isAlive() || count != 0)
							|| arguments.phase().equals("resumed") && (probe != null && probe.isAlive() || count != 1)) {
						throw new IllegalStateException("Packed-primary pickup authority failed: primary=" + primary.position() + ", probe=" + probe
								+ ", canonical count=" + count + ", phase=" + arguments.phase());
					}
					if (arguments.phase().equals("paused")) {
						probe.discard();
					}
				});
			}
			return await(verify);
		}
	}
}
