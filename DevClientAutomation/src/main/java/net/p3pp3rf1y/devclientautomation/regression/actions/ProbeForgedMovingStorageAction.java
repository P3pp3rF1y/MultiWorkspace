package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.RequestLinkedStorageContentsMessage;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProbeForgedMovingStorageAction extends TypedRegressionAction<ProbeForgedMovingStorageAction.Arguments> {
	public record Arguments(String ref) {
	}

	public ProbeForgedMovingStorageAction() {
		super("assert.movingForgedRejected", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> future;
		private CompletableFuture<Void> moved;
		private CompletableFuture<Void> restored;
		private UUID nonexistentGroup;
		private UUID authenticGroup;
		private Vec3 originalPosition;
		private BlockPos remoteSupport;
		private BlockState originalRemoteSupport;
		private int requestTick = -1;
		private int inaccessibleRequestTick = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				nonexistentGroup = UUID.randomUUID();
				future = context.onServer(player -> {
					Entity entity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					IMovingStorageEntity moving = (IMovingStorageEntity) entity;
					var original = moving.getStorageItem();
					var endpoint = LinkedStorageStackData.getEndpoint(original);
					if (endpoint == null) {
						throw new IllegalStateException("Linked carrier absent");
					}
					authenticGroup = endpoint.groupId();
					originalPosition = entity.position();
					var manager = LinkedStorageGroupsSavedData.get(player.serverLevel()).manager();
					var forged = original.copy();
					LinkedStorageStackData.setEndpoint(forged, new LinkedStorageEndpointData(endpoint.groupId(), UUID.randomUUID()));
					if (StorageLinkedStorageResolver.resolveServerCanonicalHost(forged).isPresent()) {
						throw new IllegalStateException("Forged endpoint ID resolved canonical contents");
					}
					LinkedStorageStackData.setEndpoint(forged, new LinkedStorageEndpointData(nonexistentGroup, UUID.randomUUID()));
					if (StorageLinkedStorageResolver.resolveServerCanonicalHost(forged).isPresent() || manager.resolveContents(nonexistentGroup).isPresent()
							|| NBTHelper.hasTag(forged, StorageWrapper.UUID_TAG)) {
						throw new IllegalStateException("Forged group resolved or allocated ordinary storage");
					}
				});
				return RegressionActionProgress.WAITING;
			}
			if (!future.isDone()) {
				return RegressionActionProgress.WAITING;
			}
			RegressionActionProgress result = await(future);
			if (result != RegressionActionProgress.PASSED) {
				return result;
			}
			if (requestTick < 0) {
				if (ClientLinkedStorageContents.getContents(nonexistentGroup).isPresent()) {
					return fail("Forged group already has client contents");
				}
				PacketHandler.INSTANCE.sendToServer(new RequestLinkedStorageContentsMessage(nonexistentGroup, -1L));
				requestTick = context.minecraft().player.tickCount;
			}
			if (context.minecraft().player.tickCount - requestTick < 30) {
				return RegressionActionProgress.WAITING;
			}
			if (ClientLinkedStorageContents.getContents(nonexistentGroup).isPresent()) {
				return fail("Forged tooltip request disclosed a linked group snapshot");
			}
			if (moved == null) {
				moved = context.onServer(player -> {
					Entity endpointEntity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					remoteSupport = BlockPos.containing(originalPosition.x + 12, originalPosition.y - 1, originalPosition.z);
					originalRemoteSupport = player.serverLevel().getBlockState(remoteSupport);
					context.journal().recordBlock(player.serverLevel(), remoteSupport);
					player.serverLevel().setBlock(remoteSupport, Blocks.STONE.defaultBlockState(), 3);
					endpointEntity.teleportTo(originalPosition.x + 12, originalPosition.y, originalPosition.z);
					endpointEntity.setDeltaMovement(Vec3.ZERO);
				});
				return RegressionActionProgress.WAITING;
			}
			if (!moved.isDone()) {
				return RegressionActionProgress.WAITING;
			}
			result = await(moved);
			if (result != RegressionActionProgress.PASSED) {
				return result;
			}
			if (inaccessibleRequestTick < 0) {
				ClientLinkedStorageContents.clear();
				PacketHandler.INSTANCE.sendToServer(new RequestLinkedStorageContentsMessage(authenticGroup, -1L));
				inaccessibleRequestTick = context.minecraft().player.tickCount;
			}
			if (context.minecraft().player.tickCount - inaccessibleRequestTick < 30) {
				return RegressionActionProgress.WAITING;
			}
			if (restored == null) {
				restored = context.onServer(player -> {
					Entity endpointEntity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					endpointEntity.teleportTo(originalPosition.x, originalPosition.y, originalPosition.z);
					endpointEntity.setDeltaMovement(Vec3.ZERO);
					player.serverLevel().setBlock(remoteSupport, originalRemoteSupport, 3);
				});
				return RegressionActionProgress.WAITING;
			}
			result = await(restored);
			if (result != RegressionActionProgress.PASSED) {
				return result;
			}
			return ClientLinkedStorageContents.getContents(authenticGroup).isEmpty()
					? RegressionActionProgress.PASSED
					: fail("Inaccessible genuine group disclosed a linked tooltip snapshot");
		}
	}
}
