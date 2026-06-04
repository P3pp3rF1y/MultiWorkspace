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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.devclientautomation.demo.DemoCommand;
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
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.StorageWrapperRepository;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
		private volatile String curiosColumnUpgradeRegressionIdentifier;
		private volatile ItemStack curiosColumnUpgradeRegressionBackpack = ItemStack.EMPTY;

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
				httpServer.createContext("/backpack/gui-regression/run", this::backpackGuiRegressionRun);
				httpServer.createContext("/backpack/remote-upgrade-slot-regression", this::backpackRemoteUpgradeSlotRegression);
				httpServer.createContext("/backpack/dropped-items", this::droppedItemsStatus);
				httpServer.createContext("/backpack/clear-dropped-items", this::clearDroppedItems);
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
					Path screenshotPath = Files.createTempFile("dev-client-automation-screenshot", ".png");
					try {
						image.writeToFile(screenshotPath);
						return Files.readAllBytes(screenshotPath);
					} finally {
						Files.deleteIfExists(screenshotPath);
					}
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

		private void backpackRemoteUpgradeSlotRegression(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnClient(this::runBackpackRemoteUpgradeSlotRegression));
		}

		private void backpackGuiRegressionRun(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			sendJsonHandling(exchange, () -> runBackpackGuiRegression(body));
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

			LevelSettings levelSettings = new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()),
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
			List<ItemStack> nestedBackpacks = new java.util.ArrayList<>();
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
					if (context == BackpackGuiRegressionContext.CURIOS) {
						runOnServer(this::prepareOpenCuriosBackpackServerMenuColumnUpgrade);
						return runOnServer(this::clickOpenCuriosBackpackServerMenuColumnUpgrade);
					}
					return runOnClient(() -> clickBackpackGuiRegressionColumnUpgrade(context));
				} catch (RuntimeException e) {
					lastError = e;
					sleep(50);
				}
			} while (System.nanoTime() < deadline);
			throw lastError == null ? new IllegalStateException("Timed out waiting to click " + context.jsonName() + " backpack upgrade") : lastError;
		}

		private Boolean prepareOpenCuriosBackpackServerMenuColumnUpgrade(ServerPlayer player) {
			if (!(player.containerMenu instanceof BackpackContainer menu)) {
				throw new IllegalStateException("Curios backpack server menu is not open");
			}
			menu.getStorageWrapper().getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
			menu.getStorageWrapper().getUpgradeHandler().saveInventory();
			menu.getStorageWrapper().setColumnsTaken(2, false);
			return true;
		}

		private PlacedColumnUpgradeClickExpectation clickOpenCuriosBackpackServerMenuColumnUpgrade(ServerPlayer player) {
			if (!(player.containerMenu instanceof BackpackContainer menu)) {
				throw new IllegalStateException("Curios backpack server menu is not open");
			}
			if (!menu.getCarried().isEmpty()) {
				throw new IllegalStateException("Cursor must be empty before running curios backpack regression");
			}

			ItemStack slotStack = menu.getStorageWrapper().getUpgradeHandler().getStackInSlot(1);
			if (slotStack.isEmpty() || !(slotStack.getItem() instanceof IUpgradeItem<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
				throw new IllegalStateException("Curios backpack server upgrade slot 1 must contain a column-taking upgrade");
			}

			int beforeColumnsTaken = menu.getStorageWrapper().getColumnsTaken();
			int expectedColumnsTaken = beforeColumnsTaken - upgradeItem.getInventoryColumnsTaken();
			int rows = menu.getStorageWrapper().getNumberOfSlotRows();
			int handlerSlots = menu.getStorageWrapper().getInventoryHandler().getSlots();
			int baseColumns = handlerSlots <= 81 ? 9 : 12;
			int baseStorageSlots = handlerSlots / rows == baseColumns ? handlerSlots : handlerSlots + beforeColumnsTaken * rows;
			int expectedStorageSlots = baseStorageSlots - expectedColumnsTaken * rows;

			menu.clicked(menu.getFirstUpgradeSlot() + 1, 0, ClickType.PICKUP, player);
			return new PlacedColumnUpgradeClickExpectation(expectedColumnsTaken, expectedStorageSlots);
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
					openSubBackpackColumnUpgradeRegressionWhenReady();
				}
			}
		}

		private void openSubBackpackColumnUpgradeRegressionWhenReady() {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			do {
				runOnServer(this::openSubBackpackColumnUpgradeRegression);
				sleep(100);
				if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen
						&& Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK)) {
					return;
				}
			} while (System.nanoTime() < deadline);
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
			Direction facing = player.getDirection();
			BlockPos pos = getRegressionBackpackPos(player);

			level.setBlock(pos, ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, facing.getOpposite()), 3);
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
			ServerLevel level = player.serverLevel();
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
			curiosColumnUpgradeRegressionIdentifier = identifier;
			curiosColumnUpgradeRegressionBackpack = backpack.copy();
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
			ItemStack backpack = curiosColumnUpgradeRegressionBackpack.isEmpty() ? createColumnUpgradeRegressionBackpack() : curiosColumnUpgradeRegressionBackpack.copy();
			String identifier = curiosColumnUpgradeRegressionIdentifier == null ? getCuriosBackpackIdentifier(Minecraft.getInstance().player, backpack) : curiosColumnUpgradeRegressionIdentifier;
			setCuriosStack(Minecraft.getInstance().player, identifier, 0, backpack);
			PlayerInventoryHandler inventoryHandler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
					.orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
			ItemStack clientStack = inventoryHandler.getStackInSlot(Minecraft.getInstance().player, identifier, 0);
			if (!(clientStack.getItem() instanceof BackpackItem)) {
				throw new IllegalStateException("Client Curios backpack stack was not set in " + identifier + "; found " + clientStack.getItem());
			}
			IBackpackWrapper clientWrapper = BackpackWrapper.fromStack(clientStack);
			clientWrapper.setSlotNumbers(81, 3);
			clientWrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
			clientWrapper.getUpgradeHandler().saveInventory();
			clientWrapper.setColumnsTaken(2, false);
			ItemStack upgradeStack = clientWrapper.getUpgradeHandler().getStackInSlot(1);
			if (upgradeStack.isEmpty() || !(upgradeStack.getItem() instanceof IUpgradeItem<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
				throw new IllegalStateException("Client Curios backpack upgrade slot 1 was not prepared; found " + upgradeStack);
			}
			return true;
		}

		private Boolean openCuriosBackpackColumnUpgradeRegression(ServerPlayer player) {
			String identifier = curiosColumnUpgradeRegressionIdentifier == null ? getCuriosBackpackIdentifier(player, createColumnUpgradeRegressionBackpack()) : curiosColumnUpgradeRegressionIdentifier;

			BackpackContext.Item backpackContext = new BackpackContext.Item(CompatModIds.CURIOS, identifier, 0);
			ItemStack upgradeStack = backpackContext.getBackpackWrapper(player).getUpgradeHandler().getStackInSlot(1);
			if (upgradeStack.isEmpty() || !(upgradeStack.getItem() instanceof IUpgradeItem<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
				throw new IllegalStateException("Server Curios backpack upgrade slot 1 was not prepared in " + identifier + "; found " + upgradeStack);
			}
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Curios Column Regression")), backpackContext::toBuffer);
			return true;
		}

		private String getEquippedCuriosBackpackIdentifier(Player player) {
			PlayerInventoryHandler inventoryHandler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
					.orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
			for (String identifier : inventoryHandler.getIdentifiers(player)) {
				if (inventoryHandler.getSlotCount(player, identifier) > 0 && inventoryHandler.getStackInSlot(player, identifier, 0).getItem() instanceof BackpackItem) {
					return identifier;
				}
			}
			throw new IllegalStateException("No equipped Curios backpack was found");
		}

		private Boolean setupSubBackpackColumnUpgradeRegression(ServerPlayer player) {
			player.getInventory().items.set(0, createParentBackpackWithColumnUpgradeSubBackpack());
			player.getInventory().setChanged();
			return true;
		}

		private Boolean setupParentMobCatcherBackpackRegression(ServerPlayer player) {
			player.getInventory().items.set(0, createMobCatcherRegressionBackpack(81, 3, SUB_MOB_CATCHER_PARENT_MOB_ID, 0, "Parent Pig"));
			player.getInventory().setChanged();
			return true;
		}

		private Boolean setupClientSubBackpackColumnUpgradeRegression() {
			if (Minecraft.getInstance().player == null) {
				throw new IllegalStateException("Client player is not available");
			}
			Minecraft.getInstance().player.getInventory().items.set(0, createParentBackpackWithColumnUpgradeSubBackpack());
			Minecraft.getInstance().player.getInventory().setChanged();
			return true;
		}

		private Boolean setupClientParentMobCatcherBackpackRegression() {
			if (Minecraft.getInstance().player == null) {
				throw new IllegalStateException("Client player is not available");
			}
			Minecraft.getInstance().player.getInventory().items.set(0, createMobCatcherRegressionBackpack(81, 3, SUB_MOB_CATCHER_PARENT_MOB_ID, 0, "Parent Pig"));
			Minecraft.getInstance().player.getInventory().setChanged();
			return true;
		}

		private Boolean openParentBackpackColumnUpgradeRegression(ServerPlayer player) {
			BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Parent Column Regression")), backpackContext::toBuffer);
			return true;
		}

		private Boolean openParentMobCatcherBackpackRegression(ServerPlayer player) {
			BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Mob Catcher Parent Regression")), backpackContext::toBuffer);
			return true;
		}

		private Boolean openSubBackpackColumnUpgradeRegressionFromClient() {
			PacketDistributor.sendToServer(new BackpackOpenPayload(0));
			return true;
		}

		private Boolean openSubBackpackColumnUpgradeRegression(ServerPlayer player) {
			BackpackContext.ItemSubBackpack backpackContext = new BackpackContext.ItemSubBackpack(PlayerInventoryProvider.MAIN_INVENTORY, "", 0, false, 0, true);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Sub Column Regression")), backpackContext::toBuffer);
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

		private ItemStack createParentBackpackWithColumnUpgradeSubBackpack() {
			ItemStack parentBackpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
			parentBackpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
			parentBackpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 81);
			parentBackpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 3);
			IBackpackWrapper parentWrapper = BackpackWrapper.fromStack(parentBackpack);
			parentWrapper.setSlotNumbers(81, 3);
			parentWrapper.getInventoryHandler().setStackInSlot(0, createColumnUpgradeRegressionBackpack());
			parentWrapper.getInventoryHandler().saveInventory();
			return parentBackpack;
		}

		private ItemStack createColumnUpgradeRegressionBackpack() {
			ItemStack backpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
			backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
			backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 81);
			backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 3);
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
			backpackWrapper.setSlotNumbers(81, 3);
			backpackWrapper.getInventoryHandler();
			backpackWrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
			backpackWrapper.getUpgradeHandler().saveInventory();
			backpackWrapper.setColumnsTaken(2, false);
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
			MobCatcherStorage.addCapturedMob(backpackWrapper, new CapturedMob(mobId, ResourceLocation.parse("minecraft:pig"), new CompoundTag(), mobSlot, 1, 1, 1, false, displayName, 10, 10));
			return backpack;
		}

		private void ensureCuriosSlot(ServerPlayer player, String identifier, int slots) {
			PlayerInventoryHandler inventoryHandler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
					.orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
			if (inventoryHandler.getSlotCount(player, identifier) < slots) {
				player.server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
						"curios add " + identifier + " " + player.getGameProfile().getName() + " " + slots);
			}
			if (inventoryHandler.getSlotCount(player, identifier) < slots) {
				throw new IllegalStateException("Unable to configure Curios slot " + identifier + "; slot count is " + inventoryHandler.getSlotCount(player, identifier));
			}
		}

		private String getCuriosBackpackIdentifier(LivingEntity player, ItemStack backpack) {
			for (String identifier : getCuriosItemSlotTypes(backpack, player).keySet()) {
				return identifier;
			}

			if (!(player instanceof Player inventoryPlayer)) {
				return "back";
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
				return (Map<String, ?>) curiosSlotTypesClass.getMethod("getItemSlotTypes", ItemStack.class, LivingEntity.class)
						.invoke(null, backpack, player);
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
			return clickOpenBackpackColumnUpgrade(getOpenPlacedBackpackMenu(), "Placed");
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
			return clickOpenBackpackColumnUpgrade(getOpenCuriosBackpackMenu(), "Curios");
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
			return clickOpenBackpackColumnUpgrade(getOpenSubBackpackMenu(), "Sub");
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

		private PlacedColumnUpgradeClickExpectation clickOpenBackpackColumnUpgrade(BackpackContainer menu, String contextName) {
			if (menu.getNumberOfUpgradeSlots() < 2) {
				throw new IllegalStateException(contextName + " backpack needs at least two upgrade slots");
			}
			if (!menu.getCarried().isEmpty()) {
				throw new IllegalStateException("Cursor must be empty before running " + contextName.toLowerCase(Locale.ROOT) + " backpack regression");
			}

			Slot slot = menu.upgradeSlots.get(1);
			ItemStack slotStack = slot.getItem();
			if (slotStack.isEmpty() && "Curios".equals(contextName)) {
				menu.getStorageWrapper().getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
				menu.getStorageWrapper().getUpgradeHandler().saveInventory();
				menu.getStorageWrapper().setColumnsTaken(2, false);
				slotStack = slot.getItem();
			}
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
			if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
				throw new IllegalStateException("Backpack container screen is not open");
			}
			double x = containerScreen.getGuiLeft() + slot.x + 8.0;
			double y = containerScreen.getGuiTop() + slot.y + 8.0;
			if (!containerScreen.mouseClicked(x, y, 0)) {
				throw new IllegalStateException("Backpack upgrade slot click was not handled");
			}
			containerScreen.mouseReleased(x, y, 0);
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

		private String placedColumnUpgradeRegressionJson(String name, boolean ok, PlacedColumnUpgradeClickExpectation expectation, PlacedColumnUpgradeState state,
				String error) {
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
			return BackpackWrapper.fromStack(nestedBackpack);
		}

		private SubMobCatcherRegressionState getSubMobCatcherRegressionState(BackpackContainer menu, IBackpackWrapper nestedWrapper) {
			List<CapturedMob> currentMobs = MobCatcherStorage.getCapturedMobs(menu.getStorageWrapper());
			List<CapturedMob> nestedMobs = nestedWrapper == null ? List.of() : MobCatcherStorage.getCapturedMobs(nestedWrapper);
			return new SubMobCatcherRegressionState(menu.getBackpackContext().getType().name(), menu.getStorageWrapper().getInventoryHandler().getSlots(), nestedWrapper != null,
					currentMobs.size(), currentMobs.isEmpty() ? null : currentMobs.get(0).id().toString(), nestedMobs.size(), nestedMobs.isEmpty() ? null : nestedMobs.get(0).id().toString());
		}

		private String subMobCatcherRegressionJson(String name, boolean ok, SubMobCatcherRegressionState parentState, SubMobCatcherRegressionState subState, String error) {
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
			JsonArray capturedMobs = scenario.getAsJsonArray("capturedMobs");
			CapturedMobSpec[] mobs = new CapturedMobSpec[capturedMobs.size()];
			for (int i = 0; i < capturedMobs.size(); i++) {
				JsonObject capturedMob = capturedMobs.get(i).getAsJsonObject();
				mobs[i] = new CapturedMobSpec(capturedMob.get("slot").getAsInt(), capturedMob.get("width").getAsInt(), capturedMob.get("height").getAsInt(),
						capturedMob.has("entityType") ? capturedMob.get("entityType").getAsString() : "minecraft:pig");
			}
			return mobs;
		}

		private ColumnUpgradeRegressionResult runColumnUpgradeRegressionScenario(ColumnUpgradeRegressionScenario scenario, ColumnUpgradeStackGenerator stackGenerator) {
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
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(), "stack snapshot changed");
				}
				if (scenario.expectedCapturedMobSlots().length == 0 && !beforeCapturedMobs.equals(afterCapturedMobs)) {
					return new ColumnUpgradeRegressionResult(scenario.name(), false, true, true, beforeStacks.size(), afterStacks.size(), "captured mobs changed");
				}
			} else if (!beforeStacks.equals(afterStacks) || !beforeCapturedMobs.equals(afterCapturedMobs)) {
				return new ColumnUpgradeRegressionResult(scenario.name(), false, false, false, beforeStacks.size(), afterStacks.size(), "blocked insertion mutated stacks");
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

			InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(wrapper.getInventoryLayoutParts(currentColumns, targetColumns), targetSlots, targetColumns,
					targetColumnsTaken < currentColumnsTaken);
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
				MobCatcherStorage.addCapturedMob(wrapper, new CapturedMob(new UUID(0, i + 1), ResourceLocation.parse(capturedMob.entityType()), new CompoundTag(),
						capturedMob.slot(), capturedMob.width(), capturedMob.height(), capturedMob.width() * capturedMob.height(), false, capturedMob.entityType(), 10, 10));
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
			try {
				sendJson(exchange, runOnClient(() -> RecipeViewerAutomationManager.queryJson(body)));
			} catch (RuntimeException e) {
				LOGGER.error("Recipe viewer query failed", e);
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
			double scale = minecraft.getWindow().getGuiScale();
			GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), targetX * scale, targetY * scale);
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

		private record SubMobCatcherRegressionState(String context, int storageSlots, boolean slot0Backpack, int currentMobCount, String currentMobId,
				int nestedMobCount, String nestedMobId) {
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
