package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertControllerLinkGroupsAction extends TypedRegressionAction<AssertControllerLinkGroupsAction.Arguments> {
	public record Arguments(String layout, String phase) {
	}

	public AssertControllerLinkGroupsAction() {
		super("assert.controllerLinkGroups", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("layout", "phase"), description);
		String layout = requireString(args, "layout", description);
		String phase = requireString(args, "phase", description);
		if (!layout.equals("shared") && !layout.equals("separate")) {
			throw new IllegalArgumentException("Argument layout on " + description + " must be shared or separate");
		}
		if (!phase.equals("beforeBreak") && !phase.equals("afterBreak")) {
			throw new IllegalArgumentException("Argument phase on " + description + " must be beforeBreak or afterBreak");
		}
		return new Arguments(layout, phase);
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
					BlockPos controllerPos = center.west(3);
					ServerLevel level = player.serverLevel();
					if (!(level.getBlockEntity(controllerPos) instanceof ControllerBlockEntity controller)) {
						throw new IllegalStateException("Controller is missing from link groups fixture");
					}
					context.putDiagnostic("linkedBlocks." + arguments.layout() + "." + arguments.phase(), controller.getLinkedBlocks().toString());
					context.putDiagnostic("connectedStorages." + arguments.layout() + "." + arguments.phase(), controller.getStoragePositions().toString());
					for (int x : new int[]{-1, 3}) {
						BlockPos linkPos = center.east(x).north();
						if (!(level.getBlockEntity(linkPos) instanceof StorageLinkBlockEntity link)
								|| !link.getControllerPos().filter(controllerPos::equals).isPresent() || !controller.getLinkedBlocks().contains(linkPos)) {
							throw new IllegalStateException("Storage link at offset " + x + " is disconnected " + arguments.phase());
						}
					}
					boolean afterBreak = arguments.phase().equals("afterBreak");
					int removed = arguments.layout().equals("shared") ? 1 : 0;
					if (!level.getBlockState(center.west(2)).isAir() || (arguments.layout().equals("separate") && !level.getBlockState(center.east()).isAir())
							|| (afterBreak && !level.getBlockState(center.east(removed)).isAir())) {
						throw new IllegalStateException("Barrel groups do not match " + arguments.layout() + " " + arguments.phase());
					}
					for (int x : new int[]{-1, 0, 1, 2, 3}) {
						boolean expected = (arguments.layout().equals("shared") || x != 1) && (!afterBreak || x != removed);
						BlockPos pos = center.east(x);
						if (expected) {
							if (!level.getBlockState(pos).is(ModBlocks.BARREL.get()) || !(level.getBlockEntity(pos) instanceof StorageBlockEntity storage)
									|| !storage.getControllerPos().filter(controllerPos::equals).isPresent() || !controller.isStorageConnected(pos)) {
								throw new IllegalStateException("Barrel at offset " + x + " is disconnected " + arguments.layout() + " " + arguments.phase());
							}
						} else if (!level.getBlockState(pos).isAir() || controller.isStorageConnected(pos)) {
							throw new IllegalStateException("Unexpected barrel at offset " + x + " " + arguments.layout() + " " + arguments.phase());
						}
					}
				});
			}
			return await(future);
		}
	}
}
