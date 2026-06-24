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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.p3pp3rf1y.devclientautomation.demo.DemoCommand;
import net.p3pp3rf1y.devclientautomation.demo.DemoMouseMotion;
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
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitResult;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitter;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.StorageWrapperRepository;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;
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
				httpServer.createContext("/mouse/move", this::moveMouse);
				httpServer.createContext("/command", this::command);
				httpServer.createContext("/screen/move-to-slot", this::moveToSlot);
				httpServer.createContext("/screen/throw-slot", this::throwSlot);
				httpServer.createContext("/invtweaks/sort", this::inventoryTweaksSort);
				httpServer.createContext("/inventoryessentials/drop-by-type", this::inventoryEssentialsDropByType);
				httpServer.createContext("/window/maximize", this::maximizeWindow);
				httpServer.createContext("/wait", this::waitFor);
				httpServer.createContext("/client/stop", this::stopClient);
				httpServer.createContext("/world/load", this::loadWorld);
				httpServer.createContext("/world/survival", this::setSurvivalMode);
				httpServer.createContext("/player/fill-inventory", this::fillPlayerInventory);
				httpServer.createContext("/backpack/setup", this::setupBackpacks);
				httpServer.createContext("/backpack/stress", this::stressBackpacks);
				httpServer.createContext("/backpack/status", this::backpackStatus);
				httpServer.createContext("/backpack/open-main", this::openMainBackpack);
				httpServer.createContext("/backpack/open-nested", this::openNestedBackpack);
				httpServer.createContext("/backpack/empty", this::emptyBackpacks);
				httpServer.createContext("/backpack/clear-cache", this::clearBackpackCache);
				httpServer.createContext("/backpack/magnet-settings", this::changeMagnetSettings);
				httpServer.createContext("/backpack/move", this::moveBackpacks);
				httpServer.createContext("/backpack/spread-nested", this::spreadNestedBackpacks);
				httpServer.createContext("/backpack/fill-main-noise", this::fillMainBackpackNoise);
				httpServer.createContext("/backpack/magnet-pickup", this::changeMagnetPickup);
				httpServer.createContext("/backpack/seed", this::seedBackpack);
				httpServer.createContext("/backpack/bulk-drop", this::bulkDropFromNestedBackpack);
				httpServer.createContext("/backpack/column-upgrade-regressions", this::backpackColumnUpgradeRegressions);
				httpServer.createContext("/backpack/storage-gui-regressions", this::backpackStorageGuiRegressions);
				httpServer.createContext("/backpack/gui-regression/run", this::backpackGuiRegressionRun);
				httpServer.createContext("/backpack/remote-upgrade-slot-regression", this::backpackRemoteUpgradeSlotRegression);
				httpServer.createContext("/storage/controller-double-chest-regressions", this::storageControllerDoubleChestRegressions);
				httpServer.createContext("/storage/controller-filter-regressions", this::storageControllerFilterRegressions);
				httpServer.createContext("/storage/simple-material-decoration-verification", this::simpleMaterialDecorationVerification);
				httpServer.createContext("/storage/simple-material-render-setup", this::simpleMaterialRenderSetup);
				httpServer.createContext("/storage/simple-material-overlay-comparison-setup", this::simpleMaterialOverlayComparisonSetup);
				httpServer.createContext("/model/item-diagnostics", this::itemModelDiagnostics);
				httpServer.createContext("/model/hotbar-item-diagnostics", this::hotbarItemModelDiagnostics);
				httpServer.createContext("/backpack/dropped-items", this::droppedItemsStatus);
				httpServer.createContext("/backpack/clear-dropped-items", this::clearDroppedItems);
				httpServer.createContext("/screenshot", this::screenshot);
				httpServer.createContext("/recipe-viewer/state", this::recipeViewerState);
				httpServer.createContext("/recipe-viewer/search", this::recipeViewerSearch);
				httpServer.createContext("/recipe-viewer/open", this::recipeViewerOpen);
				httpServer.createContext("/recipe-viewer/query", this::recipeViewerQuery);
				httpServer.createContext("/recipe-viewer/stats", this::recipeViewerStats);
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
			sendJsonHandling(exchange, () -> runOnClient(this::buildStateJson));
		}

		private void screen(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "GET");
			sendJsonHandling(exchange, () -> runOnClient(this::buildScreenJson));
		}

		private void clickWidget(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String text = extractString(body, "text").orElse("");
			boolean contains = extractBoolean(body, "contains").orElse(false);
			int button = extractInt(body, "button").orElse(0);
			int index = extractInt(body, "index").orElse(-1);
			sendJson(exchange, runOnClient(() -> clickWidget(text, contains, button, index)));
		}

		private void key(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String key = extractString(body, "key").orElse("");
			boolean ctrl = extractBoolean(body, "ctrl").orElse(false);
			boolean shift = extractBoolean(body, "shift").orElse(false);
			boolean alt = extractBoolean(body, "alt").orElse(false);
			sendJson(exchange, runOnClient(() -> pressKey(key, ctrl, shift, alt)));
		}

		private void moveMouse(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String position = extractString(body, "position").orElse("");
			int x = extractInt(body, "x").orElse(-1);
			int y = extractInt(body, "y").orElse(-1);
			sendJson(exchange, runOnClient(() -> moveMouse(position, x, y)));
		}

		private void command(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String command = extractString(body, "command").orElse("");
			sendJsonHandling(exchange, () -> runOnServer(player -> runCommand(player, command)));
		}

		private void moveToSlot(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int menuSlot = extractInt(body, "menuSlot").orElse(-1);
			sendJson(exchange, runOnClient(() -> moveToSlot(menuSlot)));
		}

		private void throwSlot(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int menuSlot = extractInt(body, "menuSlot").orElse(-1);
			boolean fullStack = extractBoolean(body, "fullStack").orElse(true);
			sendJson(exchange, runOnClient(() -> throwSlot(menuSlot, fullStack)));
		}

		private void inventoryTweaksSort(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			boolean playerInventory = extractBoolean(body, "playerInventory").orElse(false);
			String screenName = extractString(body, "screenName").orElse("net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen");
			sendJsonHandling(exchange, () -> runOnServer(player -> inventoryTweaksSort(player, playerInventory, screenName)));
		}

		private void inventoryEssentialsDropByType(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int menuSlot = extractInt(body, "menuSlot").orElse(-1);
			sendJsonHandling(exchange, () -> runOnClient(() -> inventoryEssentialsDropByType(menuSlot)));
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
			byte[] bytes = runOnClient(() -> {
				try (NativeImage image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
					return image.asByteArray();
				} catch (IOException e) {
					throw new IllegalStateException("Failed to capture screenshot", e);
				}
			});
			exchange.getResponseHeaders().set("Content-Type", "image/png");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(bytes);
			}
		}

		private void setupBackpacks(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			boolean mainMagnet = extractBoolean(body, "mainMagnet").orElse(false);
			int redstoneCount = extractInt(body, "redstoneCount").orElse(0);
			sendJsonHandling(exchange, () -> runOnServer(player -> setupBackpacks(player, mainMagnet, redstoneCount)));
		}

		private void stressBackpacks(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int stacks = extractInt(body, "stacks").orElse(64);
			int count = extractInt(body, "count").orElse(64);
			double radius = extractDouble(body, "radius").orElse(2.5D);
			sendJsonHandling(exchange, () -> runOnServer(player -> stressBackpacks(player, stacks, count, radius)));
		}

		private void backpackStatus(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "GET");
			sendJsonHandling(exchange, () -> runOnServer(this::backpackStatus));
		}

		private void openMainBackpack(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::openMainBackpack));
		}

		private void openNestedBackpack(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int nestedSlot = extractInt(body, "nestedSlot").orElse(0);
			sendJsonHandling(exchange, () -> runOnServer(player -> openNestedBackpack(player, nestedSlot)));
		}

		private void emptyBackpacks(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::emptyNestedBackpacks));
		}

		private void clearBackpackCache(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> {
				StorageWrapperRepository.clearCache();
				return "{\"ok\":true}";
			});
		}

		private void changeMagnetSettings(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String target = extractString(body, "target").orElse("all");
			ContentsFilterType filterType = ContentsFilterType.fromName(extractString(body, "filterType").orElse("storage"));
			sendJsonHandling(exchange, () -> runOnServer(player -> changeMagnetSettings(player, target, filterType)));
		}

		private void moveBackpacks(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String target = extractString(body, "target").orElse("nested");
			boolean clearCache = extractBoolean(body, "clearCache").orElse(true);
			sendJsonHandling(exchange, () -> runOnServer(player -> moveBackpacks(player, target, clearCache)));
		}

		private void spreadNestedBackpacks(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::spreadNestedBackpacks));
		}

		private void fillMainBackpackNoise(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::fillMainBackpackNoise));
		}

		private void changeMagnetPickup(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String target = extractString(body, "target").orElse("nested");
			boolean pickupItems = extractBoolean(body, "pickupItems").orElse(false);
			sendJsonHandling(exchange, () -> runOnServer(player -> changeMagnetPickup(player, target, pickupItems)));
		}

		private void seedBackpack(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int nestedSlot = extractInt(body, "nestedSlot").orElse(0);
			int count = extractInt(body, "count").orElse(3_072);
			sendJsonHandling(exchange, () -> runOnServer(player -> seedNestedBackpack(player, nestedSlot, Items.REDSTONE, count)));
		}

		private void bulkDropFromNestedBackpack(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int nestedSlot = extractInt(body, "nestedSlot").orElse(0);
			int maxStacks = extractInt(body, "maxStacks").orElse(128);
			int pickupDelay = extractInt(body, "pickupDelay").orElse(6_000);
			boolean clearCache = extractBoolean(body, "clearCache").orElse(false);
			sendJsonHandling(exchange,
					() -> runOnServer(player -> bulkDropFromNestedBackpack(player, nestedSlot, Items.REDSTONE, maxStacks, pickupDelay, clearCache)));
		}

		private void backpackColumnUpgradeRegressions(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::runBackpackColumnUpgradeRegressions));
		}

		private void backpackStorageGuiRegressions(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, this::runBackpackStorageGuiRegressions);
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

		private void storageControllerDoubleChestRegressions(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			boolean inspectOnly = extractBoolean(body, "inspectOnly").orElse(false);
			sendJsonHandling(exchange, () -> runOnServer(player -> runStorageControllerDoubleChestRegressions(player, inspectOnly)));
		}

		private void storageControllerFilterRegressions(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String mode = extractString(body, "mode").orElse("run");
			int runs = extractInt(body, "runs").orElse(1);
			boolean profileCapacity = extractBoolean(body, "profileCapacity").orElse(false);
			sendJsonHandling(exchange, () -> runOnServer(player -> {
				if (mode.equals("profile")) {
					return profileStorageControllerFilterRegressions(player, runs);
				}
				if (mode.equals("manualDepositProfile")) {
					return profileStorageControllerFilterManualDeposit(player, runs);
				}
				return runStorageControllerFilterRegressions(player, !mode.equals("setup"), profileCapacity);
			}));
		}

		private void simpleMaterialDecorationVerification(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::verifySimpleMaterialDecoration));
		}

		private void simpleMaterialRenderSetup(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::setupSimpleMaterialRenderVerification));
		}

		private void simpleMaterialOverlayComparisonSetup(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::setupSimpleMaterialOverlayComparison));
		}

		private void itemModelDiagnostics(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String itemName = extractString(body, "item").orElse("sophisticatedstorage:controller");
			sendJsonHandling(exchange, () -> runOnClient(() -> itemModelDiagnostics(itemName)));
		}

		private void hotbarItemModelDiagnostics(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int slot = extractInt(body, "slot").orElse(0);
			sendJsonHandling(exchange, () -> runOnClient(() -> itemModelDiagnostics(Minecraft.getInstance().player.getInventory().getItem(slot))));
		}

		private void droppedItemsStatus(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "GET");
			sendJsonHandling(exchange, () -> runOnServer(player -> droppedItemsStatus(player, Items.REDSTONE)));
		}

		private void clearDroppedItems(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(player -> clearDroppedItems(player, Items.REDSTONE)));
		}

		private void setSurvivalMode(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::setSurvivalMode));
		}

		private void fillPlayerInventory(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::fillPlayerInventory));
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

			LevelSettings levelSettings = new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
					WorldDataConfiguration.DEFAULT);
			WorldOptions worldOptions = new WorldOptions(0L, false, false);
			minecraft.createWorldOpenFlows().createFreshLevel(worldName, levelSettings, worldOptions, AutomationServer::voidFlatDimensions, null);
			return "{\"ok\":true,\"created\":true}";
		}

		private String setupBackpacks(ServerPlayer player, boolean mainMagnet, int redstoneCount) {
			player.getInventory().clearContent();
			ItemStack mainBackpack = createBackpackStack();
			ItemStack firstNestedBackpack = createNestedBackpack(Items.COBBLESTONE, Items.IRON_INGOT);
			ItemStack secondNestedBackpack = createNestedBackpack(Items.DIRT, Items.GOLD_INGOT);

			IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
			mainWrapper.setSlotNumbers(80, 5);
			UpgradeHandler mainUpgrades = mainWrapper.getUpgradeHandler();
			mainUpgrades.setStackInSlot(0, new ItemStack(ModItems.INCEPTION_UPGRADE.get()));
			if (mainMagnet) {
				mainUpgrades.setStackInSlot(1, new ItemStack(ModItems.ADVANCED_MAGNET_UPGRADE.get()));
				setMagnetFilterType(mainWrapper, ContentsFilterType.ALLOW);
			}
			mainUpgrades.saveInventory();

			InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
			mainInventory.setStackInSlot(0, firstNestedBackpack);
			mainInventory.setStackInSlot(1, secondNestedBackpack);
			mainInventory.saveInventory();

			if (redstoneCount > 0) {
				seedNestedBackpack(mainWrapper, 0, Items.REDSTONE, redstoneCount);
			}

			player.getInventory().setItem(0, mainBackpack);
			player.getInventory().setChanged();

			return "{\"ok\":true," + jsonProperty("mainUuid", mainWrapper.getContentsUuid().map(Object::toString).orElse(null)) + ","
					+ jsonProperty("nested0Uuid", BackpackWrapper.fromStack(firstNestedBackpack).getContentsUuid().map(Object::toString).orElse(null)) + ","
					+ jsonProperty("nested1Uuid", BackpackWrapper.fromStack(secondNestedBackpack).getContentsUuid().map(Object::toString).orElse(null)) + ","
					+ "\"mainMagnet\":" + mainMagnet + "," + "\"redstoneCount\":" + redstoneCount + "}";
		}

		private String runCommand(ServerPlayer player, String command) {
			String normalizedCommand = command.startsWith("/") ? command.substring(1) : command;
			player.server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), normalizedCommand);
			return "{\"ok\":true," + jsonProperty("command", normalizedCommand) + ",\"dispatched\":true}";
		}

		private String itemModelDiagnostics(String itemName) {
			Minecraft minecraft = Minecraft.getInstance();
			Item item = getItem(itemName);
			return itemModelDiagnostics(new ItemStack(item));
		}

		private String itemModelDiagnostics(ItemStack stack) {
			Minecraft minecraft = Minecraft.getInstance();
			String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0);
			List<BakedModel> passes = model.getRenderPasses(stack, false);
			StringBuilder json = new StringBuilder("{\"ok\":true,");
			json.append(jsonProperty("item", itemName)).append(',');
			json.append(jsonProperty("simpleMaterial", SimpleMaterialBlockItem.getMaterial(stack).map(ResourceLocation::toString).orElse(null))).append(',');
			json.append(jsonProperty("modelClass", model.getClass().getName())).append(',');
			json.append(jsonProperty("particle", model.getParticleIcon(ModelData.EMPTY).contents().name().toString())).append(',');
			json.append("\"passCount\":").append(passes.size()).append(',');
			json.append("\"passes\":[");
			for (int i = 0; i < passes.size(); i++) {
				if (i > 0) {
					json.append(',');
				}
				BakedModel pass = passes.get(i);
				List<BakedQuad> quads = new ArrayList<>();
				RandomSource random = RandomSource.create(42);
				for (Direction direction : Direction.values()) {
					random.setSeed(42);
					quads.addAll(pass.getQuads(null, direction, random));
				}
				random.setSeed(42);
				quads.addAll(pass.getQuads(null, null, random));
				Set<String> sprites = new HashSet<>();
				for (BakedQuad quad : quads) {
					sprites.add(quad.getSprite().contents().name().toString());
				}
				json.append('{').append(jsonProperty("class", pass.getClass().getName())).append(',').append("\"quadCount\":").append(quads.size()).append(',')
						.append("\"sprites\":[");
				int spriteIndex = 0;
				for (String sprite : sprites) {
					if (spriteIndex++ > 0) {
						json.append(',');
					}
					json.append('"').append(escapeJson(sprite)).append('"');
				}
				json.append("]}");
			}
			json.append("]}");
			return json.toString();
		}

		private ItemStack createNestedBackpack(Item firstSeedItem, Item secondSeedItem) {
			ItemStack backpack = createBackpackStack();
			IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
			wrapper.setSlotNumbers(80, 5);
			UpgradeHandler upgrades = wrapper.getUpgradeHandler();
			upgrades.setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_3.get()));
			upgrades.setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_3.get()));
			upgrades.setStackInSlot(2, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
			upgrades.setStackInSlot(3, new ItemStack(ModItems.ADVANCED_MAGNET_UPGRADE.get()));
			setMagnetFilterType(wrapper, ContentsFilterType.STORAGE);
			upgrades.saveInventory();

			InventoryHandler inventory = wrapper.getInventoryHandler();
			inventory.setStackInSlot(0, new ItemStack(firstSeedItem, 64));
			inventory.setStackInSlot(1, new ItemStack(secondSeedItem, 64));
			inventory.saveInventory();
			return backpack;
		}

		private void setMagnetFilterType(IBackpackWrapper wrapper, ContentsFilterType filterType) {
			wrapper.getUpgradeHandler().getWrappersThatImplement(MagnetUpgradeWrapper.class)
					.forEach(magnet -> magnet.getFilterLogic().setDepositFilterType(filterType));
			wrapper.getUpgradeHandler().saveInventory();
		}

		private ItemStack createBackpackStack() {
			ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
			backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
			backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 80);
			backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);
			return backpack;
		}

		private String stressBackpacks(ServerPlayer player, int stacks, int count, double radius) {
			ServerLevel level = player.serverLevel();
			Item[] items = {Items.COBBLESTONE, Items.IRON_INGOT, Items.DIRT, Items.GOLD_INGOT};
			int spawned = 0;
			for (int i = 0; i < stacks; i++) {
				double angle = Math.PI * 2D * i / Math.max(1, stacks);
				ItemStack stack = new ItemStack(items[i % items.length], Math.max(1, Math.min(count, items[i % items.length].getDefaultMaxStackSize())));
				ItemEntity entity = new ItemEntity(level, player.getX() + Math.cos(angle) * radius, player.getY() + 0.5D,
						player.getZ() + Math.sin(angle) * radius, stack);
				entity.setPickUpDelay(0);
				level.addFreshEntity(entity);
				spawned++;
			}
			return "{\"ok\":true,\"spawned\":" + spawned + "}";
		}

		private String backpackStatus(ServerPlayer player) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (!(mainBackpack.getItem() instanceof BackpackItem)) {
				return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
			}

			IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
			StringBuilder json = new StringBuilder("{\"ok\":true,");
			json.append(jsonProperty("mainUuid", mainWrapper.getContentsUuid().map(Object::toString).orElse(null))).append(',');
			json.append("\"mainItems\":").append(countItems(mainWrapper.getInventoryHandler())).append(',');
			json.append("\"nested\":[");
			boolean first = true;
			InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
			for (int slot = 0; slot < mainInventory.getSlots(); slot++) {
				ItemStack stack = mainInventory.getStackInSlot(slot);
				if (!(stack.getItem() instanceof BackpackItem)) {
					continue;
				}
				if (!first) {
					json.append(',');
				}
				first = false;
				IBackpackWrapper nestedWrapper = BackpackWrapper.fromStack(stack);
				json.append('{').append("\"slot\":").append(slot).append(',')
						.append(jsonProperty("uuid", nestedWrapper.getContentsUuid().map(Object::toString).orElse(null))).append(',').append("\"items\":")
						.append(countItems(nestedWrapper.getInventoryHandler())).append('}');
			}
			json.append("]}");
			return json.toString();
		}

		private String openNestedBackpack(ServerPlayer player, int nestedSlot) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (!(mainBackpack.getItem() instanceof BackpackItem)) {
				return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
			}
			ItemStack nestedBackpack = BackpackWrapper.fromStack(mainBackpack).getInventoryHandler().getStackInSlot(nestedSlot);
			if (!(nestedBackpack.getItem() instanceof BackpackItem)) {
				return "{\"ok\":false,\"error\":\"No nested backpack in requested slot\"}";
			}
			BackpackContext context = new BackpackContext.ItemSubBackpack(PlayerInventoryProvider.MAIN_INVENTORY, "", 0, false, nestedSlot, true);
			boolean opened = player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new BackpackContainer(windowId, menuPlayer, context),
					Component.literal("Nested automation backpack")), context::toBuffer).isPresent();
			return "{\"ok\":" + opened + ",\"nestedSlot\":" + nestedSlot + "," + jsonProperty("serverMenu", player.containerMenu.getClass().getName()) + "}";
		}

		private String openMainBackpack(ServerPlayer player) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (!(mainBackpack.getItem() instanceof BackpackItem backpackItem)) {
				return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
			}
			player.getInventory().selected = 0;
			backpackItem.use(player.level(), player, InteractionHand.MAIN_HAND);
			return "{\"ok\":true," + jsonProperty("serverMenu", player.containerMenu.getClass().getName()) + "}";
		}

		private String inventoryTweaksSort(ServerPlayer player, boolean playerInventory, String screenName) {
			try {
				Class<?> sortingClass = Class.forName("invtweaks.util.Sorting");
				Method executeSort = sortingClass.getMethod("executeSort", Player.class, boolean.class, String.class);
				executeSort.invoke(null, player, playerInventory, screenName);
				return "{\"ok\":true,\"playerInventory\":" + playerInventory + "," + jsonProperty("serverMenu", player.containerMenu.getClass().getName())
						+ "}";
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Failed to invoke Inventory Tweaks sort", e);
			}
		}

		private String changeMagnetSettings(ServerPlayer player, String target, ContentsFilterType filterType) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (!(mainBackpack.getItem() instanceof BackpackItem)) {
				return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
			}
			int changed = 0;
			IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
			if (target.equals("all") || target.equals("main")) {
				changed += setMagnetFilterTypeAndCount(mainWrapper, filterType);
			}
			if (target.equals("all") || target.equals("nested")) {
				InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
				for (int slot = 0; slot < mainInventory.getSlots(); slot++) {
					ItemStack stack = mainInventory.getStackInSlot(slot);
					if (stack.getItem() instanceof BackpackItem) {
						changed += setMagnetFilterTypeAndCount(BackpackWrapper.fromStack(stack), filterType);
					}
				}
			}
			return "{\"ok\":true,\"changed\":" + changed + "," + jsonProperty("filterType", filterType.getSerializedName()) + "}";
		}

		private String changeMagnetPickup(ServerPlayer player, String target, boolean pickupItems) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (!(mainBackpack.getItem() instanceof BackpackItem)) {
				return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
			}
			int changed = 0;
			IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
			if (target.equals("all") || target.equals("main")) {
				changed += setMagnetPickupAndCount(mainWrapper, pickupItems);
			}
			if (target.equals("all") || target.equals("nested")) {
				InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
				for (int slot = 0; slot < mainInventory.getSlots(); slot++) {
					ItemStack stack = mainInventory.getStackInSlot(slot);
					if (stack.getItem() instanceof BackpackItem) {
						changed += setMagnetPickupAndCount(BackpackWrapper.fromStack(stack), pickupItems);
					}
				}
			}
			return "{\"ok\":true,\"changed\":" + changed + ",\"pickupItems\":" + pickupItems + "}";
		}

		private int setMagnetPickupAndCount(IBackpackWrapper wrapper, boolean pickupItems) {
			List<MagnetUpgradeWrapper> magnets = wrapper.getUpgradeHandler().getWrappersThatImplement(MagnetUpgradeWrapper.class);
			magnets.forEach(magnet -> magnet.setPickupItems(pickupItems));
			wrapper.getUpgradeHandler().saveInventory();
			return magnets.size();
		}

		private int setMagnetFilterTypeAndCount(IBackpackWrapper wrapper, ContentsFilterType filterType) {
			List<MagnetUpgradeWrapper> magnets = wrapper.getUpgradeHandler().getWrappersThatImplement(MagnetUpgradeWrapper.class);
			magnets.forEach(magnet -> magnet.getFilterLogic().setDepositFilterType(filterType));
			wrapper.getUpgradeHandler().saveInventory();
			return magnets.size();
		}

		private String moveBackpacks(ServerPlayer player, String target, boolean clearCache) {
			int moved = 0;
			if (target.equals("main") || target.equals("all")) {
				ItemStack mainBackpack = player.getInventory().getItem(0);
				if (mainBackpack.getItem() instanceof BackpackItem) {
					player.getInventory().setItem(0, player.getInventory().getItem(1));
					player.getInventory().setItem(1, mainBackpack);
					player.getInventory().setItem(1, player.getInventory().getItem(0));
					player.getInventory().setItem(0, mainBackpack);
					player.getInventory().setChanged();
					moved++;
				}
			}
			if (target.equals("nested") || target.equals("all")) {
				ItemStack mainBackpack = player.getInventory().getItem(0);
				if (mainBackpack.getItem() instanceof BackpackItem) {
					InventoryHandler mainInventory = BackpackWrapper.fromStack(mainBackpack).getInventoryHandler();
					ItemStack first = mainInventory.getStackInSlot(0);
					ItemStack second = mainInventory.getStackInSlot(1);
					mainInventory.setStackInSlot(0, second);
					mainInventory.setStackInSlot(1, first);
					mainInventory.setStackInSlot(0, first);
					mainInventory.setStackInSlot(1, second);
					mainInventory.saveInventory();
					moved++;
				}
			}
			if (clearCache) {
				StorageWrapperRepository.clearCache();
			}
			return "{\"ok\":true,\"moved\":" + moved + ",\"cacheCleared\":" + clearCache + "}";
		}

		private String spreadNestedBackpacks(ServerPlayer player) {
			IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
			InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
			List<ItemStack> nestedBackpacks = new ArrayList<>();
			for (int slot = 0; slot < mainInventory.getSlots(); slot++) {
				ItemStack stack = mainInventory.getStackInSlot(slot);
				if (stack.getItem() instanceof BackpackItem) {
					nestedBackpacks.add(stack.copy());
					mainInventory.setStackInSlot(slot, ItemStack.EMPTY);
				}
			}
			int[] targetSlots = {13, 47, 72};
			for (int i = 0; i < nestedBackpacks.size() && i < targetSlots.length; i++) {
				mainInventory.setStackInSlot(targetSlots[i], nestedBackpacks.get(i));
			}
			mainInventory.saveInventory();
			return "{\"ok\":true,\"spread\":" + nestedBackpacks.size() + "}";
		}

		private String fillMainBackpackNoise(ServerPlayer player) {
			IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
			InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
			int filled = 0;
			Item[] items = {Items.REDSTONE, Items.DIAMOND, Items.OAK_LOG, Items.GRAVEL, Items.COPPER_INGOT, Items.EMERALD};
			int[] slots = {0, 2, 5, 21, 34, 63};
			for (int i = 0; i < slots.length; i++) {
				if (mainInventory.getStackInSlot(slots[i]).isEmpty()) {
					mainInventory.setStackInSlot(slots[i], new ItemStack(items[i], 16 + i));
					filled++;
				}
			}
			mainInventory.saveInventory();
			return "{\"ok\":true,\"filled\":" + filled + "}";
		}

		private String emptyNestedBackpacks(ServerPlayer player) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (!(mainBackpack.getItem() instanceof BackpackItem)) {
				return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
			}
			int removed = 0;
			InventoryHandler mainInventory = BackpackWrapper.fromStack(mainBackpack).getInventoryHandler();
			for (int slot = 0; slot < mainInventory.getSlots(); slot++) {
				ItemStack stack = mainInventory.getStackInSlot(slot);
				if (!(stack.getItem() instanceof BackpackItem)) {
					continue;
				}
				InventoryHandler nestedInventory = BackpackWrapper.fromStack(stack).getInventoryHandler();
				for (int nestedSlot = 0; nestedSlot < nestedInventory.getSlots(); nestedSlot++) {
					removed += nestedInventory.getStackInSlot(nestedSlot).getCount();
					nestedInventory.setStackInSlot(nestedSlot, ItemStack.EMPTY);
				}
				nestedInventory.saveInventory();
			}
			return "{\"ok\":true,\"removed\":" + removed + "}";
		}

		private String seedNestedBackpack(ServerPlayer player, int nestedSlot, Item item, int count) {
			IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
			int inserted = seedNestedBackpack(mainWrapper, nestedSlot, item, count);
			return "{\"ok\":true,\"inserted\":" + inserted + "}";
		}

		private int seedNestedBackpack(IBackpackWrapper mainWrapper, int nestedSlot, Item item, int count) {
			ItemStack nestedStack = mainWrapper.getInventoryHandler().getStackInSlot(nestedSlot);
			if (!(nestedStack.getItem() instanceof BackpackItem)) {
				throw new IllegalStateException("No nested backpack in slot " + nestedSlot);
			}
			InventoryHandler inventory = BackpackWrapper.fromStack(nestedStack).getInventoryHandler();
			int inserted = 0;
			while (inserted < count) {
				int toInsert = Math.min(64, count - inserted);
				ItemStack remaining = inventory.insertItem(new ItemStack(item, toInsert), false);
				inserted += toInsert - remaining.getCount();
				if (!remaining.isEmpty()) {
					break;
				}
			}
			inventory.saveInventory();
			return inserted;
		}

		private String bulkDropFromNestedBackpack(ServerPlayer player, int nestedSlot, Item item, int maxStacks, int pickupDelay, boolean clearCache) {
			IBackpackWrapper mainWrapper = getMainBackpackWrapper(player);
			ItemStack nestedStack = mainWrapper.getInventoryHandler().getStackInSlot(nestedSlot);
			if (!(nestedStack.getItem() instanceof BackpackItem)) {
				return "{\"ok\":false,\"error\":\"No nested backpack in requested slot\"}";
			}
			IBackpackWrapper nestedWrapper = BackpackWrapper.fromStack(nestedStack);
			InventoryHandler inventory = nestedWrapper.getInventoryHandler();
			int dropped = 0;
			int stacksDropped = 0;
			for (int slot = 0; slot < inventory.getSlots() && stacksDropped < maxStacks; slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				if (!stack.is(item)) {
					continue;
				}
				while (!inventory.getStackInSlot(slot).isEmpty() && inventory.getStackInSlot(slot).is(item) && stacksDropped < maxStacks) {
					ItemStack extracted = inventory.extractItem(slot, 64, false);
					if (extracted.isEmpty()) {
						break;
					}
					dropStackAtPlayer(player, extracted, pickupDelay);
					dropped += extracted.getCount();
					stacksDropped++;
				}
			}
			inventory.saveInventory();
			if (clearCache) {
				StorageWrapperRepository.clearCache();
			}
			return "{\"ok\":true,\"dropped\":" + dropped + ",\"stacksDropped\":" + stacksDropped + ",\"pickupDelay\":" + pickupDelay + ",\"cacheCleared\":"
					+ clearCache + "}";
		}

		private void dropStackAtPlayer(ServerPlayer player, ItemStack stack, int pickupDelay) {
			ItemEntity entity = new ItemEntity(player.serverLevel(), player.getX(), player.getY() + 0.5D, player.getZ(), stack);
			entity.setPickUpDelay(pickupDelay);
			player.serverLevel().addFreshEntity(entity);
		}

		private String droppedItemsStatus(ServerPlayer player, Item item) {
			AABB area = player.getBoundingBox().inflate(8D);
			int entities = 0;
			int items = 0;
			for (ItemEntity itemEntity : player.serverLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item))) {
				entities++;
				items += itemEntity.getItem().getCount();
			}
			int playerItems = 0;
			for (ItemStack stack : player.getInventory().items) {
				if (stack.is(item)) {
					playerItems += stack.getCount();
				}
			}
			return "{\"ok\":true,\"entities\":" + entities + ",\"items\":" + items + ",\"playerItems\":" + playerItems + "}";
		}

		private String clearDroppedItems(ServerPlayer player, Item item) {
			AABB area = player.getBoundingBox().inflate(32D);
			int entities = 0;
			int items = 0;
			for (ItemEntity itemEntity : player.serverLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item))) {
				entities++;
				items += itemEntity.getItem().getCount();
				itemEntity.discard();
			}
			return "{\"ok\":true,\"entitiesRemoved\":" + entities + ",\"itemsRemoved\":" + items + "}";
		}

		private String setSurvivalMode(ServerPlayer player) {
			player.setGameMode(GameType.SURVIVAL);
			return "{\"ok\":true,\"gameMode\":\"survival\"}";
		}

		private String fillPlayerInventory(ServerPlayer player) {
			int filled = 0;
			for (int slot = 1; slot < player.getInventory().items.size(); slot++) {
				player.getInventory().items.set(slot, new ItemStack(Items.STONE, 64));
				filled++;
			}
			player.getInventory().setChanged();
			return "{\"ok\":true,\"filled\":" + filled + "}";
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

			ItemStack backpack = createBackpackStack();
			IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
			UpgradeHandler upgrades = wrapper.getUpgradeHandler();
			upgrades.setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_4.get()));
			upgrades.setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_4.get()));
			upgrades.saveInventory();

			InventoryHandler inventory = wrapper.getInventoryHandler();
			inventory.setStackInSlot(0, new ItemStack(Items.IRON_NUGGET, firstSlotCount));
			inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, secondSlotCount));
			// Reserve deterministic destinations: slot 2 for blocks, slot 3 for the trigger nuggets, and no extra ingot slots.
			inventory.setStackInSlot(2, new ItemStack(Items.IRON_BLOCK));
			inventory.setStackInSlot(3, new ItemStack(Items.IRON_NUGGET));
			for (int slot = 4; slot < inventory.getSlots(); slot++) {
				inventory.setStackInSlot(slot, new ItemStack(Items.STONE));
			}
			inventory.saveInventory();

			RecipeHelper.onDataPackSync(null);
			upgrades.setStackInSlot(2, new ItemStack(ModItems.ADVANCED_COMPACTING_UPGRADE.get()));
			upgrades.saveInventory();

			ItemStack insertRemainder = inventory.insertItem(new ItemStack(Items.IRON_NUGGET, triggerCount), false);

			int actualNuggets = countItems(inventory, Items.IRON_NUGGET) - 1;
			int actualIngots = countItems(inventory, Items.IRON_INGOT);
			int actualBlocks = countItems(inventory, Items.IRON_BLOCK) - 1;
			boolean passed = insertRemainder.isEmpty() && actualNuggets == expectedNuggets && actualIngots == expectedIngots && actualBlocks == expectedBlocks;

			inventory.saveInventory();
			player.getInventory().setItem(0, backpack);
			player.getInventory().setChanged();

			String error = passed ? null : "Unexpected compacting result";
			return new AdvancedCompactingHighStackRegressionResult(name, passed, firstSlotCount, secondSlotCount, triggerCount, expectedNuggets, actualNuggets,
					expectedIngots, actualIngots, expectedBlocks, actualBlocks, insertRemainder.getCount(), error);
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
				Item parentBackpackItem = runOnServer(this::setupParentMobCatcherBackpackRegression);
				waitForClientPlayerInventorySlot(0, parentBackpackItem, "mob catcher parent backpack");
				runOnServer(this::openParentBackpackRegression);
				waitForOpenParentBackpackMenu();

				runOnServer(this::insertMobCatcherSubBackpackIntoOpenParent);

				SubMobCatcherRegressionState parentState = waitForParentMobCatcherRegressionState();
				if (!parentState.parentMatches()) {
					return subMobCatcherRegressionJson(name, false, parentState, parentState,
							"Parent backpack mob catcher data did not stay separate after inserting sub backpack");
				}

				runOnServer(this::openSubBackpackColumnUpgradeRegression);
				waitForOpenSubBackpackMenu();

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
				}
				return true;
			});
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			long closedSince = 0;
			do {
				if (runOnClient(() -> Minecraft.getInstance().screen == null
						&& (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer)))) {
					if (closedSince == 0) {
						closedSince = System.nanoTime();
					} else if (System.nanoTime() - closedSince >= TimeUnit.MILLISECONDS.toNanos(250)) {
						return;
					}
				} else {
					closedSince = 0;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);
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
					Item parentBackpackItem = runOnServer(this::setupSubBackpackColumnUpgradeRegression);
					waitForClientPlayerInventorySlot(0, parentBackpackItem, "sub backpack parent");
					runOnServer(this::openParentBackpackRegression);
					waitForOpenParentBackpackMenu();
					runOnServer(this::openSubBackpackColumnUpgradeRegression);
				}
			}
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
			ServerLevel level = player.serverLevel();
			BlockPos pos = getRegressionBackpackPos(player);

			level.setBlock(pos, net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING,
					player.getDirection().getOpposite()), 3);
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
			Minecraft.getInstance().level.setBlock(pos, net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks.GOLD_BACKPACK.get().defaultBlockState()
					.setValue(BackpackBlock.FACING, Minecraft.getInstance().player.getDirection().getOpposite()), 3);
			BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, BackpackBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Failed to create client regression backpack block"));
			backpackBlockEntity.setBackpack(createColumnUpgradeRegressionBackpack());
			return true;
		}

		private Boolean openPlacedBackpackColumnUpgradeRegression(ServerPlayer player) {
			BlockPos pos = getRegressionBackpackPos(player);

			BackpackContext.Block backpackContext = new BackpackContext.Block(pos);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Placed Column Regression")), backpackContext::toBuffer);
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

		private Item setupSubBackpackColumnUpgradeRegression(ServerPlayer player) {
			ItemStack parentBackpack = createParentBackpackWithColumnUpgradeSubBackpack();
			player.getInventory().setItem(0, parentBackpack);
			player.getInventory().setChanged();
			return parentBackpack.getItem();
		}

		private Item setupParentMobCatcherBackpackRegression(ServerPlayer player) {
			ItemStack parentBackpack = createMobCatcherRegressionBackpack(81, 3, SUB_MOB_CATCHER_PARENT_MOB_ID, 0, "Parent Pig");
			player.getInventory().setItem(0, parentBackpack);
			player.getInventory().setChanged();
			return parentBackpack.getItem();
		}

		private void waitForClientPlayerInventorySlot(int slot, Item item, String description) {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			long matchingSince = 0;
			do {
				if (runOnClient(() -> {
					if (Minecraft.getInstance().player == null) {
						return false;
					}
					ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
					return !stack.isEmpty() && stack.getItem() == item;
				})) {
					if (matchingSince == 0) {
						matchingSince = System.nanoTime();
					} else if (System.nanoTime() - matchingSince >= TimeUnit.MILLISECONDS.toNanos(250)) {
						return;
					}
				} else {
					matchingSince = 0;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);

			throw new IllegalStateException("Timed out waiting for client inventory slot " + slot + " to contain " + description + "; "
					+ runOnClient(this::getClientSubBackpackOpenState));
		}

		private Boolean openSubBackpackColumnUpgradeRegression(ServerPlayer player) {
			BackpackContext.ItemSubBackpack backpackContext = new BackpackContext.ItemSubBackpack(PlayerInventoryProvider.MAIN_INVENTORY, "", 0, false, 0,
					true);
			if (player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Sub Column Regression")), backpackContext::toBuffer).isEmpty()) {
				throw new IllegalStateException("Server refused to open sub backpack column regression menu");
			}
			return true;
		}

		private Boolean openParentBackpackRegression(ServerPlayer player) {
			BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Parent Backpack Regression")), backpackContext::toBuffer);
			return true;
		}

		private Boolean insertMobCatcherSubBackpackIntoOpenParent(ServerPlayer player) {
			if (!(player.containerMenu instanceof BackpackContainer menu)) {
				throw new IllegalStateException("Parent backpack menu is not open on server");
			}
			if (menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
				throw new IllegalStateException("Expected parent item backpack menu before inserting sub backpack");
			}
			InventoryHandler inventoryHandler = menu.getStorageWrapper().getInventoryHandler();
			inventoryHandler.setStackInSlot(0, createMobCatcherRegressionBackpack(144, 7, SUB_MOB_CATCHER_SUB_MOB_ID, 10, "Sub Cow"));
			inventoryHandler.saveInventory();
			menu.getStorageWrapper().onContentsNbtUpdated();
			menu.broadcastChanges();
			return true;
		}

		private ItemStack createParentBackpackWithColumnUpgradeSubBackpack() {
			ItemStack parentBackpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
			parentBackpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
			parentBackpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 81);
			parentBackpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 3);
			IBackpackWrapper parentWrapper = BackpackWrapper.fromStack(parentBackpack);
			parentWrapper.setSlotNumbers(81, 3);
			parentWrapper.getInventoryHandler().setStackInSlot(0, createColumnUpgradeRegressionBackpack());
			parentWrapper.getInventoryHandler().saveInventory();
			parentWrapper.onContentsNbtUpdated();
			return parentBackpack;
		}

		private ItemStack createColumnUpgradeRegressionBackpack() {
			ItemStack backpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
			backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
			backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 81);
			backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 3);
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
			backpackWrapper.setSlotNumbers(81, 3);
			backpackWrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
			backpackWrapper.getUpgradeHandler().saveInventory();
			backpackWrapper.setColumnsTaken(2, false);
			backpackWrapper.onContentsNbtUpdated();
			return backpack;
		}

		private ItemStack createMobCatcherRegressionBackpack(int inventorySlots, int upgradeSlots, UUID mobId, int mobSlot, String displayName) {
			ItemStack backpack = inventorySlots > 81 ? new ItemStack(ModItems.DIAMOND_BACKPACK.get()) : new ItemStack(ModItems.GOLD_BACKPACK.get());
			backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
			backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, inventorySlots);
			backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, upgradeSlots);
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
			backpackWrapper.setSlotNumbers(inventorySlots, upgradeSlots);
			backpackWrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
			backpackWrapper.getUpgradeHandler().saveInventory();
			MobCatcherStorage.addCapturedMob(backpackWrapper,
					new CapturedMob(mobId, ResourceLocation.parse("minecraft:pig"), new CompoundTag(), mobSlot, 1, 1, 1, false, displayName, 10, 10));
			backpackWrapper.onContentsNbtUpdated();
			return backpack;
		}

		private void ensureCuriosSlot(ServerPlayer player, String identifier, int slots) {
			PlayerInventoryHandler inventoryHandler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
					.orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
			if (inventoryHandler.getSlotCount(player, identifier) < slots) {
				MinecraftServer server = player.server;
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
						"curios add " + identifier + " " + player.getGameProfile().getName() + " " + slots);
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

			if (!(player instanceof Player inventoryPlayer)) {
				throw new IllegalStateException("No Curios slot type was found for backpack");
			}
			PlayerInventoryHandler inventoryHandler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
					.orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
			for (String identifier : inventoryHandler.getIdentifiers(inventoryPlayer)) {
				if (inventoryHandler.getSlotCount(inventoryPlayer, identifier) > 0) {
					return identifier;
				}
			}
			return "back";
		}

		@SuppressWarnings("unchecked")
		private Map<String, ?> getCuriosItemSlotTypes(ItemStack backpack, LivingEntity player) {
			try {
				Class<?> curiosSlotTypesClass = Class.forName("top.theillusivec4.curios.api.CuriosSlotTypes");
				return (Map<String, ?>) curiosSlotTypesClass.getMethod("getItemSlotTypes", ItemStack.class, LivingEntity.class).invoke(null, backpack, player);
			} catch (ReflectiveOperationException e) {
				return Map.of();
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
				if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
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
				if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
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
				if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
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
				if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK)) {
					return;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);
			throw new IllegalStateException("Timed out waiting for sub backpack screen to open; " + runOnClient(this::getClientSubBackpackOpenState));
		}

		private String getClientSubBackpackOpenState() {
			Minecraft minecraft = Minecraft.getInstance();
			String screen = minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName();
			String menu = minecraft.player == null ? "none" : minecraft.player.containerMenu.getClass().getSimpleName();
			String context = "none";
			int storageSlots = 0;
			if (minecraft.player != null && minecraft.player.containerMenu instanceof BackpackContainer backpackContainer) {
				context = backpackContainer.getBackpackContext().getType().name();
				storageSlots = backpackContainer.getNumberOfStorageInventorySlots();
			}
			String slot0Item = "none";
			String nestedSlot0Item = "none";
			if (minecraft.player != null) {
				ItemStack slot0 = minecraft.player.getInventory().getItem(0);
				slot0Item = BuiltInRegistries.ITEM.getKey(slot0.getItem()).toString();
				if (slot0.getItem() instanceof BackpackItem) {
					ItemStack nested = BackpackWrapper.fromStack(slot0).getInventoryHandler().getStackInSlot(0);
					nestedSlot0Item = BuiltInRegistries.ITEM.getKey(nested.getItem()).toString();
				}
			}
			return "screen=" + screen + ", menu=" + menu + ", context=" + context + ", storageSlots=" + storageSlots + ", slot0=" + slot0Item + ", nestedSlot0="
					+ nestedSlot0Item;
		}

		private PlacedColumnUpgradeClickExpectation clickPlacedBackpackColumnUpgrade() {
			return clickColumnUpgradeSlot(getOpenPlacedBackpackMenu(), "Placed");
		}

		private BackpackContainer getOpenPlacedBackpackMenu() {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen)
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
				throw new IllegalStateException("Placed backpack screen is not open");
			}
			if (menu.getBlockPosition().isEmpty()) {
				throw new IllegalStateException("Open backpack is not a placed backpack");
			}
			return menu;
		}

		private PlacedColumnUpgradeClickExpectation clickCuriosBackpackColumnUpgrade() {
			return clickColumnUpgradeSlot(getOpenCuriosBackpackMenu(), "Curios");
		}

		private BackpackContainer getOpenCuriosBackpackMenu() {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen)
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
				throw new IllegalStateException("Curios backpack screen is not open");
			}
			if (menu.getBlockPosition().isPresent()) {
				throw new IllegalStateException("Open backpack is not a Curios/item backpack");
			}
			return menu;
		}

		private PlacedColumnUpgradeClickExpectation clickSubBackpackColumnUpgrade() {
			return clickColumnUpgradeSlot(getOpenSubBackpackMenu(), "Sub");
		}

		private BackpackContainer getOpenSubBackpackMenu() {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen)
					|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
				throw new IllegalStateException("Sub backpack screen is not open");
			}
			if (menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_SUB_BACKPACK) {
				throw new IllegalStateException("Open backpack is not an item sub backpack");
			}
			return menu;
		}

		private PlacedColumnUpgradeClickExpectation clickColumnUpgradeSlot(BackpackContainer menu, String contextName) {
			if (menu.getNumberOfUpgradeSlots() < 2) {
				throw new IllegalStateException(contextName + " backpack needs at least two upgrade slots");
			}
			if (!menu.getCarried().isEmpty()) {
				throw new IllegalStateException("Cursor must be empty before running " + contextName.toLowerCase(Locale.ROOT) + " backpack regression");
			}

			Slot slot = menu.upgradeSlots.get(1);
			ItemStack slotStack = slot.getItem();
			if (slotStack.isEmpty() || !(slotStack.getItem() instanceof IUpgradeItem<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
				throw new IllegalStateException(contextName + " backpack upgrade slot 1 must contain a column-taking upgrade");
			}

			int beforeColumnsTaken = menu.getStorageWrapper().getColumnsTaken();
			int columnsChange = upgradeItem.getInventoryColumnsTaken();
			int expectedColumnsTaken = beforeColumnsTaken - columnsChange;
			int rows = menu.getStorageWrapper().getNumberOfSlotRows();
			int handlerSlots = menu.getStorageWrapper().getInventoryHandler().getSlots();
			int baseColumns = handlerSlots <= 81 ? 9 : 12;
			int baseStorageSlots = handlerSlots / rows == baseColumns ? handlerSlots : handlerSlots + beforeColumnsTaken * rows;
			int expectedStorageSlots = baseStorageSlots - expectedColumnsTaken * rows;

			clickSlot(Minecraft.getInstance().screen, slot);
			return new PlacedColumnUpgradeClickExpectation(expectedColumnsTaken, expectedStorageSlots);
		}

		private void clickSlot(Screen screen, Slot slot) {
			clickSlot(screen, slot, 0);
		}

		private void clickSlot(Screen screen, Slot slot, int button) {
			int leftPos = getScreenIntField(screen, "leftPos");
			int topPos = getScreenIntField(screen, "topPos");
			double x = leftPos + slot.x + 8.0;
			double y = topPos + slot.y + 8.0;
			if (!screen.mouseClicked(x, y, button)) {
				throw new IllegalStateException("Backpack upgrade slot click was not handled");
			}
			screen.mouseReleased(x, y, button);
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
			return getBackpackColumnUpgradeState(menu);
		}

		private PlacedColumnUpgradeState getCuriosBackpackColumnUpgradeState() {
			BackpackContainer menu = getOpenCuriosBackpackMenu();
			return getBackpackColumnUpgradeState(menu);
		}

		private PlacedColumnUpgradeState getSubBackpackColumnUpgradeState() {
			BackpackContainer menu = getOpenSubBackpackMenu();
			return getBackpackColumnUpgradeState(menu);
		}

		private PlacedColumnUpgradeState getBackpackColumnUpgradeState(BackpackContainer menu) {
			return new PlacedColumnUpgradeState(menu.getStorageWrapper().getColumnsTaken(), menu.getNumberOfStorageInventorySlots(),
					menu.getStorageWrapper().getInventoryHandler().getSlots(), menu.upgradeSlots.get(1).getItem().isEmpty(), !menu.getCarried().isEmpty());
		}

		private String placedColumnUpgradeRegressionJson(String name, boolean ok, PlacedColumnUpgradeClickExpectation expectation,
				PlacedColumnUpgradeState state, String error) {
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

		private SubMobCatcherRegressionState waitForParentMobCatcherRegressionState() {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			SubMobCatcherRegressionState state = runOnClient(this::getCurrentMobCatcherRegressionStateSafely);
			do {
				state = runOnClient(this::getCurrentMobCatcherRegressionStateSafely);
				if (state.parentMatches()) {
					return state;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);

			return state;
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
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen)
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
			return BackpackWrapper.fromStack(nestedBackpack);
		}

		private SubMobCatcherRegressionState getSubMobCatcherRegressionState(BackpackContainer menu, IBackpackWrapper nestedWrapper) {
			List<CapturedMob> currentMobs = MobCatcherStorage.getCapturedMobs(menu.getStorageWrapper());
			List<CapturedMob> nestedMobs = nestedWrapper == null ? List.of() : MobCatcherStorage.getCapturedMobs(nestedWrapper);
			return new SubMobCatcherRegressionState(menu.getBackpackContext().getType().name(), menu.getStorageWrapper().getInventoryHandler().getSlots(),
					nestedWrapper != null, currentMobs.size(), currentMobs.isEmpty() ? null : currentMobs.get(0).id().toString(), nestedMobs.size(),
					nestedMobs.isEmpty() ? null : nestedMobs.get(0).id().toString());
		}

		private String subMobCatcherRegressionJson(String name, boolean ok, SubMobCatcherRegressionState parentState, SubMobCatcherRegressionState subState,
				String error) {
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

		private String runBackpackStorageGuiRegressions() {
			StorageGuiRegressionSuite suite = loadStorageGuiRegressionSuite();
			List<StorageGuiRegressionResult> results = new ArrayList<>();
			for (StorageGuiRegressionScenario scenario : suite.scenarios()) {
				results.add(runStorageGuiRegressionScenario(scenario));
			}

			long failed = results.stream().filter(result -> !result.passed()).count();
			StringBuilder json = new StringBuilder("{\"ok\":").append(failed == 0).append(",\"total\":").append(results.size()).append(",\"failed\":")
					.append(failed).append(",\"results\":[");
			for (int i = 0; i < results.size(); i++) {
				if (i > 0) {
					json.append(',');
				}
				StorageGuiRegressionResult result = results.get(i);
				json.append('{').append(jsonProperty("name", result.name())).append(",\"passed\":").append(result.passed()).append(",\"actions\":")
						.append(result.actions()).append(",\"storageSlots\":").append(result.storageSlots()).append(",\"menuSlots\":")
						.append(result.menuSlots()).append(",\"upgradeSlots\":").append(result.upgradeSlots()).append(',')
						.append(jsonProperty("error", result.error())).append('}');
			}
			json.append("]}");

			return json.toString();
		}

		private StorageGuiRegressionResult runStorageGuiRegressionScenario(StorageGuiRegressionScenario scenario) {
			try {
				resetBackpackGuiRegressionState();
				runOnServer(player -> setupPlacedBackpackStorageGuiRegression(player, scenario));
				runOnClient(() -> setupClientPlacedBackpackStorageGuiRegression(scenario));
				runOnServer(player -> openPlacedBackpackStorageGuiRegression(player, scenario));
				waitForOpenPlacedBackpackMenu();

				Map<String, List<StorageGuiSlotSnapshot>> slotSnapshots = new HashMap<>();
				for (StorageGuiAction action : scenario.actions()) {
					runStorageGuiAction(scenario, action, slotSnapshots);
				}

				StorageGuiRegressionState state = runOnClient(this::getStorageGuiRegressionState);
				return new StorageGuiRegressionResult(scenario.name(), true, scenario.actions().length, state.storageSlots(), state.menuSlots(),
						state.upgradeSlots(), null);
			} catch (RuntimeException e) {
				StorageGuiRegressionState state = runOnClient(this::getStorageGuiRegressionStateSafely);
				return new StorageGuiRegressionResult(scenario.name(), false, scenario.actions().length, state.storageSlots(), state.menuSlots(),
						state.upgradeSlots(), e.getMessage());
			}
		}

		private void runStorageGuiAction(StorageGuiRegressionScenario scenario, StorageGuiAction action,
				Map<String, List<StorageGuiSlotSnapshot>> slotSnapshots) {
			switch (action.type()) {
				case "assertMenuSlotLayout" -> runOnClient(() -> assertStorageGuiMenuSlotLayout(scenario));
				case "assertProtectedStorageSlots" -> runOnClient(() -> assertProtectedStorageSlots(scenario, action.slots()));
				case "assertTrashSlotIndexCompatibility" -> runOnClient(() -> assertTrashSlotIndexCompatibility(action.slots()));
				case "assertScreenFindSlots" -> runOnClient(() -> assertScreenFindSlots(action.slots()));
				case "assertSlotRefsFind" -> runOnClient(() -> assertSlotRefsFind(action.slotRefs()));
				case "assertSlotRefsNotFind" -> runOnClient(() -> assertSlotRefsNotFind(action.slotRefs()));
				case "assertUpgradeTabSlots" -> runOnClient(() -> assertUpgradeTabSlots(action.slotRefs()));
				case "assertMobCatcherSlots" -> runOnClient(() -> assertMobCatcherSlots(scenario));
				case "assertCarriedEmpty" -> runOnClient(this::assertStorageGuiCarriedEmpty);
				case "assertCarriedStack" -> runOnClient(() -> assertStorageGuiCarriedStack(action));
				case "assertSlotContents" -> runOnClient(() -> assertStorageGuiSlotContents(action));
				case "snapshotSlotContents" -> slotSnapshots.put(action.snapshot(), runOnClient(() -> snapshotStorageGuiSlotContents(action)));
				case "assertSlotContentsUnchanged" -> runOnClient(() -> assertStorageGuiSlotContentsUnchanged(action, slotSnapshots));
				case "assertColumnState" -> runOnClient(() -> assertStorageGuiColumnState(action));
				case "setCarriedStack" -> runStorageGuiSetCarriedStack(action);
				case "clickSlot" -> runOnClient(() -> clickStorageGuiSlot(scenario, action));
				case "shiftClickSlot" -> runOnClient(() -> shiftClickStorageGuiSlot(scenario, action));
				case "pickupAllSlot" -> runOnClient(() -> pickupAllStorageGuiSlot(scenario, action));
				case "hotbarSwapSlot" -> runOnClient(() -> hotbarSwapStorageGuiSlot(scenario, action));
				case "throwSlot" -> runOnClient(() -> throwStorageGuiSlot(scenario, action));
				case "scrollStorage" -> runOnClient(() -> scrollStorageGui(action));
				case "dragCarriedStack" -> runStorageGuiDragCarriedStack(scenario, action);
				case "clickColumnUpgrade" -> runStorageGuiColumnUpgradeAction(action);
				default -> throw new IllegalArgumentException("Unknown storage GUI regression action " + action.type());
			}
		}

		private Boolean assertStorageGuiMenuSlotLayout(StorageGuiRegressionScenario scenario) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			String error = getStorageGuiMenuSlotLayoutError(menu);
			if (error != null) {
				throw new IllegalStateException(scenario.name() + ": " + error);
			}
			return true;
		}

		private String getStorageGuiMenuSlotLayoutError(BackpackContainer menu) {
			if (menu.getInventorySlotsSize() != menu.slots.size()) {
				return "inventory slot size does not match menu.slots size";
			}
			int expectedInventorySlots = menu.getNumberOfStorageInventorySlots() + StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS;
			if (menu.getInventorySlotsSize() != expectedInventorySlots) {
				return "inventory slot size mismatch expected=" + expectedInventorySlots + " actual=" + menu.getInventorySlotsSize();
			}
			if (menu.getFirstUpgradeSlot() != menu.slots.size()) {
				return "first upgrade slot does not start after menu.slots";
			}
			if (menu.getTotalSlotsNumber() != menu.slots.size() + menu.upgradeSlots.size()) {
				return "total slot count does not equal inventory plus upgrade slots";
			}

			for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
				Slot slot = menu.slots.get(slotId);
				if (slot.index != slotId) {
					return "menu slot " + slotId + " has Slot.index " + slot.index;
				}
				if (menu.getSlot(slotId) != slot) {
					return "getSlot(" + slotId + ") does not return menu.slots entry";
				}
				if (slotId < menu.getNumberOfStorageInventorySlots() && slot.getContainerSlot() != slotId) {
					return "storage menu slot " + slotId + " points to backing slot " + slot.getContainerSlot();
				}
			}

			for (int upgradeSlot = 0; upgradeSlot < menu.upgradeSlots.size(); upgradeSlot++) {
				int logicalSlot = menu.getFirstUpgradeSlot() + upgradeSlot;
				Slot slot = menu.upgradeSlots.get(upgradeSlot);
				if (slot.index != logicalSlot) {
					return "upgrade slot " + upgradeSlot + " has Slot.index " + slot.index + " expected " + logicalSlot;
				}
				if (menu.getSlot(logicalSlot) != slot) {
					return "getSlot(" + logicalSlot + ") does not return upgrade slot";
				}
			}

			return null;
		}

		private Boolean assertProtectedStorageSlots(StorageGuiRegressionScenario scenario, int[] slots) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			NoSortSettingsCategory noSortSettings = menu.getStorageWrapper().getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class);
			MemorySettingsCategory memorySettings = menu.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			for (int slotId : slots) {
				assertStorageSlotPresent(menu, slotId);
				if (contains(scenario.noSortSlots(), slotId) && !noSortSettings.isSlotSelected(slotId)) {
					throw new IllegalStateException("No Sort slot " + slotId + " was not selected");
				}
				if (contains(scenario.memorySlots(), slotId) && !memorySettings.isSlotSelected(slotId)) {
					throw new IllegalStateException("Memory slot " + slotId + " was not selected");
				}
			}
			return true;
		}

		private Boolean assertTrashSlotIndexCompatibility(int[] slots) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			for (int slotId : slots) {
				Slot slot = assertStorageSlotPresent(menu, slotId);
				if (slot.index < 0 || slot.index >= menu.slots.size()) {
					throw new IllegalStateException("Slot " + slotId + " has index outside menu.slots: " + slot.index);
				}
				Slot slotByIndex = menu.slots.get(slot.index);
				if (slotByIndex != slot) {
					throw new IllegalStateException("menu.slots.get(Slot.index) did not resolve storage slot " + slotId);
				}
				if (!ItemStack.matches(slot.getItem(), slotByIndex.getItem())) {
					throw new IllegalStateException("menu.slots.get(Slot.index) item mismatch for storage slot " + slotId);
				}
			}
			return true;
		}

		private Boolean assertScreenFindSlots(int[] slots) {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
				throw new IllegalStateException("Backpack screen is not open");
			}
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			for (int slotId : slots) {
				Slot slot = assertStorageSlotPresent(menu, slotId);
				double x = screen.getGuiLeft() + slot.x + 8.0;
				double y = screen.getGuiTop() + slot.y + 8.0;
				Slot foundSlot = screen.findSlot(x, y);
				if (foundSlot != slot) {
					throw new IllegalStateException(
							"findSlot did not return storage slot " + slotId + "; found=" + (foundSlot == null ? "null" : foundSlot.index));
				}
			}
			return true;
		}

		private Boolean assertSlotRefsFind(SlotRef[] slotRefs) {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
				throw new IllegalStateException("Backpack screen is not open");
			}
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			for (SlotRef slotRef : slotRefs) {
				Slot slot = resolveStorageGuiSlot(menu, slotRef);
				Slot foundSlot = screen.findSlot(getSlotCenterX(screen, slot), getSlotCenterY(screen, slot));
				if (foundSlot != slot) {
					throw new IllegalStateException("findSlot did not return " + slotRef + "; found=" + (foundSlot == null ? "null" : foundSlot.index));
				}
			}
			return true;
		}

		private Boolean assertSlotRefsNotFind(SlotRef[] slotRefs) {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
				throw new IllegalStateException("Backpack screen is not open");
			}
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			for (SlotRef slotRef : slotRefs) {
				Slot slot = resolveStorageGuiSlot(menu, slotRef);
				Slot foundSlot = screen.findSlot(getSlotCenterX(screen, slot), getSlotCenterY(screen, slot));
				if (foundSlot == slot) {
					throw new IllegalStateException("findSlot unexpectedly returned hidden/inactive " + slotRef);
				}
			}
			return true;
		}

		private Boolean assertUpgradeTabSlots(SlotRef[] slotRefs) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			for (SlotRef slotRef : slotRefs) {
				if (!"upgradeTab".equals(slotRef.area())) {
					throw new IllegalArgumentException("assertUpgradeTabSlots requires upgradeTab refs");
				}
				Slot slot = resolveStorageGuiSlot(menu, slotRef);
				if (slot.index < menu.getFirstUpgradeSlot()) {
					throw new IllegalStateException("Upgrade tab slot " + slotRef + " is not in logical upgrade range: " + slot.index);
				}
				if (menu.getSlot(slot.index) != slot) {
					throw new IllegalStateException("getSlot(" + slot.index + ") does not return upgrade tab slot " + slotRef);
				}
			}
			return true;
		}

		private Boolean assertMobCatcherSlots(StorageGuiRegressionScenario scenario) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			List<CapturedMob> capturedMobs = MobCatcherStorage.getCapturedMobs(menu.getStorageWrapper());
			if (capturedMobs.size() != scenario.capturedMobs().length) {
				throw new IllegalStateException("Captured mob count mismatch expected=" + scenario.capturedMobs().length + " actual=" + capturedMobs.size());
			}
			int columns = MobCatcherStorage.getColumns(menu.getStorageWrapper());
			for (CapturedMob capturedMob : capturedMobs) {
				for (int y = 0; y < capturedMob.height(); y++) {
					for (int x = 0; x < capturedMob.width(); x++) {
						int slotId = capturedMob.slot() + x + y * columns;
						assertStorageSlotPresent(menu, slotId);
					}
				}
			}
			return true;
		}

		private Boolean assertStorageGuiCarriedEmpty() {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			if (!menu.getCarried().isEmpty()) {
				throw new IllegalStateException("Storage GUI cursor is not empty: " + menu.getCarried());
			}
			return true;
		}

		private Boolean assertStorageGuiCarriedStack(StorageGuiAction action) {
			ItemStack carried = getOpenPlacedBackpackMenu().getCarried();
			assertStackMatches("carried stack", carried, action.item(), action.count());
			return true;
		}

		private Boolean assertStorageGuiSlotContents(StorageGuiAction action) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			Slot slot = resolveStorageGuiSlot(menu, action.slot());
			assertStackMatches(action.slot().toString(), slot.getItem(), action.item(), action.count());
			return true;
		}

		private List<StorageGuiSlotSnapshot> snapshotStorageGuiSlotContents(StorageGuiAction action) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			return Arrays.stream(action.slotRefs()).map(slotRef -> new StorageGuiSlotSnapshot(slotRef, resolveStorageGuiSlot(menu, slotRef).getItem().copy()))
					.toList();
		}

		private Boolean assertStorageGuiSlotContentsUnchanged(StorageGuiAction action, Map<String, List<StorageGuiSlotSnapshot>> slotSnapshots) {
			List<StorageGuiSlotSnapshot> snapshots = slotSnapshots.get(action.snapshot());
			if (snapshots == null) {
				throw new IllegalStateException("Missing storage GUI slot snapshot " + action.snapshot());
			}

			BackpackContainer menu = getOpenPlacedBackpackMenu();
			for (StorageGuiSlotSnapshot snapshot : snapshots) {
				ItemStack currentStack = resolveStorageGuiSlot(menu, snapshot.slotRef()).getItem();
				if (!ItemStack.matches(snapshot.stack(), currentStack)) {
					throw new IllegalStateException("Slot " + snapshot.slotRef() + " changed after snapshot " + action.snapshot() + "; expected="
							+ snapshot.stack() + " actual=" + currentStack);
				}
			}
			return true;
		}

		private void assertStackMatches(String name, ItemStack stack, Optional<Item> expectedItem, int expectedCount) {
			if (expectedItem.isEmpty()) {
				if (!stack.isEmpty()) {
					throw new IllegalStateException(name + " expected empty but was " + stack);
				}
				return;
			}
			if (stack.isEmpty()) {
				throw new IllegalStateException(name + " expected " + expectedItem.get() + " but was empty");
			}
			if (stack.getItem() != expectedItem.get()) {
				throw new IllegalStateException(name + " expected item " + expectedItem.get() + " but was " + stack.getItem());
			}
			if (expectedCount >= 0 && stack.getCount() != expectedCount) {
				throw new IllegalStateException(name + " expected count " + expectedCount + " but was " + stack.getCount());
			}
		}

		private Boolean assertStorageGuiColumnState(StorageGuiAction action) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			if (action.expectedColumnsTaken() >= 0 && menu.getStorageWrapper().getColumnsTaken() != action.expectedColumnsTaken()) {
				throw new IllegalStateException(
						"Expected columnsTaken=" + action.expectedColumnsTaken() + " but was " + menu.getStorageWrapper().getColumnsTaken());
			}
			if (action.expectedStorageSlots() >= 0 && menu.getNumberOfStorageInventorySlots() != action.expectedStorageSlots()) {
				throw new IllegalStateException(
						"Expected storageSlots=" + action.expectedStorageSlots() + " but was " + menu.getNumberOfStorageInventorySlots());
			}
			return true;
		}

		private Slot assertStorageSlotPresent(BackpackContainer menu, int slotId) {
			if (slotId < 0 || slotId >= menu.getNumberOfStorageInventorySlots()) {
				throw new IllegalStateException("Storage slot " + slotId + " is outside storage slot count " + menu.getNumberOfStorageInventorySlots());
			}
			if (slotId >= menu.slots.size()) {
				throw new IllegalStateException("Storage slot " + slotId + " is outside menu.slots size " + menu.slots.size());
			}
			Slot slot = menu.getSlot(slotId);
			if (menu.slots.get(slotId) != slot) {
				throw new IllegalStateException("Storage slot " + slotId + " is not present at matching menu.slots index");
			}
			return slot;
		}

		private Boolean clickStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			Slot slot = resolveStorageGuiSlot(menu, action.slot());
			clickSlot(Minecraft.getInstance().screen, slot, action.button());
			assertStorageGuiMenuSlotLayout(scenario);
			return true;
		}

		private Boolean shiftClickStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null) {
				throw new IllegalStateException("Client player/gameMode is not available");
			}
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			Slot slot = resolveStorageGuiSlot(menu, action.slot());
			minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.QUICK_MOVE, minecraft.player);
			assertStorageGuiMenuSlotLayout(scenario);
			return true;
		}

		private Boolean pickupAllStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
			handleStorageGuiClickType(scenario, action, 0, ClickType.PICKUP_ALL);
			return true;
		}

		private Boolean hotbarSwapStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
			handleStorageGuiClickType(scenario, action, action.hotbarSlot(), ClickType.SWAP);
			return true;
		}

		private Boolean throwStorageGuiSlot(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
			handleStorageGuiClickType(scenario, action, action.button(), ClickType.THROW);
			return true;
		}

		private void handleStorageGuiClickType(StorageGuiRegressionScenario scenario, StorageGuiAction action, int data, ClickType clickType) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null) {
				throw new IllegalStateException("Client player/gameMode is not available");
			}
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			Slot slot = resolveStorageGuiSlot(menu, action.slot());
			minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, data, clickType, minecraft.player);
			assertStorageGuiMenuSlotLayout(scenario);
		}

		private void runStorageGuiSetCarriedStack(StorageGuiAction action) {
			ItemStack stack = action.item().map(item -> new ItemStack(item, action.count())).orElse(ItemStack.EMPTY);
			runOnServer(player -> setStorageGuiCarriedStack(player, stack));
			runOnClient(() -> setStorageGuiClientCarriedStack(stack));
		}

		private Boolean scrollStorageGui(StorageGuiAction action) {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
				throw new IllegalStateException("Backpack screen is not open");
			}
			double x = screen.getGuiLeft() + 20.0;
			double y = screen.getGuiTop() + 30.0;
			int steps = action.count() == 0 ? 1 : Math.abs(action.count());
			double scrollY = action.count() < 0 ? 1.0 : -1.0;
			for (int i = 0; i < steps; i++) {
				screen.mouseScrolled(x, y, 0.0, scrollY);
			}
			return true;
		}

		private void runStorageGuiDragCarriedStack(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
			Item item = action.item().orElseThrow(() -> new IllegalArgumentException("Drag action needs an item"));
			runOnServer(player -> setStorageGuiCarriedStack(player, item, action.count()));
			runOnClient(() -> setStorageGuiClientCarriedStack(item, action.count()));
			runOnClient(() -> dragStorageGuiCarriedStack(scenario, action));
		}

		private Boolean dragStorageGuiCarriedStack(StorageGuiRegressionScenario scenario, StorageGuiAction action) {
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen screen)) {
				throw new IllegalStateException("Backpack screen is not open");
			}
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			if (action.slotRefs().length == 0) {
				throw new IllegalArgumentException("Drag action needs slots");
			}
			Slot firstSlot = resolveStorageGuiSlot(menu, action.slotRefs()[0]);
			double previousX = getSlotCenterX(screen, firstSlot);
			double previousY = getSlotCenterY(screen, firstSlot);
			screen.mouseClicked(previousX, previousY, action.button());
			for (SlotRef slotRef : action.slotRefs()) {
				Slot slot = resolveStorageGuiSlot(menu, slotRef);
				double x = getSlotCenterX(screen, slot);
				double y = getSlotCenterY(screen, slot);
				screen.mouseDragged(x, y, action.button(), x - previousX, y - previousY);
				previousX = x;
				previousY = y;
			}
			screen.mouseReleased(previousX, previousY, action.button());
			assertStorageGuiMenuSlotLayout(scenario);
			return true;
		}

		private void runStorageGuiColumnUpgradeAction(StorageGuiAction action) {
			if ("insert".equals(action.operation())) {
				Item item = action.item().orElseThrow(() -> new IllegalArgumentException("Column upgrade insert action needs an item"));
				runOnServer(player -> setStorageGuiCarriedStack(player, item));
				runOnClient(() -> setStorageGuiClientCarriedStack(item));
			}

			StorageGuiColumnUpgradeExpectation expectation = runOnClient(() -> clickStorageGuiColumnUpgrade(action));
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			StorageGuiColumnUpgradeState state;
			do {
				state = runOnClient(() -> getStorageGuiColumnUpgradeState(action.upgradeSlot()));
				if (state.matches(expectation) && runOnClient(() -> getStorageGuiMenuSlotLayoutError(getOpenPlacedBackpackMenu()) == null)) {
					return;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);

			throw new IllegalStateException(
					"Timed out waiting for column upgrade " + action.operation() + " sync; expected=" + expectation + " actual=" + state);
		}

		private Boolean setStorageGuiCarriedStack(ServerPlayer player, Item item) {
			return setStorageGuiCarriedStack(player, item, 1);
		}

		private Boolean setStorageGuiCarriedStack(ServerPlayer player, Item item, int count) {
			return setStorageGuiCarriedStack(player, new ItemStack(item, count));
		}

		private Boolean setStorageGuiCarriedStack(ServerPlayer player, ItemStack stack) {
			if (!(player.containerMenu instanceof BackpackContainer menu)) {
				throw new IllegalStateException("Backpack menu is not open on server");
			}
			menu.setCarried(stack.copy());
			return true;
		}

		private Boolean setStorageGuiClientCarriedStack(Item item) {
			return setStorageGuiClientCarriedStack(item, 1);
		}

		private Boolean setStorageGuiClientCarriedStack(Item item, int count) {
			return setStorageGuiClientCarriedStack(new ItemStack(item, count));
		}

		private Boolean setStorageGuiClientCarriedStack(ItemStack stack) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			menu.setCarried(stack.copy());
			return true;
		}

		private Slot resolveStorageGuiSlot(BackpackContainer menu, SlotRef slotRef) {
			return switch (slotRef.area()) {
				case "storage" -> assertStorageSlotPresent(menu, slotRef.index());
				case "player" -> resolvePlayerSlot(menu, slotRef.index());
				case "upgrade" -> resolveUpgradeSlot(menu, slotRef.index());
				case "upgradeTab" -> resolveUpgradeTabSlot(menu, slotRef.upgradeSlot(), slotRef.index());
				default -> throw new IllegalArgumentException("Unknown storage GUI slot area " + slotRef.area());
			};
		}

		private Slot resolvePlayerSlot(BackpackContainer menu, int playerSlotIndex) {
			int slotId = menu.getNumberOfStorageInventorySlots() + playerSlotIndex;
			if (playerSlotIndex < 0 || slotId >= menu.getInventorySlotsSize()) {
				throw new IllegalStateException("Player menu slot " + playerSlotIndex + " is outside inventory slot range");
			}
			return menu.getSlot(slotId);
		}

		private Slot resolveUpgradeSlot(BackpackContainer menu, int upgradeSlot) {
			if (upgradeSlot < 0 || upgradeSlot >= menu.getNumberOfUpgradeSlots()) {
				throw new IllegalStateException("Upgrade slot " + upgradeSlot + " is outside upgrade slot range " + menu.getNumberOfUpgradeSlots());
			}
			return menu.upgradeSlots.get(upgradeSlot);
		}

		private Slot resolveUpgradeTabSlot(BackpackContainer menu, int upgradeSlot, int tabSlot) {
			UpgradeContainerBase<?, ?> container = menu.getUpgradeContainers().get(upgradeSlot);
			if (container == null) {
				throw new IllegalStateException("Upgrade slot " + upgradeSlot + " does not have an upgrade container");
			}
			if (tabSlot < 0 || tabSlot >= container.getSlots().size()) {
				throw new IllegalStateException("Upgrade tab slot " + tabSlot + " is outside tab slot range " + container.getSlots().size());
			}
			return container.getSlots().get(tabSlot);
		}

		private double getSlotCenterX(AbstractContainerScreen<?> screen, Slot slot) {
			return screen.getGuiLeft() + slot.x + 8.0;
		}

		private double getSlotCenterY(AbstractContainerScreen<?> screen, Slot slot) {
			return screen.getGuiTop() + slot.y + 8.0;
		}

		private StorageGuiColumnUpgradeExpectation clickStorageGuiColumnUpgrade(StorageGuiAction action) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			if (action.upgradeSlot() < 0 || action.upgradeSlot() >= menu.upgradeSlots.size()) {
				throw new IllegalStateException("Invalid upgrade slot " + action.upgradeSlot());
			}

			Slot slot = menu.upgradeSlots.get(action.upgradeSlot());
			ItemStack upgradeStack = "insert".equals(action.operation()) ? menu.getCarried() : slot.getItem();
			if (upgradeStack.isEmpty() || !(upgradeStack.getItem() instanceof IUpgradeItem<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
				throw new IllegalStateException("Column upgrade action needs a column-taking upgrade stack");
			}

			int beforeColumnsTaken = menu.getStorageWrapper().getColumnsTaken();
			int columnsChange = upgradeItem.getInventoryColumnsTaken();
			int expectedColumnsTaken = switch (action.operation()) {
				case "insert" -> beforeColumnsTaken + columnsChange;
				case "remove" -> beforeColumnsTaken - columnsChange;
				default -> throw new IllegalArgumentException("Unknown column upgrade operation " + action.operation());
			};
			int rows = menu.getStorageWrapper().getNumberOfSlotRows();
			int handlerSlots = menu.getStorageWrapper().getInventoryHandler().getSlots();
			int baseColumns = handlerSlots <= 81 ? 9 : 12;
			int baseStorageSlots = handlerSlots / rows == baseColumns ? handlerSlots : handlerSlots + beforeColumnsTaken * rows;
			int expectedStorageSlots = baseStorageSlots - expectedColumnsTaken * rows;

			clickSlot(Minecraft.getInstance().screen, slot);
			return new StorageGuiColumnUpgradeExpectation(expectedColumnsTaken, expectedStorageSlots, "remove".equals(action.operation()),
					"insert".equals(action.operation()));
		}

		private StorageGuiColumnUpgradeState getStorageGuiColumnUpgradeState(int upgradeSlot) {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			if (upgradeSlot < 0 || upgradeSlot >= menu.upgradeSlots.size()) {
				throw new IllegalStateException("Invalid upgrade slot " + upgradeSlot);
			}
			return new StorageGuiColumnUpgradeState(menu.getStorageWrapper().getColumnsTaken(), menu.getNumberOfStorageInventorySlots(),
					menu.upgradeSlots.get(upgradeSlot).getItem().isEmpty(), menu.getCarried().isEmpty());
		}

		private StorageGuiRegressionState getStorageGuiRegressionState() {
			BackpackContainer menu = getOpenPlacedBackpackMenu();
			return new StorageGuiRegressionState(menu.getNumberOfStorageInventorySlots(), menu.slots.size(), menu.upgradeSlots.size());
		}

		private StorageGuiRegressionState getStorageGuiRegressionStateSafely() {
			try {
				if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu) {
					return new StorageGuiRegressionState(menu.getNumberOfStorageInventorySlots(), menu.slots.size(), menu.upgradeSlots.size());
				}
			} catch (RuntimeException ignored) {
				// Return an empty state below so the regression response still explains the failure.
			}
			return new StorageGuiRegressionState(0, 0, 0);
		}

		private Boolean setupPlacedBackpackStorageGuiRegression(ServerPlayer player, StorageGuiRegressionScenario scenario) {
			ServerLevel level = player.serverLevel();
			BlockPos pos = getRegressionBackpackPos(player);
			applyStorageGuiPlayerContents(player, scenario.playerContents());
			level.setBlock(pos, net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING,
					player.getDirection().getOpposite()), 3);
			BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(level, pos, BackpackBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Failed to place storage GUI regression backpack block"));
			backpackBlockEntity.setBackpack(createStorageGuiRegressionBackpack(scenario));
			return true;
		}

		private Boolean setupClientPlacedBackpackStorageGuiRegression(StorageGuiRegressionScenario scenario) {
			if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
				throw new IllegalStateException("Client level/player is not available");
			}
			applyStorageGuiPlayerContents(Minecraft.getInstance().player, scenario.playerContents());
			BlockPos pos = getRegressionBackpackPos(Minecraft.getInstance().player);
			Minecraft.getInstance().level.setBlock(pos, net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks.GOLD_BACKPACK.get().defaultBlockState()
					.setValue(BackpackBlock.FACING, Minecraft.getInstance().player.getDirection().getOpposite()), 3);
			BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, BackpackBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Failed to create client storage GUI regression backpack block"));
			backpackBlockEntity.setBackpack(createStorageGuiRegressionBackpack(scenario));
			return true;
		}

		private Boolean openPlacedBackpackStorageGuiRegression(ServerPlayer player, StorageGuiRegressionScenario scenario) {
			BackpackContext.Block backpackContext = new BackpackContext.Block(getRegressionBackpackPos(player));
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Storage GUI Regression: " + scenario.name())), backpackContext::toBuffer);
			return true;
		}

		private ItemStack createStorageGuiRegressionBackpack(StorageGuiRegressionScenario scenario) {
			ItemStack backpack = scenario.inventorySlots() > 81 ? new ItemStack(ModItems.DIAMOND_BACKPACK.get()) : new ItemStack(ModItems.GOLD_BACKPACK.get());
			backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
			backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, scenario.inventorySlots());
			backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, scenario.upgradeSlots());
			IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
			wrapper.setSlotNumbers(scenario.inventorySlots(), scenario.upgradeSlots());
			InventoryHandler inventory = wrapper.getInventoryHandler();
			for (SlotStackSpec content : scenario.contents()) {
				inventory.setStackInSlot(content.slot(), new ItemStack(content.item(), content.count()));
			}
			inventory.saveInventory();
			UpgradeHandler upgrades = wrapper.getUpgradeHandler();
			for (SlotStackSpec content : scenario.upgradeContents()) {
				upgrades.setStackInSlot(content.slot(), new ItemStack(content.item(), content.count()));
			}
			upgrades.saveInventory();
			applyProtectedSlots(wrapper, scenario.noSortSlots(), scenario.memorySlots());
			for (CapturedMobSpec capturedMob : scenario.capturedMobs()) {
				MobCatcherStorage.addCapturedMob(wrapper,
						new CapturedMob(UUID.randomUUID(), ResourceLocation.parse(capturedMob.entityType()), new CompoundTag(), capturedMob.slot(),
								capturedMob.width(), capturedMob.height(), capturedMob.width() * capturedMob.height(), false, capturedMob.entityType(), 10,
								10));
			}
			if (scenario.openTab() >= 0) {
				wrapper.setOpenTabId(scenario.openTab());
			}
			wrapper.setColumnsTaken(scenario.columnsTaken(), false);
			wrapper.onContentsNbtUpdated();
			return backpack;
		}

		private void applyStorageGuiPlayerContents(Player player, SlotStackSpec[] playerContents) {
			player.getInventory().clearContent();
			for (SlotStackSpec content : playerContents) {
				player.getInventory().setItem(content.slot(), new ItemStack(content.item(), content.count()));
			}
			player.getInventory().setChanged();
		}

		private StorageGuiRegressionSuite loadStorageGuiRegressionSuite() {
			try (InputStream inputStream = DevClientAutomation.class.getResourceAsStream("/devclientautomation/storage_gui_regressions.json")) {
				if (inputStream == null) {
					throw new IllegalStateException("Missing storage GUI regression definitions");
				}
				JsonObject root = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
				JsonArray scenarioElements = root.getAsJsonArray("scenarios");
				List<StorageGuiRegressionScenario> scenarios = new ArrayList<>();
				for (JsonElement scenarioElement : scenarioElements) {
					JsonObject scenario = scenarioElement.getAsJsonObject();
					scenarios.add(new StorageGuiRegressionScenario(scenario.get("name").getAsString(), scenario.get("inventorySlots").getAsInt(),
							scenario.has("upgradeSlots") ? scenario.get("upgradeSlots").getAsInt() : 3,
							scenario.has("columnsTaken") ? scenario.get("columnsTaken").getAsInt() : 0,
							scenario.has("openTab") ? scenario.get("openTab").getAsInt() : -1, getSlotStackSpecs(scenario, "contents"),
							getSlotStackSpecs(scenario, "playerContents"), getSlotStackSpecs(scenario, "upgradeContents"), getIntArray(scenario, "noSortSlots"),
							getIntArray(scenario, "memorySlots"), getCapturedMobs(scenario), getStorageGuiActions(scenario)));
				}
				return new StorageGuiRegressionSuite(scenarios);
			} catch (IOException e) {
				throw new IllegalStateException("Failed to read storage GUI regression definitions", e);
			}
		}

		private SlotStackSpec[] getSlotStackSpecs(JsonObject scenario, String key) {
			if (!scenario.has(key)) {
				return new SlotStackSpec[0];
			}
			JsonArray elements = scenario.getAsJsonArray(key);
			SlotStackSpec[] specs = new SlotStackSpec[elements.size()];
			for (int i = 0; i < elements.size(); i++) {
				JsonObject element = elements.get(i).getAsJsonObject();
				specs[i] = new SlotStackSpec(element.get("slot").getAsInt(), getItem(element.get("item").getAsString()),
						element.has("count") ? element.get("count").getAsInt() : 1);
			}
			return specs;
		}

		private StorageGuiAction[] getStorageGuiActions(JsonObject scenario) {
			JsonArray elements = scenario.getAsJsonArray("actions");
			StorageGuiAction[] actions = new StorageGuiAction[elements.size()];
			for (int i = 0; i < elements.size(); i++) {
				JsonObject element = elements.get(i).getAsJsonObject();
				actions[i] = new StorageGuiAction(element.get("type").getAsString(), element.has("snapshot") ? element.get("snapshot").getAsString() : "",
						getIntArray(element, "slots"), getSlotRefs(element, "slots"), getSlotRef(element, "slot"), getButton(element),
						element.has("operation") ? element.get("operation").getAsString() : "",
						element.has("upgradeSlot") ? element.get("upgradeSlot").getAsInt() : -1,
						element.has("item") ? Optional.of(getItem(element.get("item").getAsString())) : Optional.empty(),
						element.has("count") ? element.get("count").getAsInt() : 1, element.has("hotbarSlot") ? element.get("hotbarSlot").getAsInt() : 0,
						element.has("expectedColumnsTaken") ? element.get("expectedColumnsTaken").getAsInt() : -1,
						element.has("expectedStorageSlots") ? element.get("expectedStorageSlots").getAsInt() : -1);
			}
			return actions;
		}

		private int getButton(JsonObject element) {
			if (!element.has("button")) {
				return 0;
			}
			String button = element.get("button").getAsString();
			return switch (button) {
				case "left" -> 0;
				case "right" -> 1;
				default -> throw new IllegalArgumentException("Unknown mouse button " + button);
			};
		}

		private SlotRef getSlotRef(JsonObject json, String key) {
			if (!json.has(key) || !json.get(key).isJsonObject()) {
				return SlotRef.storage(0);
			}
			return getSlotRef(json.getAsJsonObject(key));
		}

		private SlotRef[] getSlotRefs(JsonObject json, String key) {
			if (!json.has(key) || !json.get(key).isJsonArray()) {
				return new SlotRef[0];
			}
			JsonArray elements = json.getAsJsonArray(key);
			if (elements.isEmpty() || elements.get(0).isJsonPrimitive()) {
				return new SlotRef[0];
			}
			SlotRef[] slotRefs = new SlotRef[elements.size()];
			for (int i = 0; i < elements.size(); i++) {
				slotRefs[i] = getSlotRef(elements.get(i).getAsJsonObject());
			}
			return slotRefs;
		}

		private SlotRef getSlotRef(JsonObject json) {
			return new SlotRef(json.has("area") ? json.get("area").getAsString() : "storage", json.get("index").getAsInt(),
					json.has("upgradeSlot") ? json.get("upgradeSlot").getAsInt() : -1);
		}

		private Item getItem(String itemName) {
			ResourceLocation itemId = ResourceLocation.parse(itemName);
			return BuiltInRegistries.ITEM.getOptional(itemId).orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemId));
		}

		private boolean contains(int[] values, int expected) {
			for (int value : values) {
				if (value == expected) {
					return true;
				}
			}
			return false;
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

		private String runStorageControllerDoubleChestRegressions(ServerPlayer player, boolean inspectOnly) {
			List<ControllerDoubleChestRegressionResult> results = List.of(
					runControllerDoubleChestRegression(player, "double_chest_then_controller", player.blockPosition().offset(0, 0, 6), false, true,
							inspectOnly),
					runControllerDoubleChestRegression(player, "controller_then_left_chest_then_right_chest", player.blockPosition().offset(6, 0, 6), true,
							false, inspectOnly),
					runControllerDoubleChestRegression(player, "controller_then_right_chest_then_left_chest", player.blockPosition().offset(12, 0, 6), true,
							true, inspectOnly));

			long failed = results.stream().filter(result -> !result.passed()).count();
			StringBuilder json = new StringBuilder("{\"ok\":").append(failed == 0).append(",\"inspectOnly\":").append(inspectOnly).append(",\"total\":")
					.append(results.size()).append(",\"failed\":").append(failed).append(",\"results\":[");
			for (int i = 0; i < results.size(); i++) {
				if (i > 0) {
					json.append(',');
				}
				ControllerDoubleChestRegressionResult result = results.get(i);
				json.append('{').append(jsonProperty("name", result.name())).append(",\"passed\":").append(result.passed()).append(",\"registeredStorages\":")
						.append(result.registeredStorages()).append(",\"slots\":").append(result.slots()).append(',')
						.append(jsonProperty("positions", result.positions())).append(',').append(jsonProperty("chestState", result.chestState())).append(',')
						.append(jsonProperty("error", result.error())).append('}');
			}
			json.append("]}");
			return json.toString();
		}

		private String verifySimpleMaterialDecoration(ServerPlayer player) {
			ServerLevel level = player.serverLevel();
			BlockPos tablePos = player.blockPosition().offset(0, 0, 4);
			level.setBlockAndUpdate(tablePos, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(tablePos, ModBlocks.DECORATION_TABLE.get().defaultBlockState());

			DecorationTableBlockEntity table = level.getBlockEntity(tablePos, ModBlocks.DECORATION_TABLE_BLOCK_ENTITY_TYPE.get()).orElse(null);
			if (table == null) {
				return "{\"ok\":false," + jsonProperty("error", "Decoration table block entity missing") + "}";
			}

			ResourceLocation expectedMaterial = BuiltInRegistries.BLOCK.getKey(Blocks.OAK_PLANKS);
			List<Item> itemsToVerify = new ArrayList<>(List.of(ModBlocks.CONTROLLER_ITEM.get(), ModBlocks.STORAGE_IO_ITEM.get(),
					ModBlocks.STORAGE_INPUT_ITEM.get(), ModBlocks.STORAGE_OUTPUT_ITEM.get(), ModBlocks.STORAGE_LINK_ITEM.get()));
			ModBlocks.STORAGE_CONNECTOR_ITEMS.values().forEach(item -> itemsToVerify.add(item.get()));

			StringBuilder results = new StringBuilder();
			int failed = 0;
			for (int i = 0; i < itemsToVerify.size(); i++) {
				Item item = itemsToVerify.get(i);
				if (i > 0) {
					results.append(',');
				}

				table.getStorageBlock().setStackInSlot(0, ItemStack.EMPTY);
				for (int slot = 0; slot < 7; slot++) {
					table.getDecorativeBlocks().setStackInSlot(slot, ItemStack.EMPTY);
				}

				ItemStack input = new ItemStack(item);
				ItemStack materialStack = new ItemStack(Items.OAK_PLANKS, 2);
				table.getStorageBlock().setStackInSlot(0, input);

				boolean slot0AcceptsMaterial = table.getDecorativeBlocks().isItemValid(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT, materialStack);
				boolean slot1AcceptsMaterial = table.getDecorativeBlocks().isItemValid(DecorationTableBlockEntity.TOP_TRIM_SLOT, materialStack);
				table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT, materialStack.copy());

				ItemStack result = table.getResult().copy();
				Optional<ResourceLocation> resultMaterial = SimpleMaterialBlockItem.getMaterial(result);
				Map<ResourceLocation, Integer> partsNeeded = table.getPartsNeeded();
				int oakPartsNeeded = partsNeeded.getOrDefault(expectedMaterial, 0);
				ItemStack extracted = table.extractResult(1);
				table.consumeIngredientsOnCraft();
				int remainingMaterialCount = table.getDecorativeBlocks().getStackInSlot(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT).getCount();

				boolean layoutSingle = table.getMaterialLayout() == DecorationTableBlockEntity.MaterialLayout.SINGLE;
				boolean resultMaterialMatches = resultMaterial.filter(expectedMaterial::equals).isPresent();
				Optional<ResourceLocation> extractedMaterial = SimpleMaterialBlockItem.getMaterial(extracted);
				boolean extractedMaterialMatches = extractedMaterial.filter(expectedMaterial::equals).isPresent();
				boolean passed = layoutSingle && table.isMaterialSlotActive(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT)
						&& !table.isMaterialSlotActive(DecorationTableBlockEntity.TOP_TRIM_SLOT) && !table.areTintsActive() && slot0AcceptsMaterial
						&& !slot1AcceptsMaterial && !result.isEmpty() && resultMaterialMatches && !extracted.isEmpty() && extractedMaterialMatches
						&& oakPartsNeeded == 24 && remainingMaterialCount == 1;
				if (!passed) {
					failed++;
				}

				results.append('{').append(jsonProperty("item", BuiltInRegistries.ITEM.getKey(item).toString())).append(',').append("\"passed\":")
						.append(passed).append(',').append(jsonProperty("layout", table.getMaterialLayout().name())).append(',').append("\"slot0Active\":")
						.append(table.isMaterialSlotActive(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT)).append(',').append("\"slot1Active\":")
						.append(table.isMaterialSlotActive(DecorationTableBlockEntity.TOP_TRIM_SLOT)).append(',').append("\"slot0AcceptsMaterial\":")
						.append(slot0AcceptsMaterial).append(',').append("\"slot1AcceptsMaterial\":").append(slot1AcceptsMaterial).append(',')
						.append("\"tintsActive\":").append(table.areTintsActive()).append(',')
						.append(jsonProperty("resultItem", result.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(result.getItem()).toString())).append(',')
						.append(jsonProperty("resultMaterial", resultMaterial.map(ResourceLocation::toString).orElse(null))).append(',')
						.append(jsonProperty("extractedMaterial", extractedMaterial.map(ResourceLocation::toString).orElse(null))).append(',')
						.append("\"oakPartsNeeded\":").append(oakPartsNeeded).append(',').append("\"remainingMaterialCount\":").append(remainingMaterialCount)
						.append('}');
			}

			return "{\"ok\":" + (failed == 0) + ',' + jsonProperty("tablePos", tablePos.toShortString()) + ',' + "\"total\":" + itemsToVerify.size() + ','
					+ "\"failed\":" + failed + ',' + "\"results\":[" + results + "]}";
		}

		private String setupSimpleMaterialRenderVerification(ServerPlayer player) {
			ServerLevel level = player.serverLevel();
			BlockPos tablePos = player.blockPosition().offset(0, 0, 4);
			BlockPos controllerPos = player.blockPosition().offset(3, 1, 4);
			level.setBlockAndUpdate(tablePos, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(controllerPos, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(controllerPos.above(), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(tablePos, ModBlocks.DECORATION_TABLE.get().defaultBlockState());

			DecorationTableBlockEntity table = level.getBlockEntity(tablePos, ModBlocks.DECORATION_TABLE_BLOCK_ENTITY_TYPE.get()).orElse(null);
			if (table == null) {
				return "{\"ok\":false," + jsonProperty("error", "Decoration table block entity missing") + "}";
			}

			table.getStorageBlock().setStackInSlot(0, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
			table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT, new ItemStack(Items.DIAMOND_BLOCK, 2));
			ItemStack decoratedController = table.extractResult(1);
			table.consumeIngredientsOnCraft();
			Optional<ResourceLocation> decoratedMaterial = SimpleMaterialBlockItem.getMaterial(decoratedController);

			player.getInventory().clearContent();
			player.getInventory().setItem(0, decoratedController.copy());
			player.getInventory().setItem(1, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));

			level.setBlockAndUpdate(controllerPos, ModBlocks.CONTROLLER.get().defaultBlockState());
			WorldHelper.getBlockEntity(level, controllerPos, ISimpleMaterialHolder.class).ifPresent(holder -> {
				SimpleMaterialBlockItem.getMaterial(decoratedController).ifPresentOrElse(holder::setMaterial, () -> holder.setMaterial(null));
				holder.setOverlayHidden(false);
			});

			return "{\"ok\":" + (!decoratedController.isEmpty() && decoratedMaterial.isPresent()) + ',' + jsonProperty("tablePos", tablePos.toShortString())
					+ ',' + jsonProperty("controllerPos", controllerPos.toShortString()) + ','
					+ jsonProperty("decoratedItem",
							decoratedController.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(decoratedController.getItem()).toString())
					+ ',' + jsonProperty("decoratedMaterial", decoratedMaterial.map(ResourceLocation::toString).orElse(null)) + '}';
		}

		private String setupSimpleMaterialOverlayComparison(ServerPlayer player) {
			ServerLevel level = player.serverLevel();
			BlockPos origin = player.blockPosition().offset(0, 0, 6);
			ResourceLocation material = BuiltInRegistries.BLOCK.getKey(Blocks.DIAMOND_BLOCK);

			BlockPos controllerShown = origin.offset(-3, 0, 0);
			BlockPos controllerHidden = origin.offset(-1, 0, 0);
			BlockPos linkShown = origin.offset(1, 0, 0);
			BlockPos linkHidden = origin.offset(3, 0, 0);
			List<BlockPos> positions = List.of(controllerShown, controllerHidden, linkShown, linkHidden);
			for (BlockPos pos : positions) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(pos.below(), Blocks.GRAY_CONCRETE.defaultBlockState());
			}

			level.setBlockAndUpdate(controllerShown, ModBlocks.CONTROLLER.get().defaultBlockState());
			level.setBlockAndUpdate(controllerHidden, ModBlocks.CONTROLLER.get().defaultBlockState());
			level.setBlockAndUpdate(linkShown, ModBlocks.STORAGE_LINK.get().defaultBlockState());
			level.setBlockAndUpdate(linkHidden, ModBlocks.STORAGE_LINK.get().defaultBlockState());

			boolean controllerShownSet = setSimpleMaterialState(level, controllerShown, material, false);
			boolean controllerHiddenSet = setSimpleMaterialState(level, controllerHidden, material, true);
			boolean linkShownSet = setSimpleMaterialState(level, linkShown, material, false);
			boolean linkHiddenSet = setSimpleMaterialState(level, linkHidden, material, true);

			player.getInventory().clearContent();
			ItemStack storageTool = new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModItems.STORAGE_TOOL.get());
			storageTool.set(ModDataComponents.TOOL_MODE, StorageToolItem.Mode.TIER_DISPLAY);
			player.getInventory().setItem(0, storageTool);
			player.getInventory().setChanged();

			return "{\"ok\":" + (controllerShownSet && controllerHiddenSet && linkShownSet && linkHiddenSet) + ','
					+ jsonProperty("material", material.toString()) + ',' + jsonProperty("controllerShown", controllerShown.toShortString()) + ','
					+ jsonProperty("controllerHidden", controllerHidden.toShortString()) + ',' + jsonProperty("linkShown", linkShown.toShortString()) + ','
					+ jsonProperty("linkHidden", linkHidden.toShortString()) + ',' + "\"controllerShownSet\":" + controllerShownSet + ','
					+ "\"controllerHiddenSet\":" + controllerHiddenSet + ',' + "\"linkShownSet\":" + linkShownSet + ',' + "\"linkHiddenSet\":" + linkHiddenSet
					+ '}';
		}

		private boolean setSimpleMaterialState(ServerLevel level, BlockPos pos, ResourceLocation material, boolean overlayHidden) {
			return WorldHelper.getBlockEntity(level, pos, ISimpleMaterialHolder.class).map(holder -> {
				holder.setMaterial(material);
				holder.setOverlayHidden(overlayHidden);
				return true;
			}).orElse(false);
		}

		private String runStorageControllerFilterRegressions(ServerPlayer player, boolean runInserts, boolean profileCapacity) {
			ServerLevel level = player.serverLevel();
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
					new ControllerFilterStorageSpec("mod_sophisticatedstorage", controllerPos.offset(3, 0, -1), ModBlocks.BARREL_ITEM.get(), true,
							PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("mod_sophisticatedbackpacks", controllerPos.offset(3, 0, 0), ModItems.GOLD_BACKPACK.get(), true,
							PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("deny_feather", controllerPos.offset(3, 0, 1), Items.FEATHER, false, PrimaryMatch.ITEM));

			List<ControllerLockedStorageSpec> lockedSpecs = createControllerFilterLockedStorageSpecs(controllerPos);
			List<BlockPos> allPositions = new ArrayList<>();
			allPositions.addAll(overflowPositions);
			filterSpecs.forEach(spec -> allPositions.add(spec.pos()));
			lockedSpecs.forEach(spec -> allPositions.add(spec.pos()));
			Map<BlockPos, Item> barrelItems = createControllerFilterBarrelItems(overflowPositions, filterSpecs, lockedSpecs);

			allPositions.forEach(pos -> placeBarrel(level, player, pos, barrelItems.getOrDefault(pos, ModBlocks.DIAMOND_BARREL_ITEM.get())));
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

			ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
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
			List<Item> overflowBarrels = List.of(ModBlocks.BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(),
					ModBlocks.DIAMOND_BARREL_ITEM.get());
			for (int i = 0; i < overflowPositions.size(); i++) {
				barrelItems.put(overflowPositions.get(i), overflowBarrels.get(i % overflowBarrels.size()));
			}
			List<Item> filterBarrels = List.of(ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(),
					ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get());
			for (int i = 0; i < filterSpecs.size(); i++) {
				barrelItems.put(filterSpecs.get(i).pos(), filterBarrels.get(i % filterBarrels.size()));
			}
			lockedSpecs.forEach(spec -> barrelItems.put(spec.pos(), spec.barrelItem()));
			return barrelItems;
		}

		private String profileStorageControllerFilterRegressions(ServerPlayer player, int runs) {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 12);
			List<String> failures = new ArrayList<>();
			List<BlockPos> overflowPositions = List.of(controllerPos.offset(1, 0, -1), controllerPos.offset(1, 0, 0), controllerPos.offset(1, 0, 1),
					controllerPos.offset(1, 0, 2));
			List<ControllerFilterStorageSpec> filterSpecs = List.of(
					new ControllerFilterStorageSpec("specific_amethyst", controllerPos.offset(2, 0, -1), Items.AMETHYST_SHARD, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("specific_honeycomb", controllerPos.offset(2, 0, 0), Items.HONEYCOMB, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("specific_echo_shard", controllerPos.offset(2, 0, 1), Items.ECHO_SHARD, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("mod_sophisticatedstorage", controllerPos.offset(3, 0, -1), ModBlocks.BARREL_ITEM.get(), true,
							PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("mod_sophisticatedbackpacks", controllerPos.offset(3, 0, 0), ModItems.GOLD_BACKPACK.get(), true,
							PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("deny_feather", controllerPos.offset(3, 0, 1), Items.FEATHER, false, PrimaryMatch.ITEM));
			List<ControllerLockedStorageSpec> lockedSpecs = createControllerFilterLockedStorageSpecs(controllerPos);
			List<BlockPos> allPositions = new ArrayList<>();
			allPositions.addAll(overflowPositions);
			filterSpecs.forEach(spec -> allPositions.add(spec.pos()));
			lockedSpecs.forEach(spec -> allPositions.add(spec.pos()));

			ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
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
					ControllerFilterInsertStats stats = runControllerFilterProfileExpectation(controller, expectation, run, failures);
					insertCalls += stats.calls();
					itemsInserted += stats.items();
				}
			}
			long insertMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
			return buildStorageControllerFilterRegressionJson(failures.isEmpty(), 0, insertMillis, 0, connectedStorages, lockedSpecs.size(), filterSpecs.size(),
					overflowPositions.size(), insertCalls, itemsInserted, failures);
		}

		private String profileStorageControllerFilterManualDeposit(ServerPlayer player, int runs) {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, 12);
			List<String> failures = new ArrayList<>();
			List<BlockPos> overflowPositions = List.of(controllerPos.offset(1, 0, -1), controllerPos.offset(1, 0, 0), controllerPos.offset(1, 0, 1),
					controllerPos.offset(1, 0, 2));
			List<ControllerFilterStorageSpec> filterSpecs = List.of(
					new ControllerFilterStorageSpec("specific_amethyst", controllerPos.offset(2, 0, -1), Items.AMETHYST_SHARD, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("specific_honeycomb", controllerPos.offset(2, 0, 0), Items.HONEYCOMB, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("specific_echo_shard", controllerPos.offset(2, 0, 1), Items.ECHO_SHARD, true, PrimaryMatch.ITEM),
					new ControllerFilterStorageSpec("mod_sophisticatedstorage", controllerPos.offset(3, 0, -1), ModBlocks.BARREL_ITEM.get(), true,
							PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("mod_sophisticatedbackpacks", controllerPos.offset(3, 0, 0), ModItems.GOLD_BACKPACK.get(), true,
							PrimaryMatch.MOD),
					new ControllerFilterStorageSpec("deny_feather", controllerPos.offset(3, 0, 1), Items.FEATHER, false, PrimaryMatch.ITEM));
			List<ControllerLockedStorageSpec> lockedSpecs = createControllerFilterLockedStorageSpecs(controllerPos);

			ControllerBlockEntity controller = level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).orElse(null);
			if (controller == null) {
				failures.add("controller block entity missing at " + controllerPos + "; run setup mode before manual deposit profile mode");
				return buildStorageControllerFilterRegressionJson("storage_controller_filter_manual_deposit", false, 0, 0, 0, 0, lockedSpecs.size(),
						filterSpecs.size(), overflowPositions.size(), 0, 0, failures);
			}
			int connectedStorages = controller.getStoragePositions().size();
			if (connectedStorages != overflowPositions.size() + filterSpecs.size() + lockedSpecs.size()) {
				failures.add("expected " + (overflowPositions.size() + filterSpecs.size() + lockedSpecs.size()) + " connected storages, got "
						+ connectedStorages + ": " + controller.getStoragePositions());
			}

			List<Item> depositItems = createControllerFilterManualDepositItems(lockedSpecs);
			long depositNanos = 0;
			long itemsDeposited = 0;
			int depositCalls = 0;
			player.getInventory().selected = 0;
			clearControllerFilterManualDepositInventory(player);
			for (int run = 0; run < runs; run++) {
				fillControllerFilterManualDepositInventory(player, depositItems);
				long beforeDeposited = countControllerFilterManualDepositRemaining(player, depositItems.size());
				long startedAt = System.nanoTime();
				controller.depositPlayerItems(player, InteractionHand.MAIN_HAND);
				controller.depositPlayerItems(player, InteractionHand.MAIN_HAND);
				depositNanos += System.nanoTime() - startedAt;
				depositCalls += 2;
				long remaining = countControllerFilterManualDepositRemaining(player, depositItems.size());
				itemsDeposited += beforeDeposited - remaining;
				if (remaining != 0) {
					failures.add("manual deposit run " + run + " left " + remaining + " items in player inventory");
				}
			}
			clearControllerFilterManualDepositInventory(player);
			long depositMillis = TimeUnit.NANOSECONDS.toMillis(depositNanos);
			return buildStorageControllerFilterRegressionJson("storage_controller_filter_manual_deposit", failures.isEmpty(), 0, depositMillis, 0,
					connectedStorages, lockedSpecs.size(), filterSpecs.size(), overflowPositions.size(), depositCalls, itemsDeposited, failures);
		}

		private List<Item> createControllerFilterManualDepositItems(List<ControllerLockedStorageSpec> lockedSpecs) {
			List<Item> depositItems = new ArrayList<>();
			lockedSpecs.stream().limit(24).map(ControllerLockedStorageSpec::item).forEach(depositItems::add);
			depositItems.add(Items.AMETHYST_SHARD);
			depositItems.add(Items.HONEYCOMB);
			depositItems.add(Items.ECHO_SHARD);
			depositItems.add(ModBlocks.GOLD_BARREL_ITEM.get());
			depositItems.add(ModItems.STACK_UPGRADE_TIER_1.get());
			depositItems.add(Items.NAUTILUS_SHELL);
			return depositItems;
		}

		private void fillControllerFilterManualDepositInventory(ServerPlayer player, List<Item> depositItems) {
			for (int slot = 0; slot < depositItems.size(); slot++) {
				player.getInventory().setItem(slot, new ItemStack(depositItems.get(slot)));
			}
		}

		private void clearControllerFilterManualDepositInventory(ServerPlayer player) {
			for (int slot = 0; slot < 36; slot++) {
				player.getInventory().setItem(slot, ItemStack.EMPTY);
			}
		}

		private long countControllerFilterManualDepositRemaining(ServerPlayer player, int slots) {
			long remaining = 0;
			for (int slot = 0; slot < slots; slot++) {
				remaining += player.getInventory().getItem(slot).getCount();
			}
			return remaining;
		}

		private List<ControllerFilterInsertExpectation> createControllerFilterInsertExpectations(BlockPos controllerPos, List<BlockPos> overflowPositions,
				Map<Item, Set<BlockPos>> lockedPositionsByItem) {
			List<ControllerFilterInsertExpectation> expectations = new ArrayList<>();
			lockedPositionsByItem
					.forEach((item, positions) -> expectations.add(new ControllerFilterInsertExpectation("locked_" + itemId(item), item, 3, 20, positions)));
			expectations.add(new ControllerFilterInsertExpectation("specific_amethyst", Items.AMETHYST_SHARD, 4, 80, Set.of(controllerPos.offset(2, 0, -1))));
			expectations.add(new ControllerFilterInsertExpectation("specific_honeycomb", Items.HONEYCOMB, 4, 80, Set.of(controllerPos.offset(2, 0, 0))));
			expectations.add(new ControllerFilterInsertExpectation("specific_echo_shard", Items.ECHO_SHARD, 4, 80, Set.of(controllerPos.offset(2, 0, 1))));
			expectations.add(new ControllerFilterInsertExpectation("mod_sophisticatedstorage", ModBlocks.GOLD_BARREL_ITEM.get(), 2, 80,
					Set.of(controllerPos.offset(3, 0, -1))));
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
				return ModBlocks.NETHERITE_BARREL_ITEM.get();
			}
			if (index < 35) {
				return ModBlocks.DIAMOND_BARREL_ITEM.get();
			}
			if (index < 43) {
				return ModBlocks.GOLD_BARREL_ITEM.get();
			}
			if (index < 48) {
				return ModBlocks.IRON_BARREL_ITEM.get();
			}
			return ModBlocks.BARREL_ITEM.get();
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

		private void placeBarrel(ServerLevel level, ServerPlayer player, BlockPos pos, Item barrelItem) {
			placeBlockWithItem(level, player, pos, new ItemStack(barrelItem));
		}

		private StorageBlockEntity getBarrelStorage(ServerLevel level, BlockPos pos) {
			return level.getBlockEntity(pos, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get()).map(storage -> (StorageBlockEntity) storage)
					.orElseThrow(() -> new IllegalStateException("Missing barrel storage at " + pos));
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
			for (int slot = firstUpgradeSlot; slot < Math.min(upgrades.getSlots(), firstUpgradeSlot + 2); slot++) {
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
				ItemStack remainder = controller.insertItem(new ItemStack(expectation.item(), expectation.count()), false);
				if (!remainder.isEmpty()) {
					failures.add(expectation.name() + " insert " + i + " returned remainder " + remainder.getCount() + "x" + itemId(remainder.getItem()));
				}
				inserted += expectation.count() - remainder.getCount();
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
				ControllerFilterInsertExpectation expectation, int run, List<String> failures) {
			long inserted = 0;
			for (int i = 0; i < expectation.calls(); i++) {
				ItemStack remainder = controller.insertItem(new ItemStack(expectation.item(), expectation.count()), false);
				inserted += expectation.count() - remainder.getCount();
			}
			return new ControllerFilterInsertStats(expectation.calls(), inserted);
		}

		private long countItemInPositions(ServerLevel level, Set<BlockPos> positions, Item item) {
			long count = 0;
			for (BlockPos pos : positions) {
				InventoryHandler inventory = getBarrelStorage(level, pos).getStorageWrapper().getInventoryHandler();
				for (int slot = 0; slot < inventory.getSlots(); slot++) {
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
			return buildStorageControllerFilterRegressionJson("storage_controller_filter_routing", ok, setupMillis, insertMillis, verifyMillis,
					connectedStorages, lockedStorages, filteredStorages, overflowStorages, insertCalls, itemsInserted, failures);
		}

		private String buildStorageControllerFilterRegressionJson(String scenario, boolean ok, long setupMillis, long insertMillis, long verifyMillis,
				int connectedStorages, int lockedStorages, int filteredStorages, int overflowStorages, int insertCalls, long itemsInserted,
				List<String> failures) {
			StringBuilder json = new StringBuilder("{\"ok\":").append(ok).append(",\"scenario\":\"").append(scenario).append("\"")
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

		private ControllerDoubleChestRegressionResult runControllerDoubleChestRegression(ServerPlayer player, String name, BlockPos controllerPos,
				boolean controllerFirst, boolean farChestFirst, boolean inspectOnly) {
			ServerLevel level = player.serverLevel();
			BlockPos leftChestPos = controllerPos.east();
			BlockPos rightChestPos = leftChestPos.east();
			if (!inspectOnly) {
				clearControllerDoubleChestRegressionArea(level, controllerPos);

				if (controllerFirst) {
					placeController(level, player, controllerPos);
				}
				if (farChestFirst) {
					placeChest(level, player, rightChestPos);
					placeChest(level, player, leftChestPos);
				} else {
					placeChest(level, player, leftChestPos);
					placeChest(level, player, rightChestPos);
				}
				if (!controllerFirst) {
					placeController(level, player, controllerPos);
				}
			}

			return level.getBlockEntity(controllerPos, ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get()).map(controller -> {
				List<BlockPos> storagePositions = controller.getStoragePositions();
				int registeredStorages = storagePositions.size();
				String positions = storagePositions.toString();
				String chestState = getChestState(level, leftChestPos) + "; " + getChestState(level, rightChestPos);
				int slots = registeredStorages == 1 ? controller.getSlots(0) : 0;
				boolean mainStorageRegistered = registeredStorages == 1 && storagePositions.contains(rightChestPos);
				boolean passed = mainStorageRegistered && slots == 54 && isDoubleChest(level, leftChestPos, rightChestPos);
				String error = null;
				if (!passed) {
					error = "expected one connected double chest registered at " + rightChestPos + " with 54 slots; slots=" + slots + "; chestState="
							+ chestState;
				}
				return new ControllerDoubleChestRegressionResult(name, passed, registeredStorages, slots, positions, chestState, error);
			}).orElseGet(() -> new ControllerDoubleChestRegressionResult(name, false, 0, 0, "[]",
					getChestState(level, leftChestPos) + "; " + getChestState(level, rightChestPos), "controller block entity missing"));
		}

		private void clearControllerDoubleChestRegressionArea(ServerLevel level, BlockPos controllerPos) {
			for (int x = -1; x <= 3; x++) {
				for (int y = -1; y <= 2; y++) {
					for (int z = -1; z <= 1; z++) {
						level.setBlock(controllerPos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
		}

		private void placeController(ServerLevel level, ServerPlayer player, BlockPos pos) {
			placeBlockWithItem(level, player, pos, new ItemStack(ModBlocks.CONTROLLER_ITEM.get()));
		}

		private void placeChest(ServerLevel level, ServerPlayer player, BlockPos pos) {
			placeBlockWithItem(level, player, pos, new ItemStack(ModBlocks.CHEST_ITEM.get()));
		}

		private void placeBlockWithItem(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
			BlockPos supportPos = pos.below();
			level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
			player.setYRot(0);
			player.setXRot(0);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false);
			player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitResult);
		}

		private boolean isDoubleChest(ServerLevel level, BlockPos leftChestPos, BlockPos rightChestPos) {
			return level.getBlockState(leftChestPos).is(ModBlocks.CHEST.get()) && level.getBlockState(rightChestPos).is(ModBlocks.CHEST.get())
					&& level.getBlockState(leftChestPos).getValue(ChestBlock.TYPE) == ChestType.LEFT
					&& level.getBlockState(rightChestPos).getValue(ChestBlock.TYPE) == ChestType.RIGHT
					&& level.getBlockEntity(leftChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(be -> be.getMainPos().equals(rightChestPos))
							.orElse(false)
					&& level.getBlockEntity(rightChestPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(be -> be.getMainPos().equals(rightChestPos))
							.orElse(false);
		}

		private String getChestState(ServerLevel level, BlockPos pos) {
			BlockState state = level.getBlockState(pos);
			if (!state.is(ModBlocks.CHEST.get())) {
				return pos + "=not_chest(" + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + ")";
			}
			return level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
					.map(be -> pos + "=type:" + state.getValue(ChestBlock.TYPE) + ",facing:" + state.getValue(ChestBlock.FACING) + ",main:" + be.getMainPos()
							+ ",hasData:" + be.hasStorageData() + ",controller:" + be.getControllerPos().map(Object::toString).orElse("none") + ",slots:"
							+ be.getStorageWrapper().getInventoryHandler().getSlots())
					.orElse(pos + "=chest_without_be");
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
			if (!elements.isEmpty() && elements.get(0).isJsonObject()) {
				return new int[0];
			}
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
			JsonArray capturedMobs = scenario.getAsJsonArray("capturedMobs");
			CapturedMobSpec[] mobs = new CapturedMobSpec[capturedMobs.size()];
			for (int i = 0; i < capturedMobs.size(); i++) {
				JsonObject capturedMob = capturedMobs.get(i).getAsJsonObject();
				mobs[i] = new CapturedMobSpec(capturedMob.get("slot").getAsInt(), capturedMob.get("width").getAsInt(), capturedMob.get("height").getAsInt(),
						capturedMob.has("entityType") ? capturedMob.get("entityType").getAsString() : "minecraft:pig");
			}
			return mobs;
		}

		private ColumnUpgradeRegressionResult runColumnUpgradeRegressionScenario(ColumnUpgradeRegressionScenario scenario,
				ColumnUpgradeStackGenerator stackGenerator) {
			ItemStack backpack = createBackpackStack(scenario.inventorySlots());
			IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
			wrapper.setSlotNumbers(scenario.inventorySlots(), 5);
			if (scenario.operation().equals("remove")) {
				wrapper.setColumnsTaken(getUpgradeColumnsTaken(scenario.upgradeItem()), false);
			}

			InventoryHandler inventory = wrapper.getInventoryHandler();
			fillRegressionStacks(inventory, scenario.occupiedSlots(), stackGenerator);
			applyProtectedSlots(wrapper, scenario.noSortSlots(), scenario.memorySlots());
			addCapturedMobs(wrapper, scenario.capturedMobs());
			inventory.saveInventory();

			Map<String, Integer> beforeStacks = snapshotStacks(inventory);
			Map<Integer, String> beforeProtectedStacks = snapshotProtectedStacks(inventory, scenario.protectedSlots());
			Map<Integer, String> beforeStableStacks = snapshotProtectedStacks(inventory, scenario.stableSlots());
			Map<String, String> beforeProtectedSettings = snapshotProtectedSettings(wrapper, scenario);
			Map<UUID, String> beforeCapturedMobs = snapshotCapturedMobs(wrapper);
			ColumnUpgradeSimulationResult simulationResult = simulateColumnUpgradeOperation(backpack, wrapper, scenario.upgradeItem(), scenario.operation());
			wrapper = BackpackWrapper.fromStack(backpack);
			if (simulationResult.fits() != scenario.expectedFits()) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, scenario.expectedFits(), simulationResult.fits(), beforeStacks.size(),
						snapshotStacks(wrapper.getInventoryHandler()).size(), "fit result mismatch");
			}

			Map<String, Integer> afterStacks = snapshotStacks(wrapper.getInventoryHandler());
			Map<Integer, String> afterProtectedStacks = snapshotProtectedStacks(wrapper.getInventoryHandler(), scenario.protectedSlots());
			Map<Integer, String> afterStableStacks = snapshotProtectedStacks(wrapper.getInventoryHandler(), scenario.stableSlots());
			Map<String, String> afterProtectedSettings = snapshotProtectedSettings(wrapper, scenario);
			Map<UUID, String> afterCapturedMobs = snapshotCapturedMobs(wrapper);
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
			if (scenario.expectedFits()) {
				Optional<String> capturedMobLayoutError = capturedMobLayoutError(wrapper);
				if (capturedMobLayoutError.isPresent()) {
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(),
							capturedMobLayoutError.get());
				}
				if (scenario.expectedCapturedMobSlots().length > 0 && !capturedMobSlotsMatch(wrapper, scenario.expectedCapturedMobSlots())) {
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(),
							"captured mob slots mismatch expected=" + Arrays.toString(scenario.expectedCapturedMobSlots()) + " actual=" + afterCapturedMobs);
				}
				if (!beforeStacks.equals(afterStacks)) {
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(),
							"stack snapshot changed");
				}
				if (scenario.expectedCapturedMobSlots().length == 0 && !beforeCapturedMobs.equals(afterCapturedMobs)) {
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(),
							"captured mobs changed");
				}
			} else if (!beforeStacks.equals(afterStacks) || !beforeCapturedMobs.equals(afterCapturedMobs)) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, false, false, beforeStacks.size(), afterStacks.size(),
						"blocked insertion mutated stacks");
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

		private ColumnUpgradeSimulationResult simulateColumnUpgradeOperation(ItemStack backpack, IBackpackWrapper wrapper, Item upgradeItem, String operation) {
			int currentColumnsTaken = wrapper.getColumnsTaken();
			int columnsTaken = getUpgradeColumnsTaken(upgradeItem);
			int targetColumnsTaken = switch (operation) {
				case "insert" -> currentColumnsTaken + columnsTaken;
				case "remove" -> currentColumnsTaken - columnsTaken;
				default -> throw new IllegalArgumentException("Unknown column upgrade regression operation " + operation);
			};
			int rows = wrapper.getNumberOfSlotRows();
			int baseSlots = wrapper.getInventoryHandler().getSlots() + currentColumnsTaken * rows;
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
			wrapper.onContentsNbtUpdated();
			wrapper.applyInventoryLayout(fitResult, targetColumns);
			wrapper.onContentsNbtUpdated();
			wrapper.getUpgradeHandler().setStackInSlot(0, operation.equals("insert") ? new ItemStack(upgradeItem) : ItemStack.EMPTY);
			wrapper.getUpgradeHandler().saveInventory();
			wrapper.getInventoryHandler().saveInventory();
			BackpackWrapper.fromStack(backpack).getInventoryHandler().saveInventory();

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

		private void addCapturedMobs(IBackpackWrapper wrapper, CapturedMobSpec[] capturedMobs) {
			if (capturedMobs.length == 0) {
				return;
			}
			wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
			wrapper.getUpgradeHandler().saveInventory();
			for (int i = 0; i < capturedMobs.length; i++) {
				CapturedMobSpec capturedMob = capturedMobs[i];
				MobCatcherStorage.addCapturedMob(wrapper,
						new CapturedMob(new UUID(0, i + 1), ResourceLocation.parse(capturedMob.entityType()), new CompoundTag(), capturedMob.slot(),
								capturedMob.width(), capturedMob.height(), capturedMob.width() * capturedMob.height(), false, capturedMob.entityType(), 10,
								10));
			}
		}

		private Map<String, Integer> snapshotStacks(InventoryHandler inventory) {
			Map<String, Integer> stacks = new HashMap<>();
			for (int slot = 0; slot < inventory.getSlots(); slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				if (stack.isEmpty()) {
					continue;
				}
				String stackKey = BuiltInRegistries.ITEM.getKey(stack.getItem()) + ":" + stack.getCount();
				stacks.merge(stackKey, 1, Integer::sum);
			}
			return stacks;
		}

		private Map<UUID, String> snapshotCapturedMobs(IBackpackWrapper wrapper) {
			Map<UUID, String> capturedMobs = new HashMap<>();
			for (CapturedMob capturedMob : MobCatcherStorage.getCapturedMobs(wrapper)) {
				capturedMobs.put(capturedMob.id(), capturedMob.slot() + ":" + capturedMob.width() + "x" + capturedMob.height());
			}
			return capturedMobs;
		}

		private boolean capturedMobSlotsMatch(IBackpackWrapper wrapper, int[] expectedSlots) {
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
			List<CapturedMob> capturedMobs = MobCatcherStorage.getCapturedMobs(wrapper);
			if (capturedMobs.isEmpty()) {
				return Optional.empty();
			}
			int columns = MobCatcherStorage.getColumns(wrapper);
			int inventorySlots = wrapper.getInventoryHandler().getSlots();
			Set<Integer> occupiedSlots = new HashSet<>();
			for (CapturedMob capturedMob : capturedMobs) {
				if (capturedMob.slot() % columns + capturedMob.width() > columns) {
					return Optional.of("captured mob crosses row at slot " + capturedMob.slot());
				}
				for (int y = 0; y < capturedMob.height(); y++) {
					for (int x = 0; x < capturedMob.width(); x++) {
						int slot = capturedMob.slot() + y * columns + x;
						if (slot >= inventorySlots) {
							return Optional.of("captured mob outside inventory at slot " + slot);
						}
						if (!occupiedSlots.add(slot)) {
							return Optional.of("captured mobs overlap at slot " + slot);
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
				if (slot >= inventory.getSlots()) {
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
			ItemStack backpack = createBackpackStack();
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

		private IBackpackWrapper getMainBackpackWrapper(ServerPlayer player) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (!(mainBackpack.getItem() instanceof BackpackItem)) {
				throw new IllegalStateException("No backpack in player inventory slot 0");
			}
			return BackpackWrapper.fromStack(mainBackpack);
		}

		private int countItems(InventoryHandler inventory) {
			int count = 0;
			for (int slot = 0; slot < inventory.getSlots(); slot++) {
				count += inventory.getStackInSlot(slot).getCount();
			}
			return count;
		}

		private int countItems(InventoryHandler inventory, Item item) {
			int count = 0;
			for (int slot = 0; slot < inventory.getSlots(); slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				if (stack.is(item)) {
					count += stack.getCount();
				}
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
			try {
				sendJson(exchange, runOnClient(() -> RecipeViewerAutomationManager.queryJson(body)));
			} catch (RuntimeException e) {
				LOGGER.error("Recipe viewer query failed", e);
				sendJson(exchange, "{\"ok\":false," + jsonProperty("error", e.getMessage()) + "}");
			}
		}

		private void recipeViewerStats(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "GET");
			try {
				sendJson(exchange, runOnClient(RecipeViewerAutomationManager::statsJson));
			} catch (RuntimeException e) {
				LOGGER.error("Recipe viewer stats failed", e);
				sendJson(exchange, "{\"ok\":false," + jsonProperty("error", e.getMessage()) + "}");
			}
		}

		private String buildStateJson() {
			Minecraft minecraft = Minecraft.getInstance();
			Screen screen = minecraft.screen;
			return "{" + jsonProperty("screenClass", screen == null ? null : screen.getClass().getName()) + ","
					+ jsonProperty("screenSimpleName", screen == null ? null : screen.getClass().getSimpleName()) + ","
					+ jsonProperty("screenTitle", screen == null ? null : screen.getTitle().getString()) + ","
					+ jsonProperty("gameDirectory", minecraft.gameDirectory.getAbsolutePath()) + "," + "\"inWorld\":" + (minecraft.level != null) + ","
					+ "\"playerLoaded\":" + (minecraft.player != null) + "," + "\"windowWidth\":" + minecraft.getWindow().getWidth() + "," + "\"windowHeight\":"
					+ minecraft.getWindow().getHeight() + "," + "\"guiWidth\":" + minecraft.getWindow().getGuiScaledWidth() + "," + "\"guiHeight\":"
					+ minecraft.getWindow().getGuiScaledHeight() + "}";
		}

		private String buildScreenJson() {
			Minecraft minecraft = Minecraft.getInstance();
			Screen screen = minecraft.screen;
			StringBuilder json = new StringBuilder("{");
			json.append(jsonProperty("screenClass", screen == null ? null : screen.getClass().getName())).append(',');
			json.append(jsonProperty("screenSimpleName", screen == null ? null : screen.getClass().getSimpleName())).append(',');
			json.append(jsonProperty("title", screen == null ? null : screen.getTitle().getString())).append(',');
			json.append("\"slots\":[");
			if (screen instanceof AbstractContainerScreen<?> containerScreen) {
				boolean first = true;
				for (int i = 0; i < containerScreen.getMenu().slots.size(); i++) {
					Slot slot = containerScreen.getMenu().slots.get(i);
					if (!first) {
						json.append(',');
					}
					first = false;
					ItemStack stack = slot.getItem();
					json.append('{').append("\"menuSlot\":").append(i).append(',').append("\"containerSlot\":").append(slot.getSlotIndex()).append(',')
							.append("\"x\":").append(containerScreen.getGuiLeft() + slot.x).append(',').append("\"y\":")
							.append(containerScreen.getGuiTop() + slot.y).append(',')
							.append(jsonProperty("item", stack.isEmpty() ? null : stack.getHoverName().getString())).append(',').append("\"count\":")
							.append(stack.getCount()).append('}');
				}
			}
			json.append("],");
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
			json.append("],");
			json.append("\"mobCatcherCapturedMobs\":[");
			if (screen instanceof AbstractContainerScreen<?> containerScreen && containerScreen.getMenu() instanceof BackpackContainer backpackContainer) {
				boolean first = true;
				for (CapturedMob capturedMob : MobCatcherStorage.getCapturedMobs(backpackContainer.getStorageWrapper())) {
					if (capturedMob.slot() >= backpackContainer.getNumberOfStorageInventorySlots()) {
						continue;
					}
					Slot slot = backpackContainer.getSlot(capturedMob.slot());
					int x = containerScreen.getGuiLeft() + slot.x - 1;
					int y = containerScreen.getGuiTop() + slot.y - 1;
					int width = capturedMob.width() * 18;
					int height = capturedMob.height() * 18;
					if (!first) {
						json.append(',');
					}
					first = false;
					json.append('{').append(jsonProperty("id", capturedMob.id().toString())).append(',')
							.append(jsonProperty("entityType", capturedMob.entityType().toString())).append(',')
							.append(jsonProperty("displayName", capturedMob.displayName())).append(',').append("\"slot\":").append(capturedMob.slot())
							.append(',').append("\"x\":").append(x).append(',').append("\"y\":").append(y).append(',').append("\"width\":").append(width)
							.append(',').append("\"height\":").append(height).append(',').append("\"releaseX\":").append(x + width / 2).append(',')
							.append("\"releaseY\":").append(y + height / 2).append('}');
				}
			}
			json.append("]}");
			return json.toString();
		}

		private String clickWidget(String text, boolean contains, int button, int targetIndex) {
			Minecraft minecraft = Minecraft.getInstance();
			Screen screen = minecraft.screen;
			if (screen == null) {
				return "{\"ok\":false,\"error\":\"No screen is open\"}";
			}
			int index = 0;
			for (GuiEventListener child : screen.children()) {
				if (child instanceof AbstractWidget widget && widget.visible && widget.active
						&& (targetIndex == index || targetIndex < 0 && textMatches(widget.getMessage().getString(), text, contains))) {
					double x = widget.getX() + widget.getWidth() / 2.0;
					double y = widget.getY() + widget.getHeight() / 2.0;
					boolean clicked = screen.mouseClicked(x, y, button);
					screen.mouseReleased(x, y, button);
					return "{\"ok\":" + clicked + ",\"index\":" + index + ",\"x\":" + x + ",\"y\":" + y + "}";
				}
				index++;
			}
			return "{\"ok\":false,\"error\":\"Widget not found\"}";
		}

		private boolean confirmExperimentalWarningIfPresent() {
			Screen screen = Minecraft.getInstance().screen;
			if (screen == null || !screen.getClass().getSimpleName().equals("BackupConfirmScreen")) {
				return false;
			}
			for (GuiEventListener child : screen.children()) {
				if (child instanceof AbstractWidget widget && widget.visible && widget.active
						&& widget.getMessage().getString().equals("I know what I'm doing!")) {
					double x = widget.getX() + widget.getWidth() / 2.0;
					double y = widget.getY() + widget.getHeight() / 2.0;
					boolean clicked = screen.mouseClicked(x, y, 0);
					screen.mouseReleased(x, y, 0);
					return clicked;
				}
			}
			return false;
		}

		private String pressKey(String keyName, boolean ctrl, boolean shift, boolean alt) {
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
				int modifiers = (ctrl ? GLFW.GLFW_MOD_CONTROL : 0) | (shift ? GLFW.GLFW_MOD_SHIFT : 0) | (alt ? GLFW.GLFW_MOD_ALT : 0);
				boolean handled = minecraft.screen.keyPressed(keyCode, 0, modifiers);
				if (!handled && keyCode == GLFW.GLFW_KEY_ESCAPE) {
					minecraft.screen.onClose();
					handled = true;
				}
				return "{\"ok\":true,\"handled\":" + handled + ",\"modifiers\":" + modifiers + "}";
			}
			return "{\"ok\":true,\"handled\":false}";
		}

		private String moveToSlot(int menuSlot) {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) {
				return "{\"ok\":false,\"error\":\"No container screen is open\"}";
			}
			if (menuSlot < 0 || menuSlot >= containerScreen.getMenu().slots.size()) {
				return "{\"ok\":false,\"error\":\"Invalid menu slot\"}";
			}
			Slot slot = containerScreen.getMenu().slots.get(menuSlot);
			int x = containerScreen.getGuiLeft() + slot.x + 8;
			int y = containerScreen.getGuiTop() + slot.y + 8;
			double scale = minecraft.getWindow().getGuiScale();
			GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), x * scale, y * scale);
			containerScreen.mouseMoved(x, y);
			return "{\"ok\":true,\"menuSlot\":" + menuSlot + ",\"x\":" + x + ",\"y\":" + y + "}";
		}

		private String throwSlot(int menuSlot, boolean fullStack) {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen) || minecraft.player == null || minecraft.gameMode == null) {
				return "{\"ok\":false,\"error\":\"No container screen is open\"}";
			}
			if (menuSlot < 0 || menuSlot >= containerScreen.getMenu().slots.size()) {
				return "{\"ok\":false,\"error\":\"Invalid menu slot\"}";
			}
			minecraft.gameMode.handleInventoryMouseClick(containerScreen.getMenu().containerId, menuSlot, fullStack ? 1 : 0, ClickType.THROW, minecraft.player);
			return "{\"ok\":true,\"menuSlot\":" + menuSlot + ",\"fullStack\":" + fullStack + "}";
		}

		private String inventoryEssentialsDropByType(int menuSlot) {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) {
				return "{\"ok\":false,\"error\":\"No container screen is open\"}";
			}
			if (menuSlot < 0 || menuSlot >= containerScreen.getMenu().slots.size()) {
				return "{\"ok\":false,\"error\":\"Invalid menu slot\"}";
			}
			try {
				Class<?> clientClass = Class.forName("net.blay09.mods.inventoryessentials.client.InventoryEssentialsClient");
				Class<?> controlsClass = Class.forName("net.blay09.mods.inventoryessentials.client.InventoryControls");
				Method getInventoryControls = clientClass.getMethod("getInventoryControls", Screen.class);
				Method dropByType = controlsClass.getMethod("dropByType", AbstractContainerScreen.class, Slot.class);
				Object controls = getInventoryControls.invoke(null, containerScreen);
				Slot slot = containerScreen.getMenu().slots.get(menuSlot);
				boolean handled = (Boolean) dropByType.invoke(controls, containerScreen, slot);
				return "{\"ok\":true,\"handled\":" + handled + ",\"menuSlot\":" + menuSlot + "," + jsonProperty("controls", controls.getClass().getName())
						+ "}";
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Failed to invoke Inventory Essentials dropByType", e);
			}
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
			DemoMouseMotion.moveTo(targetX, targetY, 12, () -> {
			});
			return "{\"ok\":true,\"x\":" + targetX + ",\"y\":" + targetY + "}";
		}

		private String maximizeWindow() {
			Minecraft minecraft = Minecraft.getInstance();
			GLFW.glfwMaximizeWindow(minecraft.getWindow().getWindow());
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
				case "KP_0", "NUMPAD0" -> GLFW.GLFW_KEY_KP_0;
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

		private record StorageGuiRegressionSuite(List<StorageGuiRegressionScenario> scenarios) {
		}

		private record StorageGuiRegressionScenario(String name, int inventorySlots, int upgradeSlots, int columnsTaken, int openTab, SlotStackSpec[] contents,
				SlotStackSpec[] playerContents, SlotStackSpec[] upgradeContents, int[] noSortSlots, int[] memorySlots, CapturedMobSpec[] capturedMobs,
				StorageGuiAction[] actions) {
		}

		private record SlotStackSpec(int slot, Item item, int count) {
		}

		private record StorageGuiAction(String type, String snapshot, int[] slots, SlotRef[] slotRefs, SlotRef slot, int button, String operation,
				int upgradeSlot, Optional<Item> item, int count, int hotbarSlot, int expectedColumnsTaken, int expectedStorageSlots) {
		}

		private record SlotRef(String area, int index, int upgradeSlot) {
			private static SlotRef storage(int index) {
				return new SlotRef("storage", index, -1);
			}
		}

		private record StorageGuiSlotSnapshot(SlotRef slotRef, ItemStack stack) {
		}

		private record StorageGuiRegressionResult(String name, boolean passed, int actions, int storageSlots, int menuSlots, int upgradeSlots, String error) {
		}

		private record StorageGuiRegressionState(int storageSlots, int menuSlots, int upgradeSlots) {
		}

		private record StorageGuiColumnUpgradeExpectation(int expectedColumnsTaken, int expectedStorageSlots, boolean upgradeSlotEmpty, boolean carriedEmpty) {
		}

		private record StorageGuiColumnUpgradeState(int columnsTaken, int storageSlots, boolean upgradeSlotEmpty, boolean carriedEmpty) {
			private boolean matches(StorageGuiColumnUpgradeExpectation expectation) {
				return columnsTaken == expectation.expectedColumnsTaken() && storageSlots == expectation.expectedStorageSlots()
						&& upgradeSlotEmpty == expectation.upgradeSlotEmpty() && carriedEmpty == expectation.carriedEmpty();
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

		private record ControllerDoubleChestRegressionResult(String name, boolean passed, int registeredStorages, int slots, String positions,
				String chestState, String error) {
		}

		private record ControllerFilterStorageSpec(String name, BlockPos pos, Item filterItem, boolean allowList, PrimaryMatch primaryMatch) {
		}

		private record ControllerLockedStorageSpec(BlockPos pos, Item item, int slot, Item barrelItem) {
		}

		private record ControllerFilterInsertExpectation(String name, Item item, int count, int calls, Set<BlockPos> expectedPositions) {
		}

		private record ControllerFilterInsertStats(int calls, long items) {
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

		private record AdvancedCompactingHighStackRegressionResult(String name, boolean passed, int firstSlotCount, int secondSlotCount, int triggerCount,
				int expectedNuggets, int actualNuggets, int expectedIngots, int actualIngots, int expectedBlocks, int actualBlocks, int insertRemainder,
				String error) {
		}

		private record SubMobCatcherRegressionState(String context, int storageSlots, boolean slot0Backpack, int currentMobCount, String currentMobId,
				int nestedMobCount, String nestedMobId) {
			private boolean parentMatches() {
				return BackpackContext.ContextType.ITEM_BACKPACK.name().equals(context) && slot0Backpack && storageSlots == 81 && currentMobCount == 1
						&& SUB_MOB_CATCHER_PARENT_MOB_ID.toString().equals(currentMobId);
			}

			private boolean subMatches() {
				return BackpackContext.ContextType.ITEM_SUB_BACKPACK.name().equals(context) && storageSlots == 144 && currentMobCount == 1
						&& SUB_MOB_CATCHER_SUB_MOB_ID.toString().equals(currentMobId);
			}
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

		private static Optional<Double> extractDouble(String json, String key) {
			return extractRawValue(json, key).map(Double::parseDouble);
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
