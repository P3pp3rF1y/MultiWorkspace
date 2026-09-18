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
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER;

public final class BackpackLinkedStoragePerformanceRegression {
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
		NeoForge.EVENT_BUS.addListener(BackpackLinkedStoragePerformanceRegression::tick);
	}

	public static void handle(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			send(exchange, "{\"ok\":false,\"error\":\"Method not allowed\"}");
			return;
		}
		try {
			JsonObject request = parseRequest(exchange);
			String mode = string(request, "mode", "status");
			String response = switch (mode) {
				case "setup" -> AutomationRuntime.runOnServer(player -> setup(player, parse(request)));
				case "start" -> AutomationRuntime.runOnServer(BackpackLinkedStoragePerformanceRegression::start);
				case "status" -> AutomationRuntime.runOnServer(BackpackLinkedStoragePerformanceRegression::status);
				case "jfrStart" -> AutomationRuntime.runOnServer(player -> startJfr(string(request, "outputPath", "")));
				case "jfrStop" -> AutomationRuntime.runOnServer(player -> stopJfr());
				default -> throw new IllegalArgumentException("Unsupported linked-storage performance mode " + mode);
			};
			send(exchange, response);
		} catch (RuntimeException e) {
			send(exchange, "{\"ok\":false,\"error\":" + quote(e.getMessage()) + "}");
		}
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

	private static String setup(ServerPlayer player, StressSpec spec) {
		if (activeRun != null) {
			cleanup(activeRun);
		}
		player.closeContainer();
		player.getInventory().clearContent();
		ServerLevel level = player.level();
		ItemStack primary = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		ItemStack linker = new ItemStack(ENDER_LINKER.get());
		if (LinkedStorageService.linkWithResult(level, player.getUUID(), linker, primary) != LinkedStorageService.LinkResult.SUCCESS) {
			throw new IllegalStateException("Could not create the linked-storage performance group");
		}
		LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "primary");
		IBackpackWrapper canonicalHost = BackpackLinkedStorageResolver.resolvePrimaryCanonicalHost(level, primary)
				.orElseThrow(() -> new IllegalStateException("Could not resolve the linked-storage performance host"));
		configureMagnet(canonicalHost);
		List<ItemStack> endpoints = new ArrayList<>();
		List<UUID> secondaryEndpointIds = new ArrayList<>();
		endpoints.add(primary);
		for (int index = 1; index < spec.endpoints(); index++) {
			ItemStack endpoint = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
			if (LinkedStorageService.linkWithResult(level, player.getUUID(), linker.copyWithCount(1), endpoint) != LinkedStorageService.LinkResult.SUCCESS) {
				throw new IllegalStateException("Could not create linked-storage performance endpoint " + index);
			}
			endpoints.add(endpoint);
			secondaryEndpointIds.add(requireEndpoint(endpoint, "secondary").endpointId());
		}
		for (int index = 0; index < endpoints.size(); index++) {
			player.getInventory().setItem(index, endpoints.get(index));
		}
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

	private static void tick(ServerTickEvent.Post event) {
		StressRun run = activeRun;
		if (run == null || !run.running || run.player.level().getServer() != event.getServer()) {
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
			workloadEvent.end();
			boolean eventWillCommit = workloadEvent.shouldCommit();
			workloadEvent.commit();
			if (eventWillCommit) {
				run.expectedJfrEvents++;
				run.jfrVerificationPending = true;
			}
			run.elapsedTicks++;
			if (run.elapsedTicks >= run.spec.ticks()) {
				run.running = false;
				run.completedNanos = System.nanoTime();
			}
		} catch (RuntimeException e) {
			run.failure = e.getMessage();
			run.running = false;
			run.completedNanos = System.nanoTime();
		}
	}

	private static void runMagnetWork(StressRun run) {
		if (run.elapsedTicks % MAGNET_PERIOD_TICKS != 0) {
			return;
		}
		for (int stack = 0; stack < run.spec.stacksPerTick(); stack++) {
			ItemEntity itemEntity = new ItemEntity(run.player.level(), run.player.getX(), run.player.getY() + 0.5D, run.player.getZ(),
					new ItemStack(Items.DIAMOND, 64));
			itemEntity.setPickUpDelay(0);
			run.player.level().addFreshEntity(itemEntity);
			run.spawnedItems += itemEntity.getItem().getCount();
		}
	}

	private static void runInventoryWork(StressRun run) {
		for (int endpointIndex = 0; endpointIndex < run.spec.endpoints(); endpointIndex++) {
			int currentEndpoint = endpointIndex;
			ItemStack endpoint = run.player.getInventory().getItem(endpointIndex);
			IBackpackWrapper resolvedHost = BackpackLinkedStorageResolver.resolveCanonicalHost(run.player.level(), endpoint)
					.orElseThrow(() -> new IllegalStateException("Could not resolve linked-storage performance endpoint " + currentEndpoint));
			if (resolvedHost != run.canonicalHost) {
				throw new IllegalStateException("Linked-storage performance endpoint recreated its canonical host");
			}
			InventoryHandler inventory = resolvedHost.getInventoryHandler();
			for (int stack = 0; stack < run.spec.stacksPerTick(); stack++) {
				int slot = 1 + stack % 8;
				ItemStack inserted = new ItemStack(stack % 2 == 0 ? Items.REDSTONE : Items.COBBLESTONE, 16);
				try (Transaction transaction = Transaction.openRoot()) {
					if (inventory.insert(slot, ItemResource.of(inserted), inserted.getCount(), transaction) != inserted.getCount()) {
						throw new IllegalStateException("Linked-storage performance inventory slot " + slot + " rejected an inserted stack");
					}
					transaction.commit();
				}
				try (Transaction transaction = Transaction.openRoot()) {
					if (inventory.extract(slot, ItemResource.of(inserted), inserted.getCount(), transaction) != inserted.getCount()) {
						throw new IllegalStateException("Linked-storage performance inventory slot " + slot + " did not extract the inserted stack");
					}
					transaction.commit();
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
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(player.level()).manager();
		long revision = manager.getRevision(run.groupId);
		long renderRevision = manager.getRenderRevision(run.groupId);
		boolean descriptorStable = run.initialDescriptor.equals(manager.getHostDescriptor(run.groupId).orElse(null));
		boolean renderStable = run.initialRenderRevision == renderRevision;
		boolean contentsChanged = revision > run.initialRevision;
		boolean jfrEventsComplete = !run.jfrVerificationPending && run.recordedJfrEvents == run.expectedJfrEvents;
		int storedDiamonds = count(run.canonicalHost.getInventoryHandler(), Items.DIAMOND);
		boolean complete = run.failure == null && run.elapsedTicks == run.spec.ticks()
				&& (run.spec.workload() == Workload.MAGNET
						? storedDiamonds > 0 && run.spawnedItems > 0
						: run.insertedStacks == run.expectedInventoryOperations() && run.extractedStacks == run.expectedInventoryOperations());
		long durationMillis = (run.completedNanos - run.startedNanos) / 1_000_000L;
		String response = "{\"ok\":" + (complete && contentsChanged && descriptorStable && renderStable && jfrEventsComplete)
				+ ",\"running\":false,\"elapsedTicks\":" + run.elapsedTicks + ",\"durationMillis\":" + durationMillis + ",\"contentsChanged\":"
				+ contentsChanged + ",\"carrierDescriptorStable\":" + descriptorStable + ",\"renderRevisionStable\":" + renderStable + ",\"jfrEventsComplete\":"
				+ jfrEventsComplete + ",\"recordedJfrEvents\":" + run.recordedJfrEvents + ",\"expectedJfrEvents\":" + run.expectedJfrEvents
				+ ",\"storedDiamonds\":" + storedDiamonds + (run.failure == null ? "" : ",\"error\":" + quote(run.failure)) + "}";
		cleanup(run);
		activeRun = null;
		return response;
	}

	private static String startJfr(String outputPath) {
		if (activeRecording != null || outputPath.isBlank()) {
			throw new IllegalStateException(
					activeRecording == null ? "JFR outputPath is required" : "A linked-storage performance JFR recording is already running");
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
			return "{\"ok\":true,\"outputPath\":" + quote(destination.toString()) + "}";
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
			if (activeRun != null) {
				activeRun.recordedJfrEvents += RecordingFile.readAllEvents(activeRecordingPath).stream()
						.filter(recordedEvent -> recordedEvent.getEventType().getName().equals("devclientautomation.LinkedStorageWorkloadTick")).count();
				activeRun.jfrVerificationPending = false;
			}
			return "{\"ok\":true,\"outputPath\":" + quote(activeRecordingPath.toString()) + "}";
		} catch (IOException e) {
			throw new IllegalStateException("Could not inspect linked-storage performance JFR recording", e);
		} finally {
			activeRecording.close();
			activeRecording = null;
			activeRecordingPath = null;
		}
	}

	private static void cleanup(StressRun run) {
		ServerLevel level = run.player.level();
		run.player.closeContainer();
		run.player.getInventory().clearContent();
		run.player.getInventory().setChanged();
		level.getEntitiesOfClass(ItemEntity.class, new AABB(run.player.blockPosition()).inflate(8), entity -> entity.getItem().is(Items.DIAMOND))
				.forEach(ItemEntity::discard);
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		run.secondaryEndpointIds.forEach(endpointId -> manager.unregisterEndpoint(run.groupId, endpointId));
		manager.discardUnboundGroup(run.groupId, run.primaryEndpointId);
	}

	private static LinkedStorageEndpointData requireEndpoint(ItemStack stack, String name) {
		LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		if (endpoint == null) {
			throw new IllegalStateException("Linked-storage performance " + name + " has no endpoint identity");
		}
		return endpoint;
	}

	private static int count(InventoryHandler inventory, net.minecraft.world.item.Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.size(); slot++) {
			if (inventory.getStackInSlot(slot).is(item)) {
				count += inventory.getStackInSlot(slot).getCount();
			}
		}
		return count;
	}

	private static JsonObject parseRequest(HttpExchange exchange) throws IOException {
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		return body.isBlank() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject();
	}

	private static String string(JsonObject request, String name, String fallback) {
		return request.has(name) ? request.get(name).getAsString() : fallback;
	}
	private static int integer(JsonObject request, String name, int fallback) {
		return request.has(name) ? request.get(name).getAsInt() : fallback;
	}
	private static String quote(String value) {
		return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
	private static void send(HttpExchange exchange, String response) throws IOException {
		byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(bytes);
		}
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
		private long recordedJfrEvents;
		private int expectedJfrEvents;
		private boolean jfrVerificationPending;
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
