package net.p3pp3rf1y.devclientautomation.scenarios.core;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.bool;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.integer;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.jsonProperty;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.longValue;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJson;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class WorldAutomationEndpoints {
	private static final Logger LOGGER = LoggerFactory.getLogger("devclientautomation");
	private static final String AUTOMATION_WORLD_NAME = "Dev Client Automation Void Platform";

	private WorldAutomationEndpoints() {
	}

	public static void register(EndpointRegistry endpoints) {
		endpoints.register("/world/load", WorldAutomationEndpoints::loadWorld);
		endpoints.register("/world/wait-autosaves", WorldAutomationEndpoints::waitForAutosaves);
		endpoints.register("/world/survival", WorldAutomationEndpoints::setSurvivalMode);
		endpoints.register("/player/fill-inventory", WorldAutomationEndpoints::fillPlayerInventory);
	}

	private static void waitForAutosaves(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String worldName = string(request, "worldName", AUTOMATION_WORLD_NAME);
		int count = integer(request, "count", 2);
		long timeoutMs = longValue(request, "timeoutMs", 700_000L);
		long pollMs = longValue(request, "pollMs", 1_000L);
		sendJsonHandling(exchange, LOGGER, () -> autosaveWaitResultJson(waitForAutosaves(worldName, count, timeoutMs, pollMs)));
	}

	private static void setSurvivalMode(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(WorldAutomationEndpoints::setSurvivalMode));
	}

	private static void fillPlayerInventory(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(WorldAutomationEndpoints::fillPlayerInventory));
	}

	private static void loadWorld(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String worldName = string(request, "worldName", string(request, "buttonText", AUTOMATION_WORLD_NAME));
		boolean autoConfirmExperimental = bool(request, "autoConfirmExperimental", true);
		long timeoutMs = longValue(request, "timeoutMs", 180_000L);

		String loadResult = AutomationRuntime.runOnClient(() -> loadOrCreateAutomationWorld(worldName));
		if (!loadResult.contains("\"ok\":true")) {
			sendJson(exchange, loadResult);
			return;
		}

		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
		while (System.nanoTime() < deadline) {
			boolean loaded = AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null && Minecraft.getInstance().player != null);
			if (loaded) {
				sendJson(exchange, "{\"ok\":true,\"worldLoaded\":true,\"timedOut\":false}");
				return;
			}
			if (autoConfirmExperimental) {
				AutomationRuntime.runOnClient(WorldAutomationEndpoints::confirmExperimentalWarningIfPresent);
			}
			sleep(100);
		}
		sendJson(exchange, "{\"ok\":false,\"worldLoaded\":false,\"timedOut\":true}");
	}

	private static String loadOrCreateAutomationWorld(String worldName) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null && minecraft.player != null) {
			return "{\"ok\":false,\"error\":\"A world is already loaded; cannot verify the requested world\"}";
		}

		if (minecraft.getLevelSource().levelExists(worldName)) {
			minecraft.createWorldOpenFlows().openWorld(worldName, () -> {
			});
			return "{\"ok\":true,\"created\":false}";
		}

		LevelSettings levelSettings = new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
				WorldDataConfiguration.DEFAULT);
		WorldOptions worldOptions = new WorldOptions(0L, false, false);
		minecraft.createWorldOpenFlows().createFreshLevel(worldName, levelSettings, worldOptions, WorldAutomationEndpoints::voidFlatDimensions, null);
		return "{\"ok\":true,\"created\":true}";
	}

	private static String setSurvivalMode(ServerPlayer player) {
		player.setGameMode(GameType.SURVIVAL);
		return "{\"ok\":true,\"gameMode\":\"survival\"}";
	}

	private static String fillPlayerInventory(ServerPlayer player) {
		int filled = 0;
		for (int slot = 1; slot < player.getInventory().items.size(); slot++) {
			player.getInventory().items.set(slot, new ItemStack(Items.STONE, 64));
			filled++;
		}
		player.getInventory().setChanged();
		return "{\"ok\":true,\"filled\":" + filled + "}";
	}

	private static AutosaveWaitResult waitForAutosaves(String worldName, int count, long timeoutMs, long pollMs) {
		Path logPath = getAutosaveLogPath();
		Path levelDatPath = getWorldLevelDatPath(worldName);
		int startingLogCount = countAutosaveMessages(logPath, worldName);
		FileTime previousLevelDatModified = getLastModifiedTime(levelDatPath);
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
		int seen = 0;
		int seenLevelDatUpdates = 0;
		while (System.nanoTime() < deadline) {
			int seenLogMessages = Math.max(0, countAutosaveMessages(logPath, worldName) - startingLogCount);
			if (seenLogMessages >= count) {
				return new AutosaveWaitResult(true, false, count, seenLogMessages, "log", logPath.toString(), levelDatPath.toString());
			}

			FileTime currentLevelDatModified = getLastModifiedTime(levelDatPath);
			if (currentLevelDatModified != null && (previousLevelDatModified == null || currentLevelDatModified.compareTo(previousLevelDatModified) > 0)) {
				seenLevelDatUpdates++;
				previousLevelDatModified = currentLevelDatModified;
			}
			seen = Math.max(seenLogMessages, seenLevelDatUpdates);
			if (seen >= count) {
				return new AutosaveWaitResult(true, false, count, seen, "level.dat", logPath.toString(), levelDatPath.toString());
			}
			sleep(Math.max(100L, pollMs));
		}
		return new AutosaveWaitResult(false, true, count, seen, "timeout", logPath.toString(), levelDatPath.toString());
	}

	private static String autosaveWaitResultJson(AutosaveWaitResult result) {
		return "{\"ok\":" + result.ok() + ",\"timedOut\":" + result.timedOut() + ",\"requested\":" + result.requested() + ",\"seen\":" + result.seen() + ","
				+ jsonProperty("source", result.source()) + "," + jsonProperty("logPath", result.logPath()) + ","
				+ jsonProperty("levelDatPath", result.levelDatPath()) + "}";
	}

	private static Path getAutosaveLogPath() {
		Path logsPath = Minecraft.getInstance().gameDirectory.toPath().resolve("logs");
		Path debugLogPath = logsPath.resolve("debug.log");
		return Files.exists(debugLogPath) ? debugLogPath : logsPath.resolve("latest.log");
	}

	private static int countAutosaveMessages(Path logPath, String worldName) {
		if (!Files.exists(logPath)) {
			return 0;
		}
		String autosaveMarker = "Gathered mod list to write to world save " + worldName;
		try {
			return countOccurrences(Files.readString(logPath), autosaveMarker);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read autosave log " + logPath, e);
		}
	}

	private static Path getWorldLevelDatPath(String worldName) {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("saves").resolve(worldName).resolve("level.dat");
	}

	private static FileTime getLastModifiedTime(Path path) {
		try {
			return Files.exists(path) ? Files.getLastModifiedTime(path) : null;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read last modified time for " + path, e);
		}
	}

	private static int countOccurrences(String text, String marker) {
		int count = 0;
		int offset = 0;
		while ((offset = text.indexOf(marker, offset)) >= 0) {
			count++;
			offset += marker.length();
		}
		return count;
	}

	private static WorldDimensions voidFlatDimensions(RegistryAccess registryAccess) {
		HolderGetter<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
		HolderGetter<PlacedFeature> placedFeatures = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
		FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct(List.of())), biomes.getOrThrow(Biomes.THE_VOID),
				FlatLevelGeneratorSettings.createLakesList(placedFeatures));
		settings.setDecoration();
		settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
		settings.updateLayers();
		return WorldPresets.createNormalWorldDimensions(registryAccess).replaceOverworldGenerator(registryAccess, new FlatLevelSource(settings));
	}

	private static boolean confirmExperimentalWarningIfPresent() {
		Screen screen = Minecraft.getInstance().screen;
		if (screen == null || !screen.getClass().getSimpleName().equals("BackupConfirmScreen")) {
			return false;
		}
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractWidget widget && widget.visible && widget.active
					&& widget.getMessage().getContents() instanceof TranslatableContents translatableContents
					&& translatableContents.getKey().equals("selectWorld.backupJoinSkipButton")) {
				double x = widget.getX() + widget.getWidth() / 2.0;
				double y = widget.getY() + widget.getHeight() / 2.0;
				boolean clicked = screen.mouseClicked(x, y, 0);
				screen.mouseReleased(x, y, 0);
				return clicked;
			}
		}
		return false;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record AutosaveWaitResult(boolean ok, boolean timedOut, int requested, int seen, String source, String logPath, String levelDatPath) {
	}
}
