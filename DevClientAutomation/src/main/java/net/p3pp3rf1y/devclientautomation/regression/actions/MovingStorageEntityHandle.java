package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import javax.annotation.Nullable;

import java.util.UUID;

/** Immutable identity for a fixture entity; never retains a live entity across game threads or actions. */
public record MovingStorageEntityHandle(UUID id, ResourceKey<Level> dimension, ResourceLocation type) {
	public static MovingStorageEntityHandle of(Entity entity) {
		if (!(entity instanceof IMovingStorageEntity)) {
			throw new IllegalArgumentException("Entity is not moving storage: " + entity.getType());
		}
		return new MovingStorageEntityHandle(entity.getUUID(), entity.level().dimension(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
	}

	public Entity requireServer(ServerPlayer player) {
		if (!player.serverLevel().dimension().equals(dimension)) {
			throw new IllegalStateException(
					"Moving entity " + id + " belongs to " + dimension.location() + ", not " + player.serverLevel().dimension().location());
		}
		Entity entity = player.serverLevel().getEntity(id);
		if (entity == null) {
			throw new IllegalStateException("Moving entity " + id + " is not loaded in " + dimension.location());
		}
		return verify(entity);
	}

	/** Returns null while the exact entity is not synchronized to the client yet. */
	@Nullable
	public Entity findClient(Minecraft minecraft) {
		if (minecraft.level == null || !minecraft.level.dimension().equals(dimension)) {
			throw new IllegalStateException("Client is not in moving entity dimension " + dimension.location());
		}
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity.getUUID().equals(id)) {
				return verify(entity);
			}
		}
		return null;
	}

	private Entity verify(Entity entity) {
		if (!(entity instanceof IMovingStorageEntity) || !BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(type)) {
			throw new IllegalStateException("Moving entity " + id + " has unexpected type " + entity.getType() + "; expected " + type);
		}
		return entity;
	}
}
