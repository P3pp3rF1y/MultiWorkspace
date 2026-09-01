package net.p3pp3rf1y.devclientautomation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssembleRailType;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import com.simibubi.create.content.logistics.chute.SmartChuteBlock;
import com.simibubi.create.content.logistics.chute.SmartChuteBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.WoodType;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.demo.DemoCommand;
import net.p3pp3rf1y.devclientautomation.recipeviewer.RecipeViewerAutomationManager;
import net.p3pp3rf1y.devclientautomation.scenarios.backpacks.BackpackRegressionEndpoints;
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
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.inception.InventoryOrder;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.util.InventoryInteractionHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitResult;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitter;
import net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.create.MountedStorageBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTab;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsTab;
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
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.DecorationTableScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.DecorationTableMenu;
import net.p3pp3rf1y.sophisticatedstorage.entity.MovingStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageBoat;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageMinecart;
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

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

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
		private static final UUID CRAFTING_TRANSFER_BACKPACK_UUID = new UUID(0L, 103L);

		private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
			Thread thread = new Thread(runnable, "Dev Client Automation");
			thread.setDaemon(true);
			return thread;
		});
		private HttpServer httpServer;
		private volatile Issue23SetupResult issue23SetupResult;

		void start() {
			try {
				httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
				httpServer.createContext("/capabilities", this::capabilities);
				httpServer.createContext("/state", this::state);
				httpServer.createContext("/screen", this::screen);
				httpServer.createContext("/click-widget", this::clickWidget);
				httpServer.createContext("/key", this::key);
				httpServer.createContext("/mouse/move", this::moveMouse);
				httpServer.createContext("/window/maximize", this::maximizeWindow);
				httpServer.createContext("/wait", this::waitFor);
				httpServer.createContext("/client/shutdown-world", this::shutdownWorld);
				httpServer.createContext("/client/stop", this::stopClient);
				httpServer.createContext("/world/load", this::loadWorld);
				httpServer.createContext("/screenshot", this::screenshot);
				httpServer.createContext("/recipe-viewer/state", this::recipeViewerState);
				httpServer.createContext("/recipe-viewer/search", this::recipeViewerSearch);
				httpServer.createContext("/recipe-viewer/open", this::recipeViewerOpen);
				httpServer.createContext("/recipe-viewer/query", this::recipeViewerQuery);
				httpServer.createContext("/recipe-viewer/backpack-crafting-transfer", this::recipeViewerBackpackCraftingTransfer);
				httpServer.createContext("/backpack/column-upgrade-regressions", this::backpackColumnUpgradeRegressions);
				httpServer.createContext("/backpack/storage-gui-regressions", this::backpackStorageGuiRegressions);
				httpServer.createContext("/backpack/gui-regression/run", this::backpackGuiRegressionRun);
				httpServer.createContext("/backpack/inception-magnet-persistence/setup", this::setupInceptionMagnetPersistence);
				httpServer.createContext("/backpack/inception-magnet-persistence/pickup", this::pickupWithInceptionMagnet);
				httpServer.createContext("/backpack/inception-magnet-persistence/status", this::inceptionMagnetPersistenceStatus);
				httpServer.createContext("/backpack/remote-upgrade-slot-regression", this::backpackRemoteUpgradeSlotRegression);
				httpServer.createContext("/backpack/filter-regression", BackpackRegressionEndpoints::runFilter);
				httpServer.createContext("/backpack/magnet-regression", BackpackRegressionEndpoints::runMagnet);
				httpServer.createContext("/backpack/pickup-regression", BackpackRegressionEndpoints::runPickup);
				httpServer.createContext("/backpack/restock-regression", BackpackRegressionEndpoints::runRestock);
				httpServer.createContext("/backpack/refill-regression", BackpackRegressionEndpoints::runRefill);
				httpServer.createContext("/backpack/linked-storage-regression", BackpackRegressionEndpoints::runLinkedStorage);
				httpServer.createContext("/backpack/linked-storage-inception-regression", BackpackRegressionEndpoints::runLinkedStorageInception);
				httpServer.createContext("/backpack/linked-storage-starter-kit", this::giveLinkedStorageStarterKit);
				httpServer.createContext("/backpack/lifecycle-regression", BackpackRegressionEndpoints::runLifecycle);
				httpServer.createContext("/backpack/access-regression", BackpackRegressionEndpoints::runAccess);
				httpServer.createContext("/backpack/curios-access-regression", BackpackRegressionEndpoints::runCuriosAccess);
				httpServer.createContext("/inventory-interactions/keybind-regression", this::inventoryInteractionsKeybindRegression);
				httpServer.createContext("/storage/controller-filter-regressions", this::storageControllerFilterRegressions);
				httpServer.createContext("/storage/item-display-preview/open", this::openStorageItemDisplayPreview);
				httpServer.createContext("/storage/decoration-table-render-preview/open", this::openDecorationTableRenderPreview);
				httpServer.createContext("/storage/decoration-table-render-preview/drag", this::dragDecorationTableRenderPreview);
				httpServer.createContext("/storage/issue-23-reproduce", this::reproduceStorageIssue23);
				httpServer.createContext("/storage/issue-23-status", this::issue23Status);
				httpServer.createContext("/storage/issue-23-open-source", this::openIssue23SourceStorage);
				httpServer.createContext("/storage/controller-ae2-profile-setup", this::storageControllerAe2ProfileSetup);
				httpServer.createContext("/storage/controller-ae2-profile-simulate-query", this::storageControllerAe2ProfileSimulateQuery);
				httpServer.setExecutor(executor);
				httpServer.start();
				writeDiscoveryFile(httpServer.getAddress().getPort());
				LOGGER.info("Dev client automation bridge started on 127.0.0.1:{}", httpServer.getAddress().getPort());
			} catch (IOException e) {
				LOGGER.error("Failed to start dev client automation bridge", e);
			}
		}

		private void capabilities(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "GET");
			sendJson(exchange,
					"{\"ok\":true,\"protocolVersion\":1,\"loader\":\"neoforge\",\"minecraftVersion\":\"1.21.10\",\"features\":[\"state\",\"world-load\",\"screenshot\",\"recipe-viewer\",\"backpacks\",\"storage\",\"inventory-interactions\"]}");
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

		private void shutdownWorld(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> {
				MinecraftServer server = runOnClient(Minecraft.getInstance()::getSingleplayerServer);
				if (server == null) {
					throw new IllegalStateException("Singleplayer server is not loaded");
				}
				server.halt(true);
				return "{\"ok\":true,\"shutdown\":true}";
			});
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
					sendJson(exchange, "{\"ok\":true,\"worldLoaded\":true,\"created\":" + loadResult.contains("\"created\":true") + ",\"timedOut\":false}");
					return;
				}
				if (autoConfirmExperimental) {
					runOnClient(this::confirmExperimentalWarningIfPresent);
				}
				sleep(100);
			}
			sendJson(exchange, "{\"ok\":false,\"worldLoaded\":false,\"timedOut\":true}");
		}

		private void giveLinkedStorageStarterKit(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(player -> {
				List<ItemStack> stacks = new ArrayList<>();
				stacks.add(new ItemStack(ModItems.GOLD_BACKPACK.get()));
				stacks.add(new ItemStack(ModItems.DIAMOND_BACKPACK.get()));
				stacks.add(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
				stacks.add(createTintedBackpack(0xFF_D32F2F, 0xFF_7F0000));
				stacks.add(createTintedBackpack(0xFF_388E3C, 0xFF_1B5E20));
				stacks.add(createTintedBackpack(0xFF_1976D2, 0xFF_0D47A1));
				for (int i = 0; i < 3; i++) {
					stacks.add(new ItemStack(ENDER_LINKER.get()));
				}
				stacks.add(new ItemStack(Items.CRAFTING_TABLE));
				stacks.forEach(stack -> player.getInventory().add(stack));
				player.getInventory().setChanged();
				player.inventoryMenu.broadcastChanges();
				return "{\"ok\":true,\"items\":" + stacks.size() + "}";
			}));
		}

		private static ItemStack createTintedBackpack(int mainColor, int accentColor) {
			ItemStack backpack = new ItemStack(ModItems.BACKPACK.get());
			BackpackItem.setColors(backpack, mainColor, accentColor);
			return backpack;
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

			LevelSettings levelSettings = new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
					new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT);
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
			sendJson(exchange, runOnClient(() -> RecipeViewerAutomationManager.queryJson(body)));
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

		private void backpackColumnUpgradeRegressions(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::runBackpackColumnUpgradeRegressions));
		}

		private void backpackStorageGuiRegressions(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, StorageGuiRegressionRunner::run);
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

		private void setupInceptionMagnetPersistence(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::setupInceptionMagnetPersistence));
		}

		private void pickupWithInceptionMagnet(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, () -> runOnServer(this::pickupWithInceptionMagnet));
		}

		private void inceptionMagnetPersistenceStatus(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "GET");
			sendJsonHandling(exchange, () -> runOnServer(this::inceptionMagnetPersistenceStatus));
		}

		private void inventoryInteractionsKeybindRegression(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			sendJsonHandling(exchange, this::runInventoryInteractionsKeybindRegression);
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
				return runStorageControllerFilterRegressions(player, !mode.equals("setup"), profileCapacity);
			}));
		}

		private void reproduceStorageIssue23(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			requireCreateForIssue23();
			issue23SetupResult = runOnServer(player -> CreateIssue23Automation.setupStorageIssue23Reproduction(this, player));
			Issue23SetupResult setupResult = getIssue23SetupResult();
			waitForServerCondition("Smart Chute to transfer exactly 64 items", player -> CreateIssue23Automation.countItemsInStorage(this,
					(ServerLevel) player.level(), setupResult.receiverPos(), Items.COBBLESTONE) == 64);
			// Allow repeated exact-mode attempts to run after the first completed batch.
			sleep(5000);
			sendJsonHandling(exchange, () -> runOnServer(player -> CreateIssue23Automation.issue23ReproductionResultJson(this, player, setupResult)));
		}

		private void issue23Status(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "GET");
			requireCreateForIssue23();
			Issue23SetupResult setupResult = getIssue23SetupResult();
			sendJsonHandling(exchange, () -> runOnServer(player -> CreateIssue23Automation.issue23ReproductionResultJson(this, player, setupResult)));
		}

		private void openIssue23SourceStorage(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			requireCreateForIssue23();
			Issue23SetupResult setupResult = getIssue23SetupResult();
			sendJsonHandling(exchange, () -> {
				runOnServer(player -> CreateItemDisplayPreviewAutomation.openCreateContraptionStorage(player, setupResult.contraptionEntityId(),
						setupResult.mountedStoragePos()));
				return "{\"ok\":true}";
			});
		}

		private Issue23SetupResult getIssue23SetupResult() {
			if (issue23SetupResult == null) {
				throw new IllegalStateException("Issue #23 reproduction has not been set up");
			}
			return issue23SetupResult;
		}

		private void requireCreateForIssue23() {
			if (!ModList.get().isLoaded("create")) {
				throw new IllegalStateException("Issue #23 automation requires Create to be loaded");
			}
		}

		private void openStorageItemDisplayPreview(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			String scenario = extractString(body, "scenario").orElse("barrel_north");
			DisplaySide displaySide = DisplaySide.fromName(extractString(body, "displaySide").orElse(DisplaySide.FRONT.getSerializedName()));
			sendJsonHandling(exchange, () -> openStorageItemDisplayPreview(scenario, displaySide));
		}

		private void openDecorationTableRenderPreview(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String itemName = extractString(readBody(exchange), "item").orElse("storage_io");
			sendJsonHandling(exchange, () -> openDecorationTableRenderPreview(itemName));
		}

		private void dragDecorationTableRenderPreview(HttpExchange exchange) throws IOException {
			requireMethod(exchange, "POST");
			String body = readBody(exchange);
			int x = extractInt(body, "x").orElseThrow(() -> new IllegalArgumentException("Missing x"));
			int y = extractInt(body, "y").orElseThrow(() -> new IllegalArgumentException("Missing y"));
			int dragX = extractInt(body, "dragX").orElseThrow(() -> new IllegalArgumentException("Missing dragX"));
			int dragY = extractInt(body, "dragY").orElseThrow(() -> new IllegalArgumentException("Missing dragY"));
			sendJsonHandling(exchange, () -> runOnClient(() -> dragDecorationTableRenderPreview(x, y, dragX, dragY)));
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
			if ("depositLimitedBarrelGuiState".equals(type)) {
				return runDepositLimitedBarrelGuiStateRegression(name, request);
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

		private String runDepositLimitedBarrelGuiStateRegression(String name, JsonObject request) {
			int depositCount = getInt(request, "depositCount", 64);
			int targetFreeSpace = getInt(request, "targetFreeSpace", 16);
			boolean locked = request.has("locked") ? request.get("locked").getAsBoolean() : true;
			boolean inventoryFilter = request.has("inventoryFilter") ? request.get("inventoryFilter").getAsBoolean() : true;

			List<DepositLimitedBarrelGuiStateResult> results = runOnServer(
					player -> runDepositLimitedBarrelGuiStateRegression(player, name, depositCount, targetFreeSpace, locked, inventoryFilter));
			boolean passed = results.stream().allMatch(DepositLimitedBarrelGuiStateResult::passed);

			StringBuilder json = new StringBuilder("{\"ok\":").append(passed).append(',').append(jsonProperty("name", name)).append(",\"depositCount\":")
					.append(depositCount).append(",\"targetFreeSpace\":").append(targetFreeSpace).append(",\"locked\":").append(locked)
					.append(",\"inventoryFilter\":").append(inventoryFilter).append(",\"results\":[");
			for (int i = 0; i < results.size(); i++) {
				if (i > 0) {
					json.append(',');
				}
				DepositLimitedBarrelGuiStateResult result = results.get(i);
				json.append('{').append(jsonProperty("scenario", result.scenario())).append(",\"passed\":").append(result.passed())
						.append(",\"backpackOpened\":").append(result.backpackOpened()).append(",\"barrelOpened\":").append(result.barrelOpened())
						.append(",\"handled\":").append(result.handled()).append(",\"slotLimit\":").append(result.slotLimit()).append(",\"backpackBefore\":")
						.append(result.backpackBefore()).append(",\"backpackAfter\":").append(result.backpackAfter()).append(",\"barrelBefore\":")
						.append(result.barrelBefore()).append(",\"barrelAfter\":").append(result.barrelAfter()).append(",\"totalBefore\":")
						.append(result.totalBefore()).append(",\"totalAfter\":").append(result.totalAfter()).append(',')
						.append(jsonProperty("error", result.error())).append('}');
			}
			json.append("]}");
			return json.toString();
		}

		private List<DepositLimitedBarrelGuiStateResult> runDepositLimitedBarrelGuiStateRegression(ServerPlayer player, String name, int depositCount,
				int targetFreeSpace, boolean locked, boolean inventoryFilter) {
			player.getInventory().clearContent();
			ServerLevel level = (ServerLevel) player.level();
			BlockPos basePos = player.blockPosition().offset(3, 0, 0);
			List<DepositLimitedBarrelGuiStateScenario> scenarios = List.of(new DepositLimitedBarrelGuiStateScenario("neitherOpened", false, false),
					new DepositLimitedBarrelGuiStateScenario("backpackOpenedOnly", true, false),
					new DepositLimitedBarrelGuiStateScenario("barrelOpenedOnly", false, true),
					new DepositLimitedBarrelGuiStateScenario("bothOpened", true, true));

			List<DepositLimitedBarrelGuiStateResult> results = new ArrayList<>();
			for (int i = 0; i < scenarios.size(); i++) {
				results.add(runDepositLimitedBarrelGuiStateScenario(player, name, scenarios.get(i), level, basePos.offset(i * 2, 0, 0), depositCount,
						targetFreeSpace, locked, inventoryFilter));
			}
			return results;
		}

		private DepositLimitedBarrelGuiStateResult runDepositLimitedBarrelGuiStateScenario(ServerPlayer player, String name,
				DepositLimitedBarrelGuiStateScenario scenario, ServerLevel level, BlockPos pos, int depositCount, int targetFreeSpace, boolean locked,
				boolean inventoryFilter) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			placeBlockWithItem(level, player, pos, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_1_ITEM.get()));
			StorageBlockEntity storage = level.getBlockEntity(pos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get())
					.map(be -> (StorageBlockEntity) be).orElseThrow(() -> new IllegalStateException("Missing limited barrel storage at " + pos));

			InventoryHandler barrelInventory = storage.getStorageWrapper().getInventoryHandler();
			int slotLimit = barrelInventory.getInternalSlotLimit(0);
			int barrelStartCount = Math.max(0, slotLimit - targetFreeSpace);
			barrelInventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, barrelStartCount));
			barrelInventory.saveInventory();
			if (locked && !storage.isLocked()) {
				storage.toggleLock();
			}

			ItemStack backpack = createBackpackStack(9);
			IBackpackWrapper wrapper = BackpackWrapper.fromStackNoCache(backpack);
			wrapper.setSlotNumbers(9, 5);
			InventoryHandler backpackInventory = wrapper.getInventoryHandler();
			ItemStack depositUpgrade = new ItemStack(ModItems.DEPOSIT_UPGRADE.get());
			depositUpgrade.set(net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents.FILTER_BY_INVENTORY, inventoryFilter);
			wrapper.getUpgradeHandler().setStackInSlot(0, depositUpgrade);
			wrapper.getUpgradeHandler().saveInventory();
			backpackInventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, depositCount));
			backpackInventory.saveInventory();
			player.getInventory().setItem(0, backpack);
			player.getInventory().setChanged();

			if (scenario.backpackOpened()) {
				BackpackContainer container = new BackpackContainer(0, player, new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0));
				container.removed(player);
				backpack = player.getInventory().getItem(0);
			}

			if (scenario.barrelOpened()) {
				net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu container = new net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu(
						0, player, pos);
				container.removed(player);
			}

			int backpackBefore = countItems(BackpackWrapper.fromStackNoCache(backpack).getInventoryHandler(), Items.DIAMOND);
			int barrelBefore = countItems(barrelInventory, Items.DIAMOND);
			boolean handled = InventoryInteractionHelper.tryInventoryInteraction(pos, level, backpack, Direction.NORTH, player);
			int backpackAfter = countItems(BackpackWrapper.fromStackNoCache(player.getInventory().getItem(0)).getInventoryHandler(), Items.DIAMOND);
			int barrelAfter = countItems(barrelInventory, Items.DIAMOND);
			int totalBefore = backpackBefore + barrelBefore;
			int totalAfter = backpackAfter + barrelAfter;
			boolean passed = handled && totalBefore == totalAfter;
			String error = passed ? null : "Deposit interaction did not preserve item count";

			return new DepositLimitedBarrelGuiStateResult(name + ":" + scenario.name(), scenario.backpackOpened(), scenario.barrelOpened(), handled, slotLimit,
					backpackBefore, backpackAfter, barrelBefore, barrelAfter, totalBefore, totalAfter, passed, error);
		}

		private String runSubMobCatcherImmediateOpenRegression(String name) {
			try {
				resetBackpackGuiRegressionState();
				runRegressionStep("setup parent mob-catcher backpack on server", () -> runOnServer(this::setupParentMobCatcherBackpackRegression));
				runOnClient(this::setupClientParentMobCatcherBackpackRegression);
				runRegressionStep("open parent mob-catcher backpack on server", () -> runOnServer(this::openParentMobCatcherBackpackRegression));
				waitForOpenParentBackpackMenu();

				runRegressionStep("insert mob-catcher sub backpack on server", () -> runOnServer(this::insertMobCatcherSubBackpackIntoOpenParent));
				runOnClient(this::insertClientMobCatcherSubBackpackIntoOpenParent);
				SubMobCatcherRegressionState parentState = waitForParentMobCatcherRegressionState();
				if (!parentState.parentMatches()) {
					return subMobCatcherRegressionJson(name, false, parentState, parentState,
							"Parent backpack mob catcher data did not stay separate after inserting sub backpack");
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

		private void runRegressionStep(String description, Runnable step) {
			try {
				step.run();
			} catch (RuntimeException e) {
				throw new IllegalStateException("Failed to " + description + ": " + e.getMessage(), e);
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
				if (runOnClient(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
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
					return backpack.getItem() instanceof BackpackItem
							&& BackpackWrapper.fromStackNoCache(backpack).getUpgradeHandler().getStackInSlot(0).is(ModItems.CRAFTING_UPGRADE.get());
				})) {
					return;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);

			throw new IllegalStateException("Timed out waiting for client inventory slot 0 to contain crafting upgrade backpack");
		}

		private ItemStack createCraftingTransferRegressionBackpack() {
			ItemStack backpack = createBackpackStack(80);
			backpack.set(ModCoreDataComponents.STORAGE_UUID, CRAFTING_TRANSFER_BACKPACK_UUID);
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
			backpackWrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
			backpackWrapper.getUpgradeHandler().saveInventory();
			backpackWrapper.onContentsUpdated();
			return backpack;
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

		private Boolean openParentMobCatcherBackpackRegression(ServerPlayer player) {
			BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Mob Catcher Parent Regression")), backpackContext::toBuffer);
			if (player.containerMenu instanceof BackpackContainer menu) {
				menu.syncClientStorageContentsToClient();
			}
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
			menu.syncClientStorageContentsToClient();
			menu.broadcastChanges();
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
					new CapturedMob(mobId, ResourceLocation.parse("minecraft:pig"), new CompoundTag(), mobSlot, 1, 1, 1, false, displayName, 10, 10));
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
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
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
			throw new IllegalStateException("Timed out waiting for sub backpack screen to open");
		}

		private SubMobCatcherRegressionState waitForParentMobCatcherRegressionState() {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			SubMobCatcherRegressionState state;
			do {
				state = runOnClient(this::getCurrentMobCatcherRegressionStateSafely);
				if (state.parentMatches()) {
					return state;
				}
				sleep(50);
			} while (System.nanoTime() < deadline);
			return state;
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
			if (!(Minecraft.getInstance().screen instanceof BackpackScreen)
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
			Map<String, String> beforeCapturedMobs = snapshotCapturedMobs(wrapper);
			Map<Integer, String> beforeProtectedStacks = snapshotProtectedStacks(inventory, scenario.protectedSlots());
			Map<Integer, String> beforeStableStacks = snapshotProtectedStacks(inventory, scenario.stableSlots());
			Map<String, String> beforeProtectedSettings = snapshotProtectedSettings(wrapper, scenario);
			ColumnUpgradeSimulationResult simulationResult = simulateColumnUpgradeOperation(backpack, wrapper, scenario.upgradeItem(), scenario.operation());
			wrapper = BackpackWrapper.fromStackNoCache(backpack);
			wrapper.setSlotNumbers(scenario.inventorySlots(), 5);
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
						new CapturedMob(new UUID(0, i + 1), ResourceLocation.parse(capturedMob.entityType()), new CompoundTag(), capturedMob.slot(),
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
			ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
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

		private String setupInceptionMagnetPersistence(ServerPlayer player) {
			player.closeContainer();
			player.getInventory().clearContent();

			ItemStack mainBackpack = createBackpackStack(80);
			mainBackpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
			IBackpackWrapper mainWrapper = BackpackWrapper.fromStack(mainBackpack);
			ItemStack inceptionUpgrade = new ItemStack(ModItems.INCEPTION_UPGRADE.get());
			inceptionUpgrade.set(net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents.INVENTORY_ORDER, InventoryOrder.INCEPTED_FIRST);
			UpgradeHandler upgrades = mainWrapper.getUpgradeHandler();
			upgrades.setStackInSlot(0, inceptionUpgrade);
			upgrades.setStackInSlot(1, new ItemStack(ModItems.MAGNET_UPGRADE.get()));
			upgrades.getWrappersThatImplement(MagnetUpgradeWrapper.class).forEach(magnet -> {
				magnet.getFilterLogic().setDepositFilterType(ContentsFilterType.BLOCK);
				magnet.setPickupItems(true);
			});
			upgrades.saveInventory();

			ItemStack nestedBackpack = new ItemStack(ModItems.BACKPACK.get());
			mainWrapper.getInventoryHandler().setStackInSlot(0, nestedBackpack);
			mainWrapper.getInventoryHandler().saveInventory();
			player.getInventory().setItem(0, mainBackpack);
			player.getInventory().setSelectedSlot(0);
			player.getInventory().setChanged();

			BackpackItem backpackItem = (BackpackItem) mainBackpack.getItem();
			backpackItem.use(player.level(), player, InteractionHand.MAIN_HAND);
			return "{\"ok\":true,\"nestedHasUuid\":" + (nestedBackpack.get(ModCoreDataComponents.STORAGE_UUID) != null) + "}";
		}

		private String pickupWithInceptionMagnet(ServerPlayer player) {
			ServerLevel level = (ServerLevel) player.level();
			ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 0.5D, player.getZ(), new ItemStack(Items.DIAMOND));
			itemEntity.setPickUpDelay(0);
			level.addFreshEntity(itemEntity);
			itemEntity.playerTouch(player);
			player.closeContainer();

			ItemStack nestedBackpack = BackpackWrapper.fromStack(player.getInventory().getItem(0)).getInventoryHandler().getStackInSlot(0);
			UUID nestedUuid = nestedBackpack.get(ModCoreDataComponents.STORAGE_UUID);
			IBackpackWrapper nestedWrapper = nestedUuid == null ? null : BackpackWrapper.fromStack(nestedBackpack);
			int nestedDiamonds = nestedWrapper == null ? 0 : countItems(nestedWrapper.getInventoryHandler(), Items.DIAMOND);
			return "{\"ok\":" + (nestedUuid != null && nestedDiamonds == 1) + ",\"nestedHasUuid\":" + (nestedUuid != null) + ",\"nestedDiamonds\":"
					+ nestedDiamonds + "," + jsonProperty("nestedUuid", nestedUuid == null ? null : nestedUuid.toString()) + "}";
		}

		private String inceptionMagnetPersistenceStatus(ServerPlayer player) {
			ItemStack mainBackpack = player.getInventory().getItem(0);
			if (!(mainBackpack.getItem() instanceof BackpackItem)) {
				return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
			}

			ItemStack nestedBackpack = BackpackWrapper.fromStack(mainBackpack).getInventoryHandler().getStackInSlot(0);
			if (!(nestedBackpack.getItem() instanceof BackpackItem)) {
				return "{\"ok\":false,\"error\":\"No nested backpack in slot 0\"}";
			}

			UUID nestedUuid = nestedBackpack.get(ModCoreDataComponents.STORAGE_UUID);
			if (nestedUuid == null) {
				return "{\"ok\":false,\"nestedHasUuid\":false,\"nestedDiamonds\":0,\"error\":\"Nested backpack UUID was not persisted\"}";
			}

			int nestedDiamonds = countItems(BackpackWrapper.fromStack(nestedBackpack).getInventoryHandler(), Items.DIAMOND);
			return "{\"ok\":" + (nestedDiamonds == 1) + ",\"nestedHasUuid\":true,\"nestedDiamonds\":" + nestedDiamonds + ","
					+ jsonProperty("nestedUuid", nestedUuid.toString()) + "}";
		}

		private static class CreateIssue23Automation {
			private static Issue23SetupResult setupStorageIssue23Reproduction(AutomationServer server, ServerPlayer player) {
				ServerLevel level = (ServerLevel) player.level();
				BlockPos motorPos = player.blockPosition().offset(10, 0, 0);
				BlockPos bearingPos = motorPos.above();
				BlockPos rootPlankPos = bearingPos.above();
				BlockPos leftChestPos = rootPlankPos.above();
				BlockPos rightChestPos = leftChestPos.east();
				BlockPos movingPsiPos = rightChestPos.east();
				clearIssue23ReproductionArea(level, motorPos);
				for (int x = -5; x <= 12; x++) {
					for (int z = -5; z <= 5; z++) {
						level.setBlock(motorPos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
					}
				}

				level.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, Direction.UP), 3);
				level.setBlock(bearingPos, AllBlocks.MECHANICAL_BEARING.getDefaultState().setValue(BlockStateProperties.FACING, Direction.UP), 3);
				for (int x = 0; x < 3; x++) {
					level.setBlock(rootPlankPos.east(x), Blocks.OAK_PLANKS.defaultBlockState(), 3);
				}
				level.setBlock(leftChestPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get().defaultBlockState()
						.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT), 3);
				level.setBlock(rightChestPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get().defaultBlockState()
						.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT), 3);
				StorageBlockEntity storage = WorldHelper.getBlockEntity(level, rightChestPos, StorageBlockEntity.class)
						.orElseThrow(() -> new IllegalStateException("Issue #23 double-chest source block entity missing"));
				storage.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
				storage.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.COBBLESTONE, 32));
				storage.setChanged();
				level.setBlock(movingPsiPos,
						AllBlocks.PORTABLE_STORAGE_INTERFACE.getDefaultState().setValue(PortableStorageInterfaceBlock.FACING, Direction.EAST), 3);
				level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(rootPlankPos, rootPlankPos.east(2))));
				level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(rootPlankPos, leftChestPos)));
				level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(leftChestPos, rightChestPos)));
				level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(rightChestPos, movingPsiPos)));

				MechanicalBearingBlockEntity bearing = WorldHelper.getBlockEntity(level, bearingPos, MechanicalBearingBlockEntity.class)
						.orElseThrow(() -> new IllegalStateException("Issue #23 mechanical bearing block entity missing"));
				bearing.assemble();
				ControlledContraptionEntity contraptionEntity = Optional.ofNullable(bearing.getMovedContraption())
						.orElseThrow(() -> new IllegalStateException("Issue #23 mechanical bearing did not assemble the contraption"));
				BlockPos mountedStoragePos = CreateItemDisplayPreviewAutomation.findMountedDoubleChestLocalPos(contraptionEntity);
				BlockPos movingPsiLocalPos = mountedStoragePos.east();
				Vec3 movingPsiConnectionPoint = contraptionEntity
						.toGlobalVector(Vec3.atCenterOf(movingPsiLocalPos).add(Vec3.atLowerCornerOf(Direction.EAST.getUnitVec3i()).scale(1.85F)), 1);
				BlockPos stationaryPsiPos = BlockPos.containing(movingPsiConnectionPoint);
				Direction movingPsiFacing = Direction
						.getApproximateNearest(contraptionEntity.applyRotation(Vec3.atLowerCornerOf(Direction.EAST.getUnitVec3i()), 1));
				BlockPos chutePos = stationaryPsiPos.below();
				BlockPos receiverPos = chutePos.below();
				level.setBlock(stationaryPsiPos,
						AllBlocks.PORTABLE_STORAGE_INTERFACE.getDefaultState().setValue(PortableStorageInterfaceBlock.FACING, movingPsiFacing.getOpposite()),
						3);
				level.setBlock(chutePos, AllBlocks.SMART_CHUTE.getDefaultState().setValue(SmartChuteBlock.POWERED, false), 3);
				SmartChuteBlockEntity chute = WorldHelper.getBlockEntity(level, chutePos, SmartChuteBlockEntity.class)
						.orElseThrow(() -> new IllegalStateException("Issue #23 Smart Chute block entity missing"));
				FilteringBehaviour filtering = chute.getBehaviour(FilteringBehaviour.TYPE);
				filtering.count = 64;
				filtering.upTo = false;
				chute.setChanged();
				level.setBlock(receiverPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get().defaultBlockState()
						.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.SINGLE), 3);
				server.setPreviewWoodType(level, receiverPos, WoodType.ACACIA);

				return new Issue23SetupResult(contraptionEntity.getId(), mountedStoragePos, receiverPos);
			}

			private static void clearIssue23ReproductionArea(ServerLevel level, BlockPos motorPos) {
				level.getEntitiesOfClass(Entity.class, new AABB(motorPos).inflate(10), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
				for (int x = -4; x <= 10; x++) {
					for (int y = -2; y <= 4; y++) {
						for (int z = -3; z <= 3; z++) {
							level.setBlock(motorPos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
						}
					}
				}
			}

			private static String issue23ReproductionResultJson(AutomationServer server, ServerPlayer player, Issue23SetupResult setupResult) {
				Entity entity = ((ServerLevel) player.level()).getEntity(setupResult.contraptionEntityId());
				if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
					throw new IllegalStateException("Issue #23 contraption is no longer present");
				}
				MountedStorageBase mountedStorage = ContraptionHelper.getMountedStorage(contraptionEntity, setupResult.mountedStoragePos());
				if (mountedStorage == null) {
					throw new IllegalStateException("Issue #23 mounted storage is no longer present");
				}
				int sourceSlot0 = countInMountedStorageSlot(mountedStorage, 0, Items.COBBLESTONE);
				int sourceSlot1 = countInMountedStorageSlot(mountedStorage, 1, Items.COBBLESTONE);
				int receiver = countItemsInStorage(server, (ServerLevel) player.level(), setupResult.receiverPos(), Items.COBBLESTONE);
				return "{\"ok\":true,\"sourceSlot0\":" + sourceSlot0 + ",\"sourceSlot1\":" + sourceSlot1 + ",\"sourceTotal\":" + (sourceSlot0 + sourceSlot1)
						+ ",\"receiver\":" + receiver + ',' + jsonProperty("mountedStoragePos", setupResult.mountedStoragePos().toShortString()) + ','
						+ jsonProperty("receiverPos", setupResult.receiverPos().toShortString()) + ",\"contraptionEntityId\":"
						+ setupResult.contraptionEntityId() + '}';
			}

			private static int countInMountedStorageSlot(MountedStorageBase mountedStorage, int slot, Item item) {
				ItemStack stack = mountedStorage.getStackInSlot(slot);
				return stack.is(item) ? stack.getCount() : 0;
			}

			private static int countItemsInStorage(AutomationServer server, ServerLevel level, BlockPos pos, Item item) {
				StorageBlockEntity storage = WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class)
						.orElseThrow(() -> new IllegalStateException("Issue #23 receiver storage block entity missing"));
				return server.countItems(storage.getStorageWrapper().getInventoryHandler(), item);
			}
		}

		private String runInventoryInteractionsKeybindRegression() {
			InventoryInteractionKeyMappings originalMappings = runOnClient(this::configureInventoryInteractionKeyMappings);
			List<String> cases = new ArrayList<>();
			try {
				runVanillaTransferKeybindRegression(true, false);
				cases.add("vanillaTransferToStorageFiltered");
				runVanillaTransferKeybindRegression(true, true);
				cases.add("vanillaTransferToStorageAll");
				runVanillaTransferKeybindRegression(false, false);
				cases.add("vanillaTransferToPlayerFiltered");
				runVanillaTransferKeybindRegression(false, true);
				cases.add("vanillaTransferToPlayerAll");
				runBackpackTransferKeybindRegression(true, false);
				cases.add("backpackTransferToStorageFiltered");
				runBackpackTransferKeybindRegression(true, true);
				cases.add("backpackTransferToStorageAll");
				runBackpackTransferKeybindRegression(false, false);
				cases.add("backpackTransferToPlayerFiltered");
				runBackpackTransferKeybindRegression(false, true);
				cases.add("backpackTransferToPlayerAll");
				runVanillaSortKeybindRegression();
				cases.add("vanillaSort");
				runPlayerInventorySortKeybindRegression();
				cases.add("playerInventorySort");
				runCraftingPlayerInventorySortKeybindRegression();
				cases.add("craftingPlayerInventorySort");
				runFurnacePlayerInventorySortKeybindRegression();
				cases.add("furnacePlayerInventorySort");
				runBackpackSortKeybindRegression();
				cases.add("backpackSort");
				return "{\"ok\":true,\"cases\":[\"" + String.join("\",\"", cases) + "\"]}";
			} finally {
				runOnClient(() -> {
					restoreInventoryInteractionKeyMappings(originalMappings);
					return null;
				});
				runOnServer(player -> {
					player.closeContainer();
					return null;
				});
			}
		}

		private InventoryInteractionKeyMappings configureInventoryInteractionKeyMappings() {
			KeyMapping sortKeybind = ClientEventHandler.SORT_KEYBIND;
			KeyMapping transferToStorageKeybind = ClientEventHandler.TRANSFER_TO_STORAGE_KEYBIND;
			KeyMapping transferToInventoryKeybind = ClientEventHandler.TRANSFER_TO_INVENTORY_KEYBIND;
			InventoryInteractionKeyMappings originalMappings = new InventoryInteractionKeyMappings(sortKeybind.getKey(), transferToStorageKeybind.getKey(),
					transferToInventoryKeybind.getKey());
			sortKeybind.setKey(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_MIDDLE));
			transferToStorageKeybind.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET));
			transferToInventoryKeybind.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET));
			KeyMapping.resetMapping();
			return originalMappings;
		}

		private void restoreInventoryInteractionKeyMappings(InventoryInteractionKeyMappings originalMappings) {
			ClientEventHandler.SORT_KEYBIND.setKey(originalMappings.sort());
			ClientEventHandler.TRANSFER_TO_STORAGE_KEYBIND.setKey(originalMappings.transferToStorage());
			ClientEventHandler.TRANSFER_TO_INVENTORY_KEYBIND.setKey(originalMappings.transferToInventory());
			KeyMapping.resetMapping();
		}

		private void runVanillaTransferKeybindRegression(boolean toStorage, boolean shift) {
			int containerId = runOnServer(player -> {
				prepareVanillaTransferKeybindRegression(player, toStorage);
				return player.containerMenu.containerId;
			});
			waitForClientScreen("vanilla chest", () -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof ChestMenu && screen.getMenu().containerId == containerId);
			requireHandled(pressTransferKeybind(toStorage, shift), "Vanilla transfer keybind was not handled");
			waitForServerCondition("vanilla transfer", player -> vanillaTransferMatches(player, toStorage, shift));
		}

		private void prepareVanillaTransferKeybindRegression(ServerPlayer player, boolean toStorage) {
			player.closeContainer();
			player.getInventory().clearContent();
			SimpleContainer container = new SimpleContainer(27);
			container.setItem(0, new ItemStack(Items.COBBLESTONE));
			if (toStorage) {
				player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
				player.getInventory().setItem(10, new ItemStack(Items.DIRT));
			} else {
				container.setItem(1, new ItemStack(Items.DIRT));
				player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
			}
			player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> ChestMenu.threeRows(windowId, inventory, container),
					Component.literal("Inventory interaction regression")));
		}

		private boolean vanillaTransferMatches(ServerPlayer player, boolean toStorage, boolean shift) {
			if (!(player.containerMenu instanceof ChestMenu menu)) {
				return false;
			}
			SimpleContainer container = (SimpleContainer) menu.getContainer();
			return transferMatches(countItems(container, Items.COBBLESTONE), countItems(container, Items.DIRT), countItems(player, Items.COBBLESTONE),
					countItems(player, Items.DIRT), toStorage, shift);
		}

		private void runBackpackTransferKeybindRegression(boolean toStorage, boolean shift) {
			runOnServer(player -> {
				prepareBackpackTransferKeybindRegression(player, toStorage);
				return null;
			});
			waitForClientBackpackInHotbar();
			int containerId = runOnServer(this::openMainBackpackForInventoryInteractionRegression);
			waitForClientScreen("backpack",
					() -> Minecraft.getInstance().screen instanceof BackpackScreen screen && screen.getMenu().containerId == containerId);
			requireHandled(pressTransferKeybind(toStorage, shift), "Backpack transfer keybind was not handled");
			try {
				waitForServerCondition("backpack transfer", player -> backpackTransferMatches(player, toStorage, shift));
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage() + ": " + runOnServer(this::backpackTransferState), e);
			}
		}

		private void prepareBackpackTransferKeybindRegression(ServerPlayer player, boolean toStorage) {
			player.closeContainer();
			player.getInventory().clearContent();
			ItemStack backpack = createBackpackStack(27);
			InventoryHandler inventory = BackpackWrapper.fromStackNoCache(backpack).getInventoryHandler();
			inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
			if (!toStorage) {
				inventory.setStackInSlot(1, new ItemStack(Items.DIRT));
			}
			inventory.saveInventory();
			player.getInventory().setItem(0, backpack);
			player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
			if (toStorage) {
				player.getInventory().setItem(10, new ItemStack(Items.DIRT));
			}
			player.getInventory().setChanged();
		}

		private int openMainBackpackForInventoryInteractionRegression(ServerPlayer player) {
			BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Inventory interaction regression")), backpackContext::toBuffer);
			return player.containerMenu.containerId;
		}

		private boolean backpackTransferMatches(ServerPlayer player, boolean toStorage, boolean shift) {
			InventoryHandler inventory = BackpackWrapper.fromStackNoCache(player.getInventory().getItem(0)).getInventoryHandler();
			return transferMatches(countItems(inventory, Items.COBBLESTONE), countItems(inventory, Items.DIRT), countItems(player, Items.COBBLESTONE),
					countItems(player, Items.DIRT), toStorage, shift);
		}

		private String backpackTransferState(ServerPlayer player) {
			InventoryHandler inventory = BackpackWrapper.fromStackNoCache(player.getInventory().getItem(0)).getInventoryHandler();
			return "storage cobblestone=" + countItems(inventory, Items.COBBLESTONE) + ", dirt=" + countItems(inventory, Items.DIRT) + "; player cobblestone="
					+ countItems(player, Items.COBBLESTONE) + ", dirt=" + countItems(player, Items.DIRT);
		}

		private boolean transferMatches(int containerCobblestone, int containerDirt, int playerCobblestone, int playerDirt, boolean toStorage, boolean shift) {
			if (toStorage) {
				return containerCobblestone == 2 && containerDirt == (shift ? 1 : 0) && playerCobblestone == 0 && playerDirt == (shift ? 0 : 1);
			}
			return containerCobblestone == 0 && containerDirt == (shift ? 0 : 1) && playerCobblestone == 2 && playerDirt == (shift ? 1 : 0);
		}

		private void runVanillaSortKeybindRegression() {
			int containerId = runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				SimpleContainer container = new SimpleContainer(27);
				container.setItem(0, new ItemStack(Items.COBBLESTONE));
				container.setItem(5, new ItemStack(Items.COBBLESTONE, 2));
				player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> ChestMenu.threeRows(windowId, inventory, container),
						Component.literal("Inventory interaction regression")));
				return player.containerMenu.containerId;
			});
			waitForClientScreen("vanilla chest", () -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof ChestMenu && screen.getMenu().containerId == containerId);
			requireHandled(pressSortKeybind(0), "Vanilla sort keybind was not handled");
			try {
				waitForServerCondition("vanilla sort", player -> {
					if (!(player.containerMenu instanceof ChestMenu menu)) {
						return false;
					}
					SimpleContainer container = (SimpleContainer) menu.getContainer();
					return countItems(container, Items.COBBLESTONE) == 3 && countStacks(container, Items.COBBLESTONE) == 1;
				});
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage() + ": " + runOnServer(this::vanillaSortState), e);
			}
		}

		private String vanillaSortState(ServerPlayer player) {
			if (!(player.containerMenu instanceof ChestMenu menu)) {
				return "no chest menu";
			}
			SimpleContainer container = (SimpleContainer) menu.getContainer();
			return "cobblestone=" + countItems(container, Items.COBBLESTONE) + ", stacks=" + countStacks(container, Items.COBBLESTONE);
		}

		private void runPlayerInventorySortKeybindRegression() {
			runOnServer(player -> {
				preparePlayerInventorySortKeybindRegression(player);
				return null;
			});
			boolean handled = runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null) {
					throw new IllegalStateException("Client player is not loaded");
				}
				InventoryScreen screen = new InventoryScreen(minecraft.player);
				minecraft.setScreen(screen);
				return postMouseButtonPressed(screen, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
			});
			requireHandled(handled, "Player inventory sort keybind was not handled");
			waitForPlayerInventorySort("player inventory sort");
		}

		private void runCraftingPlayerInventorySortKeybindRegression() {
			int containerId = runOnServer(player -> {
				preparePlayerInventorySortKeybindRegression(player);
				player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new CraftingMenu(windowId, inventory),
						Component.literal("Inventory interaction regression")));
				return player.containerMenu.containerId;
			});
			waitForClientScreen("prepared crafting table", () -> Minecraft.getInstance().screen instanceof CraftingScreen screen
					&& screen.getMenu().containerId == containerId && screen.getMenu().slots.get(10).getItem().is(Items.COBBLESTONE));
			requireHandled(pressSortKeybind(10), "Crafting-table player inventory sort keybind was not handled");
			try {
				waitForPlayerInventorySort("crafting-table player inventory sort");
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage() + ": " + runOnServer(player -> "cobblestone=" + countItems(player, Items.COBBLESTONE)
						+ ", stacks=" + countStacks(player, Items.COBBLESTONE) + ", menu=" + player.containerMenu.getClass().getSimpleName()), e);
			}
		}

		private void runFurnacePlayerInventorySortKeybindRegression() {
			int containerId = runOnServer(player -> {
				preparePlayerInventorySortKeybindRegression(player);
				player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new FurnaceMenu(windowId, inventory),
						Component.literal("Inventory interaction regression")));
				return player.containerMenu.containerId;
			});
			waitForClientScreen("furnace", () -> Minecraft.getInstance().screen instanceof FurnaceScreen screen && screen.getMenu().containerId == containerId);
			requireHandled(pressSortKeybind(3), "Furnace player inventory sort keybind was not handled");
			waitForPlayerInventorySort("furnace player inventory sort");
		}

		private void preparePlayerInventorySortKeybindRegression(ServerPlayer player) {
			player.closeContainer();
			player.getInventory().clearContent();
			player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE));
			player.getInventory().setItem(10, new ItemStack(Items.COBBLESTONE, 2));
			player.getInventory().setChanged();
		}

		private void waitForPlayerInventorySort(String description) {
			waitForServerCondition(description, player -> countItems(player, Items.COBBLESTONE) == 3 && countStacks(player, Items.COBBLESTONE) == 1);
		}

		private void runBackpackSortKeybindRegression() {
			runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				ItemStack backpack = createBackpackStack(27);
				InventoryHandler inventory = BackpackWrapper.fromStackNoCache(backpack).getInventoryHandler();
				inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
				inventory.setStackInSlot(5, new ItemStack(Items.COBBLESTONE, 2));
				inventory.saveInventory();
				player.getInventory().setItem(0, backpack);
				player.getInventory().setChanged();
				return null;
			});
			waitForClientBackpackInHotbar();
			int containerId = runOnServer(this::openMainBackpackForInventoryInteractionRegression);
			waitForClientScreen("backpack",
					() -> Minecraft.getInstance().screen instanceof BackpackScreen screen && screen.getMenu().containerId == containerId);
			requireHandled(pressSortKeybind(0), "Backpack sort keybind was not handled");
			waitForServerCondition("backpack sort", player -> {
				InventoryHandler inventory = BackpackWrapper.fromStackNoCache(player.getInventory().getItem(0)).getInventoryHandler();
				return countItems(inventory, Items.COBBLESTONE) == 3 && countStacks(inventory, Items.COBBLESTONE) == 1;
			});
		}

		private boolean pressTransferKeybind(boolean toStorage, boolean shift) {
			int keyCode = toStorage ? GLFW.GLFW_KEY_LEFT_BRACKET : GLFW.GLFW_KEY_RIGHT_BRACKET;
			return postKeyPressed(keyCode, shift ? GLFW.GLFW_MOD_SHIFT : 0);
		}

		private boolean postKeyPressed(int keyCode, int modifiers) {
			return runOnClient(() -> {
				Screen screen = Minecraft.getInstance().screen;
				if (screen == null) {
					throw new IllegalStateException("No screen is open for the transfer keybind");
				}
				ScreenEvent.KeyPressed.Pre event = new ScreenEvent.KeyPressed.Pre(screen, new KeyEvent(keyCode, 0, modifiers));
				NeoForge.EVENT_BUS.post(event);
				return event.isCanceled();
			});
		}

		private boolean pressSortKeybind(int menuSlot) {
			return runOnClient(() -> {
				if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> containerScreen)) {
					throw new IllegalStateException("No container screen is open for the sort keybind");
				}
				if (menuSlot >= 0) {
					Slot slot = containerScreen.getMenu().slots.get(menuSlot);
					double x = containerScreen.getGuiLeft() + slot.x + 8.0;
					double y = containerScreen.getGuiTop() + slot.y + 8.0;
					double scale = Minecraft.getInstance().getWindow().getGuiScale();
					GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().handle(), x * scale, y * scale);
					containerScreen.mouseMoved(x, y);
					setHoveredSlot(containerScreen, menuSlot);
				}
				return postMouseButtonPressed(containerScreen, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
			});
		}

		private void setHoveredSlot(AbstractContainerScreen<?> screen, int menuSlot) {
			try {
				Field hoveredSlotField = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
				hoveredSlotField.setAccessible(true);
				hoveredSlotField.set(screen, screen.getMenu().slots.get(menuSlot));
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Failed to set the hovered container slot", e);
			}
		}

		private boolean postMouseButtonPressed(Screen screen, int button) {
			ScreenEvent.MouseButtonPressed.Pre event = new ScreenEvent.MouseButtonPressed.Pre(screen,
					new MouseButtonEvent(0, 0, new MouseButtonInfo(button, 0)), false);
			NeoForge.EVENT_BUS.post(event);
			return event.isCanceled();
		}

		private void waitForClientScreen(String description, BooleanSupplier condition) {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (System.nanoTime() < deadline) {
				if (runOnClient(condition::getAsBoolean)) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Timed out waiting for " + description);
		}

		private void waitForServerCondition(String description, Function<ServerPlayer, Boolean> condition) {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (System.nanoTime() < deadline) {
				if (runOnServer(condition)) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Timed out waiting for " + description);
		}

		private void requireHandled(boolean handled, String message) {
			if (!handled) {
				throw new IllegalStateException(message);
			}
		}

		private int countItems(SimpleContainer container, Item item) {
			int count = 0;
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.is(item)) {
					count += stack.getCount();
				}
			}
			return count;
		}

		private int countItems(ServerPlayer player, Item item) {
			return player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
		}

		private int countStacks(SimpleContainer container, Item item) {
			int stacks = 0;
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				if (container.getItem(slot).is(item)) {
					stacks++;
				}
			}
			return stacks;
		}

		private int countStacks(ServerPlayer player, Item item) {
			return (int) player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(item)).count();
		}

		private int countStacks(InventoryHandler inventory, Item item) {
			int stacks = 0;
			for (int slot = 0; slot < inventory.size(); slot++) {
				if (inventory.getStackInSlot(slot).is(item)) {
					stacks++;
				}
			}
			return stacks;
		}

		private String openStorageItemDisplayPreview(String scenario, DisplaySide displaySide) {
			ItemDisplayPreviewSetupResult setupResult = runOnServer(player -> setupStorageItemDisplayPreview(player, scenario, displaySide));
			if (setupResult.targetType() == ItemDisplayPreviewTargetType.PLACED_STORAGE) {
				waitForClientStorageBlockEntity(setupResult.menuPos(), setupResult.limitedBarrel());
				runOnServer(player -> openStorageInventory(player, setupResult.menuPos()));
			} else if (setupResult.targetType() == ItemDisplayPreviewTargetType.BACKPACK) {
				waitForClientBackpackInHotbar();
				runOnServer(this::openItemBackpackPreview);
			} else if (setupResult.targetType() == ItemDisplayPreviewTargetType.MOVING_STORAGE) {
				waitForClientEntity(setupResult.entityId());
				runOnServer(player -> openMovingStorageInventory(player, setupResult.entityId()));
			} else if (setupResult.targetType() == ItemDisplayPreviewTargetType.CREATE_CONTRAPTION) {
				waitForClientEntity(setupResult.entityId());
				runOnServer(player -> CreateItemDisplayPreviewAutomation.openCreateContraptionStorage(player, setupResult.entityId(), setupResult.localPos()));
			}
			waitForStorageScreen();
			waitForStorageScreenAndClickSettingsTab();
			String screenName = waitForSettingsScreenAndOpenItemDisplayTab();
			return "{\"ok\":true," + jsonProperty("scenario", setupResult.scenario()) + ',' + jsonProperty("displaySide", displaySide.getSerializedName()) + ','
					+ jsonProperty("menuPos", setupResult.menuPos() == null ? null : setupResult.menuPos().toShortString()) + ','
					+ jsonProperty("localPos", setupResult.localPos() == null ? null : setupResult.localPos().toShortString()) + ',' + "\"entityId\":"
					+ setupResult.entityId() + ',' + jsonProperty("target", setupResult.target()) + ',' + jsonProperty("screen", screenName) + '}';
		}

		private String openDecorationTableRenderPreview(String itemName) {
			DecorationTableRenderPreviewSetupResult setupResult = runOnServer(player -> setupDecorationTableRenderPreview(player, itemName));
			waitForClientDecorationTable(setupResult.tablePos(), setupResult.resultItem());
			runOnServer(player -> {
				openDecorationTableScreen(player, setupResult.tablePos());
				return "";
			});
			waitForClientDecorationTableScreen();
			return "{\"ok\":true," + jsonProperty("item", setupResult.itemName()) + ',' + jsonProperty("tablePos", setupResult.tablePos().toShortString()) + ','
					+ runOnClient(this::getDecorationTableRenderBoundsJson) + '}';
		}

		private String dragDecorationTableRenderPreview(int x, int y, int dragX, int dragY) {
			if (!(Minecraft.getInstance().screen instanceof DecorationTableScreen screen)) {
				throw new IllegalStateException("Decoration table screen is not open");
			}

			MouseButtonEvent start = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
			boolean clicked = screen.mouseClicked(start, false);
			boolean dragged = clicked && screen.mouseDragged(start, dragX, dragY);
			boolean released = screen.mouseReleased(start);
			return "{\"ok\":" + (clicked && dragged && released) + ",\"dragged\":" + dragged + ",\"x\":" + (x + dragX) + ",\"y\":" + (y + dragY) + '}';
		}

		private void openDecorationTableScreen(ServerPlayer player, BlockPos tablePos) {
			player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new DecorationTableMenu(windowId, menuPlayer, tablePos),
					net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DECORATION_TABLE.get().getName()), tablePos);
		}

		private DecorationTableRenderPreviewSetupResult setupDecorationTableRenderPreview(ServerPlayer player, String itemName) {
			ServerLevel level = (ServerLevel) player.level();
			BlockPos tablePos = player.blockPosition().offset(4, 0, 0);
			clearDecorationTableRenderPreviewArea(level, tablePos);
			player.getInventory().clearContent();
			player.getInventory().setSelectedSlot(0);
			player.inventoryMenu.broadcastChanges();
			level.setBlock(tablePos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.DECORATION_TABLE.get().defaultBlockState()
					.setValue(DecorationTableBlock.FACING, Direction.NORTH), 3);
			DecorationTableBlockEntity table = WorldHelper.getBlockEntity(level, tablePos, DecorationTableBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Decoration table block entity missing"));
			ItemStack resultItem = getDecorationTablePreviewItem(itemName);
			insertDecorationTableStack(table.getStorageBlock(), 0, resultItem);
			if (resultItem.getItem() instanceof BarrelBlockItem) {
				if (itemName.equalsIgnoreCase("barrel_directional") || itemName.equalsIgnoreCase("limited_barrel_3_directional")) {
					insertDecorationTableStack(table.getDecorativeBlocks(), DecorationTableBlockEntity.TOP_CORE_SLOT, new ItemStack(Blocks.JIGSAW));
					insertDecorationTableStack(table.getDecorativeBlocks(), DecorationTableBlockEntity.SIDE_CORE_SLOT, new ItemStack(Blocks.FURNACE));
					insertDecorationTableStack(table.getDecorativeBlocks(), DecorationTableBlockEntity.BOTTOM_CORE_SLOT, new ItemStack(Blocks.STRUCTURE_BLOCK));
				} else {
					insertDecorationTableStack(table.getDecorativeBlocks(), DecorationTableBlockEntity.TOP_CORE_SLOT, new ItemStack(Blocks.DIAMOND_BLOCK));
					insertDecorationTableStack(table.getDecorativeBlocks(), DecorationTableBlockEntity.SIDE_CORE_SLOT, new ItemStack(Blocks.GOLD_BLOCK));
					insertDecorationTableStack(table.getDecorativeBlocks(), DecorationTableBlockEntity.BOTTOM_CORE_SLOT, new ItemStack(Blocks.EMERALD_BLOCK));
				}
			} else if (resultItem.getItem() instanceof SimpleMaterialBlockItem
					|| resultItem.is(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_3_ITEM.get())) {
				insertDecorationTableStack(table.getDecorativeBlocks(), DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT, new ItemStack(Blocks.DIAMOND_BLOCK));
			} else {
				table.setMainColor(0xFFFF00FF);
			}

			BlockPos cameraPos = tablePos.south(3);
			if (!player.teleportTo(level, cameraPos.getX() + 0.5D, cameraPos.getY(), cameraPos.getZ() + 0.5D, Set.of(), 180.0F, 7.0F, false)) {
				throw new IllegalStateException("Failed to position player for decoration table render preview");
			}
			return new DecorationTableRenderPreviewSetupResult(itemName, tablePos, resultItem.getItem());
		}

		private void clearDecorationTableRenderPreviewArea(ServerLevel level, BlockPos tablePos) {
			level.getEntitiesOfClass(Entity.class, new AABB(tablePos).inflate(8), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
			for (int x = -4; x <= 4; x++) {
				for (int y = -1; y <= 4; y++) {
					for (int z = -4; z <= 4; z++) {
						level.setBlock(tablePos.offset(x, y, z), y == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
			level.getEntitiesOfClass(Entity.class, new AABB(tablePos).inflate(8), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
		}

		private ItemStack getDecorationTablePreviewItem(String itemName) {
			return switch (itemName.toLowerCase(Locale.ROOT)) {
				case "backpack" -> new ItemStack(ModItems.BACKPACK.get());
				case "barrel", "barrel_directional" -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_ITEM.get());
				case "chest" -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST_ITEM.get());
				case "controller" -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER_ITEM.get());
				case "leather_boots" -> new ItemStack(Items.LEATHER_BOOTS);
				case "leather_chestplate" -> new ItemStack(Items.LEATHER_CHESTPLATE);
				case "leather_helmet" -> new ItemStack(Items.LEATHER_HELMET);
				case "leather_leggings" -> new ItemStack(Items.LEATHER_LEGGINGS);
				case "limited_barrel_3", "limited_barrel_3_directional" ->
					new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_3_ITEM.get());
				case "shulker_box" -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.SHULKER_BOX_ITEM.get());
				case "storage_link" -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.STORAGE_LINK_ITEM.get());
				case "storage_io" -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.STORAGE_IO_ITEM.get());
				default -> throw new IllegalArgumentException("Unknown decoration table preview item " + itemName);
			};
		}

		private void insertDecorationTableStack(ItemStacksResourceHandler handler, int slot, ItemStack stack) {
			try (Transaction transaction = Transaction.openRoot()) {
				int inserted = handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
				if (inserted != stack.getCount()) {
					throw new IllegalStateException("Failed to insert decoration table preview stack " + stack);
				}
				transaction.commit();
			}
		}

		private ItemDisplayPreviewSetupResult setupStorageItemDisplayPreview(ServerPlayer player, String scenario, DisplaySide displaySide) {
			ServerLevel level = (ServerLevel) player.level();
			BlockPos basePos = player.blockPosition().offset(4, 0, 0);
			clearItemDisplayPreviewArea(level, basePos);

			String normalizedScenario = scenario.toLowerCase(Locale.ROOT);
			return switch (normalizedScenario) {
				case "backpack_item" -> setupBackpackItemDisplayPreview(player, normalizedScenario, displaySide);
				case "barrel_east" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST),
						displaySide, false);
				case "barrel_up" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP),
						displaySide, false);
				case "limited_barrel_north" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_1.get().defaultBlockState()
								.setValue(LimitedBarrelBlock.HORIZONTAL_FACING, Direction.NORTH)
								.setValue(LimitedBarrelBlock.VERTICAL_FACING, VerticalFacing.NO),
						displaySide, true);
				case "limited_barrel_up" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_1.get().defaultBlockState()
								.setValue(LimitedBarrelBlock.HORIZONTAL_FACING, Direction.NORTH)
								.setValue(LimitedBarrelBlock.VERTICAL_FACING, VerticalFacing.UP),
						displaySide, true);
				case "single_chest_north" ->
					setupSingleStoragePreview(level, player, normalizedScenario, basePos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get()
							.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.SINGLE), displaySide, false);
				case "double_chest_north" -> setupDoubleChestPreview(level, player, normalizedScenario, basePos, displaySide);
				case "shulker_north" ->
					setupSingleStoragePreview(level, player, normalizedScenario, basePos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.SHULKER_BOX.get()
							.defaultBlockState().setValue(ShulkerBoxBlock.FACING, Direction.NORTH), displaySide, false);
				case "shulker_up" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.SHULKER_BOX.get().defaultBlockState().setValue(ShulkerBoxBlock.FACING, Direction.UP),
						displaySide, false);
				case "moving_minecart_barrel" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						MovingStoragePreviewVehicle.MINECART, createStoragePreviewStack("barrel"), 0);
				case "moving_minecart_chest" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						MovingStoragePreviewVehicle.MINECART, createStoragePreviewStack("chest"), 90);
				case "moving_minecart_shulker" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						MovingStoragePreviewVehicle.MINECART, createStoragePreviewStack("shulker"), 180);
				case "moving_boat_barrel" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						MovingStoragePreviewVehicle.BOAT, createStoragePreviewStack("barrel"), 0);
				case "moving_boat_chest" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide, MovingStoragePreviewVehicle.BOAT,
						createStoragePreviewStack("chest"), 90);
				case "moving_boat_shulker" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						MovingStoragePreviewVehicle.BOAT, createStoragePreviewStack("shulker"), 180);
				case "moving_boat_limited_barrel" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						MovingStoragePreviewVehicle.BOAT, createStoragePreviewStack("limited_barrel"), 270);
				case "llama_barrel" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide, MovingStoragePreviewVehicle.LLAMA,
						createStoragePreviewStack("barrel"), 0);
				case "llama_chest" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide, MovingStoragePreviewVehicle.LLAMA,
						createStoragePreviewStack("chest"), 90);
				case "create_cart_barrel_north" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.NORTH), false,
						0);
				case "create_cart_birch_barrel_north" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.NORTH), false,
						0, Optional.of(WoodType.BIRCH));
				case "create_cart_barrel_east" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST), false,
						90);
				case "create_cart_chest" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)
								.setValue(ChestBlock.TYPE, ChestType.SINGLE),
						false, 180);
				case "create_cart_double_chest" -> setupCreateContraptionDoubleChestPreview(level, player, normalizedScenario, basePos, displaySide, 180);
				case "create_cart_shulker" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.SHULKER_BOX.get().defaultBlockState().setValue(ShulkerBoxBlock.FACING,
								Direction.NORTH),
						false, 270);
				case "create_cart_limited_barrel" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_1.get().defaultBlockState()
								.setValue(LimitedBarrelBlock.HORIZONTAL_FACING, Direction.NORTH)
								.setValue(LimitedBarrelBlock.VERTICAL_FACING, VerticalFacing.NO),
						true, 0);
				case "create_cart_backpack" -> setupCreateContraptionBackpackPreview(level, player, normalizedScenario, basePos, displaySide, 90);
				default -> setupSingleStoragePreview(level, player, "barrel_north", basePos,
						net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.NORTH),
						displaySide, false);
			};
		}

		private void clearItemDisplayPreviewArea(ServerLevel level, BlockPos basePos) {
			level.getEntitiesOfClass(Entity.class, new AABB(basePos).inflate(8), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
			for (int x = -2; x <= 3; x++) {
				for (int y = -1; y <= 2; y++) {
					for (int z = -2; z <= 2; z++) {
						level.setBlock(basePos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
		}

		private ItemDisplayPreviewSetupResult setupSingleStoragePreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos pos, BlockState state,
				DisplaySide displaySide, boolean limitedBarrel) {
			if (limitedBarrel) {
				placeLimitedBarrelWithItemDisplayPreviewItem(level, player, pos, state);
			} else {
				level.setBlock(pos, state, 3);
			}
			StorageBlockEntity storageBlockEntity = WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Storage block entity missing for " + scenario));
			configureItemDisplayPreviewStorage(storageBlockEntity, displaySide, limitedBarrel);
			return new ItemDisplayPreviewSetupResult(scenario, pos, null, -1, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), limitedBarrel,
					ItemDisplayPreviewTargetType.PLACED_STORAGE);
		}

		private void placeLimitedBarrelWithItemDisplayPreviewItem(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState desiredState) {
			BlockPos supportPos = pos.below();
			level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
			player.setYRot(getPlayerYawForLimitedBarrelFacing(desiredState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)));
			player.setXRot(switch (desiredState.getValue(LimitedBarrelBlock.VERTICAL_FACING)) {
				case UP -> 90;
				case DOWN -> -90;
				case NO -> 0;
			});
			ItemStack stack = new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_1_ITEM.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false);
			player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitResult);

			BlockState placedState = level.getBlockState(pos);
			if (!placedState.is(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_1.get())
					|| placedState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING) != desiredState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)
					|| placedState.getValue(LimitedBarrelBlock.VERTICAL_FACING) != desiredState.getValue(LimitedBarrelBlock.VERTICAL_FACING)) {
				throw new IllegalStateException("Limited barrel placement produced " + placedState + " instead of " + desiredState);
			}
		}

		private float getPlayerYawForLimitedBarrelFacing(Direction horizontalFacing) {
			return switch (horizontalFacing) {
				case NORTH -> 0;
				case SOUTH -> 180;
				case EAST -> 90;
				case WEST -> -90;
				default -> 0;
			};
		}

		private ItemDisplayPreviewSetupResult setupDoubleChestPreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos leftPos,
				DisplaySide displaySide) {
			BlockPos rightPos = leftPos.east();
			level.setBlock(leftPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get().defaultBlockState()
					.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT), 3);
			level.setBlock(rightPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get().defaultBlockState()
					.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT), 3);
			setPreviewWoodType(level, leftPos, WoodType.BIRCH);
			StorageBlockEntity storageBlockEntity = level
					.getBlockEntity(rightPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
					.map(be -> (StorageBlockEntity) be).orElseThrow(() -> new IllegalStateException("Double chest main block entity missing"));
			configureItemDisplayPreviewStorage(storageBlockEntity, displaySide, false);
			return new ItemDisplayPreviewSetupResult(scenario, rightPos, null, -1,
					BuiltInRegistries.BLOCK.getKey(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get()).toString(), false,
					ItemDisplayPreviewTargetType.PLACED_STORAGE);
		}

		private ItemDisplayPreviewSetupResult setupBackpackItemDisplayPreview(ServerPlayer player, String scenario, DisplaySide displaySide) {
			player.getInventory().clearContent();
			ItemStack backpack = createBackpackStack(80);
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStackNoCache(backpack);
			backpackWrapper.setSlotNumbers(80, 5);
			configureItemDisplayPreviewWrapper(backpackWrapper, displaySide, false);
			backpackWrapper.getInventoryHandler().saveInventory();
			player.getInventory().setItem(0, backpack);
			player.getInventory().setSelectedSlot(0);
			player.getInventory().setChanged();
			player.inventoryMenu.broadcastChanges();
			return new ItemDisplayPreviewSetupResult(scenario, null, null, -1, BuiltInRegistries.ITEM.getKey(backpack.getItem()).toString(), false,
					ItemDisplayPreviewTargetType.BACKPACK);
		}

		private ItemDisplayPreviewSetupResult setupMovingStoragePreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos basePos,
				DisplaySide displaySide, MovingStoragePreviewVehicle vehicle, ItemStack storageStack, float yRot) {
			Entity entity = switch (vehicle) {
				case MINECART -> new StorageMinecart(level, basePos.getX() + 0.5D, basePos.getY(), basePos.getZ() + 0.5D);
				case BOAT -> new StorageBoat(level, basePos.getX() + 0.5D, basePos.getY(), basePos.getZ() + 0.5D);
				case LLAMA -> {
					Llama llama = EntityType.LLAMA.create(level, EntitySpawnReason.COMMAND);
					if (llama == null) {
						throw new IllegalStateException("Failed to create llama for item display preview");
					}
					llama.setTamed(true);
					yield llama;
				}
			};
			entity.setPos(basePos.getX() + 0.5D, basePos.getY(), basePos.getZ() + 0.5D);
			entity.setYRot(yRot);
			entity.setXRot(0);
			if (!(entity instanceof IMovingStorageEntity movingStorageEntity)) {
				throw new IllegalStateException(entity.getClass().getName() + " is not a moving storage entity");
			}
			movingStorageEntity.getStorageHolder().setStorageItemFrom(storageStack, true);
			configureItemDisplayPreviewWrapper(movingStorageEntity.getStorageHolder().getStorageWrapper(), displaySide,
					MovingStorageWrapper.isLimitedBarrel(storageStack));
			level.addFreshEntity(entity);
			return new ItemDisplayPreviewSetupResult(scenario, null, null, entity.getId(), BuiltInRegistries.ITEM.getKey(storageStack.getItem()).toString(),
					MovingStorageWrapper.isLimitedBarrel(storageStack), ItemDisplayPreviewTargetType.MOVING_STORAGE);
		}

		private ItemDisplayPreviewSetupResult setupCreateContraptionStoragePreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos basePos,
				DisplaySide displaySide, BlockState storageState, boolean limitedBarrel, float cartYaw) {
			if (!isCreateItemDisplayPreviewAvailable()) {
				throw new IllegalStateException("Create item-display preview scenarios are unavailable in this dev runtime");
			}
			return CreateItemDisplayPreviewAutomation.setupStoragePreview(this, level, player, scenario, basePos, displaySide, storageState, limitedBarrel,
					cartYaw, Optional.empty());
		}

		private ItemDisplayPreviewSetupResult setupCreateContraptionStoragePreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos basePos,
				DisplaySide displaySide, BlockState storageState, boolean limitedBarrel, float cartYaw, Optional<WoodType> woodType) {
			if (!isCreateItemDisplayPreviewAvailable()) {
				throw new IllegalStateException("Create item-display preview scenarios are unavailable in this dev runtime");
			}
			return CreateItemDisplayPreviewAutomation.setupStoragePreview(this, level, player, scenario, basePos, displaySide, storageState, limitedBarrel,
					cartYaw, woodType);
		}

		private ItemDisplayPreviewSetupResult setupCreateContraptionDoubleChestPreview(ServerLevel level, ServerPlayer player, String scenario,
				BlockPos basePos, DisplaySide displaySide, float cartYaw) {
			if (!isCreateItemDisplayPreviewAvailable()) {
				throw new IllegalStateException("Create item-display preview scenarios are unavailable in this dev runtime");
			}
			return CreateItemDisplayPreviewAutomation.setupDoubleChestPreview(this, level, scenario, basePos, displaySide, cartYaw);
		}

		private ItemDisplayPreviewSetupResult setupCreateContraptionBackpackPreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos basePos,
				DisplaySide displaySide, float cartYaw) {
			if (!isCreateItemDisplayPreviewAvailable()) {
				throw new IllegalStateException("Create item-display preview scenarios are unavailable in this dev runtime");
			}
			return CreateItemDisplayPreviewAutomation.setupBackpackPreview(this, level, scenario, basePos, displaySide, cartYaw);
		}

		private boolean isCreateItemDisplayPreviewAvailable() {
			try {
				Class.forName("com.simibubi.create.AllBlocks", false, DevClientAutomation.class.getClassLoader());
				Class.forName("com.simibubi.create.content.contraptions.AbstractContraptionEntity", false, DevClientAutomation.class.getClassLoader());
				return true;
			} catch (ClassNotFoundException e) {
				return false;
			}
		}

		private ItemStack createStoragePreviewStack(String storageType) {
			ItemStack storageStack = switch (storageType) {
				case "chest" -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST_ITEM.get());
				case "shulker" -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.SHULKER_BOX_ITEM.get());
				case "limited_barrel" -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_1_ITEM.get());
				default -> new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_ITEM.get());
			};
			if (storageStack.getItem() instanceof WoodStorageBlockItem) {
				WoodStorageBlockItem.setWoodType(storageStack, WoodType.BIRCH);
			}
			return storageStack;
		}

		private void configureItemDisplayPreviewStorage(StorageBlockEntity storageBlockEntity, DisplaySide displaySide, boolean limitedBarrel) {
			if (limitedBarrel && storageBlockEntity.getBlockState().getBlock() instanceof LimitedBarrelBlock limitedBarrelBlock) {
				int targetInventorySlots = limitedBarrelBlock.getNumberOfInventorySlots();
				int slotDiff = targetInventorySlots - storageBlockEntity.getStorageWrapper().getInventoryHandler().size();
				if (slotDiff != 0) {
					storageBlockEntity.changeStorageSize(slotDiff, 0);
				}
			}
			if (storageBlockEntity instanceof WoodStorageBlockEntity woodStorageBlockEntity) {
				woodStorageBlockEntity.setWoodType(WoodType.BIRCH);
			}
			configureItemDisplayPreviewWrapper(storageBlockEntity.getStorageWrapper(), displaySide, limitedBarrel);
			storageBlockEntity.setChanged();
			WorldHelper.notifyBlockUpdate(storageBlockEntity);
		}

		private void setPreviewWoodType(ServerLevel level, BlockPos pos, WoodType woodType) {
			WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).ifPresent(woodStorageBlockEntity -> {
				woodStorageBlockEntity.setWoodType(woodType);
				woodStorageBlockEntity.setChanged();
				WorldHelper.notifyBlockUpdate(woodStorageBlockEntity);
			});
		}

		private void configureItemDisplayPreviewWrapper(IStorageWrapper storageWrapper, DisplaySide displaySide, boolean limitedBarrel) {
			storageWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_AXE));
			if (limitedBarrel) {
				LimitedBarrelBlockEntity.setFixedSettings(storageWrapper, storageWrapper.getInventoryHandler().size());
			}
			ItemDisplaySettingsCategory itemDisplaySettings = storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
			if (!itemDisplaySettings.getSlots().contains(0)) {
				itemDisplaySettings.selectSlot(0);
			}
			itemDisplaySettings.setDisplaySide(displaySide);
			itemDisplaySettings.itemsChanged();
		}

		private String openStorageInventory(ServerPlayer player, BlockPos pos) {
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			ServerLevel level = (ServerLevel) player.level();
			Direction hitDirection = getStorageOpenHitDirection(level.getBlockState(pos));
			Vec3 hitLocation = Vec3.atCenterOf(pos).add(hitDirection.getStepX() * 0.5D, hitDirection.getStepY() * 0.5D, hitDirection.getStepZ() * 0.5D);
			BlockHitResult hitResult = new BlockHitResult(hitLocation, hitDirection, pos, false);
			player.gameMode.useItemOn(player, level, player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hitResult);
			return "";
		}

		private String openItemBackpackPreview(ServerPlayer player) {
			BackpackContext.Item backpackContext = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
			player.openMenu(new SimpleMenuProvider((windowId, inventory, openPlayer) -> new BackpackContainer(windowId, openPlayer, backpackContext),
					Component.literal("Item Display Preview Backpack")), backpackContext::toBuffer);
			return "";
		}

		private String openMovingStorageInventory(ServerPlayer player, int entityId) {
			Entity entity = ((ServerLevel) player.level()).getEntity(entityId);
			if (!(entity instanceof IMovingStorageEntity movingStorageEntity)) {
				throw new IllegalStateException("Moving storage entity missing for id " + entityId);
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			if (entity instanceof StorageBoat storageBoat) {
				storageBoat.interactWithContainerVehicle(player);
			} else if (entity instanceof StorageMinecart storageMinecart) {
				storageMinecart.interact(player, InteractionHand.MAIN_HAND);
			} else if (entity instanceof Llama llama) {
				llama.openCustomInventoryScreen(player);
			} else {
				movingStorageEntity.getStorageHolder().openContainerMenu(player);
			}
			return "";
		}

		private Direction getStorageOpenHitDirection(BlockState state) {
			if (state.getBlock() instanceof BarrelBlock barrelBlock && barrelBlock.getFacing(state) == Direction.UP) {
				return Direction.NORTH;
			}
			return Direction.UP;
		}

		private void waitForClientStorageBlockEntity(BlockPos pos, boolean limitedBarrel) {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
			while (System.nanoTime() < deadline) {
				if (runOnClient(() -> isClientStorageBlockEntityReady(pos, limitedBarrel))) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Timed out waiting for client storage block entity at " + pos);
		}

		private void waitForClientDecorationTable(BlockPos pos, Item resultItem) {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
			while (System.nanoTime() < deadline) {
				if (runOnClient(() -> Minecraft.getInstance().level != null
						&& WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, DecorationTableBlockEntity.class)
								.map(table -> table.getResult().is(resultItem)).orElse(false))) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Timed out waiting for client decoration table at " + pos);
		}

		private void waitForClientDecorationTableScreen() {
			waitForClientScreen("decoration table screen", () -> Minecraft.getInstance().screen instanceof DecorationTableScreen);
		}

		private String getDecorationTableRenderBoundsJson() {
			Screen screen = Minecraft.getInstance().screen;
			if (!(screen instanceof DecorationTableScreen decorationTableScreen)) {
				throw new IllegalStateException("Decoration table screen is not open");
			}
			DecorationTableMenu menu = decorationTableScreen.getMenu();
			Slot lastDyeSlot = menu.getSlot(menu.getDyeSlotRange().firstSlot() + menu.getDyeSlotRange().size() - 1);
			Slot resultSlot = menu.getResultSlot();
			Slot topCoreSlot = menu.getSlot(DecorationTableBlockEntity.TOP_CORE_SLOT);
			Slot sideCoreSlot = menu.getSlot(DecorationTableBlockEntity.SIDE_CORE_SLOT);
			Slot bottomCoreSlot = menu.getSlot(DecorationTableBlockEntity.BOTTOM_CORE_SLOT);
			int x = decorationTableScreen.getGuiLeft() + lastDyeSlot.x + 26;
			int y = decorationTableScreen.getGuiTop() + lastDyeSlot.y;
			int resultSlotX = decorationTableScreen.getGuiLeft() + resultSlot.x;
			int resultSlotY = decorationTableScreen.getGuiTop() + resultSlot.y;
			return "\"preview\":{\"x\":" + x + ",\"y\":" + y + ",\"width\":80,\"height\":" + (resultSlot.y - lastDyeSlot.y + 20) + "},\"resultSlot\":{\"x\":"
					+ resultSlotX + ",\"y\":" + resultSlotY + ",\"width\":16,\"height\":16},\"rotationTargets\":{"
					+ decorationTableRotationTargetJson("top", decorationTableScreen, topCoreSlot, -90, 180, 0) + ','
					+ decorationTableRotationTargetJson("side", decorationTableScreen, sideCoreSlot, 0, 180, 0) + ','
					+ decorationTableRotationTargetJson("bottom", decorationTableScreen, bottomCoreSlot, 90, 180, 0) + '}';
		}

		private String decorationTableRotationTargetJson(String name, DecorationTableScreen screen, Slot slot, int xAxisRotation, int yAxisRotation,
				int zAxisRotation) {
			return "\"" + name + "\":{\"x\":" + (screen.getGuiLeft() + slot.x + 8) + ",\"y\":" + (screen.getGuiTop() + slot.y + 8) + ",\"xAxisRotation\":"
					+ xAxisRotation + ",\"yAxisRotation\":" + yAxisRotation + ",\"zAxisRotation\":" + zAxisRotation + '}';
		}

		private boolean isClientStorageBlockEntityReady(BlockPos pos, boolean limitedBarrel) {
			if (Minecraft.getInstance().level == null) {
				return false;
			}
			if (limitedBarrel && !(Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof LimitedBarrelBlock)) {
				return false;
			}
			return WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, StorageBlockEntity.class).map(storageBlockEntity -> {
				if (!limitedBarrel) {
					return true;
				}
				if (!(storageBlockEntity.getBlockState().getBlock() instanceof LimitedBarrelBlock limitedBarrelBlock)) {
					return false;
				}
				return storageBlockEntity.getStorageWrapper().getInventoryHandler().size() == limitedBarrelBlock.getNumberOfInventorySlots();
			}).orElse(false);
		}

		private void waitForClientBackpackInHotbar() {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
			while (System.nanoTime() < deadline) {
				if (runOnClient(() -> Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.getInventory().getItem(0).getItem() instanceof BackpackItem)) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Timed out waiting for client backpack in hotbar slot 0");
		}

		private void waitForClientEntity(int entityId) {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
			while (System.nanoTime() < deadline) {
				if (runOnClient(() -> Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(entityId) != null)) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Timed out waiting for client entity " + entityId);
		}

		private void waitForStorageScreen() {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
			while (System.nanoTime() < deadline) {
				if (runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
						&& screen.getMenu() instanceof StorageContainerMenuBase<?>)) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Timed out waiting for storage screen");
		}

		private void waitForStorageScreenAndClickSettingsTab() {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
			while (System.nanoTime() < deadline) {
				if (runOnClient(this::clickStorageSettingsTabIfReady)) {
					return;
				}
				sleep(50);
			}
			throw new IllegalStateException("Timed out waiting to click storage settings tab");
		}

		private boolean clickStorageSettingsTabIfReady() {
			Screen screen = Minecraft.getInstance().screen;
			if (!(screen instanceof AbstractContainerScreen<?> containerScreen) || !(containerScreen.getMenu() instanceof StorageContainerMenuBase<?>)) {
				return false;
			}
			StorageSettingsTab settingsTab = findChild(screen, StorageSettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Storage settings tab was not present on " + screen.getClass().getSimpleName()));
			settingsTab.mouseClicked(new MouseButtonEvent(settingsTab.getX() + 9, settingsTab.getY() + 12, new MouseButtonInfo(0, 0)), false);
			return true;
		}

		private String waitForSettingsScreenAndOpenItemDisplayTab() {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
			while (System.nanoTime() < deadline) {
				String screenName = runOnClient(this::openItemDisplaySettingsTabIfReady);
				if (!screenName.isEmpty()) {
					return screenName;
				}
				sleep(50);
			}
			throw new IllegalStateException("Timed out waiting for storage settings screen");
		}

		private String openItemDisplaySettingsTabIfReady() {
			Screen screen = Minecraft.getInstance().screen;
			if (!(screen instanceof SettingsScreen settingsScreen)) {
				return "";
			}
			for (GuiEventListener child : settingsScreen.getSettingsTabControl().children()) {
				if (child instanceof ItemDisplaySettingsTab itemDisplaySettingsTab) {
					Optional<SettingsTab<?>> openTab = settingsScreen.getSettingsTabControl().getOpenTab();
					if (openTab.map(tab -> tab == itemDisplaySettingsTab).orElse(false)) {
						return screen.getClass().getSimpleName();
					}
					itemDisplaySettingsTab.mouseClicked(
							new MouseButtonEvent(itemDisplaySettingsTab.getX() + 9, itemDisplaySettingsTab.getY() + 12, new MouseButtonInfo(0, 0)), false);
					return "";
				}
			}
			throw new IllegalStateException("Item display settings tab was not present on " + screen.getClass().getSimpleName());
		}

		private <T extends GuiEventListener> Optional<T> findChild(GuiEventListener parent, Class<T> childClass) {
			if (childClass.isInstance(parent)) {
				return Optional.of(childClass.cast(parent));
			}
			if (parent instanceof ContainerEventHandler containerEventHandler) {
				for (GuiEventListener child : containerEventHandler.children()) {
					Optional<T> found = findChild(child, childClass);
					if (found.isPresent()) {
						return found;
					}
				}
			}
			return Optional.empty();
		}

		private static class CreateItemDisplayPreviewAutomation {
			private static String openCreateContraptionStorage(ServerPlayer player, int entityId, BlockPos localPos) {
				Entity entity = ((ServerLevel) player.level()).getEntity(entityId);
				if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
					throw new IllegalStateException("Create contraption entity missing for id " + entityId);
				}
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				if (!contraptionEntity.handlePlayerInteraction(player, localPos, Direction.UP, InteractionHand.MAIN_HAND)) {
					throw new IllegalStateException("Create contraption did not open mounted storage at " + localPos);
				}
				return "";
			}

			private static ItemDisplayPreviewSetupResult setupStoragePreview(AutomationServer server, ServerLevel level, ServerPlayer player, String scenario,
					BlockPos basePos, DisplaySide displaySide, BlockState storageState, boolean limitedBarrel, float cartYaw, Optional<WoodType> woodType) {
				BlockPos assemblerPos = basePos;
				BlockPos storagePos = assemblerPos.above();
				level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
				level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
						.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
				level.setBlock(storagePos, storageState, 3);
				StorageBlockEntity storageBlockEntity = WorldHelper.getBlockEntity(level, storagePos, StorageBlockEntity.class)
						.orElseThrow(() -> new IllegalStateException("Create contraption storage block entity missing for " + scenario));
				if (storageBlockEntity instanceof WoodStorageBlockEntity woodStorageBlockEntity) {
					woodType.ifPresent(woodStorageBlockEntity::setWoodType);
				}
				server.configureItemDisplayPreviewStorage(storageBlockEntity, displaySide, limitedBarrel);

				AbstractContraptionEntity contraptionEntity = assembleCreateCartContraption(level, assemblerPos, cartYaw);
				BlockPos localPos = findMountedStorageLocalPos(contraptionEntity);
				return new ItemDisplayPreviewSetupResult(scenario, null, localPos, contraptionEntity.getId(),
						BuiltInRegistries.BLOCK.getKey(storageState.getBlock()).toString(), limitedBarrel, ItemDisplayPreviewTargetType.CREATE_CONTRAPTION);
			}

			private static ItemDisplayPreviewSetupResult setupDoubleChestPreview(AutomationServer server, ServerLevel level, String scenario, BlockPos basePos,
					DisplaySide displaySide, float cartYaw) {
				BlockPos assemblerPos = basePos;
				BlockPos leftPos = assemblerPos.above();
				BlockPos rightPos = leftPos.east();
				level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
				level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
						.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
				BlockState leftState = net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get().defaultBlockState()
						.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
				BlockState rightState = net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get().defaultBlockState()
						.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT);
				level.setBlock(leftPos, leftState, 3);
				level.setBlock(rightPos, rightState, 3);
				level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(leftPos, rightPos)));

				server.setPreviewWoodType(level, leftPos, WoodType.BIRCH);
				StorageBlockEntity storageBlockEntity = WorldHelper.getBlockEntity(level, rightPos, StorageBlockEntity.class)
						.orElseThrow(() -> new IllegalStateException("Create contraption double chest main block entity missing for " + scenario));
				server.configureItemDisplayPreviewStorage(storageBlockEntity, displaySide, false);

				AbstractContraptionEntity contraptionEntity = assembleCreateCartContraption(level, assemblerPos, cartYaw);
				BlockPos localPos = findMountedDoubleChestLocalPos(contraptionEntity);
				return new ItemDisplayPreviewSetupResult(scenario, null, localPos, contraptionEntity.getId(),
						BuiltInRegistries.BLOCK.getKey(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CHEST.get()).toString(), false,
						ItemDisplayPreviewTargetType.CREATE_CONTRAPTION);
			}

			private static ItemDisplayPreviewSetupResult setupBackpackPreview(AutomationServer server, ServerLevel level, String scenario, BlockPos basePos,
					DisplaySide displaySide, float cartYaw) {
				BlockPos assemblerPos = basePos;
				BlockPos backpackPos = assemblerPos.above();
				level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
				level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
						.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
				BlockState backpackState = ModBlocks.DIAMOND_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING, Direction.NORTH);
				level.setBlock(backpackPos, backpackState, 3);
				BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(level, backpackPos, BackpackBlockEntity.class)
						.orElseThrow(() -> new IllegalStateException("Create contraption backpack block entity missing for " + scenario));
				ItemStack backpack = server.createBackpackStack(80);
				IBackpackWrapper backpackWrapper = BackpackWrapper.fromStackNoCache(backpack);
				backpackWrapper.setSlotNumbers(80, 5);
				server.configureItemDisplayPreviewWrapper(backpackWrapper, displaySide, false);
				backpackWrapper.getInventoryHandler().saveInventory();
				backpackBlockEntity.setBackpack(backpack);
				backpackBlockEntity.setChanged();
				WorldHelper.notifyBlockUpdate(backpackBlockEntity);

				AbstractContraptionEntity contraptionEntity = assembleCreateCartContraption(level, assemblerPos, cartYaw);
				BlockPos localPos = findMountedStorageLocalPos(contraptionEntity);
				return new ItemDisplayPreviewSetupResult(scenario, null, localPos, contraptionEntity.getId(),
						BuiltInRegistries.BLOCK.getKey(backpackState.getBlock()).toString(), false, ItemDisplayPreviewTargetType.CREATE_CONTRAPTION);
			}

			private static AbstractContraptionEntity assembleCreateCartContraption(ServerLevel level, BlockPos assemblerPos, float cartYaw) {
				Minecart cart = new Minecart(EntityType.MINECART, level);
				cart.setPos(assemblerPos.getX() + 0.5D, assemblerPos.getY(), assemblerPos.getZ() + 0.5D);
				cart.setYRot(cartYaw);
				level.addFreshEntity(cart);
				CartAssemblerBlockEntity assemblerBlockEntity = WorldHelper.getBlockEntity(level, assemblerPos, CartAssemblerBlockEntity.class)
						.orElseThrow(() -> new IllegalStateException("Create cart assembler block entity missing"));
				assemblerBlockEntity.tryAssemble(cart);
				return cart.getPassengers().stream().filter(AbstractContraptionEntity.class::isInstance).map(AbstractContraptionEntity.class::cast).findFirst()
						.orElseThrow(() -> new IllegalStateException(
								"Create cart assembler did not create a mounted contraption" + (assemblerBlockEntity.getLastAssemblyException() == null
										? ""
										: ": " + assemblerBlockEntity.getLastAssemblyException().component.getString())));
			}

			private static BlockPos findMountedStorageLocalPos(AbstractContraptionEntity contraptionEntity) {
				return ContraptionHelper.getMountedItemStorages(contraptionEntity).keySet().stream()
						.filter(localPos -> ContraptionHelper.getMountedStorage(contraptionEntity, localPos) != null).findFirst()
						.orElseThrow(() -> new IllegalStateException("Create contraption did not contain a mounted sophisticated storage"));
			}

			private static BlockPos findMountedDoubleChestLocalPos(AbstractContraptionEntity contraptionEntity) {
				return ContraptionHelper.getMountedItemStorages(contraptionEntity).keySet().stream().filter(localPos -> {
					MountedStorageBase mountedStorage = ContraptionHelper.getMountedStorage(contraptionEntity, localPos);
					return mountedStorage != null && mountedStorage.getStorageStack().getItem() instanceof ChestBlockItem
							&& ChestBlockItem.isDoubleChest(mountedStorage.getStorageStack())
							&& mountedStorage.getStorageStack().has(ModCoreDataComponents.STORAGE_UUID);
				}).findFirst().orElseGet(() -> findMountedStorageLocalPos(contraptionEntity));
			}
		}

		private enum ItemDisplayPreviewTargetType {
			PLACED_STORAGE, BACKPACK, MOVING_STORAGE, CREATE_CONTRAPTION
		}

		private enum MovingStoragePreviewVehicle {
			MINECART, BOAT, LLAMA
		}

		private record ItemDisplayPreviewSetupResult(String scenario, BlockPos menuPos, BlockPos localPos, int entityId, String target, boolean limitedBarrel,
				ItemDisplayPreviewTargetType targetType) {
		}

		private record DecorationTableRenderPreviewSetupResult(String itemName, BlockPos tablePos, Item resultItem) {
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
			return "{" + jsonProperty("screenClass", screen == null ? null : screen.getClass().getName()) + ","
					+ jsonProperty("screenSimpleName", screen == null ? null : screen.getClass().getSimpleName()) + ","
					+ jsonProperty("screenTitle", screen == null ? null : screen.getTitle().getString()) + "," + "\"inWorld\":" + (minecraft.level != null)
					+ "," + "\"playerLoaded\":" + (minecraft.player != null) + "," + "\"windowWidth\":" + minecraft.getWindow().getWidth() + ","
					+ "\"windowHeight\":" + minecraft.getWindow().getHeight() + "," + "\"guiWidth\":" + minecraft.getWindow().getGuiScaledWidth() + ","
					+ "\"guiHeight\":" + minecraft.getWindow().getGuiScaledHeight() + "}";
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
			updateMouseHandlerPosition(minecraft, targetX * scale, targetY * scale);
			if (minecraft.screen != null) {
				minecraft.screen.mouseMoved(targetX, targetY);
			}
			return "{\"ok\":true,\"x\":" + targetX + ",\"y\":" + targetY + "}";
		}

		private void updateMouseHandlerPosition(Minecraft minecraft, double x, double y) {
			try {
				Method onMove = MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
				onMove.setAccessible(true);
				onMove.invoke(minecraft.mouseHandler, minecraft.getWindow().handle(), x, y);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Failed to update the client mouse position", e);
			}
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
			String json = "{\"host\":\"127.0.0.1\",\"port\":" + port + ",\"processId\":" + ProcessHandle.current().pid() + "}";
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
			return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemName));
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
					.map(StorageBlockEntity.class::cast).orElseThrow(() -> new IllegalStateException("Missing barrel storage at " + pos));
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

		private record Issue23SetupResult(int contraptionEntityId, BlockPos mountedStoragePos, BlockPos receiverPos) {
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

		private record AdvancedCompactingHighStackRegressionResult(String name, boolean passed, int firstSlotCount, int secondSlotCount, int triggerCount,
				int expectedNuggets, int actualNuggets, int expectedIngots, int actualIngots, int expectedBlocks, int actualBlocks, int insertRemainder,
				String error) {
		}

		private record DepositLimitedBarrelGuiStateScenario(String name, boolean backpackOpened, boolean barrelOpened) {
		}

		private record DepositLimitedBarrelGuiStateResult(String scenario, boolean backpackOpened, boolean barrelOpened, boolean handled, int slotLimit,
				int backpackBefore, int backpackAfter, int barrelBefore, int barrelAfter, int totalBefore, int totalAfter, boolean passed, String error) {
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

		private record InventoryInteractionKeyMappings(InputConstants.Key sort, InputConstants.Key transferToStorage, InputConstants.Key transferToInventory) {
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
