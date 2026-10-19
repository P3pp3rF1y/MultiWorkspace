package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;

import java.util.UUID;

/** Immutable identity for a mounted linked-storage fixture; it never retains a live contraption. */
record MountedLinkedStorageHandle(UUID entityId, ResourceKey<Level> dimension, BlockPos localPos, LinkedStorageEndpointData endpoint) {
	AbstractContraptionEntity requireServer(ServerPlayer player) {
		if (!player.level().dimension().equals(dimension) || !(player.level().getEntity(entityId) instanceof AbstractContraptionEntity entity)
				|| !entity.isAlive()) {
			throw new IllegalStateException("Mounted linked storage contraption is no longer present in its original dimension");
		}
		return entity;
	}
}
