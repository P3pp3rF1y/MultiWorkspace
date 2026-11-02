package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Arrangement only. Subsequent steps exercise public gameplay or automation entry points. */
public final class FixtureCompressionAction extends TypedRegressionAction<FixtureCompressionAction.Arguments> {
	public record Arguments(boolean stackUpgrade) {
	}

	public FixtureCompressionAction() {
		super("fixture.storage.compression", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("stackUpgrade"), description);
		return new Arguments(optionalBoolean(args, "stackUpgrade", true, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.serverLevel().getBlockEntity(context.target()) instanceof StorageBlockEntity storage)) {
							throw RegressionActionFailure.infrastructure("Compression fixture requires a storage target");
						}
						context.journal().recordBlock(player.serverLevel(), context.target());
						var wrapper = storage.getStorageWrapper();
						var upgrades = wrapper.getUpgradeHandler();
						if (arguments.stackUpgrade()) {
							upgrades.setStackInSlot(2, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
						}
						var inventory = wrapper.getInventoryHandler();
						int limit = inventory.getBaseStackLimit(new ItemStack(Items.IRON_BLOCK));
						inventory.setSlotStack(0, new ItemStack(Items.IRON_BLOCK, limit));
						inventory.setSlotStack(1, new ItemStack(Items.IRON_INGOT, limit - 1));
						inventory.setSlotStack(2, new ItemStack(Items.IRON_NUGGET, 8));
						upgrades.setStackInSlot(0, new ItemStack(ModItems.COMPRESSION_UPGRADE.get()));
						upgrades.setStackInSlot(1, new ItemStack(ModItems.VOID_UPGRADE.get()));
						var voids = upgrades.getWrappersThatImplement(VoidUpgradeWrapper.class);
						if (voids.size() != 1 || !inventory.getInventoryPartitioner().getPartBySlot(0).getName().equals("compression")) {
							throw RegressionActionFailure.assertion("Compression/Void upgrades were not installed");
						}
						voids.get(0).setVoidType(VoidType.STORAGE_OVERFLOW);
						if (!storage.isLocked()) {
							storage.toggleLock();
						}
						context.putDiagnostic("compressionBaseLimit", limit);
					});
				}
				return await(future);
			}
		};
	}
}
