package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.sun.management.ThreadMXBean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionInventoryPart;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Explicit developer measurements, without timing thresholds. */
public final class ProfileCompressionAction extends TypedRegressionAction<ProfileCompressionAction.Arguments> {
	public record Arguments(boolean worldBatch) {
	}

	public ProfileCompressionAction() {
		super("profile.storage.compression", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("worldBatch"), description);
		return new Arguments(optionalBoolean(args, "worldBatch", false, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		if (arguments.worldBatch()) {
			return new CompressionWorldBatchProfile(context);
		}
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> profile(context));
				}
				return await(future);
			}
		};
	}

	private static void profile(RegressionExecutionContext context) {
		List<Item> chain = List.of(Items.CHARCOAL, Items.FLINT, Items.BRICK, Items.NETHER_BRICK, Items.STICK, Items.BOWL, Items.PAPER, Items.FEATHER,
				Items.LEATHER);
		var config = Config.SERVER.compressionUpgrade;
		List<String> original = config.additionalDecompressibleItems.get();
		List<String> entries = new ArrayList<>(original);
		for (int slot = 1; slot < chain.size(); slot++) {
			int ratio = new int[]{4, 8, 9}[(slot - 1) % 3];
			entries.add(BuiltInRegistries.ITEM.getKey(chain.get(slot - 1)) + "=" + ratio + "x" + BuiltInRegistries.ITEM.getKey(chain.get(slot)));
		}
		Map<String, Object> results = new LinkedHashMap<>();
		try {
			config.additionalDecompressibleItems.set(entries);
			config.clearCache();
			for (int slots : new int[]{3, 5, 9}) {
				// Detached production wrapper includes real NBT persistence and inventory listeners, without world/network scheduling.
				BarrelBlockEntity barrel = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState());
				var wrapper = barrel.getStorageWrapper();
				var inventory = wrapper.getInventoryHandler();
				inventory.setSlotStack(0, new ItemStack(chain.get(0), 4));
				CompressionInventoryPart part = new CompressionInventoryPart(inventory, new SlotRange(0, slots),
						() -> wrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class));
				inventory.getInventoryPartitioner().addInventoryPart(0, slots, part);
				ItemStack offered = new ItemStack(chain.get(slots - 1));
				if (!inventory.getStackInSlot(slots - 1).is(offered.getItem())) {
					throw RegressionActionFailure.assertion("Benchmark chain did not resolve " + slots + " slots");
				}
				long[] notifications = {0};
				inventory.addListener(slot -> notifications[0]++);
				Runnable pair = () -> {
					ItemStack extracted = inventory.extractItem(slots - 1, 1, false);
					if (extracted.getCount() != 1 || !inventory.insertItem(slots - 1, offered, false).isEmpty()) {
						throw RegressionActionFailure.assertion("Benchmark borrow/carry failed");
					}
				};
				for (boolean persistent : new boolean[]{false, true}) {
					inventory.setPersistent(persistent);
					results.put(slots + "-slots-persistent-" + persistent, measure(pair, 2));
				}
				results.put(slots + "-slots-simulation", measure(() -> inventory.extractItem(slots - 1, 1, true), 1));
				results.put(slots + "-slots-count-refresh", measure(part::onSlotLimitChange, 1));
				ItemStack stackOffered = offered.copyWithCount(64);
				results.put(slots + "-slots-stack-persistent-true", measure(() -> {
					if (inventory.extractItem(slots - 1, 64, false).getCount() != 64 || !inventory.insertItem(slots - 1, stackOffered, false).isEmpty()) {
						throw RegressionActionFailure.assertion("Benchmark stack transfer failed");
					}
				}, 2));
				results.put(slots + "-slots-empty-noop", measure(() -> inventory.insertItem(slots - 1, ItemStack.EMPTY, false), 1));
				results.put(slots + "-slots-listener-calls", notifications[0]);
				if (inventory.getSlotStack(0).getCount() != 4) {
					throw RegressionActionFailure.assertion("Benchmark did not conserve contents");
				}
			}
		} finally {
			config.additionalDecompressibleItems.set(original);
			config.clearCache();
		}
		context.putDiagnostic("compressionProfile", results);
		context.putDiagnostic("compressionProfileScope",
				"Warm isolated production inventory/tracker/listeners/NBT; detached wrapper excludes world/network and third-party costs. Samples are batches of 100 operations, not individual-operation tail latency.");
	}

	static Map<String, Object> measure(Runnable operation, int operationsPerCall) {
		for (int warm = 0; warm < 5000; warm++) {
			operation.run();
		}
		ThreadMXBean bean = ManagementFactory.getPlatformMXBean(ThreadMXBean.class);
		long thread = Thread.currentThread().getId();
		long allocatedBefore = bean.getThreadAllocatedBytes(thread);
		long[] samples = new long[100];
		for (int sample = 0; sample < samples.length; sample++) {
			long start = System.nanoTime();
			for (int batch = 0; batch < 100; batch++) {
				operation.run();
			}
			samples[sample] = (System.nanoTime() - start) / (100 * operationsPerCall);
		}
		long allocated = bean.getThreadAllocatedBytes(thread) - allocatedBefore;
		Arrays.sort(samples);
		return Map.of("batchP50NsPerOperation", samples[50], "batchP95NsPerOperation", samples[95], "batchP99NsPerOperation", samples[99], "bytesPerOperation",
				allocated / (10000 * operationsPerCall), "operations", 10000 * operationsPerCall);
	}
}
