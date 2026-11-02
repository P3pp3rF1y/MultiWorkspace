package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageIOBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Exercises public automation handlers; lifecycle coverage is an explicit serialized round trip, not chunk eviction. */
public final class ExerciseCompressionAutomationAction extends TypedRegressionAction<ExerciseCompressionAutomationAction.Arguments> {
	public record Arguments() {
	}

	public ExerciseCompressionAutomationAction() {
		super("exercise.storage.compressionAutomation", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(), description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ServerLevel level = player.level();
						if (!(level.getBlockEntity(context.target()) instanceof StorageBlockEntity storage)) {
							throw RegressionActionFailure.infrastructure("Missing compression storage");
						}
						BlockPos controllerPos = context.target().east();
						BlockPos inputPos = controllerPos.east();
						BlockPos ioPos = controllerPos.south();
						for (BlockPos pos : List.of(controllerPos, inputPos, ioPos)) {
							if (!level.isEmptyBlock(pos)) {
								throw RegressionActionFailure.infrastructure("Automation fixture position is occupied: " + pos);
							}
							context.journal().recordBlock(level, pos);
						}
						level.setBlockAndUpdate(controllerPos, ModBlocks.CONTROLLER.get().defaultBlockState());
						level.setBlockAndUpdate(inputPos, ModBlocks.STORAGE_INPUT.get().defaultBlockState());
						level.setBlockAndUpdate(ioPos, ModBlocks.STORAGE_IO.get().defaultBlockState());
						ControllerBlockEntity controller = (ControllerBlockEntity) level.getBlockEntity(controllerPos);
						controller.searchAndAddBoundables();
						StorageIOBlockEntity input = (StorageIOBlockEntity) level.getBlockEntity(inputPos);
						StorageIOBlockEntity io = (StorageIOBlockEntity) level.getBlockEntity(ioPos);
						input.linkToController(controllerPos);
						io.linkToController(controllerPos);
						List<ResourceHandler<ItemResource>> handlers = List.of(storage.getExternalItemHandler(null),
								controller.getExternalItemResourceHandler(), input.getExternalItemResourceHandler(null),
								io.getExternalItemResourceHandler(null));
						for (ResourceHandler<ItemResource> handler : handlers) {
							RuntimeOperations.cycle(storage, handler);
						}
						var saved = storage.getUpdateTag(level.registryAccess());
						storage.onChunkUnloaded();
						storage.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), saved));
						storage.onLoad();
						RuntimeOperations.cycle(storage, controller.getExternalItemResourceHandler());
						var inventory = storage.getStorageWrapper().getInventoryHandler();
						int limit = inventory.getBaseCapacity(ItemResource.of(new ItemStack(Items.IRON_BLOCK)));
						for (int remaining = limit; remaining > 0; remaining -= Math.min(64, remaining)) {
							RuntimeOperations.insert(controller.getExternalItemResourceHandler(), new ItemStack(Items.IRON_NUGGET, Math.min(64, remaining)),
									false);
						}
						long full = AssertCompressionAction.assertContents(inventory);
						if (full != 91L * limit) {
							throw RegressionActionFailure.assertion("Controller did not fill all compression representations");
						}
						for (VoidType type : List.of(VoidType.SLOT_OVERFLOW, VoidType.STORAGE_OVERFLOW)) {
							storage.getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(VoidUpgradeWrapper.class).getFirst().setVoidType(type);
							for (ResourceHandler<ItemResource> handler : List.of(controller.getExternalItemResourceHandler(),
									input.getExternalItemResourceHandler(null), io.getExternalItemResourceHandler(null))) {
								for (int repeat = 0; repeat < 3; repeat++) {
									RuntimeOperations.insert(handler, new ItemStack(Items.IRON_NUGGET, 64), true);
									RuntimeOperations.insert(handler, new ItemStack(Items.IRON_NUGGET, 64), false);
									if (AssertCompressionAction.assertContents(inventory) != full) {
										throw RegressionActionFailure.assertion("Overflow mutated full contents");
									}
								}
							}
						}
						ResourceHandler<ItemResource> fullHandler = controller.getExternalItemResourceHandler();
						ItemStack overflow = new ItemStack(Items.IRON_NUGGET, 64);
						context.putDiagnostic("compressionFullVoidProfile",
								ProfileCompressionAction.measure(() -> RuntimeOperations.insert(fullHandler, overflow, false), 1));
						context.putDiagnostic("compressionFullVoidSimulationProfile",
								ProfileCompressionAction.measure(() -> RuntimeOperations.insert(fullHandler, overflow, true), 1));
						context.putDiagnostic("compressionAutomation",
								"direct/controller/input/IO simulated+real insertion, both overflow modes; serialized lifecycle round trip");
					});
				}
				return await(future);
			}
		};
	}

	private static final class RuntimeOperations {
		private static void cycle(StorageBlockEntity storage, ResourceHandler<ItemResource> handler) {
			var inventory = storage.getStorageWrapper().getInventoryHandler();
			long before = AssertCompressionAction.assertContents(inventory);
			if (ProfileCompressionAction.extract(inventory, 2, new ItemStack(Items.IRON_NUGGET), 64, false) != 64) {
				throw RegressionActionFailure.assertion("Could not withdraw automation cycle contents");
			}
			insert(handler, new ItemStack(Items.IRON_NUGGET, 64), true);
			if (AssertCompressionAction.assertContents(inventory) != before - 64) {
				throw RegressionActionFailure.assertion("Simulated automation insert mutated contents");
			}
			insert(handler, new ItemStack(Items.IRON_NUGGET, 64), false);
			if (AssertCompressionAction.assertContents(inventory) != before) {
				throw RegressionActionFailure.assertion("Automation cycle did not conserve contents");
			}
		}

		private static void insert(ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate) {
			try (Transaction tx = Transaction.openRoot()) {
				int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
				if (inserted != stack.getCount()) {
					throw RegressionActionFailure
							.assertion("Automation refused " + stack.copyWithCount(stack.getCount() - inserted) + " (simulate=" + simulate + ")");
				}
				if (!simulate) {
					tx.commit();
				}
			}
		}
	}
}
