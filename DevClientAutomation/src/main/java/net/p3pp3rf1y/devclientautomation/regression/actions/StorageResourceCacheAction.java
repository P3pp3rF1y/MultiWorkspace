package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class StorageResourceCacheAction extends TypedRegressionAction<StorageResourceCacheAction.Arguments> {
	public record Arguments(String operation, String ref, int invalidations, boolean replaced) {
	}
	private record Probe(BlockCapabilityCache<IFluidHandler, Direction> fluid, BlockCapabilityCache<IEnergyStorage, Direction> energy,
			IFluidHandler originalFluid, IEnergyStorage originalEnergy, AtomicInteger fluidInvalidations, AtomicInteger energyInvalidations) {
	}

	public StorageResourceCacheAction() {
		super("capability.storageResourceCache", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("operation", "ref", "invalidations", "replaced"), description);
		String operation = requireString(args, "operation", description);
		if (!Set.of("capture", "assert").contains(operation)) {
			throw new IllegalArgumentException("Unsupported cache operation on " + description);
		}
		if (args.has("replaced") && (!args.get("replaced").isJsonPrimitive() || !args.get("replaced").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument replaced must be boolean on " + description);
		}
		return new Arguments(operation, RegressionValueStore.requireName(requireString(args, "ref", description)),
				optionalNonNegativeInt(args, "invalidations", 0, description), args.has("replaced") && args.get("replaced").getAsBoolean());
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
						if (arguments.operation().equals("capture")) {
							AtomicInteger fluids = new AtomicInteger();
							AtomicInteger energy = new AtomicInteger();
							BlockCapabilityCache<IFluidHandler, Direction> fluidCache = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK,
									player.serverLevel(), context.target(), Direction.UP, () -> true, fluids::incrementAndGet);
							BlockCapabilityCache<IEnergyStorage, Direction> energyCache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK,
									player.serverLevel(), context.target(), Direction.UP, () -> true, energy::incrementAndGet);
							context.values().put(arguments.ref(), Probe.class,
									new Probe(fluidCache, energyCache, fluidCache.getCapability(), energyCache.getCapability(), fluids, energy));
						} else {
							Probe probe = context.values().require(arguments.ref(), Probe.class);
							if (probe.fluidInvalidations().get() != arguments.invalidations()
									|| probe.energyInvalidations().get() != arguments.invalidations()) {
								throw new IllegalStateException("Expected " + arguments.invalidations() + " cache invalidations, got fluid="
										+ probe.fluidInvalidations().get() + ", energy=" + probe.energyInvalidations().get());
							}
							boolean fluidReplaced = probe.fluid().getCapability() != probe.originalFluid();
							boolean energyReplaced = probe.energy().getCapability() != probe.originalEnergy();
							if (fluidReplaced != arguments.replaced() || energyReplaced != arguments.replaced()) {
								throw new IllegalStateException("Cached resource handler replacement did not match " + arguments.replaced());
							}
						}
					});
				}
				return await(future);
			}
		};
	}
}
