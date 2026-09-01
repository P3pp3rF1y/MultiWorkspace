package net.p3pp3rf1y.devclientautomation.bridge;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

public final class AutomationRuntime {
	private static final Duration TASK_TIMEOUT = Duration.ofSeconds(10);

	private AutomationRuntime() {
	}

	public static <T> T runOnClient(Supplier<T> supplier) {
		CompletableFuture<T> future = new CompletableFuture<>();
		Minecraft.getInstance().execute(() -> {
			try {
				future.complete(supplier.get());
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
		return await(future, "client");
	}

	public static <T> T runOnServer(Function<ServerPlayer, T> function) {
		ServerTaskContext context = runOnClient(() -> {
			MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
			if (server == null || Minecraft.getInstance().player == null) {
				throw new IllegalStateException("Singleplayer server or client player is not loaded");
			}
			return new ServerTaskContext(server, Minecraft.getInstance().player.getUUID());
		});
		CompletableFuture<T> future = new CompletableFuture<>();
		context.server().execute(() -> {
			try {
				ServerPlayer player = context.server().getPlayerList().getPlayer(context.playerUuid());
				if (player == null) {
					throw new IllegalStateException("Server player is not loaded");
				}
				future.complete(function.apply(player));
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
		return await(future, "server");
	}

	private static <T> T await(CompletableFuture<T> future, String side) {
		try {
			return future.get(TASK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for " + side + " task", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Failed to run " + side + " task: " + e.getCause(), e);
		} catch (TimeoutException e) {
			throw new IllegalStateException("Timed out waiting for " + side + " task", e);
		}
	}

	private record ServerTaskContext(MinecraftServer server, UUID playerUuid) {
	}
}
