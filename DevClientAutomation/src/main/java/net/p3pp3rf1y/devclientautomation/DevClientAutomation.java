package net.p3pp3rf1y.devclientautomation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.p3pp3rf1y.devclientautomation.recipeviewer.RecipeViewerAutomationManager;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitResult;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitter;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod(value = DevClientAutomation.MOD_ID, dist = Dist.CLIENT)
public class DevClientAutomation {
    public static final String MOD_ID = "devclientautomation";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Duration CLIENT_TASK_TIMEOUT = Duration.ofSeconds(10);
    private static final String AUTOMATION_WORLD_NAME = "Dev Client Automation Void Platform";
    private static AutomationServer server;

    public DevClientAutomation(IEventBus modBus) {
        modBus.addListener(DevClientAutomation::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (server == null) {
                server = new AutomationServer();
                server.start();
            }
        });
    }

    private static class AutomationServer {
        private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "Dev Client Automation");
            thread.setDaemon(true);
            return thread;
        });
        private HttpServer httpServer;

        void start() {
            try {
                httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
                httpServer.createContext("/state", this::state);
                httpServer.createContext("/screen", this::screen);
                httpServer.createContext("/click-widget", this::clickWidget);
                httpServer.createContext("/key", this::key);
                httpServer.createContext("/mouse/move", this::moveMouse);
                httpServer.createContext("/window/maximize", this::maximizeWindow);
                httpServer.createContext("/wait", this::waitFor);
                httpServer.createContext("/client/stop", this::stopClient);
                httpServer.createContext("/world/load", this::loadWorld);
                httpServer.createContext("/screenshot", this::screenshot);
                httpServer.createContext("/recipe-viewer/state", this::recipeViewerState);
                httpServer.createContext("/recipe-viewer/search", this::recipeViewerSearch);
                httpServer.createContext("/recipe-viewer/open", this::recipeViewerOpen);
                httpServer.createContext("/recipe-viewer/query", this::recipeViewerQuery);
                httpServer.createContext("/backpack/column-upgrade-regressions", this::backpackColumnUpgradeRegressions);
                httpServer.setExecutor(executor);
                httpServer.start();
                writeDiscoveryFile(httpServer.getAddress().getPort());
                LOGGER.info("Dev client automation bridge started on 127.0.0.1:{}", httpServer.getAddress().getPort());
            } catch (IOException e) {
                LOGGER.error("Failed to start dev client automation bridge", e);
            }
        }

        private void state(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "GET");
            sendJson(exchange, runOnClient(this::buildStateJson));
        }

        private void screen(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "GET");
            sendJson(exchange, runOnClient(this::buildScreenJson));
        }

        private void clickWidget(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            String body = readBody(exchange);
            String text = extractString(body, "text").orElse("");
            boolean contains = extractBoolean(body, "contains").orElse(false);
            int button = extractInt(body, "button").orElse(0);
            sendJson(exchange, runOnClient(() -> clickWidget(text, contains, button)));
        }

        private void key(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            String body = readBody(exchange);
            String key = extractString(body, "key").orElse("");
            sendJson(exchange, runOnClient(() -> pressKey(key)));
        }

        private void moveMouse(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            String body = readBody(exchange);
            String position = extractString(body, "position").orElse("");
            int x = extractInt(body, "x").orElse(-1);
            int y = extractInt(body, "y").orElse(-1);
            sendJson(exchange, runOnClient(() -> moveMouse(position, x, y)));
        }

        private void maximizeWindow(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            sendJson(exchange, runOnClient(this::maximizeWindow));
        }

        private void waitFor(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            String body = readBody(exchange);
            String condition = extractString(body, "condition").orElse("");
            String screen = extractString(body, "screen").orElse("");
            long timeoutMs = extractLong(body, "timeoutMs").orElse(60_000L);

            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            BooleanSupplier conditionChecker = () -> runOnClient(() -> matchesCondition(condition, screen));
            while (System.nanoTime() < deadline) {
                if (conditionChecker.getAsBoolean()) {
                    sendJson(exchange, "{\"ok\":true,\"timedOut\":false}");
                    return;
                }
                sleep(100);
            }
            sendJson(exchange, "{\"ok\":false,\"timedOut\":true}");
        }

        private void stopClient(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            sendJson(exchange, "{\"ok\":true,\"stopping\":true}");
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
        }

        private void screenshot(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "GET");
            CompletableFuture<byte[]> screenshot = runOnClient(() -> {
                CompletableFuture<byte[]> future = new CompletableFuture<>();
                Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), image -> {
                    try (NativeImage nativeImage = image) {
                        Path screenshotPath = Files.createTempFile("dev-client-automation-screenshot", ".png");
                        try {
                            nativeImage.writeToFile(screenshotPath);
                            future.complete(Files.readAllBytes(screenshotPath));
                        } finally {
                            Files.deleteIfExists(screenshotPath);
                        }
                    } catch (IOException e) {
                        future.completeExceptionally(new IllegalStateException("Failed to capture screenshot", e));
                    }
                });
                return future;
            });
            byte[] bytes;
            try {
                bytes = screenshot.get(CLIENT_TASK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for screenshot", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Failed to capture screenshot: " + e.getCause(), e);
            } catch (TimeoutException e) {
                throw new IllegalStateException("Timed out while waiting for screenshot", e);
            }
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }

        private void loadWorld(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            String body = readBody(exchange);
            String worldName = extractString(body, "worldName").or(() -> extractString(body, "buttonText")).orElse(AUTOMATION_WORLD_NAME);
            boolean autoConfirmExperimental = extractBoolean(body, "autoConfirmExperimental").orElse(true);
            long timeoutMs = extractLong(body, "timeoutMs").orElse(180_000L);

            String loadResult = runOnClient(() -> loadOrCreateAutomationWorld(worldName));
            if (!loadResult.contains("\"ok\":true")) {
                sendJson(exchange, loadResult);
                return;
            }

            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            while (System.nanoTime() < deadline) {
                boolean loaded = runOnClient(() -> Minecraft.getInstance().level != null && Minecraft.getInstance().player != null);
                if (loaded) {
                    sendJson(exchange, "{\"ok\":true,\"worldLoaded\":true,\"timedOut\":false}");
                    return;
                }
                if (autoConfirmExperimental) {
                    runOnClient(this::confirmExperimentalWarningIfPresent);
                }
                sleep(100);
            }
            sendJson(exchange, "{\"ok\":false,\"worldLoaded\":false,\"timedOut\":true}");
        }

        private String loadOrCreateAutomationWorld(String worldName) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.player != null) {
                return "{\"ok\":true,\"alreadyLoaded\":true}";
            }

            if (minecraft.getLevelSource().levelExists(worldName)) {
                minecraft.createWorldOpenFlows().openWorld(worldName, () -> {});
                return "{\"ok\":true,\"created\":false}";
            }

            LevelSettings levelSettings = new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT);
            WorldOptions worldOptions = new WorldOptions(0L, false, false);
            minecraft.createWorldOpenFlows().createFreshLevel(worldName, levelSettings, worldOptions, AutomationServer::voidFlatDimensions, null);
            return "{\"ok\":true,\"created\":true}";
        }

        private static WorldDimensions voidFlatDimensions(HolderLookup.Provider registryAccess) {
            HolderGetter<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
            HolderGetter<PlacedFeature> placedFeatures = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
            FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct(List.of())), biomes.getOrThrow(Biomes.THE_VOID), FlatLevelGeneratorSettings.createLakesList(placedFeatures));
            settings.setDecoration();
            settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
            settings.updateLayers();
            return WorldPresets.createNormalWorldDimensions(registryAccess).replaceOverworldGenerator(registryAccess, new FlatLevelSource(settings));
        }

        private void recipeViewerState(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "GET");
            sendJson(exchange, runOnClient(RecipeViewerAutomationManager::stateJson));
        }

        private void recipeViewerSearch(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            String body = readBody(exchange);
            String query = extractString(body, "query").orElse("");
            sendJson(exchange, runOnClient(() -> RecipeViewerAutomationManager.searchJson(query)));
        }

        private void recipeViewerOpen(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            String body = readBody(exchange);
            sendJson(exchange, runOnClient(() -> RecipeViewerAutomationManager.openJson(body)));
        }

        private void recipeViewerQuery(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            String body = readBody(exchange);
            sendJson(exchange, runOnClient(() -> RecipeViewerAutomationManager.queryJson(body)));
        }

        private void backpackColumnUpgradeRegressions(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            sendJsonHandling(exchange, () -> runOnServer(this::runBackpackColumnUpgradeRegressions));
        }

        private String runBackpackColumnUpgradeRegressions(ServerPlayer player) {
            ColumnUpgradeRegressionSuite suite = loadColumnUpgradeRegressionSuite();

            List<ColumnUpgradeRegressionResult> results = new ArrayList<>();
            for (ColumnUpgradeRegressionScenario scenario : suite.scenarios()) {
                results.add(runColumnUpgradeRegressionScenario(scenario, suite.stackGenerator()));
            }

            long failed = results.stream().filter(result -> !result.passed()).count();
            StringBuilder json = new StringBuilder("{\"ok\":").append(failed == 0).append(",\"total\":").append(results.size()).append(",\"failed\":")
                    .append(failed).append(",\"results\":[");
            for (int i = 0; i < results.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                ColumnUpgradeRegressionResult result = results.get(i);
                json.append('{').append(jsonProperty("name", result.name())).append(",\"passed\":").append(result.passed()).append(",\"expectedFits\":")
                        .append(result.expectedFits()).append(",\"actualFits\":").append(result.actualFits()).append(",\"beforeStacks\":")
                        .append(result.beforeStacks()).append(",\"afterStacks\":").append(result.afterStacks()).append(',').append(jsonProperty("error", result.error()))
                        .append('}');
            }
            json.append("]}");

            player.getInventory().setChanged();
            return json.toString();
        }

        private ColumnUpgradeRegressionSuite loadColumnUpgradeRegressionSuite() {
            try (InputStream inputStream = DevClientAutomation.class.getResourceAsStream("/devclientautomation/backpack_column_upgrade_regressions.json")) {
                if (inputStream == null) {
                    throw new IllegalStateException("Missing backpack column upgrade regression definitions");
                }
                JsonObject root = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
                ColumnUpgradeStackGenerator stackGenerator = getStackGenerator(root.getAsJsonObject("stackGenerator"));
                JsonArray scenarioElements = root.getAsJsonArray("scenarios");
                List<ColumnUpgradeRegressionScenario> scenarios = new ArrayList<>();
                for (JsonElement scenarioElement : scenarioElements) {
                    JsonObject scenario = scenarioElement.getAsJsonObject();
                    String name = scenario.get("name").getAsString();
                    int inventorySlots = scenario.get("inventorySlots").getAsInt();
                    ResourceLocation upgradeName = ResourceLocation.parse(scenario.get("upgrade").getAsString());
                    Item upgradeItem = BuiltInRegistries.ITEM.getOptional(upgradeName).orElseThrow(() -> new IllegalArgumentException("Unknown upgrade " + upgradeName));
                    int[] occupiedSlots = getOccupiedSlots(scenario);
                    boolean expectedFits = scenario.get("expectedFits").getAsBoolean();
                    int[] noSortSlots = getIntArray(scenario, "noSortSlots");
                    int[] memorySlots = getIntArray(scenario, "memorySlots");
                    int[] stableSlots = getIntArray(scenario, "stableSlots");
					CapturedMobSpec[] capturedMobs = getCapturedMobs(scenario);
					int[] expectedCapturedMobSlots = getIntArray(scenario, "expectedCapturedMobSlots");
					String operation = scenario.has("operation") ? scenario.get("operation").getAsString() : "insert";
					scenarios.add(new ColumnUpgradeRegressionScenario(name, inventorySlots, upgradeItem, occupiedSlots, noSortSlots, memorySlots, stableSlots,
							capturedMobs, expectedCapturedMobSlots, operation, expectedFits));
				}
                return new ColumnUpgradeRegressionSuite(stackGenerator, scenarios);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read backpack column upgrade regression definitions", e);
            }
        }

        private ColumnUpgradeStackGenerator getStackGenerator(JsonObject stackGenerator) {
            JsonArray itemElements = stackGenerator.getAsJsonArray("items");
            List<Item> items = new ArrayList<>();
            for (JsonElement itemElement : itemElements) {
                ResourceLocation itemName = ResourceLocation.parse(itemElement.getAsString());
                items.add(BuiltInRegistries.ITEM.getOptional(itemName).orElseThrow(() -> new IllegalArgumentException("Unknown stack item " + itemName)));
            }
            JsonObject countSequence = stackGenerator.getAsJsonObject("countSequence");
            return new ColumnUpgradeStackGenerator(items, countSequence.get("start").getAsInt(), countSequence.get("max").getAsInt());
        }

        private int[] getOccupiedSlots(JsonObject scenario) {
            if (scenario.has("occupiedSlots")) {
                JsonArray occupiedSlots = scenario.getAsJsonArray("occupiedSlots");
                int[] slots = new int[occupiedSlots.size()];
                for (int i = 0; i < occupiedSlots.size(); i++) {
                    slots[i] = occupiedSlots.get(i).getAsInt();
                }
                return slots;
            }
            if (scenario.has("firstSlots")) {
                return firstSlots(scenario.get("firstSlots").getAsInt());
            }
            if (scenario.has("occupiedColumns")) {
                JsonObject occupiedColumns = scenario.getAsJsonObject("occupiedColumns");
                return occupiedColumns(occupiedColumns.get("rows").getAsInt(), occupiedColumns.get("columns").getAsInt(),
                        occupiedColumns.get("occupiedColumns").getAsInt());
            }
            throw new IllegalArgumentException("Scenario " + scenario.get("name").getAsString() + " does not define occupied slots");
        }

		private int[] getIntArray(JsonObject json, String key) {
            if (!json.has(key)) {
                return new int[0];
            }
            JsonArray elements = json.getAsJsonArray(key);
            int[] values = new int[elements.size()];
            for (int i = 0; i < elements.size(); i++) {
                values[i] = elements.get(i).getAsInt();
            }
			return values;
		}

		private CapturedMobSpec[] getCapturedMobs(JsonObject scenario) {
			if (!scenario.has("capturedMobs")) {
				return new CapturedMobSpec[0];
			}
			JsonArray elements = scenario.getAsJsonArray("capturedMobs");
			CapturedMobSpec[] capturedMobs = new CapturedMobSpec[elements.size()];
			for (int i = 0; i < elements.size(); i++) {
				JsonObject capturedMob = elements.get(i).getAsJsonObject();
				String entityType = capturedMob.has("entityType") ? capturedMob.get("entityType").getAsString() : "minecraft:pig";
				capturedMobs[i] = new CapturedMobSpec(capturedMob.get("slot").getAsInt(), capturedMob.get("width").getAsInt(),
						capturedMob.get("height").getAsInt(), entityType);
			}
			return capturedMobs;
		}

        private ColumnUpgradeRegressionResult runColumnUpgradeRegressionScenario(ColumnUpgradeRegressionScenario scenario, ColumnUpgradeStackGenerator stackGenerator) {
			ItemStack backpack = createBackpackStack(scenario.inventorySlots());
			IBackpackWrapper wrapper = BackpackWrapper.fromStackNoCache(backpack);
			if (scenario.operation().equals("remove")) {
				wrapper.setColumnsTaken(getUpgradeColumnsTaken(scenario.upgradeItem()), false);
			}

			InventoryHandler inventory = wrapper.getInventoryHandler();
			fillRegressionStacks(inventory, scenario.occupiedSlots(), stackGenerator);
			applyProtectedSlots(wrapper, scenario.noSortSlots(), scenario.memorySlots());
			addCapturedMobs(wrapper, scenario.capturedMobs());
			inventory.saveInventory();

			Map<String, Integer> beforeStacks = snapshotStacks(inventory);
			Map<String, String> beforeCapturedMobs = snapshotCapturedMobs(wrapper);
			Map<Integer, String> beforeProtectedStacks = snapshotProtectedStacks(inventory, scenario.protectedSlots());
			Map<Integer, String> beforeStableStacks = snapshotProtectedStacks(inventory, scenario.stableSlots());
			Map<String, String> beforeProtectedSettings = snapshotProtectedSettings(wrapper, scenario);
			ColumnUpgradeSimulationResult simulationResult = simulateColumnUpgradeOperation(backpack, wrapper, scenario.upgradeItem(), scenario.operation());
			wrapper = BackpackWrapper.fromStackNoCache(backpack);
			if (simulationResult.fits() != scenario.expectedFits()) {
                return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(),
                        snapshotStacks(wrapper.getInventoryHandler()).size(), "fit result mismatch");
            }

			Map<String, Integer> afterStacks = snapshotStacks(wrapper.getInventoryHandler());
			Map<String, String> afterCapturedMobs = snapshotCapturedMobs(wrapper);
			Map<Integer, String> afterProtectedStacks = snapshotProtectedStacks(wrapper.getInventoryHandler(), scenario.protectedSlots());
			Map<Integer, String> afterStableStacks = snapshotProtectedStacks(wrapper.getInventoryHandler(), scenario.stableSlots());
			Map<String, String> afterProtectedSettings = snapshotProtectedSettings(wrapper, scenario);
			Optional<String> capturedMobLayoutError = capturedMobLayoutError(wrapper);
			if (capturedMobLayoutError.isPresent()) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(), afterStacks.size(),
						capturedMobLayoutError.get() + " actual=" + afterCapturedMobs);
			}
            if (!beforeProtectedStacks.equals(afterProtectedStacks)) {
                return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(), afterStacks.size(),
                        "protected slot stack changed");
            }
            if (!beforeProtectedSettings.equals(afterProtectedSettings)) {
                return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(), afterStacks.size(),
                        "protected slot settings changed");
            }
			if (!beforeStableStacks.equals(afterStableStacks)) {
                return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(), afterStacks.size(),
                        "stable slot stack changed");
			}
			if (!capturedMobSlotsMatch(wrapper, scenario.expectedCapturedMobSlots())) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(), afterStacks.size(),
						"captured mob slots mismatch expected=" + Arrays.toString(scenario.expectedCapturedMobSlots()) + " actual=" + afterCapturedMobs);
			}
			if (scenario.expectedFits()) {
				if (!beforeStacks.equals(afterStacks)) {
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(), "stack snapshot changed");
				}
				if (scenario.expectedCapturedMobSlots().length == 0 && !beforeCapturedMobs.equals(afterCapturedMobs)) {
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(), "captured mob snapshot changed");
				}
			} else if (!beforeStacks.equals(afterStacks)) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, false, false, beforeStacks.size(), afterStacks.size(), "blocked insertion mutated stacks");
			} else if (!beforeCapturedMobs.equals(afterCapturedMobs)) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, false, false, beforeStacks.size(), afterStacks.size(), "blocked insertion mutated captured mobs");
			}

            return new ColumnUpgradeRegressionResult(scenario.name(), true, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(), afterStacks.size(), null);
        }

		private void applyProtectedSlots(IBackpackWrapper wrapper, int[] noSortSlots, int[] memorySlots) {
            NoSortSettingsCategory noSortSettings = wrapper.getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class);
            for (int slot : noSortSlots) {
                noSortSettings.selectSlot(slot);
            }

            MemorySettingsCategory memorySettings = wrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
            for (int slot : memorySlots) {
                memorySettings.selectSlot(slot);
            }
		}

		private void addCapturedMobs(IBackpackWrapper wrapper, CapturedMobSpec[] capturedMobs) {
			if (capturedMobs.length == 0) {
				return;
			}
			wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
			wrapper.getUpgradeHandler().saveInventory();
			for (int i = 0; i < capturedMobs.length; i++) {
				CapturedMobSpec capturedMob = capturedMobs[i];
				MobCatcherStorage.addCapturedMob(wrapper, new CapturedMob(new UUID(0, i + 1), ResourceLocation.parse(capturedMob.entityType()), new CompoundTag(),
						capturedMob.slot(), capturedMob.width(), capturedMob.height(), capturedMob.width() * capturedMob.height(), false, capturedMob.entityType(), 10, 10));
			}
		}

		private ColumnUpgradeSimulationResult simulateColumnUpgradeOperation(ItemStack backpack, IBackpackWrapper wrapper, Item upgradeItem, String operation) {
            int currentColumnsTaken = wrapper.getColumnsTaken();
            int columnsTaken = getUpgradeColumnsTaken(upgradeItem);
            int targetColumnsTaken = switch (operation) {
                case "insert" -> currentColumnsTaken + columnsTaken;
                case "remove" -> currentColumnsTaken - columnsTaken;
                default -> throw new IllegalArgumentException("Unknown column upgrade regression operation " + operation);
            };
            int rows = wrapper.getNumberOfSlotRows();
            int baseSlots = wrapper.getInventoryHandler().size() + currentColumnsTaken * rows;
            int baseColumns = baseSlots <= 81 ? 9 : 12;
            int currentColumns = baseColumns - currentColumnsTaken;
            int targetColumns = baseColumns - targetColumnsTaken;
            int targetSlots = baseSlots - targetColumnsTaken * rows;

            InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(wrapper.getInventoryLayoutParts(currentColumns, targetColumns), targetSlots, targetColumns,
                    targetColumnsTaken < currentColumnsTaken);
            if (!fitResult.fits()) {
                return new ColumnUpgradeSimulationResult(false);
            }

			if (targetColumnsTaken > currentColumnsTaken) {
				wrapper.applyInventoryLayout(fitResult, targetColumns);
			}
			wrapper.setColumnsTaken(targetColumnsTaken, false);
			wrapper.onContentsUpdated();
			wrapper.applyInventoryLayout(fitResult, targetColumns);
			wrapper.onContentsUpdated();
			wrapper.getUpgradeHandler().setStackInSlot(0, operation.equals("insert") ? new ItemStack(upgradeItem) : ItemStack.EMPTY);
			wrapper.getUpgradeHandler().saveInventory();
			wrapper.getInventoryHandler().saveInventory();
			BackpackWrapper.fromStackNoCache(backpack).getInventoryHandler().saveInventory();

            return new ColumnUpgradeSimulationResult(true);
        }

        private int getUpgradeColumnsTaken(Item upgradeItem) {
            if (!(upgradeItem instanceof IUpgradeItem<?> upgrade)) {
                throw new IllegalArgumentException("Item is not an upgrade: " + BuiltInRegistries.ITEM.getKey(upgradeItem));
            }
            return upgrade.getInventoryColumnsTaken();
        }

        private void fillRegressionStacks(InventoryHandler inventory, int[] slots, ColumnUpgradeStackGenerator stackGenerator) {
            for (int i = 0; i < slots.length; i++) {
                inventory.setStackInSlot(slots[i], new ItemStack(stackGenerator.items().get(i % stackGenerator.items().size()), stackGenerator.getCount(i)));
            }
        }

		private Map<String, Integer> snapshotStacks(InventoryHandler inventory) {
            Map<String, Integer> stacks = new HashMap<>();
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                String stackKey = BuiltInRegistries.ITEM.getKey(stack.getItem()) + ":" + stack.getCount();
                stacks.merge(stackKey, 1, Integer::sum);
            }
			return stacks;
		}

		private Map<String, String> snapshotCapturedMobs(IBackpackWrapper wrapper) {
			Map<String, String> capturedMobs = new HashMap<>();
			for (CapturedMob capturedMob : MobCatcherStorage.getCapturedMobs(wrapper)) {
				capturedMobs.put(capturedMob.id().toString(), capturedMob.slot() + ":" + capturedMob.width() + "x" + capturedMob.height());
			}
			return capturedMobs;
		}

		private boolean capturedMobSlotsMatch(IBackpackWrapper wrapper, int[] expectedSlots) {
			if (expectedSlots.length == 0) {
				return true;
			}
			List<CapturedMob> capturedMobs = MobCatcherStorage.getCapturedMobs(wrapper);
			if (capturedMobs.size() != expectedSlots.length) {
				return false;
			}
			for (int i = 0; i < expectedSlots.length; i++) {
				if (capturedMobs.get(i).slot() != expectedSlots[i]) {
					return false;
				}
			}
			return true;
		}

		private Optional<String> capturedMobLayoutError(IBackpackWrapper wrapper) {
			int columns = MobCatcherStorage.getColumns(wrapper);
			int inventorySlots = wrapper.getInventoryHandler().size();
			Set<Integer> capturedMobOccupiedSlots = new HashSet<>();
			for (CapturedMob capturedMob : MobCatcherStorage.getCapturedMobs(wrapper)) {
				if (capturedMob.slot() % columns + capturedMob.width() > columns) {
					return Optional.of("captured mob crosses row");
				}
				for (int y = 0; y < capturedMob.height(); y++) {
					for (int x = 0; x < capturedMob.width(); x++) {
						int slot = capturedMob.slot() + y * columns + x;
						if (slot < 0 || slot >= inventorySlots) {
							return Optional.of("captured mob slot out of bounds");
						}
						if (!capturedMobOccupiedSlots.add(slot)) {
							return Optional.of("captured mobs overlap");
						}
						if (!wrapper.getInventoryHandler().getStackInSlot(slot).isEmpty()) {
							return Optional.of("captured mob overlaps stack at slot " + slot);
						}
					}
				}
			}
			return Optional.empty();
		}

        private Map<Integer, String> snapshotProtectedStacks(InventoryHandler inventory, int[] slots) {
            Map<Integer, String> stacks = new HashMap<>();
            for (int slot : slots) {
                if (slot >= inventory.size()) {
                    continue;
                }
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    stacks.put(slot, BuiltInRegistries.ITEM.getKey(stack.getItem()) + ":" + stack.getCount());
                }
            }
            return stacks;
        }

        private Map<String, String> snapshotProtectedSettings(IBackpackWrapper wrapper, ColumnUpgradeRegressionScenario scenario) {
            Map<String, String> settings = new HashMap<>();
            NoSortSettingsCategory noSortSettings = wrapper.getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class);
            for (int slot : scenario.noSortSlots()) {
                settings.put("noSort:" + slot, String.valueOf(noSortSettings.getNoSortSlots().contains(slot)));
            }
            MemorySettingsCategory memorySettings = wrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
            for (int slot : scenario.memorySlots()) {
                settings.put("memory:" + slot, String.valueOf(memorySettings.isSlotSelected(slot)));
            }
            return settings;
        }

        private ItemStack createBackpackStack(int inventorySlots) {
            ItemStack backpack = new ItemStack(ModItems.BACKPACK.get());
            backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, inventorySlots);
            backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);
            return backpack;
        }

        private static int[] firstSlots(int count) {
            int[] slots = new int[count];
            for (int slot = 0; slot < count; slot++) {
                slots[slot] = slot;
            }
            return slots;
        }

        private static int[] occupiedColumns(int rows, int columns, int occupiedColumns) {
            int[] slots = new int[rows * occupiedColumns];
            int index = 0;
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < occupiedColumns; column++) {
                    slots[index++] = row * columns + column;
                }
            }
            return slots;
        }

        private String buildStateJson() {
            Minecraft minecraft = Minecraft.getInstance();
            Screen screen = minecraft.screen;
            return "{"
                    + jsonProperty("screenClass", screen == null ? null : screen.getClass().getName()) + ","
                    + jsonProperty("screenSimpleName", screen == null ? null : screen.getClass().getSimpleName()) + ","
                    + jsonProperty("screenTitle", screen == null ? null : screen.getTitle().getString()) + ","
                    + "\"inWorld\":" + (minecraft.level != null) + ","
                    + "\"playerLoaded\":" + (minecraft.player != null) + ","
                    + "\"windowWidth\":" + minecraft.getWindow().getWidth() + ","
                    + "\"windowHeight\":" + minecraft.getWindow().getHeight() + ","
                    + "\"guiWidth\":" + minecraft.getWindow().getGuiScaledWidth() + ","
                    + "\"guiHeight\":" + minecraft.getWindow().getGuiScaledHeight()
                    + "}";
        }

        private String buildScreenJson() {
            Minecraft minecraft = Minecraft.getInstance();
            Screen screen = minecraft.screen;
            StringBuilder json = new StringBuilder("{");
            json.append(jsonProperty("screenClass", screen == null ? null : screen.getClass().getName())).append(',');
            json.append(jsonProperty("screenSimpleName", screen == null ? null : screen.getClass().getSimpleName())).append(',');
            json.append(jsonProperty("title", screen == null ? null : screen.getTitle().getString())).append(',');
            json.append("\"widgets\":[");
            if (screen != null) {
                boolean first = true;
                int index = 0;
                for (GuiEventListener child : screen.children()) {
                    if (child instanceof AbstractWidget widget) {
                        if (!first) {
                            json.append(',');
                        }
                        first = false;
                        json.append('{')
                                .append("\"index\":").append(index).append(',')
                                .append(jsonProperty("type", widget.getClass().getName())).append(',')
                                .append(jsonProperty("message", widget.getMessage().getString())).append(',')
                                .append("\"x\":").append(widget.getX()).append(',')
                                .append("\"y\":").append(widget.getY()).append(',')
                                .append("\"width\":").append(widget.getWidth()).append(',')
                                .append("\"height\":").append(widget.getHeight()).append(',')
                                .append("\"active\":").append(widget.active).append(',')
                                .append("\"visible\":").append(widget.visible)
                                .append('}');
                    }
                    index++;
                }
            }
            json.append("]}");
            return json.toString();
        }

        private String clickWidget(String text, boolean contains, int button) {
            Minecraft minecraft = Minecraft.getInstance();
            Screen screen = minecraft.screen;
            if (screen == null) {
                return "{\"ok\":false,\"error\":\"No screen is open\"}";
            }
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget widget && widget.visible && widget.active && textMatches(widget.getMessage().getString(), text, contains)) {
                    double x = widget.getX() + widget.getWidth() / 2.0;
                    double y = widget.getY() + widget.getHeight() / 2.0;
					MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
					boolean clicked = screen.mouseClicked(event, false);
					screen.mouseReleased(event);
                    return "{\"ok\":" + clicked + ",\"x\":" + x + ",\"y\":" + y + "}";
                }
            }
            return "{\"ok\":false,\"error\":\"Widget not found\"}";
        }

        private boolean confirmExperimentalWarningIfPresent() {
            Screen screen = Minecraft.getInstance().screen;
            if (screen == null || !screen.getClass().getSimpleName().equals("BackupConfirmScreen")) {
                return false;
            }
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget widget && widget.visible && widget.active && widget.getMessage().getString().equals("I know what I'm doing!")) {
                    double x = widget.getX() + widget.getWidth() / 2.0;
                    double y = widget.getY() + widget.getHeight() / 2.0;
					MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
					boolean clicked = screen.mouseClicked(event, false);
					screen.mouseReleased(event);
                    return clicked;
                }
            }
            return false;
        }

        private String pressKey(String keyName) {
            Minecraft minecraft = Minecraft.getInstance();
            int keyCode = keyCode(keyName);
            if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
                return "{\"ok\":false,\"error\":\"Unknown key\"}";
            }
            if (keyCode == GLFW.GLFW_KEY_E && minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new InventoryScreen(minecraft.player));
                return "{\"ok\":true,\"handled\":true}";
            }
			if (minecraft.screen != null) {
				boolean handled = minecraft.screen.keyPressed(new KeyEvent(keyCode, 0, 0));
				if (!handled && keyCode == GLFW.GLFW_KEY_ESCAPE) {
					minecraft.screen.onClose();
					handled = true;
				}
				return "{\"ok\":true,\"handled\":" + handled + "}";
			}
            return "{\"ok\":true,\"handled\":false}";
        }

        private String moveMouse(String position, int x, int y) {
            Minecraft minecraft = Minecraft.getInstance();
            int targetX = x;
            int targetY = y;
            if (!position.isEmpty()) {
                int margin = 4;
                switch (position.toLowerCase(Locale.ROOT)) {
                    case "top-left" -> {
                        targetX = margin;
                        targetY = margin;
                    }
                    case "top-right" -> {
                        targetX = minecraft.getWindow().getGuiScaledWidth() - margin;
                        targetY = margin;
                    }
                    case "bottom-left" -> {
                        targetX = margin;
                        targetY = minecraft.getWindow().getGuiScaledHeight() - margin;
                    }
                    case "bottom-right" -> {
                        targetX = minecraft.getWindow().getGuiScaledWidth() - margin;
                        targetY = minecraft.getWindow().getGuiScaledHeight() - margin;
                    }
                    default -> {
                        return "{\"ok\":false,\"error\":\"Unknown mouse position\"}";
                    }
                }
            }
            if (targetX < 0 || targetY < 0) {
                return "{\"ok\":false,\"error\":\"Missing mouse coordinates\"}";
            }
            double scale = minecraft.getWindow().getGuiScale();
			GLFW.glfwSetCursorPos(minecraft.getWindow().handle(), targetX * scale, targetY * scale);
            return "{\"ok\":true,\"x\":" + targetX + ",\"y\":" + targetY + "}";
        }

        private String maximizeWindow() {
            Minecraft minecraft = Minecraft.getInstance();
			GLFW.glfwMaximizeWindow(minecraft.getWindow().handle());
            return "{\"ok\":true}";
        }

        private boolean matchesCondition(String condition, String screenName) {
            Minecraft minecraft = Minecraft.getInstance();
            if ("worldLoaded".equals(condition)) {
                return minecraft.level != null && minecraft.player != null;
            }
            if ("screen".equals(condition)) {
                Screen screen = minecraft.screen;
                return screen != null && (screen.getClass().getName().equals(screenName) || screen.getClass().getSimpleName().equals(screenName));
            }
            if ("noScreen".equals(condition)) {
                return minecraft.screen == null;
            }
            return false;
        }

        private void writeDiscoveryFile(int port) {
            Minecraft minecraft = Minecraft.getInstance();
            Path discoveryFile = minecraft.gameDirectory.toPath().resolve("dev-client-automation.json");
            String json = "{\"host\":\"127.0.0.1\",\"port\":" + port + "}";
            try {
                Files.writeString(discoveryFile, json, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.warn("Failed to write dev client automation discovery file {}", discoveryFile, e);
            }
        }

        private static boolean textMatches(String actual, String expected, boolean contains) {
            if (contains) {
                return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
            }
            return actual.equals(expected);
        }

        private static int keyCode(String keyName) {
            return switch (keyName.toUpperCase(Locale.ROOT)) {
                case "ESC", "ESCAPE" -> GLFW.GLFW_KEY_ESCAPE;
                case "E" -> GLFW.GLFW_KEY_E;
                case "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER;
                case "TAB" -> GLFW.GLFW_KEY_TAB;
                case "SPACE" -> GLFW.GLFW_KEY_SPACE;
                case "UP" -> GLFW.GLFW_KEY_UP;
                case "DOWN" -> GLFW.GLFW_KEY_DOWN;
                case "LEFT" -> GLFW.GLFW_KEY_LEFT;
                case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
                default -> keyName.length() == 1 ? Character.toUpperCase(keyName.charAt(0)) : GLFW.GLFW_KEY_UNKNOWN;
            };
        }

        private static String readBody(HttpExchange exchange) throws IOException {
            return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        }

        private static void requireMethod(HttpExchange exchange, String method) throws IOException {
            if (!method.equals(exchange.getRequestMethod())) {
                byte[] response = ("{\"error\":\"Method not allowed\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(405, response.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(response);
                }
                throw new IllegalStateException("Method not allowed");
            }
        }

        private static void sendJson(HttpExchange exchange, String json) throws IOException {
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        }

        private static void sendJsonHandling(HttpExchange exchange, Supplier<String> jsonSupplier) throws IOException {
            try {
                sendJson(exchange, jsonSupplier.get());
            } catch (RuntimeException e) {
                LOGGER.error("Automation endpoint failed", e);
                sendJson(exchange, "{\"ok\":false," + jsonProperty("error", e.getMessage()) + "}");
            }
        }

        private static <T> T runOnClient(Supplier<T> supplier) {
            CompletableFuture<T> future = new CompletableFuture<>();
            Minecraft.getInstance().execute(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            try {
                return future.get(CLIENT_TASK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for client task", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Failed to run client task: " + e.getCause(), e);
            } catch (TimeoutException e) {
                throw new IllegalStateException("Failed to run client task", e);
            }
        }

        private static <T> T runOnServer(Function<ServerPlayer, T> function) {
            ServerTaskContext context = runOnClient(() -> {
                MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server == null) {
                    throw new IllegalStateException("Singleplayer server is not loaded");
                }
                if (Minecraft.getInstance().player == null) {
                    throw new IllegalStateException("Client player is not loaded");
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
            try {
                return future.get(CLIENT_TASK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for server task", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Failed to run server task: " + e.getCause(), e);
            } catch (TimeoutException e) {
                throw new IllegalStateException("Failed to run server task", e);
            }
        }

        private record ColumnUpgradeRegressionSuite(ColumnUpgradeStackGenerator stackGenerator, List<ColumnUpgradeRegressionScenario> scenarios) {
        }

        private record ColumnUpgradeStackGenerator(List<Item> items, int countStart, int countMax) {
            private int getCount(int stackIndex) {
                return countStart + stackIndex % (countMax - countStart + 1);
            }
        }

		private record ColumnUpgradeRegressionScenario(String name, int inventorySlots, Item upgradeItem, int[] occupiedSlots, int[] noSortSlots, int[] memorySlots,
				int[] stableSlots, CapturedMobSpec[] capturedMobs, int[] expectedCapturedMobSlots, String operation, boolean expectedFits) {
            private int[] protectedSlots() {
                int[] slots = new int[noSortSlots.length + memorySlots.length];
                System.arraycopy(noSortSlots, 0, slots, 0, noSortSlots.length);
                System.arraycopy(memorySlots, 0, slots, noSortSlots.length, memorySlots.length);
                return slots;
			}
		}

		private record CapturedMobSpec(int slot, int width, int height, String entityType) {
		}

        private record ColumnUpgradeRegressionResult(String name, boolean passed, boolean expectedFits, boolean actualFits, int beforeStacks, int afterStacks,
                String error) {
        }

        private record ColumnUpgradeSimulationResult(boolean fits) {
        }

        private record ServerTaskContext(MinecraftServer server, UUID playerUuid) {
        }

        private static Optional<String> extractString(String json, String key) {
            String prefix = "\"" + key + "\"";
            int keyIndex = json.indexOf(prefix);
            if (keyIndex < 0) {
                return Optional.empty();
            }
            int colonIndex = json.indexOf(':', keyIndex + prefix.length());
            int startQuoteIndex = json.indexOf('"', colonIndex + 1);
            if (colonIndex < 0 || startQuoteIndex < 0) {
                return Optional.empty();
            }
            StringBuilder value = new StringBuilder();
            boolean escaped = false;
            for (int i = startQuoteIndex + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    value.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    return Optional.of(value.toString());
                } else {
                    value.append(c);
                }
            }
            return Optional.empty();
        }

        private static Optional<Boolean> extractBoolean(String json, String key) {
            return extractRawValue(json, key).map(value -> Boolean.parseBoolean(value.toLowerCase(Locale.ROOT)));
        }

        private static Optional<Integer> extractInt(String json, String key) {
            return extractRawValue(json, key).map(Integer::parseInt);
        }

        private static Optional<Long> extractLong(String json, String key) {
            return extractRawValue(json, key).map(Long::parseLong);
        }

        private static Optional<String> extractRawValue(String json, String key) {
            String prefix = "\"" + key + "\"";
            int keyIndex = json.indexOf(prefix);
            if (keyIndex < 0) {
                return Optional.empty();
            }
            int colonIndex = json.indexOf(':', keyIndex + prefix.length());
            if (colonIndex < 0) {
                return Optional.empty();
            }
            int start = colonIndex + 1;
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
                start++;
            }
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            return Optional.of(json.substring(start, end).trim());
        }

        private static String jsonProperty(String name, String value) {
            return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
        }

        private static String escapeJson(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }

        private static void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
