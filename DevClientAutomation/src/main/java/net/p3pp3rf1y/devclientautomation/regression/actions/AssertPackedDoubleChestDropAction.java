package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertPackedDoubleChestDropAction extends TypedRegressionAction<AssertPackedDoubleChestDropAction.Arguments> {
	public record Arguments(String item, int count, String upgrade) {
	}

	public AssertPackedDoubleChestDropAction() {
		super("assert.packedDoubleChestDrop", "1", Set.of(CLIENT_CAPABILITY, "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item", "count", "upgrade"), description);
		return new Arguments(requireString(args, "item", description), requirePositiveInt(args, "count", description),
				requireString(args, "upgrade", description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						var level = player.serverLevel();
						if (!level.isEmptyBlock(context.target()) || !level.isEmptyBlock(context.target().south())) {
							throw RegressionActionFailure.assertion("Expected both halves of the dry packed double chest to be removed");
						}
						var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(context.target()).inflate(3));
						if (drops.size() != 1) {
							throw RegressionActionFailure
									.assertion("Expected one packed double chest drop, got " + drops.stream().map(ItemEntity::getItem).toList());
						}
						var stack = drops.getFirst().getItem();
						if (stack.getCount() != 1 || !(stack.getItem() instanceof ChestBlockItem) || !ChestBlockItem.isDoubleChest(stack)
								|| !WoodStorageBlockItem.isPacked(stack)) {
							throw RegressionActionFailure.assertion("Expected one packed double chest, got " + stack);
						}
						var wrapper = StackStorageWrapper.fromStack(level.registryAccess(), stack.copy());
						if (!wrapper.hasContents()) {
							throw RegressionActionFailure.assertion("Packed double chest drop has no stored contents");
						}
						var stored = wrapper.getInventoryHandler().getStackInSlot(0);
						var upgrade = wrapper.getUpgradeHandler().getStackInSlot(0);
						if (!BuiltInRegistries.ITEM.getKey(stored.getItem()).equals(ResourceLocation.parse(arguments.item()))
								|| stored.getCount() != arguments.count()
								|| !BuiltInRegistries.ITEM.getKey(upgrade.getItem()).equals(ResourceLocation.parse(arguments.upgrade()))
								|| upgrade.getCount() != 1) {
							throw RegressionActionFailure.assertion("Packed drop inventory slot 0: " + stored + "; upgrade slot 0: " + upgrade);
						}
					});
				}
				return await(future);
			}
		};
	}
}
