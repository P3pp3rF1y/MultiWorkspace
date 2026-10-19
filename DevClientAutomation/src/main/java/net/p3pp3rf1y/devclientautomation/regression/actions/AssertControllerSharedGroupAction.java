package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertControllerSharedGroupAction extends TypedRegressionAction<AssertControllerSharedGroupAction.Arguments> {
	public record Arguments(int members, int slots, int offsetZ) {
	}
	public AssertControllerSharedGroupAction() {
		super("assert.controllerSharedGroup", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("members", "slots", "offsetZ"), description);
		return new Arguments(optionalPositiveInt(args, "members", 2, description), optionalPositiveInt(args, "slots", 27, description),
				optionalNonNegativeInt(args, "offsetZ", 0, description));
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						BlockPos pos = context.target().east();
						if (!(player.serverLevel().getBlockEntity(pos) instanceof ControllerBlockEntityBase controller)) {
							throw new IllegalStateException("Fixture controller is missing");
						}
						CompoundTag saved = controller.getUpdateTag();
						if (saved.getList("storageKeys", Tag.TAG_COMPOUND).size() != 1
								|| saved.getList("storageMemberKeys", Tag.TAG_COMPOUND).size() != arguments.members()
								|| controller.getSlots() != arguments.slots()) {
							throw new IllegalStateException("Expected one canonical controller range and " + arguments.members() + " visual members: " + saved);
						}
						for (int z = 0; z < arguments.members(); z++) {
							if (!(player.serverLevel().getBlockEntity(context.target().south(z + arguments.offsetZ())) instanceof StorageBlockEntity storage)
									|| storage.getControllerPos().filter(pos::equals).isEmpty()) {
								throw new IllegalStateException("Loaded visual member lost controller membership");
							}
						}
					});
				}
				return await(future);
			}
		};
	}
}
