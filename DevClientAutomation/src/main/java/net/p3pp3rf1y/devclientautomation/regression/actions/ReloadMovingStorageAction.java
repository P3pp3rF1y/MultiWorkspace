package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
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
					Entity oldEntity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					if (!(oldEntity instanceof StorageBoat || oldEntity instanceof StorageMinecart)) {
						throw new IllegalStateException("Expected one linked moving boat or minecart to reload");
					}
					IMovingStorageEntity original = (IMovingStorageEntity) oldEntity;
					var endpoint = original.getStorageItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
					if (endpoint == null) {
						throw new IllegalStateException("Cannot reload an unlinked moving carrier");
					}
					TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.level().registryAccess());
					oldEntity.saveWithoutId(output);
					CompoundTag saved = output.buildResult();
					Entity restored = oldEntity instanceof StorageBoat
							? new StorageBoat(player.level(), oldEntity.getX(), oldEntity.getY(), oldEntity.getZ())
							: new StorageMinecart(player.level(), oldEntity.getX(), oldEntity.getY(), oldEntity.getZ());
					restored.load(TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), saved));
					oldEntity.discard();
					if (!player.level().addFreshEntity(restored)) {
						throw new IllegalStateException("Failed to restore serialized moving storage entity");
					}
					if (!oldEntity.getUUID().equals(restored.getUUID())) {
						throw new IllegalStateException("Serialized moving entity changed UUID");
					}
					context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					var restoredEndpoint = ((IMovingStorageEntity) restored).getStorageItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
					if (!endpoint.equals(restoredEndpoint)
							|| !LinkedStorageGroupsSavedData.get(player.level()).manager().isEndpointMember(endpoint.groupId(), endpoint.endpointId())) {
						throw new IllegalStateException("Serialized moving carrier changed or lost its linked identity");
					}
				});
			}
			return await(future);
		}
	}
}
