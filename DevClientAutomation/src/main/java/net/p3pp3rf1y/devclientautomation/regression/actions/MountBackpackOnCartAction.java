package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssembleRailType;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpackscreateintegration.backpack.MountedSophisticatedBackpack;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MountBackpackOnCartAction extends TypedRegressionAction<MountBackpackOnCartAction.Arguments> {
	public record Arguments(String saveAs) {
	}

	public MountBackpackOnCartAction() {
		super("fixture.create.mountBackpack", "1", Set.of("minecraft:client", "sophisticatedbackpackscreateintegration"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("saveAs"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "saveAs", description)));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.UUID);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						var level = player.level();
						BlockPos backpackPos = context.target();
						if (!(level.getBlockEntity(backpackPos) instanceof BackpackBlockEntity)) {
							throw new IllegalStateException("Create fixture requires a placed backpack");
						}
						BlockPos assemblerPos = backpackPos.below();
						context.journal().recordBlock(level, backpackPos);
						context.journal().recordBlock(level, assemblerPos.below());
						context.journal().recordBlock(level, assemblerPos.west());
						context.journal().recordBlock(level, assemblerPos);
						level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
						level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
						level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
								.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
						var cart = EntityType.MINECART.create(level, EntitySpawnReason.COMMAND);
						if (cart == null) {
							throw new IllegalStateException("Could not create Create assembly cart");
						}
						cart.snapTo(assemblerPos.getX() + 0.5D, assemblerPos.getY(), assemblerPos.getZ() + 0.5D, 0.0F, 0.0F);
						context.journal().recordCreatedEntity(cart);
						if (!level.addFreshEntity(cart)) {
							throw new IllegalStateException("Could not spawn Create assembly cart");
						}
						if (!(level.getBlockEntity(assemblerPos) instanceof CartAssemblerBlockEntity assembler)) {
							throw new IllegalStateException("Create cart assembler block entity missing");
						}
						tryAssemble(assembler, cart);
						AbstractContraptionEntity entity = cart.getPassengers().stream().filter(AbstractContraptionEntity.class::isInstance)
								.map(AbstractContraptionEntity.class::cast).findFirst()
								.orElseThrow(() -> new IllegalStateException("Create cart did not assemble a backpack contraption"));
						context.journal().recordCreatedEntity(entity);
						if (ContraptionHelper.getMountedItemStorages(entity).keySet().stream()
								.noneMatch(pos -> ContraptionHelper.getMountedStorage(entity, pos) instanceof MountedSophisticatedBackpack)) {
							throw new IllegalStateException("Create contraption has no mounted sophisticated backpack");
						}
						context.values().put(arguments.saveAs(), UUID.class, entity.getUUID());
					});
				}
				return await(future);
			}
		};
	}

	private static void tryAssemble(CartAssemblerBlockEntity assembler, Entity cart) {
		try {
			Method method = java.util.Arrays.stream(assembler.getClass().getMethods()).filter(candidate -> candidate.getName().equals("tryAssemble"))
					.filter(candidate -> candidate.getParameterCount() == 1 && candidate.getParameterTypes()[0].isInstance(cart)).findFirst()
					.orElseThrow(() -> new IllegalStateException("Create cart assembler does not expose tryAssemble"));
			method.invoke(assembler, cart);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot access Create cart assembler", e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException("Create cart assembly failed", e.getCause());
		}
	}
}
