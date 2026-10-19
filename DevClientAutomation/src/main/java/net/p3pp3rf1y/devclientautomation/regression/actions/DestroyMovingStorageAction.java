package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem;

import java.util.Set;
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
					var level = player.serverLevel();
					Entity entity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					IMovingStorageEntity moving = (IMovingStorageEntity) entity;
					var endpoint = LinkedStorageStackData.getEndpoint(moving.getStorageItem());
					if (endpoint == null) {
						throw new IllegalStateException("Moving storage has no linked endpoint");
					}
					boolean previousDrops = level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS);
					AABB dropBounds = entity.getBoundingBox().inflate(3);
					var beforeDrops = level.getEntitiesOfClass(ItemEntity.class, dropBounds).stream().map(Entity::getUUID).collect(Collectors.toSet());
					try {
						level.getGameRules().getRule(GameRules.RULE_DOENTITYDROPS).set(arguments.drops(), level.getServer());
						if (moving instanceof net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageBoat boat) {
							boat.destroy(level.damageSources().generic());
						} else if (moving instanceof net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageMinecart minecart) {
							minecart.destroy(level.damageSources().generic());
						} else if (moving instanceof AbstractChestedHorse horse) {
							horse.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
						} else {
							throw new IllegalStateException("Unsupported moving storage vehicle " + entity.getType());
						}
					} finally {
						try {
							level.getEntitiesOfClass(ItemEntity.class, dropBounds, item -> !beforeDrops.contains(item.getUUID()))
									.forEach(context.journal()::recordCreatedEntity);
						} finally {
							level.getGameRules().getRule(GameRules.RULE_DOENTITYDROPS).set(previousDrops, level.getServer());
						}
					}
					if (!entity.isRemoved() && !(entity instanceof AbstractChestedHorse horse && !horse.isAlive())) {
						throw new IllegalStateException("Vehicle survived destruction");
					}
					var drops = level.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2));
					drops.removeIf(item -> {
						ItemStack carrier = moving instanceof AbstractChestedHorse ? item.getItem() : MovingStorageItem.getStorageItem(item.getItem());
						return !endpoint.equals(LinkedStorageStackData.getEndpoint(carrier));
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
					ItemStack drop = drops.get(0).getItem().copy();
					ItemStack carrier = moving instanceof AbstractChestedHorse ? drop : MovingStorageItem.getStorageItem(drop);
					if ((!(moving instanceof AbstractChestedHorse)
							&& (LinkedStorageStackData.getEndpoint(drop) != null || NBTHelper.hasTag(drop, StorageWrapper.UUID_TAG)))
							|| NBTHelper.hasTag(carrier, StorageWrapper.UUID_TAG) || !endpoint.equals(LinkedStorageStackData.getEndpoint(carrier))) {
						throw new IllegalStateException("Linked identity was not preserved exclusively on the dropped carrier");
					}
					player.getInventory().setItem(1, drop);
					drops.get(0).discard();
				});
			}
			return await(future);
		}
	}
}
