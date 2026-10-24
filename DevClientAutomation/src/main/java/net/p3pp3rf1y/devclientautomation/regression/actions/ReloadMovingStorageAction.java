package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageBoat;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageMinecart;

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
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						Entity oldEntity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
						if (!(oldEntity instanceof StorageBoat || oldEntity instanceof StorageMinecart)) {
							throw new IllegalStateException("Expected one linked moving boat or minecart to reload");
						}
						IMovingStorageEntity original = (IMovingStorageEntity) oldEntity;
						var endpoint = LinkedStorageStackData.getEndpoint(original.getStorageItem());
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
						var restoredEndpoint = LinkedStorageStackData.getEndpoint(((IMovingStorageEntity) restored).getStorageItem());
						if (!endpoint.equals(restoredEndpoint) || !LinkedStorageGroupsSavedData.get(player.serverLevel()).manager()
								.isEndpointMember(endpoint.groupId(), endpoint.endpointId())) {
							throw new IllegalStateException("Serialized moving carrier changed or lost its linked identity");
						}
					});
				}
				return await(future);
			}
		};
	}
}
