package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
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
						ServerLevel level = player.serverLevel();
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
						List<IItemHandler> handlers = List.of(itemHandler(storage), itemHandler(controller), itemHandler(input), itemHandler(io));
						for (IItemHandler handler : handlers) {
							RuntimeOperations.cycle(storage, handler);
						}
						// Use the full persistence writer: getUpdateTag() only serializes client-visible wrapper data on 1.20.1.
						CompoundTag saved = new CompoundTag();
						storage.saveAdditional(saved);
						storage.onChunkUnloaded();
						storage.load(saved);
						storage.onLoad();
						RuntimeOperations.cycle(storage, itemHandler(controller));
						var inventory = storage.getStorageWrapper().getInventoryHandler();
						int limit = inventory.getBaseStackLimit(new ItemStack(Items.IRON_BLOCK));
						for (int remaining = limit; remaining > 0; remaining -= Math.min(64, remaining)) {
							RuntimeOperations.insert(itemHandler(controller), new ItemStack(Items.IRON_NUGGET, Math.min(64, remaining)), false);
						}
						long full = AssertCompressionAction.assertContents(inventory);
						if (full != 91L * limit) {
							throw RegressionActionFailure.assertion("Controller did not fill all compression representations");
						}
						for (VoidType type : List.of(VoidType.SLOT_OVERFLOW, VoidType.STORAGE_OVERFLOW)) {
							storage.getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(VoidUpgradeWrapper.class).get(0).setVoidType(type);
							for (IItemHandler handler : List.of(itemHandler(controller), itemHandler(input), itemHandler(io))) {
								for (int repeat = 0; repeat < 3; repeat++) {
									RuntimeOperations.insert(handler, new ItemStack(Items.IRON_NUGGET, 64), true);
									RuntimeOperations.insert(handler, new ItemStack(Items.IRON_NUGGET, 64), false);
									if (AssertCompressionAction.assertContents(inventory) != full) {
										throw RegressionActionFailure.assertion("Overflow mutated full contents");
									}
								}
							}
						}
						IItemHandler fullHandler = itemHandler(controller);
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

	private static IItemHandler itemHandler(BlockEntity entity) {
		return entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
				.orElseThrow(() -> RegressionActionFailure.infrastructure("Missing item handler on " + entity.getBlockPos()));
	}

	private static final class RuntimeOperations {
		private static void cycle(StorageBlockEntity storage, IItemHandler handler) {
			var inventory = storage.getStorageWrapper().getInventoryHandler();
			long before = AssertCompressionAction.assertContents(inventory);
			if (inventory.extractItem(2, 64, false).getCount() != 64) {
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

		private static void insert(IItemHandler handler, ItemStack stack, boolean simulate) {
			ItemStack remainder = stack;
			if (handler instanceof IItemHandlerSimpleInserter inserter) {
				remainder = inserter.insertItem(stack, simulate);
			} else {
				for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
					remainder = handler.insertItem(slot, remainder, simulate);
				}
			}
			if (!remainder.isEmpty()) {
				throw RegressionActionFailure.assertion("Automation refused " + remainder + " (simulate=" + simulate + ")");
			}
		}
	}
}
