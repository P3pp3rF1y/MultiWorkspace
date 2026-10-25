package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageBoat;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageMinecart;
import net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class DestroyMovingStorageAction extends TypedRegressionAction<DestroyMovingStorageAction.Arguments> {
	public record Arguments(String ref, boolean drops) {
	}

	public DestroyMovingStorageAction() {
		super("moving.destroy", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "drops"), description);
		if (args.has("drops") && (!args.get("drops").isJsonPrimitive() || !args.get("drops").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument drops on " + description + " must be a boolean");
		}
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)), !args.has("drops") || args.get("drops").getAsBoolean());
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
					var level = player.level();
					Entity entity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					IMovingStorageEntity moving = (IMovingStorageEntity) entity;
					var endpoint = moving.getStorageItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
					if (endpoint == null) {
						throw new IllegalStateException("Moving storage has no linked endpoint");
					}
					AABB bounds = entity.getBoundingBox().inflate(3);
					Set<UUID> before = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(Entity::getUUID).collect(Collectors.toSet());
					boolean previousDrops = level.getGameRules().get(GameRules.ENTITY_DROPS);
					try {
						level.getGameRules().set(GameRules.ENTITY_DROPS, arguments.drops(), level.getServer());
						// This invokes the production vehicle destruction and drop path.
						if (moving instanceof StorageBoat boat) {
							boat.destroy(level, level.damageSources().generic());
						} else if (moving instanceof StorageMinecart minecart) {
							minecart.destroy(level, level.damageSources().generic());
						} else if (moving instanceof AbstractChestedHorse horse) {
							horse.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
						} else {
							throw new IllegalStateException("Unsupported moving storage vehicle " + entity.getType());
						}
					} finally {
						try {
							context.journal().recordNewItemDrops(level, bounds, before);
						} finally {
							level.getGameRules().set(GameRules.ENTITY_DROPS, previousDrops, level.getServer());
						}
					}
					if (!entity.isRemoved() && !(entity instanceof AbstractChestedHorse horse && !horse.isAlive())) {
						throw new IllegalStateException("Vehicle survived destruction");
					}
					var drops = level.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2));
					drops.removeIf(item -> {
						ItemStack carrier = moving instanceof AbstractChestedHorse ? item.getItem() : MovingStorageItem.getStorageItem(item.getItem());
						return !endpoint.equals(carrier.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
					});
					var manager = LinkedStorageGroupsSavedData.get(level).manager();
					boolean member = manager.isEndpointMember(endpoint.groupId(), endpoint.endpointId());
					if (member != arguments.drops()) {
						throw new IllegalStateException("Recoverable/nonrecoverable endpoint membership mismatch");
					}
					if (!arguments.drops()) {
						if (!drops.isEmpty()) {
							throw new IllegalStateException("Nonrecoverable vehicle emitted drops");
						}
						return;
					}
					if (drops.size() != 1) {
						throw new IllegalStateException("Expected exactly one linked carrier drop, found " + drops.size());
					}
					ItemStack drop = drops.getFirst().getItem().copy();
					ItemStack carrier = moving instanceof AbstractChestedHorse ? drop : MovingStorageItem.getStorageItem(drop);
					if (!(moving instanceof AbstractChestedHorse)
							&& (drop.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) || drop.has(ModCoreDataComponents.STORAGE_UUID))
							|| carrier.has(ModCoreDataComponents.STORAGE_UUID)
							|| !endpoint.equals(carrier.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))) {
						throw new IllegalStateException("Linked identity was not preserved exclusively on the dropped carrier");
					}
					player.getInventory().setItem(1, drop);
					drops.getFirst().discard();
				});
			}
			return await(future);
		}
	}
}
