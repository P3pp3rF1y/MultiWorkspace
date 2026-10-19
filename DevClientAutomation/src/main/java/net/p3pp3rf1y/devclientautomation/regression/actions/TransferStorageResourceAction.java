package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

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
						var block = entity == null ? player.serverLevel().getBlockEntity(context.target()) : null;
						int transferred;
						if (arguments.operation().equals("fill") || arguments.operation().equals("drain")) {
							IFluidHandler fluid = entity == null
									? block == null ? null : block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null)
									: entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
							if (fluid == null) {
								throw new IllegalStateException("No fluid capability at endpoint");
							}
							IFluidHandler.FluidAction action = arguments.simulate() ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
							transferred = arguments.operation().equals("fill")
									? fluid.fill(new FluidStack(arguments.fluid().equals("water") ? Fluids.WATER : Fluids.LAVA, arguments.amount()), action)
									: fluid.drain(arguments.amount(), action).getAmount();
						} else {
							IEnergyStorage energy = entity == null
									? block == null ? null : block.getCapability(ForgeCapabilities.ENERGY, Direction.UP).orElse(null)
									: entity.getCapability(ForgeCapabilities.ENERGY, Direction.UP).orElse(null);
							if (energy == null) {
								throw new IllegalStateException("No energy capability at endpoint");
							}
							transferred = arguments.operation().equals("receive")
									? energy.receiveEnergy(arguments.amount(), arguments.simulate())
									: energy.extractEnergy(arguments.amount(), arguments.simulate());
						}
						if (transferred != arguments.expected()) {
							throw new IllegalStateException("Transferred " + transferred + ", expected " + arguments.expected());
						}
					});
				}
				return await(future);
			}
		};
	}
}
