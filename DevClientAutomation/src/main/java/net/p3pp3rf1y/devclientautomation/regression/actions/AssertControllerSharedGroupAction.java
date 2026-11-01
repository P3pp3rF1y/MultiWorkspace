package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AssertControllerSharedGroupAction extends TypedRegressionAction<AssertControllerSharedGroupAction.Arguments> {
	public record Arguments(int members, int slots, int offsetZ, boolean requireRemoteMembership) {
	}
	public AssertControllerSharedGroupAction() {
		super("assert.controllerSharedGroup", "2", Set.of("minecraft:client", "sophisticatedstorage"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("members", "slots", "offsetZ", "requireRemoteMembership"), description);
		return new Arguments(optionalPositiveInt(args, "members", 2, description), optionalPositiveInt(args, "slots", 27, description),
				optionalNonNegativeInt(args, "offsetZ", 0, description), optionalBoolean(args, "requireRemoteMembership", false, description));
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
							throw RegressionActionFailure.infrastructure("Fixture controller is missing");
						}
						CompoundTag saved = controller.getUpdateTag();
						if (saved.getList("storageKeys", Tag.TAG_COMPOUND).size() != 1
								|| saved.getList("storageMemberKeys", Tag.TAG_COMPOUND).size() != arguments.members()
								|| controller.getSlots() != arguments.slots()) {
							throw RegressionActionFailure
									.assertion("Expected one canonical controller range and " + arguments.members() + " visual members: " + saved);
						}
						UUID groupId = null;
						for (int z = 0; z < arguments.members(); z++) {
							BlockPos storagePos = context.target().south(z + arguments.offsetZ());
							if (!(player.serverLevel().getBlockEntity(storagePos) instanceof StorageBlockEntity storage)
									|| storage.getControllerPos().filter(pos::equals).isEmpty()) {
								throw RegressionActionFailure.assertion("Loaded visual member lost controller membership");
							}
							if (arguments.requireRemoteMembership()) {
								if (!storage.isLinked() || !controller.isStorageConnected(storagePos) || !controller.getLinkedBlocks().contains(storagePos)) {
									throw RegressionActionFailure.assertion("Linked visual member lost remote controller membership");
								}
								if (storage.getLinkedStorageEndpointData() == null) {
									throw RegressionActionFailure.assertion("Linked visual member lost its shared group endpoint");
								}
								if (groupId == null) {
									groupId = storage.getLinkedStorageEndpointData().groupId();
								} else if (!groupId.equals(storage.getLinkedStorageEndpointData().groupId())) {
									throw RegressionActionFailure.assertion("Linked visual members do not share one group");
								}
							}
						}
					});
				}
				return await(future);
			}
		};
	}
}
