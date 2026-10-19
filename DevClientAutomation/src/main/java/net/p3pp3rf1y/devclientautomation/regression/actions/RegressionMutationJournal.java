package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;

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

/** Write-ahead world mutation record, independent of the reusable test cell. Server thread only. */
public final class RegressionMutationJournal {
	private static final String FILE_NAME = "dev-client-regression-recovery.nbt";
	private final Path path;
	private final String runId;
	private final Map<String, CompoundTag> blocks = new LinkedHashMap<>();
	private final Map<String, CompoundTag> entities = new LinkedHashMap<>();
	private CompoundTag playerSnapshot;
	private CompoundTag preparedPlayer;
	private boolean committingVerification;
	private PlayerStatePolicy playerState = PlayerStatePolicy.ISOLATED;
	private String checkpointId = "";
	private Map<String, Object> diagnostics = new LinkedHashMap<>();
	private static final Map<net.minecraft.server.MinecraftServer, UUID> SERVER_BOOTS = new WeakHashMap<>();

	private RegressionMutationJournal(Path path, String runId) {
		this.path = path;
		this.runId = runId;
	}

	public static RegressionMutationJournal open(ServerPlayer player, String runId) {
		return open(player, runId, PlayerStatePolicy.ISOLATED, "", new LinkedHashMap<>());
	}

	public static RegressionMutationJournal open(ServerPlayer player, String runId, PlayerStatePolicy policy, String checkpointId,
			Map<String, Object> diagnostics) {
		if (player.containerMenu != player.inventoryMenu) {
			throw new IllegalStateException("Close the active container before starting a regression; its transient inputs are not journal-owned");
		}
		Path path = player.level().getServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
		if (Files.exists(path)) {
			read(path).finish(player, null, false);
		}
		RegressionMutationJournal journal = new RegressionMutationJournal(path, runId);
		journal.playerState = policy;
		journal.checkpointId = checkpointId;
		journal.diagnostics = diagnostics;
		Path checkpointPath = journal.checkpointPath();
		UUID boot = SERVER_BOOTS.computeIfAbsent(player.level().getServer(), server -> UUID.randomUUID());
		diagnostics.put("serverBootId", boot.toString());
		if (policy == PlayerStatePolicy.CHECKPOINT_VERIFY) {
			CompoundTag checkpoint = readTag(checkpointPath);
			String token = checkpoint.getStringOr("token", "");
			RestartProvenance.validateToken(token, String.valueOf(diagnostics.getOrDefault("expectedPreparationToken", "")));
			RestartProvenance.validate(checkpoint.getStringOr("checkpoint", ""), checkpointId, checkpoint.getStringOr("world", ""),
					path.getParent().toAbsolutePath().toString(), UUID.fromString(checkpoint.getStringOr("player", "")), player.getUUID(),
					UUID.fromString(checkpoint.getStringOr("boot", "")), boot);
			if (!checkpoint.getCompoundOrEmpty("preparedPlayer").equals(snapshotPlayer(player))) {
				throw new IllegalStateException("Restart checkpoint player inventory/endpoints differ from preparation");
			}
			journal.playerSnapshot = checkpoint.getCompoundOrEmpty("originalPlayer").copy();
			journal.preparedPlayer = checkpoint.getCompoundOrEmpty("preparedPlayer").copy();
			diagnostics.put("restartPreparationToken", token);
			diagnostics.put("restartPreparationBootId", checkpoint.getStringOr("boot", ""));
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
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
		output.putString("player", player.getUUID().toString());
		output.putString("gameMode", player.gameMode.getGameModeForPlayer().getName());
		player.getInventory().save(output.list("inventory", ItemStackWithSlot.CODEC));
		var equipment = output.list("equipment", ItemStack.OPTIONAL_CODEC);
		for (int slot = 36; slot <= 42; slot++) {
			equipment.add(player.getInventory().getItem(slot));
		}
		output.putInt("selected", player.getInventory().getSelectedSlot());
		output.store("carried", ItemStack.OPTIONAL_CODEC, player.containerMenu.getCarried());
		var crafting = output.list("crafting", ItemStack.OPTIONAL_CODEC);
		for (int slot = 1; slot <= 4; slot++) {
			crafting.add(player.inventoryMenu.getSlot(slot).getItem());
		}
		return output.buildResult();
	}

	private static void clearPlayer(ServerPlayer player) {
		player.containerMenu.setCarried(ItemStack.EMPTY);
		for (int slot = 1; slot <= 4; slot++) {
			player.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
		}
		player.closeContainer();
		player.getInventory().clearContent();
		player.getInventory().setSelectedSlot(0);
		player.connection.send(new ClientboundSetHeldSlotPacket(0));
		player.inventoryMenu.broadcastFullState();
	}

	private void restorePlayer(ServerPlayer player, CompoundTag snapshot) {
		if (snapshot == null) {
			return;
		}
		if (!snapshot.getStringOr("player", "").equals(player.getUUID().toString())) {
			throw new IllegalStateException("Recovery journal belongs to another player");
		}
		ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), snapshot);
		clearPlayer(player);
		player.setGameMode(GameType.byName(snapshot.getStringOr("gameMode", "survival"), GameType.SURVIVAL));
		player.getInventory().load(input.listOrEmpty("inventory", ItemStackWithSlot.CODEC));
		var equipment = input.listOrEmpty("equipment", ItemStack.OPTIONAL_CODEC).iterator();
		for (int slot = 36; slot <= 42; slot++) {
			if (!equipment.hasNext()) {
				throw new IllegalStateException("Recovery journal equipment slots are incomplete");
			}
			player.getInventory().setItem(slot, equipment.next());
		}
		player.getInventory().setSelectedSlot(input.getIntOr("selected", 0));
		player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
		var crafting = input.listOrEmpty("crafting", ItemStack.OPTIONAL_CODEC).iterator();
		for (int slot = 1; slot <= 4; slot++) {
			if (!crafting.hasNext()) {
				throw new IllegalStateException("Recovery journal crafting slots are incomplete");
			}
			player.inventoryMenu.getSlot(slot).set(crafting.next());
		}
		player.inventoryMenu.setCarried(input.read("carried", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
		player.inventoryMenu.broadcastFullState();
		if (!snapshotPlayer(player).equals(snapshot)) {
			throw new IllegalStateException("Restored player game mode/inventory/cursor/crafting/selection differs from journal");
		}
	}

	public void finish(ServerPlayer player, BlockPos support, boolean successful) {
		if (playerState == PlayerStatePolicy.CHECKPOINT_VERIFY) {
			if (successful && !committingVerification) {
				committingVerification = true;
				persist();
			}
			if (!committingVerification) {
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
			checkpoint.putString("player", player.getUUID().toString());
			UUID boot = SERVER_BOOTS.get(player.level().getServer());
			checkpoint.putString("boot", boot.toString());
			String token = UUID.randomUUID().toString();
			checkpoint.putString("token", token);
			diagnostics.put("restartPreparationToken", token);
			diagnostics.put("restartPreparationBootId", boot.toString());
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
		String key = level.dimension().identifier() + ":" + pos.asLong();
		if (blocks.containsKey(key)) {
			return;
		}
		CompoundTag entry = new CompoundTag();
		entry.putString("dimension", level.dimension().identifier().toString());
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

	public void recordCreatedEntity(Entity entity) {
		recordEntity(entity, null);
	}

	/** Own only item entities produced during a native block break, not pre-existing nearby drops. */
	public void recordNewItemDrops(ServerLevel level, AABB bounds, Set<UUID> before) {
		level.getEntitiesOfClass(ItemEntity.class, bounds, entity -> !before.contains(entity.getUUID())).forEach(this::recordCreatedEntity);
	}

	public void recordExistingEntity(Entity entity) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.level().registryAccess());
		if (!entity.save(output)) {
			throw new IllegalStateException("Cannot snapshot entity " + entity.getUUID());
		}
		recordEntity(entity, output.buildResult());
	}

	private void recordEntity(Entity entity, CompoundTag snapshot) {
		String key = entity.level().dimension().identifier() + ":" + entity.getUUID();
		if (entities.containsKey(key)) {
			return;
		}
		CompoundTag entry = new CompoundTag();
		entry.putString("dimension", entity.level().dimension().identifier().toString());
		entry.putString("uuid", entity.getUUID().toString());
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

	public void restore(ServerPlayer player, BlockPos support) {
		restore(player, support, true, true);
	}

	private void restore(ServerPlayer player, BlockPos support, boolean restorePlayerState, boolean clearJournal) {
		List<String> failures = new ArrayList<>();
		List<CompoundTag> entityEntries = new ArrayList<>(entities.values());
		for (int i = entityEntries.size() - 1; i >= 0; i--) {
			CompoundTag entry = entityEntries.get(i);
			try {
				ServerLevel level = level(player, entry);
				UUID id = UUID.fromString(entry.getStringOr("uuid", ""));
				Entity current = level.getEntity(id);
				if (current != null) {
					current.discard();
				}
				if (entry.contains("original")) {
					Entity original = EntityType.loadEntityRecursive(
							TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entry.getCompoundOrEmpty("original")), level,
							EntitySpawnReason.LOAD, entity -> entity);
					if (original == null || !level.addFreshEntity(original)) {
						throw new IllegalStateException("Could not restore entity " + id);
					}
				}
			} catch (RuntimeException e) {
				failures.add("entity " + entry.getStringOr("uuid", "") + ": " + e.getMessage());
			}
		}
		List<CompoundTag> blockEntries = new ArrayList<>(blocks.values());
		for (int i = blockEntries.size() - 1; i >= 0; i--) {
			CompoundTag entry = blockEntries.get(i);
			try {
				ServerLevel level = level(player, entry);
				BlockPos pos = BlockPos.of(entry.getLongOr("position", 0));
				CompoundTag originalState = entry.getCompoundOrEmpty("state");
				String blockId = originalState.getStringOr("Name", "");
				if (blockId.isBlank() || !BuiltInRegistries.BLOCK.containsKey(Identifier.parse(blockId))) {
					throw new IllegalStateException("Original block is unavailable at " + pos + ": " + blockId);
				}
				BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), originalState);
				AABB nearby = new AABB(pos).inflate(3);
				Set<UUID> previousDrops = level.getEntitiesOfClass(ItemEntity.class, nearby).stream().map(Entity::getUUID).collect(Collectors.toSet());
				level.setBlockAndUpdate(pos, state);
				level.getEntitiesOfClass(ItemEntity.class, nearby).stream().filter(entity -> !previousDrops.contains(entity.getUUID()))
						.forEach(Entity::discard);
				if (entry.contains("blockEntity")) {
					BlockEntity blockEntity = BlockEntity.loadStatic(pos, state, entry.getCompoundOrEmpty("blockEntity"), level.registryAccess());
					if (blockEntity == null) {
						throw new IllegalStateException("Could not restore block entity at " + pos);
					}
					level.setBlockEntity(blockEntity);
					level.sendBlockUpdated(pos, state, state, 3);
				}
			} catch (RuntimeException e) {
				failures.add("block " + BlockPos.of(entry.getLongOr("position", 0)) + ": " + e.getMessage());
			}
		}
		if (support != null) {
			for (int x = -4; x <= 4; x++) {
				for (int z = -4; z <= 4; z++) {
					BlockPos pos = support.offset(x, 0, z);
					if (!player.level().getBlockState(pos).is(Blocks.STONE)) {
						failures.add("protected support missing at " + pos);
					}
				}
			}
		}
		if (restorePlayerState) {
			try {
				restorePlayer(player, playerSnapshot);
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
		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(entry.getStringOr("dimension", "")));
		ServerLevel level = player.level().getServer().getLevel(dimension);
		if (level == null) {
			throw new IllegalStateException("Dimension is unavailable: " + dimension.identifier());
		}
		return level;
	}

	private static RegressionMutationJournal read(Path path) {
		try {
			CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.create(64 * 1024 * 1024));
			RegressionMutationJournal journal = new RegressionMutationJournal(path, root.getStringOr("runId", ""));
			journal.playerState = root.contains("playerState") ? PlayerStatePolicy.parse(root.getStringOr("playerState", "")) : PlayerStatePolicy.ISOLATED;
			journal.checkpointId = root.getStringOr("checkpoint", "");
			journal.committingVerification = root.getBooleanOr("committingVerification", false);
			if (root.contains("preparedPlayer")) {
				journal.preparedPlayer = root.getCompoundOrEmpty("preparedPlayer");
			}
			if (root.contains("playerSnapshot")) {
				journal.playerSnapshot = root.getCompoundOrEmpty("playerSnapshot");
			}
			ListTag blockList = root.getListOrEmpty("blocks");
			for (int i = 0; i < blockList.size(); i++) {
				CompoundTag entry = blockList.getCompoundOrEmpty(i);
				journal.blocks.put(entry.getStringOr("dimension", "") + ":" + entry.getLongOr("position", 0), entry);
			}
			ListTag entityList = root.getListOrEmpty("entities");
			for (int i = 0; i < entityList.size(); i++) {
				CompoundTag entry = entityList.getCompoundOrEmpty(i);
				journal.entities.put(entry.getStringOr("dimension", "") + ":" + entry.getStringOr("uuid", ""), entry);
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
