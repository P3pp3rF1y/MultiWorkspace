package net.p3pp3rf1y.devclientautomation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.InputConstants;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.devclientautomation.recipeviewer.RecipeViewerAutomationManager;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitResult;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitter;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        private static final UUID SUB_MOB_CATCHER_PARENT_MOB_ID = new UUID(0L, 101L);
        private static final UUID SUB_MOB_CATCHER_SUB_MOB_ID = new UUID(0L, 102L);

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
				httpServer.createContext("/game/key-click", this::gameKeyClick);
				httpServer.createContext("/game/use", this::gameUse);
				httpServer.createContext("/game/unpause", this::gameUnpause);
				httpServer.createContext("/game/look-block", this::gameLookBlock);
				httpServer.createContext("/game/clear-look-block", this::gameClearLookBlock);
				httpServer.createContext("/mouse/move", this::moveMouse);
                httpServer.createContext("/window/maximize", this::maximizeWindow);
                httpServer.createContext("/wait", this::waitFor);
                httpServer.createContext("/client/stop", this::stopClient);
                httpServer.createContext("/world/load", this::loadWorld);
                httpServer.createContext("/screenshot", this::screenshot);
                httpServer.createContext("/backpack/column-upgrade-regressions", this::backpackColumnUpgradeRegressions);
                httpServer.createContext("/backpack/quick-move-column-upgrade-regression", this::backpackQuickMoveColumnUpgradeRegression);
                httpServer.createContext("/backpack/quick-move-column-upgrade-in-regression", this::backpackQuickMoveColumnUpgradeInRegression);
                httpServer.createContext("/backpack/gui-regression/run", this::backpackGuiRegressionRun);
                httpServer.createContext("/backpack/remote-upgrade-slot-regression", this::backpackRemoteUpgradeSlotRegression);
                httpServer.createContext("/recipe-viewer/state", this::recipeViewerState);
                httpServer.createContext("/recipe-viewer/search", this::recipeViewerSearch);
                httpServer.createContext("/recipe-viewer/open", this::recipeViewerOpen);
                httpServer.createContext("/recipe-viewer/query", this::recipeViewerQuery);
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

		private void gameKeyClick(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String key = extractString(body, "key").orElse("");
			sendJson(exchange, runOnClient(() -> clickGameKey(key)));
		}

		private void gameUse(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJson(exchange, runOnClient(this::useGameKey));
		}

		private void gameUnpause(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJson(exchange, runOnClient(this::unpauseGame));
		}

		private void gameLookBlock(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "GET");
			sendJson(exchange, runOnClient(this::lookBlockJson));
		}

		private void gameClearLookBlock(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJson(exchange, runOnClient(this::clearLookBlock));
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

        private void backpackColumnUpgradeRegressions(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            sendJson(exchange, runOnServer(this::runBackpackColumnUpgradeRegressions));
        }

        private void backpackQuickMoveColumnUpgradeRegression(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            try {
                sendJson(exchange, runQuickMoveColumnUpgradeRegression(false));
            } catch (Throwable t) {
                sendJson(exchange, regressionJson(false, t.getMessage() == null ? t.getClass().getName() : t.getMessage()));
            }
        }

        private void backpackQuickMoveColumnUpgradeInRegression(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            try {
                sendJson(exchange, runQuickMoveColumnUpgradeRegression(true));
            } catch (Throwable t) {
                sendJson(exchange, regressionJson(false, t.getMessage() == null ? t.getClass().getName() : t.getMessage()));
            }
        }

        private void backpackRemoteUpgradeSlotRegression(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            sendJsonHandling(exchange, () -> runOnClient(this::runBackpackRemoteUpgradeSlotRegression));
        }

        private void backpackGuiRegressionRun(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "POST");
            String body = readBody(exchange);
            sendJsonHandling(exchange, () -> runBackpackGuiRegression(body));
        }

        private String runBackpackGuiRegression(String body) {
            JsonObject request = body == null || body.isBlank() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject();
            String name = request.has("name") ? request.get("name").getAsString() : "unnamed";
            String type = request.has("type") ? request.get("type").getAsString() : "columnUpgradeSync";
            if ("subMobCatcherImmediateOpen".equals(type)) {
                return runSubMobCatcherImmediateOpenRegression(name);
            }
            if (!"columnUpgradeSync".equals(type)) {
                throw new IllegalArgumentException("Unknown backpack GUI regression type " + type);
            }

            BackpackGuiRegressionContext context = BackpackGuiRegressionContext.fromName(request.has("context") ? request.get("context").getAsString() : "");
            resetBackpackGuiRegressionState();
            prepareBackpackGuiRegression(context);
            waitForOpenBackpackGuiRegressionMenu(context);
            PlacedColumnUpgradeClickExpectation expectation = clickBackpackGuiRegressionColumnUpgradeWhenReady(context);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            PlacedColumnUpgradeState state;
            do {
                state = runOnClient(() -> getBackpackGuiRegressionState(context));
                if (state.matches(expectation)) {
                    return placedColumnUpgradeRegressionJson(name, true, expectation, state, null);
                }
                sleep(50);
            } while (System.nanoTime() < deadline);

            return placedColumnUpgradeRegressionJson(name, false, expectation, state, "Timed out waiting for " + context.jsonName() + " backpack column sync");
        }

        private String runSubMobCatcherImmediateOpenRegression(String name) {
            try {
                resetBackpackGuiRegressionState();
                runOnServer(this::setupParentMobCatcherBackpackRegression);
                runOnClient(this::setupClientParentMobCatcherBackpackRegression);
                runOnServer(this::openParentMobCatcherBackpackRegression);
                waitForOpenParentBackpackMenu();

                runOnServer(this::insertMobCatcherSubBackpackIntoOpenParent);
                runOnClient(this::insertClientMobCatcherSubBackpackIntoOpenParent);

                SubMobCatcherRegressionState parentState = runOnClient(this::getParentMobCatcherRegressionState);
                if (!parentState.parentMatches()) {
                    return subMobCatcherRegressionJson(name, false, parentState, parentState, "Parent backpack mob catcher data did not stay separate after inserting sub backpack");
                }

                openSubBackpackColumnUpgradeRegressionWhenReady();

                SubMobCatcherRegressionState subState = runOnClient(this::getSubMobCatcherRegressionState);
                return subMobCatcherRegressionJson(name, subState.subMatches(), parentState, subState,
                        subState.subMatches() ? null : "Sub backpack did not open with its own mob catcher data");
            } catch (RuntimeException e) {
                SubMobCatcherRegressionState state = runOnClient(this::getCurrentMobCatcherRegressionStateSafely);
                return subMobCatcherRegressionJson(name, false, state, state, e.getMessage());
            }
        }

        private void resetBackpackGuiRegressionState() {
            runOnServer(player -> {
                player.containerMenu.setCarried(ItemStack.EMPTY);
                player.closeContainer();
                player.getInventory().setChanged();
                return true;
            });
            runOnClient(() -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.containerMenu.setCarried(ItemStack.EMPTY);
                    Minecraft.getInstance().setScreen(null);
                }
                return true;
            });
            sleep(100);
        }

        private PlacedColumnUpgradeClickExpectation clickBackpackGuiRegressionColumnUpgradeWhenReady(BackpackGuiRegressionContext context) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            RuntimeException lastError = null;
            do {
                try {
                    waitForOpenBackpackGuiRegressionMenu(context);
                    return runOnClient(() -> clickBackpackGuiRegressionColumnUpgrade(context));
                } catch (RuntimeException e) {
                    lastError = e;
                    sleep(50);
                }
            } while (System.nanoTime() < deadline);
            throw lastError == null ? new IllegalStateException("Timed out waiting to click " + context.jsonName() + " backpack upgrade") : lastError;
        }

        private void prepareBackpackGuiRegression(BackpackGuiRegressionContext context) {
            switch (context) {
                case PLACED -> {
                    runOnServer(this::setupPlacedBackpackColumnUpgradeRegression);
                    runOnClient(this::setupClientPlacedBackpackColumnUpgradeRegression);
                    runOnServer(this::openPlacedBackpackColumnUpgradeRegression);
                }
                case CURIOS -> {
                    runOnServer(this::setupCuriosBackpackColumnUpgradeRegression);
                    runOnClient(this::setupClientCuriosBackpackColumnUpgradeRegression);
                    runOnServer(this::openCuriosBackpackColumnUpgradeRegression);
                }
                case SUB -> {
                    runOnServer(this::setupSubBackpackColumnUpgradeRegression);
                    runOnClient(this::setupClientSubBackpackColumnUpgradeRegression);
                    runOnServer(this::openParentBackpackColumnUpgradeRegression);
                    waitForOpenParentBackpackMenu();
                    openSubBackpackColumnUpgradeRegressionWhenReady();
                }
            }
        }

        private void openSubBackpackColumnUpgradeRegressionWhenReady() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            do {
                runOnClient(this::openSubBackpackColumnUpgradeRegressionFromClient);
                sleep(100);
                if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen
                        && Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
                        && menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK)) {
                    return;
                }
            } while (System.nanoTime() < deadline);
        }

        private Boolean openSubBackpackColumnUpgradeRegressionFromClient() {
            ClientPacketDistributor.sendToServer(new BackpackOpenPayload(0));
            return true;
        }

        private void waitForOpenBackpackGuiRegressionMenu(BackpackGuiRegressionContext context) {
            switch (context) {
                case PLACED -> waitForOpenPlacedBackpackMenu();
                case CURIOS -> waitForOpenCuriosBackpackMenu();
                case SUB -> waitForOpenSubBackpackMenu();
            }
        }

        private PlacedColumnUpgradeClickExpectation clickBackpackGuiRegressionColumnUpgrade(BackpackGuiRegressionContext context) {
            return switch (context) {
                case PLACED -> clickPlacedBackpackColumnUpgrade();
                case CURIOS -> clickCuriosBackpackColumnUpgrade();
                case SUB -> clickSubBackpackColumnUpgrade();
            };
        }

        private PlacedColumnUpgradeState getBackpackGuiRegressionState(BackpackGuiRegressionContext context) {
            return switch (context) {
                case PLACED -> getPlacedBackpackColumnUpgradeState();
                case CURIOS -> getCuriosBackpackColumnUpgradeState();
                case SUB -> getSubBackpackColumnUpgradeState();
            };
        }

        private Boolean setupPlacedBackpackColumnUpgradeRegression(ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            BlockPos pos = getRegressionBackpackPos(player);

            level.setBlock(pos, ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, player.getDirection().getOpposite()), 3);
            BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(level, pos, BackpackBlockEntity.class)
                    .orElseThrow(() -> new IllegalStateException("Failed to place regression backpack block"));

            backpackBlockEntity.setBackpack(createColumnUpgradeRegressionBackpack());
            return true;
        }

        private Boolean setupClientPlacedBackpackColumnUpgradeRegression() {
            if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
                throw new IllegalStateException("Client level/player is not available");
            }
            BlockPos pos = getRegressionBackpackPos(Minecraft.getInstance().player);
            Minecraft.getInstance().level.setBlock(pos, ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, Minecraft.getInstance().player.getDirection().getOpposite()), 3);
            BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, BackpackBlockEntity.class)
                    .orElseThrow(() -> new IllegalStateException("Failed to create client regression backpack block"));
            backpackBlockEntity.setBackpack(createColumnUpgradeRegressionBackpack());
            return true;
        }

        private Boolean openPlacedBackpackColumnUpgradeRegression(ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            BlockPos pos = getRegressionBackpackPos(player);

            BackpackContext.Block backpackContext = new BackpackContext.Block(pos);
            player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext), Component.literal("Placed Column Regression")), backpackContext::toBuffer);
            level.gameEvent(player, GameEvent.CONTAINER_OPEN, pos);
            return true;
        }

        private BlockPos getRegressionBackpackPos(LivingEntity player) {
            return player.blockPosition().relative(player.getDirection(), 2);
        }

        private Boolean setupCuriosBackpackColumnUpgradeRegression(ServerPlayer player) {
            ItemStack backpack = createColumnUpgradeRegressionBackpack();
            String identifier = getCuriosBackpackIdentifier(player, backpack);
            ensureCuriosSlot(player, identifier, 1);
            setCuriosStack(player, identifier, 0, backpack);
            PlayerInventoryHandler inventoryHandler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
                    .orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
            if (inventoryHandler.getStackInSlot(player, identifier, 0).isEmpty()) {
                throw new IllegalStateException("Curios backpack stack was not set in " + identifier);
            }
            return true;
        }

        private Boolean setupClientCuriosBackpackColumnUpgradeRegression() {
            if (Minecraft.getInstance().player == null) {
                throw new IllegalStateException("Client player is not available");
            }
            ItemStack backpack = createColumnUpgradeRegressionBackpack();
            String identifier = getCuriosBackpackIdentifier(Minecraft.getInstance().player, backpack);
            setCuriosStack(Minecraft.getInstance().player, identifier, 0, backpack);
            return true;
        }

        private Boolean openCuriosBackpackColumnUpgradeRegression(ServerPlayer player) {
            String identifier = getCuriosBackpackIdentifier(player, createColumnUpgradeRegressionBackpack());

            BackpackContext.Item backpackContext = new BackpackContext.Item(CompatModIds.CURIOS, identifier, 0);
            player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext), Component.literal("Curios Column Regression")), backpackContext::toBuffer);
            return true;
        }

        private Boolean setupSubBackpackColumnUpgradeRegression(ServerPlayer player) {
            player.getInventory().getNonEquipmentItems().set(0, createParentBackpackWithColumnUpgradeSubBackpack());
            player.getInventory().setChanged();
            return true;
        }

        private Boolean setupParentMobCatcherBackpackRegression(ServerPlayer player) {
            player.getInventory().getNonEquipmentItems().set(0, createMobCatcherRegressionBackpack(81, 3, SUB_MOB_CATCHER_PARENT_MOB_ID, 0, "Parent Pig"));
            player.getInventory().setChanged();
            return true;
        }

        private Boolean setupClientSubBackpackColumnUpgradeRegression() {
            if (Minecraft.getInstance().player == null) {
                throw new IllegalStateException("Client player is not available");
            }
            Minecraft.getInstance().player.getInventory().getNonEquipmentItems().set(0, createParentBackpackWithColumnUpgradeSubBackpack());
            Minecraft.getInstance().player.getInventory().setChanged();
            return true;
        }

        private Boolean setupClientParentMobCatcherBackpackRegression() {
            if (Minecraft.getInstance().player == null) {
                throw new IllegalStateException("Client player is not available");
            }
            Minecraft.getInstance().player.getInventory().getNonEquipmentItems().set(0, createMobCatcherRegressionBackpack(81, 3, SUB_MOB_CATCHER_PARENT_MOB_ID, 0, "Parent Pig"));
            Minecraft.getInstance().player.getInventory().setChanged();
            return true;
        }

        private Boolean openSubBackpackColumnUpgradeRegression(ServerPlayer player) {
            BackpackContext.ItemSubBackpack backpackContext = new BackpackContext.ItemSubBackpack(PlayerInventoryProvider.MAIN_INVENTORY, "", 0, false, 0, true);
            player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext), Component.literal("Sub Column Regression")), backpackContext::toBuffer);
            return true;
        }

        private Boolean openParentBackpackColumnUpgradeRegression(ServerPlayer player) {
            BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
            player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext), Component.literal("Parent Column Regression")), backpackContext::toBuffer);
            return true;
        }

        private Boolean openParentMobCatcherBackpackRegression(ServerPlayer player) {
            BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
            player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext), Component.literal("Mob Catcher Parent Regression")), backpackContext::toBuffer);
            return true;
        }

        private Boolean insertMobCatcherSubBackpackIntoOpenParent(ServerPlayer player) {
            if (!(player.containerMenu instanceof BackpackContainer menu)) {
                throw new IllegalStateException("Parent backpack menu is not open on server");
            }
            insertMobCatcherSubBackpack(menu);
            return true;
        }

        private Boolean insertClientMobCatcherSubBackpackIntoOpenParent() {
            if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
                throw new IllegalStateException("Parent backpack menu is not open on client");
            }
            insertMobCatcherSubBackpack(menu);
            return true;
        }

        private void insertMobCatcherSubBackpack(BackpackContainer menu) {
            if (menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
                throw new IllegalStateException("Expected parent item backpack menu before inserting sub backpack");
            }
            InventoryHandler inventoryHandler = menu.getStorageWrapper().getInventoryHandler();
            inventoryHandler.setStackInSlot(0, createMobCatcherRegressionBackpack(144, 7, SUB_MOB_CATCHER_SUB_MOB_ID, 10, "Sub Cow"));
            inventoryHandler.saveInventory();
            menu.getStorageWrapper().onContentsUpdated();
        }

        private ItemStack createParentBackpackWithColumnUpgradeSubBackpack() {
            ItemStack parentBackpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
            IBackpackWrapper parentWrapper = BackpackWrapper.fromStackNoCache(parentBackpack);
            parentWrapper.setSlotNumbers(81, 3);
            parentWrapper.getInventoryHandler().setStackInSlot(0, createColumnUpgradeRegressionBackpack());
            parentWrapper.onContentsUpdated();
            return parentBackpack;
        }

        private ItemStack createColumnUpgradeRegressionBackpack() {
            ItemStack backpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
            IBackpackWrapper backpackWrapper = BackpackWrapper.fromStackNoCache(backpack);
            backpackWrapper.setSlotNumbers(81, 3);
            backpackWrapper.getInventoryHandler();
            backpackWrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
            backpackWrapper.setColumnsTaken(2, false);
            backpackWrapper.onContentsUpdated();
            return backpack;
        }

        private ItemStack createMobCatcherRegressionBackpack(int inventorySlots, int upgradeSlots, UUID mobId, int mobSlot, String displayName) {
            ItemStack backpack = inventorySlots > 81 ? new ItemStack(ModItems.DIAMOND_BACKPACK.get()) : new ItemStack(ModItems.GOLD_BACKPACK.get());
            IBackpackWrapper backpackWrapper = BackpackWrapper.fromStackNoCache(backpack);
            backpackWrapper.setSlotNumbers(inventorySlots, upgradeSlots);
            backpackWrapper.getInventoryHandler();
            backpackWrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
            backpackWrapper.getUpgradeHandler().saveInventory();
            MobCatcherStorage.addCapturedMob(backpackWrapper, new CapturedMob(mobId, Identifier.parse("minecraft:pig"), new CompoundTag(), mobSlot, 1, 1, 1, false, displayName, 10, 10));
            backpackWrapper.onContentsUpdated();
            return backpack;
        }

        private void ensureCuriosSlot(ServerPlayer player, String identifier, int slots) {
            PlayerInventoryHandler inventoryHandler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
                    .orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
            if (inventoryHandler.getSlotCount(player, identifier) < slots) {
                var server = player.level().getServer();
                if (server == null) {
                    throw new IllegalStateException("Server is not available for Curios slot setup");
                }
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER).withSuppressedOutput(), "curios add " + identifier + " " + player.getGameProfile().name() + " " + slots);
            }
            if (inventoryHandler.getSlotCount(player, identifier) < slots) {
                throw new IllegalStateException("Unable to configure Curios slot " + identifier + "; slot count is " + inventoryHandler.getSlotCount(player, identifier));
            }
        }

        private String getCuriosBackpackIdentifier(LivingEntity player, ItemStack backpack) {
            for (String identifier : getCuriosItemSlotTypes(backpack, player).keySet()) {
                return identifier;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                throw new IllegalStateException("No Curios slot type was found for backpack");
            }
            PlayerInventoryHandler inventoryHandler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
                    .orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
            for (String identifier : inventoryHandler.getIdentifiers(serverPlayer)) {
                if (inventoryHandler.getSlotCount(serverPlayer, identifier) > 0) {
                    return identifier;
                }
            }
            throw new IllegalStateException("No Curios slot with available slots was found");
        }

        private Map<String, ?> getCuriosItemSlotTypes(ItemStack backpack, LivingEntity player) {
            try {
                Class<?> curiosSlotTypesClass = Class.forName("top.theillusivec4.curios.api.CuriosSlotTypes");
                return (Map<String, ?>) curiosSlotTypesClass.getMethod("getItemSlotTypes", ItemStack.class, LivingEntity.class).invoke(null, backpack, player);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to get Curios item slot types", e);
            }
        }

        private void setCuriosStack(LivingEntity player, String identifier, int slot, ItemStack backpack) {
            try {
                Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Optional<?> curiosInventory = (Optional<?>) curiosApiClass.getMethod("getCuriosInventory", LivingEntity.class).invoke(null, player);
                Object inventory = curiosInventory.orElseThrow(() -> new IllegalStateException("Player has no Curios inventory"));
                inventory.getClass().getMethod("setEquippedCurio", String.class, int.class, ItemStack.class).invoke(inventory, identifier, slot, backpack);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to set Curios backpack stack", e);
            }
        }

        private void waitForOpenPlacedBackpackMenu() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            do {
                if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen
                        && Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
                        && menu.getBlockPosition().isPresent())) {
                    return;
                }
                sleep(50);
            } while (System.nanoTime() < deadline);
            throw new IllegalStateException("Timed out waiting for placed backpack screen to open");
        }

        private void waitForOpenCuriosBackpackMenu() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            do {
                if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen
                        && Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
                        && menu.getBlockPosition().isEmpty())) {
                    return;
                }
                sleep(50);
            } while (System.nanoTime() < deadline);
            throw new IllegalStateException("Timed out waiting for curios backpack screen to open");
        }

        private void waitForOpenParentBackpackMenu() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            do {
                if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen
                        && Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
                        && menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK)) {
                    return;
                }
                sleep(50);
            } while (System.nanoTime() < deadline);
            throw new IllegalStateException("Timed out waiting for parent backpack screen to open");
        }

        private void waitForOpenSubBackpackMenu() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            do {
                if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen
                        && Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
                        && menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK)) {
                    return;
                }
                sleep(50);
            } while (System.nanoTime() < deadline);
            throw new IllegalStateException("Timed out waiting for sub backpack screen to open");
        }

        private PlacedColumnUpgradeClickExpectation clickPlacedBackpackColumnUpgrade() {
            BackpackContainer menu = getOpenPlacedBackpackMenu();
            if (menu.getNumberOfUpgradeSlots() < 2) {
                throw new IllegalStateException("Placed backpack needs at least two upgrade slots");
            }
            if (!menu.getCarried().isEmpty()) {
                throw new IllegalStateException("Cursor must be empty before running placed backpack regression");
            }

            Slot slot = menu.upgradeSlots.get(1);
            ItemStack slotStack = slot.getItem();
            if (slotStack.isEmpty() || !(slotStack.getItem() instanceof IUpgradeItem<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
                throw new IllegalStateException("Placed backpack upgrade slot 1 must contain a column-taking upgrade");
            }

            int beforeColumnsTaken = menu.getStorageWrapper().getColumnsTaken();
            int columnsChange = upgradeItem.getInventoryColumnsTaken();
            int expectedColumnsTaken = beforeColumnsTaken - columnsChange;
            int rows = menu.getStorageWrapper().getNumberOfSlotRows();
            int handlerSlots = menu.getStorageWrapper().getInventoryHandler().size();
            int baseColumns = handlerSlots <= 81 ? 9 : 12;
            int baseStorageSlots = handlerSlots / rows == baseColumns ? handlerSlots : handlerSlots + beforeColumnsTaken * rows;
            int expectedStorageSlots = baseStorageSlots - expectedColumnsTaken * rows;

            Screen screen = Minecraft.getInstance().screen;
            clickSlot(screen, slot);

            return new PlacedColumnUpgradeClickExpectation(expectedColumnsTaken, expectedStorageSlots);
        }

        private BackpackContainer getOpenPlacedBackpackMenu() {
            if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
                throw new IllegalStateException("Placed backpack screen is not open");
            }
            if (menu.getBlockPosition().isEmpty()) {
                throw new IllegalStateException("Open backpack is not a placed backpack");
            }
            return menu;
        }

        private PlacedColumnUpgradeClickExpectation clickCuriosBackpackColumnUpgrade() {
            BackpackContainer menu = getOpenCuriosBackpackMenu();
            if (menu.getNumberOfUpgradeSlots() < 2) {
                throw new IllegalStateException("Curios backpack needs at least two upgrade slots");
            }
            if (!menu.getCarried().isEmpty()) {
                throw new IllegalStateException("Cursor must be empty before running curios backpack regression");
            }

            Slot slot = menu.upgradeSlots.get(1);
            ItemStack slotStack = slot.getItem();
            if (slotStack.isEmpty() || !(slotStack.getItem() instanceof IUpgradeItem<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
                throw new IllegalStateException("Curios backpack upgrade slot 1 must contain a column-taking upgrade");
            }

            int beforeColumnsTaken = menu.getStorageWrapper().getColumnsTaken();
            int columnsChange = upgradeItem.getInventoryColumnsTaken();
            int expectedColumnsTaken = beforeColumnsTaken - columnsChange;
            int rows = menu.getStorageWrapper().getNumberOfSlotRows();
            int handlerSlots = menu.getStorageWrapper().getInventoryHandler().size();
            int baseColumns = handlerSlots <= 81 ? 9 : 12;
            int baseStorageSlots = handlerSlots / rows == baseColumns ? handlerSlots : handlerSlots + beforeColumnsTaken * rows;
            int expectedStorageSlots = baseStorageSlots - expectedColumnsTaken * rows;

            Screen screen = Minecraft.getInstance().screen;
            clickSlot(screen, slot);

            return new PlacedColumnUpgradeClickExpectation(expectedColumnsTaken, expectedStorageSlots);
        }

        private BackpackContainer getOpenCuriosBackpackMenu() {
            if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
                throw new IllegalStateException("Curios backpack screen is not open");
            }
            if (menu.getBlockPosition().isPresent()) {
                throw new IllegalStateException("Open backpack is not a Curios/item backpack");
            }
            return menu;
        }

        private PlacedColumnUpgradeClickExpectation clickSubBackpackColumnUpgrade() {
            BackpackContainer menu = getOpenSubBackpackMenu();
            if (menu.getNumberOfUpgradeSlots() < 2) {
                throw new IllegalStateException("Sub backpack needs at least two upgrade slots");
            }
            if (!menu.getCarried().isEmpty()) {
                throw new IllegalStateException("Cursor must be empty before running sub backpack regression");
            }

            Slot slot = menu.upgradeSlots.get(1);
            ItemStack slotStack = slot.getItem();
            if (slotStack.isEmpty() || !(slotStack.getItem() instanceof IUpgradeItem<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
                throw new IllegalStateException("Sub backpack upgrade slot 1 must contain a column-taking upgrade");
            }

            int beforeColumnsTaken = menu.getStorageWrapper().getColumnsTaken();
            int columnsChange = upgradeItem.getInventoryColumnsTaken();
            int expectedColumnsTaken = beforeColumnsTaken - columnsChange;
            int rows = menu.getStorageWrapper().getNumberOfSlotRows();
            int handlerSlots = menu.getStorageWrapper().getInventoryHandler().size();
            int baseColumns = handlerSlots <= 81 ? 9 : 12;
            int baseStorageSlots = handlerSlots / rows == baseColumns ? handlerSlots : handlerSlots + beforeColumnsTaken * rows;
            int expectedStorageSlots = baseStorageSlots - expectedColumnsTaken * rows;

            Screen screen = Minecraft.getInstance().screen;
            clickSlot(screen, slot);

            return new PlacedColumnUpgradeClickExpectation(expectedColumnsTaken, expectedStorageSlots);
        }

        private BackpackContainer getOpenSubBackpackMenu() {
            if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
                throw new IllegalStateException("Sub backpack screen is not open");
            }
            if (menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_SUB_BACKPACK) {
                throw new IllegalStateException("Open backpack is not an item sub backpack");
            }
            return menu;
        }

        private void clickSlot(Screen screen, Slot slot) {
            int leftPos = getScreenIntField(screen, "leftPos");
            int topPos = getScreenIntField(screen, "topPos");
            double x = leftPos + slot.x + 8.0;
            double y = topPos + slot.y + 8.0;
            MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
            if (!screen.mouseClicked(event, false)) {
                throw new IllegalStateException("Placed backpack upgrade slot click was not handled");
            }
            screen.mouseReleased(event);
        }

        private int getScreenIntField(Screen screen, String fieldName) {
            Class<?> screenClass = screen.getClass();
            while (screenClass != null) {
                try {
                    Field field = screenClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.getInt(screen);
                } catch (NoSuchFieldException e) {
                    screenClass = screenClass.getSuperclass();
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to read screen field " + fieldName, e);
                }
            }
            throw new IllegalStateException("Unable to find screen field " + fieldName);
        }

        private PlacedColumnUpgradeState getPlacedBackpackColumnUpgradeState() {
            BackpackContainer menu = getOpenPlacedBackpackMenu();
            return new PlacedColumnUpgradeState(menu.getStorageWrapper().getColumnsTaken(), menu.getNumberOfStorageInventorySlots(),
                    menu.getStorageWrapper().getInventoryHandler().size(), menu.upgradeSlots.get(1).getItem().isEmpty(), !menu.getCarried().isEmpty());
        }

        private PlacedColumnUpgradeState getCuriosBackpackColumnUpgradeState() {
            BackpackContainer menu = getOpenCuriosBackpackMenu();
            return new PlacedColumnUpgradeState(menu.getStorageWrapper().getColumnsTaken(), menu.getNumberOfStorageInventorySlots(),
                    menu.getStorageWrapper().getInventoryHandler().size(), menu.upgradeSlots.get(1).getItem().isEmpty(), !menu.getCarried().isEmpty());
        }

        private PlacedColumnUpgradeState getSubBackpackColumnUpgradeState() {
            BackpackContainer menu = getOpenSubBackpackMenu();
            return new PlacedColumnUpgradeState(menu.getStorageWrapper().getColumnsTaken(), menu.getNumberOfStorageInventorySlots(),
                    menu.getStorageWrapper().getInventoryHandler().size(), menu.upgradeSlots.get(1).getItem().isEmpty(), !menu.getCarried().isEmpty());
        }

        private String placedColumnUpgradeRegressionJson(String name, boolean ok, PlacedColumnUpgradeClickExpectation expectation, PlacedColumnUpgradeState state, @Nullable String error) {
            return "{\"ok\":" + ok
                    + "," + jsonProperty("name", name)
                    + ",\"expectedColumnsTaken\":" + expectation.expectedColumnsTaken()
                    + ",\"actualColumnsTaken\":" + state.columnsTaken()
                    + ",\"expectedStorageSlots\":" + expectation.expectedStorageSlots()
                    + ",\"actualStorageSlots\":" + state.storageSlots()
                    + ",\"actualInventoryHandlerSlots\":" + state.inventoryHandlerSlots()
                    + ",\"upgradeSlotEmpty\":" + state.upgradeSlotEmpty()
                    + ",\"carriedNotEmpty\":" + state.carriedNotEmpty()
                    + "," + jsonProperty("error", error)
                    + "}";
        }

        private SubMobCatcherRegressionState getParentMobCatcherRegressionState() {
            BackpackContainer menu = getOpenParentBackpackMenu();
            IBackpackWrapper nestedWrapper = getNestedBackpackWrapper(menu);
            return getSubMobCatcherRegressionState(menu, nestedWrapper);
        }

        private SubMobCatcherRegressionState getSubMobCatcherRegressionState() {
            BackpackContainer menu = getOpenSubBackpackMenu();
            return getSubMobCatcherRegressionState(menu, null);
        }

        private SubMobCatcherRegressionState getCurrentMobCatcherRegressionStateSafely() {
            try {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu) {
                    IBackpackWrapper nestedWrapper = menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK ? getNestedBackpackWrapper(menu) : null;
                    return getSubMobCatcherRegressionState(menu, nestedWrapper);
                }
            } catch (RuntimeException ignored) {
                // Return an empty state below so the regression response still explains the failure.
            }
            return new SubMobCatcherRegressionState("none", 0, false, 0, null, 0, null);
        }

        private BackpackContainer getOpenParentBackpackMenu() {
            if (!(Minecraft.getInstance().screen instanceof BackpackScreen) || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
                throw new IllegalStateException("Parent backpack screen is not open");
            }
            if (menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
                throw new IllegalStateException("Open backpack is not a parent item backpack");
            }
            return menu;
        }

        private IBackpackWrapper getNestedBackpackWrapper(BackpackContainer menu) {
            ItemStack nestedBackpack = menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0);
            if (!(nestedBackpack.getItem() instanceof BackpackItem)) {
                throw new IllegalStateException("Parent backpack slot 0 does not contain a backpack");
            }
            return BackpackWrapper.fromStackNoCache(nestedBackpack);
        }

        private SubMobCatcherRegressionState getSubMobCatcherRegressionState(BackpackContainer menu, IBackpackWrapper nestedWrapper) {
            List<CapturedMob> currentMobs = MobCatcherStorage.getCapturedMobs(menu.getStorageWrapper());
            List<CapturedMob> nestedMobs = nestedWrapper == null ? List.of() : MobCatcherStorage.getCapturedMobs(nestedWrapper);
            return new SubMobCatcherRegressionState(menu.getBackpackContext().getType().name(), menu.getStorageWrapper().getInventoryHandler().size(), nestedWrapper != null,
                    currentMobs.size(), currentMobs.isEmpty() ? null : currentMobs.get(0).id().toString(), nestedMobs.size(), nestedMobs.isEmpty() ? null : nestedMobs.get(0).id().toString());
        }

        private String subMobCatcherRegressionJson(String name, boolean ok, SubMobCatcherRegressionState parentState, SubMobCatcherRegressionState subState, @Nullable String error) {
            return "{\"ok\":" + ok
                    + "," + jsonProperty("name", name)
                    + "," + jsonProperty("parentContext", parentState.context())
                    + ",\"parentStorageSlots\":" + parentState.storageSlots()
                    + ",\"parentSlot0Backpack\":" + parentState.slot0Backpack()
                    + ",\"parentMobCount\":" + parentState.currentMobCount()
                    + "," + jsonProperty("parentMobId", parentState.currentMobId())
                    + ",\"parentNestedMobCount\":" + parentState.nestedMobCount()
                    + "," + jsonProperty("parentNestedMobId", parentState.nestedMobId())
                    + "," + jsonProperty("subContext", subState.context())
                    + ",\"subStorageSlots\":" + subState.storageSlots()
                    + ",\"subMobCount\":" + subState.currentMobCount()
                    + "," + jsonProperty("subMobId", subState.currentMobId())
                    + "," + jsonProperty("error", error)
                    + "}";
        }

        private String runBackpackRemoteUpgradeSlotRegression() {
            if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
                return "{\"ok\":false,\"error\":\"Player does not have a storage menu open\"}";
            }
            if (menu.getNumberOfUpgradeSlots() < 2) {
                return "{\"ok\":false,\"error\":\"Storage menu needs at least two upgrade slots\"}";
            }

            int logicalUpgradeSlot = menu.getFirstUpgradeSlot() + 1;
            menu.setRemoteSlot(logicalUpgradeSlot, menu.getSlot(logicalUpgradeSlot).getItem().copy());
            return "{\"ok\":true,\"slot\":" + logicalUpgradeSlot + "}";
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

            LevelSettings levelSettings = new LevelSettings(worldName, GameType.CREATIVE, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
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

		private String clickGameKey(String keyName) {
			Minecraft minecraft = Minecraft.getInstance();
			int keyCode = keyCode(keyName);
			if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
				return "{\"ok\":false,\"error\":\"Unknown key\"}";
			}
			minecraft.options.pauseOnLostFocus = false;
			if (minecraft.screen != null) {
				minecraft.setScreen(null);
			}
			KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
			return "{\"ok\":true}";
		}

		private String useGameKey() {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.options.pauseOnLostFocus = false;
			if (minecraft.screen != null) {
				minecraft.setScreen(null);
			}
			if (minecraft.level == null || minecraft.player == null || minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
				return "{\"ok\":false,\"error\":\"Not looking at a block\"}";
			}
			BlockPos pos = ((BlockHitResult) minecraft.hitResult).getBlockPos();
			double distance = distanceToBlock(minecraft, pos);
			if (distance < 1) {
				return "{\"ok\":false,\"error\":\"Looked-at block is too close\",\"distance\":" + distance + "}";
			}
			KeyMapping.click(minecraft.options.keyUse.getKey());
			return "{\"ok\":true,\"distance\":" + distance + "}";
		}

		private String unpauseGame() {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.options.pauseOnLostFocus = false;
			if (minecraft.screen != null) {
				minecraft.setScreen(null);
			}
			return "{\"ok\":true}";
		}

		private String lookBlockJson() {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.level == null || minecraft.player == null || minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
				return "{\"ok\":false,\"error\":\"Not looking at a block\"}";
			}
			BlockPos pos = ((BlockHitResult) minecraft.hitResult).getBlockPos();
			double distance = distanceToBlock(minecraft, pos);
			return "{\"ok\":true,\"x\":" + pos.getX() + ",\"y\":" + pos.getY() + ",\"z\":" + pos.getZ() + ","
					+ jsonProperty("block", minecraft.level.getBlockState(pos).getBlock().toString()) + ","
					+ "\"air\":" + minecraft.level.getBlockState(pos).isAir() + ","
					+ "\"distance\":" + distance + ","
					+ "\"atLeastOneBlockAway\":" + (distance >= 1) + "}";
		}

		private double distanceToBlock(Minecraft minecraft, BlockPos pos) {
			double dx = distanceToRange(minecraft.player.getX(), pos.getX(), pos.getX() + 1);
			double dy = distanceToRange(minecraft.player.getY(), pos.getY(), pos.getY() + 1);
			double dz = distanceToRange(minecraft.player.getZ(), pos.getZ(), pos.getZ() + 1);
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		private static double distanceToRange(double value, double min, double max) {
			if (value < min) {
				return min - value;
			}
			if (value > max) {
				return value - max;
			}
			return 0;
		}

		private String clearLookBlock() {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.level == null || minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK || minecraft.getSingleplayerServer() == null) {
				return "{\"ok\":false,\"error\":\"Not looking at a block in singleplayer\"}";
			}
			BlockPos pos = ((BlockHitResult) minecraft.hitResult).getBlockPos();
			boolean changed = minecraft.getSingleplayerServer().overworld().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			return "{\"ok\":true,\"changed\":" + changed + ",\"x\":" + pos.getX() + ",\"y\":" + pos.getY() + ",\"z\":" + pos.getZ() + "}";
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
                    Identifier upgradeName = Identifier.parse(scenario.get("upgrade").getAsString());
                    Item upgradeItem = BuiltInRegistries.ITEM.getOptional(upgradeName).orElseThrow(() -> new IllegalArgumentException("Unknown upgrade " + upgradeName));
                    int[] occupiedSlots = getOccupiedSlots(scenario);
                    boolean expectedFits = scenario.get("expectedFits").getAsBoolean();
                    int[] noSortSlots = getIntArray(scenario, "noSortSlots");
                    int[] memorySlots = getIntArray(scenario, "memorySlots");
                    int[] stableSlots = getIntArray(scenario, "stableSlots");
                    CapturedMobSpec[] capturedMobs = getCapturedMobs(scenario);
                    int[] expectedCapturedMobSlots = getIntArray(scenario, "expectedCapturedMobSlots");
                    String operation = scenario.has("operation") ? scenario.get("operation").getAsString() : "insert";
                    scenarios.add(new ColumnUpgradeRegressionScenario(name, inventorySlots, upgradeItem, occupiedSlots, noSortSlots, memorySlots, stableSlots, capturedMobs,
                            expectedCapturedMobSlots, operation, expectedFits));
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
                Identifier itemName = Identifier.parse(itemElement.getAsString());
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
                capturedMobs[i] = new CapturedMobSpec(
                        capturedMob.get("slot").getAsInt(),
                        capturedMob.get("width").getAsInt(),
                        capturedMob.get("height").getAsInt(),
                        entityType
                );
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
                MobCatcherStorage.addCapturedMob(wrapper, new CapturedMob(new UUID(0, i + 1), Identifier.parse(capturedMob.entityType()), new CompoundTag(),
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

        private String runQuickMoveColumnUpgradeRegression(boolean stopAfterFirstInsert) {
            runOnClient(() -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.closeContainer();
                }
                return true;
            });
            Optional<String> closeError = waitForServerInventoryMenu();
            if (closeError.isPresent()) {
                return regressionJson(false, closeError.get());
            }

            ItemStack backpack = runOnServer(this::setupQuickMoveColumnUpgradeBackpack);
            runOnClient(() -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.getInventory().setItem(0, backpack.copy());
                    Minecraft.getInstance().player.getInventory().setItem(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
                    Minecraft.getInstance().player.getInventory().setSelectedSlot(0);
                    Minecraft.getInstance().player.getInventory().setChanged();
                }
                return true;
            });
            runOnServer(player -> {
                ((BackpackItem) player.getInventory().getItem(player.getInventory().getSelectedSlot()).getItem()).use(player.level(), player, InteractionHand.MAIN_HAND);
                return true;
            });

            Optional<String> screenError = waitForBackpackScreen();
            if (screenError.isPresent()) {
                return regressionJson(false, screenError.get());
            }
            Optional<String> tankSyncError = waitForClientTankUpgradeInPlayerInventory();
            if (tankSyncError.isPresent()) {
                return regressionJson(false, tankSyncError.get());
            }
            Optional<String> initialStateError = runOnClient(() -> validateQuickMoveClientState(0, 81, false, "initial quick-move regression state"));
            if (initialStateError.isPresent()) {
                return regressionJson(false, initialStateError.get() + "; server=" + runOnServer(this::describeQuickMoveServerState));
            }

            for (int cycle = 1; cycle <= 6; cycle++) {
                Optional<String> clickError = runOnClient(this::quickMoveTankUpgradeFromPlayerInventory);
                if (clickError.isPresent()) {
                    return regressionJson(false, "cycle " + cycle + " quick-move in: " + clickError.get());
                }

                Optional<String> quickMoveInSyncError = waitForClientQuickMoveState(2, 63, true, "cycle " + cycle + " after quick-move in sync");
                if (quickMoveInSyncError.isPresent()) {
                    return regressionJson(false, quickMoveInSyncError.get());
                }

                if (stopAfterFirstInsert) {
                    return "{\"ok\":true}";
                }

                Optional<String> quickMoveOutError = runOnClient(this::quickMoveTankUpgradeFromUpgradeSlot);
                if (quickMoveOutError.isPresent()) {
                    return regressionJson(false, "cycle " + cycle + " quick-move out: " + quickMoveOutError.get());
                }

                Optional<String> quickMoveOutSyncError = waitForClientQuickMoveState(0, 81, false, "cycle " + cycle + " after quick-move out sync");
                if (quickMoveOutSyncError.isPresent()) {
                    return regressionJson(false, quickMoveOutSyncError.get());
                }
            }

            return "{\"ok\":true}";
        }

        private ItemStack setupQuickMoveColumnUpgradeBackpack(ServerPlayer player) {
            player.closeContainer();
            ItemStack backpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
            backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 81);
            backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 3);
            IBackpackWrapper wrapper = BackpackWrapper.fromStackNoCache(backpack);
            wrapper.setColumnsTaken(0, false);
            InventoryHandler inventory = wrapper.getInventoryHandler();
            fillRegressionStacks(inventory, new int[] {4, 5, 6, 7, 8, 13, 14, 15, 16, 17, 22, 23, 24, 25, 26, 31, 32, 33, 34, 35, 44, 53, 54, 55, 56, 57, 62},
                    new ColumnUpgradeStackGenerator(List.of(Blocks.OAK_LOG.asItem()), 2, 28));
            wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
            wrapper.getUpgradeHandler().saveInventory();
            MobCatcherStorage.addCapturedMob(wrapper, new CapturedMob(new UUID(0, 1), Identifier.parse("minecraft:pig"), new CompoundTag(), 0, 4, 3, 12, false, "minecraft:pig", 10, 10));
            MobCatcherStorage.addCapturedMob(wrapper, new CapturedMob(new UUID(0, 2), Identifier.parse("minecraft:pig"), new CompoundTag(), 27, 4, 3, 12, false, "minecraft:pig", 10, 10));
            MobCatcherStorage.addCapturedMob(wrapper, new CapturedMob(new UUID(0, 3), Identifier.parse("minecraft:pig"), new CompoundTag(), 40, 4, 3, 12, false, "minecraft:pig", 10, 10));
            inventory.saveInventory();

            player.getInventory().clearContent();
            player.getInventory().setItem(0, backpack);
            player.getInventory().setItem(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
            player.getInventory().setSelectedSlot(0);
            player.getInventory().setChanged();
            return backpack.copy();
        }

        private Optional<String> waitForBackpackScreen() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                Optional<String> error = runOnClient(() -> Minecraft.getInstance().screen instanceof StorageScreenBase<?> && Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer ? Optional.empty() : Optional.of("backpack screen is not open"));
                if (error.isEmpty()) {
                    return Optional.empty();
                }
                sleep(50);
            }
            return Optional.of("timed out waiting for backpack screen");
        }

        private Optional<String> waitForServerInventoryMenu() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                boolean closed = runOnServer(player -> player.containerMenu instanceof InventoryMenu);
                if (closed) {
                    return Optional.empty();
                }
                sleep(50);
            }
            return Optional.of("timed out waiting for previous backpack container to close on server");
        }

        private Optional<String> waitForClientTankUpgradeInPlayerInventory() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                Optional<String> error = runOnClient(() -> {
                    if (!(Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
                        return Optional.of("backpack storage menu is not open");
                    }
                    return findTankUpgradePlayerSlot(menu) >= 0 ? Optional.empty() : Optional.of("tank upgrade is not synced to client player inventory slots");
                });
                if (error.isEmpty()) {
                    return Optional.empty();
                }
                sleep(50);
            }
            return Optional.of("timed out waiting for tank upgrade to sync to client player inventory");
        }

        private int findTankUpgradePlayerSlot(StorageContainerMenuBase<?> menu) {
            for (int slotIndex = 0; slotIndex < menu.realInventorySlots.size(); slotIndex++) {
                if (menu.realInventorySlots.get(slotIndex).getItem().is(ModItems.TANK_UPGRADE.get())) {
                    return slotIndex;
                }
            }
            return -1;
        }

        private Optional<String> quickMoveTankUpgradeFromPlayerInventory() {
            Minecraft minecraft = Minecraft.getInstance();
            if (!(minecraft.screen instanceof StorageScreenBase<?> storageScreen) || !(minecraft.player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
                return Optional.of("backpack storage screen is not open");
            }

            int tankSlot = findTankUpgradePlayerSlot(menu);
            if (tankSlot < 0) {
                return Optional.of("tank upgrade not found in player inventory slots");
            }

            Optional<String> clickError = quickMoveSlot(storageScreen, tankSlot);
            if (clickError.isPresent()) {
                return clickError;
            }
            int upgradeSlot = menu.getFirstUpgradeSlot() + 1;
            if (!menu.getSlot(upgradeSlot).getItem().is(ModItems.TANK_UPGRADE.get())) {
                return Optional.of("client quick-move from slot " + tankSlot + " did not place tank in upgrade slot " + upgradeSlot);
            }
            return Optional.empty();
        }

        private Optional<String> quickMoveTankUpgradeFromUpgradeSlot() {
            Minecraft minecraft = Minecraft.getInstance();
            if (!(minecraft.screen instanceof StorageScreenBase<?> storageScreen) || !(minecraft.player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
                return Optional.of("backpack storage screen is not open");
            }

            int tankSlot = menu.getFirstUpgradeSlot() + 1;
            if (!menu.getSlot(tankSlot).getItem().is(ModItems.TANK_UPGRADE.get())) {
                return Optional.of("tank upgrade is not in upgrade slot 1 before quick-move out");
            }

            return quickMoveSlot(storageScreen, tankSlot);
        }

        private Optional<String> quickMoveSlot(StorageScreenBase<?> storageScreen, int slot) {
            try {
                Method handleInventoryMouseClick = StorageScreenBase.class.getDeclaredMethod("handleInventoryMouseClick", int.class, int.class, ContainerInput.class);
                handleInventoryMouseClick.setAccessible(true);
                handleInventoryMouseClick.invoke(storageScreen, slot, 0, ContainerInput.QUICK_MOVE);
            } catch (ReflectiveOperationException e) {
                return Optional.of("failed to invoke quick move click: " + e.getMessage());
            }
            return Optional.empty();
        }

        private Optional<String> waitForClientQuickMoveState(int expectedColumnsTaken, int expectedInventorySize, boolean expectTankInUpgradeSlot, String phase) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            Optional<String> lastError = Optional.empty();
            while (System.nanoTime() < deadline) {
                lastError = runOnClient(() -> validateQuickMoveClientState(expectedColumnsTaken, expectedInventorySize, expectTankInUpgradeSlot, phase));
                if (lastError.isEmpty()) {
                    return Optional.empty();
                }
                sleep(50);
            }
            String serverState = runOnServer(player -> describeQuickMoveServerState(player));
            return Optional.of("timed out waiting for " + phase + ": " + lastError.orElse("unknown state") + "; server=" + serverState);
        }

        private String describeQuickMoveServerState(ServerPlayer player) {
            if (!(player.containerMenu instanceof BackpackContainer backpackContainer)) {
                return player.containerMenu.getClass().getName();
            }
            IBackpackWrapper wrapper = backpackContainer.getStorageWrapper();
            return "columnsTaken=" + wrapper.getColumnsTaken() + ",size=" + wrapper.getInventoryHandler().size() + ",tankUpgrade=" + wrapper.getUpgradeHandler().getStackInSlot(1).is(ModItems.TANK_UPGRADE.get()) + ",stacks=" + snapshotStacks(wrapper.getInventoryHandler()).values().stream().mapToInt(Integer::intValue).sum();
        }

        private Optional<String> validateQuickMoveClientState(int expectedColumnsTaken, int expectedInventorySize, boolean expectTankInUpgradeSlot, String phase) {
            if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer backpackContainer)) {
                return Optional.of(phase + " client backpack container is not open");
            }
            if (!(Minecraft.getInstance().screen instanceof StorageScreenBase<?> storageScreen)) {
                return Optional.of(phase + " backpack screen is not open");
            }
            IBackpackWrapper wrapper = backpackContainer.getStorageWrapper();
            if (wrapper.getColumnsTaken() != expectedColumnsTaken) {
                return Optional.of(phase + " columnsTaken expected " + expectedColumnsTaken + " but was " + wrapper.getColumnsTaken());
            }
            if (wrapper.getInventoryHandler().size() != expectedInventorySize) {
                return Optional.of(phase + " inventory size expected " + expectedInventorySize + " but was " + wrapper.getInventoryHandler().size());
            }
            int expectedSlotsOnLine = expectedInventorySize / backpackContainer.getNumberOfRows();
            if (storageScreen.getSlotsOnLine() != expectedSlotsOnLine) {
                return Optional.of(phase + " screen slots on line expected " + expectedSlotsOnLine + " but was " + storageScreen.getSlotsOnLine());
            }
            boolean tankInUpgradeSlot = wrapper.getUpgradeHandler().getStackInSlot(1).is(ModItems.TANK_UPGRADE.get());
            if (tankInUpgradeSlot != expectTankInUpgradeSlot) {
                return Optional.of(phase + " tank upgrade slot presence expected " + expectTankInUpgradeSlot + " but was " + tankInUpgradeSlot);
            }
            Optional<String> layoutError = capturedMobLayoutError(wrapper);
            if (layoutError.isPresent()) {
                return Optional.of(phase + " " + layoutError.get());
            }
            int stackCount = snapshotStacks(wrapper.getInventoryHandler()).values().stream().mapToInt(Integer::intValue).sum();
            if (stackCount != 27) {
                return Optional.of(phase + " stack count expected 27 but was " + stackCount);
            }
            return Optional.empty();
        }

        private static String regressionJson(boolean ok, String error) {
            return "{\"ok\":" + ok + ",\"error\":\"" + escapeJson(error) + "\"}";
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

        private record PlacedColumnUpgradeClickExpectation(int expectedColumnsTaken, int expectedStorageSlots) {
        }

        private record PlacedColumnUpgradeState(int columnsTaken, int storageSlots, int inventoryHandlerSlots, boolean upgradeSlotEmpty, boolean carriedNotEmpty) {
            private boolean matches(PlacedColumnUpgradeClickExpectation expectation) {
                return columnsTaken == expectation.expectedColumnsTaken() && storageSlots == expectation.expectedStorageSlots()
                        && inventoryHandlerSlots == expectation.expectedStorageSlots() && upgradeSlotEmpty && carriedNotEmpty;
            }
        }

        private record SubMobCatcherRegressionState(String context, int storageSlots, boolean slot0Backpack, int currentMobCount, @Nullable String currentMobId,
                int nestedMobCount, @Nullable String nestedMobId) {
            private boolean parentMatches() {
                return BackpackContext.ContextType.ITEM_BACKPACK.name().equals(context) && slot0Backpack && storageSlots == 81 && currentMobCount == 1
                        && SUB_MOB_CATCHER_PARENT_MOB_ID.toString().equals(currentMobId) && nestedMobCount == 1
                        && SUB_MOB_CATCHER_SUB_MOB_ID.toString().equals(nestedMobId);
            }

            private boolean subMatches() {
                return BackpackContext.ContextType.ITEM_SUB_BACKPACK.name().equals(context) && storageSlots == 144 && currentMobCount == 1
                        && SUB_MOB_CATCHER_SUB_MOB_ID.toString().equals(currentMobId);
            }
        }

        private enum BackpackGuiRegressionContext {
            PLACED("placed"),
            CURIOS("curios"),
            SUB("sub");

            private final String jsonName;

            BackpackGuiRegressionContext(String jsonName) {
                this.jsonName = jsonName;
            }

            private String jsonName() {
                return jsonName;
            }

            private static BackpackGuiRegressionContext fromName(String name) {
                for (BackpackGuiRegressionContext context : values()) {
                    if (context.jsonName.equals(name)) {
                        return context;
                    }
                }
                throw new IllegalArgumentException("Unknown backpack GUI regression context " + name);
            }
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
