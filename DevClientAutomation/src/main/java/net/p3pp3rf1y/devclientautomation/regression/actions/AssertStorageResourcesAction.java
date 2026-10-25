package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
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
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.ref().isEmpty()
				? RegressionValueContract.EMPTY
				: RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE);
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
						ResourceHandler<FluidResource> fluid = entity == null
								? player.level().getCapability(Capabilities.Fluid.BLOCK, context.target(), Direction.UP)
								: entity.getCapability(Capabilities.Fluid.ENTITY, Direction.UP);
						EnergyHandler energy = entity == null
								? player.level().getCapability(Capabilities.Energy.BLOCK, context.target(), Direction.UP)
								: entity.getCapability(Capabilities.Energy.ENTITY, Direction.UP);
						if (!arguments.present()) {
							if (fluid != null || energy != null) {
								throw RegressionActionFailure.assertion("Endpoint unexpectedly exposes fluid or energy");
							}
							return;
						}
						LinkedStorageEndpointData endpoint = entity == null
								? ((StorageBlockEntity) player.level().getBlockEntity(context.target())).getLinkedStorageEndpointData()
								: ((IMovingStorageEntity) entity).getStorageItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
						if (endpoint == null) {
							throw RegressionActionFailure.infrastructure("Resource endpoint has no linked group");
						}
						context.putDiagnostic("linkedGroup", endpoint.groupId().toString());
						IStorageWrapper host = (IStorageWrapper) LinkedStorageGroupsSavedData.get(player.level()).manager().resolveVirtualHost(endpoint, false)
								.orElseThrow();
						ResourceHandler<FluidResource> canonicalFluid = fluid instanceof ContentsFilteredFluidHandler filtered
								? filtered.fluidHandler()
								: fluid;
						if (canonicalFluid != host.getFluidHandler().orElse(null) || energy != host.getEnergyHandler().orElse(null)) {
							throw RegressionActionFailure.assertion("Endpoint capabilities are not the canonical non-item handlers");
						}
						if (fluid == null || energy == null || fluid.size() == 0 || fluid.getAmountAsLong(0) != arguments.fluid()
								|| energy.getAmountAsLong() != arguments.energy()) {
							throw RegressionActionFailure.assertion("Unexpected canonical fluid/energy amounts");
						}
						RenderData render = entity == null
								? ((StorageBlockEntity) player.level().getBlockEntity(context.target())).getStorageWrapper().getRenderDataHandler().getData()
								: ((IMovingStorageEntity) entity).getStorageItem().getOrDefault(ModCoreDataComponents.RENDER_DATA, RenderData.EMPTY);
						if (!render.tanks().isEmpty() || render.battery().isPresent()) {
							throw RegressionActionFailure.assertion("Storage endpoint contains tank or battery render projection");
						}
					});
				}
				return await(future);
			}
		};
	}
}
