package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Synthetic chunk block-entity lifecycle fixture, not ticket eviction or a disk round trip. */
public final class ReloadChunkBlockEntitiesAction extends TypedRegressionAction<ReloadChunkBlockEntitiesAction.Arguments> {
	public record Arguments(int x, int y, int z, boolean assertControllerEmpty) {
	}

	public ReloadChunkBlockEntitiesAction() {
		super("fixture.chunk.reloadBlockEntities", "2", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offset", "assertControllerEmpty"), description);
		boolean assertControllerEmpty = false;
		if (args.has("assertControllerEmpty")) {
			if (!args.get("assertControllerEmpty").isJsonPrimitive() || !args.get("assertControllerEmpty").getAsJsonPrimitive().isBoolean()) {
				throw new IllegalArgumentException("Expected boolean assertControllerEmpty on " + description);
			}
			assertControllerEmpty = args.get("assertControllerEmpty").getAsBoolean();
		}
		JsonObject positionArgs = args.deepCopy();
		positionArgs.remove("assertControllerEmpty");
		positionArgs.addProperty("block", "minecraft:air");
		var position = new SetFixtureBlockAction().parseArguments(positionArgs, description);
		return new Arguments(position.x(), position.y(), position.z(), assertControllerEmpty);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						var level = player.serverLevel();
						var position = context.standingPosition().south(2).offset(arguments.x(), arguments.y(), arguments.z());
						var controller = level.getBlockEntity(context.target());
						var chunk = level.getChunkAt(position);
						if (!(controller instanceof ControllerBlockEntityBase storageController)
								|| chunk.getPos().equals(level.getChunkAt(context.target()).getPos())) {
							throw new IllegalStateException("Reload requires a live controller in a different chunk");
						}
						var oldEntities = new ArrayList<>(chunk.getBlockEntities().values());
						if (oldEntities.isEmpty()) {
							throw new IllegalStateException("Reload chunk contains no block entities");
						}
						var origin = context.standingPosition();
						for (var entity : oldEntities) {
							var entityPos = entity.getBlockPos();
							if (Math.abs(entityPos.getX() - origin.getX()) > 3 || Math.abs(entityPos.getZ() - origin.getZ()) > 3
									|| entityPos.getY() < origin.getY() || entityPos.getY() > origin.getY() + 5) {
								throw new IllegalStateException("Reload chunk contains a block entity outside the regression cell at " + entityPos);
							}
						}
						var tags = new ArrayList<net.minecraft.nbt.CompoundTag>();
						Set<ItemStack> stacks = new HashSet<>();
						for (var entity : oldEntities) {
							context.journal().recordBlock(level, entity.getBlockPos());
							tags.add(entity.saveWithFullMetadata(level.registryAccess()));
							if (arguments.assertControllerEmpty() && entity instanceof IControllableStorage storage) {
								var inventory = storage.getStorageWrapper().getInventoryHandler();
								for (int slot = 0; slot < inventory.getSlots(); slot++) {
									var stack = inventory.getStackInSlot(slot);
									if (!stack.isEmpty()) {
										stacks.add(stack.copyWithCount(1));
									}
								}
							}
						}
						if (arguments.assertControllerEmpty() && stacks.isEmpty()) {
							throw new IllegalStateException("Controller unload assertion requires a populated inventory");
						}
						// Keep strong references until replacement to make stale controller handlers observable.
						chunk.clearAllBlockEntities();
						if (arguments.assertControllerEmpty()) {
							for (var stack : stacks) {
								for (int slot = 0; slot < storageController.getSlots(); slot++) {
									var extracted = storageController.extractItem(slot, 1, true);
									if (!extracted.isEmpty() && ItemStack.isSameItemSameComponents(stack, extracted)) {
										throw new IllegalStateException("Controller retained accessible inventory after block-entity unload for " + stack);
									}
								}
							}
							context.putDiagnostic("controllerEmptyDuringUnload", true);
						}
						for (int index = 0; index < oldEntities.size(); index++) {
							var oldEntity = oldEntities.get(index);
							var replacement = BlockEntity.loadStatic(oldEntity.getBlockPos(), level.getBlockState(oldEntity.getBlockPos()), tags.get(index),
									level.registryAccess());
							if (!oldEntity.isRemoved() || replacement == null || replacement == oldEntity) {
								throw new IllegalStateException("Chunk lifecycle did not replace a removed block entity");
							}
							chunk.addAndRegisterBlockEntity(replacement);
						}
						if (level.getBlockEntity(context.target()) != controller || controller.isRemoved()) {
							throw new IllegalStateException("Controller did not remain loaded during chest chunk lifecycle");
						}
						context.putDiagnostic("reloadedChunk", chunk.getPos().toString());
						context.putDiagnostic("replacedBlockEntities", oldEntities.size());
					});
				}
				return await(future);
			}
		};
	}
}
