package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class TransferStorageResourceAction extends TypedRegressionAction<TransferStorageResourceAction.Arguments> {
	public record Arguments(String ref, String operation, int amount, int expected, String fluid, boolean simulate) {
	}

	public TransferStorageResourceAction() {
		super("capability.transferStorageResource", "1", Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "operation", "amount", "expected", "fluid", "simulate"), description);
		String operation = requireString(args, "operation", description);
		if (!Set.of("fill", "drain", "receive", "extract").contains(operation)) {
			throw new IllegalArgumentException("Unknown resource transfer " + operation);
		}
		String fluid = optionalString(args, "fluid", "water", description);
		if (!Set.of("water", "lava").contains(fluid)) {
			throw new IllegalArgumentException("Unknown transfer fluid " + fluid);
		}
		if (args.has("simulate") && (!args.get("simulate").isJsonPrimitive() || !args.get("simulate").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument simulate on " + description + " must be boolean");
		}
		int amount = requirePositiveInt(args, "amount", description);
		return new Arguments(optionalString(args, "ref", "", description), operation, amount, optionalNonNegativeInt(args, "expected", amount, description),
				fluid, args.has("simulate") && args.get("simulate").getAsBoolean());
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
						int transferred;
						try (Transaction transaction = Transaction.openRoot()) {
							if (arguments.operation().equals("fill") || arguments.operation().equals("drain")) {
								ResourceHandler<FluidResource> fluid = entity == null
										? player.level().getCapability(Capabilities.Fluid.BLOCK, context.target(), Direction.UP)
										: entity.getCapability(Capabilities.Fluid.ENTITY, Direction.UP);
								if (fluid == null) {
									throw new IllegalStateException("No fluid capability at endpoint");
								}
								FluidResource resource = FluidResource.of(arguments.fluid().equals("water") ? Fluids.WATER : Fluids.LAVA);
								if (arguments.operation().equals("fill")) {
									transferred = fluid.insert(resource, arguments.amount(), transaction);
								} else {
									int resourceIndex = TransferResourceSelection.firstNonEmptyIndex(fluid.size(),
											index -> !fluid.getResource(index).isEmpty());
									transferred = resourceIndex < 0 ? 0 : fluid.extract(fluid.getResource(resourceIndex), arguments.amount(), transaction);
								}
							} else {
								EnergyHandler energy = entity == null
										? player.level().getCapability(Capabilities.Energy.BLOCK, context.target(), Direction.UP)
										: entity.getCapability(Capabilities.Energy.ENTITY, Direction.UP);
								if (energy == null) {
									throw new IllegalStateException("No energy capability at endpoint");
								}
								transferred = arguments.operation().equals("receive")
										? energy.insert(arguments.amount(), transaction)
										: energy.extract(arguments.amount(), transaction);
							}
							TransferResourceSelection.requireExpectedTransfer(transferred, arguments.expected());
							if (!arguments.simulate()) {
								transaction.commit();
							}
						}
					});
				}
				return await(future);
			}
		};
	}
}
