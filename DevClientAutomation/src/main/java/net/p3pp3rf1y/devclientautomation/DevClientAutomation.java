package net.p3pp3rf1y.devclientautomation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.demo.DemoCommand;
import net.p3pp3rf1y.devclientautomation.recipeviewer.RecipeViewerAutomationManager;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherHandler;
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
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageBoat;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageMinecart;
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
		DemoCommand.init();
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
				httpServer.createContext("/backpack/storage-gui-regressions", this::backpackStorageGuiRegressions);
				httpServer.createContext("/backpack/quick-move-column-upgrade-regression", this::backpackQuickMoveColumnUpgradeRegression);
				httpServer.createContext("/backpack/quick-move-column-upgrade-in-regression", this::backpackQuickMoveColumnUpgradeInRegression);
				httpServer.createContext("/backpack/gui-regression/run", this::backpackGuiRegressionRun);
				httpServer.createContext("/backpack/remote-upgrade-slot-regression", this::backpackRemoteUpgradeSlotRegression);
				httpServer.createContext("/storage/controller-filter-regressions", this::storageControllerFilterRegressions);
				httpServer.createContext("/storage/controller-ae2-profile-setup", this::storageControllerAe2ProfileSetup);
				httpServer.createContext("/storage/controller-ae2-profile-simulate-query", this::storageControllerAe2ProfileSimulateQuery);
				httpServer.createContext("/creative-tabs/render-check", this::creativeTabsRenderCheck);
				httpServer.createContext("/backpack/mob-catcher-capture-effect-check", this::mobCatcherCaptureEffectCheck);
				httpServer.createContext("/storage/block-gui-smoke", this::storageBlockGuiSmoke);
				httpServer.createContext("/storage/paintbrush-smoke", this::storagePaintbrushSmoke);
				httpServer.createContext("/storage/stash-left-click-regression", this::storageStashLeftClickRegression);
				httpServer.createContext("/storage-in-motion/entity-open-check", this::storageInMotionEntityOpenCheck);
				httpServer.createContext("/port/content-registry-check", this::portContentRegistryCheck);
				httpServer.createContext("/recipe-viewer/state", this::recipeViewerState);
				httpServer.createContext("/recipe-viewer/search", this::recipeViewerSearch);
				httpServer.createContext("/recipe-viewer/open", this::recipeViewerOpen);
				httpServer.createContext("/recipe-viewer/query", this::recipeViewerQuery);
httpServer.createContext("/recipe-viewer/backpack-crafting-transfer", this::recipeViewerBackpackCraftingTransfer);
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
				Screenshot.takeScreenshot(Minecraft.getInstance().gameRenderer.mainRenderTarget(), image -> {
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

		private void backpackStorageGuiRegressions(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, StorageGuiRegressionRunner::run);
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

		private void storageControllerFilterRegressions(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String mode = extractString(body, "mode").orElse("run");
			int runs = extractInt(body, "runs").orElse(1);
			boolean profileCapacity = extractBoolean(body, "profileCapacity").orElse(false);
			if ("profile".equals(mode)) {
				sendJsonHandling(exchange, () -> runOnServer(player -> profileStorageControllerFilterRegressions(player, runs)));
				return;
			}
			sendJsonHandling(exchange, () -> runOnServer(player -> runStorageControllerFilterRegressions(player, !"setup".equals(mode), profileCapacity)));
		}

		private void storageControllerAe2ProfileSetup(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String preset = extractString(body, "preset").orElse("lagworld").toLowerCase(Locale.ROOT);
			int columns = Math.max(1, Math.min(16, extractInt(body, "columns").orElse(12)));
			int rows = Math.max(1, Math.min(12, extractInt(body, "rows").orElse(9)));
			int fillSlotsPerStorage = Math.max(1, extractInt(body, "fillSlotsPerStorage").orElse(81));
			String distribution = extractString(body, "distribution").orElse("scattered").toLowerCase(Locale.ROOT);
			int sharedPoolSize = Math.max(1, Math.min(512, extractInt(body, "sharedPoolSize").orElse("hotspot".equals(distribution) ? 32 : 64)));
			boolean includeCrafting = extractBoolean(body, "includeCrafting").orElse(true);
			boolean giveItems = extractBoolean(body, "giveItems").orElse(true);
			sendJsonHandling(exchange, () -> runOnServer(player -> setupControllerAe2Profile(player, preset, columns, rows, fillSlotsPerStorage, distribution,
					sharedPoolSize, includeCrafting, giveItems)));
		}

		private void storageControllerAe2ProfileSimulateQuery(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int iterations = Math.max(1, Math.min(20, extractInt(body, "iterations").orElse(1)));
			int maxSimulations = Math.max(1, Math.min(10_000, extractInt(body, "maxSimulations").orElse(512)));
			Optional<Integer> controllerX = extractInt(body, "controllerX");
			Optional<Integer> controllerY = extractInt(body, "controllerY");
			Optional<Integer> controllerZ = extractInt(body, "controllerZ");
			sendJsonHandling(exchange,
					() -> runOnServer(player -> simulateControllerAe2ProfileQuery(player, iterations, maxSimulations, controllerX, controllerY, controllerZ)));
		}

		private void creativeTabsRenderCheck(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, this::runCreativeTabsRenderCheck);
		}

		private String runCreativeTabsRenderCheck() {
			List<CreativeTabRenderTarget> targets = runOnClient(this::openCreativeScreenForRenderCheck);
			JsonArray results = new JsonArray();
			for (CreativeTabRenderTarget target : targets) {
				CreativeTabRenderSelection selection = runOnClient(() -> selectCreativeTabForRenderCheck(target.tab()));
				sleep(200);
				for (int row = 0; row <= selection.scrollRows(); row++) {
					int rowIndex = row;
					runOnClient(() -> scrollCreativeTabForRenderCheck(rowIndex, selection.scrollRows()));
					sleep(100);
				}

				JsonObject result = new JsonObject();
				result.addProperty("id", target.id().toString());
				result.addProperty("title", target.title());
				result.addProperty("items", selection.itemCount());
				result.addProperty("scrollRows", selection.scrollRows());
				results.add(result);
			}

			return "{\"ok\":true,\"tabs\":" + results + "}";
		}

		private List<CreativeTabRenderTarget> openCreativeScreenForRenderCheck() {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.player.connection == null) {
				throw new IllegalStateException("Client player is not available for creative tab render check");
			}

			CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(minecraft.player, minecraft.player.connection.enabledFeatures(), false);
			minecraft.gui.setScreen(screen);

			Set<String> targetNamespaces = Set.of("sophisticatedbackpacks", "sophisticatedstorage", "sophisticatedstorageinmotion", "reliquary");
			List<CreativeTabRenderTarget> targets = new ArrayList<>();
			for (CreativeModeTab tab : net.neoforged.neoforge.common.CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
				Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
				if (id != null && targetNamespaces.contains(id.getNamespace()) && tab.hasAnyItems()) {
					targets.add(new CreativeTabRenderTarget(id, tab.getDisplayName().getString(), tab));
				}
			}

			if (targets.isEmpty()) {
				throw new IllegalStateException("No Sophisticated/Reliquary creative tabs with items were found");
			}
			return targets;
		}

		private CreativeTabRenderSelection selectCreativeTabForRenderCheck(CreativeModeTab tab) {
			CreativeModeInventoryScreen screen = currentCreativeScreen();
			try {
				Method selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
				selectTab.setAccessible(true);
				selectTab.invoke(screen, tab);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Failed to select creative tab " + tab.getDisplayName().getString(), e);
			}
			CreativeModeInventoryScreen.ItemPickerMenu menu = screen.getMenu();
			menu.scrollTo(0);
			int itemCount = menu.items.size();
			int scrollRows = Math.max(0, (itemCount + 8) / 9 - 5);
			return new CreativeTabRenderSelection(itemCount, scrollRows);
		}

		private Boolean scrollCreativeTabForRenderCheck(int rowIndex, int scrollRows) {
			CreativeModeInventoryScreen screen = currentCreativeScreen();
			screen.getMenu().scrollTo(scrollRows == 0 ? 0 : rowIndex / (float) scrollRows);
			return true;
		}

		private CreativeModeInventoryScreen currentCreativeScreen() {
			Screen screen = Minecraft.getInstance().gui.screen();
			if (!(screen instanceof CreativeModeInventoryScreen creativeModeInventoryScreen)) {
				throw new IllegalStateException("Creative inventory screen is not open");
			}
			return creativeModeInventoryScreen;
		}

		private record CreativeTabRenderTarget(Identifier id, String title, CreativeModeTab tab) {
		}

		private record CreativeTabRenderSelection(int itemCount, int scrollRows) {
		}

		private void mobCatcherCaptureEffectCheck(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::runMobCatcherCaptureEffectCheck));
		}

		private void storageBlockGuiSmoke(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, this::runStorageBlockGuiSmoke);
		}

		private void storagePaintbrushSmoke(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::runStoragePaintbrushSmoke));
		}

		private void storageStashLeftClickRegression(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::runStorageStashLeftClickRegression));
		}

		private void storageInMotionEntityOpenCheck(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, this::runStorageInMotionEntityOpenCheck);
		}

		private void portContentRegistryCheck(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, this::runPortContentRegistryCheck);
		}

		private String runMobCatcherCaptureEffectCheck(ServerPlayer player) {
			player.getInventory().clearContent();

			ItemStack backpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
			IBackpackWrapper wrapper = BackpackWrapper.fromStackNoCache(backpack);
			wrapper.setSlotNumbers(81, 3);
			wrapper.getInventoryHandler().saveInventory();
			wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
			wrapper.getUpgradeHandler().saveInventory();

			player.getInventory().setItem(0, backpack);
			player.getInventory().setSelectedSlot(0);
			player.getInventory().setChanged();

			ServerLevel level = (ServerLevel) player.level();
			LivingEntity chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.EVENT);
			if (chicken == null) {
				throw new IllegalStateException("Failed to create chicken for mob catcher capture effect check");
			}

			Vec3 spawnPosition = player.position().add(player.getLookAngle().normalize().scale(2D));
			chicken.snapTo(spawnPosition.x, player.getY(), spawnPosition.z, player.getYRot() + 180F, 0F);
			if (!level.addFreshEntity(chicken)) {
				throw new IllegalStateException("Failed to spawn chicken for mob catcher capture effect check");
			}

			MobCatcherHandler.tryCapture(player, InteractionHand.MAIN_HAND, chicken);
			boolean captured = !MobCatcherStorage.getCapturedMobs(BackpackWrapper.fromStackNoCache(player.getInventory().getItem(0))).isEmpty();
			return "{\"ok\":" + captured + ",\"entity\":\"minecraft:chicken\"}";
		}

		private String runStorageBlockGuiSmoke() {
			StorageBlockGuiSpec[] specs = {
					new StorageBlockGuiSpec("diamond_barrel", net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_BARREL_ITEM.get())};

			JsonArray results = new JsonArray();
			for (int i = 0; i < specs.length; i++) {
				StorageBlockGuiSpec spec = specs[i];
				int index = i;
				StorageBlockGuiExpected expected = runOnServer(player -> openStorageBlockGuiSmoke(player, spec, index));
				StorageBlockGuiState state = waitForStorageBlockGuiSmoke(spec.name(), expected.storageSlots());
				closeCurrentContainer();

				JsonObject result = new JsonObject();
				result.addProperty("name", spec.name());
				result.addProperty("expectedStorageSlots", expected.storageSlots());
				result.addProperty("storageSlots", state.storageSlots());
				result.addProperty("upgradeSlots", state.upgradeSlots());
				result.addProperty("screen", state.screenClass());
				results.add(result);
			}

			return "{\"ok\":true,\"storages\":" + results + "}";
		}

		private StorageBlockGuiExpected openStorageBlockGuiSmoke(ServerPlayer player, StorageBlockGuiSpec spec, int index) {
			ServerLevel level = (ServerLevel) player.level();
			BlockPos pos = regressionBasePos(player).offset(index * 2, 0, 0);
			cleanupBlockArea(level, pos);
			placeBlockWithItem(level, player, pos, new ItemStack(spec.item()));

			StorageBlockEntity storage = getStorage(level, pos);
			InventoryHandler inventory = storage.getStorageWrapper().getInventoryHandler();
			if (inventory.size() <= 0) {
				throw new IllegalStateException("Storage " + spec.name() + " has no inventory slots");
			}
			inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
			inventory.saveInventory();

			int expectedStorageSlots = inventory.size();
			player.openMenu(new SimpleMenuProvider((windowId, playerInventory,
					openPlayer) -> new net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu(windowId, openPlayer, pos),
					Component.literal("Storage smoke " + spec.name())), pos);
			return new StorageBlockGuiExpected(expectedStorageSlots, storage.getStorageWrapper().getUpgradeHandler().size());
		}

		private StorageBlockGuiState waitForStorageBlockGuiSmoke(String name, int expectedStorageSlots) {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			StorageBlockGuiState lastState = null;
			while (System.nanoTime() < deadline) {
				lastState = runOnClient(this::getStorageBlockGuiState);
				if (lastState.matches(expectedStorageSlots)) {
					return lastState;
				}
				sleep(50);
			}
			throw new IllegalStateException("Storage GUI did not open for " + name + "; last=" + lastState);
		}

		private StorageBlockGuiState getStorageBlockGuiState() {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || !(minecraft.player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
				return new StorageBlockGuiState(0, 0, "");
			}
			Screen screen = minecraft.gui.screen();
			return new StorageBlockGuiState(menu.getNumberOfStorageInventorySlots(), menu.getNumberOfUpgradeSlots(),
					screen == null ? "" : screen.getClass().getSimpleName());
		}

		private String runStoragePaintbrushSmoke(ServerPlayer player) {
			ServerLevel level = (ServerLevel) player.level();
			BlockPos pos = regressionBasePos(player).offset(0, 0, 8);
			cleanupBlockArea(level, pos);
			placeBlockWithItem(level, player, pos, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_BARREL_ITEM.get()));

			StorageBlockEntity storage = getStorage(level, pos);
			int mainColor = DyeColor.RED.getTextureDiffuseColor();
			int accentColor = DyeColor.BLUE.getTextureDiffuseColor();
			ItemStack paintbrush = new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModItems.PAINTBRUSH.get());
			net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem.setMainColor(paintbrush, mainColor);
			net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem.setAccentColor(paintbrush, accentColor);

			player.getInventory().clearContent();
			player.getInventory().setSelectedSlot(0);
			player.getInventory().setItem(0, paintbrush);
			player.getInventory().add(new ItemStack(Items.DYE.red(), 64));
			player.getInventory().add(new ItemStack(Items.DYE.green(), 64));
			player.getInventory().add(new ItemStack(Items.DYE.blue(), 64));
			player.getInventory().setChanged();

			boolean painted = net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem.setColors(player, paintbrush, storage.getStorageWrapper(), null);

			int actualMainColor = storage.getStorageWrapper().getMainColor();
			int actualAccentColor = storage.getStorageWrapper().getAccentColor();
			boolean ok = painted && actualMainColor == mainColor && actualAccentColor == accentColor;
			return "{\"ok\":" + ok + ",\"painted\":" + painted + ",\"mainColor\":" + actualMainColor + ",\"accentColor\":" + actualAccentColor + "}";
		}

		private String runStorageStashLeftClickRegression(ServerPlayer player) {
			JsonArray results = new JsonArray();
			results.add(runStorageStashLeftClickCase(player, "backpack", () -> new ItemStack(ModItems.GOLD_BACKPACK.get())));
			results.add(runStorageStashLeftClickCase(player, "shulker_box",
					() -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get())));
			results.add(runStorageStashLeftClickCase(player, "moving_shulker_box", () -> {
				ItemStack movingStorage = new ItemStack(net.p3pp3rf1y.sophisticatedstorageinmotion.init.ModItems.STORAGE_MINECART.get());
				net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem.setStorageItem(movingStorage,
						new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()));
				return movingStorage;
			}));

			JsonObject response = new JsonObject();
			response.addProperty("ok", true);
			response.add("cases", results);
			return response.toString();
		}

		private JsonObject runStorageStashLeftClickCase(ServerPlayer player, String name, Supplier<ItemStack> storageStackSupplier) {
			assertStorageInCursorLeftClickStashesSlotItem(player, name, storageStackSupplier.get());
			assertStorageInCursorRightClickDoesNotStashSlotItem(player, name, storageStackSupplier.get());
			assertCarriedItemLeftClickStashesIntoSlottedStorage(player, name, storageStackSupplier.get());
			assertCarriedItemRightClickDoesNotStashIntoSlottedStorage(player, name, storageStackSupplier.get());

			JsonObject result = new JsonObject();
			result.addProperty("name", name);
			result.addProperty("passed", true);
			return result;
		}

		private void assertStorageInCursorLeftClickStashesSlotItem(ServerPlayer player, String name, ItemStack storageStack) {
			Slot slot = slotWith(new ItemStack(Items.DIAMOND, 3));
			if (!storageStack.overrideStackedOnOther(slot, ClickAction.PRIMARY, player)) {
				throw new IllegalStateException(name + " did not handle left-click slot stashing with storage in cursor");
			}
			if (!slot.getItem().isEmpty()) {
				throw new IllegalStateException(name + " left-click slot stashing left items in clicked slot: " + slot.getItem());
			}
		}

		private void assertStorageInCursorRightClickDoesNotStashSlotItem(ServerPlayer player, String name, ItemStack storageStack) {
			Slot slot = slotWith(new ItemStack(Items.DIAMOND, 3));
			boolean handled = storageStack.overrideStackedOnOther(slot, ClickAction.SECONDARY, player);
			if (handled || slot.getItem().getCount() != 3) {
				throw new IllegalStateException(name + " still stashed slot item on right-click with storage in cursor");
			}
		}

		private void assertCarriedItemLeftClickStashesIntoSlottedStorage(ServerPlayer player, String name, ItemStack storageStack) {
			Slot slot = slotWith(storageStack);
			ItemStack[] carried = {new ItemStack(Items.EMERALD, 4)};
			if (!slot.getItem().overrideOtherStackedOnMe(carried[0], slot, ClickAction.PRIMARY, player,
					SlotAccess.of(() -> carried[0], stack -> carried[0] = stack))) {
				throw new IllegalStateException(name + " did not handle left-click cursor stashing into slotted storage");
			}
			if (!carried[0].isEmpty()) {
				throw new IllegalStateException(name + " left-click cursor stashing left carried items: " + carried[0]);
			}
		}

		private void assertCarriedItemRightClickDoesNotStashIntoSlottedStorage(ServerPlayer player, String name, ItemStack storageStack) {
			Slot slot = slotWith(storageStack);
			ItemStack[] carried = {new ItemStack(Items.EMERALD, 4)};
			boolean handled = slot.getItem().overrideOtherStackedOnMe(carried[0], slot, ClickAction.SECONDARY, player,
					SlotAccess.of(() -> carried[0], stack -> carried[0] = stack));
			if (handled || carried[0].getCount() != 4) {
				throw new IllegalStateException(name + " still stashed carried item on right-click into slotted storage");
			}
		}

		private Slot slotWith(ItemStack stack) {
			SimpleContainer container = new SimpleContainer(1);
			container.setItem(0, stack);
			return new Slot(container, 0, 0, 0);
		}

		private String runStorageInMotionEntityOpenCheck() {
			runOnServer(this::openStorageMinecartRegression);
			MovingStorageRegressionState minecartState = waitForMovingStorageMenu("StorageMinecart");
			closeCurrentContainer();

			runOnServer(this::openStorageBoatRegression);
			MovingStorageRegressionState boatState = waitForMovingStorageMenu("StorageBoat");
			closeCurrentContainer();

			return "{\"ok\":true,\"minecartSlots\":" + minecartState.storageSlots() + ",\"boatSlots\":" + boatState.storageSlots() + ","
					+ jsonProperty("minecartEntity", minecartState.entityClass()) + "," + jsonProperty("boatEntity", boatState.entityClass()) + "}";
		}

		private String runPortContentRegistryCheck() {
			String[] requiredMods = {"devclientautomation", "sophisticatedbackpacks", "sophisticatedcore", "sophisticatedstorage",
					"sophisticatedstorageinmotion", "sophisticateditemactions", "sophisticatedinventoryinteractions", "reliquary"};
			String[] requiredItems = {"sophisticatedbackpacks:backpack", "sophisticatedbackpacks:gold_backpack", "sophisticatedbackpacks:mob_catcher_upgrade",
					"sophisticatedstorage:paintbrush", "sophisticatedstorage:diamond_barrel", "sophisticatedstorage:limited_copper_barrel_1",
					"sophisticatedstorageinmotion:storage_minecart", "sophisticatedstorageinmotion:storage_boat", "reliquary:sojourner_staff",
					"reliquary:potion", "reliquary:mob_charm_belt"};

			JsonArray missingMods = new JsonArray();
			for (String modId : requiredMods) {
				if (!ModList.get().isLoaded(modId)) {
					missingMods.add(modId);
				}
			}

			JsonArray missingItems = new JsonArray();
			JsonArray checkedItems = new JsonArray();
			for (String itemId : requiredItems) {
				Identifier id = Identifier.parse(itemId);
				Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
				if (item.isEmpty() || new ItemStack(item.get()).isEmpty()) {
					missingItems.add(itemId);
				} else {
					checkedItems.add(itemId);
				}
			}

			JsonObject namespaceCounts = new JsonObject();
			for (String namespace : List.of("sophisticatedbackpacks", "sophisticatedstorage", "sophisticatedstorageinmotion", "reliquary")) {
				int count = 0;
				for (Item item : BuiltInRegistries.ITEM) {
					Identifier id = BuiltInRegistries.ITEM.getKey(item);
					if (id != null && namespace.equals(id.getNamespace())) {
						count++;
					}
				}
				namespaceCounts.addProperty(namespace, count);
			}

			boolean ok = missingMods.isEmpty() && missingItems.isEmpty();
			return "{\"ok\":" + ok + ",\"missingMods\":" + missingMods + ",\"missingItems\":" + missingItems + ",\"checkedItems\":" + checkedItems
					+ ",\"itemCounts\":" + namespaceCounts + "}";
		}

		private Boolean openStorageMinecartRegression(ServerPlayer player) {
			ServerLevel level = (ServerLevel) player.level();
			clearMovingStorageRegressionEntities(level, player);
			Vec3 pos = player.position().add(player.getLookAngle().normalize().scale(3D));
			StorageMinecart minecart = new StorageMinecart(level, pos.x(), player.getY(), pos.z());
			minecart.getStorageHolder().setStorageItemFrom(new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_BARREL_ITEM.get()), true);
			level.addFreshEntity(minecart);
			minecart.getStorageHolder().openContainerMenu(player);
			return true;
		}

		private Boolean openStorageBoatRegression(ServerPlayer player) {
			ServerLevel level = (ServerLevel) player.level();
			clearMovingStorageRegressionEntities(level, player);
			Vec3 pos = player.position().add(player.getLookAngle().normalize().scale(3D)).add(1D, 0D, 0D);
			StorageBoat boat = new StorageBoat(level, pos.x(), player.getY(), pos.z());
			boat.getStorageHolder().setStorageItemFrom(new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get()),
					true);
			level.addFreshEntity(boat);
			boat.getStorageHolder().openContainerMenu(player);
			return true;
		}

		private void clearMovingStorageRegressionEntities(ServerLevel level, ServerPlayer player) {
			AABB area = player.getBoundingBox().inflate(8D);
			level.getEntities(player, area, entity -> entity instanceof StorageMinecart || entity instanceof StorageBoat).forEach(Entity::discard);
			player.closeContainer();
		}

		private BlockPos regressionBasePos(ServerPlayer player) {
			return player.blockPosition().offset(5, 0, 5);
		}

		private void cleanupBlockArea(ServerLevel level, BlockPos pos) {
			for (BlockPos cleanupPos : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 2, 1))) {
				level.setBlock(cleanupPos, Blocks.AIR.defaultBlockState(), 3);
			}
		}

		private MovingStorageRegressionState waitForMovingStorageMenu(String expectedEntityClass) {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			MovingStorageRegressionState lastState = null;
			while (System.nanoTime() < deadline) {
				lastState = runOnClient(this::getMovingStorageRegressionState);
				if (lastState.matches(expectedEntityClass)) {
					return lastState;
				}
				sleep(50);
			}
			throw new IllegalStateException("Moving storage menu did not open for " + expectedEntityClass + "; last=" + lastState);
		}

		private MovingStorageRegressionState getMovingStorageRegressionState() {
			if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof MovingStorageContainerMenu<?> menu)) {
				return new MovingStorageRegressionState("", 0, false);
			}
			String entityClass = menu.getStorageEntity().map(entity -> entity.getClass().getSimpleName()).orElse("");
			return new MovingStorageRegressionState(entityClass, menu.getNumberOfStorageInventorySlots(), menu.getStorageEntity().isPresent());
		}

		private void closeCurrentContainer() {
			runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			runOnClient(() -> {
				if (Minecraft.getInstance().player != null) {
					Minecraft.getInstance().player.closeContainer();
				}
				Minecraft.getInstance().gui.setScreen(null);
				return true;
			});
			sleep(100);
		}

		private record MovingStorageRegressionState(String entityClass, int storageSlots, boolean hasEntity) {
			private boolean matches(String expectedEntityClass) {
				return hasEntity && entityClass.equals(expectedEntityClass) && storageSlots > 0;
			}
		}

		private String runBackpackGuiRegression(String body) {
			JsonObject request = body == null || body.isBlank() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject();
			String name = request.has("name") ? request.get("name").getAsString() : "unnamed";
			String type = request.has("type") ? request.get("type").getAsString() : "columnUpgradeSync";
			if ("subMobCatcherImmediateOpen".equals(type)) {
				return runSubMobCatcherImmediateOpenRegression(name);
			}
			if ("advancedCompactingHighStack".equals(type)) {
				return runAdvancedCompactingHighStackRegression(name, request);
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

		private String runAdvancedCompactingHighStackRegression(String name, JsonObject request) {
			resetBackpackGuiRegressionState();
			int firstSlotCount = getInt(request, "firstSlotCount", 16_384);
			int secondSlotCount = getInt(request, "secondSlotCount", 16_000);
			int triggerCount = getInt(request, "triggerCount", 8);
			int expectedNuggets = getInt(request, "expectedNuggets", 12_936);
			int expectedIngots = getInt(request, "expectedIngots", 4);
			int expectedBlocks = getInt(request, "expectedBlocks", 1_820);

			AdvancedCompactingHighStackRegressionResult result = runOnServer(player -> runAdvancedCompactingHighStackRegression(player, name, firstSlotCount,
					secondSlotCount, triggerCount, expectedNuggets, expectedIngots, expectedBlocks));
			return advancedCompactingHighStackRegressionJson(result);
		}

		private AdvancedCompactingHighStackRegressionResult runAdvancedCompactingHighStackRegression(ServerPlayer player, String name, int firstSlotCount,
				int secondSlotCount, int triggerCount, int expectedNuggets, int expectedIngots, int expectedBlocks) {
			player.getInventory().clearContent();

			ItemStack backpack = createBackpackStack(80);
			IBackpackWrapper wrapper = BackpackWrapper.fromStackNoCache(backpack);
			wrapper.setSlotNumbers(80, 5);
			wrapper.getInventoryHandler();
			wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_4.get()));
			wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_4.get()));
			wrapper.getUpgradeHandler().saveInventory();

			InventoryHandler inventory = wrapper.getInventoryHandler();
			inventory.setStackInSlot(0, new ItemStack(Items.IRON_NUGGET, firstSlotCount));
			inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, secondSlotCount));
			// Reserve deterministic destinations: slot 2 for blocks, slot 3 for the trigger nuggets, and no extra ingot slots.
			inventory.setStackInSlot(2, new ItemStack(Items.IRON_BLOCK));
			inventory.setStackInSlot(3, new ItemStack(Items.IRON_NUGGET));
			for (int slot = 4; slot < inventory.size(); slot++) {
				inventory.setStackInSlot(slot, new ItemStack(Items.STONE));
			}
			inventory.saveInventory();

			RecipeHelper.onRecipesUpdated(null);
			wrapper.getUpgradeHandler().setStackInSlot(2, new ItemStack(ModItems.ADVANCED_COMPACTING_UPGRADE.get()));
			wrapper.getUpgradeHandler().saveInventory();

			int inserted;
			try (Transaction tx = Transaction.openRoot()) {
				inserted = inventory.insert(ItemResource.of(new ItemStack(Items.IRON_NUGGET)), triggerCount, tx);
				tx.commit();
			}
			int insertRemainder = triggerCount - inserted;

			int actualNuggets = countItems(inventory, Items.IRON_NUGGET) - 1;
			int actualIngots = countItems(inventory, Items.IRON_INGOT);
			int actualBlocks = countItems(inventory, Items.IRON_BLOCK) - 1;
			boolean passed = insertRemainder == 0 && actualNuggets == expectedNuggets && actualIngots == expectedIngots && actualBlocks == expectedBlocks;

			inventory.saveInventory();
			player.getInventory().setItem(0, backpack);
			player.getInventory().setChanged();

			String error = passed ? null : "Unexpected compacting result";
			return new AdvancedCompactingHighStackRegressionResult(name, passed, firstSlotCount, secondSlotCount, triggerCount, expectedNuggets, actualNuggets,
					expectedIngots, actualIngots, expectedBlocks, actualBlocks, insertRemainder, error);
		}

		private String advancedCompactingHighStackRegressionJson(AdvancedCompactingHighStackRegressionResult result) {
			return "{\"ok\":" + result.passed() + "," + jsonProperty("name", result.name()) + ",\"firstSlotCount\":" + result.firstSlotCount()
					+ ",\"secondSlotCount\":" + result.secondSlotCount() + ",\"triggerCount\":" + result.triggerCount() + ",\"expectedNuggets\":"
					+ result.expectedNuggets() + ",\"actualNuggets\":" + result.actualNuggets() + ",\"expectedIngots\":" + result.expectedIngots()
					+ ",\"actualIngots\":" + result.actualIngots() + ",\"expectedBlocks\":" + result.expectedBlocks() + ",\"actualBlocks\":"
					+ result.actualBlocks() + ",\"insertRemainder\":" + result.insertRemainder() + "," + jsonProperty("error", result.error()) + "}";
		}

		private String runSubMobCatcherImmediateOpenRegression(String name) {
			try {
				resetBackpackGuiRegressionState();
				runOnServer(this::setupParentMobCatcherBackpackRegression);
				runOnClient(this::setupClientParentMobCatcherBackpackRegression);
				runOnServer(this::openParentMobCatcherBackpackRegression);
				waitForOpenParentBackpackMenu();

				runOnServer(this::insertMobCatcherSubBackpackIntoOpenParent);

				SubMobCatcherRegressionState parentState = waitForParentMobCatcherRegressionState();
				if (!parentState.parentContainerMatchesBeforeSubOpen()) {
					return subMobCatcherRegressionJson(name, false, parentState, parentState,
							"Parent backpack mob catcher data changed after inserting sub backpack");
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
					Minecraft.getInstance().gui.setScreen(null);
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
				if (runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
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

		private SubMobCatcherRegressionState waitForParentMobCatcherRegressionState() {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			SubMobCatcherRegressionState lastState = null;
			RuntimeException lastError = null;
			do {
				try {
					lastState = runOnClient(this::getParentMobCatcherRegressionState);
					if (lastState.parentContainerMatchesBeforeSubOpen()) {
						return lastState;
					}
				} catch (RuntimeException e) {
					lastError = e;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);

			if (lastState != null) {
				return lastState;
			}
			throw lastError == null ? new IllegalStateException("Timed out waiting for parent backpack mob catcher data to sync") : lastError;
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
			Minecraft.getInstance().level.setBlock(pos, ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING,
					Minecraft.getInstance().player.getDirection().getOpposite()), 3);
			BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, BackpackBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Failed to create client regression backpack block"));
			backpackBlockEntity.setBackpack(createColumnUpgradeRegressionBackpack());
			return true;
		}

		private Boolean openPlacedBackpackColumnUpgradeRegression(ServerPlayer player) {
			ServerLevel level = (ServerLevel) player.level();
			BlockPos pos = getRegressionBackpackPos(player);

			BackpackContext.Block backpackContext = new BackpackContext.Block(pos);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Placed Column Regression")), backpackContext::toBuffer);
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
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Curios Column Regression")), backpackContext::toBuffer);
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
			Minecraft.getInstance().player.getInventory().getNonEquipmentItems().set(0,
					createMobCatcherRegressionBackpack(81, 3, SUB_MOB_CATCHER_PARENT_MOB_ID, 0, "Parent Pig"));
			Minecraft.getInstance().player.getInventory().setChanged();
			return true;
		}

		private Boolean openSubBackpackColumnUpgradeRegression(ServerPlayer player) {
			BackpackContext.ItemSubBackpack backpackContext = new BackpackContext.ItemSubBackpack(PlayerInventoryProvider.MAIN_INVENTORY, "", 0, false, 0,
					true);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Sub Column Regression")), backpackContext::toBuffer);
			return true;
		}

		private Boolean openParentBackpackColumnUpgradeRegression(ServerPlayer player) {
			BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Parent Column Regression")), backpackContext::toBuffer);
			return true;
		}

		private String setupBackpackCraftingTransferRegression(ServerPlayer player) {
			player.getInventory().clearContent();
			player.getInventory().setItem(0, createCraftingTransferRegressionBackpack());
			for (int slot = 1; slot <= 4; slot++) {
				player.getInventory().setItem(slot, new ItemStack(Items.OAK_PLANKS));
			}
			player.getInventory().setSelectedSlot(0);
			player.getInventory().setChanged();
			return "{\"ok\":true}";
		}



private Boolean setupClientBackpackCraftingTransferRegression() {
			Player player = Minecraft.getInstance().player;
			if (player == null) {
				throw new IllegalStateException("Client player is not available");
			}
			player.getInventory().clearContent();
			player.getInventory().setItem(0, createCraftingTransferRegressionBackpack());
			for (int slot = 1; slot <= 4; slot++) {
				player.getInventory().setItem(slot, new ItemStack(Items.OAK_PLANKS));
			}
			player.getInventory().setSelectedSlot(0);
			player.getInventory().setChanged();
			return true;
		}



private void waitForClientCraftingTransferBackpack() {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			do {
				if (runOnClient(() -> {
					Player player = Minecraft.getInstance().player;
					if (player == null) {
						return false;
					}
					ItemStack backpack = player.getInventory().getItem(0);
					return backpack.getItem() instanceof BackpackItem && BackpackWrapper.fromStackNoCache(backpack).getUpgradeHandler().getStackInSlot(0)
							.is(ModItems.CRAFTING_UPGRADE.get());
				})) {
					return;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);

			throw new IllegalStateException("Timed out waiting for client inventory slot 0 to contain crafting upgrade backpack");
		}



private ItemStack createCraftingTransferRegressionBackpack() {
			ItemStack backpack = createBackpackStack(80);
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStackNoCache(backpack);
			backpackWrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
			backpackWrapper.getUpgradeHandler().saveInventory();
			backpackWrapper.onContentsUpdated();
			return backpack;
		}



private Boolean openParentMobCatcherBackpackRegression(ServerPlayer player) {
			BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Mob Catcher Parent Regression")), backpackContext::toBuffer);
			return true;
		}

		private Boolean openParentBackpackCraftingTransferRegression(ServerPlayer player) {
			BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Crafting Transfer Regression")), backpackContext::toBuffer);
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
			MobCatcherStorage.addCapturedMob(backpackWrapper,
					new CapturedMob(mobId, Identifier.parse("minecraft:pig"), new CompoundTag(), mobSlot, 1, 1, 1, false, displayName, 10, 10));
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
				server.getCommands().performPrefixedCommand(
						server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER).withSuppressedOutput(),
						"curios add " + identifier + " " + player.getGameProfile().name() + " " + slots);
			}
			if (inventoryHandler.getSlotCount(player, identifier) < slots) {
				throw new IllegalStateException(
						"Unable to configure Curios slot " + identifier + "; slot count is " + inventoryHandler.getSlotCount(player, identifier));
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
				if (runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu && menu.getBlockPosition().isPresent())) {
					return;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);
			throw new IllegalStateException("Timed out waiting for placed backpack screen to open");
		}

		private void waitForOpenCuriosBackpackMenu() {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			do {
				if (runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu && menu.getBlockPosition().isEmpty())) {
					return;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);
			throw new IllegalStateException("Timed out waiting for curios backpack screen to open");
		}

		private void waitForOpenParentBackpackMenu() {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			do {
				if (runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
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
				if (runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
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

			Screen screen = Minecraft.getInstance().gui.screen();
			clickSlot(screen, slot);

			return new PlacedColumnUpgradeClickExpectation(expectedColumnsTaken, expectedStorageSlots);
		}

		private BackpackContainer getOpenPlacedBackpackMenu() {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen)
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
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

			Screen screen = Minecraft.getInstance().gui.screen();
			clickSlot(screen, slot);

			return new PlacedColumnUpgradeClickExpectation(expectedColumnsTaken, expectedStorageSlots);
		}

		private BackpackContainer getOpenCuriosBackpackMenu() {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen)
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
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

			Screen screen = Minecraft.getInstance().gui.screen();
			clickSlot(screen, slot);

			return new PlacedColumnUpgradeClickExpectation(expectedColumnsTaken, expectedStorageSlots);
		}

		private BackpackContainer getOpenSubBackpackMenu() {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen)
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
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

		private String placedColumnUpgradeRegressionJson(String name, boolean ok, PlacedColumnUpgradeClickExpectation expectation,
				PlacedColumnUpgradeState state, @Nullable String error) {
			return "{\"ok\":" + ok + "," + jsonProperty("name", name) + ",\"expectedColumnsTaken\":" + expectation.expectedColumnsTaken()
					+ ",\"actualColumnsTaken\":" + state.columnsTaken() + ",\"expectedStorageSlots\":" + expectation.expectedStorageSlots()
					+ ",\"actualStorageSlots\":" + state.storageSlots() + ",\"actualInventoryHandlerSlots\":" + state.inventoryHandlerSlots()
					+ ",\"upgradeSlotEmpty\":" + state.upgradeSlotEmpty() + ",\"carriedNotEmpty\":" + state.carriedNotEmpty() + ","
					+ jsonProperty("error", error) + "}";
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
					IBackpackWrapper nestedWrapper = menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK
							? getNestedBackpackWrapper(menu)
							: null;
					return getSubMobCatcherRegressionState(menu, nestedWrapper);
				}
			} catch (RuntimeException ignored) {
				// Return an empty state below so the regression response still explains the failure.
			}
			return new SubMobCatcherRegressionState("none", 0, false, 0, null, 0, null);
		}

		private BackpackContainer getOpenParentBackpackMenu() {
			if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen)
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
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
			return new SubMobCatcherRegressionState(menu.getBackpackContext().getType().name(), menu.getStorageWrapper().getInventoryHandler().size(),
					nestedWrapper != null, currentMobs.size(), currentMobs.isEmpty() ? null : currentMobs.get(0).id().toString(), nestedMobs.size(),
					nestedMobs.isEmpty() ? null : nestedMobs.get(0).id().toString());
		}

		private String subMobCatcherRegressionJson(String name, boolean ok, SubMobCatcherRegressionState parentState, SubMobCatcherRegressionState subState,
				@Nullable String error) {
			return "{\"ok\":" + ok + "," + jsonProperty("name", name) + "," + jsonProperty("parentContext", parentState.context()) + ",\"parentStorageSlots\":"
					+ parentState.storageSlots() + ",\"parentSlot0Backpack\":" + parentState.slot0Backpack() + ",\"parentMobCount\":"
					+ parentState.currentMobCount() + "," + jsonProperty("parentMobId", parentState.currentMobId()) + ",\"parentNestedMobCount\":"
					+ parentState.nestedMobCount() + "," + jsonProperty("parentNestedMobId", parentState.nestedMobId()) + ","
					+ jsonProperty("subContext", subState.context()) + ",\"subStorageSlots\":" + subState.storageSlots() + ",\"subMobCount\":"
					+ subState.currentMobCount() + "," + jsonProperty("subMobId", subState.currentMobId()) + "," + jsonProperty("error", error) + "}";
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
				minecraft.createWorldOpenFlows().openWorld(worldName, () -> {
				});
				return "{\"ok\":true,\"created\":false}";
			}

			LevelSettings levelSettings = new LevelSettings(worldName, GameType.CREATIVE,
					new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
			WorldOptions worldOptions = new WorldOptions(0L, false, false);
			minecraft.createWorldOpenFlows().createFreshLevel(worldName, levelSettings, worldOptions, AutomationServer::voidFlatDimensions, null);
			return "{\"ok\":true,\"created\":true}";
		}

		private static WorldDimensions voidFlatDimensions(HolderLookup.Provider registryAccess) {
			HolderGetter<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
			HolderGetter<PlacedFeature> placedFeatures = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
			FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct(List.of())), biomes.getOrThrow(Biomes.THE_VOID),
					FlatLevelGeneratorSettings.createLakesList(placedFeatures));
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
			sendJsonHandling(exchange, () -> runOnClient(() -> RecipeViewerAutomationManager.queryJson(body)));
		}

		private void recipeViewerBackpackCraftingTransfer(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			sendJsonHandling(exchange, () -> {
				runOnServer(this::setupBackpackCraftingTransferRegression);
				runOnClient(this::setupClientBackpackCraftingTransferRegression);
				waitForClientCraftingTransferBackpack();
				runOnServer(this::openParentBackpackCraftingTransferRegression);
				waitForOpenParentBackpackMenu();
				return runOnClient(() -> RecipeViewerAutomationManager.transferJson(body));
			});
		}


private String buildStateJson() {
			Minecraft minecraft = Minecraft.getInstance();
			Screen screen = minecraft.gui.screen();
			return "{" + jsonProperty("screenClass", screen == null ? null : screen.getClass().getName()) + ","
					+ jsonProperty("screenSimpleName", screen == null ? null : screen.getClass().getSimpleName()) + ","
					+ jsonProperty("screenTitle", screen == null ? null : screen.getTitle().getString()) + "," + "\"inWorld\":" + (minecraft.level != null)
					+ "," + "\"playerLoaded\":" + (minecraft.player != null) + "," + "\"windowWidth\":" + minecraft.getWindow().getWidth() + ","
					+ "\"windowHeight\":" + minecraft.getWindow().getHeight() + "," + "\"guiWidth\":" + minecraft.getWindow().getGuiScaledWidth() + ","
					+ "\"guiHeight\":" + minecraft.getWindow().getGuiScaledHeight() + "}";
		}

		private String buildScreenJson() {
			Minecraft minecraft = Minecraft.getInstance();
			Screen screen = minecraft.gui.screen();
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
						json.append('{').append("\"index\":").append(index).append(',').append(jsonProperty("type", widget.getClass().getName())).append(',')
								.append(jsonProperty("message", widget.getMessage().getString())).append(',').append("\"x\":").append(widget.getX()).append(',')
								.append("\"y\":").append(widget.getY()).append(',').append("\"width\":").append(widget.getWidth()).append(',')
								.append("\"height\":").append(widget.getHeight()).append(',').append("\"active\":").append(widget.active).append(',')
								.append("\"visible\":").append(widget.visible).append('}');
					}
					index++;
				}
			}
			json.append("]}");
			return json.toString();
		}

		private String clickWidget(String text, boolean contains, int button) {
			Minecraft minecraft = Minecraft.getInstance();
			Screen screen = minecraft.gui.screen();
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
			Screen screen = Minecraft.getInstance().gui.screen();
			if (screen == null || !screen.getClass().getSimpleName().equals("BackupConfirmScreen")) {
				return false;
			}
			for (GuiEventListener child : screen.children()) {
				if (child instanceof AbstractWidget widget && widget.visible && widget.active
						&& widget.getMessage().getString().equals("I know what I'm doing!")) {
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
			if (keyCode == GLFW.GLFW_KEY_E && minecraft.gui.screen() == null && minecraft.player != null) {
				minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
				return "{\"ok\":true,\"handled\":true}";
			}
			if (minecraft.gui.screen() != null) {
				boolean handled = minecraft.gui.screen().keyPressed(new KeyEvent(keyCode, 0, 0));
				if (!handled && keyCode == GLFW.GLFW_KEY_ESCAPE) {
					minecraft.gui.screen().onClose();
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
			if (minecraft.gui.screen() != null) {
				minecraft.gui.setScreen(null);
			}
			KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
			return "{\"ok\":true}";
		}

		private String useGameKey() {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.options.pauseOnLostFocus = false;
			if (minecraft.gui.screen() != null) {
				minecraft.gui.setScreen(null);
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
			if (minecraft.gui.screen() != null) {
				minecraft.gui.setScreen(null);
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
					+ jsonProperty("block", minecraft.level.getBlockState(pos).getBlock().toString()) + "," + "\"air\":"
					+ minecraft.level.getBlockState(pos).isAir() + "," + "\"distance\":" + distance + "," + "\"atLeastOneBlockAway\":" + (distance >= 1) + "}";
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
			if (minecraft.level == null || minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK
					|| minecraft.getSingleplayerServer() == null) {
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
						.append(result.beforeStacks()).append(",\"afterStacks\":").append(result.afterStacks()).append(',')
						.append(jsonProperty("error", result.error())).append('}');
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
					Item upgradeItem = BuiltInRegistries.ITEM.getOptional(upgradeName)
							.orElseThrow(() -> new IllegalArgumentException("Unknown upgrade " + upgradeName));
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
				capturedMobs[i] = new CapturedMobSpec(capturedMob.get("slot").getAsInt(), capturedMob.get("width").getAsInt(),
						capturedMob.get("height").getAsInt(), entityType);
			}
			return capturedMobs;
		}

		private ColumnUpgradeRegressionResult runColumnUpgradeRegressionScenario(ColumnUpgradeRegressionScenario scenario,
				ColumnUpgradeStackGenerator stackGenerator) {
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
				return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(),
						afterStacks.size(), capturedMobLayoutError.get() + " actual=" + afterCapturedMobs);
			}
			if (!beforeProtectedStacks.equals(afterProtectedStacks)) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(),
						afterStacks.size(), "protected slot stack changed");
			}
			if (!beforeProtectedSettings.equals(afterProtectedSettings)) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(),
						afterStacks.size(), "protected slot settings changed");
			}
			if (!beforeStableStacks.equals(afterStableStacks)) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(),
						afterStacks.size(), "stable slot stack changed");
			}
			if (!capturedMobSlotsMatch(wrapper, scenario.expectedCapturedMobSlots())) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(),
						afterStacks.size(),
						"captured mob slots mismatch expected=" + Arrays.toString(scenario.expectedCapturedMobSlots()) + " actual=" + afterCapturedMobs);
			}
			if (scenario.expectedFits()) {
				if (!beforeStacks.equals(afterStacks)) {
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(),
							"stack snapshot changed");
				}
				if (scenario.expectedCapturedMobSlots().length == 0 && !beforeCapturedMobs.equals(afterCapturedMobs)) {
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(),
							"captured mob snapshot changed");
				}
			} else if (!beforeStacks.equals(afterStacks)) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, false, false, beforeStacks.size(), afterStacks.size(),
						"blocked insertion mutated stacks");
			} else if (!beforeCapturedMobs.equals(afterCapturedMobs)) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, false, false, beforeStacks.size(), afterStacks.size(),
						"blocked insertion mutated captured mobs");
			}

			return new ColumnUpgradeRegressionResult(scenario.name(), true, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(),
					afterStacks.size(), null);
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
				MobCatcherStorage.addCapturedMob(wrapper,
						new CapturedMob(new UUID(0, i + 1), Identifier.parse(capturedMob.entityType()), new CompoundTag(), capturedMob.slot(),
								capturedMob.width(), capturedMob.height(), capturedMob.width() * capturedMob.height(), false, capturedMob.entityType(), 10,
								10));
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

			InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(wrapper.getInventoryLayoutParts(currentColumns, targetColumns), targetSlots,
					targetColumns, targetColumnsTaken < currentColumnsTaken);
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

		private int countItems(InventoryHandler inventory, Item item) {
			int count = 0;
			for (int slot = 0; slot < inventory.size(); slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				if (stack.is(item)) {
					count += stack.getCount();
				}
			}
			return count;
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
				Screen screen = minecraft.gui.screen();
				return screen != null && (screen.getClass().getName().equals(screenName) || screen.getClass().getSimpleName().equals(screenName));
			}
			if ("noScreen".equals(condition)) {
				return minecraft.gui.screen() == null;
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
				((BackpackItem) player.getInventory().getItem(player.getInventory().getSelectedSlot()).getItem()).use(player.level(), player,
						InteractionHand.MAIN_HAND);
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
			fillRegressionStacks(inventory, new int[]{4, 5, 6, 7, 8, 13, 14, 15, 16, 17, 22, 23, 24, 25, 26, 31, 32, 33, 34, 35, 44, 53, 54, 55, 56, 57, 62},
					new ColumnUpgradeStackGenerator(List.of(Blocks.OAK_LOG.asItem()), 2, 28));
			wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
			wrapper.getUpgradeHandler().saveInventory();
			MobCatcherStorage.addCapturedMob(wrapper,
					new CapturedMob(new UUID(0, 1), Identifier.parse("minecraft:pig"), new CompoundTag(), 0, 4, 3, 12, false, "minecraft:pig", 10, 10));
			MobCatcherStorage.addCapturedMob(wrapper,
					new CapturedMob(new UUID(0, 2), Identifier.parse("minecraft:pig"), new CompoundTag(), 27, 4, 3, 12, false, "minecraft:pig", 10, 10));
			MobCatcherStorage.addCapturedMob(wrapper,
					new CapturedMob(new UUID(0, 3), Identifier.parse("minecraft:pig"), new CompoundTag(), 40, 4, 3, 12, false, "minecraft:pig", 10, 10));
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
				Optional<String> error = runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof StorageScreenBase<?>
						&& Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer
								? Optional.empty()
								: Optional.of("backpack screen is not open"));
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
			for (int slotIndex = 0; slotIndex < menu.getInventorySlotsSize(); slotIndex++) {
				if (menu.getSlot(slotIndex).getItem().is(ModItems.TANK_UPGRADE.get())) {
					return slotIndex;
				}
			}
			return -1;
		}

		private Optional<String> quickMoveTankUpgradeFromPlayerInventory() {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.gui.screen() instanceof StorageScreenBase<?> storageScreen)
					|| !(minecraft.player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
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
			if (!(minecraft.gui.screen() instanceof StorageScreenBase<?> storageScreen)
					|| !(minecraft.player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
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
				Method handleInventoryMouseClick = StorageScreenBase.class.getDeclaredMethod("handleInventoryMouseClick", int.class, int.class,
						ContainerInput.class);
				handleInventoryMouseClick.setAccessible(true);
				handleInventoryMouseClick.invoke(storageScreen, slot, 0, ContainerInput.QUICK_MOVE);
			} catch (ReflectiveOperationException e) {
				return Optional.of("failed to invoke quick move click: " + e.getMessage());
			}
			return Optional.empty();
		}

		private Optional<String> waitForClientQuickMoveState(int expectedColumnsTaken, int expectedInventorySize, boolean expectTankInUpgradeSlot,
				String phase) {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			Optional<String> lastError = Optional.empty();
			while (System.nanoTime() < deadline) {
				lastError = runOnClient(() -> validateQuickMoveClientState(expectedColumnsTaken, expectedInventorySize, expectTankInUpgradeSlot, phase));
				if (lastError.isEmpty()) {
					return Optional.empty();
				}
				sleep(50);
			}
			String serverState = runOnServer(this::describeQuickMoveServerState);
			return Optional.of("timed out waiting for " + phase + ": " + lastError.orElse("unknown state") + "; server=" + serverState);
		}

		private String describeQuickMoveServerState(ServerPlayer player) {
			if (!(player.containerMenu instanceof BackpackContainer backpackContainer)) {
				return player.containerMenu.getClass().getName();
			}
			IBackpackWrapper wrapper = backpackContainer.getStorageWrapper();
			return "columnsTaken=" + wrapper.getColumnsTaken() + ",size=" + wrapper.getInventoryHandler().size() + ",tankUpgrade="
					+ wrapper.getUpgradeHandler().getStackInSlot(1).is(ModItems.TANK_UPGRADE.get()) + ",stacks="
					+ snapshotStacks(wrapper.getInventoryHandler()).values().stream().mapToInt(Integer::intValue).sum();
		}

		private Optional<String> validateQuickMoveClientState(int expectedColumnsTaken, int expectedInventorySize, boolean expectTankInUpgradeSlot,
				String phase) {
			if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer backpackContainer)) {
				return Optional.of(phase + " client backpack container is not open");
			}
			if (!(Minecraft.getInstance().gui.screen() instanceof StorageScreenBase<?> storageScreen)) {
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

		private String setupControllerAe2Profile(ServerPlayer player, String preset, int columns, int rows, int fillSlotsPerStorage, String distribution,
				int sharedPoolSize, boolean includeCrafting, boolean giveItems) {
			if (!List.of("lagworld", "matrix").contains(preset)) {
				throw new IllegalArgumentException("Unknown controller AE2 profile preset " + preset);
			}
			if (!List.of("sorted", "scattered", "hotspot").contains(distribution)) {
				throw new IllegalArgumentException("Unknown controller AE2 profile distribution " + distribution);
			}

			ServerLevel level = (ServerLevel) player.level();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 18);
			List<String> warnings = new ArrayList<>();

			List<BlockPos> storagePositions = "lagworld".equals(preset)
					? createControllerAe2LagWorldStoragePositions(controllerPos)
					: createControllerAe2StoragePositions(controllerPos, columns, rows);
			clearStorageControllerAe2ProfileArea(level, controllerPos, storagePositions);
			placeController(level, player, controllerPos);

			List<Item> seedItems = createControllerAe2SeedItems();
			List<Item> lagWorldSpreadItems = createControllerAe2LagWorldSpreadItems(seedItems);
			int filledSlots = 0;
			int nonEmptyStorages = 0;
			int actualStorageSlots = 0;
			for (int storageIndex = 0; storageIndex < storagePositions.size(); storageIndex++) {
				BlockPos storagePos = storagePositions.get(storageIndex);
				if ("lagworld".equals(preset)) {
					placeDoubleChest(level, player, storagePos);
				} else {
					placeBarrel(level, player, storagePos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_BARREL_ITEM.get());
				}
				StorageBlockEntity storage = getStorage(level, storagePos);
				InventoryHandler inventory = storage.getStorageWrapper().getInventoryHandler();
				int slotsToFill = "lagworld".equals(preset) ? getLagWorldFillSlotsForStorage(storageIndex) : fillSlotsPerStorage;
				actualStorageSlots += inventory.size();
				for (int slot = 0; slot < Math.min(slotsToFill, inventory.size()); slot++) {
					Item item = "lagworld".equals(preset)
							? getControllerAe2LagWorldSeedItem(seedItems, lagWorldSpreadItems, storageIndex, slot)
							: getControllerAe2SeedItem(seedItems, distribution, storageIndex, slot, fillSlotsPerStorage, sharedPoolSize);
					int count = "lagworld".equals(preset)
							? Math.min(new ItemStack(item).getMaxStackSize(), 8 + Math.floorMod(storageIndex * 11 + slot, 57))
							: Math.min(new ItemStack(item).getMaxStackSize(), 16 + Math.floorMod(storageIndex + slot, 49));
					insertStackIntoStorage(storage, new ItemStack(item, count));
				}
				int actualFilledThisStorage = getFilledInventorySlotCount(inventory);
				filledSlots += actualFilledThisStorage;
				if (actualFilledThisStorage > 0) {
					nonEmptyStorages++;
				}
				inventory.saveInventory();
				storage.getStorageWrapper().refreshInventoryForInputOutput();
			}

			ControllerBlockEntity controllerForRegistration = level
					.getBlockEntity(controllerPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
			if (controllerForRegistration != null) {
				storagePositions.forEach(controllerForRegistration::addStorage);
			}

			BlockPos storageBusPos;
			BlockPos craftingTerminalTarget;
			if ("lagworld".equals(preset)) {
				BlockPos cablePos = controllerPos.above();
				BlockPos cableExtensionPos = cablePos.east();
				BlockPos energyCellPos = cablePos.east(2);
				storageBusPos = cablePos;
				craftingTerminalTarget = cableExtensionPos;
				placeAe2ItemAsBlock(level, player, cablePos, "ae2:fluix_glass_cable", warnings);
				placeAe2ItemOnBlockFace(level, player, controllerPos, Direction.UP, "ae2:storage_bus", warnings);
				placeAe2ItemOnBlockFace(level, player, cablePos, Direction.NORTH, "ae2:terminal", warnings);
				placeAe2ItemOnBlockFace(level, player, cablePos, Direction.SOUTH, "ae2:pattern_encoding_terminal", warnings);
				placeAe2ItemAsBlock(level, player, cableExtensionPos, "ae2:fluix_glass_cable", warnings);
				placeAe2ItemOnBlockFace(level, player, cableExtensionPos, Direction.SOUTH, "ae2:crafting_terminal", warnings);
				placeAe2ItemAsBlock(level, player, energyCellPos, "ae2:creative_energy_cell", warnings);
				if (includeCrafting) {
					BlockPos patternProviderPos = cableExtensionPos.north();
					BlockPos molecularAssemblerPos = patternProviderPos.north();
					placeAe2ItemAsBlock(level, player, cableExtensionPos.above(), "ae2:1k_crafting_storage", warnings);
					placeAe2ItemAsBlock(level, player, cableExtensionPos.above(2), "ae2:crafting_accelerator", warnings);
					placeAe2ItemAsBlock(level, player, patternProviderPos, "ae2:pattern_provider", warnings);
					placeAe2ItemAsBlock(level, player, molecularAssemblerPos, "ae2:molecular_assembler", warnings);
				}
			} else {
				BlockPos cablePos = controllerPos.west(2);
				BlockPos cableExtensionPos = controllerPos.west(3);
				BlockPos energyCellPos = controllerPos.west(4);
				storageBusPos = controllerPos.west();
				craftingTerminalTarget = cablePos;
				placeAe2ItemOnBlockFace(level, player, controllerPos, Direction.WEST, "ae2:storage_bus", warnings);
				placeAe2ItemAsBlock(level, player, cablePos, "ae2:fluix_glass_cable", warnings);
				placeAe2ItemAsBlock(level, player, cableExtensionPos, "ae2:fluix_glass_cable", warnings);
				placeAe2ItemAsBlock(level, player, energyCellPos, "ae2:creative_energy_cell", warnings);
				placeAe2ItemOnBlockFace(level, player, cablePos, Direction.NORTH, "ae2:crafting_terminal", warnings);
				placeAe2ItemOnBlockFace(level, player, cablePos, Direction.SOUTH, "ae2:pattern_encoding_terminal", warnings);
				if (includeCrafting) {
					BlockPos patternProviderPos = cableExtensionPos.north();
					BlockPos molecularAssemblerPos = patternProviderPos.north();
					placeAe2ItemAsBlock(level, player, cableExtensionPos.above(), "ae2:1k_crafting_storage", warnings);
					placeAe2ItemAsBlock(level, player, cableExtensionPos.above(2), "ae2:crafting_accelerator", warnings);
					placeAe2ItemAsBlock(level, player, patternProviderPos, "ae2:pattern_provider", warnings);
					placeAe2ItemAsBlock(level, player, molecularAssemblerPos, "ae2:molecular_assembler", warnings);
				}
			}
			if (giveItems) {
				giveControllerAe2ProfileItems(player, warnings);
			}

			ControllerBlockEntity controller = level
					.getBlockEntity(controllerPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
			int connectedStorages = controller == null ? 0 : controller.getStoragePositions().size();
			int actualControllerSlots = controller == null ? 0 : controller.size();
			if (controller == null) {
				warnings.add("controller block entity missing at " + controllerPos);
			} else if (connectedStorages != storagePositions.size()) {
				warnings.add("expected " + storagePositions.size() + " connected storages, got " + connectedStorages + ": " + controller.getStoragePositions());
			}
			warnIfAir(level, storageBusPos, "AE2 storage bus/cable bus", warnings);
			warnIfAir(level, craftingTerminalTarget, "AE2 crafting terminal cable", warnings);

			StringBuilder json = new StringBuilder("{\"ok\":true").append(",\"preset\":\"").append(escapeJson(preset)).append("\"")
					.append(",\"controllerPos\":\"").append(controllerPos.toShortString()).append("\"").append(",\"storageBusPos\":\"")
					.append(storageBusPos.toShortString()).append("\"").append(",\"craftingTerminalTarget\":\"").append(craftingTerminalTarget.toShortString())
					.append("\"").append(",\"storageCount\":").append(storagePositions.size()).append(",\"connectedStorages\":").append(connectedStorages)
					.append(",\"nonEmptyStorages\":").append(nonEmptyStorages).append(",\"actualStorageSlots\":").append(actualStorageSlots)
					.append(",\"actualControllerSlots\":").append(actualControllerSlots).append(",\"filledSlots\":").append(filledSlots)
					.append(",\"warnings\":[");
			for (int i = 0; i < warnings.size(); i++) {
				if (i > 0) {
					json.append(',');
				}
				json.append('"').append(escapeJson(warnings.get(i))).append('"');
			}
			json.append("]}");
			return json.toString();
		}

		private String simulateControllerAe2ProfileQuery(ServerPlayer player, int iterations, int maxSimulations, Optional<Integer> controllerX,
				Optional<Integer> controllerY, Optional<Integer> controllerZ) {
			ServerLevel level = (ServerLevel) player.level();
			BlockPos controllerPos = controllerX.isPresent() && controllerY.isPresent() && controllerZ.isPresent()
					? new BlockPos(controllerX.get(), controllerY.get(), controllerZ.get())
					: player.blockPosition().offset(0, 0, 18);
			ControllerBlockEntity controller = level
					.getBlockEntity(controllerPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
					.orElseThrow(() -> new IllegalStateException("Missing controller at " + controllerPos));
			ResourceHandler<ItemResource> handler = controller;

			int nonEmptySlots = 0;
			int simulations = 0;
			int extracted = 0;
			long startedAt = System.nanoTime();
			for (int iteration = 0; iteration < iterations && simulations < maxSimulations; iteration++) {
				for (int slot = 0; slot < handler.size() && simulations < maxSimulations; slot++) {
					ItemResource resource = handler.getResource(slot);
					if (resource.isEmpty()) {
						continue;
					}
					if (iteration == 0) {
						nonEmptySlots++;
					}
					try (Transaction transaction = Transaction.openRoot()) {
						int amount = Math.min(handler.getAmountAsInt(slot), 1);
						int simulatedExtract = handler.extract(slot, resource, amount, transaction);
						if (simulatedExtract > 0) {
							extracted += simulatedExtract;
						}
					}
					simulations++;
				}
			}
			long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

			return new StringBuilder("{\"ok\":true").append(",\"scenario\":\"storage_controller_ae2_profile_simulate_query\"").append(",\"controllerPos\":\"")
					.append(controllerPos.toShortString()).append("\"").append(",\"controllerSlots\":").append(controller.size()).append(",\"nonEmptySlots\":")
					.append(nonEmptySlots).append(",\"iterations\":").append(iterations).append(",\"maxSimulations\":").append(maxSimulations)
					.append(",\"simulations\":").append(simulations).append(",\"simulatedExtractedItems\":").append(extracted).append(",\"elapsedMillis\":")
					.append(elapsedMillis).append("}").toString();
		}

		private String runStorageControllerFilterRegressions(ServerPlayer player, boolean runInserts, boolean profileCapacity) {
			ServerLevel level = (ServerLevel) player.level();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 12);
			List<String> failures = new ArrayList<>();
			long startedAt = System.nanoTime();

			clearStorageControllerFilterRegressionArea(level, controllerPos);
			placeController(level, player, controllerPos);

			List<BlockPos> overflowPositions = List.of(controllerPos.offset(1, 0, -1), controllerPos.offset(1, 0, 0), controllerPos.offset(1, 0, 1),
					controllerPos.offset(1, 0, 2));
			List<ControllerFilterStorageSpec> filterSpecs = List.of(
					new ControllerFilterStorageSpec("specific_amethyst", controllerPos.offset(2, 0, -1), Items.AMETHYST_SHARD, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("specific_honeycomb", controllerPos.offset(2, 0, 0), Items.HONEYCOMB, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("specific_echo_shard", controllerPos.offset(2, 0, 1), Items.ECHO_SHARD, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("mod_sophisticatedstorage", controllerPos.offset(3, 0, -1),
							net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_ITEM.get(), true, PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("mod_sophisticatedbackpacks", controllerPos.offset(3, 0, 0), ModItems.GOLD_BACKPACK.get(), true,
							PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("deny_feather", controllerPos.offset(3, 0, 1), Items.FEATHER, false, PrimaryMatch.ITEM));

			List<ControllerLockedStorageSpec> lockedSpecs = createControllerFilterLockedStorageSpecs(controllerPos);
			List<BlockPos> allPositions = new ArrayList<>();
			allPositions.addAll(overflowPositions);
			filterSpecs.forEach(spec -> allPositions.add(spec.pos()));
			lockedSpecs.forEach(spec -> allPositions.add(spec.pos()));
			Map<BlockPos, Item> barrelItems = createControllerFilterBarrelItems(overflowPositions, filterSpecs, lockedSpecs);

			allPositions.forEach(pos -> placeBarrel(level, player, pos,
					barrelItems.getOrDefault(pos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_BARREL_ITEM.get())));
			if (profileCapacity) {
				for (BlockPos pos : overflowPositions) {
					addControllerFilterProfileCapacity(getBarrelStorage(level, pos), 0);
				}
			}
			for (ControllerLockedStorageSpec spec : lockedSpecs) {
				StorageBlockEntity storage = getBarrelStorage(level, spec.pos());
				if (profileCapacity) {
					addControllerFilterProfileCapacity(storage, 0);
				}
				InventoryHandler inventory = storage.getStorageWrapper().getInventoryHandler();
				inventory.setStackInSlot(0, new ItemStack(spec.item(), 16 + spec.slot() % 32));
				inventory.saveInventory();
				if (!storage.isLocked()) {
					storage.toggleLock();
				}
			}
			for (ControllerFilterStorageSpec spec : filterSpecs) {
				configureControllerFilterStorage(getBarrelStorage(level, spec.pos()), spec, profileCapacity);
			}

			long setupMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

			ControllerBlockEntity controller = level
					.getBlockEntity(controllerPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
			if (controller == null) {
				failures.add("controller block entity missing at " + controllerPos);
				return buildStorageControllerFilterRegressionJson(false, setupMillis, 0, 0, 0, 0, 0, 0, 0, 0, failures);
			}

			int connectedStorages = controller.getStoragePositions().size();
			if (connectedStorages != allPositions.size()) {
				failures.add("expected " + allPositions.size() + " connected storages, got " + connectedStorages + ": " + controller.getStoragePositions());
			}
			assertStorageLockState(level, overflowPositions, false, "overflow", failures);
			assertStorageLockState(level, filterSpecs.stream().map(ControllerFilterStorageSpec::pos).toList(), false, "filtered", failures);
			assertStorageLockState(level, lockedSpecs.stream().map(ControllerLockedStorageSpec::pos).toList(), true, "locked", failures);

			Map<Item, Set<BlockPos>> lockedPositionsByItem = new HashMap<>();
			for (ControllerLockedStorageSpec spec : lockedSpecs) {
				lockedPositionsByItem.computeIfAbsent(spec.item(), item -> new HashSet<>()).add(spec.pos());
			}

			List<ControllerFilterInsertExpectation> expectations = createControllerFilterInsertExpectations(controllerPos, overflowPositions,
					lockedPositionsByItem);

			long insertStartedAt = System.nanoTime();
			int insertCalls = 0;
			long itemsInserted = 0;
			if (runInserts) {
				for (ControllerFilterInsertExpectation expectation : expectations) {
					ControllerFilterInsertStats stats = runControllerFilterInsertExpectation(level, controller, allPositions, expectation, failures);
					insertCalls += stats.calls();
					itemsInserted += stats.items();
				}
			}
			long insertMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - insertStartedAt);

			long verifyStartedAt = System.nanoTime();
			if (runInserts) {
				for (ControllerFilterStorageSpec spec : filterSpecs) {
					if (spec.allowList() && countItemInPositions(level, Set.of(spec.pos()), Items.FEATHER) > 0) {
						failures.add("allow-list filtered storage " + spec.name() + " received denied overflow test item");
					}
				}
			}
			long verifyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - verifyStartedAt);

			return buildStorageControllerFilterRegressionJson(failures.isEmpty(), setupMillis, insertMillis, verifyMillis, connectedStorages,
					lockedSpecs.size(), filterSpecs.size(), overflowPositions.size(), insertCalls, itemsInserted, failures);
		}

		private Map<BlockPos, Item> createControllerFilterBarrelItems(List<BlockPos> overflowPositions, List<ControllerFilterStorageSpec> filterSpecs,
				List<ControllerLockedStorageSpec> lockedSpecs) {
			Map<BlockPos, Item> barrelItems = new HashMap<>();
			List<Item> overflowBarrels = List.of(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_ITEM.get(),
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.IRON_BARREL_ITEM.get(),
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.GOLD_BARREL_ITEM.get(),
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_BARREL_ITEM.get());
			for (int i = 0; i < overflowPositions.size(); i++) {
				barrelItems.put(overflowPositions.get(i), overflowBarrels.get(i % overflowBarrels.size()));
			}
			List<Item> filterBarrels = List.of(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_BARREL_ITEM.get(),
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_BARREL_ITEM.get(),
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.GOLD_BARREL_ITEM.get(),
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_BARREL_ITEM.get(),
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_BARREL_ITEM.get(),
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_BARREL_ITEM.get());
			for (int i = 0; i < filterSpecs.size(); i++) {
				barrelItems.put(filterSpecs.get(i).pos(), filterBarrels.get(i % filterBarrels.size()));
			}
			lockedSpecs.forEach(spec -> barrelItems.put(spec.pos(), spec.barrelItem()));
			return barrelItems;
		}

		private String profileStorageControllerFilterRegressions(ServerPlayer player, int runs) {
			ServerLevel level = (ServerLevel) player.level();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 12);
			List<String> failures = new ArrayList<>();
			List<BlockPos> overflowPositions = List.of(controllerPos.offset(1, 0, -1), controllerPos.offset(1, 0, 0), controllerPos.offset(1, 0, 1),
					controllerPos.offset(1, 0, 2));
			List<ControllerFilterStorageSpec> filterSpecs = List.of(
					new ControllerFilterStorageSpec("specific_amethyst", controllerPos.offset(2, 0, -1), Items.AMETHYST_SHARD, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("specific_honeycomb", controllerPos.offset(2, 0, 0), Items.HONEYCOMB, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("specific_echo_shard", controllerPos.offset(2, 0, 1), Items.ECHO_SHARD, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("mod_sophisticatedstorage", controllerPos.offset(3, 0, -1),
							net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_ITEM.get(), true, PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("mod_sophisticatedbackpacks", controllerPos.offset(3, 0, 0), ModItems.GOLD_BACKPACK.get(), true,
							PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("deny_feather", controllerPos.offset(3, 0, 1), Items.FEATHER, false, PrimaryMatch.ITEM));
			List<ControllerLockedStorageSpec> lockedSpecs = createControllerFilterLockedStorageSpecs(controllerPos);
			List<BlockPos> allPositions = new ArrayList<>();
			allPositions.addAll(overflowPositions);
			filterSpecs.forEach(spec -> allPositions.add(spec.pos()));
			lockedSpecs.forEach(spec -> allPositions.add(spec.pos()));

			ControllerBlockEntity controller = level
					.getBlockEntity(controllerPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
			if (controller == null) {
				failures.add("controller block entity missing at " + controllerPos + "; run setup mode before profile mode");
				return buildStorageControllerFilterRegressionJson(false, 0, 0, 0, 0, lockedSpecs.size(), filterSpecs.size(), overflowPositions.size(), 0, 0,
						failures);
			}
			int connectedStorages = controller.getStoragePositions().size();
			if (connectedStorages != allPositions.size()) {
				failures.add("expected " + allPositions.size() + " connected storages, got " + connectedStorages + ": " + controller.getStoragePositions());
			}

			Map<Item, Set<BlockPos>> lockedPositionsByItem = new HashMap<>();
			for (ControllerLockedStorageSpec spec : lockedSpecs) {
				lockedPositionsByItem.computeIfAbsent(spec.item(), item -> new HashSet<>()).add(spec.pos());
			}
			List<ControllerFilterInsertExpectation> expectations = createControllerFilterInsertExpectations(controllerPos, overflowPositions,
					lockedPositionsByItem);

			long startedAt = System.nanoTime();
			int insertCalls = 0;
			long itemsInserted = 0;
			for (int run = 0; run < runs; run++) {
				for (ControllerFilterInsertExpectation expectation : expectations) {
					ControllerFilterInsertStats stats = runControllerFilterProfileExpectation(controller, expectation);
					insertCalls += stats.calls();
					itemsInserted += stats.items();
				}
			}
			long insertMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
			return buildStorageControllerFilterRegressionJson(failures.isEmpty(), 0, insertMillis, 0, connectedStorages, lockedSpecs.size(), filterSpecs.size(),
					overflowPositions.size(), insertCalls, itemsInserted, failures);
		}

		private void clearStorageControllerAe2ProfileArea(ServerLevel level, BlockPos controllerPos, List<BlockPos> storagePositions) {
			discardStorageControllerAe2ProfileItemEntities(level, controllerPos);
			int minX = controllerPos.getX() - 6;
			int maxX = controllerPos.getX() + 10;
			int minY = controllerPos.getY() - 1;
			int maxY = controllerPos.getY() + 8;
			int minZ = controllerPos.getZ() - 9;
			int maxZ = controllerPos.getZ() + 4;
			for (BlockPos storagePos : storagePositions) {
				minX = Math.min(minX, storagePos.getX() - 1);
				maxX = Math.max(maxX, storagePos.getX() + 1);
				minY = Math.min(minY, storagePos.getY() - 1);
				maxY = Math.max(maxY, storagePos.getY() + 2);
				minZ = Math.min(minZ, storagePos.getZ() - 1);
				maxZ = Math.max(maxZ, storagePos.getZ() + 1);
			}
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					for (int z = minZ; z <= maxZ; z++) {
						BlockPos pos = new BlockPos(x, y, z);
						if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
							storage.clearContent();
						}
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
			discardStorageControllerAe2ProfileItemEntities(level, controllerPos);
		}

		private void discardStorageControllerAe2ProfileItemEntities(ServerLevel level, BlockPos controllerPos) {
			AABB area = new AABB(controllerPos.getX() - 8, controllerPos.getY() - 3, controllerPos.getZ() - 11, controllerPos.getX() + 13,
					controllerPos.getY() + 10, controllerPos.getZ() + 6);
			for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
				itemEntity.discard();
			}
		}

		private List<BlockPos> createControllerAe2StoragePositions(BlockPos controllerPos, int columns, int rows) {
			List<BlockPos> positions = new ArrayList<>();
			for (int column = 1; column <= columns; column++) {
				for (int row = 0; row < rows; row++) {
					positions.add(controllerPos.offset(column, 0, row - rows / 2));
				}
			}
			return positions;
		}

		private List<BlockPos> createControllerAe2LagWorldStoragePositions(BlockPos controllerPos) {
			int[][] offsets = {{0, 0, -6}, {2, 0, -6}, {6, 0, -5}, {6, 0, -3}, {6, 0, -1}, {6, 1, -1}, {6, 1, -3}, {6, 2, -1}, {6, 2, -3}, {6, 3, -3},
					{6, 3, -1}, {6, 4, -1}, {6, 4, -3}, {6, 4, -5}, {4, 4, -6}, {4, 3, -6}, {4, 2, -6}, {4, 1, -6}, {4, 0, -6}, {2, 1, -6}, {2, 2, -6},
					{2, 3, -6}, {2, 4, -6}, {0, 4, -6}, {0, 3, -6}, {0, 2, -6}, {0, 1, -6}, {6, 1, -5}, {6, 2, -5}, {6, 3, -5}};
			List<BlockPos> positions = new ArrayList<>();
			for (int[] offset : offsets) {
				positions.add(controllerPos.offset(offset[0], offset[1], offset[2]));
			}
			return positions;
		}

		private int getLagWorldFillSlotsForStorage(int storageIndex) {
			return switch (storageIndex) {
				case 0 -> 236;
				case 4 -> 244;
				case 5 -> 64;
				case 11 -> 6;
				case 12 -> 28;
				case 14 -> 124;
				case 22 -> 79;
				case 23 -> 37;
				case 26 -> 384;
				default -> 0;
			};
		}

		private List<Item> createControllerAe2SeedItems() {
			List<Item> items = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR && new ItemStack(item).getMaxStackSize() > 1).toList();
			if (items.isEmpty()) {
				throw new IllegalStateException("No stackable items available for AE2 profile setup");
			}
			return items;
		}

		private Item getControllerAe2SeedItem(List<Item> seedItems, String distribution, int storageIndex, int slot, int slotsPerStorage, int sharedPoolSize) {
			int poolSize = Math.min(sharedPoolSize, seedItems.size());
			return switch (distribution) {
				case "sorted" -> seedItems.get(Math.floorMod(storageIndex * slotsPerStorage + slot, seedItems.size()));
				case "hotspot" -> seedItems.get(Math.floorMod(slot, poolSize));
				case "scattered" -> seedItems.get(Math.floorMod(storageIndex * 17 + slot, poolSize));
				default -> throw new IllegalArgumentException("Unknown controller AE2 profile distribution " + distribution);
			};
		}

		private List<Item> createControllerAe2LagWorldSpreadItems(List<Item> seedItems) {
			List<Item> spreadItems = new ArrayList<>();
			List.of("minecraft:coal", "minecraft:cobblestone", "minecraft:emerald", "minecraft:spider_eye", "minecraft:coal_ore", "minecraft:arrow",
					"minecraft:netherite_scrap", "minecraft:diamond", "minecraft:iron_ingot", "minecraft:stick", "minecraft:carrot", "minecraft:golden_apple",
					"minecraft:redstone", "minecraft:lapis_lazuli")
					.forEach(itemName -> getOptionalItem(itemName).filter(item -> new ItemStack(item).getMaxStackSize() > 1).ifPresent(spreadItems::add));
			if (spreadItems.isEmpty()) {
				spreadItems.add(seedItems.get(0));
			}
			return spreadItems;
		}

		private Item getControllerAe2LagWorldSeedItem(List<Item> seedItems, List<Item> spreadItems, int storageIndex, int slot) {
			if (slot % 11 == 0 || Math.floorMod(storageIndex + slot, 17) == 0) {
				return spreadItems.get(Math.floorMod(storageIndex + slot, spreadItems.size()));
			}
			return seedItems.get(Math.floorMod(storageIndex * 263 + slot, seedItems.size()));
		}

		private void giveControllerAe2ProfileItems(ServerPlayer player, List<String> warnings) {
			List.of("ae2:fluix_glass_cable", "ae2:storage_bus", "ae2:creative_energy_cell", "ae2:crafting_terminal", "ae2:pattern_encoding_terminal",
					"ae2:pattern_provider", "ae2:molecular_assembler", "ae2:1k_crafting_storage", "ae2:crafting_accelerator", "ae2:blank_pattern",
					"ae2:crafting_pattern")
					.forEach(itemName -> getOptionalItem(itemName).ifPresentOrElse(item -> player.getInventory().add(new ItemStack(item, 16)),
							() -> warnings.add("missing AE2 item " + itemName)));
			player.getInventory().add(new ItemStack(Items.COAL, 64));
			player.getInventory().add(new ItemStack(Items.COAL_BLOCK, 16));
		}

		private void placeAe2ItemAsBlock(ServerLevel level, ServerPlayer player, BlockPos pos, String itemName, List<String> warnings) {
			getOptionalItem(itemName).ifPresentOrElse(item -> placeBlockWithItem(level, player, pos, new ItemStack(item)),
					() -> warnings.add("missing AE2 item " + itemName));
		}

		private void placeAe2ItemOnBlockFace(ServerLevel level, ServerPlayer player, BlockPos targetPos, Direction side, String itemName,
				List<String> warnings) {
			getOptionalItem(itemName).ifPresentOrElse(item -> placeItemOnBlockFace(level, player, targetPos, side, new ItemStack(item)),
					() -> warnings.add("missing AE2 item " + itemName));
		}

		private Optional<Item> getOptionalItem(String itemName) {
			return BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemName));
		}

		private void warnIfAir(ServerLevel level, BlockPos pos, String description, List<String> warnings) {
			if (level.isEmptyBlock(pos)) {
				warnings.add(description + " appears to be missing at " + pos);
			}
		}

		private List<ControllerFilterInsertExpectation> createControllerFilterInsertExpectations(BlockPos controllerPos, List<BlockPos> overflowPositions,
				Map<Item, Set<BlockPos>> lockedPositionsByItem) {
			List<ControllerFilterInsertExpectation> expectations = new ArrayList<>();
			lockedPositionsByItem
					.forEach((item, positions) -> expectations.add(new ControllerFilterInsertExpectation("locked_" + itemId(item), item, 3, 20, positions)));
			expectations.add(new ControllerFilterInsertExpectation("specific_amethyst", Items.AMETHYST_SHARD, 4, 80, Set.of(controllerPos.offset(2, 0, -1))));
			expectations.add(new ControllerFilterInsertExpectation("specific_honeycomb", Items.HONEYCOMB, 4, 80, Set.of(controllerPos.offset(2, 0, 0))));
			expectations.add(new ControllerFilterInsertExpectation("specific_echo_shard", Items.ECHO_SHARD, 4, 80, Set.of(controllerPos.offset(2, 0, 1))));
			expectations.add(new ControllerFilterInsertExpectation("mod_sophisticatedstorage",
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.GOLD_BARREL_ITEM.get(), 2, 80, Set.of(controllerPos.offset(3, 0, -1))));
			expectations.add(new ControllerFilterInsertExpectation("mod_sophisticatedbackpacks", ModItems.STACK_UPGRADE_TIER_1.get(), 2, 80,
					Set.of(controllerPos.offset(3, 0, 0))));
			expectations
					.add(new ControllerFilterInsertExpectation("deny_accepts_unmatched", Items.NAUTILUS_SHELL, 4, 80, Set.of(controllerPos.offset(3, 0, 1))));
			expectations.add(new ControllerFilterInsertExpectation("denied_item_overflows", Items.FEATHER, 4, 80, Set.copyOf(overflowPositions)));
			return expectations;
		}

		private List<ControllerLockedStorageSpec> createControllerFilterLockedStorageSpecs(BlockPos controllerPos) {
			List<Item> items = List.of(Items.COBBLESTONE, Items.DIRT, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.TUFF, Items.DEEPSLATE, Items.CALCITE,
					Items.SAND, Items.RED_SAND, Items.GRAVEL, Items.CLAY_BALL, Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG,
					Items.ACACIA_LOG, Items.DARK_OAK_LOG, Items.MANGROVE_LOG, Items.CHERRY_LOG, Items.BAMBOO, Items.OAK_PLANKS, Items.SPRUCE_PLANKS,
					Items.BIRCH_PLANKS, Items.STICK, Items.COAL, Items.CHARCOAL, Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.LAPIS_LAZULI,
					Items.EMERALD, Items.DIAMOND, Items.QUARTZ, Items.FLINT, Items.STRING, Items.SPIDER_EYE, Items.BONE, Items.ROTTEN_FLESH, Items.GUNPOWDER,
					Items.LEATHER, Items.RABBIT_HIDE, Items.EGG, Items.WHEAT, Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.COBBLESTONE,
					Items.IRON_INGOT);
			List<ControllerLockedStorageSpec> specs = new ArrayList<>();
			int slot = 0;
			for (int x = 4; x <= 12 && slot < items.size(); x++) {
				for (int z = -2; z <= 3 && slot < items.size(); z++) {
					specs.add(new ControllerLockedStorageSpec(controllerPos.offset(x, 0, z), items.get(slot), slot, getControllerFilterLockedBarrelItem(slot)));
					slot++;
				}
			}
			return specs;
		}

		private Item getControllerFilterLockedBarrelItem(int index) {
			if (index < 5) {
				return net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_BARREL_ITEM.get();
			}
			if (index < 35) {
				return net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DIAMOND_BARREL_ITEM.get();
			}
			if (index < 43) {
				return net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.GOLD_BARREL_ITEM.get();
			}
			if (index < 48) {
				return net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.IRON_BARREL_ITEM.get();
			}
			return net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_ITEM.get();
		}

		private void clearStorageControllerFilterRegressionArea(ServerLevel level, BlockPos controllerPos) {
			discardStorageControllerFilterRegressionItemEntities(level, controllerPos);
			for (int x = -2; x <= 14; x++) {
				for (int y = -1; y <= 2; y++) {
					for (int z = -5; z <= 6; z++) {
						BlockPos pos = controllerPos.offset(x, y, z);
						if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
							storage.clearContent();
						}
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
			discardStorageControllerFilterRegressionItemEntities(level, controllerPos);
		}

		private void discardStorageControllerFilterRegressionItemEntities(ServerLevel level, BlockPos controllerPos) {
			AABB area = new AABB(controllerPos.getX() - 4, controllerPos.getY() - 2, controllerPos.getZ() - 7, controllerPos.getX() + 17,
					controllerPos.getY() + 5, controllerPos.getZ() + 9);
			for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
				itemEntity.discard();
			}
		}

		private void placeController(ServerLevel level, ServerPlayer player, BlockPos pos) {
			placeBlockWithItem(level, player, pos, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER_ITEM.get()));
		}

		private void placeBarrel(ServerLevel level, ServerPlayer player, BlockPos pos, Item barrelItem) {
			placeBlockWithItem(level, player, pos, new ItemStack(barrelItem));
		}

		private void placeChest(ServerLevel level, ServerPlayer player, BlockPos pos) {
			placeBlockWithItem(level, player, pos, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_CHEST_ITEM.get()));
		}

		private void placeDoubleChest(ServerLevel level, ServerPlayer player, BlockPos mainPos) {
			BlockState mainState = net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_CHEST.get().defaultBlockState()
					.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT);
			BlockState otherState = net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_CHEST.get().defaultBlockState()
					.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
			level.setBlock(mainPos, mainState, 3);
			level.setBlock(mainPos.west(), otherState, 3);
			ensureDoubleChest(level, mainPos);
		}

		private void ensureDoubleChest(ServerLevel level, BlockPos mainPos) {
			BlockPos otherPos = mainPos.west();
			ChestBlockEntity mainChest = level.getBlockEntity(mainPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
					.orElse(null);
			ChestBlockEntity otherChest = level.getBlockEntity(otherPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
					.orElse(null);
			if (mainChest == null || otherChest == null) {
				throw new IllegalStateException("Missing double chest storage at " + mainPos + " / " + otherPos);
			}
			if (!mainChest.getMainPos().equals(mainPos) || !otherChest.getMainPos().equals(mainPos)) {
				throw new IllegalStateException("Double chest main position mismatch at " + mainPos + " / " + otherPos + ": main=" + mainChest.getMainPos()
						+ ", other=" + otherChest.getMainPos());
			}
			int expectedSlots = mainChest.getBlockState().getBlock() instanceof ChestBlock chestBlock ? chestBlock.getNumberOfInventorySlots() * 2 : 0;
			if (expectedSlots > 0 && mainChest.getStorageWrapper().getInventoryHandler().size() < expectedSlots) {
				otherChest.joinWithChest(mainChest);
			}
			mainChest.tryToAddToController();
		}

		private void placeBlockWithItem(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
			BlockPos supportPos = pos.below();
			if (level.isEmptyBlock(supportPos)) {
				level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
			}
			player.setYRot(0);
			player.setXRot(0);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false);
			player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitResult);
		}

		private void placeItemOnBlockFace(ServerLevel level, ServerPlayer player, BlockPos targetPos, Direction side, ItemStack stack) {
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(targetPos), side, targetPos, false);
			player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitResult);
		}

		private StorageBlockEntity getBarrelStorage(ServerLevel level, BlockPos pos) {
			return level.getBlockEntity(pos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get())
					.map(storage -> (StorageBlockEntity) storage).orElseThrow(() -> new IllegalStateException("Missing barrel storage at " + pos));
		}

		private StorageBlockEntity getStorage(ServerLevel level, BlockPos pos) {
			if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
				return storage;
			}
			throw new IllegalStateException("Missing storage at " + pos);
		}

		private boolean insertStackIntoStorage(StorageBlockEntity storage, ItemStack stack) {
			ResourceHandler<ItemResource> handler = storage.getExternalItemHandler(null);
			if (handler == null || stack.isEmpty()) {
				return false;
			}
			try (Transaction transaction = Transaction.openRoot()) {
				int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), transaction);
				if (inserted == stack.getCount()) {
					transaction.commit();
					return true;
				}
			}
			return false;
		}

		private int getFilledInventorySlotCount(InventoryHandler inventory) {
			int filledSlots = 0;
			for (int slot = 0; slot < inventory.size(); slot++) {
				if (!inventory.getStackInSlot(slot).isEmpty()) {
					filledSlots++;
				}
			}
			return filledSlots;
		}

		private void configureControllerFilterStorage(StorageBlockEntity storage, ControllerFilterStorageSpec spec, boolean profileCapacity) {
			UpgradeHandler upgrades = storage.getStorageWrapper().getUpgradeHandler();
			upgrades.setStackInSlot(0, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModItems.ADVANCED_FILTER_UPGRADE.get()));
			if (profileCapacity) {
				addControllerFilterProfileCapacity(storage, 1);
			}
			FilterUpgradeWrapper filter = upgrades.getWrappersThatImplement(FilterUpgradeWrapper.class).stream().findFirst()
					.orElseThrow(() -> new IllegalStateException("Filter upgrade wrapper missing in " + spec.name()));
			filter.setDirection(net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction.INPUT);
			filter.getFilterLogic().setDepositFilterType(spec.allowList() ? ContentsFilterType.ALLOW : ContentsFilterType.BLOCK);
			filter.getFilterLogic().setPrimaryMatch(spec.primaryMatch());
			filter.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(spec.filterItem()));
			upgrades.saveInventory();
			storage.getStorageWrapper().refreshInventoryForInputOutput();
		}

		private void addControllerFilterProfileCapacity(StorageBlockEntity storage, int firstUpgradeSlot) {
			UpgradeHandler upgrades = storage.getStorageWrapper().getUpgradeHandler();
			for (int slot = firstUpgradeSlot; slot < Math.min(upgrades.size(), firstUpgradeSlot + 2); slot++) {
				upgrades.setStackInSlot(slot, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModItems.STACK_UPGRADE_TIER_5.get()));
			}
			upgrades.saveInventory();
		}

		private void assertStorageLockState(ServerLevel level, List<BlockPos> positions, boolean locked, String group, List<String> failures) {
			for (BlockPos pos : positions) {
				StorageBlockEntity storage = getBarrelStorage(level, pos);
				if (storage.isLocked() != locked) {
					failures.add(group + " storage lock state mismatch at " + pos + ": expected locked=" + locked + ", actual=" + storage.isLocked());
				}
			}
		}

		private ControllerFilterInsertStats runControllerFilterInsertExpectation(ServerLevel level, ControllerBlockEntity controller,
				List<BlockPos> allPositions, ControllerFilterInsertExpectation expectation, List<String> failures) {
			Set<BlockPos> outsidePositions = new HashSet<>(allPositions);
			outsidePositions.removeAll(expectation.expectedPositions());
			long expectedBefore = countItemInPositions(level, expectation.expectedPositions(), expectation.item());
			long outsideBefore = countItemInPositions(level, outsidePositions, expectation.item());
			long inserted = 0;
			for (int i = 0; i < expectation.calls(); i++) {
				int insertedNow = insertIntoController(controller, expectation.item(), expectation.count());
				if (insertedNow != expectation.count()) {
					failures.add(expectation.name() + " insert " + i + " returned remainder " + (expectation.count() - insertedNow) + "x"
							+ itemId(expectation.item()));
				}
				inserted += insertedNow;
			}
			long expectedAfter = countItemInPositions(level, expectation.expectedPositions(), expectation.item());
			long outsideAfter = countItemInPositions(level, outsidePositions, expectation.item());
			if (expectedAfter - expectedBefore != inserted) {
				failures.add(expectation.name() + " expected destination delta " + inserted + " for " + itemId(expectation.item()) + ", got "
						+ (expectedAfter - expectedBefore) + " at " + expectation.expectedPositions());
			}
			if (outsideAfter != outsideBefore) {
				failures.add(
						expectation.name() + " changed outside destination count for " + itemId(expectation.item()) + " by " + (outsideAfter - outsideBefore));
			}
			return new ControllerFilterInsertStats(expectation.calls(), inserted);
		}

		private ControllerFilterInsertStats runControllerFilterProfileExpectation(ControllerBlockEntity controller,
				ControllerFilterInsertExpectation expectation) {
			long inserted = 0;
			for (int i = 0; i < expectation.calls(); i++) {
				inserted += insertIntoController(controller, expectation.item(), expectation.count());
			}
			return new ControllerFilterInsertStats(expectation.calls(), inserted);
		}

		private int insertIntoController(ControllerBlockEntity controller, Item item, int count) {
			try (Transaction tx = Transaction.openRoot()) {
				int inserted = controller.insert(ItemResource.of(item), count, tx);
				tx.commit();
				return inserted;
			}
		}

		private long countItemInPositions(ServerLevel level, Set<BlockPos> positions, Item item) {
			long count = 0;
			for (BlockPos pos : positions) {
				InventoryHandler inventory = getBarrelStorage(level, pos).getStorageWrapper().getInventoryHandler();
				for (int slot = 0; slot < inventory.size(); slot++) {
					ItemStack stack = inventory.getStackInSlot(slot);
					if (stack.is(item)) {
						count += stack.getCount();
					}
				}
			}
			return count;
		}

		private String buildStorageControllerFilterRegressionJson(boolean ok, long setupMillis, long insertMillis, long verifyMillis, int connectedStorages,
				int lockedStorages, int filteredStorages, int overflowStorages, int insertCalls, long itemsInserted, List<String> failures) {
			StringBuilder json = new StringBuilder("{\"ok\":").append(ok).append(",\"scenario\":\"storage_controller_filter_routing\"")
					.append(",\"connectedStorages\":").append(connectedStorages).append(",\"lockedStorages\":").append(lockedStorages)
					.append(",\"filteredStorages\":").append(filteredStorages).append(",\"overflowStorages\":").append(overflowStorages)
					.append(",\"insertCalls\":").append(insertCalls).append(",\"itemsInserted\":").append(itemsInserted).append(",\"setupMillis\":")
					.append(setupMillis).append(",\"insertMillis\":").append(insertMillis).append(",\"verifyMillis\":").append(verifyMillis)
					.append(",\"failed\":").append(failures.size()).append(",\"failures\":[");
			for (int i = 0; i < failures.size(); i++) {
				if (i > 0) {
					json.append(',');
				}
				json.append('"').append(escapeJson(failures.get(i))).append('"');
			}
			json.append("]}");
			return json.toString();
		}

		private String itemId(Item item) {
			return BuiltInRegistries.ITEM.getKey(item).toString();
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

		private record ColumnUpgradeRegressionScenario(String name, int inventorySlots, Item upgradeItem, int[] occupiedSlots, int[] noSortSlots,
				int[] memorySlots, int[] stableSlots, CapturedMobSpec[] capturedMobs, int[] expectedCapturedMobSlots, String operation, boolean expectedFits) {
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

		private record ControllerFilterStorageSpec(String name, BlockPos pos, Item filterItem, boolean allowList, PrimaryMatch primaryMatch) {
		}

		private record ControllerLockedStorageSpec(BlockPos pos, Item item, int slot, Item barrelItem) {
		}

		private record ControllerFilterInsertExpectation(String name, Item item, int count, int calls, Set<BlockPos> expectedPositions) {
		}

		private record ControllerFilterInsertStats(int calls, long items) {
		}

		private record StorageBlockGuiSpec(String name, Item item) {
		}

		private record StorageBlockGuiExpected(int storageSlots, int upgradeSlots) {
		}

		private record StorageBlockGuiState(int storageSlots, int upgradeSlots, String screenClass) {
			private boolean matches(int expectedStorageSlots) {
				return storageSlots == expectedStorageSlots && upgradeSlots >= 0 && !screenClass.isBlank();
			}
		}

		private record ColumnUpgradeSimulationResult(boolean fits) {
		}

		private record PlacedColumnUpgradeClickExpectation(int expectedColumnsTaken, int expectedStorageSlots) {
		}

		private record PlacedColumnUpgradeState(int columnsTaken, int storageSlots, int inventoryHandlerSlots, boolean upgradeSlotEmpty,
				boolean carriedNotEmpty) {
			private boolean matches(PlacedColumnUpgradeClickExpectation expectation) {
				return columnsTaken == expectation.expectedColumnsTaken() && storageSlots == expectation.expectedStorageSlots()
						&& inventoryHandlerSlots == expectation.expectedStorageSlots() && upgradeSlotEmpty && carriedNotEmpty;
			}
		}

		private record SubMobCatcherRegressionState(String context, int storageSlots, boolean slot0Backpack, int currentMobCount, @Nullable String currentMobId,
				int nestedMobCount, @Nullable String nestedMobId) {
			private boolean parentContainerMatchesBeforeSubOpen() {
				return BackpackContext.ContextType.ITEM_BACKPACK.name().equals(context) && slot0Backpack && storageSlots == 81 && currentMobCount == 1
						&& SUB_MOB_CATCHER_PARENT_MOB_ID.toString().equals(currentMobId);
			}

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

		private record AdvancedCompactingHighStackRegressionResult(String name, boolean passed, int firstSlotCount, int secondSlotCount, int triggerCount,
				int expectedNuggets, int actualNuggets, int expectedIngots, int actualIngots, int expectedBlocks, int actualBlocks, int insertRemainder,
				@Nullable String error) {
		}

		private enum BackpackGuiRegressionContext {
			PLACED("placed"), CURIOS("curios"), SUB("sub");

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

		private static int getInt(JsonObject json, String property, int defaultValue) {
			return json.has(property) ? json.get(property).getAsInt() : defaultValue;
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