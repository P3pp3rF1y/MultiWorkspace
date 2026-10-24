package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProbeMovingStoragePickupAction extends TypedRegressionAction<ProbeMovingStoragePickupAction.Arguments> {
	public record Arguments(String phase, String ref, String primaryRef) {
	}

	public ProbeMovingStoragePickupAction() {
		super("moving.probePickup", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("phase", "ref", "primaryRef"), description);
		String phase = requireString(args, "phase", description);
		if (!Set.of("spawn", "assertPaused", "moveToPrimary", "assertPicked").contains(phase)) {
			throw new IllegalArgumentException(description + ": unsupported pickup probe phase " + phase);
		}
		return new Arguments(phase, RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "primaryRef", description)));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE)
				.and(RegressionValueContract.consuming(arguments.primaryRef(), RegressionValueTypes.MOVING_STORAGE));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> future;
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
			int elapsed = context.minecraft().player.tickCount - startTick;
			if (arguments.phase().equals("assertPaused") && elapsed < 25 || arguments.phase().equals("assertPicked") && elapsed < 25) {
				return RegressionActionProgress.WAITING;
			}
			if (future == null) {
				future = context.onServer(player -> {
					var level = player.serverLevel();
					Entity movingEntity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					IMovingStorageEntity moving = (IMovingStorageEntity) movingEntity;
					if (arguments.phase().equals("spawn")) {
						Entity primary = context.values().require(arguments.primaryRef(), MovingStorageEntityHandle.class).requireServer(player);
						Vec3 origin = primary.position();
						Vec3 remote = origin.add(-12, 0, 0);
						BlockPos support = BlockPos.containing(remote.x, remote.y - 1, remote.z);
						boolean placedSupport = level.getBlockState(support).isAir();
						if (placedSupport) {
							context.journal().recordBlock(level, support);
							level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
						}
						context.putDiagnostic("pickupPrimaryOrigin", origin);
						context.putDiagnostic("pickupRemoteSupport", support);
						context.putDiagnostic("pickupPlacedSupport", placedSupport);
						primary.teleportTo(remote.x, remote.y, remote.z);
						primary.setDeltaMovement(Vec3.ZERO);
						ItemEntity drop = new ItemEntity(level, movingEntity.getX(), movingEntity.getY() + 0.2, movingEntity.getZ(),
								new ItemStack(Items.EMERALD));
						drop.setDeltaMovement(0, 0, 0);
						context.journal().recordCreatedEntity(drop);
						level.addFreshEntity(drop);
						context.putDiagnostic("pickupProbe", drop.getUUID().toString());
						return;
					}
					Object probe = context.getDiagnostic("pickupProbe");
					if (!(probe instanceof String probeId)) {
						throw new IllegalStateException("Pickup probe was not spawned");
					}
					Entity drop = level.getEntity(UUID.fromString(probeId));
					if (arguments.phase().equals("moveToPrimary")) {
						if (!(drop instanceof ItemEntity item) || !item.isAlive()) {
							throw new IllegalStateException("Secondary consumed pickup item before primary resumed");
						}
						item.teleportTo(movingEntity.getX(), movingEntity.getY() + 0.2, movingEntity.getZ());
						item.setDeltaMovement(0, 0, 0);
						return;
					}
					int count = moving.getStorageHolder().getStorageWrapper().getInventoryHandler().getStackInSlot(0).getCount();
					if (arguments.phase().equals("assertPaused")) {
						Entity primary = context.values().require(arguments.primaryRef(), MovingStorageEntityHandle.class).requireServer(player);
						Vec3 origin = (Vec3) context.getDiagnostic("pickupPrimaryOrigin");
						primary.teleportTo(origin.x, origin.y, origin.z);
						primary.setDeltaMovement(Vec3.ZERO);
						if (Boolean.TRUE.equals(context.getDiagnostic("pickupPlacedSupport"))) {
							level.removeBlock((BlockPos) context.getDiagnostic("pickupRemoteSupport"), false);
						}
						if (drop == null || !drop.isAlive() || count != 0) {
							throw new IllegalStateException("Secondary pickup probe failed: item=" + drop + ", alive=" + (drop != null && drop.isAlive())
									+ ", canonical slot count=" + count + ", secondary=" + movingEntity.position() + ", player=" + player.position());
						}
					}
					if (arguments.phase().equals("assertPicked") && (drop != null && drop.isAlive() || count != 1)) {
						throw new IllegalStateException("Primary did not pick up exactly one item into canonical contents");
					}
				});
			}
			return await(future);
		}
	}
}
