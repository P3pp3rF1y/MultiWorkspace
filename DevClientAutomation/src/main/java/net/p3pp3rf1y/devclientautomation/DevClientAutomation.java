package net.p3pp3rf1y.devclientautomation;

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
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
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
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
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
