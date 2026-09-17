package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import jdk.jfr.Category;
import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;

/** Exercises shared linked Backpack hosts across the supported endpoint and tick ranges. */
public final class BackpackLinkedStoragePerformanceRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);
	private static final int DEFAULT_ENDPOINTS = 24;
	private static final int DEFAULT_TICKS = 200;
	private static final int DEFAULT_STACKS_PER_TICK = 4;
	private static final int MAGNET_PERIOD_TICKS = 10;
	private static StressRun activeRun;
	private static Recording activeRecording;
	private static Path activeRecordingPath;

	private BackpackLinkedStoragePerformanceRegression() {
	}

	public static void init() {
		MinecraftForge.EVENT_BUS.addListener(BackpackLinkedStoragePerformanceRegression::tick);
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		String mode = string(request, "mode", "status");
		sendJsonHandling(exchange, LOGGER, () -> switch (mode) {
			case "setup" -> AutomationRuntime.runOnServer(player -> setup(player, parse(request)));
			case "start" -> AutomationRuntime.runOnServer(BackpackLinkedStoragePerformanceRegression::start);
			case "status" -> AutomationRuntime.runOnServer(BackpackLinkedStoragePerformanceRegression::status);
			case "jfrStart" -> AutomationRuntime.runOnServer(player -> startJfr(string(request, "outputPath", "")));
			case "jfrStop" -> AutomationRuntime.runOnServer(player -> stopJfr());
			default -> throw new IllegalArgumentException("Unsupported linked-storage performance mode " + mode);
		});
	}

	private static StressSpec parse(JsonObject request) {
		Workload workload = switch (string(request, "workload", "magnet")) {
			case "magnet" -> Workload.MAGNET;
			case "inventory" -> Workload.INVENTORY;
			default -> throw new IllegalArgumentException("Unsupported linked-storage performance workload");
		};
		int endpoints = integer(request, "endpoints", DEFAULT_ENDPOINTS);
		int ticks = integer(request, "ticks", DEFAULT_TICKS);
		int stacksPerTick = integer(request, "stacksPerTick", DEFAULT_STACKS_PER_TICK);
		if (endpoints < 2 || endpoints > 36 || ticks < 20 || ticks > 1_200 || stacksPerTick < 1 || stacksPerTick > 16) {
			throw new IllegalArgumentException("Values must be endpoints=2..36, ticks=20..1200, stacksPerTick=1..16");
		}
		return new StressSpec(workload, endpoints, ticks, stacksPerTick);
	}

	private static String string(JsonObject request, String name, String defaultValue) {
		return request.has(name) ? request.get(name).getAsString() : defaultValue;
	}

	private static int integer(JsonObject request, String name, int defaultValue) {
		return request.has(name) ? request.get(name).getAsInt() : defaultValue;
	}

	private static String setup(ServerPlayer player, StressSpec spec) {
		if (activeRun != null) {
			cleanup(activeRun);
		}
		player.closeContainer();
		player.getInventory().clearContent();
		ServerLevel level = player.serverLevel();
		ItemStack primary = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get());
		if (LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) != LinkedStorageService.LinkResult.SUCCESS) {
			throw new IllegalStateException("Could not create the linked-storage performance group");
		}
		LinkedStorageEndpointData primaryEndpoint = LinkedStorageStackData.getEndpoint(primary);
		if (primaryEndpoint == null) {
			throw new IllegalStateException("Linked-storage performance primary has no endpoint identity");
		}
		IBackpackWrapper canonicalHost = BackpackLinkedStorageResolver.resolvePrimaryCanonicalHost(level, primary)
				.orElseThrow(() -> new IllegalStateException("Could not resolve the linked-storage performance host"));
		configureMagnet(canonicalHost);

		List<ItemStack> endpoints = new ArrayList<>();
		List<UUID> secondaryEndpointIds = new ArrayList<>();
		endpoints.add(primary);
		for (int index = 1; index < spec.endpoints(); index++) {
			ItemStack endpoint = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
			if (LinkedStorageService.linkWithResult(level, player.getUUID(), linker.copy(), endpoint) != LinkedStorageService.LinkResult.SUCCESS) {
				throw new IllegalStateException("Could not create linked-storage performance endpoint " + index);
			}
			LinkedStorageEndpointData endpointData = LinkedStorageStackData.getEndpoint(endpoint);
			if (endpointData == null) {
				throw new IllegalStateException("Linked-storage performance endpoint has no identity");
			}
			endpoints.add(endpoint);
			secondaryEndpointIds.add(endpointData.endpointId());
		}
		for (int index = 0; index < endpoints.size(); index++) {
			player.getInventory().setItem(index, endpoints.get(index));
		}
		player.getInventory().selected = 0;
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		activeRun = new StressRun(player, spec, primaryEndpoint.groupId(), primaryEndpoint.endpointId(), secondaryEndpointIds, canonicalHost,
				manager.getRevision(primaryEndpoint.groupId()), manager.getRenderRevision(primaryEndpoint.groupId()),
				manager.getHostDescriptor(primaryEndpoint.groupId()).orElseThrow());
		return "{\"ok\":true,\"workload\":\"" + spec.workload().serializedName + "\",\"endpoints\":" + spec.endpoints() + ",\"ticks\":" + spec.ticks()
				+ ",\"stacksPerTick\":" + spec.stacksPerTick() + "}";
	}

	private static void configureMagnet(IBackpackWrapper canonicalHost) {
		UpgradeHandler upgrades = canonicalHost.getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(ModItems.MAGNET_UPGRADE.get()));
		MagnetUpgradeWrapper magnet = upgrades.getWrappersThatImplement(MagnetUpgradeWrapper.class).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Linked-storage performance magnet was not initialized"));
		magnet.setPickupItems(true);
		magnet.setPickupXp(false);
		magnet.getFilterLogic().setDepositFilterType(ContentsFilterType.ALLOW);
		magnet.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
		upgrades.saveInventory();
	}

	private static String start(ServerPlayer player) {
		if (activeRun == null || activeRun.player != player || activeRun.running) {
			return "{\"ok\":false,\"error\":\"No inactive linked-storage performance run is prepared\"}";
		}
		activeRun.running = true;
		activeRun.startedNanos = System.nanoTime();
		return "{\"ok\":true,\"running\":true}";
	}

	private static void tick(TickEvent.ServerTickEvent event) {
		StressRun run = activeRun;
		if (event.phase != TickEvent.Phase.END || run == null || !run.running || run.player.serverLevel().getServer() != event.getServer()) {
			return;
		}
		try {
			LinkedStorageWorkloadTickEvent workloadEvent = new LinkedStorageWorkloadTickEvent();
			workloadEvent.workload = run.spec.workload().serializedName;
			workloadEvent.endpoints = run.spec.endpoints();
			workloadEvent.begin();
			int operationsBefore = run.operations();
			if (run.spec.workload() == Workload.MAGNET) {
				runMagnetWork(run);
			} else {
				runInventoryWork(run);
			}
			workloadEvent.operations = run.operations() - operationsBefore;
			workloadEvent.commit();
			run.elapsedTicks++;
			if (run.elapsedTicks >= run.spec.ticks()) {
				run.running = false;
				run.completedNanos = System.nanoTime();
			}
		} catch (RuntimeException e) {
			run.failure = e.getMessage();
			run.running = false;
			run.completedNanos = System.nanoTime();
			LOGGER.error("Linked-storage performance workload failed", e);
		}
	}

	private static void runMagnetWork(StressRun run) {
		if (run.elapsedTicks % MAGNET_PERIOD_TICKS != 0) {
			return;
		}
		for (int stack = 0; stack < run.spec.stacksPerTick(); stack++) {
			ItemEntity itemEntity = new ItemEntity(run.player.serverLevel(), run.player.getX(), run.player.getY() + 0.5D, run.player.getZ(),
					new ItemStack(Items.DIAMOND, 64));
			itemEntity.setPickUpDelay(0);
			run.player.serverLevel().addFreshEntity(itemEntity);
			run.spawnedItems += itemEntity.getItem().getCount();
		}
	}

	private static void runInventoryWork(StressRun run) {
		for (int endpointIndex = 0; endpointIndex < run.spec.endpoints(); endpointIndex++) {
			int endpointNumber = endpointIndex;
			ItemStack endpoint = run.player.getInventory().getItem(endpointIndex);
			IBackpackWrapper resolvedHost = BackpackLinkedStorageResolver.resolveCanonicalHost(run.player.serverLevel(), endpoint)
					.orElseThrow(() -> new IllegalStateException("Could not resolve linked-storage performance endpoint " + endpointNumber));
			if (resolvedHost != run.canonicalHost) {
				throw new IllegalStateException("Linked-storage performance endpoint recreated its canonical host");
			}
			InventoryHandler inventory = resolvedHost.getInventoryHandler();
			for (int stack = 0; stack < run.spec.stacksPerTick(); stack++) {
				int slot = 1 + stack % 8;
				ItemStack inserted = new ItemStack(stack % 2 == 0 ? Items.REDSTONE : Items.COBBLESTONE, 16);
				if (!inventory.insertItemOnlyToSlot(slot, inserted, false).isEmpty()) {
					throw new IllegalStateException("Linked-storage performance inventory slot rejected an inserted stack");
				}
				if (inventory.extractItem(slot, inserted.getCount(), false).getCount() != inserted.getCount()) {
					throw new IllegalStateException("Linked-storage performance inventory slot did not extract the inserted stack");
				}
				run.insertedStacks++;
				run.extractedStacks++;
			}
		}
	}

	private static String status(ServerPlayer player) {
		StressRun run = activeRun;
		if (run == null || run.player != player) {
			return "{\"ok\":false,\"error\":\"No linked-storage performance run is prepared\"}";
		}
		if (run.running) {
			return "{\"ok\":true,\"running\":true,\"elapsedTicks\":" + run.elapsedTicks + "}";
		}
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(player.serverLevel()).manager();
		long renderRevision = manager.getRenderRevision(run.groupId);
		boolean descriptorStable = run.initialDescriptor.equals(manager.getHostDescriptor(run.groupId).orElse(null));
		boolean renderStable = run.initialRenderRevision == renderRevision;
		boolean contentsChanged = manager.getRevision(run.groupId) > run.initialRevision;
		boolean complete = run.failure == null && run.elapsedTicks == run.spec.ticks()
				&& (run.spec.workload() == Workload.MAGNET
						? countDiamonds(run.canonicalHost.getInventoryHandler()) > 0 && run.spawnedItems > 0
						: run.insertedStacks == run.expectedInventoryOperations() && run.extractedStacks == run.expectedInventoryOperations());
		long durationMillis = (run.completedNanos - run.startedNanos) / 1_000_000L;
		String result = "{\"ok\":" + (complete && contentsChanged && descriptorStable && renderStable) + ",\"running\":false,\"workload\":\""
				+ run.spec.workload().serializedName + "\",\"elapsedTicks\":" + run.elapsedTicks + ",\"durationMillis\":" + durationMillis
				+ ",\"insertedStacks\":" + run.insertedStacks + ",\"extractedStacks\":" + run.extractedStacks + ",\"contentsChanged\":" + contentsChanged
				+ ",\"carrierDescriptorStable\":" + descriptorStable + ",\"renderRevisionStable\":" + renderStable
				+ (run.failure == null ? "" : ",\"error\":\"" + escapeJson(run.failure) + "\"") + "}";
		cleanup(run);
		activeRun = null;
		return result;
	}

	private static int countDiamonds(InventoryHandler inventory) {
		int count = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			if (inventory.getStackInSlot(slot).is(Items.DIAMOND)) {
				count += inventory.getStackInSlot(slot).getCount();
			}
		}
		return count;
	}

	private static String startJfr(String outputPath) {
		if (activeRecording != null || outputPath.isBlank()) {
			throw new IllegalStateException("A JFR outputPath is required and no recording may already be running");
		}
		try {
			Path destination = Path.of(outputPath).toAbsolutePath();
			if (!destination.getFileName().toString().endsWith(".jfr")) {
				throw new IllegalArgumentException("JFR outputPath must end in .jfr");
			}
			Files.createDirectories(destination.getParent());
			activeRecording = new Recording(Configuration.getConfiguration("profile"));
			activeRecording.enable(LinkedStorageWorkloadTickEvent.class);
			activeRecording.setToDisk(true);
			activeRecording.setDestination(destination);
			activeRecording.start();
			activeRecordingPath = destination;
			return "{\"ok\":true,\"outputPath\":\"" + escapeJson(destination.toString()) + "\"}";
		} catch (IOException | ParseException e) {
			throw new IllegalStateException("Could not start linked-storage performance JFR recording", e);
		}
	}

	private static String stopJfr() {
		if (activeRecording == null || activeRecordingPath == null) {
			throw new IllegalStateException("No linked-storage performance JFR recording is running");
		}
		try {
			activeRecording.stop();
			return "{\"ok\":true,\"outputPath\":\"" + escapeJson(activeRecordingPath.toString()) + "\"}";
		} finally {
			activeRecording.close();
			activeRecording = null;
			activeRecordingPath = null;
		}
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static void cleanup(StressRun run) {
		ServerLevel level = run.player.serverLevel();
		run.player.closeContainer();
		run.player.getInventory().clearContent();
		run.player.getInventory().setChanged();
		level.getEntitiesOfClass(ItemEntity.class, new AABB(run.player.blockPosition()).inflate(8), entity -> entity.getItem().is(Items.DIAMOND))
				.forEach(ItemEntity::discard);
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		for (UUID endpointId : run.secondaryEndpointIds) {
			manager.unregisterEndpoint(run.groupId, endpointId);
		}
		manager.discardUnboundGroup(run.groupId, run.primaryEndpointId);
	}

	private enum Workload {
		MAGNET("magnet"), INVENTORY("inventory");
		private final String serializedName;
		Workload(String serializedName) {
			this.serializedName = serializedName;
		}
	}
	private record StressSpec(Workload workload, int endpoints, int ticks, int stacksPerTick) {
	}
	private static class StressRun {
		private final ServerPlayer player;
		private final StressSpec spec;
		private final UUID groupId;
		private final UUID primaryEndpointId;
		private final List<UUID> secondaryEndpointIds;
		private final IBackpackWrapper canonicalHost;
		private final long initialRevision;
		private final long initialRenderRevision;
		private final LinkedStorageHostDescriptor initialDescriptor;
		private boolean running;
		private int elapsedTicks;
		private long startedNanos;
		private long completedNanos;
		private int spawnedItems;
		private int insertedStacks;
		private int extractedStacks;
		private String failure;
		private StressRun(ServerPlayer player, StressSpec spec, UUID groupId, UUID primaryEndpointId, List<UUID> secondaryEndpointIds,
				IBackpackWrapper canonicalHost, long initialRevision, long initialRenderRevision, LinkedStorageHostDescriptor initialDescriptor) {
			this.player = player;
			this.spec = spec;
			this.groupId = groupId;
			this.primaryEndpointId = primaryEndpointId;
			this.secondaryEndpointIds = secondaryEndpointIds;
			this.canonicalHost = canonicalHost;
			this.initialRevision = initialRevision;
			this.initialRenderRevision = initialRenderRevision;
			this.initialDescriptor = initialDescriptor;
		}
		private int expectedInventoryOperations() {
			return spec.ticks() * spec.endpoints() * spec.stacksPerTick();
		}
		private int operations() {
			return spec.workload() == Workload.MAGNET ? spawnedItems : insertedStacks + extractedStacks;
		}
	}
	@Name("devclientautomation.LinkedStorageWorkloadTick")
	@Label("Linked Storage Workload Tick")
	@Category({"DevClientAutomation", "Linked Storage"})
	private static class LinkedStorageWorkloadTickEvent extends Event {
		private String workload;
		private int endpoints;
		private int operations;
	}
}
