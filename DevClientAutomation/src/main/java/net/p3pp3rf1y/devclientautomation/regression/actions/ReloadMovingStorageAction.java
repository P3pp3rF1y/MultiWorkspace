package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageBoat;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageMinecart;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ReloadMovingStorageAction extends TypedRegressionAction<ReloadMovingStorageAction.Arguments> {
	public record Arguments(String ref) {
	}

	public ReloadMovingStorageAction() {
		super("fixture.moving.reload", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
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
		@Nullable
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					Entity oldEntity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					if (!(oldEntity instanceof StorageBoat || oldEntity instanceof StorageMinecart)) {
						throw new IllegalStateException("Expected one linked moving boat or minecart to reload");
					}
					IMovingStorageEntity original = (IMovingStorageEntity) oldEntity;
					LinkedStorageEndpointData endpoint = original.getStorageItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
					if (endpoint == null) {
						throw new IllegalStateException("Cannot reload an unlinked moving carrier");
					}
					CompoundTag saved = oldEntity.saveWithoutId(new CompoundTag());
					Entity restored = oldEntity instanceof StorageBoat
							? new StorageBoat(player.serverLevel(), oldEntity.getX(), oldEntity.getY(), oldEntity.getZ())
							: new StorageMinecart(player.serverLevel(), oldEntity.getX(), oldEntity.getY(), oldEntity.getZ());
					restored.load(saved);
					oldEntity.discard();
					if (!player.serverLevel().addFreshEntity(restored)) {
						throw new IllegalStateException("Failed to restore serialized moving storage entity");
					}
					if (!oldEntity.getUUID().equals(restored.getUUID())) {
						throw new IllegalStateException("Serialized moving entity changed UUID");
					}
					context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					LinkedStorageEndpointData restoredEndpoint = ((IMovingStorageEntity) restored).getStorageItem()
							.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
					if (!endpoint.equals(restoredEndpoint)
							|| !LinkedStorageGroupsSavedData.get(player.serverLevel()).manager().isEndpointMember(endpoint.groupId(), endpoint.endpointId())) {
						throw new IllegalStateException("Serialized moving carrier changed or lost its linked identity");
					}
				});
			}
			return await(future);
		}
	}
}
