package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.sun.management.ThreadMXBean;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionInventoryPart;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Fifty real storages per sample, with at most one sample in each server game tick. */
final class CompressionWorldBatchProfile extends TickRegressionActionExecution {
	private static final int STORAGE_COUNT = 50;
	private static final int WARMUP_TICKS = 50;
	private static final int SAMPLE_TICKS = 100;
	private final List<InventoryHandler> inventories = new ArrayList<>();
	private final List<CompressionInventoryPart> parts = new ArrayList<>();
	private final Map<String, Object> results = new LinkedHashMap<>();
	private final long[] nanos = new long[SAMPLE_TICKS];
	private final long[] allocations = new long[SAMPLE_TICKS];
	private CompletableFuture<Void> future;
	private long lastGameTick = Long.MIN_VALUE;
	private long firstSampleTick;
	private int phase;
	private int phaseTicks;
	private boolean initialized;
	private boolean complete;
	private long listenerCalls;
	private long listenerCallsBefore;

	CompressionWorldBatchProfile(RegressionExecutionContext context) {
		super(context);
	}

	@Override
	public RegressionActionProgress tick() {
		if (future != null) {
			RegressionActionProgress progress = await(future);
			if (progress != RegressionActionProgress.PASSED) {
				return progress;
			}
			future = null;
			if (complete) {
				return RegressionActionProgress.PASSED;
			}
		}
		future = context.onServer(this::serverTick);
		return RegressionActionProgress.WAITING;
	}

	private void serverTick(ServerPlayer player) {
		long tick = player.level().getGameTime();
		if (tick == lastGameTick) {
			return;
		}
		lastGameTick = tick;
		if (!initialized) {
			initialize(player);
			initialized = true;
			return;
		}
		ThreadMXBean bean = ManagementFactory.getPlatformMXBean(ThreadMXBean.class);
		long thread = Thread.currentThread().threadId();
		if (phaseTicks == WARMUP_TICKS) {
			firstSampleTick = tick;
			listenerCallsBefore = listenerCalls;
		}
		long allocatedBefore = bean.getThreadAllocatedBytes(thread);
		long start = System.nanoTime();
		updateAll();
		long elapsed = System.nanoTime() - start;
		long allocated = bean.getThreadAllocatedBytes(thread) - allocatedBefore;
		if (player.level().getGameTime() != tick) {
			throw RegressionActionFailure.assertion("Fifty-storage batch crossed a server game tick");
		}
		if (phaseTicks >= WARMUP_TICKS) {
			int sample = phaseTicks - WARMUP_TICKS;
			nanos[sample] = elapsed;
			allocations[sample] = allocated;
		}
		phaseTicks++;
		if (phaseTicks == WARMUP_TICKS + SAMPLE_TICKS) {
			finishPhase(tick);
		}
	}

	private void initialize(ServerPlayer player) {
		var level = player.serverLevel();
		BlockPos origin = context.standingPosition().south(2);
		List<BlockPos> positions = new ArrayList<>();
		for (int index = 0; index < STORAGE_COUNT; index++) {
			BlockPos pos = origin.offset(index % 5 - 2, index / 25 + 1, index / 5 % 5 - 2);
			if (!level.isEmptyBlock(pos)) {
				throw RegressionActionFailure.infrastructure("Fifty-storage fixture position is occupied: " + pos);
			}
			positions.add(pos);
			context.journal().recordBlock(level, pos);
		}
		for (BlockPos pos : positions) {
			level.setBlockAndUpdate(pos, ModBlocks.LIMITED_GOLD_BARREL_3.get().defaultBlockState());
			if (!(level.getBlockEntity(pos) instanceof StorageBlockEntity storage)) {
				throw RegressionActionFailure.infrastructure("Fifty-storage fixture has no storage block entity");
			}
			var wrapper = storage.getStorageWrapper();
			wrapper.getUpgradeHandler().setStackInSlot(2, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
			InventoryHandler inventory = wrapper.getInventoryHandler();
			int limit = inventory.getBaseStackLimit(new ItemStack(Items.IRON_BLOCK));
			inventory.setSlotStack(0, new ItemStack(Items.IRON_BLOCK, limit));
			inventory.setSlotStack(1, new ItemStack(Items.IRON_INGOT, limit));
			inventory.setSlotStack(2, new ItemStack(Items.IRON_NUGGET, limit));
			wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.COMPRESSION_UPGRADE.get()));
			storage.toggleLock();
			inventory.setPersistent(true);
			if (!(inventory.getInventoryPartitioner().getPartBySlot(0) instanceof CompressionInventoryPart part)) {
				throw RegressionActionFailure.assertion("Fifty-storage compression part missing");
			}
			inventories.add(inventory);
			parts.add(part);
			inventory.addListener(slot -> listenerCalls++);
		}
	}

	private void updateAll() {
		if (phase == 0) {
			for (CompressionInventoryPart part : parts) {
				part.onSlotLimitChange();
			}
			return;
		}
		int amount = phase == 1 || phase == 3 ? 1 : 64;
		for (InventoryHandler inventory : inventories) {
			ItemStack extracted = inventory.extractItem(2, amount, false);
			if (extracted.getCount() != amount || !inventory.insertItem(2, extracted, false).isEmpty()) {
				throw RegressionActionFailure.assertion("Fifty-storage transfer did not conserve contents");
			}
		}
	}

	private void finishPhase(long tick) {
		for (InventoryHandler inventory : inventories) {
			int limit = inventory.getBaseStackLimit(new ItemStack(Items.IRON_BLOCK));
			long expected = phase < 3 ? 91L * limit : 81;
			if (AssertCompressionAction.assertContents(inventory) != expected) {
				throw RegressionActionFailure.assertion("Fifty-storage sample changed total contents");
			}
		}
		Arrays.sort(nanos);
		String name = switch (phase) {
			case 0 -> "count-refresh";
			case 1 -> "single-extract-insert";
			case 2 -> "stack-extract-insert";
			case 3 -> "borrow-carry-single-extract-insert";
			default -> "borrow-carry-stack-extract-insert";
		};
		results.put(name, Map.of("storagesPerTick", STORAGE_COUNT, "mutationsPerTick", phase == 0 ? 0 : 2 * STORAGE_COUNT, "sampleTicks", SAMPLE_TICKS,
				"firstSampleGameTick", firstSampleTick, "lastSampleGameTick", tick, "batchP50Ns", nanos[50], "batchP95Ns", nanos[95], "batchP99Ns", nanos[99],
				"allocatedBytesPerBatch", Arrays.stream(allocations).sum() / SAMPLE_TICKS, "listenerCalls", listenerCalls - listenerCallsBefore));
		phase++;
		phaseTicks = 0;
		if (phase == 3) {
			// Separate difficult workload: every withdrawal borrows through all three representations,
			// and its matching insertion carries back to the block slot. Arrangement is outside timing.
			for (InventoryHandler inventory : inventories) {
				inventory.setStackInSlot(2, ItemStack.EMPTY);
				if (!inventory.insertItem(0, new ItemStack(Items.IRON_BLOCK), false).isEmpty() || AssertCompressionAction.assertContents(inventory) != 81) {
					throw RegressionActionFailure.assertion("Could not arrange fifty-storage borrow/carry workload");
				}
			}
		}
		if (phase == 5) {
			context.putDiagnostic("compressionFiftyStorageProfile", results);
			context.putDiagnostic("compressionFiftyStorageProfileScope",
					"50 world-attached locked Limited Barrel III storages with Compression and Stack Tier 2; persistence enabled. One entire batch per distinct server game tick, 50 warmup and 100 sampled ticks per workload. Timings include synchronous world-attached inventory/tracker/listener/NBT work, but not deferred networking, the rest of the server tick, other players or third-party transport.");
			complete = true;
		}
	}
}
