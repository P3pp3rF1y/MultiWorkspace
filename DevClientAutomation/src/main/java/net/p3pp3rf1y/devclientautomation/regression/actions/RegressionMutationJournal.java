package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/** Write-ahead record of world state owned by one test, independent of the test cell. Server thread only. */
public final class RegressionMutationJournal {
	private static final String FILE_NAME = "dev-client-regression-recovery.nbt";
	private final Path path;
	private final String runId;
	private final Map<String, CompoundTag> blocks = new LinkedHashMap<>();
	private final Map<String, CompoundTag> entities = new LinkedHashMap<>();
	@Nullable
	private CompoundTag playerSnapshot;
	@Nullable
	private CompoundTag preparedPlayer;
	private boolean committingVerification;
	private PlayerStatePolicy playerState = PlayerStatePolicy.ISOLATED;
	private String checkpointId = "";
	private Map<String, Object> diagnostics = new LinkedHashMap<>();
	private static final Map<MinecraftServer, UUID> SERVER_BOOTS = new WeakHashMap<>();

	private RegressionMutationJournal(Path path, String runId) {
		this.path = path;
		this.runId = runId;
	}

	public static RegressionMutationJournal open(ServerPlayer player, String runId) {
		return open(player, runId, PlayerStatePolicy.ISOLATED, "", new LinkedHashMap<>());
	}

	public static RegressionMutationJournal open(ServerPlayer player, String runId, PlayerStatePolicy policy, String checkpointId,
			Map<String, Object> diagnostics) {
		Path path = player.getServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
		if (Files.exists(path)) {
			RegressionMutationJournal pending = read(path);
			pending.finish(player, null, false);
		}
		if (player.containerMenu != player.inventoryMenu) {
			throw new IllegalStateException("Close the active container before starting a regression; its transient inputs are not journal-owned");
		}
		RegressionMutationJournal journal = new RegressionMutationJournal(path, runId);
		journal.playerState = policy;
		journal.checkpointId = checkpointId;
		journal.diagnostics = diagnostics;
		Path checkpointPath = journal.checkpointPath();
		UUID boot = SERVER_BOOTS.computeIfAbsent(player.getServer(), server -> UUID.randomUUID());
		diagnostics.put("serverBootId", boot.toString());
		if (policy == PlayerStatePolicy.CHECKPOINT_VERIFY) {
			CompoundTag checkpoint = readTag(checkpointPath);
			RestartProvenance.validateToken(checkpoint.getUUID("token").toString(), String.valueOf(diagnostics.getOrDefault("expectedPreparationToken", "")));
			RestartProvenance.validate(checkpoint.getString("checkpoint"), checkpointId, checkpoint.getString("world"),
					path.getParent().toAbsolutePath().toString(), checkpoint.getUUID("player"), player.getUUID(), checkpoint.getUUID("boot"), boot);
			if (!checkpoint.getCompound("preparedPlayer").equals(snapshotPlayer(player))) {
				throw new IllegalStateException("Restart checkpoint player inventory/endpoints differ from preparation");
			}
			journal.playerSnapshot = checkpoint.getCompound("originalPlayer").copy();
			journal.preparedPlayer = checkpoint.getCompound("preparedPlayer").copy();
			diagnostics.put("restartPreparationToken", checkpoint.getUUID("token").toString());
			diagnostics.put("restartPreparationBootId", checkpoint.getUUID("boot").toString());
		} else {
			if (Files.exists(checkpointPath)) {
				throw new IllegalStateException("A retained restart checkpoint must be verified before another test");
			}
			journal.playerSnapshot = snapshotPlayer(player);
		}
		journal.persist();
		if (policy != PlayerStatePolicy.CHECKPOINT_VERIFY) {
			clearPlayer(player);
		}
		return journal;
	}

	private Path checkpointPath() {
		return path.resolveSibling("dev-client-regression-checkpoint.nbt");
	}

	private static CompoundTag snapshotPlayer(ServerPlayer player) {
		CompoundTag state = new CompoundTag();
		state.putUUID("player", player.getUUID());
		state.put("inventory", player.getInventory().save(new ListTag()));
		state.putInt("selected", player.getInventory().selected);
		state.putInt("gameMode", player.gameMode.getGameModeForPlayer().getId());
		state.put("carried", player.containerMenu.getCarried().saveOptional(player.registryAccess()));
		ListTag crafting = new ListTag();
		for (int slot = 1; slot <= 4; slot++) {
			crafting.add(player.inventoryMenu.getSlot(slot).getItem().saveOptional(player.registryAccess()));
		}
		state.put("crafting", crafting);
		return state;
	}

	private static void clearPlayer(ServerPlayer player) {
		player.containerMenu.setCarried(ItemStack.EMPTY);
		for (int slot = 1; slot <= 4; slot++) {
			player.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
		}
		player.closeContainer();
		player.getInventory().clearContent();
		player.getInventory().selected = 0;
		player.connection.send(new ClientboundSetCarriedItemPacket(0));
		player.inventoryMenu.sendAllDataToRemote();
	}

	private void restorePlayer(ServerPlayer player) {
		restorePlayer(player, playerSnapshot);
	}

	private static void restorePlayer(ServerPlayer player, @Nullable CompoundTag snapshot) {
		if (snapshot == null) {
			return;
		}
		if (!snapshot.getUUID("player").equals(player.getUUID())) {
			throw new IllegalStateException("Recovery journal belongs to another player");
		}
		clearPlayer(player);
		player.getInventory().load(snapshot.getList("inventory", 10));
		player.getInventory().selected = snapshot.getInt("selected");
		player.setGameMode(GameType.byId(snapshot.getInt("gameMode")));
		player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
		ListTag crafting = snapshot.getList("crafting", 10);
		for (int slot = 1; slot <= 4; slot++) {
			player.inventoryMenu.getSlot(slot).set(ItemStack.parseOptional(player.registryAccess(), crafting.getCompound(slot - 1)));
		}
		player.inventoryMenu.setCarried(ItemStack.parseOptional(player.registryAccess(), snapshot.getCompound("carried")));
		player.inventoryMenu.sendAllDataToRemote();
		if (!snapshotPlayer(player).equals(snapshot)) {
			throw new IllegalStateException("Restored player inventory/cursor/crafting/selection differs from journal");
		}
	}

	public void finish(ServerPlayer player, @Nullable BlockPos support, boolean successful) {
		if (playerState == PlayerStatePolicy.CHECKPOINT_VERIFY) {
			if (successful && !committingVerification) {
				committingVerification = true;
				persist();
			}
			if (!committingVerification) {
				// Failed/cancelled/interrupted assertions recover to the exact prepared state for retry.
				restore(player, support, false, false);
				restorePlayer(player, preparedPlayer);
				clearJournal();
				return;
			}
		}
		boolean retain = successful && playerState == PlayerStatePolicy.CHECKPOINT_PREPARE;
		CompoundTag prepared = retain ? snapshotPlayer(player) : null;
		restore(player, support, !retain, false);
		if (retain) {
			CompoundTag checkpoint = new CompoundTag();
			checkpoint.putString("checkpoint", checkpointId);
			checkpoint.putString("world", path.getParent().toAbsolutePath().toString());
			checkpoint.putUUID("player", player.getUUID());
			checkpoint.putUUID("boot", SERVER_BOOTS.get(player.getServer()));
			checkpoint.putUUID("token", UUID.randomUUID());
			diagnostics.put("restartPreparationToken", checkpoint.getUUID("token").toString());
			diagnostics.put("restartPreparationBootId", checkpoint.getUUID("boot").toString());
			checkpoint.put("originalPlayer", playerSnapshot);
			checkpoint.put("preparedPlayer", prepared);
			writeTag(checkpointPath(), checkpoint);
		} else if (playerState != PlayerStatePolicy.ISOLATED) {
			try {
				Files.deleteIfExists(checkpointPath());
			} catch (IOException e) {
				throw new IllegalStateException("Could not consume restart checkpoint", e);
			}
		}
		clearJournal();
	}

	public void recordBlock(ServerLevel level, BlockPos pos) {
		String key = level.dimension().location() + ":" + pos.asLong();
		if (blocks.containsKey(key)) {
			return;
		}
		CompoundTag entry = new CompoundTag();
		entry.putString("dimension", level.dimension().location().toString());
		entry.putLong("position", pos.asLong());
		entry.put("state", NbtUtils.writeBlockState(level.getBlockState(pos)));
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity != null) {
			entry.put("blockEntity", blockEntity.saveWithFullMetadata(level.registryAccess()));
		}
		blocks.put(key, entry);
		try {
			persist();
		} catch (RuntimeException e) {
			blocks.remove(key);
			throw e;
		}
	}

	/** Register a UUID before spawning, so a failure after spawn is still recoverable. */
	public void recordCreatedEntity(Entity entity) {
		recordEntity(entity, null);
	}

	/** Register item entities created within a mutation's pre-snapshotted bounds. */
	public void recordNewItemDrops(ServerLevel level, AABB bounds, Set<UUID> existingItemEntityIds) {
		level.getEntitiesOfClass(ItemEntity.class, bounds, entity -> !existingItemEntityIds.contains(entity.getUUID())).forEach(this::recordCreatedEntity);
	}

	/** Record the original entity before moving, destroying, or replacing an entity not created by this test. */
	public void recordExistingEntity(Entity entity) {
		CompoundTag snapshot = new CompoundTag();
		if (!entity.save(snapshot)) {
			throw new IllegalStateException("Cannot snapshot entity " + entity.getUUID());
		}
		recordEntity(entity, snapshot);
	}

	private void recordEntity(Entity entity, @Nullable CompoundTag snapshot) {
		String key = entity.level().dimension().location() + ":" + entity.getUUID();
		if (entities.containsKey(key)) {
			return;
		}
		CompoundTag entry = new CompoundTag();
		entry.putString("dimension", entity.level().dimension().location().toString());
		entry.putUUID("uuid", entity.getUUID());
		if (snapshot != null) {
			entry.put("original", snapshot);
		}
		entities.put(key, entry);
		try {
			persist();
		} catch (RuntimeException e) {
			entities.remove(key);
			throw e;
		}
	}

	public void restore(ServerPlayer player, @Nullable BlockPos support) {
		restore(player, support, true, true);
	}

	private void restore(ServerPlayer player, @Nullable BlockPos support, boolean restorePlayerState, boolean clearJournal) {
		List<String> failures = new ArrayList<>();
		List<CompoundTag> entityEntries = new ArrayList<>(entities.values());
		for (int i = entityEntries.size() - 1; i >= 0; i--) {
			CompoundTag entry = entityEntries.get(i);
			try {
				ServerLevel level = level(player, entry);
				UUID id = entry.getUUID("uuid");
				Entity current = level.getEntity(id);
				if (current != null) {
					current.discard();
				}
				if (entry.contains("original", 10)) {
					Entity original = EntityType.loadEntityRecursive(entry.getCompound("original"), level, entity -> entity);
					if (original == null || !level.addFreshEntity(original)) {
						throw new IllegalStateException("Could not restore entity " + id);
					}
				}
			} catch (RuntimeException e) {
				failures.add("entity " + entry.getUUID("uuid") + ": " + e.getMessage());
			}
		}
		List<CompoundTag> blockEntries = new ArrayList<>(blocks.values());
		for (int i = blockEntries.size() - 1; i >= 0; i--) {
			CompoundTag entry = blockEntries.get(i);
			try {
				ServerLevel level = level(player, entry);
				BlockPos pos = BlockPos.of(entry.getLong("position"));
				CompoundTag originalState = entry.getCompound("state");
				if (!originalState.contains("Name", 8) || !BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(originalState.getString("Name")))) {
					throw new IllegalStateException("Original block is unavailable at " + pos + ": " + originalState.getString("Name"));
				}
				BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), originalState);
				AABB nearby = new AABB(pos).inflate(3);
				Set<UUID> previousDrops = level.getEntitiesOfClass(ItemEntity.class, nearby).stream().map(Entity::getUUID).collect(Collectors.toSet());
				level.setBlockAndUpdate(pos, state);
				// Container replacement can emit its contents; discard only drops created by this restoration.
				level.getEntitiesOfClass(ItemEntity.class, nearby).stream().filter(entity -> !previousDrops.contains(entity.getUUID()))
						.forEach(Entity::discard);
				if (entry.contains("blockEntity", 10)) {
					BlockEntity blockEntity = BlockEntity.loadStatic(pos, state, entry.getCompound("blockEntity"), level.registryAccess());
					if (blockEntity == null) {
						throw new IllegalStateException("Could not restore block entity at " + pos);
					}
					level.setBlockEntity(blockEntity);
					level.sendBlockUpdated(pos, state, state, 3);
				}
			} catch (RuntimeException e) {
				failures.add("block " + BlockPos.of(entry.getLong("position")) + ": " + e.getMessage());
			}
		}
		if (support != null) {
			for (int x = -4; x <= 4; x++) {
				for (int z = -4; z <= 4; z++) {
					BlockPos pos = support.offset(x, 0, z);
					if (!player.serverLevel().getBlockState(pos).is(Blocks.STONE)) {
						failures.add("protected support missing at " + pos);
					}
				}
			}
		}
		if (restorePlayerState) {
			try {
				restorePlayer(player);
			} catch (RuntimeException e) {
				failures.add("player: " + e.getMessage());
			}
		}
		if (!failures.isEmpty()) {
			throw new IllegalStateException("Cleanup of " + runId + " failed: " + String.join("; ", failures));
		}
		if (clearJournal) {
			clearJournal();
		}
	}

	private void clearJournal() {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			throw new IllegalStateException("Could not clear recovery journal " + path, e);
		}
	}

	private static ServerLevel level(ServerPlayer player, CompoundTag entry) {
		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(entry.getString("dimension")));
		ServerLevel level = player.getServer().getLevel(dimension);
		if (level == null) {
			throw new IllegalStateException("Dimension is unavailable: " + dimension.location());
		}
		return level;
	}

	private static RegressionMutationJournal read(Path path) {
		try {
			CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.create(64 * 1024 * 1024));
			RegressionMutationJournal journal = new RegressionMutationJournal(path, root.getString("runId"));
			journal.playerState = root.contains("playerState", 8) ? PlayerStatePolicy.parse(root.getString("playerState")) : PlayerStatePolicy.ISOLATED;
			journal.checkpointId = root.getString("checkpoint");
			journal.committingVerification = root.getBoolean("committingVerification");
			if (root.contains("preparedPlayer", 10)) {
				journal.preparedPlayer = root.getCompound("preparedPlayer");
			}
			if (root.contains("playerSnapshot", 10)) {
				journal.playerSnapshot = root.getCompound("playerSnapshot");
			}
			ListTag blockList = root.getList("blocks", 10);
			for (int i = 0; i < blockList.size(); i++) {
				CompoundTag entry = blockList.getCompound(i);
				journal.blocks.put(entry.getString("dimension") + ":" + entry.getLong("position"), entry);
			}
			ListTag entityList = root.getList("entities", 10);
			for (int i = 0; i < entityList.size(); i++) {
				CompoundTag entry = entityList.getCompound(i);
				journal.entities.put(entry.getString("dimension") + ":" + entry.getUUID("uuid"), entry);
			}
			return journal;
		} catch (IOException e) {
			throw new IllegalStateException("Could not read recovery journal " + path, e);
		}
	}

	private void persist() {
		CompoundTag root = new CompoundTag();
		root.putString("runId", runId);
		root.putString("playerState", playerState.serializedName());
		root.putString("checkpoint", checkpointId);
		root.putBoolean("committingVerification", committingVerification);
		if (preparedPlayer != null) {
			root.put("preparedPlayer", preparedPlayer);
		}
		if (playerSnapshot != null) {
			root.put("playerSnapshot", playerSnapshot);
		}
		ListTag blockList = new ListTag();
		blocks.values().forEach(blockList::add);
		root.put("blocks", blockList);
		ListTag entityList = new ListTag();
		entities.values().forEach(entityList::add);
		root.put("entities", entityList);
		writeTag(path, root);
	}

	private static CompoundTag readTag(Path path) {
		try {
			return NbtIo.readCompressed(path, NbtAccounter.create(64 * 1024 * 1024));
		} catch (IOException e) {
			throw new IllegalStateException("Could not read restart checkpoint " + path, e);
		}
	}

	private static void writeTag(Path path, CompoundTag root) {
		Path temp = path.resolveSibling(path.getFileName() + ".tmp");
		try {
			NbtIo.writeCompressed(root, temp);
			try {
				Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				throw new IOException("Recovery journal requires an atomic filesystem move", e);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not persist recovery journal " + path, e);
		}
	}
}
