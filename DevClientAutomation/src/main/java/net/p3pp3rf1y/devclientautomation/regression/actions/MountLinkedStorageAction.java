package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssembleRailType;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstoragecreateintegration.storage.MountedSophisticatedStorage;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class MountLinkedStorageAction extends TypedRegressionAction<MountLinkedStorageAction.Arguments> {
	public record Arguments(String saveAs) {
	}

	public MountLinkedStorageAction() {
		super("fixture.create.mountLinkedStorage", "1", Set.of("minecraft:client", "sophisticatedstoragecreateintegration"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("saveAs"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "saveAs", description)));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.MOUNTED_STORAGE);
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
					ServerLevel level = player.serverLevel();
					BlockPos storagePos = context.target();
					if (!(level.getBlockEntity(storagePos) instanceof StorageBlockEntity block) || !block.isLinkedStorage()) {
						throw new IllegalStateException("Create fixture requires a placed linked storage block");
					}
					LinkedStorageEndpointData endpoint = block.getLinkedStorageEndpointData();
					BlockPos assemblerPos = storagePos.below();
					context.journal().recordBlock(level, storagePos);
					context.journal().recordBlock(level, assemblerPos.below());
					context.journal().recordBlock(level, assemblerPos.west());
					context.journal().recordBlock(level, assemblerPos);
					level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
					level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
					level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
							.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
					Minecart cart = new Minecart(level, assemblerPos.getX() + 0.5D, assemblerPos.getY(), assemblerPos.getZ() + 0.5D);
					context.journal().recordCreatedEntity(cart);
					if (!level.addFreshEntity(cart)) {
						throw new IllegalStateException("Could not spawn Create assembly cart");
					}
					if (!(level.getBlockEntity(assemblerPos) instanceof CartAssemblerBlockEntity assembler)) {
						throw new IllegalStateException("Create cart assembler block entity missing");
					}
					assembler.tryAssemble(cart);
					AbstractContraptionEntity entity = cart.getPassengers().stream().filter(AbstractContraptionEntity.class::isInstance)
							.map(AbstractContraptionEntity.class::cast).findFirst()
							.orElseThrow(() -> new IllegalStateException("Create cart did not assemble a storage contraption"));
					context.journal().recordCreatedEntity(entity);
					BlockPos localPos = ContraptionHelper.getMountedItemStorages(entity).keySet().stream()
							.filter(pos -> ContraptionHelper.getMountedStorage(entity, pos) instanceof MountedSophisticatedStorage).findFirst()
							.orElseThrow(() -> new IllegalStateException("Create contraption has no mounted sophisticated storage"));
					context.values().put(arguments.saveAs(), MountedLinkedStorageHandle.class,
							new MountedLinkedStorageHandle(entity.getUUID(), level.dimension(), localPos, endpoint));
				});
			}
			return await(future);
		}
	}
}
