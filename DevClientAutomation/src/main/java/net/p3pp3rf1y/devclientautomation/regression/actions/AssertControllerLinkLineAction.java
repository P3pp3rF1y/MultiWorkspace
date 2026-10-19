package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertControllerLinkLineAction extends TypedRegressionAction<AssertControllerLinkLineAction.Arguments> {
	public record Arguments(String phase) {
	}

	public AssertControllerLinkLineAction() {
		super("assert.controllerLinkLine", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("phase"), description);
		String phase = requireString(args, "phase", description);
		if (!phase.equals("beforeBreak") && !phase.equals("afterBreak")) {
			throw new IllegalArgumentException("Argument phase on " + description + " must be beforeBreak or afterBreak");
		}
		return new Arguments(phase);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		@Nullable
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					BlockPos center = context.standingPosition().south(2);
					BlockPos controllerPos = center.west(2);
					BlockPos first = center.west(3);
					BlockPos linkPos = center.east();
					ServerLevel level = player.serverLevel();
					boolean afterBreak = arguments.phase().equals("afterBreak");
					if (!(level.getBlockEntity(controllerPos) instanceof ControllerBlockEntity controller)
							|| !(level.getBlockEntity(linkPos) instanceof StorageLinkBlockEntity link)) {
						throw new IllegalStateException("Controller or storage link is missing");
					}
					if (!level.getBlockState(center).is(Blocks.AIR)
							|| (afterBreak ? !level.getBlockState(first).isAir() : !level.getBlockState(first).is(ModBlocks.BARREL.get()))) {
						throw new IllegalStateException("Controller link line does not match " + arguments.phase());
					}
					context.putDiagnostic("linkedBlocks." + arguments.phase(), controller.getLinkedBlocks().toString());
					context.putDiagnostic("linkRegistered." + arguments.phase(), controller.getLinkedBlocks().contains(linkPos));
					context.putDiagnostic("connectedStorages." + arguments.phase(), controller.getStoragePositions().toString());
					context.putDiagnostic("linkController." + arguments.phase(), link.getControllerPos().toString());
					if (!link.getControllerPos().filter(controllerPos::equals).isPresent()) {
						throw new IllegalStateException("Storage link lost its controller connection " + arguments.phase());
					}
					for (int x : afterBreak ? new int[]{-1, 2, 3} : new int[]{-3, -1, 2, 3}) {
						BlockPos pos = center.east(x);
						if (!(level.getBlockEntity(pos) instanceof StorageBlockEntity storage)
								|| !storage.getControllerPos().filter(controllerPos::equals).isPresent() || !controller.isStorageConnected(pos)) {
							throw new IllegalStateException("Barrel at line offset " + x + " is disconnected " + arguments.phase());
						}
					}
					if (afterBreak && controller.isStorageConnected(first)) {
						throw new IllegalStateException("Removed barrel is still registered with the controller");
					}
					if (!controller.getLinkedBlocks().contains(linkPos)) {
						throw new IllegalStateException("Storage link is missing from the controller's linked blocks " + arguments.phase());
					}
				});
			}
			return await(future);
		}
	}
}
