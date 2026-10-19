package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstorage.block.ContentsFilteredFluidHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertStorageResourcesAction extends TypedRegressionAction<AssertStorageResourcesAction.Arguments> {
	public record Arguments(String ref, int fluid, int energy, boolean present) {
	}

	public AssertStorageResourcesAction() {
		super("assert.storageResources", "1", Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "fluid", "energy", "present"), description);
		if (args.has("present") && (!args.get("present").isJsonPrimitive() || !args.get("present").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument present must be boolean on " + description);
		}
		return new Arguments(optionalString(args, "ref", "", description), optionalNonNegativeInt(args, "fluid", 0, description),
				optionalNonNegativeInt(args, "energy", 0, description), !args.has("present") || args.get("present").getAsBoolean());
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
						Entity entity = arguments.ref().isEmpty()
								? null
								: context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
						IFluidHandler fluid = entity == null
								? player.serverLevel().getCapability(Capabilities.FluidHandler.BLOCK, context.target(), Direction.UP)
								: entity.getCapability(Capabilities.FluidHandler.ENTITY, Direction.UP);
						IEnergyStorage energy = entity == null
								? player.serverLevel().getCapability(Capabilities.EnergyStorage.BLOCK, context.target(), Direction.UP)
								: entity.getCapability(Capabilities.EnergyStorage.ENTITY, Direction.UP);
						if (!arguments.present()) {
							if (fluid != null || energy != null) {
								throw new IllegalStateException("Endpoint unexpectedly exposes fluid or energy");
							}
							return;
						}
						LinkedStorageEndpointData endpoint = entity == null
								? ((StorageBlockEntity) player.serverLevel().getBlockEntity(context.target())).getLinkedStorageEndpointData()
								: ((IMovingStorageEntity) entity).getStorageItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
						if (endpoint == null) {
							throw new IllegalStateException("Resource endpoint has no linked group");
						}
						context.putDiagnostic("linkedGroup", endpoint.groupId().toString());
						IStorageWrapper host = (IStorageWrapper) LinkedStorageGroupsSavedData.get(player.serverLevel()).manager()
								.resolveVirtualHost(endpoint, false).orElseThrow();
						IFluidHandler canonicalFluid = fluid instanceof ContentsFilteredFluidHandler filtered ? filtered.fluidHandler() : fluid;
						if (canonicalFluid != host.getFluidHandler().orElse(null) || energy != host.getEnergyStorage().orElse(null)) {
							throw new IllegalStateException("Endpoint capabilities are not the canonical non-item handlers");
						}
						if (fluid == null || energy == null || fluid.getFluidInTank(0).getAmount() != arguments.fluid()
								|| energy.getEnergyStored() != arguments.energy()) {
							throw new IllegalStateException("Unexpected canonical fluid/energy amounts");
						}
						CompoundTag render = entity == null
								? ((StorageBlockEntity) player.serverLevel().getBlockEntity(context.target())).getStorageWrapper().getRenderInfo().getNbt()
								: ((IMovingStorageEntity) entity).getStorageItem().getOrDefault(ModCoreDataComponents.RENDER_INFO_TAG, CustomData.EMPTY)
										.copyTag();
						if (render.contains("tanks") || render.contains("battery")) {
							throw new IllegalStateException("Storage endpoint contains tank or battery render projection");
						}
					});
				}
				return await(future);
			}
		};
	}
}
