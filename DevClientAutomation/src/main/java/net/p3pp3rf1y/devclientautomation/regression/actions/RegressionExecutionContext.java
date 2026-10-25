package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorageinmotion.client.gui.MovingStorageScreen;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class RegressionExecutionContext {
	private final String runId;
	private final Map<String, Object> diagnostics;
	private final RegressionValueStore values = new RegressionValueStore();
	@Nullable
	private BlockPos support;
	@Nullable
	private RegressionMutationJournal journal;
	private PlayerStatePolicy playerState = PlayerStatePolicy.ISOLATED;
	private String checkpoint = "";
	private boolean successful;

	public void configurePlayerState(PlayerStatePolicy policy, String checkpointId) {
		playerState = policy;
		checkpoint = checkpointId;
	}

	public void markSuccessful() {
		successful = true;
	}

	public RegressionExecutionContext(String runId, Map<String, Object> diagnostics) {
		this.runId = runId;
		this.diagnostics = diagnostics;
	}

	public String runId() {
		return runId;
	}

	public RegressionValueStore values() {
		return values;
	}

	public void prepareJournal(ServerPlayer player) {
		journal = RegressionMutationJournal.open(player, runId, playerState, checkpoint, diagnostics);
	}

	public RegressionMutationJournal journal() {
		if (journal == null) {
			throw new IllegalStateException("World mutation journal has not been prepared");
		}
		return journal;
	}

	public void cleanupJournal(ServerPlayer player) {
		if (journal != null) {
			journal.finish(player, support, successful);
			journal = null;
		}
	}

	public Minecraft minecraft() {
		return Minecraft.getInstance();
	}

	public CompletableFuture<Void> onServer(Consumer<ServerPlayer> task) {
		Minecraft minecraft = minecraft();
		MinecraftServer server = minecraft.getSingleplayerServer();
		if (server == null || minecraft.player == null) {
			return CompletableFuture.failedFuture(new IllegalStateException("Integrated server or client player is unavailable"));
		}
		UUID playerId = minecraft.player.getUUID();
		CompletableFuture<Void> result = new CompletableFuture<>();
		server.execute(() -> {
			try {
				ServerPlayer player = server.getPlayerList().getPlayer(playerId);
				if (player == null) {
					throw new IllegalStateException("Server player is unavailable");
				}
				task.accept(player);
				result.complete(null);
			} catch (Throwable t) {
				result.completeExceptionally(t);
			}
		});
		return result;
	}

	public void setSupport(BlockPos support) {
		this.support = support;
		putDiagnostic("supportPosition", position(support));
		putDiagnostic("supportPositionLong", support.asLong());
		putDiagnostic("standingPosition", position(standingPosition()));
		putDiagnostic("targetPosition", position(target()));
		putDiagnostic("supportSize", "9x9");
	}

	public BlockPos support() {
		if (support == null) {
			throw new IllegalStateException("Support platform has not been prepared");
		}
		return support;
	}

	public BlockPos standingPosition() {
		return support().above();
	}

	public BlockPos target() {
		return standingPosition().east(2);
	}

	public void putDiagnostic(String name, Object value) {
		diagnostics.put(name, value);
	}

	@Nullable
	public Object getDiagnostic(String name) {
		return diagnostics.get(name);
	}

	public boolean isExpectedMenu(MenuKind kind, @Nullable String ref) {
		Minecraft minecraft = minecraft();
		if (minecraft.player == null) {
			return false;
		}
		return switch (kind) {
			case MOUNTED -> false;
			case BACKPACK_BLOCK -> minecraft.screen instanceof BackpackScreen && minecraft.player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_BACKPACK
					&& menu.getBlockPosition().filter(target()::equals).isPresent();
			case BACKPACK_ITEM -> minecraft.screen instanceof BackpackScreen && minecraft.player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK && menu.getBlockPosition().isEmpty();
			case STORAGE_BLOCK -> minecraft.screen instanceof StorageScreen && minecraft.player.containerMenu instanceof StorageContainerMenu menu
					&& menu.getBlockPosition().filter(target()::equals).isPresent();
			case MOVING ->
				minecraft.screen instanceof MovingStorageScreen && minecraft.player.containerMenu instanceof MovingStorageContainerMenu<?> menu && menu
						.getStorageEntity().filter(entity -> entity == values.require(ref, MovingStorageEntityHandle.class).findClient(minecraft)).isPresent();
		};
	}

	private static Map<String, Integer> position(BlockPos position) {
		return Map.of("x", position.getX(), "y", position.getY(), "z", position.getZ());
	}
}
