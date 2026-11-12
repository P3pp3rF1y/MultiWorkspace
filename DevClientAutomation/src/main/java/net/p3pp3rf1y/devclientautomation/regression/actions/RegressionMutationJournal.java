package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;

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
	private static final String EXPERIENCE_LEVEL_TAG = "experienceLevel";
	private static final String TOTAL_EXPERIENCE_TAG = "totalExperience";
	private static final String EXPERIENCE_PROGRESS_TAG = "experienceProgress";
	private static final String DIMENSION_TAG = "dimension";
	private static final String POSITION_TAG = "position";
	private static final String BLOCK_STATE_TAG = "state";
	private static final String BLOCK_ENTITY_TAG = "blockEntity";
	private static final String BLOCK_NAME_TAG = "Name";
	private static final String ENTITY_UUID_TAG = "uuid";
	private static final String GROUP_ID_TAG = "group";
	private static final String PRIMARY_ENDPOINT_ID_TAG = "primary";
	private static final String CREATED_TANK_GROUPS_TAG = "createdTankGroups";
	private final Path path;
	private final String runId;
	private final Map<String, CompoundTag> blocks = new LinkedHashMap<>();
	private final Map<String, CompoundTag> entities = new LinkedHashMap<>();
	private final Map<String, CompoundTag> gameRules = new LinkedHashMap<>();
	private final Map<UUID, CompoundTag> createdTankGroups = new LinkedHashMap<>();
	@Nullable
	private CompoundTag playerSnapshot;
	@Nullable
	private CompoundTag playerConditionSnapshot;
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
			journal.playerConditionSnapshot = checkpoint.contains("originalPlayerConditions", 10)
					? checkpoint.getCompound("originalPlayerConditions").copy()
					: null;
			journal.preparedPlayer = checkpoint.getCompound("preparedPlayer").copy();
			diagnostics.put("restartPreparationToken", checkpoint.getUUID("token").toString());
			diagnostics.put("restartPreparationBootId", checkpoint.getUUID("boot").toString());
		} else {
			if (Files.exists(checkpointPath)) {
				throw new IllegalStateException("A retained restart checkpoint must be verified before another test");
			}
			journal.playerSnapshot = snapshotPlayer(player);
			journal.playerConditionSnapshot = snapshotPlayerConditions(player);
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
		state.putInt(EXPERIENCE_LEVEL_TAG, player.experienceLevel);
		state.putInt(TOTAL_EXPERIENCE_TAG, player.totalExperience);
		state.putFloat(EXPERIENCE_PROGRESS_TAG, player.experienceProgress);
		state.put("carried", player.containerMenu.getCarried().saveOptional(player.registryAccess()));
		ListTag crafting = new ListTag();
		for (int slot = 1; slot <= 4; slot++) {
			crafting.add(player.inventoryMenu.getSlot(slot).getItem().saveOptional(player.registryAccess()));
		}
		state.put("crafting", crafting);
		return state;
	}

	private static CompoundTag snapshotPlayerConditions(ServerPlayer player) {
		CompoundTag snapshot = new CompoundTag();
		snapshot.putFloat("health", player.getHealth());
		ListTag effects = new ListTag();
		var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		for (MobEffectInstance effect : player.getActiveEffects()) {
			effects.add(MobEffectInstance.CODEC.encodeStart(ops, effect).getOrThrow());
		}
		snapshot.put("effects", effects);
		return snapshot;
	}

	private static void restorePlayerConditions(ServerPlayer player, @Nullable CompoundTag snapshot) {
		if (snapshot == null) {
			return;
		}
		player.removeAllEffects();
		var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		ListTag effects = snapshot.getList("effects", 10);
		for (int index = 0; index < effects.size(); index++) {
			player.addEffect(MobEffectInstance.CODEC.parse(ops, effects.get(index)).getOrThrow());
		}
		player.setHealth(snapshot.getFloat("health"));
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
		if (snapshot.contains(EXPERIENCE_LEVEL_TAG)) {
			player.setExperienceLevels(snapshot.getInt(EXPERIENCE_LEVEL_TAG));
			player.totalExperience = snapshot.getInt(TOTAL_EXPERIENCE_TAG);
			player.experienceProgress = snapshot.getFloat(EXPERIENCE_PROGRESS_TAG);
		}
		player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
		ListTag crafting = snapshot.getList("crafting", 10);
		for (int slot = 1; slot <= 4; slot++) {
			player.inventoryMenu.getSlot(slot).set(ItemStack.parseOptional(player.registryAccess(), crafting.getCompound(slot - 1)));
		}
		player.inventoryMenu.setCarried(ItemStack.parseOptional(player.registryAccess(), snapshot.getCompound("carried")));
		player.inventoryMenu.sendAllDataToRemote();
		CompoundTag restored = snapshotPlayer(player);
		if (!snapshot.contains(EXPERIENCE_LEVEL_TAG)) {
			restored.remove(EXPERIENCE_LEVEL_TAG);
			restored.remove(TOTAL_EXPERIENCE_TAG);
			restored.remove(EXPERIENCE_PROGRESS_TAG);
		}
		if (!restored.equals(snapshot)) {
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
				restorePlayerConditions(player, playerConditionSnapshot);
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
			checkpoint.put("originalPlayerConditions", playerConditionSnapshot);
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
		entry.putString(DIMENSION_TAG, level.dimension().location().toString());
		entry.putLong(POSITION_TAG, pos.asLong());
		entry.put(BLOCK_STATE_TAG, NbtUtils.writeBlockState(level.getBlockState(pos)));
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity != null) {
			entry.put(BLOCK_ENTITY_TAG, blockEntity.saveWithFullMetadata(level.registryAccess()));
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

	/** Records death-respawn rules before a regression changes them so every terminal cleanup path restores them. */
	public void recordDeathRespawnRules(ServerLevel level) {
		String key = level.dimension().location().toString();
		if (gameRules.containsKey(key)) {
			return;
		}
		CompoundTag entry = new CompoundTag();
		entry.putString("dimension", key);
		entry.putBoolean("keepInventory", level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY));
		entry.putBoolean("immediateRespawn", level.getGameRules().getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN));
		gameRules.put(key, entry);
		try {
			persist();
		} catch (RuntimeException e) {
			gameRules.remove(key);
			throw e;
		}
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
		entry.putString(DIMENSION_TAG, entity.level().dimension().location().toString());
		entry.putUUID(ENTITY_UUID_TAG, entity.getUUID());
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

	public void recordCreatedTankGroup(ServerLevel level, TankBlockEntity tank) {
		LinkedStorageEndpointData endpoint = tank.getLinkedStorageEndpointData();
		CompoundTag original = blocks.get(level.dimension().location() + ":" + tank.getBlockPos().asLong());
		if (endpoint == null || original == null || original.getCompound(BLOCK_ENTITY_TAG).contains("linkedStorageEndpoint")
				|| createdTankGroups.containsKey(endpoint.groupId())
				|| !LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()))
			return;
		CompoundTag group = new CompoundTag();
		group.putString(DIMENSION_TAG, level.dimension().location().toString());
		group.putUUID(GROUP_ID_TAG, endpoint.groupId());
		group.putUUID(PRIMARY_ENDPOINT_ID_TAG, endpoint.endpointId());
		createdTankGroups.put(endpoint.groupId(), group);
		persist();
	}

	private void restore(ServerPlayer player, @Nullable BlockPos support, boolean restorePlayerState, boolean clearJournal) {
		List<String> failures = new ArrayList<>();
		for (CompoundTag entry : gameRules.values()) {
			try {
				ServerLevel level = level(player, entry);
				level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(entry.getBoolean("keepInventory"), level.getServer());
				if (entry.contains("immediateRespawn", 1)) {
					level.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(entry.getBoolean("immediateRespawn"), level.getServer());
				}
			} catch (RuntimeException e) {
				failures.add("game rule " + entry.getString("dimension") + ": " + e.getMessage());
			}
		}
		restoreEntities(player, failures);
		restoreBlocks(player, failures);
		if (support != null) {
			verifyProtectedSupport(player, support, failures);
		}
		if (restorePlayerState) {
			try {
				restorePlayer(player);
				restorePlayerConditions(player, playerConditionSnapshot);
			} catch (RuntimeException e) {
				failures.add("player: " + e.getMessage());
			}
		}
		if (!failures.isEmpty()) {
			throw new IllegalStateException("Cleanup of " + runId + " failed: " + String.join("; ", failures));
		}
		for (CompoundTag group : createdTankGroups.values()) {
			LinkedStorageGroupsSavedData.get(level(player, group)).manager().discardUnboundGroup(group.getUUID(GROUP_ID_TAG),
					group.getUUID(PRIMARY_ENDPOINT_ID_TAG));
		}
		if (clearJournal) {
			clearJournal();
		}
	}

	private void restoreEntities(ServerPlayer player, List<String> failures) {
		List<CompoundTag> entityEntries = new ArrayList<>(entities.values());
		for (int i = entityEntries.size() - 1; i >= 0; i--) {
			CompoundTag entry = entityEntries.get(i);
			try {
				ServerLevel level = level(player, entry);
				UUID id = entry.getUUID(ENTITY_UUID_TAG);
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
				failures.add("entity " + entry.getUUID(ENTITY_UUID_TAG) + ": " + e.getMessage());
			}
		}
	}

	private void restoreBlocks(ServerPlayer player, List<String> failures) {
		List<CompoundTag> blockEntries = new ArrayList<>(blocks.values());
		// Restoration owns all recorded tank positions; suppress product drops and capacity preflight for this rollback only.
		for (CompoundTag entry : blockEntries) {
			ServerLevel level = level(player, entry);
			if (level.getBlockEntity(BlockPos.of(entry.getLong(POSITION_TAG))) instanceof TankBlockEntity tank) {
				recordCreatedTankGroup(level, tank);
				tank.suppressTransactionalRemoval();
			}
		}
		for (int i = blockEntries.size() - 1; i >= 0; i--) {
			CompoundTag entry = blockEntries.get(i);
			try {
				restoreBlock(player, entry);
			} catch (RuntimeException e) {
				failures.add("block " + BlockPos.of(entry.getLong(POSITION_TAG)) + ": " + e.getMessage());
			}
		}
	}

	private void restoreBlock(ServerPlayer player, CompoundTag entry) {
		ServerLevel level = level(player, entry);
		BlockPos pos = BlockPos.of(entry.getLong(POSITION_TAG));
		CompoundTag originalState = entry.getCompound(BLOCK_STATE_TAG);
		if (!originalState.contains(BLOCK_NAME_TAG, 8)
				|| !BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(originalState.getString(BLOCK_NAME_TAG)))) {
			throw new IllegalStateException("Original block is unavailable at " + pos + ": " + originalState.getString(BLOCK_NAME_TAG));
		}
		BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), originalState);
		AABB nearby = new AABB(pos).inflate(3);
		Set<UUID> previousDrops = level.getEntitiesOfClass(ItemEntity.class, nearby).stream().map(Entity::getUUID).collect(Collectors.toSet());
		level.setBlockAndUpdate(pos, state);
		// Container replacement can emit its contents; discard only drops created by this restoration.
		level.getEntitiesOfClass(ItemEntity.class, nearby).stream().filter(entity -> !previousDrops.contains(entity.getUUID())).forEach(Entity::discard);
		if (entry.contains(BLOCK_ENTITY_TAG, 10)) {
			BlockEntity blockEntity = BlockEntity.loadStatic(pos, state, entry.getCompound(BLOCK_ENTITY_TAG), level.registryAccess());
			if (blockEntity == null) {
				throw new IllegalStateException("Could not restore block entity at " + pos);
			}
			level.setBlockEntity(blockEntity);
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	private void verifyProtectedSupport(ServerPlayer player, BlockPos support, List<String> failures) {
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				BlockPos pos = support.offset(x, 0, z);
				if (!player.serverLevel().getBlockState(pos).is(Blocks.STONE)) {
					failures.add("protected support missing at " + pos);
				}
			}
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
		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(entry.getString(DIMENSION_TAG)));
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
			if (root.contains("playerConditionSnapshot", 10)) {
				journal.playerConditionSnapshot = root.getCompound("playerConditionSnapshot");
			}
			ListTag blockList = root.getList("blocks", 10);
			for (int i = 0; i < blockList.size(); i++) {
				CompoundTag entry = blockList.getCompound(i);
				journal.blocks.put(entry.getString(DIMENSION_TAG) + ":" + entry.getLong(POSITION_TAG), entry);
			}
			ListTag entityList = root.getList("entities", 10);
			for (int i = 0; i < entityList.size(); i++) {
				CompoundTag entry = entityList.getCompound(i);
				journal.entities.put(entry.getString(DIMENSION_TAG) + ":" + entry.getUUID(ENTITY_UUID_TAG), entry);
			}
			ListTag groups = root.getList(CREATED_TANK_GROUPS_TAG, 10);
			for (int i = 0; i < groups.size(); i++) {
				CompoundTag group = groups.getCompound(i);
				journal.createdTankGroups.put(group.getUUID(GROUP_ID_TAG), group);
			}
			ListTag gameRuleList = root.getList("gameRules", 10);
			for (int i = 0; i < gameRuleList.size(); i++) {
				CompoundTag entry = gameRuleList.getCompound(i);
				journal.gameRules.put(entry.getString("dimension"), entry);
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
		if (playerConditionSnapshot != null) {
			root.put("playerConditionSnapshot", playerConditionSnapshot);
		}
		ListTag blockList = new ListTag();
		blocks.values().forEach(blockList::add);
		root.put("blocks", blockList);
		ListTag entityList = new ListTag();
		entities.values().forEach(entityList::add);
		root.put("entities", entityList);
		ListTag gameRuleList = new ListTag();
		gameRules.values().forEach(gameRuleList::add);
		root.put("gameRules", gameRuleList);
		ListTag groups = new ListTag();
		createdTankGroups.values().forEach(groups::add);
		root.put(CREATED_TANK_GROUPS_TAG, groups);
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
