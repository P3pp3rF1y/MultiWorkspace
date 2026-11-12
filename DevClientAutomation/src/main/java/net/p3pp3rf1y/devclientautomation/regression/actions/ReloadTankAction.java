package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ReloadTankAction extends TypedRegressionAction<PlaceHeldBlockAction.Arguments> {
	public ReloadTankAction() {
		super("fixture.tank.reload", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}
	@Override
	protected PlaceHeldBlockAction.Arguments parseArguments(JsonObject args, String description) {
		return new PlaceHeldBlockAction().parseArguments(args, description);
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, PlaceHeldBlockAction.Arguments args) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ServerLevel level = player.serverLevel();
						BlockPos position = context.target().offset(args.x(), args.y(), args.z());
						if (!(level.getBlockEntity(position) instanceof TankBlockEntity tank))
							throw RegressionActionFailure.infrastructure("Reload requires an existing tank");
						TankBlockEntity primary = tank.getPrimary().orElseThrow();
						List<TankBlockEntity> old = new ArrayList<>();
						List<CompoundTag> saved = new ArrayList<>();
						List<IFluidHandler> retained = new ArrayList<>();
						for (int index = 0; index < primary.getHeight(); index++) {
							TankBlockEntity segment = (TankBlockEntity) level.getBlockEntity(primary.getBlockPos().above(index));
							context.journal().recordBlock(level, segment.getBlockPos());
							old.add(segment);
							saved.add(segment.saveWithFullMetadata());
							IFluidHandler handler = segment.getExternalFluidHandler(null);
							if (handler != null)
								retained.add(handler);
						}
						for (TankBlockEntity segment : old) {
							segment.onChunkUnloaded();
							level.getChunkAt(segment.getBlockPos()).removeBlockEntity(segment.getBlockPos());
						}
						if (retained.stream().anyMatch(handler -> handler.getTanks() != 0))
							throw RegressionActionFailure.assertion("Unloaded tank retained accessible native fluid");
						for (int index = 0; index < old.size(); index++) {
							BlockPos pos = old.get(index).getBlockPos();
							BlockEntity replacement = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved.get(index));
							if (!(replacement instanceof TankBlockEntity))
								throw RegressionActionFailure.infrastructure("Saved tank failed to load");
							level.getChunkAt(pos).addAndRegisterBlockEntity(replacement);
							WorldHelper.notifyBlockUpdate(replacement);
						}
						observe(Map.of("replaced", old.size(), "retainedHandlers", "inaccessible"), Map.of("replaced", saved.size()));
					});
				}
				return await(future);
			}
		};
	}
}
